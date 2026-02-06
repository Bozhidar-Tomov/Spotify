package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import java.util.List;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;

public class CurrentTopTracksCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system) {
        return null;
    }
}
