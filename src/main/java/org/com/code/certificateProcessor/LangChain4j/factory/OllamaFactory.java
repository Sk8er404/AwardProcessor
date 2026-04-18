package org.com.code.certificateProcessor.LangChain4j.factory;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.com.code.certificateProcessor.LangChain4j.config.*;

public class OllamaFactory implements ModelFactory{
    @Override
    public ChatModel createChatModel(ChatModelConfig config) {
        return OllamaChatModel.builder()
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .timeout(config.getTimeout())
                .maxRetries(config.getMaxRetries())
                .defaultRequestParameters(DefaultChatRequestParameters.builder().responseFormat(config.getResponseFormat()).build())
                .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(StreamingModelConfig config) {
        return OllamaStreamingChatModel.builder()
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .timeout(config.getTimeout())
                .defaultRequestParameters(DefaultChatRequestParameters.builder().responseFormat(config.getResponseFormat()).build())
                .build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig config) {
        return OllamaEmbeddingModel.builder()
                .modelName(config.getModelName())
                .timeout(config.getTimeout())
                .maxRetries(config.getMaxRetries())
                .build();
    }

    @Override
    public ScoringModel createScoringModel(ScoringModelConfig config) {
        return null;
    }
}
