package bg.sofia.uni.fmi.mjt.spotify.server.models;

import bg.sofia.uni.fmi.mjt.spotify.common.models.Playlist;
import java.util.Map;
import java.util.Set;

public record PlaylistWrapper(Map<String, Set<Playlist>> data) implements DataWrapper<Set<Playlist>> {
}
