# 品質状態 実装仕様書

# 目的

Craftbound において、完成金属パーツ、将来の完成非金属パーツ、組み立て後の完成装備などが共通して利用できる **ItemStack 単位の品質状態** を実装する。

品質値は金属固有状態へ埋め込まず、独立した状態として保持する。

```text
ItemStack
├─ 既存の固有状態
│   ├─ CraftboundMetalPart
│   ├─ 将来の非金属パーツ状態
│   └─ その他の状態
└─ CraftboundQuality
    └─ Quality = 0～100
```

本実装は「品質値を安全に保存・読込・コピーする共通基盤」だけを対象とする。

以下は今回実装しない。

- `blacksmith/quality.json` の読込
- 品質段階名の解決
- 品質による攻撃力、防御力、耐久値等の性能補正
- 品質鑑定スキルによる表示切替
- 品質データがないアイテムを既定品質 `30` として扱う解決処理
- 金床、砥石、鍛冶台、クラフトによる品質継承処理

これらは本仕様で実装する `QualityStateService` を利用する別実装とする。

## 実装開始時の読み込み範囲

Codex が本仕様を実装する際は、原則として以下だけを読む。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/finished/
    MetalPartState.java
    MetalPartStateCodec.java
    MetalPartStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/part/
    RoughMetalPartState.java
    RoughMetalPartStateCodec.java
    RoughMetalPartStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/
    MetalVisualDataCodec.java
```

上記は既存の ItemStack 状態実装パターンを確認するためだけに読む。

本仕様の実装では、既存の `MetalPartState`、`RoughMetalPartState` のデータ構造を変更しない。

以下は原則として読まない。

- 他職業のコード
- Git 履歴
- 他ブランチ
- 無関係な鍛冶設備実装
- 品質性能補正の未実装領域

必要なシンボルが見つからない場合だけ限定検索する。

# 確定した仕様

## 1. 品質状態は独立状態とする

品質値を `MetalPartState` へ追加しない。

現在の完成金属パーツ状態は維持する。

```java
public record MetalPartState(
    int version,
    ResourceLocation metalId,
    MetalVisualData visualData
) {}
```

品質は別の root NBT へ保存する。

```text
CraftboundMetalPart
CraftboundQuality
```

これにより、将来の非金属パーツや完成装備でも同じ品質状態を再利用できるようにする。

## 2. パッケージ

新規クラスは以下へ配置する。

```text
src/main/java/com/magu1436/craftbound/common/quality/
    QualityState.java
    QualityStateCodec.java
    QualityStateService.java
```

品質は鍛冶師固有の金属状態ではなく、ItemStack に付与可能な共通状態として扱うため `common` 配下とする。

## 3. QualityState

`QualityState` は以下の値だけを保持する。

```java
public record QualityState(
    int version,
    int quality
) {}
```

定数:

```java
public static final int CURRENT_VERSION = 1;
public static final int MIN_QUALITY = 0;
public static final int MAX_QUALITY = 100;
```

不変条件:

```text
version == CURRENT_VERSION
0 <= quality <= 100
```

範囲外の品質値を自動的に clamp しない。

呼び出し側の計算ミスを隠さないため、範囲外は不正状態として拒否する。

## 4. NBT 形式

root key は以下とする。

```text
CraftboundQuality
```

保存形式:

```text
CraftboundQuality: {
    Version: int,
    Quality: int
}
```

例:

```text
CraftboundQuality: {
    Version: 1,
    Quality: 73
}
```

`QualityStateCodec` は他の root NBT を変更しない。

既存の `CraftboundMetalPart`、将来追加される別状態と同じ ItemStack に共存できること。

## 5. QualityStateCodec

責務は NBT と `QualityState` の変換だけとする。

想定 API:

```java
public final class QualityStateCodec {
    public static final String ROOT_KEY = "CraftboundQuality";

    public static Optional<QualityState> read(ItemStack stack);

    public static void write(
        ItemStack stack,
        QualityState state
    );
}
```

`read`:

- ItemStack が空なら `Optional.empty()`
- root NBT が存在しなければ `Optional.empty()`
- 必須フィールドが不足していれば `Optional.empty()`
- 型が異なれば `Optional.empty()`
- 未対応 version なら `Optional.empty()`
- quality が `0～100` 外なら `Optional.empty()`
- 不正データは削除・上書きしない
- 不正理由をログへ記録する

`write`:

- 有効な `QualityState` だけを書き込む
- `ItemStack#getOrCreateTag()` の他 root を保持する
- `CraftboundQuality` root だけを更新する

