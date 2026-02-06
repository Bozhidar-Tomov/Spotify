package bg.sofia.uni.fmi.mjt.spotify.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bg.sofia.uni.fmi.mjt.spotify.client.net.ResponseHandler;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;

public class SpotifyClient {
    private static final String HOST_NAME = "localhost";
    private UserDTO user = null;

    private SocketChannel socketChannel;
    private PrintWriter writer;
    private ExecutorService executorListener;

    public void start(int port) throws IOException {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number " + port);
        }

        System.out.println("Connecting to server " + HOST_NAME + ":" + port + "...");

        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(HOST_NAME, port));
        } catch (Exception e) {
            System.out.println("Server unreachable. Connection refused.");
            throw e;
        }

        writer = new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8), true);

        executorListener = Executors.newSingleThreadExecutor();
        executorListener.execute(new ResponseHandler(socketChannel));

        System.out.println("Connected.");
        // connects to server
        // adds server listener
        // adds audio receiver etc
    };

    public void stop() {
        System.out.println("Disconnecting server...");
        try {
            if (executorListener != null) {
                executorListener.shutdownNow();
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }

        System.out.println("Disconnected.");
    };

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public UserDTO getUser() {
        return user;
    }

    public void getUserInput(String message) {
        if (writer == null) {
            System.err.println("Not connected to server.");
            return;
        }
        try {
            writer.println(message);
            //HACK
            System.out.println("SENT command");
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    };
}
