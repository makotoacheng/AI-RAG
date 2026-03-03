package cn.acheng.airag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI 1.0.0-M6 配置类
 * 注意：大部分配置已改用Spring Boot自动配置，这里只保留必要的自定义配置
 *
 * 建议在application.yml中配置：
 * spring:
 *   ai:
 *     ollama:
 *       base-url: http://localhost:11434
 *       chat:
 *         options:
 *           model: llama3
 *       embedding:
 *         options:
 *           model: nomic-embed-text
 *     openai:
 *       base-url: https://api.openai.com
 *       api-key: your-api-key
 */
@Configuration
public class OllamaConfig {

    /**
     * 配置文本分割器
     * 用于将长文本按token数量分割成小块，便于向量化和检索
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    /**
     * 配置简单向量存储（内存存储）
     * 使用@Qualifier指定注入ollamaEmbeddingModel
     * @param embeddingModel Ollama嵌入模型
     * @return SimpleVectorStore实例
     * 和向量数据库冲突，去除
     */
//    @Bean
//    public SimpleVectorStore simpleVectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
//        return SimpleVectorStore.builder(embeddingModel).build();
//    }

    /**
     * 配置PostgreSQL向量存储（持久化存储）
     * 使用@Qualifier指定注入ollamaEmbeddingModel
     * @param embeddingModel Ollama嵌入模型
     * @param jdbcTemplate JDBC模板
     * @return PgVectorStore实例
     */
    @Bean
    public PgVectorStore pgVectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).build();
    }
}
