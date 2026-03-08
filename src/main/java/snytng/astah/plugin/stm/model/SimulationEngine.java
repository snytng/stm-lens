package snytng.astah.plugin.stm.model;

import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IPseudostate;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

public class SimulationEngine {

    private List<IVertex> currentVertices = new ArrayList<>();
    private List<IVertex> previousVertices = new ArrayList<>();
    private Map<IState, IVertex> historyMap = new HashMap<>();
    private ITransition lastTransition;
    
    private final TimerManager timerManager = new TimerManager();
    private Consumer<StepResult> stepListener;
    private TimerListener timerListener;

    public interface TimerListener {
        void onTimerFired(List<TransitionPath> paths);
    }

    public void setStepListener(Consumer<StepResult> listener) {
        this.stepListener = listener;
    }

    public void setFastMode(boolean fast) {
        timerManager.setFastMode(fast);
    }

    public boolean isFastMode() {
        return timerManager.isFastMode();
    }

    public void setTimerListener(TimerListener listener) {
        this.timerListener = listener;
    }

    public static class TransitionPath {
        public final List<ITransition> transitions;
        public TransitionPath(List<ITransition> transitions) {
            this.transitions = transitions;
        }
        public ITransition getRoot() { return transitions.get(0); }
        public ITransition getLast() { return transitions.get(transitions.size() - 1); }
        public IVertex getSource() { return getRoot().getSource(); }
        public IVertex getTarget() { return getLast().getTarget(); }
    }

    public static class StepResult {
        public final IVertex source;
        public final IVertex target;
        public final TransitionPath path;
        public final List<String> exitActions;
        public final List<String> transitionActions;
        public final List<String> entryActions;
        public final String doActivity;
        public final String note;

        public StepResult(IVertex source, IVertex target, TransitionPath path,
                          List<String> exitActions, List<String> transitionActions, List<String> entryActions, String doActivity, String note) {
            this.source = source;
            this.target = target;
            this.path = path;
            this.exitActions = exitActions;
            this.transitionActions = transitionActions;
            this.entryActions = entryActions;
            this.doActivity = doActivity;
            this.note = note;
        }
    }

    public StepResult start(IStateMachineDiagram diagram) {
        timerManager.cancel();
        currentVertices.clear();
        previousVertices.clear();
        historyMap.clear();
        lastTransition = null;

        if (diagram == null) return null;

        IStateMachine sm = diagram.getStateMachine();
        if (sm == null) return null;
        
        List<String> entryActions = new ArrayList<>();
        List<String> exitActions = new ArrayList<>();
        String transitionAction = null;

        for (IVertex vertex : sm.getVertexes()) {
            if (vertex instanceof IPseudostate) {
                IPseudostate ps = (IPseudostate) vertex;
                if (ps.isInitialPseudostate()) {
                    ITransition[] outgoings = ps.getOutgoings();
                    if (outgoings != null && outgoings.length > 0) {
                        ITransition t = outgoings[0];
                        transitionAction = t.getAction();
                        IVertex target = t.getTarget();
                        
                        if (target instanceof IState) {
                            String entry = ((IState) target).getEntry();
                            if (entry != null && !entry.isEmpty()) {
                                entryActions.add(entry);
                            }
                        }

                        List<String> transitionActions = new ArrayList<>();
                        if (transitionAction != null) transitionActions.add(transitionAction);
                        
                        currentVertices.addAll(drillDown(target, entryActions));
                        
                        // Update history for highlighting (Initial state as previous)
                        previousVertices.add(ps);
                        lastTransition = t;

                        TransitionPath path = new TransitionPath(Collections.singletonList(t));
                        
                        checkTimers();
                        return new StepResult(ps, target, path, exitActions, transitionActions, entryActions, null, null);
                    } else {
                        currentVertices.add(ps);
                        checkTimers();
                        return new StepResult(ps, ps, null, exitActions, null, entryActions, null, null);
                    }
                }
            }
        }
        return null;
    }

