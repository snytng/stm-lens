var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();
    var project = projectAccessor.getProject();
    
    // Diagram作成
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_No15_Complex");

    // Initial State (Top)
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 150));
    
    // StateA
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 150));
    stateAPres.getModel().setExit("exA");

    diagramEditor.createTransition(initialPres, stateAPres);

    // StateB (Parallel State Container)
    // 初期状態を持たず、複数の子状態(Region)を持つことで並行状態として扱われる想定
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(350, 50));
    stateBPres.setWidth(400);
    stateBPres.setHeight(350);
    stateBPres.getModel().setEntry("entB");
    stateBPres.getModel().setExit("exB");

    // --- Region 1 (As a State) ---
    var region1Pres = diagramEditor.createState("Region1", stateBPres, new Point2D.Double(380, 120));
    region1Pres.setWidth(340);
    region1Pres.setHeight(90);
    
    // Deep History in Region1
    var historyBPres = diagramEditor.createDeepHistoryPseudostate(region1Pres, new Point2D.Double(390, 135));
    historyBPres.getModel().setName("H*");

    var r1Init = diagramEditor.createInitialPseudostate(region1Pres, new Point2D.Double(400, 150));
    var s1 = diagramEditor.createState("S1", region1Pres, new Point2D.Double(450, 150));
    s1.getModel().setEntry("entS1");
    var s2 = diagramEditor.createState("S2", region1Pres, new Point2D.Double(550, 150));
    s2.getModel().setEntry("entS2");

    diagramEditor.createTransition(r1Init, s1);
    var t_s1_s2 = diagramEditor.createTransition(s1, s2);
    t_s1_s2.setLabel("e1");

    // --- Region 2 (As a State) ---
    var region2Pres = diagramEditor.createState("Region2", stateBPres, new Point2D.Double(380, 240));
    region2Pres.setWidth(340);
    region2Pres.setHeight(90);

    var r2Init = diagramEditor.createInitialPseudostate(region2Pres, new Point2D.Double(400, 270));
    var s3 = diagramEditor.createState("S3", region2Pres, new Point2D.Double(450, 270));
    s3.getModel().setEntry("entS3");
    var s4 = diagramEditor.createState("S4", region2Pres, new Point2D.Double(550, 270));
    s4.getModel().setEntry("entS4");

    diagramEditor.createTransition(r2Init, s3);
    var t_s3_s4 = diagramEditor.createTransition(s3, s4);
    t_s3_s4.setLabel("e2");

    // --- Transitions ---

    // StateA -> StateB (toB)
    // 通常遷移: StateBに入ると、内部のRegion1, Region2が初期化されるはず
    var t_toB = diagramEditor.createTransition(stateAPres, stateBPres);
    t_toB.setLabel("toB");

    // StateB -> StateA (back)
    // 退出時: Region1, Region2の現在状態が履歴として保存されるはず
    var t_back = diagramEditor.createTransition(stateBPres, stateAPres);
    t_back.setLabel("back");

    // StateA -> StateB.H* (toHistory)
    // 履歴遷移: StateBの履歴(Region1, Region2の状態)が復元されるはず
    var t_hist = diagramEditor.createTransition(stateAPres, historyBPres);
    t_hist.setLabel("toHistory");

    transactionManager.endTransaction();
    print("Test Model No.15 Complex created successfully.");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    if (e.stack) {
        print(e.stack);
    }
}