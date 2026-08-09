# 目的

Minecraft Forge 1.20.1環境のCraftboundへ、鍛冶師システムで使用する金属溶鉱炉 `craftbound:blacksmith_furnace` を実装する。

本実装では、以下を達成する。

- 金属溶鉱炉ブロックとBlock Entityを追加し、ワールドへ設置できるようにする
- 金属溶鉱炉へ、るつぼを手動で挿入・取り出しできるようにする
- 内部のるつぼと未反映加熱時間をワールドへ永続化する
- 真下のブロックが有効な熱源であるかを判定し、判定結果をキャッシュする
- 有効な熱源が存在する間だけ、ロード済みチャンクのサーバーtickで加熱を進行する
- るつぼに保存済みの加熱時間と、溶鉱炉に蓄積中の未反映時間を合算して現在の加熱状況を判定する
- るつぼの取り出し時および溶鉱炉の破壊時に、未反映加熱時間をるつぼへ確定する
- 危険時間へ到達した場合、黒煙と不規則な金属音でプレイヤーへ警告する
- 通常破壊、クリエイティブ破壊、爆発、管理者による置換で、状態付きるつぼを重複なく出力する
- ホッパー、Create、他MODから内部るつぼを操作できないようにする
- レシピを実装しなくても、コマンドなどで取得して一連の機能を確認できる状態にする

本書は、以下の既存文書と実装を前提とする。

- `docs/occupations/blacksmith/blacksmith.md`
- `docs/occupations/blacksmith/materials.md`
- `docs/occupations/blacksmith/data-formats.md`
- `docs/occupations/blacksmith/lifecycle.md`
- `docs/occupations/blacksmith/implementation/crucible-implementation-spec.md`
- `docs/coding-protocol.md`
- `CrucibleState`
- `CrucibleStateService`
- `MetalDefinition`
- `MetalMaterialDefinitions`
- `HeatingPhase`
- `HeatingStatus`

既存文書と本書が競合する場合、金属溶鉱炉の実装については本書を優先する。

# 確定した仕様

## 対象環境

- Minecraft 1.20.1
- Minecraft Forge 1.20.1
- Java 17
- ブロックIDは `craftbound:blacksmith_furnace`
- Block Entity IDも `craftbound:blacksmith_furnace`
- GUI、Menu、専用Screenは持たない
- すべての状態変更は論理サーバーを正とする
- レシピ、進捗、サバイバルでの入手方法は今回実装しない
- `/give`、`/setblock`、クリエイティブインベントリへの既存登録方式などから利用できればよい

## ブロックの外観と向き

金属溶鉱炉は水平4方向の向きを持つ。

```java
public static final DirectionProperty FACING =
    BlockStateProperties.HORIZONTAL_FACING;
```

設置時は、正面が設置プレイヤーを向くようにする。

```text
設置時のFACING = プレイヤーの水平向きの反対
```

以下を実装する。

- デフォルト状態は `FACING = NORTH`
- `getStateForPlacement`
- `rotate`
- `mirror`
- `createBlockStateDefinition`

加熱中かどうかを表す `LIT` プロパティは追加しない。熱源状態はBlock Entity内のキャッシュで管理し、ブロックモデルの差し替えには使用しない。

ブロックはフルブロック形状とする。専用VoxelShapeは実装しない。

## 内部状態

`BlacksmithFurnaceBlockEntity` は、少なくとも以下を保持する。

```java
private ItemStack crucible = ItemStack.EMPTY;
private long pendingHeatingTicks;
private int warningSoundCooldownTicks;
private boolean hasHeatSource;
private boolean invalidStoredState;
```

各フィールドの意味は以下。

| フィールド | 永続化 | 意味 |
|---|---:|---|
| `crucible` | する | 内部に保持しているるつぼItemStack |
| `pendingHeatingTicks` | する | 今回の挿入中に進行した、るつぼへ未反映の加熱時間 |
| `warningSoundCooldownTicks` | する | 次の警告音までの、加熱進行tick基準の残り時間 |
| `hasHeatSource` | しない | 真下が有効熱源かを表す再構築可能なキャッシュ |
| `invalidStoredState` | 原則しない | 読み込んだ内部状態が操作可能かを表す実行時フラグ |

`pendingHeatingTicks` は総加熱時間ではない。

```text
現在の実効加熱時間
    = るつぼのCrucibleState.heatingTicks
    + BlacksmithFurnaceBlockEntity.pendingHeatingTicks
```

るつぼを取り出した後、または溶鉱炉が破壊された後は、未反映加熱時間をるつぼへ確定してから `pendingHeatingTicks` を `0` にする。

## 内部インベントリ

金属溶鉱炉は、るつぼ専用の内部保持領域を1つ持つ。

ただし、GUIや外部搬送に公開する一般的なインベントリではない。MVPでは `ItemStackHandler` やItemHandler Capabilityを使用せず、Block Entityが1個の `ItemStack` フィールドを直接保持する。

以下を満たすこと。

- 保持できるのは `CrucibleItem` のItemStackだけ
- 1個だけ保持する
- 空のるつぼ、金属入りるつぼ、加熱済みるつぼをすべて保持できる
- 不正なNBTを持つるつぼは新規挿入を拒否する
- 内部ItemStackのNBTを溶鉱炉側から直接変更しない
- 加熱時間の確定には `CrucibleStateService.advanceHeating` を使用する
- ItemHandler Capabilityを公開しない
- `Container`、`WorldlyContainer`、ホッパー用APIを実装しない
- Createや他MOD向けの自動挿入・自動回収APIを追加しない

## プレイヤー操作

操作対象はメインハンドだけとする。

### るつぼの挿入

以下の条件をすべて満たす場合、手に持っているるつぼを金属溶鉱炉へ挿入する。

