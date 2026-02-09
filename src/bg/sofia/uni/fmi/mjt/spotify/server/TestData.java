package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.security.NoSuchAlgorithmException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import bg.sofia.uni.fmi.mjt.spotify.common.models.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.common.models.SongMetadata;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.server.business.DataManager;
import bg.sofia.uni.fmi.mjt.spotify.server.business.PasswordHandler;
import bg.sofia.uni.fmi.mjt.spotify.server.models.Password;
import bg.sofia.uni.fmi.mjt.spotify.server.models.UserEntity;

public class TestData {
    private static void populateData() {
        try {
            Map<String, UserEntity> users = new ConcurrentHashMap<>();
            addUser(users, "a.com", "a");
            addUser(users, "b.com", "b");
            DataManager.saveUsers(users);

            Map<String, List<Track>> tracks = new ConcurrentHashMap<>();
            addTrack(tracks, "1", "Beyoncé - 16 CARRIAGES.wav", "Beyoncé");
            addTrack(tracks, "2", "Beyoncé - HEATED.wav", "Beyoncé");
            addTrack(tracks, "3", "Beyoncé - SPIRIT.wav", "Beyoncé");
            addTrack(tracks, "4", "VOILA - Figure You Out.wav", "VOILÀ");
            DataManager.saveTracks(tracks);

            Map<String, Set<Playlist>> playlists = new ConcurrentHashMap<>();
            addPlaylist(playlists, "a.com", "act i", "1");
            addPlaylist(playlists, "a.com", "act ii", "1", "2");
            addPlaylist(playlists, "a.com", "Cowboy Carter", "1", "2", "3");
            addPlaylist(playlists, "b.com", "Pop Vibes", "4");
            addPlaylist(playlists, "b.com", "My Favorites", "2", "4");
            DataManager.savePlaylists(playlists);

            System.out.println("Test data population successful.");
        } catch (Exception e) {
            System.err.println("ERROR: populating predefined data failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addUser(Map<String, UserEntity> users, String email, String password)
            throws NoSuchAlgorithmException {
        Password hashed = PasswordHandler.hashPassword(password);
        users.put(email, new UserEntity(email, hashed));
    }

    private static void addTrack(Map<String, List<Track>> tracks, String id, String fileName, String artist) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.err.println("Warning: Audio file not found: " + fileName);
            return;
        }
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(file)) {
            String title = fileName.substring(fileName.indexOf("-") + 1, fileName.lastIndexOf(".")).strip();
            SongMetadata metadata = new SongMetadata(
                    id,
                    title.toLowerCase(),
                    artist,
                    file.getPath(),
                    AudioFormatPayload.from(audioStream.getFormat()));

            tracks.computeIfAbsent(metadata.title(), key -> new ArrayList<>())
                    .add(new Track(metadata));
        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("Error loading track " + fileName + ": " + e.getMessage());
        }
    }

    private static void addPlaylist(Map<String, Set<Playlist>> playlists, String owner, String name,
            String... trackIds) {
        Playlist playlist = new Playlist(name, owner);
        for (String id : trackIds) {
            playlist.addTrack(id);
        }
        playlists.computeIfAbsent(owner, k -> new HashSet<>()).add(playlist);
    }

    public static void main(String[] args) {
        populateData();
    }
}
