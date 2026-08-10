package com.magu1436.craftbound.occupations.blacksmith.material;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class MetalVisualDataService {
    private MetalVisualDataService() {}

    public static Optional<MetalVisualData> resolve(ResourceLocation metalId) {
        return MetalMaterialDefinitions.INSTANCE.get(metalId).map(definition ->
            new MetalVisualData(
                definition.displayColor(),
                MetalMaterialDefinitions.INSTANCE.getRepresentativeItemId(metalId).orElse(null)
            )
        );
    }
}
