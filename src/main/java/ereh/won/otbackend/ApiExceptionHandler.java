package ereh.won.otbackend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final String INVALID_PRIME_POSITION_CODE = "INVALID_PRIME_POSITION";

    @ExceptionHandler(InvalidNumberException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidNumberException(InvalidNumberException exception,
                                                                         HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                INVALID_PRIME_POSITION_CODE,
                exception.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
