package com.github.bjoernpetersen.jmusicbot.api;

import static spark.Spark.get;
import static spark.Spark.path;

import com.github.bjoernpetersen.jmusicbot.NamedPlugin;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import spark.Request;
import spark.Response;

class RestSuggesters {

  @Nonnull
  private static final Logger log = Logger.getLogger(RestSuggesters.class.getName());

  @Nonnull
  private static final String SUGGESTER_ID_PARAM = "suggesterId";
  @Nonnull
  private static final String MAX_LENGTH_PARAM = "maxLength";

  @Nonnull
  private final Map<String, Suggester> suggesters;
  @Nonnull
  private final GsonTransformer transformer;

  RestSuggesters(@Nonnull GsonTransformer transformer, @Nonnull Map<String, Suggester> suggesters) {
    this.transformer = transformer;
    this.suggesters = suggesters;

    path("/suggesters", () -> {
      get("", this::getSuggesters, transformer);
      path("/:suggesterId", () -> {
        get("/nextSuggestions", this::getSuggestions, transformer);
      });
    });
  }

  private Collection<String> getSuggesters(Request request, Response response) throws Exception {
    String providerId = request.queryParams("providerId");
    if (providerId == null || providerId.isEmpty()) {
      return suggesters.keySet();
    } else {
      return suggesters.values().stream()
          .filter(s -> s.getDependencies().contains(providerId))
          .map(NamedPlugin::getName)
          .collect(Collectors.toList());
    }
  }

  private Object getSuggestions(Request request, Response response) throws Exception {
    String suggesterId = request.params(SUGGESTER_ID_PARAM);
    Suggester suggester = suggesters.get(suggesterId);

    if (suggester == null) {
      response.status(404);
      return "No such suggester";
    }

    int maxLength = 20;
    String maxLengthParam = request.queryParams(MAX_LENGTH_PARAM);
    if (maxLengthParam != null && !maxLengthParam.isEmpty()) {
      try {
        maxLength = Integer.parseUnsignedInt(maxLengthParam);
      } catch (NumberFormatException e) {
        log.fine("Requested invalid max length " + maxLengthParam);
      }

      if (maxLength == 0) {
        log.fine("Requested 0 max length");
        maxLength = 1;
      }

      if (maxLength > 100) {
        log.fine("Requested too big max length " + maxLengthParam);
        maxLength = 100;
      }
    }

    return suggester.getNextSuggestions(maxLength);
  }
}
