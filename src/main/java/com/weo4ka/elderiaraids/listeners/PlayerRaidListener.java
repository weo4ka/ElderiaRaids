package com.weo4ka.elderiaraids.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.weo4ka.elderiaraids.managers.LootManager;
import com.weo4ka.elderiaraids.managers.RaidManager;

public class PlayerRaidListener implements Listener{
    private final LootManager lootManager;
    private final RaidManager raidManager;


    public PlayerRaidListener(LootManager lootManager, RaidManager raidManager) {
        this.lootManager = lootManager;
        this.raidManager = raidManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equalsIgnoreCase(raidManager.getCurrentRaidData().getRaidWorldName())) return;
        if (!raidManager.isPlayerInRaid(player)) return;

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        raidManager.leaveRaid(player.getUniqueId());
    }

    @EventHandler 
    public void onPlayerLeft(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lootManager.unloadPlayer(player.getUniqueId());
    }
}
