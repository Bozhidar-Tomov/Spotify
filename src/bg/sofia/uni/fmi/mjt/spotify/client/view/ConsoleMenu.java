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

    public static void displayCollection(java.util.Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            System.out.println("Nothing found.");
            return;
        }

        final String LINE = "----------------------------------------------------------------";
        final String FORMAT = "| %-4s | %-25s | %-25s |%n";

        System.out.println("\n" + LINE);
        System.out.printf(FORMAT, "#", "Title", "Artist");
        System.out.println(LINE);

        int row = 1;
        for (Object item : collection) {
            String[] details = getDetails(item);
            System.out.printf(FORMAT, row++, truncate(details[0], 25), truncate(details[1], 25));
        }

        System.out.println(LINE + "\n");
    }

    private static String[] getDetails(Object item) {
        return switch (item) {
            case bg.sofia.uni.fmi.mjt.spotify.common.models.Track t ->
                new String[] { t.metadata().title(), t.metadata().artist() };
            default -> new String[] { item.toString(), "-" };
        };
    }

    private static String truncate(String text, int maxLength) {
        if (text == null)
            return "N/A";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}