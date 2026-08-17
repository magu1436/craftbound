# 目的

`develop` の既存鍛冶・品質システムを再利用し、ユーザー設計で確定している Cataclysm / Ignis 系要素だけを Craftbound の製造工程へ統合する。

対象:

```text
cataclysm:ignitium_ingot -> craftbound:ignitium として金属処理
cataclysm:blazing_grips -> 品質付き shapeless 組立
cataclysm:ignitium_helmet
cataclysm:ignitium_chestplate
cataclysm:ignitium_leggings
cataclysm:ignitium_boots -> 既存防具パーツから品質付き組立
cataclysm:ignitium_elytra_chestplate -> Cataclysm Fusion Anvil を維持
```

併せて、今回必要になる以下の汎用基盤を実装する。

```text
1. optional Mod 依存の metal definition 読み込み
2. craftbound:quality_shapeless_assembly
3. 他Mod recipe を ID 指定で除外する汎用 recipe removal
```

Cataclysm は必須依存にしない。未導入環境でも Craftbound が起動・リロードできること。

Codex は原則として本書の「関連する既存クラス」に列挙したファイルだけを先に確認し、記載内容と `develop` が一致しない場合だけ追加探索すること。

# 確定した仕様

## Ignis 系の範囲

Cataclysm 側に既に存在する以下の Item はそのまま使用し、Craftbound 側で複製登録しない。

```text
cataclysm:ignitium_ingot
cataclysm:blazing_grips
cataclysm:ignitium_helmet
cataclysm:ignitium_chestplate
cataclysm:ignitium_leggings
cataclysm:ignitium_boots
cataclysm:ignitium_elytra_chestplate
```

Ignis は固有金属 Ignitium を持つため `ignis_core` は追加しない。

ユーザー設計に今回記載されていない Cataclysm 装備は追加探索して対象へ含めない。

## 既存資産

以下は `develop` に実装済み。再実装・置換しない。

```text
craftbound:protective_lining

craftbound:helmet_body_mold
craftbound:chestplate_body_mold
craftbound:leggings_body_mold
craftbound:boots_body_mold

craftbound:rough_helmet_body
craftbound:rough_chestplate_body
craftbound:rough_leggings_body
craftbound:rough_boots_body

craftbound:helmet_body
craftbound:chestplate_body
craftbound:leggings_body
craftbound:boots_body
```

防具パーツは既存の材質分離設計を維持する。

```text
part_type = craftbound:iron/<part>
material  = craftbound:ignitium
```

`protective_lining` は通常 ingredient とし、品質平均には含めない。既存鉄・金防具と同じ扱いとする。

## Ignitium metal

新規 metal ID:

```text
craftbound:ignitium
```

追加:

```text
src/main/resources/data/craftbound/blacksmith/metals/ignitium.json
```

原料:

```text
cataclysm:ignitium_ingot = 1 metal unit
```

初期の `lump_loss_*`、加熱曲線、danger/destroy tick、evaluator は既存 `iron.json` と同じ値を使用する。今回は Ignitium 固有バランスを設計しない。

`display_color` / `representative_item` は明示しない。既存 `MetalMaterialDefinitions` の挙動により `cataclysm:ignitium_ingot` を representative item として利用する。

### optional metal definition

`MetalMaterialDefinitions` の schema version 2 に optional field を追加する。schema version は上げない。

```json
"required_mod": "cataclysm"
```

適用規則:

```text
required_mod なし:
    既存挙動

required_mod あり + Mod loaded:
    通常 parse

required_mod あり + Mod not loaded:
    parse 前に静かに skip
    unknown item 警告を出さない
```

`ignitium.json` は `required_mod = cataclysm` を持つ。

## Gauntlet Frame

Blazing Grips 用の新規共通金属パーツ:

```text
craftbound:gauntlet_frame_mold
craftbound:rough_gauntlet_frame
craftbound:gauntlet_frame
```

`CraftboundItems` の既存 helper を使用する。

```text
registerCastingMold
registerRoughMetalPart
registerMetalPart
```

part definition:

```text
src/main/resources/data/craftbound/blacksmith/metal_parts/iron/gauntlet_frame.json
part_type = craftbound:iron/gauntlet_frame
```

Ignitium 専用 part definition は作らない。

初期値:

```text
ingredient_count = 1
cooling / forging / part_quality = 既存 iron_ring.json と同じ
```

今回はバランス調整を行わない。

