# 目的

Minecraft Forge 1.20.1 環境の Craftbound において、`feat/mold_and_rough_parts` ブランチで実装途中の粗加工金属パーツを、現在の「全パーツ共通の `craftbound:rough_metal_part` 1種類」方式から、以下の方式へ変更する。

```text
パーツ形状
    = パーツ種別ごとのItem + 共通テクスチャ

金属種類
    = ItemStackの状態データ

表示色
    = 金属ごとの動的Tint

加工結果
    = ItemStackの状態データ
```

例:

```text
craftbound:rough_pickaxe_head
    + metalId = craftbound:iron
    → 鉄色の粗加工ピッケルヘッド

craftbound:rough_pickaxe_head
    + metalId = craftbound:gold
    → 金色の粗加工ピッケルヘッド

craftbound:rough_sword_blade
    + metalId = craftbound:iron
    → 鉄色の粗加工剣身
```

本書は `magu1436/craftbound` の `feat/mold_and_rough_parts` ブランチを 2026-08-10 時点で直接参照し、既に実装されている以下を修正対象とする。

- `CraftboundItems`
- `RoughMetalPartItem`
- `RoughMetalPartState`
- `RoughMetalPartStateCodec`
- `RoughMetalPartStateService`
- `MetalPartDefinition`
- `MetalPartDefinitionSnapshot`
- `MetalPartDefinitions`
- `MetalPartSnapshotCodec`
- `CraftboundClientEvents`
- 金属パーツJSON
- 粗加工パーツのItem Model / Texture / Lang

現在実装済みの鋳型、小金属塊、金属パーツ定義の索引方式、粗加工パーツの品質・冷却・鍛造用状態データは可能な限り維持する。

今回の変更目的は、粗加工パーツの「形状」をItem種別で表し、「金属」を状態とTintで表す責務分離へ修正することである。

# 確定した仕様

## 1. 現在の実装から変更する点

現在のブランチでは以下が登録されている。

```java
public static final RegistryObject<Item> ROUGH_METAL_PART =
    ITEMS.register(
        "rough_metal_part",
        () -> new RoughMetalPartItem(
            new Item.Properties().stacksTo(1)
        )
    );
```

また `RoughMetalPartStateService` は、

```text
read:
    CraftboundItems.ROUGH_METAL_PART だけを受理

create:
    必ず CraftboundItems.ROUGH_METAL_PART を生成
```

となっている。

この方式を廃止する。

変更後は以下とする。

```text
RoughMetalPartItem Javaクラス
    → 1種類のまま共通利用

Registry Item
    → パーツ種別ごとに登録

Texture
    → パーツ種別ごとに1枚

Metal
    → NBT状態で保持

Metal Color
    → ItemColorによる動的Tint
```

Javaクラスをパーツごとに増やしてはならない。

---

## 2. 登録する粗加工パーツItem

現在実装されている16種類の標準鋳型に対応し、以下の16種類を登録する。

| RegistryObject | Item ID |
|---|---|
| `ROUGH_SWORD_BLADE` | `craftbound:rough_sword_blade` |
| `ROUGH_PICKAXE_HEAD` | `craftbound:rough_pickaxe_head` |
| `ROUGH_AXE_HEAD` | `craftbound:rough_axe_head` |
| `ROUGH_SHOVEL_HEAD` | `craftbound:rough_shovel_head` |
| `ROUGH_HOE_HEAD` | `craftbound:rough_hoe_head` |
| `ROUGH_HELMET_BODY` | `craftbound:rough_helmet_body` |
| `ROUGH_CHESTPLATE_BODY` | `craftbound:rough_chestplate_body` |
| `ROUGH_LEGGINGS_BODY` | `craftbound:rough_leggings_body` |
| `ROUGH_BOOTS_BODY` | `craftbound:rough_boots_body` |
| `ROUGH_HORSE_ARMOR_BODY` | `craftbound:rough_horse_armor_body` |
| `ROUGH_IRON_RING` | `craftbound:rough_iron_ring` |
| `ROUGH_CROSSBOW_TRIGGER` | `craftbound:rough_crossbow_trigger` |
| `ROUGH_SHIELD_BOSS` | `craftbound:rough_shield_boss` |
| `ROUGH_SHEARS_BLADES` | `craftbound:rough_shears_blades` |
| `ROUGH_FIRE_STRIKER` | `craftbound:rough_fire_striker` |
| `ROUGH_BRUSH_HEAD` | `craftbound:rough_brush_head` |

すべて以下の共通設定とする。

```text
Java型:
    RoughMetalPartItem

最大スタック数:
    1

耐久値:
    なし

金属:
    ItemStackの状態から決定

パーツ形状:
    Item IDから決定
```

