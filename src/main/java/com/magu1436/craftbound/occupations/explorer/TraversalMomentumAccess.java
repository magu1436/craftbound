package com.magu1436.craftbound.occupations.explorer;

/**
 * Stores traversal-specific horizontal momentum retention on an entity.
 */
public interface TraversalMomentumAccess {

    void craftbound$setHorizontalMomentumRetention(double retention);

    double craftbound$consumeHorizontalMomentumRetention();

    void craftbound$clearHorizontalMomentumRetention();
}
