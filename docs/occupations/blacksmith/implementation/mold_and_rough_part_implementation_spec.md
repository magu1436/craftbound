# 鋳型・粗加工パーツ実装仕様書

# 目的

Minecraft Forge 1.20.1で動作するCraftboundに、鋳造台より先に必要となる以下の基盤を実装する。

- 空鋳型
- 標準鋳型
- 金属パーツ定義の読み込みと索引化
- 状態付きの粗加工金属パーツ
- 鋳造失敗時に返却する小金属塊
- 各ItemStackの保存、検証、表示に必要なサービス

対象リポジトリは `magu1436/craftbound`、参照ブランチは `develop` とする。

本実装は、後続の鋳造台が「鋳型を識別する」「金属と鋳型から製作対象を解決する」「冷却結果を固定した粗加工パーツを生成する」「失敗時に金属塊を生成する」ための公開契約を提供する。

鋳造台、冷却tick、るつぼからの消費、鋳造操作そのものは本書では実装しない。

---

# 確定した仕様

## 1. 登録するアイテム

### 1.1 空鋳型

| 項目 | 値 |
|---|---|
| リソースID | `craftbound:blank_mold` |
| Java型 | 通常の `Item` |
| 最大スタック数 | 64 |
| 耐久値 | なし |
| 用途 | 石切台で標準鋳型へ加工する |
| 鋳造台への設置 | 不可 |

空鋳型は完成済み鋳型ではないため、`craftbound:casting_molds` タグへ含めない。

### 1.2 標準鋳型

すべて `CastingMoldItem` のインスタンスとして登録する。

| 区分 | リソースID |
|---|---|
| 武器・ツール | `sword_blade_mold` |
|  | `pickaxe_head_mold` |
|  | `axe_head_mold` |
|  | `shovel_head_mold` |
|  | `hoe_head_mold` |
| 防具 | `helmet_body_mold` |
|  | `chestplate_body_mold` |
|  | `leggings_body_mold` |
|  | `boots_body_mold` |
|  | `horse_armor_body_mold` |
| 小物 | `iron_ring_mold` |
|  | `crossbow_trigger_mold` |
|  | `shield_boss_mold` |
|  | `shears_blades_mold` |
|  | `fire_striker_mold` |
|  | `brush_head_mold` |

共通仕様は以下とする。

- 最大スタック数は1とする。
- 耐久値を持たない。
- 使用によって消費しない。
- 別の標準鋳型または空鋳型へ戻せない。
- すべて `craftbound:casting_molds` アイテムタグへ含める。
- 鋳型の種類はItemStackのNBTではなく、アイテムのリソースIDで識別する。
- 鋳造可能な金属とパーツの対応は鋳型Itemクラスへ埋め込まず、金属パーツJSONから解決する。

### 1.3 粗加工金属パーツ

| 項目 | 値 |
|---|---|
| リソースID | `craftbound:rough_metal_part` |
| Java型 | `RoughMetalPartItem` |
| 最大スタック数 | 1 |
| 耐久値 | なし |
| 生成方法 | 鋳型から取り出したとき、または冷却中の鋳造台を破壊したとき |
| 用途 | 鍛造台へ渡す状態付き中間アイテム |

金属やパーツ種別ごとに別Itemを登録しない。すべての組み合わせを1種類のItemと状態データで表現する。

### 1.4 小金属塊

鋳造失敗と鍛造失敗の返却物として、先行依存に含める。

| 項目 | 値 |
|---|---|
| リソースID | `craftbound:small_metal_lump` |
| Java型 | `MetalLumpItem` |
| 最大スタック数 | 64 |
| 耐久値 | なし |
| 保存情報 | 金属ID、1個あたり素材量、データバージョン |

同じアイテム、同じ金属ID、同じ1個あたり素材量を持つItemStackだけがスタックできる。Forge 1.20.1の通常のItemStack結合規則に従い、NBTが異なる金属塊は結合しない。

中・大の金属塊は今回実装しない。

---

## 2. レシピ

### 2.1 空鋳型

配置先:

```text
src/main/resources/data/craftbound/recipes/blacksmith/equipment/blank_mold.json
```

形式はshapelessとし、以下を消費して1個生成する。

- `minecraft:brick` × 5
- `minecraft:iron_nugget` × 3

