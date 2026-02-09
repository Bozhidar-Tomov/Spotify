package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class ShowPlaylistCommandTest {

    private ShowPlaylistCommand command;

    @Mock
    private SpotifySystem systemMock;

    @Mock
    private ResponseSender clientMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new ShowPlaylistCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of(), systemMock, clientMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Usage: show-playlist <name_of_the_playlist>", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of("p"), systemMock, clientMock);
        assertEquals(INTERNAL_SERVER_ERROR, response.statusCode());
    }

    @Test
    void testExecutePlaylistEmpty() throws Exception {
        when(systemMock.getPlaylistTracks("p", clientMock)).thenReturn(Collections.emptyList());
        Response response = command.execute(List.of("p"), systemMock, clientMock);
        assertEquals(OK, response.statusCode());
        assertEquals("Playlist 'p' is empty.", response.message());
    }

    @Test
    void testExecuteSuccess() throws Exception {
        Track track = new Track(null);
        when(systemMock.getPlaylistTracks("p", clientMock)).thenReturn(List.of(track));
        Response response = command.execute(List.of("p"), systemMock, clientMock);
        assertEquals(OK, response.statusCode());
        assertEquals("OK", response.message());
    }

    @Test
    void testExecuteValidationException() throws Exception {
        when(systemMock.getPlaylistTracks("p", clientMock)).thenThrow(new ValidationException("Error"));
        Response response = command.execute(List.of("p"), systemMock, clientMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Request: Error", response.message());
    }
}
