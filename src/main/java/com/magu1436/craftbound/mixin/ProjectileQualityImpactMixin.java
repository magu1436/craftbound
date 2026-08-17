package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.blacksmith.quality.projectile.QualityProjectileImpactContext;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileQualityImpactMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void craftbound$pushQualityProjectileImpact(HitResult result, CallbackInfo callback) {
        Projectile projectile = (Projectile) (Object) this;
        if (!projectile.level().isClientSide) {
            QualityProjectileImpactContext.push(projectile);
        }
    }

    @Inject(method = "onHit", at = @At("RETURN"))
    private void craftbound$popQualityProjectileImpact(HitResult result, CallbackInfo callback) {
        Projectile projectile = (Projectile) (Object) this;
        if (!projectile.level().isClientSide) {
            QualityProjectileImpactContext.pop(projectile);
        }
    }
}
