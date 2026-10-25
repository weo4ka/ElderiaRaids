package com.weo4ka.elderiaraids.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.weo4ka.elderiaraids.ElderiaRaids;
import com.weo4ka.elderiaraids.utils.LootItemData;
import com.weo4ka.elderiaraids.utils.RaidData;

public class LootManager {

    private Map<String, Map<String, List<LootItemData>>> pool = new HashMap<>();

    private final ElderiaRaids plugin;
    private final File file;
    private final FileConfiguration config;
    private final File dbFile;
    private final FileConfiguration dbConfig;

    public LootManager(ElderiaRaids plugin, List<RaidData> bosses) {
        this.plugin = plugin;

        this.file = new File(plugin.getDataFolder(), "raids.yml");
        this.dbFile = new File(plugin.getDataFolder(), "loot-items-db.yml");
        if (!this.file.getParentFile().exists()) {
            this.file.getParentFile().mkdirs();
        }
        if (!this.dbFile.getParentFile().exists()) {
            this.dbFile.getParentFile().mkdirs();
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        this.dbConfig = YamlConfiguration.loadConfiguration(dbFile);

        pool.clear();
        
        ConfigurationSection bossSection = config.getConfigurationSection("raids");
        if (bossSection == null) {return;}

        for (RaidData boss : bosses) {
            ConfigurationSection itemsSection = bossSection.getConfigurationSection(boss.getBossId() + ".items");
            if (itemsSection == null) {continue;}

            Map<String, List<LootItemData>> damageCategoriesMap = new HashMap<>();

            for (String damageCategory : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(damageCategory);
                if (itemSection == null) {continue;}

                List<LootItemData> categoryItems = new ArrayList<>();
                for (String itemId : itemSection.getKeys(false)) {
                    ConfigurationSection item = itemSection.getConfigurationSection(itemId);
                    if (item == null) {continue;}

                    ItemStack stack = loadLootItemFromDataBase(itemId);
                    if (stack == null) {continue;}

                    int minAmount = item.getInt("min-amount");
                    int maxAmount = item.getInt("max-amount");
                    int weight = item.getInt("chance");

                    LootItemData data = new LootItemData(stack, minAmount, maxAmount, weight);
                    categoryItems.add(data);
                }

                damageCategoriesMap.put(damageCategory, categoryItems);
            }
            pool.put(boss.getBossId(), damageCategoriesMap);
        }
    }

    public List<ItemStack> generateLootFromPool(int requiredAmount, String damageCategory) {
        List<ItemStack> loot = new ArrayList<>();
        List<LootItemData> tmpPool = pool.get(PluginManager.getInstance().getRaidManager().getNextRaidData().getBossId()).get(damageCategory);

        if (pool.isEmpty()) { return loot; }
        int finalAmount = Math.min(requiredAmount, tmpPool.size());

        for (int i = 0; i < finalAmount ; i++) {
            int totalWeight = 0;
            for (LootItemData item : tmpPool) {
                totalWeight += item.getWeight();
            }

            if (totalWeight <=0 ) { return loot; }
            int rndIdx = ThreadLocalRandom.current().nextInt(totalWeight);
            int currentSum = 0;

            for (int j = 0; j < tmpPool.size(); j++) {
                LootItemData item = tmpPool.get(j);
                currentSum += item.getWeight();

                if (rndIdx < currentSum) {
                    loot.add(item.getFinalStack());
                    tmpPool.remove(j);
                    break;
                }
            }

        }
        return loot;
    }

    public void saveLootItemToDataBase(Player player, String itemId) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        File file = new File(plugin.getDataFolder(), "loot-item-db.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ItemStack clone = stack.clone();
        clone.setAmount(1);
        config.set("items." + itemId, clone);

        synchronized (dbFile) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    config.save(file);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed writing itemstack into loot-items-db.yml");
                    e.printStackTrace();
                }
            });
        };
    }

    public ItemStack loadLootItemFromDataBase(String itemId) {
        return dbConfig.getItemStack("items." + itemId);
    }
}