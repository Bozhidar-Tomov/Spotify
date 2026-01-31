package bg.sofia.uni.fmi.mjt.spotify.client.net;

import bg.sofia.uni.fmi.mjt.spotify.client.SpotifyClient;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;

public class ServerListener implements Runnable {
    // TODO: make raw byte buffer so we can tream raw music bytes with framing 1st
    // byte - type, 2nd - size, rest - payload
    private final BufferedReader reader;
    private final SpotifyClient client;

    public ServerListener(BufferedReader reader, SpotifyClient client) {
        if (reader == null || client == null) {
            throw new IllegalArgumentException("Reader and client cannot be null");
        }

        this.reader = reader;
        this.client = client;
    }

    public void run() {
        Thread.currentThread().setName("Listener thread");

        try {
            String response;
            Gson gson = new Gson();
            while (!Thread.currentThread().isInterrupted()
                    && (response = reader.readLine()) != null) {
                if (response.startsWith("{")) {
                    try {
                        UserDTO user = gson.fromJson(response, UserDTO.class);
                        client.setUser(user);
                        System.out.println("Successfully logged in as: " + user.email());
                    } catch (Exception e) {
                        System.out.println("Server: " + response);
                    }
                } else {
                    System.out.println("Server: " + response);
                }
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
