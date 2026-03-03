# Spring AI 1.0.0-M6 升级指南

## 问题描述

在将项目从旧版本Spring AI升级到1.0.0-M6时，遇到了以下编译错误：

1. **类名找不到**：`OllamaChatClient`、`OllamaEmbeddingClient`、`OpenAiEmbeddingClient`等类无法找到
2. **包路径变化**：`PgVectorStore`的包路径发生变化
3. **API方法废弃**：`withDefaultOptions()`方法不存在
4. **构造函数参数变化**：模型类的构造函数需要更多参数
5. **自动装配冲突**：多个`EmbeddingModel` Bean导致自动装配失败

## 原因分析

Spring AI 1.0.0-M6进行了大规模的API重构：

### 1. 命名规范统一
- `*Client` → `*Model`（统一命名为模型而非客户端）
- `OllamaChatClient` → `OllamaChatModel`
- `OllamaEmbeddingClient` → `OllamaEmbeddingModel`
- `OpenAiEmbeddingClient` → `OpenAiEmbeddingModel`

### 2. 包路径调整
- `org.springframework.ai.vectorstore.PgVectorStore` → `org.springframework.ai.vectorstore.pgvector.PgVectorStore`

### 3. API设计变化
- **Builder模式**：`SimpleVectorStore`和`PgVectorStore`改用Builder模式创建
- **构造函数复杂化**：模型类构造函数需要更多参数（`ObservationRegistry`、`ModelManagementOptions`等）
- **配置类细化**：`OllamaOptions`拆分为`OllamaChatOptions`和`OllamaEmbeddingOptions`（但在M6中可能还未完全实现）

### 4. 自动配置增强
Spring AI 1.0.0-M6提供了更完善的Spring Boot自动配置，推荐使用自动配置而非手动创建Bean。


## 解决方案

### 方案选择

考虑到Spring AI 1.0.0-M6的API复杂性，采用**简化配置 + 自动配置**的方案：

1. **删除手动Bean创建**：移除`OllamaApi`、`OpenAiApi`、`OllamaChatModel`等手动配置
2. **使用自动配置**：依赖Spring Boot的自动配置机制
3. **保留必要自定义**：只保留`TokenTextSplitter`和`PgVectorStore`的配置
4. **使用@Qualifier解决冲突**：通过`@Qualifier`注解指定使用哪个`EmbeddingModel`

### 核心变化

#### 1. 简化的OllamaConfig.java

```java
package cn.acheng.airag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class OllamaConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    @Bean
    public PgVectorStore pgVectorStore(
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel, 
            JdbcTemplate jdbcTemplate) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).build();
    }
}
```

#### 2. 关键点说明

- **EmbeddingModel注入**：使用`@Qualifier("ollamaEmbeddingModel")`指定注入Ollama的嵌入模型
- **Builder模式**：`PgVectorStore.builder(jdbcTemplate, embeddingModel).build()`
- **自动配置**：`OllamaChatModel`和`EmbeddingModel`由Spring AI自动创建


## application.yml配置

### Ollama配置示例

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3
          temperature: 0.7
      embedding:
        options:
          model: nomic-embed-text
```

### OpenAI配置示例（可选）

```yaml
spring:
  ai:
    openai:
      base-url: https://api.openai.com
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
      embedding:
        options:
          model: text-embedding-3-small
```

### 数据库配置（PostgreSQL + pgvector）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_database
    username: your_username
    password: your_password
```


## 注意事项

### 1. @Qualifier的使用

当同时配置了Ollama和OpenAI时，Spring会创建两个`EmbeddingModel` Bean：
- `ollamaEmbeddingModel`
- `openAiEmbeddingModel`

必须使用`@Qualifier`注解指定使用哪个：

```java
@Bean
public PgVectorStore pgVectorStore(
        @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
        JdbcTemplate jdbcTemplate) {
    return PgVectorStore.builder(jdbcTemplate, embeddingModel).build();
}
```

### 2. SimpleVectorStore与PgVectorStore冲突

如果同时配置了`SimpleVectorStore`和`PgVectorStore`，可能会导致Bean冲突。建议：
- 开发环境：使用`SimpleVectorStore`（内存存储，快速）
- 生产环境：使用`PgVectorStore`（持久化存储）
- 使用条件注解或注释掉其中一个

### 3. 依赖版本

确保pom.xml中的版本配置正确：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.0</version>
</parent>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0-M6</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```


## 常见问题FAQ

### Q1: 编译错误 "找不到符号: 类 OllamaChatClient"
**A:** Spring AI 1.0.0-M6中类名已改为`OllamaChatModel`，使用自动配置即可，无需手动创建。

### Q2: "Could not autowire. There is more than one bean of 'EmbeddingModel' type"
**A:** 使用`@Qualifier`注解指定使用哪个Bean：
```java
@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel
```

### Q3: PgVectorStore找不到
**A:** 包路径已变更为`org.springframework.ai.vectorstore.pgvector.PgVectorStore`

### Q4: withDefaultOptions()方法不存在
**A:** 新版本使用Builder模式和自动配置，通过application.yml配置模型参数。

### Q5: 如何在Ollama和OpenAI之间切换？
**A:** 修改`@Qualifier`注解的值：
- Ollama: `@Qualifier("ollamaEmbeddingModel")`
- OpenAI: `@Qualifier("openAiEmbeddingModel")`

## 参考资料

### 官方文档（已验证）

- [Spring AI 1.0.0-M6 API Overview](https://docs.spring.io/spring-ai/docs/1.0.0-M6/api/) - 官方API文档
- [Spring AI 1.0.0-M6 Deprecated List](https://docs.spring.io/spring-ai/docs/1.0.0-M6/api/deprecated-list.html) - 废弃API列表
- [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) - 官方升级指南
- [Spring AI Reference - Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html) - 入门文档
- [Spring AI 1.0.0 M7 Released](https://spring.io/blog/2025/04/10/spring-ai-1-0-0-m7-released) - M7版本发布说明（提到结构改进）

### 具体功能文档

- [Spring AI Ollama Chat Reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [Spring AI Ollama Embeddings Reference](https://docs.spring.io/spring-ai/reference/api/embeddings/ollama-embeddings.html)
- [PgVectorStore API文档](https://docs.spring.io/spring-ai/docs/current/api/org/springframework/ai/vectorstore/pgvector/PgVectorStore.html)

### 说明

本文档的技术方案基于：
1. **官方API文档** - 类名和包路径的变化
2. **实际编译错误** - 构造函数参数、方法废弃等问题
3. **问题解决过程** - Builder模式、@Qualifier使用等实践经验

## 总结

Spring AI 1.0.0-M6的升级重点：

1. **拥抱自动配置**：减少手动Bean创建，使用Spring Boot自动配置
2. **使用Builder模式**：VectorStore等类改用Builder模式创建
3. **注意Bean冲突**：使用`@Qualifier`解决多个同类型Bean的注入问题
4. **配置驱动**：通过application.yml配置模型参数，而非代码硬编码

升级后的代码更简洁、更易维护，符合Spring Boot的最佳实践。

---

**文档创建时间**: 2026-03-03  
**Spring AI版本**: 1.0.0-M6  
**Spring Boot版本**: 3.4.0

