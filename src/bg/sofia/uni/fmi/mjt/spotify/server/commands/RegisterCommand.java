package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.UserDtoPayload;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.util.List;

public class RegisterCommand implements Command {
    @Override
    public Response execute(List<String> args, SpotifySystem system) {
        if (args == null || args.size() != 2) {
            return new Response(400, "Usage: register <email> <password>", null);
        }
        
        if (system == null || !system.isRunning()) {
            return Response.err();
        }

        String email = args.get(0);
        String password = args.get(1);

        try {
            UserDTO user = system.registerUser(email, password);
            return new Response(200, "Successful register.", new UserDtoPayload(user));
        } catch (ValidationException | AuthenticationException e) {
            return new Response(401, "Register failed: " + e.getMessage(), null);
        } catch (InternalSystemException e) {
            return Response.err();
        }
    }
}
