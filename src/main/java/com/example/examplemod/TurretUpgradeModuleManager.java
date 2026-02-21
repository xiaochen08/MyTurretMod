package com.example.examplemod;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class TurretUpgradeModuleManager {
    private List<TurretUpgradeModule> modules;
    private long registryRevision = -1L;
    private final TurretModuleState state = new TurretModuleState();

    public TurretUpgradeModuleManager(List<TurretUpgradeModule> modules) {
        reloadModules(modules);
        this.registryRevision = TurretModuleRegistry.revision();
    }

    public static TurretUpgradeModuleManager createDefault() {
        return new TurretUpgradeModuleManager(List.copyOf(TurretModuleRegistry.all()));
    }

    public TurretModuleState getState() {
        return state;
    }

    public void refresh(SkeletonTurret turret) {
        ensureRegistryUpToDate();
        state.reset();
        for (TurretUpgradeModule module : modules) {
            try {
                module.refreshState(turret, state);
            } catch (Exception e) {
                TurretModuleLog.error("refresh failed for module " + module.id(), e);
            }
        }
    }

    public void onTick(SkeletonTurret turret) {
        ensureRegistryUpToDate();
        for (TurretUpgradeModule module : modules) {
            try {
                module.onTick(turret, state);
            } catch (Exception e) {
                TurretModuleLog.error("tick failed for module " + module.id(), e);
            }
        }
    }

    public void onRangedAttack(SkeletonTurret turret, LivingEntity primaryTarget, int tier) {
        ensureRegistryUpToDate();
        for (TurretUpgradeModule module : modules) {
            try {
                module.onRangedAttack(turret, primaryTarget, tier, state);
            } catch (Exception e) {
                TurretModuleLog.error("ranged attack hook failed for module " + module.id(), e);
            }
        }
    }

    private void ensureRegistryUpToDate() {
        long currentRevision = TurretModuleRegistry.revision();
        if (currentRevision == registryRevision) {
            return;
        }
        reloadModules(List.copyOf(TurretModuleRegistry.all()));
        registryRevision = currentRevision;
    }

    private void reloadModules(List<TurretUpgradeModule> source) {
        this.modules = source.stream()
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .toList();
    }
}
