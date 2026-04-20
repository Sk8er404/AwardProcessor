package org.com.code.certificateProcessor.ElasticSearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransportConfig;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PreDestroy;
import org.com.code.certificateProcessor.ElasticSearch.constant.ESConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Elasticsearch 配置类
 * 
 * 该类负责配置和初始化 Elasticsearch 客户端，并管理索引的创建
 * 使用 Elasticsearch 提供的新的 Java API Client 替代已废弃的 High Level Client
 * 
 * 主要功能：
 * 1. 创建和配置 Elasticsearch 客户端
 * 2. 管理客户端资源的释放
 * 3. 初始化必要的索引（视频、帖子、用户）
 * 4. 定义索引的映射结构
 */
@Component
public class ElasticConfig {
    // 从 application.yml 配置文件中读取 Elasticsearch 主机地址
    @Value("${spring.elasticsearch.host}")
    String hostAddress;

    @Value("${spring.elasticsearch.apiKey}")
    String apiKey;
    
    // Elasticsearch 客户端实例
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    @Qualifier(value = "JavaTimeModule")
    private ObjectMapper objectMapper;

    @Bean("node1")
    public ElasticsearchClient createRestClient() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {


        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
        // 创建 REST 客户端，连接到配置的主机地址
        elasticsearchClient = ElasticsearchClient.of(b -> b
                .host(hostAddress)
                .apiKey(apiKey)
                .jsonMapper(jsonpMapper)
        );
        return elasticsearchClient;
    }

    @PreDestroy
    public void shutdownScheduler() throws IOException {
        if (elasticsearchClient != null) {
            elasticsearchClient.close();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndices() throws IOException {
        createIndexIfNotExists(ESConst.IndicesName.STANDARD_AWARD, "ElasticSearch/ESIndices/StandardAward.json");
        // 输出初始化完成信息到控制台
        System.out.println("Indices initialized.");
    }

    public void createIndexIfNotExists(String indexName, String mappingResourcePath) throws IOException {
        // 创建索引存在性检查请求
        ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));
        
        // 执行索引存在性检查
        boolean exists = elasticsearchClient.indices().exists(existsRequest).value();
        
        // 如果索引不存在，则创建它
        if (!exists) {
            // 从类路径资源加载映射配置文件
            ClassPathResource resource = new ClassPathResource(mappingResourcePath);
            try (InputStream inputStream = resource.getInputStream();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                
                // 使用提供的映射配置创建索引请求
                CreateIndexRequest createIndexRequest = CreateIndexRequest.of(c -> c
                        .index(indexName)
                        .withJson(reader));
                elasticsearchClient.indices().create(createIndexRequest);
            }
        }
    }
}