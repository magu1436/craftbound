# 品質性能反映システム 実装仕様書

# 目的

Craftbound の鍛冶師システムで保持している `0～100` の品質値を、完成した武器・防具・ツールの実際のゲーム性能へ反映する。

本実装では、既存の `QualityStateService` が保持する ItemStack 単位の品質状態を利用し、以下の性能を品質に応じて変更する。

- 攻撃力
- 防御力
- 防具強度
- 採掘速度
- 最大耐久値
- 遠距離武器から発生した Projectile のダメージ

ただし、品質性能補正を Minecraft / 他 MOD の全装備へ無条件に適用してはならない。

**品質性能補正の対象は、現在ロードされている Craftbound の `QualityAssemblyRecipe` から完成品として生成可能な Item のみに限定する。**

通常レシピ等から得た同一 Item が明示的な `CraftboundQuality` を持たない場合だけ、品質対象品として既定品質 `30` を使用する。

本仕様は `develop` ブランチの 2026-08-17 時点の構成を前提とする。

---

# 実装開始時の読み込み範囲

Codex は最初にリポジトリ全体を探索しないこと。

まず以下だけを読む。

## 必須で読む既存ファイル

```text
docs/occupations/blacksmith/
    blacksmith.md
    data-formats.md

docs/occupations/blacksmith/implementation/
    quality-state-implementation-spec.md
    quality_assembly_implementation_spec.md
    quality_tier_display_implementation_spec.md

src/main/java/com/magu1436/craftbound/common/quality/
    QualityState.java
    QualityStateCodec.java
    QualityStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/
    QualityAssemblyRecipe.java
    QualityAssemblyRecipeValidationListener.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/data/
    BlacksmithQualityTierDefinitions.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/client/
    BlacksmithQualityTooltipEvents.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/event/
    BlacksmithQualityTierSyncEvents.java

src/main/java/com/magu1436/craftbound/event/
    CraftboundDataReloadEventHandler.java

src/main/java/com/magu1436/craftbound/network/
    CraftboundNetwork.java

src/main/java/com/magu1436/craftbound/network/packet/
    BlacksmithQualityTierSyncPacket.java

src/main/java/com/magu1436/craftbound/mixin/
    ItemStackToolCareMixin.java

src/main/resources/
    craftbound.mixins.json

src/main/resources/data/craftbound/blacksmith/quality/
    tiers.json
```

## Forge 1.20.1 側で確認するクラス

実装中にメソッドシグネチャ確認が必要な場合だけ、Forge 1.20.1 の以下を参照する。

```text
net.minecraftforge.event.ItemAttributeModifierEvent
net.minecraftforge.event.entity.player.ArrowLooseEvent
net.minecraftforge.event.entity.living.LivingGetProjectileEvent
net.minecraftforge.event.entity.living.LivingHurtEvent
net.minecraftforge.client.event.RecipesUpdatedEvent

net.minecraft.world.item.ItemStack
net.minecraft.world.item.Item
net.minecraft.world.entity.projectile.Projectile
```

特に `ItemAttributeModifierEvent` では `getModifiers()` ではなく、補正元として `getOriginalModifiers()` を使用する。

## 原則として読まないもの

以下は本実装に必要なシンボルが見つからない場合を除き読まない。

- 他職業のコード
- 鍛造・鋳造・細工の GUI 実装
- 建築家の解体速度実装
- Git 履歴
- 他ブランチ
- 連携 MOD の全コード
- 無関係な Block / BlockEntity / Menu / Screen
- 食料品質システム

他 MOD の互換性確認が必要な場合も、対象武器クラスと Projectile クラスだけを限定して読む。

---

# 確定した仕様

## 1. 品質状態と性能反映を分離する

既存の以下は変更しない。

```text
src/main/java/com/magu1436/craftbound/common/quality/
    QualityState.java
    QualityStateCodec.java
    QualityStateService.java
```

`QualityStateService` は引き続き以下だけを担当する。

```text
品質状態の読取
品質状態の保存
品質状態のコピー
```

以下を `QualityStateService` へ追加してはならない。

```text
品質対象判定
既定品質30の補完
性能倍率計算
Attribute補正
採掘速度補正
最大耐久値補正
Projectileダメージ補正
```

これらは鍛冶師品質性能システム側へ置く。

---

## 2. 品質性能対象は QualityAssemblyRecipe から自動導出する

全 Vanilla 装備、全 MOD 装備を自動で品質対象にしてはならない。

品質対象 Item は以下とする。

```text
現在 RecipeManager にロードされている
QualityAssemblyRecipe
    ↓
各 Recipe の result Item
    ↓
品質性能対象 Item 集合
```

例:

```text
QualityAssemblyRecipe -> minecraft:iron_pickaxe
QualityAssemblyRecipe -> minecraft:iron_sword
QualityAssemblyRecipe -> cataclysm:some_weapon

QualityTargetRegistry:
    minecraft:iron_pickaxe
    minecraft:iron_sword
    cataclysm:some_weapon
```

Craftbound と無関係な MOD 装備は、攻撃力・耐久値等を持っていても `QualityAssemblyRecipe` の出力でなければ品質性能補正を受けない。

品質対象を別の item ID 一覧 JSON で二重管理してはならない。

