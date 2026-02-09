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
                   - top-global <number>
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

        final String line = "----------------------------------------------------------------";
        final int fieldLen = 25;
        final String format = "| %-4s | %-" + fieldLen + "s | %-" + fieldLen + "s |%n";

        System.out.println("\n" + line);
        System.out.printf(format, "#", "Title", "Artist");
        System.out.println(line);

        int row = 1;
        for (Object item : collection) {
            String[] details = getDetails(item);
            System.out.printf(format, row++, truncate(details[0], fieldLen), truncate(details[1], fieldLen));
        }

        System.out.println(line + "\n");
    }

    private static String[] getDetails(Object item) {
        return switch (item) {
            case bg.sofia.uni.fmi.mjt.spotify.common.models.Track t ->
                new String[] { t.metadata().title(), t.metadata().artist() };
            default -> new String[] { item.toString(), "-" };
        };
    }

    private static String truncate(String text, int maxLength) {
        final int numberPoints = 3;
        if (text == null)
            return "N/A";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - numberPoints) + "...";
    }
}