# 鍛冶システムJSON・評価関数仕様

## 1. 本書の目的

本書は、鍛冶システムで使用するデータの配置、JSONの責務、検証規則、および評価関数の境界を定義する。

ゲーム仕様は[鍛冶師システム仕様](./blacksmith.md)、素材初期値は[鍛冶素材仕様](./materials.md)、最初の具体例は[鉄のピッケル試作仕様](./iron-pickaxe.md)、検証項目は[鍛冶システムテスト仕様](./test-cases.md)に従う。

本書に記載するJava上の名称は責務を示す概念名であり、実装時にはプロジェクトの命名規則へ合わせてよい。

---

## 2. 基本方針

- 加熱時間、鍛造条件、品質段階、性能補正などの調整値をソースコードへ直接記述しない
- データパックのリロードによってJSONを再読込できる構造とする
- JSONには `schema_version` を持たせ、形式変更を検出できるようにする
- アイテム、ブロック、タグ、評価関数は名前空間付きIDで参照する
- JSONの読込、検証、評価、ゲーム状態の変更を別責務にする
- 不正なJSONは該当定義だけを無効化し、ファイルと項目を特定できるエラーを記録する
- 品質と素材消費の最終結果はサーバー側で確定する

---

## 3. データ配置

鍛冶データは、原則として以下へ配置する。

```text
data/<namespace>/blacksmith/
├─ settings.json
├─ metals/
│  └─ <metal_id>.json
├─ metal_parts/
│  └─ <material_id>/
│     └─ <part_id>.json
├─ non_metal_materials/
│  └─ <material_id>.json
├─ non_metal_parts/
│  └─ <material_id>/
│     └─ <part_id>.json
├─ shapes/
│  └─ <shape_id>.json
├─ integrations/
│  └─ <mod_id>/
├─ quality.json
└─ skill_assists.json
```

`settings.json` はパーツや金属へ依存しない共通設定を持つ。MVPの初期値は以下のとおりとする。

```json
{
  "schema_version": 1,
  "crucible_capacity_units": 16,
  "network": {
    "forging_latency_compensation_ticks": 4,
    "carving_packet_interval_ticks": 2,
    "carving_max_cells_per_request": 64,
    "workbench_interaction_distance": 8.0,
    "session_heartbeat_ticks": 20,
    "disconnect_grace_ticks": 100
  }
}
```

`crucible_capacity_units` はインゴット換算の最大容量を表す。`network` の値はサーバー側検証に使用し、クライアントから変更できないようにする。

完成品の組み立てレシピは、通常のデータパックレシピと同じ場所へ配置する。

```text
data/<namespace>/recipes/blacksmith/<recipe_id>.json
```

共通設備と道具のレシピは[設備・道具・鋳型仕様](./equipment/stations-and-tools.md)、バニラ完成品のレシピは[バニラ装備構成仕様](./equipment/vanilla.md)に従う。

他MOD連携データも連携先の名前空間を直接コードへ埋め込まず、同じ形式のJSONとレシピで定義する。ルートテーブル変更などJSONだけで表現できない初期化処理は、MODごとの連携モジュールへ分離する。

---

## 4. 金属定義

配置先は `blacksmith/metals/<metal_id>.json` とする。

金属定義は、素材そのものの加熱特性を持つ。鋳型、必要量、打撃回数など、パーツ固有の条件は持たせない。

| 項目 | 必須 | 内容 |
|---|---|---|
| `schema_version` | 必須 | JSON形式のバージョン |
| `ingredient` | 必須 | 金属として受け付けるアイテムまたはタグ |
| `lump_loss_ratio` | 必須 | 金属塊化した際に失う割合 |
| `lump_loss_rounding` | 必須 | 損失量の端数処理 |
| `castable_after_ticks` | 必須 | 鋳造可能になる最小加熱時間 |
| `score_curve` | 必須 | 加熱時間と評価値の組 |
| `danger_after_ticks` | 必須 | 全消失警告を開始する時間 |
| `destroy_after_ticks` | 必須 | 全消失する時間 |
| `heating_evaluator` | 必須 | 使用する加熱評価関数のID |

