package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.server.commands.CommandDispatcher;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class RequestHandler implements Runnable {
    private final SelectionKey key;
    private final SpotifySystem system;
    private SocketChannel clientChannel = null;
    private ByteBuffer buffer = null;

    public RequestHandler(SelectionKey key, SpotifySystem system) {
        this.key = key;
        this.system = system;
    }

    private Response parseRequest(ResponseSender sender) throws IOException {
        buffer.clear();
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            closeConnection();
            return null;
        }

        if (bytesRead == 0) {
            return null;
        }

        buffer.flip();
        byte[] clientData = new byte[buffer.remaining()];
        buffer.get(clientData);
        String request = new String(clientData, StandardCharsets.UTF_8).strip();

        return CommandDispatcher.dispatch(request, system, sender);
    }

    @Override
    public void run() {
        clientChannel = (SocketChannel) key.channel();
        buffer = (ByteBuffer) key.attachment();
        ResponseSender sender = new SocketResponseSender(clientChannel);

        try {
            Response response = parseRequest(sender);

            if (response == null) {
                return;
            }

            sender.sendResponse(response);

            if (clientChannel.isOpen()) {
                key.interestOps(SelectionKey.OP_READ);
                key.selector().wakeup();
            }

        } catch (IOException e) {
            System.err.println("Network error with client " + clientChannel + ": " + e.getMessage());
            closeConnection();
        } catch (RuntimeException e) {
            System.err.println("Internal error: " + e.getMessage());
            e.printStackTrace();

            try {
                sender.sendResponse(Response.err());
            } catch (IOException _) {
                System.err.println("Communication error: " + e.getMessage());
                closeConnection();
            }
        }
    }

    private void closeConnection() {
        try {
            key.cancel();
            key.channel().close();
        } catch (IOException ex) {
            System.err.println("Error closing connection: " + ex.getMessage());
        }
    }
}
