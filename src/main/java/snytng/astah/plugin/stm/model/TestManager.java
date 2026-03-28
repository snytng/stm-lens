package snytng.astah.plugin.stm.model;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.BasicModelEditor;
import com.change_vision.jude.api.inf.editor.ITransactionManager;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.ITaggedValue;
import com.change_vision.jude.api.inf.model.ITransition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestManager {
    public void saveTestCase(String name, String scriptText, IStateMachine stateMachine) throws Exception {
        if (name == null || name.isEmpty() || stateMachine == null || scriptText == null) return;
        
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
            saveTaggedValue(editor, stateMachine, "stm_test_case_" + name, scriptText);
            
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

    public String getTestCaseScript(String name, IStateMachine stateMachine) {
        String dataKey = "stm_test_case_" + name;
        return getTaggedValue(stateMachine, dataKey);
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