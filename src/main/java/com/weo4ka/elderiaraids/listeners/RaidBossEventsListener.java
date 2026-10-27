package com.weo4ka.elderiaraids.listeners;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.weo4ka.elderiaraids.managers.LootManager;
import com.weo4ka.elderiaraids.managers.LootMenuManager;
import com.weo4ka.elderiaraids.managers.RaidManager;
import com.weo4ka.elderiaraids.utils.RaidData;

public class RaidBossEventsListener implements Listener{

    private final RaidManager raidManager;
    private final LootManager lootManager;
    private final LootMenuManager lootMenuManager;


    public RaidBossEventsListener(RaidManager raidManager, LootManager lootManager, LootMenuManager lootMenuManager) {
        this.raidManager = raidManager;
        this.lootManager = lootManager;
        this.lootMenuManager = lootMenuManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBossKilled(EntityDeathEvent event) {
        RaidData data = raidManager.getCurrentRaidData();
        if (event.getEntity().getUniqueId() != raidManager.getCurrentRaidBossUUID()) return;

        Map<UUID, Double> damageMap = raidManager.getDamageMap();

        for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
            UUID playerUUID = entry.getKey();
            double playerDamage = entry.getValue();

            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                List<ItemStack> offlineLoot = lootManager.generateLootFromPool(data.getBossConfigKey(), (int) playerDamage);
                if (!offlineLoot.isEmpty()) lootManager.saveLeftoverToStashForOffline(playerUUID, offlineLoot);
                continue;
            }

            List<ItemStack> loot = lootManager.generateLootFromPool(data.getBossConfigKey(), (int) playerDamage);
            lootMenuManager.openLootMenu(player, loot);
        }
        raidManager.resetRaid();
    }

    @EventHandler 
    public void onMythicBossHit(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        Player damager = resolvePlayer(event.getDamager());
        if (damager == null) return;

        if (target.getUniqueId() == raidManager.getCurrentRaidBossUUID() && target.getWorld().getName().equalsIgnoreCase(raidManager.getCurrentRaidData().getRaidWorldName())) {
            raidManager.addDamage(damager.getUniqueId(), event.getDamage());
        }
    }

    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        if (entity instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) return player;
        return null;
    }
}