鋳型入手は既存 mold と同じ stonecutting:

```text
craftbound:blank_mold -> craftbound:gauntlet_frame_mold
```

追加:

```text
src/main/resources/data/craftbound/recipes/blacksmith/equipment/molds/gauntlet_frame_mold.json
```

## 品質付き shapeless recipe

ユーザー設計では Blazing Grips は shapeless。既存 `craftbound:quality_assembly` は pattern 前提なので、shaped に変更して代用しない。

新規汎用 serializer:

```text
craftbound:quality_shapeless_assembly
```

Cataclysm 専用にはしない。

Blazing Grips:

```text
Ignitium Gauntlet Frame
+ craftbound:protective_lining
-> cataclysm:blazing_grips
```

追加:

```text
src/main/resources/data/craftbound/recipes/blacksmith/quality_shapeless_assembly/blazing_grips.json
```

JSON 概形:

```json
{
  "type": "craftbound:quality_shapeless_assembly",
  "conditions": [{ "type": "forge:mod_loaded", "modid": "cataclysm" }],
  "ingredients": [
    {
      "kind": "metal_part",
      "part_type": "craftbound:iron/gauntlet_frame",
      "material": "craftbound:ignitium",
      "contributes_to_quality": true
    },
    { "kind": "ingredient", "item": "craftbound:protective_lining" }
  ],
  "result": { "item": "cataclysm:blazing_grips", "count": 1 }
}
```

品質は既存 `FinishedItemQualityCalculator` と `QualityStateService` を使用する。今回 contributor は Gauntlet Frame だけなので完成品質はその品質と同値。

### shapeless matching

最大3x3なので backtracking で ingredient と非空 slot の一対一対応を求める。custom ingredient の条件が重なる可能性があるため greedy matching は不可。

```text
非空stack数 != ingredient数 -> no match
全ingredientを未使用stackへ一対一割当できる -> match
余分なstack / ingredient不足 -> no match
```

`findMatch` は品質計算のため ingredient と container slot の対応を返す。

## Ignitium armor

既存鉄・金防具の `craftbound:quality_assembly` をそのまま利用する。新しい armor Java ロジックは作らない。

追加:

```text
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/ignitium_helmet.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/ignitium_chestplate.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/ignitium_leggings.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/ignitium_boots.json
```

全JSONに `forge:mod_loaded(cataclysm)` condition を付ける。

既存防具と同じ pattern:

```text
B
L
```

対応:

```text
cataclysm:ignitium_helmet
  B = craftbound:iron/helmet_body, material=craftbound:ignitium

cataclysm:ignitium_chestplate
  B = craftbound:iron/chestplate_body, material=craftbound:ignitium

cataclysm:ignitium_leggings
  B = craftbound:iron/leggings_body, material=craftbound:ignitium

cataclysm:ignitium_boots
  B = craftbound:iron/boots_body, material=craftbound:ignitium

全て:
  B.contributes_to_quality = true
  L = craftbound:protective_lining
```

完成品質は armor body の品質と同値。

## 汎用 recipe removal

CraftTweaker / KubeJS 等を必須依存にしない。Craftbound 自身が ID 指定で既存 recipe を除外する。

新規候補:

```text
src/main/java/com/magu1436/craftbound/integration/recipe/RecipeRemovalRegistry.java
src/main/java/com/magu1436/craftbound/integration/cataclysm/CataclysmRecipeIntegration.java
src/main/java/com/magu1436/craftbound/mixin/RecipeManagerMixin.java
```

### RecipeRemovalRegistry

```text
register(ResourceLocation)
registerAll(Collection<ResourceLocation>)
shouldRemove(ResourceLocation)
```

Cataclysm Java クラスは参照しない。

### CataclysmRecipeIntegration

Cataclysm loaded 時だけ以下を登録:

```text
cataclysm:blazing_grips
cataclysm:smithing/ignitium_helmet
cataclysm:smithing/ignitium_chestplate
cataclysm:smithing/ignitium_leggings
cataclysm:smithing/ignitium_boots
```

`Craftbound` の既存 `ModList.get().isLoaded(...)` パターンを使用する。

### RecipeManagerMixin

`RecipeManager` の private map を直接変更しない。

`RecipeManager#apply(Map<ResourceLocation, JsonElement>, ResourceManager, ProfilerFiller)` の recipe JSON map を deserialize 前にフィルタする。Mixin は Object bridge ではなく、この Map 引数を持つ overload を descriptor まで特定して target にする。

