package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Tactical movement controller driven by TurretMobilityProfile strategy.
 */
public class SmartMoveControl extends MoveControl {
    private final Supplier<TurretMobilityProfile> profileSupplier;
    private TurretMobilityProfile fixedProfile;

    private Vec3 lastXZ;
    private int stuckTicks;

    public SmartMoveControl(Mob mob, Supplier<TurretMobilityProfile> profileSupplier) {
        super(mob);
        this.profileSupplier = Objects.requireNonNull(profileSupplier, "profileSupplier");
        this.fixedProfile = null;
        this.lastXZ = new Vec3(mob.getX(), 0.0D, mob.getZ());
        this.stuckTicks = 0;
    }

    public SmartMoveControl(Mob mob, TurretMobilityProfile profile) {
        this(mob, () -> profile);
        this.fixedProfile = profile;
    }

    public void setMobilityProfile(TurretMobilityProfile profile) {
        this.fixedProfile = profile;
    }

    private TurretMobilityProfile profile() {
        if (this.fixedProfile != null) {
            return this.fixedProfile;
        }
        TurretMobilityProfile profile = this.profileSupplier.get();
        return profile == null ? TurretMobilityProfile.SCOUT_BOT : profile;
    }

    @Override
    public void tick() {
        PathNavigation navigation = this.mob.getNavigation();
        Path path = navigation != null ? navigation.getPath() : null;
        Node nextNode = path != null && !path.isDone() ? path.getNextNode() : null;

        super.tick();

        if (nextNode != null) {
            handlePredictiveUphillJump(nextNode);
            handlePredictiveGapSprintJump(nextNode);
            handleFrontBlockedFlank(nextNode, path);
        }

        handleStuckRecovery(path, nextNode);
    }