`QualityAssemblyRecipe` は現在 `CraftingRecipe` として実装され、専用 `RecipeType` を持たないため、以下の方式で抽出する。

```text
RecipeManager
    .getAllRecipesFor(RecipeType.CRAFTING)
    .stream()
    .filter(QualityAssemblyRecipe.class::isInstance)
```

フォルダ名や Recipe ID の文字列 prefix から判定しない。

---

## 3. 品質対象集合はリロード時にキャッシュする

ゲーム中の性能問い合わせごとに RecipeManager を探索してはならない。

以下を新設する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/quality/
    QualityTargetRegistry.java
```

責務:

```text
QualityAssemblyRecipe の result Item を Set<Item> として保持
対象判定を O(1) で返す
リロード時に集合全体を atomic に置換する
```

想定 API:

```java
public final class QualityTargetRegistry {

    public static void rebuild(
        RecipeManager recipeManager
    );

    public static boolean contains(
        ItemStack stack
    );

    public static boolean contains(
        Item item
    );
}
```

内部状態は変更途中の集合を他スレッドへ公開しない。

例:

```java
private static volatile Set<Item> targets = Set.of();
```

`HashSet` を構築してから `Set.copyOf(...)` で一括置換する。

### サーバー側の再構築

既存の

```text
QualityAssemblyRecipeValidationListener
```

で全 `QualityAssemblyRecipe` の検証が成功した後に `QualityTargetRegistry.rebuild(recipeManager)` を呼ぶ。

検証失敗時は target 集合を新しい不正データで更新しない。

### クライアント側の再構築

以下の client-only event handler を新設する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/client/
    BlacksmithQualityTargetRecipeEvents.java
```

Forge `RecipesUpdatedEvent` から同期済み `RecipeManager` を受け取り、

```text
QualityTargetRegistry.rebuild(event.getRecipeManager())
```

を実行する。

品質対象 Item ID を独自 packet で二重同期する実装は初期実装では行わない。

サーバーから同期された RecipeManager をクライアント側の正とする。

---

## 4. 品質値の解決

以下を新設する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/quality/
    BlacksmithQualityResolver.java
```

性能反映用の解決規則:

```text
stack が空
    → empty

QualityTargetRegistry の対象外
    → empty

明示 QualityState あり
    → 保存品質

明示 QualityState なし
    → default_quality
```

想定 API:

```java
public static OptionalInt resolveForPerformance(
    ItemStack stack
);
```

既定値はハードコードされた `30` を直接参照せず、後述の品質性能定義から取得する。

### ツールチップ用の解決規則

中間パーツは `QualityAssemblyRecipe` の完成品ではないが、既存仕様上は明示品質を表示する必要がある。

そのためツールチップでは次の順序とする。

```text
明示 QualityState あり
    → その品質を表示
    ※ QualityTargetRegistry 対象外の中間パーツでも表示する

明示 QualityState なし
    AND QualityTargetRegistry 対象
    → default_quality を表示

それ以外
    → 品質表示なし
```

`BlacksmithQualityTooltipEvents` をこの規則へ変更する。

性能用 Resolver とツールチップ用 Resolver を混同しない。

---

## 5. 品質性能倍率

基本式:

```text
性能倍率 = 0.7 + 品質 × 0.006
```

初期値:

```text
品質 0   -> 0.70
品質 30  -> 0.88
品質 50  -> 1.00
品質 80  -> 1.18
品質 100 -> 1.30
```

以下を品質性能補正対象とする。

```text
attack_damage
projectile_damage
armor
armor_toughness
mining_speed
max_durability
```

今回 `work_speed` の汎用 Hook は実装しない。

---

## 6. 品質性能定義をデータ化する

現行実装では品質表示段階が

```text
data/craftbound/blacksmith/quality/tiers.json
```

へ分離済みである。

この構成を戻して単一 `quality.json` に統合しない。

性能設定は以下へ追加する。

```text
src/main/resources/data/craftbound/blacksmith/quality/performance.json
```

初期値:

```json
{
  "schema_version": 1,
  "default_quality": 30,
  "modifiers": {
    "attack_damage": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    },
    "projectile_damage": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    },
    "armor": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006,
      "round_to": 0.5
    },
    "armor_toughness": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006,
      "round_to": 0.5
    },
    "mining_speed": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006
    },
    "max_durability": {
      "evaluator": "craftbound:linear_multiplier",
      "base": 0.7,
      "per_quality": 0.006,
      "rounding": "round",
      "preserve_damage_ratio": true
    }
  }
}
```

任意の数式文字列を実行しない。

MVP で evaluator は

```text
craftbound:linear_multiplier
```

だけを実装する。

未登録 evaluator、未知 modifier、NaN、Infinity、不正 `default_quality`、0 以下の `round_to` は定義エラーとする。

リロード失敗時は直前の有効定義を維持する。

---

## 7. 品質性能定義のクラス

以下を新設する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/quality/
    QualityPerformanceType.java
    QualityPerformanceRule.java
    QualityPerformanceService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/data/
    BlacksmithQualityPerformanceDefinitions.java
```

推奨 enum:

```java
public enum QualityPerformanceType {
    ATTACK_DAMAGE,
    PROJECTILE_DAMAGE,
    ARMOR,
    ARMOR_TOUGHNESS,
    MINING_SPEED,
    MAX_DURABILITY
}
```

