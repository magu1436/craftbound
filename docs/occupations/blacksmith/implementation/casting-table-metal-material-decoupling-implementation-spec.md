# 鋳造台 金属素材分離・動的色描画 実装仕様書

# 目的

Minecraft Forge 1.20.1 の Craftbound における鋳造処理について、現在 `MetalPartDefinition` に結合されている「パーツ形状」と「金属素材」を分離する。

現在の実装では `MetalPartDefinition` が `metalId` を保持し、`MetalPartDefinitions` が `moldItemId + metalId` の組み合わせで定義を解決している。そのため、同じ鋳型へ別の金属を流すには金属ごとにパーツ定義を複製する必要がある。

本変更では次を実現する。

```text
鋳型
  ↓
MetalPartDefinition
  ↓
作るパーツの形状・加工条件を決定

るつぼ内の metalId
  ↓
MetalMaterialDefinitions
  ↓
使用する金属素材・加熱条件・描画情報を決定

両者を CastingProcess で組み合わせる
  ↓
同じ鋳型へ鉄・金・他Mod金属を流せる
```

あわせて、鋳造台上の金属描画では `CastingProcess` に保存された `MetalVisualData` を唯一の描画情報として使用し、金属ごとに異なる色を表示する。

金属色の取得は、明示色を最優先し、明示色がない場合は代表アイテムのテクスチャ平均色から解決する。

本書は既存 `feat/casting-table` 実装に対する差分実装仕様書とする。

---

# 確定した仕様

## 1. パーツ定義と金属定義の責務

### MetalPartDefinition

`MetalPartDefinition` は「どの鋳型から何のパーツを作るか」と、そのパーツ固有の加工条件だけを定義する。

保持する情報は次とする。

```java
public record MetalPartDefinition(
    ResourceLocation id,
    int ingredientCount,
    ResourceLocation moldItemId,
    ResourceLocation roughOutputItemId,
    ResourceLocation outputItemId,
    FailureLumpDefinition failureLump,
    CoolingDefinition cooling,
    ForgingDefinition forging,
    PartQualityDefinition partQuality
) {}
```

`metalId` は削除する。

`MetalPartDefinition` は特定金属へ依存しない。

例えばピッケルヘッド鋳型の定義は、鉄・金・銅・他Mod金属で共通して使用する。

---

### MetalMaterialDefinitions

`MetalMaterialDefinitions` は金属素材固有の情報を管理する。

既存の次の情報は引き続き金属定義側を正とする。

- 金属ID
- 金属として認識するItem / Tag
- 金属量換算
- 失敗時の損失率
- 加熱可能時間
- 危険時間
- 全損時間
- 加熱スコア曲線
- 加熱評価器
- 描画色

パーツ形状、鋳型、冷却条件、鍛造条件は金属定義へ移動しない。

---

## 2. MetalPartDefinitions の解決キー

現在の

```java
Map<MoldMetalKey, MetalPartDefinition> definitionsByMoldAndMetal
```

を廃止する。

変更後は鋳型IDだけで解決する。

```java
Map<ResourceLocation, MetalPartDefinition> definitionsByMold
```

公開APIは次とする。

```java
public Optional<MetalPartDefinition> resolve(ItemStack mold)
```

解決手順:

```text
1. moldが craftbound:casting_molds タグに含まれることを確認
2. moldのItem IDを取得
3. definitionsByMold から取得
4. 見つからなければ Optional.empty()
```

金属IDはこの処理へ渡さない。

---

## 3. パーツ定義JSON

`data/<namespace>/blacksmith/metal_parts/*.json` から `metal` フィールドを削除する。

変更前:

```json
{
  "schema_version": 1,
  "metal": "craftbound:iron",
  "ingredient_count": 3,
  "mold": "craftbound:pickaxe_head_mold"
}
```

変更後:

```json
{
  "schema_version": 2,
  "ingredient_count": 3,
  "mold": "craftbound:pickaxe_head_mold"
}
```

既存の `rough_output`、`output`、`failure_lump`、`cooling`、`forging`、`part_quality` はそのまま維持する。

`schema_version` は `2` へ上げる。

旧 `schema_version: 1` を新形式として暗黙変換しない。

開発段階のデータ形式であるため、旧定義の互換読み込みは実装しない。

---

## 4. 重複定義

