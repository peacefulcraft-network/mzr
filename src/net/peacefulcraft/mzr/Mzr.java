package net.peacefulcraft.mzr;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.peacefulcraft.mzr.commands.MzrAdmin;
import net.peacefulcraft.mzr.commands.MzrCommand;
import net.peacefulcraft.mzr.config.MainConfiguration;
import net.peacefulcraft.mzr.listeners.PlayerDisconnectListener;
import net.peacefulcraft.mzr.listeners.SignClickListener;
import net.peacefulcraft.mzr.listeners.SignCreateListener;
import net.peacefulcraft.mzr.objective.DataManager;
import net.peacefulcraft.mzr.objective.ObjectiveManager;
public class Mzr extends JavaPlugin {
	
	public static final String messagingPrefix = ChatColor.GREEN + "[" + ChatColor.BLUE + "MZR" + ChatColor.GREEN + "]" + ChatColor.RESET;
	
	private static Mzr _this;
		public static Mzr _this() { return _this; }
	
	private static MainConfiguration configuration;
		public static MainConfiguration getConfiguration() { return configuration; }

	private ObjectiveManager objectiveManager;
		public ObjectiveManager getObjectiveManager() { return this.objectiveManager; }
	
	private DataManager dataManager;
		public DataManager getDataManager() { return this.dataManager; }
	
	/**
	* Called when Bukkit server enables the plguin
	* For improved reload behavior, use this as if it was the class constructor
	*/
	public void onEnable() {
		_this = this;
		// Save default config if one does not exist. Then load the configuration into memory
		configuration = new MainConfiguration();

		this.objectiveManager = new ObjectiveManager();
		this.dataManager = new DataManager();
		
		this.setupCommands();
		this.setupEventListeners();
	}
	
	public void logDebug(String message) {
		if (configuration.isDebugEnabled()) {
			this.getServer().getLogger().log(Level.INFO, message);
		}
	}
	
	public void logNotice(String message) {
		this.getServer().getLogger().log(Level.INFO, message);
	}
	
	public void logWarning(String message) {
		this.getServer().getLogger().log(Level.WARNING, message);
	}
	
	public void logSevere(String message) { 
		this.getServer().getLogger().log(Level.SEVERE, message);
	}
	
	/**
	* Called whenever Bukkit server disableds the plugin
	* For improved reload behavior, try to reset the plugin to it's initaial state here.
	*/
	public void onDisable () {
		this.getServer().getScheduler().cancelTasks(this);
	}
	
	private void setupCommands() {
		this.getCommand("mzradmin").setExecutor(new MzrAdmin());
		this.getCommand("mzr").setExecutor(new MzrCommand());
	}
	
	private void setupEventListeners() {
		this.getServer().getPluginManager().registerEvents(new SignClickListener(), this);
		this.getServer().getPluginManager().registerEvents(new SignCreateListener(), this);
		this.getServer().getPluginManager().registerEvents(new PlayerDisconnectListener(), this);
	}
}