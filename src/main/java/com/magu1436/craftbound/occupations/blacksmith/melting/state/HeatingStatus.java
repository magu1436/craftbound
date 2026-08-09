package com.magu1436.craftbound.occupations.blacksmith.melting.state;

public record HeatingStatus(
    HeatingPhase phase,
    boolean warningRequired
) {}
