package bg.sofia.uni.fmi.mjt.spotify.client;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClientMain {
    private static final String HOST_NAME = "localhost";
    private static final short SERVER_PORT = 7777;

    public static void main(String[] args) {
        Thread.currentThread().setName("Client Thread");

        try (SocketChannel socketChannel = SocketChannel.open();
                BufferedReader reader = new BufferedReader(Channels.newReader(socketChannel, "UTF-8"));
                PrintWriter writer = new PrintWriter(Channels.newWriter(socketChannel, "UTF-8"), true);
                Scanner scanner = new Scanner(System.in)) {

            System.out.println("---------------------------" + System.lineSeparator() +
                               "| Spotify Music Streaming |" + System.lineSeparator() +
                               "---------------------------");

            socketChannel.connect(new InetSocketAddress(HOST_NAME, SERVER_PORT));
                    
            System.out.println("Connected to the server.");

            while (true) {
                
            }
            
        } catch (Exception e) {
            System.err.println("Problem with network communication: " + e.getMessage());
        }
    }
}
