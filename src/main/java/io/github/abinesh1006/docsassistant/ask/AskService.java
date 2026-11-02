package io.github.abinesh1006.docsassistant.ask;

import io.github.abinesh1006.docsassistant.ingest.IngestionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Answers a question in one ChatClient call. The RAG advisor retrieves matching chunks first; the model may call
 * the search tools; the reply is mapped onto {@link Answer} (Spring AI adds the JSON format instructions and
 * parses the result). The chunks the advisor retrieved come back in the response metadata and are returned as
 * the sources, so a caller can check every answer against the text it came from.
 */
@Service
public class AskService {

    /** What the model must return. */
    public record Answer(String answer, boolean foundInDocs, List<String> citedSources) {}

    public record Source(String source, String section, double score) {}

    public record AskResult(String answer, boolean foundInDocs, List<String> citedSources, List<Source> retrieved, String model) {}

    private final ChatClient chatClient;
    private final IngestionService ingestion;

    public AskService(ChatClient chatClient, IngestionService ingestion) {
        this.chatClient = chatClient;
        this.ingestion = ingestion;
    }

    public AskResult ask(String question) {
        var response = chatClient.prompt()
                .user(question)
                .call()
                .responseEntity(Answer.class);

        Answer answer = Objects.requireNonNull(response.entity(), "The model returned no answer");
        ChatResponse chat = response.response();
        return new AskResult(
                answer.answer(),
                answer.foundInDocs(),
                knownSources(answer.citedSources()),
                retrieved(chat),
                chat == null ? null : chat.getMetadata().getModel());
    }

    /**
     * The model only proposes citations: anything that is not a real ingested file is dropped. The RAG context
     * holds document text, not file names, so a model can invent a plausible name; small local models often do.
     */
    private List<String> knownSources(List<String> cited) {
        if (cited == null) {
            return List.of();
        }
        Set<String> known = ingestion.ingestedFiles().stream()
                .map(IngestionService.IngestedFile::source)
                .collect(Collectors.toSet());
        return cited.stream().filter(known::contains).distinct().toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Source> retrieved(ChatResponse chat) {
        if (chat == null || !(chat.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS) instanceof List<?> docs)) {
            return List.of();
        }
        return ((List<Document>) docs).stream()
                .map(d -> new Source(
                        String.valueOf(d.getMetadata().get("source")),
                        String.valueOf(d.getMetadata().getOrDefault("title", "")),
                        d.getScore() == null ? 0 : d.getScore()))
                .toList();
    }
}
