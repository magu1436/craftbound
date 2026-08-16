# 鍛冶師・品質付き完成品組み立て 実装仕様書

## 1. 目的

本仕様書は、Craftbound の鍛冶師システムにおける「加工済みパーツから完成品を組み立てる工程」を実装するための方針を定義する。

対象ブランチは `develop` とする。

既存仕様では、金属パーツ・非金属パーツを加工した後、Vanilla の作業台を使用して固定レシピにより完成品を組み立てる。高品質な完成品は、組み立てに使用した品質付きパーツの品質をもとに完成品品質を算出する。

今回の実装では、以下を主要目的とする。

- Vanilla の作業台上で品質付きパーツを使用した shaped recipe を成立させる。
- パーツの種類だけでなく、素材もレシピ条件として厳密に照合する。
- 加工済みパーツの品質から完成品品質を算出し、既存の品質保持機構を通して完成品へ保存する。
- 通常素材など、品質計算対象外の材料を完成品品質の計算から除外する。
- 組み立て工程では鍛冶師経験値を付与しない。
- 組み立て専用の設備・GUI・ネットワーク処理は追加しない。

本仕様書は Codex が実装時に追加判断を行わずに済むことを優先し、MVPで必要な責務と非対象範囲を明確にする。


## 2. 対象範囲

### 2.1 実装対象

以下を実装対象とする。

- `craftbound:quality_assembly` 用の shaped crafting recipe
- 品質付き金属パーツのレシピ条件判定
- 品質付き非金属パーツのレシピ条件判定
- パーツ種別と素材の双方を用いた一致判定
- Vanilla `Ingredient` を使用する通常素材の条件判定
- 完成品品質の共通計算窓口
- 完成品への品質保存
- `quality_assembly` の JSON 読み込み
- レシピ・Serializer の登録
- 最低1件の実用レシピ追加
  - 初期確認対象は鉄のピッケルとする
- 関連仕様書・テスト仕様の更新

### 2.2 実装対象外

以下は今回の実装対象外とする。

- 組み立て専用ブロック
- 組み立て専用 Menu / Screen
- 組み立て専用 BlockEntity
- 組み立て専用ネットワークパケット
- 組み立て時のミニゲーム
- 組み立て成功時の鍛冶師経験値
- `AssemblyExperienceHook` 等の経験値付与専用クラス
- 品質によるレシピ成立可否の変更
- パーツ品質に応じた必要素材数の変更
- 任意素材から任意装備を動的生成する仕組み
- 品質計算における部位ごとの重み付け
- 素材指定のワイルドカード
- 「任意の金属」「任意の木材」等の曖昧なパーツ素材条件
- Vanilla の既存クラフトレシピ削除
- 完成品の攻撃力・耐久値等を Recipe 内で直接変更する処理

完成品性能への品質反映は既存の品質システム側の責務とし、本実装は完成品へ品質値を保存するところまでを担当する。


## 3. 前提・参照仕様

### 3.1 参照ドキュメント

実装時は `develop` ブランチ上の以下を参照すること。

- `docs/occupations/blacksmith/blacksmith.md`
- `docs/occupations/blacksmith/data-formats.md`
- `docs/occupations/blacksmith/iron-pickaxe.md`
- `docs/occupations/blacksmith/test-cases.md`

特に既存仕様の以下を維持する。

- 組み立てには Vanilla の作業台を使用する。
- 専用の組み立て設備は追加しない。
- 通常レシピは削除しない。
- 高品質完成品には `craftbound:quality_assembly` を使用する。
- 異なる品質のパーツ同士でも、要求されたパーツ種別・素材が一致すれば組み立て可能とする。
- 完成品品質の初期計算方式は、品質計算対象パーツの算術平均とする。
- 品質を持たない追加素材は完成品品質の計算対象外とする。

### 3.2 既存実装の再利用

金属パーツ・非金属パーツの状態取得、素材判定、品質取得、品質保存については既存実装を再利用する。

新しい組み立てシステム側で、既存の以下の概念を重複実装しないこと。

- 金属パーツ種別
- 非金属パーツ種別
- 金属素材定義
- 非金属素材定義
- パーツ状態の保存形式
- 品質状態の保存形式
- 品質による完成品性能補正

品質の読み書きは既存の `QualityStateService` または同等の既存共通窓口を使用すること。

パーツ種別・素材の取得も既存の state service / definition / registry を使用し、Recipe 側で同内容の文字列テーブルや enum を新設しないこと。

