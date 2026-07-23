

# カテゴリに置くファイル群

| ファイル               | 内容                          |
| ------------------ | --------------------------- |
| `config.json`      | 冒険者、鍛冶師など、存在するカテゴリの一覧       |
| `category.json`    | 「冒険者」というカテゴリの名前、アイコン、背景など   |
| `definitions.json` | 各スキルの表示名、説明、アイコン、コスト、効果     |
| `skills.json`      | スキルノードのID、座標、使用するdefinition |
| `connections.json` | スキルノード同士の接続                 |
| `experience.json`  | 経験値の獲得条件                    |

## difinitions.json 例

```json
{
  "projectile_resistance_1": {
    "title": "飛び道具耐性 I",
    "description": "飛び道具から受けるダメージを5%軽減する。",
    "icon": {
      "type": "item",
      "data": {
        "item": "minecraft:shield"
      }
    },
    "rewards": [
      {
        "type": "puffish_skills:tag",
        "data": {
          "tag": "craftbound.adventurer.projectile_resistance_1"
        }
      }
    ]
  }
}
```

上記は, 以下のようなスキルを定義している.

* スキルID： `projectile_resistance_1`
* スキル表示名： `飛び道具耐性 I`
* スキルを表すタグ(報酬)： `craftbound.adventurer.projectile_resistance_1`

## skills.json

```json
{
  "projectile_resistance_1": {
    "x": 0,
    "y": 0,
    "definition": "projectile_resistance_1",
    "root": true
  }
}
```

# スキル

スキルは以下の属性を持つ.

- 前提スキル
- コスト
- 表示名・説明
- 報酬(Rewards)

スキルそのものの定義は `definitions.json` で行う. あくまで **JSONがスキルの本体** である.  

# 報酬(Rewards)

報酬は, スキル獲得時にプレイヤーに与えられる効果である.  
報酬そのものは Java 側で定義し, `definitions.json` にてスキルに対して報酬を付与する.  
以下のコードにて, Java にてスキルオブジェクトを取得することができる.

```java
import net.puffish.skillsmod.api.SkillsAPI;

SkillsAPI
  .getCategory(categoryId)
  .flatMap(category -> getSkill(skillId));
```

なお, 引数は以下の型で与えられる.  

| 変数名 | 型 |
|:---:|:---:|
| `categoryId` | `ResourceLocation` |
| `skillId` | `String` |

## 独自報酬の定義

```java
public final class ExampleReward implements Reward {

    public static final ResourceLocation ID =
            new ResourceLocation(
                    Craftbound.MODID,
                    "example_reward"
            );

    public static void register() {
        SkillsAPI.registerReward(
                ID,
                ProjectileResistanceReward::parse
        );
    }

    // parse、update、disposeを実装
}
```

# 参考

* [公式ドキュメント](https://puffish.net/skillsmod/docs/)