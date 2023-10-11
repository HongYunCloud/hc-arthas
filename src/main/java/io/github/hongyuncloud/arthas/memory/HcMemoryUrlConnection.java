package io.github.hongyuncloud.arthas.memory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public final class HcMemoryUrlConnection extends URLConnection {
  private InputStream inputStream;

  HcMemoryUrlConnection(final URL url) {
    super(url);
  }

  @Override
  public void connect() throws IOException {
    final String repoName = url.getHost();
    final String fileName = url.getFile().substring(1);

    final HcMemoryRepository jarRepository;
    synchronized (HcMemoryService.repositoryLock) {
      jarRepository = HcMemoryService.repositoryMap.get(repoName);
    }
    if (jarRepository == null) {
      throw new FileNotFoundException(repoName + "/" + fileName);
    }
    inputStream = new ByteArrayInputStream(jarRepository.getFile(fileName));
    connected = true;
  }

  public URL getCodeSource() {
    final String repoName = url.getHost();
    final HcMemoryRepository jarRepository;
    synchronized (HcMemoryService.repositoryLock) {
      jarRepository = HcMemoryService.repositoryMap.get(repoName);
    }
    if (jarRepository == null || jarRepository.codeSource() == null) {
      return HcMemoryService.url(repoName, "/");
    } else {
      return jarRepository.codeSource();
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    connect();
    return inputStream;
  }
}