var AstahAPI = com.change_vision.jude.api.inf.AstahAPI;
var projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
var diagramViewManager = projectAccessor.getViewManager().getDiagramViewManager();
var diagram = diagramViewManager.getCurrentDiagram();

print("=== Checking Tagged Values for Stm Lens ===");

if (diagram != null) {
    try {
        if (diagram.getStateMachine) {
            var sm = diagram.getStateMachine();
            print("StateMachine: " + sm.getName());
            
            var tvs = sm.getTaggedValues();
            var found = false;
            for (var i = 0; i < tvs.length; i++) {
                var tv = tvs[i];
                print("Key: " + tv.getKey() + " | Value: " + tv.getValue());
                found = true;
            }
            if (!found) {
                print("No tagged values found on this StateMachine.");
            }
        } else {
             print("Current diagram does not support getStateMachine().");
        }
    } catch (e) {
        print("Error: " + e);
    }
} else {
    print("No diagram is opened.");
}