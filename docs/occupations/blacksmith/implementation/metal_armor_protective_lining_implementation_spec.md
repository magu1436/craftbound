# 目的

`develop` ブランチに既に存在する金属製防具本体パーツと `craftbound:quality_assembly` を利用し、鉄製防具4部位および金製防具4部位を Vanilla 作業台で完成させられるようにする。

防具本体パーツは金属ごとに別アイテム・別パーツ定義を追加せず、既存の `helmet_body` / `chestplate_body` / `leggings_body` / `boots_body` を共用する。金属種別は既存の金属パーツ状態が保持する `material` によって判定する。

防具本体は共通の品質なし補助アイテム `craftbound:protective_lining` と組み合わせて完成品へ変換する。品質計算、素材判定、完成品への品質付与は既存 `quality_assembly` 実装をそのまま利用し、新しい組み立てJavaロジックは追加しない。

Codex は原則として本仕様書で列挙したファイルだけを参照する。列挙したパスやAPIが現行 `develop` と一致しない場合のみ、必要な範囲で追加探索する。

# 確定した仕様

## 実装対象

対象は以下の8完成品とする。

| 部位 | 既存品質付き防具本体 | 鉄完成品 | 金完成品 |
|---|---|---|---|
| 頭 | `helmet_body` | `minecraft:iron_helmet` | `minecraft:golden_helmet` |
| 胴 | `chestplate_body` | `minecraft:iron_chestplate` | `minecraft:golden_chestplate` |
| 脚 | `leggings_body` | `minecraft:iron_leggings` | `minecraft:golden_leggings` |
| 足 | `boots_body` | `minecraft:iron_boots` | `minecraft:golden_boots` |

各完成レシピは以下を要求する。

- 対応する品質付き金属防具本体 ×1
- `craftbound:protective_lining` ×1

鉄用と金用で別の防具本体 Item、鋳型、金属パーツ定義、Javaクラスを追加してはならない。

金属種別はレシピの `material` 条件で区別する。

```text
鉄防具: material = craftbound:iron
金防具: material = craftbound:gold
```

`part_type` は現行実装で使用されている既存の防具本体パーツ定義IDをそのまま使う。ID中に `iron/` が含まれていても、金用に `craftbound:gold/...` を新規作成しない。

```text
craftbound:iron/helmet_body
craftbound:iron/chestplate_body
craftbound:iron/leggings_body
craftbound:iron/boots_body
```

金製完成品レシピでも上記と同じ `part_type` を使用し、`material` のみ `craftbound:gold` とする。

## Protective Lining

- アイテムID: `craftbound:protective_lining`
- 英語表示名: `Protective Lining`
- 日本語表示名: `防護用ライニング`
- 専用 Item クラスは作成しない。
- 通常の `Item` として登録する。
- 品質データを持たない。
- 鍛冶師経験値を付与しない。
- スタック数は通常の `Item.Properties()` の既定値を使用する。
- Vanilla の shapeless recipe で作成する。
- 材料は革1個 + 任意色の羊毛1個。
- 羊毛は個別アイテムIDではなく `minecraft:wool` タグを使用する。
- 生成数は1個。

追加する通常レシピ:

`src/main/resources/data/craftbound/recipes/blacksmith/protective_lining.json`

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    {
      "item": "minecraft:leather"
    },
    {
      "tag": "minecraft:wool"
    }
  ],
  "result": {
    "item": "craftbound:protective_lining",
    "count": 1
  }
}
```

## 金属製防具の組み立て

8レシピとも既存の `craftbound:quality_assembly` を使用する。

配置先:

```text
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/
├─ iron_helmet.json
├─ iron_chestplate.json
├─ iron_leggings.json
├─ iron_boots.json
├─ golden_helmet.json
├─ golden_chestplate.json
├─ golden_leggings.json
└─ golden_boots.json
```

全レシピで縦2マスの shaped pattern を使用する。

```text
B = 対応する金属製防具本体
L = protective_lining

