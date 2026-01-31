package bg.sofia.uni.fmi.mjt.spotify.server.business;

import bg.sofia.uni.fmi.mjt.spotify.common.UserDTO;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class UserEntity implements Serializable {
    private final String email;
    private final Password password;
    private final Set<String> playlistIds;
    
    private UserEntity(String email, Password password, Set<String> playlistIds) {
        this.email = email;
        this.password = password;
        this.playlistIds = new HashSet<>(playlistIds);
    }

    public UserEntity(String email, Password password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("User email or password cannot be null.");
        }

        this.email = email;
        this.password = password;
        this.playlistIds = new HashSet<>();
    }

    public String email() {
        return email;
    }
    
    public Password password() {
        return password;
    }

    public Set<String> playlistIds() {
        return Collections.unmodifiableSet(playlistIds);
    }

    public void addPlaylist(String id) {
        //TODO: system should check if playlist with that id exists
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Playlist ID cannot be null or empty");
        }
        this.playlistIds.add(id);
    }
    
    public UserDTO toDTO() {
        return new UserDTO(email, playlistIds);
    }

    public static UserEntity fromDTO(UserDTO dto, Password password) {
        return new UserEntity(dto.email(), password, dto.playlistIds());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
