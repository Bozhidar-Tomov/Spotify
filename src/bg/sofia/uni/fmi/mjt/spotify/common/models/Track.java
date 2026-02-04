package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class Track implements Serializable{
    private final SongMetadata metadata;
    private AtomicLong playCount;

    public Track(SongMetadata metadata) {
        this.metadata = metadata;
        this.playCount = new AtomicLong(0);
    }

    public SongMetadata metadata() {
        return this.metadata;
    }

    public AtomicLong playCount() {
        return playCount;
    }

    public void incrementPlayCount() {
        playCount.getAndIncrement();
    }
    
    public void incrementPlayCount(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        playCount.getAndAdd(amount);
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
