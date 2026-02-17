package com.example.examplemod;


// ✅ 补全这些导包，防止 HUD 报错

import net.minecraft.client.gui.GuiGraphics; // 关键：画图工具
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.client.Minecraft;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger; // ✅ 日志工具导入
import com.mojang.logging.LogUtils; // ✅ 日志工具导入

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;


import net.minecraftforge.fml.config.ModConfig;

@Mod("examplemod")
public class ExampleMod {
    // Reduce Ender Pearl drop probability by 60% (keep 40% of configured value).
    private static final double ENDER_PEARL_DROP_RATE_SCALE = 0.8D;
    // ✅ 1. 定义日志记录器
    private static final Logger LOGGER = LogUtils.getLogger();
    static final int TURRET_TP_PERMISSION_LEVEL = 2;
    private static final long CAPTAIN_EVAL_INTERVAL_TICKS = 20L * 60L;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "examplemod");
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "examplemod");
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "examplemod");
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "examplemod");
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "examplemod");
    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "examplemod");

    public static final RegistryObject<Item> TURRET_WAND = ITEMS.register("turret_wand", () -> new TurretItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GLITCH_CHIP = ITEMS.register("glitch_chip", () -> new GlitchChipItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> TELEPORT_UPGRADE_MODULE = ITEMS.register("teleport_upgrade_module", () -> new TeleportUpgradeItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> MULTI_SHOT_UPGRADE_MODULE = ITEMS.register("multi_shot_upgrade_module", () -> new MultiShotUpgradeModuleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DEATH_RECORD_ITEM = ITEMS.register("death_record_card", () -> new DeathRecordItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PLAYER_MANUAL = ITEMS.register("player_manual", () -> new PlayerManualItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<net.minecraft.world.level.block.Block> SUMMON_TERMINAL_BLOCK = BLOCKS.register("summon_terminal",
            () -> new SummonTerminalBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK).lightLevel(state -> state.getValue(SummonTerminalBlock.LIT) ? 8 : 0)));
    public static final RegistryObject<Item> SUMMON_TERMINAL_ITEM = ITEMS.register("summon_terminal",
            () -> new net.minecraft.world.item.BlockItem(SUMMON_TERMINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> ADD_ENDER_PEARL = LOOT_MODIFIERS.register("add_ender_pearl", EnderPearlLootModifier.CODEC);

    public static final RegistryObject<net.minecraft.world.inventory.MenuType<TurretMenu>> TURRET_MENU = MENUS.register("turret_menu",
            () -> net.minecraftforge.common.extensions.IForgeMenuType.create(TurretMenu::new));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<SummonTerminalMenu>> SUMMON_TERMINAL_MENU = MENUS.register("summon_terminal_menu",
            () -> net.minecraftforge.common.extensions.IForgeMenuType.create(SummonTerminalMenu::new));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<SummonTerminalBlockEntity>> SUMMON_TERMINAL_BE = BLOCK_ENTITY_TYPES.register(
            "summon_terminal",
            () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(SummonTerminalBlockEntity::new, SUMMON_TERMINAL_BLOCK.get()).build(null)
    );

    public static final RegistryObject<EntityType<SkeletonTurret>> TURRET_ENTITY = ENTITIES.register("skeleton_turret",
            () -> EntityType.Builder.of(SkeletonTurret::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.99f)
                    .clientTrackingRange(8)
                    .build("skeleton_turret"));
    private static final String[] RESCUE_QUOTES = {
            "§f坚持住，指挥官！我来了！", "§f检测到求救信号，全速赶往！", "§f别怕，医疗兵马上就位！",
            "§f谁敢动我的主人！撑住！", "§f正在根据定位全速支援！", "§f把手给我！我拉你起来！",
            "§f你的护盾已抵达战场！", "§f撑住，别闭上眼睛！", "§f清除路障，救援行动开始！",
            "§f稍微忍耐一下，马上就好！", "§f我在，我在！不要放弃希望！", "§f正在执行最高优先级救援指令！",
            "§f只要我还在，你就不会死！", "§f不用担心，我会带你回家！", "§f看来你需要一点帮助，长官！"
    };

    public static final GameProfile TURRET_FAKE_PLAYER_PROFILE = new GameProfile(UUID.fromString("c06f8906-4c8a-4d11-9c3c-09d6c352723c"), "[Turret]");

    public ExampleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Config
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TurretConfig.COMMON_SPEC);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::addEntityAttributes);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientModEvents::registerRenderers);
            modEventBus.addListener(ClientModEvents::registerLayerDefinitions);
            modEventBus.addListener(ClientModEvents::clientSetup);
            modEventBus.addListener(ClientModEvents::registerItemColors);
        }
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(BossBarManager.class); // Register BossBarManager
        LOGGER.info("✅ 炮台模组已加载 - 监控系统启动"); // 启动日志
        PacketHandler.register();
        ModSounds.register(modEventBus);
        // GeckoLib removed


    }

    // 修复后的BossBarManager类
    public static class BossBarManager {
        private static final Map<UUID, BossBarInfo> activeBars = new ConcurrentHashMap<>();

        private static class BossBarInfo {
            final ServerBossEvent bar;
            int remainingTicks;
            final int initialDuration;

            BossBarInfo(ServerBossEvent bar, int durationTicks) {
                this.bar = bar;
                this.remainingTicks = durationTicks;
                this.initialDuration = durationTicks;
            }
        }

        public static void showTemporaryBossBar(ServerPlayer player, Component message, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style, int durationTicks) {
            if (activeBars.containsKey(player.getUUID())) {
                BossBarInfo oldInfo = activeBars.remove(player.getUUID());
                oldInfo.bar.removePlayer(player);
            }

            ServerBossEvent bossBar = new ServerBossEvent(message, color, style);
            bossBar.setProgress(1.0f);
            bossBar.addPlayer(player);
            activeBars.put(player.getUUID(), new BossBarInfo(bossBar, durationTicks));
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Iterator<Map.Entry<UUID, BossBarInfo>> iterator = activeBars.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, BossBarInfo> entry = iterator.next();
                BossBarInfo info = entry.getValue();
                info.remainingTicks--;

                float progress = (float) info.remainingTicks / info.initialDuration;
                info.bar.setProgress(Math.max(0, progress));

                if (info.remainingTicks <= 0) {
                    ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                    if (player != null) {
                        info.bar.removePlayer(player);
                    }
                    iterator.remove();
                }
            }
        }
    }


    // 如果是1.20+可能需要 .stacksTo(64)






    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TURRET_WAND);
            event.accept(GLITCH_CHIP);
            event.accept(TELEPORT_UPGRADE_MODULE);
            event.accept(MULTI_SHOT_UPGRADE_MODULE);
            event.accept(DEATH_RECORD_ITEM);
            event.accept(PLAYER_MANUAL);
            event.accept(SUMMON_TERMINAL_ITEM);

            for (int level = 1; level <= TurretUpgradeTierPlan.maxLevel(); level++) {
                ItemStack teleportStack = new ItemStack(TELEPORT_UPGRADE_MODULE.get());
                TeleportUpgradeItem.setLevel(teleportStack, level);
                event.accept(teleportStack);

                ItemStack multiShotStack = new ItemStack(MULTI_SHOT_UPGRADE_MODULE.get());
                MultiShotUpgradeModuleItem.setLevel(multiShotStack, level);
                event.accept(multiShotStack);
            }
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SUMMON_TERMINAL_ITEM);
        }
    }

    private void addEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(TURRET_ENTITY.get(), SkeletonTurret.createAttributes().build());
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;

        // 1. SkeletonTurret Death Record Drop (100%, deterministic, exactly one)
        if (event.getEntity() instanceof SkeletonTurret turret) {
            DamageSource source = event.getSource();
            LOGGER.info("[DropSystem] Processing drops for SkeletonTurret #{}. Source: {}, Y-Pos: {}",
                turret.getEntityData().get(SkeletonTurret.UNIT_ID),
                source.getMsgId(),
                turret.getY());

            // Check if already dropped (Idempotency)
            if (turret.hasDroppedRecord()) {
                LOGGER.info("[DropSystem] ⚠ Death Record already dropped for Turret #{}, skipping.", turret.getEntityData().get(SkeletonTurret.UNIT_ID));
                return;
            }

            // Force exactly one plaque drop at the death position.
            event.getDrops().clear();
            ItemStack record = turret.createDeathRecordCard(1);
            if (record.isEmpty()) {
                LOGGER.error("[DropSystem] ❌ Failed to create record card.");
                return;
            }
            record.setCount(1);
            event.getDrops().add(new ItemEntity(
                    turret.level(),
                    turret.getX(), turret.getY(), turret.getZ(),
                    record
            ));
            turret.setDroppedRecord(true);
            LOGGER.info("[DropSystem] ✅ Forced Death Record drop at ({}, {}, {}), source={}",
                    turret.getX(), turret.getY(), turret.getZ(), source.getMsgId());
            // Turrets don't drop pearls
            return;
        }

        // Ender Pearl drop chance for hostile mobs (configurable), globally scaled down by 60%.
        if (event.getEntity() instanceof Monster) {
            // Get values from config
            double baseChance = TurretConfig.COMMON.enderPearlDropChanceBase.get();
            double bonusChance = TurretConfig.COMMON.enderPearlDropChanceBonus.get();

            // Random chance between base and base + bonus
            double rawChance = baseChance + (event.getEntity().getRandom().nextDouble() * bonusChance);
            double chance = Math.max(0.0D, Math.min(1.0D, rawChance * ENDER_PEARL_DROP_RATE_SCALE));

            if (event.getEntity().getRandom().nextDouble() < chance) {
                event.getDrops().add(new ItemEntity(
                    event.getEntity().level(),
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    new ItemStack(Items.ENDER_PEARL)
                ));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();

        LOGGER.debug("玩家登录: {}", player.getName().getString()); // 调试日志

        if (!data.contains("HasReceivedStarterKit_Final")) {
            LOGGER.info("🎁 发放新手礼包给: {}", player.getName().getString());
            player.getInventory().add(new ItemStack(TURRET_WAND.get(), 3));
            player.sendSystemMessage(Component.literal("§6[系统] §f欢迎指挥官！已发放 §b3x 毁灭守望者法杖 §f作为新地图初始资金。"));
            player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
            data.putBoolean("HasReceivedStarterKit_Final", true);
        }

        ensurePlayerManual(player);
    }

    private void ensurePlayerManual(Player player) {
        boolean hasManual = false;
        boolean updated = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(PLAYER_MANUAL.get())) {
                continue;
            }
            hasManual = true;
            if (PlayerManualItem.ensureVersion(stack)) {
                updated = true;
            }
        }

        if (!hasManual) {
            ItemStack manual = new ItemStack(PLAYER_MANUAL.get());
            PlayerManualItem.ensureVersion(manual);
            player.getInventory().add(manual);
            player.sendSystemMessage(Component.translatable("message.examplemod.manual_given"));
            return;
        }

        if (updated) {
            player.sendSystemMessage(Component.translatable("message.examplemod.manual_updated", PlayerManualItem.CURRENT_VERSION));
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Monster monster && !(event.getEntity() instanceof SkeletonTurret)) {
            if (monster.getPersistentData().getBoolean("IsFriendlyZombie")) return;
            if (monster.getPersistentData().getBoolean("IsFriendlyCreeper")) return;
            if (monster.hasCustomName() && monster.getCustomName().getString().contains("感染体")) return;
            monster.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(monster, SkeletonTurret.class, true));
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().getPersistentData().getBoolean("IsTurretArrow")) return;

        Entity projectile = event.getProjectile();
        Level level = projectile.level();

        if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
            projectile.discard();
            event.setCanceled(true);
            return;
        }

        if (event.getRayTraceResult().getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) event.getRayTraceResult();
            Entity target = hit.getEntity();
            AbstractArrow arrow = (AbstractArrow) projectile;

            if (target == arrow.getOwner() || target instanceof SkeletonTurret || target instanceof Player) {
                projectile.discard();
                event.setCanceled(true);
                return;
            }
            if (target.getPersistentData().getBoolean("IsFriendlyZombie") || target.getPersistentData().getBoolean("IsFriendlyCreeper")) {
                projectile.discard();
                event.setCanceled(true);
                return;
            }

            Entity shooter = arrow.getOwner();
            if (shooter instanceof SkeletonTurret turret) {
                turret.registerHit();
                if (target instanceof LivingEntity livingTarget) {
                    livingTarget.getPersistentData().putUUID("TurretAssistUUID", turret.getUUID());
                }
            }

            target.invulnerableTime = 0;
            int tier = event.getProjectile().getPersistentData().getInt("TurretTier");
            boolean isBrutal = event.getProjectile().getPersistentData().getBoolean("IsBrutalArrow");

            if (target instanceof LivingEntity livingTarget && level instanceof ServerLevel serverLevel) {
                if (isBrutal) livingTarget.invulnerableTime = 0;

                FakePlayer fakePlayer = FakePlayerFactory.get(serverLevel, TURRET_FAKE_PLAYER_PROFILE);
                fakePlayer.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                LivingEntity attributedShooter = shooter instanceof LivingEntity livingShooter ? livingShooter : fakePlayer;

                float damageAmount = (float) (arrow.getBaseDamage() * arrow.getDeltaMovement().length());
                if (damageAmount < 1.0f) damageAmount = (float) arrow.getBaseDamage();

                // Attribute primary projectile damage to the turret owner entity (not fake player),
                // so kill-score/upgrade hooks run on SkeletonTurret correctly.
                boolean dealt = livingTarget.hurt(serverLevel.damageSources().arrow(arrow, attributedShooter), damageAmount);
                if (!dealt && shooter instanceof SkeletonTurret turretShooter) {
                    LivingEntity locked = turretShooter.getTarget();
                    if (locked != null && locked.getUUID().equals(livingTarget.getUUID())) {
                        // 保底伤害：当骷髅已锁定目标且箭矢伤害被拦截时，改用近战/魔法源兜底
                        livingTarget.invulnerableTime = 0;
                        boolean fallback = livingTarget.hurt(serverLevel.damageSources().mobAttack(turretShooter), Math.max(1.0f, damageAmount * 0.6f));
                        if (!fallback) {
                            livingTarget.hurt(serverLevel.damageSources().magic(), 1.0f);
                        }
                    }
                }

                if (tier >= 4 && shooter instanceof LivingEntity turret) {
                    float healRate = (tier == 5) ? 0.1f : 0.0f;
                    if (healRate > 0) {
                        float heal = 5.0f * healRate;
                        turret.heal(heal);
                        level.getEntitiesOfClass(Player.class, turret.getBoundingBox().inflate(30.0)).forEach(p -> p.heal(heal));
                    }
                }

                if (tier >= 1) {
                    livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
                }

                if (tier >= 3 && level.random.nextFloat() < 0.1f) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, projectile.getX(), projectile.getY(), projectile.getZ(), 1, 0, 0, 0, 0);
                    serverLevel.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
                    level.getEntitiesOfClass(LivingEntity.class, projectile.getBoundingBox().inflate(3.5)).forEach(v -> {
                        boolean isFriendly = v instanceof SkeletonTurret || v instanceof Player ||
                                v.getPersistentData().getBoolean("IsFriendlyZombie") ||
                                v.getPersistentData().getBoolean("IsFriendlyCreeper");
                        if ((v instanceof Enemy || v instanceof IronGolem) && !isFriendly) {
                            v.hurt(serverLevel.damageSources().explosion(null, attributedShooter), 2.5f);
                            if (level.random.nextFloat() < 0.3f) v.setSecondsOnFire(3);
                        }
                    });
                }

                if (tier >= 4 && level.random.nextFloat() < 0.2f) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.moveTo(target.position());
                        bolt.setVisualOnly(true);
                        serverLevel.addFreshEntity(bolt);
                    }
                    if (shooter instanceof SkeletonTurret turretShooter) {
                        // Keep bonus lightning visuals, but attribute damage to turret for XP/upgrade chain.
                        livingTarget.hurt(serverLevel.damageSources().mobAttack(turretShooter), 7.5f);
                    } else {
                        livingTarget.hurt(serverLevel.damageSources().lightningBolt(), 7.5f);
                    }
                }
            }
            projectile.discard();
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        String msg = event.getMessage().getString();
        ServerPlayer player = event.getPlayer();
        ServerLevel level = player.serverLevel();




        // ==================== ⚔ 开启清剿模式 ====================
        if (msg.equals("开始清剿")) {
            // 1. 召集队伍
            List<SkeletonTurret> squad = level.getEntitiesOfClass(SkeletonTurret.class,
                    player.getBoundingBox().inflate(100.0), // 召集 100 格内的所有手下
                    t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID()) && t.isAlive()
                            && (t.isCaptain() || t.isSquadMember()) // 👈 只有队长或正式队员能去
            );

            if (squad.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c[系统] 附近没有可用的作战单位。"));
                return;
            }

            int count = squad.size();
            player.sendSystemMessage(Component.literal("§6[指挥] 正在部署战术... 共有 " + count + " 名敢死队员参与行动。"));

            // 2. 战术分配 (扇形搜索)
            // 原理：把 360 度平分给每个人。
            // 比如 2个人：0度, 180度
            // 比如 4个人：0度, 90度, 180度, 270度
            float angleStep = 360.0f / count;

            for (int i = 0; i < count; i++) {
                SkeletonTurret t = squad.get(i);
                float assignedAngle = i * angleStep; // 分配角度

                // 下达死命令
                t.startPurgeMode(assignedAngle);
            }
        }