### 2.2 標準鋳型

空鋳型1個から、石切台で各標準鋳型1個を生成する。

配置先:

```text
src/main/resources/data/craftbound/recipes/blacksmith/equipment/molds/<part_id>_mold.json
```

入力は常に `craftbound:blank_mold`、出力数は1とする。

標準鋳型を入力にするstonecuttingレシピは登録しない。

---

## 3. 鋳型の識別契約

鋳型判定は、Javaクラスの `instanceof` だけを正としない。

公開判定は次の順で行う。

```text
1. ItemStackが空でない
2. craftbound:casting_molds アイテムタグに含まれる
3. 必要な場面では、MetalPartDefinitionsから
   対応する金属パーツ定義を解決できる
```

この方針により、他MODのアイテムをデータパックで鋳型として追加できる余地を残す。

Craftbound標準鋳型は `CastingMoldItem` を使用するが、鋳造台は標準鋳型専用の `instanceof` 判定を使用しない。

---

## 4. 金属パーツ定義

### 4.1 読み込み場所

```text
data/<namespace>/blacksmith/metal_parts/<material_id>/<part_id>.json
```

実装クラスは `MetalPartDefinitions` とし、`SimpleJsonResourceReloadListener` を継承する。

定義IDは、JSONの名前空間と `blacksmith/metal_parts/` 以下の相対パスから決定する。

例:

```text
data/craftbound/blacksmith/metal_parts/iron/pickaxe_head.json
→ craftbound:iron/pickaxe_head
```

### 4.2 読み込む項目

既存の鍛冶JSON仕様に従い、少なくとも以下を保持する。

```java
record MetalPartDefinition(
    ResourceLocation id,
    ResourceLocation metalId,
    int ingredientCount,
    ResourceLocation moldItemId,
    ResourceLocation outputItemId,
    FailureLumpDefinition failureLump,
    CoolingDefinition cooling,
    ForgingDefinition forging,
    PartQualityDefinition partQuality
) {}
```

ネストした定義は次の情報を持つ。

```java
record FailureLumpDefinition(
    ResourceLocation itemId,
    int count,
    int unitsPerItem
) {}

record CoolingDefinition(
    long surfaceSolidTicks,
    long safeTicks,
    int minimumBreakOnHit,
    ResourceLocation evaluatorId
) {}

record ForgingDefinition(
    double strengthMin,
    double strengthMax,
    double strengthPenaltyPerPoint,
    int idealHits,
    double hitCountPenalty,
    int breakOnHit,
    double strengthWeight,
    double hitCountWeight,
    ResourceLocation evaluatorId
) {}

record PartQualityDefinition(
    double heatingWeight,
    double forgingWeight,
    ResourceLocation evaluatorId
) {}
```

### 4.3 索引

読み込み完了時に、次の不変Mapを構築する。

```java
Map<ResourceLocation, MetalPartDefinition> definitionsById;
Map<MoldMetalKey, MetalPartDefinition> definitionsByMoldAndMetal;
```

```java
record MoldMetalKey(
    ResourceLocation moldItemId,
    ResourceLocation metalId
) {}
```

公開APIは少なくとも以下を持つ。

```java
Optional<MetalPartDefinition> get(ResourceLocation definitionId);

Optional<MetalPartDefinition> resolve(
    ItemStack mold,
    ResourceLocation metalId
);

boolean isRegisteredMold(ItemStack stack);
```

### 4.4 定義の検証

以下を満たさないJSONは、その定義だけを無効にする。

- `schema_version` が1である
- `ingredient_count` が1以上である
- `metal` が `MetalMaterialDefinitions` で解決できる
- `mold` が登録済みItemである
- `output` が登録済みItemである
- `failure_lump.item` が登録済みItemである
- `failure_lump.count` と `units_per_item` が1以上である
- `surface_solid_ticks` が0以上である
- `safe_ticks >= surface_solid_ticks` である
- `minimum_break_on_hit` が1以上である
- `forging.break_on_hit >= minimum_break_on_hit` である
- 強度範囲の下限が上限以下である
- 評価の重みが0以上で、必要な重み合計が0より大きい
- すべての評価関数IDが名前空間付きIDである

