package com.magu1436.craftbound.occupations.blacksmith.crucible;

import java.util.Optional;

import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialResolver;
import com.magu1436.craftbound.occupations.blacksmith.material.ResolvedMetalMaterial;

import net.minecraft.world.item.ItemStack;

public final class CrucibleStateService {
    public static final int MAX_CAPACITY = CrucibleState.MAX_CAPACITY;

    private CrucibleStateService() {
    }

    /**
     * Reads a validated snapshot of the crucible contents.
     *
     * @param crucible target item stack
     * @return the state, or empty when the stack is not a crucible or its
     *     stored data is invalid
     */
    public static Optional<CrucibleState> read(ItemStack crucible) {
        if (!isCrucible(crucible)) {
            return Optional.empty();
        }
        return CrucibleStateCodec.read(crucible);
    }

    /**
     * Inserts whole input items into a crucible.
     * State-changing callers must invoke this on the logical server.
     * The input stack itself is not shrunk by this method; callers consume
     * exactly {@link CrucibleInsertResult#acceptedItemCount()} items after a
     * successful result.
     *
     * @param crucible target crucible
     * @param input candidate input stack
     * @param requestedItemCount maximum number of input items to insert
     * @param resolver server-owned metal material definitions
     * @return insertion result and the resulting crucible state
     */
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

    /**
     * Adds heating time without evaluating or storing heating quality.
     * This API must be called on the logical server by the melting process.
     *
     * @param crucible target crucible
     * @param ticks positive number of heating ticks to add
     * @return {@code true} when the crucible state was updated
     */
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

    /**
     * Atomically consumes material units for casting. No state is changed
     * when the requested amount is invalid or unavailable. This API must be
     * called on the logical server after the casting process has validated
     * the metal and heating requirements.
     *
     * @param crucible target crucible
     * @param amount positive material-unit amount to consume
     * @return {@code true} when the full requested amount was consumed
     */
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
            return resetToEmpty(crucible);
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

    /**
     * Discards all contents through the common empty-state reset path.
     * This API must be called on the logical server after confirmation and
     * menu validity have been checked.
     *
     * @param crucible target crucible
     * @return {@code true} when the crucible was valid, including when it was
     *     already empty
     */
    public static boolean discardAll(ItemStack crucible) {
        Optional<CrucibleState> result = read(crucible);
        if (result.isEmpty()) {
            return false;
        }
        if (result.get().processState() == CrucibleProcessState.EMPTY) {
            return true;
        }
        return resetToEmpty(crucible);
    }

    /**
     * Removes the complete stored contents and normalizes the crucible to
     * {@link CrucibleProcessState#EMPTY}. Invalid data is never overwritten.
     * This API must be called on the logical server.
     *
     * @param crucible target crucible
     * @return {@code true} when the crucible was valid and is now empty
     */
    public static boolean resetToEmpty(ItemStack crucible) {
        Optional<CrucibleState> result = read(crucible);
        if (result.isEmpty()) {
            return false;
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