- 論理サーバー側
- 使用した手がメインハンド
- 金属溶鉱炉の内部が空
- メインハンドのItemStackが `CrucibleItem`
- `CrucibleStateService.read` で有効な状態として読み込める

挿入時は、プレイヤーの手から実際のItemStackをBlock Entityへ移動する。クリエイティブモードでも複製せず、手から取り除く。

挿入後は以下を行う。

```text
内部るつぼ = プレイヤーが持っていたItemStack
pendingHeatingTicks = 0
warningSoundCooldownTicks = 0
invalidStoredState = false
Block Entityをdirty化
```

既にるつぼが入っている場合、別のるつぼを挿入できない。

### るつぼの取り出し

以下の条件をすべて満たす場合、内部のるつぼを取り出す。

- 論理サーバー側
- 使用した手がメインハンド
- プレイヤーのメインハンドが空
- 金属溶鉱炉にるつぼが入っている
- 保存状態が操作可能である

取り出し前に、`MeltingGameService.commitHeating` を呼び出す。

```text
commitHeatingが成功
    → pendingHeatingTicksを0へ戻す
    → 内部るつぼをプレイヤーのメインハンドへ設定
    → 内部るつぼを空にする
    → 警告用状態を初期化
    → Block Entityをdirty化
```

プレイヤーのメインハンドが空であることを条件にするため、インベントリ満杯による失敗は発生しない。

`commitHeating` が失敗した場合は取り出しを実行せず、るつぼと未反映時間をBlock Entityへ残す。失敗原因をログへ記録するが、同じ状態について毎tickログを出さない。

### その他の右クリック

以下の場合は内容を変更しない。

- オフハンドでの使用
- 内部が空だが、メインハンドがるつぼではない
- 内部にるつぼがあるが、メインハンドが空ではない
- 挿入対象るつぼの状態が不正
- Block Entityの保存状態が不正

GUIは開かない。

## 熱源

有効熱源はブロックタグで定義する。

```text
craftbound:blacksmith_heat_sources
```

MVPの初期値は以下。

- `minecraft:lava`
- `minecraft:fire`
- `minecraft:soul_fire`

熱源判定は以下とする。

```java
level.getBlockState(worldPosition.below())
    .is(CraftboundBlockTags.BLACKSMITH_HEAT_SOURCES)
```

熱源判定結果は `hasHeatSource` へキャッシュする。

### キャッシュ更新契機

以下の場合に `refreshHeatSource` を呼ぶ。

- Block Entityの `onLoad`
- 真下のブロックから `neighborChanged` を受けた場合

`BlacksmithFurnaceBlock#neighborChanged` では、`changedPos.equals(pos.below())` の場合だけBlock Entityへ更新を要求する。

`hasHeatSource` はBlockStateから再構築できるため、NBTへ保存しない。

`neighborChanged` はForge 1.20.1で非推奨警告が付く場合があるため、オーバーライドメソッドのみに `@SuppressWarnings("deprecation")` を付けてよい。手動で `neighborChanged` を呼び出さず、実処理は `refreshHeatSource` へ分離する。

MVPでは、データパックの `/reload` だけを契機とした全溶鉱炉の即時キャッシュ更新は実装しない。熱源タグ変更後は、チャンク再読み込みまたは真下ブロックの更新によって再評価される。

## 加熱進行

るつぼ自身はtick処理を持たない。

`BlacksmithFurnaceBlockEntity` のサーバーtickで、以下の条件をすべて満たす場合だけ加熱を1tick進める。

- 内部にるつぼが存在する
- Block Entityの保存状態が有効
- るつぼ状態を正常に読み込める
- るつぼが `EMPTY` ではない
- 金属IDに対応する `MetalDefinition` が存在する
- 真下に有効熱源が存在する
- `pendingHeatingTicks` が負数ではない

加熱tickでは、るつぼItemStackのNBTを毎tick変更しない。

```text
候補pendingHeatingTicks = pendingHeatingTicks + 1
候補値でMeltingGameService.evaluateを実行
評価成功
    → pendingHeatingTicksを候補値へ更新
    → Block Entityをdirty化
    → HeatingStatusを使って警告処理
評価失敗
    → 加熱を進めない
```

加算は飽和加算とし、`long` のオーバーフローで負数へ反転させない。

```text
Math.addExactで成功
    → 加算結果を使用
ArithmeticException
    → Long.MAX_VALUEを使用
```

チャンクがアンロードされている間とサーバー停止中はBlock Entityのtickが実行されないため、加熱も進行しない。実時刻やワールド時刻の差分から加熱時間を補完しない。

プレイヤーが周囲にいなくても、何らかの理由でチャンクがロードされている場合は加熱を進行する。

## 加熱状況

既存の `HeatingPhase` を以下の意味で使用する。

```java
public enum HeatingPhase {
    UNHEATED,
    TOO_EARLY,
    CASTABLE,
    OPTIMAL,
    OVERHEATED
}
```

判定に使用する時間は、実効加熱時間とする。

```text
effectiveHeatingTicks
    = crucibleState.heatingTicks()
    + pendingHeatingTicks
```

`MeltingGameService.evaluate` は状態を書き換えない参照処理とする。

判定規則は以下。

```text
るつぼがEMPTY、または実効加熱時間が0
    → UNHEATED

実効加熱時間 < MetalDefinition.castableAfterTicks
    → TOO_EARLY

実効加熱時間 >= MetalDefinition.dangerAfterTicks
    → OVERHEATED

実効加熱時間がscore_curve上のスコア100区間に含まれる
    → OPTIMAL

上記以外
    → CASTABLE
```

MVPでサポートする `heating_evaluator` は `craftbound:linear_curve` とする。

`OPTIMAL` は、以下のいずれかである場合に成立する。

- 実効加熱時間と一致するスコア点が `100`
- 実効加熱時間を挟む前後2点のスコアが両方 `100`

前後のスコアが異なる区間では線形補間後のスコアは100未満になるため、`OPTIMAL` としない。

