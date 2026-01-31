package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.nio.channels.SocketChannel;
import java.util.List;

public class LoginCommand implements Command {
    @Override
    public String execute(List<String> args, SocketChannel clientChannel, SpotifySystem system) {
        if (args.size() != 2) {
            return "Usage: login <email> <password>";
        }

        String email = args.get(0);
        String password = args.get(1);

        system.login(email, password);
        return "Login successful for " + email + ".";
    }

}
