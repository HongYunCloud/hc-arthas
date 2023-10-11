package io.github.hongyuncloud.arthas.memory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public final class HcMemoryUrlHandler extends URLStreamHandler {
  HcMemoryUrlHandler() {
    //
  }

  @Override
  protected URLConnection openConnection(final URL url) throws IOException {
    if (HcMemoryService.PROTOCOL.equals(url.getProtocol())) {
      return new HcMemoryUrlConnection(url);
    } else {
      throw new IOException("unsupport protocol: " + url.getProtocol());
    }
  }
}