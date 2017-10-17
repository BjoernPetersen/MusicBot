package com.github.bjoernpetersen.jmusicbot.provider;

import com.github.bjoernpetersen.jmusicbot.IdPlugin;
import com.github.bjoernpetersen.jmusicbot.InitStateWriter;
import com.github.bjoernpetersen.jmusicbot.InitializationException;
import com.github.bjoernpetersen.jmusicbot.PlaybackFactoryManager;
import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

public interface Provider extends IdPlugin {

  Set<Class<? extends PlaybackFactory>> getPlaybackDependencies();

  /**
   * Gets the base class for this provider. The base class should define relevant methods for {@link
   * Suggester} implementations depending on this provider.
   *
   * This provider must implement the returned base class.
   *
   * @return a class
   */
  @Nonnull
  Class<? extends Provider> getBaseClass();

  /**
   * Signals to the provider that it can start doing work.
   *
   * @param manager a PlaybackFactoryManager to obtain PlaybackFactory instances from
   * @param initStateWriter a writer for initialization state messages
   * @throws InitializationException If any errors occur (for example due to invalid log-in
   * credentials)
   * @throws InterruptedException if the thread is interrupted while initializing
   */
  void initialize(@Nonnull InitStateWriter initStateWriter, @Nonnull PlaybackFactoryManager manager)
      throws InitializationException, InterruptedException;

  /**
   * Searches for songs based on the given search query.
   *
   * It is recommended only to return about 30 songs.
   *
   * @param query a search query, trimmed and not empty
   * @return a list of songs
   */
  @Nonnull
  List<Song> search(@Nonnull String query);

  /**
   * Looks up a song by its ID.
   *
   * @param id the song ID
   * @return the song with the specified ID
   * @throws NoSuchSongException if the ID is invalid
   */
  @Nonnull
  Song lookup(@Nonnull String id) throws NoSuchSongException;

  /**
   * Looks up a batch of song IDs. If any can't be looked up, they will be dropped.
   *
   * @param ids a list of song IDs
   * @return a list of songs
   */
  @Nonnull
  default List<Song> lookupBatch(@Nonnull List<String> ids) {
    List<Song> result = new ArrayList<>(ids.size());
    for (String id : ids) {
      try {
        result.add(lookup(id));
      } catch (NoSuchSongException e) {
        // TODO log or something
      }
    }
    return Collections.unmodifiableList(result);
  }
}
