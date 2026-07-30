package com.magu1436.craftbound.occupations.foodproducer.farming;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerExperience;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityRolls;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodProducerFarmingEvents {

    private FoodProducerFarmingEvents() {
    }

    /** 新しく耕した農地は常に肥沃度0から始める. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFarmlandCreated(BlockEvent.BlockToolModificationEvent event) {
        if (event.isCanceled() || event.isSimulated() || !event.getToolAction().equals(ToolActions.HOE_TILL)) {
            return;
        }
        if (!(event.getContext().getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!event.getState().is(Blocks.FARMLAND) && event.getFinalState().is(Blocks.FARMLAND)) {
            FarmlandFertility.set(level, event.getPos(), 0);
        }
    }

    /** 耕地作物の植え付け時に肥沃度を検査し、成功時だけ消費する. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCropPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !FoodProducerCropTargets.consumesFarmlandFertility(event.getPlacedBlock())) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        int cost = FarmlandFertility.BASE_PLANTING_COST;
        Entity placer = event.getEntity();
        if (placer instanceof ServerPlayer player && FoodProducerSkills.fertilityManagementRank(player) >= 2) {
            cost = FarmlandFertility.FERTILITY_MANAGEMENT_II_PLANTING_COST;
        }

        if (!FarmlandFertility.consume(level, event.getPos().below(), cost)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFarmlandBroken(BlockEvent.BreakEvent event) {
        if (event.getState().is(Blocks.FARMLAND) && event.getLevel() instanceof ServerLevel level) {
            FarmlandFertility.set(level, event.getPos(), 0);
        }
        if (event.getState().is(Blocks.SUGAR_CANE)
                && event.getLevel() instanceof ServerLevel level
                && event.getPlayer() instanceof ServerPlayer player
                && !(player instanceof FakePlayer)) {
            SugarCaneHarvestTracker.begin(level, event.getPos(), player);
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrampled(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FarmlandFertility.set(level, event.getPos(), 0);
        }
    }

    /** FoodProducer対象作物への骨粉は、プレイヤー・ディスペンサーとも無効化する. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBonemeal(BonemealEvent event) {
        if (FoodProducerCropTargets.isTarget(event.getBlock())) {
            event.setCanceled(true);
        }
    }

    /** 乾燥などで土へ戻った読み込み済み農地の保存値を除去する. */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 20L == 0L) {
            FarmlandFertilitySavedData.get(level).removeInvalidLoadedPositions(level);
        }
        SugarCaneHarvestTracker.cleanup(level);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (rejectInfertilePlanting(event, state)) {
            return;
        }
        if (state.is(Blocks.COMPOSTER) && state.getValue(ComposterBlock.LEVEL) == ComposterBlock.READY) {
            extractCompost(event, state);
            return;
        }
        if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            harvestSweetBerries(event, state);
            return;
        }
        if ((state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT))
                && state.hasProperty(CaveVines.BERRIES)
                && state.getValue(CaveVines.BERRIES)) {
            harvestGlowBerries(event, state);
        }
    }

    /** 種を消費する前に肥沃度不足を拒否し、設置後キャンセルによる種の消失を防ぐ. */
    private static boolean rejectInfertilePlanting(
            PlayerInteractEvent.RightClickBlock event,
            BlockState clickedState
    ) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !clickedState.is(Blocks.FARMLAND)
                || !isFarmlandPlantingItem(event.getItemStack())) {
            return false;
        }

        int cost = FarmlandFertility.BASE_PLANTING_COST;
        if (event.getEntity() instanceof ServerPlayer player
                && FoodProducerSkills.fertilityManagementRank(player) >= 2) {
            cost = FarmlandFertility.FERTILITY_MANAGEMENT_II_PLANTING_COST;
        }
        if (FarmlandFertility.get(level, event.getPos()) >= cost) {
            return false;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            // クライアントが先に減らした手持ち数を、未消費のサーバー値へ即時復元する.
            player.containerMenu.sendAllDataToRemote();
        }
        return true;
    }

    private static boolean isFarmlandPlantingItem(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS)
                || stack.is(Items.MELON_SEEDS);
    }

    private static void extractCompost(PlayerInteractEvent.RightClickBlock event, BlockState state) {
        if (event.getLevel() instanceof ServerLevel level) {
            ServerPlayer player = realPlayer(event.getEntity());
            int count = 1;
            if (player != null
                    && FoodProducerSkills.fertilityManagementRank(player) >= 3
                    && level.random.nextBoolean()) {
                count++;
            }
            Block.popResource(level, event.getPos().above(), new ItemStack(Craftbound.COMPOST.get(), count));
            BlockState emptyState = state.setValue(ComposterBlock.LEVEL, 0);
            level.setBlock(event.getPos(), emptyState, 3);
            level.gameEvent(GameEvent.BLOCK_CHANGE, event.getPos(), GameEvent.Context.of(event.getEntity(), emptyState));
            level.playSound(null, event.getPos(), SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player != null) {
                FoodProducerExperience.add(player, 1);
            }
        }
        cancelWithSuccess(event);
    }

    private static void harvestSweetBerries(PlayerInteractEvent.RightClickBlock event, BlockState state) {
        int age = state.getValue(SweetBerryBushBlock.AGE);
        boolean fullyGrown = age == SweetBerryBushBlock.MAX_AGE;
        if ((!fullyGrown && event.getItemStack().is(Items.BONE_MEAL)) || age <= 1) {
            return;
        }

        if (event.getLevel() instanceof ServerLevel level) {
            ServerPlayer player = realPlayer(event.getEntity());
            int baseCount = 1 + level.random.nextInt(2) + (fullyGrown ? 1 : 0);
            dropQualityHarvest(level, event.getPos(), Items.SWEET_BERRIES.getDefaultInstance(), baseCount, player);
            BlockState harvestedState = state.setValue(SweetBerryBushBlock.AGE, 1);
            level.setBlock(event.getPos(), harvestedState, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, event.getPos(), GameEvent.Context.of(event.getEntity(), harvestedState));
            level.playSound(null, event.getPos(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                    SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            if (player != null) {
                FoodProducerExperience.add(player, 1);
            }
        }
        cancelWithSuccess(event);
    }

    private static void harvestGlowBerries(PlayerInteractEvent.RightClickBlock event, BlockState state) {
        if (event.getLevel() instanceof ServerLevel level) {
            ServerPlayer player = realPlayer(event.getEntity());
            dropQualityHarvest(level, event.getPos(), Items.GLOW_BERRIES.getDefaultInstance(), 1, player);
            BlockState harvestedState = state.setValue(CaveVines.BERRIES, false);
            level.setBlock(event.getPos(), harvestedState, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, event.getPos(), GameEvent.Context.of(event.getEntity(), harvestedState));
            level.playSound(null, event.getPos(), SoundEvents.CAVE_VINES_PICK_BERRIES,
                    SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            if (player != null) {
                FoodProducerExperience.add(player, 1);
            }
        }
        cancelWithSuccess(event);
    }

    private static void dropQualityHarvest(
            ServerLevel level,
            BlockPos position,
            ItemStack template,
            int baseCount,
            ServerPlayer player
    ) {
        int yieldRank = player == null ? 0 : FoodProducerSkills.yieldManagementRank(player);
        int qualityRank = player == null ? 0 : FoodProducerSkills.qualityCultivationRank(player);
        int count = baseCount;
        if (yieldRank > 0 && level.random.nextInt(100) < yieldRank * 10) {
            count++;
        }

        java.util.EnumMap<FoodQuality, Integer> counts = new java.util.EnumMap<>(FoodQuality.class);
        for (int index = 0; index < count; index++) {
            counts.merge(FoodQualityRolls.roll(level.random, qualityRank), 1, Integer::sum);
        }
        counts.forEach((quality, qualityCount) -> {
            ItemStack drop = template.copy();
            drop.setCount(qualityCount);
            FoodQualityData.initialize(drop, quality, level.getGameTime());
            Block.popResource(level, position, drop);
        });
    }

    private static ServerPlayer realPlayer(Entity entity) {
        return entity instanceof ServerPlayer player && !(player instanceof FakePlayer) ? player : null;
    }

    private static void cancelWithSuccess(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }
}
