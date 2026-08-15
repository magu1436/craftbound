package com.magu1436.craftbound.occupations.foodproducer.foraging;

import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.PlayerInventoryQualityEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

/** 移植不能で、実プレイヤーの右クリックだけから再生式に収穫できる採集源。 */
public final class RegionalForageBlock extends BushBlock {

    public static final BooleanProperty HARVESTED = BooleanProperty.create("harvested");
    public static final int REGROW_TICKS = 48_000;

    private static final VoxelShape MATURE_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 11.0D, 14.0D);
    private static final VoxelShape HARVESTED_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 5.0D, 13.0D);

    private final ResourceLocation ingredientId;

    public RegionalForageBlock(Properties properties, ResourceLocation ingredientId) {
        super(properties);
        this.ingredientId = ingredientId;
        registerDefaultState(stateDefinition.any().setValue(HARVESTED, false));
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
        if (state.getValue(HARVESTED) || player instanceof FakePlayer) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Item ingredient = ForgeRegistries.ITEMS.getValue(ingredientId);
        if (ingredient == null) {
            return InteractionResult.FAIL;
        }

        ItemStack harvest = new ItemStack(ingredient, Mth.nextInt(level.random, 2, 4));
        FoodQualityData.initialize(harvest, FoodQuality.STANDARD, level.getGameTime());
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerInventoryQualityEvents.prepareForInventoryInsertion(serverPlayer, harvest);
        }
        player.getInventory().add(harvest);
        if (!harvest.isEmpty()) {
            player.drop(harvest, false);
        }

        level.setBlock(pos, state.setValue(HARVESTED, true), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, REGROW_TICKS);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HARVESTED)) {
            level.setBlock(pos, state.setValue(HARVESTED, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return !below.isAir()
                && !below.is(BlockTags.LEAVES)
                && Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return state.getValue(HARVESTED) ? HARVESTED_SHAPE : MATURE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HARVESTED);
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockGetter level,
            BlockPos pos,
            BlockState state
    ) {
        return ItemStack.EMPTY;
    }

}
