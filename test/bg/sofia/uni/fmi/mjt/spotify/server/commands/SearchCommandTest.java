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

public class SearchCommandTest {

    private SearchCommand command;

    @Mock
    private SpotifySystem systemMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new SearchCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of(), systemMock);
        assertEquals(400, response.statusCode());
        assertEquals("Usage: search <word_1> ... <word_N>", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of("word"), systemMock);
        assertEquals(500, response.statusCode());
    }

    @Test
    void testExecuteNoTracksFound() {
        when(systemMock.search(List.of("word"))).thenReturn(Collections.emptyList());
        Response response = command.execute(List.of("word"), systemMock);
        assertEquals(200, response.statusCode());
        assertEquals("No tracks found.", response.message());
    }

    @Test
    void testExecuteSuccess() {
        Track track = new Track(null);
        when(systemMock.search(List.of("word"))).thenReturn(List.of(track));
        Response response = command.execute(List.of("word"), systemMock);
        assertEquals(200, response.statusCode());
        assertEquals("Found 1 tracks.", response.message());
    }
}
