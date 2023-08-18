package io.github.hongyuncloud.arthas;

import bot.inker.acj.JvmHacker;
import io.github.hongyuncloud.arthas.helper.LockableThreadHelper;

import java.awt.*;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HcArthas {
  public static final String PLUGIN_ID = "hc-arthas";

  private static final Logger logger = Logger.getLogger(PLUGIN_ID);

  private static final String ARTHAS_BOOTSTRAP = "com.taobao.arthas.core.server.ArthasBootstrap";
  private static final String GET_INSTANCE = "getInstance";
  private static final String IS_BIND = "isBind";

  private static volatile ClassLoader arthasClassLoader;

  private HcArthas() {
    throw new UnsupportedOperationException();
  }

  public static void run(Instrumentation instrumentation, Path coreJarPath, Map<String, String> configMap) throws IOException {
    try {
      final Class<?> spyApiClass = Class.forName("java.arthas.SpyAPI");
      final Method isInitedMethod = spyApiClass.getMethod("isInited");
      if ((boolean) isInitedMethod.invoke(null)) {
        logger.log(Level.INFO, "Arthas server already stared, skip attach.");
        return;
      }
    } catch (final Throwable e) {
      // ignore
    }

    final String arthasArgs = HcFeatureCodec.DEFAULT_COMMANDLINE_CODEC.toString(configMap);
    final ClassLoader agentLoader = loadOrDefineClassLoader(coreJarPath);
    final Thread bindingThread = new Thread(() -> {
      try {
        final Instrumentation insn = instrumentation == null ? JvmHacker.instrumentation() : instrumentation;
        bind(insn, agentLoader, arthasArgs);
      } catch (final Throwable throwable) {
        logger.log(Level.SEVERE, "Failed to bind arthas", throwable);
      }
    });

    bindingThread.setName("arthas-binding-thread");
    bindingThread.start();
    try {
      bindingThread.join();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    final String openAddress = buildOpenAddress(configMap.get("ip"));
    final String url = buildUrl(openAddress, configMap.get("httpPort"));
    final String telnetCommand = buildTelnetPort(openAddress, configMap.get("httpPort"));
    printBind(url, telnetCommand);
    openBrowser(url);

    if (Boolean.parseBoolean(configMap.get("blockBootStrap"))) {
      logger.log(Level.INFO, "blocking bootstrap. to continue, use command 'continue bootstrap'");
      LockableThreadHelper.entryLock("bootstrap");
      logger.log(Level.INFO, "continue bootstrap");
    }
  }

  private static String buildOpenAddress(final String ip) {
    return (ip == null || ip.equals("127.0.0.1") || ip.equals("0.0.0.0")) ? "localhost" : ip;
  }

  private static String buildUrl(final String openAddress, final String port) {
    final StringBuilder sb = new StringBuilder();
    sb.append("http://")
        .append(openAddress)
        .append(":")
        .append(port);
    if (!"3658".equals(port)) {
      sb.append("/?port=").append(port);
    }
    return sb.toString();
  }

  private static String buildTelnetPort(final String openAddress, final String port) {
    return "telnet " + openAddress + ":" + port;
  }

  private static void openBrowser(final String url) {
    if (url != null) {
      try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
          Desktop.getDesktop().browse(URI.create(url));
        } else {
          throw new UnsupportedOperationException("desktop browse not support in this environment");
        }
      } catch (final Throwable e) {
        logger.log(Level.FINE, "Failed to open browser", e);
      }
    }
  }

  private static void printBind(final String url, final String telnetCommand) {
    if (url != null) {
      logger.log(Level.INFO, "Open \"" + url + "\" to access arthas web console");
    }
    if (telnetCommand != null) {
      logger.log(Level.INFO, "run \"" + telnetCommand + "\" to open arthas console session");
    }
  }

  private static ClassLoader loadOrDefineClassLoader(final Path... jarFiles) throws IOException {
    if (arthasClassLoader == null) {
      URL[] urls = new URL[jarFiles.length];
      for (int i = 0; i < jarFiles.length; i++) {
        urls[i] = jarFiles[i].toUri().toURL();
      }
      arthasClassLoader = new HcArthasClassloader(urls, HcArthas.class.getClassLoader());
      // arthasClassLoader = new HcArthasClassloader(new URL[]{
      //     arthasCoreJarFile.toUri().toURL()
      // });
    }
    return arthasClassLoader;
  }

  private static void bind(final Instrumentation inst, final ClassLoader agentLoader, final String args) throws Throwable {
    /**
     * <pre>
     * ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(inst);
     * </pre>
     */
    final Class<?> bootstrapClass = agentLoader.loadClass(ARTHAS_BOOTSTRAP);
    final Object bootstrap = bootstrapClass.getMethod(GET_INSTANCE, Instrumentation.class, String.class)
        .invoke(null, inst, args);
    final boolean isBind = (Boolean) bootstrapClass.getMethod(IS_BIND)
        .invoke(bootstrap);
    if (!isBind) {
      final String errorMsg = "Arthas server port binding failed! Please check ./plugins/" + HcArthas.PLUGIN_ID + "/logs/arthas.log for more details.";
      logger.log(Level.SEVERE, errorMsg);
      throw new RuntimeException(errorMsg);
    }
    logger.log(Level.INFO, "Arthas server already bind.");

    Class.forName("io.github.hongyuncloud.arthas.injector.HcInjectCore", false, agentLoader)
        .getMethod("run")
        .invoke(null);
  }
}
