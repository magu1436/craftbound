# 目的

Minecraft Forge 1.20.1環境のCraftboundへ、鍛冶師システムで使用するアイテム `craftbound:crucible`（るつぼ）を実装する。

本実装では、以下を達成する。

- 金属素材をるつぼへ投入し、金属の種類、量、加熱経過時間、加工状態をItemStackへ保存する
- 手持ちのるつぼから専用GUIを開き、プレイヤーが手動で素材を投入できるようにする
- 通常クリックでは素材を1個、シフトクリックでは容量内に収まる最大個数を投入する
- 一度投入した素材は個別アイテムとして取り出せないようにする
- GUIから内容物を完全廃棄し、空のるつぼとして再利用できるようにする
- 溶鉱炉と鋳造処理へ、るつぼの状態を安全に変更するための専用APIを提供する
- Menu、Screen、GameManagerからItemStackのNBTを直接変更させない
- クライアントから送信された金属ID、素材量、加熱時間、加工状態を信用せず、サーバー側で検証・確定する

本書は、以下の文書を前提とする。

- `docs/occupations/blacksmith/blacksmith.md`
- `docs/occupations/blacksmith/materials.md`
- `docs/occupations/blacksmith/data-formats.md`
- `docs/occupations/blacksmith/ui.md`
- `docs/occupations/blacksmith/lifecycle.md`
- `docs/coding-protocol.md`

上記文書と本書で記述が競合する場合、今回確定した以下の変更を優先する。

- 投入済み素材は、未加熱の場合も含めて個別回収できない
- GUIに内容物の廃棄機能を追加する
- 加熱工程の品質評価はるつぼへ保存せず、鋳型へ流し込む時点で確定する

# 確定した仕様

## 対象環境

- Minecraft 1.20.1
- Minecraft Forge 1.20.1
- Java 17
- アイテムIDは `craftbound:crucible` とする
- るつぼは個別の加工状態を持つため、最大スタック数は `1` とする
- ゲーム結果とItemStack更新は論理サーバーを正とする

## 保持する状態

るつぼのItemStackは、少なくとも以下の状態を保持する。

- 保存データ形式のバージョン
- 投入された金属のリソースID
- 投入された金属の量
- 加熱経過時間（ゲームtick）
- 現在の加工状態

加工状態は以下の3種類とする。

```java
public enum CrucibleProcessState {
    EMPTY,
    UNHEATED,
    HEATED
}
```

各状態の意味は以下のとおり。

| 状態 | 意味 |
|---|---|
| `EMPTY` | 金属が入っていない |
| `UNHEATED` | 金属が入っているが、一度も加熱されていない |
| `HEATED` | 1tick以上の加熱処理を受けたことがある |

`HEATED` は、現在鋳造可能な温度であることを表さない。一度でも加熱処理を受けた履歴を表す。

鋳造不能、鋳造可能、適正時間内、過熱などの加熱段階は保存しない。金属定義と加熱経過時間から、必要な処理側で導出する。

加熱工程の品質評価はるつぼへ保存しない。鋳型へ流し込む時点で評価し、鋳造中の状態または生成される粗加工パーツへ保存する。

## 状態の不変条件

以下の条件を常に満たすこと。

### `EMPTY`

- 金属IDは未設定
- 投入量は `0`
- 加熱経過時間は `0`

### `UNHEATED`

- 金属IDが設定されている
- 投入量が `0` より大きい
- 加熱経過時間は `0`

### `HEATED`

- 金属IDが設定されている
- 投入量が `0` より大きい
- 加熱経過時間が `0` より大きい

投入量が `0` になった場合は、金属ID、投入量、加熱経過時間、加工状態をまとめて初期化し、必ず `EMPTY` へ戻す。

`hasEverBeenHeated` のような同義のbooleanは別途保存しない。加熱履歴は `CrucibleProcessState.HEATED` から判定する。

## 金属量

- 最大容量は16インゴット相当とする
- 金属量は浮動小数点数ではなく、金属素材定義が使用する整数の素材単位で保持する
- 1インゴット相当の単位数と、各金属塊の換算量は `materials.md` および `data-formats.md` の金属素材定義を正とする
- るつぼのクラスへ、鉄、金、銅などの個別IDや換算量をハードコードしない
- 同じ金属であれば、インゴットと再利用可能な金属塊を混在して投入できる
- 異なる金属、未登録素材、容量を超える投入は拒否する
- MVPでは合金を扱わない

## 素材投入

- 投入済み素材は、未加熱か加熱済みかにかかわらず個別アイテムとして取り出せない
- 投入元アイテムの内訳と投入履歴は保存しない
- 投入時にItemStackを金属IDと素材量へ変換する
- 空のるつぼへ最初の素材を投入した場合、加工状態を `UNHEATED` にする
- `UNHEATED` または `HEATED` のるつぼへ同じ金属を追加した場合、既存の加工状態と加熱経過時間を維持する
- 加熱済みのるつぼへ同じ金属を追加することを許可する。追加後も状態は `HEATED` のままとする
- 異なる金属の投入は拒否し、入力ItemStackを消費しない
- 通常クリックでは1個だけ投入する
- シフトクリックでは、容量内に完全に収まる最大個数を投入する
- アイテム1個分すら容量内へ収まらない場合は投入を拒否する
- アイテム1個の素材量を分割して投入しない。投入単位は常にアイテム個数とする

## GUIを開く条件

以下の条件をすべて満たした場合に、るつぼ用GUIを開く。

