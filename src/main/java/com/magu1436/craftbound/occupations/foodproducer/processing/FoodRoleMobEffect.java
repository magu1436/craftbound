package com.magu1436.craftbound.occupations.foodproducer.processing;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 料理JSONの割合値をAmplifierへ符号化し、登録済みAttributeへ適用する
 * 非表示の職業料理効果。値は完成料理へ保存されるため、後のJSON変更で
 * 既存料理の性能が変化しない。
 */
public final class FoodRoleMobEffect extends MobEffect {

    /** MobEffectInstanceがAmplifierを符号付きbyteで保存するため127以下に収める。 */
    private static final double AMOUNT_SCALE = 200.0D;
    private static final int MAX_ENCODED_AMOUNT = Byte.MAX_VALUE;
    private static final double MAX_AMOUNT = MAX_ENCODED_AMOUNT / AMOUNT_SCALE;

    private final AmountDisplay amountDisplay;
    private final double amountMultiplier;

    public FoodRoleMobEffect(
            int color,
            AmountDisplay amountDisplay,
            double amountMultiplier
    ) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.amountDisplay = amountDisplay;
        this.amountMultiplier = Math.max(1.0D, amountMultiplier);
    }

    public static int encodeAmount(double amount) {
        double clamped = Math.max(0.0D, Math.min(MAX_AMOUNT, amount));
        return (int) Math.round(clamped * AMOUNT_SCALE);
    }

    public static double decodeAmount(int amplifier) {
        return Math.max(0, Math.min(MAX_ENCODED_AMOUNT, amplifier)) / AMOUNT_SCALE;
    }

    public static double maximumAmount() {
        return MAX_AMOUNT;
    }

    public int encodeEffectiveAmount(double amount) {
        return encodeAmount(amount / amountMultiplier);
    }

    public double decodeEffectiveAmount(int amplifier) {
        return decodeAmount(amplifier) * amountMultiplier;
    }

    public double maximumEffectiveAmount() {
        return maximumAmount() * amountMultiplier;
    }

    public AmountDisplay amountDisplay() {
        return amountDisplay;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return decodeEffectiveAmount(amplifier);
    }

    public enum AmountDisplay {
        PERCENT,
        FLAT,
        BLOCKS
    }
}
