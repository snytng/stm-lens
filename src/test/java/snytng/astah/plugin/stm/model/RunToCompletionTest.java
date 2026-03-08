package snytng.astah.plugin.stm.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.change_vision.jude.api.inf.model.IPseudostate;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RunToCompletionTest {

    @Mock IState orthogonalState;
    @Mock IState region1, region2;
    @Mock IState s1, s2, s3, s4;
    @Mock ITransition t1, t2;

    @Test
    void testStep_ExecutesMultipleTransitionsSimultaneously() {
        // Setup: Two orthogonal regions with active states S1 and S3
        // Transition T1: S1 -> S2
        // Transition T2: S3 -> S4
        
        // Mock structure
        when(t1.getSource()).thenReturn(s1);
        when(t1.getTarget()).thenReturn(s2);
        when(t1.getAction()).thenReturn("action1");
        
        when(t2.getSource()).thenReturn(s3);
        when(t2.getTarget()).thenReturn(s4);
        when(t2.getAction()).thenReturn("action2");

        // Mock container hierarchy (simplified for LCA finding)
        // orthogonalState -> region1 -> s1
        // orthogonalState -> region2 -> s3
        when(s1.getContainer()).thenReturn(region1);
        when(s2.getContainer()).thenReturn(region1);
        when(region1.getContainer()).thenReturn(orthogonalState);
        
        when(s3.getContainer()).thenReturn(region2);
        when(s4.getContainer()).thenReturn(region2);
        when(region2.getContainer()).thenReturn(orthogonalState);

        // Prepare paths
        SimulationEngine.TransitionPath path1 = new SimulationEngine.TransitionPath(Collections.singletonList(t1));
        SimulationEngine.TransitionPath path2 = new SimulationEngine.TransitionPath(Collections.singletonList(t2));
        List<SimulationEngine.TransitionPath> paths = Arrays.asList(path1, path2);

        // Execute
        SimulationEngine engine = new SimulationEngine();
        // Manually set current vertices to simulate being in S1 and S3
        // Note: Since currentVertices is private and has no setter, we rely on step() logic 
        // not checking currentVertices for validity of source, but it uses them for exit logic.
        // To properly test this without reflection, we might need to relax encapsulation or use a helper.
        // However, step() logic primarily uses the paths provided.
        // Let's see if we can verify the result contains expected actions and targets.
        
        SimulationEngine.StepResult result = engine.step(paths, "test-rtc");

        // Verify
        assertNotNull(result);
        
        // Check transition actions - both should be present
        assertTrue(result.transitionActions.contains("action1"));
        assertTrue(result.transitionActions.contains("action2"));
        
        // Check current vertices in engine (requires access or inference)
        List<IVertex> current = engine.getCurrentVertices();
        assertTrue(current.contains(s2));
        assertTrue(current.contains(s4));
        assertFalse(current.contains(s1));
        assertFalse(current.contains(s3));
    }
}
