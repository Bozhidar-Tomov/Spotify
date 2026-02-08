package bg.sofia.uni.fmi.mjt.spotify.server.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

import bg.sofia.uni.fmi.mjt.spotify.server.models.Password;

public class PasswordHandlerTest {
    private static final String VALID_PASSWORD = "valid_pass";
    private static final String WRONG_PASSWORD = "wrong_pass";

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<PasswordHandler> constructor = PasswordHandler.class.getDeclaredConstructor();
        
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), 
            "Constructor should be private to prevent instantiation");
    }

    @Test
    void testConstructorThrowsExceptionWhenCalled() throws NoSuchMethodException {
        Constructor<PasswordHandler> constructor = PasswordHandler.class.getDeclaredConstructor();

        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                constructor::newInstance, "Constructor should throw an exception");

        assertTrue(exception.getCause() instanceof IllegalStateException, "Should throw IllegalStateException");
    }
    
    @Test
    void testHashPasswordCreatesUniqueSalts() throws NoSuchAlgorithmException {
        Password p1 = PasswordHandler.hashPassword(VALID_PASSWORD);
        Password p2 = PasswordHandler.hashPassword(VALID_PASSWORD);

        assertNotEquals(p1.salt(), p2.salt(), "Salts should be unique for every hash call.");
        assertNotEquals(p1.hash(), p2.hash(), "Hashes should be different for the same password.");
    }

    @Test
    void testHashPasswordMetadata() throws NoSuchAlgorithmException {
        Password p = PasswordHandler.hashPassword(VALID_PASSWORD);

        assertNotNull(p.hash(), "Hash should not be null");
        assertNotNull(p.salt(), "Salt should not be null");
        assertEquals(32, p.salt().length, "Salt length should be 32 bytes");
        assertEquals(4096, p.iterations(), "Iteration count should match the internal constant");
    }

    @Test
    void testVerifyPasswordWithCorrectPassword() throws NoSuchAlgorithmException {
        Password storedPassword = PasswordHandler.hashPassword(VALID_PASSWORD);

        assertTrue(PasswordHandler.verifyPassword(VALID_PASSWORD, storedPassword),
                "Verification should return true for the correct password");
    }
    
    @Test
    void testVerifyPasswordWithWrongPassword() throws NoSuchAlgorithmException {
        Password storedPassword = PasswordHandler.hashPassword(VALID_PASSWORD);
        
        assertFalse(PasswordHandler.verifyPassword(WRONG_PASSWORD, storedPassword), 
            "Verification should return false for an incorrect password");
    }

    @Test
    void testVerifyPasswordWithEmptyString() throws NoSuchAlgorithmException {
        Password storedPassword = PasswordHandler.hashPassword("");

        assertTrue(PasswordHandler.verifyPassword("", storedPassword),
                "Should be able to hash and verify an empty string");
    }
    
    @Test
    void testVerifyPasswordThrowsExceptionOnNullInput() {
        assertThrows(NullPointerException.class, () -> {
            PasswordHandler.verifyPassword(null, null);
        }, "Should throw NullPointerException when inputs are null");
    }    
}
