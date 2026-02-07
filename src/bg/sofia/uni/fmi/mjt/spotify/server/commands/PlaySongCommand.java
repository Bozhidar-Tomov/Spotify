package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AmbiguousSourceException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceAlreadyExistsException;

import java.util.List;

public class PlaySongCommand implements Command {

    @Override
    public Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        if (args == null || args.size() != 1) {
            return new Response(400, "Usage: play <song name>", null);
        }

        if (client == null || system == null || !system.isRunning()) {
            return Response.err();
        }

        String songTitle = args.get(0);

        try {
            system.streamTrack(songTitle, client);
            return null;
        } catch (ValidationException e) {
            return new Response(400, "Request: " + e.getMessage(), null);
        } catch (AuthenticationException e) {
            return new Response(401, "Auth: " + e.getMessage(), null);
        } catch (SourceNotFoundException e) {
            return new Response(404, "Missing: " + e.getMessage(), null);
        } catch (SourceAlreadyExistsException | AmbiguousSourceException e) {
            return new Response(409, "Conflict: " + e.getMessage(), null);
        } catch (InternalSystemException e) {
            return Response.err();
        } catch (Exception e) {
            return Response.err();
        }
    }
}
