package org.com.code.certificateProcessor.rocketMQ.consumer;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.com.code.certificateProcessor.ElasticSearch.constant.DocField;
import org.com.code.certificateProcessor.ElasticSearch.constant.ESConst;
import org.com.code.certificateProcessor.LangChain4j.httpclient.rerankModel.RerankService;
import org.com.code.certificateProcessor.pojo.modelInfo.AwardClassification;
import org.com.code.certificateProcessor.pojo.modelInfo.DeduplicationResult;
import org.com.code.certificateProcessor.LangChain4j.service.ClassificationService;
import org.com.code.certificateProcessor.LangChain4j.service.OCRService;
import org.com.code.certificateProcessor.exception.AIModelException;
import org.com.code.certificateProcessor.exception.ResourceNotFoundException;
import org.com.code.certificateProcessor.mapper.StandardAwardMapper;
import org.com.code.certificateProcessor.mapper.StudentMapper;
import org.com.code.certificateProcessor.pojo.entity.AwardSubmission;
import org.com.code.certificateProcessor.pojo.entity.StandardAward;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.com.code.certificateProcessor.ElasticSearch.ElasticUtil;
import org.com.code.certificateProcessor.pojo.modelInfo.AwardInfo;
import org.com.code.certificateProcessor.mapper.AwardSubmissionMapper;
import org.com.code.certificateProcessor.pojo.enums.AwardSubmissionStatus;
import org.com.code.certificateProcessor.pojo.enums.DuplicateCheckResult;
import org.com.code.certificateProcessor.rocketMQ.MQConstants;
import org.com.code.certificateProcessor.service.file.FileManageService;
import org.com.code.certificateProcessor.service.file.OSSService;
import org.com.code.certificateProcessor.util.mapKey.AwardInfoMapKey;
import org.com.code.certificateProcessor.util.mapKey.AwardSubmissionMapKey;
import org.com.code.certificateProcessor.util.mapKey.FileUploadMapKey;
import org.com.code.certificateProcessor.util.validator.AIResponseDateValidator;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets; // 3. 导入 Charsets
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RocketMQMessageListener(topic = MQConstants.Topic.SUBMISSION,
        consumerGroup = MQConstants.Consumer.STUDENT_AWARD_SUBMISSION_CONSUMER,
        selectorExpression = MQConstants.Tag.STUDENT_AWARD_SUBMISSION,
        messageModel = MessageModel.CLUSTERING,
        maxReconsumeTimes = 3,
        consumeMode = ConsumeMode.ORDERLY
)
public class studentAwardSubmissionConsumer implements RocketMQListener<MessageExt> {
    @Autowired
    private OCRService ocrService;
    @Autowired
    private AwardSubmissionMapper awardSubmissionMapper;
    @Autowired
    private RedisTemplate<String, Object> objectRedisTemplate;
    @Autowired
    private ElasticUtil elasticUtil;
    @Autowired
    private RerankService rerankService;
    @Autowired
    private StandardAwardMapper standardAwardMapper;
    @Autowired
    private ClassificationService classificationService;
    @Autowired
    StudentMapper studentMapper;

    private static final int CANDIDATE_AWARD_NUM = 15;
    @Autowired
    private OSSService oSSService;

