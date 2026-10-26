package com.weo4ka.elderiaraids.commands;

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

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class RaidsCommand {
    public static LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
        return Commands.literal(commandName)
            .then(Commands.literal("reroll_boss"))
                .executes(context -> {
                    RaidManager manager = PluginManager.getInstance().getRaidManager();
                    manager.rollNextBoss(); //Rolling Next Boss
                    final CommandSender sender = context.getSource().getSender();

                    sender.sendPlainMessage("Successfully rolled next boss. Its ID: " + manager.getNextRaidData().getBossConfigKey());
                    return Command.SINGLE_SUCCESS;
                }
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
                        } else {
                            sender.sendPlainMessage("This command can only be executed by players");
                        }

                        return Command.SINGLE_SUCCESS;
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
            .build();
    }
    
}
