package com.weo4ka.elderiaraids;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
        loadRaidConfig();

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

    private void loadRaidConfig() {
        registeredBosses.clear(); //CLearing mainly for /raids reload

        File file = new File(getDataFolder(), "raids.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            saveResource("raids.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("bosses");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection boss = section.getConfigurationSection(key);
            if (boss == null) { continue; }

            String raidWorldName = boss.getString("world");
            String bossId = boss.getString("boss-id");
            double bossSpawnLocationX = boss.getDouble("location.x");
            double bossSpawnLocationY = boss.getDouble("location.y");
            double bossSpawnLocationZ = boss.getDouble("location.z");
            float bossSpawnYaw = (float) boss.getDouble("location.yaw");
            float bossSpawnPitch = (float) boss.getDouble("location.pitch");
            double bossLevel = boss.getDouble("boss-level");
            int bossCooldown = boss.getInt("boss-cooldown");

            RaidData data = new RaidData(key, raidWorldName, bossId, bossSpawnLocationX, bossSpawnLocationY, bossSpawnLocationZ, bossSpawnYaw, bossSpawnPitch, bossLevel, bossCooldown);

            registeredBosses.put(key, data);
        }
    }

    public Map<String, RaidData> getRegisteredBosses() {
        return registeredBosses;
    }

    public RaidData getRaidDataByKey(String key) {
        return registeredBosses.getOrDefault(key, null);
    }
    
}
