package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class DeathPlaqueDataCodec {
    private static final int CURRENT_VERSION = 1;

    public static CompoundTag getDataTag(ItemStack stack) {
        return stack.hasTag() ? stack.getTag() : new CompoundTag();
    }

    public static CompoundTag buildFromTurret(SkeletonTurret turret, int fatalHits) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", CURRENT_VERSION);
        tag.putInt("UnitID", turret.getEntityData().get(SkeletonTurret.UNIT_ID));
        tag.putInt("FatalHits", Math.max(1, fatalHits));
        tag.putInt("Tier", turret.getTier());
        tag.putInt("KillCount", turret.getKillCount());
        tag.putInt("UpgradeProgress", turret.getUpgradeProgress());
        tag.putBoolean("IsBrutal", turret.isBrutal());

        // restoreFromRecord reads "BaseName".
        String baseName = turret.getBaseName();
        if (baseName != null && !baseName.isBlank()) {
            tag.putString("BaseName", baseName);
            tag.putString("TurretName", baseName);
        }

        if (turret.getOwnerUUID() != null) {
            tag.putUUID("OwnerUUID", turret.getOwnerUUID());
        }

        // Full inventory snapshot, including module slots (5-9).
        ListTag inventoryList = new ListTag();
        for (int i = 0; i < turret.inventory.getContainerSize(); i++) {
            ItemStack stack = turret.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) i);
            stack.save(itemTag);
            inventoryList.add(itemTag);
        }
        tag.put("Inventory", inventoryList);

        // Equipment snapshot for exact restore.
        ListTag equipmentList = new ListTag();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = turret.getItemBySlot(slot);
            if (equipped.isEmpty()) {
                continue;
            }
            CompoundTag equipTag = new CompoundTag();
            equipTag.putString("SlotName", slot.getName());
            equipped.save(equipTag);
            equipmentList.add(equipTag);
        }
        tag.put("Equipment", equipmentList);

        return tag;
    }
}