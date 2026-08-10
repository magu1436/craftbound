# ハンマー・鍛造台・鍛造ミニゲーム 実装仕様書

# 目的

Craftbound の鍛冶師工程における、以下を実装する。

```text
粗加工パーツ
    ↓
鍛造台へ設置
    ↓
鍛造ハンマーを持って右クリック
    ↓
鍛造GUIを開始
    ↓
打撃強度ゲージを見ながら打撃
    ↓
完成 または 破損
    ↓
品質付き完成金属パーツ / 失敗金属塊
```

対象:

- `craftbound:smithing_hammer`
- `craftbound:forging_table`
- 鍛造台 BlockEntity
- 鍛造中状態
- 鍛造ゲージ
- 鍛造評価
- 金属パーツ最終品質評価
- Menu / Screen
- Client → Server の打撃通信
- 中断・再開
- 同時操作制御
- 鍛造台上のパーツ描画
- バニラ音源によるフィードバック
- スキルレベルに応じた操作支援
- 将来のハンマー耐久消費用 no-op hook
- 将来の鍛冶経験値付与用 no-op hook

## 前提実装

本仕様の実装前に、以下の仕様書に基づく品質状態基盤が実装済みであること。

```text
quality-state-implementation-spec.md
```

最低限、以下が利用可能であること。

```java
QualityStateService.read(...)
QualityStateService.setQuality(...)
QualityStateService.copyQuality(...)
```

品質値は `MetalPartState` へ埋め込まず、完成 ItemStack の独立した `CraftboundQuality` 状態として付与する。

## 本仕様の優先関係

本仕様は現在の `develop` ブランチと、以下の既存仕様を基礎とする。

```text
docs/occupations/blacksmith/blacksmith.md
docs/occupations/blacksmith/data-formats.md
docs/occupations/blacksmith/iron-pickaxe.md
docs/occupations/blacksmith/ui.md
docs/occupations/blacksmith/lifecycle.md
docs/occupations/blacksmith/test-cases.md
docs/occupations/blacksmith/equipment/stations-and-tools.md
```

ただし、以下は今回の決定で既存文書より本仕様を優先する。

1. `skill_assists.json` は Pufferfish's Skills の個別 skill ID を直接キーにせず、既存 `IBlacksmithData` のスキルレベルを使って効果値を解決する
2. ハンマーは MVP で耐久値を消費しない
3. 将来の耐久消費処理を追加する場所だけ、鍛造プロセス内へ no-op hook として用意する
4. 鍛造経験値は今回付与しない。工程結果を渡す no-op hook だけを用意する
5. 加工感覚の補助音を含め、今回新規 `.ogg` は作成しない。バニラ `SoundEvent` を使用する
6. 鍛造GUI専用背景Textureは作成せず、`GuiGraphics` で描画する
7. 鍛造台とハンマーのTexture PNGはユーザー側で用意済みとして扱う。実装側では画像生成、編集、仮Texture作成を行わない

## Codex の読み込み範囲

実装開始時は原則として以下だけを読む。

```text
src/main/java/com/magu1436/craftbound/registry/
    CraftboundItems.java
    CraftboundBlocks.java
    CraftboundBlockEntities.java
    CraftboundMenus.java
    CraftboundCapabilities.java

src/main/java/com/magu1436/craftbound/client/event/
    CraftboundClientEvents.java

src/main/java/com/magu1436/craftbound/event/
    CraftboundDataReloadEventHandler.java

src/main/java/com/magu1436/craftbound/network/
    CraftboundNetwork.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/capability/
    IBlacksmithData.java
    BlacksmithData.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/definition/
    MetalPartDefinition.java
    MetalPartDefinitionSnapshot.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/part/
    RoughMetalPartItem.java
    RoughMetalPartState.java
    RoughMetalPartStateCodec.java
    RoughMetalPartStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/finished/
    MetalPartStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/lump/
    MetalLumpItem.java
    MetalLumpStateService.java

src/main/java/com/magu1436/craftbound/common/quality/
    QualityState.java
    QualityStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/furnace/
    BlacksmithFurnaceBlock.java
    BlacksmithFurnaceBlockEntity.java
```

`BlacksmithFurnaceBlockEntity` は BlockEntity の保存、内容物ドロップ、server ticker、SoundEvent 利用の既存実装パターンを確認するためだけに読む。

以下は原則として読まない。

- 他職業のコード
- Git 履歴
- 他ブランチ
- 旧仕様書の全探索
- 無関係な連携MODコード
- 非金属加工の未実装コード

必要なシンボルが見つからない場合だけ、そのシンボル名で限定検索する。

# 確定した仕様

## 1. 鍛造ハンマー

Item ID:

```text
craftbound:smithing_hammer
```

登録名:

```java
CraftboundItems.SMITHING_HAMMER
```

MVPでは以下とする。

- 最大スタック数1
- 耐久値なし
- 使用による消費なし
- 特殊な右クリック処理を Item クラス自身へ持たせない
- 鍛造台側が「メインハンドに鍛造ハンマーを持っているか」を判定する
- 道具保全スキルの対象外

専用 Item クラスは不要とする。

```java
new Item(
    new Item.Properties().stacksTo(1)
)
```

で登録してよい。

将来ハンマー耐久を追加する場合も、鍛造処理内の `ForgingHammerDurabilityHook` を変更することを主経路とし、鍛造処理本体へ耐久ロジックを直接追加しない。

## 2. 鍛造ハンマーのModel / Recipe

Texture は以下に既に存在するものとして実装する。

```text
assets/craftbound/textures/item/smithing_hammer.png
```

Texture PNGを生成・変更しない。

Item Model:

```text
assets/craftbound/models/item/smithing_hammer.json
```

内容:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "craftbound:item/smithing_hammer"
  }
}
```

レシピ:

```text
data/craftbound/recipes/blacksmith/equipment/smithing_hammer.json
```

shaped recipe:

```text
III
 R 
 R 
```

```text
I = minecraft:iron_ingot
R = minecraft:stick
```

出力:

```text
craftbound:smithing_hammer × 1
```

## 3. 鍛造台

Block ID:

```text
craftbound:forging_table
```

登録:

```java
CraftboundBlocks.FORGING_TABLE
CraftboundItems.FORGING_TABLE
CraftboundBlockEntities.FORGING_TABLE
CraftboundMenus.FORGING_TABLE
```

新規クラス:

```text
occupations/blacksmith/forging/
    ForgingTableBlock.java
    ForgingTableBlockEntity.java
    ForgingGameService.java
```

クライアント:

```text
occupations/blacksmith/client/
    ForgingScreen.java
    ForgingTableBlockEntityRenderer.java
```

Menu:

```text
occupations/blacksmith/forging/menu/
    ForgingMenu.java
```

## 4. 鍛造台のBlockState

鍛造台は水平4方向を持つ。

```java
HorizontalDirectionalBlock.FACING
```

相当の `DirectionProperty` を使用する。

設置時はプレイヤー側を正面とする通常の作業台系ブロックと同じ向きにする。

ピストンでは移動できないようにする。

```text
PushReaction.BLOCK
```

相当の設定を使用する。

BlockEntityを持つため `BaseEntityBlock` を使用してよい。

## 5. 鍛造台のTexture / Model

以下のTexture PNGはユーザーが用意済みとして扱う。

```text
assets/craftbound/textures/block/forging_table_top.png
assets/craftbound/textures/block/forging_table_side.png
assets/craftbound/textures/block/forging_table_front.png
```

実装側でPNGを生成、編集、置換しない。

必要JSON:

```text
assets/craftbound/blockstates/forging_table.json
assets/craftbound/models/block/forging_table.json
assets/craftbound/models/item/forging_table.json
```

Block Model は以下の面割当を使用する。

```text
up             → forging_table_top
down           → forging_table_side
north(front)   → forging_table_front
south/east/west→ forging_table_side
particle       → forging_table_side
```

`facing` に応じて blockstate 側でY回転する。

Block Item Model は block model を parent とする。

## 6. 鍛造台レシピ

配置:

```text
data/craftbound/recipes/blacksmith/equipment/forging_table.json
```

shaped recipe:

```text
SSS
 B 
 T 
```

```text
S = minecraft:smooth_stone
B = minecraft:iron_block
T = minecraft:smithing_table
```

出力:

```text
craftbound:forging_table × 1
```

## 7. 鍛造台の基本操作

処理優先順位は以下とする。

### 7.1 保留出力がある場合

`pendingOutputs` がある間は新しい加工を開始できない。

空手右クリック:

```text
全保留出力をプレイヤーInventoryへ収納可能
    → 全件を一括回収
    → pendingOutputsを空にする

1個でも収納不能
    → 何も移動しない
