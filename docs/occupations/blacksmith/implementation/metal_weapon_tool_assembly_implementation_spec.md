# 目的

`develop` の既存 `craftbound:quality_assembly` と鉄・金防具の実装方針を再利用し、鉄・金の剣／ツールを品質付きパーツから組み立てられるようにする。

対象は剣、ピッケル、斧、シャベル、クワ。

ツール用の木製柄は種類別に増やさず、既存 `pickaxe_handle` を `tool_handle` へ改名・汎用化してピッケル／斧／シャベル／クワで共有する。剣のみ形状・意味が異なるため `sword_handle` を別途追加する。

新しい組み立てロジックは作らず、既存 Java 実装を利用する。Codex は原則として本書に列挙したファイルのみ確認・変更し、パスや API が `develop` と一致しない場合だけ追加探索する。

# 確定した仕様

## 完成品と構成

| 完成品種別 | 金属パーツ | 補助パーツ |
|---|---|---|
| 剣 | `sword_blade` | `sword_handle` |
| ピッケル | `pickaxe_head` | `tool_handle` |
| 斧 | `axe_head` | `tool_handle` |
| シャベル | `shovel_head` | `tool_handle` |
| クワ | `hoe_head` | `tool_handle` |

鉄・金の両素材を対象とするため完成品は計10種類。

```text
minecraft:iron_sword
minecraft:iron_pickaxe
minecraft:iron_axe
minecraft:iron_shovel
minecraft:iron_hoe

minecraft:golden_sword
minecraft:golden_pickaxe
minecraft:golden_axe
minecraft:golden_shovel
minecraft:golden_hoe
```

既存 `iron_pickaxe.json` は削除せず、`pickaxe_handle` 参照だけ `tool_handle` へ更新する。他9レシピを追加する。

## 金属パーツ

既存の以下を共用する。

```text
craftbound:iron/sword_blade
craftbound:iron/pickaxe_head
craftbound:iron/axe_head
craftbound:iron/shovel_head
craftbound:iron/hoe_head
```

金専用のパーツ定義、Item、鋳型は追加しない。鉄・金の判定は `material` のみで行う。

```text
鉄: craftbound:iron
金: craftbound:gold
```

例: 金の斧でも `part_type = craftbound:iron/axe_head`、`material = craftbound:gold` とする。

## `pickaxe_handle` の `tool_handle` 化

既存のピッケル柄実装を新規追加ではなくリネームして汎用化する。

```text
CraftboundItems.PICKAXE_HANDLE -> TOOL_HANDLE
craftbound:pickaxe_handle     -> craftbound:tool_handle
```

変更対象:

```text
src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java
src/main/resources/data/craftbound/blacksmith/non_metal_parts/wood/pickaxe_handle.json
src/main/resources/data/craftbound/blacksmith/shapes/pickaxe_handle.json
src/main/resources/assets/craftbound/models/item/pickaxe_handle.json
src/main/resources/assets/craftbound/textures/item/pickaxe_handle.png
src/main/resources/assets/craftbound/lang/en_us.json
src/main/resources/assets/craftbound/lang/ja_jp.json
```

リネーム後:

```text
blacksmith/non_metal_parts/wood/tool_handle.json
blacksmith/shapes/tool_handle.json
models/item/tool_handle.json
textures/item/tool_handle.png
```

`tool_handle` の表示名:

```text
en_us: Tool Handle
ja_jp: 道具の柄
```

既存 `pickaxe_handle` の加工条件・shape 内容・テクスチャ内容は変更せず、そのまま `tool_handle` として使用する。

`tool_handle` Item は既存と同じ単純な `Item` とし、`stacksTo(1)` を維持する。専用 Item クラスは追加しない。

非金属パーツ定義は以下とする。

```json
{
  "schema_version": 2,
  "material_profile": "craftbound:wood",
  "ingredient": { "tag": "minecraft:planks" },
  "ingredient_count": 1,
  "output": "craftbound:tool_handle",
  "shape": "craftbound:tool_handle",
  "base_grid_size": 16,
  "warning_at_or_below_retention": 0.6,
  "break_condition": { "type": "craftbound:remaining_ratio", "threshold": 0.5 },
  "shape_evaluator": "craftbound:iou"
}
```

