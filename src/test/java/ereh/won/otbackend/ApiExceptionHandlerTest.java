package ereh.won.otbackend;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

	@Test
	void invalidNumberExceptionMapsToBadRequestWithStructuredPayload() {
		ApiExceptionHandler handler = new ApiExceptionHandler();
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURI()).thenReturn("/api/primes/getPrime");

		var response = handler.handleInvalidNumberException(new InvalidNumberException(0), request);

		assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("INVALID_PRIME_POSITION", response.getBody().code());
		assertEquals("Prime position must be greater than zero. Received: 0", response.getBody().message());
		assertEquals("/api/primes/getPrime", response.getBody().path());
		assertNotNull(response.getBody().timestamp());
	}
}
