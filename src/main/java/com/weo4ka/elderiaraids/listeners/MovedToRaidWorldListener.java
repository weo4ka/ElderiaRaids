package com.weo4ka.elderiaraids.listeners;

import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import com.weo4ka.elderiaraids.ElderiaRaids;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;

public class MovedToRaidWorldListener implements Listener {

    @EventHandler 
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (event.getPlayer().getWorld().getName() == ElderiaRaids.raid_world_name) {
            Collection<ActiveMob> activeMobs = MythicBukkit.inst().getMobManager().getActiveMobs(am -> am.getMobType().equals(ElderiaRaids.next_boss_id));

            if (activeMobs.isEmpty()) {
                MythicMob boss = MythicBukkit.inst().getMobManager().getMythicMob(ElderiaRaids.next_boss_id).orElse(null);
                if (boss != null) {
                    Location location = new Location(event.getPlayer().getWorld(), 
                                    ElderiaRaids.bossSpawnLocationX, 
                                    ElderiaRaids.bossSpawnLocationY, 
                                    ElderiaRaids.bossSpawnLocationZ, 
                                    ElderiaRaids.bossSpawnYaw, 
                                    ElderiaRaids.bossSpawnPitch);
                    boss.spawn(BukkitAdapter.adapt(location), ElderiaRaids.bossLevel);
                }
            }
        }
    }
}