// ==================== 🛑 停止清剿 ====================
        if (msg.equals("停止清剿")) {
            List<SkeletonTurret> squad = level.getEntitiesOfClass(SkeletonTurret.class,
                    player.getBoundingBox().inflate(200.0),
                    t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID())
            );

            int count = 0;
            for (SkeletonTurret t : squad) {
                if (t.isPurgeActive()) {
                    t.stopPurgeMode(); // 立即停止
                    t.setTarget(null); // 忘记敌人
                    t.getNavigation().stop(); // 停下脚步
                    count++;
                }
            }
            player.sendSystemMessage(Component.literal("§a[系统] 清剿行动已终止，" + count + " 名队员待命。"));
        }
        // ==================== ⚡ 绝对召回 (优先级最高) ====================
        if (msg.equals("来") || msg.equals("过来") ||msg.equals("lai")||msg.equals("LAI")|| msg.equalsIgnoreCase("come")) {
            LOGGER.info("指令: 玩家 {} 请求绝对召回", player.getName().getString());

            List<SkeletonTurret> allTurrets = level.getEntitiesOfClass(SkeletonTurret.class,
                    player.getBoundingBox().inflate(600.0), // 范围足够大
                    t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID())
            );

            int count = 0;
            for (SkeletonTurret t : allTurrets) {
                // ✅ 筛选：只对队员生效 (队长+队员)
                if (t.isCaptain() || t.isSquadMember()) {

                    // 1. 强制停止所有特殊模式
                    if (t.isPurgeActive()) t.stopPurgeMode();
                    if (t.isCommandScavenging()) t.setCommandScavenging(false);
                    if (t.isCommandRescue()) t.setCommandRescue(false); // 假设你有这个getter/setter

                    // 2. 强制停止战斗和移动
                    t.setTarget(null);
                    t.getNavigation().stop();

                    // 3. 强制开启跟随
                    if (!t.isFollowing()) t.setFollowing(true);

                    // 4. 执行传送
                    t.teleportToSafeSpot(player);

                    // 5. 特效
                    level.sendParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + 1.0, t.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                    count++;
                }
            }

            if (count > 0) {
                player.sendSystemMessage(Component.literal("§a[系统] ⚡ 强制召回令已执行！§e" + count + "§a 名队员已重置状态并归队。"));
            } else {
                player.sendSystemMessage(Component.literal("§c[系统] 未检测到编队成员。"));
            }
        }
        if (msg.equals("捡东西") || msg.equals("拾取物品")) {
            List<SkeletonTurret> turrets = level.getEntitiesOfClass(SkeletonTurret.class, player.getBoundingBox().inflate(600.0),
                    t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID()) && t.isFollowing()
            );
            int memberCount = 0;
            for (SkeletonTurret t : turrets) {
                if (!t.isCaptain()) {
                    t.setCommandScavenging(true);
                    memberCount++;
                }
            }
            if (memberCount > 0) {
                player.sendSystemMessage(Component.literal("§e[战术] 已命令 " + memberCount + " 名队员执行广域搜索任务！队长正在警戒。"));
            }
        }

        if (msg.contains("救")) {
            List<SkeletonTurret> turrets = level.getEntitiesOfClass(SkeletonTurret.class, player.getBoundingBox().inflate(100.0),
                    t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID())
            );
            if (!turrets.isEmpty()) {
                SkeletonTurret nearestTurret = null;
                double minDistanceSq = Double.MAX_VALUE;
                for (SkeletonTurret t : turrets) {
                    double dist = t.distanceToSqr(player);
                    if (dist < minDistanceSq) {
                        minDistanceSq = dist;
                        nearestTurret = t;
                    }
                }
                if (nearestTurret != null) {
                    nearestTurret.setCommandRescue(true);
                    nearestTurret.setCommandScavenging(false);
                    String quote = RESCUE_QUOTES[level.random.nextInt(RESCUE_QUOTES.length)];
                    player.sendSystemMessage(Component.literal("§e<" + nearestTurret.getDisplayName().getString() + "> §f" + quote));
                    level.sendParticles(ParticleTypes.HEART, nearestTurret.getX(), nearestTurret.getEyeY() + 0.5, nearestTurret.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
    }


    // ==========================================
    // ✅ 注册专属传送命令 (无需作弊权限)
    // ==========================================
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TurretCommands.register(event.getDispatcher());
        // 注册命令：/turrettp <目标UUID>
        event.getDispatcher().register(
                Commands.literal("turrettp") // 命令名
                        .requires(source -> source.hasPermission(TURRET_TP_PERMISSION_LEVEL))
                        .then(Commands.argument("targetId", StringArgumentType.string()) // 参数：UUID字符串
                                .executes(context -> {
                                    try {
                                        // 1. 获取参数
                                        String uuidStr = StringArgumentType.getString(context, "targetId");
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ServerLevel level = player.serverLevel();

                                        // 2. 寻找目标
                                        UUID uid = UUID.fromString(uuidStr);
                                        Entity target = level.getEntity(uid);

                                        if (target != null) {
                                            // 3. 执行传送
                                            player.teleportTo(target.getX(), target.getY(), target.getZ());

                                            // 4. 播放特效
                                            player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                                            player.sendSystemMessage(Component.literal("§a[系统] ⚡ 空间折跃成功！已抵达目标附近。"));
                                        } else {
                                            player.sendSystemMessage(Component.literal("§c[系统] 传送失败：目标信号丢失 (可能位于未加载区块)。"));
                                        }
                                    } catch (Exception e) {
                                        LOGGER.error("[TurretTP] Command failed: {}", e.getMessage());
                                    }
                                    return 1; // 命令执行成功
                                })
                        )
        );
    }



    // ==========================================
    // ✅ 核心修复：灵魂绑定 (死后继承数据)
    // ==========================================




    // ==========================================
    // ✅ 实时更新：小队管理与位置记录
    // ==========================================
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        CompoundTag data = player.getPersistentData();

        if (player.tickCount % 20 == 0) {
            data.putDouble("LastKnownX", player.getX());
            data.putDouble("LastKnownY", player.getY());
            data.putDouble("LastKnownZ", player.getZ());
        }
        if (player.level() instanceof ServerLevel sl) {
            long gameTime = sl.getGameTime();
            if (isCaptainEvaluationTick(gameTime)) {
                manageTurretSquad(player, sl, gameTime);
            }
        }

    }

    static boolean isCaptainEvaluationTick(long gameTime) {
        return gameTime > 0 && gameTime % CAPTAIN_EVAL_INTERVAL_TICKS == 0;
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity().getPersistentData().contains("LimitedLife")) {
            int life = event.getEntity().getPersistentData().getInt("LimitedLife");
            life--;
            if (life <= 0) event.getEntity().discard();
            else event.getEntity().getPersistentData().putInt("LimitedLife", life);
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getExploder() instanceof Creeper creeper) {
            if (creeper.getPersistentData().getBoolean("IsFriendlyCreeper")) {
                event.getAffectedBlocks().clear();
                event.getAffectedEntities().removeIf(e -> e instanceof SkeletonTurret || e instanceof Player || e instanceof IronGolem || e.getPersistentData().getBoolean("IsFriendlyZombie"));
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (attacker != null) {
            boolean isAttackerFriendly = attacker instanceof SkeletonTurret || attacker instanceof Player || attacker instanceof IronGolem || attacker.getPersistentData().getBoolean("IsFriendlyZombie") || attacker.getPersistentData().getBoolean("IsFriendlyCreeper");
            boolean isVictimFriendly = victim instanceof SkeletonTurret || victim instanceof Player || victim instanceof IronGolem || victim.getPersistentData().getBoolean("IsFriendlyZombie") || victim.getPersistentData().getBoolean("IsFriendlyCreeper");
            if (isAttackerFriendly && isVictimFriendly) event.setCanceled(true);
        }
    }


    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (attacker == null || target == null) return;
        boolean isAttackerFriendly = attacker instanceof SkeletonTurret || attacker instanceof IronGolem || attacker instanceof Player || attacker.getPersistentData().getBoolean("IsFriendlyZombie") || attacker.getPersistentData().getBoolean("IsFriendlyCreeper");
        boolean isTargetFriendly = target instanceof SkeletonTurret || target instanceof IronGolem || target instanceof Player || target.getPersistentData().getBoolean("IsFriendlyZombie") || target.getPersistentData().getBoolean("IsFriendlyCreeper");
        if (isAttackerFriendly && isTargetFriendly) event.setNewTarget(null);
    }




    // ==========================================
    // ✅ 战术中心：小队管理逻辑 (已升级选拔算法)
    // ==========================================
    private void manageTurretSquad(Player player, ServerLevel level, long evalTick) {
        // 1. 获取所有跟随我的、活着的炮台
        List<SkeletonTurret> allFollowers = level.getEntitiesOfClass(SkeletonTurret.class,
                player.getBoundingBox().inflate(200.0),
                t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID()) && t.isFollowing() && t.isAlive()
        );

        if (allFollowers.isEmpty()) {
            LOGGER.info("[CaptainEval] tick={} owner={} oldCaptain=none newCaptain=none reason=NO_FOLLOWERS scanned=0",
                    evalTick, player.getUUID());
            return;
        }

        SkeletonTurret oldCaptain = null;
        java.util.Map<String, SkeletonTurret> byId = new java.util.HashMap<>();
        java.util.List<SquadCaptainSelection.Candidate> candidates = new java.util.ArrayList<>();
        for (SkeletonTurret t : allFollowers) {
            String id = t.getStringUUID();
            byId.put(id, t);
            candidates.add(new SquadCaptainSelection.Candidate(
                    id,
                    t.getSquadScore(),
                    t.getTier(),
                    t.getKillCount(),
                    t.tickCount
            ));
            if (t.isCaptain() && oldCaptain == null) {
                oldCaptain = t;
            }
        }

        SquadCaptainSelection.Decision decision = SquadCaptainSelection.evaluate(
                candidates,
                oldCaptain == null ? null : oldCaptain.getStringUUID()
        );

        SkeletonTurret newCaptain = byId.get(decision.newCaptainId());
        if (newCaptain == null && !allFollowers.isEmpty()) {
            newCaptain = allFollowers.get(0);
        }

        java.util.List<SkeletonTurret> ordered = new java.util.ArrayList<>();
        for (String id : decision.rankedIds()) {
            SkeletonTurret t = byId.get(id);
            if (t != null) {
                ordered.add(t);
            }
        }
        if (newCaptain != null) {
            ordered.remove(newCaptain);
            ordered.add(0, newCaptain);
        }

        java.util.Set<SkeletonTurret> squadSlots = new java.util.HashSet<>();
        int maxSquad = Math.min(8, ordered.size());
        for (int i = 0; i < maxSquad; i++) {
            squadSlots.add(ordered.get(i));
        }

        for (SkeletonTurret t : allFollowers) {
            if (t == newCaptain) {
                if (!t.isCaptain()) t.setCaptain(true);
                if (t.isSquadMember()) t.setSquadMember(false);
            } else if (squadSlots.contains(t)) {
                if (t.isCaptain()) t.setCaptain(false);
                if (!t.isSquadMember()) t.setSquadMember(true);
            } else {
                if (t.isCaptain()) t.setCaptain(false);
                if (t.isSquadMember()) t.setSquadMember(false);
            }
        }

        if (oldCaptain != newCaptain && newCaptain != null) {
            newCaptain.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.sendSystemMessage(Component.literal("§6[战术] 队长已变更！新任队长: §e" + newCaptain.getDisplayName().getString()));
        }

        String oldCaptainId = oldCaptain == null ? "none" : oldCaptain.getStringUUID();
        String newCaptainId = newCaptain == null ? "none" : newCaptain.getStringUUID();
        String oldCaptainUnit = oldCaptain == null ? "none" : String.format("%03d", Math.floorMod(oldCaptain.getEntityData().get(SkeletonTurret.UNIT_ID), 1000));
        String newCaptainUnit = newCaptain == null ? "none" : String.format("%03d", Math.floorMod(newCaptain.getEntityData().get(SkeletonTurret.UNIT_ID), 1000));
        LOGGER.info("[CaptainEval] tick={} owner={} oldCaptain={} oldUnit=#{} newCaptain={} newUnit=#{} reason={} scanned={}",
                evalTick, player.getUUID(), oldCaptainId, oldCaptainUnit, newCaptainId, newCaptainUnit, decision.reason(), allFollowers.size());
    }
    private void spawnEliteZombie(Level level, net.minecraft.world.phys.Vec3 pos, int tier, int lifeTicks) {
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) return;
        zombie.moveTo(pos);
        zombie.getPersistentData().putInt("LimitedLife", lifeTicks);
        zombie.getPersistentData().putBoolean("IsFriendlyZombie", true);
        zombie.setCustomName(Component.literal("§2☣ 亡灵援军"));
        zombie.setCustomNameVisible(true);
        giveZombieEquipment(zombie, tier);

        AttributeInstance dmgAttr = zombie.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.addPermanentModifier(new AttributeModifier("TurretBuffDmg", 1.5 + (tier * 0.3) - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));

        zombie.targetSelector.removeAllGoals(g -> true);
        zombie.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(zombie, LivingEntity.class, 10, true, false, (e) -> {
            if (!(e instanceof Monster)) return false;
            if (e instanceof SkeletonTurret || e instanceof Player || e instanceof IronGolem) return false;
            if (e.getPersistentData().getBoolean("IsFriendlyZombie") || e.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;
            return true;
        }));
        level.addFreshEntity(zombie);
    }

    private void spawnFriendlyCreeper(Level level, net.minecraft.world.phys.Vec3 pos, int lifeTicks) {
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper == null) return;
        creeper.moveTo(pos);
        creeper.getPersistentData().putInt("LimitedLife", lifeTicks);
        creeper.getPersistentData().putBoolean("IsFriendlyCreeper", true);
        creeper.setCustomName(Component.literal("§a⚠ 战术核弹"));
        creeper.setCustomNameVisible(true);
        creeper.targetSelector.removeAllGoals(g -> true);
        creeper.goalSelector.addGoal(1, new SwellGoal(creeper));
        creeper.goalSelector.addGoal(2, new MeleeAttackGoal(creeper, 1.5, false));
        creeper.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(creeper, LivingEntity.class, 10, true, false, (e) -> {
            if (!(e instanceof Monster)) return false;
            if (e instanceof SkeletonTurret || e instanceof Player || e instanceof IronGolem) return false;
            if (e.getPersistentData().getBoolean("IsFriendlyZombie") || e.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;
            return true;
        }));
        level.addFreshEntity(creeper);
    }

    private void giveZombieEquipment(Zombie zombie, int tier) {
        Item weapon = (tier >= 4) ? Items.NETHERITE_SWORD : (tier >= 2 ? Items.DIAMOND_SWORD : Items.IRON_SWORD);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(weapon));
        if (tier >= 2) zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
    }
    // ==========================================

