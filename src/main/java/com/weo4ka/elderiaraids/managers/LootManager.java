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
            try {
                dbFile.createNewFile(); // Просто создаем пустой файл на диске
            } catch (IOException e) {
                plugin.getLogger().severe("Failed creating loot-items-db.yml file!");
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        this.dbConfig = YamlConfiguration.loadConfiguration(dbFile);

        loadLootConfig(bosses);
    }

    private void loadLootConfig(List<RaidData> bosses) {
        pool.clear();
        damageCategoryToMinDamage.clear();
        damageCategoryToSlotAmount.clear();

        plugin.getLogger().info("[LoadDebug] --- НАЧАЛО ЗАГРУЗКИ КОНФИГУРАЦИИ ЛУТА ---");

        ConfigurationSection bossSection = config.getConfigurationSection("raids");
        if (bossSection == null) {
            plugin.getLogger().severe("[LoadDebug] Секция 'raids' не найдена в raids.yml!");
            return;
        }

        for (RaidData boss : bosses) {
            plugin.getLogger().info("[LoadDebug] Обработка босса из списка: " + boss.getBossConfigKey());

            ConfigurationSection itemsSection = bossSection.getConfigurationSection(boss.getBossConfigKey() + ".loot");
            if (itemsSection == null) {
                plugin.getLogger().warning("[LoadDebug] Секция лута для босса '" + boss.getBossConfigKey() + "' отсутствует в raids.yml.");
                continue;
            }

            Map<String, List<LootItemData>> damageCategoriesMap = new HashMap<>();
            Map<String, Integer> categoryToAmount = new HashMap<>();
            TreeMap<Integer, String> damageToCategory = new TreeMap<>();

            for (String damageCategory : itemsSection.getKeys(false)) {
                plugin.getLogger().info("[LoadDebug] Обнаружена категория урона: " + damageCategory);

                ConfigurationSection itemSection = itemsSection.getConfigurationSection(damageCategory + ".items");
                if (itemSection == null) {
                    plugin.getLogger().warning("[LoadDebug] Секция '.items' для категории '" + damageCategory + "' пуста или отсутствует.");
                    continue;
                }

                categoryToAmount.put(damageCategory, itemsSection.getInt(damageCategory + ".max-items"));
                damageToCategory.put(itemsSection.getInt(damageCategory + ".min-damage"), damageCategory);

                List<LootItemData> categoryItems = new ArrayList<>();
                for (String itemId : itemSection.getKeys(false)) {
                    plugin.getLogger().info("[LoadDebug] Попытка загрузки itemId: '" + itemId + "' из БД...");

                    ConfigurationSection item = itemSection.getConfigurationSection(itemId);
                    if (item == null) {continue;}

                    ItemStack stack = loadLootItemFromDataBase(itemId);
                    if (stack == null) {
                        plugin.getLogger().severe("[LoadDebug] Результат БД: NULL! Предмет '" + itemId + "' не найден в loot-items-db.yml");
                        continue;
                    }

                    if (stack.getType() == org.bukkit.Material.AIR || stack.getType().isAir() || stack.getAmount() <= 0) {
                        plugin.getLogger().warning("[LoadDebug]       [!] ПРЕДУПРЕЖДЕНИЕ: Этот предмет распознан как воздух/пустой. Пропускаем его добавление в пул!");
                        continue;
                    }

                    int minAmount = item.getInt("min-amount", 1);
                    int maxAmount = item.getInt("max-amount", 1);
                    int weight = item.getInt("chance", 1);

                    plugin.getLogger().info("[LoadDebug] Успешно добавлен в пул памяти! Вес=" + weight + " Стак: " + stack.toString());
                    LootItemData data = new LootItemData(stack, minAmount, maxAmount, weight);
                    categoryItems.add(data);
                }

                damageCategoriesMap.put(damageCategory, categoryItems);
                plugin.getLogger().info("[LoadDebug] Итого добавлено предметов в категорию '" + damageCategory + "': " + categoryItems.size());
            }
            pool.put(boss.getBossConfigKey(), damageCategoriesMap);
            damageCategoryToMinDamage.put(boss.getBossConfigKey(), damageToCategory);
            damageCategoryToSlotAmount.put(boss.getBossConfigKey(), categoryToAmount);
        }
        plugin.getLogger().info("[LoadDebug] --- ЗАВЕРШЕНИЕ ЗАГРУЗКИ КОНФИГУРАЦИИ ЛУТА ---");
    }

    public List<ItemStack> generateLootFromPool(String bossConfigKey, int damage) {
        List<ItemStack> loot = new ArrayList<>();
        String damageCategory = getDamageCategoryFromDamage(bossConfigKey, damage).orElse("");

        Map<String, List<LootItemData>> bossPool = pool.get(bossConfigKey);
        if (bossPool == null) return loot;

        List<LootItemData> originalPool = bossPool.get(damageCategory);
        if (originalPool == null || originalPool.isEmpty()) return loot;

        // Делаем копию пула для текущего расчета
        List<LootItemData> tmpPool = new ArrayList<>(originalPool);

        Map<String, Integer> slotsMap = damageCategoryToSlotAmount.get(bossConfigKey);
        int maxSlots = (slotsMap != null) ? slotsMap.getOrDefault(damageCategory, 0) : 0;
        
        // Вычисляем, сколько предметов нужно выдать (не больше, чем есть в пуле)
        int finalAmount = Math.min(maxSlots, tmpPool.size());

        for (int i = 0; i < finalAmount; i++) {
            // Считаем общий вес оставшихся предметов заново на каждой итерации!
            int totalWeight = 0;
            for (LootItemData item : tmpPool) {
                totalWeight += item.getWeight();
            }

            if (totalWeight <= 0) break;
            
            int rndIdx = ThreadLocalRandom.current().nextInt(totalWeight);
            int currentSum = 0;

            // ИСПРАВЛЕНО: Безопасный поиск предмета
            LootItemData selectedItem = null;
            for (LootItemData item : tmpPool) {
                currentSum += item.getWeight();
                if (rndIdx < currentSum) {
                    selectedItem = item;
                    break;
                }
            }

            if (selectedItem != null) {
                // Генерируем финальный ItemStack (с расчетом min/max количества из LootItemData)
                ItemStack finalStack = selectedItem.getFinalStack();
                
                // Дополнительная супер-защита: если вдруг на этапе клонирования что-то пошло не так
                if (finalStack != null && !finalStack.getType().isAir()) {
                    loot.add(finalStack);
                } else {
                    plugin.getLogger().warning("[ElderiaRaids] Попытка добавить AIR в генераторе лута была заблокирована!");
                }
                
                // Безопасно удаляем объект из копии пула, чтобы он не выпал дважды за один рейд
                tmpPool.remove(selectedItem);
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

    public Map<String, Integer> getDamageCategoryToMaxSlots(String bossConfigKey) { return damageCategoryToSlotAmount.getOrDefault(bossConfigKey, new HashMap<>()); }
    public TreeMap<Integer, String> getDamageCategoryToMinDamage(String bossConfigKey) { return damageCategoryToMinDamage.getOrDefault(bossConfigKey, new TreeMap<>()); }

    @SuppressWarnings("null")
    public Optional<String> getDamageCategoryFromDamage(String bossConfigKey, int damage) {
        TreeMap<Integer, String> map = getDamageCategoryToMinDamage(bossConfigKey);
        if (map == null) return Optional.empty();

        Map.Entry<Integer, String> matchedCategory = map.floorEntry(damage);
        return Optional.ofNullable(matchedCategory).map(Map.Entry::getValue);
    }
}