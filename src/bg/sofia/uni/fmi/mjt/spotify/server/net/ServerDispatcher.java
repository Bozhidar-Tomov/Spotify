package bg.sofia.uni.fmi.mjt.spotify.server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerDispatcher implements Runnable, AutoCloseable {
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 512;
    private final int port;

    private ServerSocketChannel serverSocketChannel;
    private ExecutorService executor;
    private Selector selector;

    public ServerDispatcher(int port) {
        this.port = port;
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Dispatcher Server Thread");

        try {
            setup();
            runLoop();
        } catch (IOException e) {
            System.err.println("Error during server operation: " + e.getMessage());
        } finally {
            stop();
        }
    }

    @Override
    public void close() {
        stop();
    }

    private void setup() throws IOException {
        selector = Selector.open();

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(SERVER_HOST, port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("NIO server started on " + SERVER_HOST + ":" + port);
    }

    private void runLoop() throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            int readyChannels = selector.select();
            if (readyChannels == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    acceptClientRequest(key);
                } else if (key.isReadable()) {
                    readClientRequest(key);
                }

            }
        }
    }

    private void acceptClientRequest(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null)
            return;

        clientChannel.configureBlocking(false);
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        clientChannel.register(selector, SelectionKey.OP_READ, buffer);
        System.out.println("Accepted connection: " + clientChannel.getRemoteAddress());
    }

    private void readClientRequest(SelectionKey key) {
        if (!key.isValid())
            return;

        key.interestOps(0);

        try {
            executor.execute(new ClientHandler(key));

        } catch (Exception e) {
            System.err.println("Failed to process client: " + e.getMessage());
            cancelKey(key);
        }
    }

    private void cancelKey(SelectionKey key) {
        try {
            key.channel().close();
            key.cancel();
        } catch (IOException ignored) {
            //
        }
    }

    public void stop() {
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
            }

            if (selector != null) {
                selector.wakeup();
                selector.close();
            }
            System.out.println("Network resources released.");
        } catch (IOException e) {
            System.err.println("Error closing network: " + e.getMessage());
        }

        try {
            if (executor != null) {
                executor.close();
            }
            System.out.println("Executor stopped.");
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("NIO server closed normally.");
    }
}
