package com.weo4ka.elderiaraids.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaidDamageManager {
    private final Map<UUID, Double> damageMap = new HashMap<>();

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
}
