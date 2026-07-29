package com.magu1436.craftbound.common;

import java.util.Objects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

/**
 * スキルの各段階をプレイヤーのCapabilityへ反映する報酬。
 *
 * @param <C> 対象となるCapabilityの公開データ型
 */
public final class LevelReward<C> implements Reward {
    private static final String STAGE_KEY = "stage";

    private final Capability<C> capability;
    private final SkillLevelStateAccessor<C> stateAccessor;
    private final int stage;

    private LevelReward(
        Capability<C> capability,
        SkillLevelStateAccessor<C> stateAccessor,
        int stage
    ) {
        this.capability = Objects.requireNonNull(
            capability,
            "capability is null"
        );
        this.stateAccessor = Objects.requireNonNull(
            stateAccessor,
            "state accessor is null"
        );

        if (stage < 1) {
            throw new IllegalArgumentException(
                "stage must be greater than or equal to 1"
            );
        }

        this.stage = stage;
    }

    /**
     * LevelRewardをPufferfish's Skillsへ登録する。
     *
     * @param rewardId 報酬IDのパス
     * @param capability 対象となるCapability
     * @param stateAccessor Capabilityデータから対象スキルの状態を取得する関数
     * @param <C> Capabilityの公開データ型
     */
    public static <C> void register(
        String rewardId,
        Capability<C> capability,
        SkillLevelStateAccessor<C> stateAccessor
    ) {
        Objects.requireNonNull(rewardId, "reward id is null");
        Objects.requireNonNull(capability, "capability is null");
        Objects.requireNonNull(stateAccessor, "state accessor is null");

        SkillsAPI.registerReward(
            CraftboundUtilities.createResourceLocation(rewardId),
            context -> parseStage(context).andThen(
                stage -> Result.success(
                    new LevelReward<>(
                        capability,
                        stateAccessor,
                        stage
                    )
                )
            )
        );
    }

    private static Result<Integer, Problem> parseStage(
        RewardConfigContext context
    ) {
        return context.getData().andThen(
            data -> data.getAsObject().andThen(
                json -> json.getInt(STAGE_KEY).andThen(
                    LevelReward::validateStage
                )
            )
        );
    }

    private static Result<Integer, Problem> validateStage(int stage) {
        if (stage < 1) {
            return Result.failure(
                Problem.message(
                    "reward stage must be greater than or equal to 1"
                )
            );
        }

        return Result.success(stage);
    }

    @Override
    public void update(RewardUpdateContext context) {
        setStageActive(
            context.getPlayer(),
            context.getCount() > 0
        );
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (
            ServerPlayer player
                : context.getServer().getPlayerList().getPlayers()
        ) {
            setStageActive(player, false);
        }
    }

    private void setStageActive(
        ServerPlayer player,
        boolean active
    ) {
        player.getCapability(capability).ifPresent(data -> {
            SkillLevelState state = stateAccessor.get(data);
            state.setStageActive(stage, active);
        });
    }
}