    public List<TransitionPath> getAvailableTransitions() {
        if (currentVertices.isEmpty()) {
            return Collections.emptyList();
        }
        List<TransitionPath> paths = new ArrayList<>();
        
        for (IVertex v : currentVertices) {
            collectPaths(v, paths);
            
            IElement container = v.getContainer();
            while (container != null && !(container instanceof IStateMachine)) {
                if (container instanceof IState) {
                    collectPaths((IVertex) container, paths);
                }
                container = container.getContainer();
            }
        }
        return paths;
    }

    private void collectPaths(IVertex source, List<TransitionPath> paths) {
        for (ITransition t : source.getOutgoings()) {
            expandTransition(t, new ArrayList<>(Collections.singletonList(t)), paths);
        }
    }

    private void expandTransition(ITransition current, List<ITransition> currentPath, List<TransitionPath> paths) {
        IVertex target = current.getTarget();
        if (target instanceof IPseudostate) {
            IPseudostate ps = (IPseudostate) target;
            if (ps.isJunctionPseudostate() || ps.isChoicePseudostate()) {
                ITransition[] outgoings = ps.getOutgoings();
                if (outgoings == null || outgoings.length == 0) {
                    // Dead end junction, treat as final target
                    paths.add(new TransitionPath(currentPath));
                } else {
                    for (ITransition out : outgoings) {
                        if (currentPath.contains(out)) continue; // Prevent loops
                        List<ITransition> nextPath = new ArrayList<>(currentPath);
                        nextPath.add(out);
                        expandTransition(out, nextPath, paths);
                    }
                }
                return;
            }
        }
        paths.add(new TransitionPath(currentPath));
    }

    public StepResult step(TransitionPath path, String note) {
        timerManager.cancel();
        if (path == null || path.transitions.isEmpty()) return null;

        IVertex source = path.getSource();
        IVertex target = path.getTarget();

        // 1. Find LCA (Least Common Ancestor)
        IElement lca = findLCA(path);
        
        // Identify states being exited (from source up to, but not including, LCA)
        List<IElement> exitedStates = new ArrayList<>();
        
        // 1. Collect primary exit path (Source -> LCA)
        IElement currentElement = source;
        while (currentElement != null && !currentElement.equals(lca)) {
            if (!exitedStates.contains(currentElement)) {
                exitedStates.add(currentElement);
            }
            currentElement = currentElement.getContainer();
        }
        
        // 2. Collect all active descendants of any exited state (covers orthogonal regions)
        for (IVertex active : currentVertices) {
            boolean isExiting = false;
            for (IElement exited : exitedStates) {
                if (active.equals(exited) || isDescendant(active, exited)) {
                    isExiting = true;
                    break;
                }
            }
            
            if (isExiting) {
                IElement v = active;
                while (v != null && !v.equals(lca)) {
                    if (!exitedStates.contains(v)) {
                        exitedStates.add(v);
                    }
                    v = v.getContainer();
                }
            }
        }

        // Update History before exiting
        updateHistory(exitedStates);
        
        // 2. Collect Exit Actions
        List<String> exitActions = new ArrayList<>();

        // 2a. Handle implicit exits (descendants of exited states)
        List<IVertex> toRemove = new ArrayList<>();
        for (IVertex active : currentVertices) {
            IElement boundary = null;
            for (IElement exited : exitedStates) {
                if (active.equals(exited) || isDescendant(active, exited)) {
                    boundary = exited;
                    break; // Found the most specific exited ancestor (or self)
                }
            }
            if (boundary != null) {
                collectExitActions(active, boundary, exitActions);
                toRemove.add(active);
            }
        }
        currentVertices.removeAll(toRemove);

        // 2b. Collect Exit Actions for the main path (Source -> LCA)
        collectExitActions(source, lca, exitActions);

        // 3. Transition Action
        List<String> transitionActions = new ArrayList<>();
        for (ITransition t : path.transitions) {
            String action = t.getAction();
            if (action != null && !action.isEmpty()) {
                transitionActions.add(action);
            }
        }

        // 4. Collect Entry Actions (LCA -> Target)
        List<String> entryActions = new ArrayList<>();
        collectEntryActions(target, lca, entryActions);

        // 5. Drill down if target is composite (Initial -> ...)
        List<IVertex> nextVertices = drillDown(target, entryActions);

        // 5b. If we entered an Orthogonal State partially, enter other regions
        completeOrthogonalRegions(nextVertices, lca, entryActions);

        // 6. Do Activity
        String doActivity = null;
        // For simplicity, just take the first one's Do activity if available, or join them?
        // Let's just record the primary target's Do if it's a state.
        if (target instanceof IState) {
            doActivity = ((IState) target).getDoActivity();
        }

        // Update state
        previousVertices.clear();
        for (IElement e : exitedStates) {
            if (e instanceof IVertex) {
                previousVertices.add((IVertex) e);
            }
        }
        for (IVertex removed : toRemove) {
            if (!previousVertices.contains(removed)) {
                previousVertices.add(removed);
            }
        }
        lastTransition = path.getLast(); // Highlight the last transition in the chain? Or the first? Usually the last one entering target.
        currentVertices.addAll(nextVertices);

        checkTimers();
        // For StepResult, we return the primary target of the transition, though multiple states might be active.
        return new StepResult(source, target, path, exitActions, transitionActions, entryActions, doActivity, note);
    }

