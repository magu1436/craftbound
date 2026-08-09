package com.magu1436.craftbound.occupations.blacksmith.crucible;

import java.util.Optional;

import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialResolver;
import com.magu1436.craftbound.occupations.blacksmith.material.ResolvedMetalMaterial;

import net.minecraft.world.item.ItemStack;

public final class CrucibleStateService {
    public static final int MAX_CAPACITY = CrucibleState.MAX_CAPACITY;

    private CrucibleStateService() {
    }

    public static Optional<CrucibleState> read(ItemStack crucible) {
        if (!isCrucible(crucible)) {
            return Optional.empty();
        }
        return CrucibleStateCodec.read(crucible);
    }

    public static CrucibleInsertResult insert(
        ItemStack crucible,
        ItemStack input,
        int requestedItemCount,
        MetalMaterialResolver resolver
    ) {
        if (requestedItemCount < 1 || input.isEmpty() || resolver == null) {
            return failure(
                CrucibleInsertFailure.INVALID_REQUEST,
                readOrEmpty(crucible)
            );
        }
        if (!isCrucible(crucible)) {
            return failure(CrucibleInsertFailure.NOT_CRUCIBLE, CrucibleState.empty());
        }

        Optional<CrucibleState> currentResult = CrucibleStateCodec.read(crucible);
        if (currentResult.isEmpty()) {
            return failure(
                CrucibleInsertFailure.INVALID_CRUCIBLE_STATE,
                CrucibleState.empty()
            );
        }
        CrucibleState current = currentResult.get();

        Optional<ResolvedMetalMaterial> materialResult = resolver.resolve(input);
        if (materialResult.isEmpty()) {
            return failure(CrucibleInsertFailure.UNSUPPORTED_MATERIAL, current);
        }
        ResolvedMetalMaterial material = materialResult.get();
        CrucibleInsertResult result = calculateInsertion(
            current,
            material,
            requestedItemCount,
            input.getCount()
        );
        if (result.succeeded()) {
            CrucibleStateCodec.write(crucible, result.state());
        }
        return result;
    }

    static CrucibleInsertResult calculateInsertion(
        CrucibleState current,
        ResolvedMetalMaterial material,
        int requestedItemCount,
        int availableItemCount
    ) {
        if (requestedItemCount < 1 || availableItemCount < 1) {
            return failure(CrucibleInsertFailure.INVALID_REQUEST, current);
        }
        if (current.metalId() != null
            && !current.metalId().equals(material.metalId())) {
            return failure(CrucibleInsertFailure.DIFFERENT_METAL, current);
        }

        int requestedAvailable = Math.min(
            requestedItemCount,
            availableItemCount
        );
        int remainingCapacity = MAX_CAPACITY - current.amount();
        int acceptedItemCount = Math.min(
            requestedAvailable,
            remainingCapacity / material.amountPerItem()
        );
        if (acceptedItemCount < 1) {
            return failure(CrucibleInsertFailure.NO_CAPACITY, current);
        }

        int addedAmount = acceptedItemCount * material.amountPerItem();
        CrucibleState updated;
        if (current.processState() == CrucibleProcessState.EMPTY) {
            updated = new CrucibleState(
                CrucibleState.CURRENT_VERSION,
                material.metalId(),
                addedAmount,
                0L,
                CrucibleProcessState.UNHEATED
            );
        } else {
            updated = new CrucibleState(
                current.version(),
                current.metalId(),
                current.amount() + addedAmount,
                current.heatingTicks(),
                current.processState()
            );
        }
        return CrucibleInsertResult.success(
            acceptedItemCount,
            addedAmount,
            updated
        );
    }

    public static boolean advanceHeating(ItemStack crucible, long ticks) {
        if (ticks <= 0L) {
            return false;
        }
        Optional<CrucibleState> result = read(crucible);
        if (result.isEmpty()) {
            return false;
        }
        CrucibleState current = result.get();
        Optional<CrucibleState> updated = advanceState(current, ticks);
        if (updated.isEmpty()) {
            return false;
        }

        CrucibleStateCodec.write(crucible, updated.get());
        return true;
    }

    static Optional<CrucibleState> advanceState(
        CrucibleState current,
        long ticks
    ) {
        if (ticks <= 0L
            || current.processState() == CrucibleProcessState.EMPTY) {
            return Optional.empty();
        }

        long updatedTicks;
        try {
            updatedTicks = Math.addExact(current.heatingTicks(), ticks);
        } catch (ArithmeticException exception) {
            updatedTicks = Long.MAX_VALUE;
        }
        return Optional.of(new CrucibleState(
            current.version(),
            current.metalId(),
            current.amount(),
            updatedTicks,
            CrucibleProcessState.HEATED
        ));
    }

    public static boolean consumeForCasting(ItemStack crucible, int amount) {
        if (amount <= 0) {
            return false;
        }
        Optional<CrucibleState> result = read(crucible);
        if (result.isEmpty() || result.get().amount() < amount) {
            return false;
        }

        Optional<CrucibleState> updated = consumeState(result.get(), amount);
        if (updated.isEmpty()) {
            return false;
        }
        if (updated.get().processState() == CrucibleProcessState.EMPTY) {
            CrucibleStateCodec.remove(crucible);
        } else {
            CrucibleStateCodec.write(crucible, updated.get());
        }
        return true;
    }

    static Optional<CrucibleState> consumeState(
        CrucibleState current,
        int amount
    ) {
        if (amount <= 0 || current.amount() < amount) {
            return Optional.empty();
        }
        int remaining = current.amount() - amount;
        if (remaining == 0) {
            return Optional.of(CrucibleState.empty());
        }
        return Optional.of(new CrucibleState(
            current.version(),
            current.metalId(),
            remaining,
            current.heatingTicks(),
            current.processState()
        ));
    }

    public static boolean discardAll(ItemStack crucible) {
        Optional<CrucibleState> result = read(crucible);
        if (result.isEmpty()) {
            return false;
        }
        if (result.get().processState() == CrucibleProcessState.EMPTY) {
            return true;
        }
        CrucibleStateCodec.remove(crucible);
        return true;
    }

    private static boolean isCrucible(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CrucibleItem;
    }

    private static CrucibleState readOrEmpty(ItemStack stack) {
        return read(stack).orElse(CrucibleState.empty());
    }

    private static CrucibleInsertResult failure(
        CrucibleInsertFailure reason,
        CrucibleState state
    ) {
        return CrucibleInsertResult.failure(reason, state);
    }
}
