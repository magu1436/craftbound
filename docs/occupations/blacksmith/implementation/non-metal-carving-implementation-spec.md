# 非金属加工・細工台 実装仕様書

# 目的

Craftbound の鍛冶師工程へ、非金属素材を手作業で削り、品質付きパーツへ加工するシステムを実装する。

対象:

- 細工台
- 細工ナイフ / 細工ノミ
- 非金属素材・パーツ・理想形状 JSON Loader
- パーツ選択 GUI / 加工 GUI
- 2次元加工グリッド
- click / hold / drag 加工
- client prediction + server authoritative 同期
- 道具耐久、破損、品質評価
- 加工成功結果の経験値付与窓口
- 中断・再開・同時操作制御
- 鉄ピッケル試作用 `pickaxe_handle`
- 関連仕様書の更新

参照:

```text
docs/occupations/blacksmith/blacksmith.md
docs/occupations/blacksmith/data-formats.md
docs/occupations/blacksmith/iron-pickaxe.md
docs/occupations/blacksmith/materials.md
docs/occupations/blacksmith/ui.md
docs/occupations/blacksmith/lifecycle.md
docs/occupations/blacksmith/test-cases.md
docs/occupations/blacksmith/equipment/stations-and-tools.md
docs/coding-protocol.md
```

現在の `develop` 実装を優先して読み、同じ責務を重複実装しない。非金属加工について既存文書と本書が競合する場合は本書を優先する。

# 既存仕様からの変更

以下を確定仕様とする。

- 表示名は「細工台」「細工ナイフ」「細工ノミ」
- 公開IDは `craftbound:carving_table`、`craftbound:carving_knife`、`craftbound:carving_chisel`
- 非金属加工専用道具は細工ナイフ / 細工ノミの2種類とする
- `craftbound:unfinished_non_metal_part` は使用しない
- 加工状態は細工台 Block Entity に保存する
- GUI close / logout / disconnect は一時停止のみ
- パーツ選択後の素材取り出し、細工台破壊は現在状態で完成扱い
- パーツ未選択なら元素材を返却
- 候補数に関係なくパーツ選択 GUI を必ず表示
- 理想形状、完成見本、品質、残存率、破損閾値、道具耐久値を加工 GUI に表示しない
- 同じ細工台の同時加工は禁止し、拒否時メッセージは不要
- 入力位置誤差は MVP では実装しない

# 確定した仕様

## 1. 状態

`CarvingTableBlockEntity` が正本を持つ。

```text
material: ItemStack                     // 設置中の素材1個
progress: Optional<CarvingProgressState>
pendingOutputs: List<ItemStack>
```

`CarvingProgressState`:

```text
dataVersion
processId                   // 加工開始時に生成する一意UUID。成功結果IDとして再利用
partDefinitionId
materialProfileId
gridSize
brushRadius
carvingGrid                 // 各セル 0.0～1.0
removedUnitsRemainder       // 耐久消費用累積値
definitionSnapshot
```

`definitionSnapshot` は開始時点の以下を固定する。

```text
outputItemId
requiredTool
removePerPass
pathInterpolation
removedUnitsPerDurability
warningRetention
breakCondition
shapeEvaluatorId
idealShape gridSize + binary mask
```

精密成形で決まる `gridSize` / `brushRadius` も開始時に固定する。`/reload` 後も進行中加工には snapshot を使用する。

operation lease、heartbeat、sequence、stroke の一時状態は永続化しない。サーバー再起動後は加工状態のみ復元する。

## 2. 素材と右クリック

メインハンドのみ処理する。

### 台が空

`NonMetalPartDefinitions` のいずれかの `ingredient` に一致する ItemStack を右クリックすると1個設置する。まだ `progress` は生成しない。

### 素材あり / パーツ未選択

空手:

```text
元素材を返却 → 台を空にする
```

道具あり:

```text
素材に一致するpartを列挙
→ held tool == material profile.tool の候補を抽出
→ 1件以上: パーツ選択GUI
→ 0件: 「この道具では加工できなさそうだ」をAction Bar表示
```

### 加工開始済み

対応道具:

```text
operation lease取得 → 既存progressで加工GUI再開
```

空手:

```text
現在gridをfinalizeして完成パーツを取り出す
```

非対応道具では加工しない。

