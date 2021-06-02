package net.peacefulcraft.mzr.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.peacefulcraft.mzr.Mzr;

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
		if (Mzr._this().getObjectiveManager().getObjective(objectiveName) == null) {
			sender.sendMessage(Mzr.messagingPrefix + objectiveName + " is not a valid objective.");
			return true;
		}

		Mzr._this().getObjectiveManager().playerResumeObjective(p, objectiveName);

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
