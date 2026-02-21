package com.example.examplemod;

/**
 * Strategy-style mobility profile for turret locomotion and pathing.
 */
public enum TurretMobilityProfile {
    HEAVY_MECH(2.0F, 0.5F, 0.05D, 0.2D, 2),
    SCOUT_BOT(0.6F, 1.5F, 0.15D, 0.6D, 4);

    private final float stepHeight;
    private final float jumpStrength;
    private final double jumpImpulseXZ;
    private final double jumpImpulseY;
    private final int maxVerticalPathing;

    TurretMobilityProfile(float stepHeight, float jumpStrength, double jumpImpulseXZ, double jumpImpulseY, int maxVerticalPathing) {
        this.stepHeight = stepHeight;
        this.jumpStrength = jumpStrength;
        this.jumpImpulseXZ = jumpImpulseXZ;
        this.jumpImpulseY = jumpImpulseY;
        this.maxVerticalPathing = maxVerticalPathing;
    }

    public float stepHeight() {
        return stepHeight;
    }

    public float jumpStrength() {
        return jumpStrength;
    }

    public double jumpImpulseXZ() {
        return jumpImpulseXZ;
    }

    public double jumpImpulseY() {
        return jumpImpulseY;
    }

    public int maxVerticalPathing() {
        return maxVerticalPathing;
    }

    /**
     * Keep legacy stuck behavior threshold while making MoveControl fully profile-driven.
     */
    public int stuckTicksThreshold() {
        return 5;
    }
}
