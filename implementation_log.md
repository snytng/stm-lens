# Stm Lens for astah* 実装ログ

## 現状のステータス
*   **完了**:
    1.  プロジェクトのセットアップ（Maven, SDK）。
    2.  仕様・設計の策定 (`requirements.md`, `specification.md`, `design.md` 更新)。
    3.  UIスケルトンの実装 (`StmAnalysisView` 作成)。
    4.  **ロジックの実装**: `SimulationEngine` クラスに、初期状態探索、遷移取得、状態更新のロジックを実装。
    5.  **UIとロジックの結合**: `StmAnalysisView` から `SimulationEngine` を呼び出し、画面操作でシミュレーションが動くようにする。

## 次のステップ
6.  **図のハイライト機能の実装**: `DiagramHighlighter` クラスを作成し、現在の状態を図上で強調表示する。
