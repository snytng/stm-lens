/*
 * Anonymous Transition Test Model Script
 */

var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();

    var project = projectAccessor.getProject();
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_Anonymous");
    
    // Initial
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 50));

    // StateA
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 50));
    diagramEditor.createTransition(initialPres, stateAPres);

    // StateB
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(350, 0));

    // StateC
    var stateCPres = diagramEditor.createState("StateC", null, new Point2D.Double(350, 100));

    // Anonymous Transitions from StateA
    var t1 = diagramEditor.createTransition(stateAPres, stateBPres);
    t1.getModel().setGuard("x > 0");

    var t2 = diagramEditor.createTransition(stateAPres, stateCPres);
    t2.getModel().setGuard("else");

    transactionManager.endTransaction();
    print("Anonymous Transition Test Model created successfully.");
    
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
}