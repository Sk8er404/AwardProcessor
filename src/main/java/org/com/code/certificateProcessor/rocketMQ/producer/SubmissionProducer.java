package org.com.code.certificateProcessor.rocketMQ.producer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.com.code.certificateProcessor.exception.RocketmqException;
import org.com.code.certificateProcessor.mapper.AwardSubmissionMapper;
import org.com.code.certificateProcessor.pojo.entity.AwardSubmission;
import org.com.code.certificateProcessor.pojo.enums.AwardSubmissionStatus;
import org.com.code.certificateProcessor.util.mapKey.FileUploadMapKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SubmissionProducer {
    @Autowired
    @Qualifier("CustomizedTemplate")
    private RocketMQTemplate producerTemplate;
    @Autowired
    private AwardSubmissionMapper awardSubmissionMapper;

    /**
     * 通用的异步发送消息方法
     */
    public void asyncSendMessage(Object content, String topic, String tag) {
        String msg = JSON.toJSONString(content);
        String destination = topic + ":" + tag;

        Map<String, Object> contentMap = (Map<String, Object>) content;
        // (引用 FileUploadMapKey 来获取键名)
        String hashKey = (String) contentMap.get(FileUploadMapKey.studentId);

        Message message = new GenericMessage<>(msg);
        // 3. 调用支持 hashKey 的 asyncSendOrderly 方法
        // RocketMQ 会自动根据 hashKey 的哈希值计算，确保
        // 同一个 hashKey (studentId) 的消息总是被发送到同一个消息队列(Message Queue)
        producerTemplate.asyncSendOrderly(destination, message,hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("发送成功");
            }

            @Override
            public void onException(Throwable throwable) {
                System.err.println("====== 发送失败了！======");
                throwable.printStackTrace();

                Map<String, Object> completeUploadInfo = JSONObject.parseObject(msg, Map.class);
                String submissionId = completeUploadInfo.get(FileUploadMapKey.submissionId).toString();
                AwardSubmission awardSubmission = AwardSubmission.builder()
                        .submissionId(submissionId)
                        .status(AwardSubmissionStatus.ERROR_NEED_TO_MANUAL_REVIEW)
                        .build();
                awardSubmissionMapper.updateAwardSubmission(awardSubmission);
            }
        });
    }
}
