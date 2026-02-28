package snytng.astah.plugin.stm.model;

import com.change_vision.jude.api.inf.model.IPseudostate;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SimulationEngine {

    private IVertex currentVertex;

    public void start(IStateMachineDiagram diagram) {
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
        currentVertex = null;
    }

    public List<ITransition> getAvailableTransitions() {
        if (currentVertex == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(currentVertex.getOutgoings());
    }

    public void fire(ITransition transition) {
        if (transition != null) {
            currentVertex = transition.getTarget();
        }
    }

    public IVertex getCurrentVertex() {
        return currentVertex;
    }
}