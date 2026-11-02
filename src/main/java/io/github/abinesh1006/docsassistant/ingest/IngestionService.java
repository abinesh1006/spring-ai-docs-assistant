package io.github.abinesh1006.docsassistant.ingest;

import io.github.abinesh1006.docsassistant.config.AssistantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Loads the Markdown files in {@code assistant.docs-dir} into the vector store: each file is split by heading,
 * long sections are split again into chunks of about {@code assistant.chunk-tokens} tokens, and each chunk is
 * embedded and stored with its file name and section title.
 * <p>
 * Re-ingesting a file first deletes that file's old chunks (a metadata filter on {@code source}), so running it
 * again after an edit replaces the content instead of duplicating it.
 */
@Service
public class IngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    public record IngestedFile(String source, int chunks, List<String> sections) {}

    private final VectorStore vectorStore;
    private final AssistantProperties props;
    private final Map<String, IngestedFile> ingested = new ConcurrentHashMap<>();

    public IngestionService(VectorStore vectorStore, AssistantProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.ingestOnStartup()) {
            ingestAll();
        }
    }

    public List<IngestedFile> ingestAll() {
        Path dir = Path.of(props.docsDir());
        if (!Files.isDirectory(dir)) {
            log.warn("Docs folder {} does not exist; nothing to ingest", dir.toAbsolutePath());
            return List.of();
        }
        List<IngestedFile> result;
        try (Stream<Path> files = Files.list(dir)) {
            result = files.filter(f -> f.toString().endsWith(".md")).sorted().map(this::ingest).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (vectorStore instanceof SimpleVectorStore simple) {
            File file = new File(props.storeFile());
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            simple.save(file);
        }
        return result;
    }

    /** Everything ingested in this run, by file name; the listDocuments tool reads it. */
    public List<IngestedFile> ingestedFiles() {
        return ingested.values().stream().sorted((a, b) -> a.source().compareTo(b.source())).toList();
    }

    private IngestedFile ingest(Path file) {
        String source = file.getFileName().toString();
        var config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(true)
                .withAdditionalMetadata("source", source)
                .build();
        List<Document> sections = new MarkdownDocumentReader(new FileSystemResource(file), config).get();
        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(props.chunkTokens())
                .withMinChunkSizeChars(200)
                .build()
                .apply(sections);

        vectorStore.delete(new FilterExpressionBuilder().eq("source", source).build());
        vectorStore.add(chunks);

        List<String> titles = sections.stream()
                .map(d -> (String) d.getMetadata().get("title"))
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();
        var entry = new IngestedFile(source, chunks.size(), titles);
        ingested.put(source, entry);
        log.info("Ingested {}: {} sections, {} chunks", source, sections.size(), chunks.size());
        return entry;
    }
}
