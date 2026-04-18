// design_UI.js: Stm Lens UI Mode State Machine
var projectAccessor = Java.type("com.change_vision.jude.api.inf.AstahAPI").getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    print(">> Starting model creation (Trial 12)...");
    transactionManager.beginTransaction();

    var project = projectAccessor.getProject();
    var timestamp = new java.text.SimpleDateFormat("MMdd_HHmm_ss").format(new java.util.Date());
    var diagram = diagramEditor.createStatemachineDiagram(project, "UI_Design_" + timestamp);

    // --- 1. Nodes (Flat structure, null parent for top-level) ---
    var initial = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 100));
    var idle = diagramEditor.createState("Idle", null, new Point2D.Double(150, 100));
    var live = diagramEditor.createState("Live", null, new Point2D.Double(400, 100));
    var viewing = diagramEditor.createState("Viewing", null, new Point2D.Double(700, 100));
    var running = diagramEditor.createState("TestRunningMode", null, new Point2D.Double(400, 300));

    // --- 2. Transitions ---
    print(">> Creating transitions...");
    diagramEditor.createTransition(initial, idle); // Label 無し (安定のため)

    diagramEditor.createTransition(idle, live).setLabel("Start");
    diagramEditor.createTransition(idle, viewing).setLabel("Load");

    diagramEditor.createTransition(live, live).setLabel("FireEvent_Sync");
    diagramEditor.createTransition(live, viewing).setLabel("TimeTravel_Past");
    diagramEditor.createTransition(viewing, viewing).setLabel("TimeTravel_Move");
    diagramEditor.createTransition(viewing, live).setLabel("FireEvent_Branch");

    diagramEditor.createTransition(live, idle).setLabel("Reset");
    diagramEditor.createTransition(viewing, idle).setLabel("Reset");
    diagramEditor.createTransition(live, running).setLabel("RunScript");

    diagramEditor.createTransition(running, viewing).setLabel("RunComplete");
    diagramEditor.createTransition(running, idle).setLabel("Reset");

    transactionManager.endTransaction();
    print(">> UI Design State Machine created successfully (Trial 12).");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    if (e.javaException) {
        var sw = new java.io.StringWriter();
        e.javaException.printStackTrace(new java.io.PrintWriter(sw));
        print(sw.toString());
    }
}