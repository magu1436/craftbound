# 目的

`develop` に実装済みの非金属加工プロセスを、既存の鍛冶師経験値システムへ接続する。

本実装では以下を行う。

- 非金属加工成功時に鍛冶師経験値を付与する。
- 非金属加工によって素材が恒久的に失われた場合に失敗経験値を付与する。
- 非金属加工の経験値計算から素材数・`ingredient_count` への依存を完全に除去する。
- 加工台破壊など、通常の加工完了ではない経路から経験値が付与されないようにする。
- 既存の `BlacksmithExperienceService`、`BlacksmithProcessExperienceSource`、経験値通知処理を再利用する。
- 関連する鍛冶師仕様・テストケースを今回の確定仕様へ更新する。

非金属加工では、1回の加工工程を1つの経験値付与単位として扱う。

将来、非金属加工のデータ定義から素材数や `ingredient_count` の概念自体を削除しても、経験値関連クラス・Hook・共通経験値サービスの修正が不要な構造とする。

# 確定した仕様

## 非金属加工経験値

非金属加工の経験値は素材数に依存させない。

成功時は、加工1工程につき固定で以下を付与する。

```text
3 XP
```

素材を恒久的に失う失敗では、既存の失敗経験値ルールを適用する。

```text
max(1, floor(成功時経験値 × 0.25))
```

非金属加工では成功時経験値が固定3XPなので、失敗経験値は常に以下となる。

```text
max(1, floor(3 × 0.25))
= 1 XP
```

したがって非金属加工は以下とする。

| 結果 | 経験値 |
|---|---:|
| 正常完了 | 3 XP |
| 素材を恒久的に失う加工失敗 | 1 XP |
| 拒否・中断・素材損失を伴わない終了 | 0 XP |

品質値、削ったセル数、操作回数、加工時間、道具耐久消費量による経験値倍率は設けない。

## `ingredient_count` との分離

非金属加工経験値では以下を参照しない。

```text
NonMetalPartDefinition#ingredientCount
CarvingDefinitionSnapshot#ingredientCount
```

`CarvingExperienceResult` にも素材数を保持しない。

`BlacksmithExperienceService#processCarvingResult(...)` にも素材数を渡さない。

現在の非金属加工データ定義や加工処理が `ingredient_count` を保持していても、今回の経験値処理はその値に依存しない。

今回の実装では `NonMetalPartDefinition` や `CarvingDefinitionSnapshot` から `ingredient_count` 自体を削除しない。

ただし、将来それらから素材数の概念を削除しても、経験値系の変更が発生しないことを設計条件とする。

## 経験値付与対象

既存の `BlacksmithExperienceService` の判定をそのまま使用する。

以下をすべて満たす `ServerPlayer` のみ経験値付与対象とする。

- `operatorId` と `player.getUUID()` が一致する。
- Creativeではない。
- Spectatorではない。
- Forge `FakePlayer` ではない。

近くのプレイヤー、チームメンバー、過去に同じ加工を操作していたプレイヤーへ経験値を共有しない。

## 正常完了

通常のプレイヤー操作によって `CarvingGameService#finalizeCarving(...)` が加工結果を正常にcommitした直後に経験値を付与する。

処理順序は以下を維持する。

```text
加工結果を評価
↓
完成パーツを生成
↓
CarvingTableBlockEntity#commitOutput(...)
↓
CarvingExperienceHook.onResult(...)
↓
必要なら完成品をプレイヤーへ回収
```

`commitOutput(...)` に失敗した場合は経験値を付与しない。

経験値付与を出力回収まで遅延させない。

出力が `pendingOutputs` に残った場合でも、加工結果のcommitが成功していれば工程は完了済みとして1回だけ経験値を付与する。

## 加工失敗

`CarvingGameService#applyStroke(...)` で破損条件が成立し、加工素材が恒久的に失われた場合に失敗経験値を付与する。

現在の破損経路では `CarvingTableBlockEntity#clearBroken()` により以下が消去される。

```text
material
progress
activeSession
```

この状態遷移を素材の恒久損失確定とする。

失敗経験値Hookは `clearBroken()` が成功した後に1回だけ呼び出す。

ただし `clearBroken()` 後は `progress` が消えるため、`processId` など `CarvingExperienceResult` の生成に必要な情報は `clearBroken()` より前に退避する。