B
L
```

防具本体のみ `contributes_to_quality: true` とする。`protective_lining` は既存 Serializer の通常 Ingredient 形式 `kind: "ingredient"` を使用し、品質計算対象には含めない。

### 鉄製チェストプレートの基準例

```json
{
  "type": "craftbound:quality_assembly",
  "pattern": [
    "B",
    "L"
  ],
  "key": {
    "B": {
      "kind": "metal_part",
      "part_type": "craftbound:iron/chestplate_body",
      "material": "craftbound:iron",
      "contributes_to_quality": true
    },
    "L": {
      "kind": "ingredient",
      "item": "craftbound:protective_lining"
    }
  },
  "result": {
    "item": "minecraft:iron_chestplate",
    "count": 1
  }
}
```

### 金製チェストプレートの基準例

鉄レシピと同じ `part_type` を使用し、`material` と完成品だけを金へ変更する。

```json
{
  "type": "craftbound:quality_assembly",
  "pattern": [
    "B",
    "L"
  ],
  "key": {
    "B": {
      "kind": "metal_part",
      "part_type": "craftbound:iron/chestplate_body",
      "material": "craftbound:gold",
      "contributes_to_quality": true
    },
    "L": {
      "kind": "ingredient",
      "item": "craftbound:protective_lining"
    }
  },
  "result": {
    "item": "minecraft:golden_chestplate",
    "count": 1
  }
}
```

他6レシピも同一規則で作成する。

| レシピ | `part_type` | `material` | `result.item` |
|---|---|---|---|
| `iron_helmet.json` | `craftbound:iron/helmet_body` | `craftbound:iron` | `minecraft:iron_helmet` |
| `iron_chestplate.json` | `craftbound:iron/chestplate_body` | `craftbound:iron` | `minecraft:iron_chestplate` |
| `iron_leggings.json` | `craftbound:iron/leggings_body` | `craftbound:iron` | `minecraft:iron_leggings` |
| `iron_boots.json` | `craftbound:iron/boots_body` | `craftbound:iron` | `minecraft:iron_boots` |
| `golden_helmet.json` | `craftbound:iron/helmet_body` | `craftbound:gold` | `minecraft:golden_helmet` |
| `golden_chestplate.json` | `craftbound:iron/chestplate_body` | `craftbound:gold` | `minecraft:golden_chestplate` |
| `golden_leggings.json` | `craftbound:iron/leggings_body` | `craftbound:gold` | `minecraft:golden_leggings` |
| `golden_boots.json` | `craftbound:iron/boots_body` | `craftbound:gold` | `minecraft:golden_boots` |

## 完成品品質

- レシピ一致時の品質計算は既存 `QualityAssemblyRecipe` / `FinishedItemQualityCalculator` に任せる。
- `protective_lining` は通常 Ingredient なので品質計算対象にならない。
- 品質付き入力が防具本体1個だけであるため、完成品品質は防具本体品質と同値になる。
- 鉄と金で品質計算方式を分岐しない。
- 組み立て時に新しい品質計算ロジックを追加しない。
- 組み立てでは鍛冶師経験値を付与しない。

## テクスチャ・モデル

PNGテクスチャはユーザーが別途用意する。Codex は PNG を生成・追加・編集しない。

Codex が追加するのは次のモデルJSONのみ。

`src/main/resources/assets/craftbound/models/item/protective_lining.json`

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "craftbound:item/protective_lining"
  }
}
```

このJSONは、後から以下へ配置されるテクスチャを参照する。

```text
src/main/resources/assets/craftbound/textures/item/protective_lining.png
```

本タスクでは上記PNGが存在しなくてもよい。

# 採用した実装方針

## Codex が最初に読むファイル

1. `src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java`
   - `protective_lining` の登録先。
   - 既存の単純 Item 登録方法をそのまま使用する。
2. `src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_pickaxe.json`
   - 現行 `quality_assembly` JSON の基準例。
3. `src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipeSerializer.java`
   - `metal_part` と `ingredient` のJSON形式確認用。
4. `src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipe.java`
   - 既存の shaped 一致判定・完成品生成・品質付与処理確認用。原則変更不要。
5. `src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/FinishedItemQualityCalculator.java`
   - 品質計算確認用。変更不要。
6. `src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/MetalPartAssemblyIngredient.java`
   - `part_type` と `material` を別条件として判定していることの確認用。変更不要。
7. `src/main/resources/assets/craftbound/models/item/pickaxe_handle.json`
   - `protective_lining.json` のモデルJSON形式の参考。
