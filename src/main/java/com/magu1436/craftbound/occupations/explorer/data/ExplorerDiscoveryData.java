package com.magu1436.craftbound.occupations.explorer.data;

import java.util.Map;
import java.util.Set;

import com.magu1436.craftbound.common.capability.PlayerCapabilityData;
import com.magu1436.craftbound.occupations.explorer.discovery.StructureInstanceKey;

/** プレイヤーごとの永続的な発見履歴。 */
public interface ExplorerDiscoveryData
    extends PlayerCapabilityData<ExplorerDiscoveryData> {

    boolean hasDiscoveredBiome(String biomeId);

    boolean recordBiome(String biomeId);

    boolean hasDiscoveredStructure(
        String structureId,
        StructureInstanceKey instanceKey
    );

    boolean recordStructure(
        String structureId,
        StructureInstanceKey instanceKey
    );

    int getStructureDiscoveryCount(String structureId);

    boolean hasDiscoveredDimension(String dimensionId);

    boolean recordDimension(String dimensionId);

    Set<String> discoveredBiomes();

    Map<String, Set<StructureInstanceKey>> discoveredStructures();

    Set<String> discoveredDimensions();

    void replaceWith(
        Set<String> biomes,
        Map<String, Set<StructureInstanceKey>> structures,
        Set<String> dimensions
    );
}