同じ鋳型を複数の `MetalPartDefinition` が参照した場合、その鋳型は曖昧として解決不能にする。

```text
同じ moldItemId を持つ定義Aを読み込み
    ↓
登録

同じ moldItemId を持つ定義Bを読み込み
    ↓
重複として警告
    ↓
definitionsByMold からその鋳型を除外
```

ファイルID単位の `definitionsById` は保持してよい。

ログ例:

```text
Duplicate blacksmith metal part mold: craftbound:pickaxe_head_mold
```

---

## 5. CastingProcess

`CastingProcess` は次の役割分担とする。

```text
metalId
    実際に流し込まれた金属

definitionSnapshot
    パーツ形状・必要量・冷却・鍛造・品質条件

visualData
    流し込まれた金属の描画情報
```

`metalId` は `CastingProcess` に残す。

`definitionSnapshot` から `metalId` を削除する。

変更後:

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
) {}
```

既存構造とほぼ同じだが、snapshot側に金属IDを重複保持しない。

---

## 6. MetalPartDefinitionSnapshot

変更後:

```java
public record MetalPartDefinitionSnapshot(
    ResourceLocation definitionId,
    int ingredientCount,
    ResourceLocation moldItemId,
    ResourceLocation roughOutputItemId,
    ResourceLocation outputItemId,
    FailureLumpDefinition failureLump,
    CoolingDefinition cooling,
    ForgingDefinition forging,
    PartQualityDefinition partQuality
) {}
```

`metalId` を削除する。

snapshotは開始済み加工工程を `/reload` から保護するため引き続き使用する。

金属情報は `CastingProcess.metalId` と `CastingProcess.visualData` をsnapshotとして扱う。

---

## 7. CastingProcess の不変条件

変更後の主な不変条件:

```text
version == CURRENT_VERSION
metalAmount >= 1
heatingTicks >= 0
0 <= heatingScore <= 100
coolingTicks >= 0

partDefinitionId == definitionSnapshot.definitionId
metalAmount == definitionSnapshot.ingredientCount
metalId != null
visualData != null
```

Block Entity側では引き続き、

```text
実際のmold Item ID == definitionSnapshot.moldItemId
```

を要求する。

削除する条件:

```text
metalId == definitionSnapshot.metalId
```

---

## 8. CastingProcess / Block Entity の保存バージョン

snapshotの保存形式が変更されるため、`CastingProcess.CURRENT_VERSION` を `2` へ上げる。

`CastingTableBlockEntity` の保存形式についても、activeProcessの形式変更を明示するため `CURRENT_VERSION` を `2` へ上げる。

旧versionの保存済み鋳造台は既存方針どおり `invalidStoredState` として扱い、自動的に空状態へ変換しない。

開発段階であるためversion 1から2へのマイグレーションは今回実装しない。

新規設置した鋳造台で動作確認する。

---

## 9. CastingGameService.tryPour

流し込み前検証を次へ変更する。

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
10. MetalPartDefinitions.resolve(mold)成功
11. crucible.amount >= definition.ingredientCount
12. MetalMaterialDefinitions.get(metalId)成功
13. heating evaluatorが対応済み
14. definition.snapshot()成功
15. snapshot.roughOutputItemIdがRoughMetalPartItem
16. MetalVisualDataService.resolve(metalId)成功
17. MetalVisualDataが実際に描画可能
18. 後続処理に必要な出力を事前生成可能
```

パーツ定義の解決と金属定義の解決を完全に分離する。

```java
Optional<MetalPartDefinition> definitionResult =
    MetalPartDefinitions.INSTANCE.resolve(table.mold());

Optional<MetalDefinition> metalResult =
    MetalMaterialDefinitions.INSTANCE.get(state.metalId());
```

同じ鋳型について、`MetalMaterialDefinitions` に登録された任意の金属を使用可能とする。

---

## 10. 失敗時の金属塊

早すぎる鋳造で生成する金属塊は、引き続き `CrucibleState.metalId` を使用する。

```java
MetalLumpStateService.createSmall(
    state.metalId(),
    failure.count(),
    failure.unitsPerItem()
)
```

`FailureLumpDefinition` は回収量・出力Item・unitsPerItemを定義するだけとし、金属種類は保持しない。

