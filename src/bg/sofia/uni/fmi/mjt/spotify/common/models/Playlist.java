package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.AmbiguousSourceException;
import bg.sofia.uni.fmi.mjt.spotify.common.exceptions.ValidationException;

public class Playlist implements Serializable {
    private String name;
    private String ownerEmail;
    private Set<String> trackIds;

    public Playlist(String name, String creatorEmail) {
        this.name = name;
        this.ownerEmail = creatorEmail;
        this.trackIds = new HashSet<>();
    }

    public String name() {
        return name;
    }

    public String ownerEmail() {
        return ownerEmail;
    }

    public List<String> trackIds() {
        return List.copyOf(trackIds);
    }

    public void setName(String name) {
        if (name != null && !this.name.equals(name)) {
            this.name = name;
        }
    }

    public void addTrack(String trackId) {
        if (trackId == null) {
            throw new ValidationException("trackID should not be null");
        }

        if (!trackIds.add(trackId)) {
            throw new AmbiguousSourceException(
                    "Track with ID " + trackId + "already present in playlist " + name);
        }
        
        this.trackIds.add(trackId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Playlist playlist = (Playlist) o;
        return Objects.equals(ownerEmail, playlist.ownerEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerEmail);
    }
}