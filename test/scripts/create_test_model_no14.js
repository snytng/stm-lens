/*
 * Test Case 14: イベントと遷移のカラーペアリング用モデル生成スクリプト (ECMAScript版)
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
    var diagram = diagramEditor.createStatemachineDiagram(project, "Test Case 14");
    var machine = diagram.getStateMachine();
    
    // Top Initial
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 150));

    // StateA (Source)
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 150));
    
    // StateB (Target 1)
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(350, 50));
    
    // StateC (Target 2)
    var stateCPres = diagramEditor.createState("StateC", null, new Point2D.Double(350, 150));
    
    // StateD (Target 3)
    var stateDPres = diagramEditor.createState("StateD", null, new Point2D.Double(350, 250));
    
    // Transitions
    // Initial -> StateA
    diagramEditor.createTransition(initialPres, stateAPres);
    
    // StateA -> StateB (e1)
    var t1Pres = diagramEditor.createTransition(stateAPres, stateBPres);
    t1Pres.setLabel("e1");
    
    // StateA -> StateC (e2)
    var t2Pres = diagramEditor.createTransition(stateAPres, stateCPres);
    t2Pres.setLabel("e2");
    
    // StateA -> StateD (e3)
    var t3Pres = diagramEditor.createTransition(stateAPres, stateDPres);
    t3Pres.setLabel("e3");
    
    // StateA -> StateA (e4 - Self transition)
    var t4Pres = diagramEditor.createTransition(stateAPres, stateAPres);
    t4Pres.setLabel("e4");

    transactionManager.endTransaction();
    print("テスト用モデルの生成が完了しました。");
    
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("エラーが発生しました: " + e);
}