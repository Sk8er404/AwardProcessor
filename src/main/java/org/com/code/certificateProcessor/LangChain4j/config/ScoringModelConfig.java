package org.com.code.certificateProcessor.LangChain4j.config;

import lombok.Data;

@Data
public class ScoringModelConfig implements ModelConfig{
    private String apiKey;
    private String url;
    private String modelName;
}