- プレイヤーがるつぼをメインハンドに持っている
- プレイヤーがスニークしている
- アイテムの空中使用として処理されている
- サーバー側で、選択中のホットバースロットに有効なるつぼが存在することを確認できる

オフハンドからはMVPのGUIを開かない。

`CrucibleItem#useOn` ではGUIを開かない。ブロックを対象とした右クリックでは、ブロック側または溶鉱炉側の操作を優先する。

## GUI

GUIは以下を持つ。

- 金属素材投入用の1スロット
- 現在の金属種類の表示
- 現在量と最大容量の表示
- 現在の加工状態の表示
- 内容物を完全削除する廃棄ボタン

入力スロットは、るつぼの内容物を保存する場所ではない。素材投入操作を受け付ける一時コンテナとして扱う。

通常クリックで入力スロットへ置ける最大数は1個とする。サーバー側で即時検証し、受理した場合はるつぼ状態へ変換して入力スロットを空にする。拒否した場合は入力アイテムを失わないようにプレイヤーへ戻す。

シフトクリックは `CrucibleMenu#quickMoveStack` で処理する。プレイヤーインベントリから、容量内に収まる最大個数を直接計算して投入する。一時入力スロットへスタック全体を移してから処理する方式は採用しない。

GUIを開いた対象るつぼが入っているホットバースロットは、GUIを閉じるまで通常のクリックとシフトクリックによる移動を禁止する。

サーバー側Menuは、GUIを開いたときのItemStack参照と対象ホットバースロット番号を保持する。操作要求ごとに以下を確認する。

- 対象スロットが有効範囲内である
- 対象スロットに `craftbound:crucible` が存在する
- 対象ItemStackがGUI開始時に参照したItemStackと同一である
- 状態データを正常に読み込める

対象ItemStackが消失、移動、置換された場合は、以後の投入と廃棄を拒否し、Menuを無効化する。

## 廃棄

- 廃棄すると内容物を一切返却せず、完全に削除する
- 金属ID、投入量、加熱経過時間、加工状態をまとめて初期化する
- 廃棄後の状態は `EMPTY` とする
- 空のるつぼへの廃棄要求は何も変更しない
- 廃棄は加工成功または加工失敗として扱わない
- 廃棄によって鍛冶経験値を付与しない
- 同じ廃棄要求が複数回処理されても、二重消費やアイテム生成が発生しない
- 廃棄は不可逆操作であるため、GUIで内容物が失われることを明示する
- MVPでは、廃棄ボタンを一度押すとクライアント側の確認状態へ移行し、同じGUI内でもう一度確定した場合だけサーバーへ廃棄要求を送る
- 確認状態はGUIを閉じた場合、対象るつぼが無効になった場合、または一定時間操作がなかった場合に解除してよい

クライアント側の確認状態は誤操作防止表示にすぎない。実際の廃棄可否と初期化処理はサーバー側で確定する。

## 加熱

- るつぼ自身はtick処理を持たない
- `BlacksmithFurnaceBlockEntity` がサーバーtickで加熱条件を確認する
- 加熱条件を満たした場合、`MeltingGameManager` がるつぼ状態更新APIを呼び出す
- 正の加熱tickが初めて適用された時点で、状態を `UNHEATED` から `HEATED` へ変更する
- `HEATED` のるつぼを再加熱した場合は、加熱経過時間だけを増加させる
- 加熱時間を負数へ変更しない
- 加熱時間の更新によって品質評価を確定しない

今回のるつぼ実装では、`MeltingGameManager` と `BlacksmithFurnaceBlockEntity` の本体は実装しない。これらから呼び出せるAPIだけを提供する。

## 鋳造時の消費

- 鋳造処理は対象パーツが要求する素材量だけを消費する
- 必要量が不足している場合は何も消費しない
- 消費後に余剰が残る場合、金属ID、加熱経過時間、加工状態を維持する
- 消費後の量が `0` になった場合は、共通の初期化APIを使用して `EMPTY` へ戻す
- 鋳造時の加熱評価は、消費前の加熱経過時間と金属定義を入力として別の評価APIで計算する
- 加熱評価結果はるつぼへ保存しない

## 外部自動化

- るつぼ用GUIはプレイヤーの手動操作だけを受け付ける
- ItemHandler Capabilityなど、外部から素材を投入または回収できる機能を公開しない
- Create、ホッパー、他MODの搬送機器から、るつぼへの素材投入や内容物の廃棄を実行できないようにする

# 採用した実装方針

## パッケージ構成

るつぼ固有の共通処理は、以下へ集約する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/
```

Menu関連は、るつぼ配下の `menu` パッケージへ配置する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/menu/
```

クライアント専用画面は、既存の職業別 `client` パッケージ構成に合わせ、以下へ配置する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/client/
```

金属素材定義を解決する共通処理は、るつぼ以外の鋳造・鍛造でも再利用するため、るつぼパッケージへ閉じ込めず、以下へ配置する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/
```

ただし、`materials.md` または `data-formats.md` が異なるパッケージ名・クラス名を明示している場合は、文書側を優先する。

## 新規追加クラス

### `CrucibleItem`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleItem.java
```

責務：

- るつぼアイテムの使用処理
- メインハンド、スニーク、サーバー側の条件確認
- `NetworkHooks.openScreen` による `CrucibleMenu` のオープン
- Menu生成用追加データとして、対象ホットバースロット番号を送信
- `useOn` ではGUIを開かない
- NBTの直接操作は行わない

### `CrucibleProcessState`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleProcessState.java
```

