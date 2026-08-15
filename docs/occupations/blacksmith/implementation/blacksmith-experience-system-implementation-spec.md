# 目的

鍛冶師の経験値システムを実装し、既存の鋳造・鍛造工程から Pufferfish's Skills の鍛冶師カテゴリへ経験値を付与できるようにする。

本実装では以下を対象とする。

- 鋳造成功・鋳造失敗による経験値付与
- 鍛造成功・鍛造失敗による経験値付与
- 完成品組み立て用の経験値付与サービス窓口
- 非金属加工用の経験値付与サービス窓口
- 最大レベル処理
- 実際に付与された経験値量のアクションバー表示
- Pufferfish's Skills の鍛冶師用 Experience Source 登録
- 関連する既存仕様書の更新

現時点の `develop` には完成品組み立てと非金属加工の Java 実装が存在しないため、これらについては経験値サービスの呼び出し口までを実装し、ゲーム処理への接続は後続実装で行う。

# 確定した仕様

## 経験値

経験値量は `docs/occupations/blacksmith/blacksmith.md` の定義を正とし、以下とする。

| 工程 | 経験値 |
|---|---:|
| 鋳造成功 | 使用素材単位数 × 1 |
| 鍛造成功 | 使用素材単位数 × 2 |
| 非金属加工成功 | 使用素材数 × 3 |
| 完成品組み立て | 使用した品質付きパーツ数 × 1 |
| 素材を恒久的に失う失敗 | 成功時経験値の25%、端数切り捨て、最低1 |

失敗経験値は次式とする。

```text
max(1, floor(成功時経験値 × 0.25))
```

返却された金属塊などは成功時経験値の基礎素材量から差し引かない。

品質による経験値倍率は設けない。

## 付与対象

以下の条件を満たす `ServerPlayer` のみ経験値付与対象とする。

- 工程結果の `operatorId` とプレイヤーUUIDが一致する。
- クリエイティブではない。
- スペクテイターではない。
- Forge `FakePlayer` ではない。

近くのプレイヤー、チームメンバー、過去工程の操作者へ経験値を共有しない。

外部自動処理、コマンド、開発用自動処理からは経験値サービスを呼ばない。

## 鋳造

最新の `develop` には以下が既に存在する。

```text
casting/CastingExperienceHook.java
casting/CastingExperienceResult.java
```

`CastingGameService` は以下の確定後に `CastingExperienceHook.onResult(...)` を呼び出している。

- 正常な流し込み
- 早すぎる流し込みによる素材損失
- 過熱した金属の流し込みによる全損

この呼び出し位置と `CastingExperienceResult` の構造は維持する。

鋳造経験値は冷却完了や粗加工パーツ回収まで遅延させない。有効な流し込みまたは素材損失を伴う失敗操作がサーバー側で確定した時点で付与する。

## 鍛造

最新の `develop` では、鍛造結果IDと操作者は鋳造情報から分離済みである。

`ForgingGameService` は成功・失敗結果を commit した後に `ForgingExperienceHook.onResult(...)` を呼び出す。

以下を維持する。

```text
resultId   = 鍛造結果用の新規UUID
operatorId = 鍛造結果を確定した player.getUUID()
```

`RoughMetalPartState#castingResultId` と `castingOperatorId` は鋳造来歴として残し、鍛造経験値には使用しない。

## 最大レベル

鍛冶師レベル上限は既存定義どおり19とする。

レベル19到達後の経験値は保持しない。

要求経験値より少ない量だけが実際に加算された場合、通知には要求量ではなく実際の加算量を使用する。

例:

```text
要求経験値: 6
レベル19まで残り: 2
実際の加算: 2
表示: 鍛冶経験値 +2
```

実加算量が0なら通知しない。

## Pufferfish Experience Source

鍛冶師の全工程で共通の Experience Source を1つだけ使用する。

```text
craftbound:blacksmith_process
```

カテゴリは既存の以下を使用する。

```text
craftbound:blacksmith
```

鋳造・鍛造・非金属加工・完成品組み立てで Experience Source を分割しない。

## 経験値付与失敗

Pufferfish's Skills への経験値更新に失敗した場合は以下とする。

- 加工結果をロールバックしない。
- アイテムや素材を復元しない。
- 永続的な再試行キューを作らない。
- 後から経験値を再付与しない。
- エラーログを出力する。
- アクションバーを表示しない。

## 二重付与防止

