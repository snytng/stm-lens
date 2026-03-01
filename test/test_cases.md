# テスト仕様書 (Test Cases)

## No.11 複合状態の実装

### 準備: テスト用モデルの作成
以下の要素を持つステートマシン図を作成し、各状態にEntry/Exitアクションを設定してください。
**（create_test_model_no11.js をスクリプトエディタで実行すると自動作成できます）**

*   **Initial**: トップレベルの初期状態 -> StateA
*   **StateA**: (Entry: "entA", Exit: "exA")
*   **StateB**: (Entry: "entB", Exit: "exB") ※複合状態
    *   **Initial**: 内部の初期状態
    *   **StateB1**: (Entry: "entB1", Exit: "exB1")
    *   **StateB2**: (Entry: "entB2", Exit: "exB2")
    *   遷移: Initial -> StateB1
    *   遷移: StateB1 -> StateB2 (Event: "e2")
*   遷移: StateA -> StateB (Event: "e1")
*   遷移: StateB -> StateA (Event: "e3")
*   遷移: StateA -> StateB2 (Event: "e4")

### テストケース

#### Case 11-1: 複合状態への遷移（初期状態経由）
*   **操作**: Start -> StateAが表示される -> "e1" (StateA -> StateB) を実行
*   **確認事項**: StateBの枠ではなく、内部の **StateB1** がカレント状態になること。
*   **期待ログ順序**:
    1.  `[Exit] exA`
    2.  `[Entry] entB` (親のEntry)
    3.  `[Entry] entB1` (子のEntry: 初期状態から自動遷移)

#### Case 11-2: 複合状態内部での遷移
*   **操作**: (Case 11-1の状態から) "e2" (StateB1 -> StateB2) を実行
*   **確認事項**: StateB1からStateB2へ遷移すること。親(StateB)のEntry/Exitは出ないこと。
*   **期待ログ順序**:
    1.  `[Exit] exB1`
    2.  `[Entry] entB2`

#### Case 11-3: 複合状態からの脱出
*   **操作**: (Case 11-2の状態から) "e3" (StateB -> StateA) を実行
*   **確認事項**: 内部状態から親の枠の遷移を使って脱出できること。
*   **期待ログ順序**:
    1.  `[Exit] exB2` (子のExit)
    2.  `[Exit] exB` (親のExit)
    3.  `[Entry] entA`

#### Case 11-4: 階層をまたぐ直接遷移（飛び込み）
*   **操作**: (StateAの状態から) "e4" (StateA -> StateB2) を実行
*   **確認事項**: 複合状態の中にある状態へ直接遷移できること。
*   **期待ログ順序**:
    1.  `[Exit] exA`
    2.  `[Entry] entB` (親のEntry)
    3.  `[Entry] entB2` (子のEntry)
    *   ※StateB1や初期状態は通らないこと。