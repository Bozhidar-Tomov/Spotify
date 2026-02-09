package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import java.util.List;

public class StopCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        if (client == null || system == null || !system.isRunning()) {
            return Response.err();
        }

        if (args == null || args.size() != 0) {
            return new Response(BAD_REQUEST, "Usage: stop", null);
        }

        try {
            system.stopStreamingTrack(client);
            return new Response(OK, "Stopped", null);
        } catch (ValidationException e) {
            return new Response(BAD_REQUEST, "Request: " + e.getMessage(), null);
        } catch (InternalSystemException e) {
            return Response.err();
        } catch (Exception e) {
            return Response.err();
        }
    }
}
