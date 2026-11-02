package io.github.abinesh1006.docsassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code assistant.*}.
 *
 * @param docsDir          folder of Markdown files to answer from
 * @param ingestOnStartup  index the folder when the app starts
 * @param storeFile        where the default (file-backed) vector store is saved between runs
 * @param topK             how many chunks the RAG advisor puts in front of the model
 * @param minSimilarity    chunks below this cosine similarity are not used; 0 to 1
 * @param chunkTokens      target size of one chunk, in tokens
 */
@ConfigurationProperties(prefix = "assistant")
public record AssistantProperties(
        String docsDir,
        boolean ingestOnStartup,
        String storeFile,
        int topK,
        double minSimilarity,
        int chunkTokens) {
}
