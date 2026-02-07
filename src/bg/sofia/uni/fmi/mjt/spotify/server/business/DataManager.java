package bg.sofia.uni.fmi.mjt.spotify.server.business;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import bg.sofia.uni.fmi.mjt.spotify.common.models.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.server.models.DataWrapper;
import bg.sofia.uni.fmi.mjt.spotify.server.models.PlaylistWrapper;
import bg.sofia.uni.fmi.mjt.spotify.server.models.TrackWrapper;
import bg.sofia.uni.fmi.mjt.spotify.server.models.UserEntity;
import bg.sofia.uni.fmi.mjt.spotify.server.models.UserEntityWrapper;

public final class DataManager {
    private static final Gson gson = new Gson();
    private static final Path ROOT_DATA_DIR = Path.of("./");
    private static final Path USERS_FILE = ROOT_DATA_DIR.resolve("activeUsers.bin");
    private static final Path PLAYLISTS_FILE = ROOT_DATA_DIR.resolve("playlists.json");
    private static final Path TRACKS_FILE = ROOT_DATA_DIR.resolve("tracks.json");

    private DataManager() {
        throw new IllegalStateException("Utility class: do not instantiate.");
    }

    private static void saveToJsonFile(Path path, DataWrapper<?> data) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path to save to cannot be null");
        }

        if (data == null) {
            throw new IllegalArgumentException("Data to save cannot be null");
        }

        Files.createDirectories(path.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(data, writer);
        }
    }

    private static <T, W extends DataWrapper<T>> void loadFromJsonFile(Path path, Class<W> wrapperClass,
            Map<String, T> targetMap) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            W dataRead = gson.fromJson(reader, wrapperClass);

            if (dataRead == null || dataRead.data() == null) {
                throw new IOException(path.getFileName() + " file malformed or empty.");
            }

            targetMap.putAll(dataRead.data());
        }
    }

    public static void save(Map<String, UserEntity> usersByEmail, Map<String, Set<Playlist>> playlistsByEmail,
            Map<String, List<Track>> tracksByTitle)
            throws IOException {
        saveUsers(usersByEmail);
        savePlaylists(playlistsByEmail);
        saveTracks(tracksByTitle);
    }

    public static void savePlaylists(Map<String, Set<Playlist>> playlistsByEmail) throws IOException {
        saveToJsonFile(PLAYLISTS_FILE, new PlaylistWrapper(playlistsByEmail));
    }

    public static void saveTracks(Map<String, List<Track>> tracksByTitle) throws IOException {
        saveToJsonFile(TRACKS_FILE, new TrackWrapper(tracksByTitle));
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

            oos.writeObject(new UserEntityWrapper(usersByEmail));
            byte[] allBytes = bos.toByteArray();

            Files.write(USERS_FILE, allBytes);
        }
    }

    public static void load(Map<String, UserEntity> usersByEmail, Map<String, Set<Playlist>> playlistsByEmail,
            Map<String, List<Track>> tracksByTitle)
            throws IOException {
        loadUsers(usersByEmail);
        loadPlaylists(playlistsByEmail);
        loadTracks(tracksByTitle);
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

            if (data instanceof UserEntityWrapper wrapper) {
                usersByEmail.putAll(wrapper.data());
            } else if (data instanceof Map<?, ?> loadedMap) {
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

    public static void loadPlaylists(Map<String, Set<Playlist>> playlistsByEmail) throws IOException {
        loadFromJsonFile(PLAYLISTS_FILE, PlaylistWrapper.class, playlistsByEmail);
    }

    public static void loadTracks(Map<String, List<Track>> tracksByTitle) throws IOException {
        loadFromJsonFile(TRACKS_FILE, TrackWrapper.class, tracksByTitle);
    }
}