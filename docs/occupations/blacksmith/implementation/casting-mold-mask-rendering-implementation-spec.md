# 鋳型マスク描画システム 実装仕様書

## 参照文書

本仕様書は、以下の既存文書および `feat/casting-table` ブランチの実装を参照して作成する。

### プロジェクトナレッジ

- `実装仕様書.md`
- `adventurer.md`
- `explorer.md` 相当の探検家仕様書
- `architect.md`

特に以下の共通方針を引き継ぐ。

- Minecraft Forge 1.20.1 を対象とする。
- ゲーム結果を決める判定と永続状態は論理サーバーを正とし、クライアント専用処理を共通コードから分離する。
- JSONはロード時に検証し、実行中の描画ごとにファイルを読み直さない。
- 不正な定義は可能な限り該当定義だけを無効化し、原因を特定できるログを出す。
- 実装を過度に一般化せず、MVPで必要な責務を明確に分離する。

### `docs/occupations/blacksmith/` 配下

`implementation/` を除き、以下を参照する。

- `blacksmith.md`
- `iron-pickaxe.md`
- `data-formats.md`
- `test-cases.md`
- `materials.md`
- `ui.md`
- `lifecycle.md`
- `equipment/stations-and-tools.md`
- `equipment/part-archetypes.md`
- `equipment/vanilla.md`

本仕様は上記文書のゲーム仕様を変更しない。特に、鋳型は金属種類に依存せず再利用可能であること、鋳造・冷却・品質判定はサーバー側を正とすること、金属色は既存の金属表示データを使用することを維持する。

---

# 目的

現在の `CastingTableRenderer` は、鋳造中の金属面を鋳型形状に関係なく固定矩形で描画している。そのため、ピッケルヘッド用鋳型へ鉄を流した場合でも、ピッケルヘッドの凹部だけではなく鋳造台上部の広い矩形全体が金属色で描画される。

本実装では、パーツごとに16×16の意味付きマスクJSONを1つだけ用意し、そのマスクを次の2用途へ共通利用する。

1. 空の鋳型アイテム・鋳造台上の鋳型に、溝と凹部を描画する。
2. 鋳造中は同じマスクの凹部領域だけへ金属色を描画する。

これにより、以下を実現する。

- パーツごとに完成済み鋳型テクスチャと金属面テクスチャを別々に作成しない。
- 空の鋳型の凹部と、流し込まれた金属の位置がずれない。
- 金属種類ごとのテクスチャを作成しない。
- 鋳型形状と金属色を分離する。
- 新しい鋳型の追加時にRendererへパーツ固有の分岐を追加しない。

本実装は描画専用であり、鋳造可否、素材消費、加熱評価、冷却、品質、鍛造条件、出力パーツの決定には使用しない。

---

# 確定した仕様

## 1. マスクJSONの役割

鋳型ごとに1つの「鋳型描画マスクJSON」を定義する。

このJSONはCraftbound本体のクライアントがResourceManagerから読み込む実行時リソースである。既存のPython用テクスチャ設計図JSONとは異なる。

配置先は次とする。

```text
assets/<namespace>/blacksmith/casting_masks/<mask_id>.json
```

Craftbound標準鋳型の例:

```text
assets/craftbound/blacksmith/casting_masks/pickaxe_head.json
assets/craftbound/blacksmith/casting_masks/sword_blade.json
assets/craftbound/blacksmith/casting_masks/axe_head.json
```

マスクは金属種類ではなく鋳型へ紐付ける。

鉄、金、銅など異なる金属を同じ鋳型へ流した場合も、同じマスクを使用する。金属種類による違いは色だけとする。

## 2. マスクJSON形式

MVPの形式は以下とする。

```json
{
  "schema_version": 1,
  "mold": "craftbound:pickaxe_head_mold",
  "width": 16,
  "height": 16,
  "pixels": [
    "................",
    "................",
    "...RRRRRRR......",
    "..RCCCCCCCR.....",
    ".RCCCCCCCCCR....",
    "....RCCCR.......",
    ".....RCR........",
    ".....RCR........",
    ".....RCR........",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................"
  ]
}
```

使用可能な記号は以下の3種類に限定する。

