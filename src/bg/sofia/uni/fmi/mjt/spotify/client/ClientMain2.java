package bg.sofia.uni.fmi.mjt.spotify.client;

import java.util.Scanner;

import bg.sofia.uni.fmi.mjt.spotify.client.view.ConsoleMenu;

public class ClientMain2 {
    private final SpotifyClient system;
    private final int port;

    ClientMain2(int port) {
        this.port = port;
        this.system = new SpotifyClient();
    }

    public static void main(String[] args) {
        ClientMain2 client = new ClientMain2(7777);
        client.start();
    }

    public void start() {
        System.out.println("Starting...");

        try {
            system.start(port);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid configuration: " + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Error: Cannot start system: " + e.getMessage());
            return;
        }

        ConsoleMenu.displayHeader();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String message = scanner.nextLine().strip();

                if (message.isEmpty()) {
                    continue;
                }

                if (message.equalsIgnoreCase("disconnect")) {
                    break;
                }

                if (message.equalsIgnoreCase("help")) {
                    ConsoleMenu.displayCommands();
                    continue;
                }
                system.getUserInput(message);
            }
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during input: " + e.getMessage());
        } finally {
            System.out.println("Shutting down...");
            system.stop();
        }
    }
}