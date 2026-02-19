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
    private final DataSlot upgradeState = DataSlot.standalone(); // 0=闁稿繑濞婂Λ? 1=鐎殿喒鍋撻柛?

    // 閻庡箍鍨洪崺娑氱博椤栨稓鈧垶鏌呴悩鍙夌彜
    public TurretMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, (SkeletonTurret) playerInv.player.level().getEntity(extraData.readInt()), new SimpleContainer(45));
    }

    // 闁哄牆绉存慨鐔煎闯閵婏妇鈧垶鏌呴悩鍙夌彜
    public TurretMenu(int id, Inventory playerInv, SkeletonTurret entity, Container container) {
        super(ExampleMod.TURRET_MENU.get(), id);
        this.turret = entity;
        this.dataInventory = container;

        checkContainerSize(container, 45);
        container.startOpen(playerInv.player);
        this.upgradeState.set(1);
        this.addDataSlot(this.upgradeState); // 闁告艾鏈鐐烘偐閼哥鍋?

        // ==========================================
        // 闁虫寧妫戠粭?1. 闁瑰瓨蓱閺嬬喓鎲楅崨顓фУ缂?(5闁? - 闁告瑯浜ｉ鏉课熼垾宕囩
        // ==========================================
        // 閺夆晜鐟╅崳鐤疀閸涙番鈧繘鎮?DisplaySlot闁挎稑鑻幆渚€宕氬▎鎴濊礋閻庤鍎奸崗姗€骞庢繝鍜佹濠㈣泛娲︾€ｄ胶鎸х敮顔剧＜
        for (int i = 0; i < 5; i++) {
            this.addSlot(new DisplaySlot(container, i, 110, 25 + i * 18));
        }

        // ==========================================
        // 闁?2. 闁告娲ㄦ鍥熼垾铏仴缂?(5闁? - 闁告瑯浜ｉ崗姗€寮ㄩ崜褍顥楅悗瑙勬皑婢у潡宕?
        // ==========================================
        for (int i = 0; i < 5; i++) {
            this.addSlot(new ModuleSlot(container, 5 + i, 162, 25 + i * 18));
        }

        // ==========================================
        // 妫ｅ啯鎲?3. 闁稿被鍔庢晶璺ㄧ不?(9x3)
        // ==========================================
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                this.addSlot(new Slot(container, 10 + c + r * 9, 64 + c * 18, 118 + r * 18));
            }
        }

        // ==========================================
        // 妫ｅ啯鍣?4. 闁绘壕鏅涢宥夋嚄鐏炶棄鐦?
        // ==========================================
        int playerInvX = 64;
        int playerInvY = 174;

        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                this.addSlot(new Slot(playerInv, c + r * 9 + 9, playerInvX + c * 18, playerInvY + r * 18));
            }
        }
        // 闊浂鍋呭畵搴ㄥ冀?
        for (int c = 0; c < 9; ++c) {
            this.addSlot(new Slot(playerInv, c, playerInvX + c * 18, playerInvY + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.dataInventory.stillValid(player) && this.turret.isAlive() && this.turret.distanceTo(player) < 8.0f;
    }

    // 闁绘鍩栭埀顑跨閸ㄤ粙寮?
    public boolean isUpgrading() {
        return true;
    }

    // 濠㈣泛瀚幃濠囧箰婢舵劖灏﹂柣鎰嚀閸?(闁哄啰濞€濞撳爼宕ｉ幋婵嗙樁闁挎稑鑻褰掑箣妞嬪寒浼傞悹瀣暟閺?gameMode.handleInventoryButtonClick 闁告鍟胯ぐ?
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 1) { // ID 1: 闁告帒娲﹀畷鏌ュ础閸モ晠鐛撴俊顖椻偓宕囩
            this.upgradeState.set(1);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            // Keep installed modules stable against external "quick move" sort actions (e.g. IPN one-click sort).
            if (index >= 5 && index < 10) {
                return ItemStack.EMPTY;
            }

            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // A. 濠碘€冲€归悘澶愬及?Turret 闁煎浜滅换渚€鎯冮崟顑偅鎷?(0-36)
            if (index < 37) {
                // 閻忓繑绻嗛惁顖滅矓鐠囨彃袟闁告帗澹嗙敮铏光偓瑙勫劶閸庢宕?(37-72)
                if (!this.moveItemStackTo(itemstack1, 37, 73, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // B. 濠碘€冲€归悘澶愬及椤栨粌璐熼悗瑙勫劶閸庢宕?(37-72)
            else {
                // 1. 濠碘€冲€归悘澶嬪緞閸曨亞鑹鹃柛妤€娲ㄦ鍥熼垾宕囩闁挎稑濂旂粭鏍偋閳轰焦鎯傜紒妤嬬畱閹酣宕￠崶鈺呯崜闁哄鈧弶顐?-> 濞村吋锚閸樻稓浜稿┑濠勬Ц闁衡偓閹冨汲闁告娲ㄦ鍥?(5-9)
                boolean movedToModule = false;
                if (isUpgrading()) {
                    // 婵☆偀鍋撻柡灞诲劜濡叉悂宕ラ敂鑺バ﹂柛妤€娲ㄦ鍥偋閳轰焦鎯?(缂佺姭鍋撻柛妤佹礋椤ｂ晠宕氶妶蹇曠闁稿繗娓圭紞瀣偨?moveItemStackTo 闁告劕鎳橀崕鎾儍?mayPlace 闁硅泛锕ら崣?
                    if (this.moveItemStackTo(itemstack1, 5, 10, false)) {
                        movedToModule = true;
                    }
                }

                // 2. 濠碘€冲€归悘澶娾柦閳╁啯鏉归弶鈺傜☉瀹曞瞼鐥铦?(闁瑰瓨鐗楄啯鐎殿喖绻戦惀鍛嚕閳? -> 閻忓繑绻嗛惁顖炲绩閹冨汲闁稿被鍔庢晶璺ㄧ不?(10-36)
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
    // 妫ｅ啯鏅?闁哄秶顭堢缓鐐┍椤旂⒈妲婚柨娑欒壘婵偛顕ｉ搹鐟邦暭閻忕偞娲滈妵姘?
    // ==========================================
    public static class DisplaySlot extends Slot {
        public DisplaySlot(Container c, int i, int x, int y) { super(c, i, x, y); }

        // 妫ｅ啯鐦?缂佸倷鐒﹂娑㈠绩閹冨汲
        @Override
        public boolean mayPlace(ItemStack s) { return false; }

        // 妫ｅ啯鐦?缂佸倷鐒﹂娑㈠箯閸喖绲?(濞戞柨顑呮晶鐘典焊鏉堛劍笑婵犳洖绻嬬花鈩冩交濞嗗酣鍤嬮柨娑楃椤曢亶鎳涚紙鐘电☉闁煎疇濮ょ€ｄ焦绋夌€ｎ偅闄?
        @Override
        public boolean mayPickup(Player player) { return false; }
    }

    // ==========================================
    // 闁?闁告娲ㄦ鍥熼垾铏仴婵″弶鏋荤槐浼村矗椤忓嫬甯掗悹浣告憸婢规帞鈧姘ㄦ晶鍧楀传?
    // ==========================================
    public class ModuleSlot extends Slot {
        public ModuleSlot(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() == ExampleMod.GLITCH_CHIP.get()
                    || stack.getItem() instanceof GenericTurretModuleItem
                    || stack.is(Items.REDSTONE)
                    || stack.is(Items.DIAMOND);
        }
    }
}
