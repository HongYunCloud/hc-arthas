package io.github.hongyuncloud.arthas.util;

import java.util.Base64;

public class HcMemortUtils {
  private static final Base64.Encoder idEncoder = Base64.getEncoder().withoutPadding();

  private HcMemortUtils() {
    throw new UnsupportedOperationException();
  }

  public static String toId(long x) {
    return idEncoder.encodeToString(new byte[]{
        (byte) (x >> 56), (byte) (x >> 48), (byte) (x >> 40), (byte) (x >> 32),
        (byte) (x >> 24), (byte) (x >> 16), (byte) (x >> 8), (byte) (x)
    });
  }
}
