package snytng.astah.plugin.stm.model.test;

import java.util.List;

public interface TestRunnerContext {
    void changeTargetDiagram(String targetName) throws Exception;
    void fireEvent(String eventName) throws Exception;
    List<String> getActiveStateNames();
    List<String> getRecentLogs();
}