| 記号 | 名称 | 意味 |
|---|---|---|
| `.` | EMPTY | マスク対象外。共通の生鋳型だけを表示する |
| `R` | RIM | 溝の縁。黒に近い暗色を重ねて深い縁を表現する |
| `C` | CAVITY | 鋳型の凹部。空状態では薄い暗色を重ね、鋳造中は金属面を描画する領域 |

`R` と `C` は色そのものではなく意味を表す。マスクJSONにRGB値は持たせない。

## 3. 16×16座標規則

- `width` と `height` はMVPでは必ず `16` とする。
- `pixels` は16行とする。
- 各行はASCII文字16文字とする。
- 配列の0行目を画像上端、15行目を画像下端とする。
- 各行の0文字目を画像左端、15文字目を画像右端とする。
- 全角文字、タブ、不可視空白を使用しない。
- `.`、`R`、`C` 以外の文字を使用しない。
- `C` は最低1セル必要とする。
- `R` は原則として `C` の外周へ1セル幅で配置する。ただし極小形状など、デザイン上不要な場合は0セルでも定義を有効とする。

## 4. 空の鋳型の描画

すべての `CastingMoldItem` は、パーツ固有の完成済み鋳型PNGを正本としない。

描画は以下の合成とする。

```text
共通の生鋳型
    ↓
R領域: 黒に近い半透明色を重ねる
    ↓
C領域: より弱い黒の半透明色を重ねる
    ↓
空の鋳型として表示
```

初期描画値の目安は以下とする。

```text
RIM     = black, alpha 0.60～0.70
CAVITY  = black, alpha 0.20～0.35
```

正確な値は一箇所へ集約し、視認性調整のため容易に変更できるようにする。マスクJSONごとに色を重複定義しない。

共通の生鋳型には既存 `blank_mold` の外観を使用する。パーツ固有の違いはマスクだけで表現する。

## 5. 鋳造台上の鋳型描画

鋳造台上に設置された鋳型も、インベントリ内の鋳型と同じマスクを使用する。

鋳造台専用の別形状データを持たない。

インベントリ表示と鋳造台表示の両方が同じ16×16正規化座標を使用するよう、低レベルのマスク描画処理を共通化する。

## 6. 金属面の描画

鋳造中は `C` セルだけを金属面として描画する。

```text
CastingProcess.visualData
    ↓
MetalRenderColorResolver.resolve(...)
    ↓
RGB取得
    ↓
現在設置中の鋳型ID
    ↓
CastingMaskRegistry
    ↓
対応マスクのC領域
    ↓
C領域だけを金属色で描画
```

`R` 領域へ金属色を描画してはならない。

金属面は鋳型面よりごく小さいオフセットだけ手前に描画し、Z-fightingを防止する。

金属面はMVPでは平面とする。鋳型の深さや金属の厚みを3D形状として生成しない。

## 7. 金属色

金属色の決定ロジックは新規実装しない。

既存の粗加工パーツと同じ `MetalRenderColorResolver` を使用する。

```java
int color = MetalRenderColorResolver.resolve(process.visualData());
```

`RoughMetalPartColorHandler` 自体は `ItemStack` と `tintIndex` に依存するため直接呼び出さない。再利用する責務は `MetalRenderColorResolver` とする。

これにより、粗加工パーツと鋳造台上の金属が同じ `MetalVisualData` から同じRGBを得る。

## 8. ゲームロジックとの分離

マスクJSONはクライアント描画専用である。

以下へ使用してはならない。

- 鋳造可能判定
- `MetalPartDefinitions.resolve(...)`
- 必要金属量
- 加熱評価
- 冷却時間
- 破損回数
- 品質評価
- 鍛造評価
- 粗加工パーツ生成
- 完成品レシピ

マスクが欠落または不正であっても、サーバー上の鋳造処理を失敗させてはならない。

## 9. 既存の非金属形状JSONとの分離

既存の `data/<namespace>/blacksmith/shapes/*.json` は、非金属加工の理想形状・品質評価に使用するゲームデータである。

今回追加する

```text
assets/<namespace>/blacksmith/casting_masks/*.json
```

は鋳型のクライアント描画専用であり、別のデータ形式とする。

同じ `pickaxe_head` という概念を扱う場合でも、両者を同一Registryや同一評価処理へ統合しない。

## 11. マスクJSON生成規則の文書化

