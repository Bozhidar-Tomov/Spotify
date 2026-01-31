package bg.sofia.uni.fmi.mjt.spotify.server.business;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordHandler {
    private static final int ITERATIONS = 65536;

    public static Password hashPassword(String password) throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[32];
        random.nextBytes(salt);

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt);

        byte[] hash = md.digest(password.getBytes());

        for (int i = 0; i < ITERATIONS; i++) {
            hash = md.digest(hash);
        }
        return new Password(hash, salt, ITERATIONS);
    }
    
    public static boolean verifyPassword(String inputPassword, Password storedPassword)
            throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-512");

        md.update(storedPassword.salt());
        byte[] hash = md.digest(inputPassword.getBytes());

        for (int i = 0; i < storedPassword.iterations(); i++) {
            hash = md.digest(hash);
        }

        return MessageDigest.isEqual(hash, storedPassword.hash());
    }
}
