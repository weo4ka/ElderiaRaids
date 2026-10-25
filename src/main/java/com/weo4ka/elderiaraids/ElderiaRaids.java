package com.weo4ka.elderiaraids;

import org.bukkit.plugin.java.JavaPlugin;

import com.weo4ka.elderiaraids.listeners.MovedToRaidWorldListener;
import com.weo4ka.elderiaraids.listeners.RaidBossKilledListener;
import com.weo4ka.elderiaraids.managers.PluginManager;
import com.weo4ka.elderiaraids.utils.RaidCooldown;


public class ElderiaRaids extends JavaPlugin {
    
    public static String raid_world_name = "RAID_WORLD";
    public static String next_boss_id = "ravagerfallen";
    public static double bossSpawnLocationX = -76.5;
    public static double bossSpawnLocationY = 47.0;
    public static double bossSpawnLocationZ = -80.5;
    public static float bossSpawnYaw = (float) 180.0;
    public static float bossSpawnPitch = (float) 0.0;
    public static double bossLevel = 10.0;

    private RaidCooldown cd;

    @Override
    public void onEnable() {
        
        // Initialize managers
        PluginManager.getInstance().initialize();
        this.cd = new RaidCooldown(this);

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new MovedToRaidWorldListener(), this);
        this.getServer().getPluginManager().registerEvents(new RaidBossKilledListener(), this);
        
        getLogger().info(getDescription().getName() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(getDescription().getName() + " has been disabled!");
    }
    
}
