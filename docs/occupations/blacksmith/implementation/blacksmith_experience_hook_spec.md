# 目的

鍛冶師の経験値処理について、鋳造工程と鍛造工程を独立した工程結果として扱えるようにする。

現状の鍛造実装では、`ForgingExperienceResult` を生成する際に `RoughMetalPartState` が保持する `castingResultId` と `castingOperatorId` を流用している。このため、鍛造結果の識別子と操作者が鋳造工程の情報に依存している。

本実装では以下を行う。

- 鍛造結果IDと鋳造結果IDの流用を廃止する。
- 鍛造経験値結果の操作者を、鍛造結果を確定したプレイヤーとする。
- 既存の `ForgingExperienceHook` と同様の受け取り口を鋳造側にも追加する。
- 鋳造成功・鋳造失敗がサーバー側で確定した時点で、鋳造用経験値結果をHookへ渡せるようにする。

今回の実装では、Pufferfish's Skillsへの実際の経験値付与処理は行わない。工程ごとの正しい結果情報を経験値層へ渡せる状態までを対象とする。

# 確定した仕様

## 工程ごとの経験値結果は独立させる

鋳造と鍛造は別の工程として扱い、それぞれ独立した結果IDを持つ。

- 鋳造結果IDは、鋳造工程の結果を識別するためだけに使用する。
- 鍛造結果IDは、鍛造工程の結果を識別するためだけに使用する。
- 鍛造時に `castingResultId` を鍛造結果IDとして使用しない。
- 鍛造時に `castingOperatorId` を鍛造操作者として使用しない。

`RoughMetalPartState` が保持する `castingResultId` と `castingOperatorId` は、鋳造工程の来歴情報として残す。今回の変更では削除しない。

## 鍛造結果の操作者

鍛造経験値結果の `operatorId` は、鍛造成功または失敗をサーバー側で確定した `ServerPlayer` のUUIDとする。

途中まで別プレイヤーが鍛造していた場合でも、鋳造者や過去の鍛造担当者へは紐付けない。

## 鋳造結果の操作者

鋳造経験値結果の `operatorId` は、有効な流し込み、または素材損失を伴う失敗操作をサーバー側で確定した `ServerPlayer` のUUIDとする。

## 鋳造成功時のHook呼び出し

正常な流し込みについては、以下がすべて成功した直後を鋳造工程の確定時点とする。

1. 投入条件の検証が成功する。
2. るつぼから必要素材量の消費に成功する。
3. `CastingProcess` を生成する。
4. `CastingTableBlockEntity` に `activeProcess` として保存する。

この確定後に `CastingExperienceHook.onResult(...)` を1回呼び出す。

鋳造経験値を、冷却完了時、粗加工パーツ回収時、インベントリ回収時まで遅延させない。

## 鋳造失敗時のHook呼び出し

鋳造操作によって素材の一部または全部が恒久的に失われた場合も、素材消費と失敗結果の反映が完了した直後に `CastingExperienceHook.onResult(...)` を呼び出す。

対象となる既存経路は少なくとも以下とする。

- `handleEarlyPour(...)`
- `consumeWithoutOutput(...)`

なお、`CastingActionResult.SUCCESS` は「右クリック操作の処理に成功した」ことを示しており、鍛冶工程としての成功を意味しない。

そのため、早すぎる流し込みや過熱による素材消失では、`CastingExperienceResult.success` は `false` とする。

## 無効操作

以下では経験値Hookを呼び出さない。

- 入力条件を満たさず `CastingActionResult.PASS` となる操作
- るつぼの素材消費が行われなかった操作
- 鍛造の完成予約または結果commitに失敗した操作
- 鍛造打撃が単に受理されただけの操作
- 中断、再開、出力回収のみの操作

## 経験値結果データ

鋳造用に `CastingExperienceResult` を追加する。

フィールド構成は既存の `ForgingExperienceResult` と揃える。

```java
public record CastingExperienceResult(
    UUID resultId,
    UUID operatorId,
    int materialUnits,
    boolean success,
    boolean permanentMaterialLoss
) {}
```

意味は以下とする。

- `resultId`: その鋳造結果を一意に識別するUUID
- `operatorId`: 結果を確定したプレイヤーUUID
- `materialUnits`: その工程に投入・使用した素材単位数
- `success`: 鋳造工程として正常成功したか
- `permanentMaterialLoss`: 失敗によって素材が恒久的に失われたか

`materialUnits` は返却素材を差し引く前の、当該鋳造操作に使用した素材量とする。

## Hookの責務

`CastingExperienceHook` は既存の `ForgingExperienceHook` と同じ位置付けとする。

```java
public final class CastingExperienceHook {
    private CastingExperienceHook() {}

    public static void onResult(
        ServerPlayer player,
        CastingExperienceResult result
    ) {
        // 現時点では no-op
    }
}
```

