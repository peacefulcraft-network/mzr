package net.peacefulcraft.mzr.objective;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataManager {
	private Map<UUID, ObjectiveProgress> data;
	
	public DataManager() {
		this.data = Collections.synchronizedMap(new HashMap<UUID, ObjectiveProgress>());
	}

	public CompletableFuture<ObjectiveProgress> getData(UUID entity) {
		return CompletableFuture.supplyAsync(() -> {
			if (this.data.containsKey(entity)) {
				return this.data.get(entity);
			}

			ObjectiveProgress progress = new ObjectiveProgress(entity);
			this.data.put(entity, progress);
			return progress;
		});
	}

	public CompletableFuture<Void> flushAndEvictData(UUID entity) {
		return CompletableFuture.runAsync(() -> {
			ObjectiveProgress progress = this.data.remove(entity);
			if (progress == null) { return; }

			progress.save().join();
		});
	}
}