鉄定義の確定部分は以下のとおり。

```json
{
  "schema_version": 1,
  "ingredient": {
    "item": "minecraft:iron_ingot"
  },
  "lump_loss_ratio": 0.5,
  "lump_loss_rounding": "ceil",
  "castable_after_ticks": 200,
  "score_curve": [
    { "ticks": 200, "score": 40 },
    { "ticks": 300, "score": 100 },
    { "ticks": 360, "score": 100 },
    { "ticks": 480, "score": 40 },
    { "ticks": 560, "score": 0 }
  ],
  "danger_after_ticks": 480,
  "destroy_after_ticks": 560,
  "heating_evaluator": "craftbound:linear_curve"
}
```

1秒は20ゲームティックとして扱う。

金属塊化した際の損失量と回収量は、以下の式で計算する。

```text
損失量 = roundByMode(使用量 × lump_loss_ratio, lump_loss_rounding)
金属塊の量 = 使用量 - 損失量
```

回収量が `0` の場合は金属塊を生成しない。この規則は、早すぎる鋳造と打撃回数超過による破損の両方へ適用する。返却する金属塊のサイズと個数は、失敗した金属パーツの `failure_lump` から取得する。

---

## 5. 金属パーツ定義

配置先は `blacksmith/metal_parts/<material_id>/<part_id>.json` とする。

金属パーツ定義は、製作するパーツ固有の材料量、鋳型、冷却時間、鍛造条件、品質合成方法を持つ。

| 項目 | 必須 | 内容 |
|---|---|---|
| `schema_version` | 必須 | JSON形式のバージョン |
| `metal` | 必須 | `metals/` に定義した金属ID |
| `ingredient_count` | 必須 | 必要な金属量 |
| `mold` | 必須 | 必要な鋳型のアイテムまたはブロックID |
| `output` | 必須 | 鍛造後に生成するパーツID |
| `failure_lump` | 必須 | 失敗時に返却する金属塊のアイテム、個数、1個あたりの素材量 |
| `cooling` | 必須 | 安全冷却時間と破損回数上限の計算設定 |
| `forging` | 必須 | 打撃条件と評価設定 |
| `part_quality` | 必須 | 工程評価の合成方法 |

`forging` は以下を持つ。

| 項目 | 必須 | 内容 |
|---|---|---|
| `strength_min` | 必須 | 適正強度の下限 |
| `strength_max` | 必須 | 適正強度の上限 |
| `strength_penalty_per_point` | 必須 | 適正範囲から1離れた場合の減点 |
| `ideal_hits` | 必須 | 適正打撃回数 |
| `hit_count_penalty` | 必須 | 適正回数から1回ずれた場合の減点 |
| `break_on_hit` | 必須 | パーツが破損する打撃回数 |
| `strength_weight` | 必須 | 強度評価の重み |
| `hit_count_weight` | 必須 | 回数評価の重み |
| `evaluator` | 必須 | 使用する鍛造評価関数のID |

`cooling` は以下を持つ。

| 項目 | 必須 | 内容 |
|---|---|---|
| `surface_solid_ticks` | 必須 | 蒸気音と蒸気量の変化を発生させる表面凝固時間 |
| `safe_ticks` | 必須 | 通常の破損回数上限へ達する安全冷却時間 |
| `minimum_break_on_hit` | 必須 | 冷却時間が0の場合の最小破損回数 |
| `evaluator` | 必須 | 使用する冷却破損上限関数のID |

鉄製ピッケルヘッドについて、値が確定した項目だけを示す未完成例は以下のとおり。このまま実データとして登録することはできない。

