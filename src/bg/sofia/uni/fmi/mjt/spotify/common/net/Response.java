package bg.sofia.uni.fmi.mjt.spotify.common.net;

import java.io.Serializable;

public record Response(
    int statusCode,
    String message,
    Payload payload) implements Serializable {
    public static Response err(){
        return new Response(500, "Internal server error.", null);
        }
    }