したがって金でも鉄でも、同じパーツ定義の失敗条件を使用しつつ、生成された金属塊には実際の `metalId` が保存される。

---

## 11. 粗加工パーツへの金属状態引き継ぎ

`RoughMetalPartStateService.create(...)` では、引き続き `CastingProcess` が保持する金属情報を出力へ保存する。

少なくとも次を保持する。

```text
metalId
visualData
heatingTicks
heatingScore
coolingTicks
breakLimit
part definition snapshot
```

パーツ定義snapshotから金属IDを取得しない。

金属IDは必ず `CastingProcess.metalId` を正とする。

必要であれば `RoughMetalPartStateService.create(...)` の引数へ `metalId` を明示追加する。

現在のstate型が既に独立した `metalId` を保持している場合はそれを使用し、重複追加しない。

---

## 12. MetalVisualData

`MetalVisualData` は現行recordを維持する。

```java
public record MetalVisualData(
    Integer explicitRgb,
    ResourceLocation representativeItemId
) {}
```

優先順位は次とする。

```text
1. explicitRgb
2. representativeItemId のアイテムテクスチャ平均色
3. 解決失敗
```

`DEFAULT_COLOR` は描画処理の最終的な安全装置としてのみ残す。

通常の正常系で灰色フォールバックへ到達する状態を許容しない。

---

## 13. 金属定義の representative_item

金属JSONへ任意フィールド `representative_item` を追加する。

例:

```json
{
  "schema_version": 2,
  "ingredients": [
    { "tag": "c:ingots/tin", "size": 1 }
  ],
  "representative_item": "examplemod:tin_ingot",
  "display_color": null
}
```

実際のJSONでは `display_color` を省略してよい。

解決ルール:

```text
display_colorあり
    ↓
explicitRgbとして使用

representative_itemあり
    ↓
representativeItemIdとして使用

representative_itemなし
    ↓
ingredients内の明示itemから代表アイテムを取得

明示itemも存在しない
    ↓
visual data解決失敗
```

`representative_item` が指定された場合は登録済みItem IDであることをデータ読み込み時に検証する。

### metal JSON schema_version

金属JSONも `representative_item` 追加を明示するため `schema_version: 2` へ上げる。

既存のCraftbound組み込み金属JSONはすべてversion 2へ更新する。

旧version 1の互換読み込みは今回実装しない。

---

## 14. MetalVisualDataService

現在のように

```text
explicitRgb == null
代表アイテム == null
```

でも `Optional.of(MetalVisualData)` を返す実装を廃止する。

変更後:

```java
public static Optional<MetalVisualData> resolve(ResourceLocation metalId)
```

は、次の場合のみpresentを返す。

```text
explicitRgb != null
または
representativeItemId != null
```

両方nullなら `Optional.empty()` を返す。

これにより `CastingGameService` の事前検証で描画不能な金属を拒否できる。

---

## 15. MetalRenderColorResolver

現行の共通Resolverを維持する。

```text
explicitRgbあり
    ↓
そのRGBを使用

representativeItemIdあり
    ↓
アイテムモデルのparticle spriteを取得
    ↓
透明pixelを除外
    ↓
RGB平均値を計算
    ↓
キャッシュ
```

リソースリロード時の `clearCache()` も維持する。

平均色計算がRuntimeException等で失敗した場合は `DEFAULT_COLOR` を返してよい。

ただし、`representativeItemId` 自体が存在しない状態は `MetalVisualDataService` で弾く。

---

## 16. 鋳造台上の金属描画

`CastingTableRenderer` は `CastingProcess.visualData()` だけから色を決定する。

```java
int color = MetalRenderColorResolver.resolve(process.visualData());
```

金属IDからRenderer内で再度 `MetalMaterialDefinitions` を検索しない。

開始済みprocessは `/reload` 後も開始時点の色を維持する。

---

## 17. 金属表面用テクスチャ

現在使用している

```text
minecraft:block/white_concrete
```

を動的着色用ベースとして使用しない。

Craftbound専用のグレースケールテクスチャを追加する。

```text
assets/craftbound/textures/block/casting_metal_surface.png
```

要件:

- 16×16
- RGBA PNG
- 不透明
- 白～灰色だけで構成
- 色相を持たない
- 左上または上方を光源とした軽い金属陰影
- 強い固定色を持たない
- RGB tintを乗算した際に元色が認識できる明度

