package com.weo4ka.elderiaraids.listeners;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.weo4ka.elderiaraids.managers.LootManager;
import com.weo4ka.elderiaraids.managers.LootMenuManager;
import com.weo4ka.elderiaraids.managers.RaidDamageManager;
import com.weo4ka.elderiaraids.managers.RaidManager;
import com.weo4ka.elderiaraids.utils.RaidData;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.core.mobs.ActiveMob;

public class RaidBossEventsListener implements Listener{

    private final RaidManager raidManager;
    private final LootManager lootManager;
    private final LootMenuManager lootMenuManager;
    private final RaidDamageManager raidDamageManager;


    public RaidBossEventsListener(RaidManager raidManager, LootManager lootManager, LootMenuManager lootMenuManager, RaidDamageManager raidDamageManager) {
        this.raidManager = raidManager;
        this.lootManager = lootManager;
        this.lootMenuManager = lootMenuManager;
        this.raidDamageManager = raidDamageManager;
    }

    @EventHandler 
    public void onBossKilled(MythicMobDeathEvent event) {
        RaidData data = raidManager.getNextRaidData();
        if (event.getKiller().getWorld().getName() != data.getRaidWorldName()) {
            return;
        }

        if (event.getMobType().getInternalName() != data.getBossId()) {
            return;
        }
        Map<UUID, Double> damageMap = raidDamageManager.getDamageMap();

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

        raidDamageManager.clearDamageMap();
        raidManager.rollNextRaid();
    }

    @EventHandler 
    public void onMythicBossHit(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        Entity damager = event.getDamager();

        if (damager instanceof Player player && MythicBukkit.inst().getMobManager().isMythicMob(target)) {
            Optional<ActiveMob> mob = MythicBukkit.inst().getMobManager().getActiveMob(target.getUniqueId());
            if (mob.isPresent()) {
                ActiveMob boss = mob.get();
                if (boss.getType().getInternalName() == raidManager.getNextRaidData().getBossId()) {
                    double damage = event.getDamage();
                    raidDamageManager.addDamage(player.getUniqueId(), damage);
                }
            }
        }

    }
}
