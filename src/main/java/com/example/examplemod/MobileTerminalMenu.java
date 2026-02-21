package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Lightweight menu carrier for the mobile terminal.
 * It has no slots and exists only to sync screen opening and packet context.
 */
public class MobileTerminalMenu extends AbstractContainerMenu {
    private final UUID ownerUuid;

    public MobileTerminalMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, buf.readUUID());
    }

    public MobileTerminalMenu(int id, Inventory playerInventory, UUID ownerUuid) {
        super(ExampleMod.MOBILE_TERMINAL_MENU.get(), id);
        this.ownerUuid = ownerUuid;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.getUUID().equals(ownerUuid);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