```json
{
  "schema_version": 1,
  "metal": "craftbound:iron",
  "ingredient_count": 3,
  "mold": "craftbound:pickaxe_head_mold",
  "output": "craftbound:iron_pickaxe_head",
  "failure_lump": {
    "item": "craftbound:small_metal_lump",
    "count": 1,
    "units_per_item": 1
  },
  "cooling": {
    "surface_solid_ticks": 40,
    "safe_ticks": 100,
    "minimum_break_on_hit": 1,
    "evaluator": "craftbound:linear_cooling_break_limit"
  },
  "forging": {
    "strength_min": 55,
    "strength_max": 65,
    "strength_penalty_per_point": 5,
    "ideal_hits": 6,
    "hit_count_penalty": 20,
    "break_on_hit": 10,
    "strength_weight": 0.8,
    "hit_count_weight": 0.2,
    "evaluator": "craftbound:weighted_forging"
  },
  "part_quality": {
    "heating_weight": 0.4,
    "forging_weight": 0.6,
    "evaluator": "craftbound:weighted_metal_part"
  }
}
```

冷却破損上限関数は、鋳型から取り出した時点の冷却ティック、`cooling.safe_ticks`、`forging.break_on_hit` から有効な破損回数を計算する。安全冷却時間を超えた経過時間は切り捨て、通常の `break_on_hit` を超える値を返さない。

この関数は粗加工パーツを鋳型から取り出す際に一度だけ呼び出す。取り出し時の冷却ティックと計算結果を粗加工パーツへ保存し、その後の時間経過では更新しない。

`failure_lump.units_per_item` は、元の金属定義の `ingredient` を基準とした金属塊1個あたりの素材量を表す。鉄製ピッケルヘッドでは「金属塊・小」を1個返却し、`units_per_item: 1` を鉄インゴット1個分として扱う。金属塊は金属IDをデータとして保持し、同じ金属かつ同じサイズのものだけをスタック可能とする。中・大の金属塊が表す素材量は未決定とする。

`cooling.surface_solid_ticks` 到達時には一度だけ蒸気音を再生し、蒸気パーティクルを多い状態から少ない状態へ変更する。`cooling.safe_ticks` 到達時には、安全冷却の正解を公開する音や通知を発生させない。

---

## 6. 非金属素材・パーツ定義

### 6.1 非金属素材プロファイル

配置先は `blacksmith/non_metal_materials/<material_id>.json` とする。初期値と素材区分は[鍛冶素材仕様](./materials.md)に従う。

| 項目 | 必須 | 内容 |
|---|---|---|
| `schema_version` | 必須 | JSON形式のバージョン |
| `tool` | 必須 | 必要な加工用アイテムまたはタグ |
| `remove_per_pass` | 必須 | ブラシが1回通過したセルから減らす残存量 |
| `path_interpolation` | 必須 | ドラッグ経路の補間方式。MVPでは `supercover` 固定 |
| `removed_units_per_durability` | 必須 | 道具耐久値を1消費するセル相当の累積除去量 |

木材プロファイルは以下のとおり。

```json
{
  "schema_version": 1,
  "tool": {
    "item": "craftbound:carving_knife"
  },
  "remove_per_pass": 0.5,
  "path_interpolation": "supercover",
  "removed_units_per_durability": 8
}
```

パーツ側で素材プロファイルの値を上書きしない。異なる加工感が必要な特殊素材には、専用の素材プロファイルを追加する。

### 6.2 非金属パーツ

配置先は `blacksmith/non_metal_parts/<material_id>/<part_id>.json` とする。

| 項目 | 必須 | 内容 |
|---|---|---|
| `schema_version` | 必須 | JSON形式のバージョン |
| `material_profile` | 必須 | `non_metal_materials/` に定義した素材プロファイルID |
| `ingredient` | 必須 | 素材として受け付けるアイテムまたはタグ |
| `ingredient_count` | 必須 | 必要な素材数 |
| `output` | 必須 | 完成パーツID |
| `shape` | 必須 | パーツ種別ごとに `shapes/` へ定義した理想形状ID |
| `base_grid_size` | 必須 | スキルなしで使用する解像度 |
| `warning_at_or_below_retention` | 必須 | 危険通知を開始する理想形状の残存率 |
| `destroy_at_or_below_retention` | 必須 | 素材消失となる理想形状の残存率 |
| `shape_evaluator` | 必須 | 使用する形状評価関数のID |

