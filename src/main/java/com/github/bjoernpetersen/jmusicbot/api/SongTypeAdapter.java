package com.github.bjoernpetersen.jmusicbot.api;

import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.provider.NoSuchSongException;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

class SongTypeAdapter extends TypeAdapter<Song> {

  @Nonnull
  private final Function<String, Provider> providerLookup;

  SongTypeAdapter(@Nonnull Function<String, Provider> providerLookup) {
    this.providerLookup = providerLookup;
  }

  @Nonnull
  private static final Logger log = Logger.getLogger(SongTypeAdapter.class.getName());

  @Override
  public void write(JsonWriter writer, Song song) throws IOException {
    writer.beginObject()
      .name("songId").value(song.getId())
      .name("title").value(song.getTitle())
      .name("description").value(song.getDescription())
      .name("provider").value(song.getProviderName())
      .endObject();
  }

  @Override
  public Song read(JsonReader reader) throws IOException {
    reader.beginObject();
    String id = null;
    String providerName = null;
    for (int i = 0; i < 4; i++) {
      String name = reader.nextName();
      switch (name) {
        case "songId":
          id = reader.nextString();
          break;
        case "provider":
          providerName = reader.nextString();
          break;
        default:
          reader.nextString();
      }
    }

    if (id == null || providerName == null) {
      log.severe("Could not deserialize Song JSON!");
      return null;
    }

    Provider provider = providerLookup.apply(providerName);
    if (provider == null) {
      log.severe(String.format(
        "Could not find Provider '%s' to deserialize Song JSON!", providerName
      ));
      return null;
    }

    Song result;
    try {
      result = provider.lookup(id);
    } catch (NoSuchSongException e) {
      log.severe(String.format(
        "Song with ID '%s' could not be found by provider '%s'!", id, providerName
      ));
      return null;
    }

    reader.endObject();
    return result;
  }
}
