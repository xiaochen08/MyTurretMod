package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Action packet for per-unit and global commands from mobile terminal.
 */
public class PacketMobileTerminalAction {
    public static final int ACTION_SET_FOLLOW = 0;
    public static final int ACTION_SET_GUARD = 1;
    public static final int ACTION_OMNI_SUMMON = 2;
    public static final int ACTION_ABS_DEFENSE = 3;
    public static final int ACTION_TEAM_SCAVENGE = 4;
    public static final int ACTION_TEAM_PURGE = 5;
    public static final int ACTION_RECALL_ALL = 6;
    private static final String OMNI_SUMMON_NEXT_TICK_TAG = "MobileTerminalOmniSummonNextTick";
    private static final int OMNI_SUMMON_COOLDOWN_TICKS = 300 * 20;
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    private final int action;
    private final UUID turretUuid;

    public PacketMobileTerminalAction(int action, UUID turretUuid) {
        this.action = action;
        this.turretUuid = turretUuid;
    }

    public static void encode(PacketMobileTerminalAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeUUID(msg.turretUuid == null ? NIL_UUID : msg.turretUuid);
    }

    public static PacketMobileTerminalAction decode(FriendlyByteBuf buf) {
        int action = buf.readVarInt();
        UUID uuid = buf.readUUID();
        return new PacketMobileTerminalAction(action, NIL_UUID.equals(uuid) ? null : uuid);
    }

    public static void handle(PacketMobileTerminalAction msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof MobileTerminalMenu menu)) return;
            if (!player.getUUID().equals(menu.ownerUuid())) return;

            List<SkeletonTurret> changed = new ArrayList<>();

            if (msg.action == ACTION_SET_FOLLOW || msg.action == ACTION_SET_GUARD) {
                if (msg.turretUuid == null) return;
                SkeletonTurret turret = MobileTerminalService.findOwnedByUuid(player, msg.turretUuid);
                if (turret == null) return;
                turret.setFollowMode(msg.action == ACTION_SET_FOLLOW);
                changed.add(turret);
            } else if (msg.action == ACTION_OMNI_SUMMON) {
                CompoundTag data = player.getPersistentData();
                long now = player.serverLevel().getGameTime();
                long nextTick = data.getLong(OMNI_SUMMON_NEXT_TICK_TAG);
                if (now < nextTick) {
                    long remainTicks = nextTick - now;
                    long remainSeconds = Math.max(1L, (remainTicks + 19L) / 20L);
                    player.sendSystemMessage(Component.translatable("message.examplemod.mobile_terminal.omni_summon_cooldown", remainSeconds));
                    player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                    return;
                }

                for (SkeletonTurret turret : SummonTerminalService.findOwnedTurrets(player)) {
                    turret.teleportToSafeSpotFromTerminal(player, true);
                    changed.add(turret);
                }
                if (changed.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.examplemod.mobile_terminal.omni_summon_no_units"));
                    player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                    return;
                }
                data.putLong(OMNI_SUMMON_NEXT_TICK_TAG, now + OMNI_SUMMON_COOLDOWN_TICKS);
                player.sendSystemMessage(Component.translatable("message.examplemod.mobile_terminal.omni_summon_triggered", changed.size()));
                player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.9f, 1.0f);
            } else if (msg.action == ACTION_ABS_DEFENSE) {
                for (SkeletonTurret turret : SummonTerminalService.findOwnedTurrets(player)) {
                    turret.setFollowMode(false);
                    changed.add(turret);
                }
            } else if (msg.action == ACTION_TEAM_SCAVENGE) {
                TurretCommands.executeScavenge(player);
                for (SkeletonTurret turret : SummonTerminalService.findOwnedTurrets(player)) {
                    changed.add(turret);
                }
            } else if (msg.action == ACTION_TEAM_PURGE) {
                TurretCommands.executePurge(player);
                for (SkeletonTurret turret : SummonTerminalService.findOwnedTurrets(player)) {
                    changed.add(turret);
                }
            } else if (msg.action == ACTION_RECALL_ALL) {
                TurretCommands.executeRecall(player);
                for (SkeletonTurret turret : SummonTerminalService.findOwnedTurrets(player)) {
                    changed.add(turret);
                }
            }

            for (SkeletonTurret turret : changed) {
                float distance = (float) Math.sqrt(turret.distanceToSqr(player));
                PacketHandler.INSTANCE.sendTo(
                        new PacketMobileTerminalDelta(MobileTerminalEntry.fromTurret(turret, distance)),
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                );
            }
        });
        ctx.setPacketHandled(true);
    }
}
