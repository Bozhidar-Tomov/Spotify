package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Server Dispatcher Integration Tests")
class ServerDispatcherTest {
    private static final int TEST_PORT = 8080;
    private static final String HOST = "localhost";

    private ServerDispatcher dispatcher;
    private SpotifySystem mockSystem;
    private Thread serverThread;

    @BeforeEach
    void setUp() {
        mockSystem = mock(SpotifySystem.class);
        dispatcher = new ServerDispatcher(TEST_PORT, mockSystem);

        serverThread = new Thread(dispatcher);
        serverThread.start();

        try {
            TimeUnit.MILLISECONDS.sleep(350);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    void tearDown() {
        dispatcher.stop();
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @Nested
    @DisplayName("Connection Lifecycle Tests")
    class ConnectionTests {

        @Test
        @DisplayName("Client should connect successfully to the server")
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        void testClientCanConnectSuccessfully() throws IOException {
            try (SocketChannel client = SocketChannel.open(new InetSocketAddress(HOST, TEST_PORT))) {
                assertTrue(client.isConnected(), "Client should be able to connect to the server");
                assertTrue(client.isOpen(), "Client channel should be open");
            }
        }

        @Test
        @DisplayName("Server should handle multiple concurrent client connections")
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        void testServerHandlesMultipleConnections() throws IOException {
            try (SocketChannel client1 = SocketChannel.open(new InetSocketAddress(HOST, TEST_PORT));
                    SocketChannel client2 = SocketChannel.open(new InetSocketAddress(HOST, TEST_PORT))) {

                assertTrue(client1.isConnected(), "First client should be connected");
                assertTrue(client2.isConnected(), "Second client should be connected");
            }
        }
    }

    @Nested
    @DisplayName("Data Communication Tests")
    class CommunicationTests {

        @Test
        @DisplayName("Server should remain stable when receiving data from a client")
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        void testServerReadsDataFromClient() throws IOException {
            try (SocketChannel client = SocketChannel.open(new InetSocketAddress(HOST, TEST_PORT))) {
                client.configureBlocking(true);

                String message = "test-command\n";
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());

                while (buffer.hasRemaining()) {
                    int written = client.write(buffer);
                    assertTrue(written >= 0, "Should write data to server");
                }

                assertTrue(client.isConnected(), "Client should still be connected after writing");
                assertTrue(client.isOpen(), "Client channel should be open after writing");
            }
        }
    }

    @Nested
    @DisplayName("Server Resource Management Tests")
    class ShutdownTests {

        @Test
        @DisplayName("Server stop should release port and refuse new connections")
        void testStopClosesResources() {
            dispatcher.stop();

            assertThrows(IOException.class, () -> {
                try (SocketChannel client = SocketChannel.open()) {
                    client.socket().connect(new InetSocketAddress(HOST, TEST_PORT), 100);
                }
            }, "Connection should be refused after server stop");
        }

        @Test
        @DisplayName("Server should handle multiple stop() calls gracefully")
        void testStopIsIdempotent() {
            dispatcher.stop();
            dispatcher.stop();
            dispatcher.stop();
            assertFalse(serverThread.isAlive(), "Server thread should eventually terminate");
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle IOException during select() and break if selector is closed")
        void testRunLoopHandlesClosedSelector() throws IOException {
            dispatcher.stop(); 
            
            try {
                serverThread.join(1000);
                assertFalse(serverThread.isAlive(), "Server thread should terminate when selector is closed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Test
        @DisplayName("Should handle failure to configure client channel")
        void testAcceptClientRequestConfigurationFailure() throws Exception {
            ServerSocketChannel mockServerChannel = mock(ServerSocketChannel.class);
            SocketChannel mockClientChannel = mock(SocketChannel.class);
            SelectionKey mockKey = mock(SelectionKey.class);

            when(mockKey.channel()).thenReturn(mockServerChannel);
            when(mockServerChannel.accept()).thenReturn(mockClientChannel);

            doThrow(new IOException("Simulated config failure"))
                    .when(mockClientChannel).configureBlocking(false);


            Method method = ServerDispatcher.class
                    .getDeclaredMethod("acceptClientRequest", SelectionKey.class);
            method.setAccessible(true);

            method.invoke(dispatcher, mockKey);
            verify(mockClientChannel).close();
        }

        @Test
        @DisplayName("Should handle CancelledKeyException in readClientRequest and cleanup resources")
        void testReadClientRequestHandlesCancelledKey() throws Exception {

            SelectionKey mockKey = mock(SelectionKey.class);
            SocketChannel mockChannel = mock(SocketChannel.class);
            
            when(mockKey.isValid()).thenReturn(true);
            when(mockKey.channel()).thenReturn(mockChannel);
            
            doThrow(new CancelledKeyException()).when(mockKey).interestOps(0);

            Method method = ServerDispatcher.class
                    .getDeclaredMethod("readClientRequest", SelectionKey.class);
            method.setAccessible(true);

            assertDoesNotThrow(() -> {
                try {
                    method.invoke(dispatcher, mockKey);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }, "The dispatcher should catch CancelledKeyException and not rethrow it");

            verify(mockKey).cancel();
            verify(mockChannel).close();
        }
    }
}