/*
 * Test Case 11: 複合状態のテスト用モデル生成スクリプト (ECMAScript版)
 * astah* Script Editorで実行してください。
 */

var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();

    // プロジェクトルート取得
    var project = projectAccessor.getProject();
    
    // 1. ステートマシン図作成
    var diagram = diagramEditor.createStatemachineDiagram(project, "Test Case 11");
    var machine = diagram.getStateMachine();
    
    // 1-1. Top level Initial Pseudostate
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(20, 100));

    // 2. StateA 作成 (Top level, parent = null)
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(50, 100));
    var stateA = stateAPres.getModel();
    stateA.setEntry("entA");
    stateA.setExit("exA");
    
    // 3. StateB (複合状態) 作成 (Top level, parent = null)
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(250, 50));
    stateBPres.setWidth(300);
    stateBPres.setHeight(200);
    var stateB = stateBPres.getModel();
    stateB.setEntry("entB");
    stateB.setExit("exB");
    
    // 3-1. StateB 内部要素
    // 親要素(stateBPres)を指定して作成します
    
    // Initial
    var initialBPres = diagramEditor.createInitialPseudostate(stateBPres, new Point2D.Double(280, 100));
    
    // StateB1
    var stateB1Pres = diagramEditor.createState("StateB1", stateBPres, new Point2D.Double(320, 100));
    var stateB1 = stateB1Pres.getModel();
    stateB1.setEntry("entB1");
    stateB1.setExit("exB1");
    
    // StateB2
    var stateB2Pres = diagramEditor.createState("StateB2", stateBPres, new Point2D.Double(450, 100));
    var stateB2 = stateB2Pres.getModel();
    stateB2.setEntry("entB2");
    stateB2.setExit("exB2");
    
    // 4. 遷移作成
    // createTransition(source, target) を使用し、その後ラベルを設定します
    
    // Top Initial -> StateA
    diagramEditor.createTransition(initialPres, stateAPres);

    // Initial -> StateB1
    diagramEditor.createTransition(initialBPres, stateB1Pres);
    
    // StateA -> StateB (e1)
    var t1Pres = diagramEditor.createTransition(stateAPres, stateBPres);
    t1Pres.setLabel("e1");
    
    // StateB1 -> StateB2 (e2)
    var t2Pres = diagramEditor.createTransition(stateB1Pres, stateB2Pres);
    t2Pres.setLabel("e2");
    
    // StateB -> StateA (e3)
    var t3Pres = diagramEditor.createTransition(stateBPres, stateAPres);
    t3Pres.setLabel("e3");
    
    // StateA -> StateB2 (e4)
    var t4Pres = diagramEditor.createTransition(stateAPres, stateB2Pres);
    t4Pres.setLabel("e4");

    transactionManager.endTransaction();
    print("テスト用モデルの生成が完了しました。");
    
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("エラーが発生しました: " + e);
    if (e.stack) {
        print(e.stack);
    }
}