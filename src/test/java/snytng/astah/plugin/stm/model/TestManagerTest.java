package snytng.astah.plugin.stm.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.change_vision.jude.api.inf.model.ITransition;
import snytng.astah.plugin.stm.model.SimulationEngine.TransitionPath;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class TestManagerTest {

    @Mock
    ITransition transition1;
    @Mock
    ITransition transition2;

    @Test
    public void testRecordingFlow() {
        TestManager manager = new TestManager();
        
        // Initial state
        assertFalse(manager.isRecording());
        assertTrue(manager.getRecordedPaths().isEmpty());

        // Start Recording
        manager.startRecording();
        assertTrue(manager.isRecording());
        assertTrue(manager.getRecordedPaths().isEmpty());

        // Record Transitions
        when(transition1.getId()).thenReturn("t1");
        when(transition2.getId()).thenReturn("t2");
        
        TransitionPath path1 = new TransitionPath(Collections.singletonList(transition1));
        TransitionPath path2 = new TransitionPath(Collections.singletonList(transition2));
        
        manager.recordTransition(path1);
        manager.recordTransition(path2);
        
        assertEquals(2, manager.getRecordedPaths().size());
        assertEquals("t1", manager.getRecordedPaths().get(0).get(0));
        assertEquals("t2", manager.getRecordedPaths().get(1).get(0));

        // Stop Recording
        manager.stopRecording();
        assertFalse(manager.isRecording());
        
        // Verify transitions are still kept after stop
        assertEquals(2, manager.getRecordedPaths().size());
    }
    
    @Test
    public void testRecordTransition_NotRecording() {
        TestManager manager = new TestManager();
        
        // Record without starting
        TransitionPath path1 = new TransitionPath(Collections.singletonList(transition1));
        manager.recordTransition(path1);
        
        assertTrue(manager.getRecordedPaths().isEmpty());
    }
}