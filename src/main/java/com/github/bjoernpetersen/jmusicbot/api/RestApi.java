package com.github.bjoernpetersen.jmusicbot.api;

import com.github.bjoernpetersen.jmusicbot.MusicBot;
import com.github.bjoernpetersen.jmusicbot.ProviderManager;
import java.io.Closeable;
import java.io.IOException;
import spark.Spark;

public class RestApi implements Closeable {

  private final MusicBot bot;
  private final GsonTransformer transformer;

  public RestApi(MusicBot bot) {
    this.bot = bot;
    ProviderManager providerManager = bot.getProviderManager();
    this.transformer = new GsonTransformer(providerManager::getProvider);
    new RestPlayer(transformer, bot.getPlayer(), bot.getProviderManager());
    new RestProviders(transformer, providerManager.getProviders());
    new RestSuggesters(transformer, providerManager.getSuggesters());
  }

  @Override
  public void close() throws IOException {
    Spark.stop();
  }
}
