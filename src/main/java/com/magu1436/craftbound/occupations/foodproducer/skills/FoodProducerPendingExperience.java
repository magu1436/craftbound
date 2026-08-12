package com.magu1436.craftbound.occupations.foodproducer.skills;

import java.util.UUID;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 手動加工の開始者がオフラインだった場合の経験値を保留・再付与する。 */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodProducerPendingExperience {

    private FoodProducerPendingExperience() {
    }

    public static int get(ServerPlayer player) {
        return get(player.server, player.getUUID());
    }

    public static int get(MinecraftServer server, UUID playerId) {
        return FoodProducerPendingExperienceData.get(server).get(playerId);
    }

    /** 保留後の合計値を返す。0以下の経験値は無視する。 */
    public static int queue(MinecraftServer server, UUID playerId, int amount) {
        return FoodProducerPendingExperienceData.get(server).add(playerId, amount);
    }

    /**
     * 保留経験値を一度だけ付与する。
     *
     * @return 付与量。0は保留なし、-1はSkills APIへ付与できず保留を維持した状態。
     */
    public static int deliver(ServerPlayer player) {
        FoodProducerPendingExperienceData data = FoodProducerPendingExperienceData.get(player.server);
        int amount = data.get(player.getUUID());
        if (amount <= 0) {
            return 0;
        }
        if (!FoodProducerExperience.add(player, amount)) {
            return -1;
        }
        data.remove(player.getUUID());
        return amount;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int delivered = deliver(player);
        if (delivered > 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.craftbound.food_producer.pending_experience_delivered",
                    delivered
            ));
        }
    }
}
