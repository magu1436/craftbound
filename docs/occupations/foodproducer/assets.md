# 食料生産者（FoodProducer）アセット仕様

## 1. この文書の役割

この文書は、FoodProducerで使用するBlock、Item、GUIおよびスキル表示のアセット台帳である。ゲーム内挙動は[ゲーム仕様](foodproducer.md)を正本とし、アセットの登録ID、モデル、テクスチャ名、現在の仮表示および制作状態は本書を正本とする。

モデルJSONは実装結果の確認先であり、制作対象を決める正本にはしない。本書と実装が異なる場合は、無断でどちらかへ合わせず不一致として報告する。

## 2. 状態表記

| 状態 | 意味 |
|---|---|
| 確定 | IDとMVPでのアセット構成を変更しない |
| 仮 | 機能確認用の表示。専用アセットへ置換する |
| 要決定 | 制作前にモデル構成または表示方式の判断が必要 |
| MVP後 | MVPの機能完成には要求せず、後続段階で扱う |

## 3. 命名規則

- 名前空間は`craftbound`とする。
- Itemモデルは`assets/craftbound/models/item/<id>.json`とする。
- Itemテクスチャは`assets/craftbound/textures/item/<id>.png`とする。
- Block Stateは`assets/craftbound/blockstates/<id>.json`とする。
- Blockモデルは`assets/craftbound/models/block/<id>.json`を基点とする。
- 単一のBlockモデルで面を分ける場合は`<id>_top.png`、`<id>_front.png`、`<id>_side.png`を基本とする。追加の面や部品はモデル確定時に本書へ追記する。
- BlockItemは原則としてBlockモデルを参照し、同じ外見のItem用PNGを重複作成しない。
- 既存IDの変更はセーブ互換性に影響するため、外見変更だけを理由に変更しない。

### 3.1 制作担当者への引き渡し規則

- 本書に記載した`craftbound:<id>`を登録IDの正本とする。現在のモデルJSONが参照する`minecraft:`テクスチャ名は仮表示であり、正式ファイル名の根拠にはしない。
- Itemは本書のTexture名でPNGを作り、対応する`models/item/<id>.json`の`layer0`を`craftbound:item/<id>`へ変更する。
- Blockのモデル方式はアセット実装者が決めてよい。基準名は登録IDと同じにし、複数PNGを使う場合だけ`_top`、`_side`、部品名などの接尾辞を追加する。
- Blockモデル、ItemモデルおよびBlock StateのJSON変更もアセット実装へ含める。ただし、Java、登録ID、NBT、スロット、処理時間、当たり判定およびゲーム上の効果は変更しない。
- 現在の設備Blockは向きを保存していない。正面方向が必須のモデル、開閉、稼働アニメーションまたは内容物表示を採用したい場合は、アセットだけで実装せずJava変更事項として報告する。
- PNGはMinecraft 1.20.1の通常リソースとして使用できる透過PNGとし、基準解像度は16×16とする。高解像度化はリソースパック側で扱い、本体へ混在させない。
- 同じ登録IDがNBTで材料種や品質を保持していても、MVPでは品質段階や元材料ごとのテクスチャ差分を作らない。本書で明記した完成料理の区分だけを切り替える。

### 3.2 今回の制作依頼範囲

今回依頼するのは、設備Block 7種類、地域食材11種類のItemと採集源Block、独立Item 4種類、加工素材・完成料理である。GUI背景、スキル専用アイコン、品質別差分、稼働アニメーションおよび料理ランク別差分は含めない。

| 区分 | 制作単位 |
|---|---:|
| 設備Block | 7モデル一式。PNG枚数は採用モデルによる |
| 地域食材Item | 11 PNG |
| 地域食材の採集源Block | 成熟・収穫済みの11組、22 PNG |
| 肥料・堆肥・包丁 | 4 PNG |
| 加工素材 | 10 PNG |
| 完成料理 | 保存食＋5職業、6 PNG |

設備Blockを除く固定制作数は53 PNGである。完成料理は登録Item 1種類に対して6外見を作るため、PNG数とItem ID数は一致しない。

