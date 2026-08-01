package com.magu1436.craftbound.occupations.architect.experience;

import net.minecraft.core.BlockPos;

record ScheduledConstruction(
    BlockPos pos,
    long matureAtGameTime
) {
}
