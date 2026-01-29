package bg.sofia.uni.fmi.mjt.spotify.server.business;

import java.util.Map;

public record UserEntityWrapper(Map<String, UserEntity> usersByEmail) {}