責務：

- `EMPTY`、`UNHEATED`、`HEATED` の型安全な定義
- 保存文字列との変換
- 未知の文字列を暗黙に `EMPTY` へ変換しない

### `CrucibleState`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleState.java
```

Java 17のrecordとして実装してよい。

```java
public record CrucibleState(
    int version,
    @Nullable ResourceLocation metalId,
    int amount,
    long heatingTicks,
    CrucibleProcessState processState
) {}
```

責務：

- るつぼ状態の型付き表現
- 状態の不変条件を検証するファクトリまたは検証メソッド
- 状態を外部から部分的に変更させない

### `CrucibleStateCodec`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleStateCodec.java
```

責務：

- ItemStack NBTと `CrucibleState` の相互変換
- 保存形式バージョンの検証
- 不正NBT、未知バージョン、未知状態名の検出
- NBTキー文字列の一元管理
- 不正データを暗黙に上書きしない

### `CrucibleStateService`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleStateService.java
```

責務：

- るつぼ状態の読み取り
- 金属投入
- 加熱時間の加算
- 鋳造用の素材消費
- 内容物の完全廃棄
- `EMPTY` への共通初期化
- すべての状態変更で不変条件を保証

Menu、Screen、`MeltingGameManager`、`CastingGameManager` は、このサービスを経由して状態を変更する。

想定する公開APIは以下に相当する。

```java
public final class CrucibleStateService {
    public static Optional<CrucibleState> read(ItemStack crucible);

    public static CrucibleInsertResult insert(
        ItemStack crucible,
        ItemStack input,
        int requestedItemCount,
        MetalMaterialResolver resolver
    );

    public static boolean advanceHeating(ItemStack crucible, long ticks);

    public static boolean consumeForCasting(ItemStack crucible, int amount);

    public static boolean discardAll(ItemStack crucible);
}
```

既存コードがインスタンスサービスを使用している場合は、staticではなくインスタンスとして実装してよい。責務と呼び出し境界は変更しない。

### `CrucibleInsertResult`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleInsertResult.java
```

責務：

- 実際に受理したアイテム数
- 増加した素材量
- 投入後状態
- 拒否理由

拒否理由は文字列ではなくenumで表現する。enumを同じファイルのネスト型にするか、以下の別ファイルにしてよい。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleInsertFailure.java
```

最低限の拒否理由：

```java
public enum CrucibleInsertFailure {
    NONE,
    NOT_CRUCIBLE,
    INVALID_CRUCIBLE_STATE,
    UNSUPPORTED_MATERIAL,
    DIFFERENT_METAL,
    NO_CAPACITY,
    INVALID_REQUEST
}
```

### `MetalMaterialResolver`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/MetalMaterialResolver.java
```

責務：

- 入力ItemStackから金属IDと1個あたりの素材量を解決する
- るつぼ、鋳造、金属塊処理から共通利用できる境界を提供する
- 個別の金属IDや換算量をGUI・Item・Menuへ漏らさない

想定API：

```java
public interface MetalMaterialResolver {
    Optional<ResolvedMetalMaterial> resolve(ItemStack stack);
}
```

### `ResolvedMetalMaterial`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/material/ResolvedMetalMaterial.java
```

```java
public record ResolvedMetalMaterial(
    ResourceLocation metalId,
    int amountPerItem
) {}
```

金属素材定義のレジストリまたはリロード処理が既に `materials.md`、`data-formats.md` に定義されている場合、そこで定義された型を使用し、この2クラスを重複追加しない。

### `CrucibleMenu`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/menu/CrucibleMenu.java
```

責務：

- 対象ホットバースロット番号の保持
- サーバー側でGUI開始時のItemStack参照を保持
- プレイヤーインベントリと入力用一時コンテナの公開
- 通常投入処理
- `quickMoveStack` によるシフト投入処理
- 廃棄ボタン要求の処理
- 対象ホットバースロットの移動禁止
- `stillValid` による対象るつぼの継続検証
- Menu同期

独自パケットは使用せず、廃棄確定は `AbstractContainerMenu#clickMenuButton` で処理する。

### `CrucibleInputSlot`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/crucible/menu/CrucibleInputSlot.java
```

責務：

- 通常クリック用の一時入力スロット
- 最大配置数を1に制限
- `MetalMaterialResolver` が対応素材として解決できるItemStackだけを受け付ける
- るつぼの内容物そのものは保持しない

### `CrucibleScreen`

配置先：

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/client/CrucibleScreen.java
```

責務：

- `CrucibleMenu` の描画
- 金属種類、現在量、最大容量、加工状態の表示
- 廃棄ボタンと確認表示
- クライアント側の廃棄確認状態と確認期限の管理
- 確定時に `minecraft.gameMode.handleInventoryButtonClick` を呼ぶ
- ItemStackのNBTを変更しない
- 金属量や加工状態をサーバーへ申告しない

## 新規追加テスト

### `CrucibleStateServiceTest`

配置先：

```text
src/test/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleStateServiceTest.java
```

最低限、以下を検証する。

