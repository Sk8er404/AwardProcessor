package org.com.code.certificateProcessor.LangChain4j.config;

import dev.langchain4j.model.chat.request.ResponseFormat;
import lombok.Data;

import java.time.Duration;

@Data
public class ChatModelConfig implements ModelConfig{
    private String apiKey;
    private String url;
    private String modelName;
    private Double temperature = 0.7;
    private Integer maxTokens = 4096;
    private ResponseFormat responseFormat;
    private Duration timeout = Duration.ofMinutes(3);
    private Integer maxRetries = 3;

    public ChatModelConfig apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public ChatModelConfig url(String url) {
        this.url = url;
        return this;
    }

    public ChatModelConfig modelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public ChatModelConfig temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public ChatModelConfig maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public ChatModelConfig responseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }

    public ChatModelConfig timeout(Duration timeout){
        this.timeout = timeout;
        return this;
    }

    public ChatModelConfig maxRetries(Integer maxRetries){
        this.maxRetries = maxRetries;
        return this;
    }

    public static ChatModelConfig builder() {
        return new ChatModelConfig();
    }
}
