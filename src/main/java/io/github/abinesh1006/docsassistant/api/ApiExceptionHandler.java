package io.github.abinesh1006.docsassistant.api;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns model-provider failures into clear responses instead of a bare 500. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Bad key, bad request, unknown model: retrying won't help. */
    @ExceptionHandler(NonTransientAiException.class)
    ProblemDetail providerRejected(NonTransientAiException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "The model provider rejected the request: " + e.getMessage());
    }

    /** Rate limit or provider outage that outlasted Spring AI's retries. */
    @ExceptionHandler(TransientAiException.class)
    ProblemDetail providerUnavailable(TransientAiException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "The model provider is busy or unavailable; try again shortly.");
    }
}
