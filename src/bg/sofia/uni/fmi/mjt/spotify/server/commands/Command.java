package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import java.nio.channels.SocketChannel;
import java.util.List;

public interface Command {
    String execute(List<String> args, SocketChannel clientChannel);
}
