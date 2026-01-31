package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.UnknownCommandException;
import java.nio.channels.SocketChannel;
import java.util.List;

public class CommandDispatcher {
    public static String dispatch(String input, SocketChannel clientChannel, SpotifySystem system) {
        if (input == null || input.strip().isEmpty()) {
            throw new UnknownCommandException("No command provided.");
        }

        List<String> tokens = CommandParser.parse(input);

        if (tokens.isEmpty()) {
            throw new UnknownCommandException("No command provided.");
        }

        String commandName = tokens.get(0);
        List<String> args = tokens.subList(1, tokens.size());

        Command command = CommandFactory.createCommand(commandName);
        if (command == null) {
            throw new UnknownCommandException("Command '" + commandName + "' not found.");
        }

        return command.execute(args, clientChannel, system);
    }

}
