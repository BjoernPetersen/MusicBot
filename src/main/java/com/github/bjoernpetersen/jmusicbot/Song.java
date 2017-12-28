package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.playback.Playback;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Song implements Closeable{

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
  private final int duration;
  @Nullable
  private final String albumArtUrl;
  @Nullable
  private final CheckedConsumer<Song, IOException> onClose;
  private boolean closed;


  private Song(@Nonnull PlaybackSupplier playbackSupplier, @Nonnull SongLoader loader,
      @Nonnull Provider provider, @Nonnull String id, @Nonnull String title,
      @Nonnull String description, int duration, @Nullable String albumArtUrl,
      @Nullable CheckedConsumer<Song, IOException> onClose) {
    this.playbackSupplier = playbackSupplier;
    this.loader = loader;
    this.provider = provider;

    this.id = id;
    this.title = title;
    this.description = description;
    this.duration = duration;
    this.albumArtUrl = albumArtUrl;
    this.onClose = onClose;
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
   * Gets a Playback object for this Song. A call to this method indicates that the song will
   * actually be played.
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
  public boolean equals(Object o) {
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

  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() throws IOException {
    if(!closed){
      if(onClose != null) {
        onClose.accept(this);
      }
      closed = true;
    }
  }


  /**
   * A Builder for Song objects.<br>
   *
   * It is recommended to keep an instance of this class in your Provider with the following values
   * set: <ul> <li>{@link #provider(Provider)}</li> <li>{@link #playbackSupplier(PlaybackSupplier)}</li>
   * <li>{@link #songLoader(SongLoader)}</li> </ul>
   */
  public static final class Builder {

    private PlaybackSupplier playbackSupplier;
    private SongLoader songLoader;
    private Provider provider;

    private String id;
    private String title;
    private String description;
    private int duration = 0;

    private String albumArtUrl;
    private CheckedConsumer<Song, IOException> onClose;

    /**
     * Sets the PlaybackSupplier which will delegate to a PlaybackFactory to actually play the
     * song.
     *
     * @param playbackSupplier a PlaybackSupplier
     * @return this Builder
     */
    @Nonnull
    public Builder playbackSupplier(@Nonnull PlaybackSupplier playbackSupplier) {
      this.playbackSupplier = Objects.requireNonNull(playbackSupplier);
      return this;
    }

    /**
     * Sets a song loader to prepare the song for playing.
     *
     * If you don't need to prepare the song, use {@link SongLoader#DUMMY}.
     *
     * @param songLoader a song loader
     * @return this Builder
     */
    @Nonnull
    public Builder songLoader(@Nonnull SongLoader songLoader) {
      this.songLoader = Objects.requireNonNull(songLoader);
      return this;
    }

    /**
     * Sets the provider which created this song.
     *
     * @param provider a provider
     * @return this Builder
     */
    @Nonnull
    public Builder provider(@Nonnull Provider provider) {
      this.provider = Objects.requireNonNull(provider);
      return this;
    }

    /**
     * Sets the song ID. This ID should be unique for the provider.
     *
     * @param id an ID
     * @return this Builder
     */
    @Nonnull
    public Builder id(@Nonnull String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    /**
     * Sets the song title.
     *
     * If you can't separate the title from the artist, the full string goes here.
     *
     * @param title the title
     * @return this Builder
     */
    @Nonnull
    public Builder title(@Nonnull String title) {
      this.title = Objects.requireNonNull(title);
      return this;
    }

    /**
     * Sets the song description. Usually this should the the artist.
     *
     * @param description a description
     * @return this Builder
     */
    @Nonnull
    public Builder description(@Nonnull String description) {
      this.description = Objects.requireNonNull(description);
      return this;
    }

    /**
     * Sets the song duration in seconds. Optional with default value 0.
     *
     * @param duration the duration in seconds
     * @return this Builder
     * @throws IllegalArgumentException if duration is less than 0
     */
    @Nonnull
    public Builder duration(int duration) {
      if (duration < 0) {
        throw new IllegalArgumentException();
      }
      this.duration = duration;
      return this;
    }

    /**
     * Sets the URL to the album art. Optional.
     *
     * @param albumArtUrl a URL, or null
     * @return this Builder
     */
    @Nonnull
    public Builder albumArtUrl(@Nullable String albumArtUrl) {
      this.albumArtUrl = albumArtUrl;
      return this;
    }

    /**
     * Sets a consumer to process the the song on close
     *
     * @param onClose a Consumer or null
     * @return this builder
     */
    @Nonnull
    public Builder onClose(@Nullable CheckedConsumer<Song, IOException> onClose) {
      this.onClose = onClose;
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
          albumArtUrl,
          onClose
      );
    }

  }
}
