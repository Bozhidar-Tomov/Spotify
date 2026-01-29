package bg.sofia.uni.fmi.mjt.spotify.server;

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

public class SpotifySystem {
    private static final short TIMEOUT = 1200;
    private static final int SAVE_INTERVAL = 5;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    
    private static SpotifySystem INSTANCE = null;
    private final Map<String, UserEntity> usersByEmail = new ConcurrentHashMap<>();
    
    private ScheduledExecutorService scheduler;
    private ExecutorService networkExecutor;

    private SpotifySystem() {      
    }

    public static synchronized SpotifySystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SpotifySystem();
        }

        return INSTANCE;
    }


    public void start(int port) throws IOException {
        System.out.println("SpotifySystem: Booting server...");
        try {
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

    public void loadData() throws IOException {
        System.out.println("SpotifySystem: Loading data from files...");
        DataManager.loadUsers(usersByEmail);
        throw new IOException();
        // TODO
    }

    private void startPeriodicSave() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        saveData();
                        System.out.println("SpotifySystem: Periodic safe at " + Instant.now());
                    } catch (Exception e) {
                        System.err.println("SpotifySystem: Failed to auto-save data: " + e.getMessage());
                    }
                },
                SAVE_INTERVAL, SAVE_INTERVAL, TIME_UNIT);

        System.out.println("SpotifySystem: Periodic save scheduled every " + SAVE_INTERVAL + " "
                + TIME_UNIT.toString().toLowerCase());
    }

    public void saveData() throws IOException {
        System.out.println("SpotifySystem: Saving data to files...");
        DataManager.saveUsers(usersByEmail);
        throw new IOException();
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
