package net.peacefulcraft.mzr.listeners;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;

import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.objective.ObjectiveProgress;

public class PlayerInLavaListener implements Listener {
	
	@EventHandler
	public void onPlayerIgnite(EntityCombustByBlockEvent ev) {
		Mzr._this().logDebug("Got EC event");
		if (!(ev.getEntity() instanceof Player)) { return; }
		Mzr._this().logDebug("on player");

		Player p = (Player) ev.getEntity();

		Mzr._this().logDebug("in " + p.getLocation().getBlock().getType());
		if (p.getLocation().getBlock().getType().equals(Material.LAVA)) {
			Mzr._this().logDebug("Cleared.");
			CompletableFuture<ObjectiveProgress> cf = Mzr._this().getDataManager().getData(p.getUniqueId());
			cf.thenAcceptAsync((progress) -> {
				Mzr._this().logDebug("Dispatch");
				Mzr._this().getObjectiveManager().playerResumeObjective(p, progress.getLastObjective());
			});
		}
	}
}
