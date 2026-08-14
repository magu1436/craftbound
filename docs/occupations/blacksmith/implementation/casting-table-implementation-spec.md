# 鋳造台 実装仕様書

# 目的

Minecraft Forge 1.20.1 の Craftbound に、鍛冶師用ブロック `craftbound:casting_table` を実装する。

鋳造台は以下だけを担当する。

```text
鋳型を設置
    ↓
加熱済みるつぼから必要量の金属を流し込む
    ↓
加熱結果を確定
    ↓
ロード中だけ冷却
    ↓
取り出し時の冷却状態を確定
    ↓
既存 RoughMetalPartStateService で粗加工パーツを生成
```

状態変更・素材消費・評価結果はサーバー側を正とする。

本書を鋳造台実装で最初に読む唯一の実装方針仕様書とする。
本書に必要なゲームルールを含めるため、実装のために過去の仕様書・他ブランチ・チャット履歴を読まない。

---

## Codexの読み込み規則

- 最初に本書及び `AGENTS.md` だけを読む。
- リポジトリ全体を先に走査しない。
- 各実装ステップに記載したファイルだけを、そのステップ開始時に読む。
- 記載したクラス・メソッドが見つからない場合だけ、**シンボル名を指定して限定検索**する。
- `docs/`、Git履歴、他ブランチ、他職業コードは読まない。
- 既存クラスに目的のAPIがある場合は再実装しない。
- 既存のNBT Codec / State Serviceを迂回してNBTを直接編集しない。

---

# 確定した仕様

## 1. 追加する主要クラス

```text
occupations/blacksmith/casting/
├─ CastingTableBlock.java
├─ CastingTableBlockEntity.java
├─ CastingGameService.java
├─ CastingProcess.java
├─ CastingProcessCodec.java
└─ CoolingBreakLimitService.java

occupations/blacksmith/client/
└─ CastingTableRenderer.java
```

必要なら、処理結果を表す小さなenumまたはrecordを `casting/` 直下へ追加してよい。

GUI、Menu、Screenは追加しない。

---

## 2. ブロック

ID:

```text
craftbound:casting_table
```

Block Entity IDも同じ。

`CastingTableBlock` は `BaseEntityBlock` を継承する。

BlockState:

```java
BlockStateProperties.HORIZONTAL_FACING
```

配置方向・rotate・mirrorは既存 `BlacksmithFurnaceBlock` と同じ形式にする。

その他:

- `RenderShape.MODEL`
- サーバー側tickerのみ
- ピストン移動不可
- 外部ItemHandlerを公開しない
- ホッパー等から鋳型・出力を操作できない

---

## 3. Block Entityの状態

`CastingTableBlockEntity` は以下を正本として保持する。

```java
private ItemStack mold = ItemStack.EMPTY;

@Nullable
private CastingProcess activeProcess;

private ItemStack pendingOutput = ItemStack.EMPTY;

private boolean invalidStoredState;
private boolean transactionInProgress;
```

意味:

```text
mold
    設置済み鋳型1個

activeProcess
    流し込み済み・冷却中の工程

pendingOutput
    プレイヤーへまだ渡していない確定済み出力

invalidStoredState
    保存データが復元不能

transactionInProgress
    同一操作中の再入防止。NBT保存しない
```

導出状態:

```text
EMPTY
    moldなし / processなし / pendingなし

MOLD_INSTALLED
    moldあり / processなし / pendingなし

COOLING
    moldあり / processあり / pendingなし

OUTPUT_PENDING
    processなし / pendingあり

INVALID
    invalidStoredState
```

次は不正状態。

- `activeProcess != null` なのに `mold` が空
- `activeProcess` と `pendingOutput` が同時に存在
- 実際の鋳型IDとprocess snapshotの `moldItemId` が不一致
- process、mold、pendingOutputの保存データが不正
- 未対応version

不正状態を空状態へ自動リセットしない。

---

## 4. 鋳型

鋳型はBlock Entityが直接 `ItemStack` 1個を保持する。

```java
private ItemStack mold;
```

`ItemStackHandler`、`Container`、`WorldlyContainer` は使用しない。