shape JSON は既存 `pickaxe_handle.json` の内容をそのまま `tool_handle.json` へ移す。

## `sword_handle` の追加

剣のみ `tool_handle` を使用せず、新規 `craftbound:sword_handle` を追加する。

追加対象:

```text
CraftboundItems.SWORD_HANDLE
src/main/resources/data/craftbound/blacksmith/non_metal_parts/wood/sword_handle.json
src/main/resources/data/craftbound/blacksmith/shapes/sword_handle.json
src/main/resources/assets/craftbound/models/item/sword_handle.json
```

表示名:

```text
en_us: Sword Handle
ja_jp: 剣の柄
```

Item は `new Item(new Item.Properties().stacksTo(1))` とする。

非金属パーツ定義の加工条件は `tool_handle` と同じとし、`output` と `shape` だけ `craftbound:sword_handle` にする。

今回、剣柄固有の加工難度は設計しないため、初期 `blacksmith/shapes/sword_handle.json` は `tool_handle.json` と同じ rows を使用する。将来 JSON のみで別形状へ変更可能な状態にする。

モデル:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "craftbound:item/sword_handle"
  }
}
```

`sword_handle.png` の作成は本実装に含めない。

## quality assembly レシピ

配置先:

```text
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/
```

全レシピの pattern は既存 `iron_pickaxe.json` と同じ縦2マスとする。

```text
H
S
```

- `H`: 金属パーツ。`contributes_to_quality = true`
- `S`: `tool_handle` または `sword_handle`。`contributes_to_quality = true`

ツール側の `S` は共通して以下。

```json
"S": {
  "kind": "nonmetal_part",
  "part_type": "craftbound:wood/tool_handle",
  "material": "craftbound:wood",
  "contributes_to_quality": true
}
```

剣側のみ:

```json
"S": {
  "kind": "nonmetal_part",
  "part_type": "craftbound:wood/sword_handle",
  "material": "craftbound:wood",
  "contributes_to_quality": true
}
```

鉄の斧の完成例:

```json
{
  "type": "craftbound:quality_assembly",
  "pattern": ["H", "S"],
  "key": {
    "H": {
      "kind": "metal_part",
      "part_type": "craftbound:iron/axe_head",
      "material": "craftbound:iron",
      "contributes_to_quality": true
    },
    "S": {
      "kind": "nonmetal_part",
      "part_type": "craftbound:wood/tool_handle",
      "material": "craftbound:wood",
      "contributes_to_quality": true
    }
  },
  "result": { "item": "minecraft:iron_axe", "count": 1 }
}
```

金レシピでは金属パーツの `part_type` を変えず、`material` と完成品だけ変更する。

```text
material: craftbound:gold
result: minecraft:golden_<tool>
```

完成品質は既存 `FinishedItemQualityCalculator` により、金属パーツ品質と柄品質の整数平均とする。

# 採用した実装方針

1. 既存 `pickaxe_handle` を `tool_handle` にリネームし、4種のツールで共有する。
2. 剣だけ `sword_handle` を追加する。
3. 金属パーツは鉄・金で共用し、素材差は既存 `MetalPartState` とレシピの `material` で表現する。
4. 既存 `quality_assembly` の一致判定・品質計算をそのまま使用する。
5. `iron_pickaxe.json` は `tool_handle` 参照へ更新し、他9完成レシピを追加する。
6. 新規 Java ロジックは追加しない。Java変更は Item 登録名の変更と `sword_handle` 登録に限定する。

# 採用しなかった方針

- `pickaxe_handle` / `axe_handle` / `shovel_handle` / `hoe_handle` を個別実装: 同じ用途・材料・加工条件のデータが重複するため。
- 剣も `tool_handle` に統合: 将来、剣柄を武器固有形状・性能へ拡張しにくくなるため。
- 金専用パーツを追加: 現行の材質分離設計と重複するため。
- Vanilla の棒を柄として使用: 柄の加工品質を完成品質へ反映できないため。
- `quality_assembly` Java を変更: 現行機能だけで要件を満たせるため。

# 関連する既存クラス

優先して参照する既存実装:

```text
src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipe.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipeSerializer.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/MetalPartAssemblyIngredient.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/NonMetalPartAssemblyIngredient.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/FinishedItemQualityCalculator.java
```

基準データ:

```text
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_pickaxe.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/iron_helmet.json
src/main/resources/data/craftbound/recipes/blacksmith/quality_assembly/golden_helmet.json
src/main/resources/data/craftbound/blacksmith/non_metal_parts/wood/pickaxe_handle.json
src/main/resources/data/craftbound/blacksmith/shapes/pickaxe_handle.json
```

# データフロー

```text
板材1個
  ↓ 細工台
