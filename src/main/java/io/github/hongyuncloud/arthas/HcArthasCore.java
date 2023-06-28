package io.github.hongyuncloud.arthas;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.instrument.Instrumentation;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HcArthasCore {
  private static final Logger logger = Logger.getLogger(HcArthas.PLUGIN_ID);

  private static final int BUFFER_SIZE = 8192;

  private static final String CONFIG_YML = "config.yml";

  private static final String ARTHAS_CORE_JAR = "arthas-core.jar";
  private static final String ARTHAS_CORE_JAR_SHA256 = "HaxGHzdJ4Yx0bWBEiCw2F1WCiETjaKziRFQreO8YNNM";

  private static final String ARTHAS_SPY_JAR = "arthas-spy.jar";
  private static final String ARTHAS_SPY_JAR_SHA256 = "G56KB8wn+ZzbGHkA4GMPoh/NomwUpWf/I35mVUuJCA4";

  private static final Path PLUGIN_DIRECTORY = Paths.get("plugins", HcArthas.PLUGIN_ID);

  private final Instrumentation instrumentation;
  private Map<String, String> configMap;

  public HcArthasCore(final Instrumentation instrumentation) {
    this.instrumentation = instrumentation;
  }

  private static boolean checksum(final Path file, final String algorithm, final String hash) throws IOException {
    if (hash == null) {
      return true;
    }
    final MessageDigest md;
    try {
      md = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("Failed to checksum file", e);
    }
    try (final ByteChannel in = Files.newByteChannel(file)) {
      final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
      while (in.read(buffer) != -1) {
        buffer.flip();
        md.update(buffer);
        buffer.clear();
      }
      return Arrays.equals(Base64.getDecoder().decode(hash), md.digest());
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
    if (!Files.exists(PLUGIN_DIRECTORY)) {
      Files.createDirectories(PLUGIN_DIRECTORY);
    }
    saveResource(CONFIG_YML, false);
    saveResource("logback.xml", false);
    ensureResource(ARTHAS_CORE_JAR, ARTHAS_CORE_JAR_SHA256);
    ensureResource(ARTHAS_SPY_JAR, ARTHAS_SPY_JAR_SHA256);

    final Map<String, String> configMap = new LinkedHashMap<>();
    final Map<String, Object> config = loadConfig();
    for (Map.Entry<String, Object> entry: config.entrySet()) {
      if(entry.getValue() instanceof Collection<?>){
        final StringJoiner joiner = new StringJoiner(", ");
        for (Object element : ((Collection<?>) entry.getValue())) {
          joiner.add(element.toString());
        }
        configMap.put(entry.getKey(), joiner.toString());
      }else{
        configMap.put(entry.getKey(), entry.getValue().toString());
      }
    }
    configMap.put("outputPath", "plugins/hc-arthas/output");
    this.configMap = configMap;
  }

  private Map<String, Object> loadConfig() throws IOException {
    try(final Reader reader = Files.newBufferedReader(PLUGIN_DIRECTORY.resolve(CONFIG_YML))) {
      return new Yaml().loadAs(reader, Map.class);
    }
  }

  private void onEnableImpl() throws IOException {
    HcArthas.run(instrumentation, PLUGIN_DIRECTORY.resolve(ARTHAS_CORE_JAR), this.configMap);
  }

  private void saveResource(String resourceName, boolean replace) {
    final Path resourcePath = PLUGIN_DIRECTORY.resolve(resourceName);
    if (replace || !Files.exists(resourcePath)) {
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

  private void ensureResource(String resourceName, String sha256) throws IOException {
    final Path targetFile = PLUGIN_DIRECTORY.resolve(resourceName);
    if (Files.exists(targetFile)) {
      final boolean checksumSuccess = checksum(targetFile, "SHA-256", sha256);
      if (checksumSuccess) {
        return;
      } else {
        Files.delete(targetFile);
      }
    }
    Files.createDirectories(targetFile.getParent());
    try (final InputStream in = getResource(resourceName + ".bin")) {
      if (in == null) {
        throw new IllegalArgumentException("The embedded resource '" + resourceName + ".bin' cannot be found in " + HcArthas.PLUGIN_ID);
      }
      Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private InputStream getResource(String filename) {
    return HcArthas.class.getClassLoader().getResourceAsStream(filename);
  }
}