`destroyAfterTicks` へ到達しても、溶鉱炉内で金属やるつぼを自動削除しない。`HeatingPhase` は `OVERHEATED` のままとする。全消失を含む最終結果は、鋳型へ流し込む時点で別処理が確定する。

## `HeatingStatus`

既存の `HeatingStatus` は以下へ修正する。

```java
package com.magu1436.craftbound.occupations.blacksmith.melting.state;

public record HeatingStatus(
    HeatingPhase phase,
    boolean warningRequired
) {}
```

現在の `warningRequire` は `warningRequired` へ変更する。

`HeatingPhase` も実ファイルの配置とpackage宣言を一致させる。

```java
package com.magu1436.craftbound.occupations.blacksmith.melting.state;
```

## 警告演出

警告条件は以下。

```text
effectiveHeatingTicks >= MetalDefinition.dangerAfterTicks
```

これは `HeatingStatus.warningRequired()` から取得する。

警告演出は、有効熱源があり実際に加熱が進行しているサーバーtickだけで実行する。熱源が失われた場合は警告演出を停止する。

### 黒煙

MVPでは、警告中に10tickごとに `ParticleTypes.LARGE_SMOKE` を発生させる。

発生位置の基準はブロック上面中央付近とする。

```text
x = blockX + 0.5
y = blockY + 1.05
z = blockZ + 0.5
```

少量のランダムな水平拡散を加えてよい。

サーバー側から `ServerLevel#sendParticles` を使用し、専用パケットは追加しない。

### 金属音

警告中は、40〜100加熱tickのランダム間隔で金属音を再生する。

MVPでは以下を使用する。

```java
SoundEvents.ANVIL_HIT
SoundSource.BLOCKS
```

音量とピッチは以下を基準とし、わずかにランダム化してよい。

```text
volume = 0.25〜0.4
pitch = 0.75〜1.15
```

`warningSoundCooldownTicks` は警告中かつ加熱中だけ減算する。`0` 以下になった場合に音を鳴らし、40〜100の乱数へ再設定する。

警告状態から外れた場合は `warningSoundCooldownTicks` を `0` に戻す。

この値を永続化することで、チャンクアンロード中とサーバー停止中に警告間隔が進行しないようにする。

専用の音声ファイルや `sounds.json` は今回追加しない。

## 加熱時間の確定

`MeltingGameService.commitHeating` は、溶鉱炉に蓄積された未反映加熱時間をるつぼへ反映する。

```text
pendingHeatingTicks < 0
    → 失敗

pendingHeatingTicks == 0
    → 成功。状態変更なし

pendingHeatingTicks > 0
    → CrucibleStateService.advanceHeatingを呼ぶ
```

`CrucibleStateService.advanceHeating` が成功した場合だけ、呼び出し元が `pendingHeatingTicks` を `0` へ戻す。

加熱時間を確定するタイミングは以下。

- プレイヤーがるつぼを取り出す直前
- 溶鉱炉が破壊・置換され、内部るつぼをドロップする直前

チャンク保存、チャンクアンロード、サーバー停止だけを理由に確定しない。Block EntityのNBTへ、内部るつぼと `pendingHeatingTicks` を別々に保存する。

## 破壊と移動

金属溶鉱炉はピストンで移動できない。

```java
@Override
public PushReaction getPistonPushReaction(BlockState state) {
    return PushReaction.BLOCK;
}
```

通常破壊、クリエイティブ破壊、爆発、`/setblock` などによる別ブロックへの置換は、`BlacksmithFurnaceBlock#onRemove` の共通経路で処理する。

以下の場合だけ内容物をドロップする。

```text
state.getBlock() != newState.getBlock()
```

同じブロックのBlockState変更ではドロップしない。

破壊時処理は以下。

```text
Block Entityを取得
    → 未反映加熱時間をるつぼへ確定
    → 内部るつぼをItemEntityとして1回だけドロップ
    → Block Entityの内部るつぼを空にする
    → pendingHeatingTicksを0へ戻す
    → warningSoundCooldownTicksを0へ戻す
    → super.onRemoveを呼ぶ
```

内容物のドロップ処理を `playerWillDestroy`、`getDrops`、`onRemove` の複数箇所へ重複実装しない。内部るつぼのドロップは `onRemove` から呼ばれる単一メソッドへ集約する。

溶鉱炉ブロック本体のドロップ可否は通常のルートテーブルに従う。クリエイティブ破壊ではブロック本体が落ちなくても、内部るつぼは必ずドロップする。

`commitHeating` が異常終了した場合は、元のるつぼNBTを上書きせず、そのまま1回だけドロップしてエラーを記録する。異常時に未反映加熱時間を失う可能性があるため、位置、るつぼID、未反映時間、原因をログへ含める。

## 永続化

`BlacksmithFurnaceBlockEntity#saveAdditional` と `load` で以下を保存・復元する。

想定するNBT形式は以下。

```text
{
  Crucible: {
    // ItemStack#saveが生成する内容
  },
  PendingHeatingTicks: 120L,
  WarningSoundCooldownTicks: 46
}
```

NBTキーはBlock Entityクラス内の定数へ集約する。

```java
private static final String TAG_CRUCIBLE = "Crucible";
private static final String TAG_PENDING_HEATING_TICKS =
    "PendingHeatingTicks";
private static final String TAG_WARNING_SOUND_COOLDOWN_TICKS =
    "WarningSoundCooldownTicks";
```

保存規則は以下。

- 内部るつぼが空なら `Crucible` を保存しない
- `pendingHeatingTicks` は0の場合も保存してよい
- `warningSoundCooldownTicks` は0の場合も保存してよい
- `hasHeatSource` は保存しない
- `invalidStoredState` は保存しない
- 読み込み後に `onLoad` で熱源キャッシュを再構築する
- 読み込んだ `pendingHeatingTicks` が負数の場合は値を保持したまま操作不能にし、暗黙に0へ修正しない
- 内部ItemStackがるつぼではない場合は値を保持したまま操作不能にし、暗黙に削除しない
- 不正状態について同じBlock Entityからログを大量出力しない