入力Mapの mutability に依存しないよう `@ModifyVariable` でコピーしたMapへ差し替える方式を優先する。

```text
filtered = new HashMap(original)
filtered.entrySet().removeIf(id is registered)
return filtered to RecipeManager.apply
```

Mixin は Minecraft `RecipeManager` のみを target とするため Cataclysm 未導入でも安全に適用する。

`craftbound.mixins.json` へ登録する。

## Fusion Anvil

以下は削除・置換しない。

```text
cataclysm:weapon_infusion/ignitium_elytra_chestplate
```

Craftbound 側に代替 recipe を追加しない。

品質継承は別仕様の Fusion Anvil 品質計算基盤の責務。作業ブランチに未実装なら、このタスクへ勝手に含めず完了報告で前提未実装と報告する。

## dependency

共有された `build.gradle` には既に以下があるため追加変更不要。

```gradle
compileOnly fg.deobf("maven.modrinth:l_enders-cataclysm:${cataclysm_modrinth_version}")
runtimeOnly fg.deobf("maven.modrinth:l_enders-cataclysm:${cataclysm_modrinth_version}")
```

`mods.toml` の Cataclysm optional dependency を mandatory に変更しない。

# 採用した実装方針

1. Ignitium は既存 JSON 駆動 metal system に追加する。
2. `required_mod` を metal schema v2 の optional field とし、外部Mod金属へ再利用可能にする。
3. 防具鋳型・防具パーツ・`protective_lining` は既存実装を再利用する。
4. 新規共通パーツは `gauntlet_frame` のみ。
5. shaped 品質組立は既存 `quality_assembly`、shapeless は新規 `quality_shapeless_assembly`。
6. shapeless matching は最大9要素の backtracking。
7. recipe removal は汎用 registry + vanilla `RecipeManagerMixin`。
8. Cataclysm向け Craftbound recipe は `forge:mod_loaded` condition を持つ。
9. Fusion Anvil recipe は維持する。

# 採用しなかった方針

- CraftTweaker / KubeJS を必須化: Craftbound 自身の互換仕様のため。
- Cataclysm recipe をダミーJSONで上書き: 再利用可能な削除基盤にならないため。
- `RecipeManager` 内部mapを直接編集: 索引整合性へ依存するため。
- Blazing Grips を shaped に変更: ユーザー設計の shapeless を維持するため。
- Cataclysm専用品質serializer: 汎用化できないため。
- Ignitium専用 armor body/mold: part type と material の既存分離設計に反するため。
- Ignis Core: Ignis は固有金属を持つため。
- Ignitium Elytra Chestplate を通常クラフト化: Fusion Anvil を維持するため。

# 関連する既存クラス

## Codex が最初に読むファイル

```text
src/main/java/com/magu1436/craftbound/Craftbound.java
src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java
src/main/java/com/magu1436/craftbound/registry/CraftboundRecipeSerializers.java
src/main/resources/craftbound.mixins.json

src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/MetalMaterialDefinitions.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/MetalDefinition.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/MetalVisualDataService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/AssemblyIngredient.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipe.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipeSerializer.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/FinishedItemQualityCalculator.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipeValidationListener.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/quality/QualityTargetRegistry.java
src/main/java/com/magu1436/craftbound/common/quality/QualityStateService.java
```

基準データ:

```text
src/main/resources/data/craftbound/blacksmith/metals/iron.json
src/main/resources/data/craftbound/blacksmith/metal_parts/iron/iron_ring.json
src/main/resources/data/craftbound/blacksmith/metal_parts/iron/helmet_body.json
src/main/resources/data/craftbound/recipes/blacksmith/equipment/molds/helmet_body_mold.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_helmet.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/golden_helmet.json
src/main/resources/data/craftbound/recipes/blacksmith/protective_lining.json
```

既存登録確認済み。重複追加禁止:

```text
PROTECTIVE_LINING
HELMET_BODY_MOLD / ROUGH_HELMET_BODY / HELMET_BODY
CHESTPLATE_BODY_MOLD / ROUGH_CHESTPLATE_BODY / CHESTPLATE_BODY
LEGGINGS_BODY_MOLD / ROUGH_LEGGINGS_BODY / LEGGINGS_BODY
BOOTS_BODY_MOLD / ROUGH_BOOTS_BODY / BOOTS_BODY
```

Cataclysm 側で recipe ID 確認に必要なのは以下だけ。広範囲探索不要。

