package com.magu1436.craftbound.occupations.foodproducer.processing;

/** 職業料理の排他判定をMinecraft状態から分離した規則。 */
public final class ProfessionalMealPolicy {

    private ProfessionalMealPolicy() {
    }

    public static boolean isProfessionalMeal(boolean preserved, boolean hasEffects) {
        return !preserved && hasEffects;
    }

    public static boolean blocksCandidate(
            boolean markerActive,
            String activeRecipeId,
            String candidateRecipeId
    ) {
        if (!markerActive) {
            return false;
        }
        if (activeRecipeId == null || activeRecipeId.isBlank()
                || candidateRecipeId == null || candidateRecipeId.isBlank()) {
            return true;
        }
        return !activeRecipeId.equals(candidateRecipeId);
    }
}
