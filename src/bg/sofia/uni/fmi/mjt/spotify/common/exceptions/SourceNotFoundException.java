package bg.sofia.uni.fmi.mjt.spotify.common.exceptions;

public class SourceNotFoundException extends SpotifyException {
    public SourceNotFoundException(String message) {
        super(message);
    }

    public SourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
