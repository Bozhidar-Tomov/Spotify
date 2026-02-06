package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AmbiguousSourceException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.common.models.SongMetadata;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;
import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.server.models.Password;
import bg.sofia.uni.fmi.mjt.spotify.server.business.DataManager;
import bg.sofia.uni.fmi.mjt.spotify.server.business.PasswordHandler;
import bg.sofia.uni.fmi.mjt.spotify.server.models.UserEntity;
import bg.sofia.uni.fmi.mjt.spotify.server.net.ServerDispatcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.streaming.AudioStreamer;
import java.util.Collections;
import java.util.Comparator;

public final class SpotifySystem {
    private static final short TIMEOUT = 2;
    private static final int SAVE_INTERVAL = 90;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static volatile SpotifySystem instance = null;

    private final Runnable scheduledSaveTask = () -> {
        try {
            System.out.println("SpotifySystem: Saving data to files...");
            saveData();
            System.out.println("SpotifySystem: Periodic save at " + Instant.now());
        } catch (IOException e) {
            System.err.println("SpotifySystem: Failed to auto-save data: " + e.getMessage());
        }
    };

    // TODO: make initial lists hashmaps?
    private final Map<String, UserEntity> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, List<Playlist>> playlistsByEmail = new ConcurrentHashMap<>();
    private final Map<String, List<Track>> tracksByTitle = new ConcurrentHashMap<>();
    private final Map<String, Track> tracksById = new ConcurrentHashMap<>();
    
    private final Map<ResponseSender, AudioStreamer> activeStreams = new ConcurrentHashMap<>();
    private final Map<ResponseSender, String> userSessions = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private ExecutorService networkExecutor;

    private SpotifySystem() {
    }

    public static SpotifySystem getInstance() {
        if (instance == null) {
            synchronized (SpotifySystem.class) {
                if (instance == null) {
                    instance = new SpotifySystem();
                }
            }
        }
        return instance;
    }

    public void loadData() throws IOException {
        DataManager.load(usersByEmail, playlistsByEmail, tracksByTitle);
    }

    public void saveData() throws IOException {
        DataManager.save(usersByEmail, playlistsByEmail, tracksByTitle);
    }

    public void start(int port) throws IOException {
        System.out.println("SpotifySystem: Booting system...");

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }

        try {
            loadData();
            System.out.println("SpotifySystem: Data loaded successfully.");
        } catch (IOException e) {
            System.err.println("SpotifySystem: Failure during data load: " + e.getMessage());
            throw e;
        }

        ServerDispatcher dispatcher = new ServerDispatcher(port, this);

        networkExecutor = Executors.newSingleThreadExecutor();
        networkExecutor.execute(dispatcher);