`QualityPerformanceService` は純粋計算を担当する。

想定 API:

```java
public static double multiplier(
    QualityPerformanceType type,
    int quality
);

public static double apply(
    QualityPerformanceType type,
    double original,
    int quality
);

public static float apply(
    QualityPerformanceType type,
    float original,
    int quality
);

public static int applyMaxDurability(
    int original,
    int quality
);
```

Hot path で JSON parse を実行しない。

---

## 8. 品質性能定義の client sync

攻撃力 tooltip、採掘速度、耐久値表示はクライアントでも同じ性能定義を必要とする。

以下を追加する。

```text
src/main/java/com/magu1436/craftbound/network/packet/
    BlacksmithQualityPerformanceSyncPacket.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/event/
    BlacksmithQualityPerformanceSyncEvents.java
```

既存

```text
BlacksmithQualityTierSyncEvents
BlacksmithQualityTierSyncPacket
CraftboundNetwork
```

を実装パターンとして再利用する。

`OnDatapackSyncEvent` で現在の performance 定義を対象プレイヤーへ送る。

Packet 受信側は JSON 文字列を再 parse せず、検証済みの構造化値を復元して runtime definition を置換する。

`CraftboundNetwork.PROTOCOL_VERSION` は packet 構成変更に合わせて更新する。

---

# 採用した実装方針

## 1. 攻撃力・防御力・防具強度

Forge `ItemAttributeModifierEvent` を使用する。

Mixin で Player の最終 Attribute を変更しない。

対象 Attribute:

```text
Attributes.ATTACK_DAMAGE
Attributes.ARMOR
Attributes.ARMOR_TOUGHNESS
```

対象 `AttributeModifier.Operation`:

```text
ADDITION のみ
```

以下は変更しない。

```text
ATTACK_SPEED
KNOCKBACK
KNOCKBACK_RESISTANCE
MOVEMENT_SPEED
MULTIPLY_BASE modifier
MULTIPLY_TOTAL modifier
その他の Attribute
```

### 元値

必ず

```java
event.getOriginalModifiers()
```

を元値として使用する。

他 listener による変更後の `event.getModifiers()` を補正元にしない。

### 置換方法

対象 Attribute の `ADDITION` modifier を収集し、合計を Item の対象性能として扱う。

```text
originalTotal = Σ ADDITION amount
adjustedTotal = QualityPerformanceService.apply(...)
```

`armor` と `armor_toughness` は最終合計を 0.5 単位へ丸める。

複数 modifier が存在する場合は、合計が `adjustedTotal` になるよう各 modifier を比例配分して置換する。

元 modifier の以下を維持する。

```text
UUID
name
Operation
```

最後の modifier へ丸め誤差の residual を寄せ、置換後合計が `adjustedTotal` と一致するようにする。

`originalTotal == 0` の場合は変更しない。

品質用の新しい `MULTIPLY_TOTAL` modifier を Player へ追加する方式は採用しない。

---

## 2. 採掘速度

品質は ItemStack が返す基礎採掘速度へ適用する。

`PlayerEvent.BreakSpeed` では実装しない。

理由:

```text
ItemStack 基礎採掘速度
    ↓
Efficiency 等
    ↓
Player / Potion / 環境補正
    ↓
BreakSpeed
```

品質は先頭の ItemStack 基礎速度へ作用させる必要がある。

以下を Mixin する。

```text
ItemStack#getDestroySpeed(BlockState)
```

RETURN の元値を利用する。

```text
originalSpeed > 1.0
    → mining_speed 品質倍率を適用

originalSpeed <= 1.0
    → 変更しない
```

Pickaxe / Axe / Shovel / Sword 等のクラス判定は行わない。

品質対象 Item が、その BlockState に対して実際に高速な destroy speed を返した場合だけ自動補正する。

Mixin 内から `stack.getDestroySpeed(state)` を再呼び出してはならない。

---

## 3. 最大耐久値

以下を Mixin する。

```text
ItemStack#getMaxDamage()
```

RETURN の元値を使用する。

```text
originalMaxDamage > 0
    → max_durability 品質倍率を適用して整数 round

originalMaxDamage <= 0
    → 変更しない
```

Item / 他 MOD が行った ItemStack 依存の最大耐久値計算を先に実行させ、その返値だけを Craftbound が補正する。

Mixin 内から `stack.getMaxDamage()` を再呼び出してはならない。

### 標準耐久バー

Forge 1.20.1 の `Item#getBarWidth(ItemStack)` / `Item#getBarColor(ItemStack)` は Item 側の `getMaxDamage(stack)` を直接参照する経路を持つため、`ItemStack#getMaxDamage()` の RETURN 補正だけでは標準バーと差が出ないことを確認する。

差が出る場合は以下を追加する。

```text
src/main/java/com/magu1436/craftbound/mixin/
    ItemQualityDurabilityBarMixin.java
```

Vanilla `Item#getBarWidth` / `Item#getBarColor` 内の最大耐久値取得だけを `stack.getMaxDamage()` へ redirect し、既存計算式を再実装しない。

サブクラスが独自 `getBarWidth` / `getBarColor` を override している場合は、その独自バーを強制上書きしない。

### 品質変更時の損傷割合

完成済み品質対象 Item の品質値を後から変更する API を用意する場合は、

