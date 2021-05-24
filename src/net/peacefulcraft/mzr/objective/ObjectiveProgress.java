package net.peacefulcraft.mzr.objective;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.configuration.ConfigurationSection;

import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.config.Configuration;

public class ObjectiveProgress extends Configuration {

	private UUID entity;

	private Map<String, Map<String, Object>> objectives;
		public Map<String, Object> getObjectiveProgress(String objective) { return this.objectives.get(objective); }

		/**
		 * Replace objective progress data map for this profile.
		 * To update an existing value, use getObjectiveProgress().set(), then call .save()
		 * @param objective
		 * @param data
		 */
		public void setObjectiveProgress(String objective, Map<String, Object> data) { this.objectives.put(objective, data); }

	
	/**
	 * Performs blocking i/o on calling thread
	 * @param entity Id of entity who's profile should be loaded
	 * @throws RuntimeException On error loading objective progress data
	 */
	public ObjectiveProgress(UUID entity) throws RuntimeException {
		super("objective-progress.yml", "progress/" + entity.toString() + ".yml");

		this.entity = entity;
		this.objectives = new HashMap<String, Map<String, Object>>();
		this.loadValues();
	}

	/**
	 * Pull objective progress data out of YamlConfiguration
	 * @throws RuntimeException When loaded YAML structure does match expected format
	 * 
	 */
	private void loadValues() throws RuntimeException {
		try {
			/**
			 * We need to enforce that the top left objectives map is made up of maps.
			 * To do that we need to iterate over the top left because we can't enforce that
			 * principle as casting the Map<String, Object> to Map<String, Map<String, Object>> is not allowed.
			 */
			ConfigurationSection cfs = this.config.getConfigurationSection("objectives");
			
			// No objective data found
			if (cfs == null) { return; }
			
			Set<String> objs = cfs.getKeys(false);
			objs.forEach((objectiveName) -> {
				this.objectives.put(objectiveName, cfs.getConfigurationSection(objectiveName).getValues(true));
			});

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
