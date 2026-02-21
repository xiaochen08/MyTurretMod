package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class SummonTerminalMenu extends AbstractContainerMenu {
    private final BlockPos terminalPos;

    public SummonTerminalMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, buf.readBlockPos());
    }

    public SummonTerminalMenu(int id, Inventory playerInventory, BlockPos terminalPos) {
        super(ExampleMod.SUMMON_TERMINAL_MENU.get(), id);
        this.terminalPos = terminalPos;
    }

    public BlockPos terminalPos() {
        return terminalPos;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().getBlockState(terminalPos).getBlock() != ExampleMod.SUMMON_TERMINAL_BLOCK.get()) {
            return false;
        }
        return player.distanceToSqr(
                terminalPos.getX() + 0.5,
                terminalPos.getY() + 0.5,
                terminalPos.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
