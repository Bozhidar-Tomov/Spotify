package bg.sofia.uni.fmi.mjt.spotify.server.business;

import bg.sofia.uni.fmi.mjt.spotify.server.models.Password;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordHandler {
    private static final int ITERATIONS = 4096;

    public static Password hashPassword(String password) throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[32];
        random.nextBytes(salt);

        byte[] hash = generateHash(password, salt, ITERATIONS);
        
        return new Password(hash, salt, ITERATIONS);
    }
    
    public static boolean verifyPassword(String inputPassword, Password storedPassword)
            throws NoSuchAlgorithmException {

        byte[] testHash = generateHash(inputPassword, storedPassword.salt(), storedPassword.iterations());

        return MessageDigest.isEqual(testHash, storedPassword.hash());
    }

    private static byte[] generateHash(String password, byte[] salt, int iterations) 
            throws NoSuchAlgorithmException {
        
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt);
        
        byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));

        for (int i = 0; i < iterations; i++) {
            hash = md.digest(hash);
        }
        return hash;
    }
}
