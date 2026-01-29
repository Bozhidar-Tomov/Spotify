package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//TODO: implement command parcing and init 
// TODO: when stopping a selector use selector.wakeup()
public class ClientHandler implements Runnable {
    private final SelectionKey key;
    private final SpotifySystem system;

    public ClientHandler(SelectionKey key, SpotifySystem system) {
        this.key = key;
        this.system = system;
    }

    @Override
    public void run() {

        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        try {
            // prepares buffer for filling
            buffer.clear();
            int bytesRead = clientChannel.read(buffer);

          
            if (bytesRead == -1) {
                System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
                closeConnection();
                return;
            }
            
            buffer.put("SERVER RESPONSE".getBytes());

            // prepares buffer for emptying
            buffer.flip();
            clientChannel.write(buffer);
            // buffer.flip();
            // byte[] clientData = new byte[buffer.remaining()];
            // buffer.get(clientData);
            // String request = new String(clientData, StandardCharsets.UTF_8).trim();
            // System.out.println("Received from client: " + request);

            // // Echo for now, ensuring newline
            // String response = "SERVER RESPONSE: " + request + "\n";
            // buffer.clear();
            // buffer.put(response.getBytes(StandardCharsets.UTF_8));

            // // prepares buffer for emptying
            // buffer.flip();
            // while (buffer.hasRemaining()) {
            //     clientChannel.write(buffer);
            // }

            if (clientChannel.isOpen()) {
                key.interestOps(SelectionKey.OP_READ);
                key.selector().wakeup();
            }
        } catch (Exception e) {
            System.err.println("Communication error with client: " + e.getMessage());
            closeConnection();
        }
    }

    // private String processCommand(String rawInput, SocketChannel clientChannel) {
    //     String[] tokens = rawInput.split("\\s+");
    //     if (tokens.length == 0 || rawInput.isEmpty()) {
    //         return "Empty command";
    //     }

    //     String commandName = tokens[0];
    //     List<String> args = Arrays.asList(tokens).subList(1, tokens.length);

    //     Command command = CommandFactory.createCommand(commandName);
    //     if (command == null) {
    //         return "Unknown command: " + commandName;
    //     }

    //     return command.execute(args, clientChannel, system);
    // }

    private void closeConnection() {
        try {
            key.cancel();
            key.channel().close();
        } catch (IOException ex) {
            System.err.println("Error closing channel: " + ex.getMessage());
        }
    }
}
