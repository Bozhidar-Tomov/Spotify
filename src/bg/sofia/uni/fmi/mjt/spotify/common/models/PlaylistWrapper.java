package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.util.Map;

public record PlaylistWrapper(Map<String, Playlist> playlistsByEmail) {}