今回の段階ではHook内部からPufferfish's Skills APIを呼び出さない。

# 採用した実装方針

## 1. 鍛造結果IDを新規生成する

`ForgingGameService#experienceResult(...)` を修正し、`RoughMetalPartState#castingResultId()` を使用しない。

鍛造成功または失敗結果がcommitされた後、その鍛造結果専用のUUIDを生成して `ForgingExperienceResult` へ設定する。

鍛造経験値結果の操作者には、メソッドへ渡されている `ServerPlayer player` のUUIDを使用する。

想定する形は以下。

```java
private static ForgingExperienceResult experienceResult(
    ServerPlayer player,
    RoughMetalPartState state,
    boolean success,
    boolean permanentMaterialLoss
) {
    return new ForgingExperienceResult(
        UUID.randomUUID(),
        player.getUUID(),
        state.ingredientCount(),
        success,
        permanentMaterialLoss
    );
}
```

`complete(...)` と `fail(...)` の双方から、この新しい引数構成で呼び出す。

## 2. 鋳造用結果型とHookを追加する

以下を新規追加する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/
├─ CastingExperienceResult.java
└─ CastingExperienceHook.java
```

`CastingExperienceResult` は `ForgingExperienceResult` と同じ基本構造・入力検証方針を採用する。

最低限、以下を検証する。

- `resultId != null`
- `operatorId != null`
- `materialUnits >= 1`

## 3. 正常鋳造の結果をHookへ渡す

`CastingGameService#tryPour(...)` の正常鋳造経路で、`CastingProcess` の保存と素材消費が完了した後にHookを呼び出す。

正常鋳造では `CastingProcess#processId()` を鋳造結果IDとして使用する。

```text
resultId             = process.processId()
operatorId           = player.getUUID()
materialUnits        = definition.ingredientCount()
success              = true
permanentMaterialLoss = false
```

既存の `CastingProcess#operatorId()` も `player.getUUID()` から生成されるため、その値は引き続き粗加工パーツへ鋳造来歴として保存する。

## 4. 鋳造失敗の結果をHookへ渡す

### 早すぎる流し込み

`handleEarlyPour(...)` では、素材消費および返却用金属塊の `pendingOutput` 反映が成功した後にHookを呼び出す。

この経路には既存の `CastingProcess` が存在しないため、鋳造失敗結果専用に `UUID.randomUUID()` で `resultId` を生成する。

```text
resultId             = 新規UUID
operatorId           = player.getUUID()
materialUnits        = usedAmount
success              = false
permanentMaterialLoss = lossAmount > 0
```

このため `handleEarlyPour(...)` には `ServerPlayer player` を渡せるようにシグネチャを変更する。

### 過熱した金属の流し込み

`consumeWithoutOutput(...)` でも、素材消費と状態更新が成功した後にHookを呼び出す。

```text
resultId             = 新規UUID
operatorId           = player.getUUID()
materialUnits        = amount
success              = false
permanentMaterialLoss = true
```

このため `consumeWithoutOutput(...)` にも `ServerPlayer player` を渡せるようにシグネチャを変更する。

## 5. Hookは工程確定後だけ呼ぶ

経験値Hookは、状態変更より先に呼び出さない。

```text
状態検証
↓
素材消費
↓
工程結果を設備へ反映
↓
ExperienceResult生成
↓
ExperienceHook.onResult(...)
```

Hook呼び出し後に工程結果のcommitを行う構造にはしない。

現時点ではHookはno-opだが、後から実際の経験値付与処理を追加した際にも、経験値だけ付与されてアイテム処理が失敗する順序にならないようにする。

## 6. 鋳造来歴はそのまま維持する

以下のフィールドは変更しない。

```java
RoughMetalPartState.castingResultId
RoughMetalPartState.castingOperatorId
```

これらは「この粗加工パーツを誰がどの鋳造工程で作ったか」という来歴情報であり、鍛造経験値イベントとは切り離して扱う。

# 採用しなかった方針

## 鍛造時に鋳造経験値もまとめて処理する

不採用。

鋳造と鍛造で担当プレイヤーが異なる場合に正しい操作者へ経験値を付与できず、工程単位の経験値仕様とも一致しないため。

## `castingResultId` を鍛造結果IDとして流用する

不採用。

異なる工程が同一結果IDを共有し、将来の重複付与防止処理で鋳造と鍛造を区別できなくなるため。

## `castingOperatorId` を鍛造操作者として流用する

不採用。

途中で担当プレイヤーが変わった場合、鍛造を実際に確定したプレイヤーではなく鋳造者へ紐付いてしまうため。

## 鋳造XPを冷却完了または出力回収まで遅延する

