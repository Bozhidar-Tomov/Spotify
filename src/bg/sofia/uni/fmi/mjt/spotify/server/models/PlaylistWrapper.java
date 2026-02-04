package bg.sofia.uni.fmi.mjt.spotify.server.models;

import bg.sofia.uni.fmi.mjt.spotify.common.models.Playlist;
import java.util.List;
import java.util.Map;

public record PlaylistWrapper(Map<String, List<Playlist>> data) implements DataWrapper<List<Playlist>> {
}
