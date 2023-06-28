package io.github.hongyuncloud.arthas;

import org.bukkit.plugin.java.JavaPlugin;

public final class HcArthasPlugin extends JavaPlugin {
  static {
    final HcArthasCore core = new HcArthasCore(null);
    core.onLoad();
    core.onEnable();
  }
}