## 3. パーツ選択 GUI

`CarvingPartSelectionMenu` / `CarvingPartSelectionScreen` を加工画面と分離する。

- 候補数1件でも必ず表示
- 「鏃」「柄」等の加工先パーツ名を選ぶ画面とする
- 完成形状や理想形状を表示しない
- MVP は出力 Item の表示名を候補として使えばよい
- 候補多数時のみページ送りを追加してよい

選択確定は server で再検証する。

```text
part + material profile + shape 解決
→ precision shaping 解決
→ definitionSnapshot作成
→ 全セル1.0のgrid作成
→ progress保存
→ operation lease取得
→ 加工GUIを開く
```

選択 GUI を閉じただけでは加工開始しない。`progress` 作成後は別パーツへ変更できない。

## 4. 加工 GUI

`CarvingMenu` / `CarvingScreen`。

概念配置:

```text
┌──────────────────────────────────┐
│ <素材名> [素材icon] → <パーツ名> │
│                                  │
│            加工領域              │
│                                  │
│                       [加工完了] │
└──────────────────────────────────┘
```

表示:

- 素材名
- 素材アイコン
- パーツ名
- 現在の削れ状態
- ブラシ影響範囲 preview
- 加工完了ボタン

非表示:

- 理想形状 / 完成見本 / 完成パーツアイコン
- 品質 / IoU / 残存率
- 破損閾値
- 道具耐久値

プレイヤーが正解形状を研究・発見するゲーム設計を維持する。

## 5. グリッド・ブラシ

```text
1.0 = 完全に残る
0.0 = 完全に削れる
```

開始時は全セル `1.0`。

精密成形は既存 `skill_assists.json` を使用する。

```text
なし: 16×16 / radius 1.5
I:    24×24 / radius 1.0
II:   32×32 / radius 0.5
```

radius `0.5` はカーソルセルのみ。

対象セル判定と `remove_per_pass` は現行 `data-formats.md` の円形ブラシを初期実装とする。ブラシ計算は独立クラスにし、将来の重み付き falloff へ交換可能にする。

## 6. click / hold / drag

### click

```text
mouseDown → 現在位置を即時1回加工
```

2tick以内に離しても1回加工される。

### hold

`settings.json.carving_packet_interval_ticks` ごとに判定する。初期値は2tick。

```text
前回位置 == 現在位置
→ 現在位置をもう1回加工
```

### drag

```text
前回位置 → 現在位置
→ supercover line
→ 通過位置へbrushを適用
```

- 連続通信区間の共有端点だけ二重処理しない
- stroke 全体でセルを永久除外しない
- 同じセルへ戻れば再び削れる
- FPS を加工量へ使用しない

## 7. 共通削りロジック

client prediction と server 正式処理は同じ `CarvingStrokeProcessor` を使う。

```text
carving/logic/
├─ CarvingGrid
├─ CarvingBrush
├─ CarvingStroke
├─ CarvingStrokeProcessor
├─ CarvingStrokeResult
└─ SupercoverLine
```

client API へ依存させない。

概念API:

```java
CarvingStrokeResult apply(
    CarvingGrid grid,
    CarvingStroke stroke,
    double brushRadius,
    double removePerPass
);
```

Result:

```text
changedCells
actualRemovedUnits
```

`actualRemovedUnits` は処理前後で実際に減ったセル値の合計。

## 8. 道具耐久

server で受理した加工ごとに:

```text
removedUnitsRemainder += actualRemovedUnits

while remainder >= removedUnitsPerDurability:
    耐久1消費要求
    remainder -= removedUnitsPerDurability
```

既に0のセルを削っても耐久を消費しない。

各耐久消費要求で既存の道具保全スキルを適用する。保全成功時も accumulator の1回分は消費済みとする。

Minecraft / Forge 標準 durability API を使用する。

各加工要求で現在のメインハンドが snapshot の required tool に一致することを server が再確認する。

道具破損:

```text
progress維持
→ operation lease解放
→ GUIを閉じる
```

交換した対応道具で再開できる。

細工ナイフ最大耐久は `iron-pickaxe.md` の128。細工ノミの値と設備・道具レシピは `materials.md` / `stations-and-tools.md` を2道具構成へ更新して使用し、本書では数値を重複定義しない。

## 9. 品質

