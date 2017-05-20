package com.github.bjoernpetersen.jmusicbot.api;

import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.put;

import com.github.bjoernpetersen.jmusicbot.ProviderManager;
import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.playback.Player;
import com.github.bjoernpetersen.jmusicbot.provider.NoSuchSongException;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import spark.Request;
import spark.Response;

class RestPlayer {

  @Nonnull
  private static final Logger log = Logger.getLogger(RestPlayer.class.getName());

  @Nonnull
  private final GsonTransformer transformer;
  @Nonnull
  private final Player player;
  @Nonnull
  private final ProviderManager providerManager;

  public RestPlayer(@Nonnull GsonTransformer transformer, @Nonnull Player player,
      @Nonnull ProviderManager providerManager) {
    this.transformer = transformer;
    this.player = player;
    this.providerManager = providerManager;
    path("/player", () -> {
      get("", this::getState);
      get("/pause", this::pause);
      get("/play", this::play);
      path("/queue", () -> {
        get("", this::getQueue, transformer);
        put("/add", this::addToQueue, transformer);
        put("/remove", this::removeFromQueue, transformer);
      });
    });
  }

  private Object getState(Request request, Response response) throws Exception {
    return player.getState().getState().toString();
  }

  private Object getQueue(Request request, Response response) throws Exception {
    return player.getQueue().toList();
  }

  private Object addToQueue(Request request, Response response) throws Exception {
    Song song = null;
    try {
      song = lookupSong(request);
    } catch (LookupException e) {
      response.status(e.getStatus());
      return e.getMessage();
    }
    player.getQueue().append(song);
    return player.getQueue().toList();
  }

  private Object removeFromQueue(Request request, Response response) throws Exception {
    Song song = null;
    try {
      song = lookupSong(request);
    } catch (LookupException e) {
      response.status(e.getStatus());
      return e.getMessage();
    }
    player.getQueue().remove(song);
    return player.getQueue().toList();
  }

  private Song lookupSong(Request request) throws LookupException {
    String songId = request.queryParams("songId");
    if (songId == null) {
      throw LookupException.badRequest("songId");
    }

    String providerName = request.queryParams("providerName");
    if (providerName == null) {
      throw LookupException.badRequest("providerName");
    }

    Provider provider;
    try {
      provider = providerManager.getProvider(providerName);
    } catch (IllegalArgumentException e) {
      throw LookupException.notFound(providerName, songId);
    }

    try {
      return provider.lookup(songId);
    } catch (NoSuchSongException e) {
      log.warning(String.format(
          "Could not find song with ID '%s' with provider '%s'",
          songId,
          providerName
      ));
      throw LookupException.notFound(providerName, songId);
    }
  }

  private Object pause(Request request, Response response) {
    player.pause();
    response.status(204);
    return null;
  }

  private Object play(Request request, Response response) {
    player.play();
    response.status(204);
    return null;
  }
}
