package bg.sofia.uni.fmi.mjt.spotify.server.business;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private static final Gson GSON = new Gson();
    private static Path rootDataDir = Path.of("./");
    private static Path usersFile = rootDataDir.resolve("activeUsers.bin");
    private static Path playlistsFile = rootDataDir.resolve("playlists.json");
    private static Path tracksFile = rootDataDir.resolve("tracks.json");

    private DataManager() {
        throw new IllegalStateException("Utility class: do not instantiate.");
    }

    private static void saveToJsonFile(Path path, DataWrapper<?> data) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path to save to cannot be null");
        }

        if (data == null || data.data() == null) {
            throw new IllegalArgumentException("Data to save cannot be null");
        }

        Files.createDirectories(path.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(data, writer);
        }
    }

    private static <T> void loadFromJsonFile(
            Path path,
            Class<? extends DataWrapper<T>> wrapperClass,
            Map<String, T> targetMap) throws IOException {

        if (path == null || wrapperClass == null || targetMap == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }

        if (!Files.exists(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            DataWrapper<T> dataRead = GSON.fromJson(reader, wrapperClass);

            if (dataRead == null || dataRead.data() == null) {
                throw new IOException("JSON structure in " + path.getFileName() + " is invalid or missing data.");
            }

            targetMap.putAll(dataRead.data());

        } catch (Exception e) {
            throw new IOException("Error processing JSON file: " + path, e);
        }
    }

    public static void save(Map<String, UserEntity> usersByEmail, Map<String, Set<Playlist>> playlistsByEmail,
            Map<String, List<Track>> tracksByTitle)
            throws IOException {
        saveUsers(usersByEmail);
        savePlaylists(playlistsByEmail);
        saveTracks(tracksByTitle);
    }

    public static void saveUsers(Map<String, UserEntity> usersByEmail) throws IOException {
        if (usersByEmail == null) {
            throw new IllegalArgumentException("Target map cannot be null");
        }

        Files.createDirectories(usersFile.getParent());
        if (!Files.exists(usersFile)) {
            Files.createFile(usersFile);
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(new UserEntityWrapper(usersByEmail));
            byte[] allBytes = bos.toByteArray();

            Files.write(usersFile, allBytes);
        }
    }

    public static void savePlaylists(Map<String, Set<Playlist>> playlistsByEmail) throws IOException {
        saveToJsonFile(playlistsFile, new PlaylistWrapper(playlistsByEmail));
    }

    public static void saveTracks(Map<String, List<Track>> tracksByTitle) throws IOException {
        saveToJsonFile(tracksFile, new TrackWrapper(tracksByTitle));
    }

    public static void load(Map<String, UserEntity> usersByEmail, Map<String, Set<Playlist>> playlistsByEmail,
            Map<String, List<Track>> tracksByTitle)
            throws IOException {
        loadUsers(usersByEmail);
        loadPlaylists(playlistsByEmail);
        loadTracks(tracksByTitle);

        playlistsByEmail.forEach((email, playlists) -> {
            UserEntity user = usersByEmail.get(email);
            if (user != null) {
                playlists.forEach(p -> {
                    if (!user.playlistNames().contains(p.name())) {
                        user.addPlaylist(p.name());
                    }
                });
            }
        });
    }

    public static void loadUsers(Map<String, UserEntity> usersByEmail) throws IOException {
        if (usersByEmail == null)
            throw new IllegalArgumentException("Target map cannot be null");
        if (!Files.exists(usersFile))
            return;

        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(usersFile)))) {
            Object data = ois.readObject();

            if (data instanceof UserEntityWrapper wrapper) {
                Map<String, UserEntity> loadedData = wrapper.data();
                validateUserData(loadedData);
                usersByEmail.putAll(loadedData);
            } else {
                String type = (data == null) ? "null" : data.getClass().getName();
                throw new IOException("Unexpected data format: Expected UserEntityWrapper but found " + type);
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Class definition not found during deserialization", e);
        }
    }

    private static void validateUserData(Map<String, UserEntity> data) throws IOException {
        for (Map.Entry<String, UserEntity> entry : data.entrySet()) {
            UserEntity user = entry.getValue();
            if (entry.getKey() == null || user == null || user.email() == null || user.password() == null) {
                throw new IOException("Data corrupted - null fields.");
            }
        }
    }

    public static void loadPlaylists(Map<String, Set<Playlist>> playlistsByEmail) throws IOException {
        loadFromJsonFile(playlistsFile, PlaylistWrapper.class, playlistsByEmail);
    }

    public static void loadTracks(Map<String, List<Track>> tracksByTitle) throws IOException {
        loadFromJsonFile(tracksFile, TrackWrapper.class, tracksByTitle);
    }

    public static void setDataDirectory(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Data directory path cannot be null");
        }
        rootDataDir = path;
        usersFile = rootDataDir.resolve("activeUsers.bin");
        playlistsFile = rootDataDir.resolve("playlists.json");
        tracksFile = rootDataDir.resolve("tracks.json");
    }
}