package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import java.nio.channels.SocketChannel;
import java.util.List;

public class NextSongCommand implements Command {
    @Override
    public String execute(List<String> args, SocketChannel clientChannel) {
        return "To be implemented";
    }
}