8. 既存の防具本体 metal part 定義
   - `src/main/resources/data/craftbound/blacksmith/metal_parts/iron/helmet_body.json`
   - `src/main/resources/data/craftbound/blacksmith/metal_parts/iron/chestplate_body.json`
   - `src/main/resources/data/craftbound/blacksmith/metal_parts/iron/leggings_body.json`
   - `src/main/resources/data/craftbound/blacksmith/metal_parts/iron/boots_body.json`
   - 定義内容を確認するだけで変更しない。
9. 金属素材定義
   - `craftbound:gold` が既存素材として登録されている箇所を確認する。
   - 新しい Gold 用パーツ定義は作成しない。
10. `docs/occupations/blacksmith/equipment/vanilla.md`
    - 鉄・金防具と protective lining の仕様反映先。
11. `docs/occupations/blacksmith/data-formats.md`
    - `quality_assembly` の現行形式・配置確認用。

## 変更するファイル

### Java

`src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java`

既存の鍛冶アイテム登録群付近に `protective_lining` の登録を1件追加する。

```java
public static final RegistryObject<Item> PROTECTIVE_LINING = registerItem("protective_lining");
```

実際の補助メソッド名・型が現行 `develop` と異なる場合は、同ファイルの既存単純Item登録形式に合わせる。

新規Javaクラスは作成しない。

### 言語ファイル

- `src/main/resources/assets/craftbound/lang/en_us.json`
- `src/main/resources/assets/craftbound/lang/ja_jp.json`

追加キー:

```json
"item.craftbound.protective_lining": "Protective Lining"
```

```json
"item.craftbound.protective_lining": "防護用ライニング"
```

### ドキュメント

`docs/occupations/blacksmith/equipment/vanilla.md` を最小修正する。

- 鉄製防具4部位と金製防具4部位の完成工程で `protective_lining` 1個を必要とすることを記載する。
- `protective_lining` は革1 + `minecraft:wool` 1から shapeless で作る品質なし補助アイテムであることを記載する。
- 防具本体だけが品質付きパーツであり、完成品品質は防具本体品質と同じであることを維持する。
- 金防具では鉄防具と同一の防具本体パーツ定義を利用し、金属種別は `material = craftbound:gold` で区別することを記載する。
- 金用の `helmet_body` 等を別途追加する仕様にはしない。

## 新規追加するファイル

```text
src/main/resources/assets/craftbound/models/item/protective_lining.json
src/main/resources/data/craftbound/recipes/blacksmith/protective_lining.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_helmet.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_chestplate.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_leggings.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_boots.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/golden_helmet.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/golden_chestplate.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/golden_leggings.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/golden_boots.json
```

## 変更してはいけないもの

今回の要件では以下の assembly Java 実装を原則変更しない。

```text
AssemblyIngredient.java
VanillaAssemblyIngredient.java
MetalPartAssemblyIngredient.java
QualityAssemblyRecipe.java
QualityAssemblyRecipeSerializer.java
FinishedItemQualityCalculator.java
CraftboundRecipeSerializers.java
```

また、以下を追加・複製しない。

```text
gold/helmet_body.json
gold/chestplate_body.json
gold/leggings_body.json
gold/boots_body.json
```

既存の以下4定義も変更しない。

```text
blacksmith/metal_parts/iron/helmet_body.json
blacksmith/metal_parts/iron/chestplate_body.json
blacksmith/metal_parts/iron/leggings_body.json
blacksmith/metal_parts/iron/boots_body.json
```

## 実装順序

1. `protective_lining` の Item 登録、言語JSON、モデルJSON、shapeless recipe を追加する。
2. 既存 `iron_pickaxe.json` と現行 Serializer を基準に、鉄防具4部位の `quality_assembly` レシピを追加する。
3. 鉄レシピを基準として `material` を `craftbound:gold`、完成品を `minecraft:golden_*` に変更した金防具4レシピを追加する。金用パーツデータやJava実装は追加しない。
4. `vanilla.md` を今回の鉄・金防具完成工程に合わせて最小更新する。

# 採用しなかった方針

