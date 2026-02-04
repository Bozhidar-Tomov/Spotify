package bg.sofia.uni.fmi.mjt.spotify.server;

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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class SpotifySystem {
    private static final short TIMEOUT = 3;
    private static final int SAVE_INTERVAL = 90;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static SpotifySystem instance = null;

    private final Runnable scheduledSaveTask = () -> {
        try {
            System.out.println("SpotifySystem: Saving data to files...");
            saveData();
            System.out.println("SpotifySystem: Periodic save at " + Instant.now());
        } catch (IOException e) {
            System.err.println("SpotifySystem: Failed to auto-save data: " + e.getMessage());
        }
    };

    private final Map<String, UserEntity> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, List<Playlist>> playlistsByEmail = new ConcurrentHashMap<>();
    private final Map<String, List<Track>> tracksByTitle = new ConcurrentHashMap<>();

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

    public UserDTO registerUser(String email, String password) {
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

        return newUser.toDTO();
    }

    public UserDTO login(String email, String password) {
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

        return user.toDTO();
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
        System.out.println("SpotifySystem: Server started on port " + port);

        startPeriodicSave();
    }

    private void startPeriodicSave() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(scheduledSaveTask, SAVE_INTERVAL, SAVE_INTERVAL, TIME_UNIT);

        System.out.println("SpotifySystem: Periodic save scheduled every " + SAVE_INTERVAL + " "
                + TIME_UNIT.toString().toLowerCase());
    }

    public void loadData() throws IOException {
        DataManager.load(usersByEmail, playlistsByEmail, tracksByTitle);
    }

    public void saveData() throws IOException {
        DataManager.save(usersByEmail, playlistsByEmail, tracksByTitle);
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

    public List<Track> getTracks(String trackTitle) {
        List<Track> tracks = tracksByTitle.get(trackTitle);

        if (tracks == null || tracks.isEmpty()) {
            return List.of();
        }
        return List.copyOf(tracks);
    }

    public boolean isRunning() {
        return networkExecutor != null && !networkExecutor.isShutdown() && !networkExecutor.isTerminated();
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
}