有効な鋳型判定には既存の

```java
MetalPartDefinitions.INSTANCE.isRegisteredMold(stack)
```

を使用する。

空鋳型は設置不可。

### 設置

条件:

```text
moldなし
activeProcessなし
pendingOutputなし
手持ちが登録済み鋳型
```

成功時はメインハンドから鋳型1個を実際に移動する。

### 回収

条件:

```text
moldあり
activeProcessなし
pendingOutputなし
メインハンドが空
スニーク中
```

成功時はmoldをメインハンドへ移動する。

---

## 5. 右クリック優先順位

メインハンドだけ処理する。

サーバー側:

```text
1. pendingOutputあり + 空手
   → pendingOutputを全量回収

2. activeProcessあり + 空手
   → 現在の冷却状態で粗加工パーツを確定して回収
   → スニーク中でもこちらを優先

3. processなし + pendingなし + moldあり + CrucibleItem所持
   → 流し込み

4. processなし + pendingなし + moldなし + 登録済み鋳型所持
   → 鋳型設置

5. processなし + pendingなし + moldあり + 空手 + スニーク
   → 鋳型回収

6. その他
   → PASS
```

クライアント側は操作候補なら `SUCCESS` を返してよいが、状態変更は行わない。

---

## 6. CastingProcess

鋳造開始時の状態を以下へまとめる。

```java
public record CastingProcess(
    int version,
    UUID processId,
    UUID operatorId,
    ResourceLocation partDefinitionId,
    ResourceLocation metalId,
    int metalAmount,
    long heatingTicks,
    int heatingScore,
    long coolingTicks,
    boolean surfaceSolidificationNotified,
    MetalPartDefinitionSnapshot definitionSnapshot,
    MetalVisualData visualData
) {
    public static final int CURRENT_VERSION = 1;
}
```

不変条件:

```text
version == CURRENT_VERSION
metalAmount >= 1
heatingTicks >= 0
0 <= heatingScore <= 100
coolingTicks >= 0

partDefinitionId == snapshot.definitionId
metalId == snapshot.metalId
metalAmount == snapshot.ingredientCount
```

さらに、processを保持するBlock Entityでは、

```text
実際のmold Item ID == snapshot.moldItemId
```

を要求する。

`definitionSnapshot` は既存 `MetalPartDefinition#snapshot()` を使用する。
開始済みprocessは `/reload` 後もsnapshotの値で続行する。

---

## 7. るつぼからの流し込み

流し込みは `CastingGameService` が担当する。

```java
public static CastingActionResult tryPour(
    ServerPlayer player,
    CastingTableBlockEntity table,
    ItemStack crucible
)
```

鋳造台はるつぼNBTを直接変更しない。

読み取り:

```java
CrucibleStateService.read(crucible)
```

消費:

```java
CrucibleStateService.consumeForCasting(
    crucible,
    definition.ingredientCount()
)
```

るつぼが溶鉱炉から取り出された時点で、pending heatingは既存溶鉱炉側から `CrucibleState.heatingTicks` へcommit済みとして扱う。

---

## 8. 流し込み前検証

**素材消費前にすべて検証する。**

順序:

```text
1. tableがinvalidでない
2. transaction中でない
3. activeProcessなし
4. pendingOutputなし
5. moldあり
6. 手持ちがCrucibleItem
7. CrucibleStateService.read成功
8. crucibleがEMPTYでない
9. metalIdあり
10. MetalPartDefinitions.resolve(mold, metalId)成功
11. crucible.amount >= definition.ingredientCount
12. MetalMaterialDefinitionsからmetal定義取得成功
13. heating evaluatorが対応済み
14. snapshot生成成功
15. snapshot.roughOutputItemIdがRoughMetalPartItem
16. MetalVisualDataService.resolve(metalId)成功
17. 後続処理に必要な出力を事前生成可能
```

1つでも失敗したら、るつぼ・鋳型・Block Entityを変更しない。

---

## 9. 加熱状態による分岐

`MetalDefinition` の値を使用する。

```text
heatingTicks < castableAfterTicks
    → 早すぎる鋳造

castableAfterTicks <= heatingTicks < destroyAfterTicks
    → 鋳造成功

heatingTicks >= destroyAfterTicks
    → 全損
```

