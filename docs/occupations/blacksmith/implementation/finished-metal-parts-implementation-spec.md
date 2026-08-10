# 完成金属パーツ 実装仕様書

# 目的

Craftbound の完成金属パーツを、**パーツ種別ごとの共通 Item + 金属状態 + 動的 Tint** で実装する。

基本モデルは以下。

```text
パーツ形状 = Item ID / 共通Texture
金属種類   = MetalPartState.metalId
表示色     = MetalPartState.visualData を MetalRenderColorResolver で解決
```

例:

```text
craftbound:pickaxe_head
    + metalId = craftbound:iron
    → 鉄色のピッケルヘッド

craftbound:pickaxe_head
    + metalId = craftbound:gold
    → 金色のピッケルヘッド
```

金属ごとの Item / Model / Texture は作成しない。

本書をこの実装で最初に読む唯一の方針仕様書とする。

## Codex の読み込み範囲

実装開始時に読むファイルは原則として以下だけでよい。

```text
src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/finished/
    MetalPartItem.java
    MetalPartState.java
    MetalPartStateCodec.java
    MetalPartStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/definition/
    MetalPartDefinition.java
    MetalPartDefinitions.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/part/
    RoughMetalPartState.java
    RoughMetalPartStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/client/
    MetalPartColorHandler.java
    MetalRenderColorResolver.java

src/main/java/com/magu1436/craftbound/client/event/
    CraftboundClientEvents.java

src/main/resources/data/craftbound/blacksmith/metal_parts/
src/main/resources/assets/craftbound/models/item/
src/main/resources/assets/craftbound/lang/
```

以下は原則として読まない。

- 過去の仕様書
- 他職業のコード
- 鋳造台・溶鉱炉・るつぼの詳細実装
- Git履歴
- 他ブランチ
- 無関係なdocs
- リポジトリ全体の一括探索

必要なシンボルが見つからない場合のみ、そのシンボル名で限定検索する。

# 確定した仕様

## 1. 完成パーツのItem構造

完成パーツはパーツ種別ごとに1 Item登録する。

| Item ID | RegistryObject |
|---|---|
| `sword_blade` | `SWORD_BLADE` |
| `pickaxe_head` | `PICKAXE_HEAD` |
| `axe_head` | `AXE_HEAD` |
| `shovel_head` | `SHOVEL_HEAD` |
| `hoe_head` | `HOE_HEAD` |
| `helmet_body` | `HELMET_BODY` |
| `chestplate_body` | `CHESTPLATE_BODY` |
| `leggings_body` | `LEGGINGS_BODY` |
| `boots_body` | `BOOTS_BODY` |
| `horse_armor_body` | `HORSE_ARMOR_BODY` |
| `iron_ring` | `IRON_RING` |
| `crossbow_trigger` | `CROSSBOW_TRIGGER` |
| `shield_boss` | `SHIELD_BOSS` |
| `shears_blades` | `SHEARS_BLADES` |
| `fire_striker` | `FIRE_STRIKER` |
| `brush_head` | `BRUSH_HEAD` |

すべて既存の `MetalPartItem` を使用し、最大スタック数は1とする。

```java
private static RegistryObject<Item> registerMetalPart(String id) {
    return ITEMS.register(
        id,
        () -> new MetalPartItem(
            new Item.Properties().stacksTo(1)
        )
    );
}
```

パーツ別Javaクラスは作成しない。

## 2. 金属ごとのItemを作成しない

禁止:

```text
iron_pickaxe_head
gold_pickaxe_head
copper_pickaxe_head
```

金属差は Item ID ではなく `MetalPartState` で表現する。

現在の状態型を維持する。

```java
public record MetalPartState(
    int version,
    ResourceLocation metalId,
    MetalVisualData visualData
) {}
```

NBT root は既存の `CraftboundMetalPart` を維持する。

## 3. MetalPartDefinition.output

金属パーツ定義の `output` は、金属固有Itemではなく完成パーツ共通Itemを指す。

```json
{
  "metal": "craftbound:iron",
  "mold": "craftbound:pickaxe_head_mold",
  "rough_output": "craftbound:rough_pickaxe_head",
  "output": "craftbound:pickaxe_head"
}
```

同一パーツでは金属が異なっても `output` は共通。

```text
iron/pickaxe_head.json
    output = craftbound:pickaxe_head

gold/pickaxe_head.json
    output = craftbound:pickaxe_head
```

`MetalPartDefinitions` に既にある、

```text
outputが登録済みItemである
output ItemがMetalPartItemである
```

という検証を正とする。

Item ID文字列から金属やパーツを推測しない。

## 4. 粗加工パーツから完成パーツへの変換