理想形状は `blacksmith/shapes/*.json` の最大解像度バイナリマスクを使用する。

16 / 24 grid は `data-formats.md` の重なり面積加重平均で32×32へ展開してから評価する。

MVP evaluator:

```text
craftbound:iou
```

```text
intersection = Σ min(actual, ideal)
union        = Σ max(actual, ideal)
quality      = round(intersection / union × 100)
```

`union == 0` は0。

評価処理は副作用のない `CarvingShapeEvaluator` とする。

完成 ItemStack へ `QualityStateService.setQuality` で `0～100` を付与する。

## 10. 破損

品質評価から独立させる。

```java
interface CarvingBreakEvaluator
```

MVP:

```text
craftbound:remaining_ratio
```

`pickaxe_handle` は理想領域残存率 `<= 0.50` で即時破損。

```text
素材消失
outputなし
progress/material clear
lease解放
GUI close
```

判定種別と閾値は part JSON から取得し、`CarvingGameService` へ式を埋め込まない。

## 11. 警告

正解値・数値を公開しない。

通常警告 / 加工感覚は以下だけを使用する。

- 短い低音量の警告音
- 加工領域枠の短時間変化

Action Bar に危険率を表示しない。

`warning_at_or_below_retention` と既存 `processing_sense` / cooldown を使う。同操作で破損する場合は破損通知を優先する。

## 12. finalize / 加工成功経験値窓口

完成処理を `CarvingGameService.finalizeCarving` に統一する。

契機:

- GUI「加工完了」
- パーツ選択済み素材の空手取り出し
- 細工台破壊

```text
server grid固定
→ shape evaluator
→ output生成
→ QualityStateService.setQuality
→ pendingOutputsへ予約
→ 成功結果を確定
→ CarvingExperienceHook.onResult
→ material/progress clear
→ lease解放
→ inventory または world drop
```

出力予約前に入力状態を消さない。`complete` / `take` / `block break` が競合しても、成功結果の確定と Hook 呼び出しは1回だけ行う。

`progress == empty` なら元素材を返し、経験値窓口は呼ばない。

パーツ選択済みなら未加工でも finalize する。削り不足は低品質であり、失敗条件にしない。

細工台破壊では確定結果をworldへ1回だけdropする。破壊者が `ServerPlayer` の場合はそのプレイヤーを成功結果の operator とする。プレイヤーを特定できない自動・外部破壊では経験値窓口を呼ばない。

### 経験値付与窓口

`develop` の以下を実装パターンとして使用する。

```text
occupations/blacksmith/casting/CastingExperienceHook.java
occupations/blacksmith/casting/CastingExperienceResult.java
occupations/blacksmith/casting/CastingGameService.java
```

非金属加工側に次を追加する。

```java
public record CarvingExperienceResult(
    UUID resultId,
    UUID operatorId,
    int ingredientCount,
    boolean success,
    boolean permanentMaterialLoss
) {}

public final class CarvingExperienceHook {
    public static void onResult(ServerPlayer player, CarvingExperienceResult result) {
        // MVP: no-op. Experience integration is intentionally deferred.
    }
}
```

`CarvingExperienceResult` は `CastingExperienceResult` と同様に、`resultId` / `operatorId` を null 不可、`ingredientCount >= 1` として構築時検証する。

成功時だけ、出力と品質が確定した同じサーバー側排他処理内で次を呼ぶ。

```text
CarvingExperienceHook.onResult(
    operator,
    CarvingExperienceResult(
        progress.processId,
        operator.UUID,
        snapshot.ingredientCount,
        true,
        false
    )
)
```

`processId` はパーツ選択確定時に1回だけ生成して `CarvingProgressState` へ保存し、再開・サーバー再起動・`/reload` 後も変更しない。これを成功時の `resultId` として使用し、同一加工結果を識別できるようにする。

本実装では Pufferfish's Skills への実経験値加算は行わない。Hook は後続の共通鍛冶経験値実装から差し替え・接続できる境界だけを提供する。素材破損時の失敗経験値窓口は今回追加しない。

## 13. 同時操作・session

既存 `BlacksmithOperationSessionRegistry` を再利用する。同等 registry を作らない。

必要なら `forging/session` から共通 package へ移動し、forging 側 import を更新する。

