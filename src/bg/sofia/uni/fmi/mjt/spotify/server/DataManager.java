package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

// import com.google.gson.Gson;

import bg.sofia.uni.fmi.mjt.spotify.server.business.UserEntity;

public final class DataManager {
    // private static final Gson gson = new Gson();
    private static final Path ROOT_DATA_DIR = Path.of("src", "bg", "sofia", "uni", "fmi", "mjt", "spotify", "data");
    private static final Path USERS_FILE = ROOT_DATA_DIR.resolve("users").resolve("activeUsers.json");
    // private static final Path PLAYLISTS_FILE = ROOT_DATA_DIR.resolve("playlists").resolve("playlists.json");
    // private static final Path SONGS_FILE = ROOT_DATA_DIR.resolve("songs").resolve("songs.json");

    private DataManager() {
        throw new IllegalStateException("Utility class: do not instantiate.");
    }
    
    public static void loadUsers(Map<String, UserEntity> usersByEmail) throws IOException, ClassNotFoundException {
        if (usersByEmail == null) throw new IllegalArgumentException("Target map cannot be null");
    
        if (!Files.exists(USERS_FILE)) {
            return; 
        }

        try (InputStream fis = Files.newInputStream(USERS_FILE);
            BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis)) {
            
            //BUG: not secure
            Object data = ois.readObject();
            
            if (data instanceof Map<?, ?> loadedMap) {
                for (Map.Entry<?, ?> entry : loadedMap.entrySet()) {
                    if (entry.getKey() instanceof String email && entry.getValue() instanceof UserEntity user) {
                        if (
                            email == null ||
                            user.email() == null ||
                            user.password() == null) {
                            throw new IOException("Data corrupted - null fields.");
                        }
                        usersByEmail.put(email, user);
                    }
                }
            }
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

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(usersByEmail);
            byte[] allBytes = bos.toByteArray();

            Files.write(USERS_FILE, allBytes);
        }
    }
    
    // public static void savePlaylists() {

    // }

    // public static void saveSongs() {

    // }
}
