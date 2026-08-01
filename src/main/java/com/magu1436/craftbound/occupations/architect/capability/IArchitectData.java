package com.magu1436.craftbound.occupations.architect.capability;

import net.minecraft.resources.ResourceLocation;

import com.magu1436.craftbound.common.capability.PlayerCapabilityData;

/**
 * 建築家固有のプレイヤーデータ。
 */
public interface IArchitectData extends PlayerCapabilityData<IArchitectData> {

    int recordMaterialUse(
        ResourceLocation materialId,
        long currentMinute,
        int windowMinutes
    );

    long addPointUnits(long pointUnits, long pointUnitsPerExperience);

    long getPointUnitRemainder();
}
