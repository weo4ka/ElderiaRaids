package com.weo4ka.elderiaraids.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.weo4ka.elderiaraids.managers.LootManager;
import com.weo4ka.elderiaraids.managers.LootMenuManager;
import com.weo4ka.elderiaraids.managers.RaidManager;
import com.weo4ka.elderiaraids.utils.RaidData;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;

public class RaidBossKilledListener implements Listener{

    private final RaidManager manager;
    private final LootManager lootManager;
    private final LootMenuManager lootMenuManager;

    public RaidBossKilledListener(RaidManager manager, LootMenuManager lootMenuManager, LootManager lootManager) {
        this.manager = manager;
        this.lootMenuManager = lootMenuManager;
        this.lootManager = lootManager;
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
        
        lootMenuManager.openLootMenu((Player) event.getKiller(), lootManager.generateLootFromPool(data.getBossConfigKey(), 150));

        manager.setCooldown(data.getBossCooldown());
        manager.rollNextBoss();
    }
}
