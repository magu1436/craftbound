package com.magu1436.craftbound.common;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.util.NonNullSupplier;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import org.slf4j.Logger;

public class AttributeReward implements Reward {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String AMOUNT_KEY = "amount";
    private static final String MODIFIER_ID_KEY = "modifier_uuid";

    private final String rewardId;
    private final UUID modifierId;
    private final NonNullSupplier<? extends Attribute> attribute;
    private final double amount;
    private final Operation operation;

    protected AttributeReward(
        String rewardId,
        NonNullSupplier<? extends Attribute> attribute,
        UUID modifierId,
        double amount,
        Operation operation
    ) {
        this.rewardId = Objects.requireNonNull(
            rewardId,
            "reward id is null"
        );
        this.attribute = Objects.requireNonNull(
            attribute,
            "attribute supplier is null"
        );
        this.amount = amount;
        this.modifierId = Objects.requireNonNull(
            modifierId,
            "modifier id is null"
        );
        this.operation = Objects.requireNonNull(
            operation,
            "operation is null"
        );
    }

    public static void register(
        String rewardId,
        NonNullSupplier<? extends Attribute> attribute,
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
        NonNullSupplier<? extends Attribute> attribute,
        Operation operation,
        RewardCreator creator
    ) {
        Objects.requireNonNull(rewardId, "reward id is null");
        Objects.requireNonNull(attribute, "attribute supplier is null");
        Objects.requireNonNull(operation, "operation is null");
        Objects.requireNonNull(creator, "reward creator is null");

        SkillsAPI.registerReward(
            CraftboundUtilities.createResourceLocation(rewardId),
            context -> parseJson(context).andThen(
                values -> Result.success(
                    creator.create(values.modifierId, values.amount)
                )
            )
        );
    }

    /**
     * {@code RewardConfigContext} から報酬量 {@code amount} を取得するためのユーティリティメソッド
     * @param context 報酬の設定情報
     * @return 報酬量を含む {@code Result}
     */
    protected static Result<ParsedJsonValues, Problem> parseJson(
        RewardConfigContext context
    ) {
        return context.getData().andThen(
            data -> data.getAsObject().andThen(
                json -> json.getDouble(AMOUNT_KEY).andThen(
                    amount -> json.getString(MODIFIER_ID_KEY).andThen(
                        modifierId -> parseModifierId(amount, modifierId)
                    )
                )
            )
        );
    }

    private static Result<ParsedJsonValues, Problem> parseModifierId(
        double amount,
        String modifierId
    ) {
        try {
            return Result.success(
                new ParsedJsonValues(amount, UUID.fromString(modifierId))
            );
        } catch (IllegalArgumentException exception) {
            return Result.failure(
                Problem.message(
                    "modifier_uuid must be a valid UUID: " + modifierId
                )
            );
        }
    }

    @Override
    public void update(RewardUpdateContext context) {
        int rewardCount = context.getCount();
        getAttributeInstance(context.getPlayer()).ifPresent(
            attributeInstance -> {
                attributeInstance.removeModifier(modifierId);

                if (rewardCount <= 0) {
                    return;
                }

                attributeInstance.addTransientModifier(
                    new AttributeModifier(
                        modifierId,
                        rewardId,
                        amount * rewardCount,
                        operation.toAttributeOperation()
                    )
                );
            }
        );
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            getAttributeInstance(player).ifPresent(
                attributeInstance -> attributeInstance.removeModifier(
                    modifierId
                )
            );
        }
    }

    private Optional<AttributeInstance> getAttributeInstance(
        ServerPlayer player
    ) {
        Attribute resolvedAttribute = attribute.get();
        Optional<AttributeInstance> attributeInstance = Optional.ofNullable(
            player.getAttribute(resolvedAttribute)
        );

        if (attributeInstance.isEmpty()) {
            LOGGER.error(
                "Cannot apply reward '{}' to player '{}': attribute '{}' is unavailable",
                rewardId,
                player.getScoreboardName(),
                resolvedAttribute.getDescriptionId()
            );
        }

        return attributeInstance;
    }

    public enum Operation {
        ADDITION,
        MULTIPLY_BASE,
        MULTIPLY_TOTAL;

        private AttributeModifier.Operation toAttributeOperation() {
            return switch (this) {
                case ADDITION -> AttributeModifier.Operation.ADDITION;
                case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
                case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            };
        }
    }

    protected record ParsedJsonValues(
        double amount,
        UUID modifierId
    ) {
        protected ParsedJsonValues {
            Objects.requireNonNull(modifierId, "modifier id is null");
        }
    }

    @FunctionalInterface
    protected interface RewardCreator {
        AttributeReward create(UUID modifierId, double amount);
    }

}