失敗結果は以下とする。

```text
resultId              = progress.processId()
operatorId            = 破損を確定させた player.getUUID()
success               = false
permanentMaterialLoss = true
```

`clearBroken()` に失敗した場合は経験値Hookを呼ばない。

## 道具破損

加工中に道具だけが壊れた場合は鍛冶経験値を付与しない。

現在の `StrokeResult.TOOL_BROKEN` は加工素材そのものの恒久損失を意味しないため、経験値結果を発生させない。

その後別の道具で加工を再開し、正常完了または素材破損が確定した時点で初めて経験値を付与する。

## 加工台破壊

加工途中の加工台を破壊したこと自体では経験値を付与しない。

現在の `CarvingTableBlock#playerWillDestroy(...)` は、加工途中の場合に `CarvingGameService#finalizeCarving(...)` を呼び出して出力を確定させている。

`CarvingExperienceHook` を単純に有効化すると、この経路でも正常完了経験値が付与されるため、通常完了と設備破壊による強制確定を区別する。

以下を満たすこと。

```text
通常の加工完了:
    結果をcommitする
    経験値を付与する
    必要なら出力をプレイヤーへ渡す

加工台破壊による強制確定:
    結果をcommitしてドロップ可能な状態にする
    経験値は付与しない
    プレイヤーへ直接回収しない

システム上のonRemove:
    必要なら結果をcommitする
    経験値は付与しない
```

経験値付与可否は `deliverToOperator` と同義に扱わない。

「出力を直接渡すか」と「経験値を付与するか」は別の責務として明示する。

## 二重付与防止

非金属加工専用の処理済みresultId台帳、SavedData、Capability、`experienceGranted` フラグは追加しない。

既存方針どおり、加工状態の一方向遷移で通常の重複要求を防止する。

正常完了:

```text
progressあり
↓
completion予約
↓
commitOutput成功
↓
progress消去
↓
ExperienceHook
```

加工破損:

```text
progressあり
↓
clearBroken成功
↓
progress消去
↓
ExperienceHook
```

同じ完了要求または削り要求が再送されても、2回目は有効な `progress` が存在しないため、経験値Hookへ再到達させない。

`CarvingExperienceResult#resultId` は結果識別・来歴・ログ用途として残してよいが、経験値の永続重複排除キーとしては使用しない。

## Pufferfish連携と通知

既存の以下を再利用する。

```text
BlacksmithExperienceService
BlacksmithProcessExperienceSource
BlacksmithExperienceNotifier
craftbound:blacksmith_process
```

新しいPufferfish Experience Sourceは追加しない。

実際に1以上のXPが加算された場合だけ、既存のアクションバー通知を1回表示する。

最大レベル等により実加算量が0の場合は通知しない。

Pufferfish更新に失敗した場合は既存方針どおり以下とする。

- 加工結果をロールバックしない。
- 素材や出力を復元しない。
- 永続再試行キューを作らない。
- エラーログを出力する。
- アクションバーを表示しない。

# 採用した実装方針

## 1. `CarvingExperienceResult` から素材数を削除する

現在:

```java
public record CarvingExperienceResult(
    UUID resultId,
    UUID operatorId,
    int ingredientCount,
    boolean success,
    boolean permanentMaterialLoss
) {}
```

変更後:

```java
public record CarvingExperienceResult(
    UUID resultId,
    UUID operatorId,
    boolean success,
    boolean permanentMaterialLoss
) {}
```

コンストラクタ検証から以下を削除する。

```text
ingredientCount >= 1
```

以下の検証は維持する。

```text
resultId != null
operatorId != null
```

これにより、経験値結果型が非金属加工データ定義の素材数概念へ依存しないようにする。

## 2. `BlacksmithExperienceService` のCarving窓口を固定XP方式へ変更する

現在の `processCarvingResult(...)` から素材数引数を削除する。

想定する公開窓口:

```java
public static AwardResult processCarvingResult(
    ServerPlayer player,
    UUID operatorId,
    boolean success,
    boolean permanentMaterialLoss
)
```

非金属加工の成功時基礎経験値として固定値を定義する。

```java
private static final int CARVING_SUCCESS_EXPERIENCE = 3;
```

`CARVING_MULTIPLIER = 3` と「素材数 × 係数」という扱いは非金属加工について廃止する。

