package com.weo4ka.elderiaraids;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;

import com.weo4ka.elderiaraids.commands.RaidsCommand;
import com.weo4ka.elderiaraids.listeners.LootMenuListener;
import com.weo4ka.elderiaraids.listeners.PlayerRaidListener;
import com.weo4ka.elderiaraids.listeners.RaidBossEventsListener;
import com.weo4ka.elderiaraids.managers.PluginManager;
import com.weo4ka.elderiaraids.utils.RaidData;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;


public class ElderiaRaids extends JavaPlugin {
    public Map<String, RaidData> registeredBosses = new HashMap<>();

    @Override
    public void onEnable() {
        //Load our configs
        saveDefaultConfig();

        // Initialize managers
        PluginManager.getInstance().initialize(this);

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new RaidBossEventsListener(
                                                            PluginManager.getInstance().getRaidManager(), 
                                                            PluginManager.getInstance().getLootManager(), 
                                                            PluginManager.getInstance().getLootMenuManager()), this);
        this.getServer().getPluginManager().registerEvents(new LootMenuListener(
                                                            PluginManager.getInstance().getLootMenuManager(),
                                                            PluginManager.getInstance().getRaidManager()), this);
        this.getServer().getPluginManager().registerEvents(new PlayerRaidListener(
                                                            PluginManager.getInstance().getLootManager(),
                                                            PluginManager.getInstance().getRaidManager()), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(RaidsCommand.createCommand("raids"));
        });

        getLogger().info(getName() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(getName() + " has been disabled!");
    }
    
}