```text
src/main/resources/data/cataclysm/recipes/blazing_grips.json
src/main/resources/data/cataclysm/recipes/smithing/ignitium_helmet.json
src/main/resources/data/cataclysm/recipes/smithing/ignitium_chestplate.json
src/main/resources/data/cataclysm/recipes/smithing/ignitium_leggings.json
src/main/resources/data/cataclysm/recipes/smithing/ignitium_boots.json
src/main/resources/data/cataclysm/recipes/weapon_infusion/ignitium_elytra_chestplate.json
```

## 新規 Java

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityShapelessAssemblyRecipe.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityShapelessAssemblyRecipeSerializer.java
src/main/java/com/magu1436/craftbound/integration/recipe/RecipeRemovalRegistry.java
src/main/java/com/magu1436/craftbound/integration/cataclysm/CataclysmRecipeIntegration.java
src/main/java/com/magu1436/craftbound/mixin/RecipeManagerMixin.java
```

## 主な変更ファイル

```text
src/main/java/com/magu1436/craftbound/Craftbound.java
src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java
src/main/java/com/magu1436/craftbound/registry/CraftboundRecipeSerializers.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/MetalMaterialDefinitions.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipeSerializer.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipeValidationListener.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/quality/QualityTargetRegistry.java
src/main/resources/craftbound.mixins.json
```

`QualityAssemblyRecipeSerializer` は大規模リファクタしない。shapeless serializer が既存 ingredient/result/network codec を再利用できる範囲だけ、private static helper を package-private static に変更してよい。

`QualityAssemblyRecipeValidationListener` と `QualityTargetRegistry` は shaped / shapeless の双方を扱うよう拡張する。

テスト参考:

```text
src/test/java/com/magu1436/craftbound/occupations/blacksmith/assembly/FinishedItemQualityCalculatorTest.java
src/test/java/com/magu1436/craftbound/occupations/blacksmith/quality/QualityTargetRegistryTest.java
```

# データフロー

```text
Cataclysmなし
  -> CataclysmRecipeIntegration未登録
  -> ignitium.jsonをrequired_modでskip
  -> conditional recipe未ロード
  -> Craftbound通常動作

Cataclysmあり
  -> 既存recipe 5件を削除対象登録
  -> cataclysm:ignitium_ingot
       -> craftbound:ignitium
       -> 溶解 -> 鋳造 -> 鍛造
       -> 品質付き metal part

Ignitium armor body + protective_lining
  -> quality_assembly
  -> Cataclysm Ignitium armor + quality

Ignitium gauntlet_frame + protective_lining
  -> quality_shapeless_assembly
  -> cataclysm:blazing_grips + quality

Ignitium Chestplate + Elytra
  -> Cataclysm Fusion Anvil
  -> Ignitium Elytra Chestplate
```

# 擬似コード

```text
Craftbound初期化:
    if ModList.isLoaded("cataclysm"):
        CataclysmRecipeIntegration.register()

CataclysmRecipeIntegration.register:
    RecipeRemovalRegistry.registerAll(対象5ID)

RecipeManagerMixin:
    RecipeManager.apply の recipes 引数をコピー
    RecipeRemovalRegistry.shouldRemove(id) の entry を削除
    filtered map を apply 本体へ渡す

MetalMaterialDefinitions.apply:
    各definition:
        required_mod があり、未ロードなら continue
        既存parse

QualityShapelessAssemblyRecipe.findMatch:
    非空slotを収集
    数がingredient数と違えば失敗
    backtrackingで未使用slotへingredientを一対一割当
    完全割当を返す

QualityShapelessAssemblyRecipe.assemble:
    matchを取得
    contributesToQuality=true の対応stackだけ品質取得
    FinishedItemQualityCalculator.calculate
    QualityStateService.setQuality(result)

QualityAssemblyRecipeValidationListener:
    shaped / shapeless の assemblyIngredients を同じ規則で検証

QualityTargetRegistry:
    shaped / shapeless の result item を収集
