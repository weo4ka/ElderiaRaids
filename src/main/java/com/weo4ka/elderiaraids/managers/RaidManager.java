package com.weo4ka.elderiaraids.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.weo4ka.elderiaraids.ElderiaRaids;
import com.weo4ka.elderiaraids.utils.RaidData;

public class RaidManager {
    private long cooldown;
    private RaidData currentRaidData;
    private Map<String, RaidData> registeredBosses = new HashMap<>();

    private final ElderiaRaids plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Object fileLock = new Object();

    public RaidManager(ElderiaRaids plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile(); // Просто создаем пустой файл на диске
            } catch (IOException e) {
                plugin.getLogger().severe("Failed creating data.yml file!");
                e.printStackTrace();
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadRaidConfig();

        this.cooldown = this.dataConfig.getLong("global-raid-data.cooldown", 0);
        String currentBossId = this.dataConfig.getString("global-raid-data.next-boss");

        if (currentBossId != null && !currentBossId.trim().isEmpty()) {
            this.currentRaidData = getRaidDataByKey(currentBossId);
        }
    }

    public void setCooldown(int seconds) {
        this.cooldown = System.currentTimeMillis() + (seconds * 1000);

        synchronized (fileLock) {
            dataConfig.set("global-raid-data.cooldown", this.cooldown);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    dataConfig.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed writing cooldown to data.yml");
                    e.printStackTrace();
                }
            });
        }
    }

    public void rollNextBoss() {
        ArrayList<RaidData> bosses = new ArrayList<>(getRegisteredBosses().values());
        if (bosses.isEmpty()) {
            plugin.getLogger().warning("Failed choosing next boss: List is empty");
            this.currentRaidData = null;
            return;
        }

        this.currentRaidData = bosses.get(ThreadLocalRandom.current().nextInt(bosses.size()));

        synchronized (fileLock) {
            dataConfig.set("global-raid-data.next-boss", currentRaidData != null ? currentRaidData.getBossConfigKey() : null);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    dataConfig.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed writing next-boss to data.yml");
                    e.printStackTrace();
                }
            });
        }
    }

    public boolean isRaidOnCooldown() {
        return System.currentTimeMillis() < this.cooldown;
    }

    public long howMuchCooldownLeft() {
        if (!isRaidOnCooldown()) { return 0; }
        return (this.cooldown - System.currentTimeMillis()) / 1000;
    }

    public RaidData getNextRaidData() { return this.currentRaidData; }

    private void loadRaidConfig() {
        registeredBosses.clear(); //Clearing mainly for /raids reload

        File file = new File(plugin.getDataFolder(), "raids.yml");

        if (!file.exists()) {
            plugin.saveResource("raids.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("raids");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection boss = section.getConfigurationSection(key);
            if (boss == null) { continue; }

            String raidWorldName = boss.getString("world");
            String bossId = boss.getString("mythic-boss-id");
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
