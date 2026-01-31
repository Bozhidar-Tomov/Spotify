package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.google.gson.Gson;

import bg.sofia.uni.fmi.mjt.spotify.common.models.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.common.models.PlaylistWrapper;
import bg.sofia.uni.fmi.mjt.spotify.server.business.UserEntity;

public final class DataManager {
    private static final Gson gson = new Gson();
    private static final Path ROOT_DATA_DIR = Path.of("src", "bg", "sofia", "uni", "fmi", "mjt", "spotify", "data");
    private static final Path USERS_FILE = ROOT_DATA_DIR.resolve("users").resolve("activeUsers.json");
    private static final Path PLAYLISTS_FILE = ROOT_DATA_DIR.resolve("playlists").resolve("playlists.json");
    // private static final Path SONGS_FILE =
    // ROOT_DATA_DIR.resolve("songs").resolve("songs.json");

    private DataManager() {
        throw new IllegalStateException("Utility class: do not instantiate.");
    }

    public static void load(Map<String, UserEntity> usersByEmail, Map<String, Playlist> playlistsByEmail)
            throws IOException {
        loadUsers(usersByEmail);
        loadPlaylists(playlistsByEmail);
    }

    public static void loadUsers(Map<String, UserEntity> usersByEmail) throws IOException {
        if (usersByEmail == null) {
            throw new IllegalArgumentException("Target map cannot be null");
        }

        if (!Files.exists(USERS_FILE)) {
            return;
        }

        try (InputStream fis = Files.newInputStream(USERS_FILE);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis)) {

            Object data;
            try {
                data = ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("Cannot construct Object", e);
            }

            if (data instanceof Map<?, ?> loadedMap) {
                for (Map.Entry<?, ?> entry : loadedMap.entrySet()) {
                    if (entry.getKey() instanceof String email && entry.getValue() instanceof UserEntity user) {
                        if (email == null ||
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

    public static void loadPlaylists(Map<String, Playlist> playlistsByEmail) throws IOException {
        if (playlistsByEmail == null)
            throw new IllegalArgumentException("Target map cannot be null");

        if (!Files.exists(PLAYLISTS_FILE)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(PLAYLISTS_FILE)) {
            PlaylistWrapper data = gson.fromJson(reader, PlaylistWrapper.class);

            if (data == null || data.playlistsByEmail() == null) {
                throw new IOException("Playlists file malformed or empty.");
            }

            playlistsByEmail.putAll(data.playlistsByEmail());
        }
    }

    // public static void loadSongs() {

    // }

    public static void save(Map<String, UserEntity> usersByEmail, Map<String, Playlist> playlistsByEmail)
            throws IOException {
        saveUsers(usersByEmail);
        savePlaylists(playlistsByEmail);
    }

    public static void saveUsers(Map<String, UserEntity> usersByEmail) throws IOException {
        if (usersByEmail == null) {
            throw new IllegalArgumentException("Target map cannot be null");
        }

        Files.createDirectories(USERS_FILE.getParent());
        if (!Files.exists(USERS_FILE)) {
            Files.createFile(USERS_FILE);
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(usersByEmail);
            byte[] allBytes = bos.toByteArray();

            Files.write(USERS_FILE, allBytes);
        }
    }

    public static void savePlaylists(Map<String, Playlist> playlistsByEmail) throws IOException {
        if (playlistsByEmail == null) {
            throw new IllegalArgumentException("Target map cannot be null");
        }

        Files.createDirectories(PLAYLISTS_FILE.getParent());
        if (!Files.exists(PLAYLISTS_FILE)) {
            Files.createFile(PLAYLISTS_FILE);
        }

        PlaylistWrapper wrapper = new PlaylistWrapper(playlistsByEmail);

        try (BufferedWriter writer = Files.newBufferedWriter(PLAYLISTS_FILE,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(wrapper, writer);

        }
    }
    // public static void saveSongs() {

    // }
}
