# Mobドロップ置換基盤（Global Loot Modifier）実装仕様書

## Codex向け最短参照セット

本実装は `develop` ブランチを基準とする。Codexは最初に以下だけを参照し、必要がない限り全リポジトリ探索を行わない。

### 必ず参照するファイル

1. `src/main/java/com/magu1436/craftbound/occupations/foodproducer/loot/FoodProducerLootModifiers.java`
   - 既存の Global Loot Modifier serializer 登録方法をそのまま踏襲する。
2. `src/main/java/com/magu1436/craftbound/occupations/foodproducer/loot/CropHarvestLootModifier.java`
   - `LootModifier`、`codecStart`、`RecordCodecBuilder`、`doApply`、`codec()` の既存実装例として参照する。
3. `src/main/java/com/magu1436/craftbound/Craftbound.java`
   - 新しい共通Loot Modifier登録クラスをMod Event Busへ登録する箇所だけ変更する。

### 必要時のみ参照するファイル

4. `src/main/resources/data/craftbound/loot_modifiers/crop_harvest.json`
   - 既存GLM JSONの配置場所と `type` / `conditions` の形式確認用。変更しない。
5. `src/main/resources/data/forge/loot_modifiers/global_loot_modifiers.json`
   - 現在のGLM一覧の形式確認用。基盤実装だけでは変更しない。
6. `src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java`
   - 後続タスクで実際の置換先アイテムIDを確認するときだけ参照する。基盤実装では変更しない。

### 原則として参照不要

- `docs/occupations/**`
- 鍛冶・錬金等の職業サービス実装
- Screen / Menu / Network関連
- Block / BlockEntity関連
- 他ModのJavaクラス

本基盤はLoot生成結果の変換だけを担当し、職業ロジックやクラフト処理とは接続しない。

## 変更ファイル一覧

### 新規作成

- `src/main/java/com/magu1436/craftbound/loot/CraftboundLootModifiers.java`
- `src/main/java/com/magu1436/craftbound/loot/ReplaceItemLootModifier.java`

### 変更

- `src/main/java/com/magu1436/craftbound/Craftbound.java`
  - `CraftboundLootModifiers` のimportを追加する。
  - `CraftboundLootModifiers.register(modEventBus);` を既存の登録処理群へ1行追加する。

### 今回は変更しない

- `src/main/java/com/magu1436/craftbound/occupations/foodproducer/loot/FoodProducerLootModifiers.java`
- `src/main/java/com/magu1436/craftbound/occupations/foodproducer/loot/CropHarvestLootModifier.java`
- `src/main/resources/data/craftbound/loot_modifiers/crop_harvest.json`
- `src/main/resources/data/forge/loot_modifiers/global_loot_modifiers.json`
- `src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java`

既存FoodProducerのGLMを共通クラスへ移動するリファクタリングは行わない。

# 目的

他ModのMobが完成済みの武器・装備をLoot Tableから直接ドロップする場合に、その完成品をCraftbound上の製作用アイテムへ置換できる共通基盤を実装する。

例：

```text
他Mod Mob死亡
    ↓
他Mod本来のLoot Tableで武器が抽選される
    ↓
CraftboundのReplaceItemLootModifier
    ↓
抽選済み武器を削除
    ↓
武器製作用の素材・コア等を追加
```

Craftbound側で元Modのドロップ確率を再実装しない。元ModのLoot Tableが実際に対象武器を生成した場合だけ置換することで、元Mod側の確率、Looting、難易度条件等の結果をそのまま利用する。

本実装は「他ModのLoot Table JSONそのものを上書きする機能」ではなく、「Loot Tableが生成した結果をForge Global Loot Modifierで置換する機能」とする。

# 確定した仕様

- Forge 1.20.1のGlobal Loot Modifierを使用する。
- 汎用Modifierのserializer IDは `craftbound:replace_item` とする。
- 対象アイテムはリソースIDで指定する。
- 置換先は1種類以上のアイテムを指定できる。
- 対象アイテムが生成済みLootに存在した場合だけ処理する。
- 対象アイテムが存在しない場合はLootを一切変更しない。
- 対象アイテムのドロップ確率をCraftbound側で再抽選しない。
- 対象アイテムと一致する全ItemStackを削除する。
- 削除した対象アイテムの合計個数を `removedCount` とする。
- 各置換先の最終個数は `removedCount × 設定count` とする。
- 置換後の個数が対象Itemの最大スタック数を超える場合は複数ItemStackへ分割する。
- 対象以外のLootは順序を含め可能な限りそのまま維持する。置換アイテムは既存Lootの後ろへ追加してよい。
- 対象武器のNBT、耐久値、エンチャント、カスタム名等は置換先へ引き継がない。
- 他Modへのcompile-time依存は追加しない。
- 他ModのItemクラスやEntityクラスを直接importしない。
- targetとreplacementは `ResourceLocation` としてCodecで読み込み、実行時にForge Item Registryから解決する。
- `conditions` は通常のGLM条件をそのまま使用する。実運用の各定義では原則 `forge:loot_table_id` を指定し、対象MobのLoot Tableへ範囲を限定する。
- 同一のreplacement itemを複数行記述した場合は、それぞれのcountを累積的に追加してよい。
- `replacements` は1件以上を必須とする。
- 各replacementの `count` は1以上64以下とする。
- replacementにtargetと同じリソースIDが含まれる定義は無効扱いとし、Lootを変更しない。
- targetまたはreplacementのItem IDをRegistryから解決できない場合は、その置換処理全体をno-opとする。対象武器だけを消してはならない。

