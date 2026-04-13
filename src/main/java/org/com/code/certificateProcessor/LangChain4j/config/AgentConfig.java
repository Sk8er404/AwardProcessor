package org.com.code.certificateProcessor.LangChain4j.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import lombok.Getter;
import org.com.code.certificateProcessor.LangChain4j.agent.ClassificationAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentConfig {

    private final ModelConst modelConst;

    @Autowired
    public AgentConfig(ModelConst modelConst) {
        this.modelConst = modelConst;
    }

    public String getApiKey() {
        return modelConst.getDashScopeApiKey();
    }

    public static final int DimensionOfEmbeddingModel = ModelConst.DimensionOfEmbeddingModel;

    public static final String textEmbeddingModel = ModelConst.DashScopeModel.textEmbeddingModel;
    public static final String qwen3VLFlash = ModelConst.DashScopeModel.qwen3VLFlash;
    public static final String qwenFlash = ModelConst.DashScopeModel.qwenFlash;

    public Builder builder() {
        return new Builder(modelConst.getDashScopeApiKey());
    }

    public static class Builder {
        private final String apiKey;
        private ChatModel chatModel;

        public Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        public Builder chatModel(String modelName) {
            this.chatModel = Model.useQwen()
                    .withConfig(ModelConfig.builder()
                            .apiKey(apiKey)
                            .modelName(modelName)
                            .maxTokens(4096))
                    .build();

            return this;
        }

        public ClassificationAgent buildClassificationAgent() {
            return AiServices.builder(ClassificationAgent.class)
                    .chatModel(chatModel)
                    .build();
        }

        public EmbeddingModel embeddingModel(String embeddingModelName) {
            return Model.useQwen()
                    .withConfig(ModelConfig.builder()
                            .apiKey(apiKey)
                            .modelName(embeddingModelName)
                            .dimension(DimensionOfEmbeddingModel))
                    .buildEmbeddingModel();
        }
    }
}