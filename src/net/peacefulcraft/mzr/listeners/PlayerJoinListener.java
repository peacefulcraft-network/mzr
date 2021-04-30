package net.peacefulcraft.mzr.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.peacefulcraft.mzr.Mzr;

public class PlayerJoinListener implements Listener {
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent ev) {
    ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Welcome to the server! -Templateus");
  }
}