package com.claude.rag.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j配置类
 * 配置OpenAI模型、Milvus向量存储等
 */
@Configuration
public class LangChain4jConfig {

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.chat-model}")
    private String chatModel;

    @Value("${openai.embedding-model}")
    private String embeddingModel;

    @Value("${openai.timeout:60s}")
    private Duration timeout;

    @Value("${openai.temperature:0.7}")
    private Double temperature;

    @Value("${openai.max-tokens:1000}")
    private Integer maxTokens;

    @Value("${milvus.host:localhost}")
    private String milvusHost;

    @Value("${milvus.port:19530}")
    private Integer milvusPort;

    @Value("${milvus.database:default}")
    private String milvusDatabase;

    @Value("${milvus.collection-name:documents}")
    private String collectionName;

    @Value("${milvus.dimension:1536}")
    private Integer dimension;

    /**
     * 配置OpenAI Embedding模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .timeout(timeout)
                .build();
    }

    /**
     * 配置OpenAI Chat模型
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName(chatModel)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .build();
    }

    /**
     * 配置Milvus向量存储
     */
    @Bean
    public EmbeddingStore embeddingStore() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .withDatabaseName(milvusDatabase)
                .build();

        return MilvusEmbeddingStore.builder()
                .connectParam(connectParam)
                .collectionName(collectionName)
                .dimension(dimension)
                .build();
    }

    /**
     * 获取Milvus连接参数
     */
    @Bean
    public ConnectParam connectParam() {
        return ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .withDatabaseName(milvusDatabase)
                .build();
    }
}
