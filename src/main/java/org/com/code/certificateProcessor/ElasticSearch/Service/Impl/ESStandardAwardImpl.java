package org.com.code.certificateProcessor.ElasticSearch.Service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.com.code.certificateProcessor.ElasticSearch.constant.ESConst;
import org.com.code.certificateProcessor.ElasticSearch.ElasticUtil;
import org.com.code.certificateProcessor.ElasticSearch.Service.ESStandardAwardService;
import org.com.code.certificateProcessor.LangChain4j.service.EmbeddingService;
import org.com.code.certificateProcessor.exception.elastic.ElasticGeneralException;
import org.com.code.certificateProcessor.pojo.dto.document.StandardAwardDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ESStandardAwardImpl implements ESStandardAwardService {
    @Autowired
    private ElasticUtil elasticUtil;
    @Autowired
    private EmbeddingService embeddingService;
     @Override
    public void bulkCreateStandardAwardIndex(List<StandardAwardDocument> standardAwardDocumentList) {
        try{
            List<String> standardAwardNameList = standardAwardDocumentList.stream().map(StandardAwardDocument::getName).toList();
            List<float[]> ebeddingList = embeddingService.getEmbeddings(standardAwardNameList);

            elasticUtil.bulkIndex(
                    standardAwardDocumentList,
                    ESConst.IndicesName.STANDARD_AWARD,
                    StandardAwardDocument::getStandardAwardId,(doc,i)->{
                        doc.setIsActive(true);
                        doc.setNameVector(ebeddingList.get(i));
                    });

        }catch (Exception e){
            throw new ElasticGeneralException("创建标准奖状ES索引失败",e);
        }
    }

    @Override
    public void deleteStandardAwardIndex(List<String> standardAwardIdList) {
        try{
            elasticUtil.bulkDelete(standardAwardIdList, ESConst.IndicesName.STANDARD_AWARD);
        }catch (Exception e){
            throw new ElasticGeneralException("删除标准奖状ES索引失败",e);
        }
    }

    @Override
    public void updateStandardAwardIndex(List<StandardAwardDocument> standardAwardDocumentList) {
        try{
            List<String> standardAwardNameList = standardAwardDocumentList.stream().map(StandardAwardDocument::getName).toList();
            List<float[]> nameEbeddingList = embeddingService.getEmbeddings(standardAwardNameList);

            elasticUtil.bulkUpdate(
                    standardAwardDocumentList,
                    ESConst.IndicesName.STANDARD_AWARD,
                    StandardAwardDocument::getStandardAwardId,(doc,counter)->{
                        //这一步很重要,因为上传的需要更新的文档中,有的需要更新字段,有的不需要
                        //所以在这个 bulkUpdate 方法执行前的名字向量化处理中
                        //假设 一个文档的列表[0,10],只有第 5 ,8 个文档是需要更新名字的,所以只有第5和8的文档有携带name字段
                        // embeddingService.getEmbeddings 获取这些文档列表的name列表
                        // 返回size为2,且为包含第 5 ,8 个文档的名字的向量列表 ebeddingList
                        // 所以在后续更新文档某个字段的向量的时候必须检查这个向量对应的那个文档字段是否为null
                        // 不为null就说明这个字段对应的向量字段应该更新,刚好对应 ebeddingList.get(j)的向量,j++
                        // j 作为遍历ebeddingList的计数器必须和遍历文档列表的计数器 i 作区分
                        int j = counter.getCounterNum();

                        if (doc.getName() != null) {
                            doc.setNameVector(nameEbeddingList.get(j));
                        }
                        counter.setCounterNum(j + 1);
                    });
        }catch (Exception e){
            throw new ElasticGeneralException("更新标准奖状ES索引失败",e);
        }
    }
}
