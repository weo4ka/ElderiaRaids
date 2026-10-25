package com.weo4ka.elderiaraids.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.weo4ka.elderiaraids.ElderiaRaids;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;

public class RaidBossKilledListener implements Listener{

    @EventHandler 
    public void BossKilled(MythicMobDeathEvent event) {
        if (event.getKiller().getWorld().getName() != ElderiaRaids.raid_world_name) {
            return;
        }

        if (event.getMobType().getInternalName() != ElderiaRaids.next_boss_id) {
            return;
        }
        
    }
}
