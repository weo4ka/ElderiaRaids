package com.weo4ka.elderiaraids.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
    private Map<String, Map<String, Integer>> damageCategoryToSlotAmount = new HashMap<>();
    private Map<String, TreeMap<Integer, String>> damageCategoryToMinDamage = new HashMap<>();

    private final ElderiaRaids plugin;
    private final File file;
    private final FileConfiguration config;
    private final File dbFile;
    private FileConfiguration dbConfig;

    public LootManager(ElderiaRaids plugin, List<RaidData> bosses) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.file = new File(plugin.getDataFolder(), "raids.yml");
        this.dbFile = new File(plugin.getDataFolder(), "loot-items-db.yml");

        if (!file.exists()) {
            plugin.saveResource("raids.yml", false);
        }
        if (!dbFile.exists()) {
            plugin.saveResource("loot-items-db.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        this.dbConfig = YamlConfiguration.loadConfiguration(dbFile);

        loadLootConfig(bosses);
    }

    private void loadLootConfig(List<RaidData> bosses) {
        pool.clear();
        
        ConfigurationSection bossSection = config.getConfigurationSection("raids");
        if (bossSection == null) {return;}

        for (RaidData boss : bosses) {
            ConfigurationSection itemsSection = bossSection.getConfigurationSection(boss.getBossId() + ".loot");
            if (itemsSection == null) {continue;}

            Map<String, List<LootItemData>> damageCategoriesMap = new HashMap<>();
            Map<String, Integer> categoryToAmount = new HashMap<>();
            TreeMap<Integer, String> damageToCategory = new TreeMap<>();

            for (String damageCategory : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(damageCategory + ".items");
                if (itemSection == null) {continue;}

                categoryToAmount.put(damageCategory, itemsSection.getInt(damageCategory + ".max-items"));
                damageToCategory.put(itemsSection.getInt(damageCategory + ".min-damage"), damageCategory);

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
            damageCategoryToMinDamage.put(boss.getBossId(), damageToCategory);
            damageCategoryToSlotAmount.put(boss.getBossId(), categoryToAmount);
        }
    }

    public List<ItemStack> generateLootFromPool(String bossId, String damageCategory) {
        List<ItemStack> loot = new ArrayList<>();
        
        Map<String, List<LootItemData>> bossPool = pool.get(bossId);
        if (bossPool == null) return loot;

        List<LootItemData> originalPool = bossPool.get(damageCategory);
        if (originalPool == null || originalPool.isEmpty()) return loot;

        List<LootItemData> tmpPool = new ArrayList<>(originalPool);

        Map<String, Integer> slotsMap = damageCategoryToSlotAmount.get(bossId);
        int maxSlots = (slotsMap != null) ? slotsMap.getOrDefault(damageCategory, 0) : 0;
        int finalAmount = Math.min(maxSlots, tmpPool.size());

        for (int i = 0; i < finalAmount; i++) {
            int totalWeight = 0;
            for (LootItemData item : tmpPool) {
                totalWeight += item.getWeight();
            }

            if (totalWeight <= 0) break;
            
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
        ItemStack clone = stack.clone();
        clone.setAmount(1);

        dbConfig.set("items." + itemId, clone);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                dbConfig.save(dbFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed saving item to loot-items-db.yml!");
                e.printStackTrace();
            }
        });
    }

    public ItemStack loadLootItemFromDataBase(String itemId) {
        ItemStack item = dbConfig.getItemStack("items." + itemId);
        return item != null ? item.clone() : null;
    }

    public Map<String, Integer> getDamageCategoryToMaxSlots(String bossId) { return damageCategoryToSlotAmount.getOrDefault(bossId, new HashMap<>()); }
    public TreeMap<Integer, String> getDamageCategoryToMinDamage(String bossId) { return damageCategoryToMinDamage.getOrDefault(bossId, new TreeMap<>()); }

    public Optional<String> getDamageCategoryFromDamage(String bossId, int damage) {
        TreeMap<Integer, String> map = getDamageCategoryToMinDamage(bossId);
        if (map == null) return Optional.empty();

        Map.Entry<Integer, String> matchedCategory = map.floorEntry(damage);
        return Optional.ofNullable(matchedCategory).map(Map.Entry::getValue);
    }
}