package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.server.commands.CommandDispatcher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class RequestHandler implements Runnable, ResponseSender{
    private final SelectionKey key;
    private final SpotifySystem system;
    private SocketChannel clientChannel = null;
    private ByteBuffer buffer = null;

    public RequestHandler(SelectionKey key, SpotifySystem system) {
        this.key = key;
        this.system = system;
    }

    private Response parseRequest() throws IOException {
        
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

        return CommandDispatcher.dispatch(request, system, this);
    }

    @Override
    public void sendResponse(Response response) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(response);
            oos.flush();
            byte[] responseBytes = bos.toByteArray();
            send(responseBytes);
        }
    }

    @Override
    public void run() {
        clientChannel = (SocketChannel) key.channel();
        buffer = (ByteBuffer) key.attachment();

        try {
            Response response = parseRequest();

            if (response == null) {
                return;
            }

            sendResponse(response);

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
                sendResponse(Response.err());
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

    private void send(byte[] bytes) throws IOException {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
        lengthBuffer.putInt(bytes.length);
        lengthBuffer.flip();
        
        while (lengthBuffer.hasRemaining()) {
            clientChannel.write(lengthBuffer);
        }

        ByteBuffer payloadBuffer = ByteBuffer.wrap(bytes);
        while (payloadBuffer.hasRemaining()) {
            clientChannel.write(payloadBuffer);
        }
    }
}