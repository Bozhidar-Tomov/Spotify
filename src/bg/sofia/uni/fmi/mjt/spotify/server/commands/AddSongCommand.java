package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AmbiguousSourceException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.util.List;

public class AddSongCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        if (args.size() != 2) {
            return new Response(400, "Usage: add-song-to <playlist_name> <song_name>", null);
        }

        if (system == null || !system.isRunning()) {
            return Response.err();
        }

        String playlistName = args.get(0);
        String songTitle = args.get(1);

        try {
            system.addSongToPlaylist(playlistName, songTitle, client);
            return new Response(200, "Song added to playlist.", null);
        } catch(ValidationException e){
            return new Response(400, "Request: " + e.getMessage(), null);
        } catch(AuthenticationException e){
            return new Response(401, "Auth: " + e.getMessage(), null);
        } catch(SourceNotFoundException e){
            return new Response(404, "Missing: " + e.getMessage(), null);
        } catch(SourceAlreadyExistsException | AmbiguousSourceException e){
            return new Response(409, "Conflict: " + e.getMessage(), null);
        } catch (Exception e) {
            return Response.err();
        }
    }
}
