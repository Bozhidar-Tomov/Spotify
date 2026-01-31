package bg.sofia.uni.fmi.mjt.spotify.common.exceptions;

public class AuthenticationException extends SpotifyException {
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
