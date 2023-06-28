package io.github.hongyuncloud.arthas;

import java.lang.instrument.Instrumentation;

public final class HcArthasMain {
  public static void premain(final String args, final Instrumentation instrumentation) {
    main(args, instrumentation);
  }

  public static void agentmain(final String args, final Instrumentation instrumentation) {
    main(args, instrumentation);
  }

  private static void main(final String args, final Instrumentation instrumentation) {
    final HcArthasCore core = new HcArthasCore(instrumentation);
    core.onLoad();
    core.onEnable();
  }
}
