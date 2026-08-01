package com.magu1436.craftbound.occupations.architect.capability;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * 座標ごとの状態をビットフラグで保持する建築チャンクデータ。
 */
public final class ConstructionChunkDataImpl
    implements ConstructionChunkData {

    private static final int DATA_VERSION = 2;
    private static final byte PLAYER_PLACED = 1;
    private static final byte XP_REWARDED = 1 << 1;
    private static final String VERSION_KEY = "Version";
    private static final String LEGACY_POSITIONS_KEY = "Positions";
    private static final String POSITION_STATES_KEY = "PositionStates";
    private static final String PENDING_CONSTRUCTIONS_KEY =
        "PendingConstructions";
    private static final String POSITION_KEY = "Position";
    private static final String BLOCK_KEY = "Block";
    private static final String PLAYER_KEY = "Player";
    private static final String MATURE_AT_KEY = "MatureAt";
    private static final String LIFETIME_USE_COUNT_KEY = "LifetimeUseCount";
    private static final String RECENT_USE_COUNT_KEY = "RecentUseCount";

    private final int minBuildHeight;
    private final Int2ByteOpenHashMap positionStates =
        new Int2ByteOpenHashMap();
    private final Int2ObjectOpenHashMap<PendingConstruction>
        pendingConstructions = new Int2ObjectOpenHashMap<>();

    public ConstructionChunkDataImpl(int minBuildHeight) {
        this.minBuildHeight = minBuildHeight;
    }

    @Override
    public boolean isPlayerPlaced(BlockPos pos) {
        return hasFlag(encode(pos), PLAYER_PLACED);
    }

    @Override
    public boolean markPlayerPlaced(BlockPos pos) {
        return setFlag(encode(pos), PLAYER_PLACED);
    }

    @Override
    public boolean clearPlayerPlaced(BlockPos pos) {
        return clearFlag(encode(pos), PLAYER_PLACED);
    }

    @Override
    public int clearAllPlayerPlaced(IntCollection localPositions) {
        Objects.requireNonNull(localPositions, "local positions is null");
        int changed = 0;
        for (int localPosition : localPositions) {
            if (clearFlag(localPosition, PLAYER_PLACED)) {
                changed++;
            }
        }
        return changed;
    }

    @Override
    public boolean isXpRewarded(BlockPos pos) {
        return hasFlag(encode(pos), XP_REWARDED);
    }

    @Override
    public boolean tryMarkXpRewarded(BlockPos pos) {
        return setFlag(encode(pos), XP_REWARDED);
    }

    @Override
    public Optional<PendingConstruction> getPending(BlockPos pos) {
        return Optional.ofNullable(pendingConstructions.get(encode(pos)));
    }

    @Override
    public void putPending(BlockPos pos, PendingConstruction pending) {
        pendingConstructions.put(
            encode(pos),
            Objects.requireNonNull(pending, "pending is null")
        );
    }

    @Override
    public boolean removePending(BlockPos pos) {
        return pendingConstructions.remove(encode(pos)) != null;
    }

    @Override
    public int[] getPendingPositions() {
        return pendingConstructions.keySet().toIntArray();
    }

    @Override
    public int[] getPlayerPlacedPositions() {
        IntArrayList positions = new IntArrayList();
        for (var entry : positionStates.int2ByteEntrySet()) {
            if ((entry.getByteValue() & PLAYER_PLACED) != 0) {
                positions.add(entry.getIntKey());
            }
        }
        return positions.toIntArray();
    }

    @Override
    public CompoundTag savePersistentData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, DATA_VERSION);
        tag.putIntArray(POSITION_STATES_KEY, encodePositionStates());

        ListTag pendingTags = new ListTag();
        for (var entry : pendingConstructions.int2ObjectEntrySet()) {
            PendingConstruction pending = entry.getValue();
            CompoundTag pendingTag = new CompoundTag();
            pendingTag.putInt(POSITION_KEY, entry.getIntKey());
            pendingTag.putString(BLOCK_KEY, pending.expectedBlockId().toString());
            pendingTag.putUUID(PLAYER_KEY, pending.playerId());
            pendingTag.putLong(MATURE_AT_KEY, pending.matureAtGameTime());
            pendingTag.putInt(
                LIFETIME_USE_COUNT_KEY,
                pending.lifetimeUseCountSnapshot()
            );
            pendingTag.putInt(
                RECENT_USE_COUNT_KEY,
                pending.recentUseCountSnapshot()
            );
            pendingTags.add(pendingTag);
        }
        tag.put(PENDING_CONSTRUCTIONS_KEY, pendingTags);
        return tag;
    }

    @Override
    public void loadPersistentData(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag is null");
        positionStates.clear();
        pendingConstructions.clear();

        int version = tag.contains(VERSION_KEY, Tag.TAG_INT)
            ? tag.getInt(VERSION_KEY)
            : 0;
        if (version == 1) {
            loadLegacyPositions(tag);
            return;
        }
        if (version != DATA_VERSION) {
            return;
        }

        int[] encodedStates = tag.getIntArray(POSITION_STATES_KEY);
        for (int index = 0; index + 1 < encodedStates.length; index += 2) {
            byte state = (byte) encodedStates[index + 1];
            if (state != 0) {
                positionStates.put(encodedStates[index], state);
            }
        }
        loadPendingConstructions(tag);
    }

    public static int encodeLocalPosition(
        BlockPos pos,
        int minBuildHeight
    ) {
        Objects.requireNonNull(pos, "pos is null");
        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        int localY = pos.getY() - minBuildHeight;
        return (localY << 8) | (localZ << 4) | localX;
    }

    private int encode(BlockPos pos) {
        return encodeLocalPosition(pos, minBuildHeight);
    }

    private boolean hasFlag(int localPosition, byte flag) {
        return (positionStates.get(localPosition) & flag) != 0;
    }

    private boolean setFlag(int localPosition, byte flag) {
        byte previous = positionStates.get(localPosition);
        if ((previous & flag) != 0) {
            return false;
        }
        positionStates.put(localPosition, (byte) (previous | flag));
        return true;
    }

    private boolean clearFlag(int localPosition, byte flag) {
        byte previous = positionStates.get(localPosition);
        if ((previous & flag) == 0) {
            return false;
        }
        byte updated = (byte) (previous & ~flag);
        if (updated == 0) {
            positionStates.remove(localPosition);
        } else {
            positionStates.put(localPosition, updated);
        }
        return true;
    }

    private int[] encodePositionStates() {
        int[] encoded = new int[positionStates.size() * 2];
        int index = 0;
        for (var entry : positionStates.int2ByteEntrySet()) {
            encoded[index++] = entry.getIntKey();
            encoded[index++] = entry.getByteValue();
        }
        return encoded;
    }

    private void loadLegacyPositions(CompoundTag tag) {
        if (!tag.contains(LEGACY_POSITIONS_KEY, Tag.TAG_LONG_ARRAY)) {
            return;
        }
        for (long packedPosition : tag.getLongArray(LEGACY_POSITIONS_KEY)) {
            setFlag(encode(BlockPos.of(packedPosition)), PLAYER_PLACED);
        }
    }

    private void loadPendingConstructions(CompoundTag tag) {
        ListTag pendingTags = tag.getList(
            PENDING_CONSTRUCTIONS_KEY,
            Tag.TAG_COMPOUND
        );
        for (Tag rawTag : pendingTags) {
            CompoundTag pendingTag = (CompoundTag) rawTag;
            ResourceLocation blockId = ResourceLocation.tryParse(
                pendingTag.getString(BLOCK_KEY)
            );
            if (blockId == null || !pendingTag.hasUUID(PLAYER_KEY)) {
                continue;
            }
            pendingConstructions.put(
                pendingTag.getInt(POSITION_KEY),
                new PendingConstruction(
                    blockId,
                    pendingTag.getUUID(PLAYER_KEY),
                    pendingTag.getLong(MATURE_AT_KEY),
                    pendingTag.getInt(LIFETIME_USE_COUNT_KEY),
                    pendingTag.contains(RECENT_USE_COUNT_KEY, Tag.TAG_INT)
                        ? pendingTag.getInt(RECENT_USE_COUNT_KEY)
                        : 1
                )
            );
        }
    }
}