内部るつぼや未反映時間が変化した場合は `setChanged()` を呼ぶ。警告演出だけをクライアントへ同期する専用Block Entityパケットは不要。

## 外部自動化

以下を実装しない。

- `ForgeCapabilities.ITEM_HANDLER` の公開
- `IItemHandler` の公開
- `Container` / `WorldlyContainer`
- ホッパーによる挿入・回収
- Createによる挿入・回収
- 他MOD向けの自動化API
- レッドストーン信号による自動取り出し
- コンパレーター出力

# 採用した実装方針

## 実装ステップ1：ブロックとBlock Entityの追加

### 目的

無機能な金属溶鉱炉を登録し、正しいモデルと向きで設置・破壊できる状態にする。

### 新規クラス

#### `BlacksmithFurnaceBlock`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/furnace/
    BlacksmithFurnaceBlock.java
```

継承元：

```java
BaseEntityBlock
```

責務：

- 水平向きのBlockState管理
- Block Entityの生成
- サーバーtickerの提供
- 右クリック処理の入口
- 真下の変更に対する熱源キャッシュ更新通知
- Block Entity破壊時の共通内容物ドロップ
- ピストン移動の禁止

ステップ1では、右クリック、熱源、内容物ドロップの実処理は空実装または後続ステップ用のメソッド境界だけ用意してよい。

#### `BlacksmithFurnaceBlockEntity`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/furnace/
    BlacksmithFurnaceBlockEntity.java
```

責務：

- Block Entity型として生成できること
- 後続ステップで使用する内部状態の保持
- `saveAdditional` と `load`
- `onLoad`
- サーバーtickの入口

ステップ1では、tickは何もしなくてよい。

### 変更する登録クラス

既存方式を確認し、以下へ追加する。

```text
CraftboundBlocks
CraftboundItems
CraftboundBlockEntities
```

想定する登録名：

```java
CraftboundBlocks.BLACKSMITH_FURNACE
CraftboundItems.BLACKSMITH_FURNACE
CraftboundBlockEntities.BLACKSMITH_FURNACE
```

BlockItemは既存のブロックアイテム登録方式へ合わせる。

### ブロックプロパティ

以下に相当するプロパティを使用する。

```java
BlockBehaviour.Properties.of()
    .strength(3.5F, 6.0F)
    .sound(SoundType.METAL)
    .requiresCorrectToolForDrops()
```

バニラ溶鉱炉のプロパティをそのまま `copy` すると、未使用の `LIT` 状態を前提とした設定が混ざる可能性があるため、必要な値を明示する。

### ステップ1の完了条件

- `craftbound:blacksmith_furnace` が登録される
- `/give` でBlockItemを取得できる
- ワールドへ設置できる
- 設置方向に応じて正面が回転する
- 破壊時にブロック本体が通常ルートテーブルに従って処理される
- Block Entityが生成される
- 保存・再読み込みでクラッシュしない
- ピストンで移動できない
- 専用サーバーでクライアントクラス参照エラーが発生しない

## 実装ステップ2：るつぼの挿入・取り出し・破壊時出力

### 目的

金属溶鉱炉が、るつぼを1個だけ安全に保持し、プレイヤー操作と設備破壊で状態付きるつぼを失わず移動できるようにする。

### `BlacksmithFurnaceBlockEntity`へ追加する責務

- `ItemStack crucible`
- `long pendingHeatingTicks`
- `int warningSoundCooldownTicks`
- 状態の保存と復元
- るつぼ挿入可否の判定
- るつぼ挿入処理
- るつぼ取り出し処理
- 破壊時のるつぼドロップ処理
- 状態初期化処理

想定する公開またはpackage-private API：

```java
public boolean hasCrucible();

public ItemStack getCrucible();

public boolean tryInsertCrucible(
    ServerPlayer player,
    InteractionHand hand
);

public boolean tryExtractCrucible(
    ServerPlayer player
);

public void dropStoredCrucible(ServerLevel level);

private boolean commitPendingHeating();

private void clearStoredCrucible();
```

実際の引数構成は既存規約に合わせてよいが、状態変更処理をBlockクラスへ分散させない。

### `BlacksmithFurnaceBlock#use`

サーバー側でBlock Entityへ操作を委譲する。

処理順序：

```text
オフハンド
    → PASS

Block Entityが取得できない
    → PASS

内部が空 かつ メインハンドがるつぼ
    → 挿入を試行

内部にるつぼあり かつ メインハンドが空
    → 取り出しを試行

それ以外
    → PASS
```

クライアント側では、サーバーで成功する可能性がある操作について `InteractionResult.sidedSuccess` 相当を返してよい。状態変更は行わない。

### 破壊時処理

`BlacksmithFurnaceBlock#onRemove` から、Block Entityの `dropStoredCrucible` を1回だけ呼ぶ。

`dropStoredCrucible` 内で、以下を同じ処理単位として行う。

```text
内部るつぼがなければ終了
未反映加熱時間を確定
るつぼをワールドへドロップ
内部状態を空へ戻す
```

破壊時のBlockEntity内容物をルートテーブルへ含めない。

### ステップ2の完了条件

- 空の溶鉱炉へるつぼを挿入できる
- 挿入したItemStackがプレイヤーの手から消え、Block Entityへ移る
- 内部にるつぼがある場合、別のるつぼを挿入できない
- メインハンドが空の場合だけるつぼを取り出せる
- 取り出したるつぼがメインハンドへ戻る
- 不正状態のるつぼを挿入できない
- チャンク再読み込み後も内部るつぼが維持される
- 通常破壊で内部るつぼが1回だけドロップする
- 爆発で内部るつぼが1回だけドロップする
- クリエイティブ破壊でも内部るつぼがドロップする
- `/setblock` による置換でも内部るつぼがドロップする
- 同じブロックの向き変更では内部るつぼをドロップしない
- ホッパーや他MODから内部るつぼへアクセスできない

