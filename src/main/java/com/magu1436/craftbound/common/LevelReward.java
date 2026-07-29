package com.magu1436.craftbound.common;

import java.util.Objects;
import java.util.function.Function;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
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
    private final Function<C, SkillLevelState> stateAccessor;
    private final int stage;

    private LevelReward(
        Capability<C> capability,
        Function<C, SkillLevelState> stateAccessor,
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
        Function<C, SkillLevelState> stateAccessor
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
        JsonElement data = context.getData().getSuccessOrElse(null);

        if (data == null) {
            return Result.failure(
                Problem.message("reward data is missing")
            );
        }

        var json = data.getAsObject().getSuccessOrElse(null);

        if (json == null) {
            return Result.failure(
                Problem.message("reward data must be an object")
            );
        }

        Integer stage = json.getInt(STAGE_KEY).getSuccessOrElse(null);

        if (stage == null) {
            return Result.failure(
                Problem.message("reward stage is missing")
            );
        }
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
            SkillLevelState state = Objects.requireNonNull(
                stateAccessor.apply(data),
                "skill level state is null"
            );

            state.setStageActive(stage, active);
        });
    }
}
