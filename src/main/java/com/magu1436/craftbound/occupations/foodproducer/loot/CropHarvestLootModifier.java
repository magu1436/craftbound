package com.magu1436.craftbound.occupations.foodproducer.loot;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.magu1436.craftbound.occupations.foodproducer.farming.FoodProducerCropTargets;
import com.magu1436.craftbound.occupations.foodproducer.farming.SugarCaneHarvestTracker;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityRolls;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerExperience;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.util.FakePlayer;

/** 成熟した対象作物の手動収穫報酬と、手動・自動収穫の初期品質を確定する. */
public final class CropHarvestLootModifier extends LootModifier {

    public static final Codec<CropHarvestLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, CropHarvestLootModifier::new));

    private CropHarvestLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null || !FoodProducerCropTargets.hasQualityHarvestDrop(state)) {
            return generatedLoot;
        }

        Entity actingEntity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        ServerPlayer player = actingEntity instanceof ServerPlayer serverPlayer
                && !(serverPlayer instanceof FakePlayer)
                ? serverPlayer
                : null;
        boolean rewardEligible = FoodProducerCropTargets.isMatureHarvest(state);

        if (state.is(net.minecraft.world.level.block.Blocks.SUGAR_CANE)) {
            Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
            if (origin != null) {
                Optional<SugarCaneHarvestTracker.HarvestContext> sugarCaneContext =
                        SugarCaneHarvestTracker.find(context.getLevel(), BlockPos.containing(origin));
                if (sugarCaneContext.isPresent()) {
                    player = sugarCaneContext.get().player();
                    rewardEligible = sugarCaneContext.get().claimReward();
                }
            }
        }
        int yieldRank = player == null ? 0 : FoodProducerSkills.yieldManagementRank(player);
        int qualityRank = player == null ? 0 : FoodProducerSkills.qualityCultivationRank(player);

        Item primaryProduct = FoodProducerCropTargets.primaryProduct(state);
        if (rewardEligible
                && primaryProduct != Items.AIR
                && yieldRank > 0
                && context.getRandom().nextInt(100) < yieldRank * 10) {
            generatedLoot.add(new ItemStack(primaryProduct));
        }

        ObjectArrayList<ItemStack> qualityLoot = applyQualityPerItem(
                generatedLoot,
                qualityRank,
                context.getLevel().getGameTime(),
                context
        );

        if (rewardEligible && player != null) {
            FoodProducerExperience.add(player, 1);
        }
        return qualityLoot;
    }

    private static ObjectArrayList<ItemStack> applyQualityPerItem(
            ObjectArrayList<ItemStack> generatedLoot,
            int qualityRank,
            long gameTime,
            LootContext context
    ) {
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
        for (ItemStack original : generatedLoot) {
            if (!FoodQualityItems.isQualityTarget(original)) {
                result.add(original);
                continue;
            }

            Map<FoodQuality, Integer> counts = new EnumMap<>(FoodQuality.class);
            for (int index = 0; index < original.getCount(); index++) {
                FoodQuality quality = FoodQualityRolls.roll(context.getRandom(), qualityRank);
                counts.merge(quality, 1, Integer::sum);
            }
            counts.forEach((quality, count) -> {
                ItemStack stack = original.copy();
                stack.setCount(count);
                FoodQualityData.initialize(stack, quality, gameTime);
                result.add(stack);
            });
        }
        return result;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