後続タスクで実際の置換定義を追加する場合は、原則として「1 Loot Table + 1 target item」を1ファイルとする。

推奨配置：

```text
src/main/resources/data/craftbound/loot_modifiers/compat/<modid>/<rule_name>.json
```

推奨GLM ID：

```text
craftbound:compat/<modid>/<rule_name>
```

定義例：

```json
{
  "type": "craftbound:replace_item",
  "conditions": [
    {
      "condition": "forge:loot_table_id",
      "loot_table_id": "examplemod:entities/example_boss"
    }
  ],
  "target_item": "examplemod:legendary_sword",
  "replacements": [
    {
      "item": "craftbound:legendary_sword_core",
      "count": 1
    },
    {
      "item": "craftbound:ancient_metal_chunk",
      "count": 3
    }
  ]
}
```

この場合、元Loot Tableが `examplemod:legendary_sword` を1個生成したときだけ、剣を削除し、コア1個と金属片3個を追加する。

# 採用した実装方針

## 1. 共通GLM登録クラスを新設する

新規：

```text
src/main/java/com/magu1436/craftbound/loot/CraftboundLootModifiers.java
```

既存の `FoodProducerLootModifiers.java` と同じ登録方式を使用する。

責務：

- `ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS` 用の `DeferredRegister` を保持する。
- `ReplaceItemLootModifier.CODEC` を `replace_item` で登録する。
- `register(IEventBus)` を公開する。

概念形：

```java
public final class CraftboundLootModifiers {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS = ...;

    public static final RegistryObject<Codec<ReplaceItemLootModifier>> REPLACE_ITEM =
        SERIALIZERS.register("replace_item", () -> ReplaceItemLootModifier.CODEC);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
```

既存 `FoodProducerLootModifiers` は変更・統合しない。

## 2. 汎用置換Modifierを新設する

新規：

```text
src/main/java/com/magu1436/craftbound/loot/ReplaceItemLootModifier.java
```

`CropHarvestLootModifier.java` と同じく `LootModifier` を継承する。

保持データ：

```text
LootItemCondition[] conditions
ResourceLocation targetItemId
List<Replacement> replacements
```

`Replacement` は別ファイルを増やさず、`ReplaceItemLootModifier` 内のrecordとして実装してよい。

```text
Replacement
- ResourceLocation itemId
- int count
```

Codecは次を処理する。

```text
conditions      <- codecStart(instance)
target_item     <- ResourceLocation.CODEC
replacements    <- Replacement.CODEC.listOf()
Replacement.item  <- ResourceLocation.CODEC
Replacement.count <- Codec.intRange(1, 64)
```

`replacements` が空配列の場合はCodecのdecode失敗とする。実装を不必要に複雑化する場合でも、最低限コンストラクタまたは `doApply` 冒頭で空配列を無効としてno-opにする。

## 3. 置換処理は原子的に行う

`doApply` 内で、先に対象を削除してからreplacement解決に失敗する実装は禁止する。

処理順序：

1. `targetItemId` をForge Item Registryから解決する。
2. 解決できなければ元Lootを返す。
3. generatedLoot内の対象ItemStack個数を合計する。この段階では削除しない。
4. 合計が0なら元Lootを返す。
5. 全replacement IDをRegistryから解決する。
6. 1件でも解決不能、またはtargetと同一IDなら元Lootを返す。
7. replacementとして追加するItemStack群を一時リストへ構築する。
8. すべて正常に構築できた後で対象ItemStackをgeneratedLootから削除する。
9. replacement ItemStack群をgeneratedLootへ追加する。
10. generatedLootを返す。

これにより設定不備で「完成武器だけ消えて代替素材が出ない」状態を防ぐ。

## 4. 外部Modの存在をJavaコードへ組み込まない

対象Mod判定に `ModList` や外部Modクラスを使用しない。

