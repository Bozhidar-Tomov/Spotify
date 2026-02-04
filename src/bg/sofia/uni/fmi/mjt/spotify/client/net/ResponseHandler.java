package bg.sofia.uni.fmi.mjt.spotify.client.net;

import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.BinaryPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;

import javax.sound.sampled.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class ResponseHandler implements Runnable {
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
                    if (line != null) {
                        try {
                            // drain() blocks until the internal buffer is empty
                            line.drain();
                        } catch (Exception ignored) {
                            // verify drain
                        }
                        line.stop();
                        line.close();
                        System.out.println("Playback finished.");
                    }
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
        // 1. Read Length (4 bytes)
        ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
        while (lengthBuffer.hasRemaining()) {
            int bytesRead = clientChannel.read(lengthBuffer);
            if (bytesRead == -1) {
                clientChannel.close();
                return null;
            }
        }
        lengthBuffer.flip();
        int length = lengthBuffer.getInt();

        // 2. Read Payload
        if (length < 0) {
            throw new IOException("Invalid response length: " + length);
        }

        ByteBuffer payloadBuffer = ByteBuffer.allocate(length);
        while (payloadBuffer.hasRemaining()) {
            int bytesRead = clientChannel.read(payloadBuffer);
            if (bytesRead == -1) {
                clientChannel.close();
                throw new IOException("Connection closed while reading payload");
            }
        }
        payloadBuffer.flip();

        byte[] clientData = new byte[payloadBuffer.remaining()];
        payloadBuffer.get(clientData);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(clientData);
                ObjectInputStream ois = new ObjectInputStream(bais)) {

            return (Response) ois.readObject();

        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize Response: Class not found", e);
        }
    }

    private void initAudio(AudioFormatPayload payload) throws LineUnavailableException {
        if (line != null && line.isOpen()) {
            line.close();
        }

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, payload.toAudioFormat());
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open();
        line.start();
    }
}