# 目的

Cataclysm の Fusion Anvil による `weapon_fusion` の結果へ、Craftbound の既存品質システムを統合する。

Cataclysm が導入されている環境では、Fusion Anvil の2入力が持つ Craftbound 品質を読み取り、既存の完成品品質計算規則に従って結果 `ItemStack` の品質を決定する。

Cataclysm が導入されていない環境では、この連携処理を適用せず、Craftbound 単体で従来通り起動・動作できることを必須とする。Cataclysm を Craftbound の必須依存にはしない。

今回実装するのは Fusion Anvil と Craftbound 品質システムを接続する基盤のみとする。個別の Cataclysm アイテム、個別レシピ、ドロップ置換、素材追加は扱わない。

Codex は原則として本書に列挙したファイルのみ確認・変更し、パス、API、Cataclysm 3.31 の実装が `develop` と一致しない場合だけ追加探索する。

# 確定した仕様

## 品質計算対象

Cataclysm の以下のレシピ実装を対象とする。

```text
com.github.L_Ender.cataclysm.crafting.WeaponfusionRecipe
```

対象メソッド:

```java
ItemStack assemble(Container container, RegistryAccess registryAccess)
```

Cataclysm 1.20.1 の現行実装では、`assemble` はレシピ結果を `copy()` した後、スロット0の `base` の NBT を結果へコピーして返す。

Craftbound はこの既存処理を置換しない。`assemble` の `RETURN` 後に結果 `ItemStack` を取得し、Craftbound の品質情報だけを再計算・上書きする。

## Fusion 入力

Fusion Anvil の入力は Cataclysm の既存仕様に従う。

```text
slot 0: base
slot 1: addition
```

Craftbound 側では両方の `ItemStack` について `QualityStateService.read` を呼び出す。

品質を持つ入力だけを完成品質の計算対象とする。

| base | addition | 結果品質 |
|---|---|---|
| 品質あり | 品質あり | 2つの品質の整数平均 |
| 品質あり | 品質なし | base の品質 |
| 品質なし | 品質あり | addition の品質 |
| 品質なし | 品質なし | 品質を付与しない |

整数平均は既存 `FinishedItemQualityCalculator.calculate` をそのまま使用する。

例:

```text
base Q80 + addition Q60 -> result Q70
base Q80 + addition 品質なし -> result Q80
base 品質なし + addition Q60 -> result Q60
base 品質なし + addition 品質なし -> result 品質なし
```

端数処理も `FinishedItemQualityCalculator` の既存仕様に従い、整数除算で切り捨てる。

## 結果 Stack の扱い

Cataclysm が生成した結果 `ItemStack` をそのまま使用する。

Craftbound は新しい結果 Stack を作成し直さない。

品質付与には以下を使用する。

```text
QualityStateService.setQuality(result, calculatedQuality)
```

これにより `CraftboundQuality` のみを書き込み、Cataclysm が結果へ設定したその他の NBT を維持する。

Cataclysm の既存 `assemble` が行っている base NBT のコピー処理も変更・キャンセルしない。

## Cataclysm が存在しない場合

Cataclysm は任意依存のままとする。

現行 `src/main/resources/META-INF/mods.toml` には既に以下が存在するため、この依存定義は新規追加しない。

```toml
[[dependencies.${mod_id}]]
    modId="cataclysm"
    mandatory=false
    versionRange="${cataclysm_version_range}"
    ordering="AFTER"
    side="BOTH"
```

現行 `gradle.properties` では以下を使用している。

```text
cataclysm_version=3.31
cataclysm_modrinth_version=C3H0azzB
cataclysm_version_range=[3.31]
```

Mixin は Cataclysm のクラスを Java の class literal で直接参照せず、`@Pseudo` と文字列 target を使用する。

想定形:

```java
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.crafting.WeaponfusionRecipe")
public abstract class WeaponfusionRecipeMixin {
    ...
}
```

これにより Cataclysm の対象クラスが実行環境に存在しない場合、当該 Mixin は適用対象を持たない任意連携として扱う。

`WeaponfusionRecipe` を import して `@Mixin(WeaponfusionRecipe.class)` とする実装は採用しない。実行時に Cataclysm クラスへの不要な直接リンクを作らないためである。

