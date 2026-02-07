package bg.sofia.uni.fmi.mjt.spotify.server.streaming;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.BinaryPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioStreamer {
    private static final int BUFFER_SIZE = 4096;
    private final ResponseSender sender;
    private final ExecutorService executor;
    private final Track track;

    public AudioStreamer(ResponseSender sender, Track track) {
        this.sender = sender;
        this.executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
        this.track = track;
    }

    public void startStream() {
        executor.execute(() -> {
            try (AudioInputStream audioStream = AudioSystem
                    .getAudioInputStream(new File(track.metadata().filePath()))) {
                AudioFormat format = audioStream.getFormat();
                sender.sendResponse(
                        new Response(200, "Playing " + track.metadata().title(), AudioFormatPayload.from(format)));

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                // HACK
                int i = 0;

                while (!Thread.currentThread().isInterrupted() && (bytesRead = audioStream.read(buffer)) != -1) {
                    byte[] chunk = (bytesRead == buffer.length) ? buffer : Arrays.copyOf(buffer, bytesRead);
                    sender.sendResponse(new Response(200, "STREAM", new BinaryPayload(chunk)));
                    if (i % 100 == 0) {
                        System.out.println("STREAMING #chunk " + i);
                    }
                    ++i;
                }

                endStream();
            } catch (Exception e) {
                System.err.println("Error during playback of " + track.metadata().title() + ": " + e.getMessage());
            }
        });
    }

    public void endStream() {
        executor.shutdownNow();
        try {
            sender.sendResponse(new Response(200, "STREAM_END", null));
        } catch (IOException e) {
            System.err.println("Error sending stream end response: " + e.getMessage());
            throw new InternalSystemException(null);
        }
    }

    public Track track() {
        return Track.copyOf(track);
    }
}