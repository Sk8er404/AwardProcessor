package org.com.code.certificateProcessor.LangChain4j.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.Getter;
import org.com.code.certificateProcessor.LangChain4j.Model;
import org.com.code.certificateProcessor.LangChain4j.agent.ClassificationAgent;
import org.com.code.certificateProcessor.LangChain4j.config.ChatModelConfig;
import org.com.code.certificateProcessor.LangChain4j.constant.ModelConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Getter
public class ClassificationService {
    private final ClassificationAgent classificationAgent;

    @Autowired
    public ClassificationService(ModelConst modelConst) {
        ChatModel qwenFlash = Model.<ChatModel>useQwen()
                .withConfig(ChatModelConfig.builder()
                        .apiKey(modelConst.getDASHSCOPE_API_KEY())
                        .modelName(ModelConst.DashScopeModel.qwenFlash)
                        .maxTokens(4096))
                .build();

       this.classificationAgent = AiServices.builder(ClassificationAgent.class)
               .chatModel(qwenFlash)
               .build();
    }
}
