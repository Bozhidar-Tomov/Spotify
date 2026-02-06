package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketResponseSender implements ResponseSender {
    private final SocketChannel clientChannel;

    public SocketResponseSender(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    public void sendResponse(Response response) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(response);
            oos.flush();
            byte[] responseBytes = bos.toByteArray();
            send(responseBytes);
        }
    }

    private void send(byte[] bytes) throws IOException {
        ByteBuffer payloadBuffer = ByteBuffer.wrap(bytes);
        ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
        lengthBuffer.putInt(bytes.length);
        lengthBuffer.flip();

        synchronized (clientChannel) {
            while (lengthBuffer.hasRemaining()) {
                clientChannel.write(lengthBuffer);
            }
            while (payloadBuffer.hasRemaining()) {
                clientChannel.write(payloadBuffer);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SocketResponseSender that = (SocketResponseSender) o;
        return clientChannel.equals(that.clientChannel);
    }

    @Override
    public int hashCode() {
        return clientChannel.hashCode();
    }
}
