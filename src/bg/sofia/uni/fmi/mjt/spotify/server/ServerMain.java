package bg.sofia.uni.fmi.mjt.spotify.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import bg.sofia.uni.fmi.mjt.spotify.server.net.ServerDispatcher;

public class ServerMain {
    private static final short TIMEOUT = 1200;
    private final int port;

    private ExecutorService managerThread;

    public ServerMain(int port) {
        this.port = port;
    }

    public void start() {
        System.out.println("Server booting...");
        managerThread = Executors.newSingleThreadExecutor();
        managerThread.execute(new ServerDispatcher(port));
    }

    public void stop() {
        System.out.println("Server shutdown...");
        if (managerThread == null) {
            return;
        }

        managerThread.shutdown();
        try {
            if (!managerThread.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS)) {
                managerThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            managerThread.shutdownNow();
        }
    }

    public static void main(String[] args) {
        Thread.currentThread().setName("Main Server Thread");
        ServerMain server = new ServerMain(7777);
        server.start();
        server.stop();
    }
}
