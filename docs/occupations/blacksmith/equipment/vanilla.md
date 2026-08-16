# バニラ装備構成仕様

## 1. 本書の目的

本書は、MVPで鍛冶製作へ対応するバニラの武器、防具、ツールについて、共通パーツ、素材量、加工方式、組み立て方を定義する。

品質、加工工程、通常レシピ品の扱いは[鍛冶師システム仕様](../blacksmith.md)、素材特性は[鍛冶素材仕様](../materials.md)、加工条件の初期値は[パーツ原型仕様](./part-archetypes.md)、最初の具体例は[鉄のピッケル試作仕様](../iron-pickaxe.md)に従う。本書では個別の加熱時間や鍛造強度を持たず、各素材・パーツJSONから参照する。

---

## 2. 対象範囲

MVPでは以下を鍛冶製作の対象とする。

- 剣、ピッケル、斧、シャベル、クワ
- ヘルメット、チェストプレート、レギンス、ブーツ
- 弓、クロスボウ、トライデント、メイス
- 盾、ハサミ、火打石と打ち金、釣竿、ブラシ
- カメの甲羅、オオカミの鎧、馬鎧

エリトラ、コンパス、時計、バケツ、リード、消耗品は鍛冶製作の対象外とする。エリトラは防具ではなく探索報酬として扱い、MVPでは品質を付与しない。

---

## 3. 素材別の加工方式

| 素材区分 | 対象 | 加工方式 |
|---|---|---|
| 木材 | 木製装備、柄、弓、盾、釣竿 | 非金属加工 |
| 石材 | 石製武器・ツール | 非金属加工 |
| 金属 | 鉄、金、銅を使用するパーツ、チェーン | 加熱、鋳造、鍛造 |
| 宝石 | ダイヤモンド製パーツ | 非金属加工 |
| 革・繊維 | 革防具、弦、ライニング | 非金属加工 |
| 特殊素材 | カメのウロコ、アルマジロのウロコ、ブリーズロッド | 非金属加工 |
| ネザライト | ダイヤモンド装備からの鍛冶台強化 | ベース装備の品質を維持 |

木、石、鉄、金、ダイヤモンドの各装備は、同じパーツ種別と完成品レシピを使い、素材定義だけを切り替える。ネザライト装備は専用パーツから新規製作せず、バニラの鍛冶台レシピでダイヤモンド装備の品質と損傷割合を引き継ぐ。

---

## 4. 武器・ツール原型

| 原型 | 主パーツ | 素材量 | 補助パーツ | 対応素材 |
|---|---|---:|---|---|
| 剣 | `sword_blade` | 2 | `sword_handle` 1個 | 木、石、鉄、金、ダイヤモンド |
| ピッケル | `pickaxe_head` | 3 | `pickaxe_handle` 1個 | 木、石、鉄、金、ダイヤモンド |
| 斧 | `axe_head` | 3 | `axe_handle` 1個 | 木、石、鉄、金、ダイヤモンド |
| シャベル | `shovel_head` | 1 | `shovel_handle` 1個 | 木、石、鉄、金、ダイヤモンド |
| クワ | `hoe_head` | 2 | `hoe_handle` 1個 | 木、石、鉄、金、ダイヤモンド |

対象完成品IDは各素材のバニラIDとする。金だけは接頭辞 `golden_`、木だけは接頭辞 `wooden_` を使用する。ネザライトの剣・ツールは対応するダイヤモンド装備から品質を引き継ぐ。

主パーツが金属の場合は `metal_parts/<material_id>/`、木、石、宝石の場合は `non_metal_parts/<material_id>/` に素材別定義を置く。補助パーツの理想形状は素材ではなく、`sword_handle`、`pickaxe_handle` などのパーツ種別で共有する。

金属パーツには[パーツ原型仕様](./part-archetypes.md)の原型を割り当て、生成した具体的な冷却時間、適正強度、適正回数、破損回数をJSONへ保存する。非金属パーツは[鍛冶素材仕様](../materials.md)の素材プロファイルを参照する。

### 4.1 対象完成品ID