登録ヘルパーを追加してよい。

```java
private static RegistryObject<Item> registerRoughMetalPart(
    String id
) {
    return ITEMS.register(
        id,
        () -> new RoughMetalPartItem(
            new Item.Properties().stacksTo(1)
        )
    );
}
```

### 廃止するItem

以下は削除する。

```text
craftbound:rough_metal_part
CraftboundItems.ROUGH_METAL_PART
```

このブランチは実装途中であり、正式リリース済みデータの互換維持は今回要求しない。

開発用ワールド内に旧 `rough_metal_part` が存在する場合のMigrationFixerは実装しない。

---

## 3. パーツと金属の責務

Item IDはパーツ形状のみを表す。

例えば、

```text
craftbound:rough_pickaxe_head
```

から判断できるのは、

```text
このItemはピッケルヘッド形状である
```

ことだけとする。

以下をItem IDへ埋め込まない。

```text
iron
gold
copper
その他MOD金属
```

したがって以下のようなItemは登録しない。

```text
craftbound:rough_iron_pickaxe_head
craftbound:rough_gold_pickaxe_head
craftbound:rough_copper_pickaxe_head
```

金属種類は引き続き `RoughMetalPartState.metalId` を正とする。

---

## 4. MetalPartDefinitionへrough_outputを追加

現在の `MetalPartDefinition` は、

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
)
```

となっている。

これを以下へ変更する。

```java
record MetalPartDefinition(
    ResourceLocation id,
    ResourceLocation metalId,
    int ingredientCount,
    ResourceLocation moldItemId,
    ResourceLocation roughOutputItemId,
    ResourceLocation outputItemId,
    FailureLumpDefinition failureLump,
    CoolingDefinition cooling,
    ForgingDefinition forging,
    PartQualityDefinition partQuality
)
```

### JSON

金属パーツ定義へ以下を追加する。

```json
"rough_output": "craftbound:rough_pickaxe_head"
```

既存の

```json
"output": "craftbound:iron_pickaxe_head"
```

は削除しない。

役割は以下。

```text
rough_output
    = 鋳造後、鍛造前の粗加工パーツItem

output
    = 鍛造完了後の完成金属パーツItem
```

例:

```json
{
  "schema_version": 1,
  "metal": "craftbound:iron",
  "ingredient_count": 3,
  "mold": "craftbound:pickaxe_head_mold",
  "rough_output": "craftbound:rough_pickaxe_head",
  "output": "craftbound:iron_pickaxe_head"
}
```

将来、金のピッケルヘッド定義を追加する場合も、

```json
{
  "metal": "craftbound:gold",
  "mold": "craftbound:pickaxe_head_mold",
  "rough_output": "craftbound:rough_pickaxe_head",
  "output": "craftbound:gold_pickaxe_head"
}
```

とする。

つまり、

```text
同一パーツ:
    rough_outputは金属間で共通

完成パーツ:
    outputは金属ごとに異なる
```

### parse時の検証

`MetalPartDefinitions` は `rough_output` をregistered itemとして解決する。

追加検証:

```text
rough_outputが登録済みItemである
rough_outputのItemがRoughMetalPartItemである
```

Craftbound外のMODが粗加工パーツを追加する場合は、Craftbound互換の `RoughMetalPartItem` を登録したうえでデータ定義から参照する。

MVPでは、データパックだけで未知の新規粗加工パーツItemを追加することは対象外とする。

### 命名規則から推測しない

以下は採用しない。

```text
pickaxe_head_mold
    ↓ 文字列加工
