package snytng.astah.plugin.stm.model.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TestRunner {
    private final TestRunnerContext context;

    public TestRunner(TestRunnerContext context) {
        this.context = context;
    }

    public TestResult run(TestScript script) {
        List<String> details = new ArrayList<>();
        boolean success = true;

        try {
            for (TestScript.Command cmd : script.commands) {
                if (cmd instanceof TestScript.TargetCommand) {
                    context.changeTargetDiagram(((TestScript.TargetCommand) cmd).targetName);
                    details.add("Target changed: " + ((TestScript.TargetCommand) cmd).targetName);
                } else if (cmd instanceof TestScript.AssertStateCommand) {
                    List<String> expected = ((TestScript.AssertStateCommand) cmd).states;
                    List<String> actual = context.getActiveStateNames();
                    if (isSameStates(expected, actual)) {
                        details.add("✅ AssertState Passed: " + expected);
                    } else {
                        details.add("❌ AssertState Failed! Expected: " + expected + ", Actual: " + actual);
                        success = false;
                        break;
                    }
                } else if (cmd instanceof TestScript.FireCommand) {
                    String event = ((TestScript.FireCommand) cmd).eventName;
                    context.fireEvent(event);
                    details.add("Fired: " + event);
                } else if (cmd instanceof TestScript.AssertLogCommand) {
                    String expectedLog = ((TestScript.AssertLogCommand) cmd).logPattern;
                    if (containsLog(context.getRecentLogs(), expectedLog)) {
                        details.add("✅ AssertLog Passed: " + expectedLog);
                    } else {
                        details.add("❌ AssertLog Failed! Expected log containing: " + expectedLog);
                        success = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            details.add("❌ Error during execution: " + e.getMessage());
            success = false;
        }

        return new TestResult(success, details);
    }

    private boolean isSameStates(List<String> expected, List<String> actual) {
        if (expected == null || actual == null) return false;
        // 期待される状態が、現在のアクティブな階層（親状態を含む）にすべて含まれているかを確認
        return new HashSet<>(actual).containsAll(expected);
    }

    private boolean containsLog(List<String> actualLogs, String expectedLog) {
        return actualLogs.stream().anyMatch(log -> log.contains(expectedLog));
    }
}