package com.weo4ka.elderiaraids.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
            .then(Commands.literal("reroll-boss"))
                .executes(context -> {
                    RaidManager manager = PluginManager.getInstance().getRaidManager();
                    manager.rollNextBoss(); //Rolling Next Boss
                    final CommandSender sender = context.getSource().getSender();

                    sender.sendPlainMessage("Successfully rolled next boss. Its ID: " + manager.getNextRaidData().getBossConfigKey());
                    return Command.SINGLE_SUCCESS;
                }
            )
            .then(Commands.literal("add-loot")
                .then(Commands.argument("item-id", StringArgumentType.string())
                    .executes(context -> {
                        final String itemId = context.getArgument("item-id", String.class);
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
            .then(Commands.literal("roll-loot")
                .then(Commands.argument("boss-config-key", StringArgumentType.string())
                    .then(Commands.argument("damage", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            final String bossConfigKey = context.getArgument("boss-config-key", String.class);
                            final int damage = context.getArgument("damage", Integer.class);
                            LootManager manager = PluginManager.getInstance().getLootManager();
                            List<ItemStack> loot = manager.generateLootFromPool(bossConfigKey, damage);

                            final CommandSender sender = context.getSource().getSender();
                            sender.sendPlainMessage("Generated Loot: " + loot.toString());

                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .build();
    }
    
}
