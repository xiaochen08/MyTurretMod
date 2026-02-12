package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TurretMenu extends AbstractContainerMenu {
    public final SkeletonTurret turret;
    private final Container dataInventory;
    private final DataSlot upgradeState = DataSlot.standalone(); // 0=关闭, 1=开启

    // 客户端构造器
    public TurretMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, (SkeletonTurret) playerInv.player.level().getEntity(extraData.readInt()), new SimpleContainer(45));
    }

    // 服务器构造器
    public TurretMenu(int id, Inventory playerInv, SkeletonTurret entity, Container container) {
        super(ExampleMod.TURRET_MENU.get(), id);
        this.turret = entity;
        this.dataInventory = container;

        checkContainerSize(container, 45);
        container.startOpen(playerInv.player);
        this.addDataSlot(this.upgradeState); // 同步状态

        // ==========================================
        // ⚔️ 1. 战斗装备组 (5格) - 只读模式
        // ==========================================
        // 这里必须用 DisplaySlot，否则玩家能把装备拿走！
        for (int i = 0; i < 5; i++) {
            this.addSlot(new DisplaySlot(container, i, 110, 25 + i * 18));
        }

        // ==========================================
        // ⚡ 2. 升级模块组 (5格) - 只能放特定物品
        // ==========================================
        for (int i = 0; i < 5; i++) {
            this.addSlot(new ModuleSlot(container, 5 + i, 162, 25 + i * 18));
        }

        // ==========================================
        // 📦 3. 储物箱 (9x3)
        // ==========================================
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                this.addSlot(new Slot(container, 10 + c + r * 9, 64 + c * 18, 118 + r * 18));
            }
        }

        // ==========================================
        // 👤 4. 玩家背包
        // ==========================================
        int playerInvX = 64;
        int playerInvY = 174;

        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                this.addSlot(new Slot(playerInv, c + r * 9 + 9, playerInvX + c * 18, playerInvY + r * 18));
            }
        }
        // 快捷栏
        for (int c = 0; c < 9; ++c) {
            this.addSlot(new Slot(playerInv, c, playerInvX + c * 18, playerInvY + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.dataInventory.stillValid(player) && this.turret.isAlive() && this.turret.distanceTo(player) < 8.0f;
    }

    // 状态判断
    public boolean isUpgrading() {
        return this.upgradeState.get() == 1;
    }

    // 处理按钮点击 (无需发包，客户端调用 gameMode.handleInventoryButtonClick 即可)
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 1) { // ID 1: 切换升级模式
            int current = this.upgradeState.get();
            this.upgradeState.set(current == 0 ? 1 : 0);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // A. 如果是 Turret 自己的槽位 (0-36)
            if (index < 37) {
                // 尝试移动到玩家背包 (37-72)
                if (!this.moveItemStackTo(itemstack1, 37, 73, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // B. 如果是玩家背包 (37-72)
            else {
                // 1. 如果处于升级模式，且物品符合升级条件 -> 优先尝试放入升级槽 (5-9)
                boolean movedToModule = false;
                if (isUpgrading()) {
                    // 检查是否是升级物品 (简单预判，具体由 moveItemStackTo 内部的 mayPlace 把关)
                    if (this.moveItemStackTo(itemstack1, 5, 10, false)) {
                        movedToModule = true;
                    }
                }

                // 2. 如果没放进升级槽 (或模式没开) -> 尝试放入储物箱 (10-36)
                if (!movedToModule) {
                    if (!this.moveItemStackTo(itemstack1, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.dataInventory.stopOpen(player);
    }

    // ==========================================
    // 🔒 核心修复：加强版展示槽
    // ==========================================
    public static class DisplaySlot extends Slot {
        public DisplaySlot(Container c, int i, int x, int y) { super(c, i, x, y); }

        // 🚫 禁止放入
        @Override
        public boolean mayPlace(ItemStack s) { return false; }

        // 🚫 禁止拿取 (之前就是漏了这个！导致你能拿下来)
        @Override
        public boolean mayPickup(Player player) { return false; }
    }

    // ==========================================
    // ⚡ 升级模块槽：只允许特定物品
    // ==========================================
    public class ModuleSlot extends Slot {
        public ModuleSlot(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // 🔒 只有在开启升级模式时，才允许放入
            if (!isUpgrading()) return false;

            // 🔍 检查逻辑：只有在这个列表里的东西才能放进去
            return stack.getItem() == ExampleMod.GLITCH_CHIP.get()
                    || stack.getItem() == ExampleMod.TELEPORT_UPGRADE_MODULE.get() // ✅ 允许传送模块
                    || stack.is(Items.REDSTONE)
                    || stack.is(Items.DIAMOND);
        }
    }
}