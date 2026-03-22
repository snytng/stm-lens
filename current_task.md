# 現在の作業タスク (In Progress)

## タスク名
N5. 内部遷移（Internal Transition）の実装

## 実装方針・設計メモ
*   **振る舞いの仕様**:
    *   特定の状態に滞在中にトリガーされる。
    *   遷移を実行しても、その状態から退出・進入しない（カレントステートは変わらない）。
    *   状態の `Exit`, `Entry` アクションは実行されない。
    *   内部遷移自身に定義された `Action` (エフェクト) は実行される（ログに出力する）。
*   **astah* APIの確認事項 (探索的検証)**:
    *   内部遷移は `IState.getInternalTransitions()` で取得できる。
    *   【検証結果】取得した `ITransition` の `getSource()` と `getTarget()` は両方とも **自分自身（non-null）** を返す。
    *   【方針】そのため、通常の自己遷移と区別するには、`source` 状態の `getInternalTransitions()` にその遷移が含まれているか（IDで比較）を判定する必要がある。
    *   親状態に定義された内部遷移も、子状態滞在時に発火可能とする（UML仕様準拠）。
*   **エンジンの修正 (`step` メソッド)**:
    *   実行対象の遷移が「内部遷移」であるかを判定するロジックを追加し、該当する場合はLCA計算や状態の更新（`currentVertices` の変更）、Entry/Exitの実行をスキップする。

## TODOリスト
- [x] 1. astah* API検証スクリプトの作成（内部遷移を持つモデルを作成し、APIからどう見えるかダンプする）
- [x] 2. 1の結果に基づき、実装方針の確定と設計ドキュメント(`design.md`, `specification.md`)の更新
- [x] 3. `SimulationEngine.getAvailableTransitions()` の修正（内部遷移の抽出）
- [x] 4. `SimulationEngine.step()` の修正（内部遷移実行時の状態更新スキップ処理）
- [x] 5. 単体テストの作成とテストスクリプトの準備