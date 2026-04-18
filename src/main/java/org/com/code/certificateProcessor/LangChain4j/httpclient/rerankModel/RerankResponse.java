package org.com.code.certificateProcessor.LangChain4j.httpclient.rerankModel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RerankResponse(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("usage") RerankUsage usage,
        @JsonProperty("output") RerankOutput output
) {
    public record RerankUsage(@JsonProperty("total_tokens") Integer totalTokens) {}

    public record RerankOutput(@JsonProperty("results") List<RerankItem> results) {}

    public record RerankItem(
            @JsonProperty("index") Integer index,
            @JsonProperty("relevance_score") Double relevanceScore,
            @JsonProperty("document") RerankDocument document
    ) {}

    public record RerankDocument(@JsonProperty("text") String text) {}
}