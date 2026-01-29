package bg.sofia.uni.fmi.mjt.spotify.client.net;

import java.io.BufferedReader;
import java.io.IOException;

public class ServerListener implements Runnable {
    // TODO: make raw byte buffer so we can tream raw music bytes with framing 1st byte - type, 2nd - size, rest - payload 
    private final BufferedReader reader;

    public ServerListener(BufferedReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Buffered reader cannot be null");
        }

        this.reader = reader;
    }

    public void run() {
        Thread.currentThread().setName("Listener thread");

        try {
            String response;
            while (!Thread.currentThread().isInterrupted()
                    && (response = reader.readLine()) != null) {
                System.out.println("Server: " + response);
            }
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("Listener thread shutting down.");
            } else {
                System.err.println("Connection lost: " + e.getMessage());
            }
        }
    }
}
