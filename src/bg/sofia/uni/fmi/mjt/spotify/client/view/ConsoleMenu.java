package bg.sofia.uni.fmi.mjt.spotify.client.view;

import java.util.List;

public final class ConsoleMenu {
    private ConsoleMenu() {
        throw new IllegalStateException("Utility class: do not instantiate.");
    }
    
    public static void displayHeader() {
        System.out.println(
            "Type 'help' to see commands." + System.lineSeparator() +
            "---------------------------" + System.lineSeparator() +
            "| Spotify Music Streaming |" + System.lineSeparator() +
            "---------------------------" + System.lineSeparator());
    } 

    public static void displayCommands() {
        System.out.println(
                "register <email> <password>" + System.lineSeparator() +
                        "login <email> <password>" + System.lineSeparator() +
                        "search <words>" + System.lineSeparator() +
                        "top <number>" + System.lineSeparator() +
                        "create-playlist <name_of_the_playlist>" + System.lineSeparator() +
                        "add-song-to <name_of_the_playlist> <song>" + System.lineSeparator() +
                        "show-playlist <name_of_the_playlist>" + System.lineSeparator() +
                        "play <song>" + System.lineSeparator() +
                        "stop" + System.lineSeparator() +
                        "disconnect" + System.lineSeparator());
    }
    
    public static void displayUser(List<String> args) {
        System.out.println(
            "Logged in as " + args.getFirst()
        );
    } 
}
