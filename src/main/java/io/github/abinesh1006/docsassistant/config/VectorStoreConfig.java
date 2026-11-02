package io.github.abinesh1006.docsassistant.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;

/**
 * The default vector store: in memory, saved to a JSON file so embeddings survive a restart. Needs no database.
 * With the {@code pgvector} profile this bean is not created and Spring AI's PgVectorStore is used instead.
 */
@Configuration
@Profile("!pgvector")
public class VectorStoreConfig {

    @Bean
    SimpleVectorStore vectorStore(EmbeddingModel embeddingModel, AssistantProperties props) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File file = new File(props.storeFile());
        if (file.isFile()) {
            store.load(file);
        }
        return store;
    }
}
