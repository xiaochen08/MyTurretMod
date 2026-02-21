package com.example.examplemod;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.monster.Monster;
import java.util.EnumSet;
import java.util.List;

public class TurretEmergencyTeleportGoal extends Goal {
    private final SkeletonTurret turret;
    private int closeMonsterTime = 0;
    private final double TRIGGER_DISTANCE_SQR = 3.0 * 3.0; // 3 meters
    private int triggerEntityId = -1;
    private Vec3 startPos = Vec3.ZERO;

    public TurretEmergencyTeleportGoal(SkeletonTurret turret) {
        this.turret = turret;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK)); // Exclusive priority
    }

    @Override
    public boolean canUse() {
        if (turret.level().isClientSide) return false;
        if (!turret.isFollowing()) return false;
        
        // Check for Teleport Module and Cooldown
        if (!turret.hasTeleportModule() || !turret.canTeleport()) return false;
        
        // 1. Check if we are already safe (optimization) or if teleport is on cooldown
        // (Cooldown is usually handled by goal selector delay, but we can add one if needed)
        
        // 2. Scan for monsters within 3m
        List<Monster> nearbyMonsters = turret.level().getEntitiesOfClass(
            Monster.class, 
            turret.getBoundingBox().inflate(3.0),
            m -> m != turret && m.isAlive() && !m.isAlliedTo(turret)
        );

        boolean dangerous = false;
        for (Monster m : nearbyMonsters) {
            if (m.distanceToSqr(turret) < TRIGGER_DISTANCE_SQR) {
                dangerous = true;
                this.triggerEntityId = m.getId();
                break;
            }
        }

        if (dangerous) {
            closeMonsterTime++;
        } else {
            closeMonsterTime = 0;
            return false;
        }

        // 3. Condition: Persist for >= 1.5s (30 ticks)
        return closeMonsterTime >= 30;
    }

    @Override
    public void start() {
        this.startPos = turret.position();
        
        // Spawn particles at start position before teleporting
        if (turret.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, startPos.x, startPos.y + 0.5, startPos.z, 16, 0.5, 0.5, 0.5, 0.1);
        }

        teleportToPlayer();
        closeMonsterTime = 0; // Reset counter
    }

    private void teleportToPlayer() {
        LivingEntity owner = turret.getOwner();
        if (owner == null && turret.getOwnerUUID() != null) {
             Player p = turret.level().getPlayerByUUID(turret.getOwnerUUID());
             if (p != null) owner = p;
        }
        
        if (owner == null) return; 

        // 1. Calculate target: Player + 4m random radius
        double angle = turret.getRandom().nextDouble() * 2 * Math.PI;
        double radius = 4.0;
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        
        double targetX = owner.getX() + offsetX;
        double targetY = owner.getY();
        double targetZ = owner.getZ() + offsetZ;

        // 2. Find safe Y (simple ground check)
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(targetX, targetY, targetZ);
        
        // Search from slightly above owner (head level) downwards to find a valid standing spot
        // Try range: OwnerY+1 down to OwnerY-2
        pos.set(targetX, targetY + 1, targetZ);
        boolean found = false;
        
        for (int i = 0; i < 4; i++) {
            if (isSafePos(pos)) {
                found = true;
                targetY = pos.getY(); // Update Y to the safe spot
                break;
            }
            pos.move(0, -1, 0);
        }
        
        // Execute through unified teleport gateway (emergency teleport does not spawn black hole)
        final double teleportY = targetY;
        boolean success = TeleportRequestGateway.guardTurretTeleport(
                turret,
                TeleportRequestSource.TURRET_EMERGENCY,
                false,
                () -> turret.randomTeleport(targetX, teleportY, targetZ, true)
        );
        if (!success) {
            return;
        }

        // 3. Post-teleport effects
        turret.notifyTeleport();
        turret.onTeleportCompleted(startPos, false);
        
        // 4. Log
        System.out.printf("[TurretLog] Time=%d, Reason=Emergency, Start=[%.2f, %.2f, %.2f], End=[%.2f, %.2f, %.2f], TriggerEntityID=%d%n",
            turret.level().getGameTime(),
            startPos.x, startPos.y, startPos.z,
            targetX, targetY, targetZ,
            triggerEntityId
        );
    }
    
    private boolean isSafePos(BlockPos pos) {
        // Simple check: Block below is solid, block at pos is empty, block above is empty
        return turret.level().getBlockState(pos.below()).isSolid() && 
               turret.level().isEmptyBlock(pos) && 
               turret.level().isEmptyBlock(pos.above());
    }
}
