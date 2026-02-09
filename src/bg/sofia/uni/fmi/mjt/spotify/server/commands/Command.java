package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.util.List;

public interface Command {
    static final int OK = 200;
    static final int BAD_REQUEST = 400;
    static final int UNAUTHORIZED = 401;
    static final int NOT_FOUND = 404;
    static final int CONFLICT = 409;
    static final int INTERNAL_SERVER_ERROR = 500;

    default Response execute(List<String> args, SpotifySystem system) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    default Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        return execute(args, system);
    }
}
