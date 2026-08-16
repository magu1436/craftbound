package com.magu1436.craftbound.occupations.foodproducer.ranch;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerExperience;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 牧畜ブロック管理下の成長停止、手動繁殖条件、追加個体、経験値を適用する。 */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RanchManagementEvents {

    private static final String FORCE_EXTRA_CHILD = "craftbound_test_force_extra_child";

    private RanchManagementEvents() {
    }

    private static long directFeedSequence;

    /** 管理経験のある子どもは、牧畜ブロック側が進めた分だけ成長する。 */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AgeableMob animal)
                || animal.level().isClientSide
                || !animal.isBaby()
                || !RanchAnimalData.wasEverManaged(animal)) {
            return;
        }

        int remaining = RanchAnimalData.getRemainingGrowth(animal);
        if (remaining <= 0) {
            animal.setAge(0);
        } else {
            // このイベント後にバニラが1tick進めるため、1を足して相殺する。
            animal.setAge(-remaining - 1);
        }
    }

    /** 対象家畜への餌やりを、同じ牧畜ブロック・給餌済み・容量内の手動繁殖に限定する。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAnimalInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Animal animal)
                || !animal.isFood(event.getItemStack())
                || FoodQualityData.isSpoiled(event.getItemStack())) {
            return;
        }

        RanchBlockEntity ranch = RanchManager.findManagingRanch(animal);
        if (animal.isBaby()) {
            if (RanchAnimalData.wasEverManaged(animal)
                    || ranch != null) {
                reject(event, player, "message.craftbound.ranch.child_feed_disabled");
            }
            return;
        }

        if (animal.getAge() != 0 || !animal.canFallInLove()) {
            return;
        }
        if (ranch == null || !RanchManager.isManagedBy(animal, ranch)) {
            reject(event, player, "message.craftbound.ranch.outside");
            return;
        }

        List<Animal> managed = RanchManager.getManagedAnimals(ranch);
        if (ranch.getRegistrationCount() >= ranch.getManagementCapacity()) {
            reject(event, player, "message.craftbound.ranch.capacity_full");
            return;
        }
        if (!RanchAnimalData.isFed(animal)) {
            reject(event, player, "message.craftbound.ranch.needs_feed");
            return;
        }

        boolean hasPartner = managed.stream().anyMatch(other -> other != animal
                && !other.isBaby()
                && other.getAge() == 0
                && RanchAnimalData.isFed(other));
        if (!hasPartner) {
            reject(event, player, "message.craftbound.ranch.needs_pair");
            return;
        }

        RanchAnimalData.rememberDirectFeeder(
                animal,
                player.getUUID(),
                (level.getGameTime() << 20) | (++directFeedSequence & 0xFFFFFL),
                FoodProducerSkills.rank(player, FoodProducerSkills.BREEDING_MANAGEMENT)
        );
    }

    /** バニラ繁殖の生成直前に再検証し、最後に餌を与えたプレイヤーのランクを使う。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof Animal parentA)
                || !(event.getParentB() instanceof Animal parentB)
                || !(parentA.level() instanceof ServerLevel level)) {
            return;
        }

        RanchBlockEntity ranch = RanchManager.findManagingRanch(parentA);
        List<Animal> managed = ranch == null ? List.of() : RanchManager.getManagedAnimals(ranch);
        ServerPlayer player = lastFeeder(level, parentA, parentB, event);
        if (ranch == null
                || RanchManager.findManagingRanch(parentB) != ranch
                || !managed.contains(parentA)
                || !managed.contains(parentB)
                || !RanchAnimalData.isFed(parentA)
                || !RanchAnimalData.isFed(parentB)
                || ranch.getRegistrationCount() >= ranch.getManagementCapacity()
                || player == null
                || event.getChild() == null) {
            event.setCanceled(true);
            return;
        }

        AgeableMob child = event.getChild();
        RanchAnimalData.initializeNewborn(child);
        if (!(child instanceof Animal childAnimal) || !ranch.registerNewborn(childAnimal)) {
            event.setCanceled(true);
            return;
        }

        Animal lastFedParent = RanchAnimalData.getLastDirectFeedTime(parentA)
                >= RanchAnimalData.getLastDirectFeedTime(parentB) ? parentA : parentB;
        int breedingRank = RanchAnimalData.getLastBreedingRank(lastFedParent);
        boolean forceExtraChild = player.getPersistentData().getBoolean(FORCE_EXTRA_CHILD);
        player.getPersistentData().remove(FORCE_EXTRA_CHILD);
        if (ranch.hasRegistrationSpace()
                && (forceExtraChild || level.random.nextInt(100) < breedingRank * 10)) {
            AgeableMob extraChild = parentA.getBreedOffspring(level, parentB);
            if (extraChild != null) {
                extraChild.setBaby(true);
                extraChild.moveTo(parentA.getX(), parentA.getY(), parentA.getZ(), 0.0F, 0.0F);
                RanchAnimalData.initializeNewborn(extraChild);
                if (extraChild instanceof Animal extraAnimal && ranch.registerNewborn(extraAnimal)) {
                    level.addFreshEntity(extraChild);
                }
            }
        }

        RanchAnimalData.setFed(parentA, false);
        RanchAnimalData.setFed(parentB, false);
        long nextFeed = level.getGameTime() + RanchAnimalData.ADULT_FEED_INTERVAL_TICKS;
        RanchAnimalData.setNextFeedTime(parentA, nextFeed);
        RanchAnimalData.setNextFeedTime(parentB, nextFeed);
        FoodProducerExperience.add(player, 5);
    }

    /** 死亡した個体の登録枠を即時解放し、幽霊枠を残さない。 */
    @SubscribeEvent
    public static void onAnimalDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Animal animal) || animal.level().isClientSide) {
            return;
        }
        RanchBlockEntity ranch = RanchManager.findAssignedRanch(animal);
        if (ranch != null) {
            ranch.releaseRegistration(animal.getUUID());
        }
    }

    /** OP向けテストで、次の正常な手動繁殖だけ追加個体抽選を成功扱いにする。 */
    public static void forceNextExtraChild(ServerPlayer player) {
        player.getPersistentData().putBoolean(FORCE_EXTRA_CHILD, true);
    }

    @Nullable
    private static ServerPlayer lastFeeder(
            ServerLevel level,
            Animal parentA,
            Animal parentB,
            BabyEntitySpawnEvent event
    ) {
        Animal lastFed = RanchAnimalData.getLastDirectFeedTime(parentA)
                >= RanchAnimalData.getLastDirectFeedTime(parentB) ? parentA : parentB;
        UUID playerId = RanchAnimalData.getLastFeeder(lastFed);
        if (playerId != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                return player;
            }
        }
        return event.getCausedByPlayer() instanceof ServerPlayer player ? player : null;
    }

    private static void reject(
            PlayerInteractEvent.EntityInteract event,
            ServerPlayer player,
            String translationKey
    ) {
        player.displayClientMessage(Component.translatable(translationKey), true);
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }
}
