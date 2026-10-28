package com.example.spearbackport.client;

import com.example.spearbackport.SpearBackport;
import com.example.spearbackport.SpearItem;

import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

@EventBusSubscriber(modid = SpearBackport.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    /**
     * Vanilla slows the player to 20% while using an item. That would make a spear charge
     * impossible, so we cancel the slowdown by multiplying the input before vanilla applies it.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player.isUsingItem() && !player.isPassenger()
                && player.getUseItem().getItem() instanceof SpearItem) {
            Input input = event.getInput();
            input.forwardImpulse *= SpearItem.CHARGE_MOVE_BOOST;
            input.leftImpulse *= SpearItem.CHARGE_MOVE_BOOST;
        }
    }
}
