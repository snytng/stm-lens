# 現在の作業タスク (進行中)

## タスク名
T43 (B27). UI状態マシンの再設計

## 時間記録
*   **開始日時**: 2024-05-25 17:00
*   **終了日時**: -
*   **所要時間**: -

## 実装方針・設計メモ
### [設計の正解]
*   UI状態マシンの構成は、`c:\tools\astah-plugins\stm\test\scripts\design_UI.js` (Trial 12) で定義されたモデルを正解とする。
*   Mermaid図に依存せず、astah*上で生成される図を正として実装・検証を行う。

### [合意済み] T1.1: モード統合・合流ロジックの定義
*   **目的**: データのソース（自作かロードか）に依存せず、「同期中か保護中か」というユーザーの作業コンテキストに基づき振る舞いを決定する。
*   **Live (同期)**: エディタと同期し、操作を追記し続ける状態。
*   **Viewing (閲覧)**: 履歴を遡っている、あるいはロードした資産を表示している保護（ReadOnly）状態。
*   **合流（Branching）**: 
    *   `Viewing` 状態でイベントを発火させた場合、そこまでの履歴を元にエディタを確定（未来削除、または再構築）させ、`Live` へ遷移する。

### [合意済み] T1.2: UI振る舞いの定義
*   **Viewing (閲覧)**:
    *   「Zero Manual Edit」に基づき、エディタは **ReadOnly** とする。
    *   エディタ背景色: `new Color(245, 245, 245)` (Light Gray) 等で「非同期・保護状態」を明示。
*   **Live (最新)**:
    *   エディタ編集: `setEditable(false)`。プログラムによる自動追記のみを許可し、ユーザーの手動タイピングを完全に遮断する。
    *   エディタ背景色: デフォルト (White)。

## TODOリスト
- [x] T0: design.md の設計を Astah (design_UI.js) で確認可能にする
    - [x] Trial 1: Mermaid (Failed - Preview Error)
    - [x] Trial 2: Astah Script (Failed - InvalidEditingException: Parameter should be set correctly)
    - [x] Trial 3: Astah Script (Failed - InvalidEditingException: Parameter should be set correctly - Boundary issue)
    - [x] Trial 4: Astah Script (Failed - ClassCastException - Model vs Presentation 不整合)
    - [x] Trial 5: Astah Script (Failed - TypeError - ModelEditorFactory.getStateMachineModelEditor メソッド未定義)
    - [x] Trial 6: Astah Script (Failed - InvalidEditingException: Parameter should be set correctly - transition creation)
    - [x] Trial 7: Astah Script (Failed - InvalidEditingException: Parameter should be set correctly - Initialize transition)
    - [x] Trial 8: Astah Script (Failed - InvalidEditingException: Parameter should be set correctly - Initialize transition)
    - [x] Trial 9: Astah Script (Failed - InvalidEditingException at Initialize transition)
    - [x] Trial 10: Astah Script (Success - 遷移作成の安定化とコンテナ明示)
    - [x] Trial 11: Astah Script (Failed - ClassCastException: Cannot cast zd to INodePresentation - Model vs Presentation 不整合)
    - [x] Trial 12: Astah Script (Success - 親要素に null を指定してフラット化を完遂)

### T1: 設計合意
- [x] T1.1: モード統合・合流ロジックのコンセプト合意
- [x] T1.2: UI振る舞い（背景色・ReadOnly等）の定義合意

### T2: 実装（合意後に着手）
- [x] T2.1: 状態管理基盤 (UiMode Enum) の導入
- [x] T2.2: ライフサイクル(Start/Load/Reset/Run)に伴う遷移実装
- [x] T2.3: UIフィードバック（背景色・編集不可）の実装
- [x] T2.3.1: StmAnalysisView における UiMode フィールド欠落の修正 (Fixing Compilation Error)
- [x] T2.4: 履歴閲覧中の表示共通化（[VIEWING] ラベルの追加など）
- [x] T2.5: モード合流 (Branching) ロジックの実装 (手動イベント発火による LIVE 復帰を確認)
- [x] T2.6: ステートマシン設計(Idle/Running含む)に基づく表示変更処理の一元化
- [x] T2.7: 重複した View クラスの削除とパッケージ構成の整理

### T3: 最終検証
- [ ] T3.1: クリーンアップ（古い判定メソッド削除）
- [ ] T3.2: 全モード間の遷移テスト実施