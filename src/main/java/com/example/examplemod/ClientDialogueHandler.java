package com.example.examplemod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
public class ClientDialogueHandler {

    private static final Map<Integer, DialogueState> activeDialogues = new HashMap<>();
    private static final Random RAND = new Random();

    // 👇👇👇 核心修改 1：动态时长算法 👇👇👇
    private static class DialogueState {
        String fullText;
        int totalTicks;     // 总存活时间
        int currentTick;    // 当前播放时间
        boolean isGlitching;// 是否乱码

        // 定义打字速度：每 3 tick 蹦出一个字
        static final int TYPE_SPEED = 3;

        public DialogueState(String text) {
            this.fullText = text;
            this.currentTick = 0;
            this.isGlitching = false;

            // 算法：打字所需时间 + 60 tick (3秒) 阅读时间
            int typingTime = text.length() * TYPE_SPEED;
            int readTime = 60; // 停留 3 秒

            this.totalTicks = typingTime + readTime;
        }
    }

    // 接收网络包发来的指令
    public static void startDialogue(int entityId, String text) {
        activeDialogues.put(entityId, new DialogueState(text));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Iterator<Map.Entry<Integer, DialogueState>> it = activeDialogues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, DialogueState> entry = it.next();
                if (updateLogic(entry.getValue())) {
                    it.remove();
                }
            }
        }
    }

    private static boolean updateLogic(DialogueState state) {
        state.currentTick++;
        if (state.currentTick > state.totalTicks) {
            return true; // 时间到，应该被移除
        }
        // 5% 概率触发乱码，每 5 tick 判定一次，避免闪烁过快
        if (state.currentTick % 5 == 0) {
            state.isGlitching = RAND.nextFloat() < 0.01f;
        }
        return false;
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof SkeletonTurret)) return;
        if (!activeDialogues.containsKey(entity.getId())) return;

        DialogueState state = activeDialogues.get(entity.getId());
        renderUI(event.getPoseStack(), event.getMultiBufferSource(), entity, state);
    }

    private static void renderUI(PoseStack poseStack, MultiBufferSource buffer, Entity entity, DialogueState state) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        if (mc.player.distanceToSqr(entity) > 256) return;

        poseStack.pushPose();

        float height = entity.getBbHeight() + 1.2f;
        poseStack.translate(0, height, 0);

        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        // 👇👇👇 核心修改 2：应用新的打字速度 👇👇👇
        int charCount = state.currentTick / DialogueState.TYPE_SPEED;

        // 防止数组越界
        if (charCount > state.fullText.length()) charCount = state.fullText.length();
        if (charCount < 0) charCount = 0;

        String showText = state.fullText.substring(0, charCount);

        // --- 乱码特效 ---
        if (state.isGlitching && showText.length() > 0) {
            String glitchChar = String.valueOf("#&%@$?!§".charAt(RAND.nextInt(8)));
            showText = showText.substring(0, showText.length() - 1) + "§c" + glitchChar;
        }

        // --- 绘制背景黑框 ---
        int textWidth = font.width(showText);
        int halfWidth = textWidth / 2;
        int bgPadding = 4;

        // 只有当有文字显示时才画框，避免刚开始是个空框
        if (showText.length() > 0) {
            RenderSystem.disableDepthTest();
            fill(poseStack, buffer, -halfWidth - bgPadding, -5, halfWidth + bgPadding, 12, 0x80000000);
            RenderSystem.enableDepthTest();
        }

        // --- 绘制文字 ---
        // 使用 SEE_THROUGH 模式让文字可以透视方块 (像名字牌一样)
        font.drawInBatch(showText, -halfWidth, 0, 0xFFFFFF, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);

        poseStack.popPose();
    }

    private static void fill(PoseStack poseStack, MultiBufferSource buffer, int minX, int minY, int maxX, int maxY, int color) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.guiOverlay());

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        vertexConsumer.vertex(matrix, minX, maxY, 0).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix, maxX, maxY, 0).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix, maxX, minY, 0).color(r, g, b, a).endVertex();
        vertexConsumer.vertex(matrix, minX, minY, 0).color(r, g, b, a).endVertex();
    }
}