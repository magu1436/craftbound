package com.magu1436.craftbound.integration.jade;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.furnace.BlacksmithFurnaceBlock;
import com.magu1436.craftbound.occupations.blacksmith.furnace.BlacksmithFurnaceBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlock;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class CraftboundJadePlugin implements IWailaPlugin {

    private static final ResourceLocation BLACKSMITH_FURNACE_HEAT_SOURCE =
            CraftboundUtilities.createResourceLocation(
                "blacksmith_furnace_heat_source"
            );
    private static final ResourceLocation FORGING_TABLE_PART =
            CraftboundUtilities.createResourceLocation(
                "forging_table_part"
            );
    private static final String TAG_HAS_HEAT_SOURCE = "HasHeatSource";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
            BlacksmithFurnaceDataProvider.INSTANCE,
            BlacksmithFurnaceBlockEntity.class
        );
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(BLACKSMITH_FURNACE_HEAT_SOURCE, true);
        registration.registerBlockComponent(
            BlacksmithFurnaceComponentProvider.INSTANCE,
            BlacksmithFurnaceBlock.class
        );
        registration.addConfig(FORGING_TABLE_PART, true);
        registration.registerBlockComponent(
            ForgingTableComponentProvider.INSTANCE,
            ForgingTableBlock.class
        );
    }

    private enum ForgingTableComponentProvider
        implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(
            ITooltip tooltip,
            BlockAccessor accessor,
            IPluginConfig config
        ) {
            BlockEntity blockEntity = accessor.getBlockEntity();
            if (!(blockEntity instanceof ForgingTableBlockEntity forgingTable)) {
                return;
            }
            ItemStack displayStack = forgingTable.getDisplayStack();
            if (displayStack.isEmpty()) {
                return;
            }
            tooltip.add(Component.translatable(
                "jade.craftbound.forging_table.part",
                resolvePartName(displayStack)
            ));
        }

        @Override
        public ResourceLocation getUid() {
            return FORGING_TABLE_PART;
        }
    }

    private static Component resolvePartName(ItemStack stack) {
        return RoughMetalPartStateService.read(stack).<Component>map(state -> {
            Item output = ForgeRegistries.ITEMS.getValue(state.outputItemId());
            Component partName = output == null
                ? Component.literal(state.definitionId().toString())
                : output.getDescription();
            return Component.translatable(
                "item.craftbound.metal_part.named",
                Component.translatable(
                    "metal." + state.metalId().getNamespace() + "."
                        + state.metalId().getPath().replace('/', '.')
                ),
                partName
            );
        }).orElseGet(stack::getHoverName);
    }

    private enum BlacksmithFurnaceDataProvider
        implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(
            CompoundTag data,
            BlockAccessor accessor
        ) {
            BlockEntity blockEntity = accessor.getBlockEntity();
            if (blockEntity instanceof BlacksmithFurnaceBlockEntity furnace) {
                data.putBoolean(
                    TAG_HAS_HEAT_SOURCE,
                    furnace.hasHeatSource()
                );
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BLACKSMITH_FURNACE_HEAT_SOURCE;
        }
    }

    private enum BlacksmithFurnaceComponentProvider
        implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(
            ITooltip tooltip,
            BlockAccessor accessor,
            IPluginConfig config
        ) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(TAG_HAS_HEAT_SOURCE)) {
                return;
            }

            Component status = Component.translatable(
                data.getBoolean(TAG_HAS_HEAT_SOURCE)
                    ? "jade.craftbound.heat_source.present"
                    : "jade.craftbound.heat_source.absent"
            );
            tooltip.add(Component.translatable(
                "jade.craftbound.blacksmith_furnace.heat_source",
                status
            ));
        }

        @Override
        public ResourceLocation getUid() {
            return BLACKSMITH_FURNACE_HEAT_SOURCE;
        }
    }
}
