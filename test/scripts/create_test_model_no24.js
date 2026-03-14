// create_test_model_no24.js
//
// This script creates a simple state machine diagram for testing TC24 (Abnormal State Detection).
//
// To run:
// 1. Open Astah.
// 2. Open a project.
// 3. Select [Tool] -> [Script Editor].
// 4. Paste this script and run.

var projectAccessor = Java.type("com.change_vision.jude.api.inf.AstahAPI").getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();

    var project = projectAccessor.getProject();
    var diagramName = "TestModel_No24";

    // Create Diagram
    var diagram = diagramEditor.createStatemachineDiagram(project, diagramName);
    var stateMachine = diagram.getStateMachine();

    // Create elements
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 150));
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 125));
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(350, 125));

    // Create transitions
    diagramEditor.createTransition(initialPres, stateAPres);
    var t1 = diagramEditor.createTransition(stateAPres, stateBPres);
    t1.setLabel("e1");

    transactionManager.endTransaction();
    print("Diagram '" + diagramName + "' created successfully.");
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
}