本実装と同時に、`docs/occupations/blacksmith/data-formats.md` へ「鋳型描画マスクJSON」の章を追加し、少なくとも以下を正式仕様として記述することを必須とする。

- 配置先 `assets/<namespace>/blacksmith/casting_masks/`
- `schema_version`
- `mold`
- `width = 16`
- `height = 16`
- `pixels` の16行×16文字制約
- `.` / `R` / `C` の意味
- 行・列と画面上の上下左右の対応
- `R` を原則として `C` の外周へ配置する生成規則
- `C` が実際に金属で満たされる領域であること
- 金属種類ごとにマスクを複製しないこと
- 同じ鋳型について空状態と充填状態で別マスクを作らないこと
- 既存のPython用16×16テクスチャ設計図JSONとは別形式であること
- 読込時の検証規則
- 不正マスク時のフォールバック方針

また `docs/occupations/blacksmith/test-cases.md` にマスク読込・リロード・描画に関するテスト項目を追加する。

必要に応じて `equipment/stations-and-tools.md` に「標準鋳型は鋳型描画マスクを1つ持つ」旨を追記する。ただし、マスクをゲームロジック上の鋳造条件として扱わないことを明記する。

---

# 採用した実装方針

## 1. クライアントResource Reloadでマスクを読み込む

マスクJSONはクライアントのResource Reload時に一括読込する。

描画フレームごとにJSONを開いてはならない。

想定クラス:

```text
CastingMaskDefinition
CastingMaskReloadListener
CastingMaskRegistry
```

責務は以下とする。

### `CastingMaskDefinition`

1鋳型分の検証済みマスクを表す。

概念例:

```java
record CastingMaskDefinition(
    ResourceLocation id,
    ResourceLocation moldItemId,
    int width,
    int height,
    CastingMaskGeometry geometry
) {}
```

### `CastingMaskReloadListener`

- `assets/*/blacksmith/casting_masks/*.json` を検索する。
- JSONをパースする。
- スキーマを検証する。
- `mold` が有効な名前空間付きIDであることを確認する。
- 16×16と記号制約を検証する。
- 描画用geometryへ前処理する。
- 正常な定義だけをRegistryへ反映する。

### `CastingMaskRegistry`

描画時に鋳型IDからマスクを取得する。

```java
Optional<CastingMaskDefinition> findByMold(ResourceLocation moldItemId)
```

同じ `mold` を複数のJSONが定義した場合は順序依存で上書きせず、重複として警告し、その鋳型のマスクを無効化する。

## 2. 描画用geometryをロード時に生成する

16×16配列を毎フレーム解析しない。

ロード時に `R` と `C` を描画単位へ変換する。

MVPでは行ごとの連続セルを1つのspanへまとめればよい。

```java
record MaskSpan(
    int row,
    int startColumn,
    int endColumnExclusive
) {}
```

```java
record CastingMaskGeometry(
    List<MaskSpan> rimSpans,
    List<MaskSpan> cavitySpans
) {}
```

これにより、16×16最大256セルを個別に文字判定する処理を描画ループから除外する。

複雑な矩形結合アルゴリズムはMVPでは不要とする。

## 3. 低レベル描画処理を共通化する

次のようなクライアント専用描画ユーティリティを追加する。

```text
CastingMaskRenderer
```

責務:

- 共通の生鋳型を描画する。
- `R` spanへ溝の縁を描画する。
- `C` spanへ空の凹部を描画する。
- 指定されたRGBで `C` spanへ金属面を描画する。
- 16×16セル座標を正規化された描画座標へ変換する。

形状情報と色決定を分離する。

```text
CastingMaskDefinition
    → どこを描くか

CastingMaskRenderStyle
    → 空の鋳型をどう暗く見せるか

MetalRenderColorResolver
    → 何色の金属か
```

## 4. `CastingMoldItem` の表示を共通生鋳型 + マスクへ変更する

現在の標準鋳型は `CastingMoldItem` として登録されているため、この型へクライアント専用のカスタム描画経路を追加する。

推奨方針は `IClientItemExtensions` と専用Item Rendererを使用することとする。

想定クラス:

```text
CastingMoldItemRenderer
```

描画フロー:

```text
CastingMoldItemのItemStack
    ↓
ForgeRegistries.ITEMSからmoldItemId取得
    ↓
CastingMaskRegistry.findByMold(moldItemId)
    ↓
共通blank_mold外観を描画
    ↓
R領域を暗く描画
    ↓
C領域を薄く暗く描画
```