板材からピッケルの柄を作る定義の確定部分は以下のとおり。

```json
{
  "schema_version": 1,
  "material_profile": "craftbound:wood",
  "ingredient": {
    "tag": "minecraft:planks"
  },
  "ingredient_count": 1,
  "output": "craftbound:pickaxe_handle",
  "shape": "craftbound:pickaxe_handle",
  "base_grid_size": 16,
  "warning_at_or_below_retention": 0.6,
  "destroy_at_or_below_retention": 0.5,
  "shape_evaluator": "craftbound:iou"
}
```

加工道具の最大耐久値は[鍛冶素材仕様](./materials.md)で定義する。ドラッグ中は直前の入力位置から現在位置までの通過セルを補間し、通過セルごとに除去処理を行う。描画フレーム数は除去量と耐久消費へ影響させない。耐久消費に満たない累積除去量は加工状態へ保存する。

入力座標は加工領域に対する `[0.0, 1.0)` の正規化座標とする。`supercover` は始点と終点を結ぶ線分が交差する全セルを列挙する。同じストロークIDの通信区間で重複したセルと、直前の通信区間の終点に相当する先頭セルは除外する。ストロークIDが変わった場合は重複履歴を破棄し、同じセルを再び加工できる。

ブラシ半径 `0.5` はカーソルを含むセルだけを返す。半径が `0.5` より大きい場合は、セル単位へ変換したカーソル位置とセル中心のユークリッド距離が半径以下のセルを返す。グリッド外のセルは処理対象に含めない。

理想形状として残すべき領域の残存率が `warning_at_or_below_retention` 以下になった場合は、警告音とGUI枠の色変化を発生させる。`destroy_at_or_below_retention` 以下になった場合は即時失敗とする。専用の亀裂表示はデータ上の必須要素とせず、低コストで共通描画できる場合だけ追加する。

---

## 7. 理想形状定義

配置先は `blacksmith/shapes/<shape_id>.json` とする。

理想形状のIDは、`pickaxe_handle` などのパーツ種別を表す。素材名や、そのパーツを使用する完成アイテム名では定義しない。

同じパーツ種別を複数の素材から加工する場合は、各非金属パーツ定義から同じ理想形状IDを参照する。素材による硬さ、削れ幅、入力誤差などは加工素材側で表現し、理想形状へ含めない。

理想形状は、ゲーム内で利用可能な最大グリッド解像度のバイナリマスクとして定義する。現時点の最大解像度は `32×32` とする。

理想形状JSONは以下の項目を持つ。

| 項目 | 必須 | 内容 |
|---|---|---|
| `schema_version` | 必須 | JSON形式のバージョン |
| `grid_size` | 必須 | 形状を定義する正方形グリッドの一辺。ゲーム内の最大解像度と一致させる |
| `encoding` | 必須 | MVPでは `binary_rows` 固定 |
| `rows` | 必須 | `#`と`.`で構成する理想形状の行配列 |

`binary_rows` では、`#`を残すべき領域、`.`を削るべき領域として扱う。`rows` の要素数と各文字列の長さは、どちらも `grid_size` と一致させる。

下位解像度での加工結果は、正規化座標を使って最大解像度へ展開してから採点する。理想形状自体を下位解像度へ縮小して採点しない。

展開処理には、入力セルと出力セルの正規化領域が重なる面積による加重平均を使用する。

```text
出力セル値 = Σ(入力セル値 × 入力セルと出力セルの重なり面積) / 出力セル面積
```

入力セルと出力セルは、それぞれのグリッドを `[0, 1) × [0, 1)` へ等分した半開区間として扱う。演算順による差を避けるため倍精度で合計し、最終結果だけを `0.0～1.0` へ丸める。最近傍補間と双線形補間は使用しない。