### 3.3 既存仕様からの変更点

現在の `blacksmith.md` に組み立て成功時の経験値付与が残っている場合、本実装に合わせて削除する。

変更後の方針は以下とする。

> 完成品の組み立てでは鍛冶師経験値を付与しない。鍛冶師経験値は、鋳造・鍛造・非金属加工など、完成品を構成するパーツを加工した工程で付与する。

したがって、組み立て処理から Experience Source、ActionBar 通知、Experience Hook 等を呼び出してはならない。


## 4. 実装方針

### 4.1 基本構成

`craftbound:quality_assembly` は Vanilla の crafting table から利用可能な `CraftingRecipe` として実装する。

想定クラス構成は以下とする。

```text
QualityAssemblyRecipe
├─ shaped pattern の保持
├─ key ごとの AssemblyIngredient の保持
├─ matches(...)
└─ assemble(...)

QualityAssemblyRecipeSerializer
├─ JSON 読み込み
├─ JSON 検証
└─ network serialization

AssemblyIngredient
├─ VanillaIngredient
├─ MetalPartIngredient
└─ NonMetalPartIngredient

FinishedItemQualityCalculator
└─ 品質平均と丸め
```

既存パッケージ構成や命名規則に適合する場合は名称変更を許容する。ただし、責務の分離は維持すること。

### 4.2 RecipeType について

新しい専用作業台を作るわけではないため、`quality_assembly` 専用の新規作業台向け `RecipeType` は不要とする。

Vanilla crafting table が通常の crafting recipe と同様に検索・成立判定できる構成にする。

必要なのは少なくとも以下である。

- `QualityAssemblyRecipe`
- `RecipeSerializer<QualityAssemblyRecipe>` の登録
- Serializer ID `craftbound:quality_assembly`

Forge 1.20.1 および本リポジトリ既存の Recipe 登録方式に従うこと。

### 4.3 副作用の禁止

以下のメソッドは純粋なレシピ判定・結果生成のみを担当する。

```java
matches(...)
assemble(...)
```

これらの処理内で以下を実行してはならない。

- XP 付与
- プレイヤー Capability 更新
- ActionBar 表示
- 効果音再生
- 入力 ItemStack の直接消費
- 入力 ItemStack の状態変更
- ワールド状態変更

入力材料の消費は Vanilla の crafting 処理に任せる。


## 5. 詳細仕様

### 5.1 Shaped recipe

`quality_assembly` は shaped recipe とする。

レシピは `pattern` と `key` を持ち、各記号に対応する材料条件を定義する。

基本挙動は Vanilla の shaped recipe に合わせる。

- 空白文字は空きスロットを意味する。
- pattern に存在する記号は必ず `key` に定義する。
- `key` に未定義の記号を pattern で使用してはならない。
- pattern の有効範囲外に余分な ItemStack が存在する場合は不成立とする。
- Vanilla shaped recipe と同様、左右反転配置を許可する。
- crafting grid 内で pattern が収まる位置へのオフセットを許可する。

### 5.2 材料種別

組み立てレシピで扱う材料条件は、以下の3種類とする。

#### 通常素材

Vanilla の `Ingredient` と同等の条件を使用する。

用途例:

- 糸
- 革
- その他の品質計算対象外素材

通常素材は完成品品質の計算対象にしない。

#### 金属パーツ

金属パーツでは、以下の双方を必須条件とする。

- `part_type`
- `material`

一致条件:

```text
入力ItemStackが金属パーツ
AND
保存されているpart_type == レシピ要求part_type
AND
保存されているmaterial == レシピ要求material
```

品質値はレシピ成立条件に含めない。

たとえば鉄のピッケル用レシピに対して、形状が正しいピッケルヘッドであっても素材が銅なら不成立とする。

#### 非金属パーツ

非金属パーツも以下の双方を必須条件とする。

- `part_type`
- `material`

一致条件:

```text
入力ItemStackが非金属パーツ
AND
保存されているpart_type == レシピ要求part_type
AND
保存されているmaterial == レシピ要求material
```

品質値はレシピ成立条件に含めない。

### 5.3 素材条件

MVPでは、加工済みパーツの素材条件は必須かつ完全一致とする。

以下は許可しない。

```text
material 未指定
material = "*"
material = "any"
任意の金属タグによる代替
任意の木材タグによる代替
```

これにより、既存装備を作る場合に完成品と構成素材の整合性を保証する。

例:

```text
minecraft:iron_pickaxe
→ 要求された金属パーツは鉄素材でなければならない
```

実際の素材IDは既存の Craftbound 素材定義IDを使用すること。

### 5.4 異なる品質のパーツ

以下のような組み合わせを許可する。

```text
金属パーツ品質: 80
非金属パーツ品質: 85
```

両者の `part_type` と `material` がレシピ条件を満たしている限り、品質値が異なっていてもレシピは成立する。

品質値は `matches()` の判定材料に使用しない。

### 5.5 品質計算対象の指定

Recipe は、どの入力が完成品品質へ寄与するかを明示的に保持する。

品質計算対象は原則として加工済み金属パーツ・加工済み非金属パーツとする。

通常素材が何らかの理由で品質情報を持っていた場合でも、自動的に完成品品質へ含めてはならない。

つまり、以下の方式は禁止する。

```text
入力 ItemStack を全部走査
→ QualityStateService で品質が取れたものを全部平均
```

代わりに以下とする。

```text
Recipe上で品質計算対象として定義された入力を抽出
→ 各パーツの品質を既存品質APIから取得
→ 共通品質計算APIへ渡す
```

### 5.6 完成品品質

完成品品質は品質計算対象パーツの算術平均とする。

概念式:

```text
完成品品質
= 品質計算対象パーツの品質合計
  / 品質計算対象パーツ数
```

例:

```text
80 + 85 = 165
165 / 2 = 82.5
→ 完成品品質 82
```

端数処理は **切り捨て** とする。

ただし、Recipe クラス内で平均値や丸めを直接実装してはならない。

共通品質計算窓口へ責務を置く。

想定:

```java
int calculateFinishedQuality(List<Integer> qualities)
```

または既存品質サービスに同等のメソッドを追加する。

丸め方式を将来変更する場合も、この共通窓口のみを変更すればよい構造とする。

### 5.7 品質値の取得

品質値の取得は既存 `QualityStateService` の契約に従う。

Recipe 側で以下を独自定義してはならない。

- 品質NBTキー
- 品質欠損時の補完値
- 品質範囲
- 品質の正規化方法

品質データの欠損時に既存品質サービスが既定品質を返す仕様を持つ場合は、その結果を利用する。

### 5.8 品質計算対象が0件の場合

品質計算対象パーツが0件の場合は、完成品へ明示的な品質値を設定しない。

以下のような処理は禁止する。

```text
対象0件
→ quality = 0 を設定
```

結果 ItemStack は通常の Recipe result として返し、品質未設定状態を維持する。

### 5.9 完成品への品質保存

`assemble(...)` では以下の順序で結果を生成する。

```text
1. result ItemStack を生成
2. Recipe上の品質計算対象スロットを特定
3. 各対象パーツの品質を既存品質APIから取得
4. 品質計算対象が0件なら、そのままresultを返す
5. 共通品質計算APIへ品質一覧を渡す
6. 算出された品質を QualityStateService 経由で result へ保存
7. result を返す
```

Recipe 自身は完成品の攻撃力・採掘速度・耐久値等を直接変更しない。

### 5.10 通常レシピとの共存

Vanilla および既存通常レシピを削除しない。

同じ完成品に以下の2経路が存在してよい。

```text
通常レシピ
→ 既存仕様に基づく固定低品質

quality_assembly
→ 使用パーツから算出した品質
```

通常レシピの固定低品質処理は既存品質システムの責務とし、`QualityAssemblyRecipe` へ複製しない。

### 5.11 組み立て経験値

組み立て成功時の鍛冶師経験値は **0** とする。

以下は実装しない。

```text
品質付きパーツ数 × 1 XP
```

組み立て時に以下を呼び出してはならない。

- Pufferfish's Skills の Experience Source
- `CastingExperienceHook` に類する組み立て用Hook
- ActionBar の経験値獲得メッセージ

組み立てに使用された各パーツについて、鋳造・鍛造・非金属加工などの前工程で既に経験値を評価する。


## 6. データ形式

### 6.1 基本形式

`quality_assembly` JSON は、Vanilla shaped recipe に近い `pattern` / `key` / `result` 形式とする。

概念例:

```json
{
  "type": "craftbound:quality_assembly",
  "pattern": [
    " H ",
    " S ",
    " S "
  ],
  "key": {
    "H": {
      "kind": "metal_part",
      "part_type": "<existing_pickaxe_head_part_type_id>",
      "material": "<existing_iron_material_id>",
      "contributes_to_quality": true
    },
    "S": {
      "kind": "nonmetal_part",
      "part_type": "<existing_handle_part_type_id>",
      "material": "<existing_wood_material_id>",
      "contributes_to_quality": true
    }
  },
  "result": {
    "item": "minecraft:iron_pickaxe",
    "count": 1
  }
}
```

