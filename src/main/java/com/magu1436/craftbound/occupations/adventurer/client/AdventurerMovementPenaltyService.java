package com.magu1436.craftbound.occupations.adventurer.client;

import java.util.List;
import java.util.Objects;

import com.magu1436.craftbound.client.event.MovementPenaltyReductionService;
import com.magu1436.craftbound.event.MovementPenaltyReductionRule;

import net.minecraftforge.client.event.MovementInputUpdateEvent;

public final class AdventurerMovementPenaltyService {

    private final List<MovementPenaltyReductionRule> rules;

    public AdventurerMovementPenaltyService(
        List<MovementPenaltyReductionRule> rules
    ) {
        this.rules = List.copyOf(
            Objects.requireNonNull(rules, "rules must not be null")
        );
    }

    public void reduceMovementPenalty(
        MovementInputUpdateEvent event
    ) {
        MovementPenaltyReductionService.reduceMovementPenalty(
            event,
            rules
        );
    }
}
