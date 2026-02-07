package bg.sofia.uni.fmi.mjt.spotify.server.models;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.SourceAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;

public class UserEntity implements Serializable {
    private final String email;
    private final Password password;
    private final Set<String> playlistNames;

    public UserEntity(String email, Password password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("User email or password cannot be null.");
        }

        this.email = email;
        this.password = password;
        this.playlistNames = new HashSet<>();
    }

    public String email() {
        return email;
    }

    public Password password() {
        return password;
    }

    public Set<String> playlistIds() {
        return Collections.unmodifiableSet(playlistNames);
    }

    public void addPlaylist(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Playlist ID cannot be null or empty");
        }
        if (!playlistNames.add(name)) {
            throw new SourceAlreadyExistsException("Playlist '" + name + "' already exists.");
        }
    }

    public UserDTO toDTO() {
        return new UserDTO(email, playlistNames);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserEntity that = (UserEntity) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
