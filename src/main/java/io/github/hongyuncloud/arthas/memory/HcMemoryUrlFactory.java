package io.github.hongyuncloud.arthas.memory;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public final class HcMemoryUrlFactory implements URLStreamHandlerFactory {
  private final URLStreamHandlerFactory fallback;

  HcMemoryUrlFactory(final URLStreamHandlerFactory fallback) {
    this.fallback = fallback;
  }

  @Override
  public URLStreamHandler createURLStreamHandler(final String protocol) {
    if (HcMemoryService.PROTOCOL.equals(protocol)) {
      return HcMemoryService.handlerInstance;
    } else if (fallback != null) {
      return fallback.createURLStreamHandler(protocol);
    } else {
      return null;
    }
  }
}