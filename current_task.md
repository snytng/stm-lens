# 現在の作業タスク (T40)

## タスク名
B21: ドッグフーディングによる自作ツールの設計検証

## 時間記録
*   **開始日時**: 2024-05-24 18:00
*   **終了日時**: 2024-05-24 19:30
*   **所要時間**: -

## 実装方針・設計メモ
1.  **メタ・モデルの作成**: `design.md` の Mermaid 定義に基づいた状態マシン図（StmLens_UI_Modes）を astah* 上に作成する。
    *   APIスクリプト (`create_test_model_no40.js`) を使用して、骨格を自動生成する。
2.  **検証シナリオの作成**: プラグインの主要なユースケースを網羅するテストスクリプトを作成する。
    *   Case A: 手動操作によるテスト作成（Idle -> AutoGenerate -> FireEvent）
    *   Case B: 保存済みテストのデバッグ（Idle -> Protected -> RunScript -> Protected）
    *   Case C: 派生テストの作成（Protected -> [FireEvent/TimeTravel] -> AutoGenerate）
3.  **セルフシミュレーションの実施**: Stm Lens を使って、Stm Lens のモデルを動かし、矛盾がないか確認する。
4.  **KPTによる振り返り**:
    *   **Keep**:
        *   Mermaid定義からAPIスクリプトを通じてastah*モデルを自動生成する手法は、設計と実装の同期に非常に有効だった。
        *   `AssertLog` を活用することで、状態遷移に伴う内部アクション（エディタの再構築など）の実行を確実に検証できた。
        *   自作ツールで自作ツールの設計を動かすことで、UI上のラベル名とモデル上のイベント名の一致ルール（アクション名を含めるかどうか）の不一致を早期に発見できた。
    *   **Problem**:
        *   astah* APIの `setLabel` はイベント、ガード、アクションをパースするため、テストスクリプト側で指定する「ボタン名」との完全一致が崩れやすい。
        *   APIスクリプトの実行環境（Nashorn/GraalJS）によるクラスアクセスの細かな差異で数回のリトライが発生した。
    *   **Try**:
        *   `TestRunner` におけるイベントボタンの検索ロジックを、完全一致だけでなく「前方一致」や「正規表現」でも可能にする検討をする（UXの柔軟性向上）。
        *   **TimeTravelの振る舞い精緻化**: 閲覧モード(Protected)と分岐モード(AutoGenerate)を厳密に区別するよう実装を見直す。
        *   今後の複雑な擬似状態（Fork/Join）の実装前にも、同様のメタ・モデルによる検証プロセスを組み込む。

## TODOリスト
- [x] 検証用モデル生成スクリプトの修正と実行 (`create_test_model_no40.js`)
- [x] 生成された図（StmLens_UI_Modes_Diagram）をastah*上で見やすく整える
- [x] テストスクリプト (`c:\tools\astah-plugins\stm\test\verification_scenario_no40.txt`) の作成と修正
- [x] 設計の精緻化（AutoGenerateModeの複合状態化とSaveイベント追加）
- [x] 修正したメタ・モデルによる再検証と動作確認
- [x] シミュレーション実行と KPT (B21の結果) の記録