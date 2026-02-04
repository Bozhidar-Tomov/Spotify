package bg.sofia.uni.fmi.mjt.spotify.server.streaming;

import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.BinaryPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;

public class AudioStreamer implements Streamer<AudioFormat> {
    private final ResponseSender sender;

    public AudioStreamer(ResponseSender sender) {
        this.sender = sender;
    }

    @Override
    public void startStream(AudioFormat format, String message) throws IOException {
        sender.sendResponse(new Response(200, message, AudioFormatPayload.from(format)));
    }

    @Override
    public void sendChunk(byte[] data) throws IOException {
        sender.sendResponse(new Response(200, "STREAM", new BinaryPayload(data)));
    }

    @Override
    public void endStream() throws IOException {
        sender.sendResponse(new Response(200, "STREAM_END", null));
    }
}
