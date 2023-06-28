package io.github.hongyuncloud.arthas;

import bot.inker.acj.JvmHacker;

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
    } catch (Throwable e) {
      // ignore
    }

    final String arthasArgs = HcFeatureCodec.DEFAULT_COMMANDLINE_CODEC.toString(configMap);
    final ClassLoader agentLoader = loadOrDefineClassLoader(coreJarPath);
    final Thread bindingThread = new Thread() {
      @Override
      public void run() {
        try {
          bind(
              instrumentation == null ? JvmHacker.instrumentation() : instrumentation,
              agentLoader,
              arthasArgs
          );
        } catch (Throwable throwable) {
          logger.log(Level.SEVERE, "Failed to bind arthas", throwable);
        }
      }
    };

    bindingThread.setName("arthas-binding-thread");
    bindingThread.start();
    try {
      bindingThread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    openBrowser(configMap.get("ip"), configMap.get("httpPort"));
  }

  private static void openBrowser(final String ip, final String port) {
    if (port == null) {
      return;
    }
    String openAddress = ip;
    if (openAddress == null || openAddress.equals("127.0.0.1") || openAddress.equals("0.0.0.0")) {
      openAddress = "localhost";
    }
    final String url = "http://" + openAddress + ":" + port;
    try {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI.create(url));
      } else {
        throw new UnsupportedOperationException("desktop browse not support in this environment");
      }
    } catch (Throwable e) {
      logger.log(Level.FINE, "Failed to open browser", e);
    }
    logger.log(Level.INFO, "Open \"" + url + "\" to access arthas console");
  }

  private static ClassLoader loadOrDefineClassLoader(Path arthasCoreJarFile) throws IOException {
    if (arthasClassLoader == null) {
      arthasClassLoader = new HcArthasClassloader(new URL[]{arthasCoreJarFile.toUri().toURL()});
    }
    return arthasClassLoader;
  }

  private static void bind(Instrumentation inst, ClassLoader agentLoader, String args) throws Throwable {
    /**
     * <pre>
     * ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(inst);
     * </pre>
     */
    Class<?> bootstrapClass = agentLoader.loadClass(ARTHAS_BOOTSTRAP);
    Object bootstrap = bootstrapClass.getMethod(GET_INSTANCE, Instrumentation.class, String.class).invoke(null, inst, args);
    boolean isBind = (Boolean) bootstrapClass.getMethod(IS_BIND).invoke(bootstrap);
    if (!isBind) {
      String errorMsg = "Arthas server port binding failed! Please check $HOME/logs/arthas/arthas.log for more details.";
      logger.log(Level.SEVERE, errorMsg);
      throw new RuntimeException(errorMsg);
    }
    logger.log(Level.INFO, "Arthas server already bind.");
  }
}
