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
    *   `timerModeCheckbox`: 実時間モードと高速モードを切り替える `JCheckBox` (またはトグルスイッチ)。

### 2.2. Model (ロジック層) - `model` package
*   **`SimulationEngine`**: シミュレーションの中核ロジック。
    *   `currentVertices`: 現在アクティブな `IVertex` のリスト（平行状態対応のため複数保持）。
    *   `historyMap`: 各複合状態(`IState`)において、最後にアクティブだった直下の子状態(`IVertex`)を保持するマップ。履歴状態(`H`, `H*`)の復元に使用。
    *   `executionLog`: シミュレーションの実行履歴（ログテキストまたは構造化データ）を保持する。
    *   `start(IStateMachineDiagram)`: 図から初期状態を探して開始。
    *   `getAvailableTransitions()`: 現在の状態から遷移可能な `ITransition` のリストを返す。
    *   `step(ITransition)`: 指定された遷移を実行し、遷移結果（アクションログ等）と新しいカレントステートを返す。
    *   `scheduleTimeEvent(long delay, Runnable callback)`: 時間イベントをスケジュールする（タイマー管理）。
    *   **複合状態対応ロジック**:
        *   `findLCA(IVertex source, IVertex target)`: 遷移元と遷移先の共通の親（Least Common Ancestor）を特定する。
        *   `executeExitChain(IVertex current, IVertex lca)`: 現在の状態からLCAまでのExitアクションを順に実行する。**この際、退出する複合状態に対して、現在のアクティブな子状態を `historyMap` に記録する。**
        *   `executeEntryChain(IVertex target, IVertex lca)`: LCAからターゲットまでのEntryアクションを順に実行する。
*   **`DiagramHighlighter`**: astah* API (`IDiagramViewManager`) を使用して、現在の状態、直前の状態、遷移をハイライトするヘルパークラス。
    *   `highlightAvailableTransitions(Map<ITransition, Color> transitionColors)`: 遷移可能な遷移を指定された色でハイライトする。

### 2.3. Utility
*   **`TimerManager`**: 時間イベントのスケジューリングとキャンセルを管理する。Swingのタイマーまたは `java.util.Timer` をラップし、UIスレッドでのイベント発火を保証する。
    *   `setMode(TimerMode mode)`: 実時間/高速モードを設定する。
*   **`TestManager`**: テストケースの記録・保存・再生を管理する。
    *   `startRecording()`: 記録モードを開始する。
    *   `stopRecording()`: 記録モードを終了する。
    *   `recordTransition(ITransition transition)`: 実行された遷移（のID）を一時リストに追加する。
    *   `saveTestCase(String name, IStateMachine stateMachine)`: 記録したシーケンスをJSON形式でシリアライズし、StateMachineのタグ付き値(`stm_test_cases`)に保存する。
    *   `loadTestCases(IStateMachine stateMachine)`: TaggedValueからテストケース一覧を読み込む。
    *   `playTestCase(String name, SimulationEngine engine)`: 指定されたテストケースの遷移シーケンスを順次実行する。

## 3. 動的構造（処理フロー）

### 3.1. シミュレーション開始フロー
1.  ユーザーが [Start] ボタンを押下。
2.  `StmAnalysisView` が `SimulationEngine.start()` を呼び出す。
3.  `SimulationEngine` は `IStateMachineDiagram` から `InitialPseudostate` を探索。
4.  初期状態からの遷移を辿り、最初の `State` を特定して `currentVertex` に設定。
5.  `DiagramHighlighter` が初期状態をハイライト。
6.  `StmAnalysisView` が `getAvailableTransitions()` を呼び出し、イベントボタンを生成して表示。
    *   この際、各遷移に色を割り当て、ボタンと図上の遷移線を同じ色で表示するよう `DiagramHighlighter` に指示する。

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
5.  **タイマー処理**: 新しい状態に時間イベント(`tm`)を持つ遷移がある場合、`TimerManager` にタイマーをセットする。
6.  `StmAnalysisView` がログエリアに遷移履歴を追記。
7.  `StmAnalysisView` がイベントボタンリストを更新（次の状態で可能なイベントに差し替え）。

### 3.3. 条件分岐（ジャンクション/選択）の処理
*   **遷移候補の探索 (`getAvailableTransitions`)**:
    *   遷移先が `Junction` または `Choice` の場合、そこで止まらずにその先の遷移（Outgoing）を再帰的に探索する。
    *   ユーザーには、分岐点を超えた最終的な遷移先ごとの選択肢（ボタン）として提示する（例: `Trigger [x>0]`, `Trigger [else]`）。

### 3.4. 時間イベント（タイマー）の処理
*   **自動発火**: 状態遷移後、`tm(msec)` が定義された遷移がある場合、タイマーを開始。時間が経過すると自動的にイベントが発火し、遷移が実行される。
*   **高速モード**: 高速モードが有効な場合、指定された `msec` を無視し、即時（または最小遅延）でイベントを発火させる。
*   **手動介入**: UI上に「タイマー待ち」の状態を表示し、ユーザーが [Skip Wait] ボタン等を押すことで、時間を待たずに即座に発火させることも可能にする（デバッグ効率化のため）。
*   **キャンセル**: タイムアウト前に別のイベントで遷移した場合、セットされていたタイマーはキャンセルされる。

### 3.5. テスト記録・再生の処理
*   **記録**:
    *   ユーザーが [Record] ボタンを押すと記録開始。
    *   イベントボタン押下時に `TestManager.recordTransition` が呼ばれ、遷移IDがリストに蓄積される。
    *   [Stop] ボタンで記録終了し、名前を付けて保存する。
*   **再生**:
    *   ユーザーがテストケースを選択して [Play] ボタンを押す。
    *   シミュレーションをリセット。
    *   記録された遷移IDリストを順に読み込み、現在の状態から出る遷移の中でIDが一致するものを探索して `engine.step()` を実行する。
    *   一致する遷移が見つからない場合（モデルが変更された場合など）はエラーとして停止する。