## 6. QualityStateService

ゲームロジックから Codec を直接呼ばない。

品質状態への公開窓口を `QualityStateService` に集約する。

想定 API:

```java
public final class QualityStateService {

    public static Optional<QualityState> read(
        ItemStack stack
    );

    public static boolean setQuality(
        ItemStack stack,
        int quality
    );

    public static boolean copyQuality(
        ItemStack source,
        ItemStack target
    );
}
```

### `read`

`QualityStateCodec.read` を利用して状態を返す。

ここでは既定品質を補完しない。

```text
品質状態なし
    → Optional.empty()
```

とする。

### `setQuality`

以下を満たす場合だけ書き込む。

```text
stack が空ではない
0 <= quality <= 100
```

成功時は `true`、拒否時は `false` を返す。

品質値は clamp しない。

### `copyQuality`

`source` に有効な品質状態がある場合だけ、その品質値を `target` へコピーする。

```text
source に品質あり + target が非空
    → target に同じ QualityState を書込
    → true

source に品質なし
    → target を変更しない
    → false
```

この API は将来、金床、砥石、鍛冶台、派生装備変換などの品質継承処理から利用できる。

今回、それらの呼び出し側は実装しない。

## 7. 「品質なし」と「品質30」を区別する

現在の鍛冶仕様では、将来的に品質データのない対象アイテムを既定品質 `30` として扱う。

ただし、この既定値は **品質状態そのものではない**。

したがって、以下を区別する。

```text
QualityStateService.read(stack) == empty
    → 品質状態が保存されていない

将来の QualityResolver.resolve(stack)
    → quality.json.default_quality を参照して 30 と解決可能
```

`QualityStateService#read` が勝手に `30` を返してはならない。

これにより、

- 実際に品質30として採点されたアイテム
- 品質状態をまだ持たないバニラ生成品

を内部的に区別できる。

## 8. Item 種別を制限しない

`QualityStateService` 自体は、対象 Item が `MetalPartItem` かどうかを判定しない。

理由:

```text
完成金属パーツ
完成非金属パーツ
完成装備
将来の品質対象アイテム
```

で共通利用するため。

「どの Item に品質を付与してよいか」は各工程・レシピ側が判断する。

# 採用した実装方針

- 品質状態を ItemStack の独立 NBT root として保存する
- `QualityState` は `version` と `quality` だけを保持する
- 品質値の有効範囲は `0～100`
- 範囲外の値は clamp せず拒否する
- Codec はシリアライズだけを担当する
- ゲームロジックは `QualityStateService` を窓口とする
- 品質状態がない場合は `Optional.empty()` とする
- 既定品質 `30` の解決は将来の品質定義システムへ分離する
- Item 種別による制限を品質状態基盤へ持たせない
- 既存の `MetalPartState` と `RoughMetalPartState` は変更しない

# 採用しなかった方針

## `MetalPartState` に `quality` を追加する

非金属パーツと完成装備でも同じ品質を扱うため、金属状態へ品質を埋め込むと重複実装になる。

## `RoughMetalPartState` に最終品質を保存する

粗加工パーツは鍛造完了前であり、最終品質はまだ確定していないため保存しない。

## 品質データがない場合に Codec で `30` を返す

保存状態と既定値解決の責務が混ざるため採用しない。

## Capability へ品質を保存する

品質はプレイヤーやワールドではなく個々の ItemStack に付随するため不要。

## 品質値を範囲内へ自動補正する

不正な評価処理を隠すため採用しない。

# 関連する既存クラス

## `MetalPartState`

完成金属パーツ固有の `metalId` と `visualData` を保持する。

品質は追加しない。

## `MetalPartStateCodec`

`CraftboundMetalPart` root を担当する。

`CraftboundQuality` を扱わせない。

## `MetalPartStateService`

完成金属パーツ生成を担当する。

本品質状態実装では変更しない。

後続の鍛造台実装で、完成パーツ生成後に `QualityStateService#setQuality` を呼び出す。

