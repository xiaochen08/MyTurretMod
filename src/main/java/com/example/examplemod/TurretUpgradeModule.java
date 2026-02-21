package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

public interface TurretUpgradeModule {
    String id();
    default int priority() { return 0; }
    TurretModuleMetadata metadata();

    default void activate(SkeletonTurret turret, TurretModuleState state) {}

    default void deactivate(SkeletonTurret turret, TurretModuleState state) {}

    default int getCooldown(SkeletonTurret turret, TurretModuleState state) { return 0; }

    void refreshState(SkeletonTurret turret, TurretModuleState state);

    default void onTick(SkeletonTurret turret, TurretModuleState state) {}

    default void onRangedAttack(SkeletonTurret turret, LivingEntity primaryTarget, int tier, TurretModuleState state) {}

    default List<Component> getDisplayStats(int level) { return List.of(); }
}