- 空のるつぼを `EMPTY` として読み込める
- 最初の投入で `UNHEATED` へ遷移する
- 同じ金属を追加できる
- 異なる金属を拒否する
- 容量超過時に完全に入る個数だけを受理する
- 1個も入らない場合は入力を変更しない
- 初回加熱で `HEATED` へ遷移する
- 再加熱で加熱時間だけが増える
- 一部鋳造消費後に金属ID、加熱時間、状態が維持される
- 全量消費で `EMPTY` へ戻る
- 廃棄で `EMPTY` へ戻る
- 廃棄済み状態へ再度廃棄しても結果が変化しない
- NBT保存と再読み込みで状態が一致する
- 未知の保存バージョンを上書きしない
- 不変条件に反するNBTを操作可能な状態として返さない

必要に応じて以下も追加する。

```text
src/test/java/com/magu1436/craftbound/occupations/blacksmith/crucible/CrucibleStateCodecTest.java
```

## ItemStackへの保存

Forge 1.20.1のItemStack NBTへ、るつぼ専用のネストしたCompoundTagを保存する。

想定形式：

```text
{
  CraftboundCrucible: {
    Version: 1,
    Metal: "minecraft:iron",
    Amount: 3000,
    HeatingTicks: 1200L,
    ProcessState: "HEATED"
  }
}
```

NBTキーは `CrucibleStateCodec` の定数へ集約し、複数クラスへ文字列を分散させない。

空のるつぼでは `CraftboundCrucible` タグ全体を省略してよい。タグが存在しない場合は、正規化済みの `EMPTY` 状態として読み込む。

`EMPTY` へ戻す場合は、古い `Metal`、`Amount`、`HeatingTicks`、`ProcessState` を個別に残さず、るつぼ状態タグ全体を削除するか、完全な `EMPTY` 状態で上書きする。実装内で方式を統一する。

保存形式には `Version` を持たせ、将来の形式変更に対応できるようにする。

## Menuのオープン

`CrucibleItem#use` から、サーバー側で `NetworkHooks.openScreen` を使用する。

Menu生成用の追加データとして、対象るつぼが存在するホットバースロット番号を送る。

サーバー側では、送信値やクライアント状態を正とせず、Menuを開く直前に以下を再確認する。

- 使用した手がメインハンドである
- 選択中スロット番号と送信するスロット番号が一致する
- 対象ItemStackが `CraftboundItems.CRUCIBLE` である
- 対象ItemStackの状態を正常に読み込める

クライアント側Menuコンストラクタは、受信したスロット番号からプレイヤーインベントリを参照する。クライアント側の参照は表示用であり、状態変更の正本にしない。

## 通常クリック投入

通常クリックでは、一時入力スロットの最大スタック数を1にする。

一時入力スロットが変更された場合、サーバー側Menuが以下を実行する。

1. 対象るつぼが有効か確認する
2. 入力ItemStackを素材定義から解決する
3. `CrucibleStateService.insert` へ1個の投入を要求する
4. 成功した場合だけ入力ItemStackを1個縮小する
5. 入力スロットを空にする
6. 拒否された場合は入力アイテムをプレイヤーへ返す
7. Menu変更を同期する

再帰的に `slotsChanged` が呼ばれて二重処理されないよう、Menu内部に処理中フラグを設けるか、入力コンテナの更新経路を一か所へ限定する。

## シフトクリック投入

`CrucibleMenu#quickMoveStack` では、クリックされたプレイヤーインベントリ内のItemStackを直接処理する。

1. 対応素材か確認する
2. 現在のるつぼ状態と空き容量を取得する
3. 容量へ完全に収まる最大個数を求める
4. `CrucibleStateService.insert` へ最大個数を要求する
5. 実際に受理された個数だけ元ItemStackを縮小する
6. 受理数が0の場合は元ItemStackを変更しない
7. Menu変更を同期する

プレイヤーインベントリ内の通常移動も維持する必要があるため、非対応素材または投入不能素材のシフトクリックは、無理に入力スロットへ移動させず変更なしとして返してよい。

## 廃棄ボタン

廃棄確定はMenuボタンとして扱う。

クライアントは以下だけを送る。

- 現在の `containerId`
- 固定の廃棄ボタンID

サーバー側の `CrucibleMenu#clickMenuButton` は、以下を再検証する。

- Menuが有効である
- 対象ホットバースロットにGUI開始時と同じるつぼが存在する
- 状態を正常に読み込める
- 内容量が0より大きい

条件を満たした場合だけ `CrucibleStateService.discardAll` を呼ぶ。

廃棄確認の1回目のクリックはクライアント表示だけで処理し、サーバーへ送信しない。2回目の確定操作だけを送信する。

## dirty化と同期

るつぼ状態を変更した場合は、サーバー側プレイヤーインベントリを正とし、Menuの標準同期経路で更新済みItemStackをクライアントへ反映する。

NBTを直接変更しただけで同期されない場合は、対象スロットへ同じItemStackを再設定するか、既存コードで採用しているインベントリ同期方法へ合わせる。

るつぼ状態の変更ごとに独自ネットワークパケットを追加しない。

## 不正データの扱い

読み込んだ状態が不変条件を満たさない場合は、内容物を暗黙に削除して `EMPTY` へ戻さない。

- 状態変更操作を拒否する
- GUIでは不正状態として操作不能にする
- 原因を特定できるログを出力する
- 同じItemStackについて毎tick大量にログを出さない
- 未対応の新しいデータバージョンを上書きしない

既知の旧バージョンが追加された場合は、`CrucibleStateCodec` 内で段階的に最新版へ移行する。

# 採用しなかった方針

## 投入素材を取り出せるようにする

インゴットと複数サイズの金属塊を混在できるため、投入履歴の保存または返却アイテム選択GUIが必要になる。MVPでは実装量に対する利点が小さいため採用しない。

