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
    private static final String ATTACK_SPEED_BONUS_NAME = "attack_speed_bonus";

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
                ATTACK_SPEED_BONUS.get()
        );
    }

    @Nonnull
    private static String createTranslateName(String attributeName) {
        return "attribute.name.craftbound." + attributeName;
    }
}
