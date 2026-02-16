package ereh.won.otbackend;

public class PrimeUtils {
    private PrimeUtils() {
    }

    public static int calculateNthPrime(int position) {
        if (position < 1) {
            throw new InvalidNumberException(position);
        }

        if (position == 1) {
            return 2;
        }

        int primesFound = 1;
        int candidate = 1;

        while (primesFound < position) {
            candidate += 2;
            if (isPrime(candidate)) {
                primesFound++;
            }
        }

        return candidate;
    }


    public static boolean isPrime(int candidate) {
        if (candidate < 1) {
            throw new InvalidNumberException(candidate);
        }

        if (candidate < 2) {
            return false;
        }

        int checkBound = (int) Math.ceil(Math.sqrt(candidate));
        for (int i = 2; i <= checkBound; i++) {
            if (candidate % i == 0) {
                return false;
            }
        }

        return true;
    }
}
