package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class ServerDispatcher implements Runnable, AutoCloseable {
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 512;
    private final int port;
    private final SpotifySystem system;

    private ServerSocketChannel serverSocketChannel;
    private ExecutorService executor;
    private Selector selector;

    public ServerDispatcher(int port, SpotifySystem system) {
        this.port = port;
        this.system = system;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Dispatcher Server Thread");

        try {
            setup();
            runLoop();
        } catch (IOException | SpotifyException e) {
            System.err.println("Fatal server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    @Override
    public void close() {
        stop();
    }

    private void setup() throws IOException {
        System.out.println("NIO server starting...");
        selector = Selector.open();

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(SERVER_HOST, port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("NIO server started on " + SERVER_HOST + ":" + port);
    }

    private void runLoop() throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (selector.select() == 0)
                    continue;
                processSelectedKeys(selector.selectedKeys());
            } catch (ClosedSelectorException e) {
                break;
            } catch (IOException e) {
                if (handleLoopError(e))
                    break;
            }
        }
    }

    private void processSelectedKeys(Set<SelectionKey> selectedKeys) {
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();

            if (key.isValid()) {
                handleKey(key);
            }
        }
    }

    private void handleKey(SelectionKey key) {
        try {
            if (key.isAcceptable())
                acceptClientRequest(key);
            if (key.isReadable())
                readClientRequest(key);
        } catch (Exception e) {
            System.err.println("Error handling client key: " + e.getMessage());
            cancelKey(key);
        }
    }

    private boolean handleLoopError(IOException e) {
        System.err.println("Error during select: " + e.getMessage());
        return !selector.isOpen();
    }

    private void acceptClientRequest(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel == null) {
            return;
        }
        try {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
            System.out.println("Accepted connection: " + clientChannel.getRemoteAddress());
        } catch (IOException e) {
            System.err.println("Failed to configure new client connection: " + e.getMessage());
            clientChannel.close();
        }
    }

    private void readClientRequest(SelectionKey key) {
        if (!key.isValid()) {
            return;
        }

        try {
            key.interestOps(0);
            executor.execute(new RequestHandler(key, system));
        } catch (RejectedExecutionException e) {
            System.err.println(
                "Server busy, pool exhausted or or shutting down.Closing client connection.");
            cancelKey(key);
        } catch (CancelledKeyException e) {
            System.err.println("Key already cancelled");
            cancelKey(key);
        } catch (Exception e) {
            System.err.println("Error during processing client request: " + e.getMessage());
            cancelKey(key);
        }
    }

    private void cancelKey(SelectionKey key) {
        try {
            if (key == null) {
                return;
            }
            key.cancel();

            if (key.channel() == null) {
                return;
            }
            key.channel().close();
        } catch (IOException e) {
            System.err.println("Error closing key/channel: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (executor != null) {
                executor.close();
            }
            System.out.println("Client tasks stopped.");
        } catch (Exception e) {
            System.err.println("Error closing tasks: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        try {
            if (selector != null && selector.isOpen()) {
                selector.wakeup();
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing selector: " + e.getMessage());
        }

        try {
            if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket channel: " + e.getMessage());
        }

        System.out.println("NIO server resources released.");
    }
}