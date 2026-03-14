package snytng.astah.plugin.stm.model;

import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IFinalState;
import com.change_vision.jude.api.inf.model.INamedElement;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.function.Consumer;

public class SimulationEngine {

    private List<IVertex> currentVertices = new ArrayList<>();
    private List<IVertex> previousVertices = new ArrayList<>();
    private Map<IState, IVertex> historyMap = new HashMap<>();
    private Map<IVertex, Long> entryTimeMap = new HashMap<>();
    private ITransition lastTransition;
    private List<SimulationSnapshot> historySnapshots = new ArrayList<>();
    private int currentSnapshotIndex = -1;
    
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
        checkTimers();
    }

    public boolean isFastMode() {
        return timerManager.isFastMode();
    }

    public static class SimulationSnapshot {
        public final List<IVertex> currentVertices;
        public final List<IVertex> previousVertices;
        public final Map<IState, IVertex> historyMap;
        public final Map<IVertex, Long> entryTimeMap;
        public final ITransition lastTransition;

        public SimulationSnapshot(SimulationEngine engine) {
            this.currentVertices = new ArrayList<>(engine.currentVertices);
            this.previousVertices = new ArrayList<>(engine.previousVertices);
            this.historyMap = new HashMap<>(engine.historyMap);
            this.entryTimeMap = new HashMap<>(engine.entryTimeMap);
            this.lastTransition = engine.lastTransition;
        }

        public void restore(SimulationEngine engine) {
            engine.currentVertices.clear();
            engine.currentVertices.addAll(this.currentVertices);
            engine.previousVertices.clear();
            engine.previousVertices.addAll(this.previousVertices);
            engine.historyMap.clear();
            engine.historyMap.putAll(this.historyMap);
            engine.entryTimeMap.clear();
            engine.entryTimeMap.putAll(this.entryTimeMap);
            engine.lastTransition = this.lastTransition;
        }
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
        public final List<TransitionPath> executedPaths;

        public StepResult(IVertex source, IVertex target, TransitionPath path,
                          List<String> exitActions, List<String> transitionActions, List<String> entryActions, String doActivity, String note, List<TransitionPath> executedPaths) {
            this.source = source;
            this.target = target;
            this.path = path;
            this.exitActions = exitActions;
            this.transitionActions = transitionActions;
            this.entryActions = entryActions;
            this.doActivity = doActivity;
            this.note = note;
            this.executedPaths = executedPaths;
        }
    }

    public StepResult start(IStateMachineDiagram diagram) {
        timerManager.cancel();
        currentVertices.clear();
        previousVertices.clear();
        historyMap.clear();
        entryTimeMap.clear();
        lastTransition = null;
        historySnapshots.clear();
        currentSnapshotIndex = -1;

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
                        
                        currentVertices.addAll(drillDown(target, entryActions, new HashSet<>()));
                        
                        // Update history for highlighting (Initial state as previous)
                        previousVertices.add(ps);
                        lastTransition = t;

                        TransitionPath path = new TransitionPath(Collections.singletonList(t));
                        
                        checkTimers();
                        saveSnapshot();
                        return new StepResult(ps, target, path, exitActions, transitionActions, entryActions, null, null, Collections.singletonList(path));
                    } else {
                        currentVertices.add(ps);
                        checkTimers();
                        saveSnapshot();
                        return new StepResult(ps, ps, null, exitActions, null, entryActions, null, null, null);
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
        Set<IVertex> processedVertices = new HashSet<>();
        
        for (IVertex v : currentVertices) {
            IVertex current = v;
            while (current != null) {
                if (!processedVertices.contains(current)) {
                    collectPaths(current, paths);
                    processedVertices.add(current);
                }
                
                IElement container = current.getContainer();
                if (container instanceof IStateMachine) break;
                current = (container instanceof IVertex) ? (IVertex) container : null;
            }
        }
        return paths;
    }

    private void collectPaths(IVertex source, List<TransitionPath> paths) {
        ITransition[] outgoings = source.getOutgoings();
        if (outgoings != null) {
            for (ITransition t : outgoings) {
                expandTransition(t, new ArrayList<>(Collections.singletonList(t)), paths);
            }
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

    public StepResult step(List<TransitionPath> paths, String note) {
        timerManager.cancel();
        if (paths == null || paths.isEmpty()) return null;

        // For logging, we'll use the first path as representative.
        TransitionPath representativePath = paths.get(0);
        IVertex source = representativePath.getSource();
        IVertex target = representativePath.getTarget();

        // --- Run-to-Completion Step ---

        // 1. Collect all unique states to be exited from all paths
        Set<IElement> exitedStates = new LinkedHashSet<>();
        for (TransitionPath path : paths) {
            IElement lca = findLCA(path);
            IElement currentElement = path.getSource();
            while (currentElement != null && !currentElement.equals(lca)) {
                exitedStates.add(currentElement);
                currentElement = currentElement.getContainer();
            }
        }

        // 2. Collect all active vertices that are descendants of the exited states
        Set<IVertex> verticesToExit = new LinkedHashSet<>();
        for (IVertex activeVertex : currentVertices) {
            for (IElement exitedState : exitedStates) {
                if (activeVertex.equals(exitedState) || isDescendant(activeVertex, exitedState)) {
                    verticesToExit.add(activeVertex);
                    break;
                }
            }
        }

        // 3. Perform exit procedures
        List<String> allExitActions = new ArrayList<>();
        for (IVertex vertexToExit : verticesToExit) {
            // Find the LCA for the context of this specific exit.
            // This is complex. A simplification: assume exit up to the highest-level exited state's container.
            IElement exitBoundary = findExitBoundary(vertexToExit, exitedStates);
            collectExitActions(vertexToExit, exitBoundary, allExitActions);
        }

        // Update history and clear entry times for all exited vertices
        updateHistory(new ArrayList<>(exitedStates));
        verticesToExit.forEach(entryTimeMap::remove);
        
        // 4. Collect all transition actions
        List<String> allTransitionActions = new ArrayList<>();
        paths.forEach(p -> p.transitions.forEach(t -> {
            String action = t.getAction();
            if (action != null && !action.isEmpty()) {
                allTransitionActions.add(action);
            }
        }));

        // 5. Collect all entry actions and determine next active states
        List<String> allEntryActions = new ArrayList<>();
        List<IVertex> nextVertices = new ArrayList<>();
        for (TransitionPath path : paths) {
            IElement lca = findLCA(path);
            collectEntryActions(path.getTarget(), lca, allEntryActions);
            nextVertices.addAll(drillDown(path.getTarget(), allEntryActions, new HashSet<>()));
        }

        // Remove duplicates from next vertices
        nextVertices = nextVertices.stream().distinct().collect(Collectors.toList());

        // Update simulation state
        previousVertices.clear();
        previousVertices.addAll(verticesToExit);
        currentVertices.removeAll(verticesToExit);
        currentVertices.addAll(nextVertices);
        lastTransition = representativePath.getLast();

        checkTimers();

        validateCurrentStates();

        saveSnapshot();

        // Return a representative result for logging
        return new StepResult(source, target, representativePath, allExitActions, allTransitionActions, allEntryActions, null, note, paths);
    }

    // Wrapper for single path transition for backward compatibility (e.g., with timers)
    public StepResult step(TransitionPath path, String note) {
        return step(Collections.singletonList(path), note);
    }

    private IElement findExitBoundary(IVertex vertexToExit, Set<IElement> exitedStates) {
        IElement container = vertexToExit.getContainer();
        while(container != null) {
            if(exitedStates.contains(container)){
                container = container.getContainer();
            } else {
                return container;
            }
        }
        return null;
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

    private void saveSnapshot() {
        // If we are not at the end of the history, truncate the future
        if (currentSnapshotIndex < historySnapshots.size() - 1) {
            historySnapshots.subList(currentSnapshotIndex + 1, historySnapshots.size()).clear();
        }
        historySnapshots.add(new SimulationSnapshot(this));
        currentSnapshotIndex = historySnapshots.size() - 1;
    }

    public boolean canStepBack() {
        return currentSnapshotIndex > 0;
    }

    public boolean canStepForward() {
        return currentSnapshotIndex < historySnapshots.size() - 1;
    }

    public void stepBack() {
        if (canStepBack()) {
            restoreSnapshot(currentSnapshotIndex - 1);
        }
    }

    public void stepForward() {
        if (canStepForward()) {
            restoreSnapshot(currentSnapshotIndex + 1);
        }
    }

    public void goToStart() {
        if (!historySnapshots.isEmpty()) {
            restoreSnapshot(0);
        }
    }

    public void goToEnd() {
        if (!historySnapshots.isEmpty()) {
            restoreSnapshot(historySnapshots.size() - 1);
        }
    }

    private void restoreSnapshot(int index) {
        if (index >= 0 && index < historySnapshots.size()) {
            timerManager.cancel();
            currentSnapshotIndex = index;
            historySnapshots.get(index).restore(this);
            checkTimers();
        }
    }

    public int getCurrentSnapshotIndex() {
        return currentSnapshotIndex;
    }

    public int getHistorySize() {
        return historySnapshots.size();
    }

    /**
     * Validates the consistency of the current active states.
     * This method is intended to be called after each simulation step to ensure the engine is in a legal state.
     *
     * @throws IllegalSimulationStateException if an inconsistency is found, such as multiple states in a non-parallel region.
     */
    private void validateCurrentStates() throws IllegalSimulationStateException {
        // 1. Check for lost states (unless a final state is active, which is a valid termination)
        if (currentVertices.isEmpty()) {
            throw new IllegalSimulationStateException("Illegal State: Current active state is lost.");
        }
        if (currentVertices.stream().anyMatch(v -> v instanceof IFinalState)) {
            // If a final state is reached, the simulation has validly terminated for that path.
            return;
        }

        // 2. Group active states by their container to check for illegal parallel states
        Map<IElement, List<IVertex>> statesByContainer = new HashMap<>();
        for (IVertex v : currentVertices) {
            statesByContainer.computeIfAbsent(v.getContainer(), k -> new ArrayList<>()).add(v);
        }

        // 3. Check each container for illegal multiple active states
        for (Map.Entry<IElement, List<IVertex>> entry : statesByContainer.entrySet()) {
            IElement container = entry.getKey();
            List<IVertex> verticesInContainer = entry.getValue();

            if (verticesInContainer.size() > 1) {
                // Multiple states in the same container are only allowed if the container is a parallel (orthogonal) state.
                boolean isContainerParallel = (container instanceof IState) && isOrthogonalState((IState) container);
                if (!isContainerParallel) {
                    String conflictingStates = verticesInContainer.stream()
                            .map(v -> v.getName() == null || v.getName().isEmpty() ? "[unnamed]" : v.getName())
                            .collect(Collectors.joining(", "));
                    String containerName = "Unknown";
                    if (container instanceof INamedElement) {
                        INamedElement namedContainer = (INamedElement) container;
                        containerName = namedContainer.getName() == null || namedContainer.getName().isEmpty() ? "[unnamed]" : namedContainer.getName();
                    } else if (container == null) {
                        containerName = "[Top Level]";
                    }

                    throw new IllegalSimulationStateException(
                        "Illegal State: Multiple active states (" + conflictingStates + ") found in a non-parallel region '" + containerName + "'.");
                }
            }
        }
    }

    private void checkTimers() {
        List<TransitionPath> paths = getAvailableTransitions();
        long minDelay = -1;
        List<TransitionPath> minPaths = new ArrayList<>();

        for (TransitionPath path : paths) {
            String event = path.getRoot().getEvent();
            long definedDelay = TimerManager.parseDuration(event);
            if (definedDelay >= 0) {
                long remainingDelay = definedDelay;
                Long entryTime = entryTimeMap.get(path.getSource());
                if (entryTime != null && !isFastMode()) {
                    remainingDelay = definedDelay - (System.currentTimeMillis() - entryTime);
                }

                if (minPaths.isEmpty() || remainingDelay < minDelay) {
                    minDelay = remainingDelay;
                    minPaths.clear();
                    minPaths.add(path);
                } else if (remainingDelay == minDelay) {
                    minPaths.add(path);
                }
            }
        }

        if (!minPaths.isEmpty()) {
            final List<TransitionPath> pathsToFire = new ArrayList<>(minPaths);
            boolean isFast = timerManager.isFastMode();
            timerManager.schedule(Math.max(0, minDelay), () -> {
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
                            nextVertices.addAll(drillDown(region, entryActions, new HashSet<>()));
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

    private List<IVertex> drillDown(IVertex current, List<String> entryActions, Set<IVertex> visited) {
        if (visited.contains(current)) {
            return new ArrayList<>();
        }
        visited.add(current);

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
                                        restoredVertices.addAll(restoreDeepHistory(region, entryActions, visited));
                                    } else { // Shallow History
                                        IVertex history = historyMap.get(region);
                                        if (history != null) {
                                            restoredVertices.addAll(drillDown(history, entryActions, visited));
                                        } else {
                                            restoredVertices.addAll(drillDown(region, entryActions, visited));
                                        }
                                    }
                                }
                            }
                        }
                        if (!restoredVertices.isEmpty()) {
                            return restoredVertices;
                        }
                        // Fallback if no regions were found, enter the state normally.
                        return drillDown(parent, entryActions, visited);
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
                                return restoreDeepHistory(history, entryActions, visited);
                            } else {
                                return drillDown(history, entryActions, visited);
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
                                return drillDown(next, entryActions, visited);
                            } else {
                                // Fallback: If no history and no default transition, behave as if entering the composite state normally.
                                // UML Spec: Target is semantically equivalent to a Transition to the composite State itself.
                                return drillDown((IVertex) container, entryActions, visited);
                            }
                        }
                    }
                }
            }
        }

        List<IVertex> results = new ArrayList<>();
        if (!(current instanceof IState)) {
            results.add(current);
            entryTimeMap.put(current, System.currentTimeMillis());
            return results;
        }

        IState state = (IState) current;
        IVertex[] sv = state.getSubvertexes();
        if (sv == null || sv.length == 0) {
            results.add(current);
            entryTimeMap.put(current, System.currentTimeMillis());
            return results;
        }
        
        List<IVertex> subvertexes = Arrays.asList(sv);

        if (isOrthogonalState(state)) {
            // If no Initial Pseudostate found, assume Orthogonal State (Parallel)
            // Treat sub-states as Regions and drill down into them.
            boolean foundRegion = false;
            
            Set<IState> regions = new LinkedHashSet<>();
            
            // 1. Get real regions
            try {
                for (IState r : state.getStateRegions()) {
                    regions.add(r);
                }
            } catch (Exception e) {}
            
            // 2. Get subvertex states (for script models or API variations)
            for (IVertex v : subvertexes) {
                if (v instanceof IState) {
                    regions.add((IState) v);
                }
            }

            for (IState region : regions) {
                    if (region.equals(state)) continue; // Prevent self-recursion
                    foundRegion = true;
                    // Note: Regions (IState) usually don't have entry actions, but if they do, we collect them.
                    String entry = region.getEntry();
                    if (entry != null && !entry.isEmpty()) {
                        entryActions.add(entry);
                    }
                    results.addAll(drillDown(region, entryActions, visited));
            }
            if (!foundRegion) {
                results.add(current);
                entryTimeMap.put(current, System.currentTimeMillis());
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
                        results.addAll(drillDown(next, entryActions, visited));
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

    private List<IVertex> restoreDeepHistory(IVertex current, List<String> entryActions, Set<IVertex> visited) {
        if (visited.contains(current)) {
            return new ArrayList<>();
        }
        visited.add(current);
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
                return restoreDeepHistory(historyChild, entryActions, visited);
            } else {
                results.add(historyChild);
                entryTimeMap.put(historyChild, System.currentTimeMillis());
                return results;
            }
        } else {
            // No history for this level, revert to normal drillDown (Initial)
            return drillDown(current, entryActions, visited);
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
            dumpStateMachine(sb, diagram.getStateMachine(), "", new HashSet<>());
        } else {
            sb.append(" (No Diagram/StateMachine loaded)\n");
        }
        
        return sb.toString();
    }

    private void dumpStateMachine(StringBuilder sb, IStateMachine sm, String indent, Set<Object> visited) {
        if (visited.contains(sm)) {
            sb.append(indent).append("[Recursive StateMachine: ").append(sm.getName()).append("]\n");
            return;
        }
        visited.add(sm);

        for (IVertex v : sm.getVertexes()) {
            // Check if the vertex is a direct child of a StateMachine (top-level)
            // We check if container is NOT a Vertex (it should be the StateMachine)
            if (!(v.getContainer() instanceof IVertex)) {
                dumpVertex(sb, v, indent, visited, false);
            }
        }
    }

    private void dumpVertex(StringBuilder sb, IVertex v, String indent, Set<Object> visited, boolean isRegion) {
        if (visited.contains(v)) {
            return;
        }
        visited.add(v);

        sb.append(indent).append("- ").append(v.getName()).append(" (").append(v.getClass().getSimpleName()).append(")\n");
        
        // Outgoings
        for (ITransition t : v.getOutgoings()) {
            sb.append(indent).append("  -> ").append(t.getTarget().getName()).append(" : ").append(t.getName()).append("\n");
        }

        // Children (if Composite)
        if (v instanceof IState) {
            IState state = (IState) v;
            if (state.isSubmachineState()) {
                IStateMachine sub = state.getSubmachine();
                if (sub != null) {
                    sb.append(indent).append("    [Submachine: ").append(sub.getName()).append("]\n");
                    dumpStateMachine(sb, sub, indent + "    ", visited);
                }
            }

            // 1. Try to get regions (for Orthogonal State)
            try {
                for (IState region : state.getStateRegions()) {
                    dumpVertex(sb, region, indent + "    ", visited, true);
                }
            } catch (Exception e) {
                // Ignore if method not found or fails
            }

            // 2. Dump Subvertexes (Standard Composite State or contents of Region)
            IVertex[] children = state.getSubvertexes();
            if (children != null && children.length > 0) {
                for (IVertex child : children) {
                    // If current state is a Region, we assume all subvertexes are its children (skipping container check)
                    // If current state is NOT a Region, we enforce container check to avoid flattening
                    if (isRegion || isChildOf(child, state)) {
                        dumpVertex(sb, child, indent + "    ", visited, false);
                    }
                }
            }
        }
    }

    private boolean isChildOf(IElement child, IElement parent) {
        IElement container = child.getContainer();
        if (container == null) return false;
        if (container.equals(parent)) return true;
        
        String cId = container.getId();
        String pId = parent.getId();
        if (cId != null && pId != null && cId.equals(pId)) return true;
        
        return false;
    }
}