### 共通内部処理

現在の共通内部処理が `materialUnits` と `multiplier` を要求する構造の場合、以下のように「成功時基礎経験値」を受け取る構造へ整理する。

```java
private static AwardResult processResult(
    ServerPlayer player,
    UUID operatorId,
    int successExperience,
    boolean success,
    boolean permanentMaterialLoss
)
```

各公開窓口で成功時経験値を決定してから共通処理へ渡す。

概念上は以下とする。

```text
鋳造:
    successExperience = materialUnits × 1

鍛造:
    successExperience = materialUnits × 2

非金属加工:
    successExperience = 3

組み立て:
    successExperience = qualityPartCount × 1
```

これにより、共通失敗計算・対象プレイヤー判定・Pufferfish連携を維持しつつ、非金属加工だけを素材数から独立させる。

失敗経験値の計算は共通処理で維持する。

```text
success:
    requestedExperience = successExperience

!success && permanentMaterialLoss:
    requestedExperience = max(1, floor(successExperience × 0.25))

それ以外:
    0 XP
```

## 3. `CarvingExperienceHook` を既存経験値サービスへ接続する

現在no-opの `CarvingExperienceHook#onResult(...)` を実装する。

想定:

```java
public static void onResult(
    ServerPlayer player,
    CarvingExperienceResult result
) {
    AwardResult awardResult =
        BlacksmithExperienceService.processCarvingResult(
            player,
            result.operatorId(),
            result.success(),
            result.permanentMaterialLoss()
        );

    BlacksmithExperienceNotifier.notifyGranted(
        player,
        awardResult
    );
}
```

Hookから以下を参照しない。

```text
NonMetalPartDefinition
CarvingDefinitionSnapshot
ingredient_count
```

HookからPufferfish APIを直接呼ばない。

## 4. 正常完了時の結果生成を修正する

`CarvingGameService#finalizeCarving(...)` の正常完了経路では、`commitOutput(...)` 成功後に `CarvingExperienceResult` を生成する。

現在の以下の値は削除する。

```text
progress.definitionSnapshot().ingredientCount()
```

変更後:

```java
new CarvingExperienceResult(
    progress.processId(),
    operator.getUUID(),
    true,
    false
)
```

通常完了時だけ `CarvingExperienceHook.onResult(...)` へ渡す。

## 5. 加工破損時に失敗結果をHookへ渡す

`CarvingGameService#applyStroke(...)` の破損判定経路を変更する。

処理順序:

```text
破損条件成立
↓
progress.processId() を退避
↓
失敗用 CarvingExperienceResult を生成
↓
table.clearBroken()
├─ false: ERRORまたは既存方針に従って終了
└─ true:
      CarvingExperienceHook.onResult(...)
      GUIを閉じる
      BROKEN
```

想定:

```java
CarvingExperienceResult experienceResult =
    new CarvingExperienceResult(
        progress.processId(),
        player.getUUID(),
        false,
        true
    );

if (!table.clearBroken()) {
    return StrokeResult.ERROR;
}

CarvingExperienceHook.onResult(
    player,
    experienceResult
);

player.closeContainer();
return StrokeResult.BROKEN;
```

状態消去より先に経験値を付与しない。

## 6. 通常完了と設備破壊を分離する

`CarvingGameService#finalizeCarving(...)` の内部処理で、以下を独立した条件として扱う。

```text
deliverToOperator
grantExperience
```

実装方法は既存構造へ合わせてよいが、呼び出し側で意味が明確になる形にする。

最小変更案:

```java
private static FinalizeResult finalizeCarving(
    CarvingTableBlockEntity table,
    ServerPlayer operator,
    boolean deliverToOperator,
    boolean grantExperience
)
```

通常完了用の公開メソッド:

```text
deliverToOperator = true
grantExperience   = true
```

加工台破壊:

```text
deliverToOperator = false
grantExperience   = false
```

システム上の `onRemove`:

```text
operator          = null
deliverToOperator = false
grantExperience   = false
```

`grantExperience == true && operator != null` の場合だけ `CarvingExperienceHook` を呼ぶ。

boolean引数が呼び出し側で不明瞭になる場合は、通常完了用と設備破壊用のラッパーメソッドを分けてもよい。

例:

