package bg.sofia.uni.fmi.mjt.spotify.server.models;

import java.io.Serializable;

public final record Password(byte[] hash, byte[] salt, int iterations) implements Serializable {
}
