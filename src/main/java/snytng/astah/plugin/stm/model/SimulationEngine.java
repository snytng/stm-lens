package snytng.astah.plugin.stm.model;

import com.change_vision.jude.api.inf.model.IPseudostate;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;
import java.util.Arrays;
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
        public final String exitAction;
        public final String transitionAction;
        public final String entryAction;
        public final String doActivity;

        public StepResult(IVertex source, IVertex target, ITransition transition,
                          String exitAction, String transitionAction, String entryAction, String doActivity) {
            this.source = source;
            this.target = target;
            this.transition = transition;
            this.exitAction = exitAction;
            this.transitionAction = transitionAction;
            this.entryAction = entryAction;
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
        return Arrays.asList(currentVertex.getOutgoings());
    }

    public StepResult step(ITransition transition) {
        if (transition == null) return null;

        IVertex source = currentVertex;
        IVertex target = transition.getTarget();

        // Collect actions
        String exitAction = null;
        if (source instanceof IState) {
            exitAction = ((IState) source).getExit();
        }

        String transitionAction = transition.getAction();

        String entryAction = null;
        String doActivity = null;
        if (target instanceof IState) {
            entryAction = ((IState) target).getEntry();
            doActivity = ((IState) target).getDoActivity();
        }

        // Update state
        previousVertex = source;
        lastTransition = transition;
        currentVertex = target;

        return new StepResult(source, target, transition, exitAction, transitionAction, entryAction, doActivity);
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
}