すべての鋳型について、パーツ固有の完成済み鋳型PNGを要求しない。

既存の `*_mold` item model JSONは、カスタムレンダラを呼び出す共通モデルへ統一する。Forge 1.20.1でカスタムItem Rendererを有効化するために `builtin/entity` 系のモデルが必要な場合は、共通親モデルを1つ作成し各鋳型から参照する。

表示コンテキストごとのTransformは専用Item Renderer側で共通化し、パーツごとに個別Transformを持たない。

## 5. `CastingTableRenderer` の固定矩形を廃止する

現在の以下の固定値による金属面描画を廃止する。

```text
SURFACE_MIN
SURFACE_MAX
SURFACE_HEIGHT
固定4頂点Quad
```

`CastingTableRenderer` は次の流れへ変更する。

```text
設置鋳型を取得
    ↓
moldItemIdを取得
    ↓
CastingMaskRegistryからmask取得
    ↓
共通の鋳型描画処理で空の鋳型を描画
    ↓
activeProcessが存在するか
    ├─ ない → 終了
    └─ ある
         ↓
       MetalRenderColorResolverで色取得
         ↓
       同じmaskのC領域だけを金属色で描画
```

鋳型と金属面は同じ `CastingMaskGeometry` を使用するため、パーツ形状と充填位置が別ファイル由来でずれる状態を作らない。

鋳造台上での描画は、現在と同様にBlockの `FACING` に追従して回転させる。

## 6. ItemColor処理の再利用範囲

既存の粗加工パーツは `RoughMetalPartColorHandler` から `MetalRenderColorResolver` を呼び出している。

今回の金属面も `MetalRenderColorResolver` を直接使用する。

以下は行わない。

- 鋳造台専用の平均色計算を新設する。
- 金属IDごとの色switchを追加する。
- `RoughMetalPartColorHandler` に鋳造台描画の責務を持たせる。
- 金属種類ごとのマスクや鋳型画像を追加する。

## 7. クライアント専用クラスの分離

マスク読込、Registry、Item Renderer、Block Entity Renderer、描画geometryはクライアント専用パッケージへ置く。

専用サーバーから `Minecraft`、`TextureAtlasSprite`、`RenderType`、Item Rendererなどのクライアントクラスをロードしない。

`CastingProcess`、`CastingTableBlockEntity`、`MetalVisualData` 等の共通状態クラスへ描画専用型を持ち込まない。

---

# 採用しなかった方針

## パーツごとに完成済み鋳型テクスチャと金属面マスクを2枚用意する

採用しない。

理由:

- パーツ追加ごとに管理対象が増える。
- 2枚の位置が人手編集でずれる可能性がある。
- 空の鋳型と金属面の形状について正本が2つになる。

## 粗加工パーツのアイテムテクスチャAlphaを鋳型形状として使用する

採用しない。

理由:

- 粗加工パーツの見栄え用シルエットと鋳型の溝表現を同一責務にしてしまう。
- アイテムテクスチャのデザイン変更が鋳型描画へ影響する。
- 鋳型には `RIM` と `CAVITY` の2種類の意味が必要で、単一Alphaだけでは表現が不足する。

## 金属面を固定矩形のまま残す

採用しない。

現在の不具合原因であるため削除する。

## 金属ごとに鋳型・金属面テクスチャを用意する

採用しない。

形状は鋳型、色は `MetalVisualData` という既存の責務分離に反する。

## マスクJSONをサーバーのゲームデータとして扱う

採用しない。

マスクは描画結果だけを変えるため、`data/<namespace>/blacksmith/` ではなく `assets/<namespace>/blacksmith/` に置く。マスク欠落によって鋳造可否や品質が変わる状態を作らない。

## 既存Pythonテクスチャ設計図をMod本体から読み込む

採用しない。

既存JSONはPNG生成用の開発ツールの入力であり、Runtime Resourceとして設計されていない。

---

# 関連する既存クラス

## `CastingTableRenderer`

現状の問題箇所。

- 鋳型ItemStackを鋳造台上へ描画している。
- `MetalRenderColorResolver` で金属色を取得している。
- `SURFACE_MIN` / `SURFACE_MAX` を使った固定矩形で金属面を描画している。