各理想形状は、利用可能な下位解像度へ変換してから最大解像度へ戻した場合に、元の形状と一致してはならない。一致する場合は、下位解像度でも完全再現できてスキルによる精度差が失われるため、定義を無効とする。

`pickaxe_handle` の初期バイナリマスクは以下のとおりとする。全高27セル、上端の接続部は最大幅8セル、中央の軸は幅4～5セル、下端の握り部は最大幅7セルとし、1セル単位の段差を含める。

```json
{
  "schema_version": 1,
  "grid_size": 32,
  "encoding": "binary_rows",
  "rows": [
    "................................",
    "................................",
    "................................",
    "............########............",
    "............########............",
    "............########............",
    ".............#######............",
    ".............######.............",
    "..............#####.............",
    "..............####..............",
    "..............####..............",
    "..............####..............",
    "..............#####.............",
    "..............####..............",
    "..............####..............",
    "..............####..............",
    "..............####..............",
    ".............#####..............",
    "..............####..............",
    "..............####..............",
    "..............####..............",
    "..............####..............",
    "..............#####.............",
    "..............#####.............",
    ".............######.............",
    ".............######.............",
    "............#######.............",
    "............#######.............",
    "............#######.............",
    "............#######.............",
    "................................",
    "................................"
  ]
}
```

このマスクはプレイヤーへ正解ガイドとして表示しない。プレイテストによる形状調整はJSONの変更だけで行う。

---

## 8. 品質定義

配置先は `blacksmith/quality.json` とする。

品質定義は、表示段階、通常レシピ品の品質、アイテム種別ごとの性能補正を持つ。

任意の数式文字列をJSONから実行する方式は採用しない。登録済みの評価関数IDと数値パラメータを指定する。

```json
{
  "schema_version": 1,
  "default_quality": 30,
  "tiers": [
    { "min": 0, "max": 19, "translation_key": "quality.craftbound.poor" },
    { "min": 20, "max": 39, "translation_key": "quality.craftbound.low" },
    { "min": 40, "max": 59, "translation_key": "quality.craftbound.standard" },
    { "min": 60, "max": 79, "translation_key": "quality.craftbound.high" },
    { "min": 80, "max": 100, "translation_key": "quality.craftbound.masterwork" }
  ],
  "modifiers": {
    "attack_damage": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    },
    "projectile_damage": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    },
    "armor": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006,
      "round_to": 0.5
    },
    "armor_toughness": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006,
      "round_to": 0.5
    },
    "mining_speed": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    },
    "work_speed": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    },
    "max_durability": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006,
      "rounding": "round",
      "preserve_damage_ratio": true
    }
  }
}
```

攻撃速度、ノックバック、ノックバック耐性、エンチャント効果は `modifiers` に含めず、品質で変更しない。各倍率は登録時の基礎性能へ一度だけ適用する。

---

## 9. スキル操作支援定義

配置先は `blacksmith/skill_assists.json` とする。

スキル操作支援定義は、スキルレベルに応じて利用可能になるゲージ目盛り、削り幅、グリッド解像度を持つ。品質加点、採点基準の緩和、レシピ解放は定義しない。

MVPでは以下の操作支援段階を使用する。スキルレベルと各段階の対応は未決定とする。

```json
{
  "schema_version": 1,
  "forging_gauge": {
    "min": 0,
    "max": 100,
    "cycle_ticks": 60,
    "waveform": "triangle",
    "initial_value": 0,
    "initial_direction": "up"
  },
  "tiers": [
    {
      "id": "craftbound:base",
      "gauge_mark_interval": 25,
      "grid_size": 16,
      "brush_radius": 1.5
    },
    {
      "id": "craftbound:intermediate",
      "gauge_mark_interval": 10,
      "grid_size": 24,
      "brush_radius": 1.0
    },
    {
      "id": "craftbound:advanced",
      "gauge_mark_interval": 5,
      "grid_size": 32,
      "brush_radius": 0.5
    }
  ]
}
```

