package io.github.abinesh1006.docsassistant.api;

import io.github.abinesh1006.docsassistant.ask.AskService;
import io.github.abinesh1006.docsassistant.ingest.IngestionService;
import io.github.abinesh1006.docsassistant.tools.DocsTools;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AssistantController {

    public record AskRequest(@NotBlank @Size(max = 1000) String question) {}

    private final AskService askService;
    private final IngestionService ingestion;
    private final DocsTools docsTools;

    public AssistantController(AskService askService, IngestionService ingestion, DocsTools docsTools) {
        this.askService = askService;
        this.ingestion = ingestion;
        this.docsTools = docsTools;
    }

    /** Ask a question; answered by Claude from the retrieved documentation. */
    @PostMapping("/ask")
    public AskService.AskResult ask(@Valid @RequestBody AskRequest request) {
        return askService.ask(request.question());
    }

    /** Vector search only, no model call: useful to check what retrieval finds for a question. */
    @GetMapping("/search")
    public List<DocsTools.Snippet> search(@RequestParam("q") String query,
                                          @RequestParam(value = "source", required = false) String source) {
        return docsTools.searchDocuments(query, source);
    }

    /** Re-read the docs folder; changed files replace their old chunks. */
    @PostMapping("/ingest")
    public List<IngestionService.IngestedFile> ingest() {
        return ingestion.ingestAll();
    }

    @GetMapping("/documents")
    public List<IngestionService.IngestedFile> documents() {
        return ingestion.ingestedFiles();
    }
}
