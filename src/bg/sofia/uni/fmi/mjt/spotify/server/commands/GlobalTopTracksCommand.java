package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.util.List;

public class GlobalTopTracksCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system) {
        return null;
    }
}
