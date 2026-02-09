package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.SongMetadata;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpotifySystemTest {

    private SpotifySystem spotifySystem;

    @Mock
    private ResponseSender responseSenderMock;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);

        Field instanceField = SpotifySystem.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        spotifySystem = SpotifySystem.getInstance();
    }

    @Test
    void testRegisterUserSuccess() {
        assertDoesNotThrow(() -> spotifySystem.registerUser("test@example.com", "password123"),
                "Registration should succeed with valid credentials");
    }

    @Test
    void testRegisterUserNullOrEmpty() {
        assertThrows(ValidationException.class, () -> spotifySystem.registerUser(null, "pass"),
                "Should throw ValidationException for null email");
        assertThrows(ValidationException.class, () -> spotifySystem.registerUser("", "pass"),
                "Should throw ValidationException for empty email");
        assertThrows(ValidationException.class, () -> spotifySystem.registerUser("email", null),
                "Should throw ValidationException for null password");
        assertThrows(ValidationException.class, () -> spotifySystem.registerUser("email", ""),
                "Should throw ValidationException for empty password");
    }

    @Test
    void testRegisterUserAlreadyExists() {
        spotifySystem.registerUser("test@example.com", "password123");
        assertThrows(AuthenticationException.class,
                () -> spotifySystem.registerUser("test@example.com", "newpass"),
                "Should throw AuthenticationException if user already exists");
    }

    @Test
    void testLoginSuccess() {
        spotifySystem.registerUser("test@example.com", "password123");
        UserDTO user = spotifySystem.login("test@example.com", "password123", responseSenderMock);
        assertNotNull(user, "Login should return a UserDTO");
        assertEquals("test@example.com", user.email(), "UserDTO should have correct email");
    }

    @Test
    void testLoginFailure() {
        spotifySystem.registerUser("test@example.com", "password123");

        assertThrows(AuthenticationException.class,
                () -> spotifySystem.login("test@example.com", "wrongpass", responseSenderMock),
                "Should throw AuthenticationException for wrong password");

        assertThrows(AuthenticationException.class,
                () -> spotifySystem.login("nonexistent@example.com", "password123", responseSenderMock),
                "Should throw AuthenticationException for nonexistent user");
    }

    @Test
    void testCreatePlaylistSuccess() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);

        UserDTO userDTO = spotifySystem.createPlaylist("act i", responseSenderMock);
        assertNotNull(userDTO, "Should return updated UserDTO");
        assertTrue(userDTO.playlistNames().contains("act i"), "User playlist list should contain the new playlist");
    }

    @Test
    void testCreatePlaylistNotLoggedIn() {
        assertThrows(AuthenticationException.class,
                () -> spotifySystem.createPlaylist("Playlist", responseSenderMock),
                "Should throw AuthenticationException if not logged in");
    }

    @Test
    void testCreatePlaylistAlreadyExists() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);

        spotifySystem.createPlaylist("Repeat", responseSenderMock);
        assertThrows(SourceAlreadyExistsException.class,
                () -> spotifySystem.createPlaylist("Repeat", responseSenderMock),
                "Should throw SourceAlreadyExistsException for duplicate playlist name");
    }

    @Test
    void testAddSongToPlaylistSuccess() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);
        spotifySystem.createPlaylist("MyPlaylist", responseSenderMock);

        SongMetadata metadata = new SongMetadata("id1", "Title", "Artist", "path", null);
        spotifySystem.addNewTrack(metadata);

        assertDoesNotThrow(() -> spotifySystem.addSongToPlaylist("MyPlaylist", "Title", responseSenderMock));

        List<Track> tracks = spotifySystem.getPlaylistTracks("MyPlaylist", responseSenderMock);
        assertEquals(1, tracks.size(), "Playlist should contain 1 track");
        assertEquals("Title", tracks.get(0).metadata().title(), "Track title should match");
    }

    @Test
    void testAddSongToPlaylistSongNotFound() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);
        spotifySystem.createPlaylist("MyPlaylist", responseSenderMock);

        assertThrows(SourceNotFoundException.class,
                () -> spotifySystem.addSongToPlaylist("MyPlaylist", "NonExistent", responseSenderMock),
                "Should throw SourceNotFoundException if song does not exist");
    }

    @Test
    void testAddSongToPlaylistPlaylistNotFound() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);

        SongMetadata metadata = new SongMetadata("id1", "Title", "Artist", "path", null);
        spotifySystem.addNewTrack(metadata);

        assertThrows(SourceNotFoundException.class,
                () -> spotifySystem.addSongToPlaylist("NonExistentPlaylist", "Title", responseSenderMock),
                "Should throw SourceNotFoundException if playlist does not exist");
    }

    @Test
    void testSearch() {
        spotifySystem.addNewTrack(new SongMetadata("1", "Hello World", "Artist One", "p1", null));
        spotifySystem.addNewTrack(new SongMetadata("2", "Another Song", "Artist Two", "p2", null));
        spotifySystem.addNewTrack(new SongMetadata("3", "Hello Sunshine", "Artist One", "p3", null));

        List<Track> results = spotifySystem.search(List.of("hello"));
        assertEquals(2, results.size(), "Should find 2 songs with 'hello'");

        results = spotifySystem.search(List.of("artist one"));
        assertEquals(2, results.size(), "Should find 2 songs by 'Artist One'");

        results = spotifySystem.search(List.of("world"));
        assertEquals(1, results.size(), "Should find 1 song with 'world'");
    }

    @Test
    void testTopGlobalPlayingTracks() {
        SongMetadata m1 = new SongMetadata("1", "T1", "A1", "p1", null);
        SongMetadata m2 = new SongMetadata("2", "T2", "A2", "p2", null);
        spotifySystem.addNewTrack(m1);
        spotifySystem.addNewTrack(m2);

        List<Track> top = spotifySystem.topGlobalPlayingTracks(2);
        assertTrue(top.isEmpty() || top.stream().allMatch(t -> t.playCount() == 0),
                "Should be empty or all zero play counts initially");

        spotifySystem.search(List.of("T1")).get(0).incrementPlayCount();
        spotifySystem.search(List.of("T1")).get(0).incrementPlayCount();
        spotifySystem.search(List.of("T2")).get(0).incrementPlayCount();

        top = spotifySystem.topGlobalPlayingTracks(2);
        assertEquals("T1", top.get(0).metadata().title(), "T1 should be first");
        assertEquals("T2", top.get(1).metadata().title(), "T2 should be second");
    }

    @Test
    void testStreamTrackSuccess() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);

        SongMetadata metadata = new SongMetadata("id1", "Title", "Artist", "path", null);
        spotifySystem.addNewTrack(metadata);

        assertDoesNotThrow(() -> spotifySystem.streamTrack("Title", responseSenderMock),
                "Should start streaming successfully");

        List<Track> current = spotifySystem.topCurrentPlayingTracks(1);
        assertEquals(1, current.size(), "Should have 1 currently playing track");
        assertEquals("Title", current.get(0).metadata().title());
    }

    @Test
    void testStreamTrackNotLoggedIn() {
        assertThrows(AuthenticationException.class,
                () -> spotifySystem.streamTrack("Title", responseSenderMock),
                "Should throw AuthenticationException if not logged in");
    }

    @Test
    void testStreamTrackSongNotFound() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);

        assertThrows(SourceNotFoundException.class,
                () -> spotifySystem.streamTrack("NonExistent", responseSenderMock),
                "Should throw SourceNotFoundException if song not found");
    }

    @Test
    void testStreamTrackAlreadyPlaying() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);

        SongMetadata metadata = new SongMetadata("id1", "Title", "Artist", "path", null);
        spotifySystem.addNewTrack(metadata);

        spotifySystem.streamTrack("Title", responseSenderMock);

        assertThrows(SourceAlreadyExistsException.class,
                () -> spotifySystem.streamTrack("Title", responseSenderMock),
                "Should throw SourceAlreadyExistsException if same song is already playing");
    }

    @Test
    void testStopStreamingTrackSuccess() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);

        SongMetadata metadata = new SongMetadata("id1", "Title", "Artist", "path", null);
        spotifySystem.addNewTrack(metadata);

        spotifySystem.streamTrack("Title", responseSenderMock);

        assertDoesNotThrow(() -> spotifySystem.stopStreamingTrack(responseSenderMock),
                "Should stop streaming successfully");

        List<Track> current = spotifySystem.topCurrentPlayingTracks(1);
        assertTrue(current.isEmpty(), "Should have no currently playing tracks after stop");
    }

    @Test
    void testStopStreamingTrackNothingPlaying() {
        assertThrows(ValidationException.class,
                () -> spotifySystem.stopStreamingTrack(responseSenderMock),
                "Should throw ValidationException if nothing is playing");
    }

    @Test
    void testGetPlaylistTracksEmpty() {
        spotifySystem.registerUser("test@example.com", "password123");
        spotifySystem.login("test@example.com", "password123", responseSenderMock);
        spotifySystem.createPlaylist("EmptyPlaylist", responseSenderMock);

        List<Track> tracks = spotifySystem.getPlaylistTracks("EmptyPlaylist", responseSenderMock);
        assertNotNull(tracks, "Should return a list");
        assertTrue(tracks.isEmpty(), "Should return empty list for empty playlist");
    }

    @Test
    void testGetPlaylistTracksNotLoggedIn() {
        assertThrows(AuthenticationException.class,
                () -> spotifySystem.getPlaylistTracks("Playlist", responseSenderMock),
                "Should throw AuthenticationException if not logged in");
    }

}
