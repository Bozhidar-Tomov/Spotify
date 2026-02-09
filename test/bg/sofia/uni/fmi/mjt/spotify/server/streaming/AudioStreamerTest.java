package bg.sofia.uni.fmi.mjt.spotify.server.streaming;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.InternalSystemException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.SongMetadata;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioStreamer Tests")
class AudioStreamerTest {

    @Mock
    private ResponseSender senderMock;
    @Mock
    private AudioInputStream audioInputStreamMock;

    private Track testTrack;
    private AudioStreamer audioStreamer;

    @BeforeEach
    void setUp() {
        SongMetadata testMetadata = new SongMetadata("id1", "Title1", "Artist1", "path",
                new AudioFormatPayload("PCM", 44100, 16, 2, 4, 44100, false));
        testTrack = new Track(testMetadata);
        audioStreamer = new AudioStreamer(senderMock, testTrack, new SynchronousExecutorStub());
    }

    @Nested
    @DisplayName("Validation and Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Constructors should throw IllegalArgumentException when parameters are null")
        void testConstructorsThrowOnNullParameters() {
            assertThrows(IllegalArgumentException.class,
                    () -> new AudioStreamer(null, testTrack),
                    "Should throw when sender is null");

            assertThrows(IllegalArgumentException.class,
                    () -> new AudioStreamer(senderMock, null),
                    "Should throw when track is null");

            assertThrows(IllegalArgumentException.class,
                    () -> new AudioStreamer(null, testTrack, null),
                    "Primary constructor should throw when sender is null");
            assertThrows(IllegalArgumentException.class,
                    () -> new AudioStreamer(senderMock, null, null),
                    "Primary constructor should throw when track is null");

            assertThrows(IllegalArgumentException.class,
                    () -> new AudioStreamer(senderMock, testTrack, null),
                    "Primary constructor should throw when executor is null");
        }

        @Test
        @DisplayName("Two-parameter constructor should initialize correctly with default executor")
        void testTwoParameterConstructorInitializes() {
            AudioStreamer streamer = new AudioStreamer(senderMock, testTrack);

            assertEquals(testTrack.metadata().id(), streamer.track().metadata().id(),
                    "Streamer should be initialized with the provided track");
        }
    }

    @Nested
    @DisplayName("Streaming Lifecycle Tests")
    class StreamingLifecycle {

        @Test
        @DisplayName("startStream() should send format, audio chunks, and end signal")
        void testStartStreamSendsFormatAndData() throws Exception {
            try (MockedStatic<AudioSystem> mockedAudioSystem = mockStatic(AudioSystem.class)) {
                mockedAudioSystem.when(() -> AudioSystem.getAudioInputStream(any(File.class)))
                        .thenReturn(audioInputStreamMock);

                AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);
                when(audioInputStreamMock.getFormat()).thenReturn(format);
                when(audioInputStreamMock.read(any(byte[].class))).thenReturn(4096, 1024, -1);

                audioStreamer.startStream();

                verify(senderMock, times(1))
                        .sendResponse(argThat(r -> r.statusCode() == OK && r.message().startsWith("Playing")));

                verify(senderMock, times(2))
                        .sendResponse(argThat(r -> r.statusCode() == OK && "STREAM".equals(r.message())));

                verify(senderMock, times(1))
                        .sendResponse(argThat(r -> r.statusCode() == OK && "STREAM_END".equals(r.message())));
            }
        }

        @Test
        @DisplayName("endStream() should send STREAM_END signal explicitly")
        void testEndStreamSendsStreamEnd() throws IOException {
            audioStreamer.endStream();
            verify(senderMock).sendResponse(argThat(r -> r.statusCode() == OK && "STREAM_END".equals(r.message())));
        }
    }

    @Test
    @DisplayName("track() should return a deep copy with identical values but different reference")
    void testTrackReturnsDeepCopy() {
        Track result = audioStreamer.track();

        assertNotSame(testTrack, result, "Should return a different instance for thread safety");
        assertEquals(testTrack, result, "The copy should have the same values as the original track");
        assertSame(testTrack.metadata(), result.metadata(),
                "Metadata is immutable and should not be copied for memory efficiency.");
        assertEquals(testTrack.playCount(), result.playCount(),
                "The play count value should be preserved in the copy");
    }


    @Test
    @DisplayName("endStream() should wrap IOException into InternalSystemException")
    void testStopStreamThrowsInternalExceptionOnIO() throws IOException {
        doThrow(IOException.class).when(senderMock).sendResponse(any());

        assertThrows(InternalSystemException.class,
                () -> audioStreamer.endStream(),
                "Expected endStream to throw InternalSystemException when network fails");
    }


    private class SynchronousExecutorStub extends AbstractExecutorService {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return true;
        }

        @Override
        public boolean isTerminated() {
            return true;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}