```text
finalizeCarving(...)
finalizeCarvingWithoutExperience(...)
```

重要なのは、`deliverToOperator == false` かどうかだけで経験値付与可否を暗黙判断しないことである。

## 7. 仕様書・テストケースを更新する

`docs/occupations/blacksmith/blacksmith.md` の非金属加工経験値定義を変更する。

変更前の意味:

```text
使用素材数 × 3
```

変更後:

```text
非金属加工1工程の正常完了 = 3 XP
```

素材を恒久的に失う失敗:

```text
1 XP
```

非金属加工の経験値説明では `ingredient_count` や使用素材数を基礎値として記載しない。

`docs/occupations/blacksmith/test-cases.md` では少なくとも以下を確認できるようにする。

- 非金属加工正常完了で3XP。
- 非金属加工による素材破損で1XP。
- 道具破損のみでは0XP。
- 中断・再開・削り操作だけでは0XP。
- 加工台破壊による強制確定では0XP。
- 通常完了要求の再送で経験値が重複しない。
- 素材破損後の操作再送で経験値が重複しない。
- Creative / Spectator / FakePlayerでは0XP。
- 加工担当者が途中で変わった場合、正常完了または素材破損を確定したプレイヤーだけがXPを得る。

テストケースも素材数をパラメータとする仕様にしない。

# 採用しなかった方針

- `ingredient_count × 3` で経験値を計算しない。非金属加工では素材数を経験値概念へ持ち込まないため。
- `processCarvingResult(..., 1, ...)` のように固定値1を素材数として渡す実装にしない。動作上は同じでも、経験値APIに不要な素材数概念が残るため。
- `CarvingExperienceResult` に `ingredientCount` を残さない。将来の素材数仕様削除時に経験値系まで修正対象になるため。
- 加工台破壊時に正常完了XPを付与しない。設備破壊のみで経験値を得られるため。
- 加工台破壊を素材永久損失失敗として1XPにしない。現在は結果を確定・ドロップする経路であり、素材損失そのものを意味しないため。
- 道具破損を加工失敗として1XPにしない。加工素材が残っており再開可能なため。
- 非金属加工専用のPufferfish Experience Sourceを追加しない。既存の `craftbound:blacksmith_process` を共有できるため。
- 非金属加工専用の経験値重複排除台帳を追加しない。既存の一方向状態遷移で通常の重複要求を防げるため。
- 今回 `NonMetalPartDefinition` / `CarvingDefinitionSnapshot` から `ingredient_count` を削除しない。今回の対象は経験値統合であり、加工素材仕様そのものの整理は別変更とする。

# 関連する既存クラス

## `CarvingExperienceHook`

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/carving/CarvingExperienceHook.java
```

現在no-op。

今回、`BlacksmithExperienceService#processCarvingResult(...)` と `BlacksmithExperienceNotifier` へ接続する。

## `CarvingExperienceResult`

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/carving/CarvingExperienceResult.java
```

現在 `ingredientCount` を保持している。

今回、経験値系から素材数依存を除去するためフィールドを削除する。

## `CarvingGameService`

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/carving/CarvingGameService.java
```

主な変更対象。

- 正常完了時の `CarvingExperienceResult` 生成から素材数を削除する。
- 加工破損時に失敗経験値結果を発生させる。
- 通常完了と設備破壊による強制確定で経験値付与可否を分離する。

## `CarvingTableBlockEntity`

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/carving/CarvingTableBlockEntity.java
```

基本的に既存実装を利用する。

以下の状態遷移を経験値付与前提として使用する。

```text
正常完了: commitOutput(...)
加工破損: clearBroken()
```

経験値専用フィールドは追加しない。

## `CarvingTableBlock`

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/carving/CarvingTableBlock.java
```

加工台破壊時の `finalizeCarving(...)` 呼び出しを、経験値なしの強制確定経路へ変更する。

通常の右クリック完了では経験値ありの経路を使用する。

## `BlacksmithExperienceService`

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/experience/BlacksmithExperienceService.java
```

既存の非金属加工窓口を固定3XP方式へ変更する。

非金属加工の公開APIから素材数引数を削除する。

共通内部処理を成功時基礎経験値ベースへ整理し、鋳造・鍛造・組み立ての既存計算結果を変えない。

## `BlacksmithExperienceNotifier`

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/experience/BlacksmithExperienceNotifier.java
```

