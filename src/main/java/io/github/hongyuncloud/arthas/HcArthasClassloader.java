package io.github.hongyuncloud.arthas;

import io.github.hongyuncloud.arthas.memory.HcMemoryService;
import io.github.hongyuncloud.arthas.memory.HcMemoryUrlConnection;
import io.github.hongyuncloud.arthas.util.HcIOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;

public final class HcArthasClassloader extends URLClassLoader {
  private final ClassLoader nullClassLoaderReference;
  private final ClassLoader systemClassLoader;
  private final ClassLoader realParentClassLoader;

  public HcArthasClassloader(final URL[] urls, final ClassLoader realParentClassLoader) {
    super(urls, null);
    this.nullClassLoaderReference = new ClassLoader() {
    };
    this.systemClassLoader = ClassLoader.getSystemClassLoader().getParent();
    this.realParentClassLoader = realParentClassLoader;
  }

  @Override
  protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      Class<?> clazz = findLoadedClass(name);

      if (clazz == null && name.startsWith("io.github.hongyuncloud.arthas.injector.")) {
        try(final InputStream in = realParentClassLoader.getResourceAsStream(name.replace('.', '/') + ".class")) {
          if(in != null) {
            final byte[] content = HcIOUtils.readAllBytes(in);
            clazz = defineClass(name, content, 0, content.length);
          }
        } catch (IOException e) {
          //
        }
      }

      if (clazz == null) {
        try {
          clazz = nullClassLoaderReference.loadClass(name);
        } catch (final ClassNotFoundException e) {
          //
        }
      }

      if(clazz == null) {
        try {
          clazz = findClass(name);
        } catch (final ClassNotFoundException e) {
          //
        }
      }

      if (clazz == null) {
        try {
          clazz = systemClassLoader.loadClass(name);
        } catch (final ClassNotFoundException e) {
          //
        }
      }

      if (clazz == null) {
        try {
          clazz = realParentClassLoader.loadClass(name);
        } catch (final ClassNotFoundException e) {
          //
        }
      }

      if (clazz == null) {
        throw new ClassNotFoundException(name);
      }

      if (resolve) {
        resolveClass(clazz);
      }

      return clazz;
    }
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    // 测试用，memory protocol暂未启用
    if (true) {
      return super.findClass(name);
    }
    try {
      final String path = name.replace('.', '/').concat(".class");
      final URL resource = findResource(path);
      if (resource == null || !HcMemoryService.PROTOCOL.equals(resource.getProtocol())) {
        return super.findClass(name);
      }
      final byte[] content;
      final HcMemoryUrlConnection connection = (HcMemoryUrlConnection) resource.openConnection();
      final URL codeSource = connection.getCodeSource();
      try (final InputStream in = connection.getInputStream()) {
        content = HcIOUtils.readAllBytes(in);
      }
      return defineClass(name, content, 0, content.length, new CodeSource(codeSource, (Certificate[]) null));
    } catch (IOException e) {
      throw new ClassNotFoundException(name, e);
    }
  }
}