rough_pickaxe_head
```

どの粗加工Itemを生成するかは必ず `rough_output` に明示する。

---

## 5. MetalPartDefinitionSnapshot

現在のsnapshotへ `roughOutputItemId` を追加する。

```java
public record MetalPartDefinitionSnapshot(
    ResourceLocation definitionId,
    ResourceLocation metalId,
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

`MetalPartDefinition#snapshot()` でも値を引き継ぐ。

これにより `/reload` 後も開始済み鋳造・取り出し済み粗加工パーツが、生成対象の粗加工Itemを現在のJSONへ再問い合わせせず識別できる。

---

## 6. MetalPartSnapshotCodec

現在のsnapshot NBTへ以下を追加する。

```text
RoughOutput: "craftbound:rough_pickaxe_head"
```

例:

```text
DefinitionSnapshot:
{
    Definition: "craftbound:iron/pickaxe_head",
    Metal: "craftbound:iron",
    Mold: "craftbound:pickaxe_head_mold",
    RoughOutput: "craftbound:rough_pickaxe_head",
    Output: "craftbound:iron_pickaxe_head",
    ...
}
```

`read` 時は `RoughOutput` を必須とする。

`valid` では以下を追加する。

```text
roughOutputItemId != null
ForgeRegistries.ITEMSに存在する
ItemがRoughMetalPartItemである
```

旧snapshot形式の自動移行は今回実装しない。

---

## 7. RoughMetalPartState

現在保持している以下は維持する。

```text
castingResultId
castingOperatorId
definitionId
metalId
outputItemId
ingredientCount
heatingTicks
heatingScore
coolingTicksAtRemoval
effectiveBreakOnHit
definitionSnapshot
```

Itemがパーツ種別を表すようになっても、

```text
definitionId
outputItemId
definitionSnapshot
```

は削除しない。

理由:

- `definitionId` はどの金属パーツ定義から生成されたかを示す
- `outputItemId` は鍛造完了後の完成Itemを示す
- `definitionSnapshot` は `/reload` から開始済み工程を隔離する
- Item IDだけでは金属固有の鍛造・品質条件を復元できない

ただし粗加工Item IDをトップレベルへ重複保存しない。

粗加工Item IDは、

```text
実際のItemStack.getItem()
definitionSnapshot.roughOutputItemId
```

の一致で検証する。

---

## 8. 金属描画情報

粗加工パーツは金属ごとの専用Textureを持たない。

ItemStackへ、クライアントが色を決定するための最小情報を保存する。

共通クラスとして以下を追加する。

```java
public record MetalVisualData(
    @Nullable Integer explicitRgb,
    @Nullable ResourceLocation representativeItemId
) {}
```

配置候補:

```text
com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData
```

この型は後続の鋳造台上の溶融金属描画でも再利用する。

### RoughMetalPartStateへの追加

```java
MetalVisualData visualData
```

を追加する。

例:

```java
public record RoughMetalPartState(
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
    MetalPartDefinitionSnapshot definitionSnapshot,
    MetalVisualData visualData
) {}
```

`visualData` はゲーム判定へ使用しない。

描画専用のsnapshotである。

---

## 9. MetalDefinitionの描画情報

現在の金属定義へ任意の表示色を追加する。

```java
public record MetalDefinition(
    ResourceLocation id,
    double lumpLossRatio,
    LumpLossRounding lumpLossRounding,
    long castableAfterTicks,
    List<HeatingScorePoint> scoreCurve,
    long dangerAfterTicks,
    long destroyAfterTicks,
    ResourceLocation heatingEvaluator,
    @Nullable Integer displayColor
) {}
```

JSON:

```json
"display_color": "#A7A7A7"
```

規則:

- 任意項目
- `#RRGGBB`
- 省略可能
- 省略した既存JSONはそのまま読み込める
- 不正値はその金属定義をinvalidとする
- schema_versionは1のままとする

この値は、

```text
特殊なMOD金属
リソースパック上の見た目から平均色を取りにくい金属
意図した色を明示したい金属
```

で利用する。

---

## 10. representativeItemId

`display_color` がない場合、金属の代表素材Itemからクライアント側で平均色を求める。

`MetalMaterialDefinitions` へ以下を追加する。

```java
public Optional<ResourceLocation> getRepresentativeItemId(
    ResourceLocation metalId
);
```

現在の `ingredients` を定義順に走査し、

```text
対象metalId
    ↓
最初の直接item指定
    ↓
そのItemのRegistry ID
```

を返す。

例:

```text
craftbound:iron
    ingredients:
        minecraft:iron_ingot
        minecraft:iron_block
        minecraft:raw_iron

    representativeItemId:
        minecraft:iron_ingot
```

タグしか持たない金属では `Optional.empty()` でよい。

MVPではタグ内から代表Itemを自動選択しない。

---

## 11. MetalVisualDataの生成

共通Serviceを追加する。

```java
public final class MetalVisualDataService {

    public static Optional<MetalVisualData> resolve(
        ResourceLocation metalId
    );
}
```

処理:

```text
MetalMaterialDefinitions.INSTANCE.get(metalId)
    ↓
MetalDefinition取得失敗:
    empty

displayColorあり:
    MetalVisualData(
        explicitRgb = displayColor,
        representativeItemId = 任意
    )

displayColorなし:
    getRepresentativeItemId(metalId)
    MetalVisualData(
        explicitRgb = null,
        representativeItemId = 取得結果
    )
```

代表Itemを取得できない場合も、

```java
new MetalVisualData(null, null)
```

として生成可能とする。

この場合クライアントは既定灰色へフォールバックする。

---

## 12. RoughMetalPartStateCodec

現在の `CraftboundRoughMetalPart` ROOTは維持する。

以下を追加する。

```text
Visual:
{
    ExplicitRgb: <int>       // 任意
    RepresentativeItem: <string> // 任意
}
```

`MetalVisualDataCodec` を独立クラスとして実装してよい。

例:

```java
public final class MetalVisualDataCodec {

    public static CompoundTag write(MetalVisualData data);

    public static Optional<MetalVisualData> read(
        CompoundTag tag
    );
}
```

検証:

```text
ExplicitRgb:
    存在する場合 0x000000..0xFFFFFF

RepresentativeItem:
    存在する場合 namespaced ResourceLocation
    ForgeRegistries.ITEMSに存在する
```

両方ない状態は有効とする。

---

## 13. RoughMetalPartStateService.read

現在の、

```java
stack.is(CraftboundItems.ROUGH_METAL_PART.get())
```

を廃止する。

変更後:

```text
stackが空:
    empty

stack.getItem() が RoughMetalPartItem ではない:
    empty

RoughMetalPartStateCodec.read:
    失敗ならempty

実際のstack Item IDを取得
snapshot.roughOutputItemIdを取得

両者が不一致:
    empty

一致:
    stateを返す
```

概念コード:

```java
public static Optional<RoughMetalPartState> read(
    ItemStack stack
) {
    if (stack.isEmpty()
        || !(stack.getItem() instanceof RoughMetalPartItem)) {
        return Optional.empty();
    }

    Optional<RoughMetalPartState> result =
        RoughMetalPartStateCodec.read(stack);

    if (result.isEmpty()) {
        return Optional.empty();
    }

    ResourceLocation actualItemId =
        ForgeRegistries.ITEMS.getKey(stack.getItem());

    if (!result.get()
        .definitionSnapshot()
        .roughOutputItemId()
        .equals(actualItemId)) {
        return Optional.empty();
    }

    return result;
}
```

---

## 14. RoughMetalPartStateService.create

現在の、

```java
new ItemStack(CraftboundItems.ROUGH_METAL_PART.get())
```

を廃止する。

生成Itemはsnapshotの `roughOutputItemId` から解決する。

処理:

```text
snapshot.roughOutputItemIdを取得
    ↓
ForgeRegistries.ITEMSからItemを解決
    ↓
未登録:
    empty
    ↓
RoughMetalPartItemでない:
    empty
    ↓
MetalVisualDataService.resolve(snapshot.metalId)
    ↓
RoughMetalPartState生成
    ↓
new ItemStack(roughOutputItem)
    ↓
Codec.write
    ↓
readで再検証
```

シグネチャは現在のものを維持してもよい。

```java
public static Optional<ItemStack> create(
    UUID resultId,
    UUID operatorId,
    MetalPartDefinitionSnapshot snapshot,
    long heatingTicks,
    int heatingScore,
    long coolingTicksAtRemoval,
    int effectiveBreakOnHit
)
```

`visualData` はService内部でmetalIdから解決する。

ただし、後続の鋳造台で流し込み開始時点のvisual snapshotを固定する設計を採用する場合は、最終的に以下へ変更してよい。

```java
create(
    ...,
    MetalVisualData visualData
)
```

鋳造台実装時には、開始済み工程で `/reload` によってrepresentative itemが変わらないよう、鋳造開始時に `MetalVisualData` を固定して粗加工パーツへ引き継ぐ方式を優先する。

---

## 15. RoughMetalPartItem

Javaクラスは1つのまま維持する。

パーツごとのサブクラスは作成しない。

現在の `getName` は基本的に維持できる。

現在:

```text
state.outputItemId
    ↓
完成パーツItemの表示名
    ↓
item.craftbound.rough_metal_part.named
```

例:

```text
iron_pickaxe_head = 鉄製ピッケルヘッド
    ↓
粗加工の鉄製ピッケルヘッド
```

この動作は新方式でも利用する。

### 状態がない場合の名前

NBTがない、または不正なItemStackではItem IDごとの通常翻訳を表示する。

例:

```text
item.craftbound.rough_pickaxe_head
    = 粗加工ピッケルヘッド
```

正常状態では現在と同じ動的名称を優先する。

---

## 16. Item Model

旧:

```text
models/item/rough_metal_part.json
```

は削除する。

各パーツに1つずつ追加する。

例:

```text
models/item/rough_pickaxe_head.json
models/item/rough_sword_blade.json
...
```

形式:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "craftbound:item/rough_pickaxe_head"
  }
}
```

すべてlayer0だけを使用する。

`layer0` へItemColor Tintを適用する。

---

## 17. 必要な粗加工パーツTexture

以下の16枚を用意する。

```text
rough_sword_blade.png
rough_pickaxe_head.png
rough_axe_head.png
rough_shovel_head.png
rough_hoe_head.png

