package net.peacefulcraft.mzr.objective;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;

import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.config.Configuration;

public class Objective extends Configuration {
	
	private String name;
		public String getName() { return this.name; }

	private Location lobbypoint;
		public Location getLobbyPoint() { return this.lobbypoint; }

	private List<Location> checkpoints;
		public List<Location> getCheckpoints() { return Collections.unmodifiableList(this.checkpoints); }
		public Location getCheckpoint(Integer i) throws IndexOutOfBoundsException { return this.checkpoints.get(i); }

	public Objective(String name) {
		super("objectives/" + name + ".yml");

		this.name = name;
		this.checkpoints = Collections.synchronizedList(new ArrayList<Location>());
		this.loadValues();
	}

	/**
	 * Method is intended to be used during object initialization only. Initialization is a SYNC process.
	 * Calling this method outside of the constructor may throw ConcurrentModificationExceptions
	 */
	private void loadValues() {
		this.lobbypoint = this.config.getLocation("lobbypoint");
		List<Map<?, ?>> pointMap = this.config.getMapList("checkpoints");

		pointMap.forEach(loc -> {
			World w = Mzr._this().getServer().getWorld((String) loc.get("world"));
			if (w == null) {
				Mzr._this().logWarning("Objective " + name + " had invalid checkpoint location. Ignoring.");
				
				// Add null to keep correct CP indexes for remaining CPs
				checkpoints.add(null);
			}

			this.checkpoints.add(
				new Location(
					w,
					(Double) loc.get("x"),
					(Double) loc.get("y"),
					(Double) loc.get("z")
				)
			);
		});
	}

	/**
	 * Save the checkpoints on this objective.
	 * @return
	 */
	private CompletableFuture<Boolean> saveCheckpoints() {
		// Make sure file i/o happens off the Bukkit Thread
		return CompletableFuture.supplyAsync(() -> {
			try {
				synchronized(this.checkpoints) {
					this.config.set("checkpoints", this.checkpoints);
				}

				synchronized(this) {
					this.config.set("lobbypoint", this.lobbypoint);
				}

				synchronized(this.config) {
					this.config.save(this.configFile);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				Mzr._this().logSevere("Error saving objective " + this.name  + "'s configuration file.");
				return false;
			}
			return true;
		});
	}

	/**
	 * Asynchoniously set the lobby teleport point for this objective
	 * @param lobbypoint New lobby teleport location
	 * @return A CompletableFuture that completes once the change is commited to disk
	 */
	public CompletableFuture<Void> setLobbypoint(Location lobbypoint) {
		return CompletableFuture.runAsync(() -> {
			synchronized(this) {
				this.lobbypoint = lobbypoint;
			}
			this.saveCheckpoints().join();
		});
	}

	/**
	 * Asynchroniously adds new checkpoint to this objective.
	 * @param checkpoint Checkpoint to add.
	 * @return Index of new checkpoint. Returns -1 if an error occured.
	 */
	public CompletableFuture<Integer> addCheckpoint(Location checkpoint) {
		/**
		 * File IO is async so it is best to make the sync happen on another thread to prevent blocking Minecraft.
		 * CompletableFuture allows caller to get feedback on whether result is comitted to disk or not.
		 */
		return CompletableFuture.supplyAsync(() -> {
			this.checkpoints.add(checkpoint);

			if (this.saveCheckpoints().join()) {
				return this.checkpoints.size() - 1;
			}

			return -1;
		});
	}

	/**
	 * Asynchroniously adds new checkpoint to this objective.
	 * @param i Index at which to insert the checkpoint.
	 * @param checkpoint Checkpoint to add.
	 * @return Index of new checkpoint (will match i). Returns -1 if an error occured.
	 */
	public CompletableFuture<Integer> addCheckpoint(Integer i, Location checkpoint) {
		/**
		 * File IO is async so it is best to make the sync happen on another thread to prevent blocking Minecraft.
		 * CompletableFuture allows caller to get feedback on whether result is comitted to disk or not.
		 */
		return CompletableFuture.supplyAsync(() -> {
			this.checkpoints.add(i, checkpoint);

			if (this.saveCheckpoints().join()) {
				return this.checkpoints.size() - 1;
			}

			return -1;
		});
	}

	/**
	 * Update the checkpoint at index i.
	 * @param i Index to update.
	 * @param checkpoint Checkpoint location.
	 * @return True on success, false if an error occured during file i/o.
	 */
	public CompletableFuture<Boolean> setCheckpoint(Integer i, Location checkpoint) {
		/**
		 * File IO is async so it is best to make the sync happen on another thread to prevent blocking Minecraft.
		 * CompletableFuture allows caller to get feedback on whether result is comitted to disk or not.
		 */
		return CompletableFuture.supplyAsync(() -> {
			this.checkpoints.set(i, checkpoint);

			if (this.saveCheckpoints().join()) {
				return true;
			}

			return false;
		});
	}

	/**
	 * Remove the checkpoint at index i.
	 * @param i Index to remove.
	 * @return True on success, false if an error occured during file i/o.
	 */
	public void removeCheckpoint(Integer i) {
		this.checkpoints.remove((int) i);
		this.saveCheckpoints();
	}
}
