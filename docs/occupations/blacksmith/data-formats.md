# 鍛冶システムJSON・評価関数仕様

## 1. 本書の目的

本書は、鍛冶システムで使用するデータの配置、JSONの責務、検証規則、および評価関数の境界を定義する。

ゲーム仕様は[鍛冶師システム仕様](./blacksmith.md)、最初の具体例は[鉄のピッケル試作仕様](./iron-pickaxe.md)に従う。

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
├─ metals/
│  └─ <metal_id>.json
├─ metal_parts/
│  └─ <part_id>.json
├─ non_metal_parts/
│  └─ <part_id>.json
├─ shapes/
│  └─ <shape_id>.json
├─ quality.json
└─ skill_assists.json
```

完成品の組み立てレシピは、通常のデータパックレシピと同じ場所へ配置する。

```text
data/<namespace>/recipes/blacksmith/<recipe_id>.json
```

他MOD連携データも連携先の名前空間を直接コードへ埋め込まず、同じ形式のJSONとレシピで定義する。ルートテーブル変更などJSONだけで表現できない初期化処理は、MODごとの連携モジュールへ分離する。

---

## 4. 金属定義

配置先は `blacksmith/metals/<metal_id>.json` とする。

金属定義は、素材そのものの加熱特性を持つ。鋳型、必要量、打撃回数など、パーツ固有の条件は持たせない。

| 項目 | 必須 | 内容 |
|---|---|---|
| `schema_version` | 必須 | JSON形式のバージョン |
| `ingredient` | 必須 | 金属として受け付けるアイテムまたはタグ |
| `lump_item` | 必須 | 再加熱可能な金属塊のアイテムID |
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
  "lump_item": "craftbound:iron_lump",
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

回収量が `0` の場合は金属塊を生成しない。この規則は、早すぎる鋳造と打撃回数超過による破損の両方へ適用する。

---

## 5. 金属パーツ定義

配置先は `blacksmith/metal_parts/<part_id>.json` とする。

金属パーツ定義は、製作するパーツ固有の材料量、鋳型、冷却時間、鍛造条件、品質合成方法を持つ。

| 項目 | 必須 | 内容 |
|---|---|---|
| `schema_version` | 必須 | JSON形式のバージョン |
| `metal` | 必須 | `metals/` に定義した金属ID |
| `ingredient_count` | 必須 | 必要な金属量 |
| `mold` | 必須 | 必要な鋳型のアイテムまたはブロックID |
| `output` | 必須 | 鍛造後に生成するパーツID |
| `cooling_ticks` | 必須 | 鋳造後の冷却時間 |
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

鉄製ピッケルヘッドについて、値が確定した項目だけを示す未完成例は以下のとおり。このまま実データとして登録することはできない。

```json
{
  "schema_version": 1,
  "metal": "craftbound:iron",
  "ingredient_count": 3,
  "mold": "craftbound:pickaxe_head_mold",
  "output": "craftbound:iron_pickaxe_head",
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

`cooling_ticks` は必須項目だが、鉄製ピッケルヘッドの値が未決定であるため、上記の確定部分には記載していない。実データを登録する前に値を決定し、項目を追加する。

---

## 6. 非金属パーツ定義

配置先は `blacksmith/non_metal_parts/<part_id>.json` とする。

| 項目 | 必須 | 内容 |
|---|---|---|
| `schema_version` | 必須 | JSON形式のバージョン |
| `ingredient` | 必須 | 素材として受け付けるアイテムまたはタグ |
| `ingredient_count` | 必須 | 必要な素材数 |
| `tool` | 必須 | 必要な加工用アイテムまたはタグ |
| `output` | 必須 | 完成パーツID |
| `shape` | 必須 | パーツ種別ごとに `shapes/` へ定義した理想形状ID |
| `base_grid_size` | 必須 | スキルなしで使用する解像度 |
| `destroy_below_retention` | 必須 | 素材消失となる理想形状の残存率 |
| `shape_evaluator` | 必須 | 使用する形状評価関数のID |

板材からピッケルの柄を作る定義の確定部分は以下のとおり。

```json
{
  "schema_version": 1,
  "ingredient": {
    "tag": "minecraft:planks"
  },
  "ingredient_count": 1,
  "tool": {
    "item": "craftbound:carving_knife"
  },
  "output": "craftbound:pickaxe_handle",
  "shape": "craftbound:pickaxe_handle",
  "base_grid_size": 16,
  "destroy_below_retention": 0.5,
  "shape_evaluator": "craftbound:iou"
}
```

---

## 7. 理想形状定義

配置先は `blacksmith/shapes/<shape_id>.json` とする。

理想形状のIDは、`pickaxe_handle` などのパーツ種別を表す。素材名や、そのパーツを使用する完成アイテム名では定義しない。

同じパーツ種別を複数の素材から加工する場合は、各非金属パーツ定義から同じ理想形状IDを参照する。素材による硬さ、削れ幅、入力誤差などは加工素材側で表現し、理想形状へ含めない。

理想形状は、解像度に依存しない正規化座標として定義する。実際の `16×16`、`24×24`、`32×32` グリッドへ変換して使用する。

具体的な形状データの表現方法は、以下のいずれかを実装前に選択する。

- 基準解像度の行列をJSON配列で保持する
- 矩形や多角形などの図形要素をJSONで保持し、グリッドへラスタライズする

`pickaxe_handle` の理想形状と表現方式は未決定とする。

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
    "mining_speed": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    },
    "max_durability": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    }
  }
}
```

---

## 9. スキル操作支援定義

配置先は `blacksmith/skill_assists.json` とする。

スキル操作支援定義は、スキルレベルに応じて利用可能になるゲージ目盛り、削り幅、グリッド解像度を持つ。品質加点、採点基準の緩和、レシピ解放は定義しない。

鉄のピッケル試作では、グリッド解像度として `16`、`24`、`32` を使用する。各解像度を解放するスキルレベルと削り幅は未決定とする。

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

完成品レシピは、バニラの作業台から品質付き完成品を生成する独自レシピタイプとして定義する。

レシピは少なくとも以下を持つ。

- 完成品ID
- 必要な品質付きパーツ
- 品質を持たない追加材料
- 完成品品質評価関数のID
- 評価関数へ渡す重みなどのパラメータ

鉄のピッケルでは、鉄製ピッケルヘッドと `pickaxe_handle` を要求し、算術平均を行う完成品品質評価関数を使用する。

---

## 12. 評価関数の境界

評価関数は、JSON定義と加工結果を受け取り、評価値または倍率を返す副作用のない処理とする。

最低限、以下の責務を分離する。

| 評価関数 | 入力 | 出力 |
|---|---|---|
| 加熱評価 | 加熱ティック数、金属定義 | `0～100`の加熱評価 |
| 鍛造評価 | 打撃履歴、金属パーツ定義 | `0～100`の鍛造評価 |
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
- 名前空間付きIDの形式が正しい
- 必須項目が存在する
- 品質、打撃強度、重みが許容範囲内である
- 加熱評価曲線のティック値が昇順である
- `castable_after_ticks < danger_after_ticks < destroy_after_ticks` を満たす
- 適正強度の下限が上限以下である
- 破損回数が適正打撃回数より大きい
- 評価の重み合計が `1.0` である
- 品質段階が `0～100` を重複なく覆う
- グリッド解像度が実装上の上限を超えない
- 参照先の金属、パーツ、形状、評価関数が存在する

検証に失敗した場合は、ファイルパス、項目名、問題の値をログへ出力する。該当定義は登録せず、他の正常な定義の読込は継続する。

---

## 14. クライアント同期

JSONはサーバー側を正とする。クライアント描画に必要なゲージ範囲、品質段階、グリッド解像度などは、ログイン時またはデータパック再読込時にサーバーから同期する。

クライアントは表示と入力送信だけを担当し、品質評価や素材消費を確定しない。

---

## 15. 未決定事項

- 鉄製ピッケルヘッドの `cooling_ticks`
- 理想形状JSONの表現方式
- `pickaxe_handle` の理想形状
- スキルレベルと操作支援の対応形式
- 完成品レシピの具体的なJSON形式とレシピタイプID
- データパック再読込時に進行中の加工へ新旧どちらの定義を適用するか
