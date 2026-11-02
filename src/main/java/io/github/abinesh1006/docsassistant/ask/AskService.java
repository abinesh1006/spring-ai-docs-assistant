package io.github.abinesh1006.docsassistant.ask;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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

    public AskService(ChatClient chatClient) {
        this.chatClient = chatClient;
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
                answer.citedSources() == null ? List.of() : answer.citedSources(),
                retrieved(chat),
                chat == null ? null : chat.getMetadata().getModel());
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
