package com.example.examplemod;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class EnderPearlLootModifier extends LootModifier {
    public static final Supplier<Codec<EnderPearlLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, EnderPearlLootModifier::new)));

    private final Random random = new Random();

    public EnderPearlLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // Verify target is a Monster (Hostile Mob)
        if (!context.hasParam(LootContextParams.THIS_ENTITY)) {
            return generatedLoot;
        }

        Entity entity = context.getParam(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof Monster)) {
            return generatedLoot;
        }

        // Get drop chance from Config
        double chance = TurretConfig.COMMON.enderPearlDropChanceBase.get();
        // Optionally add bonus based on other factors if needed, but for now just base chance
        
        if (random.nextDouble() < chance) {
            generatedLoot.add(new ItemStack(Items.ENDER_PEARL));
        }
        
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
