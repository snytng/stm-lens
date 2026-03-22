// dump_internal_transitions.js
// 目的: astah* 上の IState が持つ内部遷移と自己遷移の違いをAPIからどう見えるかダンプする。

var api = Java.type("com.change_vision.jude.api.inf.AstahAPI").getAstahAPI();
var projectAccessor = api.getProjectAccessor();
var viewManager = projectAccessor.getViewManager().getDiagramViewManager();
var diagram = viewManager.getCurrentDiagram();

var IStateMachineDiagram = Java.type("com.change_vision.jude.api.inf.model.IStateMachineDiagram");
var IState = Java.type("com.change_vision.jude.api.inf.model.IState");

print("--- Start Dumping Internal Transitions ---");

function dumpState(state) {
    print("State: " + state.getName() + " (ID: " + state.getId() + ")");
    
    // 通常の遷移 (getOutgoings)
    var outgoings = state.getOutgoings();
    print("  Outgoings (count: " + outgoings.length + "):");
    for (var i = 0; i < outgoings.length; i++) {
        var t = outgoings[i];
        var sourceName = t.getSource() ? t.getSource().getName() : "null";
        var targetName = t.getTarget() ? t.getTarget().getName() : "null";
        
        print("    [" + i + "] " + sourceName + " -> " + targetName);
        print("        source is null? : " + (t.getSource() === null));
        print("        target is null? : " + (t.getTarget() === null));
    }
    
    // 内部遷移 (getInternalTransitions)
    var internals = state.getInternalTransitions();
    print("  InternalTransitions (count: " + internals.length + "):");
    for (var j = 0; j < internals.length; j++) {
        var it = internals[j];
        var itSourceName = it.getSource() ? it.getSource().getName() : "null";
        var itTargetName = it.getTarget() ? it.getTarget().getName() : "null";
        
        print("    [" + j + "] " + itSourceName + " -> " + itTargetName);
        print("        source is null? : " + (it.getSource() === null));
        print("        target is null? : " + (it.getTarget() === null));
    }
    print("----------------------------------------");
}

function dumpVertices(vertexes) {
    if (!vertexes) return;
    for (var i = 0; i < vertexes.length; i++) {
        var v = vertexes[i];
        if (v instanceof IState) {
            dumpState(v);
            if (v.getSubvertexes) {
                dumpVertices(v.getSubvertexes());
            }
        }
    }
}

if (diagram instanceof IStateMachineDiagram) {
    dumpVertices(diagram.getStateMachine().getVertexes());
} else {
    print("Please open a State Machine Diagram and try again.");
}
print("--- End Dump ---");