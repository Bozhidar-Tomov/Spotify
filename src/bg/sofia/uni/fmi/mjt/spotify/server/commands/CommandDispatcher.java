package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;

import java.util.ArrayList;
import java.util.List;

public final class CommandDispatcher {
    public static Response dispatch(String input, SpotifySystem system, ResponseSender client) {
        if (input == null || input.strip().isEmpty()) {
            return new Response(404, "No command provided", null);
        }

        List<String> tokens = parse(input);

        if (tokens.isEmpty()) {
            return new Response(404, "No command provided", null);
        }

        String commandName = tokens.get(0);
        List<String> args = tokens.subList(1, tokens.size());

        Command command = CommandFactory.createCommand(commandName);
        if (command == null) {
            return new Response(404, "Command '" + commandName + "' not found.", null);
        }

        if (command instanceof PlaySongCommand playCommand) {
            playCommand.setResponseSender(client);
        }

        return command.execute(args, system);
    }

    private static List<String> parse(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;

        for (char c : input.strip().toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);
            }
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        return tokens;
    }

}