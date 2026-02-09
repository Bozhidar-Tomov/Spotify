package bg.sofia.uni.fmi.mjt.spotify.client.net;

import bg.sofia.uni.fmi.mjt.spotify.client.audio.AudioPlayer;
import bg.sofia.uni.fmi.mjt.spotify.client.SpotifyClient;
import bg.sofia.uni.fmi.mjt.spotify.client.view.ConsoleMenu;
import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.BinaryPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.CollectionPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.UserDtoPayload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class ResponseHandler implements Runnable {
    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
    private final SocketChannel clientChannel;
    private final AudioPlayer audioPlayer;
    private final SpotifyClient client;

    public ResponseHandler(SocketChannel clientChannel, AudioPlayer audioPlayer, SpotifyClient client) {
        this.clientChannel = clientChannel;
        this.audioPlayer = audioPlayer;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && clientChannel.isOpen()) {
                Response response = parseResponse();

                if (response == null) {
                    continue;
                }

                handleResponse(response);
            }
        } catch (ClosedChannelException _) {
            System.out.println("Connection closed.");
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleResponse(Response response) {
        if (response.payload() instanceof AudioFormatPayload formatPayload) {
            handleAudioFormat(response.message(), formatPayload);
        } else if (response.payload() instanceof UserDtoPayload userDtoPayload) {
            handleUserDto(response.message(), userDtoPayload);
        } else if (response.payload() instanceof CollectionPayload<?> collectionPayload) {
            handleCollection(response.message(), collectionPayload);
        } else if (response.payload() instanceof BinaryPayload binaryPayload && "STREAM".equals(response.message())) {
            handleAudioStream(binaryPayload);
        } else if ("STREAM_END".equals(response.message())) {
            handleStreamEnd();
        } else {
            handleGenericResponse(response);
        }
    }

    private void handleCollection(String message, CollectionPayload<?> collectionPayload) {
        if (message != null && !message.isEmpty()) {
            System.out.println(message);
        }
        ConsoleMenu.displayCollection(collectionPayload.data());
    }

    private void handleAudioFormat(String message, AudioFormatPayload formatPayload) {
        System.out.println(message);
        try {
            audioPlayer.init(formatPayload);
        } catch (Exception e) {
            System.err.println("Failed to initialize audio: " + e.getMessage());
        }
    }

    private void handleUserDto(String message, UserDtoPayload userDtoPayload) {
        client.setUser(userDtoPayload.data());
        System.out.println(message);
    }

    private void handleAudioStream(BinaryPayload binaryPayload) {
        if (audioPlayer.isActive()) {
            audioPlayer.playChunk(binaryPayload.data());
        }
    }

    private void handleStreamEnd() {
        audioPlayer.stop();
    }

    private void handleGenericResponse(Response response) {
        final int ok = OK;
        if (response.statusCode() != ok) {
            System.err.println("Error: " + response.message());
        } else {
            System.out.println(response.message());
        }
    }

    private Response parseResponse() throws IOException {
        lengthBuffer.clear();
        if (!readFully(lengthBuffer))
            return null;
        int length = lengthBuffer.flip().getInt();

        if (length < 0)
            throw new IOException("Invalid response length: " + length);
        if (length == 0)
            return null;

        ByteBuffer payloadBuffer = ByteBuffer.allocate(length);
        if (!readFully(payloadBuffer))
            return null;

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payloadBuffer.array()))) {
            return (Response) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Deserialization failed", e);
        }
    }

    private boolean readFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (clientChannel.read(buffer) == -1) {
                clientChannel.close();
                return false;
            }
        }
        return true;
    }

    private void cleanup() {
        audioPlayer.close();
    }
}
