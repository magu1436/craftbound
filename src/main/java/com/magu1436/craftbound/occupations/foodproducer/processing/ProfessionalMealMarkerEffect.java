package com.magu1436.craftbound.occupations.foodproducer.processing;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 職業料理を同時に1種類へ制限する、表示しない内部マーカー。 */
public final class ProfessionalMealMarkerEffect extends MobEffect {

    public ProfessionalMealMarkerEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC98A55);
    }
}
