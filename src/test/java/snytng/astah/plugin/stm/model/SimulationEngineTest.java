package snytng.astah.plugin.stm.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.change_vision.jude.api.inf.model.IPseudostate;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;

@ExtendWith(MockitoExtension.class)
public class SimulationEngineTest {

    @Mock
    IStateMachineDiagram diagram;
    @Mock
    IStateMachine stateMachine;
    @Mock
    IPseudostate initialPseudostate;
    @Mock
    IState stateA;
    @Mock
    ITransition transition;

    @Test
    public void testStart_FindsInitialStateAndTransitionsToNext() {
        // --- Setup (Given) ---
        // 1. Diagram has a StateMachine
        when(diagram.getStateMachine()).thenReturn(stateMachine);

        // 2. StateMachine has vertices (Initial and StateA)
        IVertex[] vertices = new IVertex[] { initialPseudostate, stateA };
        when(stateMachine.getVertexes()).thenReturn(vertices);

        // 3. InitialPseudostate setup
        when(initialPseudostate.isInitialPseudostate()).thenReturn(true);
        when(initialPseudostate.getOutgoings()).thenReturn(new ITransition[] { transition });

        // 4. Transition setup (Initial -> StateA)
        when(transition.getTarget()).thenReturn(stateA);
        
        // 5. StateA setup
        // (Optional) If StateA has entry actions, mock getEntry() here.
        // when(stateA.getEntry()).thenReturn("entry action");

        // --- Execute (When) ---
        SimulationEngine engine = new SimulationEngine();
        SimulationEngine.StepResult result = engine.start(diagram);

        // --- Verify (Then) ---
        assertNotNull(result, "Start result should not be null");
        assertEquals(initialPseudostate, result.source, "Source should be Initial");
        assertEquals(stateA, result.target, "Target should be StateA");
        
        assertEquals(1, engine.getCurrentVertices().size());
        assertEquals(stateA, engine.getCurrentVertices().get(0), "Current vertex should be StateA");
    }
}