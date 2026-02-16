package ereh.won.otbackend;

import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static ereh.won.otbackend.PrimeUtils.calculateNthPrime;
import static ereh.won.otbackend.PrimeUtils.isPrime;
import static org.junit.jupiter.api.Assertions.*;

class PrimeUtilsTest {

    @ParameterizedTest
    @MethodSource("indexedPrimes")
    void testCalculateNthPrime(int position, int expectedValue) {
        val result = calculateNthPrime(position);
        assertEquals(expectedValue, result);
    }

    private static Stream<Arguments> indexedPrimes() {
        return Stream.of(
            Arguments.of(5, 11),
            Arguments.of(6, 13),
            Arguments.of(7, 17),
            Arguments.of(10, 29),
            Arguments.of(30, 113),
            Arguments.of(50, 229)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {13, 17, 19, 23, 29, 31})
    void testIsPrime(int candidate) {
        assertTrue(isPrime(candidate));
    }

    @ParameterizedTest
    @ValueSource(ints = {12*13, 45, 69, 17*19*31})
    void testIsNotPrime(int candidate) {
        assertFalse(isPrime(candidate));
    }
}