| 素材 | 剣・ツール完成品ID |
|---|---|
| 木 | `wooden_sword`、`wooden_pickaxe`、`wooden_axe`、`wooden_shovel`、`wooden_hoe` |
| 石 | `stone_sword`、`stone_pickaxe`、`stone_axe`、`stone_shovel`、`stone_hoe` |
| 鉄 | `iron_sword`、`iron_pickaxe`、`iron_axe`、`iron_shovel`、`iron_hoe` |
| 金 | `golden_sword`、`golden_pickaxe`、`golden_axe`、`golden_shovel`、`golden_hoe` |
| ダイヤモンド | `diamond_sword`、`diamond_pickaxe`、`diamond_axe`、`diamond_shovel`、`diamond_hoe` |
| ネザライト | `netherite_sword`、`netherite_pickaxe`、`netherite_axe`、`netherite_shovel`、`netherite_hoe` |

表中のIDはすべて `minecraft` 名前空間とする。ネザライト以外は鍛冶用固定レシピ、ネザライトは対応するダイヤモンド装備からの品質継承経路を持つ。

---

## 5. 防具原型

| 原型 | パーツ種別 | 素材量 | 対応素材 |
|---|---|---:|---|
| ヘルメット | `helmet_body` | 5 | 革、チェーン、鉄、金、ダイヤモンド |
| チェストプレート | `chestplate_body` | 8 | 革、チェーン、鉄、金、ダイヤモンド |
| レギンス | `leggings_body` | 7 | 革、チェーン、鉄、金、ダイヤモンド |
| ブーツ | `boots_body` | 4 | 革、チェーン、鉄、金、ダイヤモンド |

革防具とダイヤモンド防具は非金属加工、鉄・金防具は金属加工を使用する。チェーン防具では、鉄塊1個から `iron_ring` 1個を作り、表の素材量と同じ個数の `iron_ring` を加工して防具本体を作る。

防具本体だけが品質付きパーツとなるため、完成品品質は防具本体品質と同じ値になる。ネザライト防具は対応するダイヤモンド防具から品質と損傷割合を引き継ぐ。

鉄製防具4部位と金製防具4部位は、対応する防具本体1個と `craftbound:protective_lining` 1個を `craftbound:quality_assembly` で縦に並べて完成させる。`protective_lining` は品質を持たない共通補助アイテムであり、革1個と `#minecraft:wool` 1個から shapeless recipe で1個作成する。品質計算には防具本体だけが寄与するため、完成品品質は防具本体品質と同じ値になる。

鉄製防具と金製防具は、いずれも `craftbound:iron/helmet_body`、`craftbound:iron/chestplate_body`、`craftbound:iron/leggings_body`、`craftbound:iron/boots_body` の共通パーツ定義を使用する。鉄と金の区別は防具本体の `material` で行い、鉄完成レシピは `craftbound:iron`、金完成レシピは `craftbound:gold` を要求する。金用の防具本体パーツ定義は別途追加しない。

### 5.1 対象完成品ID

| 素材 | ヘルメット | チェストプレート | レギンス | ブーツ |
|---|---|---|---|---|
| 革 | `leather_helmet` | `leather_chestplate` | `leather_leggings` | `leather_boots` |
| チェーン | `chainmail_helmet` | `chainmail_chestplate` | `chainmail_leggings` | `chainmail_boots` |
| 鉄 | `iron_helmet` | `iron_chestplate` | `iron_leggings` | `iron_boots` |
| 金 | `golden_helmet` | `golden_chestplate` | `golden_leggings` | `golden_boots` |
| ダイヤモンド | `diamond_helmet` | `diamond_chestplate` | `diamond_leggings` | `diamond_boots` |
| ネザライト | `netherite_helmet` | `netherite_chestplate` | `netherite_leggings` | `netherite_boots` |

表中のIDはすべて `minecraft` 名前空間とする。ネザライト以外は鍛冶用固定レシピ、ネザライトは対応するダイヤモンド防具からの品質継承経路を持つ。

---

## 6. 遠距離・特殊装備

