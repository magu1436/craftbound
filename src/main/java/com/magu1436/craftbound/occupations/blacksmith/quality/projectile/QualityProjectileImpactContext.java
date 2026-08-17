package com.magu1436.craftbound.occupations.blacksmith.quality.projectile;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import net.minecraft.world.entity.projectile.Projectile;

public final class QualityProjectileImpactContext {
    private static final ThreadLocal<Deque<Projectile>> CURRENT =
        ThreadLocal.withInitial(ArrayDeque::new);

    private QualityProjectileImpactContext() {}

    public static void push(Projectile projectile) {
        CURRENT.get().push(projectile);
    }

    public static Optional<Projectile> current() {
        return Optional.ofNullable(CURRENT.get().peek());
    }

    public static void pop(Projectile projectile) {
        Deque<Projectile> projectiles = CURRENT.get();
        if (projectiles.peek() == projectile) {
            projectiles.pop();
        } else {
            projectiles.removeFirstOccurrence(projectile);
        }
        if (projectiles.isEmpty()) CURRENT.remove();
    }
}