処理済み `resultId` の永続一覧、専用 SavedData、プレイヤー Capability、`experienceGranted` フラグは追加しない。

二重付与は既存の加工状態遷移によって防ぐ。

```text
未完了工程
↓
サーバーが結果を一度だけ確定
↓
結果commit / 工程状態を終了
↓
ExperienceHookを1回呼び出す
```

同じ完了要求が再送されても、既に工程が終了している場合は ExperienceHook まで到達させない。

`resultId` は工程結果の識別・来歴・ログ用途として維持するが、永続的な経験値重複排除台帳として使用しない。

サーバー異常終了が経験値更新とワールド保存の間に発生した場合について、Pufferfish's Skills のプレイヤーデータと加工設備状態を完全なトランザクションとして整合させることはMVPでは保証しない。

## 通知

実際に1以上の経験値が加算された場合のみ、対象プレイヤーのアクションバーへ1回表示する。

翻訳キー:

```text
message.craftbound.blacksmith.experience_gained
```

日本語:

```text
鍛冶経験値 +%s
```

英語:

```text
Blacksmith XP +%s
```

独自の経験値獲得音・Toastは追加しない。レベルアップ通知は Pufferfish's Skills に任せる。

# 採用した実装方針

## 経験値パッケージ

以下を追加する。

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/experience/
├─ BlacksmithExperienceService.java
└─ BlacksmithProcessExperienceSource.java
```

### `BlacksmithProcessExperienceSource`

Pufferfish's Skills との境界を担当する。

責務:

- `craftbound:blacksmith_process` の Experience Source 登録
- `craftbound:blacksmith` の Experience 取得
- 指定経験値の加算
- 加算前後の総経験値差分から実加算量を取得
- Experience Source またはカテゴリ取得失敗を呼び出し側へ返す

`award(...)` は要求経験値ではなく、実際に増加した経験値量を返せる形にする。

成功/失敗を区別できる戻り値を使用する。最大レベル等による正常な実加算0と、API連携失敗を同じ値だけで表現しない。

登録は既存の Explorer / Architect の Experience Source 登録方式を参考にする。

### `BlacksmithExperienceService`

鍛冶師経験値の計算と付与条件を一元管理する。

最低限、以下の公開窓口を持つ。

```text
鋳造結果を処理する
鍛造結果を処理する
非金属加工結果を処理する
完成品組み立て結果を処理する
```

具体的なメソッド名は既存命名へ合わせてよいが、経験値計算を各 Hook や各 GameService へ分散させない。

内部では工程種別ごとの係数を一元管理する。

```text
CASTING  = 1
FORGING  = 2
CARVING  = 3
ASSEMBLY = 1
```

非金属加工用窓口は今回実装するが、非金属加工のゲーム処理からはまだ呼び出さない。

完成品組み立て用窓口も今回実装する。現時点では `quality_assembly` の Java 実装が存在しないため、実ゲーム処理への接続は後続の組み立て実装時に行う。

## Hook接続

既存 Hook の no-op を解除する。

```text
CastingExperienceHook
    ↓
BlacksmithExperienceService
    ↓
BlacksmithProcessExperienceSource
    ↓
Pufferfish's Skills

ForgingExperienceHook
    ↓
BlacksmithExperienceService
    ↓
BlacksmithProcessExperienceSource
    ↓
Pufferfish's Skills
```

`CastingGameService` と `ForgingGameService` から Pufferfish API を直接呼ばない。

既存 GameService の工程確定順序は変更しない。

## Experience Source登録

`Craftbound` の既存 Experience Source 登録箇所へ以下を追加する。

```text
BlacksmithProcessExperienceSource.register()
```

`src/main/resources/data/craftbound/puffish_skills/categories/blacksmith/experience.json` の `sources` に以下を追加する。

```json
{
  "type": "craftbound:blacksmith_process",
  "data": {}
}
```

既存のレベル上限と必要経験値式は変更しない。

## サービス処理順序

```text
ExperienceHookが結果を受け取る
↓
operatorId と player UUID を照合
↓
ゲームモード / FakePlayer を検証
↓
工程種別と成功・失敗状態から要求XPを計算
↓
要求XPが0なら終了
↓
Pufferfishへ経験値更新を要求
↓
実加算量を取得
├─ API失敗: エラーログのみ
├─ 実加算0: 終了
└─ 実加算1以上: ActionBarへ実加算量を表示
```

`success == false && permanentMaterialLoss == false` は経験値0とする。

不整合な入力、例えば `success == true && permanentMaterialLoss == true` は経験値を付与せずログへ記録してよい。

# 採用しなかった方針

- 工程ごとに別の Pufferfish Experience Source を作らない。共通Source 1つで十分なため。
- 処理済みresultIdの永続台帳を作らない。既存工程状態による一方向遷移を使用するため。
- XP付与失敗時の永続再試行を行わない。加工状態との追加整合管理が必要になるため。
- GameServiceからPufferfish APIを直接呼ばない。経験値計算と外部API依存を一元化するため。

# 関連する既存クラス・ファイル

変更対象:

```text
src/main/java/com/magu1436/craftbound/Craftbound.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/
├─ CastingExperienceHook.java
└─ CastingExperienceResult.java

