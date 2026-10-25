package com.weo4ka.elderiaraids.utils;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class RaidCooldown {
    private long cooldown = 0;
    
    private final JavaPlugin plugin;
    private final File file;
    private final FileConfiguration config;

    public RaidCooldown(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");

        if (!this.file.getParentFile().exists()) {
            this.file.getParentFile().mkdirs();
        }

        this.config = YamlConfiguration.loadConfiguration(file);

        this.cooldown = this.config.getLong("global-raid-data.cooldown", 0);
    }

    public void setCooldown(int seconds) {
        this.cooldown = System.currentTimeMillis() + (seconds * 1000);

        config.set("global-raid-data.cooldown", this.cooldown);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                config.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed writing cooldown to data.yml");
                e.printStackTrace();
            }
        });
    }

    public boolean isRaidOnCooldown() {
        return System.currentTimeMillis() < this.cooldown;
    }

    public long howMuchCooldownLeft() {
        if (!isRaidOnCooldown()) { return 0; }
        return (this.cooldown - System.currentTimeMillis()) / 1000;
    }
}