同じ `moldItemId + metalId` に複数定義が一致した場合は、暗黙に一方を選ばず、その組み合わせを解決不能としてログへ出力する。

### 4.5 スナップショット

加工開始後のデータパック再読み込みによって結果が変わらないよう、鋳造開始時には定義の必要部分を値としてコピーする。

```java
record MetalPartDefinitionSnapshot(
    ResourceLocation definitionId,
    ResourceLocation metalId,
    int ingredientCount,
    ResourceLocation moldItemId,
    ResourceLocation outputItemId,
    FailureLumpDefinition failureLump,
    CoolingDefinition cooling,
    ForgingDefinition forging,
    PartQualityDefinition partQuality
) {}
```

スナップショットはJSON定義への参照ではなく、独立した不変値とする。

---

## 5. 粗加工パーツ状態

### 5.1 Javaモデル

```java
record RoughMetalPartState(
    int version,
    UUID castingResultId,
    UUID castingOperatorId,
    ResourceLocation definitionId,
    ResourceLocation metalId,
    ResourceLocation outputItemId,
    int ingredientCount,
    long heatingTicks,
    int heatingScore,
    long coolingTicksAtRemoval,
    int effectiveBreakOnHit,
    MetalPartDefinitionSnapshot definitionSnapshot
) {}
```

`CURRENT_VERSION` は1とする。

### 5.2 不変条件

- `version == CURRENT_VERSION`
- すべてのUUIDとIDがNullでない
- `ingredientCount >= 1`
- `heatingTicks >= 0`
- `0 <= heatingScore <= 100`
- `coolingTicksAtRemoval >= 0`
- `effectiveBreakOnHit >= 1`
- `effectiveBreakOnHit <= definitionSnapshot.forging().breakOnHit()`
- `definitionId`、`metalId`、`outputItemId` がスナップショットと一致する
- スナップショットの `moldItemId` は空でない
- ItemStackは `craftbound:rough_metal_part` である

状態が不正なItemStackは操作不能とし、空状態へ初期化しない。

### 5.3 NBT形式

ルートキー:

```text
CraftboundRoughMetalPart
```

概念上の形式:

```text
CraftboundRoughMetalPart: {
    Version: 1,
    CastingResultId: <UUID>,
    CastingOperatorId: <UUID>,
    Definition: "craftbound:iron/pickaxe_head",
    Metal: "craftbound:iron",
    Output: "craftbound:iron_pickaxe_head",
    IngredientCount: 3,
    HeatingTicks: 320L,
    HeatingScore: 100,
    CoolingTicksAtRemoval: 100L,
    EffectiveBreakOnHit: 10,
    DefinitionSnapshot: {
        ...
    }
}
```

スナップショット内には、後続の鍛造再開と採点に必要な数値、評価関数ID、失敗出力定義をすべて保存する。

### 5.4 CodecとService

```java
final class RoughMetalPartStateCodec {
    static Optional<RoughMetalPartState> read(ItemStack stack);
    static void write(ItemStack stack, RoughMetalPartState state);
}
```

```java
final class RoughMetalPartStateService {
    static Optional<RoughMetalPartState> read(ItemStack stack);

    static ItemStack create(
        UUID castingResultId,
        UUID castingOperatorId,
        MetalPartDefinitionSnapshot snapshot,
        long heatingTicks,
        int heatingScore,
        long coolingTicksAtRemoval,
        int effectiveBreakOnHit
    );
}
```

外部クラスへ、NBTキーを直接操作させない。

`create` は不変条件を満たさない引数を受け取った場合、空ItemStackを返さず例外または明示的な失敗結果を返す。推奨は `Result` 相当の独自結果型、または `Optional<ItemStack>` である。

---

## 6. 金属塊状態

### 6.1 Javaモデル

```java
record MetalLumpState(
    int version,
    ResourceLocation metalId,
    int unitsPerItem
) {}
```

### 6.2 NBT

ルートキー:

```text
CraftboundMetalLump
```

```text
CraftboundMetalLump: {
    Version: 1,
    Metal: "craftbound:iron",
    UnitsPerItem: 1
}
```

### 6.3 Service

```java
final class MetalLumpStateService {
    static Optional<MetalLumpState> read(ItemStack stack);

    static ItemStack createSmall(
        ResourceLocation metalId,
        int count,
        int unitsPerItem
    );
}
```

