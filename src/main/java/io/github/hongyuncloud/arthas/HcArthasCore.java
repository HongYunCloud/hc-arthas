package io.github.hongyuncloud.arthas;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public final class HcArthasCore {
  private static final Logger logger = Logger.getLogger(HcArthas.PLUGIN_ID);

  private static final int BUFFER_SIZE = 8192;

  private static final String CONFIG_YML = "config.yml";

  private static final Path PLUGIN_DIRECTORY = Paths.get("plugins", HcArthas.PLUGIN_ID);

  private final Instrumentation instrumentation;
  private Map<String, String> configMap;

  public HcArthasCore(final Instrumentation instrumentation) {
    this.instrumentation = instrumentation;
  }

  private static boolean checksum(final Path file, final long hash) throws IOException {
    final CRC32 md = new CRC32();
    try (final InputStream in = Files.newInputStream(file)) {
      int readSize;
      final byte[] buffer = new byte[BUFFER_SIZE];
      while ((readSize = in.read(buffer)) != -1) {
        md.update(buffer, 0, readSize);
      }
      final long digest = md.getValue();
      if (digest == hash) {
        return true;
      } else {
        logger.log(Level.INFO, "checksum not valid for \"" + file + "\", expected \"" + hash + "\", got \"" + digest + "\"");
        return false;
      }
    }
  }

  private static InputStream getResource(final String resourceUrl) {
    if (resourceUrl.startsWith("resource:")) {
      return HcArthas.class
          .getClassLoader()
          .getResourceAsStream(resourceUrl.substring("resource:".length()));
    } else {
      try {
        final URL rawUrl = new URL(resourceUrl);
        final URLConnection connection = rawUrl.openConnection();
        connection.setRequestProperty("User-Agent", "HcArthas(hongyuncloud@proton.me)");
        return connection.getInputStream();
      } catch (FileNotFoundException e) {
        return null;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  public void onLoad() {
    try {
      onLoadImpl();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void onEnable() {
    try {
      onEnableImpl();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void onLoadImpl() throws IOException {
    logger.info("Loading " + HcArthas.PLUGIN_ID);
    if (!Files.exists(PLUGIN_DIRECTORY)) {
      Files.createDirectories(PLUGIN_DIRECTORY);
    }
    saveResource(CONFIG_YML);
    saveResource("logback.xml");
    ensureResource(HcArthasResource.ARTHAS_CORE);
    ensureResource(HcArthasResource.ARTHAS_SPY);

    final Map<String, String> configMap = new LinkedHashMap<>();
    configMap.put("outputPath", "plugins/" + HcArthas.PLUGIN_ID + "/output");
    final Map<String, Object> config = loadConfig();
    for (Map.Entry<String, Object> entry : config.entrySet()) {
      if (entry.getValue() instanceof Collection<?>) {
        final StringJoiner joiner = new StringJoiner(", ");
        for (Object element : ((Collection<?>) entry.getValue())) {
          joiner.add(element.toString());
        }
        configMap.put(entry.getKey(), joiner.toString());
      } else {
        configMap.put(entry.getKey(), entry.getValue().toString());
      }
    }
    configMap.compute("telnetPort", this::resolvePort);
    configMap.compute("httpPort", this::resolvePort);

    if (Boolean.parseBoolean(configMap.get("enableNativeSupport"))) {
      ensureResource(HcArthasResource.ARTHAS_JNI_LINUX_AARCH64);
      ensureResource(HcArthasResource.ARTHAS_JNI_LINUX_X64);
      ensureResource(HcArthasResource.ARTHAS_JNI_WINDOWS_X64);
      ensureResource(HcArthasResource.ARTHAS_JNI_MACOS_X64);
    }

    if (Boolean.parseBoolean(configMap.get("enableProfilerSupport"))) {
      ensureResource(HcArthasResource.ASYNC_PROFILER_LINUX_ARM64);
      ensureResource(HcArthasResource.ASYNC_PROFILER_LINUX_MUSL_ARM64);
      ensureResource(HcArthasResource.ASYNC_PROFILER_LINUX_X64);
      ensureResource(HcArthasResource.ASYNC_PROFILER_LINUX_MUSL_X64);
      ensureResource(HcArthasResource.ASYNC_PROFILER_MACOS);
    }

    this.configMap = configMap;
  }

  private String resolvePort(String key, String oldValue) {
    if (oldValue != null && Integer.parseInt(oldValue) != 0) {
      return oldValue;
    }
    final int availablePort;
    try (final ServerSocket serverSocket = new ServerSocket(0)) {
      availablePort = serverSocket.getLocalPort();
      if (availablePort <= 0) {
        throw new IOException("Failed to bind server socket");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return Integer.toString(availablePort);
  }

  private Map<String, Object> loadConfig() throws IOException {
    try (final Reader reader = Files.newBufferedReader(PLUGIN_DIRECTORY.resolve(CONFIG_YML))) {
      return new Yaml().loadAs(reader, Map.class);
    }
  }

  private void onEnableImpl() throws IOException {
    logger.info("Enabling " + HcArthas.PLUGIN_ID);
    HcArthas.run(instrumentation,
        PLUGIN_DIRECTORY.resolve(HcArthasResource.ARTHAS_CORE.saveTo()),
        this.configMap
    );
  }

  private void saveResource(String resourceName) {
    final Path resourcePath = PLUGIN_DIRECTORY.resolve(resourceName);
    if (!Files.exists(resourcePath)) {
      try {
        Files.createDirectories(resourcePath.getParent());
        try (final InputStream in = getResource(resourceName)) {
          if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourceName + "' cannot be found in " + HcArthas.PLUGIN_ID);
          }
          Files.copy(in, resourcePath, StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Could not save " + resourceName + " to " + resourcePath, e);
      }
    }
  }

  private void ensureResource(HcArthasResource resource) throws IOException {
    if (!resource.required()) {
      return;
    }
    final Path targetFile = PLUGIN_DIRECTORY.resolve(resource.saveTo());
    if (Files.exists(targetFile)) {
      final boolean checksumSuccess = checksum(targetFile, resource.crc32());
      if (checksumSuccess) {
        return;
      } else {
        Files.delete(targetFile);
      }
    }
    Files.createDirectories(targetFile.getParent());
    final Path tempFile = PLUGIN_DIRECTORY.resolve(resource.saveTo() + ".tmp");
    logger.info("downloading \"" + resource.saveTo() + "\" from \"" + resource.url() + "\"");
    try (final InputStream in = getResource(resource.url())) {
      if (in == null) {
        throw new IllegalArgumentException("The resource '" + resource.url() + "' cannot be found");
      }
      Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
    }
    final boolean checksumSuccess = checksum(tempFile, resource.crc32());
    if (checksumSuccess) {
      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } else {
      Files.delete(tempFile);
      throw new IOException("Failed to download file \"" + resource.saveTo() + "\": checksum failure");
    }
  }
}