    public List<IVertex> getCurrentVertices() {
        return currentVertices;
    }

    public List<IVertex> getPreviousVertices() {
        return previousVertices;
    }

    public ITransition getLastTransition() {
        return lastTransition;
    }

    private void checkTimers() {
        List<TransitionPath> paths = getAvailableTransitions();
        long minDelay = -1;
        List<TransitionPath> minPaths = new ArrayList<>();

        for (TransitionPath path : paths) {
            String event = path.getRoot().getEvent();
            long delay = TimerManager.parseDuration(event);
            if (delay >= 0) {
                if (minPaths.isEmpty() || delay < minDelay) {
                    minDelay = delay;
                    minPaths.clear();
                    minPaths.add(path);
                } else if (delay == minDelay) {
                    minPaths.add(path);
                }
            }
        }

        if (!minPaths.isEmpty()) {
            final List<TransitionPath> pathsToFire = new ArrayList<>(minPaths);
            boolean isFast = timerManager.isFastMode();
            timerManager.schedule(minDelay, () -> {
                if (pathsToFire.size() == 1) {
                    String note = isFast ? "(Fast)" : null;
                    StepResult res = step(pathsToFire.get(0), note);
                    if (stepListener != null) {
                        stepListener.accept(res);
                    }
                } else {
                    if (timerListener != null) {
                        timerListener.onTimerFired(pathsToFire);
                    }
                }
            });
        }
    }

    private void completeOrthogonalRegions(List<IVertex> nextVertices, IElement lca, List<String> entryActions) {
        // Collect all states being entered (ancestors of nextVertices up to lca)
        Set<IState> enteredStates = new LinkedHashSet<>();
        for (IVertex v : nextVertices) {
            List<IElement> ancestors = getAncestors(v);
            for (IElement a : ancestors) {
                if (a.equals(lca)) break;
                if (a instanceof IState) {
                    enteredStates.add((IState) a);
                }
            }
        }

        // Check each entered state: if it is orthogonal, ensure all its regions are entered
        // We iterate a copy to allow modification of nextVertices/enteredStates if needed (though we append to nextVertices)
        // A simple approach: Iterate enteredStates. If orthogonal, check children.
        List<IState> statesToCheck = new ArrayList<>(enteredStates);
        for (IState state : statesToCheck) {
            if (isOrthogonalState(state)) {
                IVertex[] subvertexes = state.getSubvertexes();
                for (IVertex sub : subvertexes) {
                    if (sub instanceof IState) { // Region
                        IState region = (IState) sub;
                        if (!enteredStates.contains(region)) {
                            // This region is not entered explicitly. Enter it implicitly.
                            String entry = region.getEntry();
                            if (entry != null && !entry.isEmpty()) {
                                entryActions.add(entry);
                            }
                            nextVertices.addAll(drillDown(region, entryActions));
                        }
                    }
                }
            }
        }
    }

