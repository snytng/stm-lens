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