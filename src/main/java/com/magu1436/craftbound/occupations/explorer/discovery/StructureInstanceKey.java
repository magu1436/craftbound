package com.magu1436.craftbound.occupations.explorer.discovery;

import java.util.Objects;

/** 生成された構造物個体を開始チャンク単位で識別するキー。 */
public record StructureInstanceKey(
    String dimensionId,
    String structureId,
    int startChunkX,
    int startChunkZ
) {
    public StructureInstanceKey {
        requireId(dimensionId, "dimension id");
        requireId(structureId, "structure id");
    }

    public String serialize() {
        return dimensionId + "|" + structureId + "|"
            + startChunkX + "|" + startChunkZ;
    }

    private static void requireId(String id, String name) {
        Objects.requireNonNull(id, name + " is null");
        if (id.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
    }
}