// ==========================================
    // ✅ 战术面板 HUD (赛博战术终端 2.0 - 高端排版)
    // ==========================================
    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 1. 获取并筛选小队
        List<SkeletonTurret> mySquad = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof SkeletonTurret t) {
                if (t.getOwnerUUID() != null && t.getOwnerUUID().equals(mc.player.getUUID()) && t.isFollowing()) {
                    mySquad.add(t);
                }
            }
        }
        if (mySquad.isEmpty()) return;

        // 2. 排序 (队长 > 等级 > 杀敌)
        mySquad.sort((a, b) -> {
            if (a.isCaptain()) return -1;
            if (b.isCaptain()) return 1;
            return Integer.compare(b.getTier(), a.getTier());
        });

        GuiGraphics gfx = event.getGuiGraphics();

        // ==================== 📐 布局计算 ====================
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int memberCount = Math.min(mySquad.size(), 8);

        // 尺寸参数
        int rowHeight = 18;   // 行高 (加大，为了“下方空一行”的视觉效果)
        int headerHeight = 16; // 标题栏高度
        int hudWidth = 180;   // 总宽度 (加宽以容纳更多信息)
        int totalHeight = headerHeight + (memberCount * rowHeight) + 4;

        // 位置 (左侧垂直居中)
        int startX = 6;
        int startY = (screenHeight / 2) - (totalHeight / 2);

        // ==================== 🖌️ 绘制容器 ====================

        // 1. 全局背景 (极淡的黑影)
        // gfx.fill(startX, startY, startX + hudWidth, startY + totalHeight, 0x40000000);

        // 2. 标题栏 (深色科技风)
        gfx.fill(startX, startY, startX + hudWidth, startY + headerHeight, 0xE6101010); // 90% 黑
        // 3. 标题栏底部分割线 (青色霓虹光条)
        gfx.fill(startX, startY + headerHeight - 1, startX + hudWidth, startY + headerHeight, 0xFF00E5FF); // 亮青色

        // 4. 绘制标题文字 (粗体 + 阴影)
        // 使用 "TACTICAL SQUAD" 显得更洋气，或者 "戰術終端"
        String title = "§3§l⚡ 战 术 面 板 §7(" + memberCount + "/8)";
        gfx.drawString(mc.font, title, startX + 6, startY + 4, 0xFFFFFF, true);

        // ==================== 📝 绘制成员列表 ====================

        int currentY = startY + headerHeight + 4; // 内容起始 Y 坐标

        for (int i = 0; i < memberCount; i++) {
            SkeletonTurret t = mySquad.get(i);

            // --- [A. 数据准备] ---
            boolean isDowned = false; // 已移除倒地状态
            int hp = (int)Math.ceil(t.getHealth());
            int maxHp = (int)t.getMaxHealth();
            int killPercent = t.getKillProgressPercent();

            // --- [B. 绘制单行背景条] ---
            // 奇偶行变色 (斑马纹)，增加可读性
            int rowColor = (i % 2 == 0) ? 0x80202020 : 0x80101010; // 半透明深灰
            if (t.isCaptain()) rowColor = 0x80302010; // 队长是微微的金底
            if (isDowned) rowColor = 0x80301010;      // 濒死是微微的红底

            gfx.fill(startX, currentY - 2, startX + hudWidth, currentY + 8, rowColor);

            // --- [C. 核心排版] ---
            // 格式: [ID] [名称]         [进度] [血量]

            // 1. 代号 ID (灰色斜体)
            // 既然要 "556" 这种纯数字感，我们把 # 去掉，或者保留 # 但变灰
            String idStr = "§7§o#" + t.getEntityData().get(SkeletonTurret.UNIT_ID); // §o 是斜体
            gfx.drawString(mc.font, idStr, startX + 4, currentY, 0xFFFFFF, true);

            // 2. 名字 & 军衔
            String rankIcon = t.isCaptain() ? "§6👑" : (isDowned ? "§c⚠" : "§8▪");
            String nameColor = isDowned ? "§c" : (t.isCaptain() ? "§6" : "§f"); // 队长名字加粗
            // 获取纯净名字
            String rawName = t.getDisplayName().getString().replaceAll("§.", "").replace("[队伍]", "").replace("👑", "").trim();
            // 截断过长的名字
            // ✅ 新增：这里砍一刀！如果名字里有 "#"，就把后面的编号全删掉
            if (rawName.contains("#")) {
                rawName = rawName.substring(0, rawName.indexOf("#")).trim();
            }
            if (rawName.length() > 6) rawName = rawName.substring(0, 6);

            String nameStr = rankIcon + " " + nameColor + rawName;
            gfx.drawString(mc.font, nameStr, startX + 32, currentY, 0xFFFFFF, true);

            // 3. 升级进度 (靠右显示)
            String progColor;
            if (killPercent < 20) progColor = "§a";
            else if (killPercent < 50) progColor = "§e";
            else if (killPercent < 80) progColor = "§6";
            else progColor = "§c§l"; // 满级红色加粗

            String progStr = progColor + "⚡" + killPercent + "%";
            int progWidth = mc.font.width(progStr);
            // 放在总宽度 - 55 的位置
            gfx.drawString(mc.font, progStr, startX + hudWidth - 55 - progWidth, currentY, 0xFFFFFF, true);

            // 4. 血量 (最右侧)
            // 格式: ❤ 55
            String hpColor = (hp < maxHp * 0.3) ? "§c§l" : "§f"; // 低血量变红加粗
            String hpStr = "§c❤ " + hpColor + hp;
            // 右对齐计算
            int hpWidth = mc.font.width(hpStr);
            gfx.drawString(mc.font, hpStr, startX + hudWidth - 5 - hpWidth, currentY, 0xFFFFFF, true);

            // --- [D. 额外状态] ---
            if (t.isPurgeActive()) {
                gfx.drawString(mc.font, "§4⚔", startX + hudWidth + 2, currentY, 0xFFFFFF, true);
            }

            // 换行 (间距已经在 rowHeight 里包含了)
            currentY += rowHeight;
        }
    }// ==========================================
    // ✅ 新增：箭矢清理系统 (防止水下堆积卡顿)
    // ==========================================

