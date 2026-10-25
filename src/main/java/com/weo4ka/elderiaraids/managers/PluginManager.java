package com.weo4ka.elderiaraids.managers;

import com.weo4ka.elderiaraids.ElderiaRaids;

public class PluginManager {
    private static PluginManager instance;
    
    private PluginManager() {}
    private RaidManager raidManager;


    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public void initialize(ElderiaRaids plugin) {
        this.raidManager = new RaidManager(plugin);
    }

    public RaidManager getRaidManager() { return raidManager; }
}
