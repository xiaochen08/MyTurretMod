package com.example.examplemod;

import net.minecraft.world.entity.LivingEntity;

public interface TurretModuleAbility {
    default void onTick(SkeletonTurret turret, TurretModuleState state, int level) {}

    default void onRangedAttack(SkeletonTurret turret, LivingEntity primaryTarget, int turretTier, TurretModuleState state, int level) {}
}
