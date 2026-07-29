package com.magu1436.craftbound.common.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * プレイヤーへ付与するCapabilityデータの共通契約。
 *
 * @param <T> Capabilityが公開するデータ型
 */
public interface PlayerCapabilityData<
    T extends PlayerCapabilityData<T>
> {

    /**
     * 永続化するデータをNBTへ保存する。
     *
     * @return 保存データ
     */
    CompoundTag savePersistentData();

    /**
     * 永続化されたデータをNBTから読み込む。
     *
     * @param tag 保存データ
     */
    void loadPersistentData(CompoundTag tag);

    /**
     * 死亡後に生成されたプレイヤーへ引き継ぐデータをコピーする。
     *
     * @param original コピー元
     */
    void copyOnDeathFrom(T original);
}
