package com.magu1436.craftbound.occupations.blacksmith.carving;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

public final class CarvingTableBlock extends BaseEntityBlock {
    public CarvingTableBlock(Properties properties) { super(properties); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CarvingTableBlockEntity(pos, state);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
            || !(level.getBlockEntity(pos) instanceof CarvingTableBlockEntity table)) return InteractionResult.PASS;
        ItemStack held = player.getMainHandItem();
        if (table.hasPendingOutputs()) return held.isEmpty() && table.collect(serverPlayer)
            ? InteractionResult.CONSUME : InteractionResult.PASS;
        if (table.material().isEmpty()) {
            if (!CarvingGameService.canInsert(held)) return InteractionResult.PASS;
            ItemStack one = held.copy(); one.setCount(1);
            if (!table.insertMaterial(one)) return InteractionResult.PASS;
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            if (table.progress().isPresent()) return CarvingGameService.finalizeCarving(table, serverPlayer)
                == CarvingGameService.FinalizeResult.COMPLETED ? InteractionResult.CONSUME : InteractionResult.PASS;
            ItemStack returned = table.takeUnselectedMaterial();
            if (returned.isEmpty()) return InteractionResult.PASS;
            player.setItemInHand(hand, returned); return InteractionResult.CONSUME;
        }
        // Part selection and carving menus are connected in Step 3.
        return InteractionResult.CONSUME;
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer operator
            && level.getBlockEntity(pos) instanceof CarvingTableBlockEntity table && table.progress().isPresent()) {
            CarvingGameService.finalizeCarving(table, operator, false);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
    @SuppressWarnings("deprecation") @Override public void onRemove(BlockState state, Level level, BlockPos pos,
        BlockState next, boolean moved) {
        if (state.getBlock() != next.getBlock() && level instanceof ServerLevel server
            && level.getBlockEntity(pos) instanceof CarvingTableBlockEntity table) {
            if (table.progress().isPresent()) CarvingGameService.finalizeCarving(table, null);
            table.dropContents(server);
        }
        super.onRemove(state, level, pos, next, moved);
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
}