変更後は固定矩形を削除し、`CastingMaskRenderer` を使用する。

## `CastingMoldItem`

現状は機能を持たない単純な `Item`。

変更後はクライアント側で専用Rendererを使用し、共通生鋳型 + マスク表示を行う。

## `CraftboundItems`

標準鋳型を `CastingMoldItem` として登録している。

既存の登録方式を維持する。鋳型ごとのJavaサブクラスは追加しない。

## `CraftboundClientEvents`

現状は以下を登録している。

- `CastingTableRenderer`
- `RoughMetalPartColorHandler`
- `MetalPartColorHandler`
- `MetalRenderColorResolver` のリソースリロード時キャッシュクリア

ここへ `CastingMaskReloadListener` の登録を追加する。

## `MetalRenderColorResolver`

金属RGBの唯一の描画用解決窓口として再利用する。

今回の実装で色解決ロジックを追加しない。

## `RoughMetalPartColorHandler`

`MetalRenderColorResolver` を使用して粗加工パーツのItemColorを返す既存実装。

今回の設計参考とするが、鋳造台から直接呼び出さない。

## `CastingProcess`

`visualData` を通じて鋳造中の金属表示情報を保持する。

マスク情報を `CastingProcess` へ追加しない。マスクは設置鋳型IDからクライアント側で解決する。

## `CastingTableBlockEntity`

設置鋳型とactive processをクライアント描画へ同期する既存責務を維持する。

マスクJSONそのものをNBTやネットワークパケットへ同期しない。

---

# 新規クラス案

```text
occupations/blacksmith/client/casting/
├─ CastingMaskDefinition.java
├─ CastingMaskGeometry.java
├─ CastingMaskReloadListener.java
├─ CastingMaskRegistry.java
├─ CastingMaskRenderer.java
├─ CastingMaskRenderStyle.java
└─ CastingMoldItemRenderer.java
```

既存パッケージ構成へ合わせて場所は調整してよいが、以下の責務は分離する。

- JSON parsing / validation
- Registry / lookup
- geometry preprocessing
- low-level drawing
- item rendering
- casting table rendering

---

# データフロー

## Resource Reload

```text
Minecraft Resource Reload
    ↓
CastingMaskReloadListener
    ↓
assets/*/blacksmith/casting_masks/*.json を取得
    ↓
JSON parse
    ↓
schema / mold / 16×16 / symbol検証
    ↓
R・CをMaskSpanへ変換
    ↓
CastingMaskRegistryを一括置換
```

ロード途中の一時状態を公開しない。

## 空の鋳型アイテム

```text
CastingMoldItemを描画
    ↓
mold item ID取得
    ↓
CastingMaskRegistry.findByMold
    ↓
blank_mold base描画
    ↓
R spanを暗く描画
    ↓
C spanを薄く暗く描画
```

## 鋳造台・未充填

```text
CastingTableRenderer
    ↓
BlockEntityからmold取得
    ↓
mask取得
    ↓
鋳造台の向きに合わせる
    ↓
blank_mold + R + Cを描画
```

## 鋳造台・充填中

```text
CastingTableRenderer
    ↓
moldからmask取得
    ↓
blank_mold + R + Cを描画
    ↓
BlockEntityからactiveProcess取得
    ↓
process.visualData
    ↓
MetalRenderColorResolver
    ↓
metalRgb
    ↓
mask.cavitySpansだけへmetalRgbを描画
```

---

# 擬似コード

## マスク読込

```text
リソースリロード時:
    casting_masks配下のJSONをすべて取得する
    新しい一時Registryを作成する

    各JSONについて:
        schema_versionが1か確認する
        moldが名前空間付きIDか確認する
        widthとheightが16か確認する
        pixelsが16行か確認する

        各行について:
            16文字か確認する
            各文字が '.', 'R', 'C' のいずれか確認する

        Cが1セル以上存在するか確認する
        Rの連続領域をspanへ変換する
        Cの連続領域をspanへ変換する

        同じmoldの定義がすでに存在する場合:
            両定義を競合として無効化する
            警告ログを出す
            次へ進む

        一時Registryへ登録する

    読込完了後:
        既存Registryを一時Registryで一括置換する
```

## 鋳型アイテム描画