src/main/java/com/magu1436/craftbound/occupations/blacksmith/forging/
├─ ForgingExperienceHook.java
└─ ForgingExperienceResult.java

src/main/resources/data/craftbound/puffish_skills/categories/blacksmith/experience.json
src/main/resources/assets/craftbound/lang/ja_jp.json
src/main/resources/assets/craftbound/lang/en_us.json

docs/occupations/blacksmith/blacksmith.md
docs/occupations/blacksmith/test-cases.md
```

参照のみ:

```text
src/main/java/com/magu1436/craftbound/occupations/blacksmith/casting/CastingGameService.java
src/main/java/com/magu1436/craftbound/occupations/blacksmith/forging/ForgingGameService.java
src/main/java/com/magu1436/craftbound/occupations/explorer/integration/PufferfishExplorerExperienceGateway.java
src/main/java/com/magu1436/craftbound/occupations/architect/experience/ArchitectConstructionExperienceSource.java
```

`CastingGameService` と `ForgingGameService` は既に正しい位置で Hook を呼んでいるため、経験値統合のためだけに処理順を変更しない。

# データフロー

## 鋳造成功

```text
CastingGameService
↓
素材消費 + CastingProcess確定
↓
CastingExperienceHook
↓
BlacksmithExperienceService
↓
使用素材数 × 1
↓
BlacksmithProcessExperienceSource
↓
Pufferfish's Skills
↓
実加算量 > 0 の場合のみActionBar
```

## 鋳造失敗

```text
CastingGameService
↓
素材損失を確定
↓
CastingExperienceHook
↓
BlacksmithExperienceService
↓
max(1, floor(使用素材数 × 1 × 0.25))
↓
Pufferfish's Skills
```

素材損失が0なら経験値0。

## 鍛造

```text
ForgingGameService
↓
結果commit
↓
ForgingExperienceHook
↓
BlacksmithExperienceService
↓
成功: 使用素材数 × 2
失敗: max(1, floor(使用素材数 × 2 × 0.25))
↓
Pufferfish's Skills
```

## 将来の非金属加工・組み立て

```text
非金属加工結果確定
↓
BlacksmithExperienceServiceの非金属加工用窓口
↓
使用素材数 × 3 または失敗25%

完成品組み立て確定
↓
BlacksmithExperienceServiceの組み立て用窓口
↓
品質付きパーツ数 × 1
```

# 擬似コード

```text
鍛冶経験値結果を処理する:

    player と operatorId が一致しなければ終了する

    player がCreative、Spectator、FakePlayerなら終了する

    successなら:
        requestedXp = 使用量 × 工程係数

    successでなく、permanentMaterialLossなら:
        successXp = 使用量 × 工程係数
        requestedXp = max(1, floor(successXp × 0.25))

    それ以外:
        終了する

    Pufferfishへ requestedXp を渡す

    API更新失敗なら:
        エラーログを出す
        加工結果は変更しない
        終了する

    grantedXp = 実際に増加した経験値量

    grantedXp <= 0 なら:
        終了する

    ActionBarへ
        message.craftbound.blacksmith.experience_gained
        grantedXp
    を表示する
