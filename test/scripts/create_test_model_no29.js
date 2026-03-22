/*
 * Test Case 29: 内部遷移のテスト用モデル生成スクリプト
 */
var AstahAPI = Java.type("com.change_vision.jude.api.inf.AstahAPI");
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramEditor = projectAccessor.getDiagramEditorFactory().getStateMachineDiagramEditor();
var transactionManager = projectAccessor.getTransactionManager();
var Point2D = java.awt.geom.Point2D;

try {
    transactionManager.beginTransaction();

    var project = projectAccessor.getProject();
    var diagram = diagramEditor.createStatemachineDiagram(project, "TestModel_No29");
    
    var initialPres = diagramEditor.createInitialPseudostate(null, new Point2D.Double(50, 150));
    var stateAPres = diagramEditor.createState("StateA", null, new Point2D.Double(150, 150));
    stateAPres.getModel().setEntry("entryA");
    stateAPres.getModel().setExit("exitA");
    
    var stateBPres = diagramEditor.createState("StateB", null, new Point2D.Double(350, 150));
    stateBPres.getModel().setEntry("entryB");
    stateBPres.getModel().setExit("exitB");

    diagramEditor.createTransition(initialPres, stateAPres);
    
    var t1 = diagramEditor.createTransition(stateAPres, stateBPres);
    t1.setLabel("to_b");

    // 自己遷移 (Self Transition) - 比較用
    var tSelf = diagramEditor.createTransition(stateAPres, stateAPres);
    tSelf.setLabel("self_event");
    tSelf.getModel().setAction("self_act");

    transactionManager.endTransaction();
    print("===============================================================");
    print("モデルの生成が完了しました。");
    print("【重要: 手動操作のお願い】");
    print("astah* APIの制限により、内部遷移をスクリプトから直接作成できません。");
    print("生成された StateA のプロパティビューから「内部遷移」タブを開き、");
    print("以下の内部遷移を手動で追加してください：");
    print("  イベント: internal_event");
    print("  アクション: internal_act");
    print("===============================================================");
    
} catch (e) {
    if (transactionManager.isInTransaction()) {
        transactionManager.abortTransaction();
    }
    print("エラーが発生しました: " + e);
}