変更不要。

既存の実加算量通知をそのまま使用する。

## `BlacksmithProcessExperienceSource`

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/experience/BlacksmithProcessExperienceSource.java
```

変更不要。

既存のPufferfish Experience Sourceをそのまま使用する。

## 非金属加工定義

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/carving/definition/NonMetalPartDefinition.java
```

今回の経験値計算では `ingredientCount` を参照しない。

今回の変更でフィールド自体を削除する必要はない。

# データフロー

## 正常完了

```text
プレイヤーが通常の加工完了を要求
↓
CarvingGameService#finalizeCarving
↓
品質評価
↓
完成パーツ生成
↓
CarvingTableBlockEntity#commitOutput
↓
progress消去・pendingOutput確定
↓
CarvingExperienceResult
    resultId = processId
    operatorId = 完了プレイヤー
    success = true
    permanentMaterialLoss = false
↓
CarvingExperienceHook
↓
BlacksmithExperienceService
↓
固定3XP
↓
BlacksmithProcessExperienceSource
↓
Pufferfish's Skills
↓
実加算量 > 0 の場合のみActionBar
```

## 加工破損

```text
削り操作受理
↓
破損条件成立
↓
processIdを退避
↓
CarvingTableBlockEntity#clearBroken
↓
material / progress消去
↓
CarvingExperienceResult
    resultId = processId
    operatorId = 最後の削り操作プレイヤー
    success = false
    permanentMaterialLoss = true
↓
CarvingExperienceHook
↓
BlacksmithExperienceService
↓
固定成功XP 3 の25%
↓
最低値補正
↓
1XP
↓
Pufferfish's Skills
```

## 道具破損

```text
削り操作
↓
道具耐久が0
↓
TOOL_BROKEN
↓
加工素材・progressは維持
↓
ExperienceHookを呼ばない
```

## 加工台破壊

```text
加工途中の加工台を破壊
↓
必要なら加工結果を強制確定
↓
出力をドロップ可能な状態にする
↓
ExperienceHookを呼ばない
↓
BlockEntity内容をドロップ
```

# 擬似コード

```text
非金属加工結果を経験値へ変換する:

    player と operatorId が一致しなければ終了する

    player がCreative、Spectator、FakePlayerなら終了する

    successなら:
        requestedXp = 3

    successでなく、permanentMaterialLossなら:
        requestedXp = max(1, floor(3 * 0.25))

    それ以外:
        終了する

    Pufferfishへ requestedXp を渡す

    更新失敗なら:
        エラーログを出す
        加工結果は巻き戻さない
        通知しない
        終了する

    実加算量が1以上なら:
        既存ActionBar通知を1回表示する
```

```text
通常の非金属加工完了:

    completion予約に失敗したら終了する

    progressがなければ予約を解除して終了する

    品質を計算する
    完成パーツを生成する

    commitOutputに失敗したら:
        completion予約を解除する
        経験値を付与しない
        終了する

    operatorが存在し、通常完了経路なら:
        processIdとoperatorIdだけを持つ成功ExperienceResultを生成する
        CarvingExperienceHookへ渡す

    deliverToOperatorなら:
        出力を回収する
```

```text
非金属加工中に素材が破損した:

    破損条件を判定する

    破損しないなら通常処理を続行する

    processIdをprogressから取得する

    失敗ExperienceResultを生成する

    clearBrokenを実行する

    clearBrokenに失敗したら:
        経験値を付与しない
        ERRORとして終了する

    CarvingExperienceHookへ失敗結果を渡す
    GUIを閉じる
    BROKENとして終了する
```

# 実装手順

## Step 1: 経験値モデルを素材数から分離

変更する。

- `CarvingExperienceResult` から `ingredientCount` を削除する。
- `BlacksmithExperienceService#processCarvingResult(...)` から素材数引数を削除する。
- 非金属加工成功XPを固定3とする。
- 共通内部処理を必要に応じて `successExperience` ベースへ整理する。
- 鋳造・鍛造・組み立ての既存XP計算結果を変えない。

この段階で、非金属加工経験値系から `ingredient_count` への参照が0件になること。

## Step 2: 非金属加工Hookを接続

変更する。

