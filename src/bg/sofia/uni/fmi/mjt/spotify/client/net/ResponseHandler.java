package bg.sofia.uni.fmi.mjt.spotify.client.net;

import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.BinaryPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class ResponseHandler implements Runnable {
    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
    private final SocketChannel clientChannel;
    private SourceDataLine line;

    public ResponseHandler(SocketChannel clientChannel) throws IOException {
        this.clientChannel = clientChannel;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && clientChannel.isOpen()) {
                Response response = parseResponse();

                if (response == null) {
                    System.out.println("Nothing from server.");
                    continue;
                }

                if (response.payload() instanceof AudioFormatPayload formatPayload) {
                    System.out.println(response.message());
                    initAudio(formatPayload);
                } else if (response.payload() instanceof BinaryPayload binaryPayload
                        && "STREAM".equals(response.message())) {

                    // Streaming logic
                    if (line != null && line.isOpen()) {
                        line.write(binaryPayload.data(), 0, binaryPayload.data().length);
                    }
                } else if ("STREAM_END".equals(response.message())) {
                    stopAudio();
                    System.out.println("Playback finished.");
                } else {
                    if (response.statusCode() != 200) {
                        System.out.println("Error: " + response.message());
                    } else {
                        System.out.println(response.message());
                    }
                }
            }
        } catch (ClosedChannelException e) {
            System.out.println("Connection closed.");
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            if (line != null) {
                line.close();
            }
        }
    }

    private Response parseResponse() throws IOException {
        lengthBuffer.clear();
        readFully(lengthBuffer);
        int length = lengthBuffer.flip().getInt();

        if (length < 0)
            throw new IOException("Invalid response length: " + length);
        if (length == 0)
            return null;

        ByteBuffer payloadBuffer = ByteBuffer.allocate(length);

        readFully(payloadBuffer);

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payloadBuffer.array()))) {
            return (Response) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Deserialization failed", e);
        }
    }

    private void readFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (clientChannel.read(buffer) == -1) {
                clientChannel.close();
                throw new IOException("Connection closed prematurely.");
            }
        }
    }

    private void initAudio(AudioFormatPayload payload) throws LineUnavailableException {
        stopAudio();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, payload.toAudioFormat());
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open();
        line.start();
    }

    private void stopAudio() {
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
            line = null;
        }
    }
}