### 3.3 ゲーム仕様上の記載箇所

今回制作する対象はすべてゲーム仕様書に存在し、実装にも登録済みである。見た目の説明は本書を正本とする。

| 対象 | ゲーム上の用途を定める箇所 | 見た目・ファイル名を定める箇所 |
|---|---|---|
| 調理台、手回しミル、乾燥器、調理鍋 | [ゲーム仕様](foodproducer.md)7.1～7.4 | 本書4章 |
| 牧畜ブロック | [ゲーム仕様](foodproducer.md)6章 | 本書4章 |
| 保存設備I・II | [ゲーム仕様](foodproducer.md)4章 | 本書4章 |
| 農業用肥料、堆肥 | [ゲーム仕様](foodproducer.md)5.5～5.7 | 本書5章 |
| 簡易調理包丁、鍛冶師製料理包丁 | [ゲーム仕様](foodproducer.md)7.2 | 本書5章 |
| 小麦粉から調理前素材セットまで | [ゲーム仕様](foodproducer.md)7.4 | 本書6章 |
| 保存食・職業料理 | [ゲーム仕様](foodproducer.md)7.9～7.10 | 本書6章 |
| 地域固有採集食材 | [ゲーム仕様](foodproducer.md)7.10.1 | 本書7章 |

したがって、制作担当者が対象の存在や名称をモデルJSONから推測する必要はない。JSONは登録IDと現在の参照を確認し、本書どおりに参照先を変更するために使用する。

## 4. 設備Block台帳

7設備の登録IDと用途は確定している。現在のモデルは機能確認用であり、正式なモデル構成と必要PNG枚数は未確定である。制作者は本表だけで立方体用PNGを先行量産せず、モデル方針の決定後に着手する。

| ID | 日本語名 | 用途・外見上必要な要素 | 現在の仮表示 | 正式アセット状態 |
|---|---|---|---|---|
| `craftbound:cooking_table` | 調理台 | 調理面、前面、側面を識別できる作業設備 | 作業台上面を全面へ使用 | 確定：モデル方式はアセット実装者へ委任 |
| `craftbound:hand_mill` | 手回しミル | 石臼とハンドルを識別できる | 丸石 | 確定：モデル方式はアセット実装者へ委任 |
| `craftbound:drying_rack` | 乾燥器 | 枠、棚または吊り下げ部を識別できる | オークの板材 | 確定：モデル方式はアセット実装者へ委任 |
| `craftbound:cooking_pot` | 調理鍋 | 鍋本体と開口部を識別できる | 鉄ブロック | 確定：モデル方式はアセット実装者へ委任 |
| `craftbound:ranch_block` | 牧畜ブロック | 餌と管理設備であることが分かる | 樹皮を剥いだオーク、干草、板材 | 確定：モデル方式はアセット実装者へ委任 |
| `craftbound:preservation_storage_1` | 保存設備I | 木製の保存設備 | 樽 | 確定：モデル方式はアセット実装者へ委任 |
| `craftbound:preservation_storage_2` | 保存設備II | Iより上位と分かる保存設備 | 銅ブロック、切り込み入りの銅 | 確定：モデル方式はアセット実装者へ委任 |

立方体ベースか専用立体モデルかはアセット実装者へ委任する。ただし、本表の識別要素、16×16基準のMinecraftらしい表現、既存の当たり判定および操作位置を維持する。向き、開閉、処理中表示など新しいゲーム状態の追加はBlock Stateや同期処理へ影響するため、アセット実装者だけでは追加しない。

### 4.1 設備別の制作指示