rough_helmet_body.png
rough_chestplate_body.png
rough_leggings_body.png
rough_boots_body.png
rough_horse_armor_body.png

rough_iron_ring.png
rough_crossbow_trigger.png
rough_shield_boss.png
rough_shears_blades.png
rough_fire_striker.png
rough_brush_head.png
```

配置:

```text
src/main/resources/assets/craftbound/textures/item/
```

### Texture設計規則

- 16×16 RGBA PNG
- 透明背景
- 1文字 = 1ピクセルのJSON設計図を正とする既存プロジェクト規則に従う
- 高解像度画像を縮小して作らない
- パーツ形状ごとに1枚だけ
- 金属ごとのTextureは作らない
- 色はニュートラルな白～グレースケールを中心にする
- Tint乗算後にも立体感が残るよう、ハイライトと影をグレースケールの明度差で表現する
- 鉄色、金色、銅色をTexture自体へ焼き込まない

旧:

```text
rough_metal_part.png
```

は削除する。

---

## 18. Item Color登録

Forge 1.20.1 の `RegisterColorHandlersEvent.Item` を使用する。

`CraftboundClientEvents` は既にMOD Bus / `Dist.CLIENT` のsubscriberなので、ここへ追加する。

```java
@SubscribeEvent
public static void registerItemColors(
    RegisterColorHandlersEvent.Item event
) {
    event.register(
        RoughMetalPartColorHandler::getColor,
        CraftboundItems.ROUGH_SWORD_BLADE.get(),
        CraftboundItems.ROUGH_PICKAXE_HEAD.get(),
        CraftboundItems.ROUGH_AXE_HEAD.get(),
        ...
        CraftboundItems.ROUGH_BRUSH_HEAD.get()
    );
}
```

粗加工パーツすべてへ同じhandlerを登録する。

パーツごとの色handlerを作らない。

---

## 19. RoughMetalPartColorHandler

クライアント専用クラスとして実装する。

配置候補:

```text
com.magu1436.craftbound.occupations.blacksmith.client.RoughMetalPartColorHandler
```

責務:

```text
ItemStack
    ↓
