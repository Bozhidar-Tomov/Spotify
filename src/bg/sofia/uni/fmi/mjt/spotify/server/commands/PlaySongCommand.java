package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;

import java.util.List;

public class PlaySongCommand implements Command {

    @Override
    public Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        if (client == null || system == null || !system.isRunning()) {
            return Response.err();
        }

        if (args == null || args.size() != 1) {
            return new Response(400, "Usage: play <song name>", null);
        }

        return system.streamTrack(args.get(0), client);
    }
}
