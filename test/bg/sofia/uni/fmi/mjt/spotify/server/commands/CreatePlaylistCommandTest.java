package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreatePlaylistCommandTest {

    private CreatePlaylistCommand command;

    @Mock
    private SpotifySystem systemMock;

    @Mock
    private ResponseSender clientMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new CreatePlaylistCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of(), systemMock, clientMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Usage: create-playlist <playlist_name>", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of("p"), systemMock, clientMock);
        assertEquals(INTERNAL_SERVER_ERROR, response.statusCode());
    }

    @Test
    void testExecuteSuccess() throws Exception {
        UserDTO user = new UserDTO("email", Set.of("p"));
        when(systemMock.createPlaylist("p", clientMock)).thenReturn(user);

        Response response = command.execute(List.of("p"), systemMock, clientMock);

        verify(systemMock).createPlaylist("p", clientMock);
        assertEquals(OK, response.statusCode());
        assertEquals("Playlist created.", response.message());
    }

    @Test
    void testExecuteValidationException() throws Exception {
        doThrow(new ValidationException("Error")).when(systemMock).createPlaylist("p", clientMock);
        Response response = command.execute(List.of("p"), systemMock, clientMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Request: Error", response.message());
    }

    @Test
    void testExecuteAuthenticationException() throws Exception {
        doThrow(new AuthenticationException("Error")).when(systemMock).createPlaylist("p", clientMock);
        Response response = command.execute(List.of("p"), systemMock, clientMock);
        assertEquals(UNAUTHORIZED, response.statusCode());
        assertEquals("Auth: Error", response.message());
    }

    @Test
    void testExecuteSourceAlreadyExistsException() throws Exception {
        doThrow(new SourceAlreadyExistsException("Error")).when(systemMock).createPlaylist("p", clientMock);
        Response response = command.execute(List.of("p"), systemMock, clientMock);
        assertEquals(CONFLICT, response.statusCode());
        assertEquals("Conflict: Error", response.message());
    }
}
