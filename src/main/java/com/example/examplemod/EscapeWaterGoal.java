package com.example.examplemod;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class EscapeWaterGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;

    // 构造函数：告诉它是谁要逃离水，速度是多少
    public EscapeWaterGoal(Mob mob, double speed) {
        this.mob = mob;
        this.speedModifier = speed;
        // 设置这个目标的互斥标志，表示它会控制移动和跳跃
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    // 判断是否可以使用这个 AI：只有在水里的时候才启动
    @Override
    public boolean canUse() {
        return this.mob.isInWater() && this.mob.getFluidHeight(net.minecraft.tags.FluidTags.WATER) > this.mob.getFluidJumpThreshold() || this.mob.isInLava();
    }

    // AI 执行时的逻辑：每时每刻尝试往上游
    @Override
    public void tick() {
        if (this.mob.getRandom().nextFloat() < 0.8F) {
            this.mob.getJumpControl().jump(); // 尝试跳跃（在水里就是上浮）
        }
        // 设置移动速度
        this.mob.setSpeed((float)this.speedModifier);
    }
}