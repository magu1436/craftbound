# 設備・道具・鋳型仕様

## 1. 本書の目的

本書は、鍛冶システムで使用する設備、加工道具、鋳型のMVP入手方法を定義する。ゲーム内の動作は[鍛冶師システム仕様](../blacksmith.md)、IDとJSON形式は[鍛冶システムJSON・評価関数仕様](../data-formats.md)に従う。

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
| `smithing_hammer` | `craftbound:smithing_hammer` | shaped | `III /  S  /  S ` | 鉄インゴット3、棒2 |
| `carving_knife` | `craftbound:carving_knife` | shaped | `I / S` | 鉄インゴット1、棒1 |

この表では、加工道具の `S` は `minecraft:stick` を表す。彫刻ナイフの最大耐久値は `128` とする。ハンマーはMVPでは耐久値を消費しない。ハンマーの耐久システムは将来拡張とする。

---

## 5. 鋳型

ピッケルヘッド用鋳型はshapelessレシピとする。

| レシピID | 出力 | 材料 |
|---|---|---|
| `pickaxe_head_mold` | `craftbound:pickaxe_head_mold` | レンガ5、鉄塊3 |

鋳型はMVPでは耐久値を持たず、繰り返し使用できる。新しいパーツ種別を追加する場合は、同じディレクトリへ鋳型レシピを追加し、金属パーツJSONから鋳型IDを参照する。

---

## 6. 受け入れ条件

- 各レシピが指定した材料数だけを消費し、出力を1個生成する
- 板材タグに含まれる任意のバニラ板材で非金属加工用作業台を製作できる
- レシピ競合がある場合に、ファイルと競合先を特定できるログを出力する
- 未導入MODのアイテムを共通設備レシピから参照しない
- 鋳型を使用しても耐久値や個数が減少しない