`dangerAfterTicks` は警告開始時間であり、鋳造不能時間ではない。

既存 `MeltingGameService.evaluate(crucible, 0)` の `OVERHEATED` は `dangerAfterTicks` 以後を表すため、`OVERHEATED` だけを理由に全損扱いしない。

### 加熱スコア

粗加工パーツへ `heatingScore` を保存する必要があるため、既存 `MeltingGameService` へ以下を追加する。

```java
public static OptionalInt evaluateHeatingScore(
    ItemStack crucible
);
```

`craftbound:linear_curve` では `MetalDefinition.scoreCurve()` の隣接2点を線形補間し、最終値を0～100の整数へ丸める。

補間処理を `CastingGameService` へ複製しない。

---

## 10. 早すぎる鋳造

条件:

```text
heatingTicks < castableAfterTicks
```

必要素材量を `usedAmount` とする。

金属塊へ戻る量:

```text
lossAmount =
    roundByMode(
        usedAmount * lumpLossRatio,
        lumpLossRounding
    )

recoveredAmount =
    usedAmount - lossAmount
```

`recoveredAmount <= 0` なら返却物なし。

返却物がある場合は `MetalPartDefinition.failureLump` を使用して、

```java
MetalLumpStateService.createSmall(
    metalId,
    failureLump.count(),
    failureLump.unitsPerItem()
)
```

を素材消費前に成功させる。

`failureLump.count * unitsPerItem` と `recoveredAmount` が一致しない定義では処理を開始しない。

その後、

```text
CrucibleStateService.consumeForCasting
    ↓
成功
    ↓
返却物あり → pendingOutputへ保存
返却物なし → idleへ戻る
```

moldは残す。

---

## 11. 鋳造成功

条件:

```text
castableAfterTicks <= heatingTicks < destroyAfterTicks
```

素材消費前に以下を作る。

```text
heatingScore
MetalPartDefinitionSnapshot
MetalVisualData
CastingProcess
```

`MetalVisualData` は既存

```java
MetalVisualDataService.resolve(metalId)
```

を使用する。

処理順:

```text
事前検証
    ↓
process候補を生成
    ↓
transactionInProgress = true
    ↓
CrucibleStateService.consumeForCasting
    ↓
成功した場合だけ activeProcess = process
    ↓
setChanged + 同期
    ↓
finally transactionInProgress = false
```

`consumeForCasting` の成功後に失敗し得る検証を残さない。

---

## 12. 全損

条件:

```text
heatingTicks >= destroyAfterTicks
```

処理:

```text
必要量をconsumeForCasting
粗加工パーツなし
金属塊返却なし
activeProcessなし
moldは維持
```

---

## 13. 冷却

`CastingTableBlock` からサーバー側tickerを接続する。

`activeProcess != null` の間だけ、

```text
coolingTicks += 1
```

とする。

規則:

- チャンクロード中だけ進行
- サーバー停止中は進行しない
- 絶対gameTimeや実時間から差分計算しない
- `Long.MAX_VALUE` で飽和
- process変更時は `setChanged()`
- クライアントへ毎tick同期しない

描画用同期は5tick程度に1回でよい。

---

## 14. 表面凝固

初めて

```text
coolingTicks >= snapshot.cooling.surfaceSolidTicks
```

になった時だけ、

```text
surfaceSolidificationNotified = true
```

にする。

そのタイミングで蒸気音・パーティクル等の表面凝固演出を1回発生させる。

`safeTicks` 到達時には正解時間を知らせる専用通知・効果音を出さない。

ロード時点ですでにsurface solid時間を超えている場合、過去の演出を重複再生しない。

---

## 15. 冷却による破損回数上限

粗加工パーツを取り出す時だけ計算する。

```java
public final class CoolingBreakLimitService {

    public static OptionalInt evaluate(
        MetalPartDefinitionSnapshot snapshot,
        long coolingTicks
    );
}
```

対応evaluator:

```text
craftbound:linear_cooling_break_limit
```

計算:

```text
rate =
    clamp(
        coolingTicks / safeTicks,
        0.0,
        1.0
    )

effectiveBreakOnHit =
    max(
        minimumBreakOnHit,
        floor(breakOnHit * rate)
    )

effectiveBreakOnHit =
    min(
        effectiveBreakOnHit,
        breakOnHit
    )
```

`safeTicks <= 0` の場合は `breakOnHit` を返す。

計算結果は粗加工パーツへ保存し、その後の時間経過では更新しない。

---

## 16. 粗加工パーツの生成

空手で冷却中の鋳造台を右クリックすると、その時点の冷却状態を固定する。

生成には既存 `RoughMetalPartStateService` を使用する。

現在のServiceはvisual dataをmetalIdから再解決するため、鋳造開始時の `CastingProcess.visualData` をそのまま渡せるoverloadを追加する。

```java
public static Optional<ItemStack> create(
    UUID resultId,
    UUID operatorId,
    MetalPartDefinitionSnapshot snapshot,
    long heatingTicks,
    int heatingScore,
    long coolingTicksAtRemoval,
    int effectiveBreakOnHit,
    MetalVisualData visualData
);
```

既存 `create(...)` は必要なら互換用に残し、`MetalVisualDataService.resolve` 後に上記overloadへ委譲する。

鋳造台から粗加工パーツNBTを直接書かない。

処理:

```text
CoolingBreakLimitService.evaluate
    ↓
RoughMetalPartStateService.create(..., process.visualData)
    ↓
生成Item ID == snapshot.roughOutputItemId を確認
    ↓
pendingOutput = roughPart
activeProcess = null
    ↓
setChanged + 同期
    ↓
メインハンドが空ならpendingOutputをそのまま手へ移動
```

この順序により、プレイヤーへ渡す前に出力がBlock Entityへ確定される。

---

## 17. pendingOutput

```java
private ItemStack pendingOutput = ItemStack.EMPTY;
```

対象:

- 早すぎる鋳造の返却金属塊
- 確定済み粗加工パーツ

pending中:

- 新規鋳造不可
- 鋳型回収不可
- 空手右クリックで全量回収

回収時:

```text
メインハンドが空
    ↓
pendingOutputをメインハンドへ移動
    ↓
pendingOutput = EMPTY
    ↓
setChanged + 同期
```

一部だけ渡す処理は実装しない。

---

## 18. 鋳型の再利用

成功・失敗・粗加工パーツ回収後もmoldは鋳造台へ残る。

次の状態に戻る。

```text
moldあり
activeProcessなし
pendingOutputなし
```

鋳型は空手スニーク右クリックでのみ回収する。

---

## 19. ブロック破壊

`CastingTableBlock#onRemove` は、

```java
state.getBlock() != newState.getBlock()
```

の場合だけBlock Entityへ破壊処理を委譲する。

```java
CastingTableBlockEntity.dropStoredContents(ServerLevel level)
```

### moldのみ

moldをドロップする。

### activeProcessあり

破壊時点を「取り出した瞬間」として扱う。

```text
現在のcoolingTicks
    ↓
CoolingBreakLimitService
    ↓
RoughMetalPartStateService
    ↓
粗加工パーツ生成
```

moldと粗加工パーツを退避してから内部状態を先に空化し、その後ワールドへdropする。

### pendingOutputあり

pendingOutputをそのままdropする。
activeProcessを再計算しない。

moldもdropする。

---

## 20. 永続化

`CastingTableBlockEntity`:

```text
Version
Mold
ActiveProcess
PendingOutput
```

を保存する。

`CastingProcess` のNBT変換は `CastingProcessCodec` へ分離する。

既存の

```text
MetalPartSnapshotCodec
MetalVisualDataCodec
```

を内部データのCodecとして再利用する。

不正データ:

- 空状態へ書き換えない
- serverTick停止
- 右クリックによる加工操作停止
- 座標と原因をserver logへ出す

---

## 21. クライアント同期

BERに必要なデータだけ同期する。

`CastingTableBlockEntity` でForge/vanillaのBlock Entity update packetを使用する。

最低限同期:

```text
mold
activeProcessの有無
activeProcess.visualData
activeProcess.coolingTicks
activeProcess.definitionSnapshot.cooling.surfaceSolidTicks
surfaceSolidificationNotified
pendingOutputの有無
```

同期不要:

```text
operatorId
processId
heatingScore
forging設定
part_quality設定
failure_lump設定
```

`markChangedAndSync()` のような共通private methodを作ってよい。

同期タイミング:

- 鋳型設置
- 鋳型回収
- 鋳造開始
- 表面凝固
- 出力確定・回収
- 冷却中5tick程度ごと

---

## 22. Block Entity Renderer

`CastingTableRenderer` で鋳造台上の動的部分だけ描画する。

静的な台本体は通常Block Model。

### 鋳型

moldがある場合、上面へ鋳型を描画する。

設計:

```text
共通の薄い3D鋳型モデル
+
鋳型ごとの16×16差分テクスチャ
```

パーツ形状ごとの差はテクスチャで表す。

鋳型の空洞部分を透明にし、その少し下へ金属面を描く方式を採用する。

### 金属

`activeProcess` がある間だけ金属面を描画する。

金属種類ごとのTextureは作らない。

共通Texture:

```text
assets/craftbound/textures/item/casting_metal_surface.png
```

色:

```java
MetalRenderColorResolver.resolve(
    activeProcess.visualData()
)
```

既存 `MetalRenderColorResolver` をそのまま利用し、新しい平均色計算を実装しない。

---

## 23. 登録

### CraftboundBlocks

```java
public static final RegistryObject<CastingTableBlock> CASTING_TABLE
```

を追加する。

基本properties:

```text
strength 3.5F, 6.0F
metal sound
requiresCorrectToolForDrops
```

### CraftboundItems

`CASTING_TABLE` のBlockItemを追加する。

### CraftboundBlockEntities

```java
CASTING_TABLE =
    BlockEntityType.Builder.of(
        CastingTableBlockEntity::new,
        CraftboundBlocks.CASTING_TABLE.get()
    ).build(null)
```

### CraftboundClientEvents

`EntityRenderersEvent.RegisterRenderers` で

```java
CastingTableRenderer
```

を登録する。

既存のItem Tint登録・`MetalRenderColorResolver` cache clear処理は変更しない。

---

## 24. リソース

追加:

```text
assets/craftbound/blockstates/casting_table.json
assets/craftbound/models/block/casting_table.json
assets/craftbound/models/item/casting_table.json

assets/craftbound/textures/block/casting_table_top.png
assets/craftbound/textures/block/casting_table_front.png
assets/craftbound/textures/block/casting_table_side.png

assets/craftbound/textures/item/casting_metal_surface.png

data/craftbound/recipes/blacksmith/equipment/casting_table.json
data/craftbound/loot_tables/blocks/casting_table.json
```

Block modelは水平向き対応のorientable形式とする。

レシピ:

```text
SSS
ICI
 S
```

```text
S = minecraft:smooth_stone
I = minecraft:iron_ingot
C = minecraft:stonecutter
```

ブロックLoot Tableはcasting table自身だけを返す。
内部mold/process/pendingOutputはBlock Entity側で処理する。

必要なblock textureは別途作成するため、Java実装時に画像生成しない。

---

# 採用した実装方針

## 責務

### CastingTableBlock

- BlockState / facing
- Block Entity生成
- ticker接続
- use入口
- onRemove
- ピストン拒否

### CastingTableBlockEntity

- mold
- activeProcess
- pendingOutput
- save/load
- cooling tick
- client sync
- block break時の内部内容物処理

### CastingGameService

- 流し込み前検証
- mold + metalからdefinition解決
- 加熱状態分岐
- るつぼ消費
- process生成
- 早期失敗出力生成
- 粗加工パーツ確定

### MeltingGameService

- 既存加熱判定
- 数値加熱スコア取得

### CoolingBreakLimitService

- 冷却時間から `effectiveBreakOnHit` を計算する純粋処理

### CastingTableRenderer

- mold描画
- 金属面描画
- 金属色適用

---

# 採用しなかった方針

