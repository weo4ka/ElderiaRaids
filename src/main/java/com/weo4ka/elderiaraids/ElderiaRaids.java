package com.weo4ka.elderiaraids;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;

import com.weo4ka.elderiaraids.listeners.MovedToRaidWorldListener;
import com.weo4ka.elderiaraids.listeners.RaidBossKilledListener;
import com.weo4ka.elderiaraids.managers.PluginManager;
import com.weo4ka.elderiaraids.utils.RaidData;


public class ElderiaRaids extends JavaPlugin {
    public Map<String, RaidData> registeredBosses = new HashMap<>();

    @Override
    public void onEnable() {
        //Load our configs
        saveDefaultConfig();

        // Initialize managers
        PluginManager.getInstance().initialize(this);

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new MovedToRaidWorldListener(PluginManager.getInstance().getRaidManager()), this);
        this.getServer().getPluginManager().registerEvents(new RaidBossKilledListener(PluginManager.getInstance().getRaidManager()), this);
        
        getLogger().info(getName() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(getName() + " has been disabled!");
    }

    
    
}
