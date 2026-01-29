package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Playlist implements Serializable{
    private String name;
    private String ownerEmail; // acts as ID
    private List<String> songIds;

    public Playlist(String name, String creatorEmail) {
        this.name = name;
        this.ownerEmail = creatorEmail;
        this.songIds = new ArrayList<>();
    }
        
    public String name() {
        return name;
    }
    
    public String ownerEmail() {
        return ownerEmail;
    }
        
    public List<String> songIds() {
        return songIds;
    }
        
    public void setName(String name) {
        if (name != null && !this.name.equals(name)) {
            this.name = name;
        }
    }

    public void addSong(String songId) {
        //TODO: check if song with that id exists
        if (songId == null) {
            throw new IllegalArgumentException("SongID should not be null");
        }
        this.songIds.add(songId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        
        if (o == null || getClass() != o.getClass()) return false;
        
        Playlist playlist = (Playlist) o;
        return Objects.equals(ownerEmail, playlist.ownerEmail);
    }
}