上記の `<...>` は説明用プレースホルダである。

実際のデータファイルでは、既存のパーツ定義・素材定義で使用している正式IDへ置き換えること。

### 6.2 通常素材

通常素材を含む場合の概念例:

```json
{
  "kind": "ingredient",
  "item": "minecraft:string"
}
```

タグを利用する場合は Vanilla `Ingredient` が扱える既存形式へ合わせる。

通常素材に `contributes_to_quality` を設定してはならない。

### 6.3 金属パーツ形式

```json
{
  "kind": "metal_part",
  "part_type": "<part_type_id>",
  "material": "<material_id>",
  "contributes_to_quality": true
}
```

必須:

- `kind`
- `part_type`
- `material`

`contributes_to_quality` は加工済み品質パーツでは `true` とする。

将来の拡張余地を残す場合でも、今回追加する完成品レシピでは品質付き加工パーツを `true` として定義する。

### 6.4 非金属パーツ形式

```json
{
  "kind": "nonmetal_part",
  "part_type": "<part_type_id>",
  "material": "<material_id>",
  "contributes_to_quality": true
}
```

必須:

- `kind`
- `part_type`
- `material`

### 6.5 JSON検証

読み込み時に以下を検証する。

- `pattern` が存在する。
- `pattern` が crafting grid に収まる。
- 各行の幅が整合する。
- 空白以外の pattern 記号が `key` に存在する。
- `key` の記号が1文字である。
- `kind` がサポート対象である。
- `metal_part` に `part_type` が存在する。
- `metal_part` に `material` が存在する。
- `nonmetal_part` に `part_type` が存在する。
- `nonmetal_part` に `material` が存在する。
- 指定されたパーツ種別IDが既存定義に存在する。
- 指定された素材IDが既存定義に存在する。
- `ingredient` に品質寄与指定が存在しない。
- `result.item` が存在する。
- `result.count` が省略された場合は1とする。

不正なレシピはサイレントに別条件へフォールバックせず、データパック読み込み時に原因を特定できるエラーとして扱う。


## 7. 実装対象・変更内容

### 7.1 Step 1: 材料条件モデルと品質計算窓口

最初に、Recipe が既存 ItemStack の意味情報を安全に参照できる基盤を作る。

実装内容:

- `AssemblyIngredient` 相当の共通表現
- 通常 `Ingredient` 用条件
- 金属パーツ用条件
- 非金属パーツ用条件
- `part_type + material` の一致判定
- 完成品品質計算用の共通サービスまたは共通メソッド

品質計算窓口では以下を保証する。

```text
入力: 品質値一覧
出力: 算術平均を切り捨てた整数品質
```

Recipe 側へ丸め処理を持たせない。

### 7.2 Step 2: QualityAssemblyRecipe と Serializer

実装内容:

- `QualityAssemblyRecipe`
- `QualityAssemblyRecipeSerializer`
- Serializer 登録
- `craftbound:quality_assembly` ID の登録
- JSON `pattern` / `key` / `result` の読み込み
- shaped matching
- 左右反転 matching
- result 生成
- 品質計算対象パーツ抽出
- 完成品への品質保存

このStepでは、プレイヤー固有処理を追加しない。

### 7.3 Step 3: 実レシピ追加

最低1件、鉄のピッケルを `quality_assembly` で作成できるレシピを追加する。

レシピでは少なくとも以下を保証する。

- ピッケル用金属パーツである。
- 金属素材が鉄である。
- 要求された非金属パーツ種別である。
- 非金属素材もレシピ指定素材と一致する。
- 両パーツが品質計算対象である。
- 出力が `minecraft:iron_pickaxe` である。

既存 `iron-pickaxe.md` と実際のパーツ定義を参照し、正式な `part_type` / `material` IDを使用する。

### 7.4 Step 4: 既存仕様書の同期

実装と同じ変更で以下のドキュメントを更新する。

#### `blacksmith.md`

- 組み立てXPの記述を削除する。
- 経験値表から完成品組み立てを削除する。
- 組み立ては shaped recipe であることを明記する。
- パーツ種別だけでなく素材条件も必須とすることを明記する。

#### `data-formats.md`

