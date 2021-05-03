package net.peacefulcraft.mzr.objective;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.peacefulcraft.mzr.Mzr;
import net.peacefulcraft.mzr.config.YAMLFileFilter;

public class ObjectiveManager {
	
	private List<String> objectiveNames;
	private List<String> activeObjectiveNames;
		public List<String> getActiveObjectiveNames() {
			synchronized(this.activeObjectiveNames) {
				return Collections.unmodifiableList(new ArrayList<String>(this.activeObjectiveNames));
			}
		}

	private Map<String, Objective> objectives;

	public ObjectiveManager() {
		this.objectiveNames = Collections.synchronizedList(new ArrayList<String>());
		this.activeObjectiveNames = Collections.synchronizedList(new ArrayList<String>());
		this.objectives = Collections.synchronizedMap(new HashMap<String, Objective>());

		this.loadObjectives();
	}

	/**
	 * Method is intended to be used during object initialization only. Initialization is a SYNC process.
	 * Calling this method after the plugin onEnable() has emitted may cause ConccurrentModificationExceptions.
	 */
	private void loadObjectives() {
		File objectivesFolder = new File(Mzr._this().getDataFolder().toString() + "/objectives");
		if (!objectivesFolder.exists() || !objectivesFolder.isDirectory()) {
			Mzr._this().logWarning("Found no objectives folder. Skipping objective loading. If you have not configured any objectives this is expected.");
			return;
		}

		File[] objectiveFiles = objectivesFolder.listFiles(new YAMLFileFilter());
		if (objectiveFiles.length == 0) {
			Mzr._this().logWarning("Found no objective configuration files in objective configration folder. If you have not configured any objectives this is expected.");
			return;
		}

		for(File f : objectiveFiles) {
			Mzr._this().logDebug("Attempting to load objective configuration file at " + f.getPath());
			String objectiveName = YAMLFileFilter.removeExtension(f.getName());
			this.objectiveNames.add(objectiveName);
			try {
				this.objectives.put(YAMLFileFilter.removeExtension(f.getName()), new Objective(f.getName()));
				this.activeObjectiveNames.add(objectiveName);
				Mzr._this().logDebug("Finished load sequence for configuration for objective " + objectiveName);
			} catch (Exception ex) {
				ex.printStackTrace();
				Mzr._this().logSevere("Error loading configuration file for objective " + objectiveName);
			}
		}
	}

	/**
	 * Create a new objective in memory and on disk. Checks for duplicates an errors if an objective with
	 * the given name already exists.
	 * @param name Name of objective to create.
	 * @return New Objective.
	 * @throws RuntimeException Objective with the given name already exists.
	 */
	public CompletableFuture<Objective> createNewObjective(String name) throws RuntimeException {
		return CompletableFuture.supplyAsync(() -> {
			try {
				if (this.objectiveFileExists(name).get()) {
					throw new RuntimeException("An objective with name " + name + " already exists.");
				}
			} catch (InterruptedException | ExecutionException ex) {
				ex.printStackTrace();
				throw new RuntimeException("Error checking for existing objective file.", ex);
			}

			Objective objective = new Objective(name);
			
			this.objectives.put(name, objective);
			this.activeObjectiveNames.add(name);
			this.objectiveNames.add(name);
			
			return objective;
		});
	}

	/**
	 * Fetch an active Objective object with the given name.
	 * @param objective Name of the Objective to fetch.
	 * @return Objective object or null of no Objective by the given name exists.
	 */
	public Objective getObjective(String objective) {
		return this.objectives.get(objective);
	}

	/**
	 * Asynchroniosuly delete an existing Objective.
	 * @param name Objective to delete.
	 * @return A CompletableFuture is returned for accounting. Future will complete once file has actaully been deleted.
	 */
	public CompletableFuture<Void> deleteObjective(String name) {
		return CompletableFuture.runAsync(() -> {
			this.objectives.remove(name).getConfigFile().delete();
			
			synchronized(this.activeObjectiveNames) {
				Iterator<String> names = this.activeObjectiveNames.iterator();
				while (names.hasNext()) {
					if (name.equalsIgnoreCase(names.next())) {
						names.remove();
						break;
					}
				}
			}

			synchronized(this.objectiveNames) {
				Iterator<String> names = this.objectiveNames.iterator();
				while (names.hasNext()) {
					if (name.equalsIgnoreCase(names.next())) {
						names.remove();
						break;
					}
				}
			}
		});
	}

	/**
	 * Look at all matched objective configuration files on disk from load sequence to avoid overwriting
	 * an objective configuration file if an error occured during load and the objective didn't make it into
	 * the HashMap of loaded objectives. This is a superset of getObjectives().keyset().
	 * @param name Objective name to check for (no extension).
	 * @return True if an objective by that name exists, false otherwise.
	 */
	private CompletableFuture<Boolean> objectiveFileExists(String name) {
		return CompletableFuture.supplyAsync(() -> {
			synchronized(this.objectiveNames) {
				for (String i : this.objectiveNames) {
					if (name.equalsIgnoreCase(i)) { return true; }
				}
				return false;
			}
		});
	}
}
