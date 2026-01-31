package bg.sofia.uni.fmi.mjt.spotify.common.exceptions;

public class InternalSystemException extends SpotifyException {
    public InternalSystemException(String message) {
        super(message);
    }

    public InternalSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