- 金属ごとに `helmet_body` / `chestplate_body` / `leggings_body` / `boots_body` の Item を増やす方式: 既存システムが金属種別をパーツ状態の `material` で保持できるため。
- 金用の `metal_parts/gold/...` 定義を複製する方式: 鉄用と同じパーツ定義を使用し、素材条件だけで区別できるため。
- 金用の鋳型を別途追加する方式: パーツ形状は鉄と同一で、金属種別は鋳型ではなく素材状態で管理するため。
- 鉄専用の assembly Java 実装を追加し、その後に金用ロジックを追加する方式: `quality_assembly` は既に素材条件をデータで指定できるため。
- 防具本体から直接完成防具へ変換する方式: `protective_lining` を共通補助素材として組み合わせる現在の防具組み立て方針を維持するため。
- 部位ごとに `helmet_lining`、`chestplate_lining` 等を作る方式: アイテム・レシピ数を不必要に増やさないため。
- `protective_lining` に品質を持たせる方式: 完成防具の品質は防具本体品質のみから継承するため。
- `protective_lining` を非金属加工ミニゲームで作る方式: 今回は単純な補助素材として扱い、加工工程を追加しないため。
- 鍛造時に防具本体を複数種類へ分岐させる方式: 現行鍛造システムにパーツ分岐はなく、今回も追加しないため。
- `quality_assembly` Serializer / Recipe の機能拡張: 既存の `metal_part`、`ingredient`、`material` 条件で要件を満たせるため。
- 銅防具を対象へ含める方式: 今回の対象外とするため。
- チェーン・革・ダイヤモンド・ネザライト防具まで同時実装する方式: 金属製防具の鉄・金対応に範囲を限定するため。
- PNGテクスチャをCodexに作らせる方式: テクスチャはユーザーが別途用意するため。

# 関連する既存クラス

| ファイル | 役割 | 今回 |
|---|---|---|
| `CraftboundItems.java` | Craftboundアイテム登録 | `PROTECTIVE_LINING` のみ追加 |
| `QualityAssemblyRecipeSerializer.java` | `quality_assembly` JSON読込 | 参照のみ |
| `QualityAssemblyRecipe.java` | shaped一致判定と完成品生成・品質付与 | 原則変更なし |
| `FinishedItemQualityCalculator.java` | 品質寄与パーツから完成品品質を算出 | 変更なし |
| `VanillaAssemblyIngredient.java` | 通常 Ingredient の一致判定 | 変更なし |
| `MetalPartAssemblyIngredient.java` | 金属パーツの `part_type` / `material` 一致判定 | 変更なし |
| `CraftboundRecipeSerializers.java` | `craftbound:quality_assembly` 登録 | 変更なし |

既存JSONの最優先参考ファイルは `src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_pickaxe.json` とする。

# データフロー

```text
minecraft:leather ×1
+ #minecraft:wool ×1
    ↓ Vanilla shapeless crafting
craftbound:protective_lining ×1
    品質なし
```

鉄製防具:

```text
craftbound:iron
  ↓ 既存の加熱・鋳造・鍛造
craftbound:<armor_body>
  part_type = craftbound:iron/<armor_body>
  material  = craftbound:iron
  quality   = Q

craftbound:<armor_body> + craftbound:protective_lining
  ↓ craftbound:quality_assembly
minecraft:iron_<armor>
  quality = Q
```

金製防具:

```text
craftbound:gold
  ↓ 鉄と同じ既存の加熱・鋳造・鍛造システム
craftbound:<armor_body>
  part_type = craftbound:iron/<armor_body>
  material  = craftbound:gold
  quality   = Q

craftbound:<armor_body> + craftbound:protective_lining
  ↓ 鉄と同じ craftbound:quality_assembly
minecraft:golden_<armor>
  quality = Q
```

金対応のために新しいパーツ生成フローを追加しない。既存パーツの `material` が金であることを完成レシピ側で検査するだけとする。

# 擬似コード

