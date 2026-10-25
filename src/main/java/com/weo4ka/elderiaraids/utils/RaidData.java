package com.weo4ka.elderiaraids.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class RaidData {
    private final String bossConfigKey;
    private final String raidWorldName;
    private final String bossId;
    private final double bossSpawnLocationX;
    private final double bossSpawnLocationY;
    private final double bossSpawnLocationZ;
    private final float bossSpawnYaw;
    private final float bossSpawnPitch;
    private final double bossLevel;
    private final int bossCooldown;

    public RaidData(String bossConfigKey, String raidWorldName, String bossId, double bossSpawnLocationX, double bossSpawnLocationY, double bossSpawnLocationZ, double bossSpawnYaw, double bossSpawnPitch, double bossLevel, int bossCooldown) {
        this.bossConfigKey = bossConfigKey;
        this.raidWorldName = raidWorldName;
        this.bossId = bossId;
        this.bossSpawnLocationX = bossSpawnLocationX;
        this.bossSpawnLocationY = bossSpawnLocationY;
        this.bossSpawnLocationZ = bossSpawnLocationZ;
        this.bossSpawnYaw = (float)bossSpawnYaw;
        this.bossSpawnPitch = (float)bossSpawnPitch;
        this.bossLevel = bossLevel;
        this.bossCooldown = bossCooldown;
    }

    public Location getSpawnLocation() {
        return new Location(Bukkit.getWorld(raidWorldName), bossSpawnLocationX, bossSpawnLocationY, bossSpawnLocationZ, bossSpawnYaw, bossSpawnPitch);
    }

    public String getBossConfigKey() {
        return bossConfigKey;
    }

    public String getRaidWorldName() {
        return raidWorldName;
    }

    public String getBossId() {
        return bossId;
    }

    public double getBossLevel() {
        return bossLevel;
    }

    public int getBossCooldown() {
        return bossCooldown;
    }

    

}
