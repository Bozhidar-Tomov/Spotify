package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AmbiguousSourceException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.common.net.UserDtoPayload;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.util.List;

public class CreatePlaylistCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        if (args == null || args.size() != 1) {
            return new Response(BAD_REQUEST, "Usage: create-playlist <playlist_name>", null);
        }

        if (system == null || !system.isRunning()) {
            return Response.err();
        }

        String playlistName = args.get(0);

        try {
            UserDTO user = system.createPlaylist(playlistName, client);
            return new Response(OK, "Playlist created.", new UserDtoPayload(user));
        } catch (ValidationException e) {
            return new Response(BAD_REQUEST, "Request: " + e.getMessage(), null);
        } catch (AuthenticationException e) {
            return new Response(UNAUTHORIZED, "Auth: " + e.getMessage(), null);
        } catch (SourceNotFoundException e) {
            return new Response(NOT_FOUND, "Missing: " + e.getMessage(), null);
        } catch (SourceAlreadyExistsException | AmbiguousSourceException e) {
            return new Response(CONFLICT, "Conflict: " + e.getMessage(), null);
        } catch (Exception e) {
            return Response.err();
        }
    }
}
