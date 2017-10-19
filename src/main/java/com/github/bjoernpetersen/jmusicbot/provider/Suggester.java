package com.github.bjoernpetersen.jmusicbot.provider;

import com.github.bjoernpetersen.jmusicbot.IdPlugin;
import com.github.bjoernpetersen.jmusicbot.InitStateWriter;
import com.github.bjoernpetersen.jmusicbot.InitializationException;
import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.playback.SongEntry;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

public interface Suggester extends IdPlugin {

  /**
   * <p>Suggest the next song to play.</p>
   *
   * <p>When a song has been returned by this method, it can be assumed it's going to be played.
   * This means it should be removed from the 'next suggestions' list.</p>
   *
   * @return a song to play
   * @throws BrokenSuggesterException if the suggester can't suggest anything
   */
  @Nonnull
  Song suggestNext() throws BrokenSuggesterException;

  /**
   * <p>Gets a list of next suggestions which will be returned by calling {@link
   * #suggestNext()}.</p>
   *
   * <p>The returned list must contain at least one element.</p>
   *
   * @param maxLength the maximum length of the returned list
   * @return a list of next suggestions
   * @throws BrokenSuggesterException if the suggester can't suggest anything
   */
  @Nonnull
  List<Song> getNextSuggestions(int maxLength) throws BrokenSuggesterException;

  /**
   * <p>Notifies this Suggester that the specified song entry has been played.</p>
   *
   * <p>It is recommended that the song will be removed from the next suggestions when this method
   * is called.</p>
   *
   * <p>It is guaranteed that the song comes from a provider this suggester depends on.</p>
   *
   * <p>The default implementation calls {@link #removeSuggestion(Song)}.</p>
   *
   * @param songEntry a SongEntry
   */
  default void notifyPlayed(@Nonnull SongEntry songEntry) {
    removeSuggestion(songEntry.getSong());
  }

  /**
   * <p>Removes the specified song from the {@link #getNextSuggestions(int) suggestions}.</p>
   *
   * @param song a song
   */
  void removeSuggestion(@Nonnull Song song);

  /**
   * <p>Indicates a user disliking the specified song.</p>
   *
   * The default implementation calls {@link #removeSuggestion(Song)}.
   *
   * @param song a song
   */
  default void dislike(@Nonnull Song song) {
    removeSuggestion(song);
  }

  /**
   * <p>Signals to the provider that it can start doing work.</p>
   *
   * <p>The Suggester should remain operational until {@link #close()} is called.</p>
   *
   * <p>The dependency Map is guaranteed to contain all dependencies requested by {@link
   * #getDependencies()}. It will also contain all satisfiable dependencies that were requested by
   * {@link #getOptionalDependencies()}.</p>
   *
   * @param dependencies the Provider instances requested by {@link #getDependencies()} and {@link
   * #getOptionalDependencies()}
   * @param initStateWriter a writer for initialization state messages
   * @throws InitializationException If any errors occur (for example due to invalid log-in
   * credentials)
   * @throws InterruptedException if the thread is interrupted while initializing
   */
  void initialize(@Nonnull InitStateWriter initStateWriter,
      @Nonnull DependencyMap<Provider> dependencies)
      throws InitializationException, InterruptedException;

  /**
   * <p>Gets a set of providers this Suggester depends on.</p>
   *
   * The default implementation returns an empty set.
   *
   * @return a set of provider base classes
   */
  default Set<Class<? extends Provider>> getDependencies() {
    return Collections.emptySet();
  }

  /**
   * <p>Gets a set of providers this Suggester may use, but doesn't need.</p>
   *
   * The default implementation returns an empty set.
   *
   * @return a set of provider base classes
   */
  default Set<Class<? extends Provider>> getOptionalDependencies() {
    return Collections.emptySet();
  }
}
