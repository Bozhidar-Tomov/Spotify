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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlaySongCommandTest {

    private PlaySongCommand command;

    @Mock
    private SpotifySystem systemMock;

    @Mock
    private ResponseSender clientMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new PlaySongCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of(), systemMock, clientMock);
        assertEquals(400, response.statusCode());
        assertEquals("Usage: play <song name>", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of("song"), systemMock, clientMock);
        assertEquals(500, response.statusCode());
    }

    @Test
    void testExecuteSuccess() throws Exception {
        Response response = command.execute(List.of("song"), systemMock, clientMock);
        verify(systemMock).streamTrack("song", clientMock);
        assertNull(response, "PlaySongCommand returns null on success as per implementation");
    }

    @Test
    void testExecuteValidationException() throws Exception {
        doThrow(new ValidationException("Error")).when(systemMock).streamTrack("song", clientMock);
        Response response = command.execute(List.of("song"), systemMock, clientMock);
        assertEquals(400, response.statusCode());
        assertEquals("Request: Error", response.message());
    }

    @Test
    void testExecuteAuthenticationException() throws Exception {
        doThrow(new AuthenticationException("Error")).when(systemMock).streamTrack("song", clientMock);
        Response response = command.execute(List.of("song"), systemMock, clientMock);
        assertEquals(401, response.statusCode());
        assertEquals("Auth: Error", response.message());
    }

    @Test
    void testExecuteSourceNotFoundException() throws Exception {
        doThrow(new SourceNotFoundException("Error")).when(systemMock).streamTrack("song", clientMock);
        Response response = command.execute(List.of("song"), systemMock, clientMock);
        assertEquals(404, response.statusCode());
        assertEquals("Missing: Error", response.message());
    }
}
