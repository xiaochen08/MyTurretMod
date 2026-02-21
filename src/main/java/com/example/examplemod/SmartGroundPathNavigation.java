package com.example.examplemod;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Ground navigator that swaps vanilla node evaluation for SmartNodeEvaluator.
 */
public class SmartGroundPathNavigation extends GroundPathNavigation {
    /**
     * IMPORTANT:
     * PathNavigation's ctor calls createPathFinder() before this subclass ctor body runs.
     * So this field must always be non-null at declaration time.
     */
    private Supplier<TurretMobilityProfile> profileSupplier = () -> TurretMobilityProfile.SCOUT_BOT;

    public SmartGroundPathNavigation(Mob mob, Level level) {
        this(mob, level, () -> TurretMobilityProfile.SCOUT_BOT);
    }

    public SmartGroundPathNavigation(Mob mob, Level level, Supplier<TurretMobilityProfile> profileSupplier) {
        super(mob, level);
        this.profileSupplier = Objects.requireNonNullElse(profileSupplier, () -> TurretMobilityProfile.SCOUT_BOT);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        // Pass a dynamic resolver so the evaluator always sees the latest profile.
        this.nodeEvaluator = new SmartNodeEvaluator(this::currentProfile);
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    private TurretMobilityProfile currentProfile() {
        Supplier<TurretMobilityProfile> supplier = this.profileSupplier;
        if (supplier == null) {
            return TurretMobilityProfile.SCOUT_BOT;
        }
        TurretMobilityProfile profile = supplier.get();
        return profile == null ? TurretMobilityProfile.SCOUT_BOT : profile;
    }
}