// ==========================================
    // ✅ 全局清理系统 (气泡 + 箭矢)
    // ==========================================
    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        // 只在服务端、每帧结束时运行
        if (event.level.isClientSide || event.phase != TickEvent.Phase.END) return;

        // [Part A] 气泡清理与动画 (每 tick 运行，保证丝滑上升)
        // 在 onLevelTick 方法里
        // [Part A] 气泡清理
        if (event.level instanceof ServerLevel level) {
            for (Entity e : level.getAllEntities()) {
                // ✅ 修改：检测 ArmorStand 而不是 TextDisplay
                if (e instanceof net.minecraft.world.entity.decoration.ArmorStand && e.getPersistentData().contains("BubbleLife")) {
                    int life = e.getPersistentData().getInt("BubbleLife");
                    if (life <= 0) {
                        e.discard();
                    } else {
                        e.getPersistentData().putInt("BubbleLife", life - 1);
                        e.setPos(e.getX(), e.getY() + 0.02, e.getZ());
                    }
                }
            }
        }

        // [Part B] 箭矢垃圾回收 (每 20 tick / 1秒 运行一次，节省性能)
        if (event.level.getGameTime() % 20 == 0 && event.level instanceof ServerLevel level) {
            List<Entity> toRemove = new ArrayList<>();

            for (Entity entity : level.getAllEntities()) {
                // 筛选炮台射出的箭
                if (entity instanceof AbstractArrow arrow && arrow.getPersistentData().getBoolean("IsTurretArrow")) {
                    // 条件1: 掉水里了
                    // 条件2: 存在太久了 (>20秒)
                    if (arrow.isInWater() || arrow.tickCount > 400) {
                        toRemove.add(arrow);
                    }
                }
            }

            // 批量销毁
            for (Entity e : toRemove) {
                level.sendParticles(ParticleTypes.BUBBLE, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0.1);
                e.discard();
            }
        }
    }


}
