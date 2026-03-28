# 現在の作業タスク (In Progress)

## タスク名
T38: テストケースコンボボックスのキャンセル操作時の誤作動修正 (B19)

## 時間記録
*   **開始日時**: 2024-05-24 15:30 (※仮置き)
*   **終了日時**: -
*   **所要時間**: -

## 実装方針・設計メモ
*   **原因**: `JComboBox` の `ActionListener` は、ポップアップがキャンセル（外側をクリック等）された際にも発火してしまうため。
*   **対策1 (ItemListener)**: `ActionListener` を廃止し、`ItemListener` で `ItemEvent.SELECTED` を検知した時のみロード処理を行うようにする。
*   **対策2 (フラグ制御)**: `isUpdatingComboBox` フラグを導入し、`updateTestCaseList` によるリスト再構築時や、保存・リネーム後のプログラムからの `setSelectedItem` 時に、ロード処理が誤って連鎖発火するのを防ぐ。

## TODOリスト
- [ ] 1. 設計・仕様の合意（現在ここ）
- [ ] 2. `StmAnalysisView.java` への `isUpdatingComboBox` フラグの追加。
- [ ] 3. イベントリスナーの `ItemListener` への変更。
- [ ] 4. プログラムからのコンボボックス操作箇所へのフラグ適用。
- [ ] 5. 動作確認。