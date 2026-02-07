package bg.sofia.uni.fmi.mjt.spotify.client.audio;

import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class StreamingAudioPlayer implements AudioPlayer {
    private SourceDataLine line;

    @Override
    public synchronized void init(AudioFormatPayload payload) throws LineUnavailableException {
        stop();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, payload.toAudioFormat());
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open();
        line.start();
    }

    @Override
    public synchronized void playChunk(byte[] data) {
        if (line != null && line.isOpen()) {
            line.write(data, 0, data.length);
        }
    }

    @Override
    public synchronized void stop() {
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
            line = null;
        }
    }

    @Override
    public synchronized boolean isActive() {
        return line != null && line.isOpen();
    }

    @Override
    public void close() {
        stop();
    }
}