一方、Mixin Annotation Processor が Cataclysm 側 target と `assemble` を開発時に解決・remap できるよう、Cataclysm 本体は `compileOnly` にも追加する。

現行の `runtimeOnly` は残す。

```gradle
compileOnly fg.deobf("maven.modrinth:l_enders-cataclysm:${cataclysm_modrinth_version}")
runtimeOnly fg.deobf("maven.modrinth:l_enders-cataclysm:${cataclysm_modrinth_version}")
```

`compileOnly` は開発・コンパイル時の参照用であり、Craftbound の production runtime の必須依存にはしない。

## Mixin の注入位置

`WeaponfusionRecipe#assemble` の末尾、`RETURN` に `@Inject` する。

既存メソッドを `@Overwrite` しない。

Mixin は結果 Stack を再生成せず、`CallbackInfoReturnable<ItemStack>` から返却値を取得してその Stack を更新する。

想定処理:

```java
@Inject(method = "assemble", at = @At("RETURN"))
private void craftbound$applyFusionQuality(
    Container container,
    RegistryAccess registryAccess,
    CallbackInfoReturnable<ItemStack> callback
) {
    CataclysmFusionQualityService.apply(
        container.getItem(0),
        container.getItem(1),
        callback.getReturnValue()
    );
}
```

`cancellable = true` は不要。返却値を差し替えず、既存の結果 Stack を更新するためである。

## 品質計算サービス

Mixin 内に品質計算ロジックを直接展開せず、新規 `CataclysmFusionQualityService` へ分離する。

配置:

```text
src/main/java/com/magu1436/craftbound/integration/cataclysm/CataclysmFusionQualityService.java
```

責務は次のみに限定する。

1. base / addition の品質を `QualityStateService` で取得する。
2. 存在する品質値だけをリストへ入れる。
3. `FinishedItemQualityCalculator.calculate` で完成品質を計算する。
4. 品質が計算できた場合だけ `QualityStateService.setQuality` で result へ書き込む。

Cataclysm の Item、Recipe、Registry、Fusion Anvil Block などへ直接依存させない。

API は以下程度とする。

```java
public final class CataclysmFusionQualityService {
    private CataclysmFusionQualityService() {}

    public static void apply(ItemStack base, ItemStack addition, ItemStack result) {
        ...
    }
}
```

このクラスは Minecraft の `ItemStack` と Craftbound の品質クラスだけを使用する。

# 採用した実装方針

1. Cataclysm の `WeaponfusionRecipe#assemble` の `RETURN` へ Mixin で後処理を追加する。
2. Cataclysm の既存 Fusion Anvil、RecipeType、RecipeSerializer、Menu、BlockEntity は変更しない。
3. Cataclysm が生成した結果 Stack と既存 NBT を維持し、`CraftboundQuality` だけを後段で補正する。
4. base と addition のうち、実際に Craftbound 品質を持つ入力だけを品質計算へ参加させる。
5. 完成品質計算には既存 `FinishedItemQualityCalculator` を再利用する。
6. 品質読み書きには既存 `QualityStateService` を再利用する。
7. Cataclysm 固有処理は `integration/cataclysm` と `mixin/integration/cataclysm` に隔離する。
8. Cataclysm target は `@Pseudo` + 文字列 target とし、Cataclysm が存在しない環境で Craftbound の起動を妨げない。
9. `build.gradle` へ Cataclysm の `compileOnly` を追加し、既存 `runtimeOnly` と任意依存 metadata を維持する。
10. `Craftbound.java` に Fusion Anvil 用の `ModList.get().isLoaded("cataclysm")` 分岐は追加しない。Mixin の適用可否は Mixin 側で完結させる。

# 採用しなかった方針

