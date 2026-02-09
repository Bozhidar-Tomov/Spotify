package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.CollectionPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.util.List;

public class SearchCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system) {
        if (args == null || args.size() < 1) {
            return new Response(BAD_REQUEST, "Usage: search <word_1> ... <word_N>", null);
        }

        if (system == null || !system.isRunning()) {
            return Response.err();
        }

        List<Track> result = system.search(args);

        if (result == null || result.isEmpty()) {
            return new Response(OK, "No tracks found.", null);
        }

        try {
            return new Response(OK, "Found " + result.size() + " tracks.", new CollectionPayload<>(result));
        } catch (SpotifyException e) {
            return Response.err();
        }
    }
}
