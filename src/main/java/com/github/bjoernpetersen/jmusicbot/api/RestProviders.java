package com.github.bjoernpetersen.jmusicbot.api;

import static spark.Spark.get;
import static spark.Spark.path;

import com.github.bjoernpetersen.jmusicbot.provider.NoSuchSongException;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import spark.Request;
import spark.Response;

class RestProviders {

  private static final String PROVIDER_ID_PARAM = "providerId";

  @Nonnull
  private final Map<String, Provider> providers;

  RestProviders(@Nonnull GsonTransformer transformer, @Nonnull Map<String, Provider> providers) {
    this.providers = providers;

    path("/providers", () -> {
      get("", this::getProviders, transformer);
      get("/:" + PROVIDER_ID_PARAM + "/search", this::searchSong, transformer);
      get("/:" + PROVIDER_ID_PARAM + "/lookup", this::lookupSong, transformer);
    });
  }

  private Collection<String> getProviders(Request request, Response response) throws Exception {
    return providers.keySet();
  }

  private Object searchSong(Request request, Response response) throws Exception {
    String id = request.params(PROVIDER_ID_PARAM);
    if (id == null) {
      response.status(400);
      return "Missing provider ID";
    }

    Provider provider = providers.get(id);
    if (provider == null) {
      response.status(404);
      return "No such provider.";
    }

    String query = request.queryParams("query");
    if (query == null) {
      response.status(400);
      return "Missing 'query' query param";
    }

    return provider.search(query);
  }

  private Object lookupSong(Request request, Response response) throws Exception {
    String id = request.params(PROVIDER_ID_PARAM);
    if (id == null) {
      response.status(400);
      return "Missing provider ID";
    }

    Provider provider = providers.get(id);
    if (provider == null) {
      response.status(404);
      return "No such provider.";
    }

    String songId = request.queryParams("songId");
    if (songId == null) {
      response.status(400);
      return "Missing songId query param";
    }

    try {
      return provider.lookup(songId);
    } catch (NoSuchSongException e) {
      response.status(404);
      return "No such song";
    }
  }
}
