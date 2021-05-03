package net.peacefulcraft.mzr.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import net.peacefulcraft.mzr.Mzr;

public class PlayerDisconnectListener implements Listener {
  @EventHandler
  public void onPlayerJoin(PlayerQuitEvent ev) {
    Mzr._this().getDataManager().flushAndEvictData(ev.getPlayer().getUniqueId());
  }
}