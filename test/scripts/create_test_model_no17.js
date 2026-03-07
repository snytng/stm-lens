var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();
    var project = projectAccessor.getProject();
    
    // Diagram作成
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_No17");
    var stateMachine = diagram.getStateMachine();

    // Initial State
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 50));
    
    // StateA
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 50));
    stateAPres.getModel().setExit("exA");

    diagramEditor.createTransition(initialPres, stateAPres);

    // Junction
    var junctionPres = diagramEditor.createJunctionPseudostate(null, new Point2D.Double(300, 50));

    // StateB
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(450, 0));
    stateBPres.getModel().setEntry("entB");

    // StateC
    var stateCPres = diagramEditor.createState("StateC", null, new Point2D.Double(450, 100));
    stateCPres.getModel().setEntry("entC");

    // Transitions
    // StateA -> Junction (e1)
    var t1 = diagramEditor.createTransition(stateAPres, junctionPres);
    t1.setLabel("e1");

    // Junction -> StateB (x > 0)
    var t2 = diagramEditor.createTransition(junctionPres, stateBPres);
    t2.setLabel("[x > 0]");
    t2.getModel().setGuard("x > 0");

    // Junction -> StateC (else)
    var t3 = diagramEditor.createTransition(junctionPres, stateCPres);
    t3.setLabel("[else]");
    t3.getModel().setGuard("else");

    transactionManager.endTransaction();
    print("Test Model No.17 created successfully.");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    if (e.stack) {
        print(e.stack);
    }
}