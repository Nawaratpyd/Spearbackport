package com.example.spearbackport;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SpearBackport.MODID)
public class SpearBackport {
    public static final String MODID = "spearbackport";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<SpearItem> WOODEN_SPEAR =
            ITEMS.register("wooden_spear", () -> new SpearItem(Tiers.WOOD, new Item.Properties()));
    public static final DeferredItem<SpearItem> STONE_SPEAR =
            ITEMS.register("stone_spear", () -> new SpearItem(Tiers.STONE, new Item.Properties()));
    public static final DeferredItem<SpearItem> COPPER_SPEAR =
            ITEMS.register("copper_spear", () -> new SpearItem(ModTiers.COPPER, new Item.Properties()));
    public static final DeferredItem<SpearItem> IRON_SPEAR =
            ITEMS.register("iron_spear", () -> new SpearItem(Tiers.IRON, new Item.Properties()));
    public static final DeferredItem<SpearItem> GOLDEN_SPEAR =
            ITEMS.register("golden_spear", () -> new SpearItem(Tiers.GOLD, new Item.Properties()));
    public static final DeferredItem<SpearItem> DIAMOND_SPEAR =
            ITEMS.register("diamond_spear", () -> new SpearItem(Tiers.DIAMOND, new Item.Properties()));
    public static final DeferredItem<SpearItem> NETHERITE_SPEAR =
            ITEMS.register("netherite_spear",
                    () -> new SpearItem(Tiers.NETHERITE, new Item.Properties().fireResistant()));

    public SpearBackport(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addToCreativeTab);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(WOODEN_SPEAR.get());
            event.accept(STONE_SPEAR.get());
            event.accept(COPPER_SPEAR.get());
            event.accept(IRON_SPEAR.get());
            event.accept(GOLDEN_SPEAR.get());
            event.accept(DIAMOND_SPEAR.get());
            event.accept(NETHERITE_SPEAR.get());
        }
    }
}