    // --- Helper Methods for Composite State ---

    private IElement findLCA(TransitionPath path) {
        if (path == null || path.transitions.isEmpty()) return null;

        // Start with ancestors of the first vertex
        List<IElement> commonAncestors = new ArrayList<>(getAncestors(path.getSource()));

        // Intersect with ancestors of all other vertices involved in the path
        for (ITransition t : path.transitions) {
            commonAncestors.retainAll(getAncestors(t.getSource()));
            commonAncestors.retainAll(getAncestors(t.getTarget()));
        }

        return commonAncestors.isEmpty() ? null : commonAncestors.get(0);
    }

    private List<IElement> getAncestors(IVertex v) {
        List<IElement> ancestors = new ArrayList<>();
        IElement current = v.getContainer();
        while (current != null) {
            ancestors.add(current);
            if (current instanceof IStateMachine) break;
            current = current.getContainer();
        }
        return ancestors;
    }

    private void collectExitActions(IVertex current, IElement lca, List<String> actions) {
        IVertex v = current;
        while (v != null && v != lca) {
            if (v instanceof IState) {
                String exit = ((IState) v).getExit();
                if (exit != null && !exit.isEmpty()) {
                    actions.add(exit);
                }
            }
            IElement container = v.getContainer();
            v = (container instanceof IVertex) ? (IVertex) container : null;
        }
    }

    private void collectEntryActions(IVertex target, IElement lca, List<String> actions) {
        List<String> temp = new ArrayList<>();
        IVertex v = target;
        while (v != null && v != lca) {
            if (v instanceof IState) {
                String entry = ((IState) v).getEntry();
                if (entry != null && !entry.isEmpty()) {
                    temp.add(entry);
                }
            }
            IElement container = v.getContainer();
            v = (container instanceof IVertex) ? (IVertex) container : null;
        }
        Collections.reverse(temp);
        actions.addAll(temp);
    }

    private void updateHistory(List<IElement> exitedStates) {
        for (IElement exited : exitedStates) {
            if (exited instanceof IState) {
                IState state = (IState) exited;
                // Do not record history for an orthogonal state itself.
                // Its history is implicitly the history of its regions.
                if (isOrthogonalState(state)) {
                    continue;
                }
                // Find the direct child of 'state' that is currently active (or is an ancestor of an active state)
                for (IVertex active : currentVertices) {
                    IVertex child = getDirectChild(state, active);
                    if (child != null) {
                        historyMap.put(state, child);
                        break; // For a non-orthogonal state, there's only one active child path.
                    }
                }
            }
        }
    }

    private IVertex getDirectChild(IState parent, IVertex descendant) {
        IElement current = descendant;
        while (current != null) {
            IElement container = current.getContainer();
            if (container != null && container.equals(parent)) {
                return (current instanceof IVertex) ? (IVertex) current : null;
            }
            current = container;
        }
        return null;
    }