```text
Aがactive
Bが同じ台を右クリック
→ 拒否、表示なし
```

GUI close / logout / death / dimension change / heartbeat timeout は lease だけ解放する。

heartbeat、disconnect grace、interaction distance は既存 `BlacksmithSettings` を使用する。

## 14. Network / prediction

結果は server authoritative。

C2S stroke:

```text
sessionId
sequence
strokeId
normalizedStart
normalizedEnd
```

座標は `[0.0, 1.0)`。

server 検証:

```text
CarvingMenu
session / activePlayer / sequence
distance
tool
coordinate
carving_max_cells_per_request
```

client は mouseDown で即時送信し、押下中は2tickごとに送る。

`CarvingClientSessionState`:

```text
authoritativeGrid
predictedGrid
pendingStrokes(sequence順)
```

client は送信時に共通 Processor を `predictedGrid` へ即適用する。

S2C は `ackSequence + authoritative changed cells` を返す。

```text
authoritativeGridへdelta適用
→ ack以下pending削除
→ predictedGrid = authoritativeGrid copy
→ 未ack strokeを再適用
```

開始・再開時は full snapshot、通常加工は delta を使用する。

品質、破損、耐久、素材消費、完成出力を client で確定しない。

## 15. 加工表示用素材ColorMap

加工 GUI の素材描画生成を `CarvingScreen` / ゲームロジックから分離する。

```text
CarvingMaterialTextureProvider
DefaultCarvingMaterialTextureProvider
CarvingMaterialColorMap
```

デフォルト:

```text
実際に置いたItemStack
→ 代表sprite
→ 16×16 RGB mapへnearest sampling
→ 不透明pixelはRGB維持
→ 透明pixelは最寄り不透明pixelのRGBで補完
→ gridへnearest sampling
→ RGB=color map / Alpha=remaining
```

最短候補が複数なら固定走査順で1つ選ぶ。複数色を平均して新色を作らない。

Item / texture 単位でcacheし、resource reloadで破棄する。毎frame生成しない。

特殊素材用に material profile schema v2 へ optional `carving_texture` を追加し、指定時は自動生成より優先する。

ColorMap は表示専用で server 判定に使用しない。

## 16. 細工台のワールド描画

`material` がある場合だけ BER で元素材 ItemStack を天面へ描画する。

加工グリッドの詳細をワールド上へ描画する必要はない。

# JSON変更

## 非金属素材 profile schema v2

```text
data/craftbound/blacksmith/non_metal_materials/wood.json
```

```json
{
  "schema_version": 2,
  "tool": { "item": "craftbound:carving_knife" },
  "remove_per_pass": 0.5,
  "path_interpolation": "supercover",
  "removed_units_per_durability": 8
}
```

追加 optional field:

```text
carving_texture: namespaced texture id
```

## 非金属 part schema v2

```text
data/craftbound/blacksmith/non_metal_parts/wood/pickaxe_handle.json
```

```json
{
  "schema_version": 2,
  "material_profile": "craftbound:wood",
  "ingredient": { "tag": "minecraft:planks" },
  "ingredient_count": 1,
  "output": "craftbound:pickaxe_handle",
  "shape": "craftbound:pickaxe_handle",
  "base_grid_size": 16,
  "warning_at_or_below_retention": 0.6,
  "break_condition": {
    "type": "craftbound:remaining_ratio",
    "threshold": 0.5
  },
  "shape_evaluator": "craftbound:iou"
}
```

旧 `destroy_at_or_below_retention` は使用しない。

## shape

```text
data/craftbound/blacksmith/shapes/pickaxe_handle.json
```

`data-formats.md` に既存の32×32マスクをそのまま使用する。再定義しない。shape schema は現行のままでよい。

理想形状は client GUI へ送らない。

# 主な新規クラス

既存 package 構成へ合わせて調整してよい。

