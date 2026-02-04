package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Playlist implements Serializable {
    private String name;
    private String ownerEmail;
    private List<String> trackIds;

    public Playlist(String name, String creatorEmail) {
        this.name = name;
        this.ownerEmail = creatorEmail;
        this.trackIds = new ArrayList<>();
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
        // TODO: check if track with that id exists
        if (trackId == null) {
            throw new IllegalArgumentException("trackID should not be null");
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