## 実装ステップ3：熱源判定・加熱・警告

### 目的

有効熱源がある場合だけ加熱時間を進行し、実効加熱時間から加熱状況と警告を判定できるようにする。

### 新規クラス

#### `MeltingGameService`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/melting/
    MeltingGameService.java
```

MVPでは、状態を持たない `final` クラスとして実装してよい。

想定API：

```java
public final class MeltingGameService {

    public static Optional<HeatingStatus> evaluate(
        ItemStack crucible,
        long pendingHeatingTicks
    );

    public static boolean commitHeating(
        ItemStack crucible,
        long pendingHeatingTicks
    );
}
```

責務：

- `CrucibleStateService.read` による状態取得
- `MetalMaterialDefinitions.INSTANCE.get` による金属定義取得
- 実効加熱時間の飽和加算
- `HeatingPhase` の導出
- 警告要否の導出
- 未反映加熱時間の確定
- るつぼNBTを直接操作しない

`evaluate` は副作用を持たない。

`commitHeating` は `CrucibleStateService.advanceHeating` だけを通じて状態を変更する。

### 既存クラスの修正

#### `HeatingPhase`

ファイル配置とpackageを一致させる。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/melting/state/
    HeatingPhase.java
```

```java
package com.magu1436.craftbound.occupations.blacksmith.melting.state;
```

#### `HeatingStatus`

ファイル配置とpackageを一致させ、フィールド名を修正する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/melting/state/
    HeatingStatus.java
```

```java
package com.magu1436.craftbound.occupations.blacksmith.melting.state;

public record HeatingStatus(
    HeatingPhase phase,
    boolean warningRequired
) {}
```

### `BlacksmithFurnaceBlockEntity`へ追加する責務

- `boolean hasHeatSource`
- `refreshHeatSource`
- サーバーtick処理
- 候補未反映時間の計算
- `MeltingGameService.evaluate` の呼び出し
- `HeatingStatus` に基づく警告演出
- `warningSoundCooldownTicks` の更新
- 加熱中のdirty化

想定API：

```java
public void refreshHeatSource();

public static void serverTick(
    Level level,
    BlockPos pos,
    BlockState state,
    BlacksmithFurnaceBlockEntity furnace
);
```

### ステップ3の完了条件

- 真下が溶岩、炎、魂の炎の場合に熱源ありと判定する
- 真下がその他のブロックの場合に加熱しない
- 真下のブロック変更でキャッシュが更新される
- チャンク読み込み時にキャッシュが再構築される
- 熱源あり、金属入りるつぼありの場合だけ `pendingHeatingTicks` が増える
- 空のるつぼは加熱しない
- チャンクアンロード中とサーバー停止中に加熱が進まない
- 保存・再読み込み後も未反映加熱時間が維持される
- 取り出し時に未反映加熱時間がるつぼへ反映される
- 破壊時に未反映加熱時間がるつぼへ反映される
- るつぼの加熱時間と未反映時間の合計から `HeatingPhase` を判定する
- 危険時間へ到達すると黒煙が発生する
- 危険時間へ到達すると不規則な金属音が鳴る
- 熱源を失うと加熱と警告が停止する
- `destroyAfterTicks` 到達時に溶鉱炉内で内容物を削除しない
- 未登録の金属定義や未対応評価方式でクラッシュしない
- 状態が不正な場合に暗黙初期化しない
- `gradlew build` が成功する

# JSON・リソース仕様

## 1. ブロックステート

配置先：

```text
src/main/resources/assets/craftbound/blockstates/
    blacksmith_furnace.json
```

内容：

```json
{
  "variants": {
    "facing=north": {
      "model": "craftbound:block/blacksmith_furnace"
    },
    "facing=east": {
      "model": "craftbound:block/blacksmith_furnace",
      "y": 90,
      "uvlock": true
    },
    "facing=south": {
      "model": "craftbound:block/blacksmith_furnace",
      "y": 180,
      "uvlock": true
    },
    "facing=west": {
      "model": "craftbound:block/blacksmith_furnace",
      "y": 270,
      "uvlock": true
    }
  }
}
```

## 2. ブロックモデル

配置先：

```text
src/main/resources/assets/craftbound/models/block/
    blacksmith_furnace.json
```

内容：

```json
{
  "parent": "minecraft:block/orientable",
  "textures": {
    "top": "craftbound:block/blacksmith_furnace_top",
    "front": "craftbound:block/blacksmith_furnace_front",
    "side": "craftbound:block/blacksmith_furnace_side"
  }
}
```

`minecraft:block/orientable` では底面にも `side` テクスチャが使用される。底面専用テクスチャは不要。

## 3. アイテムモデル

配置先：

```text
src/main/resources/assets/craftbound/models/item/
    blacksmith_furnace.json
```

内容：

```json
{
  "parent": "craftbound:block/blacksmith_furnace"
}
```

## 4. ブロックルートテーブル

配置先：

```text
src/main/resources/data/craftbound/loot_tables/blocks/
    blacksmith_furnace.json
```

内容：

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "craftbound:blacksmith_furnace"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ]
    }
  ]
}
```

このルートテーブルは溶鉱炉ブロック本体だけを扱う。内部るつぼはBlock Entityの `onRemove` 経路で別途ドロップする。

## 5. 熱源ブロックタグ

配置先：

```text
src/main/resources/data/craftbound/tags/blocks/
    blacksmith_heat_sources.json
```

内容：

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

Java側では、既存の `CraftboundBlockTags` に以下に相当するキーを追加する。

