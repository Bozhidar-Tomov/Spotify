package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SocketResponseSenderTest {

    @Mock
    private SocketChannel socketChannelMock;

    @Mock
    private Response responseMock;

    private SocketResponseSender responseSender;

    @BeforeEach
    void setUp() {
        responseSender = new SocketResponseSender(socketChannelMock);
    }

    @Test
    void testSendResponseThrowsIOExceptionWhenChannelFails() throws IOException {
        when(socketChannelMock.write(any(ByteBuffer.class))).thenThrow(new IOException("Connection reset"));

        assertThrows(IOException.class, () -> responseSender.sendResponse(new Response(400, "fail", null)),
                "Should propagate IOException from SocketChannel");
    }

    @Test
    void testEqualsAndHashCode() {
        SocketChannel otherChannel = mock(SocketChannel.class);
        SocketResponseSender sender1 = new SocketResponseSender(socketChannelMock);
        SocketResponseSender sender2 = new SocketResponseSender(socketChannelMock);
        SocketResponseSender sender3 = new SocketResponseSender(otherChannel);

        assertEquals(sender1, sender2, "Senders with same channel should be equal");
        assertNotEquals(sender1, sender3, "Senders with different channels should not be equal");
        assertEquals(sender1.hashCode(), sender2.hashCode(), "HashCodes should match for equal objects");
    }
}