```

一部回収は禁止。

空手以外の操作は拒否する。

### 7.2 作業中パーツがない場合

メインハンドの ItemStack が有効な `RoughMetalPartItem` であり、`RoughMetalPartStateService.read` に成功した場合:

```text
粗加工パーツ1個を鍛造台へ移動
```

粗加工パーツは最大スタック1なので、ItemStack全体を移動してよい。

`ForgingProgressState` の root が存在するが読込不能な場合は、アイテムを変更せず挿入を拒否する。

### 7.3 作業中パーツがある場合 + ハンマー

メインハンドが `CraftboundItems.SMITHING_HAMMER` の場合:

```text
サーバー側で開始条件検証
    ↓
操作権取得
    ↓
鍛造セッション開始または再開
    ↓
ForgingMenuを開く
```

他プレイヤーが操作中の場合は開かない。

### 7.4 作業中パーツがある場合 + 空手

操作セッションがない場合だけ、空手右クリックで状態付き粗加工パーツを取り出せる。

一度以上打撃済みでも取り出し可能。

```text
ForgingProgressState
```

を保持した同じ ItemStack を返す。

操作中は取り出しを拒否する。

## 8. BlockEntity が保持する永続データ

`ForgingTableBlockEntity` は以下を永続化する。

```text
workingPart: ItemStack
pendingOutputs: List<ItemStack>
```

active session は永続化しない。

保存NBT例:

```text
WorkingPart: <ItemStack NBT>
PendingOutputs: [<ItemStack NBT>, ...]
```

以下は transient とする。

```text
activeSession
completionReserved
invalidStoredState
```

ただし `invalidStoredState` は load 時に保存内容から再判定する。

## 9. 鍛造進捗は粗加工パーツの別 root とする

既存 `RoughMetalPartState` は鋳造工程由来の immutable な情報として維持する。

以下を追加しない。

```text
RoughMetalPartState.strikeHistory
RoughMetalPartState.gaugeValue
RoughMetalPartState.gaugeDirection
```

代わりに同じ ItemStack へ独立した鍛造進捗 root を付与する。

新規クラス:

```text
occupations/blacksmith/forging/state/
    GaugeDirection.java
    ForgingProgressState.java
    ForgingProgressStateCodec.java
    ForgingProgressStateService.java
```

root key:

```text
CraftboundForgingProgress
```

状態:

```java
public record ForgingProgressState(
    int version,
    List<Double> strikeHistory,
    double gaugeValue,
    GaugeDirection gaugeDirection
) {}
```

`CURRENT_VERSION = 1`。

NBT例:

```text
CraftboundForgingProgress: {
    Version: 1,
    StrikeHistory: [60.0d, 58.4d, 63.1d],
    GaugeValue: 72.5d,
    GaugeDirection: "down"
}
```

`strikeHistory` は順序を維持する。

各値は有限値で `0～100` の範囲とする。

`List.copyOf` 等で外部変更できない状態にする。

## 10. 鍛造進捗の初期状態

`CraftboundForgingProgress` が存在しない粗加工パーツは未鍛造として扱う。

初回セッション開始時に `skill_assists.json` の `forging_gauge` から初期値を取得する。

MVP初期値:

```text
strikeHistory = []
gaugeValue = 0
gaugeDirection = UP
```

進捗 root は、初回鍛造開始時または最初に進捗を保存する時点で作成する。

## 11. 鍛造進捗の検証

`ForgingProgressStateService` は `RoughMetalPartStateService` と組み合わせて検証する。

以下を不正とする。

- ItemStack が粗加工パーツではない
- `RoughMetalPartState` が不正
- StrikeHistory に NaN / Infinity がある
- StrikeHistory に `0～100` 外がある
- GaugeValue に NaN / Infinity がある
- GaugeValue が `0～100` 外
- GaugeDirection が未知
- 保存済み打撃回数が `effectiveBreakOnHit` 以上

最後の状態は、本来すでに即時破損しているはずなので、黙って金属塊化せず「移行不能な加工状態」として操作不能にする。

元NBTは保持する。

## 12. 鍛造台上の描画

鍛造台上の加工中パーツは `ForgingTableBlockEntityRenderer` で描画する。

`ItemRenderer` へ `workingPart` の ItemStack をそのまま渡す。

```text
RoughMetalPartItem
    ↓
既存Item Model
    ↓
既存RoughMetalPartColorHandler
    ↓
MetalVisualDataのTint
```

を再利用する。

鍛造台側でパーツ形状や金属色を再計算しない。

保留出力があり `workingPart` がない場合は、先頭の `pendingOutputs` を表示対象としてよい。

BlockEntityの表示用 ItemStack は通常の BlockEntity update packet / update tag でクライアントへ同期する。

active session、打撃履歴、破損回数などを BER 用同期データへ追加しない。

## 13. 鍛造ゲージ

新規クラス:

```text
occupations/blacksmith/forging/
    ForgingGaugeCalculator.java
```

ゲージ仕様:

```text
min = 0
max = 100
waveform = triangle
cycle = 60 game ticks
0 → 100 = 30 ticks
100 → 0 = 30 ticks
```

代表値:

```text
0 tick  → 0
15 tick → 50
30 tick → 100
45 tick → 50
60 tick → 0
```

ゲージ値は `double` の連続値として扱う。

目盛りへ丸めない。

### 13.1 毎tick値を書き換えない

BlockEntity / ItemStack の `gaugeValue` を毎tick変更しない。

active session は以下を保持する。

```text
baseServerTick
baseGaugeValue
baseDirection
```

現在値は必要な時だけ純粋計算する。

```text
ForgingGaugeCalculator.valueAt(
    baseServerTick,
    baseGaugeValue,
    baseDirection,
    targetServerTick,
    gaugeDefinition
)
```

### 13.2 任意位置からの再開

UP時:

```text
phase = (value - min) / (max - min) × 30
```

DOWN時:

```text
phase = 60 - (value - min) / (max - min) × 30
```

その phase へ経過tickを加算し、60で循環させる。

実装では `min/max/cycle_ticks` から一般化してよいが、MVPでは `triangle` だけをサポートする。

## 14. GUI終了時のゲージ保存

通常の画面終了時:

```text
サーバー側現在ゲージ値を計算
    ↓
currentGaugeValue
currentDirection
    ↓
ForgingProgressStateへ保存
    ↓
active sessionを解放
```

再度ハンマー右クリックした場合:

```text
保存済み gaugeValue / direction
    ↓
新しい baseServerTick を設定
    ↓
同じ位置・方向から再開
```

GUIを閉じている間は進行しない。

## 15. サーバー保存中のactive session

active session 自体は保存しない。

ただし `saveAdditional` が active session 中に呼ばれた場合、保存NBTへ書き込む `workingPart` のコピーについて、保存時点のサーバーtickから現在ゲージを計算し、`CraftboundForgingProgress` の gaugeValue / direction を最新化したコピーを保存する。

実行中の in-memory session はそのまま継続してよい。

サーバー再起動後:

- 加工途中 ItemStack は復元する
- 打撃履歴を復元する
- 保存時点のゲージ位置・方向を復元する
- active player を復元しない
- session ID を復元しない
- lease を復元しない

## 16. ForgingGameService

鍛造工程の状態変更を `ForgingGameService` へ集約する。

Block、BlockEntity、Menu、Screen に採点式を直接書かない。

主な責務:

```text
start / resume
strike
pause
complete
fail
pending output recovery
session validation
```

想定 API は責務の目安として以下とする。

```java
startSession(...)
acceptStrike(...)
pauseSession(...)
complete(...)
fail(...)
recoverPendingOutputs(...)
```

実際の引数型は既存コードへ合わせてよい。

## 17. ハンマー所持判定

セッション開始時だけでなく、**各打撃要求の受理時にも**サーバー側でメインハンドを確認する。

```text
mainHand == craftbound:smithing_hammer
```

を満たさない場合、打撃を拒否する。

打撃履歴、ゲージ状態、ハンマー状態を変更しない。

ハンマーを持ち直せば同じセッションまたは再開セッションから続行可能。

## 18. ハンマー耐久用 hook

新規クラス:

```text
occupations/blacksmith/forging/
    ForgingHammerDurabilityHook.java
```

想定 API:

```java
public final class ForgingHammerDurabilityHook {
    public static void onAcceptedStrike(
        ServerPlayer player,
        ItemStack hammer
    ) {
        // MVP: no-op
    }
}
```

`ForgingGameService` は、打撃がサーバーで正式に受理され、打撃履歴へ追加された後に **毎回必ずこのhookを1回だけ呼ぶ**。

MVPでは何もしない。

将来ここへ以下を追加できるようにする。

```text
バニラ耐久消費
    ↓
耐久力(Unbreaking)判定
    ↓
鍛冶師「道具保全」判定
    ↓
