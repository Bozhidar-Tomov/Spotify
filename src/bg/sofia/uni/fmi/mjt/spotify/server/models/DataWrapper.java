package bg.sofia.uni.fmi.mjt.spotify.server.models;

import java.io.Serializable;
import java.util.Map;

public interface DataWrapper<V> extends Serializable {
    Map<String, V> data();
}
