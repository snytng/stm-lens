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

public class SimulationEngine {

    private List<IVertex> currentVertices = new ArrayList<>();
    private List<IVertex> previousVertices = new ArrayList<>();
    private Map<IState, IVertex> historyMap = new HashMap<>();
    private ITransition lastTransition;

    public static class StepResult {
        public final IVertex source;
        public final IVertex target;
        public final ITransition transition;
        public final List<String> exitActions;
        public final String transitionAction;
        public final List<String> entryActions;
        public final String doActivity;

        public StepResult(IVertex source, IVertex target, ITransition transition,
                          List<String> exitActions, String transitionAction, List<String> entryActions, String doActivity) {
            this.source = source;
            this.target = target;
            this.transition = transition;
            this.exitActions = exitActions;
            this.transitionAction = transitionAction;
            this.entryActions = entryActions;
            this.doActivity = doActivity;
        }
    }

    public StepResult start(IStateMachineDiagram diagram) {
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
                        
                        currentVertices.addAll(drillDown(target, entryActions));
                        
                        // Update history for highlighting (Initial state as previous)
                        previousVertices.add(ps);
                        lastTransition = t;
                        
                        return new StepResult(ps, target, t, exitActions, transitionAction, entryActions, null);
                    } else {
                        currentVertices.add(ps);
                        return new StepResult(ps, ps, null, exitActions, null, entryActions, null);
                    }
                }
            }
        }
        return null;
    }

    public List<ITransition> getAvailableTransitions() {
        if (currentVertices.isEmpty()) {
            return Collections.emptyList();
        }
        Set<ITransition> transitions = new LinkedHashSet<>();
        
        for (IVertex v : currentVertices) {
            transitions.addAll(Arrays.asList(v.getOutgoings()));
            IElement container = v.getContainer();
            while (container != null && !(container instanceof IStateMachine)) {
                if (container instanceof IState) {
                    transitions.addAll(Arrays.asList(((IState) container).getOutgoings()));
                }
                container = container.getContainer();
            }
        }
        return new ArrayList<>(transitions);
    }

    public StepResult step(ITransition transition) {
        if (transition == null) return null;

        IVertex source = transition.getSource();
        IVertex target = transition.getTarget();

        // 1. Find LCA (Least Common Ancestor)
        IElement lca = findLCA(source, target);
        
        // Identify states being exited (from source up to, but not including, LCA)
        // We must include active descendants of the source, as exiting a composite state exits all its substates.
        List<IElement> exitedStates = new ArrayList<>();
        
        for (IVertex active : currentVertices) {
            if (active.equals(source) || isDescendant(active, source)) {
                IElement current = active;
                while (current != null && !current.equals(lca)) {
                    if (!exitedStates.contains(current)) {
                        exitedStates.add(current);
                    }
                    current = current.getContainer();
                }
            }
        }

        // Ensure source path is included (fallback)
        IElement currentElement = source;
        while (currentElement != null && !currentElement.equals(lca)) {
            if (!exitedStates.contains(currentElement)) {
                exitedStates.add(currentElement);
            }
            currentElement = currentElement.getContainer();
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
        String transitionAction = transition.getAction();

        // 4. Collect Entry Actions (LCA -> Target)
        List<String> entryActions = new ArrayList<>();
        collectEntryActions(target, lca, entryActions);

        // 5. Drill down if target is composite (Initial -> ...)
        List<IVertex> nextVertices = drillDown(target, entryActions);

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
        lastTransition = transition;
        currentVertices.addAll(nextVertices);

        // For StepResult, we return the primary target of the transition, though multiple states might be active.
        return new StepResult(source, target, transition, exitActions, transitionAction, entryActions, doActivity);
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

    // --- Helper Methods for Composite State ---

    private IElement findLCA(IVertex source, IVertex target) {
        List<IElement> sourceAncestors = getAncestors(source);
        List<IElement> targetAncestors = getAncestors(target);

        for (IElement s : sourceAncestors) {
            if (targetAncestors.contains(s)) {
                return s;
            }
        }
        return null; // Should be StateMachine or null
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
                // Find the direct child of 'state' that is currently active (or is an ancestor of an active state)
                for (IVertex active : currentVertices) {
                    if (isDescendant(active, state)) {
                        // Find the direct child
                        IVertex child = getDirectChild(state, active);
                        if (child != null) {
                            historyMap.put(state, child);
                        }
                        break; // Assume one active child per region (simplified for now)
                    } else {
                        IElement container = active.getContainer();
                        if (container != null && container.equals(state)) {
                             historyMap.put(state, active);
                             break;
                        }
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

        boolean foundInitial = false;
        for (IVertex v : subvertexes) {
            if (v instanceof IPseudostate && ((IPseudostate) v).isInitialPseudostate()) {
                ITransition[] outgoings = v.getOutgoings();
                if (outgoings != null && outgoings.length > 0) {
                    foundInitial = true;
                    ITransition t = outgoings[0];
                    String action = t.getAction();
                    if (action != null && !action.isEmpty()) {
                        entryActions.add(action); // Add transition action to entry sequence
                    }
                    // Recursively drill down
                    IVertex next = t.getTarget();
                    // Note: Entry action of 'next' will be collected in the next recursive call if we structured it differently,
                    // but here we need to manually add it or rely on a unified structure.
                    // Let's manually add the entry of the next state here before recursing.
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
        
        if (!foundInitial) {
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
        }
        
        return results;
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
}