最終耐久消費
```

現在はハンマーに `durability(...)` を設定しない。

## 19. 鍛造評価

新規クラス:

```text
occupations/blacksmith/forging/evaluation/
    ForgingEvaluator.java
    MetalPartQualityEvaluator.java
```

### 19.1 1打の強度評価

`MetalPartDefinitionSnapshot.forging()` を使用する。

```text
strengthMin
strengthMax
strengthPenaltyPerPoint
```

適正範囲内:

```text
distance = 0
```

適正範囲外:

```text
distance = 適正範囲の最寄り端点との差
```

```text
hitScore = clamp(
    100 - distance × strengthPenaltyPerPoint,
    0,
    100
)
```

### 19.2 強度評価

```text
strikeHistoryが空
    → strengthScore = 0

1回以上
    → 全hitScoreの算術平均
```

### 19.3 打撃回数評価

```text
hitCountScore = clamp(
    100 - abs(actualHits - idealHits) × hitCountPenalty,
    0,
    100
)
```

### 19.4 鍛造評価

`evaluatorId` が以下の場合をMVPでサポートする。

```text
craftbound:weighted_forging
```

```text
forgingScore = round(
    strengthScore × strengthWeight
    + hitCountScore × hitCountWeight
)
```

最終結果を `0～100` に clamp する。

未知の evaluator ID は完成処理を失敗させる。

作業中パーツを消費しない。

## 20. 金属パーツ最終品質

`MetalPartDefinitionSnapshot.partQuality()` を使用する。

MVP evaluator:

```text
craftbound:weighted_metal_part
```

```text
finalQuality = round(
    heatingScore × heatingWeight
    + forgingScore × forgingWeight
)
```

結果を `0～100` に clamp する。

鉄製ピッケルヘッド例:

```text
heatingWeight = 0.4
forgingWeight = 0.6
```

評価式を `ForgingTableBlock`、`ForgingTableBlockEntity`、`ForgingMenu`、`ForgingScreen` に実装しない。

## 21. 完成パーツ生成

完成要求が成功した場合:

```text
workingPart
    ↓
MetalPartStateService.createFromRoughPart(workingPart)
    ↓
完成MetalPartItem
    ↓
QualityStateService.setQuality(output, finalQuality)
    ↓
品質付き完成金属パーツ
```

`MetalPartStateService` のNBTを鍛造処理から直接編集しない。

`QualityStateService#setQuality` が失敗した場合は、完成トランザクションを確定しない。

workingPart を保持したままエラーとして扱う。

## 22. 即時破損

打撃を受理し、履歴へ追加した直後に以下を判定する。

```text
strikeHistory.size() >= roughState.effectiveBreakOnHit()
```

成立した場合:

```text
即時鍛造失敗
```

完成ボタンを待たない。

鍛造評価と最終品質を計算しない。

`definitionSnapshot.failureLump()` と `metalId` を使用して失敗出力を生成する。

現在の `MetalLumpStateService` が `SMALL_METAL_LUMP` 固定APIだけを持つ場合は、以下を追加する。

```java
create(
    ResourceLocation lumpItemId,
    ResourceLocation metalId,
    int count,
    int unitsPerItem
)
```

- `lumpItemId` が `MetalLumpItem` であることを検証する
- 既存 `createSmall` は上記への wrapper として残してよい

これにより `failure_lump.item` を正しく使用する。

## 23. 完成・失敗トランザクション

完成と失敗は単一トランザクションとして扱う。

```text
入力状態を検証
    ↓
completionReserved を取得
    ↓
評価 / 出力生成
    ↓
出力をPlayer InventoryまたはpendingOutputsへ予約
    ↓
workingPartを除去
    ↓
結果hookを呼ぶ
    ↓
セッション解放
```

出力生成途中で失敗した場合:

- `workingPart` を消費しない
- `pendingOutputs` を変更しない
- セッションを破壊しない
- `completionReserved` を解除する
- GUIを閉じない
- 短いエラーだけクライアントへ返す

同じ完成要求の再送で出力を二重生成しない。

## 24. Inventory満杯時

完成品または失敗出力をプレイヤーInventoryへ全量収納できない場合:

```text
地面へドロップしない
    ↓
ForgingTableBlockEntity.pendingOutputsへ保持
```

`pendingOutputs` が存在する間:

- 新しい粗加工パーツを投入できない
- 鍛造GUIを開けない
- 空手右クリックによる全量回収だけ許可する

作業台を破壊した場合だけ通常Itemとしてドロップする。

## 25. 鍛造経験値用 hook

今回、Pufferfish's Skills への具体的な経験値付与は実装しない。

新規窓口:

```text
occupations/blacksmith/forging/
    ForgingExperienceHook.java
    ForgingExperienceResult.java
```

例:

```java
public record ForgingExperienceResult(
    UUID resultId,
    UUID operatorId,
    int materialUnits,
    boolean success,
    boolean permanentMaterialLoss
) {}
```

```java
public final class ForgingExperienceHook {
    public static void onResult(
        ServerPlayer player,
        ForgingExperienceResult result
    ) {
        // MVP: no-op
    }
}
```

成功・素材損失を伴う失敗の結果確定後に1回だけ呼ぶ。

今回:

- 経験値を増加させない
- アクションバーを表示しない
- Pufferfish API を呼ばない
- `experience_rewards.json` Loader を実装しない

後続の経験値実装ではこの窓口を置き換える。

## 26. `settings.json`

現在リソースとして未実装なので、本実装で追加する。

配置:

```text
src/main/resources/data/craftbound/blacksmith/settings.json
```

内容:

```json
{
  "schema_version": 1,
  "crucible_capacity_units": 16,
  "network": {
    "forging_latency_compensation_ticks": 4,
    "carving_packet_interval_ticks": 2,
    "carving_max_cells_per_request": 64,
    "workbench_interaction_distance": 8.0,
    "session_heartbeat_ticks": 20,
    "disconnect_grace_ticks": 100
  }
}
```

新規 Loader:

```text
occupations/blacksmith/data/
    BlacksmithSettingsDefinitions.java
    BlacksmithSettings.java
```

鍛造実装が使用する値:

```text
forging_latency_compensation_ticks = 4
workbench_interaction_distance = 8.0
session_heartbeat_ticks = 20
disconnect_grace_ticks = 100
```

クライアント申告値を使用しない。

ファイルが欠落または不正な場合は、鍛造セッション開始を拒否しログを出す。

ソースコード内へ同じ値をfallbackとして重複定義しない。

既存るつぼ実装を今回 `crucible_capacity_units` 参照へ移行する作業は対象外。

## 27. `skill_assists.json`

本実装で以下を追加する。

```text
src/main/resources/data/craftbound/blacksmith/skill_assists.json
```

既存 `IBlacksmithData` のレベル名に合わせる。

対応:

```text
precision_scale    = 精密目盛
force_reading      = 力量把握
strike_reference   = 打撃基準
precision_shaping  = 精密成形
tool_preservation  = 道具保全
material_insight   = 素材理解
smithing_instinct  = 加工感覚
quality_appraisal  = 品質鑑定
```

Pufferfish's Skills の個別ノードIDを JSON へ重複記述しない。

初期JSON:

```json
{
  "schema_version": 1,
  "forging_gauge": {
    "min": 0,
    "max": 100,
    "cycle_ticks": 60,
    "waveform": "triangle",
    "initial_value": 0,
    "initial_direction": "up",
    "base_mark_interval": 25
  },
  "precision_scale": {
    "levels": [
      { "level": 1, "mark_interval": 20 },
      { "level": 2, "mark_interval": 10 },
      { "level": 3, "mark_interval": 5 },
      { "level": 4, "mark_interval": 2 }
    ]
  },
  "force_reading": {
    "enabled_at_level": 1,
    "show_current_value": true
  },
  "strike_reference": {
    "enabled_at_level": 1,
    "max_markers": 1,
    "persistence": "session"
  },
  "precision_shaping": {
    "base_grid_size": 16,
    "base_brush_radius": 1.5,
    "levels": [
      { "level": 1, "grid_size": 24, "brush_radius": 1.0 },
      { "level": 2, "grid_size": 32, "brush_radius": 0.5 }
    ]
  },
  "tool_preservation": {
    "levels": [
      { "level": 1, "prevent_damage_chance": 0.05 },
      { "level": 2, "prevent_damage_chance": 0.10 },
      { "level": 3, "prevent_damage_chance": 0.15 },
      { "level": 4, "prevent_damage_chance": 0.20 }
    ]
  },
  "material_insight": {
    "levels": [
      { "level": 1, "display": "tools_and_methods" },
      { "level": 2, "display": "qualitative_properties" },
      { "level": 3, "display": "numeric_public_properties" }
    ],
    "rules": {
      "processing_resistance": {
        "low_at_or_above_remove_per_pass": 0.5,
        "standard_at_or_above_remove_per_pass": 0.25
      },
      "tool_load": {
        "low_at_or_above_removed_units": 8,
        "standard_at_or_above_removed_units": 4
      },
      "public_fields": [
        "tool",
        "remove_per_pass",
        "removed_units_per_durability"
      ]
    }
  },
  "smithing_instinct": {
    "levels": [
      {
        "level": 1,
        "forging_remaining_hits": 3,
        "non_metal_ideal_remaining_ratio": 0.70
      },
      {
        "level": 2,
        "forging_remaining_hits": 4,
        "non_metal_ideal_remaining_ratio": 0.75
      },
      {
        "level": 3,
        "forging_remaining_hits": 5,
        "non_metal_ideal_remaining_ratio": 0.80
      }
    ],
    "audio": {
      "sound": "minecraft:block.anvil.hit",
      "source": "blocks",
      "volume": 0.5,
      "cooldown_ticks": 20,
      "suppress_on_normal_warning": true,
      "suppress_on_failure": true,
      "pitch": {
        "caution": 0.8,
        "danger": 1.0,
        "critical": 1.2
      }
    }
  },
  "quality_appraisal": {
    "enabled_at_level": 1,
    "show_internal_quality": true
  }
}
```