```text
CastingMoldItem描画時:
    item IDを取得する
    対応maskを取得する

    共通blank_moldを描画する

    maskが存在しない場合:
        開発ログを一度だけ出す
        共通blank_moldだけを表示する
        終了する

    R領域へ濃い黒の半透明overlayを描画する
    C領域へ薄い黒の半透明overlayを描画する
```

## 鋳造台描画

```text
CastingTableRenderer描画時:
    moldを取得する
    moldが空なら終了する

    mold IDからmaskを取得する
    共通blank_moldを描画する

    maskが存在する場合:
        R領域を描画する
        C領域を暗く描画する

    activeProcessを取得する
    activeProcessがなければ終了する

    maskが存在しない場合:
        金属面を描画しない
        固定矩形へフォールバックしない
        終了する

    rgb = MetalRenderColorResolver.resolve(activeProcess.visualData)

    C領域を鋳型面より小さいoffsetだけ上へ移動する
    rgbでC領域だけを不透明描画する
```

---

# 境界条件・例外

## 不正なJSON

以下は該当マスクを無効化する。

- JSON構文エラー
- 未対応 `schema_version`
- `mold` 欠落
- `mold` が不正なResourceLocation
- `width != 16`
- `height != 16`
- `pixels` が16行でない
- 行長が16でない
- 未定義記号を含む
- `C` が0セル
- 同じ鋳型に複数マスクが存在する

不正な1ファイルのためクライアント全体をクラッシュさせない。

ファイルID、mold ID、原因をログへ出す。

## マスク欠落

マスクがない鋳型は以下とする。

- 共通の生鋳型だけを表示する。
- 金属面は描画しない。
- 現在の固定矩形金属面へフォールバックしない。
- サーバー側の鋳造処理は通常どおり継続する。
- 同じ欠落について毎フレームログを出さず、リロード単位で一度だけ警告する。

## Resource Reload中

- 描画中に中途半端なRegistryを公開しない。
- 新定義の検証完了後にRegistry参照を一括交換する。
- リロード後の次フレームから新しいマスクを使用する。

## 専用サーバー

- マスク関連クラスを専用サーバーで初期化しない。
- 共通コードからクライアント描画クラスをstatic参照しない。
- マスクが存在しないサーバー環境でも鋳造ロジックへ影響させない。

## Z-fighting

空の凹部overlay、縁overlay、金属面を完全に同一平面へ重ねない。

描画順と微小offsetを一箇所で管理する。

例:

```text
base mold plane
cavity/rim overlay = base + small offset
metal fill          = overlay + small offset
```

offsetは視認できる厚みを表現する値ではなく、深度競合回避だけを目的とする。

## 透明描画

RIMと空CAVITYは半透明overlayを使用する。

金属充填CAVITYは原則不透明とする。

半透明用RenderTypeと不透明金属用RenderTypeを分離し、alpha値をcutout描画へ誤って渡さない。

---

# 実装ステップ

## ステップ1: マスクデータ基盤

- `CastingMaskDefinition` を追加する。
- `CastingMaskGeometry` / `MaskSpan` を追加する。
- `CastingMaskReloadListener` を追加する。
- `CastingMaskRegistry` を追加する。
- `CraftboundClientEvents` からClient Reload Listenerを登録する。
- 16×16、記号、重複mold、schemaの検証を実装する。
- `pickaxe_head` 用の最小マスクJSONをテストデータとして追加する。

## ステップ2: 鋳型アイテム描画

- 共通生鋳型を描画する低レベル処理を追加する。
- `R` と `C` のoverlay描画を実装する。
- `CastingMoldItem` に専用Item Rendererを接続する。
- `*_mold` の個別完成済みテクスチャ依存を外す。
- インベントリ、手持ち、地面、額縁相当のItemDisplayContextで表示崩れがないことを確認する。

## ステップ3: 鋳造台の形状描画

- `CastingTableRenderer` の固定矩形金属面を削除する。
- 鋳型描画へ共通 `CastingMaskRenderer` を使用する。
- active processがある場合だけC領域へ金属色を描画する。
- 金属色は `MetalRenderColorResolver` から取得する。
- block facingに合わせてマスク方向も回転する。
- 金属面と鋳型凹部の位置一致を確認する。

## ステップ4: ドキュメントと検証

