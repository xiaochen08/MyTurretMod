package com.example.examplemod;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.resources.ResourceLocation;

public class TurretRenderer extends HumanoidMobRenderer<SkeletonTurret, TurretModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("examplemod", "textures/entity/skeleton_turret.png");

    public TurretRenderer(EntityRendererProvider.Context context) {
        super(context, new TurretModel(context.bakeLayer(TurretModel.LAYER_LOCATION)), 0.5F);
        
        // ✅ Fix: Add Armor Layer to support equipment rendering
        // The default HumanoidMobRenderer adds a layer using the main model (TurretModel) which has 0 deformation.
        // We must add a specific layer using SkeletonModel with INNER/OUTER armor deformations to avoid z-fighting.
        this.addLayer(new HumanoidArmorLayer<>(this, 
            new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)), 
            new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)), 
            context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(SkeletonTurret entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(SkeletonTurret entity) {
        return 0.0F; // ❌ 禁止死亡倒地旋转 (Death Rotation)
    }

    // Manually implementing render as requested (though super does the heavy lifting for vanilla models)
    @Override
    public void render(SkeletonTurret entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // 🏗️ Scale Up Effect during Printing (Spawn)
        if (entity.getPrintState() == 1 || entity.tickCount < 40) {
            float progress = Math.min(1.0F, entity.tickCount / 40.0F);
            // Easing function (Elastic Out)
            float scale = (float) (Math.pow(2, -10 * progress) * Math.sin((progress * 10 - 0.75) * (2 * Math.PI) / 3) + 1);
            if (progress >= 1.0F) scale = 1.0F;
            
            // Apply Scale
            poseStack.scale(scale, scale, scale);
            
            // 🟥 Flash Red if "Glitch" State (State 2 or 3) is about to happen? No, just keep it simple.
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
