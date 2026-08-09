package com.magu1436.craftbound.occupations.foodproducer.storage;

import java.util.List;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** 27スロットを持つ、連結しない設置型保存設備。 */
public final class PreservationStorageBlock extends BaseEntityBlock {

    private final double preservationMultiplier;

    public PreservationStorageBlock(Properties properties, double preservationMultiplier) {
        super(properties);
        this.preservationMultiplier = Math.max(1.0D, preservationMultiplier);
    }

    public double preservationMultiplier() {
        return preservationMultiplier;
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
                && level.getBlockEntity(pos) instanceof PreservationStorageBlockEntity storage) {
            NetworkHooks.openScreen(serverPlayer, storage, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PreservationStorageBlockEntity(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack result = new ItemStack(this);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof PreservationStorageBlockEntity storage) {
            storage.saveToItem(result);
        }
        return List.of(result);
    }

    @Override
    public void playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof PreservationStorageBlockEntity storage
                && !storage.isEmpty()) {
            ItemStack result = new ItemStack(this);
            storage.saveToItem(result);
            popResource(level, pos, result);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PreservationStorageBlockEntity storage) {
            storage.settleAndPauseContents();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack result = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof PreservationStorageBlockEntity storage) {
            storage.saveToItem(result);
        }
        return result;
    }
}