```java
public static final TagKey<Block> BLACKSMITH_HEAT_SOURCES =
    BlockTags.create(
        new ResourceLocation(
            Craftbound.MODID,
            "blacksmith_heat_sources"
        )
    );
```

実際の `ResourceLocation` 生成方法は既存コードの方式へ合わせる。

## 6. 採掘道具タグ

配置先：

```text
src/main/resources/data/minecraft/tags/blocks/mineable/
    pickaxe.json
```

既存ファイルがある場合は値を追記する。新規作成する場合の内容：

```json
{
  "replace": false,
  "values": [
    "craftbound:blacksmith_furnace"
  ]
}
```

必要採掘レベルは今回指定しない。`needs_stone_tool`、`needs_iron_tool` などへは追加しない。

## 7. 翻訳

`ja_jp.json` へ追加：

```json
{
  "block.craftbound.blacksmith_furnace": "金属溶鉱炉"
}
```

`en_us.json` へ追加：

```json
{
  "block.craftbound.blacksmith_furnace": "Blacksmith Furnace"
}
```

既存JSONへ追加する場合、ファイル全体を上記内容で置換しない。

# 必要なテクスチャ

以下の3枚をユーザー側で用意する。

```text
src/main/resources/assets/craftbound/textures/block/
├── blacksmith_furnace_front.png
├── blacksmith_furnace_side.png
└── blacksmith_furnace_top.png
```

仕様：

- 各画像は `16x16` ピクセル
- PNG形式
- ブロックテクスチャとして使用可能な正方形
- `front` は炉口や扉など、正面と判断できる意匠を持たせる
- `side` は左右側面と底面へ共通使用する
- `top` は上面へ使用する
- 透明部分は必須ではない
- 加熱中・非加熱中の差分テクスチャは不要
- 警告表現はパーティクルと音で行うため、警告用テクスチャは不要
- BlockItem専用テクスチャは不要。ブロックモデルをそのまま使用する
- GUIテクスチャは不要
- パーティクル用テクスチャは不要
- 専用サウンドファイルは不要

将来、底面を側面と分けたい場合は、`minecraft:block/orientable` ではなく独自モデルJSONへ変更し、`blacksmith_furnace_bottom.png` を追加する。MVPでは行わない。

# 採用しなかった方針

## るつぼの加熱時間を毎tick直接更新する

毎tickItemStack NBTを書き換える必要が生じる。溶鉱炉側で `pendingHeatingTicks` を整数加算し、取り出し・破壊時だけ確定する方が更新範囲を限定できるため採用しない。

## 挿入時刻とワールド時刻の差分から加熱時間を求める

チャンクアンロード中やサーバー停止中にもワールド時間が進み、実際に処理されたtickだけを数える仕様と一致しないため採用しない。

## 毎tick真下のBlockStateを取得する

溶鉱炉の数が少ないため性能上は成立するが、真下の変更時とロード時にキャッシュを更新すれば十分であるため採用しない。

## `hasHeatSource` をNBTへ保存する

真下のBlockStateから再構築でき、保存値と実際のワールド状態が不一致になる可能性があるため採用しない。

## `ItemStackHandler` を使用する

外部Capability公開を誤って許可しやすく、1個のるつぼを内部保持するだけの仕様には過剰であるため採用しない。

## GUIを追加する

操作は右クリックによるるつぼの挿入・取り出しだけで成立するため採用しない。

## `LIT` BlockStateを追加する

加熱中モデルを用意しないうえ、熱源状態をBlock EntityキャッシュとBlockStateで二重管理することになるため採用しない。

## `destroyAfterTicks` 到達時に金属を即時削除する

加熱結果と全消失は鋳型へ流し込む時点で確定する方針に反するため採用しない。

## 警告済みbooleanだけを保持する

警告音は一度だけではなく不規則に繰り返す必要があり、booleanでは再生間隔を表現できないため採用しない。

## 独自パーティクル、独自サウンドを追加する

MVPではバニラの黒煙パーティクルと金属音で要件を満たせるため採用しない。

## 破壊処理を複数イベントへ分散する

通常破壊、爆発、クリエイティブ破壊などで重複ドロップが発生しやすくなるため、`onRemove` の単一経路へ集約する。

# 関連する既存クラス

## `CrucibleState`

利用内容：

- `metalId()`
- `heatingTicks()`
- `processState()`
- `CrucibleProcessState.EMPTY`

変更しない。

## `CrucibleStateService`

利用内容：

```java
CrucibleStateService.read(ItemStack)
CrucibleStateService.advanceHeating(ItemStack, long)
```

変更しない。

`MeltingGameService` とBlock Entityは、るつぼNBTを直接変更しない。

## `MetalDefinition`

利用内容：

```java
castableAfterTicks()
scoreCurve()
dangerAfterTicks()
destroyAfterTicks()
heatingEvaluator()
```

変更しない。

## `MetalMaterialDefinitions`

利用内容：

```java
MetalMaterialDefinitions.INSTANCE.get(metalId)
```

変更しない。

既存のデータリロード登録を利用する。

## `HeatingPhase`

packageを実ファイル位置へ合わせる。列挙値は変更しない。

## `HeatingStatus`

packageを実ファイル位置へ合わせ、`warningRequire` を `warningRequired` へ変更する。

## `CraftboundBlocks`

金属溶鉱炉ブロックを既存方式で登録する。

## `CraftboundItems`

金属溶鉱炉のBlockItemを既存方式で登録する。

## `CraftboundBlockEntities`

金属溶鉱炉のBlock Entity型を既存方式で登録する。

## `CraftboundBlockTags`

熱源タグキーを追加する。

## `Craftbound`

`CraftboundBlocks`、`CraftboundItems`、`CraftboundBlockEntities` が既にイベントバスへ登録されているため、原則として変更しない。

## `CraftboundClientEvents`

金属溶鉱炉はScreenを持たないため変更しない。

# データフロー

## るつぼを挿入する