RoughMetalPartStateService.read
    ↓
MetalVisualData
    ↓
MetalRenderColorResolver
    ↓
Tint色
```

疑似仕様:

```text
tintIndex != 0:
    白を返す

state読取失敗:
    白を返す

visualData取得:
    MetalRenderColorResolver.resolve

解決失敗:
    既定灰色
```

正常な金属色は不透明色として返す。

---

## 20. MetalRenderColorResolver

鋳造台でも再利用するクライアント専用Resolverとする。

配置候補:

```text
com.magu1436.craftbound.occupations.blacksmith.client.MetalRenderColorResolver
```

優先順位:

```text
1. MetalVisualData.explicitRgb
2. representativeItemIdのTexture平均色
3. DEFAULT_COLOR = 0x9A9A9A
```

### 平均色

representative itemのBakedModelから代表TextureAtlasSpriteを取得する。

MVPでは以下を優先する。

```text
ItemRenderer#getModel
    ↓
BakedModel#getParticleIcon
    ↓
TextureAtlasSprite / SpriteContents
```

代表spriteを取得できない場合は、BakedQuadからspriteを取得するフォールバックを実装してよい。

ピクセル平均:

```text
完全透過(alpha == 0)
    → 集計しない

alpha > 0
    → RGBを加算
    → count++

count == 0
    → DEFAULT_COLOR

count > 0
    → RGB平均
```

### Cache

平均色は毎フレーム計算しない。

```java
Map<ResourceLocation, Integer> colorCache;
```

で `representativeItemId` ごとにcacheする。

リソース再読み込みでcacheをclearする。

リソースパック変更後も旧Textureの平均色を使い続けてはならない。

---

## 21. 金属色とTextureの合成

粗加工Textureはグレースケールで作成し、ItemColorの色乗算を利用する。

概念:

```text
rough_pickaxe_head.png
    白～グレーの形状

× Metal Color
    iron = gray
    gold = yellow
    copper = orange-brown

