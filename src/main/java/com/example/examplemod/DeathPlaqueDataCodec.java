package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class DeathPlaqueDataCodec {

    // 从物品堆中获取 NBT 数据
    public static CompoundTag getDataTag(ItemStack stack) {
        // 如果物品有数据标签，直接返回；如果没有，创建一个空的
        return stack.hasTag() ? stack.getTag() : new CompoundTag();
    }

    // 将炮台的数据打包成一个标签（Tag），用于记录在物品上
    public static CompoundTag buildFromTurret(SkeletonTurret turret, int fatalHits) {
        CompoundTag tag = new CompoundTag();

        // 记录炮台的名字
        if (turret.hasCustomName()) {
            tag.putString("TurretName", turret.getCustomName().getString());
        } else {
            tag.putString("TurretName", "未命名机体");
        }

        // 记录杀敌数
        tag.putInt("KillCount", turret.getEntityData().get(SkeletonTurret.KILL_COUNT));

        // 记录被击毁时的致命伤害次数
        tag.putInt("FatalHits", fatalHits);

        // 记录炮台等级
        tag.putInt("Tier", turret.getTier());

        return tag;
    }
}