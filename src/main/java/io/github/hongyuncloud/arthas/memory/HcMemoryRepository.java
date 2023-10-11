package io.github.hongyuncloud.arthas.memory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public final class HcMemoryRepository extends HashMap<String, byte[]> {
  private final String repoName;
  private final URL codeSource;

  public HcMemoryRepository(final String repoName, final URL codeSource) {
    this.repoName = repoName;
    this.codeSource = codeSource;
  }

  public URL codeSource() {
    return codeSource;
  }

  public byte[] getFile(String fileName) throws IOException {
    byte[] result = get(fileName);
    if (result == null) {
      throw new FileNotFoundException(repoName + "/" + fileName);
    }
    return result;
  }
}