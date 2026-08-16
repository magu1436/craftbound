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
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.menu.*;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

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
        if (table.progress().isPresent()) {
            var session = BlacksmithOperationSessionRegistry.acquire(serverPlayer, table);
            if (session.isEmpty()) return InteractionResult.PASS;
            CarvingMenu.open(serverPlayer, table, session.get());
            return InteractionResult.CONSUME;
        }
        ResourceLocation heldId = ForgeRegistries.ITEMS.getKey(held.getItem());
        List<NonMetalPartDefinition> candidates = NonMetalPartDefinitions.INSTANCE.matching(table.material())
            .stream().filter(part -> NonMetalMaterialDefinitions.INSTANCE.get(part.materialProfileId())
                .map(material -> material.toolItemId().equals(heldId)).orElse(false)).toList();
        if (candidates.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.craftbound.carving.wrong_tool"), true);
            return InteractionResult.CONSUME;
        }
        CarvingPartSelectionMenu.open(serverPlayer, pos, candidates);
        return InteractionResult.CONSUME;
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer
            && level.getBlockEntity(pos) instanceof CarvingTableBlockEntity table && table.progress().isPresent()) {
            CarvingGameService.finalizeCarvingWithoutExperience(table);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
    @SuppressWarnings("deprecation") @Override public void onRemove(BlockState state, Level level, BlockPos pos,
        BlockState next, boolean moved) {
        if (state.getBlock() != next.getBlock() && level instanceof ServerLevel server
            && level.getBlockEntity(pos) instanceof CarvingTableBlockEntity table) {
            BlacksmithOperationSessionRegistry.release(table);
            if (table.progress().isPresent()) CarvingGameService.finalizeCarvingWithoutExperience(table);
            table.dropContents(server);
        }
        super.onRemove(state, level, pos, next, moved);
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
}
