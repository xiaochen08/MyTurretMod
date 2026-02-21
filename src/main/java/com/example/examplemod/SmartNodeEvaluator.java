package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Smart node evaluator for short-gap traversal.
 *
 * Behavior:
 * - Keeps short suspended air cells as OPEN instead of treating them as hard stops.
 * - Adds explicit jump-across neighbors with a high malus, so flat ground still wins.
 */
public class SmartNodeEvaluator extends WalkNodeEvaluator {
    private static final int MAX_GAP_BLOCKS = 2;
    private static final float GAP_OPEN_EXTRA_COST = 6.0F;
    private static final float EXTREME_VERTICAL_COST = 4096.0F;

    private final Supplier<TurretMobilityProfile> profileSupplier;
    private int startY;

    public SmartNodeEvaluator() {
        this(() -> TurretMobilityProfile.SCOUT_BOT);
    }

    public SmartNodeEvaluator(Supplier<TurretMobilityProfile> profileSupplier) {
        this.profileSupplier = Objects.requireNonNull(profileSupplier, "profileSupplier");
        this.startY = Integer.MIN_VALUE;
    }

    private TurretMobilityProfile profile() {
        TurretMobilityProfile profile = this.profileSupplier.get();
        return profile == null ? TurretMobilityProfile.SCOUT_BOT : profile;
    }

    @Override
    public Node getStart() {
        Node start = super.getStart();
        this.startY = start.y;
        return start;
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, Mob mob) {
        BlockPathTypes vanillaType = super.getBlockPathType(level, x, y, z, mob);
        if (isShortGapAirCell(level, x, y, z)) {
            return BlockPathTypes.OPEN;
        }
        return vanillaType;
    }

    @Override
    public int getNeighbors(Node[] output, Node node) {
        int count = super.getNeighbors(output, node);
        count = addGapJumpNeighbors(output, count, node);
        applyVerticalPenalty(output, count);
        return count;
    }

    private int addGapJumpNeighbors(Node[] output, int count, Node from) {
        int[][] dirs = new int[][]{
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };

        for (int[] dir : dirs) {
            if (count >= output.length) {
                return count;
            }

            int dx = dir[0];
            int dz = dir[1];

            for (int gap = 1; gap <= MAX_GAP_BLOCKS; gap++) {
                int gapX = from.x + dx * gap;
                int gapZ = from.z + dz * gap;
                if (!isJumpableGapCell(gapX, from.y, gapZ)) {
                    break;
                }

                int landingX = from.x + dx * (gap + 1);
                int landingZ = from.z + dz * (gap + 1);
                Node landingNode = findLandingNode(landingX, from.y, landingZ, gap);
                if (landingNode != null && !containsNode(output, count, landingNode) && !landingNode.closed) {
                    output[count++] = landingNode;
                    break;
                }
            }
        }

        return count;
    }

    private Node findLandingNode(int x, int baseY, int z, int gapSize) {
        for (int dy = 1; dy >= -1; dy--) {
            int y = baseY + dy;
            if (!isSafeLandingCell(x, y, z)) {
                continue;
            }

            Node node = this.getNode(x, y, z);
            node.type = BlockPathTypes.OPEN;

            float base = Math.max(0.0F, this.mob.getPathfindingMalus(BlockPathTypes.OPEN));
            float heightPenalty = Math.abs(dy) * 0.75F;
            node.costMalus = Math.max(node.costMalus, base + GAP_OPEN_EXTRA_COST + gapSize + heightPenalty);
            applyVerticalPenalty(node);
            return node;
        }

        return null;
    }

    private void applyVerticalPenalty(Node[] output, int count) {
        for (int i = 0; i < count; i++) {
            applyVerticalPenalty(output[i]);
        }
    }

    private void applyVerticalPenalty(Node node) {
        int originY = this.startY == Integer.MIN_VALUE ? node.y : this.startY;
        int verticalDelta = Math.abs(node.y - originY);
        int allowedVertical = profile().maxVerticalPathing();
        if (verticalDelta > allowedVertical) {
            float penalty = EXTREME_VERTICAL_COST + (verticalDelta - allowedVertical) * 512.0F;
            node.costMalus = Math.max(node.costMalus, penalty);
        }
    }

    private boolean isShortGapAirCell(BlockGetter level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        BlockState belowState = level.getBlockState(feet.below());
        FluidState belowFluid = belowState.getFluidState();

        if (!feetState.isAir() || !headState.isAir()) {
            return false;
        }
        if (!belowState.isAir()) {
            return false;
        }
        if (!belowFluid.isEmpty()) {
            return false;
        }

        int[][] dirs = new int[][]{
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };
        for (int[] dir : dirs) {
            int dx = dir[0];
            int dz = dir[1];
            for (int d = 1; d <= MAX_GAP_BLOCKS; d++) {
                if (isSafeLandingCell(x + dx * d, y, z + dz * d)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isJumpableGapCell(int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockState feetState = this.level.getBlockState(feet);
        BlockState headState = this.level.getBlockState(feet.above());
        BlockState belowState = this.level.getBlockState(feet.below());
        FluidState belowFluid = belowState.getFluidState();

        return feetState.isAir()
                && headState.isAir()
                && belowState.isAir()
                && belowFluid.isEmpty();
    }

    private boolean isSafeLandingCell(int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockState feetState = this.level.getBlockState(feet);
        BlockState headState = this.level.getBlockState(feet.above());
        BlockState belowState = this.level.getBlockState(feet.below());

        if (!feetState.isAir() || !headState.isAir()) {
            return false;
        }
        if (!belowState.blocksMotion() || !belowState.getFluidState().isEmpty()) {
            return false;
        }

        BlockPathTypes type = super.getBlockPathType(this.level, x, y, z, this.mob);
        float malus = this.mob.getPathfindingMalus(type);
        if (malus < 0.0F) {
            return false;
        }

        return type != BlockPathTypes.DANGER_FIRE
                && type != BlockPathTypes.DANGER_OTHER
                && type != BlockPathTypes.DAMAGE_FIRE
                && type != BlockPathTypes.DAMAGE_OTHER
                && type != BlockPathTypes.LAVA;
    }

    private static boolean containsNode(Node[] output, int count, Node candidate) {
        for (int i = 0; i < count; i++) {
            Node current = output[i];
            if (current.x == candidate.x && current.y == candidate.y && current.z == candidate.z) {
                return true;
            }
        }
        return false;
    }
}