| ID | 基準モデル・Texture名 | 見た目の説明 | 避ける表現 |
|---|---|---|---|
| `cooking_table` | `block/cooking_table` | 木製の食品加工台。上面にまな板、作業面または包丁跡を表現し、通常の作業台と区別する | 金属加工台に見える表現、現在未実装の収納扉や向き依存の操作面 |
| `hand_mill` | `block/hand_mill` | 上下の石臼と手回し用ハンドルが分かる小型製粉設備 | 電動機械、大型Create装置、動作アニメーション前提の表現 |
| `drying_rack` | `block/drying_rack` | 木製の枠、棚、網または吊り下げ棒を持つ乾燥設備。中身が空でも用途を判別できる | 特定の肉・果物が常時入っている表現、内容物によりモデルが変わる前提 |
| `cooking_pot` | `block/cooking_pot` | 開口部、鍋本体、取っ手を持つ加熱用の鍋設備。鉄または暗色金属を基本にする | 中身や炎が常時表示される表現、向きを保存する前提 |
| `ranch_block` | `block/ranch_block` | 飼料箱、飼い葉桶または干草を組み合わせ、動物管理設備と判断できる | 動物種を固定する絵、常時満杯の餌表示、チェストと見分けにくい箱だけの表現 |
| `preservation_storage_1` | `block/preservation_storage_1` | 木製の保存箱、食品庫または通気性のある収納設備。通常チェスト・樽と区別する | 冷凍・電気設備、保存設備IIと同等に見える重装備 |
| `preservation_storage_2` | `block/preservation_storage_2` | 保存設備Iの上位品。補強金属、銅、密閉構造などで5倍保存の上位感を示す | 電力が必要に見える表示、稼働ランプやアニメーション前提の表現 |

`block/<id>`はモデルおよびTextureの基準名を表す。立体モデルで部品別PNGが必要な場合は、例として`block/hand_mill_stone.png`、`block/hand_mill_handle.png`のように基準名へ部品名を加え、作成した全ファイルをモデルJSONから参照する。

## 5. 独立Item台帳

次のID、表示名、用途および基本モデル種別は確定している。専用テクスチャは未作成で、現在はバニラアセットを仮表示している。

| ID | 日本語名 | 見た目の根拠 | モデル | 正式Texture名 | 現在の仮表示 |
|---|---|---|---|---|---|
| `craftbound:agricultural_fertilizer` | 農業用肥料 | 耕地へ散布する肥料 | generated | `item/agricultural_fertilizer.png` | 骨粉 |
| `craftbound:compost` | 堆肥 | コンポスターから得る土状の資材 | generated | `item/compost.png` | 茶色の染料 |
| `craftbound:cooking_knife` | 簡易調理包丁 | 誰でも作れる低耐久の包丁 | handheld | `item/cooking_knife.png` | 鉄の剣 |
| `craftbound:blacksmith_cooking_knife` | 鍛冶師製料理包丁 | 鍛冶師製の高耐久な包丁 | handheld | `item/blacksmith_cooking_knife.png` | 鉄の剣 |

包丁2種の形状差、配色および鍛冶師製包丁の正式素材は未確定である。性能差は[ゲーム仕様](foodproducer.md)7.2を参照する。

### 5.1 独立Itemの制作指示

- 農業用肥料は骨粉そのものではなく、耕地へ撒く混合肥料と分かる袋、粉末または粒状資材として表現する。堆肥と区別できる明るい土色・灰白色を使用してよい。
- 堆肥は湿った有機物と土を混ぜた資材として、濃い茶色を基調に葉や植物片を少量含める。茶色の染料そのものに見えない輪郭にする。
- 簡易調理包丁は鉄刃と簡素な柄を持つ家庭用包丁として、剣より短く幅広い刃にする。
- 鍛冶師製料理包丁は同じ用途を保ちつつ、整った刃、補強された柄または鍔で上位品と判別できるようにする。正式素材が未確定なので、特定の希少金属固有色へ固定しない。
- 2種類の包丁はどちらも`handheld`表示を使用する。アイコンの輪郭だけでも見分けられる差を持たせる。

## 6. 加工素材Item台帳

加工素材は入力材料の種類、品質、Diet値などをNBTへ保持する場合があるが、同じ登録IDでは共通テクスチャを使用する。元材料ごとの見た目切り替えはMVPへ含めない。

