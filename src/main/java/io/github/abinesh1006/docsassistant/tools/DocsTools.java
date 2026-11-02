package io.github.abinesh1006.docsassistant.tools;

import io.github.abinesh1006.docsassistant.ingest.IngestionService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tools the model can call during a turn. The RAG advisor already puts the best-matching chunks in front of the
 * model; these let it look further when those are not enough: see which documents exist, then search again with
 * other words or inside one document. Both only read.
 */
@Component
public class DocsTools {

    private static final int MAX_RESULTS = 5;

    public record Snippet(String source, String section, String text) {}

    private final VectorStore vectorStore;
    private final IngestionService ingestion;

    public DocsTools(VectorStore vectorStore, IngestionService ingestion) {
        this.vectorStore = vectorStore;
        this.ingestion = ingestion;
    }

    @Tool(description = "List the documents that can be searched, with the section titles in each.")
    public List<IngestionService.IngestedFile> listDocuments() {
        return ingestion.ingestedFiles();
    }

    @Tool(description = "Search the documentation again, for example with different words or inside one document. "
            + "Returns the most relevant passages with the document and section they come from.")
    public List<Snippet> searchDocuments(
            @ToolParam(description = "What to look for, in plain words") String query,
            @ToolParam(description = "Only search this document (its file name, as listDocuments returns it)", required = false)
            String source) {
        var request = SearchRequest.builder().query(query).topK(MAX_RESULTS);
        if (source != null && !source.isBlank()) {
            request.filterExpression(new FilterExpressionBuilder().eq("source", source).build());
        }
        return vectorStore.similaritySearch(request.build()).stream().map(DocsTools::snippet).toList();
    }

    static Snippet snippet(Document d) {
        return new Snippet(
                String.valueOf(d.getMetadata().get("source")),
                String.valueOf(d.getMetadata().getOrDefault("title", "")),
                d.getText());
    }
}