- `docs/occupations/blacksmith/data-formats.md` にマスクJSON生成規則を追記する。
- `docs/occupations/blacksmith/test-cases.md` にMASK系テストを追加する。
- 必要に応じて `equipment/stations-and-tools.md` に標準鋳型とマスクの関係を追記する。
- Python用テクスチャ設計図JSONとRuntimeマスクJSONが別物であることを明記する。
- 標準鋳型追加時のチェックリストへ「マスクJSONを追加」を含める。

---

# 追加するテスト項目

`test-cases.md` へ少なくとも以下を追加する。

| ID例 | 区分 | 条件 | 期待結果 |
|---|---|---|---|
| MASK-001 | 単体 | 正常な16×16マスクを読む | mold IDに対応するgeometryが登録される |
| MASK-002 | 単体 | 15行または17行のpixels | 該当マスクだけ無効化される |
| MASK-003 | 単体 | 1行が15文字または17文字 | 該当マスクだけ無効化される |
| MASK-004 | 単体 | `X` など未定義記号を含める | 該当マスクだけ無効化される |
| MASK-005 | 単体 | CAVITYが0セル | 該当マスクだけ無効化される |
| MASK-006 | 単体 | 同じmoldを2ファイルで定義 | 順序依存で上書きせず競合として無効化する |
| MASK-007 | クライアント | Resource Reloadでmaskを変更 | 再起動なしで新形状へ更新される |
| MASK-008 | クライアント | 空のpickaxe_head_moldを表示 | RとCが同じマスク位置に表示される |
| MASK-009 | クライアント | 鉄をpickaxe_head_moldへ流す | C領域だけが鉄色になる |
| MASK-010 | クライアント | 金を同じ鋳型へ流す | MASK-009と同じ形状が金色になる |
| MASK-011 | クライアント | maskを欠落させる | クラッシュせず固定矩形も描画しない |
| MASK-012 | 統合 | mask欠落状態で鋳造処理を行う | サーバー側の素材消費・冷却・出力は通常どおり進行する |
| MASK-013 | クライアント | 鋳造台を4方向へ設置する | 鋳型と金属面が同じ向きで回転する |
| MASK-014 | 専用サーバー | client assetsなしで起動する | client class loading errorなしで起動できる |

---

# 完了条件

- `CastingTableRenderer` から固定矩形の金属面描画が削除されている。
- ピッケルヘッド用鋳型へ金属を流した場合、ピッケルヘッドの `C` 領域だけに金属が表示される。
- 空のピッケルヘッド用鋳型は、共通生鋳型に同じマスクの `R` / `C` が重なって表示される。
- 空状態の凹部と金属充填位置が同じマスクから生成され、手作業で位置を二重管理していない。
- 鉄と金で形状は同一、色だけが変化する。
- 金属色は `MetalRenderColorResolver` を再利用する。
- マスクJSONはクライアントリソースリロード時に読まれ、描画ごとにファイルI/Oを行わない。
- 不正なマスク1件でクライアント全体をクラッシュさせない。
- マスク欠落時に固定矩形描画へ戻らない。
- マスク欠落が鋳造可否、素材消費、品質、冷却、出力へ影響しない。
- 専用サーバーでクライアントクラス参照エラーが発生しない。
- `docs/occupations/blacksmith/data-formats.md` にマスクJSON生成規則が正式に追記されている。
- `docs/occupations/blacksmith/test-cases.md` にマスク関連テストが追加されている。

コード基盤の完了確認には `pickaxe_head_mold` を代表ケースとして使用する。その他の標準鋳型については同一システムで追加でき、Renderer側へパーツ固有分岐を追加する必要がないことを確認する。

---

# 今回の対象外

- 金属種類とパーツ定義の責務分離
- `MetalPartDefinition` から `metalId` を除去する変更
- 金を流せない問題そのものの修正
- 金属色解決ロジックの再設計
- 金属の温度による赤熱表現
- 冷却時間に応じた金属色の連続変化
- 金属面の3D厚み表現
- 液体が流れ込むアニメーション
- 鋳型内部を徐々に満たす充填アニメーション
- マスクを品質評価や鋳造成功判定へ利用する処理
- 非金属加工の理想形状JSONとの統合
- Python `pixel_texture_renderer.py` のMod本体への組み込み
- パーツごとの特殊Renderer実装
- 他MODが独自に追加する任意ItemをCastingMoldItemとして動的対応する汎用API
