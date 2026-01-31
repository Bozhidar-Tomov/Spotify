package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.server.business.Password;
import bg.sofia.uni.fmi.mjt.spotify.server.business.PasswordHandler;
import bg.sofia.uni.fmi.mjt.spotify.server.business.UserEntity;
import bg.sofia.uni.fmi.mjt.spotify.server.net.ServerDispatcher;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.Instant;

import bg.sofia.uni.fmi.mjt.spotify.common.UserDTO;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;

public final class SpotifySystem {
    private static final short TIMEOUT = 1200;
    private static final int SAVE_INTERVAL = 90;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private static SpotifySystem INSTANCE = null;
    private final Map<String, UserEntity> usersByEmail = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private ExecutorService networkExecutor;

    private SpotifySystem() {
    }

    public static SpotifySystem getInstance() {
        synchronized (SpotifySystem.class) {
            if (INSTANCE == null) {
                INSTANCE = new SpotifySystem();
            }
        }
        return INSTANCE;
    }

    public void registerUser(String email, String password) {
        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            throw new ValidationException("Email and password cannot be null or empty.");
        }

        if (usersByEmail.containsKey(email)) {
            throw new AuthenticationException("User with email " + email + " already exists.");
        }

        try {
            Password hashed = PasswordHandler.hashPassword(password);
            usersByEmail.put(email, new UserEntity(email, hashed));
        } catch (Exception e) {
            throw new InternalSystemException("Cannot hash password", e);
        }
    }

    public UserDTO login(String email, String password) {
        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            throw new ValidationException("Email and password cannot be null or empty.");
        }

        UserEntity user = usersByEmail.get(email);
        if (user == null) {
            throw new AuthenticationException("User " + email + " not found.");
        }

        try {
            if (!PasswordHandler.verifyPassword(password, user.password())) {
                throw new AuthenticationException("Invalid credentials.");
            }
        } catch (Exception e) {
            throw new InternalSystemException("Cannot execute login.", e);
        }
        
        return user.toDTO();
    }

    public void start(int port) throws Exception {
        System.out.println("SpotifySystem: Booting system...");
        try {
            System.out.println("SpotifySystem: Loading data from files...");
            loadData();
        } catch (IOException e) {
            System.err.println("SpotifySystem: Failed to load data: " + e.getMessage());
            System.err.println("Aborting...");
            throw e;
        }

        networkExecutor = Executors.newSingleThreadExecutor();
        networkExecutor.execute(new ServerDispatcher(port, this));

        startPeriodicSave();
    }

    public void loadData() throws Exception {
        DataManager.loadUsers(usersByEmail);
        // throw new IOException("TEST EXC");
        // TODO
    }

    private void startPeriodicSave() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        System.out.println("SpotifySystem: Saving data to files...");
                        saveData();
                        System.out.println("SpotifySystem: Periodic safe at " + Instant.now());
                    } catch (IOException e) {
                        System.err.println("SpotifySystem: Failed to auto-save data: " + e.getMessage());
                    }
                },
                SAVE_INTERVAL, SAVE_INTERVAL, TIME_UNIT);

        System.out.println("SpotifySystem: Periodic save scheduled every " + SAVE_INTERVAL + " "
                + TIME_UNIT.toString().toLowerCase());
    }

    public void saveData() throws IOException {
        DataManager.saveUsers(usersByEmail);
        throw new IOException("TEST EXC");
        // TODO:
    }

    public void stop() {
        System.out.println("SpotifySystem: Stopping...");

        stopExecutor(networkExecutor);
        stopExecutor(scheduler);

        try {
            saveData();
        } catch (IOException e) {
            System.err.println("Error: Saving data fail on stop() " + e.getMessage());
        }
    }

    private void stopExecutor(ExecutorService executor) {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }
}
