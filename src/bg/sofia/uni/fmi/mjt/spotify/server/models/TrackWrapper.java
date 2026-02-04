package bg.sofia.uni.fmi.mjt.spotify.server.models;

import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import java.util.List;
import java.util.Map;

public record TrackWrapper(Map<String, List<Track>> data) implements DataWrapper<List<Track>> {
}