不採用。

有効な流し込みがサーバー側で確定した時点を鋳造工程の確定とし、その場で鋳造経験値結果を発生させる方針とするため。

## Hook内部で今回Pufferfish's Skillsへ直接経験値を付与する

不採用。

今回の目的は工程ごとの結果ID・操作者の不整合解消と、鋳造側の経験値受け取り口追加である。実際の経験値計算、重複防止、Pufferfish連携、通知は後続実装として分離する。

# 関連する既存クラス

## `ForgingGameService`

パス:

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/forging/ForgingGameService.java
```

主な変更対象。

現在、成功時と失敗時に `ForgingExperienceHook.onResult(...)` を呼んでいる。

`experienceResult(...)` が `RoughMetalPartState#castingResultId()` と `castingOperatorId()` を使用しているため、鍛造固有の結果IDと `player.getUUID()` を使うように変更する。

## `ForgingExperienceResult`

パス:

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/forging/ForgingExperienceResult.java
```

既存のデータ構造はそのまま利用できる。

原則としてrecordのフィールド追加・削除は不要。

## `ForgingExperienceHook`

パス:

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/forging/ForgingExperienceHook.java
```

既存の鍛造経験値受け取り口。

今回もno-opのままでよい。

鋳造側のHook実装時の形式参考とする。

## `RoughMetalPartState`

パス:

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/part/RoughMetalPartState.java
```

`castingResultId` と `castingOperatorId` を保持する。

今回これらを削除・改名しない。鍛造経験値結果生成からのみ切り離す。

## `CastingGameService`

パス:

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/CastingGameService.java
```

鋳造経験値Hookの主な追加対象。

対象経路:

- `tryPour(...)` の正常鋳造
- `handleEarlyPour(...)` の早すぎる流し込み
- `consumeWithoutOutput(...)` の過熱による素材消失

## `CastingProcess`

パス:

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/CastingProcess.java
```

正常鋳造では既存の `processId` を鋳造結果IDとして使用する。

既存の `operatorId` も鋳造来歴として維持する。

## `CastingTableBlockEntity`

パス:

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/CastingTableBlockEntity.java
```

正常鋳造後の `activeProcess` と失敗時の `pendingOutput` を保持する。

今回、冷却完了や `tryFinalizeAndCollect(...)` から経験値Hookを呼び出さない。

# データフロー

## 正常鋳造

```text
プレイヤーがるつぼを鋳造台へ流し込む
↓
CastingGameService.tryPour(...)
↓
入力条件・素材量・加熱状態を検証
↓
CastingProcess生成
    processId = 鋳造結果ID
    operatorId = player UUID
↓
るつぼから素材を消費
↓
CastingTableBlockEntity.activeProcessへ保存
↓
CastingExperienceResult生成
    resultId = processId
    operatorId = player UUID
    success = true
↓
CastingExperienceHook.onResult(...)
```

## 鋳造失敗

```text
プレイヤーが不適切な状態の金属を流し込む
↓
CastingGameService
↓
失敗条件を確定
↓
使用素材量を確定
↓
素材を消費
↓
返却物または失敗状態を設備へ反映
↓
失敗結果専用UUID生成
↓
CastingExperienceResult生成
    operatorId = player UUID
    success = false
    permanentMaterialLoss = 実際の損失有無
↓
CastingExperienceHook.onResult(...)
```

## 鍛造成功

```text
プレイヤーが鍛造完成要求を送信
↓
ForgingGameService.complete(...)
↓
品質評価・完成品生成
↓
ForgingTableBlockEntity.commitReservedResult(...)
↓
鍛造結果専用UUID生成
↓
ForgingExperienceResult生成
    operatorId = player UUID
    materialUnits = roughState.ingredientCount
    success = true
↓
ForgingExperienceHook.onResult(...)
```

## 鍛造失敗

```text
プレイヤーの操作によって鍛造失敗を確定
↓
ForgingGameService.fail(...)
↓
失敗出力を生成
↓
ForgingTableBlockEntity.commitReservedResult(...)
↓
鍛造結果専用UUID生成
↓
ForgingExperienceResult生成
    operatorId = player UUID
    success = false
    permanentMaterialLoss = true
↓
ForgingExperienceHook.onResult(...)
```

# 擬似コード

