package com.magu1436.craftbound.occupations.blacksmith.furnace;

import javax.annotation.Nullable;

import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

public final class BlacksmithFurnaceBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public BlacksmithFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
            FACING,
            context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlacksmithFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
            type,
            CraftboundBlockEntities.BLACKSMITH_FURNACE.get(),
            BlacksmithFurnaceBlockEntity::serverTick
        );
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
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (level.isClientSide) {
            return heldItem.isEmpty()
                || heldItem.getItem() instanceof CrucibleItem
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BlacksmithFurnaceBlockEntity furnace)) {
            return InteractionResult.PASS;
        }

        boolean insertionCandidate = !furnace.hasCrucible()
            && heldItem.getItem() instanceof CrucibleItem;
        boolean extractionCandidate = furnace.hasCrucible()
            && heldItem.isEmpty();
        if (!insertionCandidate && !extractionCandidate) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        boolean succeeded = insertionCandidate
            ? furnace.tryInsertCrucible(serverPlayer, hand)
            : furnace.tryExtractCrucible(serverPlayer);
        return succeeded ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState newState,
        boolean movedByPiston
    ) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BlacksmithFurnaceBlockEntity furnace) {
                furnace.dropStoredCrucible(serverLevel);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos changedPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, changedPos, movedByPiston);
        if (!level.isClientSide && changedPos.equals(pos.below())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BlacksmithFurnaceBlockEntity furnace) {
                furnace.refreshHeatSource();
            }
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
