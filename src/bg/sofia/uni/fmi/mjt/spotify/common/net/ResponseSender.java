package bg.sofia.uni.fmi.mjt.spotify.common.net;

import java.io.IOException;

@FunctionalInterface
public interface ResponseSender {
    void sendResponse(Response response) throws IOException;
}
