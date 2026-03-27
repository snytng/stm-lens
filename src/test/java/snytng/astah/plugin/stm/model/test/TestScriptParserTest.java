package snytng.astah.plugin.stm.model.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class TestScriptParserTest {

    @Test
    void testParseBasicCommands() throws ParseException {
        String text = "Target: Diagram1\n" +
                      "Fire: EventA\n" +
                      "AssertState: State1, State2 \n" +
                      "AssertLog: log message here";

        TestScriptParser parser = new TestScriptParser();
        TestScript script = parser.parse(text);

        List<TestScript.TestCommand> commands = script.getCommands();
        assertEquals(4, commands.size());

        assertTrue(commands.get(0) instanceof TestScript.TargetCommand);
        assertEquals("Diagram1", ((TestScript.TargetCommand) commands.get(0)).targetName);

        assertTrue(commands.get(1) instanceof TestScript.FireCommand);
        assertEquals("EventA", ((TestScript.FireCommand) commands.get(1)).eventName);

        assertTrue(commands.get(2) instanceof TestScript.AssertStateCommand);
        List<String> states = ((TestScript.AssertStateCommand) commands.get(2)).expectedStates;
        assertEquals(2, states.size());
        assertEquals("State1", states.get(0));
        assertEquals("State2", states.get(1));

        assertTrue(commands.get(3) instanceof TestScript.AssertLogCommand);
        assertEquals("log message here", ((TestScript.AssertLogCommand) commands.get(3)).expectedLogLine);
    }

    @Test
    void testParseEmptyLinesAndSpaces() throws ParseException {
        String text = "  \n" +
                      "  Target:   Diagram2  \n" +
                      "\n" +
                      "Fire: EventB \n";

        TestScriptParser parser = new TestScriptParser();
        TestScript script = parser.parse(text);

        List<TestScript.TestCommand> commands = script.getCommands();
        assertEquals(2, commands.size());

        assertTrue(commands.get(0) instanceof TestScript.TargetCommand);
        assertEquals("Diagram2", ((TestScript.TargetCommand) commands.get(0)).targetName);

        assertTrue(commands.get(1) instanceof TestScript.FireCommand);
        assertEquals("EventB", ((TestScript.FireCommand) commands.get(1)).eventName);
    }

    @Test
    void testUnknownCommandThrowsException() {
        String text = "Target: Diagram1\n" +
                      "Unknown: something\n";

        TestScriptParser parser = new TestScriptParser();
        
        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(text));
        
        assertTrue(exception.getMessage().contains("Line 2"));
        assertTrue(exception.getMessage().contains("Unknown: something"));
    }
}