package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import java.util.List;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.CollectionPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;

public class ShowPlaylistCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        if (client == null || system == null || !system.isRunning()) {
            return Response.err();
        }

        if (args == null || args.size() != 1) {
            return new Response(BAD_REQUEST, "Usage: show-playlist <name_of_the_playlist>", null);
        }

        String playlistName = args.getFirst();

        try {
            List<Track> tracks = system.getPlaylistTracks(playlistName, client);

            if (tracks == null || tracks.isEmpty()) {
                return new Response(OK, "Playlist '" + playlistName + "' is empty.", null);
            }

            return new Response(OK, "OK", new CollectionPayload<>(tracks));
        } catch (ValidationException e) {
            return new Response(BAD_REQUEST, "Request: " + e.getMessage(), null);
        } catch (InternalSystemException e) {
            return Response.err();
        } catch (SpotifyException e) {
            return Response.err();
        }
    }
}