- GUI / Menuを追加する
- 鋳型をItemStackHandlerで保持する
- ホッパー等へItemHandlerを公開する
- るつぼNBTを直接編集する
- `HeatingPhase.OVERHEATED` を全損判定として使う
- 加熱スコア補間をCastingGameServiceへ複製する
- `gameTime` 差分でアンロード中も冷却させる
- 出力をプレイヤーへ渡してからprocessを削除する
- 金属ごとの鋳造台Textureを作る
- 金属平均色計算を新規実装する
- mold Item IDからrough output Item IDを文字列推測する
- 粗加工パーツNBTを鋳造台から直接生成する
- 安全冷却時間をプレイヤーへ明示通知する

---

# 関連する既存クラス

以下を再利用する。

```text
BlacksmithFurnaceBlock
BlacksmithFurnaceBlockEntity

CrucibleItem
CrucibleState
CrucibleStateService

MeltingGameService
HeatingPhase

MetalDefinition
MetalMaterialDefinitions
MetalVisualData
MetalVisualDataService
MetalVisualDataCodec

MetalPartDefinitions
MetalPartDefinition
MetalPartDefinitionSnapshot
MetalPartSnapshotCodec

RoughMetalPartItem
RoughMetalPartStateService

MetalLumpStateService

MetalRenderColorResolver

CraftboundBlocks
CraftboundItems
CraftboundBlockEntities
CraftboundClientEvents
```

---

# データフロー

## 成功

```text
CrucibleItem
    ↓ read
metalId / amount / heatingTicks
    ↓
MetalPartDefinitions.resolve(mold, metalId)
    ↓
definition
    ↓
heatingScore + snapshot + visualData
    ↓
consumeForCasting
    ↓
CastingProcess
    ↓ server ticks
coolingTicks
    ↓ empty-hand take
CoolingBreakLimitService
    ↓
RoughMetalPartStateService
    ↓
rough_<part>
```

## 早すぎる鋳造

```text
crucible
    ↓
loss ratio
    ↓
recoveredAmount
    ↓
MetalLumpStateService
    ↓
consumeForCasting
    ↓
pendingOutput
```

---

# 実装手順

## Step 1: 鋳造ドメインと既存Service接続

このStepで読む:

```text
MeltingGameService.java
MetalDefinition.java
MetalPartDefinition.java
MetalPartDefinitionSnapshot.java
RoughMetalPartStateService.java
MetalLumpStateService.java
MetalVisualData.java
MetalVisualDataService.java
```

実装:

1. `MeltingGameService.evaluateHeatingScore`
2. `CastingProcess`
3. `CastingProcessCodec`
4. `CoolingBreakLimitService`
5. `RoughMetalPartStateService#create(..., MetalVisualData)` overload
6. 必要なら小さな `CastingActionResult`

このStepではBlock、Renderer、resourcesを触らない。

## Step 2: CastingTableBlockEntityとCastingGameService

このStepで読む:

```text
CrucibleStateService.java
MetalPartDefinitions.java
BlacksmithFurnaceBlockEntity.java
```

Step 1で作成したcastingクラスだけ追加で読む。

実装:

1. mold / activeProcess / pendingOutput
2. save/load
3. invalid state検証
4. `CastingGameService.tryPour`
5. 早期失敗・成功・全損
6. server cooling tick
7. 表面凝固
8. 粗加工パーツ確定
9. pendingOutput回収
10. `dropStoredContents`
11. Block Entity update packet

このStepではRendererを実装しない。

## Step 3: Block・Registry・静的resources

このStepで読む:

```text
BlacksmithFurnaceBlock.java
CraftboundBlocks.java
CraftboundItems.java
CraftboundBlockEntities.java
```

実装:

1. `CastingTableBlock`
2. `CASTING_TABLE` block登録
3. BlockItem登録
4. Block Entity登録
5. blockstate
6. block model / item model
7. recipe
8. loot table
9. 必要なlang / mining tag

Java側からTextureを生成しない。

## Step 4: Rendererと統合

このStepで読む:

```text
CraftboundClientEvents.java
MetalRenderColorResolver.java
```

Step 2の `CastingTableBlockEntity` だけ追加で読む。

