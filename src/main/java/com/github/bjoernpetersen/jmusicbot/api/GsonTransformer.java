package com.github.bjoernpetersen.jmusicbot.api;

import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.function.Function;
import javax.annotation.Nonnull;
import spark.ResponseTransformer;

class GsonTransformer implements ResponseTransformer {

  @Nonnull
  private final Gson gson;

  public GsonTransformer(Function<String, Provider> songLookup) {
    this.gson = new GsonBuilder()
      .registerTypeAdapter(Song.class, new SongTypeAdapter(songLookup))
      .create();
  }

  @Override
  public String render(Object o) throws Exception {
    return gson.toJson(o);
  }

  public <T> T deserialize(String json, Class<T> type) {
    return gson.fromJson(json, type);
  }
}
