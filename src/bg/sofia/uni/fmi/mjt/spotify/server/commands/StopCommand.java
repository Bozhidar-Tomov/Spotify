package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.util.List;

public class StopCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        if (client == null || system == null || !system.isRunning()) {
            return Response.err();
        }

        if (args == null || args.size() != 0) {
            return new Response(400, "Usage: stop", null);
        }

        try {
            return system.stopStreamingTrack(client);
        } catch (Exception e) {
            return Response.err();
        }
    }
}
