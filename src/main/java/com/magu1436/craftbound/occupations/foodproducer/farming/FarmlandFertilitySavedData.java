package com.magu1436.craftbound.occupations.foodproducer.farming;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

final class FarmlandFertilitySavedData extends SavedData {

    private static final String DATA_NAME = "craftbound_food_producer_fertility";
    private static final String ENTRIES_TAG = "entries";
    private static final String POSITION_TAG = "position";
    private static final String FERTILITY_TAG = "fertility";

    private final Long2IntOpenHashMap fertilityByPosition = new Long2IntOpenHashMap();

    FarmlandFertilitySavedData() {
        fertilityByPosition.defaultReturnValue(0);
    }

    static FarmlandFertilitySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                FarmlandFertilitySavedData::load,
                FarmlandFertilitySavedData::new,
                DATA_NAME
        );
    }

    static FarmlandFertilitySavedData load(CompoundTag root) {
        FarmlandFertilitySavedData data = new FarmlandFertilitySavedData();
        ListTag entries = root.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (Tag rawEntry : entries) {
            CompoundTag entry = (CompoundTag) rawEntry;
            int fertility = FarmlandFertility.clamp(entry.getInt(FERTILITY_TAG));
            if (fertility > 0) {
                data.fertilityByPosition.put(entry.getLong(POSITION_TAG), fertility);
            }
        }
        return data;
    }

    int get(BlockPos position) {
        return fertilityByPosition.get(position.asLong());
    }

    void set(BlockPos position, int fertility) {
        int clamped = FarmlandFertility.clamp(fertility);
        if (clamped == 0) {
            fertilityByPosition.remove(position.asLong());
        } else {
            fertilityByPosition.put(position.asLong(), clamped);
        }
        setDirty();
    }

    void removeInvalidLoadedPositions(ServerLevel level) {
        boolean changed = false;
        LongIterator iterator = fertilityByPosition.keySet().iterator();
        while (iterator.hasNext()) {
            BlockPos position = BlockPos.of(iterator.nextLong());
            if (level.hasChunkAt(position) && !level.getBlockState(position).is(Blocks.FARMLAND)) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag entries = new ListTag();
        fertilityByPosition.long2IntEntrySet().forEach(entry -> {
            CompoundTag tag = new CompoundTag();
            tag.putLong(POSITION_TAG, entry.getLongKey());
            tag.putInt(FERTILITY_TAG, entry.getIntValue());
            entries.add(tag);
        });
        root.put(ENTRIES_TAG, entries);
        return root;
    }
}
