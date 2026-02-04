package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CommandFactory {
    private static final Map<String, Supplier<Command>> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put("register", RegisterCommand::new);
        REGISTRY.put("login", LoginCommand::new);
        REGISTRY.put("search", SearchCommand::new);
        REGISTRY.put("top", TopCommand::new);
        REGISTRY.put("create-playlist", CreatePlaylistCommand::new);
        REGISTRY.put("add-song-to", AddSongCommand::new);
        REGISTRY.put("show-playlist", ShowPlaylistCommand::new);
        REGISTRY.put("play", PlaySongCommand::new);
        REGISTRY.put("stop", StopCommand::new);
    }

    public static Command createCommand(String commandType) {
        Supplier<Command> commandSupplier = REGISTRY.get(commandType);
        if (commandSupplier == null) {
            return null;
        }
        return commandSupplier.get();
    }
}
