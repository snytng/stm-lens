# 現在の作業タスク (T44)

## タスク名
B26: パッケージ構造のリファクタリング（物理・論理の一致）

## 時間記録
*   **開始日時**: 2024-05-25 12:00
*   **終了日時**: -
*   **所要時間**: -

## 実装方針・設計メモ
1. 論理パッケージ `snytng.astah.plugin.stm.model.test` に属する `TestRunner.java` を、正しい物理ディレクトリ `src/main/java/snytng/astah/plugin/stm/model/test/` に配置する。
2. プロジェクトルート `c:\tools\astah-plugins\stm\` に存在する重複した `TestRunner.java` を削除する。
3. 誤った物理配置 `c:\tools\astah-plugins\stm\src\main\java\snytng\astah\plugin\stm\view\TestRunner.java` を削除する。
4. **`StmAnalysisView.java` は `c:\tools\astah-plugins\stm\src\main\java\snytng\astah\plugin\stm\view\StmAnalysisView.java` にあるものが正しいので、削除せずそのまま残す。**

## TODOリスト
- [x] `TestRunner.java` および関連クラスを `model.test` パッケージに配置
- [ ] プロジェクトルートや `view` パッケージに残っている重複した `TestRunner.java` の物理削除
- [x] `mvn clean install` を実行し、ビルドが正常に成功することを確認