= 表示Item
```

Textureの暗部はTint後も暗くなり、ハイライト部は金属色に近くなる。

したがって単色Textureを作らず、明度差を持つグレースケールTextureとする。

---

## 22. Lang

旧:

```text
item.craftbound.rough_metal_part
```

は削除する。

正常状態の動的名称用:

```text
item.craftbound.rough_metal_part.named
```

は維持する。

各Itemのfallback名称を追加する。

例:

```json
"item.craftbound.rough_sword_blade": "粗加工剣身",
"item.craftbound.rough_pickaxe_head": "粗加工ピッケルヘッド",
"item.craftbound.rough_axe_head": "粗加工斧頭"
```

英語も同様に追加する。

---

## 23. アイテムタグ

粗加工金属パーツ共通判定用として以下を追加する。

```text
craftbound:rough_metal_parts
```

配置:

```text
data/craftbound/tags/items/rough_metal_parts.json
```

16種類すべてを含める。

用途:

- データ上で粗加工パーツ群を識別する
- 将来の鍛造台入力判定
- 他MOD連携
- Recipe / advancement等

ただし `RoughMetalPartStateService` の状態読取では、

```text
タグだけ
```

を正としない。

NBT契約を扱うJavaコードでは `RoughMetalPartItem` とsnapshot整合性も検証する。

---

# 採用した実装方針

## パーツごとにItemを分ける

パーツの見た目・形状とMinecraft上のItem IDが一致し、デバッグ、モデル、Texture、鍛造台入力の理解が容易になるため。

## 金属ごとにはItemを分けない

金属追加のたびにItem登録、Model、Textureを増やす必要がなく、他MOD金属への拡張性を維持できるため。

## RoughMetalPartItemクラスは共通化する

パーツごとの差はRegistry IDとTextureだけであり、状態読取・名称・加工ロジックは共通だから。

## rough_outputをJSONへ明示する

現在の金属パーツ定義が既にmoldとoutputを明示するデータ駆動設計であり、暗黙の文字列命名規則を追加しないため。

## 加工状態NBTは維持する

Item IDだけでは金属、加熱評価、冷却結果、鍛造条件、定義snapshotを表現できないため。

## MetalVisualDataをItemStackへ保存する

Dedicated Server側のデータ定義とClient側のTexture Resourceを直接共有できないため、金属色決定に必要な最小情報をItemStackへ持たせる。

## 平均色はClientで計算する

TextureとResource PackはClient Resourceだから。

## 金属色Resolverを粗加工パーツ専用にしない

後続の鋳造台上の溶融金属描画でも同じ色解決が必要だから。

# 採用しなかった方針

## `rough_metal_part` 1種類を継続

パーツ形状までNBTと動的モデルで解決する必要があり、現在の固定16パーツでは構造が抽象的すぎるため。

## 金属×パーツごとにItem登録

組み合わせ数が金属追加に比例して増大し、他MOD金属対応と相性が悪いため。

## `RoughPickaxeHeadItem` 等のJavaサブクラスを16個作る

パーツ固有のJava動作がなく、重複コードになるため。

## mold IDからrough Item IDを文字列変換

リソースID命名規則への暗黙依存を作るため。

## 金属ごとにTextureを作る

金属追加のたびにアセットが増えるため。

## metalIdだけを見てClientでMetalMaterialDefinitionsを再検索

Dedicated ServerのServer Data Reload状態をClient描画の前提にできないため。

## 平均RGBそのものをServer側NBTへ保存

ServerはClientのTexture/Resource Packを参照できず、見た目と一致する値を計算できないため。

## 毎フレーム代表Textureを走査

描画負荷が不要に増えるため。

# 関連する既存クラス

## `CraftboundItems`

現在:

```text
16標準鋳型
ROUGH_METAL_PART
SMALL_METAL_LUMP
IRON_PICKAXE_HEAD
```

が登録されている。

変更:

```text
ROUGH_METAL_PARTを削除
16種類のROUGH_*を追加
```

鋳型と小金属塊は変更しない。

## `RoughMetalPartItem`

現在の動的名称処理を維持する。

パーツ固有サブクラスは作らない。

## `RoughMetalPartState`

現在の品質・工程状態を維持する。

追加:

```text
MetalVisualData visualData
```

## `RoughMetalPartStateCodec`

現在のROOT:

```text
CraftboundRoughMetalPart
```

を維持する。

Visual dataの読書きを追加する。

## `RoughMetalPartStateService`

最も大きく変更する。

現在:

```text
Item固定
```

変更後:

```text
snapshot.roughOutputItemIdから生成Itemを解決
```

## `MetalPartDefinition`

追加:

```text
roughOutputItemId
```

## `MetalPartDefinitionSnapshot`

追加:

```text
roughOutputItemId
```

## `MetalPartDefinitions`

JSON `rough_output` をparse・validateする。

既存の

```text
definition ID index
mold + metal index
```

は変更しない。

## `MetalPartSnapshotCodec`

NBT `RoughOutput` を追加する。

## `MetalMaterialDefinitions`

追加:

```java
getRepresentativeItemId(metalId)
```

既存の金属素材解決は維持する。

## `MetalDefinition`

追加:

```text
optional displayColor
```

## `CraftboundClientEvents`

現在はCrucible Screen登録のみ。

追加:

```text
RegisterColorHandlersEvent.Item
```

## `rough_metal_part.json`

削除対象。

## `rough_metal_part.png`

削除対象。

# データフロー

## データパック読み込み

```text
iron/pickaxe_head.json
    ↓
