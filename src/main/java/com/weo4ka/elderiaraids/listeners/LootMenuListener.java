package com.weo4ka.elderiaraids.listeners;

import java.util.List;
import java.util.Map;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.weo4ka.elderiaraids.ElderiaRaids;
import com.weo4ka.elderiaraids.managers.LootMenuManager;
import com.weo4ka.elderiaraids.managers.PluginManager;
import com.weo4ka.elderiaraids.managers.RaidManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LootMenuListener implements Listener{
    private final LootMenuManager manager;
    private final RaidManager raidManager;
    private final ElderiaRaids plugin;

    public LootMenuListener(LootMenuManager manager, RaidManager raidManager, ElderiaRaids plugin) {
        this.manager = manager;
        this.raidManager = raidManager;
        this.plugin = plugin;
    }

    @EventHandler 
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (event.getClickedInventory() == null) return;

        Component expectedTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(manager.getTitle());
        if (!event.getView().title().equals(expectedTitle)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == player.getInventory()) return;

        int slot = event.getSlot();
        ItemStack clickedStack = event.getCurrentItem();

        if (clickedStack == null || clickedStack.getType().isAir()) return;

        List<Integer> dropSlots = manager.getDropSlots();

        if (dropSlots.contains(slot)) {
            Map<Integer, ItemStack> leftOver = player.getInventory().addItem(clickedStack);
            if (leftOver.isEmpty()) {
                inventory.setItem(slot, null);
                player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } else {
                ItemStack remaining = leftOver.get(0);
                inventory.setItem(slot, remaining);
                player.sendMessage("Inventory Full!");
                player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            return;
        }

        String buttonKey = manager.getItemConfigKeyBySlot(slot);
        plugin.getLogger().info("Clicked on slot: " + slot + ". Found button key for this slot: " + buttonKey);
        if (buttonKey != null) {
            switch (buttonKey) {
                case "exit-raid":
                    player.sendMessage("Вы &cпокинули&f рейд и были перемещены в &6Грейхевен&f.");
                    player.closeInventory();
                    raidManager.leaveRaid(player.getUniqueId());
                    break;
            }
        }
    }

    @EventHandler 
    public void onMenuClose(InventoryCloseEvent event) {
        Component expectedTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(manager.getTitle());
        if (!event.getView().title().equals(expectedTitle)) return;

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        PluginManager.getInstance().getLootManager().saveLeftoverLootToStash(player, inventory);
    }
}
