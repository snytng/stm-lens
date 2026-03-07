var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();
    var project = projectAccessor.getProject();
    
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_Junction_MultipleEntries");

    // Initial
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 50));
    
    // StateA
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 50));
    diagramEditor.createTransition(initialPres, stateAPres);

    // Junction
    var junctionPres = diagramEditor.createJunctionPseudostate(null, new Point2D.Double(300, 50));

    // StateB
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(450, 0));
    
    // StateC
    var stateCPres = diagramEditor.createState("StateC", null, new Point2D.Double(450, 100));

    // Transitions from StateA to Junction
    // Event e1
    var t1 = diagramEditor.createTransition(stateAPres, junctionPres);
    t1.setLabel("e1");
    
    // Event e2
    var t2 = diagramEditor.createTransition(stateAPres, junctionPres);
    t2.setLabel("e2");

    // Transitions from Junction
    var t3 = diagramEditor.createTransition(junctionPres, stateBPres);
    t3.setLabel("[x > 0]");
    t3.getModel().setGuard("x > 0");

    var t4 = diagramEditor.createTransition(junctionPres, stateCPres);
    t4.setLabel("[else]");
    t4.getModel().setGuard("else");

    transactionManager.endTransaction();
    print("Test Model created successfully.");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    if (e.stack) {
        print(e.stack);
    }
}