Rendererの `SURFACE_TEXTURE` を次へ変更する。

```java
new ResourceLocation("craftbound", "block/casting_metal_surface")
```

このテクスチャは色そのものを表現せず、金属表面の明暗だけを担当する。

---

## 18. 描画色の責務

```text
casting_metal_surface.png
    ↓
形・陰影・質感

MetalVisualData
    ↓
金属色

MetalRenderColorResolver
    ↓
最終RGB

CastingTableRenderer
    ↓
texture × RGB tint
```

金属ごとの鋳造台用テクスチャは追加しない。

```text
casting_metal_surface_iron.png
casting_metal_surface_gold.png
casting_metal_surface_copper.png
```

のような方式は禁止する。

---

## 19. 組み込み金属

既存組み込み金属は原則 `display_color` を明示する。

例:

```text
iron -> #A7A7A7
gold -> #F5D142
```

Craftbound組み込み金属では安定した見た目を優先し、代表アイテムからの平均色計算へ依存しなくてよい。

他Mod金属では、互換データパックが `display_color` または `representative_item` のどちらかを指定できる。

---

## 20. ログ

正常な右クリック失敗を大量にINFOログへ出力しない。

ただし開発時の原因特定を容易にするため、`CastingGameService` にデバッグログを追加してよい。

通常ビルドで常時出力する必要はない。

確認可能にしたい失敗理由:

```text
NO_MOLD_DEFINITION
UNKNOWN_METAL
INSUFFICIENT_METAL
NO_VISUAL_DATA
NOT_CASTABLE_YET
DESTROYED_BY_OVERHEAT
```

本変更では `CastingActionResult` を上記すべてへ細分化することは必須としない。

---

# 採用した実装方針

## 1. パーツ定義を鋳型単位にする

パーツの形状と金属素材は独立した概念であるため、`MetalPartDefinition` は鋳型単位で解決する。

同じ鋳型を使用する限り、金属ごとに定義ファイルを複製しない。

---

## 2. 金属情報は CastingProcess で合成する

鋳造開始時に、

```text
MetalPartDefinitionSnapshot
+
metalId
+
MetalVisualData
```

を `CastingProcess` に保存する。

これによりデータパック `/reload` 後も開始済み工程の状態を固定できる。

---

## 3. 金属色は MetalVisualData を共通基盤とする

鋳造台、粗加工パーツ、完成パーツで別々の色解決処理を持たない。

既存 `MetalRenderColorResolver` を共通利用する。

---

## 4. 明示色を優先し、平均色はフォールバックとする

Minecraftや他Modのアイテムモデルは単純な1枚テクスチャとは限らないため、安定性のある `display_color` を最優先する。

他Mod互換用として `representative_item` を利用できるようにする。

---

## 5. 描画用テクスチャと色データを分離する

専用グレースケールテクスチャは質感のみを担当し、RGBは `MetalVisualData` から動的に与える。

---

# 採用しなかった方針

## 金属ごとに MetalPartDefinition を複製する

金属数 × パーツ数だけ定義が増え、他Mod金属対応で組み合わせ数が急増するため採用しない。

## 金属ごとに鋳造台用テクスチャを用意する

金属追加ごとにリソース追加が必要になり、他Mod互換性を失うため採用しない。

## Rendererが毎フレームmetalIdから金属定義を再検索する

開始済みprocessが `/reload` の影響を受け、描画状態が途中変更されるため採用しない。

## `DEFAULT_COLOR` を通常の正常系として利用する

描画データ欠落を発見しにくくするため採用しない。通常系は `MetalVisualDataService` で事前検証する。

## ingredientsのtagから実行時に毎回代表アイテムを探索する

代表アイテムが不定であり、レジストリ順序等へ依存しやすいため採用しない。タグのみの金属は `representative_item` を明示する。

## 旧schemaを自動変換する

現時点は開発段階であり、互換コードを恒久的に持つ利益が小さいため採用しない。

---

# 関連する既存クラス

