package org.com.code.certificateProcessor.LangChain4j.httpclient;

import org.com.code.certificateProcessor.LangChain4j.httpclient.rerankModel.interceptor.LoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class Client {
    @Bean
    public RestClient.Builder restClientBuilder() {
        // 配置连接工厂（用于设置超时）
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        // HTTP Body 在默认情况下你读过一遍，它就消失了,是一个单向流
        //第一次读取：如果你在 LoggingInterceptor里为了打印日志读取了响应内容。
        //第二次读取：拦截器执行完毕，响应对象传回 RerankService。当 RestClient 执行 .body(RerankResponse.class) 准备解析 JSON 时，
        //它会发现响应流已经被读完了（EOF），于是报错。
        //BufferingClientHttpRequestFactory会把网络返回的原始数据先存入内存（字节数组）。
        //当拦截器读取数据时，读取的是内存里的副本。
        //当后续业务代码（Jackson 解析器）读取数据时，它会重新提供一份完整的内存副本。
        return RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .requestInterceptor(new LoggingInterceptor());

    }
}
