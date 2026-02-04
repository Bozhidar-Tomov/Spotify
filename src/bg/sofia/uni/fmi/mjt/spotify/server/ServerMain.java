package bg.sofia.uni.fmi.mjt.spotify.server;

import java.util.Scanner;

public class ServerMain {
    private final SpotifySystem system;
    private final int port;

    public ServerMain(int port) {
        this.port = port;
        this.system = SpotifySystem.getInstance();
    }

    public void start() throws Exception {
        system.start(port);
    }

    public void stop() {
        system.stop();
    }

    public static void main(String[] args) {
        // TODO: start several instances of spotifysystem and remove singleton, reverse proxy
        Thread.currentThread().setName("Server Host Thread");

        ServerMain server = new ServerMain(7777);
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Error: cannot start system.");
            return;
        }

        System.out.println("Type 'QUIT' to shut down the server.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
            server.stop();
        }
    }
}
