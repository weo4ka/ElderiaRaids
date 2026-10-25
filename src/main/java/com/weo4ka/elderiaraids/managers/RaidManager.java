package com.weo4ka.elderiaraids.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.weo4ka.elderiaraids.ElderiaRaids;
import com.weo4ka.elderiaraids.utils.RaidData;

public class RaidManager {
    private long cooldown;
    private RaidData currentRaidData;

    private final ElderiaRaids plugin;
    private final File file;
    private final FileConfiguration config;
    private final Random random = new Random();

    public RaidManager(ElderiaRaids plugin) {
        this.plugin = plugin;

        this.file = new File(plugin.getDataFolder(), "data.yml");

        if (!this.file.getParentFile().exists()) {
            this.file.getParentFile().mkdirs();
        }

        this.config = YamlConfiguration.loadConfiguration(file);

        this.cooldown = this.config.getLong("global-raid-data.cooldown", 0);
        String currentBossId = this.config.getString("global-raid-data.next-boss");

        if (currentBossId != null || currentBossId != "") {
            this.currentRaidData = plugin.getRaidDataByKey(currentBossId);
        }
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

    public void rollNextBoss() {
        ArrayList<RaidData> bosses = new ArrayList<>(plugin.getRegisteredBosses().values());

        this.currentRaidData = bosses.get(random.nextInt(bosses.size()));

        config.set("global-raid-data.next-boss", currentRaidData != null ? currentRaidData.getBossConfigKey() : null);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                config.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed writing next-boss to data.yml");
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

    public RaidData getNextRaidData() { return this.currentRaidData; }
}