```text
鍛造成功時:
    完成予約に失敗したら終了する
    完成品と品質を計算する
    完成品を設備へcommitする
    commitに失敗したらExperienceHookを呼ばない

    新しい鍛造結果UUIDを生成する
    result.operatorIdに現在のplayer UUIDを設定する
    result.materialUnitsに粗加工パーツのingredientCountを設定する
    success=trueでForgingExperienceHookへ渡す

鍛造失敗時:
    失敗結果を設備へcommitする
    commitに失敗したらExperienceHookを呼ばない

    新しい鍛造結果UUIDを生成する
    result.operatorIdに現在のplayer UUIDを設定する
    success=false, permanentMaterialLoss=trueで
    ForgingExperienceHookへ渡す

正常鋳造時:
    鋳造条件を検証する
    CastingProcessを新しいprocessIdとplayer UUIDで生成する
    るつぼから必要素材を消費する
    activeProcessを鋳造台へ保存する

    processIdをresultIdとしてCastingExperienceResultを生成する
    success=true, permanentMaterialLoss=falseで
    CastingExperienceHookへ渡す

早すぎる鋳造時:
    使用素材量と返却量、損失量を計算する
    必要なら返却用金属塊を生成する
    るつぼから使用素材量を消費する
    返却物をpendingOutputへ保存する

    新しい失敗結果UUIDを生成する
    operatorIdに現在のplayer UUIDを設定する
    materialUnitsには返却分を差し引く前のusedAmountを設定する
    success=falseとする
    permanentMaterialLossはlossAmount > 0とする
    CastingExperienceHookへ渡す

過熱鋳造時:
    るつぼから対象素材量を消費する
    鋳造台の状態更新を確定する

    新しい失敗結果UUIDを生成する
    operatorIdに現在のplayer UUIDを設定する
    success=false, permanentMaterialLoss=trueで
    CastingExperienceHookへ渡す

無効操作時:
    素材を消費しない
    ExperienceResultを生成しない
    ExperienceHookを呼ばない
```

# 境界条件・例外

- `ForgingExperienceResult.resultId` に `castingResultId` を設定しない。
- `ForgingExperienceResult.operatorId` に `castingOperatorId` を設定しない。
- `RoughMetalPartState` の鋳造来歴は維持する。
- 経験値Hookは、素材消費や結果commitが成功する前に呼び出さない。
- `CastingActionResult.SUCCESS` と `CastingExperienceResult.success` を同一視しない。
- 早すぎる流し込みで素材が一部返却された場合でも、`materialUnits` は使用した全素材量とする。
- 早すぎる流し込みで恒久損失が0なら `permanentMaterialLoss=false` とする。
- 過熱によって全素材が失われる場合は `permanentMaterialLoss=true` とする。
- 通常の鋳造成功では `permanentMaterialLoss=false` とする。
- `CastingProcess` を生成しない失敗経路では、失敗結果専用のUUIDを新規生成する。
- nullプレイヤーや無効な入力によって処理が拒否された場合、Hookを呼ばない。
- Hookは現時点ではno-opであり、Hookの例外処理やPufferfish API失敗時の再試行は今回扱わない。
- 今回生成する鍛造結果ID・鋳造失敗結果IDの永続化方式は定義しない。将来の重複付与防止機構で必要になった場合は別途設計する。

# 完了条件

- `ForgingGameService` が `castingResultId` を鍛造結果IDとして使用していない。
- `ForgingGameService` が `castingOperatorId` を鍛造経験値結果の操作者として使用していない。
- 鍛造成功時に、鍛造固有の新規UUIDと結果確定プレイヤーUUIDを持つ `ForgingExperienceResult` がHookへ渡される。
- 鍛造失敗時にも同様に、鍛造固有の新規UUIDと結果確定プレイヤーUUIDが使用される。
- `CastingExperienceResult` が追加されている。
- `CastingExperienceHook` が追加されている。
- 正常鋳造の確定直後に `CastingExperienceHook.onResult(...)` が1回呼ばれる。
- 早すぎる流し込みで素材消費が確定した場合、失敗用 `CastingExperienceResult` がHookへ渡される。
- 過熱した金属の流し込みで素材消費が確定した場合、失敗用 `CastingExperienceResult` がHookへ渡される。
- 無効操作や素材消費前の拒否ではHookが呼ばれない。
- `RoughMetalPartState#castingResultId` と `castingOperatorId` は鋳造来歴として維持される。
- `ForgingExperienceHook` と `CastingExperienceHook` は今回の変更では実際の経験値付与を行わない。
- 既存の鋳造、冷却、鍛造、出力回収の挙動を変更しない。

# 今回の対象外

- Pufferfish's Skillsへの実際の鍛冶経験値付与
- 鋳造・鍛造の経験値量計算
- 成功時XP、失敗時25%XPの計算ロジック
- 最大レベル判定
- レベルアップ処理
- アクションバーへの「鍛冶経験値 +N」表示
- 経験値の二重付与防止用LedgerまたはCapability
- 結果IDの永続化と再起動後のexactly-once保証
- 非金属加工の経験値Hook
- 品質付き完成品組み立ての経験値Hook
- `BlacksmithData` への経験値・レベル情報追加
- `experience.json` のExperience Source登録
