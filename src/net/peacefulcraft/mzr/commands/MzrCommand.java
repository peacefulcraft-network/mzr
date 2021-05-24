package net.peacefulcraft.mzr.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.objective.Objective;
import net.peacefulcraft.mzr.objective.ObjectiveProgress;

public class MzrCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Mzr.messagingPrefix + "MZR command is only usable by players.");
			return true;
		}
		Player p = (Player) sender;
		
		if (args.length < 1) {
			sender.sendMessage(Mzr.messagingPrefix + "Please include a puzzle name.");
			return true;
		}

		String objectiveName = args[0];
		Objective objective = Mzr._this().getObjectiveManager().getObjective(objectiveName);
		if (objective == null) {
			sender.sendMessage(Mzr.messagingPrefix + objectiveName + " is not a valid objective.");
			return true;
		}

		CompletableFuture<ObjectiveProgress> cf = Mzr._this().getDataManager().getData(p.getUniqueId());
			cf.exceptionally((ex) -> {
				ex.printStackTrace();
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					sender.sendMessage(Mzr.messagingPrefix + "Error reading datafile. Please contact an admin if this issue persists.");
				});
				return null;
			});
			cf.thenAccept((objp) -> {
				Map<String, Object> objectiveData = objp.getObjectiveProgress(objective.getName());
				
				// No data for this objective, teleport to start point
				if (objectiveData == null) {
					Mzr._this().logDebug("Player " + p.getName() + " requested resume on objective " + objective.getName() + ", but had no previous save data. Creating.");
					Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
						try {
							p.teleport(objective.getCheckpoint(0));
						} catch (IndexOutOfBoundsException ex) {
							sender.sendMessage(Mzr.messagingPrefix + "Objective appears to be misconfigured. Please contact an admin.");
						}
					});
					return;
				}

				// Data object exists, should have resume point
				Integer resumeCheckpoint = Integer.valueOf((String) objectiveData.get("resume"));
				Mzr._this().logDebug("Player " + p.getName() + " requested resume on objective " + objective.getName() + ". Found they were on checkpoint " + resumeCheckpoint);
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					try {
						p.teleport(objective.getCheckpoint(resumeCheckpoint));
					} catch (IndexOutOfBoundsException ex) {
						sender.sendMessage(Mzr.messagingPrefix + "Objective appears to be misconfigured. Please contact an admin.");
					}
				});
			});

		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		ArrayList<String> opts = new ArrayList<String>();
		if (args.length == 0) { return opts; }

		String prefix = args[0];
		opts.addAll(Mzr._this().getObjectiveManager().getActiveObjectiveNames());


		// String match what the user has typed in
		Iterator<String> opti = opts.iterator();
		while(opti.hasNext()) {
			String opt = opti.next();
			if (!opt.startsWith(prefix)) {
				opti.remove();
			}
		}

		return opts;
	}
}
