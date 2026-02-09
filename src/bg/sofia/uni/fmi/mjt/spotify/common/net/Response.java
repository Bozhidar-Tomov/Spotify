package bg.sofia.uni.fmi.mjt.spotify.common.net;

import java.io.Serializable;

public record Response(
    int statusCode,
    String message,
    Payload payload) implements Serializable {
        
    public static Response err() {
        final int errCode = INTERNAL_SERVER_ERROR;
        return new Response(errCode, "Internal server error.", null);
    }
}
