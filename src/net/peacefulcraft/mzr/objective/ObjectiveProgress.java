package net.peacefulcraft.mzr.objective;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.config.Configuration;

public class ObjectiveProgress extends Configuration {

	private UUID entity;

	private Map<String, Map<String, String>> objectives;
		public Map<String, String> getObjectiveProgress(String objective) { return this.objectives.get(objective); }

		/**
		 * Replace objective progress data map for this profile.
		 * To update an existing value, use getObjectiveProgress().set(), then call .save()
		 * @param objective
		 * @param data
		 */
		public void setObjectiveProgress(String objective, Map<String, String> data) { this.objectives.put(objective, data); }

	
	/**
	 * Performs blocking i/o on calling thread
	 * @param entity Id of entity who's profile should be loaded
	 * @throws RuntimeException On error loading objective progress data
	 */
	public ObjectiveProgress(UUID entity) throws RuntimeException {
		super("progress/" + entity.toString() + ".yml");

		this.entity = entity;
		this.loadValues();
		if (this.objectives == null) {
			this.objectives = new HashMap<String, Map<String, String>>();
		}
	}

	/**
	 * Pull objective progress data out of YamlConfiguration
	 * @throws RuntimeException When loaded YAML structure does match expected format
	 * 
	 * Supress unchecked cast because this is the easiest way to get the value out of the YAML class.
	 * The alternative involves 2 iterattors and still makes assumptions about values on the inner map.
	 */
	@SuppressWarnings("unchecked")
	private void loadValues() throws RuntimeException {
		try {
			this.objectives = (Map<String, Map<String, String>>) this.config.get("objectives");
		} catch (ClassCastException ex) {
			ex.printStackTrace();
			Mzr._this().logSevere("Error loading objective progress for entity " + this.entity.toString() + ". Is save file corrupt?");
			throw new RuntimeException("Error loading objective progress for entity " + this.entity.toString() + ". Is save file corrupt?" , ex);
		}
	}

	/**
	 * Async save data to disk
	 * @return True on success, false if i/o error occured
	 */
	public CompletableFuture<Boolean> save() {
		this.config.set("objectives", this.objectives);

		return CompletableFuture.supplyAsync(() -> {
			try {
				synchronized(this.config) {
					this.config.save(this.configFile);
				}
				return true;

			} catch (IOException ex) {
				ex.printStackTrace();
				Mzr._this().logSevere("Error saving objective progress data for entity " + entity.toString());
				return false;
			}
		});
	}
}
