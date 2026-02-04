package bg.sofia.uni.fmi.mjt.spotify.common.exceptions;

public class AmbiguousSourceException extends SpotifyException {
    public AmbiguousSourceException(String message) {
        super(message);
    }

    public AmbiguousSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