```text
Protective Lining登録:
    既存の単純Item登録方法を使って protective_lining を登録する
    専用ItemクラスやCapabilityは作成しない

Protective Lining作成:
    Vanilla shapeless recipe を使う
    leather 1個を要求する
    #minecraft:wool 1個を要求する
    protective_lining 1個を返す
    品質は設定しない
    鍛冶師経験値は付与しない

鉄製防具レシピ作成:
    4部位それぞれについて quality_assembly JSON を作成する
    対応する既存 metal_part の part_type を指定する
    material = craftbound:iron を要求する
    protective_lining 1個を通常Ingredientとして要求する
    metal_part のみ contributes_to_quality = true にする
    完成品を対応する minecraft:iron_* にする

金製防具レシピ作成:
    鉄製防具と同じ4つの part_type を使用する
    新しい gold 用 metal_part 定義は作成しない
    material 条件だけ craftbound:gold に変更する
    protective_lining 1個を通常Ingredientとして要求する
    metal_part のみ contributes_to_quality = true にする
    完成品を対応する minecraft:golden_* にする

レシピ一致判定:
    part_type が対象部位と一致することを確認する
    material がレシピ指定素材と一致することを確認する
    protective_lining が指定位置に存在することを確認する
    品質値そのものは一致条件にしない

完成品生成:
    既存 QualityAssemblyRecipe.assemble に任せる
    防具本体だけを品質寄与対象とする
    完成品品質 = 防具本体品質 とする
    鉄と金で品質計算処理を分岐しない
    組み立て経験値は付与しない
```

# 境界条件・例外

- 鉄レシピへ `material = craftbound:gold` の防具本体を入れても成立しない。
- 金レシピへ `material = craftbound:iron` の防具本体を入れても成立しない。
- 同じ Item ID の防具本体であっても、`material` が異なれば対応する完成レシピだけに一致する。
- 金用に `craftbound:gold/helmet_body` 等の `part_type` を要求しない。存在しない金専用パーツ定義を追加しない。
- 防具本体の品質値はレシピ一致条件にしない。品質の高低に関係なく組み立て可能とする。
- `protective_lining` は `kind: "ingredient"` とし、品質寄与設定を追加しない。
- `protective_lining` に偶発的に品質状態が付与されても完成品品質へ寄与させない。
- Vanilla の鉄防具・金防具レシピは削除・上書きしない。
- 防具本体の既存 `ingredient_count`、鋳造条件、鍛造条件は変更しない。
- 金属素材 `craftbound:gold` の既存定義・登録内容を変更しない。
- `protective_lining.png` が未配置でも、本タスクではモデルJSONの参照先が正しければよい。
- 既存 `quality_assembly` に本タスクを阻害する明確な不具合が見つかった場合のみ、要件達成に必要な最小修正を許可する。一般的なリファクタリングは行わない。

# 完了条件

- `craftbound:protective_lining` が通常 Item として登録されている。
- 革1個 + 任意色の羊毛1個から `protective_lining` 1個を作成できる。
- `protective_lining` に品質付与処理が存在しない。
- 鉄製防具4部位の `quality_assembly` JSONが存在する。
- 金製防具4部位の `quality_assembly` JSONが存在する。
- 鉄用4レシピは既存防具本体の `material = craftbound:iron` を要求する。
- 金用4レシピは同じ既存防具本体 `part_type` を使用し、`material = craftbound:gold` を要求する。
- 金用 `metal_parts` 定義、金専用防具本体Item、金専用鋳型が追加されていない。
- 例えば品質87・素材ironの `chestplate_body` から作成した `minecraft:iron_chestplate` の品質が87になる。
- 例えば品質82・素材goldの同じ `chestplate_body` から作成した `minecraft:golden_chestplate` の品質が82になる。
- 素材が異なる完成レシピへ誤一致しない。
- `protective_lining` が不足している場合は完成レシピが成立しない。
- Vanilla の通常鉄防具・金防具レシピが引き続き利用可能である。
- `en_us.json` と `ja_jp.json` に `protective_lining` の表示名が追加されている。
- `protective_lining` のモデルJSONが正しいテクスチャパスを参照する。
- Codex が `protective_lining.png` を生成・追加していない。
- `vanilla.md` に今回の鉄・金防具完成工程が反映されている。
- 既存 assembly Java 実装および既存 metal part 定義に不要な変更が入っていない。

# 今回の対象外

- `protective_lining.png` の作成・追加・編集
- 銅防具
- チェーン防具
- 革防具
- ダイヤモンド防具
- ネザライト防具の品質継承処理
- 馬鎧、カメの甲羅、オオカミの鎧
- 他MOD防具向けの追加レシピ
- 金専用の防具本体 Item / metal part 定義 / 鋳型
- `protective_lining` の品質システム
- `protective_lining` の非金属加工ミニゲーム対応
- 防具本体の鋳造・鍛造条件の変更
- 鍛造工程でのパーツ分岐機構
- `quality_assembly` の新機能追加・一般化・リファクタリング
- 組み立て時の鍛冶師経験値付与
