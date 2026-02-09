package bg.sofia.uni.fmi.mjt.spotify.client.net;

import bg.sofia.uni.fmi.mjt.spotify.client.SpotifyClient;
import bg.sofia.uni.fmi.mjt.spotify.client.audio.AudioPlayer;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;
import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.BinaryPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.UserDtoPayload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerTest {

    @Mock
    private SocketChannel socketChannel;
    @Mock
    private AudioPlayer audioPlayer;
    @Mock
    private SpotifyClient client;

    @InjectMocks
    private ResponseHandler responseHandler;

    private void mockServerResponse(Response response) throws IOException {
        byte[] data;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(response);
            data = baos.toByteArray();
        }

        when(socketChannel.isOpen()).thenReturn(true, false);

        when(socketChannel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buffer = invocation.getArgument(0);
            if (buffer.remaining() == 4) {
                buffer.putInt(data.length);
                return 4;
            } else {
                buffer.put(data);
                return data.length;
            }
        });
    }

    @Test
    void testHandleAudioFormat() throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        Response response = new Response(200, "OK", AudioFormatPayload.from(format));

        mockServerResponse(response);
        responseHandler.run();

        verify(audioPlayer).init(any(AudioFormatPayload.class));
    }

    @Test
    void testHandleUserDto() throws Exception {
        UserDTO user = new UserDTO("test@fmi.bg", Set.of());
        Response response = new Response(200, "Welcome", new UserDtoPayload(user));

        mockServerResponse(response);
        responseHandler.run();

        verify(client).setUser(user);
    }

    @Test
    void testHandleAudioStream() throws Exception {
        byte[] chunk = { 0, 1, 0, 1 };
        Response response = new Response(200, "STREAM", new BinaryPayload(chunk));

        when(audioPlayer.isActive()).thenReturn(true);
        mockServerResponse(response);
        responseHandler.run();

        verify(audioPlayer).playChunk(chunk);
    }

    @Test
    void testHandleStreamEnd() throws Exception {
        Response response = new Response(200, "STREAM_END", null);

        mockServerResponse(response);
        responseHandler.run();

        verify(audioPlayer).stop();
    }

    @Test
    void testConnectionClosedByServer() throws Exception {
        when(socketChannel.isOpen()).thenReturn(true, false);
        when(socketChannel.read(any(ByteBuffer.class))).thenReturn(-1);

        responseHandler.run();

        verify(socketChannel).close();
        verify(audioPlayer).close();
    }
}