```text
ResourceLocation
    ↓
ForgeRegistries.ITEMS
    ↓
存在すれば置換
存在しなければno-op
```

この構造により、互換対象Modが導入されていない環境でもCraftbound本体をロード可能にする。

## 5. 実際の互換定義は後続タスクで追加する

今回のタスクではserializerと汎用Modifierの基盤だけを実装する。

したがって、現在の以下のファイルは変更しない。

```text
src/main/resources/data/forge/loot_modifiers/global_loot_modifiers.json
```

実際の他Mod向け定義を追加するときだけ、既存の

```json
{
  "replace": false,
  "entries": [
    "craftbound:crop_harvest"
  ]
}
```

へ新しいIDを追記する。`replace` は `false` のままとする。

# 採用しなかった方針

## 他ModのLoot Table JSONを丸ごと上書きする

元Mod更新時にLoot Tableの変更を取り込めず、他の互換Modとも競合しやすいため採用しない。

## `LivingDropsEvent` で全Mobドロップを後処理する

通常のLoot Table由来ドロップにはGLMの方がデータ駆動で対象を限定しやすいため、基盤には採用しない。Loot Tableを使用しない特殊Mobや、CraftboundのGLMより後に別GLMが対象武器を追加するケースだけ将来の個別互換候補とする。

## 削除Modifierと追加Modifierを分離する

処理順序や条件のずれにより、武器だけ消える・素材だけ出る状態を作りうるため採用しない。削除と追加は1つの `ReplaceItemLootModifier` 内で原子的に行う。

## Craftbound側で元武器のドロップ確率を再実装する

元Mod側のLoot Table変更、Looting、難易度条件等と乖離するため採用しない。元の武器が実際に生成されたことを置換条件とする。

## `FoodProducerLootModifiers` に汎用Modifierを追加する

Mob装備置換はFoodProducerの責務ではないため採用しない。既存実装は参考にするだけとし、共通パッケージへ独立した登録クラスを置く。

# 関連する既存クラス

| ファイル | 扱い | 用途 |
|---|---|---|
| `src/main/java/com/magu1436/craftbound/Craftbound.java` | 変更 | `CraftboundLootModifiers.register(modEventBus)` を追加 |
| `src/main/java/com/magu1436/craftbound/occupations/foodproducer/loot/FoodProducerLootModifiers.java` | 参照のみ | `DeferredRegister<Codec<? extends IGlobalLootModifier>>` の既存登録パターン |
| `src/main/java/com/magu1436/craftbound/occupations/foodproducer/loot/CropHarvestLootModifier.java` | 参照のみ | `LootModifier`、Codec、`doApply` の既存実装パターン |
| `src/main/resources/data/craftbound/loot_modifiers/crop_harvest.json` | 参照のみ | 既存GLM JSON形式 |
| `src/main/resources/data/forge/loot_modifiers/global_loot_modifiers.json` | 参照のみ | GLMエントリ一覧。基盤実装では変更しない |
| `src/main/java/com/magu1436/craftbound/registry/CraftboundItems.java` | 原則参照不要 | 後続の具体的互換定義で置換先IDを確認する場合のみ参照 |

新規クラス同士の依存は次だけに限定する。

```text
Craftbound.java
    ↓ register
CraftboundLootModifiers
    ↓ codec
ReplaceItemLootModifier
```

`ReplaceItemLootModifier` から鍛冶師、冒険家、錬金術師等の職業クラスへ依存してはならない。

# データフロー

```text
Mob死亡
    ↓
元ModのLoot Table実行
    ↓
元Mod側の確率・Looting・条件を反映したgeneratedLoot生成
    ↓
GLM conditions評価
    ↓
対象Loot Tableでなければ何もしない
    ↓
ReplaceItemLootModifier#doApply
    ↓
generatedLootにtarget_itemが存在するか確認
    ├─ 存在しない → そのまま返す
    └─ 存在する
          ↓
       target個数を合計
          ↓
       replacement IDをすべて解決
          ├─ 解決失敗 → 元Lootをそのまま返す
          └─ 成功
                ↓
             replacement ItemStack群を構築
                ↓
             target ItemStackを削除
                ↓
             replacement ItemStack群を追加
                ↓
             変更済みLootを返す
```

例：

```text
元Loot:
- examplemod:legendary_sword x1
- minecraft:diamond x2

定義:
- target_item = examplemod:legendary_sword
- legendary_sword_core x1
- ancient_metal_chunk x3

結果:
- minecraft:diamond x2
- craftbound:legendary_sword_core x1
- craftbound:ancient_metal_chunk x3
```

# 擬似コード