    private List<IVertex> drillDown(IVertex current, List<String> entryActions) {
        // Handle History Pseudostates
        if (current instanceof IPseudostate) {
            IPseudostate ps = (IPseudostate) current;
            if (ps.isShallowHistoryPseudostate() || ps.isDeepHistoryPseudostate()) {
                IElement container = ps.getContainer();
                if (container instanceof IState) {
                    IState parent = (IState) container;

                    // Special handling for history transition into an orthogonal state
                    if (isOrthogonalState(parent)) {
                        List<IVertex> restoredVertices = new ArrayList<>();
                        IVertex[] subvertexes = parent.getSubvertexes();
                        if (subvertexes != null) {
                            for (IVertex sub : subvertexes) {
                                if (sub instanceof IState) { // It's a region
                                    IState region = (IState) sub;
                                    String entry = region.getEntry();
                                    if (entry != null && !entry.isEmpty()) {
                                        entryActions.add(entry);
                                    }
                                    if (ps.isDeepHistoryPseudostate()) {
                                        restoredVertices.addAll(restoreDeepHistory(region, entryActions));
                                    } else { // Shallow History
                                        IVertex history = historyMap.get(region);
                                        if (history != null) {
                                            restoredVertices.addAll(drillDown(history, entryActions));
                                        } else {
                                            restoredVertices.addAll(drillDown(region, entryActions));
                                        }
                                    }
                                }
                            }
                        }
                        if (!restoredVertices.isEmpty()) {
                            return restoredVertices;
                        }
                        // Fallback if no regions were found, enter the state normally.
                        return drillDown(parent, entryActions);
                    } else {
                        // Original logic for non-orthogonal states
                        IVertex history = historyMap.get(parent);
                        if (history != null) {
                            // Restore history
                            if (history instanceof IState) {
                                String entry = ((IState) history).getEntry();
                                if (entry != null && !entry.isEmpty()) {
                                    entryActions.add(entry);
                                }
                            }
                            if (ps.isDeepHistoryPseudostate()) {
                                return restoreDeepHistory(history, entryActions);
                            } else {
                                return drillDown(history, entryActions);
                            }
                        } else {
                            // No history: Follow default transition (outgoing from History Pseudostate)
                            ITransition[] outgoings = ps.getOutgoings();
                            if (outgoings != null && outgoings.length > 0) {
                                ITransition t = outgoings[0];
                                String action = t.getAction();
                                if (action != null && !action.isEmpty()) {
                                    entryActions.add(action);
                                }
                                IVertex next = t.getTarget();
                                if (next instanceof IState) {
                                    String entry = ((IState) next).getEntry();
                                    if (entry != null && !entry.isEmpty()) {
                                        entryActions.add(entry);
                                    }
                                }
                                return drillDown(next, entryActions);
                            } else {
                                // Fallback: If no history and no default transition, behave as if entering the composite state normally.
                                // UML Spec: Target is semantically equivalent to a Transition to the composite State itself.
                                return drillDown((IVertex) container, entryActions);
                            }
                        }
                    }
                }
            }
        }

        List<IVertex> results = new ArrayList<>();
        if (!(current instanceof IState)) {
            results.add(current);
            return results;
        }

        IState state = (IState) current;
        IVertex[] sv = state.getSubvertexes();
        if (sv == null || sv.length == 0) {
            results.add(current);
            return results;
        }
        
        List<IVertex> subvertexes = Arrays.asList(sv);

        if (isOrthogonalState(state)) {
            // If no Initial Pseudostate found, assume Orthogonal State (Parallel)
            // Treat sub-states as Regions and drill down into them.
            boolean foundRegion = false;
            for (IVertex v : subvertexes) {
                if (v instanceof IState) {
                    foundRegion = true;
                    // Note: Regions (IState) usually don't have entry actions, but if they do, we collect them.
                    String entry = ((IState) v).getEntry();
                    if (entry != null && !entry.isEmpty()) {
                        entryActions.add(entry);
                    }
                    results.addAll(drillDown(v, entryActions));
                }
            }
            if (!foundRegion) {
                results.add(current);
            }
        } else {
            // Standard Composite State with Initial Pseudostate
            for (IVertex v : subvertexes) {
                if (v instanceof IPseudostate && ((IPseudostate) v).isInitialPseudostate()) {
                    ITransition[] outgoings = v.getOutgoings();
                    if (outgoings != null && outgoings.length > 0) {
                        ITransition t = outgoings[0];
                        String action = t.getAction();
                        if (action != null && !action.isEmpty()) {
                            entryActions.add(action); // Add transition action to entry sequence
                        }
                        // Recursively drill down
                        IVertex next = t.getTarget();
                        
                        if (next instanceof IState) {
                            String entry = ((IState) next).getEntry();
                            if (entry != null && !entry.isEmpty()) {
                                entryActions.add(entry);
                            }
                        }
                        results.addAll(drillDown(next, entryActions));
                    }
                }
            }
        }
        
        return results;
    }