```text
occupations/blacksmith/carving/
├─ CarvingTableBlock
├─ CarvingTableBlockEntity
├─ CarvingGameService
├─ CarvingExperienceHook
├─ CarvingExperienceResult
├─ definition/
│  ├─ NonMetalMaterialDefinition(s)
│  ├─ NonMetalPartDefinition(s)
│  ├─ CarvingShapeDefinition(s)
│  └─ CarvingDefinitionSnapshot
├─ evaluation/
│  ├─ CarvingShapeEvaluator
│  ├─ IouCarvingShapeEvaluator
│  ├─ CarvingBreakEvaluator
│  └─ RemainingRatioBreakEvaluator
├─ logic/
│  ├─ CarvingGrid
│  ├─ CarvingBrush
│  ├─ CarvingStroke
│  ├─ CarvingStrokeProcessor
│  ├─ CarvingStrokeResult
│  └─ SupercoverLine
├─ menu/
│  ├─ CarvingPartSelectionMenu
│  └─ CarvingMenu
├─ session/CarvingSessionState
└─ state/
   ├─ CarvingProgressState
   └─ CarvingProgressStateCodec
```

client:

```text
occupations/blacksmith/client/carving/
├─ CarvingPartSelectionScreen
├─ CarvingScreen
├─ CarvingClientSessionState
├─ CarvingMaterialTextureProvider
├─ DefaultCarvingMaterialTextureProvider
└─ CarvingMaterialColorMap
```

network minimum:

```text
CarvingStrokeRequestPacket
CarvingHeartbeatPacket
CarvingSessionSyncPacket
CarvingDeltaSyncPacket
CarvingFeedbackPacket
```

パーツ選択と完成は、追加データが不要なら `AbstractContainerMenu#clickMenuButton` を使い専用 C2S packet を増やさない。

# 再利用する既存実装

先に読む。

```text
common/quality/QualityStateService.java
common/network/CraftboundNetwork.java
registry/CraftboundBlocks.java
registry/CraftboundItems.java
registry/CraftboundBlockEntities.java
registry/CraftboundMenus.java
client/event/CraftboundClientEvents.java
occupations/blacksmith/data/BlacksmithSettings*.java
occupations/blacksmith/data/BlacksmithSkillAssist*.java
occupations/blacksmith/forging/session/BlacksmithOperationSessionRegistry.java
occupations/blacksmith/forging/ForgingGameService.java
occupations/blacksmith/forging/ForgingPacketHandler.java
occupations/blacksmith/casting/CastingExperienceHook.java
occupations/blacksmith/casting/CastingExperienceResult.java
occupations/blacksmith/casting/CastingGameService.java
occupations/blacksmith/forging/ForgingTableBlock*.java
occupations/blacksmith/forging/menu/ForgingMenu.java
occupations/blacksmith/client/ForgingScreen.java
```

Forge 1.20.1 の保存・Codec・同期は現行 develop の方式へ合わせる。

# データフロー

## 開始

```text
素材設置
→ 対応道具で右クリック
→ 候補GUI
→ serverでpart選択検証
→ snapshot + grid生成
→ lease取得
→ 加工GUI
```

## 加工

```text
mouseDown / 2tick
→ client prediction
→ stroke packet
→ server validation
→ 共通Processor
→ durability
→ warning / break
→ authoritative delta
→ client reconcile
```

## 完成

```text
complete / take / block break
→ server grid
→ max gridへ展開
→ evaluator
→ quality付きoutput
→ pending output予約
→ stable resultIdでCarvingExperienceHook
→ state clear
```

# 擬似コード

## stroke

```text
stroke受理:
    session / sequence / tool / 座標を検証

    stroke開始:
        現在位置を1回処理
    else if 前回 == 現在:
        現在位置を1回処理
    else:
        supercover(前回, 現在)
        共有端点だけ除外
        各位置へbrush適用

    actualRemovedを集計
    durability accumulator更新

    break evaluatorがtrue:
        素材消失
        state clear
        lease release
        failure sync
        return

    warning判定
    authoritative delta送信
```

## finalize

```text
progressなし:
    元素材返却
    return

completionを排他予約
quality = shape evaluator(server grid, snapshot shape)
output生成
QualityStateService.setQuality(output, quality)
pendingOutputsへ予約

operatorがServerPlayerなら:
    result = CarvingExperienceResult(
        progress.processId,
        operator.UUID,
        snapshot.ingredientCount,
        true,
        false
    )
    CarvingExperienceHook.onResult(operator, result)

material/progress clear
lease release
inventory or world drop
```

# 境界条件

