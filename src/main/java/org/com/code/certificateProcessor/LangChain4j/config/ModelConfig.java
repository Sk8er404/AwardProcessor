package org.com.code.certificateProcessor.LangChain4j.config;

import dev.langchain4j.model.chat.request.ResponseFormat;
import lombok.Data;

import java.time.Duration;

@Data
public class ModelConfig {
    private String apiKey;
    private String url;
    private String modelName;
    private Double temperature = 0.7;
    private Integer maxTokens = 4096;
    private ResponseFormat responseFormat;
    private Duration timeout = Duration.ofMinutes(3);
    private Integer maxRetries = 3;
    private Integer dimension;

    public static ModelConfig builder() {
        return new ModelConfig();
    }

    public ModelConfig apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public ModelConfig url(String url) {
        this.url = url;
        return this;
    }

    public ModelConfig modelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public ModelConfig temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public ModelConfig maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public ModelConfig responseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }

    public ModelConfig dimension(Integer dimension) {
        this.dimension = dimension;
        return this;
    }
}