本実装で鍛造処理が直接使用するのは以下。

```text
forging_gauge
precision_scale
force_reading
strike_reference
smithing_instinct.forging_remaining_hits
smithing_instinct.audio
```

`precision_shaping`、`tool_preservation`、`material_insight`、`quality_appraisal` は将来の共通利用のため読み込み・検証可能な状態にするが、鍛造ハンマー耐久や品質表示へは今回適用しない。


## 28. Skill Assist Loader / Resolver

新規:

```text
occupations/blacksmith/data/
    BlacksmithSkillAssistDefinitions.java
    BlacksmithSkillAssistDefinition.java
    BlacksmithSkillAssistResolver.java
```

`BlacksmithSkillAssistDefinitions`:

- `blacksmith/skill_assists.json` をデータパックreload対象として読む
- `schema_version` を検証する
- reload成功時に定義をatomic replaceする
- 不正時に旧データやNBTを勝手に変更しない
- 初回読込失敗時は鍛造開始を拒否する

`BlacksmithSkillAssistResolver`:

- `IBlacksmithData` の各 level を取得する
- JSON の `level` と照合する
- player level 以下で最も高い定義を採用する
- level 0 は base 値を使用する

例:

```text
getPrecisionScaleLevel() = 2
    ↓
precision_scale.level=2
    ↓
markInterval = 10
```

スキル取得状態を JSON から判定しない。

## 29. Skill Assist JSON 検証

最低限以下を検証する。

### forging_gauge

- min / max は有限
- min < max
- cycle_ticks >= 2
- MVP waveform は `triangle` のみ
- initial_value は範囲内
- initial_direction は `up` / `down`
- base_mark_interval は正の整数
- ゲージ範囲を割り切る

### precision_scale

- level は1以上
- 重複levelなし
- mark_interval は正の整数
- ゲージ範囲を割り切る
- 上位levelほど mark_interval は小さくなる

### tool_preservation

- chance は `0.0～1.0`
- 上位levelほど低下しない

### smithing_instinct

- forging_remaining_hits >= 1
- 上位levelほど通知開始が早くなるため値は増加する
- cooldown_ticks >= 1
- volume >= 0
- pitch は有限かつ正
- sound ID が登録済み SoundEvent を指す

### precision_shaping

- grid_size は上位levelほど増加
- brush_radius は正
- 上位levelほど減少

## 30. スキルのクライアント同期

クライアントへスキルレベルそのものや破損閾値を送らない。

表示に必要な解決済み情報だけ送る。

新規 DTO:

```java
public record ForgingAssistSnapshot(
    int markInterval,
    boolean showCurrentValue,
    boolean strikeReferenceEnabled
) {}
```

クライアントへ送信してよいもの:

```text
markInterval
showCurrentValue
strikeReferenceEnabled
```

送信しないもの:

```text
smithingInstinctの残り打撃閾値
effectiveBreakOnHit
idealHits
strengthMin / strengthMax
strikeHistory
forgingScore
finalQuality予測
```

加工感覚の危険判定はサーバー側だけで行う。

## 31. スキル状態のライブ更新

GUI中にスキル状態が変更された場合:

- 精密目盛 → 最新レベルの markInterval へ変更
- 力量把握喪失 → 数値表示を消す
- 打撃基準喪失 → 基準線を即時破棄
- 同GUI内で打撃基準再取得 → 以前の線は復元しない
- 加工感覚 → 次の危険判定から最新レベルを使用

`ForgingMenu` またはセッション側で前回送信した `ForgingAssistSnapshot` と現在値を比較し、変化した場合だけ同期packetを送る。

毎tick同一packetを送らない。

## 32. 操作セッション

active session は BlockEntity 内の transient 状態とする。

新規:

```text
occupations/blacksmith/forging/session/
    ForgingSessionState.java
    BlacksmithOperationSessionRegistry.java
```

`ForgingSessionState` の概念フィールド:

```text
sessionId: UUID
activePlayerId: UUID
lastSequence: long
baseServerTick: long
baseGaugeValue: double
baseGaugeDirection: GaugeDirection
lastHeartbeatTick: long
disconnectedAtTick: optional long
lastInstinctWarningTick: long
```

session ID は開始ごとに新しい UUID を生成する。

## 33. 1プレイヤー1操作セッション

`BlacksmithOperationSessionRegistry` は同じプレイヤーが複数の鍛冶作業台を同時操作しないための共通窓口とする。

現在は鍛造台だけが利用する。

新しい鍛造台でセッションを開始する場合:

```text
同プレイヤーの旧セッションなし
    → 取得

同じ鍛造台の再取得
    → 許可

別鍛造台の旧セッションあり
    → 旧セッションをpause/release
    → 新セッションを取得
```

将来、鋳造台と非金属加工台も同じ registry を利用できる構造にする。

registry 自体を保存データにしない。

サーバー再起動時は空になる。

## 34. 同時操作

同じ鍛造台について:

```text
Player A がactive
Player B が右クリック
    → 拒否
```

最初に取得したプレイヤーだけが操作できる。

通信切断猶予中も、別プレイヤーは取得できない。

同じUUIDのプレイヤーだけ再取得可能。

## 35. Heartbeat / Disconnect

`settings.json`:

```text
session_heartbeat_ticks = 20
disconnect_grace_ticks = 100
```

GUIを開いているクライアントは20tickごとに heartbeat を送信する。

サーバーは以下で即時解放する。

- 正常GUI終了
- 死亡
- 別ディメンション移動
- 鍛造台から8ブロック超過
- 同プレイヤーが別鍛冶作業台を開始

通信切断だけは100tickの猶予を持つ。

```text
切断から0～99tick
    → 同じplayer UUIDだけ再取得可能

100tick以上
    → lease解放
    → 他player取得可能
```

正常GUI終了と切断を区別するため、クライアントの通常 `onClose` では pause request を送る。

packetが届かなくても、プレイヤーがオンラインのまま `ForgingMenu` を閉じたことをサーバー側で検知した場合は即時releaseする。

プレイヤー自体がサーバーから消えている場合だけ disconnect grace とする。

## 36. Network

既存 `CraftboundNetwork` の `SimpleChannel` を再利用する。

新しいchannelを作らない。

packet追加に伴い protocol version を更新する。

```text
"2" → "3"
```

既存 packet ID と衝突しないよう、全packetを連番登録する。

### C2S

```text
ForgingStrikeRequestPacket
ForgingCompleteRequestPacket
ForgingPauseRequestPacket
ForgingHeartbeatPacket
```

### S2C

```text
ForgingSessionSyncPacket
ForgingFeedbackPacket
```

必要なら命名は既存パケット規則に合わせてよいが、責務は分ける。

## 37. C2S packet 共通フィールド

全操作packetに以下を持たせる。

```text
sessionId: UUID
sequence: long
```

client は session 内で単調増加させる。

サーバー:

```text
sequence <= lastSequence
    → reject
```

受理した要求だけ `lastSequence` を更新する。

## 38. BlockPos を操作packetへ送らない

打撃等の C2S packet に BlockPos を含めない。

サーバーは sender の現在 Menu から対象を取得する。

```text
sender.containerMenu
    ↓
ForgingMenu
    ↓
menu.blockPos
```

その後:

- `Level#hasChunkAt(blockPos)` を確認
- `ForgingTableBlockEntity` を取得
- active session ID を検証

クライアント指定の任意座標を信用しない。

## 39. 打撃packet

概念形式:

```java
record ForgingStrikeRequestPacket(
    UUID sessionId,
    long sequence,
    long estimatedServerTick
) {}
```

**strength を送信しない。**

クライアントが送るのは「いつクリックしたか」だけ。

