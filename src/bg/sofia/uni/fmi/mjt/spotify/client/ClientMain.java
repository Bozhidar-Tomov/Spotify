package bg.sofia.uni.fmi.mjt.spotify.client;

import java.util.Scanner;

import bg.sofia.uni.fmi.mjt.spotify.client.view.ConsoleMenu;

public class ClientMain {
    private final SpotifyClient system;
    private final int port;

    ClientMain(int port) {
        this.port = port;
        this.system = new SpotifyClient();
    }

    public static void main(String[] args) {
        final int port = 7777;
        ClientMain client = new ClientMain(port);
        client.start();
    }

    public void start() {
        System.out.println("Starting...");
        try {
            system.start(port);
            ConsoleMenu.displayHeader();
            runInputLoop();
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid configuration: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: Cannot start system: " + e.getMessage());
        } finally {
            System.out.println("Shutting down...");
            system.stop();
        }
    }

    private void runInputLoop() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.hasNextLine() ? scanner.nextLine().strip() : "disconnect";

                if (input.isEmpty())
                    continue;
                if (input.equalsIgnoreCase("disconnect"))
                    break;

                processCommand(input);
            }
        } catch (Exception e) {
            System.err.println("Unexpected error during input: " + e.getMessage());
        }
    }

    private void processCommand(String message) {
        if (message.equalsIgnoreCase("help")) {
            ConsoleMenu.displayCommands();
        } else {
            system.getUserInput(message);
        }
    }
}