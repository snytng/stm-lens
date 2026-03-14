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

### No.19 時間遷移イベント（タイマー）の実装

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no19.js をスクリプトエディタで実行すると自動作成できます）**

*   **StateA**: 開始地点
*   **StateB**: 遷移先
*   遷移: **StateA** -> **StateB** (Event: tm(1000))

#### テストケース

##### Case 19-1: 時間経過による自動遷移
*   **操作**: Start -> StateA
*   **確認事項**:
    *   StateAに遷移後、約1000ms経過すると自動的にStateBへ遷移すること。
    *   ログに `[Timer] tm(1000) fired` 等が表示されること。
    *   (高速モード実装後) 高速モードONの場合、待ち時間なしで遷移すること。

nch### No.20 テスト記録・再生機能の実装

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no20.js をスクリプトエディタで実行すると自動作成できます）**

*   **StateA**: 開始地点
*   **StateB**: 中間地点
*   **StateC**: 終了地点
*   遷移: **StateA** -> **StateB** (Event: e1)
*   遷移: **StateB** -> **StateC** (Event: e2)

#### テストケース

##### Case 20-1: テストの記録と再生
*   **操作**:
    1.  Start -> StateA
    2.  [Record] ボタンを押下（記録開始）。
    3.  "e1" -> StateB, "e2" -> StateC と遷移させる。
    4.  [Stop] ボタンを押下し、テスト名 "Test1" で保存。
    5.  [Reset] ボタンを押下。
    6.  テストケース一覧から "Test1" を選択し、[Play] ボタンを押下。
*   **確認事項**:
    *   自動的に StateA -> StateB -> StateC と遷移し、StateC で停止すること。
    *   ログに再生による遷移であることが示されること（任意）。

### No.21 時間遷移イベント（タイマー）の実装

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no21.js をスクリプトエディタで実行すると自動作成できます）**

*   **SelectTest**: テストケース分岐用の状態
*   **Wait1sec**: tm(1000)を持つ状態
*   **Wait5sec_Or_Cancel**: tm(5000)とcancelイベントを持つ状態
*   **Fast_Wait10sec**: tm(10000)を持つ状態（高速モード確認用）
*   **Composite_Timer**: 内部にタイマー遷移を持つ複合状態

#### テストケース

##### Case 21-1: 基本的なタイマー遷移
*   **操作**:
    1.  Start -> SelectTest
    2.  "test_basic" -> Wait1sec
*   **確認事項**:
    *   Wait1secに遷移後、約1000ms経過すると自動的に **AutoMoved** へ遷移すること。
    *   ログに `[Timer] tm(1000) fired` 等が表示されること。

##### Case 21-2: タイマーのキャンセル
*   **操作**:
    1.  (Case 21-1の続き) AutoMoved -> "next" -> Wait5sec_Or_Cancel
    2.  遷移後、すぐに "cancel" を実行 -> ManualMoved
*   **確認事項**:
    *   ManualMovedへ遷移すること。
    *   その後5000ms経過しても、**TimeoutMoved** への遷移（タイマー発火）が発生しないこと。

##### Case 21-3: 高速モード（Fast Mode）
*   **前提**: 高速モードをONにする（UI実装後）。
*   **操作**:
    1.  Start -> SelectTest
    2.  "test_fast" -> Fast_Wait10sec
*   **確認事項**:
    *   Fast_Wait10secに遷移後、10000ms待つことなく、即座（または短時間）に **Fast_Done** へ遷移すること。

##### Case 21-4: 不正なフォーマットの無視
*   **操作**:
    1.  Start -> SelectTest
    2.  "test_invalid" -> Invalid_Wait
*   **確認事項**:
    *   `tm(abc)` や `tm(-100)` などのイベント定義があってもエラーが発生しないこと。
    *   自動遷移が発生せず、Invalid_Waitにとどまること。

##### Case 21-5: 複合状態内のタイマー
*   **操作**:
    1.  Start -> SelectTest
    2.  "test_composite" -> Composite_Timer (-> Comp_Wait1sec)
*   **確認事項**:
    *   内部の初期状態から **Comp_Wait1sec** へ遷移すること。
    *   約1000ms経過後、自動的に **Comp_Done** へ遷移すること。

### No.22 タイマーモードの動的切替対応

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no21.js を使用）**

#### テストケース

##### Case 22-1: Normalモード待機中にFastモードへ切り替え
*   **操作**:
    1.  Start -> SelectTest -> "test_fast" -> Fast_Wait10sec へ遷移。（Normalモードで実行）
    2.  `tm(10000)` の待機中（ボタンが `(Waiting...)` 表示）に、[Fast Mode] チェックボックスをONにする。
*   **確認事項**:
    *   チェックボックスをONにした直後、10秒待つことなく **Fast_Done** へ自動遷移すること。
    *   ログに `--- Event: tm(10000) (Fast) ---` と表示されること。

### No.23 並行状態の同時遷移（Run-to-Completion）

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no23.js をスクリプトエディタで実行すると自動作成できます）**

