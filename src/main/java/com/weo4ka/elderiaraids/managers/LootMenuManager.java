package com.weo4ka.elderiaraids.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.weo4ka.elderiaraids.ElderiaRaids;
import com.weo4ka.elderiaraids.utils.LootMenuItem;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LootMenuManager {

    private int slotsAmount;
    private List<Integer> dropSlots = new ArrayList<>();
    private String title;

    private List<LootMenuItem> menuItems = new ArrayList<>();

    private final ElderiaRaids plugin;
    private final File menuFile;
    private FileConfiguration menuConfigFile;

    public LootMenuManager(ElderiaRaids plugin) {
        this.plugin = plugin;

        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        this.menuFile = new File(plugin.getDataFolder(), "menu.yml");
        if (!this.menuFile.exists()) {
            this.plugin.saveResource("menu.yml", false);
        }

        this.menuConfigFile = YamlConfiguration.loadConfiguration(menuFile);
        loadMenuConfig();
    }

    private void loadMenuConfig() {
        this.slotsAmount = menuConfigFile.getInt("menu.size");
        this.dropSlots = menuConfigFile.getIntegerList("menu.loot-slots");
        this.title = menuConfigFile.getString("menu.title");

        ConfigurationSection itemsSection = menuConfigFile.getConfigurationSection("menu.items");
        for (String itemConfigKey : itemsSection.getKeys(false)) {
            ConfigurationSection item = itemsSection.getConfigurationSection(itemConfigKey);
            if (item == null) {continue;}

            String configKey = itemConfigKey;
            String displayName = item.getString("display-name");
            String displayLore = item.getString("lore");
            String material = item.getString("material");
            int slot = item.getInt("slot");

            LootMenuItem i = new LootMenuItem(configKey, displayName, displayLore, material, slot);
            menuItems.add(i);
        }
    }

    public int getSlotAmount() { return slotsAmount; }
    public List<Integer> getDropSlots() { return dropSlots; }
    public String getTitle() { return title; }
    public String getItemConfigKeyBySlot(int slot) {
        for (LootMenuItem item : menuItems) {
            if (item.getSlot() == slot) return item.getConfigKey();
        }
        return null;
    }

    public Inventory fillLootMenuInventory(Inventory inventory) {

        for (LootMenuItem item : this.menuItems) {
            Material material = Material.matchMaterial(item.getMaterial());
            if (material == null) material = Material.AIR;

            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (item.getDisplayName() != null) {
                    meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(item.getDisplayName()));
                }

                if (item.getDisplayLore() != null && !item.getDisplayLore().isEmpty()) {
                    meta.lore(List.of(LegacyComponentSerializer.legacyAmpersand().deserialize(item.getDisplayLore())));
                }
                stack.setItemMeta(meta);
            }

            inventory.setItem(item.getSlot(), stack);
        }
        return inventory;
    }

    public void openLootMenu(Player player, List<ItemStack> generatedLoot) {
        Inventory stashInv = PluginManager.getInstance().getLootManager().getStash(player);

        for (int dropSlot : this.dropSlots) {
            stashInv.setItem(dropSlot, null);
        }

        if (generatedLoot != null && !generatedLoot.isEmpty() && !dropSlots.isEmpty()) {
            int lootSize = Math.min(generatedLoot.size(), dropSlots.size());
            int startIndex = (dropSlots.size() - lootSize) / 2;

            for (int i = 0; i < lootSize; i++) {
                int targetSlot = dropSlots.get(startIndex + i);
                stashInv.setItem(targetSlot, generatedLoot.get(i));
            }
        }

        player.openInventory(stashInv);
    }
}
