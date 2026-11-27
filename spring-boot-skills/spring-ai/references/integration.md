# Spring AI Integration

Start with the project's Spring Boot and Spring AI BOM versions. Choose provider,
vector-store, and advisor artifacts from that release's reference; artifact names
and APIs change between Spring AI lines. Keep secrets in environment-backed
configuration and never add credentials to examples or committed config.

For the current Spring AI lines, pair 2.1.x with Spring Boot 4.2.x and 2.0.x
with Spring Boot 4.0.x or 4.1.x. Recheck the
[official getting-started](https://docs.spring.io/spring-ai/reference/getting-started.html)
and [upgrade guidance](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
when selecting or upgrading versions.

## Dependencies

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Example provider for chat and embeddings; replace for the project's provider. -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>

    <!-- Spring AI 2.1 session advisors, JDBC repository, and auto-configuration. -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-session-jdbc</artifactId>
    </dependency>

    <!-- RAG with PGVector and QuestionAnswerAdvisor. -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-vector-store-advisor</artifactId>
    </dependency>
</dependencies>
```

Use the Spring AI release compatible with the project's Spring Boot line. Do not
copy starter coordinates or configuration properties from another major version.
Remove optional session or RAG dependencies when the application does not use them.
If the chat provider does not supply embeddings, add a separate embedding provider.

## ChatClient — Basic Usage

```java
@Service
@RequiredArgsConstructor
public class DocumentSummaryService {

    private final ChatClient chatClient;

    public String summarize(String content) {
        return chatClient.prompt()
            .user(u -> u.text("Summarize the following document in 3 bullet points:\n\n{content}")
                .param("content", content))
            .call()
            .content();
    }

    // With system prompt
    public String analyzeFinancial(String document, String language) {
        return chatClient.prompt()
            .system("You are a financial analyst. Respond in {language}.")
            .system(s -> s.param("language", language))
            .user(document)
            .call()
            .content();
    }
}
```

For stateful calls, pass a session identifier that the application has authorized
for the caller. Never use one shared conversation ID for all users.

## Conversation state

Treat a `ChatClient` as stateless. If the application needs conversation history,
use a durable session store appropriate to the retention and privacy requirements,
and bind every session to the authenticated caller and tenant. Never use a shared
default conversation ID.

Spring AI 2.1 provides session advisors. Its older `ChatMemory` advisor API is
deprecated in favor of sessions. For Spring AI 2.0.x, use that release's documented
memory API and supply a caller-scoped conversation ID on every request. Do not mix
snippets across those APIs. See the [session advisor reference](https://docs.spring.io/spring-ai/reference/api/session/chat-client.html).

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    // Configure sessionService with a persistent repository for the application's needs.
    .defaultAdvisors(SessionAdvisors.builder(sessionService).build())
    .build();

String answer = chatClient.prompt()
    .user(message)
    .advisors(SessionAdvisors.session(sessionId))
    .call()
    .content();
```

Keep session creation, expiry, deletion, concurrent-turn handling, and authorization
in the application boundary; a random session ID alone is not authorization. The
dependency example uses the JDBC session starter; manage its production tables
with the application's migration workflow.

## Prompt Templates (externalized)

Put reusable prompt text in `src/main/resources/prompts/` and bind every
placeholder with `.param()`. Send only the fields the task needs; prompt input
remains untrusted data. The [service template](../templates/AiServiceTemplate.java)
and its [prompt](../templates/analyze-order.st) show this pattern.

## Structured Output

Use `.entity(TargetType.class)` or the release's generic type overload to map
structured output. Treat parsing as deserialization, not validation; validate
the resulting values before business use. The service template includes an
example record that rejects invalid output.

## Retrieval augmented generation

Authorize document ingestion and retrieval independently. Apply tenant and caller
filters in the vector-store query, bound the number and size of retrieved chunks,
and treat retrieved text as untrusted input. Do not assume a vector-store advisor
provides access control.

```java
@Configuration
public class RagConfig {

    @Bean
    public ChatClient ragChatClient(ChatClient.Builder builder, VectorStore vectorStore) {
        return builder
            .defaultAdvisors(
                QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(SearchRequest.builder().topK(5).build())
                    .build()
            )
            .build();
    }
}

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final VectorStore vectorStore;
    private final ChatClient ragChatClient;

    // Ingest documents
    public void ingest(List<String> documents) {
        // This simple example is for a public corpus only. For private data,
        // attach authorization metadata and scope both ingestion and retrieval.
        List<Document> docs = documents.stream()
            .map(content -> new Document(content))
            .toList();
        vectorStore.add(docs);
    }

    // Query with RAG
    public String ask(String question) {
        return ragChatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```

## Streaming Responses

```java
public Flux<String> stream(String prompt) {
    return chatClient.prompt()
        .user(prompt)
        .stream()
        .content();
}
```

## Provider configuration

Configure only the provider used by the application. The property names below are
an example; confirm them in the selected Spring AI release's provider reference.

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: ${OPENAI_CHAT_MODEL}
      embedding:
        model: ${OPENAI_EMBEDDING_MODEL}
    vectorstore:
      pgvector:
        dimensions: ${OPENAI_EMBEDDING_DIMENSIONS}
```

Configure vector-store dimensions to match the selected embedding model. Do not
enable automatic schema initialization in production without an explicit migration plan.


Keep prompt placeholders and `.param()` names identical. For structured output,
validate the returned object before using it for business decisions; parsing JSON
does not make model output trustworthy. Retry only transient failures, with a
bounded retry policy and an overall request deadline.

## References

- [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Spring AI session memory](https://docs.spring.io/spring-ai/reference/api/session/chat-client.html)
- [Spring AI session JDBC](https://docs.spring.io/spring-ai/reference/api/session.html)
- [Spring AI RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Spring AI OpenAI chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
- [Spring AI OpenAI embeddings](https://docs.spring.io/spring-ai/reference/api/embeddings/openai-embeddings.html)
