package bg.sofia.uni.fmi.mjt.spotify.server.business;



import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import bg.sofia.uni.fmi.mjt.spotify.common.models.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.common.models.SongMetadata;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.server.models.Password;
import bg.sofia.uni.fmi.mjt.spotify.server.models.UserEntity;


public class DataManagerTest {

    @TempDir
    Path tempDir;

    private UserEntity testUser;
    private Playlist testPlaylist;
    private Track testTrack;

    @BeforeEach
    void setUp() {
        DataManager.setDataDirectory(tempDir);

        testUser = new UserEntity("test@fmi.com", new Password(new byte[] { 1 }, new byte[] { 2 }, 100));
        testPlaylist = new Playlist("P1", "test@fmi.com");

        SongMetadata metadata = new SongMetadata("id1", "Title1", "Artist1", "path",
                new AudioFormatPayload("PCM", 44100, 16, 2, 4, 44100, false));
        testTrack = new Track(metadata);
    }

    @Test
    void testPersistenceCycle() throws IOException {
        testUser.addPlaylist(testPlaylist.name());

        DataManager.save(
            Map.of(testUser.email(), testUser),
            Map.of(testUser.email(), Set.of(testPlaylist)),
            Map.of(testTrack.metadata().title(), List.of(testTrack)));

        Map<String, UserEntity> users = new HashMap<>();
        Map<String, Set<Playlist>> playlists = new HashMap<>();
        Map<String, List<Track>> tracks = new HashMap<>();
        DataManager.load(users, playlists, tracks);

        assertAll("Verify data persistence cycle",
                () -> assertEquals(1, users.size(), "Should load one user"),
                () -> assertEquals(testUser, users.get(testUser.email()), "User data mismatch"),

                () -> {
                    Set<Playlist> userPlaylists = playlists.get(testUser.email());
                    assertNotNull(userPlaylists, "Playlist set for user should not be null");
                    assertTrue(userPlaylists.contains(testPlaylist), "Playlist not found in user's playlists");
                },

                () -> assertEquals(testTrack, tracks.get("Title1").get(0), "Track data mismatch"),
                () -> assertTrue(users.get(testUser.email()).playlistNames().contains("P1"),
                        "User-Playlist sync failed"),
                () -> assertEquals(1, playlists.get(testUser.email()).size(), "Playlist count mismatch"));
    }

    @Test
    void testLoadEmptyState() throws IOException {
        Map<String, UserEntity> users = new HashMap<>();
        Map<String, Set<Playlist>> playlists = new HashMap<>();
        Map<String, List<Track>> tracks = new HashMap<>();
        
        DataManager.load(users, playlists, tracks);

        assertTrue(users.isEmpty() && playlists.isEmpty() && tracks.isEmpty(),
                "DataManager should handle missing files by returning empty maps");
    }

    @Test
    void testValidationLogic() {
        assertAll("Null checks for DataManager methods",
                () -> assertThrows(IllegalArgumentException.class, () -> DataManager.setDataDirectory(null),
                        "Should reject null directory"),
                () -> assertThrows(IllegalArgumentException.class, () -> DataManager.saveUsers(null),
                        "Should reject null user map"),
                () -> assertThrows(IllegalArgumentException.class, () -> DataManager.savePlaylists(null),
                        "Should reject null playlist map"),
                () -> assertThrows(IllegalArgumentException.class, () -> DataManager.saveTracks(null),
                        "Should reject null track map"),
                () -> assertThrows(IllegalArgumentException.class, () -> DataManager.loadUsers(null),
                        "Should reject null user map"),
                () -> assertThrows(IllegalArgumentException.class, () -> DataManager.loadPlaylists(null),
                        "Should reject null playlist map"),
                () -> assertThrows(IllegalArgumentException.class, () -> DataManager.loadTracks(null),
                        "Should reject null track map"));
    }

    @Test
    void testSetDataDirectoryUpdatesPaths() throws IOException {
        Path newDir = tempDir.resolve("newDir");
        Files.createDirectories(newDir);
        DataManager.setDataDirectory(newDir);

        DataManager.saveUsers(Map.of(testUser.email(), testUser));
        assertTrue(Files.exists(newDir.resolve("activeUsers.bin")), "Data should be saved in the new directory");
    }

    @Test
    void testLoadSynchronizationRestoresMissingPlaylistLinks() throws IOException {
        UserEntity userWithNoPlaylists = new UserEntity(testUser.email(), testUser.password());

        DataManager.saveUsers(Map.of(userWithNoPlaylists.email(), userWithNoPlaylists));
        DataManager.savePlaylists(Map.of(userWithNoPlaylists.email(), Set.of(testPlaylist)));

        Map<String, UserEntity> users = new HashMap<>();
        Map<String, Set<Playlist>> playlists = new HashMap<>();
        Map<String, List<Track>> tracks = new HashMap<>();

        DataManager.load(users, playlists, tracks);

        UserEntity loadedUser = users.get(testUser.email());

        assertAll("Synchronization Logic",
                () -> assertNotNull(loadedUser, "User should exist"),
                () -> assertTrue(loadedUser.playlistNames().contains(testPlaylist.name()),
                        "The load method should have injected the playlist name into the UserEntity"),
                () -> assertEquals(1, loadedUser.playlistNames().size(),
                        "User should have exactly one playlist after sync"));
    }

    @Test
    void testLoadPlaylistForNonExistentUserDoesNotCrash() throws IOException {
        String orphanEmail = "does_not_exist@fmi.com";
        DataManager.savePlaylists(Map.of(orphanEmail, Set.of(testPlaylist)));
        DataManager.saveUsers(Collections.emptyMap());

        Map<String, UserEntity> users = new HashMap<>();
        Map<String, Set<Playlist>> playlists = new HashMap<>();
        Map<String, List<Track>> tracks = new HashMap<>();

        assertDoesNotThrow(() -> DataManager.load(users, playlists, tracks),
                "Loading should be resilient to playlists with no active users");

        assertAll("Child with no parent Data Handling",
                () -> assertTrue(users.isEmpty(), "Users map should remain empty"),
                () -> assertTrue(playlists.containsKey(orphanEmail), "Playlist should still be in the map"),
                () -> assertEquals(testPlaylist, playlists.get(orphanEmail).iterator().next(),
                        "Playlist data integrity maintained"));
    }

    @Test
    void testCorruptedFiles() throws IOException {
        Files.writeString(tempDir.resolve("activeUsers.bin"), "corrupted_binary_data");
        Files.writeString(tempDir.resolve("playlists.json"), "{ invalid: )json( }");
        Files.writeString(tempDir.resolve("tracks.json"), "");

        assertAll("File corruption error handling",
                () -> assertThrows(IOException.class, () -> DataManager.loadUsers(new HashMap<>()),
                        "Binary corruption should throw IOException"),
                () -> assertThrows(IOException.class, () -> DataManager.loadPlaylists(new HashMap<>()),
                        "Malformed JSON should throw IOException"),
                () -> assertThrows(IOException.class, () -> DataManager.loadTracks(new HashMap<>()),
                        "Empty JSON file should throw IOException"));
    }
}