- Cataclysm の `WeaponfusionRecipe` を置き換える: Cataclysm 本来の NBT 継承や将来の処理を失うため。
- `@Overwrite` で `assemble` 全体を再実装する: 他 Mod との競合範囲が大きく、Cataclysm 更新への追従コストも高いため。
- Fusion Anvil の Menu / BlockEntity へ介入する: 品質決定はレシピ結果生成時だけで完結し、GUI・進行処理への変更が不要なため。
- base の品質だけをコピーする: addition が品質を持つ場合にその品質が欠落するため。
- 品質計算専用の新しい平均計算ロジックを作る: `FinishedItemQualityCalculator` と仕様が重複するため。
- `Craftbound.java` の通常 Mod 初期化時に Cataclysm Mixin を登録する: Mixin は通常の Mod 初期化より前段で target class を変換するため、この責務を Mod コンストラクタへ持ち込まない。
- Cataclysm を `implementation` または mandatory dependency に変更する: Craftbound 単体起動を維持できなくなるため。
- Cataclysm のクラスを `@Mixin(WeaponfusionRecipe.class)` と直接参照する: 任意依存連携で不要な runtime class link を避けるため。

# 関連する既存クラス

Codex は以下を優先して参照する。

## Craftbound: 品質状態

```text
src/main/java/com/magu1436/craftbound/common/quality/QualityState.java
src/main/java/com/magu1436/craftbound/common/quality/QualityStateService.java
src/main/java/com/magu1436/craftbound/common/quality/QualityStateCodec.java
```

特に `QualityStateService` の以下を再利用する。

```text
read(ItemStack)
setQuality(ItemStack, int)
```

`QualityStateCodec` では品質を ItemStack NBT の以下へ保存している。

```text
CraftboundQuality
```

新しい NBT key は作らない。

## Craftbound: 完成品品質計算

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/FinishedItemQualityCalculator.java
```

既存 `calculate(List<Integer>)` をそのまま使用する。

品質平均ロジックを複製しない。

既存利用例として以下を参照する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipe.java
```

`QualityAssemblyRecipe#assemble` は、品質に参加する入力の `QualityState` を読み取り、`FinishedItemQualityCalculator` で平均し、`QualityStateService.setQuality` で結果へ書き込んでいる。

今回の Fusion 品質計算もこの処理順序へ合わせる。

## Craftbound: Mixin 実装例

```text
src/main/java/com/magu1436/craftbound/mixin/ItemStackQualityPerformanceMixin.java
src/main/resources/craftbound.mixins.json
```

`ItemStackQualityPerformanceMixin` を `@Inject(... at = @At("RETURN"))` の記述例として参照する。

現行 `craftbound.mixins.json` は以下の設定を持つ。

```text
package = com.magu1436.craftbound.mixin
required = true
minVersion = 0.8
compatibilityLevel = JAVA_17
refmap = craftbound.refmap.json
```

今回、新規 Mixin を以下の相対パスで `mixins` へ追加する。

```text
integration.cataclysm.WeaponfusionRecipeMixin
```

## Craftbound: 任意 Mod 連携の既存構成

```text
src/main/java/com/magu1436/craftbound/integration/diet/FoodProducerDietIntegration.java
src/main/java/com/magu1436/craftbound/Craftbound.java
src/main/resources/META-INF/mods.toml
```

`integration/<modid>` というパッケージ構成を今回も踏襲する。

Diet 連携では通常イベント登録を `ModList.get().isLoaded("diet")` で分岐しているが、今回の Mixin 連携では同じ登録方式を使用しない。参考にするのは任意 Mod 連携のパッケージ分離方針のみ。

## Craftbound: ビルド・依存関係

```text
build.gradle
gradle.properties
src/main/resources/META-INF/mods.toml
```

現行 `build.gradle` には MixinGradle と Mixin Annotation Processor が設定済みである。

```text
id 'org.spongepowered.mixin' version '0.7.38'
annotationProcessor "org.spongepowered:mixin:0.8.5:processor"
```

また Cataclysm 3.31 は既に development runtime 用に `runtimeOnly` 登録されている。

変更は Cataclysm の同一 artifact を `compileOnly` に追加することだけとし、既存 `runtimeOnly`、Lionfish API、Curios の runtime 設定は削除・変更しない。

`gradle.properties` の Cataclysm version / Modrinth version ID は変更しない。

`mods.toml` の Cataclysm `mandatory=false` も変更しない。

## Cataclysm: 参照対象

外部リポジトリ:

```text
https://github.com/lender544/new1.20.1
```

優先参照ファイル:

```text
src/main/java/com/github/L_Ender/cataclysm/crafting/WeaponfusionRecipe.java
```