## 投入履歴を保存する

素材を取り出さない仕様では不要であり、ItemStackの保存量と状態遷移を複雑にするため採用しない。

## るつぼへ加熱品質を保存する

加熱評価は鋳型へ流し込んだ工程結果に属する。余剰金属がるつぼへ残る場合の責務も曖昧になるため採用しない。

## `hasEverBeenHeated` を別属性として保存する

`CrucibleProcessState.HEATED` から判定でき、同じ意味の値を二重管理するため採用しない。

## 加熱段階を永続化する

鋳造可能、適正、過熱などの段階は、金属定義と加熱経過時間から導出できる。データ定義変更時の不整合を増やすため採用しない。

## るつぼ自身にtick処理を持たせる

Itemはワールド上で自律的に稼働する加工設備ではない。溶鉱炉側が加熱条件を判定し、GameManagerと専用APIを通じて状態を更新するため採用しない。

## ItemHandler Capabilityを公開する

ホッパーや他MODによる自動投入が可能になり、手動操作だけを受け付ける仕様に反するため採用しない。

## 浮動小数点数で素材量を保存する

比較誤差によって容量判定と残量0判定が不安定になるため採用しない。

## MenuやScreenからNBTを直接操作する

状態の不変条件を複数箇所で管理することになり、初期化漏れや異種金属混入が起こりやすいため採用しない。

## 廃棄専用の独自パケットを追加する

既存のMenuボタン通信で十分であり、通信経路を増やす必要がないため採用しない。

## るつぼのために新しいグローバルクライアントイベントクラスを作る

既存の `CraftboundClientEvents` でMenuとScreenを登録できるため採用しない。

# 関連する既存クラス

## 変更する既存クラス

### `CraftboundItems`

```text
src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java
```

変更内容：

- `RegistryObject<Item>` として `CRUCIBLE` を登録する
- アイテムIDは `crucible`
- `CrucibleItem` を使用する
- `new Item.Properties().stacksTo(1)` 相当を設定する
- 既存のDeferredRegister方式へ合わせる

想定例：

```java
public static final RegistryObject<Item> CRUCIBLE = ITEMS.register(
    "crucible",
    () -> new CrucibleItem(new Item.Properties().stacksTo(1))
);
```

実際の登録ヘルパーが存在する場合は既存方式を使用する。

### `CraftboundMenus`

```text
src/main/java/com/magu1436/craftbound/registry/CraftboundMenus.java
```

変更内容：

- `CrucibleMenu` 用の `MenuType` を登録する
- 登録IDは `crucible`
- 追加データとしてホットバースロット番号を受け取れる `IContainerFactory` 形式を使用する

想定名：

```java
public static final RegistryObject<MenuType<CrucibleMenu>> CRUCIBLE;
```

### `CraftboundClientEvents`

```text
src/main/java/com/magu1436/craftbound/client/event/CraftboundClientEvents.java
```

変更内容：

- `FMLClientSetupEvent` 内で `CrucibleMenu` と `CrucibleScreen` を登録する
- 必要に応じて `event.enqueueWork` 内で `MenuScreens.register` を呼ぶ
- クライアント専用クラス参照をこのクラスよりサーバー共通側へ漏らさない

想定処理：

```java
MenuScreens.register(
    CraftboundMenus.CRUCIBLE.get(),
    CrucibleScreen::new
);
```

既存の画面登録一覧化方式が存在する場合は、その方式へ追加する。

### `CraftboundDataReloadEventHandler`

```text
src/main/java/com/magu1436/craftbound/event/CraftboundDataReloadEventHandler.java
```

変更は、金属素材定義のリロードリスナーが未実装で、今回の実装範囲へ含める場合だけ行う。

- `materials.md` と `data-formats.md` に従う金属素材定義リロードリスナーを登録する
- 既存のリロードリスナー登録方式へ合わせる
- るつぼ専用の別リロードイベントハンドラーを追加しない

金属素材定義が既に別実装されている場合、このクラスは変更しない。

### `Craftbound`

```text
src/main/java/com/magu1436/craftbound/Craftbound.java
```

`CraftboundItems` と `CraftboundMenus` が既にMODイベントバスへ登録されている場合は変更しない。

未登録の場合だけ、既存レジストリと同じ方式で登録する。るつぼのためだけに個別初期化処理を追加しない。

### `ja_jp.json`

```text
src/main/resources/assets/craftbound/lang/ja_jp.json
```

最低限、以下に相当する翻訳キーを追加する。

```text
item.craftbound.crucible
screen.craftbound.crucible
screen.craftbound.crucible.metal
screen.craftbound.crucible.amount
screen.craftbound.crucible.state
screen.craftbound.crucible.discard
screen.craftbound.crucible.discard_warning
screen.craftbound.crucible.discard_confirm
screen.craftbound.crucible.invalid_state
```

### `en_us.json`

```text
src/main/resources/assets/craftbound/lang/en_us.json
```

`ja_jp.json` と同じキーを英語で追加する。

## 追加するリソース

### アイテムモデル

```text
src/main/resources/assets/craftbound/models/item/crucible.json
```

既存アイテムモデルの形式へ合わせる。

### アイテムテクスチャ

```text
src/main/resources/assets/craftbound/textures/item/crucible.png
```

専用テクスチャが未準備の場合、実装確認用の仮テクスチャを使用してよい。ただし、他MODやバニラのテクスチャを無断で複製しない。

### GUIテクスチャ

