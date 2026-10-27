package com.weo4ka.elderiaraids.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.weo4ka.elderiaraids.ElderiaRaids;
import com.weo4ka.elderiaraids.utils.RaidData;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.DespawnMode;

public class RaidManager {
    private long cooldown;
    private RaidData currentRaidData;
    private Map<String, RaidData> registeredBosses = new HashMap<>();
    private final Map<UUID, Double> damageMap = new HashMap<>();
    private Map<RaidData, List<UUID>> raidToPlayersMap = new HashMap<>();
    private Map<RaidData, UUID> raidToBossMap = new HashMap<>();

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

        plugin.getLogger().info("Set cooldown on: " + seconds);

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

    public void rollNextRaid() {
        ArrayList<RaidData> bosses = new ArrayList<>(getRegisteredBosses().values());
        if (bosses.isEmpty()) {
            plugin.getLogger().warning("Failed choosing next boss: List is empty");
            this.currentRaidData = null;
            return;
        }

        this.currentRaidData = bosses.get(ThreadLocalRandom.current().nextInt(bosses.size()));

        setCooldown(currentRaidData.getBossCooldown());
        plugin.getLogger().info("Rolled boss: " + this.currentRaidData.getBossConfigKey());

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

    public RaidData getCurrentRaidData() { return this.currentRaidData; }

    public void loadRaidConfig() {
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

    public void enterOrStartRaid(UUID uuid) {
        World raid_world = Bukkit.getWorld(getCurrentRaidData().getRaidWorldName());
        Player player = Bukkit.getPlayer(uuid);

        if (isRaidOnCooldown() || isPlayerInRaid(player)) return;

        if (getCurrentRaidBossUUID() == null || Bukkit.getEntity(getCurrentRaidBossUUID()) == null) {
            ActiveMob mob = MythicBukkit.inst().getMobManager().spawnMob(getCurrentRaidData().getBossId(), getCurrentRaidData().getSpawnLocation());
            addBossToRaidToBossMap(mob);
            mob.setLevel(getCurrentRaidData().getBossLevel());
            mob.setDespawnMode(DespawnMode.NEVER);
        }

        addPlayerToActiveRaid(player);
        player.performCommand("world teleport world:" + raid_world.getName().toLowerCase(Locale.ROOT));
    }

    public void leaveRaid(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (!isPlayerInRaid(player)) return;

        removePlayerFromActiveRaid(player);

        player.performCommand("spawn");
    }

    public void addDamage(UUID uuid, double damage) {
        if (damage <= 0) return;
        damageMap.put(uuid, damageMap.getOrDefault(uuid, 0.0) + damage);
    }

    public double getPlayerDamage(UUID uuid) {
        return damageMap.getOrDefault(uuid, 0.0);
    }

    public Map<UUID, Double> getDamageMap() {
        return damageMap;
    }

    public void clearDamageMap() {
        damageMap.clear();
    }

    public void addPlayerToActiveRaid(Player player) {
        raidToPlayersMap.get(getCurrentRaidData()).add(player.getUniqueId());
    }

    public void removePlayerFromActiveRaid(Player player) {
        raidToPlayersMap.get(getCurrentRaidData()).remove(player.getUniqueId());
    }

    public List<Player> getPlayersInActiveRaid() {
        if (raidToPlayersMap.isEmpty()) return new ArrayList<Player>();
        List<Player> players = new ArrayList<Player>();
        for (UUID uuid : raidToPlayersMap.get(getCurrentRaidData())) {
            players.add(Bukkit.getPlayer(uuid));
        }
        return players;
    }

    public RaidData getPlayerRaid(Player player) {
        if (raidToPlayersMap.get(getCurrentRaidData()).contains(player.getUniqueId())) return getCurrentRaidData();
        else return null;
    }

    public boolean isPlayerInRaid(Player player) {return getPlayerRaid(player) != null;}
    public void clearRaidToPlayersMap() {raidToPlayersMap.clear();}
    public void addRaidToRaidToPlayerMap(RaidData raid) {raidToPlayersMap.put(raid, new ArrayList<UUID>());}

    public void addBossToRaidToBossMap(ActiveMob boss) {raidToBossMap.put(getCurrentRaidData(), boss.getUniqueId());}
    public UUID getCurrentRaidBossUUID() {return raidToBossMap.get(getCurrentRaidData());}
    public void clearRaidToBossUUIDMap() {raidToBossMap.clear();}
}
