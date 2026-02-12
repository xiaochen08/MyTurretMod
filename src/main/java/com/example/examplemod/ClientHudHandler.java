package com.example.examplemod;



import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class ClientHudHandler {

    // 你的贴图路径
    private static final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("examplemod", "textures/gui/hud/digital_vignette.png");

    @SubscribeEvent

    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        // 只在渲染 "Vignette" 层之后绘制，保证覆盖在最上层
        if (event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 1. 射线检测：玩家是否看着我们的炮台？
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hit).getEntity();

            if (target instanceof SkeletonTurret turret) {
                // 2. 检查炮台状态：是否正在打印(1) 或 死机(2)
                int state = turret.getPrintState();
                if (state == 1 || state == 2) {
                    renderDigitalVignette(event.getGuiGraphics(), state);
                }
            }
        }
    }

    private static void renderDigitalVignette(net.minecraft.client.gui.GuiGraphics gfx, int state) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 设置颜色：正常=青色，死机=红色
        if (state == 2) {
            RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 0.6f); // 红
        } else {
            RenderSystem.setShaderColor(0.0f, 1.0f, 1.0f, 0.6f); // 青
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, VIGNETTE_TEXTURE);

        // 铺满全屏
        int width = gfx.guiWidth();
        int height = gfx.guiHeight();

        gfx.blit(VIGNETTE_TEXTURE, 0, 0, -90, 0.0f, 0.0f, width, height, width, height);

        // 重置颜色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}