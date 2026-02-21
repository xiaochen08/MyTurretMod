package com.example.examplemod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

// 🚍 这辆车是 Bus.FORGE (默认，游戏运行时专用)
@Mod.EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onRenderEntity(RenderLivingEvent.Post<net.minecraft.world.entity.LivingEntity, ?> event) {
        // 1. 只处理我们的炮台
        if (!(event.getEntity() instanceof SkeletonTurret turret)) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        Font font = Minecraft.getInstance().font;

        // 2. 获取数据 (普通状态)
        String status = turret.getOverheadStatus();
        if (status == null || status.isEmpty()) return;

        // 3. 准备绘图工具
        poseStack.pushPose();

        // 4. 坐标调整 (原版渲染器的原点在脚底)
        // 名字牌一般在 height + 0.5
        // 我们往上一点，+0.85
        double height = turret.getBbHeight() + 0.85D;
        poseStack.translate(0.0D, height, 0.0D);

        // 旋转面向相机
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // 缩放
        float scale = 0.025F;
        poseStack.scale(-scale, -scale, scale);

        // 5. 开始绘制
        Matrix4f matrix4f = poseStack.last().pose();
        int packedLightCoords = 0xF000F0; // 满亮度

        Component textComp = TurretTextResolver.resolveOverheadStatus(status);
        float xOffset = (float)(-font.width(textComp) / 2);

        // 关闭深度测试 (透视)
        RenderSystem.disableDepthTest();

        // 画文字
        font.drawInBatch(textComp, xOffset, 0, 0xFFFFFFFF, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, packedLightCoords);

        // 恢复深度测试
        RenderSystem.enableDepthTest();

        poseStack.popPose();
    }
}