```text
プレイヤーがるつぼをメインハンドに持って右クリック
    ↓
BlacksmithFurnaceBlock#use
    ↓
サーバー側Block Entityを取得
    ↓
内部が空であることを確認
    ↓
CrucibleStateService.readでるつぼ状態を検証
    ↓
プレイヤーの手からItemStackをBlock Entityへ移動
    ↓
pendingHeatingTicksと警告状態を初期化
    ↓
Block Entityをdirty化
```

## 加熱する

```text
BlacksmithFurnaceBlockEntityのサーバーtick
    ↓
内部るつぼ、保存状態、熱源を検証
    ↓
pendingHeatingTicks + 1の候補値を飽和加算
    ↓
MeltingGameService.evaluate(
    crucible,
    candidatePendingHeatingTicks
)
    ↓
CrucibleState.heatingTicksと候補値を合算
    ↓
MetalMaterialDefinitionsからMetalDefinitionを取得
    ↓
HeatingPhaseとwarningRequiredを計算
    ↓
評価成功時だけpendingHeatingTicksを更新
    ↓
警告中なら黒煙と金属音を処理
    ↓
Block Entityをdirty化
```

## るつぼを取り出す

```text
プレイヤーが空のメインハンドで右クリック
    ↓
BlacksmithFurnaceBlock#use
    ↓
BlacksmithFurnaceBlockEntity.tryExtractCrucible
    ↓
MeltingGameService.commitHeating
    ↓
CrucibleStateService.advanceHeating
    ↓
成功時だけpendingHeatingTicksを0へ戻す
    ↓
内部るつぼをプレイヤーのメインハンドへ移動
    ↓
Block Entityの内部状態を空へ戻す
```

## 溶鉱炉を破壊する

```text
BlacksmithFurnaceBlock#onRemove
    ↓
別ブロックへの置換であることを確認
    ↓
BlacksmithFurnaceBlockEntity.dropStoredCrucible
    ↓
未反映加熱時間を確定
    ↓
状態付きるつぼを1回だけドロップ
    ↓
Block Entityの内部状態を空へ戻す
    ↓
super.onRemove
    ↓
ルートテーブルが溶鉱炉ブロック本体を処理
```

## 熱源を更新する

```text
真下のブロックが変化
    ↓
BlacksmithFurnaceBlock#neighborChanged
    ↓
changedPos == furnacePos.belowを確認
    ↓
BlacksmithFurnaceBlockEntity.refreshHeatSource
    ↓
真下のBlockStateが熱源タグに含まれるか確認
    ↓
hasHeatSourceキャッシュを更新
```

# 擬似コード

```text
ブロック設置時:
    設置プレイヤーの水平向きを取得する
    反対方向をFACINGへ設定する
    Block Entity生成後のonLoadで熱源を再評価する
```

```text
右クリック時:
    オフハンドならPASSを返す
    Block Entityを取得できなければPASSを返す

    クライアント側:
        挿入または取り出し候補ならSUCCESS相当を返す
        それ以外はPASSを返す

    サーバー側:
        内部が空かつメインハンドがるつぼなら挿入を試す
        内部にるつぼがありメインハンドが空なら取り出しを試す
        それ以外はPASSを返す
```

```text
るつぼ挿入時:
    Block Entity状態が不正なら失敗する
    内部にるつぼがあるなら失敗する
    手持ちItemStackがるつぼでなければ失敗する
    CrucibleStateService.readで状態を読めなければ失敗する

    手持ちItemStackをBlock Entityへ移す
    プレイヤーのメインハンドを空にする
    pendingHeatingTicksを0にする
    warningSoundCooldownTicksを0にする
    dirty化する
    成功を返す
```

```text
サーバーtick:
    内部るつぼがなければ終了する
    Block Entity状態が不正なら終了する
    hasHeatSourceがfalseなら終了する

    るつぼ状態を読み込む
    読み込めなければ操作不能として終了する
    EMPTYなら終了する

    pendingHeatingTicksへ1を飽和加算した候補値を作る
    候補値をMeltingGameService.evaluateへ渡す
    評価できなければ終了する

    pendingHeatingTicksを候補値へ更新する
    警告処理を実行する
    dirty化する
```

```text
MeltingGameService.evaluate:
    pendingHeatingTicksが負数ならemptyを返す
    CrucibleStateService.readで状態を取得する
    状態を取得できなければemptyを返す

    EMPTYならHeatingStatus(UNHEATED, false)を返す

    MetalMaterialDefinitions.INSTANCE.getで金属定義を取得する
    定義がなければemptyを返す
    heatingEvaluatorがcraftbound:linear_curveでなければemptyを返す

    heatingTicksとpendingHeatingTicksを飽和加算する
    HeatingPhaseを判定する
    effectiveHeatingTicks >= dangerAfterTicksを警告条件とする
    HeatingStatusを返す
```

```text
HeatingPhase判定:
    effectiveHeatingTicksが0ならUNHEATED
    castableAfterTicks未満ならTOO_EARLY
    dangerAfterTicks以上ならOVERHEATED
    scoreCurve上のスコア100区間ならOPTIMAL
    それ以外ならCASTABLE
```

```text
警告処理:
    warningRequiredがfalse:
        warningSoundCooldownTicksを0にする
        終了する

    10tickごと:
        ブロック上部へLARGE_SMOKEを送信する

    warningSoundCooldownTicksが0以下:
        ANVIL_HITを再生する
        warningSoundCooldownTicksを40から100の乱数へ設定する
    それ以外:
        warningSoundCooldownTicksを1減らす
```

```text
加熱時間確定:
    pendingHeatingTicksが負数なら失敗する
    pendingHeatingTicksが0なら成功する
    CrucibleStateService.advanceHeatingを呼ぶ
    成功時だけpendingHeatingTicksを0へ戻す
```

