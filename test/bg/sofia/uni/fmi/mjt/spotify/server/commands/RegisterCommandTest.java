package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegisterCommandTest {

    private RegisterCommand command;

    @Mock
    private SpotifySystem systemMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new RegisterCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of("email"), systemMock);
        assertEquals(BAD_REQUEST, response.statusCode());
        assertEquals("Usage: register <email> <password>", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of("email", "pass"), systemMock);
        assertEquals(INTERNAL_SERVER_ERROR, response.statusCode());
    }

    @Test
    void testExecuteSuccess() throws Exception {
        Response response = command.execute(List.of("email", "pass"), systemMock);
        verify(systemMock).registerUser("email", "pass");
        assertEquals(OK, response.statusCode());
        assertEquals("Successful register. Now you can login", response.message());
    }

    @Test
    void testExecuteValidationException() throws Exception {
        doThrow(new ValidationException("Error")).when(systemMock).registerUser("email", "pass");
        Response response = command.execute(List.of("email", "pass"), systemMock);
        assertEquals(UNAUTHORIZED, response.statusCode());
        assertEquals("Register failed: Error", response.message());
    }
}