`count <= 0`、`unitsPerItem <= 0`、不正な金属IDでは生成しない。

---

## 7. 表示

### 7.1 鋳型

各標準鋳型は固有の翻訳キー、アイテムモデル、16×16テクスチャを持つ。

```text
item.craftbound.blank_mold
item.craftbound.pickaxe_head_mold
...
```

アイテムモデル:

```text
assets/craftbound/models/item/<item_id>.json
```

テクスチャ:

```text
assets/craftbound/textures/item/<item_id>.png
```

テクスチャ生成時はプロジェクトの16×16テクスチャ生成規則に従う。

### 7.2 粗加工パーツ

粗加工パーツは共通アイコンを使用する。MVPではパーツ種別ごとの動的アイコンを実装しない。

ItemStackの表示名は保存された `outputItemId` から組み立てる。

例:

```text
粗加工の鉄製ピッケルヘッド
```

翻訳キー例:

```text
item.craftbound.rough_metal_part.named
```

正確な加熱評価、冷却tick、破損回数上限は通常ツールチップへ表示しない。将来、品質鑑定や加工感覚などのスキル表示から参照できるよう、状態データは保持する。

### 7.3 金属塊

金属IDを使用して動的表示名を生成する。

例:

```text
鉄の金属塊・小
```

金属表示名を解決できない場合は、リソースIDを表示してItemStack自体は保持する。

---

# 採用した実装方針

## 1. パッケージ

推奨構成:

```text
com.magu1436.craftbound.occupations.blacksmith.casting
├─ mold
│  └─ CastingMoldItem.java
├─ definition
│  ├─ MetalPartDefinition.java
│  ├─ MetalPartDefinitionSnapshot.java
│  ├─ MetalPartDefinitions.java
│  └─ MoldMetalKey.java
├─ part
│  ├─ RoughMetalPartItem.java
│  ├─ RoughMetalPartState.java
│  ├─ RoughMetalPartStateCodec.java
│  └─ RoughMetalPartStateService.java
└─ lump
   ├─ MetalLumpItem.java
   ├─ MetalLumpState.java
   ├─ MetalLumpStateCodec.java
   └─ MetalLumpStateService.java
```

## 2. レジストリ

`CraftboundItems` に以下を追加する。

- `BLANK_MOLD`
- 16種類の標準鋳型
- `ROUGH_METAL_PART`
- `SMALL_METAL_LUMP`

大量の標準鋳型登録を手書きで分散させず、同じ登録メソッドを使用する。

```java
private static RegistryObject<Item> registerCastingMold(String id) {
    return ITEMS.register(
        id,
        () -> new CastingMoldItem(new Item.Properties().stacksTo(1))
    );
}
```

## 3. NBTの扱い

現行の `CrucibleState`、`CrucibleStateCodec` と同様に、次を分離する。

- Java上の型付き不変レコード
- NBT変換専用Codec
- 状態操作専用Service
- Item本体

Forge 1.20.1では現行コードと同じItemStack NBTを使用する。Itemクラス内へNBTキー、検証、移行処理を直接記述しない。

## 4. データ再読み込み

`MetalPartDefinitions.INSTANCE` をサーバーのリソース再読み込みリスナーへ登録する。

再読み込み処理は新しいMapをローカルで構築し、すべての解析が終わってからvolatileフィールドを一括置換する。読み込み途中のMapをゲーム処理へ公開しない。

## 5. テストしやすい純粋処理

以下はワールドやプレイヤーに依存しない純粋処理として分離する。

- JSONからの定義解析
- `mold + metal` の索引生成
- 粗加工パーツ状態の不変条件検証
- NBTの読み書き
- 金属塊状態の読み書き

---

# 採用しなかった方針

## 金属・パーツごとに粗加工Itemを登録する

組み合わせ数が増え、他MOD連携のたびにJavaレジストリ追加が必要になるため採用しない。

## 1個の鋳型Itemへ種類をNBT保存する

レシピ、翻訳、アイテムモデル、データパック参照、JEI表示が複雑になるため採用しない。

## 鋳型Itemクラスへ金属と必要量を埋め込む

同じ鋳型を複数金属で使用できず、金属パーツJSONと責務が重複するため採用しない。

