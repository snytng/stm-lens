# 現在の作業タスク (T40)

## タスク名
B21: ドッグフーディングによる自作ツールの設計検証

## 時間記録
*   **開始日時**: 2024-05-24 18:00
*   **終了日時**: -
*   **所要時間**: -

## 実装方針・設計メモ
1.  **メタ・モデルの作成**: `design.md` の Mermaid 定義に基づいた状態マシン図（StmLens_UI_Modes）を astah* 上に作成する。
    *   APIスクリプト (`create_test_model_no40.js`) を使用して、骨格を自動生成する。
2.  **検証シナリオの作成**: プラグインの主要なユースケースを網羅するテストスクリプトを作成する。
    *   Case A: 手動操作によるテスト作成（Idle -> AutoGenerate -> FireEvent）
    *   Case B: 保存済みテストのデバッグ（Idle -> Protected -> RunScript -> Protected）
    *   Case C: 派生テストの作成（Protected -> [FireEvent/TimeTravel] -> AutoGenerate）
3.  **セルフシミュレーションの実施**: Stm Lens を使って、Stm Lens のモデルを動かし、矛盾がないか確認する。

## TODOリスト
- [x] 検証用モデル生成スクリプトの修正と実行 (`create_test_model_no40.js`)
- [ ] 生成された図（StmLens_UI_Modes_Diagram）をastah*上で見やすく整える
- [x] テストスクリプト (`c:\tools\astah-plugins\stm\test\verification_scenario_no40.txt`) の作成と修正
- [ ] シミュレーション実行と KPT (B21の結果) の記録