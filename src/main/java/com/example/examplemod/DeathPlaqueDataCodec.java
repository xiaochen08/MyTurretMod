package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class DeathPlaqueDataCodec {
    public static final int CURRENT_VERSION = 3;

    private DeathPlaqueDataCodec() {}

    public static CompoundTag buildFromTurret(SkeletonTurret turret, int fatalHitCount) {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", CURRENT_VERSION);

        CompoundTag data = new CompoundTag();
        data.putInt("UnitID", turret.getEntityData().get(SkeletonTurret.UNIT_ID));
        data.putInt("Tier", turret.getTier());
        data.putInt("XP", turret.getEntityData().get(turret.getDataXpAccessor()));
        data.putInt("KillCount", turret.getKillCount());
        data.putInt("UpgradeProgress", turret.getUpgradeProgress());
        data.putBoolean("IsBrutal", turret.isBrutal());
        data.putInt("Heat", turret.getHeat());
        data.putBoolean("HasTeleportModule", turret.hasTeleportModule());
        data.putInt("FatalHitCount", fatalHitCount);
        data.putLong("RecordedAtGameTime", turret.level().getGameTime());

        if (turret.getOwnerUUID() != null) {
            data.putUUID("OwnerUUID", turret.getOwnerUUID());
        }

        data.putString("BaseName", turret.getBaseName());

        // Upgrade modules only (slot 5-9)
        ListTag modules = new ListTag();
        for (int i = 5; i < 10; i++) {
            ItemStack s = turret.inventory.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag moduleTag = new CompoundTag();
                moduleTag.putByte("Slot", (byte) i);
                moduleTag.putString("ItemId", s.getItem().toString());
                moduleTag.putInt("Count", s.getCount());
                modules.add(moduleTag);
            }
        }
        data.put("UpgradeModules", modules);

        ListTag inv = new ListTag();
        for (int i = 0; i < turret.inventory.getContainerSize(); i++) {
            ItemStack stack = turret.inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                inv.add(itemTag);
            }
        }
        data.put("Inventory", inv);

        ListTag equipment = new ListTag();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = turret.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putString("SlotName", slot.getName());
                stack.save(itemTag);
                equipment.add(itemTag);
            }
        }
        data.put("Equipment", equipment);

        root.put("Data", data);
        return root;
    }

    public static CompoundTag getDataTag(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains("Data")) return null;

        CompoundTag data = root.getCompound("Data");
        int version = root.contains("Version") ? root.getInt("Version") : 1;

        // v1 compatibility normalization
        if (version < 3) {
            if (!data.contains("FatalHitCount")) {
                data.putInt("FatalHitCount", data.contains("DropCount") ? data.getInt("DropCount") : 0);
            }
            if (!data.contains("HasTeleportModule")) {
                data.putBoolean("HasTeleportModule", false);
            }
        }
        return data;
    }
}