mold = pickaxe_head_mold
rough_output = rough_pickaxe_head
output = iron_pickaxe_head
metal = iron
    ↓
MetalPartDefinitions.parse
    ↓
MetalPartDefinition
    ↓
snapshot生成時
roughOutputItemIdも固定
```

## 粗加工パーツ生成

```text
鋳造結果確定
    ↓
MetalPartDefinitionSnapshot
    ↓
roughOutputItemId取得
    ↓
craftbound:rough_pickaxe_headをRegistryから解決
    ↓
metalIdからMetalVisualData取得
    ↓
RoughMetalPartState生成
    ↓
rough_pickaxe_head ItemStackへNBT保存
```

## Item描画

```text
rough_pickaxe_head ItemStack
    ↓
Item Model
    ↓
rough_pickaxe_head.png
    ↓
RegisterColorHandlersEvent.Item
    ↓
RoughMetalPartColorHandler
    ↓
RoughMetalPartState.visualData
    ↓
explicitRgbあり:
    その色

なし:
    representativeItemId
        ↓
    Client Texture平均色
        ↓
    Cache

取得不能:
    DEFAULT_GRAY
    ↓
Texture × Tint
```

## 鍛造台への受け渡し

```text
rough_pickaxe_head
    ↓
RoughMetalPartStateService.read
    ↓
実Item ID
    == snapshot.roughOutputItemId
    ↓
状態有効
    ↓
metalId / heatingScore / cooling結果 /
forging snapshotを鍛造処理へ渡す
```

# 擬似コード

```text
CraftboundItems登録:
    ROUGH_SWORD_BLADEをRoughMetalPartItemとして登録
    ROUGH_PICKAXE_HEADをRoughMetalPartItemとして登録
    ...
    ROUGH_BRUSH_HEADをRoughMetalPartItemとして登録

    ROUGH_METAL_PARTは削除
```

```text
MetalPartDefinitions.parse:
    metalを読む
    ingredient_countを読む
    moldを読む

    rough_outputを読む
    rough_outputが登録済みか確認
    rough_output ItemがRoughMetalPartItemか確認

    outputを読む
    failure_lumpを読む
    coolingを読む
    forgingを読む
    part_qualityを読む

    MetalPartDefinitionを生成
```

```text
RoughMetalPartStateService.create:
    snapshot.roughOutputItemIdを取得
    RegistryからItemを取得

    Itemが存在しない:
        empty

    RoughMetalPartItemではない:
        empty

    MetalVisualDataを取得

    RoughMetalPartStateを生成
    rough output ItemStackを生成
    NBTを書き込む

    readで再読込
    整合する:
        ItemStackを返す

    整合しない:
        empty
```

```text
RoughMetalPartStateService.read:
    stackが空:
        empty

    stack ItemがRoughMetalPartItemでない:
        empty

    Codec.read
    失敗:
        empty

    actualItemIdをRegistryから取得

    actualItemId != snapshot.roughOutputItemId:
        invalid
        empty

    stateを返す
```

```text
MetalVisualDataService.resolve:
    metalIdからMetalDefinition取得

    失敗:
        empty

    displayColorあり:
        explicitRgbへ設定

    representative itemを取得できる:
        representativeItemIdへ設定

    どちらもない:
        null / null

    MetalVisualDataを返す
```

```text
RoughMetalPartColorHandler:
    tintIndex != 0:
        WHITE

    RoughMetalPartStateService.read
    失敗:
        WHITE

    visualDataを取得
    MetalRenderColorResolver.resolve

    色を返す
```

```text
MetalRenderColorResolver.resolve:
    visualData.explicitRgbあり:
        return explicitRgb

    representativeItemIdなし:
        return DEFAULT_GRAY

    cacheに存在:
        return cachedColor

    Item modelを取得
    TextureAtlasSpriteを取得

    RGB合計 = 0
    pixelCount = 0

    spriteの各ピクセル:
        alpha == 0:
            continue

        RGBを加算
        pixelCount++

    pixelCount == 0:
        color = DEFAULT_GRAY
    それ以外:
        color = RGB平均

    cacheへ保存
    return color
