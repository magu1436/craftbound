package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodRoleMobEffect;
import com.magu1436.craftbound.occupations.foodproducer.processing.ProfessionalMealMarkerEffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CraftboundMobEffects {

    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Craftbound.MODID);

    public static final RegistryObject<MobEffect> PROFESSIONAL_MEAL_ACTIVE =
            MOB_EFFECTS.register("professional_meal_active", ProfessionalMealMarkerEffect::new);

    public static final RegistryObject<FoodRoleMobEffect> ADVENTURER_MEAL_ARMOR =
            registerRoleEffect("adventurer_meal_armor", 0xB53A2D, FoodRoleMobEffect.AmountDisplay.FLAT, 2.0D);
    public static final RegistryObject<FoodRoleMobEffect> ADVENTURER_MEAL_PROJECTILE_REDUCTION =
            registerRoleEffect("adventurer_meal_projectile_reduction", 0xB53A2D, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> ADVENTURER_MEAL_EXPLOSION_REDUCTION =
            registerRoleEffect("adventurer_meal_explosion_reduction", 0xB53A2D, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> ADVENTURER_MEAL_ACTION_RESISTANCE =
            registerRoleEffect("adventurer_meal_action_resistance", 0xB53A2D, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> ADVENTURER_MEAL_BURNING_RESISTANCE =
            registerRoleEffect("adventurer_meal_burning_resistance", 0xB53A2D, FoodRoleMobEffect.AmountDisplay.PERCENT);

    public static final RegistryObject<FoodRoleMobEffect> EXPLORER_MEAL_ENDURANCE =
            registerRoleEffect("explorer_meal_endurance", 0x4C9A5F, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> EXPLORER_MEAL_TOOL_CARE =
            registerRoleEffect("explorer_meal_tool_care", 0x4C9A5F, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> EXPLORER_MEAL_SURE_FOOTED =
            registerRoleEffect("explorer_meal_sure_footed", 0x4C9A5F, FoodRoleMobEffect.AmountDisplay.BLOCKS);
    public static final RegistryObject<FoodRoleMobEffect> EXPLORER_MEAL_CLIMBING =
            registerRoleEffect("explorer_meal_climbing", 0x4C9A5F, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> EXPLORER_MEAL_DIVING =
            registerRoleEffect("explorer_meal_diving", 0x4C9A5F, FoodRoleMobEffect.AmountDisplay.PERCENT);

    public static final RegistryObject<FoodRoleMobEffect> ARCHITECT_MEAL_DEMOLITION =
            registerRoleEffect("architect_meal_demolition", 0xD19A45, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> ARCHITECT_MEAL_FALL_REDUCTION =
            registerRoleEffect("architect_meal_fall_reduction", 0xD19A45, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> ARCHITECT_MEAL_PLACEMENT_REACH =
            registerRoleEffect("architect_meal_placement_reach", 0xD19A45, FoodRoleMobEffect.AmountDisplay.BLOCKS);
    public static final RegistryObject<FoodRoleMobEffect> ARCHITECT_MEAL_SCAFFOLDING =
            registerRoleEffect("architect_meal_scaffolding", 0xD19A45, FoodRoleMobEffect.AmountDisplay.PERCENT);
    public static final RegistryObject<FoodRoleMobEffect> ARCHITECT_MEAL_FIREWORK_CONSERVATION =
            registerRoleEffect("architect_meal_firework_conservation", 0xD19A45, FoodRoleMobEffect.AmountDisplay.PERCENT);

    private static boolean attributesConfigured;

    private CraftboundMobEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    /** 全レジストリ確定後に一度だけ、職業料理効果とAttributeを接続する。 */
    public static synchronized void configureAttributeModifiers() {
        if (attributesConfigured) {
            return;
        }
        add(ADVENTURER_MEAL_ARMOR, Attributes.ARMOR,
                "bf9b3cb8-10b6-4a86-b8f2-ae0defa63da1");
        add(ADVENTURER_MEAL_PROJECTILE_REDUCTION, CraftboundAttributes.PROJECTILE_DAMAGE_REDUCTION.get(),
                "541daf7a-ac89-4b70-8fd8-18d8f374058a");
        add(ADVENTURER_MEAL_EXPLOSION_REDUCTION, CraftboundAttributes.EXPLOSION_DAMAGE_REDUCTION.get(),
                "77f9813a-0a85-47d2-bac4-677b8e1e1a31");
        add(ADVENTURER_MEAL_ACTION_RESISTANCE, CraftboundAttributes.ACTION_RESISTANCE.get(),
                "39b48a7f-714a-435d-b4c3-209203b46b76");
        add(ADVENTURER_MEAL_BURNING_RESISTANCE, CraftboundAttributes.BURNING_RESISTANCE.get(),
                "4e7f1f4d-5554-4f06-a0d7-b91da6705608");

        add(EXPLORER_MEAL_ENDURANCE, CraftboundAttributes.EXPEDITION_ENDURANCE.get(),
                "7b06b63b-cb9f-462b-b372-0230b283346e");
        add(EXPLORER_MEAL_TOOL_CARE, CraftboundAttributes.TOOL_CARE.get(),
                "bafccf93-b2fb-4b79-803d-351c11b2d94a");
        add(EXPLORER_MEAL_SURE_FOOTED, ForgeMod.STEP_HEIGHT_ADDITION.get(),
                "365d049d-3bea-4af6-b927-8fe2b246798f");
        add(EXPLORER_MEAL_CLIMBING, CraftboundAttributes.CLIMBING.get(),
                "c3d106cf-df8a-40cb-ad72-7330d7edb28e");
        add(EXPLORER_MEAL_DIVING, CraftboundAttributes.DIVING.get(),
                "cfa282ed-16d0-4ad1-bac9-a969f959d243");

        add(ARCHITECT_MEAL_DEMOLITION, CraftboundAttributes.DEMOLITION_SPEED.get(),
                "a7d561f5-b207-460c-9437-406218d711d4");
        add(ARCHITECT_MEAL_FALL_REDUCTION, CraftboundAttributes.FALL_DAMAGE_REDUCTION.get(),
                "7008ca7f-d17f-4bba-9265-36605d2499e1");
        add(ARCHITECT_MEAL_PLACEMENT_REACH, ForgeMod.BLOCK_REACH.get(),
                "41d2ab60-66f2-4b18-9f44-96694654ba6d");
        add(ARCHITECT_MEAL_SCAFFOLDING, CraftboundAttributes.SCAFFOLDING_MOBILITY.get(),
                "2551912c-14d3-45af-84f3-ab370c398d89");
        add(ARCHITECT_MEAL_FIREWORK_CONSERVATION, CraftboundAttributes.FIREWORK_CONSERVATION_CHANCE.get(),
                "030b516f-fb16-44a2-a96c-f1800c3d7bf2");
        attributesConfigured = true;
    }

    private static void add(
            RegistryObject<FoodRoleMobEffect> effect,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            String modifierId
    ) {
        effect.get().addAttributeModifier(
                attribute,
                modifierId,
                0.0D,
                AttributeModifier.Operation.ADDITION
        );
    }

    private static RegistryObject<FoodRoleMobEffect> registerRoleEffect(
            String id,
            int color,
            FoodRoleMobEffect.AmountDisplay amountDisplay
    ) {
        return registerRoleEffect(id, color, amountDisplay, 1.0D);
    }

    private static RegistryObject<FoodRoleMobEffect> registerRoleEffect(
            String id,
            int color,
            FoodRoleMobEffect.AmountDisplay amountDisplay,
            double amountMultiplier
    ) {
        return MOB_EFFECTS.register(
                id,
                () -> new FoodRoleMobEffect(color, amountDisplay, amountMultiplier)
        );
    }
}
