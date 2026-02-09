package bg.sofia.uni.fmi.mjt.spotify.server.net;

import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.server.commands.CommandDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RequestHandlerTest {
    private SelectionKey selectionKeyMock;
    private SpotifySystem spotifySystemMock;
    private SocketChannel socketChannelMock;
    private Selector selectorMock;
    private ByteBuffer buffer;
    private ResponseSender responseSenderMock;

    @BeforeEach
    void setUp() {
        selectionKeyMock = mock(SelectionKey.class);
        spotifySystemMock = mock(SpotifySystem.class);
        socketChannelMock = mock(SocketChannel.class);
        selectorMock = mock(Selector.class);
        responseSenderMock = mock(ResponseSender.class);

        buffer = ByteBuffer.allocate(512);

        when(selectionKeyMock.channel()).thenReturn(socketChannelMock);
        when(selectionKeyMock.attachment()).thenReturn(buffer);
        when(selectionKeyMock.selector()).thenReturn(selectorMock);
        when(selectionKeyMock.isValid()).thenReturn(true);
        when(socketChannelMock.isOpen()).thenReturn(true);
    }

    private RequestHandler createHandlerWithMockSender() {
        return new RequestHandler(selectionKeyMock, spotifySystemMock) {
            @Override
            ResponseSender createResponseSender(SocketChannel channel) {
                return responseSenderMock;
            }
        };
    }

    @Test
    void testRunSuccessfulRequest() throws IOException {
        String requestString = "login user pass";

        Response mockResponse = mock(Response.class);
        when(mockResponse.statusCode()).thenReturn(OK);
        when(mockResponse.message()).thenReturn("Welcome");

        buffer.clear();
        buffer.put(requestString.getBytes(StandardCharsets.UTF_8));
        buffer.flip();

        when(socketChannelMock.read(any(ByteBuffer.class))).thenReturn(requestString.length());

        try (MockedStatic<CommandDispatcher> dispatcherMock = mockStatic(CommandDispatcher.class)) {
            dispatcherMock.when(() -> CommandDispatcher.dispatch(any(), eq(spotifySystemMock), any()))
                    .thenReturn(mockResponse);

            RequestHandler handler = createHandlerWithMockSender();
            handler.run();

            verify(selectionKeyMock).interestOps(SelectionKey.OP_READ);
            verify(selectorMock).wakeup();
            verify(responseSenderMock).sendResponse(mockResponse);
        }
    }

    @Test
    void testRunClientDisconnect() throws IOException {
        when(socketChannelMock.read(any(ByteBuffer.class))).thenReturn(-1);

        RequestHandler handler = createHandlerWithMockSender();
        handler.run();

        verify(selectionKeyMock).cancel();
        verify(socketChannelMock).close();
        verifyNoInteractions(responseSenderMock);
    }

    @Test
    void testRunIOExceptionDuringRead() throws IOException {
        when(socketChannelMock.read(any(ByteBuffer.class))).thenThrow(new IOException("Connection reset"));

        RequestHandler handler = createHandlerWithMockSender();
        handler.run();

        verify(selectionKeyMock).cancel();
        verify(socketChannelMock).close();
        verifyNoInteractions(responseSenderMock);
    }

    @Test
    void testRunClientSendsEmptyRequest() throws IOException {
        when(socketChannelMock.read(any(ByteBuffer.class))).thenReturn(0);

        RequestHandler handler = createHandlerWithMockSender();
        handler.run();

        verify(selectionKeyMock, never()).cancel();
        verify(socketChannelMock, never()).close();
        verifyNoInteractions(responseSenderMock);
    }

    @Test
    void testRunInternalErrorSendsErrorResponse() throws IOException {
        String requestString = "cmd";
        when(socketChannelMock.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer b = invocation.getArgument(0);
            b.put(requestString.getBytes(StandardCharsets.UTF_8));
            return requestString.length();
        });

        try (MockedStatic<CommandDispatcher> dispatcherMock = mockStatic(CommandDispatcher.class)) {
            dispatcherMock.when(() -> CommandDispatcher.dispatch(any(), any(), any()))
                    .thenThrow(new RuntimeException("Database down"));

            RequestHandler handler = createHandlerWithMockSender();
            handler.run();

            verify(responseSenderMock)
                    .sendResponse(argThat(r -> r.statusCode() == INTERNAL_SERVER_ERROR && r.message().equals("Internal server error.")));
        }
    }

    @Test
    void testRunResponseSendingFails() throws IOException {
        String requestString = "cmd";
        when(socketChannelMock.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer b = invocation.getArgument(0);
            b.put(requestString.getBytes(StandardCharsets.UTF_8));
            return requestString.length();
        });

        try (MockedStatic<CommandDispatcher> dispatcherMock = mockStatic(CommandDispatcher.class)) {
            dispatcherMock.when(() -> CommandDispatcher.dispatch(any(), any(), any()))
                    .thenReturn(new Response(OK, "OK", null));

            doThrow(new IOException("Write failed")).when(responseSenderMock).sendResponse(any());

            RequestHandler handler = createHandlerWithMockSender();
            handler.run();

            verify(selectionKeyMock).cancel();
            verify(socketChannelMock).close();
            verify(responseSenderMock).sendResponse(any());
        }
    }

    @Test
    void testRunInternalErrorAndResponseSendingFails() throws IOException {
        String requestString = "cmd";
        when(socketChannelMock.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer b = invocation.getArgument(0);
            b.put(requestString.getBytes(StandardCharsets.UTF_8));
            return requestString.length();
        });

        try (MockedStatic<CommandDispatcher> dispatcherMock = mockStatic(CommandDispatcher.class)) {
            dispatcherMock.when(() -> CommandDispatcher.dispatch(any(), any(), any()))
                    .thenThrow(new RuntimeException("Something went wrong"));

            doThrow(new IOException("Failed to send error response")).when(responseSenderMock)
                    .sendResponse(argThat(r -> r.statusCode() == INTERNAL_SERVER_ERROR));

            RequestHandler handler = createHandlerWithMockSender();
            handler.run();

            verify(selectionKeyMock).cancel();
            verify(socketChannelMock).close();
            verify(responseSenderMock).sendResponse(argThat(r -> r.statusCode() == INTERNAL_SERVER_ERROR));
        }
    }
}