## 主な変更対象

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/
├─ CastingGameService.java
├─ CastingProcess.java
├─ CastingProcessCodec.java
└─ CastingTableBlockEntity.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/definition/
├─ MetalPartDefinition.java
├─ MetalPartDefinitionSnapshot.java
├─ MetalPartDefinitions.java
└─ MetalPartSnapshotCodec.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/
├─ MetalMaterialDefinitions.java
├─ MetalVisualData.java
├─ MetalVisualDataCodec.java
└─ MetalVisualDataService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/client/
├─ CastingTableRenderer.java
├─ MetalRenderColorResolver.java
├─ RoughMetalPartColorHandler.java
└─ MetalPartColorHandler.java
```

## 状態引き継ぎ確認対象

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/part/
└─ RoughMetalPartStateService.java
```

完成パーツへ `metalId` / `visualData` を引き継ぐ既存処理がある場合は、その処理も確認する。

## データ

```text
src/main/resources/data/craftbound/blacksmith/metals/*.json
src/main/resources/data/craftbound/blacksmith/metal_parts/*.json
```

## 描画リソース

```text
src/main/resources/assets/craftbound/textures/block/casting_metal_surface.png
```

---

# データフロー

## 鋳型設置

```text
プレイヤーが鋳型を右クリック
    ↓
MetalPartDefinitions.isRegisteredMold
    ↓
CastingTableBlockEntity.moldへ保存
```

この段階では金属種類を決定しない。

---

## 流し込み

```text
るつぼを持って鋳造台を右クリック
    ↓
CrucibleStateService.read
    ↓
metalId / amount / heatingTicks取得
    ↓
MetalPartDefinitions.resolve(mold)
    ↓
パーツ形状・必要量・加工条件取得
    ↓
MetalMaterialDefinitions.get(metalId)
    ↓
金属固有の加熱条件取得
    ↓
MetalVisualDataService.resolve(metalId)
    ↓
描画情報取得
    ↓
加熱判定・素材量判定
    ↓
MetalPartDefinitionSnapshot作成
    ↓
CastingProcess作成
    ↓
CrucibleStateService.consumeForCasting
    ↓
CastingTableBlockEntity.activeProcessへ保存
    ↓
クライアント同期
```

---

## クライアント描画

```text
CastingTableBlockEntity同期
    ↓
CastingProcess.visualData
    ↓
MetalRenderColorResolver.resolve
    ├─ explicitRgb
    └─ representativeItemId -> 平均色
    ↓
最終RGB
    ↓
casting_metal_surface.pngへtint
    ↓
鋳造台上部へ描画
```

---

## 粗加工パーツ生成

```text
プレイヤーが冷却中パーツを回収
    ↓
CastingProcess
    ├─ metalId
    ├─ visualData
    └─ definitionSnapshot
    ↓
RoughMetalPartStateService.create
    ↓
粗加工パーツNBTへ保存
    ↓
RoughMetalPartColorHandler
    ↓
同じMetalRenderColorResolverで描画
```

---

# 擬似コード

## MetalPartDefinitions 読み込み

```text
データリロード時:
    definitionsByIdを空にする
    definitionsByMoldを空にする
    ambiguousMoldsを空にする

    各metal part JSONについて:
        schema_version == 2 を検証する
        metalフィールドは読まない
        mold Item IDを検証する
        rough_outputを検証する
        outputを検証する
        冷却・鍛造・品質条件を検証する
        MetalPartDefinitionを生成する

        definitionsByIdへ登録する

        moldがすでにambiguousなら:
            definitionsByMoldへ登録しない
            次へ

        definitionsByMoldに同じmoldが存在するなら:
            definitionsByMoldから削除する
            ambiguousMoldsへ追加する
            警告を出す
            次へ

        definitionsByMoldへ登録する
```

---

## パーツ定義解決

```text
resolve(mold):
    moldが空ならempty
    casting_moldsタグに含まれなければempty

    mold Item IDを取得する
    definitionsByMoldから取得する
    存在すれば返す
    存在しなければempty
```

---

## MetalVisualDataService

```text
resolve(metalId):
    MetalMaterialDefinitionsからmetal definitionを取得する
    存在しなければempty

    displayColorが存在するなら:
        explicitRgb = displayColor
    それ以外:
        explicitRgb = null

    representative_itemが存在するなら:
        representativeItemId = representative_item
    それ以外でingredientsに明示itemがあるなら:
        representativeItemId = 最初の明示item
    それ以外:
        representativeItemId = null

    explicitRgbもrepresentativeItemIdもnullなら:
        empty

    MetalVisualDataを返す
```

---

