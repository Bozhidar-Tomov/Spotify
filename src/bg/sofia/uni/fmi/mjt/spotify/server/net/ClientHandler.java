package bg.sofia.uni.fmi.mjt.spotify.server.net;

import java.nio.channels.SelectionKey;

public class ClientHandler implements Runnable {
    private final SelectionKey key;

    public ClientHandler(SelectionKey key) {
        this.key = key;
    }
    
    @Override
    public void run() {
        System.out.println("Processing client request.");
    }
}
