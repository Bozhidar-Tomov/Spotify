package bg.sofia.uni.fmi.mjt.spotify.server.streaming;

import java.io.IOException;

public interface Streamer<T> {
    void startStream(T format, String message) throws IOException;

    void sendChunk(byte[] data) throws IOException;

    void endStream() throws IOException;
}