ゲージは `0 → 100 → 0` を3秒（60ゲームティック）で往復する。内部値は連続値として進行し、`gauge_mark_interval` へ丸めない。`brush_radius` は現在のグリッドにおけるセル単位の円形半径であり、`0.5` は単一セル相当として扱う。

---

## 10. 熱源定義

有効な熱源は、以下のブロックタグで定義する。

```text
data/craftbound/tags/blocks/blacksmith_heat_sources.json
```

MVPの初期値は以下のとおり。

```json
{
  "replace": false,
  "values": [
    "minecraft:lava",
    "minecraft:fire",
    "minecraft:soul_fire"
  ]
}
```

MVPではすべての熱源を同じ加熱速度として扱う。熱源ごとの速度倍率や燃料消費は将来拡張とする。

---

## 11. 完成品レシピ

完成品レシピは、バニラの作業台から品質付き完成品を生成する独自レシピタイプ `craftbound:quality_assembly` として定義する。

レシピは少なくとも以下を持つ。

- 完成品ID
- 必要な品質付きパーツ
- 品質を持たない追加材料
- 完成品品質評価関数のID
- 評価関数へ渡す重みなどのパラメータ

鉄のピッケルでは、鉄製ピッケルヘッドと `pickaxe_handle` を要求し、算術平均を行う完成品品質評価関数を使用する。

```json
{
  "type": "craftbound:quality_assembly",
  "pattern": [
    "H",
    "S"
  ],
  "key": {
    "H": {
      "item": "craftbound:iron_pickaxe_head"
    },
    "S": {
      "item": "craftbound:pickaxe_handle"
    }
  },
  "result": {
    "item": "minecraft:iron_pickaxe"
  },
  "quality": {
    "evaluator": "craftbound:arithmetic_mean"
  }
}
```

`key` で品質付きパーツを要求したスロットは、品質データを持つ完成パーツだけを受け付ける。クラフト時に `quality.evaluator` を呼び出し、結果の品質と品質から計算した性能補正を完成品へ保存する。

バニラの通常レシピは削除しない。通常レシピから生成した鉄のピッケルには品質 `30` を付与し、`craftbound:quality_assembly` から生成したものだけが入力パーツから品質を計算する。

---

## 12. 評価関数の境界

評価関数は、JSON定義と加工結果を受け取り、評価値または倍率を返す副作用のない処理とする。

最低限、以下の責務を分離する。

| 評価関数 | 入力 | 出力 |
|---|---|---|
| 加熱評価 | 加熱ティック数、金属定義 | `0～100`の加熱評価 |
| 鍛造評価 | 打撃履歴、金属パーツ定義 | `0～100`の鍛造評価 |
| 冷却破損上限 | 鋳型から取り出した時点の冷却ティック、金属パーツ定義 | 固定する有効破損回数 |
| 非金属形状評価 | 加工グリッド、理想形状定義 | `0～100`の形状評価 |
| 金属パーツ品質 | 加熱評価、鍛造評価、金属パーツ定義 | `0～100`のパーツ品質 |
| 完成品品質 | 使用パーツ品質、完成品レシピ | `0～100`の完成品品質 |
| 品質段階変換 | 品質、品質定義 | 表示段階ID |
| 性能補正 | 品質、性能補正定義 | 補正倍率 |

評価関数内では、以下を行わない。

- ワールドやBlock Entityの変更
- アイテムの消費または生成
- GUI状態の変更
- プレイヤースキル値による直接加点
- JSONやレジストリの再読込

作業台やレシピ処理は、評価関数の戻り値を受け取った後に、サーバー側で状態変更とアイテム生成を行う。

評価関数はIDで登録し、JSONから選択できるようにする。未登録の評価関数IDを指定した定義は無効とする。

---

## 13. 検証規則

JSON読込時に、少なくとも以下を検証する。