```text
oldRatio = oldDamage / oldMaxDamage
newDamage = round(newMaxDamage * oldRatio)
```

を使用する。

0～newMaxDamage の範囲へ clamp する。

既存 `QualityStateService#setQuality` 自体の責務は変更しない。

必要なら `QualityPerformanceService` または専用 helper に

```java
setQualityPreservingDamageRatio(...)
```

を追加し、完成済み品質対象 Item の将来の品質変更ではその API を使用する。

現在の `quality_assembly` は新規 `ItemStack` を生成するため、組み立て時は damage 0 のままでよい。

---

## 4. 遠距離武器

遠距離武器の Projectile ダメージは `Attributes.ATTACK_DAMAGE` として扱わない。

Bow / Crossbow / MOD 遠距離武器が生成した Projectile に、

```text
「どの品質の武器から発射されたか」
```

を引き継ぎ、Projectile が実際に与えようとしたダメージの最終値へ品質倍率を掛ける。

これにより、Craftbound が各 MOD の独自 Projectile 基礎ダメージ値を知らなくてもよい。

### 適用順序

遠距離のみ以下とする。

```text
Vanilla / MOD が Projectile ダメージを計算
Power 等のエンチャント補正
MOD 固有補正
弾薬固有補正
    ↓
LivingHurtEvent に到達した amount
    ↓
projectile_damage 品質倍率
```

遠距離については「基礎ダメージだけを先に品質補正」しない。

MOD 互換性を優先し、Projectile が最終的に生成した直接命中ダメージへ倍率を適用する。

---

# 遠距離品質のデータフロー

```text
品質対象の Bow / Crossbow / MOD ranged weapon
    ↓
ArrowLooseEvent
または
LivingGetProjectileEvent
    ↓
発射元 Player + 品質 + server gameTime を一時記録
    ↓
同 tick に Projectile が EntityJoinLevelEvent
    ↓
Projectile#getPersistentData()
へ ranged quality を保存
    ↓
Projectile が飛翔
    ↓
Projectile#onHit(...)
    ↓
QualityProjectileImpactContext に現在 Projectile を push
    ↓
MOD / Vanilla の onHitEntity(...)
    ↓
target.hurt(...)
    ↓
LivingHurtEvent
    ↓
Projectile の保存品質を取得
    ↓
event.amount × projectile_damage multiplier
    ↓
Projectile#onHit RETURN
    ↓
context pop
```

---

# 遠距離実装クラス

以下を新設する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/quality/projectile/
    QualityProjectileStateService.java
    QualityProjectileFiringContext.java
    QualityProjectileImpactContext.java
```

### QualityProjectileStateService

Projectile の Forge persistent data を使用する。

推奨 key:

```text
CraftboundRangedQuality
```

保存値は `0～100` の整数だけとする。

ItemStack の `CraftboundQuality` root を Entity へそのままコピーしない。

### QualityProjectileFiringContext

サーバー側だけで使用する。

保持内容:

```text
shooter UUID
quality
server gameTime
```

`ArrowLooseEvent` と `LivingGetProjectileEvent` で更新する。

同じ tick に owner が一致する Projectile が Join した場合だけ品質を引き継ぐ。

複数 Projectile を同時発射する武器に対応するため、1 Projectile へ転写した時点で即削除しない。

過去 tick の context は無効として扱う。

Map が無制限に UUID を保持し続けないよう、古い entry の pruning または logout cleanup を実装する。

### QualityProjectileImpactContext

`ThreadLocal<Deque<Projectile>>` 相当とする。

単一変数ではなく stack 形式を推奨する。

API 例:

```java
public static void push(Projectile projectile);
public static Optional<Projectile> current();
public static void pop(Projectile projectile);
```

クライアントでは使用しない。

---

# Projectile Mixin

以下を新設する。

```text
src/main/java/com/magu1436/craftbound/mixin/
    ProjectileQualityImpactMixin.java
```

対象:

```text
Projectile#onHit(HitResult)
```

HEAD:

```text
QualityProjectileImpactContext.push(this)
```

RETURN:

```text
QualityProjectileImpactContext.pop(this)
```

`craftbound.mixins.json` へ登録する。

この context は、MOD の DamageSource が Projectile 自体を direct entity として保持しない場合の補助経路である。

---

# LivingHurtEvent での遠距離ダメージ解決

以下の順で Projectile を探す。

```text
1. event.getSource().getDirectEntity()
   が Projectile かつ CraftboundRangedQuality を持つ
       → その Projectile

2. QualityProjectileImpactContext.current()
   が品質付き Projectile
       → その Projectile

3. それ以外
       → 品質補正なし
```

両方が同一 Projectile を示しても補正は1回だけ行う。

サーバー側だけで `event.setAmount(...)` する。

Projectile impact context 中に同期的に発生した直接的な命中ダメージを対象とする。

着弾後に別 tick で発生する毒・炎上・継続効果などは、DamageSource 自体が品質付き Projectile を直接指さない限り自動補正しない。

---

# 品質性能 Event Handler

以下を新設する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/event/
    BlacksmithQualityPerformanceEvents.java
```

担当 event:

```text
ItemAttributeModifierEvent
ArrowLooseEvent
LivingGetProjectileEvent
EntityJoinLevelEvent
LivingHurtEvent
必要なら PlayerLoggedOutEvent
```

