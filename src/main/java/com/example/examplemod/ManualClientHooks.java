package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ManualClientHooks {
    private ManualClientHooks() {}

    public static void openManual(Player player, InteractionHand hand) {
        if (player == null) return;
        ItemStack stack = player.getItemInHand(hand);
        int sourceSlot = hand == InteractionHand.OFF_HAND ? 40 : player.getInventory().selected;
        Minecraft.getInstance().setScreen(new PlayerManualScreen(stack.copy(), sourceSlot));
    }
}