## 40. Server tick 推定

セッション開始時、S2C sync で以下を送る。

```text
serverGameTime
baseGaugeValue
baseGaugeDirection
```

クライアントは受信時の client game time との差から server tick offset を保持する。

画面描画と打撃 request の `estimatedServerTick` に使用する。

サーバーから再syncを受けた場合は offset を更新する。

## 41. 打撃遅延補正

`settings.json`:

```text
forging_latency_compensation_ticks = 4
```

serverReceiveTick を `now`、request の tick を `requested` とする。

```text
requested > now
    → reject

0 <= now - requested <= 4
    → requested tick のゲージ値で評価

now - requested >= 5
    → 過去値を信用しない
    → now のゲージ値で評価
```

requested tick が session の開始前など計算不能な時刻の場合も拒否する。

## 42. 打撃処理順

サーバー側:

```text
sender / menu / chunk / BE を検証
sessionId を検証
sequence を検証
距離を検証
メインハンドの鍛造ハンマーを検証
requested tick を検証
評価tickを決定
ゲージ強度をサーバー側で計算
strikeHistoryへdoubleで追加
sequenceを確定
ForgingHammerDurabilityHookを1回呼ぶ
破損判定
危険通知判定
必要な状態をsetChanged
クライアントへsync
```

## 43. 完成packet

完成packetは current gauge 値を必要としない。

```java
record ForgingCompleteRequestPacket(
    UUID sessionId,
    long sequence
) {}
```

「完成」を1回押したら client は `completionPending = true` とし、結果を受け取るまで同じボタンから再送しない。

サーバー側でも `completionReserved` と sequence により二重完成を防ぐ。

## 44. Pause packet

正常なGUI終了時に送信する。

概念形式:

```java
record ForgingPauseRequestPacket(
    UUID sessionId,
    long sequence,
    long estimatedServerTick
) {}
```

4tick以内の遅延なら指定tickのゲージ状態を保存し、それより古ければ受信tickを使用する。

pause は採点を行わない。

## 45. Heartbeat packet

```java
record ForgingHeartbeatPacket(
    UUID sessionId,
    long sequence
) {}
```

受理時:

- session owner を確認
- 距離を確認
- Menu が開いていることを確認
- lastHeartbeatTick を更新
- sequence を更新

状態変更や打撃を発生させない。

## 46. S2C Session Sync

最低限以下を送る。

```text
sessionId
lastAcceptedSequence
serverGameTime
baseGaugeValue
baseGaugeDirection
ForgingAssistSnapshot
```

必要なら `status` / error reason を別packetへ分離する。

以下は送らない。

- 適正強度
- 理想打撃回数
- 正確な打撃履歴
- 正確な打撃回数
- 破損閾値
- 最終品質予測

## 47. ForgingMenu

`ForgingMenu` は加工データの保存先にしない。

責務:

- BlockPos の保持
- 対応 BE の参照解決
- `stillValid`
- session と画面の接続
- skill assist 表示snapshotの変更検出

クライアントconstructorには BlockPos を extra data で渡す。

`CraftboundMenus.FORGING_TABLE` は `IForgeMenuType.create` を使用する。

Inventory slot は不要。

```java
quickMoveStack(...) -> ItemStack.EMPTY
```

でよい。

`stillValid`:

- 対象 block が forging_table
- chunk loaded
- player と block center の距離が `settings.network.workbench_interaction_distance` 以下

を確認する。

## 48. 鍛造GUI

`ForgingScreen` は `AbstractContainerScreen<ForgingMenu>` とする。

専用GUI背景Textureは使用しない。

`GuiGraphics` の矩形、線、文字、ItemRendererで描画する。

推奨画面サイズ:

```text
imageWidth  = 240
imageHeight = 176
```

厳密なピクセル位置は描画崩れがない範囲で調整してよい。

構成:

```text
┌──────────────────────────────┐
│          鍛造中              │
│                              │
│       ┌────────────┐         │
│       │            │         │
│       │  粗パーツ   │ ←打撃領域
│       │            │         │
│       └────────────┘         │
│                              │
│ 0     25    50    75    100 │
│ │─────│─────│─────│─────│   │
│             ▲                │
│                              │
│                     [完成]   │
└──────────────────────────────┘
```

中央の ItemStack は `ItemRenderer` で拡大描画する。

## 49. 打撃領域

専用の矩形領域を持つ。

- 左クリック press 1回につき打撃要求1回
- dragで連続打撃しない
- 完成ボタンを打撃扱いしない
- ゲージを打撃扱いしない
- 余白を打撃扱いしない

client はクリック直後に軽いパーツ揺れ等の視覚演出だけを楽観実行してよい。

打撃履歴は client で確定しない。

## 50. ゲージ描画

目盛り:

```text
未取得 → 25
I       → 20
II      → 10
III     → 5
IV      → 2
```

これは `ForgingAssistSnapshot.markInterval` から描画する。

内部値は変更しない。

主要目盛り:

```text
0
25
50
75
100
```

は常に長く描画し、数字を表示する。

minor mark と重複する位置では二重描画しない。

## 51. 力量把握

`showCurrentValue == true` の場合だけ、クライアント計算した連続ゲージ値を四捨五入して整数表示する。

```text
59.6 → 60表示
```

打撃packetへ60を送らない。

server は元の連続値を計算する。

## 52. 打撃基準

`strikeReferenceEnabled == true` の場合:

- ゲージ上左クリック → 基準線を設定または移動
- 1本だけ
- ゲージ上右クリック → 削除
- server packet を送らない
- 打撃履歴を増やさない
- GUIを閉じたら破棄

スキルを失った場合は即時破棄。

同GUIで再取得しても自動復元しない。

## 53. 正解情報を表示しない

GUIには以下を表示しない。

- strengthMin / strengthMax
- idealHits
- breakOnHit
- effectiveBreakOnHit
- strikeHistory
- exact hit count
- forgingScore
- finalQuality prediction

## 54. 通常危険通知

打撃受理後、破損しなかった場合に

```text
remainingHits = effectiveBreakOnHit - strikeHistory.size()
```

をサーバーで計算する。

```text
remainingHits <= 2
```

なら通常危険通知。

- 鈍い打撃音
- 既存パーティクル

を使用する。

専用ひびTextureは作らない。

既存パーティクルは専用画像を必要としないものを使用する。

## 55. 加工感覚

`IBlacksmithData#getSmithingInstinctLevel()` と `skill_assists.json` をサーバーで解決する。

レベルごとの初期閾値:

```text
I   → remainingHits <= 3
II  → remainingHits <= 4
III → remainingHits <= 5
```

通常危険通知 `remainingHits <= 2` より前に補助通知できる。

判定:

```text
打撃によって初めてスキル閾値内へ入った
    OR
閾値内でさらに危険な打撃を行った
```

かつ

```text
前回加工感覚音から cooldown_ticks 以上
```

の場合だけ通知する。

時間経過だけでは再生しない。

通常危険通知または破損が同じ打撃で起きる場合は、加工感覚音を抑止する。

## 56. 音

新規 `.ogg` を作成しない。

MVPではバニラSoundEventだけを利用する。

基本候補:

```text
通常打撃       → minecraft:block.anvil.hit
危険打撃       → minecraft:block.anvil.hit の低めpitch
破損           → minecraft:block.anvil.destroy
完成           → minecraft:block.anvil.use
加工感覚       → skill_assists.json の minecraft:block.anvil.hit
```

Javaでは可能な限り登録済み SoundEvent を解決して利用する。

通常打撃、危険打撃、破損、完成は鍛造台位置を音源とし `SoundSource.BLOCKS` を使用する。

加工感覚だけは操作中プレイヤー本人へ個別送信し、クライアントで local sound として再生する。

周囲へ加工感覚音を送らない。

今回、独自字幕 `加工物が軋む` のための custom SoundEvent / sounds.json / `.ogg` は作成しない。

独自音声化する場合は将来差し替える。

## 57. 完成時の画面動作

完成成功:

```text
完成品またはpending outputを確定
    ↓
完成音
    ↓
GUIを閉じる
```

完成処理内部エラー:

```text
workingPart維持
    ↓
短いエラー表示
    ↓
GUIを維持
```

## 58. 破損時の画面動作

即時破損:

```text
failure output確定
    ↓
破損音
    ↓
GUIを閉じる
```

Inventory満杯なら failure output を pendingOutputs へ保持する。

## 59. BlockEntity破壊

通常破壊、爆発、クリエイティブ破壊で同じ内容物処理を使用する。

active session 中なら、破壊tick時点のゲージをまず `workingPart` へsnapshotする。

その後:

```text
workingPart が存在
    → 状態付きItemStackを1回だけdrop

pendingOutputs が存在
    → 全出力を通常Itemとしてdrop
```

active sessionを解放する。