```

# 境界条件・例外

- `rough_output` が存在しないJSONはinvalidとして読み込まない
- `rough_output` が未登録Itemならinvalid
- `rough_output` が `RoughMetalPartItem` でなければinvalid
- `output` と `rough_output` を混同しない
- 同じパーツの異なる金属定義では同じ `rough_output` を指定してよい
- 同じ `rough_output` から異なる完成 `output` を生成してよい
- Item IDに金属名を含めない
- `metalId` は引き続きNBT状態を正とする
- NBTがない粗加工Itemはゲーム工程上は無効として扱う
- 不正NBTを自動で正常化しない
- ItemStack実体とsnapshot.roughOutputItemIdが異なる場合は無効
- `definitionId` をItem IDから逆算しない
- `/reload` 後もItemStackのsnapshotを維持する
- ItemColorはゲーム状態を変更しない
- Dedicated ServerでClient専用色解決クラスをロードしない
- `display_color` がない既存金属JSONを読み込める
- representativeItemが存在しなくても描画クラッシュしない
- representativeItemのModel取得に失敗してもDEFAULT_GRAYへfallbackする
- 透明Textureだけの場合もDEFAULT_GRAYへfallbackする
- 平均色は毎フレーム計算しない
- Resource Pack reload後は色cacheを破棄する
- invalid状態のItemは白Tintまたはfallback表示とし、描画クラッシュしない
- Texture自体へ鉄・金・銅固有色を焼き込まない
- 金属ごとのItem Modelを作らない
- 金属ごとのTextureを作らない
- 旧 `rough_metal_part` のMigrationは実装しない
- 小金属塊の現在実装をこの変更で壊さない
- 鋳型Itemとcasting_moldsタグをこの変更で壊さない
- `MetalPartDefinitions.resolve(mold, metalId)` の索引方式を変更しない

# 完了条件

- `CraftboundItems.ROUGH_METAL_PART` が削除されている
- 16種類の `ROUGH_*` RegistryObjectが登録されている
- すべて `RoughMetalPartItem` を使用している
- すべてstack size 1である
- `craftbound:rough_metal_parts` タグに16種類が含まれる
- `MetalPartDefinition` に `roughOutputItemId` が存在する
- `MetalPartDefinitionSnapshot` に `roughOutputItemId` が存在する
- `MetalPartSnapshotCodec` が `RoughOutput` を保存・復元できる
- `MetalPartDefinitions` がJSONの `rough_output` を検証できる
- iron/pickaxe_head.json が `craftbound:rough_pickaxe_head` を指定する
- `RoughMetalPartStateService.create` がsnapshotに応じて異なるItemを生成できる
- `RoughMetalPartStateService.read` が16種類の粗加工パーツを受理できる
- 実Itemとsnapshotのrough outputが不一致ならreadが失敗する
- `RoughMetalPartState` が既存の加熱・冷却・鍛造情報を維持する
- `RoughMetalPartState` に描画用 `MetalVisualData` が保存される
- `display_color` を金属JSONへ任意指定できる
- `MetalMaterialDefinitions#getRepresentativeItemId` が直接item定義から代表Itemを返せる
- `RegisterColorHandlersEvent.Item` で16種類へ同じTint handlerが登録される
- `rough_pickaxe_head` と `rough_sword_blade` が異なるTexture形状で表示される
- 同じ `rough_pickaxe_head` でもironとgoldで色が変わる
- 鉄用・金用・銅用の粗加工パーツTextureを個別に作っていない
- explicit colorがあればそれを使用する
- explicit colorがなければ代表Item Textureの平均色を使用する
- 平均色取得不能なら `0x9A9A9A` を使用する
- Texture平均色をcacheできる
- Resource Pack reloadでcacheを破棄できる
- 旧 `rough_metal_part.json` が削除されている
- 旧 `rough_metal_part.png` が削除されている
- 16種類のItem Modelが存在する
- 16種類の16×16粗加工Textureが存在する
- 正常状態の表示名が金属固有の完成パーツ名を利用して表示される
- NBTなしItemではパーツごとのfallback名称が表示される
- Dedicated Server起動時にClient class load errorが発生しない
- 既存の鋳型・小金属塊機能が動作する
- MetalPartDefinitionsのmold + metal解決が従来どおり動作する
- `gradlew build` が成功する

# 今回の対象外

- 鋳型の仕様変更
- 空鋳型の仕様変更
- 鋳型Textureの再作成
- 小金属塊の状態設計変更
- 鋳造台本体
- 鋳造台の冷却tick
- 鋳造台BER
- 鋳造台上の金属面描画
- 金属溶鉱炉
- るつぼ
- 鍛造台
- 鍛造ミニゲーム
- 完成金属パーツの実装拡張
- データパックだけによる未知の新規粗加工Item登録
- 旧 `craftbound:rough_metal_part` のworld migration
- 金属ごとの専用Texture
- 金属ごとの粗加工Item
- パーツごとのJavaサブクラス
- 複雑なPBR・反射表現
- Resource Packごとの明示色補正UI
