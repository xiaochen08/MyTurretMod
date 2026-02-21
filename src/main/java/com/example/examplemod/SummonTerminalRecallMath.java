package com.example.examplemod;

import net.minecraft.world.phys.Vec3;

public final class SummonTerminalRecallMath {
    private SummonTerminalRecallMath() {}

    public static Vec3 computeAnchor(Vec3 playerPos, int seed) {
        double angle = (seed % 360) * (Math.PI / 180.0);
        double radius = 2.0;
        return new Vec3(
                playerPos.x + Math.cos(angle) * radius,
                playerPos.y,
                playerPos.z + Math.sin(angle) * radius
        );
    }
}
