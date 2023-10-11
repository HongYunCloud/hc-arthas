package io.github.hongyuncloud.arthas.injector;

import com.taobao.arthas.core.server.ArthasBootstrap;

public class HcInjectCore {
  public static void run() {
    ArthasBootstrap.getInstance()
        .getShellServer()
        .registerCommandResolver(new HcCommandResolver());
  }
}
