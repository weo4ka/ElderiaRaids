package com.weo4ka.elderiaraids.listeners;

import java.util.Collection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import com.weo4ka.elderiaraids.managers.RaidManager;
import com.weo4ka.elderiaraids.utils.RaidData;

import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;

public class MovedToRaidWorldListener implements Listener {

    private final RaidManager manager;

    public MovedToRaidWorldListener(RaidManager manager) {
        this.manager = manager;
    }

    @EventHandler 
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        RaidData data = manager.getNextRaidData();
        if (event.getPlayer().getWorld().getName() == data.getRaidWorldName()) {
            Collection<ActiveMob> activeMobs = MythicBukkit.inst().getMobManager().getActiveMobs(am -> am.getMobType().equals(data.getBossId()));

            if (activeMobs.isEmpty() && !manager.isRaidOnCooldown()) {
                MythicMob boss = MythicBukkit.inst().getMobManager().getMythicMob(data.getBossId()).orElse(null);
                if (boss != null) {
                    boss.spawn(BukkitAdapter.adapt(data.getSpawnLocation()), data.getBossLevel());
                }
            }
        }
    }
}
