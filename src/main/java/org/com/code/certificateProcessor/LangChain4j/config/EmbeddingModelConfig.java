package org.com.code.certificateProcessor.LangChain4j.config;

import lombok.Data;

import java.time.Duration;

@Data
public class EmbeddingModelConfig implements ModelConfig{
    private String apiKey;
    private String url;
    private String modelName;
    private Duration timeout = Duration.ofMinutes(3);
    private Integer maxRetries = 3;
    private Integer dimension;

    public EmbeddingModelConfig apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public EmbeddingModelConfig url(String url) {
        this.url = url;
        return this;
    }

    public EmbeddingModelConfig modelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public EmbeddingModelConfig timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }
    public EmbeddingModelConfig maxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }
    public EmbeddingModelConfig dimension(Integer dimension) {
        this.dimension = dimension;
        return this;
    }
    public static EmbeddingModelConfig builder() {
        return new EmbeddingModelConfig();
    }
}
