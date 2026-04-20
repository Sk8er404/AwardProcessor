package org.com.code.certificateProcessor.ElasticSearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SearchRequestBody;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import lombok.Getter;
import lombok.Setter;
import org.com.code.certificateProcessor.LangChain4j.service.EmbeddingService;
import org.com.code.certificateProcessor.exception.elastic.ElasticGeneralException;
import org.com.code.certificateProcessor.exception.elastic.ReciprocalRankMergeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ElasticUtil {
    @Autowired
    @Qualifier("node1")
    private ElasticsearchClient client;
    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 搜索相似度阈值,只有高于这个阈值，才会被knn的向量相似度查询命中返回给用户,
     * AI的RAG搜索的阈值我设置成0.8，推荐搜索的阈值我设置成0.7
     */
    public static final float RAGSearchMinSimilarity = 0.8f;
    public static final int resultLimit  = 30;

    public static final double textWeight = 0.7;
    public static final double knnWeight = 1.0;

    public static final int RRF_CONSTANT=60;

    // k 是每个分片返回的数量
    public static final int resultNumberReturnedBySingleShard = 20;

    // numCandidates 是每个分片扫描的候选数量（一般为 k 的 5-10 倍）
    public static final int numberOfCandidate =  100;


    /**
     * 批量创建或覆盖 Elasticsearch 文档
     *
     * @param <T>               文档实体类的类型。
     * @param documents         要索引的文档对象列表。
     * @param indexName         目标 Elasticsearch 索引名称。
     * @param idExtractor       ID 提取函数，用于从实体中获取文档唯一标识
     * @param documentPopulator 文档填充器：定义如何定义实体对象内部状态
     * 接受两个参数：(T doc, Integer index)，其中 index 通常用于从外部列表中获取对应的向量或数据。
     * @throws IOException
     */
    public <T> void bulkIndex(
            List<T> documents,
            String indexName,
            Function<T, String> idExtractor,
            BiConsumer<T, Integer> documentPopulator
    ) throws IOException {

        if (documents == null || documents.isEmpty()) {
            return; // 如果没有文档，则直接返回
        }

        // 1. 创建一个BulkOperation列表
        List<BulkOperation> bulkOperations = new ArrayList<>();

        for (int i = 0; i < documents.size(); i++) {
            T doc = documents.get(i);

            documentPopulator.accept(doc,i);

            bulkOperations.add(BulkOperation.of(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(idExtractor.apply(doc))
                            .document(doc)
                    )
            ));
        }

        // 2. 构建并执行 BulkRequest
        BulkRequest bulkRequest = BulkRequest.of(b -> b
                .operations(bulkOperations)
        );

        BulkResponse response = client.bulk(bulkRequest);
        responsePrinter(response);
    }

    /**
     * 批量更新 Elasticsearch 文档。
     *
     * @param <T>               文档实体类的类型
     * @param documentsToUpdate 待更新的文档对象列表
     * @param indexName         Elasticsearch 索引名称
     * @param idExtractor       ID 提取器：定义如何从实体对象中获取 ES 文档 ID
     * @param documentPopulator 文档填充器：定义如何更新实体对象内部状态。
     * 接受两个参数：(T doc, Integer index)，其中 index 通常用于从外部列表中获取对应的向量或数据。
     * @throws IOException
     */
    public <T> void bulkUpdate(
            List<T> documentsToUpdate,
            String indexName,
            Function<T, String> idExtractor,
            BiConsumer<T, Counter> documentPopulator
    ) throws IOException {

        if (documentsToUpdate == null || documentsToUpdate.isEmpty()) {
            return; // 如果没有文档，则直接返回
        }

        List<BulkOperation> bulkOperations = new ArrayList<>();

        Counter counter = new Counter();
        for (int i = 0; i < documentsToUpdate.size(); i++) {
            T doc = documentsToUpdate.get(i);

            documentPopulator.accept(doc,counter);

            bulkOperations.add(BulkOperation.of(op -> op
                    .update(idx -> idx
                            .index(indexName)
                            .id(idExtractor.apply(doc)) // 这里需要一个唯一的ID
                            .action(a -> a
                                    .doc(doc)
                            )
                    )
            ));

        }

        BulkRequest bulkRequest = BulkRequest.of(b -> b
                .operations(bulkOperations)
        );
        BulkResponse response = client.bulk(bulkRequest);
        responsePrinter(response);
    }

    @Getter
    @Setter
    public class Counter{
        private int counterNum = 0;
    }

    /**
     * 批量删除 Elasticsearch 文档
     *
     * @param idList
     * @param indexName
     * @throws IOException
     */

    public void bulkDelete(List<String> idList, String indexName) throws IOException {
        if (idList == null || idList.isEmpty()) {
            return; // 如果没有文档，则直接返回
        }

        List<BulkOperation> bulkOperations = new ArrayList<>();

        for (String standardAwardId : idList) {
            BulkOperation operation = BulkOperation.of(op -> op
                    .delete(idx -> idx
                            .index(indexName)
                            .id(standardAwardId) // 这里需要一个唯一的ID
                    )
            );
            bulkOperations.add(operation);
        }
        BulkRequest bulkRequest = BulkRequest.of(b -> b
                .operations(bulkOperations)
        );
        BulkResponse response = client.bulk(bulkRequest);
        responsePrinter(response);
    }

    /**
     *
     * @param keyword
     * @param indexName
     * @param textFieldList
     * @param vectorFieldList
     * @param sourceFieldToRemain
     * @return
     * 假设 sourceFieldToRemain = {"title_text", "content"}
     *
     * 最终返回 Map 列表 ,
     * {
     *     {
     *         "title_text":xxx,
     *         "content":xxx
     *     },
     *     {
     *         "title_text":xxx,
     *         "content":xxx
     *     },
     *     {
     *         "title_text":xxx,
     *         "content":xxx
     *     }
     * }
     */
    public List<Map> hybridSearch(//进行搜索的关键字
                                  String keyword,
                                  //文档所在的索引名
                                  String indexName,
                                  //进行BM25搜索的字段名列表
                                  List<String> textFieldList,
                                  //进行向量搜索的字段名
                                  List<String> vectorFieldList,
                                  //搜索结果中,一篇文档需要保留的字段,其余字段不返回
                                  List<String> sourceFieldToRemain,
                                  int maxResultSize) throws IOException {
        try {
            // Step 1: 将用户关键词转换为向量，用于后续的 KNN 搜索
            float[] queryVectorArray = embeddingService.getEmbedding(keyword);
            List<Float> queryVector = new ArrayList<>();
            for (float f : queryVectorArray) {
                queryVector.add(f);
            }



            SourceConfig sourceConfig = SourceConfig.of(s -> s
                    .filter(f -> f
                            .includes(sourceFieldToRemain)
                    )
            );

            List<SearchRequestBody> requestBodyList = new ArrayList<>();


            Query textQuery = Query.of(q -> q
                    .multiMatch(m -> m
                            .query(keyword)
                            .fields(textFieldList)));


            SearchRequestBody searchRequestBody  = SearchRequestBody.of(s -> s
                    .source(sourceConfig)
                    .query(textQuery)
                    .size(resultLimit * 2));// 获取更多结果以便合并

            requestBodyList.add(searchRequestBody);

            for(String v : vectorFieldList){
                KnnSearch knnSearch= new KnnSearch.Builder()
                        .field(v)
                        .queryVector(queryVector)
                        .k(resultNumberReturnedBySingleShard)
                        .numCandidates(numberOfCandidate)
                        .similarity(RAGSearchMinSimilarity).build();

                SearchRequestBody knnSearchRequestBody = SearchRequestBody.of(s -> s
                        .source(sourceConfig)
                        .knn(knnSearch)
                        .size(resultLimit * 2) // 获取更多结果以便合并
                );

                requestBodyList.add(knnSearchRequestBody);
            }

            // 使用 MSearch将原本需要多次网络往返的请求合并为一次，显著降低了 I/O 耗时
            MsearchResponse<Map> msearchResponse = client.msearch(m -> m.searches(
                    requestBodyList.stream().map(req -> RequestItem.of(i -> i.header(h -> h.index(indexName)).body(req))).toList()
            ), Map.class);

            List<List<Hit<Map>>> hitsList = new ArrayList<>();
            List<MultiSearchResponseItem<Map>> responseItemList = msearchResponse.responses();
            for (MultiSearchResponseItem m : responseItemList){
                if (m.isFailure()) {
                    System.err.println("MSearch item failed: {}"+ m.failure().error().reason());
                    hitsList.add(Collections.emptyList()); // 填充空列表保证下标对齐
                } else {
                    hitsList.add(m.result().hits().hits());
                }
            }

            // 使用 RRF（Reciprocal Rank Fusion）算法融合两种搜索结果，
            // 得到按综合相关性排序的文本列表
            return reciprocalRankMerge(hitsList, textWeight, knnWeight,maxResultSize);

        }catch (ReciprocalRankMergeException e){
            throw e;
        }catch (Exception e){
            throw new ElasticGeneralException("ES混合搜索发生错误",e);
        }
    }


    public List<Map> reciprocalRankMerge(
            List<List<Hit<Map>>> hitsList,
            double textWeight,
            double knnWeight,
            int maxResultSize) throws ReciprocalRankMergeException{

        /**
         * rrfRank存储
         * {
         *     "id":xxx,
         *     "rrfScore":xxx
         * }
         * id为 ES 文档的唯一标识符,rrfScore为这个文档的相关性得分
         */

        Map<String, Double> rrfRank = new HashMap<>();

        /**
         * 假设 sourceFieldToRemain = {"title_text", "content"}
         * chunkMap 存储
         * {
         *     "id":xxx,
         *     {
         *          "title_text":xxx
         *          "content":xxx
         *     }
         * }
         * id为 ES 文档的唯一标识符,剩下的为 Map ,而这个 Map 存储的就是我们的文档内容
         */
        Map<String,Map> chunkMap = new  HashMap<>();


        List<Hit<Map>> textHits = hitsList.get(0);
        // 处理关键字搜索结果：按顺序分配 rank（从 1 开始）
        for (int i = 0; i < textHits.size(); i++) {
            //元数据对应文档的唯一标识符Id
            String id = textHits.get(i).id();
            //元数据
            Map source = textHits.get(i).source();

            int rank = i + 1; // rank 从 1 开始
            double rrfScore = textWeight * (1.0 / (RRF_CONSTANT + rank));
            chunkMap.putIfAbsent(id, source);
            rrfRank.merge(id, rrfScore, Double::sum);
        }

        for (int i=1;i<hitsList.size();i++){
            List<Hit<Map>> knnHits = hitsList.get(i);

            for (int j = 0; j < knnHits.size(); j++) {
                String id = knnHits.get(j).id();
                Map source = knnHits.get(j).source();

                int rank = j + 1;
                double rrfScore = knnWeight * (1.0 / (RRF_CONSTANT + rank));
                chunkMap.putIfAbsent(id, source);
                rrfRank.merge(id, rrfScore, Double::sum);
            }
        }

        // 按 RRF 分数降序排序
        return rrfRank.entrySet().stream()
                .sorted((o,t)-> Double.compare(t.getValue(), o.getValue()))
                .map(entry -> chunkMap.get(entry.getKey()))
                .filter(Objects::nonNull)
                .limit(maxResultSize)
                .collect(Collectors.toList());

    }

    public void responsePrinter(BulkResponse response){
        if (response.errors()) {
            System.err.println("Bulk indexing had errors:");
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    System.err.println("Index: " + item.index() +
                            ", ID: " + item.id() +
                            ", Error: " + item.error().reason());
                }
            }
        } else {
            System.out.println("Bulk indexing successful in " + response.took() + " ms");
        }
    }
}