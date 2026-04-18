package org.com.code.certificateProcessor.LangChain4j.config;

import dev.langchain4j.model.chat.request.ResponseFormat;
import lombok.Data;

import java.time.Duration;

@Data
public class StreamingModelConfig implements ModelConfig{
    private String apiKey;
    private String url;
    private String modelName;
    private Double temperature = 0.7;
    private Integer maxTokens = 4096;
    private ResponseFormat responseFormat;
    private Duration timeout = Duration.ofMinutes(3);
    private Integer maxRetries = 3;


    public StreamingModelConfig apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public StreamingModelConfig url(String url) {
        this.url = url;
        return this;
    }

    public StreamingModelConfig modelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public StreamingModelConfig temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public StreamingModelConfig maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public StreamingModelConfig responseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }

    public StreamingModelConfig timeout(Duration timeout){
        this.timeout = timeout;
        return this;
    }

    public StreamingModelConfig maxRetries(Integer maxRetries){
        this.maxRetries = maxRetries;
        return this;
    }

    public static ChatModelConfig builder() {
        return new ChatModelConfig();
    }
}
