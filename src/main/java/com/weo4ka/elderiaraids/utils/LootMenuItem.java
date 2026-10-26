package com.weo4ka.elderiaraids.utils;

public class LootMenuItem {
    private final String configKey;
    private final String displayName;
    private final String displayLore;
    private final String material;
    private final int slot;

    public LootMenuItem(String configKey, String displayName, String displayLore, String material, int slot) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.displayLore = displayLore;
        this.material = material;
        this.slot = slot;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayLore() {
        return displayLore;
    }

    public String getMaterial() {
        return material;
    }

    public int getSlot() {
        return slot;
    }

}
