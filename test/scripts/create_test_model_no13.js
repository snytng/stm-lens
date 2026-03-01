/*
 * Test Case 13: 複合状態退出時のハイライト強化用モデル生成スクリプト (ECMAScript版)
 * astah* Script Editorで実行してください。
 */

var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();

    var project = projectAccessor.getProject();
    var diagram = diagramEditor.createStatemachineDiagram(project, "Test Case 13");
    var machine = diagram.getStateMachine();
    
    // Top Initial
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(20, 100));

    // StateA (Composite)
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(100, 50));
    stateAPres.setWidth(200);
    stateAPres.setHeight(200);
    var stateA = stateAPres.getModel();
    stateA.setEntry("entA");
    stateA.setExit("exA");
    
    // Internal S1
    var init1Pres = diagramEditor.createInitialPseudostate(stateAPres, new Point2D.Double(120, 100));
    var s1Pres = diagramEditor.createState("S1", stateAPres, new Point2D.Double(160, 100));
    s1Pres.getModel().setEntry("entS1");
    s1Pres.getModel().setExit("exS1");
    
    // StateB (External)
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(400, 100));
    
    // Transitions
    // Top -> StateA
    diagramEditor.createTransition(initialPres, stateAPres);
    
    // Internal: Init -> S1
    diagramEditor.createTransition(init1Pres, s1Pres);
    
    // External: StateA -> StateB (e1)
    var t1Pres = diagramEditor.createTransition(stateAPres, stateBPres);
    t1Pres.setLabel("e1");
    
    // Back: StateB -> StateA (e2)
    var t2Pres = diagramEditor.createTransition(stateBPres, stateAPres);
    t2Pres.setLabel("e2");

    transactionManager.endTransaction();
    print("テスト用モデルの生成が完了しました。");
    
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("エラーが発生しました: " + e);
}