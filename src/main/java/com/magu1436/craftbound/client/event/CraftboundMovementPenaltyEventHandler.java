package com.magu1436.craftbound.client.event;

import java.util.Objects;

import com.magu1436.craftbound.occupations.adventurer.client.AdventurerMovementPenaltyService;

import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CraftboundMovementPenaltyEventHandler {

    private final AdventurerMovementPenaltyService adventurerService;

    public CraftboundMovementPenaltyEventHandler(
        AdventurerMovementPenaltyService adventurerService
    ) {
        this.adventurerService = Objects.requireNonNull(
            adventurerService,
            "adventurerService must not be null"
        );
    }

    @SubscribeEvent
    public void onMovementInputUpdate(
        MovementInputUpdateEvent event
    ) {
        adventurerService.reduceMovementPenalty(event);
    }
}
