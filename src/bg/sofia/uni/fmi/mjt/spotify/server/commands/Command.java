package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.util.List;

public interface Command {
    default Response execute(List<String> args, SpotifySystem system) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    default Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        return execute(args, system);
    }
}
