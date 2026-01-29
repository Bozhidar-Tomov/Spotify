package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.server.commands.Command;
import bg.sofia.uni.fmi.mjt.spotify.server.commands.CommandFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;

//TODO: implement command parcing and init 
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
            buffer.clear();
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
                closeConnection();
                return;
            }

            buffer.flip();

            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            // String rawInput = new String(bytes).trim();

            // String response = processCommand(rawInput, clientChannel);

            buffer.clear();
            // buffer.put(response.getBytes());
            buffer.flip();

            clientChannel.write(buffer);

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
