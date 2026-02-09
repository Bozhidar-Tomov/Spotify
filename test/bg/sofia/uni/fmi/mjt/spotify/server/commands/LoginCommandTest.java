package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AuthenticationException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class LoginCommandTest {

    private LoginCommand command;

    @Mock
    private SpotifySystem systemMock;

    @Mock
    private ResponseSender clientMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new LoginCommand();
        when(systemMock.isRunning()).thenReturn(true);
    }

    @Test
    void testExecuteInvalidArgsCount() {
        Response response = command.execute(List.of("email"), systemMock, clientMock);
        assertEquals(400, response.statusCode());
        assertEquals("Usage: login <email> <password>", response.message());
    }

    @Test
    void testExecuteSystemNotRunning() {
        when(systemMock.isRunning()).thenReturn(false);
        Response response = command.execute(List.of("email", "pass"), systemMock, clientMock);
        assertEquals(500, response.statusCode());
    }

    @Test
    void testExecuteSuccess() throws Exception {
        UserDTO user = new UserDTO("email", Set.of());
        when(systemMock.login("email", "pass", clientMock)).thenReturn(user);

        Response response = command.execute(List.of("email", "pass"), systemMock, clientMock);

        assertEquals(200, response.statusCode());
        assertEquals("Successful login.", response.message());
    }

    @Test
    void testExecuteAuthenticationException() throws Exception {
        when(systemMock.login("email", "pass", clientMock)).thenThrow(new AuthenticationException("Invalid"));
        Response response = command.execute(List.of("email", "pass"), systemMock, clientMock);
        assertEquals(401, response.statusCode());
        assertEquals("Login failed: Invalid", response.message());
    }
}