実装:

1. `CastingTableRenderer`
2. BER登録
3. mold描画
4. generic metal surface描画
5. `MetalRenderColorResolver` によるTint
6. 同期頻度の最終調整
7. `gradlew build`

最後に限定検索:

```text
CastingTable
casting_table
```

で登録漏れだけ確認する。
無関係なコードやdocsを追加探索しない。

---

# 擬似コード

```text
use:
    if offhand:
        PASS

    if client:
        return candidate ? SUCCESS : PASS

    if pending + empty:
        collectPending

    else if process + empty:
        finalizeRoughPart

    else if idle + mold + crucible:
        tryPour

    else if idle + noMold + registeredMold:
        insertMold

    else if idle + mold + empty + sneaking:
        removeMold

    else:
        PASS
```

```text
serverTick:
    if invalid or no activeProcess:
        return

    coolingTicks = saturatedIncrement(coolingTicks)

    if surface threshold crossed:
        mark notified
        play effect
        sync
    else if coolingTicks % 5 == 0:
        sync

    setChanged
```

```text
finalizeRoughPart:
    evaluate cooling break limit
    create rough part using process snapshot + process visualData

    if creation failed:
        do not mutate process
        return false

    pendingOutput = roughPart
    activeProcess = null
    save/sync

    if main hand empty:
        move pendingOutput to hand

    return true
```

---

# 境界条件・例外

- 金属量不足では何も消費しない
- 対応しないmold + metalでは何も消費しない
- invalid crucibleでは何も消費しない
- invalid tableでは加工しない
- `consumeForCasting` 失敗時はprocessを作らない
- `dangerAfterTicks <= heatingTicks < destroyAfterTicks` は鋳造可能
- `destroyAfterTicks` 以上は全損
- process中は鋳型を外せない
- pendingOutput中は新規鋳造できない
- チャンクアンロード中は冷却しない
- Block Entityロード時に過去の表面凝固演出を重複再生しない
- ブロック破壊中に同じ出力を2回生成しない
- `activeProcess` と `pendingOutput` を同時に保持しない
- `/reload` 後の開始済み工程はsnapshotで継続する
- 金属描画色はprocess開始時の `MetalVisualData` を使用する
- 粗加工パーツへ同じ `MetalVisualData` を引き継ぐ
- Dedicated Serverからclient renderer classを参照しない
- texture不足でもserver logicを壊さない

---

# 完了条件

- `craftbound:casting_table` を設置・破壊できる
- 水平方向を保持する
- 標準鋳型を1個設置できる
- idle時に鋳型を回収できる
- 対応するmold + metalを `MetalPartDefinitions` から解決できる
- 必要量不足ではるつぼを変更しない
- 早すぎる鋳造では規定量を失い、回収分を金属塊として生成できる
- 鋳造可能時間では `CastingProcess` が開始する
- destroy時間以後では全損する
- 冷却はロード中のserver tickだけで進む
- 表面凝固演出は1回だけ発生する
- 任意の冷却時点で粗加工パーツを取り出せる
- 取り出し時の `effectiveBreakOnHit` が粗加工パーツへ固定される
- 粗加工パーツのItemは `snapshot.roughOutputItemId` から生成される
- 粗加工パーツは流し込み時のmetal色を保持する
- 出力確定前後で素材複製・消失がない
- ブロック破壊時にmoldと加工中/保留出力を適切にdropできる
- moldと流し込まれた金属をBERで描画できる
- 金属色は既存 `MetalRenderColorResolver` で変化する
- 金属種類ごとの鋳造台Textureを必要としない
- 外部自動搬送から内部状態を操作できない
- `gradlew build` が成功する

---

# 今回の対象外

- 鋳型・粗加工パーツ自体の再設計
- 完成金属パーツの実装
- 鍛造台
- 鍛造ミニゲーム
- 鍛冶師XP付与
- GUI / Menu / Screen
- 自動搬送
- 合金
- アンロード中の冷却
- 安全冷却時刻のUI表示
- 金属ごとの専用Texture
- casting table用16×16 PNGそのものの制作
- 鋳型用16×16 PNGそのものの制作
