package org.com.code.certificateProcessor.LangChain4j.httpclient.rerankModel.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        // 执行请求
        ClientHttpResponse response = execution.execute(request, body);

        // 打印响应状态
        logger.info("响应状态: {}", response.getStatusCode());

        // 读取 Body 并打印（因为有了 Buffering，这里读了之后后续还能读）
        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
        logger.info("响应内容: {}", new String(responseBody, StandardCharsets.UTF_8));

        return response;
    }
}
