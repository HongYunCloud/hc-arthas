package io.github.hongyuncloud.arthas.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LockableThreadHelper {
  private static final Object globalLock = new Object();
  private static final Map<String, CompletableFuture<?>> locks = new HashMap<>();

  private LockableThreadHelper() {
    throw new UnsupportedOperationException();
  }

  public static void entryLock(final String lockName) {
    CompletableFuture<?> lock;
    synchronized (globalLock) {
      lock = locks.get(lockName);
      if (lock == null || lock.isDone()) {
        lock = new CompletableFuture<>();
        locks.put(lockName, lock);
      }
    }
    try {
      lock.get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      //
    }
  }

  public static void exitLock(final String lockName) {
    synchronized (globalLock) {
      CompletableFuture<?> lock = locks.get(lockName);
      if (lock != null && !lock.isDone()) {
        lock.complete(null);
      }
    }
  }
}
