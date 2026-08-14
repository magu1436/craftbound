package com.magu1436.craftbound.occupations.blacksmith.material;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class MetalVisualDataService {
    private MetalVisualDataService() {}

    public static Optional<MetalVisualData> resolve(ResourceLocation metalId) {
        return MetalMaterialDefinitions.INSTANCE.get(metalId).flatMap(definition -> {
            Integer color = definition.displayColor();
            ResourceLocation representativeItemId = definition.representativeItemId();
            if (color == null && representativeItemId == null) {
                return Optional.empty();
            }
            return Optional.of(new MetalVisualData(color, representativeItemId));
        });
    }
}
