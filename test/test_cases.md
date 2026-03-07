# Stm Lens for astah* テストケース

## 1. 概要
本ドキュメントは、Stm Lens for astah* プラグインの品質を保証するためのテストケースを管理する。
テストは以下の2つの観点で行う。

*   **妥当性確認 (Validation)**: `specification.md` に基づき、ユーザーの課題を解決する正しい機能が実装されているかを確認する。
*   **機能検証 (Verification)**: `design.md` に基づき、各機能が設計通りに正しく動作するかを確認する。

---

## 2. 妥当性確認テスト (Validation Tests)

`specification.md` の要求仕様が満たされているかを確認する。

| ID | テスト対象 (仕様) | テスト内容 | 期待される結果 |
| :--- | :--- | :--- | :--- |


---

## 3. 機能検証テスト (Verification Tests)

`design.md` の設計仕様が満たされているかを確認する。

| ID | テスト対象 (機能) | テスト内容 | 期待される結果 |
| :--- | :--- | :--- | :--- |

## 4. 実装フェーズ別テストケース

### No.11 複合状態の実装

#### 準備: テスト用モデルの作成
以下の要素を持つステートマシン図を作成し、各状態にEntry/Exitアクションを設定してください。
**（test/scripts/create_test_model_no11.js をスクリプトエディタで実行すると自動作成できます）**

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

#### テストケース

##### Case 11-1: 複合状態への遷移（初期状態経由）
*   **操作**: Start -> StateAが表示される -> "e1" (StateA -> StateB) を実行
*   **確認事項**: StateBの枠ではなく、内部の **StateB1** がカレント状態になること。
*   **期待ログ順序**:
    1.  `[Exit] exA`
    2.  `[Entry] entB` (親のEntry)
    3.  `[Entry] entB1` (子のEntry: 初期状態から自動遷移)

##### Case 11-2: 複合状態内部での遷移
*   **操作**: (Case 11-1の状態から) "e2" (StateB1 -> StateB2) を実行
*   **確認事項**: StateB1からStateB2へ遷移すること。親(StateB)のEntry/Exitは出ないこと。
*   **期待ログ順序**:
    1.  `[Exit] exB1`
    2.  `[Entry] entB2`

##### Case 11-3: 複合状態からの脱出
*   **操作**: (Case 11-2の状態から) "e3" (StateB -> StateA) を実行
*   **確認事項**: 内部状態から親の枠の遷移を使って脱出できること。
*   **期待ログ順序**:
    1.  `[Exit] exB2` (子のExit)
    2.  `[Exit] exB` (親のExit)
    3.  `[Entry] entA`

##### Case 11-4: 階層をまたぐ直接遷移（飛び込み）
*   **操作**: (StateAの状態から) "e4" (StateA -> StateB2) を実行
*   **確認事項**: 複合状態の中にある状態へ直接遷移できること。
*   **期待ログ順序**:
    1.  `[Exit] exA`
    2.  `[Entry] entB` (親のEntry)
    3.  `[Entry] entB2` (子のEntry)
    *   ※StateB1や初期状態は通らないこと。

### No.12 平行状態の実装

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no12.js をスクリプトエディタで実行すると自動作成できます）**
*   **StateA**: 複合状態。内部に2つの独立した遷移フロー（リージョン）を持つ。
    *   Region 1: Initial -> S1 -> S2 (Event: e1)
    *   Region 2: Initial -> S3 -> S4 (Event: e2)
*   **StateEnd**: StateAからの遷移先 (Event: e3)

#### テストケース

##### Case 12-1: 平行状態への遷移と同時アクティブ化
*   **操作**: Start -> StateAへ遷移
*   **確認事項**: StateA内部の **S1** と **S3** が同時にカレント状態として表示されること。
*   **期待ログ**: `[Entry] entA`, `[Entry] entS1`, `[Entry] entS3` (順不同可)

##### Case 12-2: 各リージョンの独立遷移
*   **操作**: "e1" (S1 -> S2) を実行
*   **確認事項**: S1がS2に遷移し、カレント状態が **S2, S3** になること。S3は変化しないこと。
*   **期待ログ**: `[Exit] exS1`, `[Entry] entS2`

##### Case 12-3: 複合状態からの脱出（全リージョンの終了）
*   **操作**: "e3" (StateA -> StateEnd) を実行
*   **確認事項**: StateAから抜け、内部のS2, S3も全て終了し、StateEndのみになること。
*   **期待ログ**: 
    1. `[Exit] exS2` (または exS3)
    2. `[Exit] exS3` (または exS2)
    3. `[Exit] exA`
    4. `Transition: StateA -> StateEnd`

