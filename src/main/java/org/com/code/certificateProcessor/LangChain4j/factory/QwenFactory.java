package org.com.code.certificateProcessor.LangChain4j.factory;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import org.com.code.certificateProcessor.LangChain4j.config.ModelConfig;

@AllArgsConstructor
public class QwenFactory implements ModelFactory {

    @Override
    public ChatModel createChatModel(ModelConfig config) {
        return QwenChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .temperature(config.getTemperature().floatValue())
                .maxTokens(config.getMaxTokens())
                .defaultRequestParameters(DefaultChatRequestParameters.builder().responseFormat(config.getResponseFormat()).build())
                .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(ModelConfig config) {
        return QwenStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .temperature(config.getTemperature().floatValue())
                .maxTokens(config.getMaxTokens())
                .defaultRequestParameters(DefaultChatRequestParameters.builder().responseFormat(config.getResponseFormat()).build())
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(ModelConfig config) {
        return QwenEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .dimension(config.getDimension())
                .build();
    }
}
