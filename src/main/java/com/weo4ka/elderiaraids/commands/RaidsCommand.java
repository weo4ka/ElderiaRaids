package com.weo4ka.elderiaraids.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.weo4ka.elderiaraids.managers.LootManager;
import com.weo4ka.elderiaraids.managers.PluginManager;
import com.weo4ka.elderiaraids.managers.RaidManager;
import com.weo4ka.elderiaraids.utils.RaidData;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class RaidsCommand {
    public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
        return Commands.literal(commandName)
            .then(Commands.literal("reroll_boss")
                .executes(context -> {
                    RaidManager manager = PluginManager.getInstance().getRaidManager();
                    manager.rollNextRaid();
                    final CommandSender sender = context.getSource().getSender();

                    if (manager.getCurrentRaidData() != null) {
                        sender.sendPlainMessage("Successfully rolled next boss. Its ID: " + manager.getCurrentRaidData().getBossConfigKey() + ". Raid will start in " + manager.howMuchCooldownLeft() + " seconds");
                        return Command.SINGLE_SUCCESS;
                    } else {
                        sender.sendPlainMessage("Failed to roll next boss. Map is empty or raids.yml is missing.");
                        return Command.SINGLE_SUCCESS;
                    }
                })
            )
            .then(Commands.literal("add_loot")
                .then(Commands.argument("item_id", StringArgumentType.string())
                    .executes(context -> {
                        final String itemId = context.getArgument("item_id", String.class);
                        final CommandSender sender = context.getSource().getSender();

                        if (sender instanceof Player) {
                            LootManager manager = PluginManager.getInstance().getLootManager();
                            manager.saveLootItemToDataBase((Player) sender, itemId);
                            sender.sendPlainMessage("Successfully saved item data to DataBase");

                            PluginManager.getInstance().getLootManager().loadLootConfig(new ArrayList<RaidData>(PluginManager.getInstance().getRaidManager().getRegisteredBosses().values()));
                            return Command.SINGLE_SUCCESS;
                        } else {
                            sender.sendPlainMessage("This command can only be executed by players!");
                            return Command.SINGLE_SUCCESS;
                        }
                    })
                )
            )
            .then(Commands.literal("roll_loot")
                .then(Commands.argument("boss_config_key", StringArgumentType.string())
                    .then(Commands.argument("damage", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            final String bossConfigKey = context.getArgument("boss_config_key", String.class);
                            final int damage = context.getArgument("damage", Integer.class);
                            LootManager manager = PluginManager.getInstance().getLootManager();
                            List<ItemStack> loot = manager.generateLootFromPool(bossConfigKey, damage);

                            final CommandSender sender = context.getSource().getSender();
                            if (sender instanceof Player) {
                                PluginManager.getInstance().getLootMenuManager().openLootMenu((Player) sender, loot);
                                return Command.SINGLE_SUCCESS;
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(Commands.literal("stash")
                .executes(context -> {
                    if (context.getSource().getSender() instanceof Player player) {
                        Inventory stash = PluginManager.getInstance().getLootManager().getStash(player);
                        player.openInventory(stash);
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("reload")
                .executes(context -> {
                    PluginManager managerInstance = PluginManager.getInstance();
                    managerInstance.getRaidManager().loadRaidConfig();
                    managerInstance.getLootMenuManager().loadMenuConfig();
                    managerInstance.getLootManager().loadLootConfig(new ArrayList<>(managerInstance.getRaidManager().getRegisteredBosses().values()));
                    
                    context.getSource().getSender().sendPlainMessage("ElderiaRaids configuration successfully reloaded!");
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("enter")
                .executes(context -> {
                    RaidManager raidManager = PluginManager.getInstance().getRaidManager();
                    CommandSender sender = context.getSource().getSender();
                    if (raidManager.isRaidOnCooldown()) {
                        sender.sendMessage("&cRaid is on cooldown, " + raidManager.howMuchCooldownLeft() + " seconds left.");
                        return Command.SINGLE_SUCCESS;
                    }
                    if (sender instanceof Player player) {
                        if (raidManager.isPlayerInRaid(player)) {
                            player.sendMessage("&cYou are already in raid!");
                            return Command.SINGLE_SUCCESS;
                        }

                        raidManager.enterOrStartRaid(player.getUniqueId());
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage("This command can only be executed by players!");

                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