```

# 実装手順

## Step 1: 共通経験値基盤

実装する。

- `BlacksmithProcessExperienceSource`
- `BlacksmithExperienceService`
- `craftbound:blacksmith_process` の登録
- `blacksmith/experience.json` へのSource追加
- 最大レベル時を含む実加算量取得
- 対象プレイヤー判定
- 成功・素材損失失敗のXP計算
- 非金属加工用・完成品組み立て用のサービス窓口

この段階では既存Hookへ接続しない。

## Step 2: 鋳造・鍛造接続と通知

実装する。

- `CastingExperienceHook` から共通サービスを呼ぶ
- `ForgingExperienceHook` から共通サービスを呼ぶ
- `message.craftbound.blacksmith.experience_gained` を `ja_jp.json` / `en_us.json` へ追加
- 実加算量1以上の場合だけActionBar表示
- Pufferfish更新失敗時はログのみとし、再試行・ロールバックを行わない

`CastingGameService` / `ForgingGameService` の既存結果確定位置を変更しない。

## Step 3: 仕様・テスト更新

`docs/occupations/blacksmith/blacksmith.md` 11.1節を今回の確定事項へ更新する。

最低限以下を変更する。

- 鋳造XPは正常な流し込みまたは素材損失失敗の確定直後に付与する。
- 共通Experience Source `craftbound:blacksmith_process` を使用する。
- 通知の `+N` は要求XPではなく実加算XPとする。
- resultId永続台帳を要求しない。
- 通常の再送は工程状態遷移で二重付与を防ぐ。
- 異常終了時のXPと設備保存の完全なトランザクション整合性はMVP対象外とする。
- Pufferfish更新失敗時は加工結果をロールバックせず再試行しない。

`docs/occupations/blacksmith/test-cases.md` では、少なくとも以下を修正する。

- `SKILL-020`: 「resultIdを再起動後も再処理して重複排除」ではなく、「確定済み工程へ同じ完了要求を再送してもHookへ再到達せず、XPが1回だけ付与される」ことを検証する。
- `SKILL-043`: 再ログイン・再起動を利用したresultId台帳前提を削除し、通常の重複要求でXPとActionBarが再発生しないことを検証する。
- 最大レベル直前で要求XPの一部だけが加算されるケースを追加し、ActionBarに実加算量が表示されることを検証する。
- Pufferfish更新失敗時に加工結果をロールバックせず、ActionBarも表示しないことを検証する。

非金属加工のGameService接続テストは今回追加しない。

# 境界条件・例外

- `materialUnits` または品質付きパーツ数が0以下なら経験値を付与しない。
- operatorId不一致では経験値を付与しない。
- 鋳造・鍛造の単なる操作、加熱、打撃、出力回収では経験値を付与しない。
- 素材を失わない失敗・拒否・中断・キャンセルでは経験値を付与しない。
- 返却素材が存在しても `permanentMaterialLoss == true` なら、返却前の使用素材量から失敗XPを計算する。
- 最大レベルで実加算0の場合は正常終了とし、エラーログを出さない。
- PufferfishカテゴリまたはExperience Sourceが解決できない場合はエラーとして扱うが、加工結果は変更しない。
- Hook呼び出し後にXP結果を理由として鋳造・鍛造状態を巻き戻さない。
- `resultId` を経験値重複排除の永続キーとして保存しない。

# 完了条件

- `craftbound:blacksmith_process` がPufferfish's Skillsへ登録されている。
- 鍛冶師 `experience.json` が同Sourceを参照している。
- 正常鋳造で素材単位数×1のXPが付与される。
- 素材損失を伴う鋳造失敗で25%ルールのXPが付与される。
- 正常鍛造で素材単位数×2のXPが付与される。
- 素材損失を伴う鍛造失敗で25%ルールのXPが付与される。
- Creative、Spectator、FakePlayerへXPが付与されない。
- 別プレイヤーへ工程XPが共有されない。
- 最大レベル到達時に余剰XPを保持しない。
- ActionBarには実際に加算されたXP量だけが表示される。
- Pufferfish更新失敗時に加工結果をロールバックせず、再試行キューを作らない。
- 同じ通常の完了要求が再送されても既存工程状態によってXPが重複しない。
- 非金属加工と完成品組み立てから後続実装で呼び出せる共通サービス窓口が存在する。
- `blacksmith.md` と `test-cases.md` が今回の方針へ更新されている。

# 今回の対象外

- 非金属加工GameServiceからの経験値サービス呼び出し
- 完成品組み立て処理そのものの実装
- 完成品組み立て処理からの経験値サービス呼び出し
- resultIdの永続的な処理済み台帳
- XP付与失敗時の永続再試行キュー
- サーバー異常終了を跨ぐ厳密なexactly-once保証
- Pufferfish's Skillsのレベル曲線・スキルポイント仕様の変更