専用背景画像を使用する場合のみ追加する。

```text
src/main/resources/assets/craftbound/textures/gui/crucible.png
```

バニラのコンテナ描画部品だけで構成する場合は追加不要。

## 参照する将来クラス

以下は今回新規実装しないが、公開APIの利用者として想定する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/melting/MeltingGameManager.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/CastingGameManager.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/furnace/BlacksmithFurnaceBlockEntity.java
```

これらのクラスが今後別のパッケージ名で実装される場合も、`CrucibleStateService` のAPI境界は維持する。

## 追加後の想定ディレクトリ

```text
src/main/java/com/magu1436/craftbound/
├── client
│   └── event
│       └── CraftboundClientEvents.java                  # 変更
├── occupations
│   └── blacksmith
│       ├── client
│       │   └── CrucibleScreen.java                     # 新規
│       ├── crucible
│       │   ├── CrucibleInsertFailure.java              # 新規、ネストenumでも可
│       │   ├── CrucibleInsertResult.java               # 新規
│       │   ├── CrucibleItem.java                       # 新規
│       │   ├── CrucibleProcessState.java               # 新規
│       │   ├── CrucibleState.java                      # 新規
│       │   ├── CrucibleStateCodec.java                 # 新規
│       │   ├── CrucibleStateService.java               # 新規
│       │   └── menu
│       │       ├── CrucibleInputSlot.java              # 新規
│       │       └── CrucibleMenu.java                   # 新規
│       └── material
│           ├── MetalMaterialResolver.java              # 新規または既存型を再利用
│           └── ResolvedMetalMaterial.java              # 新規または既存型を再利用
└── registry
    ├── CraftboundItems.java                            # 変更
    └── CraftboundMenus.java                            # 変更
```

```text
src/test/java/com/magu1436/craftbound/occupations/blacksmith/crucible/
├── CrucibleStateCodecTest.java                         # 必要に応じて新規
└── CrucibleStateServiceTest.java                       # 新規
```

# データフロー

## GUIを開く

```text
プレイヤーがメインハンドにるつぼを持つ
    ↓
空中でスニーク右クリックする
    ↓
CrucibleItem#useがメインハンドとスニーク状態を確認する
    ↓
サーバー側で選択中ホットバースロットとるつぼItemStackを再確認する
    ↓
NetworkHooks.openScreenでCrucibleMenuを開く
    ↓
追加データとして対象ホットバースロット番号を送る
    ↓
クライアントがCrucibleScreenを表示する
```

## 通常クリックで投入する

```text
プレイヤーが入力スロットへ素材を1個置く
    ↓
サーバー側CrucibleMenuが入力スロット変更を検知する
    ↓
MetalMaterialResolverが金属IDと1個あたり素材量を解決する
    ↓
CrucibleStateServiceが金属種類、状態、空き容量を検証する
    ↓
成功した場合だけるつぼ状態へ1個分を加算する
    ↓
入力ItemStackを1個減らす
    ↓
入力スロットを空にする
    ↓
更新後のるつぼItemStackをMenu同期でクライアントへ反映する
```

## シフトクリックで投入する

```text
プレイヤーが対応素材をシフトクリックする
    ↓
CrucibleMenu#quickMoveStackが対象るつぼと素材を検証する
    ↓
空き容量へ収まる最大の整数個数を計算する
    ↓
CrucibleStateServiceへ計算した個数の投入を要求する
    ↓
実際に受理された個数だけプレイヤーのItemStackを減らす
    ↓
更新後のるつぼItemStackをMenu同期でクライアントへ反映する
```

## 廃棄する

```text
プレイヤーが廃棄ボタンを押す
    ↓
CrucibleScreenが確認状態へ移行する
    ↓
プレイヤーが廃棄を確定する
    ↓
handleInventoryButtonClickでMenuボタン要求を送る
    ↓
サーバー側CrucibleMenuが対象るつぼと現在状態を再検証する
    ↓
CrucibleStateService.discardAllを呼ぶ
    ↓
るつぼ状態タグをEMPTYへ初期化する
    ↓
更新後のItemStackをMenu同期でクライアントへ反映する
```

## 溶鉱炉で加熱する

```text
BlacksmithFurnaceBlockEntityのサーバーtick
    ↓
内部にるつぼがあり、有効熱源があることを確認する
    ↓
MeltingGameManagerへるつぼと加熱tick数を渡す
    ↓
MeltingGameManagerがCrucibleStateService.advanceHeatingを呼ぶ
    ↓
加熱経過時間を増加させる
    ↓
初回加熱ならUNHEATEDからHEATEDへ遷移する
```

## 鋳型へ流し込む

```text
CastingGameManagerが対象パーツ、金属種類、必要量を検証する
    ↓
るつぼの加熱経過時間を加熱評価APIへ渡す
    ↓
今回流し込む金属の加熱評価を確定する
    ↓
CrucibleStateService.consumeForCastingを呼ぶ
    ↓
必要量だけ消費する
    ↓
残量があれば既存状態を維持する
残量が0ならEMPTYへ初期化する
    ↓
確定した加熱評価を鋳造中状態または粗加工パーツへ保存する
```

# 擬似コード

```text
るつぼ使用時:
    使用した手がメインハンドでなければ何もしない
    プレイヤーがスニークしていなければ何もしない

    クライアント側の場合:
        成功結果だけ返す

    サーバー側の場合:
        選択中ホットバースロット番号を取得する
        対象スロットに有効なるつぼがなければ何もしない
        対象るつぼの状態を読み込めなければ何もしない
        スロット番号を追加データとしてMenuを開く
