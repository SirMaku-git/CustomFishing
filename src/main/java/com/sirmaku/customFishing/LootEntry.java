package com.sirmaku.customFishing;

import org.bukkit.inventory.ItemStack;

public record LootEntry(int chance, int durabilityLoss, ItemStack item) {}
