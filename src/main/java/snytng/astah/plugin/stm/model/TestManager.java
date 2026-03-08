package snytng.astah.plugin.stm.model;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.BasicModelEditor;
import com.change_vision.jude.api.inf.editor.ITransactionManager;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.ITaggedValue;
import com.change_vision.jude.api.inf.model.ITransition;
import snytng.astah.plugin.stm.model.SimulationEngine.TransitionPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestManager {
    private boolean isRecording = false;
    private List<List<String>> recordedPaths = new ArrayList<>();

    public void startRecording() {
        isRecording = true;
        recordedPaths.clear();
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void recordTransition(TransitionPath path) {
        if (isRecording && path != null) {
            List<String> ids = new ArrayList<>();
            for (ITransition t : path.transitions) {
                ids.add(t.getId());
            }
            recordedPaths.add(ids);
        }
    }
    
    public List<List<String>> getRecordedPaths() {
        return new ArrayList<>(recordedPaths);
    }

    public void saveTestCase(String name, IStateMachine stateMachine) throws Exception {
        if (name == null || name.isEmpty() || stateMachine == null) return;
        
        ITransactionManager tm = AstahAPI.getAstahAPI().getProjectAccessor().getTransactionManager();
        try {
            BasicModelEditor editor = AstahAPI.getAstahAPI().getProjectAccessor().getModelEditorFactory().getBasicModelEditor();
            
            tm.beginTransaction();
            
            // 1. Update test case list
            String listKey = "stm_test_case_names";
            String currentList = getTaggedValue(stateMachine, listKey);
            List<String> names = new ArrayList<>();
            if (currentList != null && !currentList.isEmpty()) {
                names.addAll(Arrays.asList(currentList.split(",")));
            }
            if (!names.contains(name)) {
                names.add(name);
                String newList = String.join(",", names);
                saveTaggedValue(editor, stateMachine, listKey, newList);
            }
            
            // 2. Save test case data
            // Format: id1,id2;id3;id4,id5 (semicolon separates steps, comma separates IDs in path)
            StringBuilder sb = new StringBuilder();
            for (List<String> step : recordedPaths) {
                if (sb.length() > 0) sb.append(";");
                sb.append(String.join(",", step));
            }
            saveTaggedValue(editor, stateMachine, "stm_test_case_" + name, sb.toString());
            
            tm.endTransaction();
        } catch (Exception e) {
            tm.abortTransaction();
            throw e;
        }
    }

    public List<String> loadTestCaseNames(IStateMachine stateMachine) {
        String listKey = "stm_test_case_names";
        String currentList = getTaggedValue(stateMachine, listKey);
        if (currentList != null && !currentList.isEmpty()) {
            return Arrays.asList(currentList.split(","));
        }
        return new ArrayList<>();
    }

    public void playTestCase(String name, IStateMachine stateMachine, SimulationEngine engine, Runnable onStep) {
        String dataKey = "stm_test_case_" + name;
        String data = getTaggedValue(stateMachine, dataKey);
        if (data == null || data.isEmpty()) return;
        
        String[] steps = data.split(";");
        for (String stepData : steps) {
            if (stepData.isEmpty()) continue;
            List<String> targetIds = Arrays.asList(stepData.split(","));
            
            List<TransitionPath> available = engine.getAvailableTransitions();
            TransitionPath matchedPath = null;
            
            for (TransitionPath path : available) {
                List<String> pathIds = path.transitions.stream().map(ITransition::getId).collect(Collectors.toList());
                if (pathIds.equals(targetIds)) {
                    matchedPath = path;
                    break;
                }
            }
            
            if (matchedPath != null) {
                engine.step(matchedPath, null);
                if (onStep != null) onStep.run();
            } else {
                System.err.println("Playback failed: Matching transition not found for step " + stepData);
                break;
            }
        }
    }
    
    public void deleteTestCase(String name, IStateMachine stateMachine) throws Exception {
        if (name == null || name.isEmpty() || stateMachine == null) return;

        ITransactionManager tm = AstahAPI.getAstahAPI().getProjectAccessor().getTransactionManager();
        try {
            BasicModelEditor editor = AstahAPI.getAstahAPI().getProjectAccessor().getModelEditorFactory().getBasicModelEditor();
            tm.beginTransaction();

            // 1. Update list
            String listKey = "stm_test_case_names";
            String currentList = getTaggedValue(stateMachine, listKey);
            List<String> names = new ArrayList<>();
            if (currentList != null && !currentList.isEmpty()) {
                names.addAll(Arrays.asList(currentList.split(",")));
            }
            if (names.contains(name)) {
                names.remove(name);
                String newList = String.join(",", names);
                saveTaggedValue(editor, stateMachine, listKey, newList);
            }

            // 2. Remove data
            removeTaggedValue(editor, stateMachine, "stm_test_case_" + name);

            tm.endTransaction();
        } catch (Exception e) {
            tm.abortTransaction();
            throw e;
        }
    }

    public void renameTestCase(String oldName, String newName, IStateMachine stateMachine) throws Exception {
        if (oldName == null || newName == null || stateMachine == null) return;
        if (oldName.equals(newName)) return;

        ITransactionManager tm = AstahAPI.getAstahAPI().getProjectAccessor().getTransactionManager();
        try {
            BasicModelEditor editor = AstahAPI.getAstahAPI().getProjectAccessor().getModelEditorFactory().getBasicModelEditor();
            tm.beginTransaction();

            // 1. Check if new name exists
            String listKey = "stm_test_case_names";
            String currentList = getTaggedValue(stateMachine, listKey);
            List<String> names = new ArrayList<>();
            if (currentList != null && !currentList.isEmpty()) {
                names.addAll(Arrays.asList(currentList.split(",")));
            }
            if (names.contains(newName)) {
                throw new IllegalArgumentException("Test case '" + newName + "' already exists.");
            }

            // 2. Get old data
            String dataKey = "stm_test_case_" + oldName;
            String data = getTaggedValue(stateMachine, dataKey);
            if (data == null) {
                 throw new IllegalArgumentException("Test case '" + oldName + "' not found.");
            }

            // 3. Save new data
            saveTaggedValue(editor, stateMachine, "stm_test_case_" + newName, data);

            // 4. Update list (remove old, add new)
            if (names.contains(oldName)) {
                names.remove(oldName);
            }
            names.add(newName);
            String newList = String.join(",", names);
            saveTaggedValue(editor, stateMachine, listKey, newList);

            // 5. Remove old data
            removeTaggedValue(editor, stateMachine, dataKey);

            tm.endTransaction();
        } catch (Exception e) {
            tm.abortTransaction();
            throw e;
        }
    }

    public String getTestCaseInfo(String name, IStateMachine stateMachine) {
        String dataKey = "stm_test_case_" + name;
        String data = getTaggedValue(stateMachine, dataKey);
        if (data == null) return "Test case not found.";
        
        String[] steps = data.split(";");
        return String.format("Test Case: %s\nSteps: %d\nRaw Data: %s", name, steps.length, data);
    }

    public boolean isRecording() {
        return isRecording;
    }

    private String getTaggedValue(IStateMachine sm, String key) {
        for (ITaggedValue tv : sm.getTaggedValues()) {
            if (tv.getKey().equals(key)) {
                return tv.getValue();
            }
        }
        return null;
    }
    
    private void saveTaggedValue(BasicModelEditor editor, IStateMachine sm, String key, String value) throws Exception {
        removeTaggedValue(editor, sm, key);
        editor.createTaggedValue(sm, key, value);
    }

    private void removeTaggedValue(BasicModelEditor editor, IStateMachine sm, String key) throws Exception {
        ITaggedValue targetTv = null;
        for (ITaggedValue tv : sm.getTaggedValues()) {
            if (tv.getKey().equals(key)) {
                targetTv = tv;
                break;
            }
        }
        if (targetTv != null) {
            editor.delete(targetTv);
        }
    }
}