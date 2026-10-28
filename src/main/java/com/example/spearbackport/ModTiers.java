package com.example.spearbackport;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/** Vanilla 1.21.1 has no copper tier, so we define one. Numbers are approximations. */
public final class ModTiers {
    public static final Tier COPPER = new SimpleTier(
            BlockTags.INCORRECT_FOR_STONE_TOOL,
            190,   // durability
            5.0F,  // mining speed (unused by spears)
            1.0F,  // attack damage bonus
            13,    // enchantability
            () -> Ingredient.of(Items.COPPER_INGOT));

    private ModTiers() {
    }
}