### No.13 複合状態退出時のハイライト強化

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no13.js をスクリプトエディタで実行すると自動作成できます）**
*   **StateA**: 複合状態 (内部に S1)
*   **StateB**: 外部状態
*   遷移: **StateA** -> **StateB** (Event: e1)
*   遷移: **StateB** -> **StateA** (Event: e2)

#### テストケース

##### Case 13-1: 複合状態退出時のハイライト
*   **操作**: Start -> StateA (S1) -> "e1" (StateA -> StateB) を実行
*   **確認事項**:
    *   カレント状態は **StateB** (黄色/緑枠)。
    *   直前の状態として、**StateA** (親) と **S1** (子) の**両方**がマゼンタの枠線でハイライトされること。
    *   遷移矢印 (e1) がマゼンタになること。

### No.14 イベントと遷移のカラーペアリング

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no14.js をスクリプトエディタで実行すると自動作成できます）**
*   **StateA**: 分岐元
*   遷移: **StateA** -> **StateB** (e1)
*   遷移: **StateA** -> **StateC** (e2)
*   遷移: **StateA** -> **StateD** (e3)
*   遷移: **StateA** -> **StateA** (e4)

#### テストケース

##### Case 14-1: カラーペアリングの確認
*   **操作**: Start -> StateA に遷移。
*   **確認事項**:
    *   イベントボタン (e1, e2, e3, e4) がそれぞれ異なる背景色で表示されること。
    *   イベントボタンの文字色が背景色に合わせて調整されていること（濃い色など）。
    *   図上の遷移矢印 (e1, e2, e3, e4) が、対応するボタンと同じ色でハイライトされること。

### No.15 履歴状態の実装

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no15.js をスクリプトエディタで実行すると自動作成できます）**

*   **StateA**: 開始地点
*   **StateB**: 複合状態 (浅い履歴テスト用)
    *   **H**: 浅い履歴 (Shallow History)
    *   **Initial** -> **S1**
    *   **S1** -> **S2** (Event: e1)
*   **StateC**: 複合状態 (深い履歴テスト用)
    *   **H***: 深い履歴 (Deep History)
    *   **Initial** -> **C1** (複合状態)
        *   **Initial** -> **Sub1**
        *   **Sub1** -> **Sub2** (Event: e2)
*   遷移: **StateA** -> **StateB** (Event: toB_init) ※初期状態へ
*   遷移: **StateA** -> **StateB.H** (Event: toB_hist) ※履歴へ
*   遷移: **StateB** -> **StateA** (Event: back)
*   遷移: **StateA** -> **StateC** (Event: toC_init) ※初期状態へ
*   遷移: **StateA** -> **StateC.H*** (Event: toC_hist) ※履歴へ
*   遷移: **StateC** -> **StateA** (Event: back)

#### テストケース

##### Case 15-1: 浅い履歴 (Shallow History) の動作
*   **操作**:
    1.  Start -> StateA
    2.  "toB_init" -> StateB (S1)
    3.  "e1" -> S2
    4.  "back" -> StateA
    5.  "toB_hist" -> StateB (履歴経由)
*   **確認事項**: StateB内部で、初期状態のS1ではなく、直前の **S2** に復帰すること。
*   **期待ログ**: `[Entry] entS2` (S1は通らない)

##### Case 15-2: 深い履歴 (Deep History) の動作
*   **操作**:
    1.  Start -> StateA
    2.  "toC_init" -> StateC (C1 -> Sub1)
    3.  "e2" -> Sub2
    4.  "back" -> StateA
    5.  "toC_hist" -> StateC (履歴経由)
*   **確認事項**: StateC内部のC1内部で、初期状態のSub1ではなく、深い階層の **Sub2** に復帰すること。
*   **期待ログ**: `[Entry] entC1`, `[Entry] entSub2` (Sub1は通らない)

##### Case 15-3: 複合・並行・履歴の組み合わせ
**（test/scripts/create_test_model_no15_complex.js でモデル作成）**

*   **モデル概要**:
    *   **StateA**: 開始状態
    *   **StateB**: 並行状態（Region1, Region2）。
        *   **Region1**: H* (Deep History) あり。S1 -> S2
        *   **Region2**: 履歴なし。S3 -> S4
*   **選定理由**:
    *   並行状態において、履歴状態を持つリージョンと持たないリージョンの復帰動作の違いを確認する。
    *   Region1は履歴により状態が復元され、Region2は初期化されることを検証する。

*   **操作**:
    1.  Start -> StateA
    2.  "toB" -> StateB (初期状態: S1, S3)
    3.  "e1" -> S2 (Region1の状態変更)
    4.  "e2" -> S4 (Region2の状態変更)
    5.  "back" -> StateA (StateBから退出、履歴保存)
    6.  "toHistory" -> StateB (履歴へ遷移)

