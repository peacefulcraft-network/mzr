package net.peacefulcraft.mzr.listeners;

import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.objective.Objective;

/**
 * Detect sign placements and validate them if they appear to be MZR signs
 */
public class SignCreateListener implements Listener {
	
	@EventHandler
	public void onSignPlace(BlockPlaceEvent ev) {
		if (ev.getBlock() instanceof Sign) {
			Sign s = (Sign) ev.getBlock();
			if (s.getLines().length == 0) { return; }

			if (s.getLine(0).contains("[MZR]")) {
				Objective objective = null;
				try {
					objective = Mzr._this().getObjectiveManager().getObjective(s.getLine(1));
				} catch (IndexOutOfBoundsException ex) {}
				if (objective == null) {
					ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Sign appears to be an MZR sign, but objective " + s.getLine(0) + " is not recognized.");
					return;
				}

				String teleportTarget = "-1";
				try {
					teleportTarget = s.getLine(2);
				} catch (IndexOutOfBoundsException ex) {}
				if (teleportTarget.equalsIgnoreCase("resume")) {
					ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Sign syntax is valid.");
				} else {
					try {
						objective.getCheckpoint(Integer.valueOf(teleportTarget));
						ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Sign syntax is valid.");
					} catch (IndexOutOfBoundsException ex) {
						ev.getPlayer().sendMessage(Mzr.messagingPrefix + "Sign appears to be an MZR sign for objective " + objective.getName() + ", but checkpoint " + s.getLine(3) + " is not known.");
					}
				}
			}
		}
	}
}
