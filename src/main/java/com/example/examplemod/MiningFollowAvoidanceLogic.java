package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class MiningFollowAvoidanceLogic {
    private MiningFollowAvoidanceLogic() {}

    public static Direction primaryDirectionFromLook(Vec3 look) {
        double ax = Math.abs(look.x);
        double ay = Math.abs(look.y);
        double az = Math.abs(look.z);
        if (ay > ax && ay > az) {
            return look.y >= 0 ? Direction.UP : Direction.DOWN;
        }
        if (ax > az) {
            return look.x >= 0 ? Direction.EAST : Direction.WEST;
        }
        return look.z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static int manhattanDistance(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    public static Vec3 computeRetreatPosition(Vec3 turretPos, Vec3 ownerPos, Vec3 ownerLook, double step) {
        Vec3 away = turretPos.subtract(ownerPos);
        if (away.lengthSqr() < 1.0E-6) {
            away = ownerLook.scale(-1.0);
        }
        Vec3 dir = away.normalize();
        return turretPos.add(dir.scale(step));
    }

    public static Direction leftOf(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    public static Direction rightOf(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    public static BlockPos chooseNearestSideCandidate(BlockPos turretPos, BlockPos miningPos, Direction dir) {
        BlockPos left = miningPos.relative(leftOf(dir));
        BlockPos right = miningPos.relative(rightOf(dir));
        int dl = manhattanDistance(turretPos, left);
        int dr = manhattanDistance(turretPos, right);
        return dl <= dr ? left : right;
    }

    public static boolean shouldRecalculatePath(int continuousDirBlocks, long elapsedNanos) {
        return continuousDirBlocks > 3 && elapsedNanos <= 200_000_000L;
    }
}