## 流し込み

```text
tryPour(player, table, crucible):
    tableが鋳造開始可能でなければPASS
    crucibleでなければPASS

    CrucibleStateを読む
    読めなければPASS
    EMPTYならPASS
    metalIdがなければPASS

    partDefinition = MetalPartDefinitions.resolve(table.mold)
    なければPASS

    metalDefinition = MetalMaterialDefinitions.get(state.metalId)
    なければPASS

    必要量未満ならPASS

    heatingScoreを評価する
    失敗ならPASS

    visualData = MetalVisualDataService.resolve(state.metalId)
    失敗ならPASS

    partDefinition.snapshotを作る
    失敗ならPASS

    roughOutputがRoughMetalPartItemであることを確認する
    失敗ならPASS

    heatingTicks < castableAfterTicksなら:
        早すぎる鋳造処理
        return

    heatingTicks >= destroyAfterTicksなら:
        全損処理
        return

    CastingProcessを作成する
        metalId = state.metalId
        metalAmount = snapshot.ingredientCount
        visualData = 解決済みvisualData
        definitionSnapshot = snapshot

    transaction開始
    crucibleから必要量を消費する
    失敗ならPASS

    table.activeProcessへprocessを設定する
    クライアントへ同期する
    SUCCESS
```

---

## Renderer

```text
activeProcessが存在する場合:
    visualData = process.visualData
    rgb = MetalRenderColorResolver.resolve(visualData)

    casting_metal_surface spriteを取得する

    各vertexへ:
        texture UV
        rgb tint
        alpha 255
        light
        normal
    を設定して描画する
```

---

# 実装手順

## Step 1: パーツ定義とsnapshotから金属依存を除去

対象:

```text
MetalPartDefinition.java
MetalPartDefinitionSnapshot.java
MetalPartSnapshotCodec.java
MetalPartDefinitions.java
metal_parts/*.json
```

実施内容:

- `metalId` をDefinition / Snapshotから削除
- `MoldMetalKey` を廃止
- 鋳型ID単独のMapへ変更
- `resolve(mold, metalId)` を `resolve(mold)` へ変更
- パーツJSON schema_versionを2へ更新
- snapshot NBTから `Metal` を削除
- 重複鋳型検出を実装

完了後、1つの鋳型定義が金属に依存せず解決できる状態にする。

---

## Step 2: CastingProcessと流し込み処理を更新

対象:

```text
CastingProcess.java
CastingProcessCodec.java
CastingGameService.java
CastingTableBlockEntity.java
RoughMetalPartStateService.java
```

実施内容:

- `CastingProcess.metalId` を金属の正本として維持
- snapshotとの `metalId` 一致検証を削除
- process versionを2へ更新
- Block Entity versionを2へ更新
- `tryPour` を鋳型定義解決と金属定義解決へ分離
- 粗加工パーツ生成時にprocessのmetalIdを明示的に引き継ぐ

完了後、同じ鋳型へ複数の登録済み金属を流せる状態にする。

---

## Step 3: 金属描画情報の解決を強化

対象:

```text
MetalMaterialDefinitions.java
MetalVisualDataService.java
MetalVisualData.java
MetalVisualDataCodec.java
metals/*.json
```

実施内容:

- metal JSON schema_versionを2へ更新
- 任意の `representative_item` を追加
- `display_color` を最優先
- `representative_item` を次優先
- 明示item ingredientを最後の自動候補とする
- 描画情報を1つも解決できない場合はOptional.empty

完了後、タグだけで登録された他Mod金属でも代表アイテムを明示できる状態にする。

---

## Step 4: 鋳造台の動的色描画を確定

対象:

```text
CastingTableRenderer.java
MetalRenderColorResolver.java
CraftboundClientEvents.java
assets/craftbound/textures/block/casting_metal_surface.png
```

実施内容:

- `white_concrete` を専用グレースケールテクスチャへ置換
- `process.visualData()` からのみ色解決
- cache clear listenerを維持
- 鉄と金で明確に色差が表示されることを確認

---

# 境界条件・例外

## 鋳型定義なし

`craftbound:casting_molds` タグへ含まれていても、対応する `MetalPartDefinition` がなければ流し込み不可。

素材は消費しない。

---

## 未登録金属

