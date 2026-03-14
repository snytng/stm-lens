// create_test_model_no24_2.js
//
// This script creates an INVALID state machine diagram for testing TC24-2.
// It has two transitions from StateA with the same event "e1", causing an illegal state.

var projectAccessor = Java.type("com.change_vision.jude.api.inf.AstahAPI").getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();

    var project = projectAccessor.getProject();
    var diagramName = "TestModel_No24_2";

    // Create Diagram
    var diagram = diagramEditor.createStatemachineDiagram(project, diagramName);
    var stateMachine = diagram.getStateMachine();

    // Create elements
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 150));
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 125));
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(350, 50));
    var state0Pres = diagramEditor.createState("状態0", null, new Point2D.Double(350, 200));

    // Create transitions
    diagramEditor.createTransition(initialPres, stateAPres);
    var t1 = diagramEditor.createTransition(stateAPres, stateBPres);
    t1.setLabel("e1");
    var t2 = diagramEditor.createTransition(stateAPres, state0Pres);
    t2.setLabel("e1"); // Same event!

    transactionManager.endTransaction();
    print("Diagram '" + diagramName + "' created successfully.");
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
}