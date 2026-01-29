package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.google.gson.Gson;

import bg.sofia.uni.fmi.mjt.spotify.server.business.UserEntity;
import bg.sofia.uni.fmi.mjt.spotify.server.business.UserEntityWrapper;

public final class DataManager {
    private static final Gson gson = new Gson();
    private static final Path ROOT_DATA_DIR = Path.of("src", "bg", "sofia", "uni", "fmi", "mjt", "spotify", "data");
    private static final Path USERS_FILE = ROOT_DATA_DIR.resolve("users").resolve("activeUsers.json");
    // private static final Path PLAYLISTS_FILE = ROOT_DATA_DIR.resolve("playlists").resolve("playlists.json");
    // private static final Path SONGS_FILE = ROOT_DATA_DIR.resolve("songs").resolve("songs.json");

    private DataManager() {
        throw new IllegalStateException("Utility class: do not instantiate.");
    }

    public static void loadUsers(Map<String, UserEntity> usersByEmail) throws IOException {
        if (usersByEmail == null) {
            throw new IllegalArgumentException("Target map cannot be null");
        }

        if (!Files.exists(USERS_FILE)) {
            Files.createFile(USERS_FILE);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(USERS_FILE)) {
            UserEntityWrapper data = gson.fromJson(reader, UserEntityWrapper.class);

            if (data == null || data.usersByEmail() == null) {
                throw new IOException("User data file malformed or empty.");
            }

            usersByEmail.putAll(data.usersByEmail());   
        }
    }
    
    // public static void loadPlaylists() {
        
    // }
    // public static void loadSongs() {
        
    // }

    public static void saveUsers(Map<String, UserEntity> usersByEmail) throws IOException {
        if (usersByEmail == null) {
            throw new IllegalArgumentException("Target map cannot be null");
        }
        
        Files.createDirectories(USERS_FILE.getParent());

        UserEntityWrapper wrapper = new UserEntityWrapper(usersByEmail);

        try (BufferedWriter writer = Files.newBufferedWriter(USERS_FILE,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(wrapper, writer);
        }
    }
    
    // public static void savePlaylists() {

    // }

    // public static void saveSongs() {

    // }
}
