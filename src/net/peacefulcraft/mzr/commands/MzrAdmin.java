package net.peacefulcraft.mzr.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.objective.Objective;

public class MzrAdmin implements CommandExecutor, TabCompleter {

  /**
   * @param sender Entity which used the command.
   * @param Command Object with details about the executed command
   * @param String label String with the command run, without the passed arguements.
   * @param String args The arguements passed by the player. Each space in the command counts as a new argument. 
   */
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("mzr.admin")) {
			sender.sendMessage(Mzr.messagingPrefix + "You do not have permssion to execute this command.");
			return true;
		}

		if (args.length < 2) {
			sender.sendMessage(Mzr.messagingPrefix + "Indicate the desired action [ create, delete, [ objective ] ]");
			return true;
		}

		/**
		 * Create objective
		 */
		if (args[0].equalsIgnoreCase("create")) {
			if (args.length < 2) {
				sender.sendMessage(Mzr.messagingPrefix + "Include an objective name.");
				return true;
			}

			String objectiveName = args[1];
			if (!this.objectiveExists(objectiveName)) {
				sender.sendMessage(Mzr.messagingPrefix + "Unknown objective " + args[1]);
				return true;
			}

			CompletableFuture<Objective> cf = Mzr._this().getObjectiveManager().createNewObjective(objectiveName);
			cf.exceptionally((ex) -> {
				ex.printStackTrace();
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					sender.sendMessage(Mzr.messagingPrefix + "Error creating objective " + objectiveName + ". " + ex.getMessage());
				});
				return null;
			});
			cf.thenAcceptAsync((objective) -> {
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					sender.sendMessage(Mzr.messagingPrefix + "Objective " + objectiveName + " succesfully created.");
				});
			});
			return true;
		}

		/**
		 * Delete objective
		 */
		if (args[0].equalsIgnoreCase("delete")) {
			if (args.length < 2) {
				sender.sendMessage(Mzr.messagingPrefix + "Include an objective name.");
				return true;
			}
			
			String objectiveName = args[1];
			if (!this.objectiveExists(objectiveName)) {
				sender.sendMessage(Mzr.messagingPrefix + "Unknown objective " + args[1]);
				return true;
			}

			
			CompletableFuture<Void> cf = Mzr._this().getObjectiveManager().deleteObjective(objectiveName);
			cf.exceptionally((ex) -> {
				ex.printStackTrace();
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					sender.sendMessage(Mzr.messagingPrefix + "Error creating objective " + objectiveName + ". " + ex.getMessage());
				});
				return null;
			});
			cf.thenAccept((objective) -> {
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					sender.sendMessage(Mzr.messagingPrefix + "Objective " + objectiveName + " succesfully deleted.");
				});
			});
			return true;
		}

		/**
		 * Setters & Getters
		 * executor should be a player
		 * args[1] should be an objective name
		 * args.length should be > 3
		 */
		if (args.length < 3) {
			sender.sendMessage(Mzr.messagingPrefix + "Objective name, checkpoint action, and checkpoint # are required.");
			return true;
		}
		String objectiveName = args[0];
		String objectiveAction = args[1];
		String objectiveCheckpoint = args[2];

		if (!(sender instanceof Player)) {
			sender.sendMessage(Mzr.messagingPrefix + "Objective manipulation commands require location data and are not executable by non-players.");
			return true;
		}
		Player p = (Player) sender;

		Objective objective = Mzr._this().getObjectiveManager().getObjective(objectiveName);
		if (objective == null) {
			sender.sendMessage(Mzr.messagingPrefix + "Unknown objective " + args[0]);
			return true;
		}

		/**
		 * mzra get [#]
		 * Teleport executor to a checkpoint location
		 */
		if (objectiveAction.equalsIgnoreCase("get")) {
			Location teleportLocation = null;
			if (objectiveCheckpoint.equalsIgnoreCase("lobbypoint")) {
				teleportLocation = objective.getLobbyPoint();
				if (teleportLocation == null ) {
					sender.sendMessage(Mzr.messagingPrefix + "Objective " + objectiveName + " does not have a lobby point set.");
					return true;
				}

			} else {
				// Try to treat the arg as a teleport location index
				try {
					Integer index = Integer.valueOf(objectiveCheckpoint);
					if (index > -1 && index < objective.getCheckpoints().size()) {
						teleportLocation = objective.getCheckpoint(index);
					} else {
						// Number, but is too big
						sender.sendMessage(Mzr.messagingPrefix + "Invalid teleport point. Provide a checkpoint number or 'lobbypoint'.");
						return true;
					}
				} catch (NumberFormatException ex) {
					// Was not 'lobbypoint', or a legal number.
					sender.sendMessage(Mzr.messagingPrefix + "Invalid teleport point. Provide a checkpoint number or 'lobbypoint'.");
					return true;
				}
			}

			try {
				p.teleport(teleportLocation);
				sender.sendMessage(Mzr.messagingPrefix + "Teleported to " + objectiveName + "(" + objectiveCheckpoint + ")");
			} catch (IndexOutOfBoundsException|NullPointerException ex) {
				sender.sendMessage(Mzr.messagingPrefix + "Objective " + objectiveName + " has no checkpoint " + objectiveCheckpoint);
			}
			return true;
		}

		/**
		 * mzra set [#]
		 * Add or replace an existing checkpoint
		 */
		if (objectiveAction.equalsIgnoreCase("set")) {
			CompletableFuture<?> cf = null;
			if (objectiveCheckpoint.equalsIgnoreCase("lobbypoint")) {
				cf = objective.setLobbypoint(p.getLocation());

			} else {
				// Try to treat the arg as a teleport location index
				try {
					Integer index = Integer.valueOf(objectiveCheckpoint);
					// Set existing point
					if (index > -1 && index < objective.getCheckpoints().size()) {
						cf = objective.setCheckpoint(index, p.getLocation());

					// Add new checkpoint
					} else if (index > -1 && index == objective.getCheckpoints().size()) {
						cf = objective.addCheckpoint(index, p.getLocation());

					// Number, but is too big or negative
					} else {
						sender.sendMessage(Mzr.messagingPrefix + "Invalid teleport point. Provide a checkpoint number or 'lobbypoint'.");
						return true;
					}
				} catch (NumberFormatException ex) {
					// Was not 'lobbypoint', or a legal number.
					sender.sendMessage(Mzr.messagingPrefix + "Invalid teleport point. Provide a checkpoint number or 'lobbypoint'.");
					return true;
				}
			}

			cf.exceptionally((ex) -> {
				ex.printStackTrace();
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					sender.sendMessage(Mzr.messagingPrefix + "Error setting " + objectiveName + "(" + objectiveCheckpoint + ")");
				});
				return null;
			});
			cf.thenAccept((checkpointIndex) -> {
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					if (checkpointIndex instanceof Integer) {
						sender.sendMessage(Mzr.messagingPrefix + "Checkpoint " + objectiveName + "(" + checkpointIndex + ") succesfully set.");
					} else {
						sender.sendMessage(Mzr.messagingPrefix + "Checkpoint lobbypoint succesfully set.");
					}
				});
			});
			return true;
		}

		/**
		 * mzra remove [#]
		 * Remove an existing checkpoint
		 */
		if (objectiveAction.equalsIgnoreCase("remove")) {
			Integer index = Integer.valueOf(objectiveCheckpoint);

			if (index < 0 || index >= objective.getCheckpoints().size()) {
				sender.sendMessage(Mzr.messagingPrefix + "Objective " + objectiveName + " has no checkpoint " + objectiveCheckpoint);
				return true;
			}

			CompletableFuture<Void> cf = objective.removeCheckpoint(index);
			cf.exceptionally((ex) -> {
				ex.printStackTrace();
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					sender.sendMessage(Mzr.messagingPrefix + "Error removing " + objectiveName + "(" + objectiveCheckpoint + ")");
				});
				return null;
			});
			cf.thenAccept((checkpointIndex) -> {
				Mzr._this().getServer().getScheduler().runTask(Mzr._this(), () -> {
					sender.sendMessage(Mzr.messagingPrefix + "Sucesfully deleted " + objectiveName + "(" + objectiveCheckpoint + ")");
				});
			});

			return true;
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		ArrayList<String> opts = new ArrayList<String>();
		String prefix = "";
		
		if (!sender.hasPermission("mzr.admin")) { return opts; }

		if (args.length == 1) {
			opts.add("create");
			opts.add("delete");

			opts.addAll(Mzr._this().getObjectiveManager().getActiveObjectiveNames());

			prefix = args[0];

		} else if(args.length == 2) {
			// Check objective name is valid before returning options
			Objective obj = Mzr._this().getObjectiveManager().getObjective(args[0]);
			if (obj == null) { return opts; }

			opts.add("get");
			opts.add("set");
			opts.remove("remove");
			prefix = args[1];

		} else if (args.length == 3) {
			// Check objective name is valid before returning options
			Objective obj = Mzr._this().getObjectiveManager().getObjective(args[0]);
			if (obj == null) { return opts; }

			if (args[1].equalsIgnoreCase("get")) {
				Integer numCheckpoints = obj.getCheckpoints().size();
				for (Integer i=0; i<numCheckpoints; i++) {
					opts.add(i.toString());
				}
				if (obj.getLobbyPoint() != null) {
					opts.add("lobbypoint");
				}
			} else if(args[1].equalsIgnoreCase("set")) {
				Integer numCheckpoints = obj.getCheckpoints().size();
				Integer i = 0;
				for (; i<numCheckpoints; i++) {
					opts.add(i.toString());
				}
				opts.add(i.toString());
				opts.add("lobbypoint");

			} else if (args[1].equalsIgnoreCase("remove")) {
				Integer numCheckpoints = obj.getCheckpoints().size();
				for (Integer i=0; i<numCheckpoints; i++) {
					opts.add(i.toString());
				}
			}

		}

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

	private Boolean objectiveExists(String name) {
		return Mzr._this().getObjectiveManager().getObjective(name) == null;
	}
}