# 開発環境セットアップ

このドキュメントでは、Visual Studio Code（VS Code）を使用してCraftboundの開発環境をセットアップする手順を説明する。

## 前提環境

開発には次のソフトウェアを使用する。

- Git
- 64-bit版JDK 17
- Visual Studio Code
- VS Code拡張機能
  - Extension Pack for Java
  - Gradle for Java

VS Codeでこのリポジトリを開くと、必要な拡張機能が推奨として表示される。表示されない場合は、拡張機能ビューから上記の拡張機能をインストールする。

GradleはGradle Wrapperを使用するため、別途インストールする必要はない。

## 1. JDKの確認

ターミナルで次のコマンドを実行する。

```powershell
java -version
```

出力にJava 17と64-bit JVMを示す情報が含まれていることを確認する。

続けて、Gradleが使用するJVMを確認する。

```powershell
.\gradlew.bat --version
```

`JVM` が17以外の場合は、`JAVA_HOME`をJDK 17のインストール先へ設定してからVS Codeを再起動する。

## 2. VS Codeの起動設定を生成する

リポジトリのルートディレクトリをVS Codeで開き、PowerShellで次のコマンドを実行する。

```powershell
.\gradlew.bat genVSCodeRuns
```

macOS、Linux、Git Bashでは次のコマンドを使用する。

```bash
./gradlew genVSCodeRuns
```

この処理は、VS CodeでMinecraftを起動するための `.vscode/launch.json` と `.vscode/tasks.json` を生成する。生成内容にはローカル環境の絶対パスが含まれるため、これらのファイルはGit管理対象外とする。

初回実行時はGradle、Forge、Minecraft関連の依存関係をダウンロードするため、インターネット接続が必要となり、完了まで時間がかかる場合がある。

次の場合は、Gradleプロジェクトを再読み込みしてから `genVSCodeRuns` を再実行する。

- 初めてリポジトリを取得したとき
- リポジトリの配置場所を変更したとき
- `build.gradle`、`settings.gradle`、`gradle.properties` を変更または更新したとき
- VS Codeの実行構成が古い、または起動に失敗するとき

## 3. 開発用Minecraftを起動する

1. VS Codeの「実行とデバッグ」ビューを開く。
2. 実行構成から `runClient` を選択する。
3. F5キー、または「デバッグの開始」を実行する。

ターミナルから直接起動する場合は、次のコマンドを使用する。

```powershell
.\gradlew.bat runClient
```

開発用Minecraftのゲームデータは `run/` に保存される。このディレクトリはGit管理対象外である。

## 4. ビルドする

PowerShellで次のコマンドを実行する。

```powershell
.\gradlew.bat build
```

macOS、Linux、Git Bashでは次のコマンドを使用する。

```bash
./gradlew build
```

ビルドが成功すると、配布可能なModのJARファイルが `build/libs/` に生成される。

## トラブルシューティング

### 依存関係を再取得する

依存関係の取得に失敗した場合やキャッシュの不整合が疑われる場合は、次のコマンドを実行する。

```powershell
.\gradlew.bat --refresh-dependencies
```

### ビルド結果と起動設定を作り直す

ビルド結果を削除し、VS Codeの起動設定を再生成する。

```powershell
.\gradlew.bat clean
.\gradlew.bat genVSCodeRuns
```

`clean` は生成されたビルド結果を削除するが、`src/` 内のソースコードは削除しない。

### VS CodeがJDKや依存関係を認識しない

次の順序で確認する。

1. `java -version` と `.\gradlew.bat --version` がJDK 17を使用していることを確認する。
2. VS CodeでGradleプロジェクトを再読み込みする。
3. コマンドパレットから `Java: Clean Java Language Server Workspace` を実行する。
4. VS Codeを再起動し、`.\gradlew.bat genVSCodeRuns` を再実行する。