- `schema_version` が対応範囲内である
- `crucible_capacity_units` と各 `network` 設定が `1` 以上である
- 名前空間付きIDの形式が正しい
- 必須項目が存在する
- 品質、打撃強度、重みが許容範囲内である
- 加熱評価曲線のティック値が昇順である
- `castable_after_ticks < danger_after_ticks < destroy_after_ticks` を満たす
- `surface_solid_ticks < safe_ticks` を満たす
- 適正強度の下限が上限以下である
- 破損回数が適正打撃回数より大きい
- 安全冷却時間が `1` 以上である
- 最小破損回数が `1` 以上かつ通常の破損回数以下である
- `failure_lump.count` と `failure_lump.units_per_item` がともに `1` 以上である
- `failure_lump.count × failure_lump.units_per_item` が金属塊化後の回収量と一致する
- 非金属パーツが参照する `material_profile` が存在する
- 非金属素材プロファイルが使用可能な加工道具を参照する
- `remove_per_pass` が `0` より大きく `1` 以下である
- `removed_units_per_durability` が `0` より大きい
- `path_interpolation` が `supercover` である
- `0 < destroy_at_or_below_retention < warning_at_or_below_retention <= 1` を満たす
- 操作支援段階IDが重複しない
- 操作支援段階が上がるにつれて、グリッド解像度は単調増加し、目盛り間隔とブラシ半径は単調減少する
- 評価の重み合計が `1.0` である
- 品質段階が `0～100` を重複なく覆う
- グリッド解像度が実装上の上限を超えない
- 理想形状の `grid_size` がゲーム内の最大グリッド解像度と一致する
- 理想形状の行数と各行の文字数が `grid_size` と一致する
- 理想形状が `#`と`.`以外の文字を含まない
- 理想形状を各下位解像度へ変換して戻した結果が、元の形状と一致しない
- 参照先の金属、パーツ、形状、評価関数が存在する
- 品質補正対象と丸め設定が登録済みの形式である
- 各データコンポーネントの `data_version` が移行可能な範囲にある

検証に失敗した場合は、ファイルパス、項目名、問題の値をログへ出力する。該当定義は登録せず、他の正常な定義の読込は継続する。

---

## 14. 加工状態の保存

るつぼ、粗加工パーツ、鍛造中パーツ、非金属加工中パーツは、工程に応じて以下の状態をデータコンポーネントまたはBlock Entityへ保存する。

| 項目 | 内容 |
|---|---|
| `data_version` | 保存形式のバージョン |
| `part_id` | 対象パーツ種別ID |
| `material` | 素材ID、素材プロファイルID、素材量 |
| `heating_ticks` | 固定または進行中の加熱経過時間 |
| `heating_score` | 鋳型へ流し込んだ時点で確定した加熱評価 |
| `cooling_ticks` | 鋳型から取り出した時点の冷却時間 |
| `surface_solid_notified` | 表面凝固の音を再生済みかどうか |
| `effective_break_on_hit` | 冷却時間から計算して固定した破損回数上限 |
| `strike_history` | 打撃強度を順番に保持する履歴 |
| `gauge_value` | 中断時の打撃強度ゲージ位置 |
| `gauge_direction` | 中断時の打撃強度ゲージ移動方向 |
| `carving_grid` | 各セルの残存量 |
| `carving_removed_units` | 次の道具耐久消費までの累積除去量 |
| `completed` | 工程が完了済みかどうか |
| `final_quality` | 完成時に確定した品質 |
| `processing_state` | `active`、`output_pending` など、設備の排他状態 |
| `pending_outputs` | インベントリ満杯時に設備が保持する確定済み出力一覧 |
| `definition_snapshot` | 工程開始時の評価関数ID、閾値、重み、その他必要な設定 |
| `session_id` | 操作要求を加工状態へ関連付ける一意なID |
| `last_sequence` | 最後に受理したクライアント要求の連番 |
| `active_player` | 現在の操作権を持つプレイヤーID |
| `last_heartbeat_ticks` | 最後に生存通知を受理したサーバーティック |
| `lease_expires_at` | 通信切断後の操作権を解放するサーバーティック |
| `active_stroke_id` | 現在の連続ドラッグを識別するID |
| `last_drag_cell` | 通信区間をまたぐ重複処理を防ぐ直前セル |