| 完成品 | 品質付きパーツ | 品質なし追加材料 | 備考 |
|---|---|---|---|
| `minecraft:bow` | `bow_limb`（棒3）、`bow_string`（糸3） | なし | 射撃ダメージと耐久値を品質補正する |
| `minecraft:crossbow` | `crossbow_stock`（棒3）、`crossbow_string`（糸2）、`crossbow_trigger`（鉄1） | トリップワイヤーフック1 | 射撃ダメージと耐久値を品質補正する |
| `minecraft:shield` | `shield_frame`（板材6）、`shield_boss`（鉄1） | なし | 耐久値だけを品質補正する |
| `minecraft:shears` | `shears_blades`（鉄2） | なし | 耐久値と作業速度を品質補正する |
| `minecraft:flint_and_steel` | `fire_striker`（鉄1） | 火打石1 | 耐久値だけを品質補正する |
| `minecraft:fishing_rod` | `fishing_rod_body`（棒3）、`fishing_line`（糸2） | なし | 耐久値だけを品質補正する |
| `minecraft:brush` | `brush_head`（銅1） | 羽根1、棒1 | 耐久値と作業速度を品質補正する |
| `minecraft:mace` | `mace_head`（ヘビーコア1）、`mace_handle`（ブリーズロッド1） | なし | 攻撃力と耐久値を品質補正する |

トライデントを直接ドロップする抽選に成功したドラウンドは、代わりに `craftbound:trident_fragment` を3個ドロップする。`trident_fragment` 3個から `trident_head`、プリズマリンの欠片2個から `trident_handle` を加工し、`minecraft:trident` を組み立てる。元のドロップ抽選確率は変更しない。

トライデントは近接攻撃力、射撃ダメージ、耐久値へ品質補正を適用する。メイスとトライデントの特殊効果倍率は品質で変更しない。

---

## 7. 特殊防具

| 完成品 | パーツ | 素材量 | 加工方式 |
|---|---|---:|---|
| `minecraft:turtle_helmet` | `turtle_helmet_body` | カメのウロコ5 | 非金属加工 |
| `minecraft:wolf_armor` | `wolf_armor_body` | アルマジロのウロコ6 | 非金属加工 |
| `minecraft:leather_horse_armor` | `horse_armor_body` | 革7 | 非金属加工 |
| `minecraft:iron_horse_armor` | `horse_armor_body` | 鉄7 | 金属加工 |
| `minecraft:golden_horse_armor` | `horse_armor_body` | 金7 | 金属加工 |
| `minecraft:diamond_horse_armor` | `horse_armor_body` | ダイヤモンド7 | 非金属加工 |

既存の宝箱や取引から取得した馬鎧は品質データを持たないため、品質 `30` として扱う。鍛冶製作品には防御力の品質補正を適用する。

---

## 8. データ配置

完成品ごとの構成は、以下へ独自レシピとして配置する。

```text
data/craftbound/recipes/blacksmith/vanilla/<item_id>.json
```

パーツ条件は、以下へ素材とパーツ種別の組み合わせごとに配置する。

```text
data/craftbound/blacksmith/metal_parts/<material>/<part_id>.json
data/craftbound/blacksmith/non_metal_parts/<material>/<part_id>.json
```

同じ完成品について通常レシピは残し、通常レシピ品を品質 `30` とする。

組み込みのパーツJSONは[パーツ原型仕様](./part-archetypes.md)の生成規則から作成する。ゲーム実行時に原型の式から値を補完せず、生成済みの具体値を読み込む。

---

## 9. 受け入れ条件

- 対象範囲に列挙したすべての完成品へ、鍛冶用固定レシピまたは品質継承経路が存在する
- バニラレシピが存在する完成品では、素材量の合計を原則として維持する
- 同じパーツ種別の理想形状を素材ごとに重複定義しない
- 各金属パーツが原型から生成した具体的な加工条件を持つ
- 各非金属パーツが登録済みの素材プロファイルを参照する
- ネザライト強化後も品質と損傷割合が維持される
- 通常レシピ、宝箱、取引、コマンドなどから得た品質なし装備を品質 `30` として扱う
- 各完成品が指定した性能項目だけに品質補正を適用する
