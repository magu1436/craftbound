package com.magu1436.craftbound.integration.jade;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.furnace.BlacksmithFurnaceBlock;
import com.magu1436.craftbound.occupations.blacksmith.furnace.BlacksmithFurnaceBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
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
