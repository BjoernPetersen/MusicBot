package com.github.bjoernpetersen.jmusicbot;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public final class PluginLoader<T> {

  private static final Logger log = Logger.getLogger(PluginLoader.class.getName());

  private static ClassLoader loader;

  @Nonnull
  private final File pluginFolder;
  @Nonnull
  private final Class<T> type;

  public PluginLoader(@Nonnull File pluginFolder, @Nonnull Class<T> type) {
    this.pluginFolder = pluginFolder;
    this.type = type;
  }

  @Nonnull
  public Collection<T> load() {
    if (!pluginFolder.isDirectory()) {
      if (!pluginFolder.exists()) {
        if (!pluginFolder.mkdirs()) {
          log.warning(String.format("Could not create plugin folder '%s'", pluginFolder.getPath()));
        }
      }
      return Collections.emptyList();
    }

    ClassLoader loader;
    if (PluginLoader.loader != null) {
      loader = PluginLoader.loader;
    } else {
      File[] pluginFiles = pluginFolder.listFiles(path -> path.getName().endsWith(".jar"));
      if (pluginFiles == null) {
        return Collections.emptyList();
      }
      try {
        PluginLoader.loader = loader = createLoader(pluginFiles);
      } catch (MalformedURLException e) {
        throw new UncheckedIOException(e);
      }
    }

    List<T> result = new LinkedList<>();
    try {
      Collection<T> plugins = loadPlugins(loader);
      result.addAll(plugins);
      log.info(String.format(
          "Loaded %d plugins of type '%s' from plugin folder: %s",
          plugins.size(),
          type.getSimpleName(),
          pluginFolder.getName()
      ));
    } catch (Exception | Error e) {
      log.severe("Error loading plugins: " + e);
      e.printStackTrace();
    }

    return result;
  }

  @Nonnull
  private ClassLoader createLoader(@Nonnull File[] files) throws MalformedURLException {
    URL[] urls = new URL[files.length];
    for (int i = 0; i < files.length; i++) {
      urls[i] = files[i].toURI().toURL();
    }

    return new URLClassLoader(urls, getClass().getClassLoader());
  }

  @Nonnull
  private Collection<T> loadPlugins(@Nonnull ClassLoader classLoader)
      throws MalformedURLException, ServiceConfigurationError, NoClassDefFoundError {
    ServiceLoader<T> loader = ServiceLoader.load(type, classLoader);

    List<T> result = new LinkedList<>();
    for (T plugin : loader) {
      if (type.isInstance(plugin)) {
        result.add(plugin);
      } else {
        log.severe(String.format(
            "Loaded plugin '%s' is not instance of desired type %s",
            plugin,
            type.getSimpleName()
        ));
      }
    }
    return result;
  }

  static void reset() {
    loader = null;
  }
}