```

```text
状態読み込み時:
    対象ItemStackがCraftboundItems.CRUCIBLEでなければ失敗を返す
    るつぼ状態タグがなければEMPTY状態を返す
    Versionを確認する
    未対応バージョンなら失敗を返し、データを変更しない
    Metal、Amount、HeatingTicks、ProcessStateを読み込む
    リソースIDと数値型を検証する
    状態の不変条件を検証する
    不正ならログを出して失敗を返す
    検証済みCrucibleStateを返す
```

```text
金属投入時:
    サーバー側のみ処理する
    requestedItemCountが1未満ならINVALID_REQUESTを返す
    対象ItemStackがるつぼでなければNOT_CRUCIBLEを返す
    現在のるつぼ状態を読み込めなければINVALID_CRUCIBLE_STATEを返す
    入力素材をMetalMaterialResolverへ渡す
    未対応素材ならUNSUPPORTED_MATERIALを返す

    状態がEMPTYの場合:
        空き容量を最大容量とする
        完全に入る個数を計算する
        1個も入らなければNO_CAPACITYを返す
        金属IDを入力素材の金属IDへ設定する
        素材量を加算する
        加熱時間を0にする
        状態をUNHEATEDへ変更する
        状態を保存する
        受理個数と成功結果を返す

    状態がUNHEATEDまたはHEATEDの場合:
        現在の金属IDと入力素材の金属IDが異なればDIFFERENT_METALを返す
        空き容量から完全に入る個数を計算する
        1個も入らなければNO_CAPACITYを返す
        素材量だけを加算する
        加熱時間と加工状態は変更しない
        状態を保存する
        受理個数と成功結果を返す
```

```text
通常クリック投入時:
    入力スロットにItemStackがなければ終了する
    処理中フラグがtrueなら終了する
    処理中フラグをtrueにする

    対象るつぼが無効なら:
        入力ItemStackをプレイヤーへ返す
        Menuを無効化する
        処理中フラグをfalseにして終了する

    CrucibleStateService.insertへ1個の投入を要求する

    成功した場合:
        入力ItemStackを受理個数だけ減らす

    残った入力ItemStackをプレイヤーへ返す
    入力スロットを空にする
    Menu変更を同期する
    処理中フラグをfalseにする
```

```text
シフトクリック投入時:
    クリックされたスロットが対象るつぼのホットバースロットなら変更しない
    クリックされたスロットがプレイヤーインベントリでなければ変更しない
    対象ItemStackが対応金属素材でなければ変更しない

    現在個数をrequestedItemCountとしてinsertへ渡す
    insertが返した受理個数だけ対象ItemStackを減らす
    受理個数が0ならスロットを変更しない
    Menu変更を同期する
```

```text
加熱時:
    サーバー側のみ処理する
    ticksが0以下なら何もしない
    現在状態を読み込めなければ何もしない
    EMPTYなら何もしない
    加熱時間の加算でlongがオーバーフローしないことを確認する
    加熱経過時間へticksを加算する
    UNHEATEDならHEATEDへ変更する
    HEATEDなら状態を維持する
    状態を保存する
```

```text
鋳造用消費時:
    サーバー側のみ処理する
    amountが1未満なら失敗する
    現在状態を読み込めなければ失敗する
    EMPTYなら失敗する
    現在量がamount未満なら失敗する
    現在量からamountを減らす

    残量が0の場合:
        resetToEmptyを呼ぶ
    それ以外:
        金属ID、加熱経過時間、加工状態を維持して保存する
```

```text
廃棄時:
    サーバー側のみ処理する
    現在状態を読み込めなければ失敗する
    EMPTYなら変更なしを返す
    resetToEmptyを呼ぶ
    成功を返す
```

```text
resetToEmpty:
    るつぼ状態タグ全体を削除する
    読み直した場合にEMPTY状態となることを保証する
```

```text
Menu有効性確認時:
    プレイヤーが生存しているか確認する
    対象ホットバースロットが0から8の範囲か確認する
    対象スロットにるつぼが存在するか確認する
    対象ItemStackがGUI開始時に保持した参照と同一か確認する
    状態を正常に読み込めるか確認する
    一つでも満たさなければMenuを無効として扱う
```

```text
廃棄ボタン処理時:
    ボタンIDが廃棄IDでなければ拒否する
    Menuが有効でなければ拒否する
    対象るつぼの状態を読み込む
    EMPTYなら変更なしで成功扱いにしてよい
    discardAllを呼ぶ
    Menu変更を同期する
