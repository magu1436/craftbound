package com.magu1436.craftbound.occupations.foodproducer.processing;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;

/** FoodProducer の初期加工設備に共通する設置ブロック。 */
public final class FoodProcessingBlock extends BaseEntityBlock {

    public FoodProcessingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FoodProcessingBlockEntity processor) {
            if (isCreateDeployer(player)) {
                processor.startCreateAutomation();
                return InteractionResult.CONSUME;
            }
            NetworkHooks.openScreen(serverPlayer, processor, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isCreateDeployer(Player player) {
        return ModList.get().isLoaded("create")
                && player instanceof FakePlayer
                && player.getClass().getName().equals(
                        "com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer"
                );
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FoodProcessingBlockEntity processor) {
            Containers.dropContents(level, pos, processor);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoodProcessingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, Craftbound.FOOD_PROCESSING_BLOCK_ENTITY.get(),
                        FoodProcessingBlockEntity::serverTick);
    }
}
