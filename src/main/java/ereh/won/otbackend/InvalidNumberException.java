package ereh.won.otbackend;

public class InvalidNumberException extends RuntimeException {

    public InvalidNumberException(int value) {
        super("Prime position must be greater than zero. Received: " + value);
    }
}
