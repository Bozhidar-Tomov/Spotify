package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddSongCommandTest {

    private AddSongCommand command;

    @Mock
    private SpotifySystem systemMock;

    @Mock
    private ResponseSender clientMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new AddSongCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of("playlist"), systemMock, clientMock);
        assertEquals(400, response.statusCode());
        assertEquals("Usage: add-song-to <playlist_name> <song_name>", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of("p", "s"), systemMock, clientMock);
        assertEquals(500, response.statusCode());
    }

    @Test
    void testExecuteSuccess() throws ValidationException, SourceNotFoundException, AuthenticationException {
        Response response = command.execute(List.of("p", "s"), systemMock, clientMock);
        verify(systemMock).addSongToPlaylist("p", "s", clientMock);
        assertEquals(200, response.statusCode());
        assertEquals("Song added to playlist.", response.message());
    }

    @Test
    void testExecuteValidationException() throws ValidationException, SourceNotFoundException, AuthenticationException {
        doThrow(new ValidationException("Error")).when(systemMock).addSongToPlaylist("p", "s", clientMock);
        Response response = command.execute(List.of("p", "s"), systemMock, clientMock);
        assertEquals(400, response.statusCode());
        assertEquals("Request: Error", response.message());
    }

    @Test
    void testExecuteAuthenticationException()
            throws ValidationException, SourceNotFoundException, AuthenticationException {
        doThrow(new AuthenticationException("Error")).when(systemMock).addSongToPlaylist("p", "s", clientMock);
        Response response = command.execute(List.of("p", "s"), systemMock, clientMock);
        assertEquals(401, response.statusCode());
        assertEquals("Auth: Error", response.message());
    }

    @Test
    void testExecuteSourceNotFoundException()
            throws ValidationException, SourceNotFoundException, AuthenticationException {
        doThrow(new SourceNotFoundException("Error")).when(systemMock).addSongToPlaylist("p", "s", clientMock);
        Response response = command.execute(List.of("p", "s"), systemMock, clientMock);
        assertEquals(404, response.statusCode());
        assertEquals("Missing: Error", response.message());
    }
}