```

# 境界条件・例外

- Cataclysm 未導入で `NoClassDefFoundError` / `ClassNotFoundException` を起こさない。
- Cataclysm 未導入で `ignitium.json` の missing item 警告を出さない。
- Cataclysm向け Craftbound recipe は全て `forge:mod_loaded` condition を持つ。
- recipe removal は ID 完全一致のみ。存在しないIDは無視する。
- `cataclysm:weapon_infusion/ignitium_elytra_chestplate` は削除しない。
- shapeless は配置順に依存せず、余分なstackがあれば失敗する。
- custom ingredient 条件が重なっても完全matchingを求める。
- quality contributor が0なら品質を書かない。1個ならその値をそのまま使用する。
- 既存 shaped `quality_assembly` の挙動を変えない。
- 既存鉄・金防具 recipe を変更しない。
- 既存 armor mold / armor body / `protective_lining` を重複登録しない。
- Cataclysm Item を Craftbound 側で登録しない。

# デザイン資産の扱い

テクスチャPNG、16x16設計図、理想形状など、デザイン判断が必要な資産は Codex の対象外。

Codex は placeholder PNG を作らず、既存の無関係なテクスチャを仮流用しない。

今回新規に必要になる見込みのテクスチャ:

```text
src/main/resources/assets/craftbound/textures/item/gauntlet_frame_mold.png
src/main/resources/assets/craftbound/textures/item/rough_gauntlet_frame.png
src/main/resources/assets/craftbound/textures/item/gauntlet_frame.png
```

model JSON と lang entry は既存形式を複製して追加してよい。PNGはユーザーが後から追加する。探索を避けるため、追加先は以下とする。

```text
src/main/resources/assets/craftbound/models/item/gauntlet_frame_mold.json
src/main/resources/assets/craftbound/models/item/rough_gauntlet_frame.json
src/main/resources/assets/craftbound/models/item/gauntlet_frame.json
src/main/resources/assets/craftbound/lang/en_us.json
src/main/resources/assets/craftbound/lang/ja_jp.json
```

model の基準は既存 `helmet_body_mold.json` / `rough_helmet_body.json` / `helmet_body.json` とする。

Codex は完了報告時、今回の実装によって必要になった未配置デザイン資産を**必ず正確なパス付きで列挙すること**。本書に無い texture / shape / model 等が必要と判明した場合も、勝手にデザインせず用途とパスを報告する。

現行 metal part system は carving のような空間的 ideal-shape JSON を要求しないため、`gauntlet_frame` 用 shape は本書では追加しない。作業時点の `develop` で必須になっていた場合だけ報告する。

# 完了条件

- Cataclysm を必須依存へ変更していない。
- Cataclysm あり/なし双方で起動・data reload できる。
- `craftbound:ignitium` が Cataclysm 導入時だけロードされる。
- `cataclysm:ignitium_ingot` が1 metal unitとして扱われる。
- 既存 armor mold / armor body / protective lining を再利用している。
- `gauntlet_frame_mold` / `rough_gauntlet_frame` / `gauntlet_frame` が追加され、既存 casting -> forging で製造できる。
- `craftbound:quality_shapeless_assembly` が登録される。
- Blazing Grips が Gauntlet Frame + Protective Lining の shapeless 品質クラフトになる。
- Ignitium 防具4部位が既存 armor body + Protective Lining の品質クラフトになる。
- shaped / shapeless の完成品が `QualityTargetRegistry` の対象になる。
- Cataclysm の `blazing_grips` と Ignitium smithing 4件が削除される。
- Ignitium Elytra Chestplate の Fusion Anvil recipe は残る。
- 外部レシピ削除Modを必須依存にしていない。
- 既存 `quality_assembly` の挙動を壊していない。
- `./gradlew test` が成功する。
- Codex がPNGを生成しておらず、完了報告に不足デザイン資産一覧がある。

追加推奨テスト:

```text
QualityShapelessAssemblyRecipeTest
  - 順番違い
  - 余分/不足stack
  - 重複条件でbacktracking
  - 品質contributorsのみ計算

RecipeRemovalRegistryTest
  - 登録IDのみ削除
  - 未登録ID維持
  - 入力Map非破壊

QualityTargetRegistryTest
  - shapeless result もtargetになる
```

# 今回の対象外

- PNG / 16x16設計図 / ideal shape の制作
- Ignitium固有の加熱・鍛造バランス調整
- 既存防具パーツ鋳型の再実装
- `protective_lining` の再実装・変更
- Ignis Core
- ユーザー設計に記載されていない Ignis/Cataclysm 装備
- 他ボス系統
- Cataclysm ドロップ変更
- Fusion Anvil 本体の置換
- Fusion Anvil品質計算基盤そのもの（別仕様）
- Ignitium Elytra Chestplate fusion recipe の変更
- CraftTweaker / KubeJS 等の導入