設備Block自身のdropは通常規則に従う。

BlockStateの facing 変更等、同じ Block の state transition では内容物をdropしない。

## 60. 外部自動化

鍛造台は加工対象・出力に対する外部ItemHandlerを公開しない。

以下を不可とする。

- hopper投入
- hopper回収
- Forge item capability 経由の投入・回収
- Create 等の外部搬送MODからの操作

BER、Menu、内部ロジック用の ItemStack 保持と外部Capabilityを分ける。

## 61. BlockEntity client sync

BER表示に必要な最小状態だけ同期する。

最低限:

```text
displayStack
```

`workingPart` があればそれを表示。

なければ pending output の先頭を表示してよい。

`getUpdateTag` / `getUpdatePacket` 等、Forge/Minecraft標準の BlockEntity 同期方法を利用する。

内容物変更時は `setChanged()` と block update 通知を行う。

session packet と world rendering packet を混同しない。

# 採用した実装方針

- ハンマーは通常 Item として登録し、MVPでは耐久なし
- ハンマー耐久処理位置だけ no-op hook として確保する
- 鍛造台は BlockEntity を使用する
- 作業中パーツと保留出力は BlockEntity へ保存する
- 鍛造進捗は `RoughMetalPartState` へ埋め込まず、同じ ItemStack の独立rootへ保存する
- active sessionは永続化しない
- ゲージを毎tickNBTへ書かず、基準tick・値・方向から純粋計算する
- pause時だけゲージ値と方向を進捗状態へ確定する
- 打撃強度をclientから送らず、server tickからサーバーで計算する
- 4tickまで過去時刻を補正する
- packetに任意BlockPosを含めず、現在Menuから対象BEを特定する
- session ID + monotonic sequenceで再送を拒否する
- 1台につき1人だけ操作可能
- 同一プレイヤーは1鍛冶操作セッションだけ保持する
- 完成・失敗は単一トランザクションで確定する
- 完成品は既存 `MetalPartStateService` で生成する
- 最終品質は独立 `QualityStateService` で付与する
- 失敗金属塊は `MetalLumpStateService` を利用する
- 評価式は専用Evaluatorへ分離する
- スキル効果値をJSONへ分離する
- スキル取得状態は既存 `IBlacksmithData` を正とする
- GUIは表示・入力送信のみ
- GUI専用背景PNGは使用しない
- 鍛造台上パーツは ItemRenderer を再利用する
- バニラSoundEventだけを使用する
- 経験値は no-op hook だけ作る

# 採用しなかった方針

## ゲージ値をBlockEntityで毎tick更新する

保存・同期回数が不要に増え、遅延補正にも不利なため採用しない。

## clientから打撃強度を送る

改ざん可能になるため採用しない。

## `RoughMetalPartState` へ打撃履歴を直接追加する

鋳造由来の確定状態と鍛造進捗を分離し、既存状態versionを変更しないため採用しない。

## `MetalPartState` へ品質を追加する

品質は非金属パーツと完成装備でも使用するため、共通 `QualityState` を使う。

## ハンマーへ今すぐ耐久値を設定する

MVP仕様と異なるため採用しない。

## 耐久hook自体を作らない

将来の耐久追加時に打撃処理へ直接差し込む必要が出るため、no-op hookだけ先に用意する。

## 鍛造経験値を今回実装する

別実装とし、今回の工程には no-op hook だけを設ける。

## Pufferfishのskill IDをskill_assists.jsonへ記述する

既存 `IBlacksmithData` がスキルレベルを保持しており、IDを二重管理することになるため採用しない。

## DataSlotだけで全GUI状態を同期する

UUID、long tick、double gauge、複数boolean等を扱うには不向きなため、custom packetを使用する。

## GUI背景専用Texture

初期実装コストを増やすため採用しない。

## 独自Sound `.ogg`

MVPではバニラ音源で成立するため採用しない。

## 外部ItemHandler公開

加工状態の排他性と複製防止を壊すため採用しない。

# 関連する既存クラス

## `CraftboundItems`

以下を追加する。

```text
SMITHING_HAMMER
FORGING_TABLE BlockItem
```

既存の粗加工パーツ・完成金属パーツ登録は変更しない。

## `CraftboundBlocks`

`FORGING_TABLE` を追加する。

## `CraftboundBlockEntities`

`FORGING_TABLE` を追加する。

## `CraftboundMenus`

`FORGING_TABLE` を `IForgeMenuType.create` で追加する。

## `CraftboundClientEvents`

以下を追加する。

```text
ForgingMenu → ForgingScreen
ForgingTableBlockEntity → ForgingTableBlockEntityRenderer
```

Screenは既存の `SCREENS` 一覧方式へ追加する。

BERは Forge の renderer registration event で登録する。

## `CraftboundDataReloadEventHandler`

以下をreload listenerへ追加する。

```text
BlacksmithSettingsDefinitions
BlacksmithSkillAssistDefinitions
```

## `CraftboundNetwork`

既存 SimpleChannel へ鍛造packetを追加する。

protocol version を更新する。

## `RoughMetalPartState`

変更しない。

以下を利用する。

```text
metalId
ingredientCount
heatingScore
effectiveBreakOnHit
definitionSnapshot
visualData
```

## `RoughMetalPartStateService`

粗加工パーツの正当性判定に使用する。

## `MetalPartDefinitionSnapshot`

以下を使用する。

```text
outputItemId
failureLump
forging
partQuality
```

鍛造開始後もこのsnapshotを正とし、MetalPartDefinitionsを再検索しない。

## `MetalPartStateService`

完成パーツ生成の唯一の窓口として再利用する。

NBTを鍛造処理から直接編集しない。

## `MetalLumpStateService`

失敗出力生成に利用する。

必要なら generic lump item ID を受け取るAPIを追加する。

## `IBlacksmithData`

以下を使用する。

```text
getPrecisionScaleLevel()
hasForceReadingLevel()
hasStrikeReferenceLevel()
getSmithingInstinctLevel()
```

JSON側でPufferfish skill IDを再判定しない。

## `QualityStateService`

鍛造完了時に最終品質を完成パーツへ付与する。

# データフロー

## 粗加工パーツ設置

```text
Player main hand
    RoughMetalPartItem
        ↓
ForgingTableBlock#use
        ↓
RoughMetalPartStateService.read
        ↓ valid
ForgingTableBlockEntity.workingPart
        ↓
client BE sync
        ↓
ForgingTableBlockEntityRenderer
        ↓
ItemRenderer
```

## セッション開始

```text
Player holds smithing_hammer
    ↓ right click
ForgingTableBlock
    ↓
ForgingGameService.startSession
    ↓
BlacksmithOperationSessionRegistry.acquire
    ↓
ForgingSessionState
    ↓
ServerPlayer open menu
    ↓
ForgingMenu
    ↓
ForgingSessionSyncPacket
    ↓
ForgingScreen
```

## 打撃

```text
ForgingScreen strike area left click
    ↓
ForgingStrikeRequestPacket
    sessionId
    sequence
    estimatedServerTick
    ↓
Server
    ↓
現在ForgingMenuからBlockPos取得
    ↓
session / sequence / distance / hammer検証
    ↓
ForgingGaugeCalculator
    ↓
server authoritative strength
    ↓
ForgingProgressState.strikeHistoryへ追加
    ↓
ForgingHammerDurabilityHook(no-op)
    ↓
破損判定
    ├─ 破損 → failure transaction
    └─ 継続 → warning判定 + sync
```

## 完成

```text
ForgingScreen [完成]
    ↓
ForgingCompleteRequestPacket
    ↓
ForgingGameService.complete
    ↓
completion reserve
    ↓
ForgingEvaluator
    strikeHistory
    definitionSnapshot.forging
    ↓
forgingScore
    ↓
MetalPartQualityEvaluator
    heatingScore
    forgingScore
    definitionSnapshot.partQuality
    ↓
finalQuality
    ↓
MetalPartStateService.createFromRoughPart
    ↓
completedMetalPart
    ↓
QualityStateService.setQuality
    ↓
quality付きcompletedMetalPart
    ↓
Inventory or pendingOutputs
    ↓
ForgingExperienceHook(no-op)
    ↓
session release
```

## GUI中断

```text
Screen close
    ↓
ForgingPauseRequestPacket
    ↓
server authoritative gauge snapshot
    ↓
ForgingProgressStateへ
    gaugeValue
    gaugeDirection
    ↓
session release
    ↓
workingPartは鍛造台へ残る
```

## 鍛造台破壊

```text
Block remove
    ↓
activeなら現在ゲージsnapshot
    ↓
session release
    ↓
workingPart drop
pendingOutputs drop
```

# 実装手順

## Step 1: ハンマー・鍛造台・BlockEntity基盤

読む対象:

```text
CraftboundItems.java
CraftboundBlocks.java
CraftboundBlockEntities.java
CraftboundMenus.java
BlacksmithFurnaceBlock.java
BlacksmithFurnaceBlockEntity.java
RoughMetalPartStateService.java
```

実施:

1. `SMITHING_HAMMER` を最大スタック1、耐久なしで登録する
2. `FORGING_TABLE` Block / BlockItem を登録する
3. `ForgingTableBlock` を実装する
4. 水平 `FACING` を実装する
5. piston移動不可にする
6. `ForgingTableBlockEntity` を実装する
7. `workingPart` と `pendingOutputs` の保存・読込を実装する
8. 粗加工パーツの挿入、空手回収、pending output回収の基本操作を実装する
9. 外部ItemHandlerを公開しない
10. 破壊時内容物dropの基盤を実装する
11. 以下JSONを追加する

```text
assets/craftbound/models/item/smithing_hammer.json
assets/craftbound/blockstates/forging_table.json
assets/craftbound/models/block/forging_table.json
assets/craftbound/models/item/forging_table.json
data/craftbound/recipes/blacksmith/equipment/smithing_hammer.json
data/craftbound/recipes/blacksmith/equipment/forging_table.json
```

12. 翻訳キーを追加する

```text
item.craftbound.smithing_hammer
block.craftbound.forging_table
container.craftbound.forging_table
```

Texture PNGは作成しない。

以下が既に存在する前提でJSON参照だけ作る。

```text
textures/item/smithing_hammer.png
textures/block/forging_table_top.png
textures/block/forging_table_side.png
textures/block/forging_table_front.png
```

このStepでは鍛造GUIと採点処理を実装しない。

## Step 2: 鍛造進捗・ゲージ・評価・結果生成

読む対象:

```text
RoughMetalPartState.java
RoughMetalPartStateService.java
MetalPartDefinition.java
MetalPartDefinitionSnapshot.java
MetalPartStateService.java
MetalLumpStateService.java
QualityStateService.java
```

新規実装:

```text
forging/state/GaugeDirection.java
forging/state/ForgingProgressState.java
forging/state/ForgingProgressStateCodec.java
forging/state/ForgingProgressStateService.java
forging/ForgingGaugeCalculator.java
forging/evaluation/ForgingEvaluator.java
forging/evaluation/MetalPartQualityEvaluator.java
forging/ForgingGameService.java
forging/ForgingHammerDurabilityHook.java
forging/ForgingExperienceHook.java
forging/ForgingExperienceResult.java
```

実施:

1. `CraftboundForgingProgress` root を実装する
2. 打撃履歴、ゲージ値、方向を保存できるようにする
3. `RoughMetalPartState` は変更しない
4. 0→100→0 / 60tick の純粋ゲージ計算を実装する
5. 任意の値・方向から再開できるようにする
6. `craftbound:weighted_forging` 評価を実装する
7. `craftbound:weighted_metal_part` 評価を実装する
8. 即時破損判定を実装する
9. `failure_lump` に従う失敗出力生成を実装する
10. 完成パーツを `MetalPartStateService` で生成する
11. `QualityStateService` で最終品質を付与する
12. Inventory満杯時の `pendingOutputs` を実装する
13. 完成・失敗の排他予約を実装する
14. ハンマー耐久hookを受理打撃ごとに1回呼び、現在はno-opとする
15. 経験値hookを結果確定時に1回呼び、現在はno-opとする

このStepではclient GUIを実装しない。

Server側の単体ロジックだけで以下を満たす状態にする。

```text
0,15,30,45,60tick → 0,50,100,50,0
strength60 × 6 → forgingScore100
strength60 × 5 → forgingScore96
strength50 × 6 → forgingScore80
0 hit → forgingScore0
```

## Step 3: JSON定義・セッション・Network・Menu

読む対象:

```text
IBlacksmithData.java
BlacksmithData.java
CraftboundCapabilities.java
CraftboundDataReloadEventHandler.java
CraftboundNetwork.java
CraftboundMenus.java
```

新規実装:

```text
blacksmith/data/BlacksmithSettings.java
blacksmith/data/BlacksmithSettingsDefinitions.java
blacksmith/data/BlacksmithSkillAssistDefinition.java
blacksmith/data/BlacksmithSkillAssistDefinitions.java
blacksmith/data/BlacksmithSkillAssistResolver.java
forging/session/ForgingSessionState.java
forging/session/BlacksmithOperationSessionRegistry.java
forging/menu/ForgingMenu.java
network/packet/ForgingStrikeRequestPacket.java
network/packet/ForgingCompleteRequestPacket.java
network/packet/ForgingPauseRequestPacket.java
network/packet/ForgingHeartbeatPacket.java
network/packet/ForgingSessionSyncPacket.java
network/packet/ForgingFeedbackPacket.java
```

Resource:

```text
data/craftbound/blacksmith/settings.json
data/craftbound/blacksmith/skill_assists.json
```

実施:

1. settings Loaderを登録する
2. skill assists Loaderを登録する
3. Javaスキルレベル → JSON効果値のResolverを実装する
4. ForgingAssistSnapshotを実装する
5. playerごとのactive session registryを実装する
6. 同一台の排他操作を実装する
7. 同一プレイヤーの旧セッション解放を実装する
8. heartbeat 20tickを実装する
9. disconnect grace 100tickを実装する
10. 8ブロック距離制限を実装する
11. `ForgingMenu` を実装する
12. client constructorへBlockPosを渡す
13. `CraftboundNetwork` protocolを `3` へ上げる
14. session ID + monotonic sequence を全C2Sに実装する
15. BlockPosをpacketに含めずMenuから解決する
16. 4tickの打撃遅延補正を実装する
17. server authoritative strength を実装する
18. skill assistの変更時だけS2C syncする
19. future tick / duplicate sequence / invalid sessionを拒否する

このStep終了時点で、GUI表示が未完成でも packet と server session の往復が成立すること。

## Step 4: Screen・BER・音・中断再開・統合

読む対象:

```text
CraftboundClientEvents.java
RoughMetalPartColorHandler.java
MetalRenderColorResolver.java
ForgingMenu.java
ForgingTableBlockEntity.java
```

実施:

1. `ForgingScreen` を実装する
2. GuiGraphicsだけで背景、枠、ゲージ、目盛り、完成ボタンを描画する
3. ItemRendererで中央の粗加工パーツを描画する
4. 専用打撃領域の左クリックだけを打撃packetへ変換する
5. client側server tick推定を実装する
6. 力量把握の整数表示を実装する
7. 精密目盛のminor/major markを実装する
8. 打撃基準のclient-only線を実装する
9. skill assistのライブ変更を反映する
10. 20tick heartbeatを送信する
11. 通常closeでpause packetを送信する
12. `ForgingTableBlockEntityRenderer` を実装する
13. ItemRendererで鍛造台上のItemStackを描画する
14. BERをclient eventへ登録する
15. `ForgingScreen` を既存SCREENSへ登録する
16. 通常打撃、危険、完成、破損にバニラSoundEventを適用する
17. 加工感覚をoperator-only local soundとして実装する
18. 危険時に既存particleを併用する
19. GUI終了、死亡、距離超過、dimension移動のsession releaseを完成させる
20. Block破壊時にactive gaugeをsnapshotして状態付きpartをdropする
21. server restart時に加工状態だけ復元しoperation leaseを復元しないことを確認する
22. clientへ正解値・破損閾値・履歴を表示しない
23. PNG生成処理を追加していないことを確認する

このStepで鍛造台・ハンマー・鍛造ミニゲームを一体として完成させる。

# 擬似コード

```text
鍛造台右クリック:
    サーバー側のみ状態変更する

    pendingOutputsがある:
        メインハンドが空でなければ拒否
        全出力がInventoryへ入るか確認
        入らなければ拒否
        全量を移動
        pendingOutputsを空にする
        更新通知
        終了

    workingPartが空:
        held itemが有効な粗加工パーツでなければ何もしない
        不正なForgingProgress rootがあれば拒否
        held itemをworkingPartへ移動
        更新通知
        終了

    held itemがsmithing_hammer:
        ForgingGameServiceへstart/resumeを要求
        成功した場合だけMenuを開く
        終了

    held itemが空 && active sessionなし:
        workingPartをplayer main handへ返す
        workingPartを空にする
        更新通知
```

```text
鍛造セッション開始:
    settingsとskill_assistsが有効か確認
    workingPartのRoughMetalPartStateを確認
    ForgingProgressStateを確認
    pendingOutputsがないことを確認
    playerが8ブロック以内か確認
    smithing_hammerを持っているか確認
    他playerのactive sessionがないか確認

    playerが別の鍛冶sessionを持っていれば旧sessionをpause/release

    progressがなければ初期progressを作る

    sessionIdを生成
    baseServerTick = server game time
    baseGaugeValue = progress.gaugeValue
    baseDirection = progress.direction
    lastSequenceを初期化
    heartbeat時刻を保存

    operation registryへ登録
    Menuを開く
    SessionSyncを送る
```