完成パーツ生成は `MetalPartStateService` を唯一の窓口とする。

既存の

```java
create(
    ResourceLocation outputItemId,
    ResourceLocation metalId,
    MetalVisualData visualData
)
```

および

```java
createFromRoughPart(ItemStack roughPart)
```

を利用する。

`createFromRoughPart` は次を引き継ぐ。

```text
RoughMetalPartState.definitionSnapshot.outputItemId
    → 生成する完成パーツItem

RoughMetalPartState.metalId
    → MetalPartState.metalId

RoughMetalPartState.visualData
    → MetalPartState.visualData
```

鍛造処理側からNBTを直接編集しない。

## 5. 表示名

`MetalPartItem#getName` の既存方式を維持する。

正常な状態では `metalId` とItem自身の通常名称から表示名を構築する。

例:

```text
craftbound:pickaxe_head
metalId = craftbound:iron
→ 鉄製ピッケルヘッド
```

状態がない、または読込不能な場合は通常のItem翻訳へfallbackする。

## 6. Texture / Model

完成パーツはパーツ種別ごとに共通Textureを1枚持つ。

必要なTexture名:

```text
sword_blade.png
pickaxe_head.png
axe_head.png
shovel_head.png
hoe_head.png
helmet_body.png
chestplate_body.png
leggings_body.png
boots_body.png
horse_armor_body.png
iron_ring.png
crossbow_trigger.png
shield_boss.png
shears_blades.png
fire_striker.png
brush_head.png
```

金属別Textureは作成しない。

各Item Modelは以下の形式。

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "craftbound:item/pickaxe_head"
  }
}
```

Texture仕様:

- 16×16 RGBA
- 透明背景
- ニュートラルなグレースケール
- 金属色を焼き込まない
- 金属色はTintで乗算する
- パーツ形状ごとに1枚のみ

PNGが未作成の場合、今回のJava実装のために画像生成は行わない。Model JSONだけ正しいTexture IDを参照させる。

## 7. Tint

既存の以下を再利用する。

```text
MetalPartColorHandler
MetalRenderColorResolver
MetalVisualData
```

色解決:

```text
MetalPartStateService.read
    ↓
state.visualData
    ↓
MetalRenderColorResolver.resolve
    ↓
Tint
```

パーツごとのColor Handlerは作らない。

`CraftboundClientEvents#registerItemColors` で16種類すべての完成パーツへ `MetalPartColorHandler` を登録する。

# 採用した実装方針

- パーツ種別はItemで表現する
- 金属種別は`MetalPartState.metalId`で表現する
- 描画用金属情報は`MetalVisualData`を利用する
- 全完成パーツで`MetalPartItem`を共用する
- 全完成パーツで`MetalPartColorHandler`を共用する
- `MetalPartDefinitions.output`を完成パーツItemの正本とする
- 粗加工パーツからの状態移行は`MetalPartStateService`へ集約する
- Textureは「形状」、Tintは「金属色」を担当する

# 採用しなかった方針

- 金属 × パーツごとのItem登録
- 金属 × パーツごとのModel
- 金属 × パーツごとのTexture
- 金属別Java Itemクラス
- パーツ別Java Itemクラス
- Item IDからmetalIdを推測する処理
- 鍛造処理からNBTを直接編集する処理
- 金属色をTextureへ焼き込む方式
- 既存の`MetalPartState`と並行する別状態システム

# 関連する既存クラス

## `CraftboundItems`
完成パーツ登録を集約する。既存の `registerMetalPart` を利用して16種類へ展開する。

## `MetalPartItem`
完成パーツ共通Item。動的表示名を担当する。

## `MetalPartState`
`version`、`metalId`、`visualData`を保持する。変更不要。

## `MetalPartStateCodec`
`CraftboundMetalPart` NBTを担当する。変更不要。

## `MetalPartStateService`
完成パーツ生成・読込を担当する。既存APIを利用する。

## `MetalPartDefinitions`
JSONの `output` が `MetalPartItem` を指すことを検証する。既存検証を維持する。

## `MetalPartColorHandler`
完成パーツのTint入口。全完成パーツで共有する。

## `MetalRenderColorResolver`
金属色の解決とcacheを担当する。新しいResolverを作成しない。

# データフロー

```text
MetalPartDefinition
    output = craftbound:pickaxe_head
        ↓
MetalPartDefinitionSnapshot
        ↓
粗加工パーツ
    Item = rough_pickaxe_head
    metalId = iron
    visualData = iron
        ↓
鍛造完了
        ↓
MetalPartStateService.createFromRoughPart
        ↓
完成パーツ
    Item = pickaxe_head
    metalId = iron
    visualData = iron
        ↓
MetalPartColorHandler
        ↓
pickaxe_head.png × iron Tint
```