    private void handlePredictiveUphillJump(Node nextNode) {
        TurretMobilityProfile profile = profile();
        double yDiff = (double) nextNode.y - this.mob.getY();
        double uphillThreshold = Math.max(0.35D, profile.stepHeight() * 0.6D);
        if (this.mob.onGround() && yDiff > uphillThreshold) {
            this.mob.getJumpControl().jump();
            Vec3 forward = horizontalDirTo(nextNode);
            this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(
                    forward.x * (profile.jumpImpulseXZ() * 0.35D),
                    profile.jumpImpulseY() * 0.35D,
                    forward.z * (profile.jumpImpulseXZ() * 0.35D)
            ));
        }
    }

    private void handlePredictiveGapSprintJump(Node nextNode) {
        if (!this.mob.onGround()) {
            return;
        }

        TurretMobilityProfile profile = profile();
        Vec3 forward = horizontalDirTo(nextNode);
        if (forward.lengthSqr() < 1.0E-6D) {
            return;
        }
        if (!hasShortSafeGapAhead(forward)) {
            return;
        }

        this.mob.setSprinting(true);
        this.mob.getJumpControl().jump();
        this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(
                forward.x * profile.jumpImpulseXZ(),
                profile.jumpImpulseY(),
                forward.z * profile.jumpImpulseXZ()
        ));
    }

    private void handleStuckRecovery(Path path, Node nextNode) {
        if (path == null || path.isDone() || nextNode == null) {
            this.stuckTicks = 0;
            this.lastXZ = new Vec3(this.mob.getX(), 0.0D, this.mob.getZ());
            return;
        }

        TurretMobilityProfile profile = profile();
        double stuckDelta = Math.max(0.02D, profile.jumpImpulseXZ() * 0.25D);
        double stuckDeltaSqr = stuckDelta * stuckDelta;

        Vec3 nowXZ = new Vec3(this.mob.getX(), 0.0D, this.mob.getZ());
        if (nowXZ.distanceToSqr(this.lastXZ) < stuckDeltaSqr) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = 0;
            this.lastXZ = nowXZ;
        }

        if (this.stuckTicks <= profile.stuckTicksThreshold()) {
            return;
        }

        Vec3 forward = horizontalDirTo(nextNode);
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).normalize();
        if (this.mob.getRandom().nextBoolean()) {
            side = side.scale(-1.0D);
        }

        this.mob.getJumpControl().jump();
        this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(
                side.x * (profile.jumpImpulseXZ() * 0.9D),
                profile.jumpImpulseY() * 0.7D,
                side.z * (profile.jumpImpulseXZ() * 0.9D)
        ));
        this.lastXZ = nowXZ;
        this.stuckTicks = 0;
    }

    private void handleFrontBlockedFlank(Node nextNode, Path path) {
        if (path == null || path.isDone()) {
            return;
        }

        TurretMobilityProfile profile = profile();
        Vec3 forward = horizontalDirTo(nextNode);
        if (forward.lengthSqr() < 1.0E-6D) {
            return;
        }

        double frontRayLen = Math.max(0.9D, this.mob.getBbWidth() + profile.jumpImpulseXZ() * 3.0D);
        Vec3 eye = this.mob.getEyePosition();
        if (!isRayBlocked(eye, eye.add(forward.scale(frontRayLen)))) {
            return;
        }

        Vec3 left45 = rotateYaw(forward, 45.0D).normalize();
        Vec3 right45 = rotateYaw(forward, -45.0D).normalize();
        boolean leftClear = !isRayBlocked(eye, eye.add(left45.scale(frontRayLen)));
        boolean rightClear = !isRayBlocked(eye, eye.add(right45.scale(frontRayLen)));
        if (!leftClear && !rightClear) {
            return;
        }

        Vec3 chosen;
        float strafeSign;
        float strafeMagnitude = (float) Math.min(0.75D, 0.2D + profile.jumpImpulseXZ() * 2.5D);
        if (leftClear && !rightClear) {
            chosen = left45;
            strafeSign = -strafeMagnitude;
        } else if (!leftClear) {
            chosen = right45;
            strafeSign = strafeMagnitude;
        } else {
            boolean goLeft = this.mob.getRandom().nextBoolean();
            chosen = goLeft ? left45 : right45;
            strafeSign = goLeft ? -strafeMagnitude : strafeMagnitude;
        }

        this.mob.setXxa(strafeSign);
        this.mob.setZza((float) Math.min(0.5D, 0.15D + profile.jumpImpulseXZ()));
        this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(
                chosen.x * (profile.jumpImpulseXZ() * 0.6D),
                0.0D,
                chosen.z * (profile.jumpImpulseXZ() * 0.6D)
        ));
    }

    private boolean hasShortSafeGapAhead(Vec3 forward) {
        for (int i = 1; i <= 2; i++) {
            BlockPos midFeet = BlockPos.containing(
                    this.mob.getX() + forward.x * i,
                    this.mob.getY(),
                    this.mob.getZ() + forward.z * i
            );
            if (!isGapSegment(midFeet)) {
                break;
            }

            BlockPos landingFeet = BlockPos.containing(
                    this.mob.getX() + forward.x * (i + 1),
                    this.mob.getY(),
                    this.mob.getZ() + forward.z * (i + 1)
            );
            if (isSafeLanding(landingFeet)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGapSegment(BlockPos feet) {
        return this.mob.level().getBlockState(feet).isAir()
                && this.mob.level().getBlockState(feet.above()).isAir()
                && this.mob.level().getBlockState(feet.below()).isAir()
                && this.mob.level().getFluidState(feet.below()).isEmpty();
    }

    private boolean isSafeLanding(BlockPos feet) {
        return this.mob.level().getBlockState(feet).isAir()
                && this.mob.level().getBlockState(feet.above()).isAir()
                && this.mob.level().getBlockState(feet.below()).blocksMotion()
                && this.mob.level().getFluidState(feet.below()).isEmpty();
    }

    private boolean isRayBlocked(Vec3 from, Vec3 to) {
        HitResult hit = this.mob.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private Vec3 horizontalDirTo(Node nextNode) {
        Vec3 toNext = new Vec3(
                (double) nextNode.x + 0.5D - this.mob.getX(),
                0.0D,
                (double) nextNode.z + 0.5D - this.mob.getZ()
        );
        return toNext.lengthSqr() < 1.0E-6D ? Vec3.ZERO : toNext.normalize();
    }

    private static Vec3 rotateYaw(Vec3 vec, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(
                vec.x * cos - vec.z * sin,
                vec.y,
                vec.x * sin + vec.z * cos
        );
    }
}