- `CarvingExperienceHook` のno-opを解除する。
- `BlacksmithExperienceService#processCarvingResult(...)` を呼ぶ。
- `BlacksmithExperienceNotifier` で実加算量を通知する。
- `finalizeCarving(...)` の正常完了結果から素材数を削除する。
- `applyStroke(...)` の素材破損経路で失敗結果をHookへ渡す。

状態commitより前にHookを呼ばない。

## Step 3: 強制確定経路を経験値対象外にする

変更する。

- 通常完了と加工台破壊の `finalizeCarving(...)` 呼び出しを区別する。
- `playerWillDestroy(...)` では経験値を付与しない。
- `onRemove(...)` では経験値を付与しない。
- 出力配送可否と経験値付与可否を別条件として扱う。
- 道具破損のみではHookを呼ばない。

## Step 4: 仕様・テストケース更新

更新する。

```text
docs/occupations/blacksmith/blacksmith.md
docs/occupations/blacksmith/test-cases.md
```

非金属加工経験値を以下へ統一する。

```text
成功 = 3 XP / 工程
素材永久損失失敗 = 1 XP / 工程
```

素材数や `ingredient_count` を経験値基礎値として扱う記述を削除する。

# 境界条件・例外

- `CarvingExperienceResult` は素材数を持たない。
- 非金属加工経験値コードから `ingredientCount()` を呼ばない。
- 正常完了は `commitOutput(...)` 成功後だけ経験値対象とする。
- `commitOutput(...)` 失敗時は0XP。
- 加工素材の破損は `clearBroken()` 成功後だけ失敗XP対象とする。
- `clearBroken()` 失敗時は0XP。
- 道具破損のみでは0XP。
- 削り操作、ドラッグ、警告状態到達だけでは0XP。
- 加工中断・GUIクローズ・再開だけでは0XP。
- 未加工素材の取り出しでは0XP。
- 完成品回収だけでは0XP。
- 加工台破壊だけでは0XP。
- `onRemove` による強制確定では0XP。
- Creative、Spectator、FakePlayerでは0XP。
- `operatorId` 不一致では0XP。
- `success == true && permanentMaterialLoss == true` は既存共通処理どおり不整合入力としてXPを付与しない。
- 最大レベルで実加算0の場合は正常終了とし通知しない。
- Pufferfish更新失敗時に加工状態をロールバックしない。
- サーバー異常終了を跨ぐ厳密なexactly-once保証は今回追加しない。
- 将来 `ingredient_count` が非金属加工定義から削除されても、経験値関連コードの修正を必要としない。

# 完了条件

- `CarvingExperienceResult` に素材数フィールドが存在しない。
- `BlacksmithExperienceService#processCarvingResult(...)` が素材数を受け取らない。
- 非金属加工経験値コードが `NonMetalPartDefinition#ingredientCount` / `CarvingDefinitionSnapshot#ingredientCount` を参照しない。
- 正常な非金属加工完了で3XPが付与される。
- 素材を恒久的に失う加工破損で1XPが付与される。
- 道具破損のみではXPが付与されない。
- 加工台破壊ではXPが付与されない。
- `onRemove` による強制確定ではXPが付与されない。
- 通常の完了要求を再送してもXPが重複しない。
- 素材破損確定後に削り要求を再送してもXPが重複しない。
- Creative、Spectator、FakePlayerへXPが付与されない。
- 経験値は工程を最終確定したプレイヤーだけに付与される。
- 実加算量1以上の場合だけ既存ActionBar通知が表示される。
- 既存の鋳造・鍛造・組み立て経験値計算結果が変化しない。
- `blacksmith.md` と `test-cases.md` が固定3XP方式へ更新されている。

# 今回の対象外

- `NonMetalPartDefinition` から `ingredient_count` を削除する変更
- `CarvingDefinitionSnapshot` から素材数フィールドを削除する変更
- 非金属加工の素材投入・消費仕様そのものの変更
- 非金属加工の品質計算方式変更
- 非金属加工UI・同期方式変更
- 道具耐久処理の変更
- 鋳造・鍛造経験値の素材単位数ベース計算変更
- 完成品組み立て経験値仕様の変更
- 新しいPufferfish Experience Sourceの追加
- resultIdの永続重複排除台帳
- XP付与失敗時の永続再試行
- サーバー異常終了を跨ぐ厳密なトランザクション保証
