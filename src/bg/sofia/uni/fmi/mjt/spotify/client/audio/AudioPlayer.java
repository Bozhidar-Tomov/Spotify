package bg.sofia.uni.fmi.mjt.spotify.client.audio;

import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import javax.sound.sampled.LineUnavailableException;

public interface AudioPlayer extends AutoCloseable {
    /**
     * Initializes the audio line with the given format.
     */
    void init(AudioFormatPayload format) throws LineUnavailableException;

    /**
     * Plays a chunk of audio data.
     */
    void playChunk(byte[] data);

    /**
     * Stops the audio playback and closes the line.
     */
    void stop();

    /**
     * Returns true if the audio line is open and playing.
     */
    boolean isActive();

    @Override
    void close();
}
