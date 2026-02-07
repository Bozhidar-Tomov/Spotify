package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import java.util.List;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.CollectionPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;

public class GlobalTopTracksCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system) {
        if (args.size() != 1) {
            return new Response(400, "Usage: top-global <number>", null);
        }

        if (system == null || !system.isRunning()) {
            return Response.err();
        }

        long n;

        try {
            n = Long.parseLong(args.getFirst());
            if (n <= 0) {
                return new Response(400, "Must be positive integer.", null);
            }
        } catch (NumberFormatException e) {
            return new Response(400,
                    String.format("Invalid number format: '%s'", args.getFirst()),
                    null);
        }

        try {
            List<Track> topTracks = system.topGlobalPlayingTracks(n);

            if (topTracks == null || topTracks.isEmpty()) {
                return new Response(200, "No tracks are globally played.", null);
            }

            return new Response(200, "OK", new CollectionPayload<>(topTracks));
        } catch (InternalSystemException e) {
            return Response.err();
        }
    }
}