```text
打撃request:
    senderの現在MenuがForgingMenuか確認
    menu位置のchunkがloadedか確認
    ForgingTableBlockEntityを取得
    sessionId確認
    sequence確認
    distance確認
    hammer確認

    requestedTick > nowなら拒否

    now-requestedTick <= latencyCompensation:
        targetTick = requestedTick
    それ以外:
        targetTick = now

    strength = ForgingGaugeCalculator.valueAt(targetTick)

    progress.strikeHistoryへstrength追加
    ForgingHammerDurabilityHookを呼ぶ

    hitCount >= effectiveBreakOnHit:
        即時failure処理
        終了

    remainingHitsを計算

    remainingHits <= 2:
        通常危険通知
    それ以外でsmithingInstinct閾値内:
        cooldownを満たす場合だけoperatorへ補助音

    state sync
```

```text
完成request:
    session / sequence / distance / playerを検証
    completion reservationを取得

    roughStateを読む
    progressを読む

    forgingScore = ForgingEvaluator.evaluate(
        progress.strikeHistory,
        roughState.definitionSnapshot.forging
    )

    finalQuality = MetalPartQualityEvaluator.evaluate(
        roughState.heatingScore,
        forgingScore,
        roughState.definitionSnapshot.partQuality
    )

    output = MetalPartStateService.createFromRoughPart(workingPart)
    生成失敗ならreservation解除して終了

    QualityStateService.setQuality(output, finalQuality)
    失敗ならreservation解除して終了

    outputをInventoryへ全量収納可能:
        Inventoryへ入れる
    それ以外:
        pendingOutputsへ保存

    workingPartを空にする

    ForgingExperienceHookを1回呼ぶ
    active sessionを解放
    完成音
    GUIを閉じる
```

```text
即時破損:
    completion reservationを取得

    snapshot.failureLumpを読む
    MetalLumpStateServiceでfailure outputを生成

    生成失敗:
        workingPartを保持
        reservation解除
        エラー
        終了

    Inventoryへ入る:
        failure outputを収納
    入らない:
        pendingOutputsへ保存

    workingPartを空にする
    ForgingExperienceHook(success=false, permanentMaterialLoss=true)を呼ぶ
    session解放
    破損音
    GUIを閉じる
```

```text
GUI pause:
    sessionを検証
    requested close tickを検証
    保存に使うtargetTickを決める
    targetTick時点のgaugeValue / directionを計算
    ForgingProgressStateへ保存
    session解放
    採点しない
```

```text
鍛造台破壊:
    workingPartがありactive sessionなら
        破壊tick時点のgaugeValue / directionをworkingPartへ保存

    session解放

    workingPartがあればdrop
    pendingOutputsを全drop
    BlockEntity内容物をclear
```

# 境界条件・例外

## Item / Block

- ハンマーは最大スタック1
- ハンマーはMVPで耐久なし
- ハンマーTextureがコード側で生成されない
- 鍛造台Textureがコード側で生成されない
- 鍛造台はピストン移動不可
- block state rotationだけで内容物をdropしない

## 粗加工パーツ

- `RoughMetalPartStateService.read` 失敗品を鍛造しない
- 別ItemへNBTだけコピーした偽装粗加工パーツを受け付けない
- `definitionSnapshot` を正とする
- 鍛造中に metal part JSON がreloadされても進行中partのsnapshotを維持する
- `ForgingProgress` がないpartは未鍛造
- `ForgingProgress` rootがあるが不正なら自動初期化しない

## ゲージ

- 0と100を含む
- doubleで計算する
- minor markへ丸めない
- GUIを閉じている間は進まない
- server停止中は進まない
- active sessionをrestart後に復元しない

## 打撃

- 打撃領域以外をクリックしても履歴を増やさない
- 完成ボタンで打撃を追加しない
- ゲージクリックで打撃を追加しない
- hammerを持っていない打撃は拒否
- duplicate sequenceは拒否
- future tickは拒否
- 5tick以上古いtickはserver受信tickへfallback
- 破損打撃では鍛造評価を計算しない

## Session

- 他player操作中は開けない
- 同playerは別鍛冶作業台開始時に旧sessionを解放する
- GUI close / death / distance / dimension changeは即時release
- disconnectだけ100tick grace
- heartbeatでlease維持
- server restartでleaseを復元しない

## 完成

- 0打撃完成を許可する
- 0打撃の forgingScore は0
- qualityは0～100
- output生成失敗時にworkingPartを消費しない
- QualityState書込失敗時にworkingPartを消費しない
- 同じcomplete requestで二重生成しない
- Inventory満杯で地面へ投棄しない

## 破損

- `hitCount >= effectiveBreakOnHit` で即時
- completionを待たない
- forgingScoreを計算しない
- finalQualityを計算しない
- failure output生成失敗時にworkingPartを黙って削除しない

## 保存

- workingPartを永続化
- pendingOutputsを永続化
- active playerを永続化しない
- sessionIdを永続化しない
- leaseを永続化しない
- invalid stateを黙って初期化しない

## Client

- server authoritative状態だけを確定表示に使う
- clientは正解範囲を知らなくてよい
- exact history/countを画面へ表示しない
- strike referenceはclient-only
- processing instinct閾値をclientへ送らない

# 完了条件

- `craftbound:smithing_hammer` をクラフトできる
- `craftbound:forging_table` をクラフト・設置できる
- 鍛造台が向きを持ち、Texture JSONが用意済みPNGを参照する
- ハンマーModel JSONが用意済みPNGを参照する
- 実装処理がPNGを生成していない
- 粗加工パーツを鍛造台へ設置できる
- 鍛造台上に既存Tint込みのItemStackが描画される
- 空手で状態付き粗加工パーツを回収できる
- ハンマー右クリックで鍛造GUIを開始できる
- 同じ台を2人で同時操作できない
- ゲージが0→100→0を60tickで往復する
- 0/15/30/45/60tickで0/50/100/50/0になる
- GUI close後に同じ値・方向から再開する
- clientからstrengthを送らずserverで計算する
- 4tick以内の打撃遅延補正が働く
- 未来tickを拒否する
- duplicate sequenceを拒否する
- 8ブロック超過で操作を終了する
- heartbeatを20tickごとに処理する
- disconnectから100tickでleaseを解放する
- 精密目盛が25/20/10/5/2で変化する
- 目盛りによって内部ゲージ値が変化しない
- 力量把握時だけ現在値を整数表示する
- 打撃基準を1本設定・削除でき、GUI closeで消える
- 加工感覚がレベル別閾値でserver判定される
- 通常危険状態で鈍い音と既存particleが出る
- 新規 `.ogg` を必要としない
- 通常打撃にバニラ音を利用する
- 破損にバニラ音を利用する
- 完成にバニラ音を利用する
- 強度60×6で鍛造評価100
- 強度60×5で鍛造評価96
- 強度50×6で鍛造評価80
- 0打撃完成で鍛造評価0
- `effectiveBreakOnHit` 到達で即時破損する
- 破損時に定義済みfailure lumpを生成する
- 完成時に `MetalPartStateService` で完成金属パーツを生成する
- 完成金属パーツへ `QualityStateService` で最終品質を付与する
- `MetalPartState` 自体へqualityを追加していない
- ハンマー耐久hookが受理打撃ごとに1回呼ばれるが、MVPでは耐久が減らない
- 経験値hookが結果確定時に呼ばれるが、MVPでは経験値が増えない
- Inventory満杯時にoutputを鍛造台へ保持する
- pending outputを全量一括回収できる
- pending output中に新規加工を開始できない
- 鍛造台破壊時に加工途中パーツを状態付きで1回だけdropする
- 破壊時にpending outputも1回だけdropする
- hopper等へ加工Inventoryを公開しない
- server restart後に加工状態を復元し、operation leaseは復元しない

# 今回の対象外

- ハンマー耐久値の実消費
- ハンマーへの Unbreaking / Mending 対応
- ハンマーへの道具保全適用
- Pufferfish's Skills への鍛造経験値付与
- `experience_rewards.json` Loader
- 経験値アクションバー表示
- 品質段階名表示
- 品質鑑定による品質数値表示
- `blacksmith/quality.json` Loader
- 完成パーツ品質による性能補正
- 完成非金属パーツ
- 非金属加工台
- 完成装備組み立て
- 金床、砥石、鍛冶台の品質継承
- 鋳造台実装
- 独自SoundEvent用 `.ogg`
- 独自 `processing_strain` 音声
- GUI専用背景Texture
- 専用ひびTexture
- ハンマーTexture生成
- 鍛造台Texture生成
- Texture修正
