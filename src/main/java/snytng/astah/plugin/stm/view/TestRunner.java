package snytng.astah.plugin.stm.model.test;

import java.util.HashSet;
import java.util.List;

public class TestRunner {
    private final TestRunnerContext context;

    public TestRunner(TestRunnerContext context) {
        this.context = context;
    }

    public TestResult run(TestScript script) {
        TestResult result = new TestResult();
        
        for (TestScript.TestCommand cmd : script.getCommands()) {
            try {
                if (cmd instanceof TestScript.TargetCommand) {
                    String targetName = ((TestScript.TargetCommand) cmd).targetName;
                    context.changeTargetDiagram(targetName);
                    result.details.add("Target changed: " + targetName);
                } 
                else if (cmd instanceof TestScript.FireCommand) {
                    String eventName = ((TestScript.FireCommand) cmd).eventName;
                    context.fireEvent(eventName);
                    result.details.add("Fired: " + eventName);
                } 
                else if (cmd instanceof TestScript.AssertStateCommand) {
                    List<String> expected = ((TestScript.AssertStateCommand) cmd).expectedStates;
                    List<String> actual = context.getActiveStateNames();
                    
                    if (!isSameStates(expected, actual)) {
                        result.success = false;
                        result.details.add("\u274c AssertState Failed! Expected: " + expected + ", Actual: " + actual);
                        break;
                    }
                    result.details.add("\u2705 AssertState Passed: " + expected);
                }
                else if (cmd instanceof TestScript.AssertLogCommand) {
                    String expectedLog = ((TestScript.AssertLogCommand) cmd).expectedLogLine;
                    List<String> actualLogs = context.getRecentLogs();
                    
                    if (!containsLog(actualLogs, expectedLog)) {
                        result.success = false;
                        result.details.add("\u274c AssertLog Failed! Expected log not found: " + expectedLog);
                        break;
                    }
                    result.details.add("\u2705 AssertLog Passed: " + expectedLog);
                }
            } catch (Exception e) {
                result.success = false;
                result.details.add("\u274c Error during execution: " + e.getMessage());
                break;
            }
        }
        return result;
    }
    
    private boolean isSameStates(List<String> expected, List<String> actual) {
        if (expected == null || actual == null) return false;
        // 期待値と実際の状態（フルパス）の集合が完全に一致することを要求（厳密モード）
        return new HashSet<>(actual).equals(new HashSet<>(expected));
    }

    private boolean containsLog(List<String> actualLogs, String expectedLog) {
        if (actualLogs == null || expectedLog == null) return false;
        for (String log : actualLogs) {
            if (log.contains(expectedLog)) {
                return true;
            }
        }
        return false;
    }
}