品質付き tool_handle / sword_handle

金属
  ↓ 溶解・鋳造・鍛造
品質付き sword_blade / tool head

金属パーツ + 柄
  ↓ craftbound:quality_assembly
各入力の品質を取得
  ↓
整数平均
  ↓
鉄製または金製の完成品へ品質を付与
```

ツール4種では同じ `tool_handle` を使用する。どの完成品になるかは組み合わせる金属パーツの `part_type` とレシピで決まる。

# 擬似コード

```text
既存ピッケル柄の汎用化:
    CraftboundItems の PICKAXE_HANDLE を TOOL_HANDLE に変更
    item ID を pickaxe_handle から tool_handle に変更
    非金属パーツ定義・shape・model・texture のファイル名と参照IDを tool_handle へ変更
    lang key を tool_handle へ変更

剣柄追加:
    SWORD_HANDLE を登録
    wood/sword_handle 非金属パーツ定義を追加
    sword_handle shape を追加
    item model と lang を追加

完成レシピ:
    iron_pickaxe の柄条件を wood/tool_handle へ変更

    剣・斧・シャベル・クワの鉄レシピを追加
    剣・ピッケル・斧・シャベル・クワの金レシピを追加

    ツールの場合:
        柄 = wood/tool_handle
    剣の場合:
        柄 = wood/sword_handle

    鉄の場合:
        material = craftbound:iron
    金の場合:
        material = craftbound:gold

    金属パーツと柄の contributes_to_quality を true にする
```

# 境界条件・例外

- 旧 `craftbound:pickaxe_handle` は互換アイテムとして残さない。開発中データの移行対応も行わない。
- `pickaxe_handle` を参照するレシピ・モデル・lang・データ定義が残らないよう検索して更新する。
- `tool_handle` はピッケル／斧／シャベル／クワのどのレシピにも使用可能だが、金属パーツ種別が一致しない別レシピへ誤マッチしてはならない。
- 鉄パーツを金完成品へ、金パーツを鉄完成品へ使用できないこと。`material` 判定は既存処理に任せる。
- 品質データを持たない通常 Item は `nonmetal_part` / `metal_part` 条件を満たさない。
- `sword_handle.png` が未配置の場合でも Java／データ実装自体は完了扱いとする。テクスチャ欠落は既知の表示上の問題として許容する。
- 銅、ダイヤモンド、ネザライト、木、石の完成レシピは追加しない。

# 完了条件

- `pickaxe_handle` が `tool_handle` へ置換され、旧参照が残っていない。
- 細工台で板材1個から品質付き `tool_handle` を作成できる。
- 細工台で板材1個から品質付き `sword_handle` を作成できる。
- 鉄・金それぞれで剣、ピッケル、斧、シャベル、クワを `quality_assembly` できる。
- ピッケル／斧／シャベル／クワが同じ `tool_handle` を要求する。
- 剣だけ `sword_handle` を要求する。
- 金完成レシピが既存 `craftbound:iron/<part>` 定義と `material = craftbound:gold` を使用する。
- 完成品質が金属パーツ品質と柄品質の整数平均になる。
- `quality_assembly` / carving の既存 Java ロジックに不要な変更がない。
- 新しい金専用パーツ定義・金専用 Item・金専用鋳型が追加されていない。

# 今回の対象外

- 銅装備
- 木・石・ダイヤモンド製ツールの組み立て
- ネザライトへの品質継承
- 剣柄とツール柄で異なる加工難度・shape を設計すること
- `sword_handle` の PNG テクスチャ作成
- 完成品品質による具体的な性能補正の追加・変更
- `quality_assembly` 自体の仕様変更
- 組み立て時の鍛冶経験値付与
- 既存ワールド／旧 `pickaxe_handle` のデータ移行
