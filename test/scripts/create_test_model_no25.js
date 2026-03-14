// create_test_model_no25.js
//
// This script creates a state machine diagram with a branch for testing Time Travel Debugging.
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
    var diagramName = "TestModel_No25";

    // Create Diagram
    var diagram = diagramEditor.createStatemachineDiagram(project, diagramName);
    var stateMachine = diagram.getStateMachine();

    // Create elements
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 150));
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 150));
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(300, 150));
    var stateCPres = diagramEditor.createState("StateC", null, new Point2D.Double(450, 100));
    var stateDPres = diagramEditor.createState("StateD", null, new Point2D.Double(450, 200));

    // Create transitions
    diagramEditor.createTransition(initialPres, stateAPres);
    diagramEditor.createTransition(stateAPres, stateBPres).setLabel("e1");
    diagramEditor.createTransition(stateBPres, stateCPres).setLabel("e2");
    diagramEditor.createTransition(stateBPres, stateDPres).setLabel("e3");

    transactionManager.endTransaction();
    print("Diagram '" + diagramName + "' created successfully.");
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
}