# 実装手順

## Step 1: 完成パーツItemを16種類へ展開

読む対象:

```text
CraftboundItems.java
assets/craftbound/lang/en_us.json
assets/craftbound/lang/ja_jp.json
```

実施:

1. `registerMetalPart` を再利用する
2. 16種類の完成パーツを登録する
3. 各Itemのfallback翻訳を追加する
4. 金属名を含むItem IDを新規追加しない

このStepでは状態クラスやTint処理を変更しない。

## Step 2: MetalPartDefinitionのoutput契約を揃える

読む対象:

```text
data/craftbound/blacksmith/metal_parts/
MetalPartDefinitions.java
```

実施:

1. 既存定義の `output` を対応する共通完成パーツItemへ向ける
2. 同一パーツでは金属が異なっても同じ `output` を使用する
3. `rough_output` は変更しない
4. `MetalPartDefinitions` の既存 `MetalPartItem` 検証を維持する

例:

```text
pickaxe_head_mold
    rough_output = rough_pickaxe_head
    output       = pickaxe_head
```

データ定義がまだ存在しない金属×パーツの組み合わせを、この作業だけのために新規作成しない。

## Step 3: ModelとTint登録を16種類へ展開

読む対象:

```text
assets/craftbound/models/item/
CraftboundClientEvents.java
MetalPartColorHandler.java
```

実施:

1. 16種類のItem Model JSONを用意する
2. 各Modelは対応する共通Textureをlayer0へ指定する
3. `registerItemColors` で16種類すべてを `MetalPartColorHandler` へ登録する
4. `MetalRenderColorResolver` は再利用する
5. 金属別Model / Texture参照を作らない

Texture PNGが存在しない場合は生成しない。

## Step 4: 統合確認と不要参照の除去

限定検索する文字列:

```text
iron_pickaxe_head
gold_pickaxe_head
copper_pickaxe_head
```

実施:

1. Java登録・現行JSON・Modelに金属固有完成パーツ参照が残っていないことを確認する
2. 見つかった場合、現行実装で使用される参照だけ共通パーツへ置換する
3. 過去docsやGit履歴は探索・修正しない
4. `MetalPartStateService.createFromRoughPart` が共通完成パーツを生成できることを確認する
5. `gradlew build` を実行する

# 擬似コード

```text
register finished parts:
    for each fixed part id:
        registerMetalPart(partId)
```

```text
finish forging:
    roughState = RoughMetalPartStateService.read(roughPart)
    if invalid:
        fail

    finished = MetalPartStateService.createFromRoughPart(roughPart)
    if failed:
        fail

    return finished
```

```text
render finished part:
    state = MetalPartStateService.read(stack)

    if invalid:
        WHITE

    return MetalRenderColorResolver.resolve(
        state.visualData
    )
```

# 境界条件・例外

- `output` が未登録Itemなら定義をinvalidとする
- `output` が `MetalPartItem` でなければ定義をinvalidとする
- NBTなしの完成パーツは加工データとしては無効
- NBTなしでも表示処理はクラッシュさせない
- `metalId` をItem IDから推測しない
- `visualData` を完成時に再計算せず粗加工パーツから引き継ぐ
- 金属ごとの専用Textureへ切り替えない
- Client専用Tint処理をDedicated Server側から参照しない
- PNG不足を理由にJava側で代替Texture生成を実装しない
- 未定義の金属×パーツデータを推測で追加しない

# 完了条件

- 16種類の完成パーツが`MetalPartItem`として登録されている
- 各完成パーツはstack size 1
- 金属固有の完成パーツItemを登録していない
- `MetalPartState`が`metalId`と`visualData`を保持している
- `MetalPartStateService.createFromRoughPart`を利用できる
- 金属パーツ定義の`output`が共通完成パーツItemを指す
- `output`が`MetalPartItem`であることをデータ読込時に検証する
- 16種類のItem Modelが共通パーツTextureを参照する
- 16種類すべてに`MetalPartColorHandler`が登録されている
- 同じ`pickaxe_head` Itemを鉄・金など複数金属で利用できる
- 金属差はTintで表現される
- 金属別Model / Textureを必要としない
- `gradlew build`が成功する

# 今回の対象外

- 鋳造台の実装
- 鍛造ミニゲーム本体
- 加熱・冷却評価ロジック
- 粗加工パーツの再設計
- `MetalVisualData`の再設計
- `MetalRenderColorResolver`の再設計
- 金属定義JSONの追加
- 未実装金属パーツ定義の大量追加
- 16×16 PNGテクスチャそのものの制作
- レシピ追加
- 過去データのmigration
- 過去docsの修正