#### テストケース

##### Case 23-1: イベントによる同時遷移
*   **操作**:
    1.  Start -> StateA (S1, S3 がアクティブ)
    2.  "e1" を実行
*   **確認事項**:
    *   S1 -> S2, S3 -> S4 の両方の遷移が一度に実行されること。
    *   カレントステートが S2, S4 になること。

##### Case 23-2: 独立したタイマー動作
*   **操作**:
    1.  (Case 23-1の後) "toB" -> StateB (T1, T3 がアクティブ)
    2.  そのまま待機（Normalモード推奨）
*   **確認事項**:
    *   約500ms後に T3 -> T4 が遷移すること。
    *   そのさらに約500ms後（開始から計1000ms後）に T1 -> T2 が遷移すること。
    *   T3の遷移によってT1のタイマーがリセットされ、合計1500msかかったりしないこと。

### No.24 異常状態の検出とエラー表示

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no24.js をスクリプトエディタで実行すると自動作成できます）**
*   **StateA**: 開始地点
*   **StateB**: 遷移先
*   遷移: **StateA** -> **StateB** (Event: e1)

#### テストケース

##### Case 24-1: 異常状態検出時のUI応答テスト
*   **手順**:
    1.  （事前準備）`SimulationEngine.step()`メソッド内の、古い状態をクリアする処理（例: `currentVertices.clear()`）を一時的にコメントアウトし、プラグインをビルドする。
    2.  `TestModel_No24`をastah*で開く。
    3.  Stm Lensを起動し、[Start]ボタンを押す。カレントステートが`StateA`になることを確認する。
    4.  `StateA`から`StateB`へ遷移するイベントボタン "e1" をクリックする。
*   **期待結果**:
    1.  内部的にカレントステートが`[StateA, StateB]`の2つになる。
    2.  状態検証ロジックがこれを検知し、例外をスローする。
    3.  UI上に「【異常検出】シミュレーションを停止しました。」といったモーダルダイアログが表示され、エラー内容（例: 非並行状態で複数のカレントステートが検出されました）が記載されていることを確認する。
    4.  ダイアログを閉じた後、イベントボタンがすべて非活性化され、[Reset]ボタンのみが押せる状態になっていることを確認する。
*   **事後処理**:
    *   一時的に変更した`SimulationEngine.step()`のコードを元に戻す。

##### Case 24-2: 不正なモデルによる異常状態の検知
*   **モデル**: TestModel_No24_2 (test/scripts/create_test_model_no24_2.js)
    *   StateAから "e1" で StateB と 状態0 の両方へ遷移する（UML違反のモデル）。
*   **操作**:
    1.  Start -> StateA
    2.  "e1" を実行
*   **期待結果**:
    1.  内部的にカレントステートが `StateB` と `状態0` の2つになる（非並行領域で複数状態）。
    2.  エラーダイアログが表示され、「Multiple active states...」といったメッセージが出ること。
    3.  シミュレーションが安全に停止すること。

### No.25 タイムトラベルデバッグ機能

#### 準備: テスト用モデルの作成
**（test/scripts/create_test_model_no25.js をスクリプトエディタで実行すると自動作成できます）**
*   **StateA**: 開始状態
*   **StateB**: 分岐元
*   **StateC**: 到達先1
*   **StateD**: 到達先2
*   遷移: **StateA** -> **StateB** (Event: e1)
*   遷移: **StateB** -> **StateC** (Event: e2)
*   遷移: **StateB** -> **StateD** (Event: e3)

#### テストケース

##### Case 25-1: 基本的なナビゲーション操作
*   **操作**:
    1.  Start を押し `StateA` になることを確認。
    2.  "e1" を押し `StateB` になることを確認。
    3.  "e2" を押し `StateC` になることを確認。
    4.  `[<]` (戻る) ボタンを押す。
*   **確認事項**:
    *   カレントステートが `StateB` に戻り、図のハイライトも `StateB` に戻ること。
    *   イベントボタンとして "e2", "e3" が表示されること。
*   **操作**:
    5.  `[|<<]` (最初へ) ボタンを押す。
*   **確認事項**:
    *   `StateA` に戻ること。イベントボタンは "e1" のみ。
*   **操作**:
    6.  `[>>|]` (最後へ) ボタンを押す。
*   **確認事項**:
    *   `StateC` に進むこと。

##### Case 25-2: 過去からの履歴の分岐
*   **操作**:
    1.  上記の `StateC` まで進めた状態から、`[<]` ボタンを押して `StateB` に戻る。
    2.  この状態で、まだ実行していない "e3" ボタンを押す。
*   **確認事項**:
    *   `StateD` へ遷移すること。
*   **操作**:
    3.  `[<]` を押して再び `StateB` に戻る。
    4.  `[>]` (進む) ボタンを押す。
*   **確認事項**:
    *   古い履歴だった `StateC` ではなく、新しい履歴である `StateD` へ進むこと（未来の履歴が上書きされていること）。
