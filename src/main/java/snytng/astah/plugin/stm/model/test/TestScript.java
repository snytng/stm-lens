package snytng.astah.plugin.stm.model.test;

import java.util.ArrayList;
import java.util.List;

public class TestScript {
    private final List<TestCommand> commands = new ArrayList<>();

    public void addCommand(TestCommand command) {
        commands.add(command);
    }

    public List<TestCommand> getCommands() {
        return commands;
    }

    public interface TestCommand { }

    public static class TargetCommand implements TestCommand {
        public final String targetName;
        public TargetCommand(String targetName) { this.targetName = targetName; }
    }

    public static class FireCommand implements TestCommand {
        public final String eventName;
        public FireCommand(String eventName) { this.eventName = eventName; }
    }

    public static class AssertStateCommand implements TestCommand {
        public final List<String> expectedStates;
        public AssertStateCommand(List<String> expectedStates) { this.expectedStates = expectedStates; }
    }

    public static class AssertLogCommand implements TestCommand {
        public final String expectedLogLine;
        public AssertLogCommand(String expectedLogLine) { this.expectedLogLine = expectedLogLine; }
    }
}