| ID | 日本語名 | 内容・形状の根拠 | 正式Texture名 | 現在の仮表示 |
|---|---|---|---|---|
| `craftbound:wheat_flour` | 小麦粉 | 粉砕した小麦 | `item/wheat_flour.png` | 砂糖 |
| `craftbound:dough` | 生地 | 小麦粉と水を混ぜた生地 | `item/dough.png` | 粘土玉 |
| `craftbound:sliced_meat` | 肉の切り身 | 切断済みの生肉 | `item/sliced_meat.png` | 生の牛肉 |
| `craftbound:ground_meat` | 挽肉 | 粉砕した肉 | `item/ground_meat.png` | 生のウサギ肉 |
| `craftbound:chopped_vegetable` | 刻み野菜 | 切断済みの野菜 | `item/chopped_vegetable.png` | ビートルート |
| `craftbound:fruit_pieces` | 果物片 | 切断済みの果物 | `item/fruit_pieces.png` | リンゴ |
| `craftbound:dried_meat` | 乾燥肉 | 乾燥処理した肉 | `item/dried_meat.png` | ステーキ |
| `craftbound:dried_vegetable` | 乾燥野菜 | 乾燥処理した野菜 | `item/dried_vegetable.png` | ベイクドポテト |
| `craftbound:dried_fruit` | 乾燥果物 | 乾燥処理した果物 | `item/dried_fruit.png` | スイートベリー |
| `craftbound:prepared_ingredient_set` | 調理前素材セット | 加熱前の材料をまとめた容器 | `item/prepared_ingredient_set.png` | ボウル |
| `craftbound:food_dish` | 完成料理 | 保存食および職業料理の共通完成品 | 下記6区分のTexture | ウサギシチュー |

### 6.1 加工素材の制作指示

| ID | 見た目の説明 | 共通化する範囲 |
|---|---|---|
| `wheat_flour` | 小麦色または薄いクリーム色の粉。砂糖より穀物らしい色と、粉袋・粉山など識別できる輪郭を持たせる | 使用した小麦の品質による差分なし |
| `dough` | 丸めた柔らかいパン生地。粘土玉より温かい小麦色にする | Diet値や品質による差分なし |
| `sliced_meat` | 調理前の肉の切り身。動物種を限定しない赤身と脂身の表現にする | 牛、豚、羊など元の肉種で共通 |
| `ground_meat` | 細かく挽いた生肉の塊。肉の切り身と輪郭・粒感を明確に変える | 元の肉種で共通 |
| `chopped_vegetable` | 複数の角切り野菜をまとめたもの。緑、橙、赤などを少量使い、ビートルート単体に見せない | ニンジン、ジャガイモ、ビートルート、カボチャで共通 |
| `fruit_pieces` | 切った果物やベリー片の盛り合わせ。リンゴ単体ではなく複数色の小片として表現する | リンゴと各ベリーで共通 |
| `dried_meat` | 水分が抜けた細長い肉、干し肉またはジャーキー。生肉より暗い赤褐色にする | 元の肉種で共通 |
| `dried_vegetable` | 薄切りまたは小片状の乾燥野菜。刻み野菜より彩度を落とす | 元の野菜種で共通 |
| `dried_fruit` | しわのある果物片または乾燥ベリー。生の果物片より濃い色にする | 元の果物種で共通 |
| `prepared_ingredient_set` | ボウル、包みまたは小皿へ複数の加工済み材料をまとめた加熱前セット。完成料理には見せない | 完成予定の料理30種類で共通 |

仕様書およびゲーム内表示の正式名称は「肉の切り身」で、登録IDは`craftbound:sliced_meat`とする。「薄切り肉」は制作依頼時の説明名としては使用できるが、別Item IDを作成しない。

### 完成料理の表示契約

保存食5種類と職業料理25種類は、料理ごとのItem IDではなく、共通の`craftbound:food_dish`とレシピIDを含むNBTで区別する。現在の`models/item/food_dish.json`は`minecraft:item/rabbit_stew`を参照しており、Craftbound専用の完成料理Textureはまだ存在しない。

正式表示では、職業料理を対象職業ごとに切り替える。同じ職業のランクI～Vは同じ外見を使用し、ランクごとの個別テクスチャは作成しない。保存食I～Vも保存食用の共通外見1種類を使用する。

