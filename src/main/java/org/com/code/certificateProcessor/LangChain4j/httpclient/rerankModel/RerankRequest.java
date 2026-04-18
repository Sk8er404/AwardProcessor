package org.com.code.certificateProcessor.LangChain4j.httpclient.rerankModel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record RerankRequest(
        String model,
        Input input,
        Parameters parameters
) {
    public record Input(
            Query query,
            List<DocumentItem> documents
    ) {
        public static List<DocumentItem> ofTextList(List<String> textList) {
            List<DocumentItem> itemList = new ArrayList<>();
            for(String text : textList) {
                itemList.add(new DocumentItem(text, null, null));
            }
            return itemList;
        }
    }

    public record Query(String text) {}

    public record DocumentItem(
            String text,
            String image,
            String video
    ){}

    public record Parameters(
            @JsonProperty("return_documents") Boolean returnDocuments,
            @JsonProperty("top_n") Integer topN,
            Double fps
    ) {}
}