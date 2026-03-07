var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();
    var project = projectAccessor.getProject();
    
    // Diagram作成
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_No15");
    var stateMachine = diagram.getStateMachine();

    // Initial State (Top)
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(20, 100));
    
    // StateA
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(100, 100));

    // Initial -> StateA
    diagramEditor.createTransition(initialPres, stateAPres);

    // --- StateB (Shallow History Test) ---
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(300, 50));
    stateBPres.setWidth(250);
    stateBPres.setHeight(200);
    
    // Shallow History in StateB
    var historyBPres = diagramEditor.createShallowHistoryPseudostate(stateBPres, new Point2D.Double(320, 80));
    historyBPres.getModel().setName("H");

    // Initial in StateB
    var initialBPres = diagramEditor.createInitialPseudostate(stateBPres, new Point2D.Double(320, 150));
    
    // S1 in StateB
    var s1Pres = diagramEditor.createState("S1", stateBPres, new Point2D.Double(360, 150));
    s1Pres.getModel().setEntry("entS1");

    // S2 in StateB
    var s2Pres = diagramEditor.createState("S2", stateBPres, new Point2D.Double(450, 150));
    s2Pres.getModel().setEntry("entS2");

    // Transitions inside StateB
    diagramEditor.createTransition(initialBPres, s1Pres);
    var t_s1_s2 = diagramEditor.createTransition(s1Pres, s2Pres);
    t_s1_s2.setLabel("e1");

    // --- StateC (Deep History Test) ---
    var stateCPres = diagramEditor.createState("StateC", null, new Point2D.Double(300, 300));
    stateCPres.setWidth(300);
    stateCPres.setHeight(250);

    // Deep History in StateC
    var historyCPres = diagramEditor.createDeepHistoryPseudostate(stateCPres, new Point2D.Double(320, 330));
    historyCPres.getModel().setName("H*");

    // Initial in StateC
    var initialCPres = diagramEditor.createInitialPseudostate(stateCPres, new Point2D.Double(320, 400));

    // C1 (Composite) in StateC
    var c1Pres = diagramEditor.createState("C1", stateCPres, new Point2D.Double(360, 380));
    c1Pres.setWidth(200);
    c1Pres.setHeight(150);
    c1Pres.getModel().setEntry("entC1");

    // Initial in C1
    var initialC1Pres = diagramEditor.createInitialPseudostate(c1Pres, new Point2D.Double(380, 420));

    // Sub1 in C1
    var sub1Pres = diagramEditor.createState("Sub1", c1Pres, new Point2D.Double(420, 420));
    sub1Pres.getModel().setEntry("entSub1");

    // Sub2 in C1
    var sub2Pres = diagramEditor.createState("Sub2", c1Pres, new Point2D.Double(500, 420));
    sub2Pres.getModel().setEntry("entSub2");

    // Transitions inside C1
    diagramEditor.createTransition(initialC1Pres, sub1Pres);
    var t_sub1_sub2 = diagramEditor.createTransition(sub1Pres, sub2Pres);
    t_sub1_sub2.setLabel("e2");

    // Transitions inside StateC (Initial -> C1)
    diagramEditor.createTransition(initialCPres, c1Pres);

    // --- Top Level Transitions ---
    
    // StateA -> StateB (toB_init)
    var t_toB_init = diagramEditor.createTransition(stateAPres, stateBPres);
    t_toB_init.setLabel("toB_init");

    // StateA -> StateB.H (toB_hist)
    var t_toB_hist = diagramEditor.createTransition(stateAPres, historyBPres);
    t_toB_hist.setLabel("toB_hist");

    // StateB -> StateA (back)
    var t_backB = diagramEditor.createTransition(stateBPres, stateAPres);
    t_backB.setLabel("back");

    // StateA -> StateC (toC_init)
    var t_toC_init = diagramEditor.createTransition(stateAPres, stateCPres);
    t_toC_init.setLabel("toC_init");

    // StateA -> StateC.H* (toC_hist)
    var t_toC_hist = diagramEditor.createTransition(stateAPres, historyCPres);
    t_toC_hist.setLabel("toC_hist");

    // StateC -> StateA (back)
    var t_backC = diagramEditor.createTransition(stateCPres, stateAPres);
    t_backC.setLabel("back");

    transactionManager.endTransaction();
    print("Test Model No.15 created successfully.");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    if (e.stack) {
        print(e.stack);
    }
}