CrucibleStateにmetalIdが存在しても `MetalMaterialDefinitions` に定義がなければ流し込み不可。

素材は消費しない。

---

## 描画情報なし

金属定義自体が存在しても、

```text
display_colorなし
representative_itemなし
明示item ingredientなし
```

の場合は `MetalVisualDataService.resolve` を失敗させ、鋳造開始しない。

灰色で続行しない。

---

## representative_itemが不正

存在しないItem IDの場合、その金属定義自体をデータ読み込み時に無効化する。

実行時に毎回警告しない。

---

## 代表アイテムのテクスチャ平均色計算失敗

クライアント上のモデル・sprite取得で例外が発生した場合は `MetalRenderColorResolver.DEFAULT_COLOR` を使用してよい。

ゲーム結果には影響しない。

---

## `/reload`

開始前:

- 新しいパーツ定義
- 新しい金属定義
- 新しいdisplay_color / representative_item

を使用する。

開始済みprocess:

- definitionSnapshot
- metalId
- visualData

を使用し、途中変更しない。

---

## 旧保存データ

version 1の鋳造台またはCastingProcessは新実装ではinvalidとして扱う。

自動初期化しない。

開発確認時は鋳造台を再設置する。

---

## 同じ鋳型の定義重複

重複した鋳型はどちらかを暗黙採用せず、その鋳型を解決不能にする。

---

## 金属量不足

必要量未満ならprocessを開始せず、金属を消費しない。

必要量はパーツ定義の `ingredient_count` を使用する。

---

## 金属ごとの必要量差

同じ形状である限り `ingredient_count` はパーツ側で共通とする。

「金は鉄より同じパーツに必要量が多い」など、金属密度による必要量差は今回扱わない。

---

## 金属ごとの冷却速度差

冷却条件は現時点ではパーツ定義側を正とする。

金属によって冷却時間が変化する仕様は今回追加しない。

---

# 完了条件

## データ定義

- `MetalPartDefinition` に `metalId` が存在しない
- `MetalPartDefinitionSnapshot` に `metalId` が存在しない
- `MetalPartDefinitions.resolve` が鋳型だけを引数に取る
- 同じ鋳型を重複定義すると曖昧として無効になる
- metal_parts JSONがschema version 2で読み込まれる
- metals JSONがschema version 2で読み込まれる
- ログ上で組み込み金属定義とパーツ定義がエラーなく読み込まれる

## 鋳造

- 1つの鋳型定義だけで鉄を流せる
- 同じ鋳型定義へ金を流せる
- 金用のMetalPartDefinition複製が不要
- 登録済みの別金属でも同じ鋳型を利用できる
- 未登録金属は拒否される
- 金属量不足時に消費されない
- `/reload` 後も開始済みprocessの金属種類・描画色・加工条件が変化しない

## 保存

- CastingProcessのNBTへmetalIdが1箇所だけ保存される
- DefinitionSnapshot NBTへMetalフィールドを保存しない
- process version 2を正常に保存・復元できる
- Block Entity version 2を正常に保存・復元できる

## 描画

- 鋳造台上で鉄が鉄用色で表示される
- 鋳造台上で金が金用色で表示される
- 鉄と金が同じ灰色として表示されない
- `display_color` 指定金属ではそのRGBが使用される
- `representative_item` 指定金属では代表アイテム由来の平均色が使用される
- 粗加工パーツでも鋳造台と同じ金属色解決ロジックが使用される
- 完成パーツ側に既存の動的着色がある場合も同じResolverを共有する
- リソースリロード後に平均色キャッシュがクリアされる

## 拡張性

- 新規金属追加時、既存パーツごとの定義複製が不要
- 新規パーツ追加時、対応金属ごとの定義複製が不要
- 他Mod金属はMetalMaterialDefinitions側のデータ追加だけで既存鋳型へ流せる

---

# 今回の対象外

- 金属ごとの冷却速度変更
- 金属ごとの鍛造強度範囲変更
- 金属密度による `ingredient_count` 補正
- 合金
- 複数金属を混合した鋳造
- 鋳型内部形状に沿った液面メッシュ生成
- 金属量に応じた液面高さ変更
- 液体アニメーション
- 発光金属のemissive描画
- 溶融温度に応じた赤熱色の時間変化
- Resource Packからの高度なPBR表現
- version 1保存データの自動マイグレーション
