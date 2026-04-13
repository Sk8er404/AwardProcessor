package org.com.code.certificateProcessor.ElasticSearch.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.com.code.certificateProcessor.ElasticSearch.ESConst;
import org.com.code.certificateProcessor.LangChain4j.service.EmbeddingService;
import org.com.code.certificateProcessor.exeption.ElasticSearchException;
import org.com.code.certificateProcessor.pojo.dto.document.StandardAwardDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * 批量索引方法
     * @param documents
     * @param indexName
     * @param vectorList
     * @throws IOException
     */
    public void bulkIndex(List<StandardAwardDocument> documents, String indexName, List<float[]> vectorList) throws IOException {
        if (documents == null || documents.isEmpty()) {
            return; // 如果没有文档，则直接返回
        }

        for (int i = 0; i < documents.size(); i++) {
            documents.get(i).setNameVector(vectorList.get(i));
            documents.get(i).setIsActive(true);
        }

        // 1. 创建一个BulkOperation列表
        List<BulkOperation> bulkOperations = new ArrayList<>();

        for (StandardAwardDocument doc : documents) {
            // 为每个文档创建一个 "index" 操作
            BulkOperation operation = BulkOperation.of(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(doc.getStandardAwardId()) // 这里需要一个唯一的ID
                            .document(doc)
                    )
            );
            bulkOperations.add(operation);
        }

        // 2. 构建并执行 BulkRequest
        BulkRequest bulkRequest = BulkRequest.of(b -> b
                .operations(bulkOperations)
        );

        client.bulk(bulkRequest);
    }

    public void bulkUpdate(List<StandardAwardDocument> documentsToUpdate, String indexName, List<float[]> vectorList) throws IOException {
        if (documentsToUpdate == null || documentsToUpdate.isEmpty()) {
            return; // 如果没有文档，则直接返回
        }

        int j = 0;
        for (int i = 0; i < documentsToUpdate.size(); i++) {
            if(documentsToUpdate.get(i).getName() != null){
                documentsToUpdate.get(i).setNameVector(vectorList.get(j));
                j++;
            }
        }

        List<BulkOperation> bulkOperations = new ArrayList<>();

        for (StandardAwardDocument doc : documentsToUpdate) {
            BulkOperation operation = BulkOperation.of(op -> op
                    .update(idx -> idx
                            .index(indexName)
                            .id(doc.getStandardAwardId()) // 这里需要一个唯一的ID
                            .action(a -> a
                                    .doc(doc)
                            )
                    )
            );
            bulkOperations.add(operation);
        }
        BulkRequest bulkRequest = BulkRequest.of(b -> b
                .operations(bulkOperations)
        );
        client.bulk(bulkRequest);
    }


    public void bulkDelete(List<String> standardAwardIdList, String indexName) throws IOException {
        if (standardAwardIdList == null || standardAwardIdList.isEmpty()) {
            return; // 如果没有文档，则直接返回
        }

        List<BulkOperation> bulkOperations = new ArrayList<>();

        for (String standardAwardId : standardAwardIdList) {
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
        client.bulk(bulkRequest);
    }

    /**
     * @param keyword
     * @param indexName
     * @param textSearchFields
     * @return
     * @throws IOException
     */
    public List<String> hybridSearch(String keyword, String indexName, List<String> textSearchFields) throws IOException {
        // Step 1: 将用户关键词转换为向量，用于后续的 KNN 搜索
        float[] queryVectorArray = embeddingService.getEmbedding(keyword);
        List<Float> queryVector = new ArrayList<>();
        for (float f : queryVectorArray) {
            queryVector.add(f);
        }

        // 执行基于关键词的全文检索（BM25），返回匹配的文本块
        SearchResponse<Map> textSearchResponse = performTextSearch(keyword, indexName, textSearchFields);

        // 执行基于向量的 KNN 搜索，返回语义相似的文本块
        SearchResponse<Map> knnSearchResponse = performKnnSearch(queryVector,indexName);

        // 使用 RRF（Reciprocal Rank Fusion）算法融合两种搜索结果，
        // 得到按综合相关性排序的文本块 ID 列表（rankedChunkIds）
        List<String> rankedAwardIds = reciprocalRankMerge(textSearchResponse, knnSearchResponse, textWeight, knnWeight);

        return rankedAwardIds;
    }

    public SearchResponse<Map> performTextSearch(String keyword,String indexName, List<String> textSearchFields){
        try {
            // 2. 执行关键字搜索 (Text Search)
            Query textQuery = Query.of(q -> q
                    .multiMatch(m -> m
                            .query(keyword)
                            .fields(textSearchFields)));

            SearchRequest textSearchRequest  = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(textQuery)
                    .size(resultLimit * 2));// 获取更多结果以便合并

            return client.search(textSearchRequest, Map.class);
        } catch (Exception e) {
            throw new ElasticSearchException("执行关键字搜索失败", e);
        }
    }

    public SearchResponse<Map> performKnnSearch(List<Float> queryVector,String indexName) throws IOException {
        try{
            // 3. 执行向量搜索 (KNN Search)
            SearchRequest knnSearchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .knn(k -> k
                            .field(ESConst.StandardAwardField.NameVector.getFieldName())
                            .queryVector(queryVector)
                            .k(10)
                            .numCandidates(100)
                            .similarity(RAGSearchMinSimilarity)
                    )
                    .size(resultLimit * 2) // 获取更多结果以便合并
            );
            return client.search(knnSearchRequest, Map.class);
        }catch (Exception e){
            throw new ElasticSearchException("执行向量搜索失败", e);
        }
    }


    public List<String> reciprocalRankMerge(
            SearchResponse<Map> textResponse,
            SearchResponse<Map> knnResponse,
            double textWeight,
            double knnWeight) {

        Map<String, Double> rrfScores = new HashMap<>();

        // 处理关键字搜索结果：按顺序分配 rank（从 1 开始）
        List<Hit<Map>> textHits = textResponse.hits().hits();
        for (int i = 0; i < textHits.size(); i++) {
            String id = textHits.get(i).id();
            int rank = i + 1; // rank 从 1 开始
            double rrf = textWeight * (1.0 / (RRF_CONSTANT + rank));
            rrfScores.merge(id, rrf, Double::sum); // 累加（支持重复ID）
        }

        // 处理向量搜索结果
        List<Hit<Map>> knnHits = knnResponse.hits().hits();
        for (int i = 0; i < knnHits.size(); i++) {
            String id = knnHits.get(i).id();
            int rank = i + 1;
            double rrf = knnWeight * (1.0 / (RRF_CONSTANT + rank));
            rrfScores.merge(id, rrf, Double::sum);
        }

        // 按 RRF 分数降序排序，返回 ID 列表
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}