確認すべき箇所は `assemble(Container, RegistryAccess)` のみでよい。

現行実装の要点:

```text
result.copy()
    ↓
slot 0(base) の NBT を copy
    ↓
結果 ItemStack を return
```

今回、Cataclysm 側の Fusion Anvil Block、Menu、BlockEntity、RecipeSerializer、個別 recipe JSON を追加探索する必要はない。

## 新規追加するクラス

```text
src/main/java/com/magu1436/craftbound/integration/cataclysm/CataclysmFusionQualityService.java
src/main/java/com/magu1436/craftbound/mixin/integration/cataclysm/WeaponfusionRecipeMixin.java
```

テストを追加する場合:

```text
src/test/java/com/magu1436/craftbound/integration/cataclysm/CataclysmFusionQualityServiceTest.java
```

## 変更する既存ファイル

```text
build.gradle
src/main/resources/craftbound.mixins.json
```

## 原則変更しないファイル

```text
src/main/java/com/magu1436/craftbound/Craftbound.java
src/main/java/com/magu1436/craftbound/common/quality/QualityState.java
src/main/java/com/magu1436/craftbound/common/quality/QualityStateService.java
src/main/java/com/magu1436/craftbound/common/quality/QualityStateCodec.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/FinishedItemQualityCalculator.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/QualityAssemblyRecipe.java
gradle.properties
src/main/resources/META-INF/mods.toml
```

既存 API では要件を満たせないことが確認された場合だけ変更を検討する。

# データフロー

Cataclysm 導入時:

```text
Fusion Anvil
  ↓
Cataclysm WeaponfusionRecipe#assemble
  ↓
Cataclysm が result.copy() を生成
  ↓
Cataclysm が base の NBT を result へコピー
  ↓
RETURN
  ↓
WeaponfusionRecipeMixin
  ↓
base ItemStack ─────┐
                    ├─ QualityStateService.read
addition ItemStack ─┘
  ↓
存在する品質値だけ収集
  ↓
FinishedItemQualityCalculator.calculate
  ↓
品質あり
  ↓
QualityStateService.setQuality(result, quality)
  ↓
Cataclysm が生成した result をそのまま返す
```

Cataclysm 未導入時:

```text
WeaponfusionRecipe target class が存在しない
  ↓
@Pseudo target を適用しない
  ↓
Craftbound の通常ロードを継続
  ↓
品質システム、通常 Mixin、その他機能へ影響なし
```

# 擬似コード

```text
CataclysmFusionQualityService.apply(base, addition, result):
    result が empty なら終了する

    qualities = 空のリスト

    base の品質を QualityStateService.read で取得する
    品質が存在するなら qualities に追加する

    addition の品質を QualityStateService.read で取得する
    品質が存在するなら qualities に追加する

    quality = FinishedItemQualityCalculator.calculate(qualities)

    quality が存在しないなら終了する

    QualityStateService.setQuality(result, quality) を実行する
```

```text
WeaponfusionRecipeMixin:
    @Pseudo を付与する
    target は文字列で WeaponfusionRecipe を指定する

    assemble の RETURN に Inject する

    callback の returnValue を取得する
    container slot 0 を base として取得する
    container slot 1 を addition として取得する

    CataclysmFusionQualityService.apply(base, addition, result) を呼ぶ

    returnValue の差し替えや cancel は行わない
```

```text
build.gradle:
    Cataclysm の既存 runtimeOnly を維持する
    同じ artifact を compileOnly に追加する
    mandatory dependency へ変更しない
```

```text
craftbound.mixins.json:
    mixins 配列へ以下を追加する

    integration.cataclysm.WeaponfusionRecipeMixin
```

## 推奨テスト

```text
CataclysmFusionQualityServiceTest:
    base Q80, addition Q60 -> result Q70
    base Q80, addition 品質なし -> result Q80
    base 品質なし, addition Q60 -> result Q60
    両方品質なし -> result に品質を追加しない
    result に既存の非品質 NBT がある -> 品質適用後もその NBT が残る
```

`FinishedItemQualityCalculator` 自体の平均値・切り捨てテストは既存 `FinishedItemQualityCalculatorTest` が担当するため、同じロジックを重複テストしなくてよい。

