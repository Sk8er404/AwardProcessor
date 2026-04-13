package org.com.code.certificateProcessor.LangChain4j.config;

import lombok.Getter;
import org.com.code.certificateProcessor.ElasticSearch.ESConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ModelConst {
    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    public static final int DimensionOfEmbeddingModel = ESConst.Vector_DIM;

    public static final class DashScopeModel {
        private DashScopeModel() {}

        public static final String textEmbeddingModel = "text-embedding-v4";
        public static final String qwen3VLFlash = "qwen3-vl-flash";
        public static final String qwenFlash = "qwen-flash";
    }
}
