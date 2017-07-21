package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.playback.Playback;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Song {

  @Nonnull
  private final PlaybackSupplier playbackSupplier;
  @Nonnull
  private final SongLoader loader;
  @Nonnull
  private final Provider provider;

  @Nonnull
  private final String id;
  @Nonnull
  private final String title;
  @Nonnull
  private final String description;
  @Nonnull
  private final int duration;
  @Nullable
  private final String albumArtUrl;


  private Song(@Nonnull PlaybackSupplier playbackSupplier, @Nonnull SongLoader loader,
      @Nonnull Provider provider, @Nonnull String id, @Nonnull String title,
      @Nonnull String description, int duration, @Nullable String albumArtUrl) {
    this.playbackSupplier = playbackSupplier;
    this.loader = loader;
    this.provider = provider;

    this.id = id;
    this.title = title;
    this.description = description;
    this.duration = duration;
    this.albumArtUrl = albumArtUrl;
  }

  /**
   * Schedules this song for loading. Actual loading will be done asynchronously (see {@link
   * #hasLoaded()}).
   */
  public void load() {
    SongLoaderExecutor.getInstance().execute(this);
  }

  /**
   * <p>Whether this specified song has been successfully loaded.</p>
   *
   * <p>This method will block until the loading is done.</p>
   *
   * @return whether loading was successful
   * @throws InterruptedException if the thread is interrupted while waiting for the song to load
   * @throws IllegalStateException if the song is not scheduled for loading (call {@link #load()}
   * before
   */
  public boolean hasLoaded() throws InterruptedException {
    return SongLoaderExecutor.getInstance().hasLoaded(this);
  }

  SongLoader getLoader() {
    return loader;
  }

  /**
   * Gets a Playback object for this Song.
   * A call to this method indicates that the song will actually be played.
   *
   * @return a Playback object
   * @throws IOException if no Playback can be created
   */
  @Nonnull
  public Playback getPlayback() throws IOException {
    load();
    try {
      if (!hasLoaded()) {
        throw new IOException("Loading of song unsuccessful: " + this);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
    return playbackSupplier.supply(this);
  }

  @Nonnull
  public Optional<String> getAlbumArtUrl() {
    return Optional.ofNullable(albumArtUrl);
  }

  @Nonnull
  public String getId() {
    return id;
  }

  @Nonnull
  public String getTitle() {
    return title;
  }

  @Nonnull
  public String getDescription() {
    return description;
  }

  public int getDuration() {
    return duration;
  }

  @Nonnull
  public Provider getProvider() {
    return provider;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Song song = (Song) o;

    return id.equals(song.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return "Song{"
        + "id='" + id + '\''
        + ", providerName='" + getProvider().getId() + '\''
        + '}';
  }

  public static final class Builder {

    private PlaybackSupplier playbackSupplier;
    private SongLoader songLoader;
    private Provider provider;

    private String id;
    private String title;
    private String description;
    private int duration = 0;

    private String albumArtUrl;

    @Nonnull
    public Builder playbackSupplier(@Nonnull PlaybackSupplier playbackSupplier) {
      this.playbackSupplier = Objects.requireNonNull(playbackSupplier);
      return this;
    }

    @Nonnull
    public Builder songLoader(@Nonnull SongLoader songLoader) {
      this.songLoader = Objects.requireNonNull(songLoader);
      return this;
    }

    @Nonnull
    public Builder provider(@Nonnull Provider provider) {
      this.provider = Objects.requireNonNull(provider);
      return this;
    }

    @Nonnull
    public Builder id(@Nonnull String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    @Nonnull
    public Builder title(@Nonnull String title) {
      this.title = Objects.requireNonNull(title);
      return this;
    }

    @Nonnull
    public Builder description(@Nonnull String description) {
      this.description = Objects.requireNonNull(description);
      return this;
    }

    @Nonnull
    public Builder duration(int duration) {
      if (duration < 0) {
        throw new IllegalArgumentException();
      }
      this.duration = duration;
      return this;
    }

    @Nonnull
    public Builder albumArtUrl(@Nullable String albumArtUrl) {
      this.albumArtUrl = albumArtUrl;
      return this;
    }

    @Nonnull
    public Song build() {
      if (playbackSupplier == null
          || songLoader == null
          || provider == null
          || id == null
          || title == null
          || description == null) {
        throw new IllegalStateException("Not all values specified.");
      }
      return new Song(
          playbackSupplier,
          songLoader,
          provider,
          id,
          title,
          description,
          duration,
          albumArtUrl
      );
    }

  }
}
