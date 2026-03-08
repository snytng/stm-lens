var projectAccessor = Java.type("com.change_vision.jude.api.inf.AstahAPI").getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();

    var project = projectAccessor.getProject();
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_No23");
    var statemachine = diagram.getStateMachine();

    // Initial
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 50));

    // StateA (Orthogonal State for Event Test)
    // Note: No Initial Pseudostate directly under StateA -> Treated as Orthogonal by SimulationEngine
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 50));
    stateAPres.setWidth(300);
    stateAPres.setHeight(300);
    var stateA = stateAPres.getModel();

    // Region 1
    var region1Pres = diagramEditor.createState("Region1", stateAPres, new Point2D.Double(170, 80));
    region1Pres.setWidth(120);
    region1Pres.setHeight(100);
    var r1Init = diagramEditor.createInitialPseudostate(region1Pres, new Point2D.Double(180, 100));
    var s1 = diagramEditor.createState("S1", region1Pres, new Point2D.Double(200, 100));
    var s2 = diagramEditor.createState("S2", region1Pres, new Point2D.Double(250, 100));
    diagramEditor.createTransition(r1Init, s1);
    var t1 = diagramEditor.createTransition(s1, s2);
    t1.setLabel("e1");

    // Region 2
    var region2Pres = diagramEditor.createState("Region2", stateAPres, new Point2D.Double(170, 200));
    region2Pres.setWidth(120);
    region2Pres.setHeight(100);
    var r2Init = diagramEditor.createInitialPseudostate(region2Pres, new Point2D.Double(180, 220));
    var s3 = diagramEditor.createState("S3", region2Pres, new Point2D.Double(200, 220));
    var s4 = diagramEditor.createState("S4", region2Pres, new Point2D.Double(250, 220));
    diagramEditor.createTransition(r2Init, s3);
    var t2 = diagramEditor.createTransition(s3, s4);
    t2.setLabel("e1");

    // StateB (Orthogonal State for Timer Test)
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(500, 50));
    stateBPres.setWidth(300);
    stateBPres.setHeight(300);
    
    // Region 3 (Timer 1000ms)
    var region3Pres = diagramEditor.createState("Region3", stateBPres, new Point2D.Double(520, 80));
    region3Pres.setWidth(120);
    region3Pres.setHeight(100);
    var r3Init = diagramEditor.createInitialPseudostate(region3Pres, new Point2D.Double(530, 100));
    var t_state1 = diagramEditor.createState("T1", region3Pres, new Point2D.Double(550, 100));
    var t_state2 = diagramEditor.createState("T2", region3Pres, new Point2D.Double(600, 100));
    diagramEditor.createTransition(r3Init, t_state1);
    var tr1 = diagramEditor.createTransition(t_state1, t_state2);
    tr1.setLabel("tm(1000)");

    // Region 4 (Timer 500ms)
    var region4Pres = diagramEditor.createState("Region4", stateBPres, new Point2D.Double(520, 200));
    region4Pres.setWidth(120);
    region4Pres.setHeight(100);
    var r4Init = diagramEditor.createInitialPseudostate(region4Pres, new Point2D.Double(530, 220));
    var t_state3 = diagramEditor.createState("T3", region4Pres, new Point2D.Double(550, 220));
    var t_state4 = diagramEditor.createState("T4", region4Pres, new Point2D.Double(600, 220));
    diagramEditor.createTransition(r4Init, t_state3);
    var tr2 = diagramEditor.createTransition(t_state3, t_state4);
    tr2.setLabel("tm(500)");

    // Transitions
    diagramEditor.createTransition(initialPres, stateAPres);
    
    var toB = diagramEditor.createTransition(stateAPres, stateBPres);
    toB.setLabel("toB");

    transactionManager.endTransaction();
    print("Test Model No.23 created successfully.");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    e.printStackTrace();
}