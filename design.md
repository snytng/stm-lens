# Stm Lens for astah* 設計書

## 1. アーキテクチャ
astah* Plug-in SDKを利用したJavaプラグインとして実装する。
MVC (Model-View-Controller) パターンを適用し、状態マシンのロジックとUI表示を分離する。

## 2. クラス構成 (パッケージ: snytng.astah.plugin.stm)

### 2.1. View (UI層) - `actions` / `ui` package
*   **`StmAnalysisView`**: プラグインのエントリーポイントとなるメインパネル。`BorderLayout`を採用。
    *   Top: コントロールパネル（Start/Resetボタン）。
    *   Center: イベントボタンパネル（動的にボタンを配置）。
    *   Bottom: ログ出力エリア (`JTextArea`)。

### 2.2. Model (ロジック層) - `model` package
*   **`SimulationEngine`**: シミュレーションの中核ロジック。
    *   `currentVertex`: 現在の `IVertex` (State or Pseudostate)。
    *   `start(IStateMachineDiagram)`: 図から初期状態を探して開始。
    *   `getAvailableTransitions()`: 現在の状態から遷移可能な `ITransition` のリストを返す。
    *   `fire(ITransition)`: 指定された遷移を実行し、カレントステートを更新する。
*   **`DiagramHighlighter`**: astah* API (`IDiagramViewManager`) を使用して、現在の状態、直前の状態、遷移をハイライトするヘルパークラス。

## 3. 動的構造（処理フロー）

### 3.1. シミュレーション開始フロー
1.  ユーザーが [Start] ボタンを押下。
2.  `StmAnalysisView` が `SimulationEngine.start()` を呼び出す。
3.  `SimulationEngine` は `IStateMachineDiagram` から `InitialPseudostate` を探索。
4.  初期状態からの遷移を辿り、最初の `State` を特定して `currentVertex` に設定。
5.  `DiagramHighlighter` が初期状態をハイライト。
6.  `StmAnalysisView` が `getAvailableTransitions()` を呼び出し、イベントボタンを生成して表示。

### 3.2. イベント発火フロー
1.  ユーザーがイベントボタンを押下。
2.  `StmAnalysisView` が `SimulationEngine.fire(transition)` を呼び出す。
3.  `SimulationEngine` は `currentVertex` を遷移先の `IVertex` に更新。
4.  `DiagramHighlighter` が古い状態のハイライトを解除し、新しい状態をハイライト。
5.  `StmAnalysisView` がログエリアに遷移履歴を追記。
6.  `StmAnalysisView` がイベントボタンリストを更新（次の状態で可能なイベントに差し替え）。
