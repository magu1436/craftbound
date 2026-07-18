# 設備・道具・鋳型仕様

## 1. 本書の目的

本書は、鍛冶システムで使用する設備、加工道具、鋳型のMVP入手方法を定義する。ゲーム内の動作は[鍛冶師システム仕様](../blacksmith.md)、素材と道具の対応は[鍛冶素材仕様](../materials.md)、IDとJSON形式は[鍛冶システムJSON・評価関数仕様](../data-formats.md)に従う。

MVPではバニラ素材だけを要求し、スキルや職業レベルによるレシピ制限を設けない。すべて通常の作業台で製作し、出力数は1個とする。

---

## 2. レシピ配置

レシピは以下へ配置する。

```text
data/craftbound/recipes/blacksmith/equipment/<recipe_id>.json
```

材料記号は以下のとおり。

| 記号 | 材料 |
|---|---|
| `B` | `minecraft:brick` |
| `I` | `minecraft:iron_ingot` |
| `N` | `minecraft:iron_nugget` |
| `S` | `minecraft:smooth_stone` |
| `R` | `minecraft:stick` |
| `P` | `#minecraft:planks` |
| `C` | レシピごとに指定する中心設備 |
| `T` | `minecraft:smithing_table` |

---

## 3. 設備

| レシピID | 出力 | 形式 | パターン | 追加キー |
|---|---|---|---|---|
| `blacksmith_furnace` | `craftbound:blacksmith_furnace` | shaped | `BIB / IFI / BIB` | `F = minecraft:blast_furnace` |
| `casting_table` | `craftbound:casting_table` | shaped | `SSS / ICI /  S ` | `C = minecraft:stonecutter` |
| `forging_table` | `craftbound:forging_table` | shaped | `SSS /  B  /  T ` | `B = minecraft:iron_block` |
| `non_metal_workbench` | `craftbound:non_metal_workbench` | shaped | `IPI / PCP` | `C = minecraft:crafting_table` |

設備レシピの材料数は以下のとおり。

| 出力 | 材料数 |
|---|---|
| 溶鉱炉 | 溶鉱炉1、鉄インゴット4、レンガ4 |
| 鋳造用作業台 | 石切台1、滑らかな石4、鉄インゴット2 |
| 鍛造台 | 鍛冶台1、鉄ブロック1、滑らかな石3 |
| 非金属加工用作業台 | 作業台1、鉄インゴット2、板材3 |

---

## 4. 加工道具

| レシピID | 出力 | 形式 | パターン | 材料数 |
|---|---|---|---|---|
| `crucible` | `craftbound:crucible` | shaped | `B B / B B / BBB` | レンガ7 |
| `smithing_hammer` | `craftbound:smithing_hammer` | shaped | `III /  R  /  R ` | 鉄インゴット3、棒2 |
| `carving_knife` | `craftbound:carving_knife` | shaped | `I / R` | 鉄インゴット1、棒1 |
| `chisel` | `craftbound:chisel` | shaped | `I / N / R` | 鉄インゴット1、鉄塊1、棒1 |
| `tailoring_shears` | `craftbound:tailoring_shears` | shaped | ` I / RI` | 鉄インゴット2、棒1 |

彫刻ナイフの最大耐久値は `128`、タガネは `192`、裁断ばさみは `238` とする。ハンマーはMVPでは耐久値を消費しない。ハンマーの耐久システムは将来拡張とする。

---

## 5. 鋳型

標準鋳型の素材となる空鋳型はshapelessレシピとする。

| レシピID | 出力 | 材料 |
|---|---|---|
| `blank_mold` | `craftbound:blank_mold` | レンガ5、鉄塊3 |

空鋳型1個を石切台へ入れ、以下の標準鋳型のいずれか1個へ加工する。加工後の鋳型を空鋳型または別の鋳型へ戻すことはできない。

| 区分 | 標準鋳型ID |
|---|---|
| 武器・ツール | `sword_blade_mold`、`pickaxe_head_mold`、`axe_head_mold`、`shovel_head_mold`、`hoe_head_mold` |
| 防具 | `helmet_body_mold`、`chestplate_body_mold`、`leggings_body_mold`、`boots_body_mold`、`horse_armor_body_mold` |
| 小物 | `iron_ring_mold`、`crossbow_trigger_mold`、`shield_boss_mold`、`shears_blades_mold`、`fire_striker_mold`、`brush_head_mold` |

各変換は以下へstonecuttingレシピとして配置する。

```text
data/craftbound/recipes/blacksmith/equipment/molds/<part_id>_mold.json
```

標準鋳型は金属の種類に依存せず、MVPでは耐久値を持たず、繰り返し使用できる。新しい標準金属パーツを追加する場合は石切台レシピを追加し、金属パーツJSONから鋳型IDを参照する。

ボス素材装備などの専用鋳型は空鋳型から製作できない。対象MODのボス、構造物、探索報酬から入手し、入手方法は連携仕様で定義する。

---

## 6. 受け入れ条件

- 各レシピが指定した材料数だけを消費し、出力を1個生成する
- 板材タグに含まれる任意のバニラ板材で非金属加工用作業台を製作できる
- レシピ競合がある場合に、ファイルと競合先を特定できるログを出力する
- 未導入MODのアイテムを共通設備レシピから参照しない
- 空鋳型1個から任意の標準鋳型1個を石切台で製作できる
- 完成した標準鋳型を別種類へ再変換できない
- 専用鋳型を空鋳型から製作できない
- 鋳型を使用しても耐久値や個数が減少しない
