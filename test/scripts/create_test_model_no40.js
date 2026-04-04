// TestModel_No40: Stm Lens Meta-State Machine
var projectAccessor = Java.type("com.change_vision.jude.api.inf.AstahAPI").getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

function createModel() {
    try {
        transactionManager.beginTransaction();

        var project = projectAccessor.getProject();
        var diagram = diagramEditor.createStatemachineDiagram(project, "StmLens_UI_Modes_Diagram");

        // Pseudostates
        var initial = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 100));

        // States
        var idle = diagramEditor.createState("Idle", null, new Point2D.Double(150, 100));
        var autoGen = diagramEditor.createState("AutoGenerateMode", null, new Point2D.Double(400, 100));
        var protected = diagramEditor.createState("ProtectedMode", null, new Point2D.Double(150, 250));
        var running = diagramEditor.createState("TestRunningMode", null, new Point2D.Double(400, 250));

        // Transitions
        diagramEditor.createTransition(initial, idle);
        
        // From Idle
        diagramEditor.createTransition(idle, autoGen).setLabel("Start() [手動]");
        diagramEditor.createTransition(idle, protected).setLabel("Load()");

        // From AutoGenerateMode
        diagramEditor.createTransition(autoGen, idle).setLabel("Reset()");
        diagramEditor.createTransition(autoGen, protected).setLabel("Load()");
        diagramEditor.createTransition(autoGen, running).setLabel("RunScript()");
        diagramEditor.createTransition(autoGen, autoGen).setLabel("FireEvent() / TimeTravel()");

        // From ProtectedMode
        diagramEditor.createTransition(protected, idle).setLabel("Reset()");
        diagramEditor.createTransition(protected, protected).setLabel("Load()");
        diagramEditor.createTransition(protected, running).setLabel("RunScript()");
        // The core logic of deriving tests:
        var t_derive = diagramEditor.createTransition(protected, autoGen);
        t_derive.setLabel("FireEvent() / TimeTravel()");
        t_derive.getModel().setAction("エディタを再構築");

        // From TestRunningMode
        diagramEditor.createTransition(running, protected).setLabel("RunComplete()");
        diagramEditor.createTransition(running, idle).setLabel("Reset() [強制]");

        transactionManager.endTransaction();
        print("TestModel_No40 (Meta-Model) created successfully.");
    } catch (e) {
        if (transactionManager.isInTransaction()) {
            transactionManager.abortTransaction();
        }
        print("Error: " + e);
    }
}
createModel();