責務を各 service へ委譲し、Event Handler 内へ計算式や NBT 形式を直接書かない。

---

# 採用しなかった方針

## 1. 全装備を自動品質対象にする

採用しない。

理由:

Craftbound と連携していない MOD 装備まで品質 `30` と解釈され、元性能を意図せず低下させるため。

---

## 2. item ID ごとの性能プロファイル

例:

```json
{
  "minecraft:diamond_pickaxe": [
    "mining_speed",
    "max_durability"
  ]
}
```

採用しない。

理由:

Vanilla / MOD 装備が増えるほど手動定義が増え、連携コストが高い。

品質対象だけを Recipe から判定し、実際に存在する標準性能は runtime で自動検出する。

そのため Pickaxe が ATTACK_DAMAGE を持つ場合、攻撃力にも品質倍率が適用されてよい。

---

## 3. Item クラスによる分類

以下のような判定を主方式にしない。

```text
instanceof SwordItem
instanceof PickaxeItem
instanceof ArmorItem
```

理由:

MOD 独自 Item クラスへの互換性が低下するため。

---

## 4. PlayerEvent.BreakSpeed で品質採掘倍率を掛ける

採用しない。

理由:

Efficiency、Potion、Player補正等を含んだ後の速度へ品質倍率が掛かり、仕様上の「Item 基礎採掘性能の補正」にならないため。

---

## 5. 品質用 MULTIPLY_TOTAL AttributeModifier

採用しない。

理由:

Item 本来の攻撃力・防御力だけでなく Player 基礎値や他 modifier まで巻き込む可能性があるため。

---

## 6. AbstractArrow#getBaseDamage だけを変更する

採用しない。

理由:

MOD Projectile が `AbstractArrow` を継承していても、独自 damage field と独自 `entity.hurt(...)` を使用する場合があるため。

Projectile が最終的に与える damage を `LivingHurtEvent` で補正する。

---

## 7. ArrowLooseEvent だけで遠距離発射を検出する

採用しない。

理由:

独自 `releaseUsing()` 実装が Forge `onArrowLoose` を呼ばない MOD 武器が存在し得る。

`LivingGetProjectileEvent` も併用する。

---

# 関連する既存クラス

## 変更対象

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/
    QualityAssemblyRecipeValidationListener.java
        - validation成功後に QualityTargetRegistry を rebuild

src/main/java/com/magu1436/craftbound/occupations/blacksmith/client/
    BlacksmithQualityTooltipEvents.java
        - 明示品質優先
        - 対象完成品の品質欠落時だけ default_quality

src/main/java/com/magu1436/craftbound/event/
    CraftboundDataReloadEventHandler.java
        - BlacksmithQualityPerformanceDefinitions.INSTANCE を登録

src/main/java/com/magu1436/craftbound/network/
    CraftboundNetwork.java
        - performance sync packet 登録
        - PROTOCOL_VERSION 更新

src/main/resources/
    craftbound.mixins.json
        - ItemStackQualityPerformanceMixin
        - ProjectileQualityImpactMixin
        - 必要なら ItemQualityDurabilityBarMixin

docs/occupations/blacksmith/
    blacksmith.md
    data-formats.md
        - 品質対象の定義
        - performance.json の実配置
        - 遠距離は最終 Projectile damage に適用することを反映
```

## 原則変更しない

```text
src/main/java/com/magu1436/craftbound/common/quality/
    QualityState.java
    QualityStateCodec.java
    QualityStateService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/assembly/
    QualityAssemblyRecipe.java
    QualityAssemblyRecipeSerializer.java
    FinishedItemQualityCalculator.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/data/
    BlacksmithQualityTierDefinitions.java
```

必要な理由が発生しない限り変更しない。

---

# 推奨新規ファイル

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/quality/
    QualityTargetRegistry.java
    BlacksmithQualityResolver.java
    QualityPerformanceType.java
    QualityPerformanceRule.java
    QualityPerformanceService.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/quality/projectile/
    QualityProjectileStateService.java
    QualityProjectileFiringContext.java
    QualityProjectileImpactContext.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/data/
    BlacksmithQualityPerformanceDefinitions.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/event/
    BlacksmithQualityPerformanceEvents.java
    BlacksmithQualityPerformanceSyncEvents.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/client/
    BlacksmithQualityTargetRecipeEvents.java

src/main/java/com/magu1436/craftbound/network/packet/
    BlacksmithQualityPerformanceSyncPacket.java

src/main/java/com/magu1436/craftbound/mixin/
    ItemStackQualityPerformanceMixin.java
    ProjectileQualityImpactMixin.java
    ItemQualityDurabilityBarMixin.java   // 必要な場合

src/main/resources/data/craftbound/blacksmith/quality/
    performance.json
```

クラスを細分化しすぎないこと。

同じ責務を満たせる既存 utility がある場合は再利用し、同義クラスを追加しない。

---

# データフロー

## 近接・防具 Attribute

```text
ItemAttributeModifierEvent
    ↓
QualityTargetRegistry.contains(stack)
    ↓ false
return

    ↓ true
BlacksmithQualityResolver.resolveForPerformance(stack)
    ↓
quality
    ↓
event.getOriginalModifiers()
    ↓
対象 Attribute の ADDITION modifier 合計
    ↓
QualityPerformanceService
    ↓
元 modifier を同 UUID / name / Operation の補正値へ置換
```

