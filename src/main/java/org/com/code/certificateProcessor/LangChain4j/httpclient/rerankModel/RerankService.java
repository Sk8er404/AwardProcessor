package org.com.code.certificateProcessor.LangChain4j.httpclient.rerankModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class RerankService {

    private final RestClient restClient;
    private static final Integer TOP_N = 5;
    private static final Double FPS = 1.0;
    private static final Boolean IF_RETURN_DOCUMENTS = false;

    @Autowired
    public RerankService(RestClient.Builder builder, @Value("${spring.ai.dashscope.api-key}") String apiKey) {
        this.restClient = builder
                .baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public List<Integer> rerankDocuments(String query,List<String> docList) {
        RerankRequest.Input input = new RerankRequest.Input(
                new RerankRequest.Query(query),
                RerankRequest.Input.ofTextList(docList)
        );

        RerankRequest.Parameters parameters = new RerankRequest.Parameters(IF_RETURN_DOCUMENTS, TOP_N, FPS);

        RerankRequest requestData = new RerankRequest("qwen3-vl-rerank", input, parameters);

        RerankResponse rerankResponse = restClient.post()
                .uri("/api/v1/services/rerank/text-rerank/text-rerank") // 使用完整的新路径
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new RuntimeException("API调用失败，状态码：" + response.getStatusCode());
                })
                .body(RerankResponse.class);

        return rerankResponse.output().results().stream()
                .map(t->{
                    return t.index();
                }).toList();
    }
}