- JSON参照切れ、未登録item/evaluatorは該当定義だけ無効化
- 不正shapeは該当shapeだけ無効化
- invalid client quality / removed amount は信用しない
- stale session / duplicate sequence / invalid distance / tool mismatch は拒否
- GUI close は finalize しない
- `progress == empty` の素材は元素材として返す
- `progress != empty` の取り出し / block break は必ず finalize
- complete と block break が競合しても出力と成功経験値Hookは1回だけ
- 成功Hookの `resultId` は加工開始時の `processId` を再利用し、finalize時に毎回生成しない
- pending output 中は新規素材投入を拒否
- hopper / Create 等へ内部 inventory を公開しない
- ピストン移動を拒否
- off-hand は状態変更しない
- creative破壊でも内容物を重複なく処理
- prediction 不一致時は server 状態を正とする
- resource reload で client ColorMap cache は破棄するが進行中 snapshot は変更しない

# 既存仕様書の変更

本実装の一部として更新する。

## `blacksmith.md`

- 非金属加工用作業台 → 細工台
- `carving_table` / 2道具IDへ更新
- `unfinished_non_metal_part` 削除
- Block Entity 保存と新 finalize lifecycle へ更新

## `data-formats.md`

- non-metal material / part schema v2
- optional `carving_texture`
- `destroy_at_or_below_retention` → `break_condition`
- 初期 break type `craftbound:remaining_ratio`
- 非金属途中 Item 状態を削除し Block Entity 保存へ変更
- stationary hold の再加工を追記
- stroke 全体の重複除外を廃止し共有端点だけ除外
- prediction / authoritative delta を追記

## `iron-pickaxe.md`

- 非金属加工成功時に結果確定者を経験値付与窓口へ渡すことを追記
- 彫刻ナイフ → 細工ナイフ
- 細工台 → パーツ選択 → 加工の流れへ更新
- take / block break でも品質確定
- unfinished item 再開を削除し Block Entity 再開へ変更
- 受け入れ条件を更新

## `lifecycle.md`

- 細工台破壊時、未選択なら元素材、選択済みなら現在gridで完成
- GUI close / logout / disconnect は lease のみ解放

## `ui.md`

- 候補数に関係なくパーツ選択GUI表示
- ヘッダ `<素材名> [icon] → <パーツ名>`
- 理想形状、完成見本、品質、残存率、閾値、耐久を非表示
- warning は控えめな音 + 一時的な枠変化

## `materials.md` / `equipment/stations-and-tools.md`

- 細工ナイフ / 細工ノミの2道具へ整理
- 古い tool ID / recipe を新構成へ整合
- 木材は `carving_knife`

## `test-cases.md`

- 本書の完了条件に対応する CARVING ケースを追加

# 実装手順

## Step 1: 定義・評価・削りロジック

読む:

```text
MetalMaterialDefinitions
MetalPartDefinitions
BlacksmithSkillAssistResolver
QualityStateService
CraftboundDataReloadEventHandler
```

実装:

1. NonMetalMaterialDefinitions
2. NonMetalPartDefinitions
3. CarvingShapeDefinitions
4. JSON検証 + reload登録
5. CarvingDefinitionSnapshot
6. CarvingGrid / Brush / Supercover / StrokeProcessor
7. 下位grid → 最大gridの面積加重展開
8. IoU evaluator
9. RemainingRatio break evaluator
10. CarvingProgressState / Codec（加工開始時に固定する `processId` を含む）
11. wood / pickaxe_handle / shape resource

単体確認:

```text
planks → pickaxe_handle候補
16/24/32 grid
click / stationary hold / drag
actualRemoved
IoU 0..100
retention <= 0.5 break
```

## Step 2: Block・道具・server処理

読む:

```text
ForgingTableBlock / BlockEntity
ForgingGameService
QualityStateService
CraftboundBlocks / Items / BlockEntities
```

実装:

1. carving_table Block / BlockEntity / BlockItem
2. carving_knife / carving_chisel
3. pickaxe_handle、最大stack 1
4. material / progress / pendingOutputs 保存復元
5. 右クリック操作
6. CarvingGameService start / stroke / finalize
7. durability accumulator + tool preservation
8. warning / break
9. QualityStateService接続
10. block破壊処理
11. pending output排他
12. 外部inventory非公開 / piston拒否
13. CarvingExperienceResult / CarvingExperienceHook
14. 成功finalizeからHookを1回だけ呼ぶ接続

