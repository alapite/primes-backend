package ereh.won.otbackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrimesControllerTest {
	@Mock
	private PrimesService primesService;

	@InjectMocks
	private PrimesController primesController;

	@Test
	void getPrimeReturnsOkResponseWithServiceResult() {
		when(primesService.getPrime(10)).thenReturn(29);

		var response = primesController.getPrime(10);

		assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
		assertEquals(29, response.getBody());
		verify(primesService).getPrime(10);
	}
}
