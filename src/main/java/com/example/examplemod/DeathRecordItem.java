package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public class DeathRecordItem extends Item {

    public DeathRecordItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("Version"); // 有版本号才闪烁
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = context.getItemInHand();
        CompoundTag masterTag = itemStack.getTag();

        // 1. 基础合法性检查
        if (masterTag == null || !masterTag.contains("Version")) {
            // 兼容旧版逻辑 (如果存在)
            if (masterTag != null && masterTag.contains("SavedUnitId")) {
                return restoreLegacy(context, masterTag, serverLevel);
            }
            Player player = context.getPlayer();
            if (player != null) {
                player.sendSystemMessage(Component.literal("§c[错误] 记录卡数据无效或版本不兼容！"));
            }
            return InteractionResult.FAIL;
        }

        // 2. 数据完整性校验 (Checksum)
        CompoundTag dataTag = masterTag.getCompound("Data");
        String currentChecksum = Integer.toHexString(dataTag.toString().hashCode());
        String savedChecksum = masterTag.getString("Checksum");

        boolean isCorrupted = !currentChecksum.equals(savedChecksum);
        if (isCorrupted) {
            Player player = context.getPlayer();
            if (player != null) {
                player.sendSystemMessage(Component.literal("§e[警告] 主数据校验失败，尝试使用备份数据..."));
            }
            if (masterTag.contains("Backup")) {
                dataTag = masterTag.getCompound("Backup");
                // 再次校验备份
                // 简单的备份可用性检查 (实际应有单独的校验和)
                if (dataTag.isEmpty()) {
                    if (player != null) {
                        player.sendSystemMessage(Component.literal("§c[错误] 备份数据也已损坏，无法恢复！"));
                    }
                    return InteractionResult.FAIL;
                }
            } else {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§c[错误] 数据损坏且无备份！"));
                }
                return InteractionResult.FAIL;
            }
        }

        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos spawnPos = pos.relative(direction);

        double spawnX = spawnPos.getX() + 0.5;
        double spawnY = spawnPos.getY();
        double spawnZ = spawnPos.getZ() + 0.5;
        if (dataTag.contains("DeathX") && dataTag.contains("DeathY") && dataTag.contains("DeathZ")) {
            spawnX = dataTag.getDouble("DeathX");
            spawnY = dataTag.getDouble("DeathY");
            spawnZ = dataTag.getDouble("DeathZ");
        }

        // 3. 实体生成与数据恢复
        Entity entity = ExampleMod.TURRET_ENTITY.get().create(serverLevel);
        if (entity instanceof SkeletonTurret turret) {
            turret.moveTo(spawnX, spawnY, spawnZ, context.getRotation(), 0.0F);

            // [A] 恢复基础属性
            turret.getEntityData().set(SkeletonTurret.UNIT_ID, dataTag.getInt("UnitID"));
            // RangeLevel is derived from Tier now

            // 注意：Tier 可能是私有的，需要 setter，或者通过 EntityData 直接设置如果它是 public static
            // 查看 SkeletonTurret 源码，TIER 是 private static final，但有 getTier/setTier 方法吗？
            // 假设我们直接操作 EntityData，因为 TIER 的 ID 是 SkeletonTurret.TIER
            // 由于 TIER 是 private，我们需要通过 turret.getEntityData().set(...) 但 TIER 变量本身不可见
            // 必须依赖 SkeletonTurret 提供的 public 方法或 public 字段
            // 查看 SkeletonTurret.java，TIER 是 private static final... 
            // 这里的 SkeletonTurret.TIER 访问不到。
            // 这是一个问题。我们需要在 SkeletonTurret 中添加 public static 访问器 或者 public setTier 方法。
            // 暂时假设我们添加了 setTier 方法或者 TIER 变成了 public。
            // 为了安全，我稍后会修改 SkeletonTurret.java 确保 TIER 可访问或提供 setter。
            // 现有的 SkeletonTurret 代码中：
            // private static final EntityDataAccessor<Integer> TIER = ...
            // 没有 setTier 方法? 
            // 让我们先写代码，稍后去 SkeletonTurret.java 补充 setter。
            
            // 暂时使用 NBT 恢复方式 (如果 readAdditionalSaveData 支持的话)
            // 或者直接用 EntityData 如果能访问。
            // 让我们在 SkeletonTurret 添加 restoreFromRecord(CompoundTag tag) 方法，这样更封装。
            // 这样 DeathRecordItem 只需要调用 turret.restoreFromRecord(dataTag) 即可。
            // 这是一个更好的设计模式。
            
            turret.restoreFromRecord(dataTag);

            // 4. 状态设定
            turret.setHealth(turret.getMaxHealth()); // 复活满血
            turret.getPersistentData().putBoolean("RecordSummoned", true);

            serverLevel.addFreshEntity(turret);
            itemStack.shrink(1);

            // Effects
            level.playSound(null, spawnPos, SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            
            Player player = context.getPlayer();
            if (player != null) {
                player.sendSystemMessage(Component.literal("§a[系统] 战术人形已重构 (ID:" + dataTag.getInt("UnitID") + ")"));
                if (isCorrupted) {
                    player.sendSystemMessage(Component.literal("§e[注意] 数据通过备份恢复"));
                }
            }
        }

        return InteractionResult.CONSUME;
    }
    
    // 旧版兼容逻辑
    private InteractionResult restoreLegacy(UseOnContext context, CompoundTag tag, ServerLevel level) {
         BlockPos pos = context.getClickedPos();
         Direction direction = context.getClickedFace();
         BlockPos spawnPos = pos.relative(direction);
         
         Entity entity = ExampleMod.TURRET_ENTITY.get().create(level);
         if (entity instanceof SkeletonTurret turret) {
            turret.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, context.getRotation(), 0.0F);
            
            int savedId = tag.getInt("SavedUnitId");
            int savedLevel = tag.getInt("SavedRangeLevel");
            
            // 使用新方法设置 (需要去 SkeletonTurret 添加)
            // 这里暂时保留旧逻辑的意图，实际通过 restoreFromRecord 统一处理更好
            // 但为了兼容，我们构造一个伪造的 dataTag
            CompoundTag fakeData = new CompoundTag();
            fakeData.putInt("UnitID", savedId);
            fakeData.putInt("RangeLevel", savedLevel);
            fakeData.putInt("Tier", Math.max(0, savedLevel - 1)); // Map legacy RangeLevel to Tier
            // 其他默认值
            fakeData.putInt("Level", 1);
            
            turret.restoreFromRecord(fakeData);
            turret.setHealth(turret.getMaxHealth());
            turret.getPersistentData().putBoolean("RecordSummoned", true);
            
            level.addFreshEntity(turret);
            context.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
         }
         return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        CompoundTag masterTag = stack.getTag();
        if (masterTag != null && masterTag.contains("Version")) {
            CompoundTag data = masterTag.getCompound("Data");
            tooltip.add(Component.literal("§7ID: §e" + data.getInt("UnitID")));
            tooltip.add(Component.literal("§7等级: §bLv." + data.getInt("Level") + " (XP: " + data.getInt("XP") + ")"));
            tooltip.add(Component.literal("§7射程: §aLv." + data.getInt("RangeLevel")));
            
            int invCount = 0;
            if (data.contains("Inventory")) invCount = data.getList("Inventory", Tag.TAG_COMPOUND).size();
            tooltip.add(Component.literal("§7物品: §f" + invCount + "项"));
            
            if (masterTag.contains("Backup")) {
                tooltip.add(Component.literal("§8[已启用冗余备份]"));
            }
        } else if (masterTag != null && masterTag.contains("SavedUnitId")) {
            // 旧版显示
            int id = masterTag.getInt("SavedUnitId");
            int range = masterTag.getInt("SavedRangeLevel");
            tooltip.add(Component.literal("§7已记录机体: §e" + id));
            tooltip.add(Component.literal("§7射程等级: §bLv." + range));
            tooltip.add(Component.literal("§c[旧版数据]"));
        } else {
            tooltip.add(Component.literal("§8空记录卡"));
        }
    }
}