    private boolean isOrthogonalState(IState state) {
        IVertex[] subvertexes = state.getSubvertexes();
        if (subvertexes == null) return false;
        
        for (IVertex v : subvertexes) {
            if (v instanceof IPseudostate && ((IPseudostate) v).isInitialPseudostate()) {
                return false; // Has Initial -> Not Orthogonal (Simplified logic)
            }
        }
        // If it has subvertexes but no Initial, assume Orthogonal if it has Region-like states
        return subvertexes.length > 0;
    }

    private List<IVertex> restoreDeepHistory(IVertex current, List<String> entryActions) {
        List<IVertex> results = new ArrayList<>();
        
        if (!(current instanceof IState)) {
            results.add(current);
            return results;
        }
        
        IState state = (IState) current;
        // Check if we have history for this state
        IVertex historyChild = historyMap.get(state);
        
        if (historyChild != null) {
            // We have history, follow it
            if (historyChild instanceof IState) {
                String entry = ((IState) historyChild).getEntry();
                if (entry != null && !entry.isEmpty()) {
                    entryActions.add(entry);
                }
                // Recursively restore
                return restoreDeepHistory(historyChild, entryActions);
            } else {
                results.add(historyChild);
                return results;
            }
        } else {
            // No history for this level, revert to normal drillDown (Initial)
            return drillDown(current, entryActions);
        }
    }

    private boolean isDescendant(IVertex v, IElement ancestor) {
        IElement current = v.getContainer();
        while (current != null) {
            if (current.equals(ancestor)) {
                return true;
            }
            current = current.getContainer();
        }
        return false;
    }

    // --- Debug Utilities ---

    public String getDebugInfo(IStateMachineDiagram diagram) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Simulation Engine Debug Info ===\n");
        
        // 1. Current Status
        sb.append("\n[Current Vertices]\n");
        for (IVertex v : currentVertices) {
            sb.append(" - ").append(v.getName()).append(" (").append(v.getClass().getSimpleName()).append(")\n");
        }
        
        // 2. History Map
        sb.append("\n[History Map]\n");
        if (historyMap.isEmpty()) {
            sb.append(" (Empty)\n");
        } else {
            for (Map.Entry<IState, IVertex> entry : historyMap.entrySet()) {
                sb.append(" - State: ").append(entry.getKey().getName())
                  .append(" -> Last Active: ").append(entry.getValue().getName()).append("\n");
            }
        }

        // 3. Model Structure
        sb.append("\n[Model Structure]\n");
        if (diagram != null && diagram.getStateMachine() != null) {
            dumpStateMachine(sb, diagram.getStateMachine(), "");
        } else {
            sb.append(" (No Diagram/StateMachine loaded)\n");
        }
        
        return sb.toString();
    }

    private void dumpStateMachine(StringBuilder sb, IStateMachine sm, String indent) {
        for (IVertex v : sm.getVertexes()) {
            dumpVertex(sb, v, indent);
        }
    }

    private void dumpVertex(StringBuilder sb, IVertex v, String indent) {
        sb.append(indent).append("- ").append(v.getName()).append(" (").append(v.getClass().getSimpleName()).append(")\n");
        
        // Outgoings
        for (ITransition t : v.getOutgoings()) {
            sb.append(indent).append("  -> ").append(t.getTarget().getName()).append(" : ").append(t.getName()).append("\n");
        }

        // Children (if Composite)
        if (v instanceof IState) {
            IVertex[] children = ((IState) v).getSubvertexes();
            if (children != null && children.length > 0) {
                for (IVertex child : children) {
                    dumpVertex(sb, child, indent + "    ");
                }
            }
        }
    }
}