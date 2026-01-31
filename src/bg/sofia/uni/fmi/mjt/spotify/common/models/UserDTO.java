package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

public record UserDTO(String email, Set<String> playlistIds) implements Serializable {
    public UserDTO {
        playlistIds = Collections.unmodifiableSet(playlistIds);
    }
}