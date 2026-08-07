package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 腐敗品を手動・自動とも確実に堆肥化し、自動回収でも堆肥を返す。 */
@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void craftbound$acceptAnySpoiledFood(
            BlockState state,
            Level level,
            BlockPos position,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> callback
    ) {
        ItemStack stack = player.getItemInHand(hand);
        craftbound$advanceQuality(stack, level);
        int currentLevel = state.getValue(ComposterBlock.LEVEL);
        if (!FoodQualityData.isSpoiled(stack) || currentLevel >= 7) {
            return;
        }

        if (!level.isClientSide) {
            BlockState updatedState = craftbound$addGuaranteedItem(player, state, level, position);
            level.levelEvent(1500, position, updatedState != state ? 1 : 0);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        callback.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private static void craftbound$insertAnySpoiledFood(
            Entity entity,
            BlockState state,
            net.minecraft.server.level.ServerLevel level,
            ItemStack stack,
            BlockPos position,
            CallbackInfoReturnable<BlockState> callback
    ) {
        FoodQualityData.advanceLoadedTime(stack, level.getGameTime(), 1.0D);
        if (!FoodQualityData.isSpoiled(stack) || state.getValue(ComposterBlock.LEVEL) >= 7) {
            return;
        }
        BlockState updatedState = craftbound$addGuaranteedItem(entity, state, level, position);
        stack.shrink(1);
        callback.setReturnValue(updatedState);
    }

    @Inject(method = "getContainer", at = @At("HEAD"), cancellable = true)
    private void craftbound$provideQualityAwareAutomation(
            BlockState state,
            LevelAccessor level,
            BlockPos position,
            CallbackInfoReturnable<WorldlyContainer> callback
    ) {
        int currentLevel = state.getValue(ComposterBlock.LEVEL);
        if (currentLevel < 7) {
            callback.setReturnValue(new SpoiledAwareInputContainer(state, level, position));
        } else if (currentLevel == ComposterBlock.READY) {
            callback.setReturnValue(new CompostOutputContainer(state, level, position));
        }
    }

    @Inject(method = "addItem", at = @At("HEAD"), cancellable = true)
    private static void craftbound$alwaysAcceptSpoiledFood(
            @Nullable Entity entity,
            BlockState state,
            LevelAccessor level,
            BlockPos position,
            ItemStack stack,
            CallbackInfoReturnable<BlockState> callback
    ) {
        craftbound$advanceQuality(stack, level);
        int currentLevel = state.getValue(ComposterBlock.LEVEL);
        if (!FoodQualityData.isSpoiled(stack) || currentLevel >= 7) {
            return;
        }

        callback.setReturnValue(craftbound$addGuaranteedItem(entity, state, level, position));
    }

    private static BlockState craftbound$addGuaranteedItem(
            @Nullable Entity entity,
            BlockState state,
            LevelAccessor level,
            BlockPos position
    ) {
        int currentLevel = state.getValue(ComposterBlock.LEVEL);
        int updatedLevel = currentLevel + 1;
        BlockState updatedState = state.setValue(ComposterBlock.LEVEL, updatedLevel);
        level.setBlock(position, updatedState, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, position, GameEvent.Context.of(entity, updatedState));
        if (updatedLevel == 7) {
            level.scheduleTick(position, state.getBlock(), 20);
        }
        return updatedState;
    }

    private static BlockState craftbound$addNormalItem(
            BlockState state,
            LevelAccessor level,
            BlockPos position,
            ItemStack stack
    ) {
        int currentLevel = state.getValue(ComposterBlock.LEVEL);
        float chance = ComposterBlock.COMPOSTABLES.getFloat(stack.getItem());
        if ((currentLevel != 0 || !(chance > 0.0F))
                && !(level.getRandom().nextDouble() < chance)) {
            return state;
        }
        return craftbound$addGuaranteedItem(null, state, level, position);
    }

    private static final class SpoiledAwareInputContainer extends SimpleContainer implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos position;
        private boolean changed;

        private SpoiledAwareInputContainer(BlockState state, LevelAccessor level, BlockPos position) {
            super(1);
            this.state = state;
            this.level = level;
            this.position = position;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.UP ? new int[]{0} : new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            craftbound$advanceQuality(stack, level);
            return !changed
                    && side == Direction.UP
                    && (FoodQualityData.isSpoiled(stack)
                    || ComposterBlock.COMPOSTABLES.containsKey(stack.getItem()));
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return false;
        }

        @Override
        public void setChanged() {
            ItemStack stack = getItem(0);
            if (stack.isEmpty()) {
                return;
            }
            craftbound$advanceQuality(stack, level);
            changed = true;
            BlockState updatedState = FoodQualityData.isSpoiled(stack)
                    ? craftbound$addGuaranteedItem(null, state, level, position)
                    : craftbound$addNormalItem(state, level, position, stack);
            level.levelEvent(1500, position, updatedState != state ? 1 : 0);
            removeItemNoUpdate(0);
        }
    }

    private static void craftbound$advanceQuality(ItemStack stack, LevelAccessor level) {
        if (level instanceof ServerLevel serverLevel) {
            FoodQualityData.advanceLoadedTime(stack, serverLevel.getGameTime(), 1.0D);
        }
    }

    private static final class CompostOutputContainer extends SimpleContainer implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos position;
        private boolean changed;

        private CompostOutputContainer(BlockState state, LevelAccessor level, BlockPos position) {
            super(new ItemStack(Craftbound.COMPOST.get()));
            this.state = state;
            this.level = level;
            this.position = position;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.DOWN ? new int[]{0} : new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return !changed && side == Direction.DOWN && stack.is(Craftbound.COMPOST.get());
        }

        @Override
        public void setChanged() {
            BlockState emptyState = state.setValue(ComposterBlock.LEVEL, 0);
            level.setBlock(position, emptyState, 3);
            level.gameEvent(GameEvent.BLOCK_CHANGE, position, GameEvent.Context.of(null, emptyState));
            changed = true;
        }
    }
}
