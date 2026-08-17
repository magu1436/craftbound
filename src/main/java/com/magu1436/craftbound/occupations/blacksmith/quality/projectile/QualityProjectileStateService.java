package com.magu1436.craftbound.occupations.blacksmith.quality.projectile;

import com.magu1436.craftbound.common.quality.QualityState;
import java.util.OptionalInt;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.projectile.Projectile;

public final class QualityProjectileStateService {
    private static final String QUALITY_KEY = "CraftboundRangedQuality";

    private QualityProjectileStateService() {}

    public static boolean setQuality(Projectile projectile, int quality) {
        if (!QualityState.isValidQuality(quality)) return false;
        projectile.getPersistentData().putInt(QUALITY_KEY, quality);
        return true;
    }

    public static OptionalInt readQuality(Projectile projectile) {
        if (!projectile.getPersistentData().contains(QUALITY_KEY, Tag.TAG_INT)) {
            return OptionalInt.empty();
        }
        int quality = projectile.getPersistentData().getInt(QUALITY_KEY);
        return QualityState.isValidQuality(quality) ? OptionalInt.of(quality) : OptionalInt.empty();
    }
}