        startPeriodicSave();
    }

    private void startPeriodicSave() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(scheduledSaveTask, SAVE_INTERVAL, SAVE_INTERVAL, TIME_UNIT);

        System.out.println("SpotifySystem: Periodic save scheduled every " + SAVE_INTERVAL + " "
                + TIME_UNIT.toString().toLowerCase());
    }

    public void stop() {
        System.out.println("SpotifySystem: Stopping...");

        stopExecutor(networkExecutor);
        stopExecutor(scheduler);

        try {
            saveData();
            System.out.println("SpotifySystem: Data saved successfully.");
        } catch (IOException e) {
            System.err.println("Error: Saving data on shutdown failed: " + e.getMessage());
        }
        System.out.println("SpotifySystem: Shutdown complete.");
    }

    private static void stopExecutor(ExecutorService executor) {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(TIMEOUT, TIME_UNIT)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return networkExecutor != null && !networkExecutor.isShutdown() && !networkExecutor.isTerminated();
    }

    private Track getTrack(String title) {
        if (title == null) {
            throw new ValidationException("Client or title cannot be null");
        }

        List<Track> tracks = tracksByTitle.getOrDefault(title, Collections.emptyList());

        if (tracks.isEmpty()) {
            throw new SourceNotFoundException("Song '" + title + "' not found.");
        }

        // TODO: add specification by artist or ID
        if (tracks.size() != 1) {
            throw new AmbiguousSourceException("Multiple songs with title '" + title + "' found");
        }

        return tracks.getFirst();
    }

    private String getUserEmail(ResponseSender client) {
        return userSessions.get(client);
    }

    // TODO: move the logic below in separate classes, away from SpotifySystem to
    // avoid God Object

    public void registerUser(String email, String password) {
        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            throw new ValidationException("Email and password cannot be null or empty.");
        }

        if (usersByEmail.containsKey(email)) {
            throw new AuthenticationException("User with email '" + email + "'' already exists.");
        }

        Password hashed;
        try {
            hashed = PasswordHandler.hashPassword(password);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalSystemException("Hashing algorithm configuration error.", e);
        }

        UserEntity newUser = new UserEntity(email, hashed);
        usersByEmail.put(email, newUser);
    }

    public UserDTO login(String email, String password, ResponseSender client) {
        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            throw new ValidationException("Email and password cannot be null or empty.");
        }

        UserEntity user = usersByEmail.get(email);

        try {
            if (user == null || !PasswordHandler.verifyPassword(password, user.password())) {
                throw new AuthenticationException("Invalid email or password.");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new InternalSystemException("Hashing algorithm configuration error.", e);
        }

        userSessions.put(client, email);
        return user.toDTO();
    }

    public void createPlaylist(String playlistName, ResponseSender client) {
        if (client == null || playlistName == null || playlistName.isBlank()) {
            throw new ValidationException("User and playlist name cannot be empty.");
        }

        String userEmail = getUserEmail(client);

        if (userEmail == null) {
            throw new AuthenticationException("You must be logged in to create a playlist.");
        }

        if (!usersByEmail.containsKey(userEmail)) {
            throw new SourceNotFoundException("User not found.");
        }

        playlistsByEmail.compute(userEmail, (email, playlists) -> {
            if (playlists == null) {
                playlists = new ArrayList<>();
                playlists.add(new Playlist(playlistName, email));
                return playlists;
            }

            boolean exists = playlists.stream()
                    .anyMatch(p -> p.name().equals(playlistName));

            if (exists) {
                throw new SourceAlreadyExistsException("Playlist '" + playlistName + "' already exists.");
            }

            playlists.add(new Playlist(playlistName, email));
            return playlists;
        });
    }
    
    public void addSongToPlaylist(String playlistName, String songTitle, ResponseSender client) {
        if (client == null || playlistName == null || songTitle == null || playlistName.isBlank()
                || songTitle.isBlank()) {
            throw new ValidationException("Playlist name and song title cannot be null");
        }

        String userEmail = getUserEmail(client);
        if (userEmail == null) {
            throw new AuthenticationException("You must be logged in to create a playlist.");
        }

        if (!playlistsByEmail.containsKey(userEmail)) {
            throw new SourceNotFoundException("No playlists found for user " + userEmail);
        }

        Track track = getTrack(songTitle);

        playlistsByEmail.computeIfPresent(userEmail, (email, userPlaylists) -> {
            Playlist playlist = userPlaylists.stream()
                    .filter(p -> p.name().equals(playlistName))
                    .findFirst()
                    .orElseThrow(() -> new SourceNotFoundException("Playlist '" + playlistName + "' not found."));

            playlist.addTrack(track.metadata().id());

            return userPlaylists;
        });
    }

    public void addNewTrack(String id, String title, String artist, Path filePath)
            throws IOException, UnsupportedAudioFileException {

        if (id == null || title == null || artist == null || filePath == null) {
            throw new ValidationException("Track metadata and file path cannot be null.");
        }

        if (!Files.exists(filePath)) {
            throw new SourceNotFoundException("Audio file not found at: " + filePath.toAbsolutePath());
        }

        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(filePath.toFile())) {
            addNewTrack(new SongMetadata(id, title, artist, filePath.toString(),
                    AudioFormatPayload.from(audioStream.getFormat())));
        }
    }

    public void addNewTrack(SongMetadata track) {
        if (track == null) {
            throw new ValidationException("Track metadata cannot be null.");
        }

        boolean alreadyExists = tracksByTitle.values().stream()
                .flatMap(List::stream)
                .anyMatch(t -> t.metadata().id().equals(track.id()));

        if (alreadyExists) {
            throw new SourceAlreadyExistsException("Track with ID " + track.id() + " already exists.");
        }

        tracksByTitle.computeIfAbsent(track.title(), key -> new ArrayList<>()).add(new Track(track));
    }

    public Response streamTrack(String title, ResponseSender client) {
        if (client == null || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Client cannot be null");
        }

        List<Track> tracks = tracksByTitle.getOrDefault(title, Collections.emptyList());

        if (tracks.isEmpty()) {
            return new Response(404, "Song '" + title + "' not found.", null);
        }

        // TODO: add specification by artist or ID
        if (tracks.size() != 1) {
            return new Response(400, "Multiple songs with title '" + title + "' found", null);
        }

        Track track = tracks.getFirst();
        AudioStreamer streamer = new AudioStreamer(client);

        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(track.metadata().filePath()))) {
            streamer.startStream(audioStream.getFormat(), "Playing " + track.metadata().title());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = audioStream.read(buffer)) != -1) {
                byte[] chunk = (bytesRead == buffer.length) ? buffer : Arrays.copyOf(buffer, bytesRead);
                streamer.sendChunk(chunk);
            }

            streamer.endStream();
            return new Response(200, "Playback finished", null);
        } catch (Exception e) {
            System.err.println("Error during playback: " + e.getMessage());
            return Response.err();
        }
    }

    public Response stopStreamingTrack(ResponseSender client) throws IOException {
        AudioStreamer streamer = new AudioStreamer(client);
        streamer.endStream();
        return new Response(200, "Playback finished", null);
    }
}