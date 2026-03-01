/*
 * Test Case 12: 平行状態のテスト用モデル生成スクリプト (ECMAScript版)
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
    var diagram = diagramEditor.createStatemachineDiagram(project, "Test Case 12");
    var machine = diagram.getStateMachine();
    
    // Top Initial
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(20, 100));

    // StateA (Composite & Parallel)
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(100, 50));
    stateAPres.setWidth(400);
    stateAPres.setHeight(300);
    var stateA = stateAPres.getModel();
    stateA.setEntry("entA");
    stateA.setExit("exA");
    
    // Region 1 (Implicit via Initial State 1)
    var init1Pres = diagramEditor.createInitialPseudostate(stateAPres, new Point2D.Double(120, 100));
    var s1Pres = diagramEditor.createState("S1", stateAPres, new Point2D.Double(160, 100));
    s1Pres.getModel().setEntry("entS1");
    s1Pres.getModel().setExit("exS1");
    
    var s2Pres = diagramEditor.createState("S2", stateAPres, new Point2D.Double(300, 100));
    s2Pres.getModel().setEntry("entS2");
    s2Pres.getModel().setExit("exS2");
    
    // Region 2 (Implicit via Initial State 2)
    var init2Pres = diagramEditor.createInitialPseudostate(stateAPres, new Point2D.Double(120, 200));
    var s3Pres = diagramEditor.createState("S3", stateAPres, new Point2D.Double(160, 200));
    s3Pres.getModel().setEntry("entS3");
    s3Pres.getModel().setExit("exS3");
    
    var s4Pres = diagramEditor.createState("S4", stateAPres, new Point2D.Double(300, 200));
    s4Pres.getModel().setEntry("entS4");
    s4Pres.getModel().setExit("exS4");
    
    // State End
    var stateEndPres = diagramEditor.createState("StateEnd", null, new Point2D.Double(550, 150));
    
    // Transitions
    // Top -> StateA
    diagramEditor.createTransition(initialPres, stateAPres);
    
    // Region 1: Init -> S1
    diagramEditor.createTransition(init1Pres, s1Pres);
    // Region 1: S1 -> S2 (e1)
    var t1 = diagramEditor.createTransition(s1Pres, s2Pres);
    t1.setLabel("e1");
    
    // Region 2: Init -> S3
    diagramEditor.createTransition(init2Pres, s3Pres);
    // Region 2: S3 -> S4 (e2)
    var t2 = diagramEditor.createTransition(s3Pres, s4Pres);
    t2.setLabel("e2");
    
    // Cross Region? No, just independent.
    
    // Exit Composite: StateA -> StateEnd (e3)
    var t3 = diagramEditor.createTransition(stateAPres, stateEndPres);
    t3.setLabel("e3");

    transactionManager.endTransaction();
    print("平行状態テスト用モデルの生成が完了しました。");
    
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("エラーが発生しました: " + e);
}