    @Override
    public void onMessage(MessageExt message) {
        String completeUploadInfoJsonMessage = new String(message.getBody(), StandardCharsets.UTF_8);
        /**
         * completeUploadInfo 包含:
         * String imageObjectKey ,String submissionId ,String studentId ,AwardSubmissionStatus status
         */
        Map<String, Object> completeUploadInfo = JSONObject.parseObject(completeUploadInfoJsonMessage, Map.class);
        String submissionId = completeUploadInfo.get(FileUploadMapKey.submissionId).toString();

        try {

            // 我们只“看” (GET)，不“删” (DELETE)
            Object revocationCheck = objectRedisTemplate.opsForHash().get(FileManageService.IfSubmissionGotRevoked, submissionId);

            if (revocationCheck == null) {
                // 如果Key不存在，说明：
                // a) 用户确实撤销了
                // b) 之前的消费“成功”了，并删除了这个key
                // c) 之前的消费失败并转为“人工审核”，并删除了这个key
                // 无论哪种情况，这个消息都不应再被处理。我们直接 `return`，ACK这条消息。
                return;
            }

            String imageObjectKey = completeUploadInfo.get(FileUploadMapKey.imageObjectKey).toString();

            String temporaryCompressedImageUrl = oSSService.generateTemporaryCompressedImageUrl(imageObjectKey,180);

            AwardInfo awardInfo;
            try {
                String ocrResult = ocrService.getOCRResult(temporaryCompressedImageUrl);
                // 1. 先将 AI 响应解析为通用的 Map
                Map<String, Object> ocrMap = JSONObject.parseObject(ocrResult, Map.class);
                if (ocrMap == null) {
                    throw new AIModelException("AI 返回了空的 JSON 响应");
                }

                // 2. 从 Map 中提取可能存在问题的日期字符串
                String awardDateStr = null;
                if (ocrMap.containsKey(AwardInfoMapKey.awardDate)) {
                    Object dateObj = ocrMap.get(AwardInfoMapKey.awardDate);
                    if (dateObj != null) {
                        awardDateStr = dateObj.toString();
                    }
                }

                // 3. 使用校验类来解析和清洗日期
                LocalDate validDate = AIResponseDateValidator.parseAndCleanDate(awardDateStr);

                // 4. 将清洗后的结果放回 Map 中
                // 这一步至关重要：
                // - 如果日期有效，我们将其转为 fastjson 必定认识的标准格式。
                // - 如果日期无效，我们将其从 Map 中移除或设为 null。
                // 这样，fastjson 在第 5 步进行解析时，绝对不会再遇到 "2000-00-00"。
                if (validDate != null) {
                    // 转回 fastjson 认识的标准 ISO 字符串格式
                    ocrMap.put(AwardInfoMapKey.awardDate, validDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                } else {
                    // 设为 null，AwardInfo.java 中的 awardDate 字段将保持 null
                    ocrMap.put(AwardInfoMapKey.awardDate, null);
                }

                // 5. 将 清洗过 的 Map 转换为最终的 AwardInfo 对象
                // 这一步现在是绝对安全的
                awardInfo = JSONObject.parseObject(JSONObject.toJSONString(ocrMap), AwardInfo.class);
            }catch (AIModelException e){
                throw e;
            }
            catch (Exception e) {
                throw new AIModelException("视觉模型分析图片发生错误", e);
            }

            String studentName = studentMapper.getStudentNameById(completeUploadInfo.get(FileUploadMapKey.studentId).toString());
            AwardSubmission awardSubmission = AwardSubmission.builder()
                    .submissionId(completeUploadInfo.get(AwardSubmissionMapKey.submissionId).toString())
                    .status(AwardSubmissionStatus.AI_REJECTED)
                    .ocrFullText(awardInfo.toMap())
                    .build();

            if(awardInfo.getIfCertification().equals("No")){
                awardSubmission.setReason("不是一张奖状或证书");
            }else if(awardInfo.getIsReadable().equals("No")){
                awardSubmission.setReason("图片是奖状或证书，但内容模糊无法准确识别。");
            }else if(awardInfo.getStudentName() == null||awardInfo.getStudentName().isEmpty()){
                awardSubmission.setReason("图片中没有出现学生姓名");
            }else if(!awardInfo.getStudentName().equals(studentName)){
                awardSubmission.setReason("学生姓名不匹配");
            }else if (awardInfo.getAwardName() == null||awardInfo.getAwardName().isEmpty()){
                awardSubmission.setReason("图片中没有出现奖项名称");
            }else{
                /**
                 * 如果是奖状，则 awardInfo 包含以下字段信息
                 *     {
                 *       "studentName": "学生姓名",
                 *       "awardName": "标准奖项名称"
                 *       "awardDate": "2025年12月9日"
                 *       "ifCertification":"Yes"
                 *     }
                 */
                // 开始 RAG 混合搜索
                List<Map> rankedAwardIds = elasticUtil.hybridSearch(
                        awardInfo.getAwardName(), ESConst.IndicesName.STANDARD_AWARD,
                        List.of(DocField.StandardAwardField.AWARD_NAME),
                        List.of(DocField.StandardAwardField.NAME_VECTOR),
                        List.of(DocField.StandardAwardField.STANDARD_AWARD_ID),
                        CANDIDATE_AWARD_NUM);

                List<String> candidateAwardIds =
                        rankedAwardIds.stream()
                                .map(map -> (String) map.get(DocField.StandardAwardField.STANDARD_AWARD_ID))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                if(candidateAwardIds.isEmpty()){
                    throw new ResourceNotFoundException("数据库中还没有任何标准奖项，无法比较");
                }
                List<StandardAward> standardAwards = standardAwardMapper.getAwardsByBatchId(candidateAwardIds);
                if(standardAwards == null || standardAwards.isEmpty()){
                    throw new ResourceNotFoundException("Mysql 数据库和 ElasticSearch 数据不匹配，转人工");
                }

                //开始重排
                List<String> awardNameList = standardAwards.stream().map(StandardAward::getName).toList();

                List<Integer> indexList = rerankService.rerankDocuments(awardInfo.getAwardName(),awardNameList);

                List<StandardAward> rerankedStandardAwardList = indexList.stream()
                        .map(i->{
                            return standardAwards.get(i);
                        }).toList();

                AwardClassification awardClassification =classificationService.getClassificationAgent().classifyAward(awardInfo,rerankedStandardAwardList);

                if(!awardClassification.getMatchFound()){
                    awardSubmission.setReason("奖项匹配结果 : "+awardClassification.getReasoning());
                }

                List<AwardSubmission> awardSubmissions = awardSubmissionMapper.getSubmissionsForDuplicateCheck(
                        completeUploadInfo.get(AwardSubmissionMapKey.studentId).toString(),
                        List.of(AwardSubmissionStatus.AI_APPROVED.toString(),
                                AwardSubmissionStatus.MANUAL_APPROVED.toString()
                        )
                );

                StringBuilder reasonBuilder = new StringBuilder();
                DeduplicationResult deduplicationResult = classificationService.getClassificationAgent().checkForDuplicate(awardInfo,awardSubmissions);
                reasonBuilder.append("奖项匹配结果 : ").append(awardClassification.getReasoning())
                        .append("\n");


                if(deduplicationResult.getDuplicated()){
                    reasonBuilder.append("查重结果 : ").append("可能相似的旧奖项的名称为 <").append(deduplicationResult.getMatchedAwardName())
                            .append(">，认为奖项重复的理由 ：").append(deduplicationResult.getReasoning());

                    awardSubmission.setDuplicateCheckResult(DuplicateCheckResult.IS_DUPLICATE);
                    awardSubmission.setDuplicateSubmissionId(deduplicationResult.getMatchedAwardId());
                    awardSubmission.setReason(reasonBuilder.toString());
                } else{
                    reasonBuilder.append("查重结果 : ").append("可能相似的旧奖项名称为 <")
                            .append(deduplicationResult.getMatchedAwardName())
                            .append(">，认为奖项不重复的理由 ：").append(deduplicationResult.getReasoning());

                    List<Map<String, Object>> standardAwardList = standardAwards.stream().map(StandardAward::toMap).toList();
                    double matchedAwardScore = standardAwards.stream()
                            .filter(award -> award.getStandardAwardId().equals(awardClassification.getMatchedAwardId()))
                            .map(StandardAward::getScore)
                            .findFirst()
                            .orElse(0.0);

                    awardSubmission.setStatus(AwardSubmissionStatus.AI_APPROVED);
                    awardSubmission.setFinalScore(matchedAwardScore);
                    awardSubmission.setMatchedAwardId(awardClassification.getMatchedAwardId());
                    awardSubmission.setDuplicateCheckResult(DuplicateCheckResult.NOT_DUPLICATE);
                    awardSubmission.setReason(reasonBuilder.toString());
                    awardSubmission.setSuggestion(standardAwardList);
                }
            }

            // 业务“成功”后，删除这个key，表示“处理完毕”
            objectRedisTemplate.opsForHash().delete(FileManageService.IfSubmissionGotRevoked, submissionId);
            awardSubmissionMapper.updateAwardSubmission(awardSubmission);
        } catch (ResourceNotFoundException e){
            // 更新数据库状态为 ERROR_NEED_TO_MANUAL_REVIEW
            AwardSubmission awardSubmission = AwardSubmission.builder()
                    .submissionId(submissionId)
                    .status(AwardSubmissionStatus.ERROR_NEED_TO_MANUAL_REVIEW)
                    .reason(e.getMessage())
                    .build();
            awardSubmissionMapper.updateAwardSubmission(awardSubmission);
        } catch (Exception e) {
            // 核心容错逻辑
            int retryCount = message.getReconsumeTimes();

            // 我们要重试3次 (retryCount 0, 1, 2)
            if (retryCount > 2) {
                // 这是第3次失败 (retryCount=2)，不再重试
                LoggerFactory.getLogger(studentAwardSubmissionConsumer.class)
                        .error("消息处理失败 3 次, submissionId: {}. 放弃重试，转为人工审核。", e);

                // 更新数据库状态为 ERROR_NEED_TO_MANUAL_REVIEW
                AwardSubmission awardSubmission = AwardSubmission.builder()
                        .submissionId(submissionId)
                        .status(AwardSubmissionStatus.ERROR_NEED_TO_MANUAL_REVIEW)
                        .reason("图片处理异常: " + e.getMessage())
                        .build();
                awardSubmissionMapper.updateAwardSubmission(awardSubmission);

                // 关键：不抛出异常，消息被“成功消费”，不会再重试或进入死信队列
                // 此时也“删除”key，因为这个消息的处理“已终结”
                objectRedisTemplate.opsForHash().delete(FileManageService.IfSubmissionGotRevoked, submissionId);
            } else {
                // 第1次(retryCount=0)或第2次(retryCount=1)失败，抛出异常以触发重试
                LoggerFactory.getLogger(studentAwardSubmissionConsumer.class)
                        .warn("消息处理失败, submissionId: {}  尝试次数: {}/3 即将重试...", submissionId,retryCount + 1, e);
            }
        }
    }
}
