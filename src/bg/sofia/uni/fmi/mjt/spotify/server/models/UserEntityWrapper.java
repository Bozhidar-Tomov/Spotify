package bg.sofia.uni.fmi.mjt.spotify.server.models;

import java.util.Map;

public record UserEntityWrapper(Map<String, UserEntity> data) implements DataWrapper<UserEntity> {
}
