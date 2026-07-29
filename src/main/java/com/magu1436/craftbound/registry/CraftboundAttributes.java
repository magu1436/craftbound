package com.magu1436.craftbound.registry;

import javax.annotation.Nonnull;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CraftboundAttributes {

    private static final String EXPLOSION_DAMAGE_REDUCTION_NAME = "explosion_damage_reduction";
    private static final String PROJECTILE_DAMAGE_REDUCTION_NAME = "projectile_damage_reduction";
    private static final String ATTACK_SPEED_BONUS_NAME = "attack_speed_bonus";
    private static final String PHYSICAL_RESISTANCE_NAME = "physical_resistance";
    private static final String ACTION_RESISTANCE_NAME = "action_resistance";
    private static final String SENSORY_RESISTANCE_NAME = "sensory_resistance";
    private static final String SHIELD_FOOTWORK_NAME = "shield_footwork";
    private static final String RANGED_FOOTWORK_NAME = "ranged_footwork";
    private static final String FIELD_RESUPPLY_NAME = "field_resupply";

    private static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(
            ForgeRegistries.ATTRIBUTES,
            Craftbound.MODID
        );

    /**
     * 爆発ダメージの軽減
     */
    public static final RegistryObject<Attribute> EXPLOSION_DAMAGE_REDUCTION =
        ATTRIBUTES.register(
            EXPLOSION_DAMAGE_REDUCTION_NAME,
            () -> new RangedAttribute(
                createTranslateName(EXPLOSION_DAMAGE_REDUCTION_NAME),
                0.0D,   // デフォルト値
                0.0D,   // 最小値
                1.0D    // 最大値
            ).setSyncable(true)
        );

    /**
     * 飛び道具ダメージの軽減
     */
    public static final RegistryObject<Attribute> PROJECTILE_DAMAGE_REDUCTION =
        ATTRIBUTES.register(
            PROJECTILE_DAMAGE_REDUCTION_NAME,
            () -> new RangedAttribute(
                createTranslateName(PROJECTILE_DAMAGE_REDUCTION_NAME),
                0.0D,   // デフォルト値
                0.0D,   // 最小値
                1.0D    // 最大値
            ).setSyncable(true)
        );

    /**
     * 攻撃速度の割合補正
     */
    public static final RegistryObject<Attribute> ATTACK_SPEED_BONUS =
        ATTRIBUTES.register(
            ATTACK_SPEED_BONUS_NAME,
            () -> new RangedAttribute(
                createTranslateName(ATTACK_SPEED_BONUS_NAME),
                0.0D,
                0.0D,
                1.0D
            ).setSyncable(true)
        );

    public static final RegistryObject<Attribute> PHYSICAL_RESISTANCE =
        ATTRIBUTES.register(
            PHYSICAL_RESISTANCE_NAME,
            () -> new RangedAttribute(
                createTranslateName(PHYSICAL_RESISTANCE_NAME),
                0.0D,
                0.0D,
                0.4D
            ).setSyncable(true)
        );

    public static final RegistryObject<Attribute> ACTION_RESISTANCE =
        ATTRIBUTES.register(
            ACTION_RESISTANCE_NAME,
            () -> new RangedAttribute(
                createTranslateName(ACTION_RESISTANCE_NAME),
                0.0D,
                0.0D,
                0.4D
            ).setSyncable(true)
        );

    public static final RegistryObject<Attribute> SENSORY_RESISTANCE =
        ATTRIBUTES.register(
            SENSORY_RESISTANCE_NAME,
            () -> new RangedAttribute(
                createTranslateName(SENSORY_RESISTANCE_NAME),
                0.0D,
                0.0D,
                0.4D
            ).setSyncable(true)
        );

    /**
     * 盾歩法の取得段階
     */
    public static final RegistryObject<Attribute> SHIELD_FOOTWORK =
        ATTRIBUTES.register(
            SHIELD_FOOTWORK_NAME,
            () -> new RangedAttribute(
                createTranslateName(SHIELD_FOOTWORK_NAME),
                0.0D,
                0.0D,
                4.0D
            ).setSyncable(true)
        );

    /**
     * 射撃歩法の取得段階
     */
    public static final RegistryObject<Attribute> RANGED_FOOTWORK =
        ATTRIBUTES.register(
            RANGED_FOOTWORK_NAME,
            () -> new RangedAttribute(
                createTranslateName(RANGED_FOOTWORK_NAME),
                0.0D,
                0.0D,
                4.0D
            ).setSyncable(true)
        );

    /**
     * 戦地補給の取得段階
     */
    public static final RegistryObject<Attribute> FIELD_RESUPPLY =
        ATTRIBUTES.register(
            FIELD_RESUPPLY_NAME,
            () -> new RangedAttribute(
                createTranslateName(FIELD_RESUPPLY_NAME),
                0.0D,
                0.0D,
                4.0D
            ).setSyncable(true)
        );

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(CraftboundAttributes::addAttributesToEntities);
    }

    /**
     * 既存プレイヤー EntityType に独自 Attribute を追加
     */
    private static void addAttributesToEntities(
        EntityAttributeModificationEvent event
    ) {
        event.add(
                EntityType.PLAYER,
                EXPLOSION_DAMAGE_REDUCTION.get()
        );
        event.add(
                EntityType.PLAYER,
                PROJECTILE_DAMAGE_REDUCTION.get()
        );
        event.add(
                EntityType.PLAYER,
                ATTACK_SPEED_BONUS.get()
        );
        event.add(
                EntityType.PLAYER,
                PHYSICAL_RESISTANCE.get()
        );
        event.add(
                EntityType.PLAYER,
                ACTION_RESISTANCE.get()
        );
        event.add(
                EntityType.PLAYER,
                SENSORY_RESISTANCE.get()
        );
        event.add(
                EntityType.PLAYER,
                SHIELD_FOOTWORK.get()
        );
        event.add(
                EntityType.PLAYER,
                RANGED_FOOTWORK.get()
        );
        event.add(
                EntityType.PLAYER,
                FIELD_RESUPPLY.get()
        );
    }

    @Nonnull
    private static String createTranslateName(String attributeName) {
        return "attribute.name.craftbound." + attributeName;
    }
}
