package snytng.astah.plugin.stm.model.test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestScriptParser {
    public TestScript parse(String text) throws ParseException {
        TestScript script = new TestScript();
        if (text == null || text.trim().isEmpty()) {
            return script;
        }

        String[] lines = text.split("\\r?\\n");
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            if (trimmedLine.startsWith("Target:")) {
                String targetName = trimmedLine.substring("Target:".length()).trim();
                script.addCommand(new TestScript.TargetCommand(targetName));
            } else if (trimmedLine.startsWith("Fire:")) {
                String eventName = trimmedLine.substring("Fire:".length()).trim();
                script.addCommand(new TestScript.FireCommand(eventName));
            } else if (trimmedLine.startsWith("AssertState:")) {
                String statesStr = trimmedLine.substring("AssertState:".length()).trim();
                List<String> states = Arrays.stream(statesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                script.addCommand(new TestScript.AssertStateCommand(states));
            } else if (trimmedLine.startsWith("AssertLog:")) {
                String logLine = trimmedLine.substring("AssertLog:".length()).trim();
                script.addCommand(new TestScript.AssertLogCommand(logLine));
            } else {
                throw new ParseException("Line " + lineNumber + ": 未知のコマンドです: " + trimmedLine);
            }
        }
        return script;
    }
}