## 採掘速度

```text
ItemStack#getDestroySpeed
    ↓
Vanilla / MOD の original return
    ↓
品質対象か
    ↓
original > 1.0 か
    ↓
mining_speed multiplier
    ↓
return adjusted
```

## 最大耐久値

```text
ItemStack#getMaxDamage
    ↓
Vanilla / MOD の original return
    ↓
品質対象か
    ↓
original > 0 か
    ↓
max_durability multiplier
    ↓
round
    ↓
return adjusted
```

## 遠距離

```text
weapon ItemStack
    ↓
ArrowLoose / LivingGetProjectile
    ↓
same-tick firing context
    ↓
Projectile join
    ↓
Projectile persistent quality
    ↓
Projectile impact
    ↓
LivingHurtEvent
    ↓
projectile_damage multiplier
```

---

# 擬似コード

## 品質対象再構築

```text
品質組立レシピ検証完了時:
    新しい空 Set<Item> を作る

    RecipeManager の crafting recipe を走査する

    QualityAssemblyRecipe だけを抽出する

    各 recipe の result Item を Set へ追加する

    immutable Set に変換する

    QualityTargetRegistry の現在集合を一括置換する
```

## 性能品質解決

```text
性能品質を取得:
    stack が空なら empty

    QualityTargetRegistry 対象外なら empty

    QualityStateService から明示品質を読む

    明示品質があればその値を返す

    なければ performance definition の default_quality を返す
```

## ツールチップ品質解決

```text
tooltip品質を取得:
    QualityStateService から明示品質を読む

    明示品質があればその値を返す

    明示品質がなく
    QualityTargetRegistry 対象なら
        default_quality を返す

    それ以外は empty
```

## Attribute 補正

```text
ItemAttributeModifierEvent:
    性能品質を解決する
    品質なしなら return

    ATTACK_DAMAGE, ARMOR, ARMOR_TOUGHNESS を順に処理する

    event.getOriginalModifiers() から
    対象 Attribute かつ ADDITION の modifier だけ収集する

    空なら何もしない

    amount 合計を計算する

    品質ルールを適用する

    armor / toughness は合計を0.5単位へ丸める

    元 modifier を event から remove する

    UUID / name / operation を保持した置換 modifier を add する
```

## 採掘速度

```text
ItemStack#getDestroySpeed RETURN:
    originalSpeed を取得する

    originalSpeed <= 1.0 なら return

    性能品質を解決する
    品質なしなら return

    adjusted =
        QualityPerformanceService.apply(
            MINING_SPEED,
            originalSpeed,
            quality
        )

    return adjusted
```

## 最大耐久値

```text
ItemStack#getMaxDamage RETURN:
    originalMax を取得する

    originalMax <= 0 なら return

    性能品質を解決する
    品質なしなら return

    adjusted =
        QualityPerformanceService.applyMaxDurability(
            originalMax,
            quality
        )

    return max(1, adjusted)
```

## 発射 context

```text
ArrowLooseEvent または LivingGetProjectileEvent:
    client side なら return

    weapon stack を取得する

    性能品質を解決する

    品質なしなら return

    shooter UUID
    quality
    current gameTime
    を firing context に保存する
```

## Projectile Join

```text
EntityJoinLevelEvent:
    client side なら return

    entity が Projectile でなければ return

    owner が Player でなければ return

    owner UUID の firing context を取得する

    context.gameTime != current gameTime なら return

    Projectile persistent data へ quality を保存する
```

## Projectile impact

```text
Projectile#onHit HEAD:
    current projectile を ThreadLocal stack へ push

Projectile#onHit RETURN:
    current projectile を pop
```

## 遠距離ダメージ

```text
LivingHurtEvent:
    client side なら return

    source.directEntity が
    品質付き Projectile ならそれを使う

    そうでなければ
    impact context.current() を取得する

    品質付き Projectile が見つからなければ return

    event.amount に
    PROJECTILE_DAMAGE の品質倍率を適用する

    event.setAmount(adjusted)
```

---

# 実装手順

実装は以下の **5ステップ** で行う。

各ステップ終了時にコンパイル可能な状態を維持する。

テストだけを独立した第6ステップにはしない。
各ステップで追加した純粋ロジックには、そのステップ内で対応する unit test を追加する。

## Step 1: 品質性能定義・倍率計算基盤

実装するもの:

```text
QualityPerformanceType
QualityPerformanceRule
QualityPerformanceService
BlacksmithQualityPerformanceDefinitions
performance.json
CraftboundDataReloadEventHandler への listener 登録
```

行うこと:

1. `performance.json` を追加する。
2. `default_quality` と6種の modifier を parse / validate する。
3. `craftbound:linear_multiplier` evaluator を実装する。
4. armor / armor_toughness の `round_to` を実装する。
5. max_durability の integer round を実装する。
6. リロード失敗時は直前の有効定義を保持する。
7. hot path では parse せず immutable runtime definition を読む。
8. performance definition の unit test を追加する。

このステップでは Item の実性能へまだ Hook しない。

完了条件:

```text
quality 0/30/50/80/100 の倍率が仕様値と一致
armor / toughness が0.5単位へ丸まる
max durability が整数 round
不正JSONで直前値を維持
```

---