- `craftbound:quality_assembly` の正式JSON形式を追加する。
- `metal_part` / `nonmetal_part` の必須項目を追加する。
- JSON検証条件を追加する。

#### `test-cases.md`

- 組み立てXPを期待するテストを削除・修正する。
- 素材不一致でクラフト不可となるテストを追加する。
- 完成品品質平均のテストを追加する。

#### `iron-pickaxe.md`

既存記述と今回の実装が矛盾する場合のみ修正する。

特に以下を確認する。

- 鉄製パーツの素材条件
- 非金属パーツの素材条件
- shaped pattern
- 完成品品質計算


## 8. テスト仕様

最低限、以下を確認する。

| No. | ケース | 期待結果 |
|---:|---|---|
| 1 | 正しいパーツ種別・素材を正しい shaped 配置で置く | レシピ成立 |
| 2 | 正しい材料を左右反転配置する | レシピ成立 |
| 3 | pattern を crafting grid 内でずらして配置する | Vanilla shaped recipe と同様に成立 |
| 4 | 金属パーツの `part_type` が異なる | 不成立 |
| 5 | 金属パーツ種別は正しいが素材が異なる | 不成立 |
| 6 | 非金属パーツの `part_type` が異なる | 不成立 |
| 7 | 非金属パーツ種別は正しいが素材が異なる | 不成立 |
| 8 | パーツ品質だけが異なる | レシピ成立 |
| 9 | 品質80と85の2パーツを使用 | 完成品品質82 |
| 10 | 品質計算対象外の通常素材を含む | 通常素材は平均から除外 |
| 11 | 品質計算対象0件の `quality_assembly` | 完成品へ品質を明示設定しない |
| 12 | pattern外に余分なアイテムがある | 不成立 |
| 13 | `metal_part` の `part_type` 欠落JSON | 読み込みエラー |
| 14 | `metal_part` の `material` 欠落JSON | 読み込みエラー |
| 15 | `nonmetal_part` の `part_type` 欠落JSON | 読み込みエラー |
| 16 | `nonmetal_part` の `material` 欠落JSON | 読み込みエラー |
| 17 | 存在しないパーツ種別ID | 読み込みエラー |
| 18 | 存在しない素材ID | 読み込みエラー |
| 19 | 正しい `quality_assembly` で鉄ピッケルを作る | `minecraft:iron_pickaxe` を取得 |
| 20 | 銅など別金属のピッケル用パーツで鉄ピッケルレシピを試す | 不成立 |
| 21 | 組み立てを完了する | 鍛冶師XPは増加しない |
| 22 | Vanilla の既存通常レシピを使用する | 既存仕様どおりクラフト可能 |
| 23 | `matches()` を繰り返し呼ぶ | 入力やプレイヤー状態に副作用なし |
| 24 | result preview 更新で `assemble()` が複数回呼ばれる | XP等の副作用なし |

可能であれば、Recipe の JSON パース・matching・品質計算は GameTest だけに依存せず、通常の単体テストとして分離する。


## 9. 完了条件

以下をすべて満たした時点で実装完了とする。

- `craftbound:quality_assembly` が登録されている。
- Vanilla の作業台から `quality_assembly` を使用できる。
- shaped pattern に基づいて成立判定される。
- 金属パーツでは `part_type` と `material` の双方が一致しなければクラフトできない。
- 非金属パーツでも `part_type` と `material` の双方が一致しなければクラフトできない。
- 品質の違いだけではレシピ不成立にならない。
- 品質計算対象パーツのみから完成品品質が算出される。
- 算術平均の端数処理が共通品質計算窓口で切り捨てられる。
- Recipe 自身が平均・丸め方式を直接実装していない。
- 品質計算対象が0件の場合、完成品に品質0を設定しない。
- 完成品品質が既存 `QualityStateService` 等の共通窓口を通して保存される。
- Recipe 内で完成品性能を直接変更していない。
- 組み立て時に鍛冶師経験値を付与しない。
- 組み立て経験値用Hookを追加していない。
- Vanilla の通常クラフトレシピが引き続き利用できる。
- 鉄のピッケル用 `quality_assembly` レシピが1件以上実装されている。
- 鉄のピッケルに対し、異なる金属素材のパーツではクラフトできない。
- `blacksmith.md`、`data-formats.md`、`test-cases.md` が実装内容と同期されている。
- 必要に応じて `iron-pickaxe.md` も同期されている。
- 本仕様書のテストケースを満たしている。
