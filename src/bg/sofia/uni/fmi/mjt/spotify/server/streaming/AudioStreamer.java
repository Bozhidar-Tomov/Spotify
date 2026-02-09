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
    private final Track track;
    private final ExecutorService executor;

    public AudioStreamer(ResponseSender sender, Track track) {
        if (sender == null || track == null) {
            throw new IllegalArgumentException("Sender and track cannot be null"); 
        }
        this(sender, track, Executors.newSingleThreadExecutor(Thread.ofVirtual().factory()));
    }

    public AudioStreamer(ResponseSender sender, Track track, ExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("Executor Service cannot be null");
        }
        
        this.sender = sender;
        this.track = track;
        this.executor = executor;
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

                while (!Thread.currentThread().isInterrupted() && (bytesRead = audioStream.read(buffer)) != -1) {
                    byte[] chunk = (bytesRead == buffer.length) ? buffer : Arrays.copyOf(buffer, bytesRead);
                    sender.sendResponse(new Response(200, "STREAM", new BinaryPayload(chunk)));
                }

            } catch (Exception e) {
                System.err.println("Error during playback of " + track.metadata().title() + ": " + e.getMessage());
            } finally {
                endStream();
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