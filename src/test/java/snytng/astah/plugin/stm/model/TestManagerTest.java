package snytng.astah.plugin.stm.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.ITaggedValue;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class TestManagerTest {

    @Mock
    IStateMachine stateMachine;

    @Mock
    ITaggedValue taggedValueNames;

    @Mock
    ITaggedValue taggedValueScript;

    @Test
    public void testLoadTestCaseNames_Empty() {
        TestManager manager = new TestManager();
        when(stateMachine.getTaggedValues()).thenReturn(new ITaggedValue[0]);
        
        List<String> names = manager.loadTestCaseNames(stateMachine);
        assertTrue(names.isEmpty());
    }
    
    @Test
    public void testLoadTestCaseNames_HasNames() {
        TestManager manager = new TestManager();
        
        when(taggedValueNames.getKey()).thenReturn("stm_test_case_names");
        when(taggedValueNames.getValue()).thenReturn("Test1,Test2,Test3");
        when(stateMachine.getTaggedValues()).thenReturn(new ITaggedValue[]{taggedValueNames});
        
        List<String> names = manager.loadTestCaseNames(stateMachine);
        assertEquals(3, names.size());
        assertTrue(names.contains("Test1"));
        assertTrue(names.contains("Test2"));
        assertTrue(names.contains("Test3"));
    }

    @Test
    public void testGetTestCaseScript_Found() {
        TestManager manager = new TestManager();
        
        when(taggedValueScript.getKey()).thenReturn("stm_test_case_Test1");
        when(taggedValueScript.getValue()).thenReturn("Target: model\nFire: event1\nAssertState: State2");
        when(stateMachine.getTaggedValues()).thenReturn(new ITaggedValue[]{taggedValueScript});
        
        String script = manager.getTestCaseScript("Test1", stateMachine);
        assertEquals("Target: model\nFire: event1\nAssertState: State2", script);
    }
}