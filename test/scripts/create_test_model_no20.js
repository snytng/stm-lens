var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();
    var project = projectAccessor.getProject();
    
    // Diagram作成
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_No20");
    var stateMachine = diagram.getStateMachine();

    // Initial State
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 50));
    
    // StateA
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 50));
    stateAPres.getModel().setEntry("entA");

    diagramEditor.createTransition(initialPres, stateAPres);

    // StateB
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(300, 50));
    stateBPres.getModel().setEntry("entB");

    // StateC
    var stateCPres = diagramEditor.createState("StateC", null, new Point2D.Double(450, 50));
    stateCPres.getModel().setEntry("entC");

    // Transitions
    var t1 = diagramEditor.createTransition(stateAPres, stateBPres);
    t1.setLabel("e1");

    var t2 = diagramEditor.createTransition(stateBPres, stateCPres);
    t2.setLabel("e2");

    transactionManager.endTransaction();
    print("Test Model No.20 created successfully.");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    if (e.stack) {
        print(e.stack);
    }
}