工程で使用しない項目は省略してよい。るつぼを溶鉱炉から取り出した場合は `heating_ticks` を進行させず、再投入時に同じ値から再開する。鋳型へ流し込んだ時点で加熱評価を確定し、その後は `heating_score` を更新しない。

`pending_outputs` は作業台のBlock Entityだけに保存する。`processing_state` が `output_pending` の間は新しい素材投入と加工開始を拒否し、全出力をプレイヤーへ渡せた場合だけ一覧を空にして待機状態へ戻す。サーバー再起動後も保留出力を復元する。

`definition_snapshot` は、加工の再開と採点に必要な定義を自己完結して保持する。データパック再読み込み後も開始済みの加工にはスナップショットを適用し、再読み込み後に開始した加工だけが新しい定義を使用する。

加工途中のパーツと完成パーツはスタック不可とする。金属塊は金属IDとサイズを保存し、両方が一致する場合だけスタック可能とする。

---

## 15. データコンポーネント

アイテムへ保存する鍛冶データは、型付きのカスタムデータコンポーネントとして以下へ分離する。汎用の文字列キーだけを持つ非構造化データへまとめない。

| コンポーネントID | 対象 | 主な内容 |
|---|---|---|
| `craftbound:quality` | 完成パーツ、完成品 | `0～100` の品質整数 |
| `craftbound:crucible_contents` | るつぼ | データバージョン、金属ID、量、加熱時間、加熱済みフラグ、定義スナップショット |
| `craftbound:metal_lump` | 金属塊 | データバージョン、金属ID、サイズ、素材量 |
| `craftbound:metal_part_state` | 粗加工・鍛造中パーツ | データバージョン、パーツID、素材、加熱評価、冷却、破損上限、打撃履歴、定義スナップショット |
| `craftbound:non_metal_part_state` | 非金属加工途中品 | データバージョン、パーツID、素材、解像度、加工グリッド、耐久消費用の累積除去量、定義スナップショット |

各コンポーネントは永続化用Codecと通信同期用StreamCodecを持つ。`craftbound:quality` は範囲検証付き整数とし、それ以外の複合コンポーネントは `data_version` を持つ。読込時にはバージョンを検証し、対応する旧形式は現在形式へ移行する。実装より新しいバージョンや移行不能な値は黙って初期化せず、対象アイテムを使用不可にして原因をログへ記録する。

完成したパーツから加工途中コンポーネントを除去し、`craftbound:quality` だけを付与する。通常レシピ品やコンポーネントを持たない対象アイテムは、読取API上で品質 `30` を返す。読取のためだけに既存アイテムを書き換えない。

操作セッションはアイテムコンポーネントへ保存せず、作業台のBlock Entityで管理する。Block EntityはセッションID、操作中プレイヤー、最後に受理した連番、最終生存通知ティック、リース期限、排他状態、保留出力を持つ。サーバー再起動時は加工データ、排他状態、保留出力だけを復元し、操作中プレイヤーとリースを復元しない。

---

## 16. クライアント同期

JSONはサーバー側を正とする。クライアント描画に必要なゲージ範囲、品質段階、グリッド解像度などは、ログイン時またはデータパック再読込時にサーバーから同期する。

クライアントは表示と入力送信だけを担当し、品質評価や素材消費を確定しない。

操作パケットはセッションIDと単調増加する連番を持つ。鍛造入力には同期済みサーバーティック、非金属加工入力にはストロークIDと正規化した始点・終点を含める。サーバーは `settings.json` の通信上限と、加工状態へ保存した `last_sequence` および `active_player` を使用して検証する。

---

## 17. 未決定事項

- スキルレベルと操作支援の対応形式
- 中・大の金属塊が表す素材量
- 溶鉱炉から取り出した時点で冷却を開始する方式へ変更するか