## Step 2: Recipe 由来の品質対象判定と品質 Resolver

実装するもの:

```text
QualityTargetRegistry
BlacksmithQualityResolver
BlacksmithQualityTargetRecipeEvents
QualityAssemblyRecipeValidationListener の更新
BlacksmithQualityTooltipEvents の更新
```

行うこと:

1. `QualityAssemblyRecipe` の result Item をサーバーで収集する。
2. validation 成功後に `QualityTargetRegistry` を一括更新する。
3. client は `RecipesUpdatedEvent` で同期済み recipe から同じ集合を再構築する。
4. 性能用 Resolver を追加する。
5. target + explicit quality -> explicit quality。
6. target + no state -> `default_quality`。
7. non-target -> 性能品質なし。
8. tooltip は explicit quality を最優先し、中間パーツの既存品質表示を維持する。
9. target 完成品だけ、品質 NBT 欠落時に default tier を表示する。
10. registry / resolver の unit test を追加する。

完了条件:

```text
QualityAssemblyRecipe 出力だけ target
連携外MOD item は target にならない
target の品質NBT欠落 -> 30
non-target の品質NBT欠落 -> 品質なし
中間パーツの明示品質 tooltip は消えない
```

---

## Step 3: Attribute・採掘速度・最大耐久値への反映

実装するもの:

```text
BlacksmithQualityPerformanceEvents
ItemStackQualityPerformanceMixin
必要なら ItemQualityDurabilityBarMixin
craftbound.mixins.json 更新
```

行うこと:

1. `ItemAttributeModifierEvent` で攻撃力、防御力、防具強度を補正する。
2. `getOriginalModifiers()` の ADDITION だけを対象にする。
3. 元 UUID / name / operation を保持して modifier を置換する。
4. `ItemStack#getDestroySpeed` RETURN で基礎採掘速度を補正する。
5. `ItemStack#getMaxDamage` RETURN で最大耐久値を補正する。
6. Mixin 内で同一 getter を再呼び出ししない。
7. 標準耐久バーが品質後最大耐久値と一致することを確認する。
8. 一致しない場合だけ `ItemQualityDurabilityBarMixin` を追加する。
9. 独自 MOD durability bar は上書きしない。
10. 完成済み item の品質変更用 damage-ratio helper を必要最小限で追加する。

完了条件:

```text
品質50 -> 基準性能
品質30 -> 88%
品質80 -> 118%

Pickaxe:
    attack damage も存在すれば補正される
    destroy speed >1 の block で採掘速度補正
    max durability 補正

Armor:
    armor / toughness / durability 補正

attack speed:
    変化しない

Efficiency:
    quality補正済み base speed の後段として機能
```

---

## Step 4: 遠距離 Projectile 品質継承とダメージ補正

実装するもの:

```text
QualityProjectileStateService
QualityProjectileFiringContext
QualityProjectileImpactContext
ProjectileQualityImpactMixin

BlacksmithQualityPerformanceEvents へ:
    ArrowLooseEvent
    LivingGetProjectileEvent
    EntityJoinLevelEvent
    LivingHurtEvent
    cleanup event
```

行うこと:

1. Bow/Crossbow 発射時の品質を firing context へ保存する。
2. `LivingGetProjectileEvent` も併用する。
3. 同 tick・同 owner の Projectile Join で品質を Entity persistent data へ転写する。
4. 複数 projectile 発射に対応する。
5. `Projectile#onHit` の範囲を ThreadLocal impact context で囲む。
6. `LivingHurtEvent` で direct projectile を優先する。
7. direct entity が projectile でない場合は impact context を fallback とする。
8. `event.getAmount()` の最終 damage へ `PROJECTILE_DAMAGE` 倍率を1回だけ適用する。
9. 発射 context の古い entry を cleanup する。
10. Vanilla Bow / Crossbow の動作確認を追加する。
11. 連携 MOD の代表的な独自 Projectile は integration check として確認し、汎用経路で拾えない場合のみ別対応を検討する。

完了条件:

```text
Vanilla Bow:
    品質に応じて矢の最終ダメージが変わる

Crossbow:
    同様に変わる

Power enchant:
    MOD/Vanilla側の計算後に品質倍率

品質対象外 Bow:
    一切変化しない

Projectile 以外の通常ダメージ:
    変化しない
```

---

## Step 5: client sync・統合・仕様更新

実装するもの:

```text
BlacksmithQualityPerformanceSyncPacket
BlacksmithQualityPerformanceSyncEvents
CraftboundNetwork 更新
blacksmith.md 更新
data-formats.md 更新
必要な既存 test 更新
```

行うこと:

1. server の performance definition を `OnDatapackSyncEvent` で client へ送る。
2. packet decode 時にも値を検証する。
3. client runtime definition を server 値へ置換する。
4. `CraftboundNetwork` へ packet を追加する。
5. protocol version を更新する。
6. login 後、datapack reload 後の両方で client と server の性能ルールが一致することを確認する。
7. target registry は独自 packet ではなく synced RecipeManager から再構築する。
8. `blacksmith.md` の品質対象を「QualityAssemblyRecipe 出力 Item」に明確化する。
9. 「品質データを持たない既存 Item は30」の記述を「品質対象 Item に限る」と明確化する。
10. `data-formats.md` の単一 `blacksmith/quality.json` 記述を現行実装へ合わせ、少なくとも
    - `blacksmith/quality/tiers.json`
    - `blacksmith/quality/performance.json`
    に整理する。
