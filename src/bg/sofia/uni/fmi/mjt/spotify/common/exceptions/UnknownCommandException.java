package bg.sofia.uni.fmi.mjt.spotify.common.exceptions;

public class UnknownCommandException extends SpotifyException {
    public UnknownCommandException(String message) {
        super(message);
    }

    public UnknownCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