```

# 境界条件・例外

## 異なる金属

既に金属が入っているるつぼへ異なる金属を投入した場合は、投入を拒否する。既存状態と入力ItemStackを変更しない。

## 容量超過

通常クリックでは、対象アイテム1個分が完全に入らない場合は拒否する。シフトクリックでは、完全に入る個数だけを投入し、残りをプレイヤーインベントリへ残す。

## 未登録素材

金属素材定義から金属IDと素材量を解決できないアイテムは投入できない。アイテム名、クラス名、タグ名だけから素材量を推測しない。

## 加熱済みるつぼへの追加投入

同じ金属であれば許可する。加工状態は `HEATED`、加熱経過時間は既存値を維持する。追加した未加熱素材だけを別管理しない。

## 内容量0

鋳造または廃棄によって内容量が0になった場合は、必ず共通初期化処理を呼ぶ。金属ID、加熱時間、加工状態の一部だけが残る状態を作らない。

## ItemStackの移動

GUIの対象ホットバースロットは操作中にロックする。コマンド、死亡、他MOD処理などで対象ItemStackが消失または置換された場合は、それ以降の投入と廃棄要求を拒否してMenuを無効化する。

## GUI終了時の入力スロット

一時入力スロットには処理後のItemStackを残さない。例外によりItemStackが残ったままGUIが閉じる場合は、プレイヤーインベントリへ返し、入らなければプレイヤー位置へドロップする。アイテムを消失させない。

## 重複要求

同じ廃棄要求が再送された場合、最初の処理で `EMPTY` になった後の要求は何も変更しない。

投入処理はサーバー側の実際のItemStack個数を正とする。クライアントが申告した個数だけを根拠に消費しない。

## オーバーフロー

加熱経過時間は `long` で保持する。加算前にオーバーフローを検出し、不正な負数へ反転させない。

実用上の上限を設ける場合は、金属定義で使用する最大判定時間より十分大きい値へクランプする。

## 不正NBT

不正状態を自動的に空へ変換して内容物を削除しない。操作を停止し、データを上書きせず、管理者が原因を確認できるログを出す。

## クライアント改変

クライアントから金属ID、素材量、空き容量、加熱時間、加工状態を受け取らない。クライアントは通常のスロット操作と固定MenuボタンIDだけを送る。

## 外部搬送

るつぼItemStackおよびMenuから素材投入用ItemHandler Capabilityを公開しない。外部MODからMenuの一時入力スロットへアクセスできる構造にしない。

## クリエイティブモード

MVPでは通常のMenu操作と同じく、投入に使用したItemStackを消費する実装でよい。クリエイティブ時の非消費を採用する場合は、別途仕様を確定してから実装する。

## 専用サーバー

`CrucibleScreen` を共通コード、Item、Menu、レジストリ初期化から直接参照しない。画面登録は `CraftboundClientEvents` に限定し、専用サーバーでクライアントクラス読み込みエラーを起こさない。

# 完了条件

- `CraftboundItems` に `craftbound:crucible` が登録される
- るつぼの最大スタック数が1になる
- `CraftboundMenus` にるつぼ用MenuTypeが登録される
- `CraftboundClientEvents` で `CrucibleScreen` が登録される
- るつぼをメインハンドに持ち、空中でスニーク右クリックするとGUIが開く
- 通常の空中右クリック、オフハンド使用、ブロック対象右クリックではGUIが開かない
- GUIに素材投入用1スロット、金属種類、容量、加工状態、廃棄ボタンが表示される
- 通常クリックで対応素材を1個投入できる
- シフトクリックで容量内へ収まる最大個数を投入できる
- 空のるつぼへ投入すると、金属ID、量、`UNHEATED` 状態が保存される
- 同じ金属のインゴットと金属塊を混在して投入できる
- 異なる金属、未登録素材、容量超過を拒否し、入力ItemStackを消費しない
- 投入した素材をGUIから取り出せない
- 加熱済みるつぼへ同じ金属を追加しても、加熱時間と `HEATED` 状態が維持される
- 廃棄の確認操作後、内容物が返却されず `EMPTY` へ初期化される
- 廃棄によって鍛冶経験値や加工失敗結果が発生しない
- `CrucibleStateService.advanceHeating` に正のtickを渡すと加熱時間が増え、初回加熱時に `HEATED` へ遷移する
- `CrucibleStateService.consumeForCasting` が必要量だけを消費する
- 一部消費後は金属ID、加熱時間、加工状態が維持される
- 全量消費後はすべての状態が `EMPTY` へ初期化される
- るつぼに加熱品質が保存されない
- 状態変更がすべて `CrucibleStateService` を経由し、Menu、Screen、GameManagerからNBTを直接変更していない
- 不正NBTまたは未対応バージョンを暗黙に上書きしない
- GUIを開いている間、対象るつぼのホットバースロットを通常操作で移動できない
- 対象るつぼが外部要因で移動または置換された場合、以後の操作が拒否される
- GUI終了時に一時入力スロットのItemStackが消失しない
- ItemHandler Capabilityを通じた自動投入ができない
- クライアントが偽の金属ID、量、加熱時間を送信して状態を変更できない
- ワールド保存、再ログイン、サーバー再起動後もるつぼ状態が維持される
- `CrucibleStateServiceTest` が成功する
- 専用サーバーでクライアント専用クラス参照による起動エラーが発生しない
- `gradlew build` が成功する

# 今回の対象外

- `BlacksmithFurnaceBlock` と `BlacksmithFurnaceBlockEntity` の本体実装
- 熱源の検知と `hasHeatSource` キャッシュ
- `MeltingGameManager` の本体実装
- `CastingGameManager` の本体実装
- 金属ごとの加熱時間、適正時間、全消失時間の評価
- 加熱警告の黒煙、金属音、警告間隔
- 鋳造用作業台、鋳型、冷却処理
- 加熱品質の計算と粗加工パーツへの保存
- 鍛造ミニゲーム
- 非金属加工
- 鍛冶経験値の付与
- 合金
- 投入済み素材の取り出し
- 投入履歴の保存
- 自動投入と自動廃棄
- るつぼ内容物のJade表示
- るつぼ内容物に応じた動的モデル描画
- 正式なアイテム・GUIアートの制作
- クリエイティブモード専用の素材非消費処理
