package bg.sofia.uni.fmi.mjt.spotify.common.net;

import bg.sofia.uni.fmi.mjt.spotify.common.models.UserDTO;

public record UserDtoPayload(UserDTO data) implements Payload {
}