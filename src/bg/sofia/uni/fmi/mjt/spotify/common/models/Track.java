package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.io.Serializable;

public class Track implements Serializable{
    private final SongMetadata metadata;
    private long playCount;

    public Track(SongMetadata metadata) {
        this.metadata = metadata;
        this.playCount = 0;
    }

    public String id() {
        return metadata.id();
    }
    public String title() {
        return metadata.title();
    }

    public String artist() {
        return metadata.artist();
    }

    public long playCount() {
        return playCount;
    }

    public void incrementPlayCount() {
        playCount += 1;
    }
    
    public void incrementPlayCount(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        playCount += amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Track track))
            return false;
        return this.metadata.id().equals(track.metadata.id());
    }

    @Override
    public int hashCode() {
        return metadata.id().hashCode();
    }
}