11. 遠距離ダメージは final Projectile damage へ倍率を掛けることを仕様へ記載する。

完了条件:

```text
Dedicated server + client で性能値が一致
/datapack reload 後も一致
品質対象 Recipe の追加・削除が target 集合へ反映
通常レシピ産の品質対象 Item は性能上 quality 30
連携外 MOD Item は補正なし
```

---

# 境界条件・例外

## 1. 品質対象だが明示品質がない

```text
QualityTargetRegistry = true
QualityState = empty
```

性能上は `default_quality` を使用する。

現在値は 30。

NBT を読むたびに自動書き込みする必要はない。

---

## 2. 明示品質を持つ中間パーツ

性能 target でなくても品質 tooltip は維持する。

性能補正は行わない。

---

## 3. 品質対象外 MOD 装備

Attack Damage、Armor、MaxDamage、DestroySpeed を持っていても何もしない。

---

## 4. zero / negative Attribute modifier

対象 Attribute の ADDITION 合計が 0 の場合は変更しない。

負値が存在する場合は合計値へ通常の倍率を適用するが、modifier 置換で NaN / Infinity を生成してはならない。

---

## 5. MaxDamage 0

非耐久 Item として変更しない。

---

## 6. DestroySpeed 1.0 以下

品質採掘補正を行わない。

---

## 7. 他 MOD の独自 Attribute

Craftbound が知らない Attribute は変更しない。

---

## 8. 他 MOD の MULTIPLY_* 基礎性能

安全性を優先し、初期実装では変更しない。

必要な連携 MOD で問題が確認された場合だけ adapter を検討する。

---

## 9. 独自 Projectile が Projectile#onHit を通らない

自動対応範囲外。

その MOD が Craftbound の連携対象であり、品質対応が必要な場合だけ個別 integration adapter を追加する。

---

## 10. Projectile が別 tick に追加ダメージを発生

persistent data を持つ Projectile が DamageSource direct entity なら補正可能。

Projectile impact context だけに依存する後続ダメージは補正しない。

---

## 11. 発射品質の途中変更

Projectile へ転写した時点の品質を固定する。

発射後に弓の ItemStack 品質や装備を変更しても飛翔中 Projectile の品質は変化しない。

---

## 12. 複数 Projectile

同一 tick に同一 shooter が発生させる複数 Projectile は同じ firing context を使用できる。

最初の Projectile Join で context を削除しない。

---

## 13. client/server

ゲーム性能の確定は server を正とする。

client 側の品質性能計算は tooltip、耐久表示、採掘体感等の予測・表示整合のため server definition と同期する。

---

# 完了条件

以下をすべて満たすこと。

- 品質性能対象は `QualityAssemblyRecipe` の完成品 Item だけである。
- 品質対象一覧を item ID JSON で二重管理していない。
- 品質対象外の Vanilla / MOD Item へ勝手に品質30を適用しない。
- 品質対象の明示品質欠落時だけ既定品質30を使用する。
- 中間パーツの明示品質 tooltip を維持する。
- 攻撃力は Item の `ATTACK_DAMAGE` ADDITION だけを品質補正する。
- 攻撃速度は変化しない。
- 防御力と防具強度は品質補正される。
- 防御力と防具強度の丸めは0.5単位である。
- 採掘速度は `ItemStack#getDestroySpeed` の基礎値へ適用される。
- `PlayerEvent.BreakSpeed` へ品質倍率を掛けていない。
- 最大耐久値は ItemStack 依存の元値へ後段適用される。
- 品質50で元最大耐久値と同じになる。
- 標準耐久バーが補正済み最大耐久値と整合する。
- 遠距離 Projectile は発射時の武器品質を保持する。
- Vanilla Bow / Crossbow の射撃ダメージが品質で変化する。
- 遠距離品質は Projectile の最終 direct-hit damage へ適用される。
- Projectile 以外の `LivingHurtEvent` へ誤適用しない。
- performance rule は datapack reload 可能である。
- performance rule は server から client へ同期される。
- Recipe reload 後に品質対象集合が更新される。
- hot path で Recipe 全探索や JSON parse を行わない。
- Mixin 内で対象 getter を再帰呼び出ししない。
- `QualityStateService` の保存責務を肥大化させていない。
- `./gradlew test` が成功する。
- `./gradlew compileJava` が成功する。
- 既存の鍛冶組立・品質段階表示テストを破壊していない。

---

# 今回の対象外

- 品質による Attack Speed 変更
- 品質による Knockback 変更
- 品質による Knockback Resistance 変更
- 品質による enchantment level / effect 変更
- `work_speed` の汎用 Hook
- Item ID ごとの品質性能プロファイル
- 全 MOD 装備の自動品質対象化
- 自由素材から生成される動的装備
- 製作後の再鍛造システム
- 修理システムの独自実装
- 非 Projectile 型の銃・beam・magic attack の完全自動対応
- 各 MOD の特殊ゲージや energy bar の上書き
- 特殊 MOD Item 用の stat-specific blacklist

最後の blacklist / integration adapter は、実際に Craftbound の `QualityAssemblyRecipe` 対象として追加した MOD Item で不整合が確認された場合だけ追加する。
