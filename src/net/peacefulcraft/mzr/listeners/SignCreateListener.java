package net.peacefulcraft.mzr.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import net.md_5.bungee.api.ChatColor;
import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.objective.Objective;

/**
 * Detect sign placements and validate them if they appear to be MZR signs
 */
public class SignCreateListener implements Listener {
	
	@EventHandler
	public void onSignPlace(SignChangeEvent ev) {
		if (ev.getLines().length == 0) { return; }

		if (ChatColor.stripColor(ev.getLine(0)).equalsIgnoreCase("[MZR]")) {
			Objective objective = null;
			try {
				objective = Mzr._this().getObjectiveManager().getObjective(ChatColor.stripColor(ev.getLine(1)));
			} catch (IndexOutOfBoundsException ex) {}
			if (objective == null) {
				ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Sign appears to be an MZR sign, but objective " + ev.getLine(1) + " is not recognized.");
				return;
			}

			String teleportTarget = "-1";
			try {
				teleportTarget = ChatColor.stripColor(ev.getLine(2));
			} catch (IndexOutOfBoundsException ex) {}
			if (teleportTarget.equalsIgnoreCase("resume")) {
				ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Sign syntax is valid.");
			} else {
				try {
					objective.getCheckpoint(Integer.valueOf(teleportTarget));
					ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Sign syntax is valid.");
				} catch (IndexOutOfBoundsException ex) {
					ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Sign appears to be an MZR sign for objective " + objective.getName() + ", but checkpoint " + ev.getLine(2) + " is not known.");
				}
			}
		}
	}
}
