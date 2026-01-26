package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import java.nio.channels.SocketChannel;
import java.util.List;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.UnknownCommandException;

public class CommandDispatcher {

    private static final String WHITE_SPAE_REGEX = "\\s+";

    public String dispatch(String input, SocketChannel clientChannel) {
        if (input == null || input.strip().isEmpty()) {
            throw new UnknownCommandException("No command provided.");
        }
        
        List<String> tokens = List.of(input.strip().split(WHITE_SPAE_REGEX));

        String commandName = tokens.get(0);
        List<String> args = tokens.subList(1, tokens.size());
        
        Command command = CommandFactory.createCommand(commandName);
        if (command == null) {
            throw new UnknownCommandException("Command" + commandName + " not found.");
        }
        
        return command.execute(args, clientChannel);
    }
}
