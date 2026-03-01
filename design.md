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
    *   `showActionsCheckbox`: アクションログの表示/非表示を制御する `JCheckBox`。

### 2.2. Model (ロジック層) - `model` package
*   **`SimulationEngine`**: シミュレーションの中核ロジック。
    *   `currentVertices`: 現在アクティブな `IVertex` のリスト（平行状態対応のため複数保持）。
    *   `executionLog`: シミュレーションの実行履歴（ログテキストまたは構造化データ）を保持する。
    *   `start(IStateMachineDiagram)`: 図から初期状態を探して開始。
    *   `getAvailableTransitions()`: 現在の状態から遷移可能な `ITransition` のリストを返す。
    *   `step(ITransition)`: 指定された遷移を実行し、遷移結果（アクションログ等）と新しいカレントステートを返す。
    *   **複合状態対応ロジック**:
        *   `findLCA(IVertex source, IVertex target)`: 遷移元と遷移先の共通の親（Least Common Ancestor）を特定する。
        *   `executeExitChain(IVertex current, IVertex lca)`: 現在の状態からLCAまでのExitアクションを順に実行する。
        *   `executeEntryChain(IVertex target, IVertex lca)`: LCAからターゲットまでのEntryアクションを順に実行する。
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
3.  `SimulationEngine` は `step(transition)` を実行し、状態を更新する。
    *   **複合状態の考慮**:
        1.  遷移元(`source`)と遷移先(`target`)のLCAを特定。
        2.  `source` から `lca` までのExitアクションを実行（記録）。
        3.  遷移自体のActionを実行（記録）。
        4.  `lca` から `target` までのEntryアクションを実行（記録）。
        5.  `target` が複合状態の場合、内部の初期状態(`InitialPseudostate`)を探索し、そこからの遷移を自動実行（再帰的にEntry/Doを実行）。
4.  `DiagramHighlighter` が古い状態のハイライトを解除し、新しい状態をハイライト。
5.  `StmAnalysisView` がログエリアに遷移履歴を追記。
6.  `StmAnalysisView` がイベントボタンリストを更新（次の状態で可能なイベントに差し替え）。