## 粗加工パーツへ定義IDだけを保存する

データパック再読み込み後に既存パーツの鍛造条件が変化するため採用しない。

## 不正NBTを空状態へ初期化する

素材や加工結果の消失、状態リセットによる不正利用につながるため採用しない。

## 鋳型へ耐久値を付ける

MVP仕様では繰り返し使用できるため採用しない。

---

# 関連する既存クラス

| クラス | 関係 |
|---|---|
| `Craftbound` | レジストリとリロードイベントの初期化元 |
| `CraftboundItems` | 新規Itemの登録先 |
| `CrucibleState` | 不変レコードと状態検証の参考 |
| `CrucibleStateCodec` | ItemStack NBT codecの参考 |
| `CrucibleStateService` | Item本体から状態操作を分離する参考 |
| `MetalMaterialDefinitions` | `SimpleJsonResourceReloadListener`、金属ID解決、JSON検証の既存実装 |
| `MetalDefinition` | 金属加熱特性と損失規則の参照元 |

参照仕様:

```text
docs/occupations/blacksmith/blacksmith.md
docs/occupations/blacksmith/data-formats.md
docs/occupations/blacksmith/equipment/stations-and-tools.md
docs/occupations/blacksmith/equipment/part-archetypes.md
docs/occupations/blacksmith/lifecycle.md
docs/occupations/blacksmith/iron-pickaxe.md
```

---

# データフロー

## 1. 起動・リロード

```text
データパックを読み込む
    ↓
MetalMaterialDefinitionsが金属定義を更新
    ↓
MetalPartDefinitionsが金属パーツJSONを解析
    ↓
登録Item、金属ID、評価値を検証
    ↓
definitionId索引を構築
    ↓
moldItemId + metalId索引を構築
    ↓
不変Mapへ一括置換
```

金属定義より後に金属パーツ定義を検証できる登録順を使用する。

## 2. 鋳型の入手

```text
通常クラフトで空鋳型を作る
    ↓
石切台で標準鋳型を1種類選ぶ
    ↓
標準鋳型を取得
    ↓
再変換不可
```

## 3. 後続の鋳造台から粗加工パーツを生成

```text
鋳造台がMetalPartDefinitionSnapshotを保持
    ↓
取り出し時の冷却tickを確定
    ↓
冷却評価サービスがeffectiveBreakOnHitを計算
    ↓
RoughMetalPartStateService.createを呼ぶ
    ↓
状態付きrough_metal_partを生成
    ↓
鍛造台へ渡す
```

## 4. 失敗時の金属塊

```text
鋳造または鍛造失敗を確定
    ↓
金属損失量と返却量を計算
    ↓
FailureLumpDefinitionを参照
    ↓
MetalLumpStateService.createSmallを呼ぶ
    ↓
金属ID付きsmall_metal_lumpを生成
```

---

# 擬似コード

```text
標準鋳型登録時:
    16個の固定リソースIDをCraftboundItemsへ登録する
    すべて最大スタック数1にする
    すべてcasting_moldsタグへ追加する
```

```text
金属パーツ定義リロード時:
    新しいdefinitionIdMapを作る
    新しいmoldMetalMapを作る

    各JSONについて:
        schema_versionを検証する
        必須IDと数値を解析する
        金属IDをMetalMaterialDefinitionsで解決する
        mold、output、failure_lumpのItemを解決する
        数値範囲と関係を検証する

        同じdefinitionIdが重複していたら無効にする
        同じmold + metalが重複していたら組み合わせを無効にする
        正常なら両Mapへ追加する

    読み込み完了後:
        Map.copyOfで不変化する
        volatileフィールドを一括置換する
```

```text
粗加工パーツ生成時:
    引数を検証する
    definitionSnapshotと各IDの一致を確認する
    coolingTicksAtRemovalを0以上にクランプしない
    不正値は明示的な失敗として返す
    正常ならRoughMetalPartStateを生成する
    rough_metal_partを1個作る
    Codecで状態を書き込む
    書き込み後に再読込して検証する
    正常なItemStackを返す
```

