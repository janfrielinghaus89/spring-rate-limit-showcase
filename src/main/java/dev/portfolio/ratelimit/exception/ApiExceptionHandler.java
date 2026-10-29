package dev.portfolio.ratelimit.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(
            RateLimitExceededException exception,
            HttpServletRequest request) {
        long retryAfterSeconds = ceilSeconds(exception.getRetryAfter().toNanos());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Retry after the indicated delay.");
        problem.setTitle("Rate limit exceeded");
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("retryAfterSeconds", retryAfterSeconds);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ValidationError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.");
        problem.setTitle("Invalid request");
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body is missing, malformed, or unreadable.");
        problem.setTitle("Malformed request body");
        problem.setProperty("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(problem);
    }

    private static long ceilSeconds(long nanos) {
        long oneSecond = 1_000_000_000L;
        long fullSeconds = nanos / oneSecond;
        long roundedSeconds = nanos % oneSecond == 0L ? fullSeconds : fullSeconds + 1L;
        return Math.max(1L, roundedSeconds);
    }
}