必要な完成料理の外見区分は、保存食、Blacksmith、Explorer、Adventurer、Alchemist、Architectの計6種類とする。共通ItemのNBTから所属区分を判定してモデルまたはテクスチャを切り替える実装が必要であり、単純なPNG差し替えだけでは完了しない。

| 区分 | Texture名 | 見た目の説明 |
|---|---|---|
| 保存食 | `item/food_dish_preserved.png` | 包み、携帯食、乾燥素材など、日常食・遠征食と分かる保存食。ランクI～V共通 |
| Blacksmith | `item/food_dish_blacksmith.png` | 肉料理、鉄鍋料理など、炉の近くで働く職人向けの力強い食事。ランクI～V共通 |
| Explorer | `item/food_dish_explorer.png` | 旅パン、ベリー、携帯スープなど、移動と探索を連想できる食事。ランクI～V共通 |
| Adventurer | `item/food_dish_adventurer.png` | 肉串、戦士向け肉料理、英雄の食卓など、戦闘前の高栄養食。ランクI～V共通 |
| Alchemist | `item/food_dish_alchemist.png` | 香草、キノコ、カカオなどを使う調合料理。ポーション瓶そのものには見せない。ランクI～V共通 |
| Architect | `item/food_dish_architect.png` | パン、卵、野菜、肉包みなど、建築作業中の食事を連想できる職人食。ランクI～V共通 |

`models/item/food_dish.json`は共通入口として維持する。区分別モデルが必要な方式を採用する場合は`models/item/food_dish_preserved.json`など同じ接尾辞で作成する。区分を判定するコードまたはモデル切替データはFoodProducer側で別途実装する。

## 7. 地域固有採集食材台帳

各食材は同名のBlock IDとItem IDを持つ。Item用PNGと、地表に生成する採集源Block用PNGは別アセットとする。

| Tier | ID | 日本語名 | 見た目の根拠 | Item Texture |
|---:|---|---|---|---|
| I | `craftbound:wild_garlic` | 野生ニンニク | 平原に自生するニンニク・葉 | `item/wild_garlic.png` |
| I | `craftbound:forest_thyme` | 森林タイム | 森林に自生するタイム | `item/forest_thyme.png` |
| II | `craftbound:juniper_berry` | ジュニパーベリー | タイガの針葉と青紫系の実 | `item/juniper_berry.png` |
| II | `craftbound:cactus_fig` | サボテンイチジク | 乾燥地のサボテン果実 | `item/cactus_fig.png` |
| III | `craftbound:water_celery` | 水辺セロリ | 湿地の水辺に生える葉物 | `item/water_celery.png` |
| III | `craftbound:jungle_pepper` | ジャングルペッパー | ジャングルに生る香辛料の実 | `item/jungle_pepper.png` |
| IV | `craftbound:cherry_herb` | 桜香草 | 桜色を連想できる香草 | `item/cherry_herb.png` |
| IV | `craftbound:alpine_leek` | 高山ネギ | 高地に自生するネギ | `item/alpine_leek.png` |
| V | `craftbound:mushroom_truffle` | キノコトリュフ | キノコ島で採れる菌類 | `item/mushroom_truffle.png` |
| V | `craftbound:ice_crystal_berry` | 氷晶ベリー | 樹氷に対応する冷色の実 | `item/ice_crystal_berry.png` |
| V | `craftbound:badlands_saffron` | 荒野サフラン | 荒野に対応する橙・赤系の花材 | `item/badlands_saffron.png` |

Item Textureは採集して手に持つ食材を描き、採集源Blockをそのまま縮小した画像にはしない。香草は葉や小枝、果実は収穫物、ネギ・ニンニクは可食部が識別できる構図にする。

正式な採集源は11種類を個別表示し、すべて平面の`cross`モデルを使用する。各IDについて次を基本名とする。

```text
textures/block/<id>.png
textures/block/<id>_harvested.png
models/block/<id>.json
models/block/<id>_harvested.json
```

現在の実装はTier I～Vごとの成熟・収穫済みモデル、計10モデルを共有している。この共有は仮表示であり、上記の個別`cross`モデルへ移行するときに11個のBlock State参照も更新する。

