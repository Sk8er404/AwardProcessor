package org.com.code.certificateProcessor.LangChain4j.constant;

import lombok.Getter;
import org.com.code.certificateProcessor.ElasticSearch.constant.ESConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ModelConst {
    @Value("${spring.ai.dashscope.api-key}")
    private String DASHSCOPE_API_KEY;

    public static final String DASH_SCOPE_URL = "https://dashscope.aliyuncs.com/compatible-api/v1";

    public static final String HTTP_LOCALHOST_11434 = "http://localhost:11434";

    public static final int DimensionOfEmbeddingModel = ESConst.Vector_DIM;

    public static final class DashScopeModel {
        private DashScopeModel() {}

        public static final String textEmbeddingModel = "text-embedding-v4";
        public static final String qwen3VLFlash = "qwen3-vl-flash";
        public static final String qwenFlash = "qwen-flash";
        public static final String qwen3Rerank = "qwen3-vl-rerank";
    }

    public static final class OllamaModel {
        private OllamaModel() {}

        public static final String gemma3 = "gemma3:1b";
        public static final String qwen3_5_0_8b = "qwen3.5:0.8b";

        public static final String snowflake33m_embedding = "snowflake-arctic-embed:33m ";
        public static final String qwen3_embedding = "qwen3-embedding:0.6b";

    }
}
