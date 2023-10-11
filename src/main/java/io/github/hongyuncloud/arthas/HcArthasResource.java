package io.github.hongyuncloud.arthas;

import io.github.hongyuncloud.arthas.util.HcOSUtils;

public enum HcArthasResource {
  ARTHAS_CORE("file:arthas-core.jar", "resource:arthas-core.jar.bin", 1851301032L, true),
  ARTHAS_SPY("file:arthas-spy.jar", "resource:arthas-spy.jar.bin", 534078992L, true),
  // Native support
  ARTHAS_JNI_LINUX_AARCH64(
      "file:lib/libArthasJniLibrary-aarch64.so",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/lib/libArthasJniLibrary-aarch64.so",
      3914822205L,
      HcOSUtils.isLinux() && HcOSUtils.isArm64()
  ),
  ARTHAS_JNI_LINUX_X64(
      "file:lib/libArthasJniLibrary-x64.so",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/lib/libArthasJniLibrary-x64.so",
      2200934224L,
      HcOSUtils.isLinux() && HcOSUtils.isX86_64()
  ),
  ARTHAS_JNI_WINDOWS_X64(
      "file:lib/libArthasJniLibrary-x64.dll",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/lib/libArthasJniLibrary-x64.dll",
      2672813214L,
      HcOSUtils.isWindows() && HcOSUtils.isX86_64()
  ),
  ARTHAS_JNI_MACOS_X64(
      "file:lib/libArthasJniLibrary.dylib",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/lib/libArthasJniLibrary.dylib",
      3286357566L,
      HcOSUtils.isMac() // && HcOSUtils.isX86_64()
  ),
  // AsyncProfiler support
  ASYNC_PROFILER_LINUX_ARM64(
      "file:async-profiler/libasyncProfiler-linux-arm64.so",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/async-profiler/libasyncProfiler-linux-arm64.so",
      2945535008L,
      HcOSUtils.isLinux() && HcOSUtils.isArm64() && !HcOSUtils.isMuslLibc()
  ),
  ASYNC_PROFILER_LINUX_MUSL_ARM64(
      "file:async-profiler/libasyncProfiler-linux-musl-arm64.so",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/async-profiler/libasyncProfiler-linux-musl-arm64.so",
      49959941L,
      HcOSUtils.isLinux() && HcOSUtils.isArm64() && HcOSUtils.isMuslLibc()
  ),
  ASYNC_PROFILER_LINUX_X64(
      "file:async-profiler/libasyncProfiler-linux-x64.so",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/async-profiler/libasyncProfiler-linux-x64.so",
      3337767191L,
      HcOSUtils.isLinux() && HcOSUtils.isX86_64() && !HcOSUtils.isMuslLibc()
  ),
  ASYNC_PROFILER_LINUX_MUSL_X64(
      "file:async-profiler/libasyncProfiler-linux-musl-x64.so",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/async-profiler/libasyncProfiler-linux-musl-x64.so",
      35345756L,
      HcOSUtils.isLinux() && HcOSUtils.isX86_64() && HcOSUtils.isMuslLibc()
  ),
  ASYNC_PROFILER_MACOS(
      "file:async-profiler/libasyncProfiler-mac.so",
      "https://hc-arthas-libs.bgp.ink/arthas-packaging-3.6.9-bin/async-profiler/libasyncProfiler-mac.so",
      3405645284L,
      HcOSUtils.isMac() // && HcOSUtils.isX86_64()
  );

  private final String saveTo;
  private final String url;
  private final long crc32;
  private final boolean required;

  HcArthasResource(final String saveTo, final String url, final long crc32, final boolean required) {
    this.saveTo = saveTo;
    this.url = url;
    this.crc32 = crc32;
    this.required = required;
  }

  public String saveTo() {
    return saveTo;
  }

  public String url() {
    return url;
  }

  public long crc32() {
    return crc32;
  }

  public boolean required() {
    return required;
  }
}