```text
るつぼ取り出し時:
    メインハンドが空でなければ失敗する
    内部るつぼがなければ失敗する
    未反映加熱時間を確定できなければ失敗する

    内部るつぼをプレイヤーのメインハンドへ設定する
    Block Entityの内部るつぼを空にする
    pendingHeatingTicksを0にする
    warningSoundCooldownTicksを0にする
    dirty化する
    成功を返す
```

```text
Block Entity保存時:
    内部るつぼがある場合はItemStackをCompoundTagへ保存する
    pendingHeatingTicksを保存する
    warningSoundCooldownTicksを保存する
    hasHeatSourceは保存しない
```

```text
Block Entity読込時:
    ItemStackを復元する
    pendingHeatingTicksを復元する
    warningSoundCooldownTicksを復元する

    内部ItemStackが空でもるつぼでもない場合:
        値を削除せず操作不能にする

    pendingHeatingTicksが負数の場合:
        値を0へ直さず操作不能にする

    onLoadで熱源を再評価する
```

```text
ブロック破壊時:
    同じブロックへの状態変更なら何もしない
    サーバー側Block Entityを取得する
    内部るつぼがあれば未反映時間を確定する
    状態付きるつぼを1回だけドロップする
    Block Entityの内容を空にする
    super.onRemoveを呼ぶ
```

# 境界条件・例外

## 空のるつぼ

空のるつぼは挿入できる。ただし、熱源があっても加熱時間を進めない。

## 不正なるつぼNBT

`CrucibleStateService.read` が失敗するるつぼは挿入しない。

保存後に不正となった内部るつぼは、自動的に空へ変換したりNBTを削除したりしない。操作を停止し、ログへ記録する。

## 未登録金属

るつぼに金属IDが存在しても、`MetalMaterialDefinitions.INSTANCE.get` で定義を取得できない場合は加熱を進めない。状態を削除しない。

## 未対応評価方式

`heating_evaluator` が `craftbound:linear_curve` 以外の場合、MVPでは加熱状況を評価不能として加熱を進めない。毎tick同じ警告ログを出さない。

## `pendingHeatingTicks` のオーバーフロー

`long` の上限へ飽和させる。負数へ反転させない。

## 熱源の消失

真下の熱源が消えた時点で、次のサーバーtickから加熱を停止する。既に蓄積した `pendingHeatingTicks` は維持する。

## 熱源の再設置

真下へ有効熱源を戻すと、保存済みの `pendingHeatingTicks` から加熱を再開する。

## チャンクアンロード

Block Entityのtickが停止するため加熱を進めない。再ロード後に内部るつぼ、未反映時間、警告音の残り時間を復元する。

## 同一tickの取り出しと破壊

サーバー処理順で最初に成功した処理だけが内部るつぼを取得する。取り出し成功時にBlock Entityを空へ戻すため、その後の破壊処理ではドロップしない。

## クリエイティブモード

挿入時はサバイバルと同様に手持ちのるつぼをBlock Entityへ移す。複製しない。

破壊時は溶鉱炉本体が落ちなくても、内部るつぼは落とす。

## 爆発

溶鉱炉本体は通常の爆発ルートテーブルに従う。内部るつぼは `onRemove` の共通処理で状態を保持して1回だけドロップする。

## `/setblock`

別ブロックへの置換は破壊と同じ中断処理を使用する。内部るつぼをドロップする。

## BlockStateの回転

同一ブロックの `FACING` 変更では内部るつぼをドロップしない。

## クライアント改変

クライアントから以下を受け取らない。

- るつぼの金属ID
- 加熱時間
- 未反映加熱時間
- HeatingPhase
- 警告状態
- 熱源判定結果

サーバー側のItemStack、Block Entity、ワールド状態だけから判定する。

# 完了条件

- 3つの実装ステップが順番に実行可能な粒度で分離されている
- `craftbound:blacksmith_furnace` のBlock、BlockItem、Block Entityが登録される
- 指定したblockstate、block model、item model、loot table、block tagが追加される
- ユーザーが用意する3枚の16x16テクスチャでモデルが表示される
- 正面が設置方向に応じて回転する
- るつぼを手動で挿入・取り出しできる
- 内部るつぼがワールド保存後も維持される
- 外部アイテム搬送から操作できない
- 熱源判定を真下の変更時とロード時にキャッシュする
- ロード済みサーバーtickだけで加熱が進行する
- るつぼNBTを毎tick書き換えない
- `pendingHeatingTicks` を永続化する
- 取り出し・破壊時に未反映時間を確定する
- `MeltingGameService.evaluate` が副作用なく `HeatingStatus` を返す
- `HeatingStatus.warningRequired` が危険時間からtrueになる
- 警告中に黒煙と不規則な金属音が発生する
- `destroyAfterTicks` 到達時に内容物を自動削除しない
- 通常破壊、爆発、クリエイティブ破壊、管理者置換で内部るつぼが重複しない
- ピストンで移動できない
- 不正データを黙って初期化しない
- 専用サーバーで起動できる
- `gradlew build` が成功する

# 今回の対象外

- 金属溶鉱炉のクラフトレシピ
- 進捗、ルートチェスト、村人取引などの入手経路
- 金属溶鉱炉専用GUI
- 内部るつぼの3D描画
- 加熱中の発光モデル
- `LIT` BlockState
- 加熱中・非加熱中の差分テクスチャ
- 底面専用テクスチャ
- 独自パーティクル
- 独自サウンド
- `sounds.json`
- ホッパー、Create、他MODによる自動化
- レッドストーン制御
- コンパレーター出力
- Jade、WTHITなどへの表示連携
- データパックリロード直後の全設置済み溶鉱炉キャッシュ一括更新
- 鋳型へ流し込む処理
- 鋳造品質の最終評価
- 金属塊化と全消失の確定
- 鋳造台、鍛造台、非金属加工台
- 鍛冶経験値
- 合金
