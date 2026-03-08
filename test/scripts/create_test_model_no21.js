var projectAccessor = Java.type("com.change_vision.jude.api.inf.AstahAPI").getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();

    var project = projectAccessor.getProject();
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_No21");
    var statemachine = diagram.getStateMachine();

    // States
    // Initial
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 50));
    
    // SelectTest
    var selectTestPres = diagramEditor.createState("SelectTest", null, new Point2D.Double(150, 50));
    
    // Final
    var finalStatePres = diagramEditor.createFinalState(null, new Point2D.Double(800, 300));

    // 1. Basic Test (T21-01, 02, 03)
    var state1Pres = diagramEditor.createState("Wait1sec", null, new Point2D.Double(300, 50));
    var state2Pres = diagramEditor.createState("AutoMoved", null, new Point2D.Double(450, 50));
    var state3Pres = diagramEditor.createState("Wait5sec_Or_Cancel", null, new Point2D.Double(600, 50));
    var state4Pres = diagramEditor.createState("ManualMoved", null, new Point2D.Double(750, 0));
    var state5Pres = diagramEditor.createState("TimeoutMoved", null, new Point2D.Double(750, 100));

    // 2. Fast Mode Test (T21-04)
    var stateFast1Pres = diagramEditor.createState("Fast_Wait10sec", null, new Point2D.Double(300, 200));
    var stateFast2Pres = diagramEditor.createState("Fast_Done", null, new Point2D.Double(500, 200));

    // 3. Invalid Format Test (T21-05)
    var stateInv1Pres = diagramEditor.createState("Invalid_Wait", null, new Point2D.Double(300, 300));
    var stateInv2Pres = diagramEditor.createState("Invalid_Done", null, new Point2D.Double(500, 300));

    // 4. Composite State Test (T21-06)
    var stateCompPres = diagramEditor.createState("Composite_Timer", null, new Point2D.Double(300, 450));
    stateCompPres.setWidth(300);
    stateCompPres.setHeight(200);
    
    var compInitPres = diagramEditor.createInitialPseudostate(stateCompPres, new Point2D.Double(320, 500));
    var stateComp1Pres = diagramEditor.createState("Comp_Wait1sec", stateCompPres, new Point2D.Double(360, 500));
    var stateComp2Pres = diagramEditor.createState("Comp_Done", stateCompPres, new Point2D.Double(500, 500));

    // Transitions
    // Initial -> SelectTest
    diagramEditor.createTransition(initialPres, selectTestPres);

    // 1. Basic Path
    var t_basic = diagramEditor.createTransition(selectTestPres, state1Pres);
    t_basic.setLabel("test_basic");
    
    var t1 = diagramEditor.createTransition(state1Pres, state2Pres);
    t1.setLabel("tm(1000)");
    
    var t2 = diagramEditor.createTransition(state2Pres, state3Pres);
    t2.setLabel("next");
    
    var t3 = diagramEditor.createTransition(state3Pres, state4Pres);
    t3.setLabel("cancel");
    
    var t4 = diagramEditor.createTransition(state3Pres, state5Pres);
    t4.setLabel("tm(5000)");
    
    var t_end1 = diagramEditor.createTransition(state4Pres, finalStatePres);
    t_end1.setLabel("end");
    var t_end2 = diagramEditor.createTransition(state5Pres, finalStatePres);
    t_end2.setLabel("end");

    // 2. Fast Path
    var t_fast = diagramEditor.createTransition(selectTestPres, stateFast1Pres);
    t_fast.setLabel("test_fast");
    
    var t_fast_tm = diagramEditor.createTransition(stateFast1Pres, stateFast2Pres);
    t_fast_tm.setLabel("tm(10000)");
    
    var t_fast_end = diagramEditor.createTransition(stateFast2Pres, finalStatePres);
    t_fast_end.setLabel("end");

    // 3. Invalid Path
    var t_inv = diagramEditor.createTransition(selectTestPres, stateInv1Pres);
    t_inv.setLabel("test_invalid");
    
    var t_inv_tm1 = diagramEditor.createTransition(stateInv1Pres, stateInv2Pres);
    t_inv_tm1.setLabel("tm(abc)");
    
    var t_inv_tm2 = diagramEditor.createTransition(stateInv1Pres, stateInv2Pres);
    t_inv_tm2.setLabel("tm(-100)");
    
    var t_inv_end = diagramEditor.createTransition(stateInv2Pres, finalStatePres);
    t_inv_end.setLabel("end");

    // 4. Composite Path
    var t_comp = diagramEditor.createTransition(selectTestPres, stateCompPres);
    t_comp.setLabel("test_composite");
    
    diagramEditor.createTransition(compInitPres, stateComp1Pres);
    
    var t_comp_tm = diagramEditor.createTransition(stateComp1Pres, stateComp2Pres);
    t_comp_tm.setLabel("tm(1000)");
    
    var t_comp_end = diagramEditor.createTransition(stateComp2Pres, finalStatePres);
    t_comp_end.setLabel("end");

    transactionManager.endTransaction();
    print("Timer Test Model created successfully.");

} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("Error: " + e);
    e.printStackTrace();
}