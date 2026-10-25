package com.weo4ka.elderiaraids.utils;

import java.util.Random;

import org.bukkit.inventory.ItemStack;

public class LootItemData {
    private final ItemStack stack;
    private final int minAmount;
    private final int maxAmount;
    private final int weight;

    private final Random random = new Random();

    public LootItemData(ItemStack stack, int minAmount, int maxAmount, int weight) {
        this.stack = stack;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.weight = weight;
    }

    public ItemStack getRawStack() {
        return stack;
    }

    public ItemStack getFinalStack() {
        ItemStack clone = stack.clone();
        int amount = random.nextInt(minAmount, maxAmount);
        clone.setAmount(amount);
        return clone;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public int getWeight() {
        return weight;
    }

}
