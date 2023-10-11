package io.github.hongyuncloud.arthas.memory;

import io.github.hongyuncloud.arthas.util.HcIOUtils;
import io.github.hongyuncloud.arthas.util.HcMemortUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class HcMemoryService {
  public static final String PROTOCOL = "hc-arthas-memory";
  static final HcMemoryUrlHandler handlerInstance = new HcMemoryUrlHandler();
  static final Object repositoryLock = new Object();
  static final Map<String, HcMemoryRepository> repositoryMap = new HashMap<>();
  private static final Object factoryInstanceLock = new Object();
  private static final Map<URLStreamHandlerFactory, HcMemoryUrlFactory> factoryInstanceMap = new IdentityHashMap<>();
  private static final Function<URLStreamHandlerFactory, HcMemoryUrlFactory> factoryInstanceFunction = HcMemoryUrlFactory::new;
  private static long repositoryAllocator = Long.MIN_VALUE;

  private HcMemoryService() {
    throw new UnsupportedOperationException();
  }

  public static URLStreamHandlerFactory factory() {
    synchronized (factoryInstanceLock) {
      return factoryInstanceMap.computeIfAbsent(null, factoryInstanceFunction);
    }
  }

  public static URLStreamHandlerFactory factory(final URLStreamHandlerFactory fallback) {
    synchronized (factoryInstanceLock) {
      return factoryInstanceMap.computeIfAbsent(fallback, factoryInstanceFunction);
    }
  }

  public static URLStreamHandler handler() {
    return handlerInstance;
  }

  public static URL url(final String host, final String file) {
    try {
      return new URL(PROTOCOL, host, -1, file, handlerInstance);
    } catch (final MalformedURLException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static URL url(final String spec) throws MalformedURLException {
    if (spec.startsWith(PROTOCOL + ":")) {
      return new URL(null, spec, handlerInstance);
    } else {
      return new URL(null, PROTOCOL + ":" + spec, handlerInstance);
    }
  }

  public static URL save(final byte[] jarContent) throws IOException {
    return save(jarContent, null);
  }

  public static URL save(final byte[] jarBytes, final URL codeSource) throws IOException {
    synchronized (repositoryLock) {
      final long allocId = (repositoryAllocator++);
      final String jarId = HcMemortUtils.toId(allocId);
      final HcMemoryRepository repository = new HcMemoryRepository(jarId, codeSource);
      try (final JarInputStream jarIn = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
        JarEntry entry;
        while ((entry = jarIn.getNextJarEntry()) != null) {
          if (!entry.isDirectory()) {
            repository.put(entry.getName(), HcIOUtils.readAllBytes(jarIn));
          }
        }
      }
      repositoryMap.put(jarId, repository);
      return url(jarId, "/");
    }
  }

  public static HcMemoryRepository getRepository(String host) {
    synchronized (repositoryLock) {
      return repositoryMap.get(host);
    }
  }
}