```text
粗加工パーツ読込時:
    対象Itemがrough_metal_partでなければ失敗
    ルートCompoundがなければ失敗
    Versionと全必須フィールドの型を確認
    ResourceLocationとUUIDを解析
    スナップショットを解析
    RoughMetalPartStateコンストラクタで不変条件を検証
    不正なら元NBTを変更せずOptional.emptyを返す
```

```text
小金属塊生成時:
    metalId、count、unitsPerItemを検証する
    small_metal_lumpのItemStackをcount個作る
    MetalLumpStateをNBTへ書く
    同じNBTの金属塊だけ通常のItemStack規則で結合させる
```

```text
粗加工パーツ表示名取得時:
    状態を読む
    読めなければ通常のアイテム名を返す
    outputItemIdをItemレジストリで解決する
    解決できれば「粗加工の%s」翻訳へ出力Item名を渡す
    解決できなければdefinitionIdを表示する
```

---

# 境界条件・例外

- 空鋳型は鋳造台へ設置できない。
- 標準鋳型は使用しても消費、耐久減少しない。
- `casting_molds` タグへ入っていても、対象金属との定義がなければ鋳造できない。
- 同じ鋳型でも、金属ごとに異なる定義を持てる。
- 同じ鋳型と金属の組み合わせへ複数定義がある場合は鋳造を拒否する。
- データパックから鋳型Itemが削除されても、既存の粗加工パーツは保存済みスナップショットから読める。
- 保存された出力Itemが削除されても、粗加工パーツNBTを初期化しない。
- 実装より新しいデータバージョンは上書きしない。
- 必須Compoundや型が不正なItemStackは、使用不能としてログへ記録する。
- 不正ItemStackを読み込むたびに大量ログを出さないよう、同一原因のログを抑制してよい。
- `heatingScore` や `effectiveBreakOnHit` をクライアントから受信して確定しない。
- 粗加工パーツと標準鋳型はスタック不可とする。
- 小金属塊は同一Itemかつ同一NBTの場合だけスタックする。
- データパック再読み込みは、既に生成済みの粗加工パーツへ遡及しない。
- `MetalPartDefinitions` の解決は読み込み済みMapを参照し、ゲームtickごとにJSONを読み直さない。
- 16種類の標準鋳型に対応する最終金属パーツJSONをすべて用意する作業は、基盤実装と分離してよい。ただし鉄製ピッケルヘッド定義を受け入れテスト用に1件用意する。

---

# 完了条件

- `develop` の現行登録方式に従って全Itemを登録できる。
- 空鋳型を指定材料から1個クラフトできる。
- 空鋳型から16種類の標準鋳型を石切台で作成できる。
- 標準鋳型を別種類または空鋳型へ戻せない。
- 標準鋳型が耐久値を持たず、使用しても減少しない。
- 空鋳型が `casting_molds` タグに含まれない。
- 標準鋳型が `casting_molds` タグに含まれる。
- `MetalPartDefinitions` が正常なJSONを読み込める。
- 不正JSONが他の正常定義を巻き込まず無効化される。
- 同じ鋳型と金属の重複定義を検出できる。
- 鉄とピッケルヘッド鋳型から鉄製ピッケルヘッド定義を解決できる。
- 粗加工パーツを1種類のItemとNBT状態で生成できる。
- 保存、再読込、サーバー再起動相当の往復で状態が一致する。
- 定義再読み込み後も既存の粗加工パーツ状態が変化しない。
- 不正NBTを自動初期化しない。
- 粗加工パーツがスタックしない。
- 異なる金属IDの小金属塊がスタックしない。
- 同じ金属IDと素材量の小金属塊がスタックできる。
- 通常ツールチップへ正確な加熱評価と破損回数上限を公開しない。
- クライアント専用表示コードを専用サーバー側から参照しない。
- `gradlew build` が成功する。

---

# 今回の対象外

- 鋳造台ブロックとBlock Entity
- るつぼからの金属消費
- 冷却tickの進行
- 加熱評価の確定
- 冷却破損上限の計算実行
- 鍛造台と鍛造ミニゲーム
- 最終金属パーツの生成
- 完成品の組み立て
- 専用鋳型のボス、構造物、ルートテーブルへの配置
- 中・大の金属塊
- 粗加工パーツのパーツ種別ごとの動的3Dモデル
- 16×16テクスチャの絵柄設計そのもの
- 全金属と全標準パーツのJSONデータ投入
