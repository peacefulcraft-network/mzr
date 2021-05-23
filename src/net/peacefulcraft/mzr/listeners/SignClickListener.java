package net.peacefulcraft.mzr.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import net.md_5.bungee.api.ChatColor;
import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.objective.DataManager;
import net.peacefulcraft.mzr.objective.Objective;
import net.peacefulcraft.mzr.objective.ObjectiveProgress;

/**
 * Detect clicks on MZR signs and trigger associated actions
 */
public class SignClickListener implements Listener {
	
	@EventHandler
	public void onSignClick(PlayerInteractEvent ev) {
		if (ev.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }

		if (ev.getClickedBlock().getState() instanceof Sign) {
			Sign s = (Sign) ev.getClickedBlock().getState();

			// Check for MZR header
			if (!ChatColor.stripColor(s.getLine(0)).equalsIgnoreCase("[MZR]")) { return; }

			Objective objective = Mzr._this().getObjectiveManager().getObjective(ChatColor.stripColor(s.getLine(1)));
			if (objective == null) { return; }

			DataManager dm = Mzr._this().getDataManager();
			String teleportTarget = ChatColor.stripColor(s.getLine(2));

			/**
			 * Resume objective sign
			 */
			if (teleportTarget.equalsIgnoreCase("resume")) {
				CompletableFuture<ObjectiveProgress> cf = dm.getData(ev.getPlayer().getUniqueId());
				cf.exceptionally((ex) -> {
					Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
						ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Error reading datafile. Please contact an admin if this issue persists.");
					});
					return null;
				});
				cf.thenAccept((objp) -> {
					Map<String, Object> objectiveData = objp.getObjectiveProgress(objective.getName());
					
					// No data for this objective, teleport to start point
					if (objectiveData == null) {
						Mzr._this().logDebug("Player " + ev.getPlayer() + " requested resume on objective " + objective.getName() + ", but had no previous save data. Creating.");
						Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
							try {
								ev.getPlayer().teleport(objective.getCheckpoint(0));
							} catch (IndexOutOfBoundsException ex) {
								ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Objective appears to be misconfigured. Please contact an admin.");
							}
						});
						return;
					}

					// Data object exists, should have resume point
					Integer resumeCheckpoint = Integer.valueOf((String) objectiveData.get("resume"));
					Mzr._this().logDebug("Player " + ev.getPlayer() + " requested resume on objective " + objective.getName() + ". Found they were on checkpoint " + resumeCheckpoint);
					Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
						try {
							ev.getPlayer().teleport(objective.getCheckpoint(resumeCheckpoint));
						} catch (IndexOutOfBoundsException ex) {
							ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Objective appears to be misconfigured. Please contact an admin.");
						}
					});
				});
			
			// Should be a checkpoint number, save progress to ObjectiveProgress file
			} else {
				Integer checkpointNumber = Integer.valueOf(teleportTarget);
				CompletableFuture<ObjectiveProgress> cf = dm.getData(ev.getPlayer().getUniqueId());
				cf.exceptionally((ex) -> {
					Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
						ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Error reading datafile. Please contact an admin if this issue persists.");
					});
					return null;
				});
				cf.thenAccept((objp) -> {
					Map<String, Object> objectiveData = objp.getObjectiveProgress(objective.getName());
					if (objectiveData == null) {
						objectiveData = new HashMap<String, Object>();
						objp.setObjectiveProgress(objective.getName(), objectiveData);
					}

					// Objective complete. Reached final checkpoint
					if (checkpointNumber+1 == objective.getCheckpoints().size()) {
						objectiveData.put("resume", "0");
						objectiveData.put("complete" , "1");
						Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
							ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Congratulations! You've completed " + objective.getName());
							ev.getPlayer().teleport(objective.getLobbyPoint());
						});

					// Standard checkpoint number. Save new location
					} else {
						objectiveData.put("resume", checkpointNumber.toString());
					}

					// Commit to disk
					CompletableFuture<Boolean> cf2 = objp.save();
					cf2.exceptionally((ex) -> {
						Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
							ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Error saving datafile. Try clicking the sign again. Contact an admin if this issue persists.");
						});
						return null;
					});
					cf2.thenAccept((ret) -> {
						Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
							ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Checkpoint saved. Great work!");
						});
					});
				});
			}
		}
	}
}
