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

public class SimulationEngine {

    private IVertex currentVertex;
    private IVertex previousVertex;
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

    public void start(IStateMachineDiagram diagram) {
        currentVertex = null;
        previousVertex = null;
        lastTransition = null;

        if (diagram == null) return;

        IStateMachine sm = diagram.getStateMachine();
        if (sm == null) return;

        for (IVertex vertex : sm.getVertexes()) {
            if (vertex instanceof IPseudostate) {
                IPseudostate ps = (IPseudostate) vertex;
                if (ps.isInitialPseudostate()) {
                    ITransition[] outgoings = ps.getOutgoings();
                    if (outgoings != null && outgoings.length > 0) {
                        currentVertex = outgoings[0].getTarget();
                    } else {
                        currentVertex = ps;
                    }
                    return;
                }
            }
        }
    }

    public List<ITransition> getAvailableTransitions() {
        if (currentVertex == null) {
            return Collections.emptyList();
        }
        List<ITransition> transitions = new ArrayList<>();
        transitions.addAll(Arrays.asList(currentVertex.getOutgoings()));

        IElement container = currentVertex.getContainer();
        while (container instanceof IState) {
            transitions.addAll(Arrays.asList(((IState) container).getOutgoings()));
            container = container.getContainer();
        }
        return transitions;
    }

    public StepResult step(ITransition transition) {
        if (transition == null) return null;

        IVertex source = currentVertex;
        IVertex target = transition.getTarget();

        // 1. Find LCA (Least Common Ancestor)
        IElement lca = findLCA(source, target);

        // 2. Collect Exit Actions (Source -> LCA)
        List<String> exitActions = new ArrayList<>();
        collectExitActions(source, lca, exitActions);

        // 3. Transition Action
        String transitionAction = transition.getAction();

        // 4. Collect Entry Actions (LCA -> Target)
        List<String> entryActions = new ArrayList<>();
        collectEntryActions(target, lca, entryActions);

        // 5. Drill down if target is composite (Initial -> ...)
        IVertex finalTarget = drillDown(target, entryActions);

        // 6. Do Activity
        String doActivity = null;
        if (finalTarget instanceof IState) {
            doActivity = ((IState) finalTarget).getDoActivity();
        }

        // Update state
        previousVertex = source;
        lastTransition = transition;
        currentVertex = finalTarget;

        return new StepResult(source, finalTarget, transition, exitActions, transitionAction, entryActions, doActivity);
    }

    public IVertex getCurrentVertex() {
        return currentVertex;
    }

    public IVertex getPreviousVertex() {
        return previousVertex;
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
            if (current instanceof IState) {
                current = ((IState) current).getContainer();
            } else if (current instanceof IStateMachine) {
                current = null; // Root reached
            } else {
                current = null;
            }
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

    private IVertex drillDown(IVertex current, List<String> entryActions) {
        if (!(current instanceof IState)) return current;

        IState state = (IState) current;
        IVertex[] subvertexes = state.getSubvertexes();
        if (subvertexes == null) return current;

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
                    // Note: Entry action of 'next' will be collected in the next recursive call if we structured it differently,
                    // but here we need to manually add it or rely on a unified structure.
                    // Let's manually add the entry of the next state here before recursing.
                    if (next instanceof IState) {
                        String entry = ((IState) next).getEntry();
                        if (entry != null && !entry.isEmpty()) {
                            entryActions.add(entry);
                        }
                    }
                    return drillDown(next, entryActions);
                }
            }
        }
        return current;
    }
}