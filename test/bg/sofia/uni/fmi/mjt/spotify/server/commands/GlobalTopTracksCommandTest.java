package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GlobalTopTracksCommandTest {

    private GlobalTopTracksCommand command;

    @Mock
    private SpotifySystem systemMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new GlobalTopTracksCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of(), systemMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Usage: top-global <number>", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of("5"), systemMock);
        assertEquals(INTERNAL_SERVER_ERROR, response.statusCode());
    }

    @Test
    void testExecuteNegativeNumber() {
        Response response = command.execute(List.of("-1"), systemMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Must be positive integer.", response.message());
    }

    @Test
    void testExecuteInvalidNumberFormat() {
        Response response = command.execute(List.of("abc"), systemMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Invalid number format: 'abc'", response.message());
    }

    @Test
    void testExecuteNoTracks() {
        when(systemMock.topGlobalPlayingTracks(5)).thenReturn(Collections.emptyList());
        Response response = command.execute(List.of("5"), systemMock);
        assertEquals(OK, response.statusCode());
        assertEquals("No tracks are globally played.", response.message());
    }

    @Test
    void testExecuteSuccess() {
        Track track = new Track(null);
        when(systemMock.topGlobalPlayingTracks(1)).thenReturn(List.of(track));
        Response response = command.execute(List.of("1"), systemMock);
        assertEquals(OK, response.statusCode());
        assertEquals("OK", response.message());
    }
}