```text
Mod初期化時:
    既存のCraftbound登録処理と同じMod Event Busを使用する
    CraftboundLootModifiers.register(modEventBus)を呼び出す

CraftboundLootModifiers登録時:
    GLOBAL_LOOT_MODIFIER_SERIALIZERS用DeferredRegisterを登録する
    craftbound:replace_item に ReplaceItemLootModifier.CODEC を登録する

ReplaceItemLootModifier読み込み時:
    通常のGLM conditionsを読み込む
    target_itemをResourceLocationとして読み込む
    replacementsを読み込む
    replacement countは1～64だけ許可する
    replacementsが空なら無効とする

Loot生成後:
    target_itemをItem Registryから解決する
    解決できない場合:
        generatedLootを変更せず返す

    removedCount = 0
    generatedLootを走査する
    target_itemと一致する各ItemStackについて:
        removedCount += stack.count

    removedCount == 0 の場合:
        generatedLootを変更せず返す

    一時replacementリストを作成する

    replacementsを順番に処理する
        replacement.itemをItem Registryから解決する
        解決できない場合:
            generatedLootを変更せず返す

        replacement.item == target_item の場合:
            generatedLootを変更せず返す

        totalCount = removedCount * replacement.count
        Itemの最大スタック数以下になるようItemStackへ分割する
        分割したItemStackを一時replacementリストへ追加する

    ここまで全処理が成功したら:
        generatedLootからtarget_itemと一致するItemStackをすべて削除する
        generatedLootへ一時replacementリストを追加する

    generatedLootを返す
```

# 境界条件・例外

- target itemがLootに存在しない場合はno-opとする。
- target itemのRegistry IDが存在しない場合はno-opとする。
- replacement itemのうち1件でもRegistry IDが存在しない場合は置換全体をno-opとする。
- replacementがtarget自身を指定している場合は置換全体をno-opとする。
- replacement定義が空の場合は無効とする。
- 対象武器が複数スタック生成された場合は全スタックのcountを合計して置換する。
- 置換先ItemStackは各Itemの最大スタック数を超えないよう分割する。
- count乗算は `long` 等の十分な幅で中間計算し、異常なオーバーフローが起きる場合は元Lootを維持する。
- target武器のNBT、耐久、Enchant等はreplacementへコピーしない。
- GLMの適用順序上、CraftboundのModifierより後に別ModのGLMが同じ武器を追加した場合、その後発分は本Modifierでは削除できない。
- Mobが通常のLoot Tableを使わずJavaコードで直接ItemEntityを生成する場合、本基盤では置換できない。
- 上記2ケースは `LivingDropsEvent` 等による個別互換の検討対象とし、基盤へ混在させない。
- クライアント処理、Packet同期、Capabilityは不要とする。
- 専用サーバーでもクライアント専用クラスを参照しない。

# 完了条件

- `CraftboundLootModifiers.java` が新規作成されている。
- `ReplaceItemLootModifier.java` が新規作成されている。
- serializer `craftbound:replace_item` が登録されている。
- `Craftbound.java` から `CraftboundLootModifiers.register(modEventBus)` が呼ばれている。
- 既存 `FoodProducerLootModifiers` と `CropHarvestLootModifier` の動作を変更していない。
- `global_loot_modifiers.json` の既存 `craftbound:crop_harvest` を削除・置換していない。
- `./gradlew compileJava` が成功する。
- 開発環境起動時にGLM serializer登録エラーが発生しない。
- 一時的なローカル検証用GLM定義で、targetが実際にLootへ生成された場合だけtargetが消えreplacementが出ることを確認できる。
- targetが生成されなかった場合にreplacementが出ない。
- target以外の元Lootが維持される。
- replacementを複数指定した場合にすべて追加される。
- targetが複数個生成された場合にreplacement数が比例する。
- 不正なreplacement IDを用いた検証でtargetだけが消失しない。
- 検証用の一時定義はコミット対象へ残さない。

# 今回の対象外

- 実際の他Mod名、Mob ID、Loot Table ID、武器IDの選定
- 実際のCraftbound製作用アイテムの追加
- 個別武器ごとの置換JSON作成
- `global_loot_modifiers.json` への個別互換エントリ追加
- 武器を製作するレシピ・鍛冶工程の追加
- 置換素材のドロップ確率を独自に設定する機能
- Item Tagによる複数武器一括置換
- NBT条件付き置換
- エンチャント・耐久値・品質等をreplacementへ継承する処理
- Loot Tableを使用しないMobの独自ドロップ互換
- 後発GLMによって再追加された武器の最終除去
- `LivingDropsEvent` によるフォールバック基盤
- 既存FoodProducer GLM登録の共通化リファクタリング
- 新しいJUnit / GameTest基盤の導入
