package com.weo4ka.elderiaraids.managers;

import java.util.ArrayList;

import com.weo4ka.elderiaraids.ElderiaRaids;
import com.weo4ka.elderiaraids.utils.RaidData;

public class PluginManager {
    private static PluginManager instance;
    
    private PluginManager() {}
    private RaidManager raidManager;
    private LootManager lootManager;
    private LootMenuManager lootMenuManager;

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public void initialize(ElderiaRaids plugin) {
        this.raidManager = new RaidManager(plugin);
        this.lootMenuManager = new LootMenuManager(plugin);
        this.lootManager = new LootManager(plugin, new ArrayList<RaidData>(this.raidManager.getRegisteredBosses().values()), getLootMenuManager());

    }

    public RaidManager getRaidManager() { return raidManager; }
    public LootManager getLootManager() { return lootManager; }
    public LootMenuManager getLootMenuManager() { return lootMenuManager; }
}