## `RoughMetalPartState`

加熱評価、冷却結果、定義 snapshot など鍛造前情報を保持する。

本実装では変更しない。

# データフロー

品質73を完成パーツへ設定する場合:

```text
工程評価
    ↓
quality = 73
    ↓
QualityStateService.setQuality(outputStack, 73)
    ↓
QualityState(
    version = 1,
    quality = 73
)
    ↓
QualityStateCodec.write
    ↓
ItemStack NBT
    CraftboundQuality {
        Version = 1
        Quality = 73
    }
```

読込:

```text
ItemStack
    ↓
QualityStateService.read
    ↓
QualityStateCodec.read
    ↓
有効な CraftboundQuality
    → Optional<QualityState>

状態なし / 不正状態
    → Optional.empty()
```

将来の品質継承:

```text
source ItemStack
    ↓
QualityStateService.copyQuality
    ↓
target ItemStack
```

# 実装手順

## Step 1: QualityState と Codec を実装する

新規作成:

```text
common/quality/QualityState.java
common/quality/QualityStateCodec.java
```

実施:

1. `QualityState` に version と quality を定義する
2. `0～100` の不変条件を実装する
3. `CraftboundQuality` root の read/write を実装する
4. 不正データを変更せず `Optional.empty()` とログへ落とす
5. 他 root NBT を保持する

この Step では既存の金属パーツクラスを変更しない。

## Step 2: QualityStateService を実装する

新規作成:

```text
common/quality/QualityStateService.java
```

実施:

1. `read`
2. `setQuality`
3. `copyQuality`
4. 空 ItemStack と範囲外品質を拒否する
5. 既定品質30の補完を実装しない
6. Item 種別による制限を実装しない

この Step の終了時点で、任意の非空 ItemStack へ品質状態を保存・読込・コピーできればよい。

# 擬似コード

```text
品質を設定する:
    ItemStack が空なら false
    quality が 0 未満または 100 より大きければ false

    QualityState(version=1, quality=quality) を生成する
    QualityStateCodec で ItemStack へ書き込む
    true を返す
```

```text
品質を読み込む:
    ItemStack が空なら empty
    CraftboundQuality root がなければ empty
    Version と Quality の型を検証する
    QualityState を生成する
    不変条件違反ならログを出して empty
    正常なら QualityState を返す
```

```text
品質をコピーする:
    source の品質状態を読む
    状態がなければ false
    target が空なら false
    source の品質値を target へ設定する
    成否を返す
```

# 境界条件・例外

- `quality = 0` は有効
- `quality = 100` は有効
- `quality = -1` は拒否
- `quality = 101` は拒否
- ItemStack が空なら書込拒否
- `CraftboundQuality` が存在しない場合は正常な「品質状態なし」として扱う
- root はあるが型が不正な場合はログを出して `Optional.empty()`
- 未対応 version は自動変換しない
- 不正 root を読込時に削除しない
- 品質を書き込んでも `CraftboundMetalPart` 等の既存 NBT を保持する
- `copyQuality` で source に品質がない場合、target の既存品質を削除しない
- 同じ品質値を再設定しても結果は同じで、副作用を増やさない

# 完了条件

- `QualityState` が version と `0～100` の品質値を保持できる
- `CraftboundQuality` root へ品質状態を保存できる
- 保存した品質を再読込できる
- 他の ItemStack NBT を破壊しない
- 品質状態のない ItemStack を `Optional.empty()` として扱える
- 不正品質を clamp せず拒否できる
- 品質を別 ItemStack へコピーできる
- `MetalPartState` と `RoughMetalPartState` を変更していない
- `quality.json`、性能補正、品質表示へ依存していない
- 後続の鍛造台実装から `QualityStateService#setQuality` を直接利用できる

# 今回の対象外

- `blacksmith/quality.json` の Loader / Registry
- `default_quality = 30` の解決
- 品質段階名
- 品質鑑定スキル
- ツールチップ / Jade 表示
- 攻撃力、防御力、採掘速度、作業速度、耐久値等への品質補正
- 完成非金属パーツの実装
- 完成装備の組み立て
- 金床、砥石、Mending、鍛冶台による品質継承
- 通常クラフト品への既定品質付与
- 鍛造評価および金属パーツ最終品質の計算
