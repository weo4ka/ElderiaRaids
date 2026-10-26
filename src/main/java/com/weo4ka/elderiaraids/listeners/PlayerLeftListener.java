package com.weo4ka.elderiaraids.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.weo4ka.elderiaraids.managers.LootManager;

public class PlayerLeftListener implements Listener{
    private final LootManager manager;

    public PlayerLeftListener(LootManager manager) {
        this.manager = manager;
    }

    @EventHandler 
    public void onPlayerLeft(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        manager.unloadPlayer(player.getUniqueId());
    }
}
