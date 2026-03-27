package snytng.astah.plugin.stm.model.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TestRunnerTest {

    static class DummyContext implements TestRunnerContext {
        public String currentTarget = "";
        public List<String> firedEvents = new ArrayList<>();
        public List<String> activeStates = new ArrayList<>();
        public List<String> recentLogs = new ArrayList<>();

        @Override
        public void changeTargetDiagram(String targetName) {
            this.currentTarget = targetName;
        }

        @Override
        public void fireEvent(String eventName) {
            this.firedEvents.add(eventName);
        }

        @Override
        public List<String> getActiveStateNames() {
            return activeStates;
        }

        @Override
        public List<String> getRecentLogs() {
            return recentLogs;
        }
    }

    @Test
    void testRunSuccess() {
        TestScript script = new TestScript();
        script.addCommand(new TestScript.TargetCommand("Diagram1"));
        script.addCommand(new TestScript.FireCommand("EventA"));
        script.addCommand(new TestScript.AssertStateCommand(Arrays.asList("State1", "State2")));
        script.addCommand(new TestScript.AssertLogCommand("Action executed"));

        DummyContext context = new DummyContext();
        context.activeStates = Arrays.asList("State2", "State1"); // 順不同でもOK
        context.recentLogs = Arrays.asList("Some log", "Action executed successfully", "Another log");

        TestRunner runner = new TestRunner(context);
        TestResult result = runner.run(script);

        assertTrue(result.success);
        assertEquals("Diagram1", context.currentTarget);
        assertTrue(context.firedEvents.contains("EventA"));
    }

    @Test
    void testAssertStateFailed() {
        TestScript script = new TestScript();
        script.addCommand(new TestScript.AssertStateCommand(Arrays.asList("State1", "State2")));

        DummyContext context = new DummyContext();
        context.activeStates = Arrays.asList("State1", "State3"); // 異なる状態

        TestRunner runner = new TestRunner(context);
        TestResult result = runner.run(script);

        assertFalse(result.success);
        assertTrue(result.details.stream().anyMatch(msg -> msg.contains("AssertState Failed!")));
    }

    @Test
    void testAssertLogFailed() {
        TestScript script = new TestScript();
        script.addCommand(new TestScript.AssertLogCommand("Expected Action"));

        DummyContext context = new DummyContext();
        context.recentLogs = Arrays.asList("Some log", "Different action");

        TestRunner runner = new TestRunner(context);
        TestResult result = runner.run(script);

        assertFalse(result.success);
        assertTrue(result.details.stream().anyMatch(msg -> msg.contains("AssertLog Failed!")));
    }
}