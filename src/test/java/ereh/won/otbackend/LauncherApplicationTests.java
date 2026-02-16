package ereh.won.otbackend;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LauncherApplicationTests {

    @Autowired
    private PrimesController primesController;

	@Test
	void contextLoads() {
        assertThat(primesController).isNotNull();
	}

}
