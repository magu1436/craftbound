package com.magu1436.craftbound.occupations.blacksmith.state;

public record HeatingStatus(
    HeatingPhase phase,
    boolean warningRequired
) {}