# 境界条件・例外

- Cataclysm がインストールされていない状態で Craftbound が起動失敗してはならない。
- Cataclysm がない状態で `NoClassDefFoundError: com.github.L_Ender.cataclysm...` が発生してはならない。
- 新規 service クラスから Cataclysm API を import しない。
- Mixin target は class literal ではなく文字列 target とする。
- Mixin は `@Pseudo` を付与する。
- Cataclysm 3.31 が存在する場合だけ `WeaponfusionRecipe` へ注入されること。
- Cataclysm の `assemble` が生成した結果 Stack を別 Stack へ差し替えない。
- Cataclysm が base からコピーした NBT、Enchantments、Damage、その他独自 NBT を Craftbound 側で削除しない。
- `QualityStateService.setQuality` 以外の方法で result root tag 全体を `setTag` しない。
- base と addition の両方に品質がある場合、base の品質をそのまま残さず、2値の整数平均へ上書きする。
- 片方だけ品質を持つ場合、品質のない入力を 0 として平均しない。
- 両方品質なしの場合、Craftbound 側から新しい品質を生成しない。
- 無効な `CraftboundQuality` は既存 `QualityStateService.read` / `QualityStateCodec` の挙動に従う。今回独自の修復ロジックを追加しない。
- result が empty の場合は何も書き込まず終了する。
- `FinishedItemQualityCalculator` の有効品質範囲・整数平均仕様を変更しない。
- Cataclysm の個別 recipe ID や result Item ID による条件分岐を実装しない。すべての `WeaponfusionRecipe` に共通適用する。
- client 専用処理にしない。Recipe の結果計算に関わるため common Mixin とする。
- Fusion Anvil の表示、アニメーション、進行時間、GUI 同期には介入しない。
- Cataclysm の version property、optional dependency metadata、runtime libraries を不要に変更しない。

# 完了条件

- `CataclysmFusionQualityService` が追加され、base / addition の品質から既存ルールで結果品質を算出できる。
- `WeaponfusionRecipeMixin` が `WeaponfusionRecipe#assemble` の `RETURN` に注入されている。
- Mixin が Cataclysm の結果 Stack を再生成・キャンセルせず、品質だけを後処理する。
- base Q80 + addition Q60 の Fusion 結果が Q70 になる。
- base Q80 + addition 品質なしの Fusion 結果が Q80 になる。
- base 品質なし + addition Q60 の Fusion 結果が Q60 になる。
- 両入力が品質なしの場合、Craftbound 品質が新規付与されない。
- Cataclysm が生成した非品質 NBT が品質計算後も維持される。
- `FinishedItemQualityCalculator` と `QualityStateService` が再利用され、新しい品質保存形式・平均計算ロジックが追加されていない。
- `build.gradle` に Cataclysm の `compileOnly` が追加され、既存 `runtimeOnly` が維持されている。
- `mods.toml` の Cataclysm dependency が `mandatory=false` のままである。
- Cataclysm 3.31 導入環境で Craftbound が起動し、Fusion 品質処理が適用される。
- Cataclysm 未導入環境でも Craftbound が正常起動する。
- Cataclysm 未導入環境で Cataclysm クラスを原因とする class loading error が発生しない。
- `./gradlew test` が成功する。
- 実装後、`develop` 単体で既存の品質付き通常組み立て処理が壊れていない。

# 今回の対象外

- Cataclysm の個別武器・ツール・防具に対する対応
- Cataclysm の個別 Fusion recipe JSON の追加・変更
- Fusion recipe の材料条件変更
- Cataclysm の Mob ドロップ変更
- Global Loot Modifier の追加・変更
- Cataclysm 向けの Craftbound 素材・金属・パーツ・Core の追加
- Fusion Anvil 自体の GUI、Block、BlockEntity、Menu、レンダリング、アニメーション変更
- Fusion 成功時の鍛冶経験値付与
- Fusion に品質以外の Craftbound 状態を引き継ぐ仕組み
- 品質による性能補正ロジックの追加・変更
- `FinishedItemQualityCalculator` の計算式変更
- `QualityState` / `QualityStateCodec` のデータ形式変更
- Cataclysm 3.31 以外への互換保証
