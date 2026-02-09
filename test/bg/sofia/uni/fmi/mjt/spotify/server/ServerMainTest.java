package bg.sofia.uni.fmi.mjt.spotify.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServerMainTest {

    @Mock
    private SpotifySystem spotifySystemMock;

    private ServerMain server;
    private final int testPort = 7777;

    @BeforeEach
    void setUp() throws Exception {
        server = new ServerMain(testPort);
        injectMock(spotifySystemMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        injectMock(null);
    }

    @Test
    void testStartCallsSystemWithCorrectPort() throws Exception {
        server.start();
        verify(spotifySystemMock, times(1)).start(testPort);
    }

    @Test
    void testStopCallsSystemStop() {
        server.stop();
        verify(spotifySystemMock, times(1)).stop();
    }

    @Test
    void testStartPropagatesExceptions() throws Exception {
        doThrow(new RuntimeException("Network Error")).when(spotifySystemMock).start(anyInt());
        
        assertThrows(RuntimeException.class, () -> server.start(), 
            "ServerMain should not swallow exceptions from the system.");
    }

    private void injectMock(SpotifySystem mock) throws Exception {
        Field instanceField = SpotifySystem.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, mock);

        Field systemField = ServerMain.class.getDeclaredField("system");
        systemField.setAccessible(true);
        systemField.set(server, mock);
    }

    @Test
    void testMainMethodFullFlow() throws Exception {
        String simulatedInput = "random_command\nQUIT\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

        ServerMain.main(new String[]{});

        assertEquals("Server Host Thread", Thread.currentThread().getName(), 
            "The main thread name should be updated");

        verify(spotifySystemMock, times(1)).start(7777);
        verify(spotifySystemMock, times(1)).stop();
    }

    @Test
    void testMainMethodStartupFailure() throws Exception {
        doThrow(new RuntimeException("Port Conflict")).when(spotifySystemMock).start(anyInt());
        ServerMain.main(new String[]{});
        verify(spotifySystemMock, never()).stop();
    }
}