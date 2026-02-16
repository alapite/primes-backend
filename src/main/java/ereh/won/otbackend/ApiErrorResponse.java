package ereh.won.otbackend;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, String path, Instant timestamp) {
}
