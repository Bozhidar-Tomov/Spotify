package bg.sofia.uni.fmi.mjt.spotify.server.commands;

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

public class StopCommandTest {

    private StopCommand command;

    @Mock
    private SpotifySystem systemMock;

    @Mock
    private ResponseSender clientMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new StopCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of("extra"), systemMock, clientMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Usage: stop", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of(), systemMock, clientMock);
        assertEquals(INTERNAL_SERVER_ERROR, response.statusCode());
    }

    @Test
    void testExecuteSuccess() throws Exception {
        Response response = command.execute(List.of(), systemMock, clientMock);
        verify(systemMock).stopStreamingTrack(clientMock);
        assertEquals(OK, response.statusCode());
        assertEquals("Stopped", response.message());
    }

    @Test
    void testExecuteValidationException() throws Exception {
        doThrow(new ValidationException("Error")).when(systemMock).stopStreamingTrack(clientMock);
        Response response = command.execute(List.of(), systemMock, clientMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Request: Error", response.message());
    }
}