### 7.1 採集源Blockの制作指示

| ID | 成熟Texture | 収穫済みTexture | 成熟時の見た目 | 収穫済みの見た目 |
|---|---|---|---|---|
| `wild_garlic` | `block/wild_garlic.png` | `block/wild_garlic_harvested.png` | 細長い緑葉と白い花または球根を連想できる野草 | 花や可食部がなくなった短い葉 |
| `forest_thyme` | `block/forest_thyme.png` | `block/forest_thyme_harvested.png` | 森床に広がる低い小葉の香草。小さな紫系の花を使用してよい | 花と密度を減らした細い茎 |
| `juniper_berry` | `block/juniper_berry.png` | `block/juniper_berry_harvested.png` | 針葉と青紫系の実を持つ低木 | 実のない針葉と枝 |
| `cactus_fig` | `block/cactus_fig.png` | `block/cactus_fig_harvested.png` | 小型のサボテン状葉と赤紫系の果実 | 果実を取り除いたサボテン状葉 |
| `water_celery` | `block/water_celery.png` | `block/water_celery_harvested.png` | 湿地に生える明るい緑の葉と細い茎 | 刈り取られた短い茎と少量の葉 |
| `jungle_pepper` | `block/jungle_pepper.png` | `block/jungle_pepper_harvested.png` | 濃い緑の葉と目立つ赤・橙系の香辛料の実 | 実のない葉と蔓または茎 |
| `cherry_herb` | `block/cherry_herb.png` | `block/cherry_herb_harvested.png` | 桜色の小花または葉先を持つ香草 | 花を失った淡い緑の葉 |
| `alpine_leek` | `block/alpine_leek.png` | `block/alpine_leek_harvested.png` | 高地に生える淡緑色の太い葉・茎。白い花を使用してよい | 短く刈り取られた葉または茎 |
| `mushroom_truffle` | `block/mushroom_truffle.png` | `block/mushroom_truffle_harvested.png` | キノコ島の菌類と暗色のトリュフを連想できる群生 | 小さな菌糸・傘だけが残った状態 |
| `ice_crystal_berry` | `block/ice_crystal_berry.png` | `block/ice_crystal_berry_harvested.png` | 霜を帯びた葉と水色・青白色の結晶状の実 | 実のない霜付きの葉または枝 |
| `badlands_saffron` | `block/badlands_saffron.png` | `block/badlands_saffron_harvested.png` | 荒野に合う橙・赤系の花と目立つ花芯 | 花または花芯を採った細い葉と茎 |

成熟と収穫済みは遠目でも区別できるよう、果実・花・可食部の有無だけでなく、色または密度にも差を付ける。収穫済みでもBlock自体は残って再生するため、枯死・消滅した植物には見せない。11種類はTierではなく食材ごとに識別できる色と輪郭を持たせる。

## 8. GUI・スキル表示

| 対象 | 現在の表示 | 状態 |
|---|---|---|
| 保存設備UI | 専用Menu・Screenを実装済み。背景はバニラの54枠チェスト画像を27枠相当として使用 | Craftbound専用GUI背景画像はMVP外 |
| 牧畜ブロックUI | 専用Menu・Screenを実装済み。背景、枠、文字をコードで描画 | 専用GUI背景画像とレイアウト本調整はMVP外 |
| 調理設備UI | 専用Menu・Screenを実装済み。4設備で共通Screenを使用し、設備別に枠と操作を切替 | 専用GUI背景画像と情報配置の本調整はMVP外 |
| スキルカテゴリ | 小麦アイコン、耕地背景 | MVPではバニラ画像を使用し、専用化はMVP外 |
| スキルノード | バニラItemと一部Craftbound Itemをアイコンに使用 | 専用スキルアイコンはMVP外 |

## 9. MVP外のアセット

次の項目は機能実装の不足ではなく、MVP後に扱うアート制作範囲である。現在のGUIとスキルツリーは専用画像なしで動作する。

1. Craftbound専用GUI背景画像。
2. 専用スキルアイコン。

設備のモデル方式、完成料理の外見区分、採集源の`cross`モデル使用は確定済みである。
