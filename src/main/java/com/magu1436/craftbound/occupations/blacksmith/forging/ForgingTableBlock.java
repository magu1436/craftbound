package com.magu1436.craftbound.occupations.blacksmith.forging;

import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartItem;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.occupations.blacksmith.forging.menu.ForgingMenu;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class ForgingTableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ForgingTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return new ForgingTableBlockEntity(pos, state);
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
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack heldItem = player.getMainHandItem();
        if (level.isClientSide) {
            return heldItem.isEmpty() || heldItem.getItem() instanceof RoughMetalPartItem
                || heldItem.is(CraftboundItems.SMITHING_HAMMER.get())
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ForgingTableBlockEntity forgingTable)) {
            return InteractionResult.PASS;
        }

        boolean succeeded;
        if (forgingTable.hasPendingOutputs()) {
            succeeded = heldItem.isEmpty() && forgingTable.tryCollectPendingOutputs(serverPlayer);
        } else if (!forgingTable.hasWorkingPart()) {
            succeeded = forgingTable.tryInsertWorkingPart(serverPlayer, hand);
        } else if (heldItem.is(CraftboundItems.SMITHING_HAMMER.get())) {
            var session = BlacksmithOperationSessionRegistry.acquire(serverPlayer, forgingTable);
            if (session.isEmpty()) return InteractionResult.PASS;
            NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new ForgingMenu(containerId, inventory, pos),
                    Component.translatable("container.craftbound.forging_table")
                ),
                buffer -> buffer.writeBlockPos(pos)
            );
            ForgingPacketHandler.sync(serverPlayer, session.get());
            succeeded = true;
        } else {
            succeeded = heldItem.isEmpty() && forgingTable.getActiveSession() == null
                && forgingTable.tryExtractWorkingPart(serverPlayer);
        }
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
            if (blockEntity instanceof ForgingTableBlockEntity forgingTable) {
                BlacksmithOperationSessionRegistry.release(forgingTable, true);
                forgingTable.dropContents(serverLevel);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
