# 現在の作業タスク (T45)

## タスク名
B22 & B23: 実行ログとデバッグ情報の状態表示のフルパス化

## 時間記録
*   **開始日時**: 2024-05-25 14:30
*   **終了日時**: -
*   **所要時間**: -

## 実装方針・設計メモ
1.  **`SimulationEngine.java`**:
    *   `getFullPath(IVertex vertex)` ヘルパーメソッドを追加する。これは、与えられた `IVertex` からルートまでのフルパス文字列（例: "Root/Parent/Child"）を生成する。
    *   `getDebugInfo` メソッド内の `[Current Vertices]` および `[History Map]` の出力で、この `getFullPath` を使用し、状態名をフルパスで表示するように変更する。
2.  **`StmAnalysisView.java`**:
    *   `printStepResult` メソッド内のログ出力 (`Transition: %s -> %s`) を修正し、遷移の直接のターゲットだけでなく、その遷移によって最終的にアクティブになった状態（ドリルダウン後のリーフ状態のフルパス）も表示するように変更する。具体的には、`Transition: Source -> DirectTarget` の後に `Active States: FullPath1, FullPath2` の行を追加する。

## TODOリスト
- [x] `SimulationEngine.java` に `getFullPath(IVertex)` ヘルパーメソッドを追加
- [x] `SimulationEngine.java` の `getDebugInfo` を修正し、`[Current Vertices]` と `[History Map]` でフルパスを出力
- [x] `StmAnalysisView.java` の `printStepResult` を修正し、ログに最終的なアクティブ状態のフルパスを出力
- [x] `mvn clean install` でビルド成功を確認