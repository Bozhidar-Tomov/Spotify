package bg.sofia.uni.fmi.mjt.spotify.common.exceptions;

public class SourceAlreadyExistsException extends SpotifyException{
    public SourceAlreadyExistsException(String message) {
        super(message);
    }

    public SourceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
