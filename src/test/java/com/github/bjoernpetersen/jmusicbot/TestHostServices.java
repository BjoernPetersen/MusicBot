package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.platform.ContextSupplier;
import com.github.bjoernpetersen.jmusicbot.platform.HostServices;
import java.net.URL;
import org.jetbrains.annotations.NotNull;

public class TestHostServices implements HostServices {

  @Override
  public void openBrowser(@NotNull URL url) {
  }

  @NotNull
  @Override
  public ContextSupplier contextSupplier() throws IllegalStateException {
    throw new IllegalStateException();
  }
}