*   **確認事項**:
    *   StateBに復帰した際、**Region1** (H*あり) は直前の **S2** に復帰すること。
    *   **Region2** (H*なし) は初期状態経由で **S3** になること (S4には戻らない)。
*   **期待ログ**:
    *   `[Entry] entS2`
    *   `[Entry] entS3`
    *   (順不同可)

##### Case 15-4: 履歴なしでの履歴状態への遷移
*   **モデル**: Case 15-3 と同じ (TestModel_No15_Complex)
*   **操作**:
    1.  Start -> StateA
    2.  "toHistory" -> StateB (履歴へ遷移) ※一度もStateBに入っていない状態で実行
*   **確認事項**:
    *   履歴がないため、デフォルトの挙動として初期状態から開始されること。
    *   Region1 (H*あり) は S1 (初期状態) になること。
    *   Region2 (H*なし) は S3 (初期状態) になること。
*   **期待ログ**:
    *   `[Entry] entS1`
    *   `[Entry] entS3`
    *   (順不同可)

### No.17 条件分岐（ジャンクション）の実装

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no17.js をスクリプトエディタで実行すると自動作成できます）**

*   **StateA**: 開始地点
*   **Junction1**: ジャンクション
*   **StateB**: 分岐先1 (Guard: x > 0)
*   **StateC**: 分岐先2 (Guard: else)
*   遷移: **StateA** -> **Junction1** (Event: e1)
*   遷移: **Junction1** -> **StateB** (Guard: x > 0)
*   遷移: **Junction1** -> **StateC** (Guard: else)

#### テストケース

##### Case 17-1: ジャンクションによる分岐
*   **操作**: Start -> StateA
*   **確認事項**:
    *   イベントボタンとして "e1 [x > 0]" と "e1 [else]" の2つが表示されること。
*   **操作**: "e1 [x > 0]" を選択
*   **確認事項**: StateB に遷移すること。
*   **期待ログ**: `[Exit] exA`, `[Entry] entB` (Junctionは通過のみ)

### No.18 条件分岐（選択）の実装

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no18_choice.js をスクリプトエディタで実行すると自動作成できます）**

*   **StateA**: 開始地点
*   **Choice1**: 選択 (Choice)
*   **StateB**: 分岐先1 (Guard: x > 0)
*   **StateC**: 分岐先2 (Guard: else)
*   遷移: **StateA** -> **Choice1** (Event: e1)
*   遷移: **Choice1** -> **StateB** (Guard: x > 0)
*   遷移: **Choice1** -> **StateC** (Guard: else)

#### テストケース

##### Case 18-1: 選択（Choice）による分岐
*   **操作**: Start -> StateA
*   **確認事項**:
    *   イベントボタンとして "e1 [x > 0]" と "e1 [else]" の2つが表示されること。
*   **操作**: "e1 [else]" を選択
*   **確認事項**: StateC に遷移すること。
*   **期待ログ**: `[Exit] exA`, `[Entry] entC` (Choiceは通過のみ)

### No.19 遷移パスのLCA計算ロジック修正

#### 準備: テスト用モデルの作成
*   **外部Choice**: `test/scripts/create_test_model_no19_external_choice.js`
*   **内部Choice**: `test/scripts/create_test_model_no19_internal_choice.js`

#### テストケース

##### Case 19-1: 外部の擬似状態を経由する遷移
*   **モデル**: 外部Choiceモデル (`StateA`が`State0`の内部、`Choice`が`State0`の外部)
*   **操作**:
    1.  Start -> `State0` (`StateA`)
    2.  "e1 [else]" を選択 (`StateA` -> `Choice` -> `StateA`)
*   **確認事項**:
    *   遷移パスが親状態(`State0`)の境界をまたぐため、`State0`のExit/Entryが実行されること。
*   **期待ログ**: `[Exit] exA`, `[Exit] exState0`, `[Entry] entState0`, `[Entry] entA` (順序は実装によるが、親のExit/Entryが含まれること)

##### Case 19-2: 内部の擬似状態を経由する遷移
*   **モデル**: 内部Choiceモデル (`StateA`と`Choice`が`State0`の内部)
*   **操作**:
    1.  Start -> `State0` (`StateA`)
    2.  "e1 [else]" を選択 (`StateA` -> `Choice` -> `StateA`)
*   **確認事項**:
    *   遷移パスが親状態(`State0`)の内部で完結するため、`State0`のExit/Entryは実行されないこと。
*   **期待ログ**: `[Exit] exA`, `[Entry] entA` (親のExit/Entryが含まれないこと)
