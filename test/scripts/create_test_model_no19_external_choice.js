var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();
    var project = projectAccessor.getProject();
    
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_No19_ExternalChoice");

    // Top-level Initial
    var initialTopPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(20, 100));

    // State0 (Composite)
    var state0Pres = diagramEditor.createState("State0", null, new Point2D.Double(100, 50));
    state0Pres.setWidth(200);
    state0Pres.setHeight(150);
    state0Pres.getModel().setEntry("entState0");
    state0Pres.getModel().setExit("exState0");

    // StateA (inside State0)
    var stateAPres = diagramEditor.createState("StateA", state0Pres, new Point2D.Double(150, 100));
    stateAPres.getModel().setEntry("entA");
    stateAPres.getModel().setExit("exA");

    // Initial inside State0
    var initial0Pres = diagramEditor.createInitialPseudostate(state0Pres, new Point2D.Double(120, 100));
    diagramEditor.createTransition(initial0Pres, stateAPres);

    // Choice (outside State0)
    var choicePres = diagramEditor.createChoicePseudostate(null, new Point2D.Double(350, 100));

    // StateB (outside State0)
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(500, 50));
    stateBPres.getModel().setEntry("entB");

    // StateC (outside State0)
    var stateCPres = diagramEditor.createState("StateC", null, new Point2D.Double(500, 150));
    stateCPres.getModel().setEntry("entC");

    // --- Transitions ---
    // Top Initial -> State0
    diagramEditor.createTransition(initialTopPres, state0Pres);

    // StateA -> Choice (e1)
    var t1 = diagramEditor.createTransition(stateAPres, choicePres);
    t1.setLabel("e1");

    // Choice -> StateB ([x > 0])
    var t2 = diagramEditor.createTransition(choicePres, stateBPres);
    t2.setLabel("[x > 0]");
    t2.getModel().setGuard("x > 0");

    // Choice -> StateC ([x <= 0])
    var t3 = diagramEditor.createTransition(choicePres, stateCPres);
    t3.setLabel("[x <= 0]");
    t3.getModel().setGuard("x <= 0");

    // Choice -> StateA ([else])
    var t4 = diagramEditor.createTransition(choicePres, stateAPres);
    t4.setLabel("[else]");
    t4.getModel().setGuard("else");

    transactionManager.endTransaction();
    print("Test Model No.19 (External Choice) created successfully.");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    if (e.stack) {
        print(e.stack);
    }
}