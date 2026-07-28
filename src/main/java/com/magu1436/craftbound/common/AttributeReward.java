package com.magu1436.craftbound.common;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public class AttributeReward implements Reward {
    private static final String AMOUNT_KEY = "amount";
    private static final String MODIFIER_ID_KEY = "modifier_uuid";

    private final String rewardId;
    private final UUID modifierId;
    private final Supplier<? extends Attribute> attribute;
    private final double amount;
    private final Operation operation;

    protected AttributeReward(
        String rewardId,
        Supplier<? extends Attribute> attribute,
        UUID modifierId,
        double amount,
        Operation operation
    ) {
        this.rewardId = rewardId;
        this.attribute = attribute;
        this.amount = amount;
        this.modifierId = modifierId;
        this.operation = operation;
    }

    public static void register(
        String rewardId,
        Supplier<? extends Attribute> attribute,
        Operation operation
    ) {
        register(
            rewardId,
            attribute,
            operation,
            (modifierId, amount) -> new AttributeReward(
                rewardId,
                attribute,
                modifierId,
                amount,
                operation
            )
        );
    }

    protected static void register(
        String rewardId,
        Supplier<? extends Attribute> attribute,
        Operation operation,
        RewardCreator creator
    ) {
        if (rewardId == null) throw new NullPointerException("reward id is null");
        SkillsAPI.registerReward(
            CraftboundUtilities.createResourceLocation(rewardId),
            context -> {
                ParsedJsonValues values = AttributeReward
                    .parseJson(context)
                    .getSuccessOrElse(null);
                if (values == null) return Result.failure(Problem.message("parse json failed"));
                return Result.success(creator.create(values.modifierId, values.amount));
            }
        );
    }

    /**
     * {@code RewardConfigContext} から報酬量 {@code amount} を取得するためのユーティリティメソッド
     * @param context 報酬の設定情報
     * @return 報酬量を含む {@code Result}
     */
    protected static Result<ParsedJsonValues, Problem> parseJson( RewardConfigContext context ) {
        JsonElement result = context.getData()
            .getSuccess()
            .orElse(null);
        if (result == null) {
            return Result.failure(Problem.message("Some error happened on parsing json. CODE: 0"));
        }

        var json = result
            .getAsObject()
            .getSuccess()
            .orElse(null);
        
        if (json == null) {
            return Result.failure(Problem.message("Some error happened on parsing json. CODE: 1"));
        }

        Double amount = json
            .getDouble(AMOUNT_KEY)
            .getSuccessOrElse(null);
        UUID modifierId = UUID.fromString(json
            .getString(MODIFIER_ID_KEY)
            .getSuccessOrElse(null)
        );

        if (amount == null) {
            return Result.failure(Problem.message("Some error happened on parsing json. CODE: 2"));
        }

        return Result.success(new ParsedJsonValues(amount, modifierId) );
    }

    @Override
    public void update(RewardUpdateContext context) {
        int rewardCount = context.getCount();   // 同一報酬が取得された回数

        Attribute attribute = Objects.requireNonNull(
            this.attribute.get(),
            "Attribute is null"
        );
        UUID modifierId = Objects.requireNonNull(
            this.modifierId,
            "UUID is null"
        );
        String rewardId = Objects.requireNonNull(
            this.rewardId,
            "rewardId is null"
        );

        AttributeInstance attr = Objects.requireNonNull(
            context.getPlayer().getAttribute(attribute),
            "AttributeInstance is null"
        );

        attr.removeModifier(modifierId);  // 重複を防ぐために削除

        if (rewardCount <= 0) return;

        AttributeModifier modifier = new AttributeModifier(
            modifierId,
            rewardId,
            this.amount * rewardCount,
            operation.toAttributeOperation()
        );

        attr.addTransientModifier(modifier);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            Attribute attribute = Objects.requireNonNull(
                this.attribute.get(),
                "Attribute is null"
            );
            UUID modifierId = Objects.requireNonNull(
                this.modifierId,
                "UUID is null"
            );
            Objects
                .requireNonNull(
                    player.getAttribute(attribute),
                    "AttributeInstance is null"
                )
                .removeModifier(modifierId);
        }
    }

    public enum Operation {
        
        ADDITION,
        MULTIPLY_BASE,
        MULTIPLY_TOTAL;

        @Nonnull
        private AttributeModifier.Operation toAttributeOperation() {
            switch (this) {
                case ADDITION:
                    return AttributeModifier.Operation.ADDITION;
                case MULTIPLY_BASE:
                    return AttributeModifier.Operation.MULTIPLY_BASE;
                case MULTIPLY_TOTAL:
                    return AttributeModifier.Operation.MULTIPLY_TOTAL;
            }
            throw new NullPointerException("Operation is null");
        }

    }

    protected record ParsedJsonValues(
        double amount,
        UUID modifierId
    ) {}

    @FunctionalInterface
    protected interface RewardCreator {
        AttributeReward create(UUID modifierId, double amount);
    }

}
