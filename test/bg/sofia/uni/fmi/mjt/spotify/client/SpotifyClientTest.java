package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SpotifyClientTest {
    private ServerSocket gameServer;
    private ExecutorService serverExecutor;
    private SpotifyClient client;
    private int port;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() throws IOException {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        gameServer = new ServerSocket(0);
        port = gameServer.getLocalPort();
        serverExecutor = Executors.newSingleThreadExecutor();

        client = new SpotifyClient();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.stop();
        }
        if (gameServer != null && !gameServer.isClosed()) {
            gameServer.close();
        }
        serverExecutor.shutdownNow();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testClientConnects() throws IOException, InterruptedException {
        startServer();
        client.start(port);

        Thread.sleep(100);
        assertTrue(outContent.toString().contains("Connected."),
                "Client should print 'Connected.' upon successful connection");
    }

    @Test
    void testClientHandlesConnectionRefused() {
        int unusedPort = findUnusedPort();
        try {
            client.start(unusedPort);
            fail("Should throw IOException when connection is refused");
        } catch (Exception e) {
            assertTrue(outContent.toString().contains("Server unreachable. Connection refused."),
                    "Client should print error message");
        }
    }

    @Test
    void testSetAndGetUser() {
        UserDTO user = new UserDTO("test@test.com", Set.of("Playlist1", "Playlist2"));
        client.setUser(user);
        assertEquals(user, client.getUser(), "User getter should return the set user");
    }

    @Test
    void testUserInputSendsCommand() throws IOException, InterruptedException, java.util.concurrent.ExecutionException {
        java.util.concurrent.Future<String> receivedCommandFuture = serverExecutor.submit(() -> {
            try (java.net.Socket clientSocket = gameServer.accept();
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(clientSocket.getInputStream()))) {
                return reader.readLine();
            }
        });

        client.start(port);
        Thread.sleep(100);

        String testCommand = "test-command";
        client.getUserInput(testCommand);

        String receivedCommand = receivedCommandFuture.get();
        assertEquals(testCommand, receivedCommand, "Server should receive the command sent by client");
    }

    private void startServer() {
        serverExecutor.submit(() -> {
            try {
                gameServer.accept();
            } catch (IOException e) {
            }
        });
    }

    private int findUnusedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 9999;
        }
    }
}
