package com.weo4ka.elderiaraids.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.weo4ka.elderiaraids.managers.RaidManager;
import com.weo4ka.elderiaraids.utils.RaidData;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;

public class RaidBossKilledListener implements Listener{

    private final RaidManager manager;

    public RaidBossKilledListener(RaidManager manager) {
        this.manager = manager;
    }

    @EventHandler 
    public void onBossKilled(MythicMobDeathEvent event) {
        RaidData data = manager.getNextRaidData();
        if (event.getKiller().getWorld().getName() != data.getRaidWorldName()) {
            return;
        }

        if (event.getMobType().getInternalName() != data.getBossId()) {
            return;
        }
        
        manager.setCooldown(data.getBossCooldown());
        manager.rollNextBoss();
    }
}
