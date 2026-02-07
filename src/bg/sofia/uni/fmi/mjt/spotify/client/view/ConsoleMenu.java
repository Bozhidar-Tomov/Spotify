package bg.sofia.uni.fmi.mjt.spotify.client.view;

import java.util.List;

public final class ConsoleMenu {

    private ConsoleMenu() {
        throw new IllegalStateException("Utility class: do not instantiate.");
    }

    public static void displayHeader() {
        System.out.println("""
            Type 'help' to see commands.
            ---------------------------
            | Spotify Music Streaming |
            ---------------------------
            """);
    }

    public static void displayCommands() {
        System.out.println("""
            Available Commands:
               - register <email> <password>
               - login <email> <password>
               - search <words>
               - top <number>
               - create-playlist <name>
               - add-song-to <playlist_name> <song>
               - show-playlist <playlist_name>
               - get-all-playlists
               - play <song>
               - stop
               - disconnect
            """);
    }

    public static void displayUser(List<String> args) {
        if (args == null || args.isEmpty()) {
            return;
        }
        System.out.println("Logged in as " + args.getFirst());
    }
}