`CastingExperienceHook` / `CastingExperienceResult` / `CastingGameService` と同じ責務分離にする。Hook は no-op 境界に留め、Pufferfish's Skillsへの実加算は行わない。失敗時Hookは今回追加しない。

PNGは新規生成しない。設備・道具 recipe は既存仕様書の値を更新して使用する。

## Step 3: Menu・Session・Network・prediction

読む:

```text
BlacksmithOperationSessionRegistry
ForgingMenu / ForgingPacketHandler
ForgingClientSessionState
CraftboundNetwork / CraftboundMenus
BlacksmithSettings
```

実装:

1. PartSelectionMenu / CarvingMenu
2. operation registry再利用
3. 排他、heartbeat、timeout、distance
4. CarvingSessionState
5. sessionId / sequence / strokeId
6. stroke / heartbeat packet
7. full sync / delta sync / feedback
8. CarvingClientSessionState
9. authoritative / predicted / pending queue
10. ack後のpending replay
11. pause / failure / complete のlease解放
12. network protocol更新

Step終了時に Screen の見た目が未完成でも packet 往復、prediction、補正、再開が成立すること。

## Step 4: Screen・描画・統合・ドキュメント

読む:

```text
CraftboundClientEvents
ForgingScreen
ForgingTableBlockEntityRenderer
BlacksmithSkillAssistResolver
```

実装:

1. PartSelectionScreen
2. CarvingScreen
3. header / central grid / brush preview / 完了button
4. hidden情報を表示しない
5. warning feedback
6. CarvingMaterialTextureProvider + ColorMap cache
7. nearest opaque pixel補完
8. optional carving_texture
9. 細工台 BER の素材Item描画
10. client registration / resource reload cache clear
11. GUI close / logout / tool break / resume統合
12. 関連8仕様書を本書へ整合
13. 単体・統合テスト追加

`pickaxe_handle` の正式な専用テクスチャ制作は含めない。

# 完了条件

- 任意のバニラ板材1個を細工台へ設置できる
- パーツ未選択なら元素材を回収できる
- 細工ナイフで候補数に関係なくパーツ選択GUIを開ける
- 非対応道具では指定Action Barを表示する
- `pickaxe_handle` を16/24/32 gridで開始できる
- GUIが素材名 + 素材icon → パーツ名だけを目的情報として示す
- 理想形状等の非表示情報を表示しない
- click即時、hold 2tick、drag supercoverが成立する
- client predictionとserver補正が成立する
- 実除去量だけを耐久 accumulator に加算する
- 木材8セル相当ごとに耐久1消費要求が発生する
- tool preservationが耐久要求単位で適用される
- tool破損後に状態を失わず再開できる
- IoUで0～100品質を計算できる
- `remaining_ratio <= 0.5` で即時破損する
- complete / take / block break で品質付きパーツを1回だけ生成できる
- 成功時に `CarvingExperienceResult` を構築し `CarvingExperienceHook.onResult` を1回だけ呼べる
- 成功結果の `resultId` が加工開始時の `processId` と一致し、再開後も変化しない
- `progress == empty` または操作者不明の外部処理では成功経験値Hookを呼ばない
- 未加工でもパーツ選択済みなら低品質完成品になる
- GUI close / restart 後も加工状態を復元できる
- operation leaseはrestartで復元しない
- 同一台の同時操作を拒否できる
- hopper等から内部状態を操作できない
- `/reload` 後も進行中加工のsnapshotが変わらない
- 不正JSONだけを無効化できる
- 関連仕様書が新 lifecycle / ID / schema と整合する

# 採用しなかった方針

- 加工途中 Item を返す: 状態は細工台へ保持し、取り出し・破壊時は完成扱い
- client 結果を正とする: 結果確定は server
- 同時加工: 1台1操作者
- 理想形状・完成見本表示: 発見重視の設計と競合するため非表示

# 今回の対象外

- `pickaxe_handle` の正式な専用テクスチャ制作
- `quality_assembly` / 金属工程の再実装
- 鍛冶経験値システム全体の新設
- 入力位置ランダム誤差
- 理想形状の図鑑・研究記録システム
- connectivity 等の追加 break evaluator
- 重み付きブラシ falloff の具体調整
- 素材種類による完成装備性能差
