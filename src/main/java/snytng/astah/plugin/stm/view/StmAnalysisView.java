package snytng.astah.plugin.stm.view;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionEvent;
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionListener;
import com.change_vision.jude.api.inf.view.IViewManager;
import com.change_vision.jude.api.inf.view.IEntitySelectionEvent;
import com.change_vision.jude.api.inf.view.IEntitySelectionListener;
import snytng.astah.plugin.stm.model.DiagramHighlighter;
import snytng.astah.plugin.stm.model.SimulationEngine;
import snytng.astah.plugin.stm.model.TestManager;
import snytng.astah.plugin.stm.model.TimerManager;
import snytng.astah.plugin.stm.model.IllegalSimulationStateException;
import snytng.astah.plugin.stm.model.test.ParseException;
import snytng.astah.plugin.stm.model.test.TestResult;
import snytng.astah.plugin.stm.model.test.TestRunner;
import snytng.astah.plugin.stm.model.test.TestRunnerContext;
import snytng.astah.plugin.stm.model.test.TestScript;
import snytng.astah.plugin.stm.model.test.TestScriptParser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class StmAnalysisView extends JPanel implements IPluginExtraTabView {

    private JButton startButton;
    private JButton resetButton;
    private JButton copyDebugButton;
    private JLabel stateLabel;
    private JCheckBox showActionsCheckbox;
    private JCheckBox fastModeCheckbox;
    private JPanel eventPanel;
    private JToggleButton testToggleBtn;
    
    // Test UI
    private JButton saveButton;
    private JButton renameButton;
    private JButton deleteButton;
    private JComboBox<String> testCaseCombo;

    // Test Script UI
    private JTextArea testScriptArea;
    private JTextArea testResultArea;
    private JButton runTestScriptButton;
    private JLabel testStatusLabel;
    
    // Navigation UI
    private JButton startNavButton;
    private JButton prevNavButton;
    private JButton nextNavButton;
    private JButton endNavButton;
    private JLabel stepLabel;
    
    private JTextArea logArea;

    private final SimulationEngine engine = new SimulationEngine();
    private final TestManager testManager = new TestManager();
    private DiagramHighlighter highlighter;
    private final Set<String> firedTimers = new HashSet<>();
    
    // Script Generation History
    private String currentTargetDiagramName;
    private List<String> initialStates = new ArrayList<>();
    private final List<ScriptStep> scriptHistory = new ArrayList<>();

    private static class ScriptStep {
        String eventName;
        List<String> states;
        ScriptStep(String eventName, List<String> states) {
            this.eventName = eventName;
            this.states = states;
        }
    }

    private final IDiagramEditorSelectionListener diagramEditorSelectionListener = e -> {
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
            IDiagram currentDiagram = viewManager.getCurrentDiagram();
            SwingUtilities.invokeLater(() -> 
                updateTestCaseListIfStateMachineDiagram(currentDiagram)
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    };

    private final IEntitySelectionListener entitySelectionListener = new IEntitySelectionListener() {
        @Override
        public void entitySelectionChanged(IEntitySelectionEvent e) {
            try {
                ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
                IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
                IDiagram currentDiagram = viewManager.getCurrentDiagram();
                SwingUtilities.invokeLater(() -> updateTestCaseListIfStateMachineDiagram(currentDiagram));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    private static final Color[] EVENT_COLORS = {
        new Color(255, 180, 180), // Red-ish
        new Color(180, 255, 180), // Green-ish
        new Color(180, 180, 255), // Blue-ish
        new Color(255, 255, 180), // Yellow-ish
        new Color(255, 180, 255), // Magenta-ish
        new Color(180, 255, 255)  // Cyan-ish
    };

    public StmAnalysisView() {
        initComponents();
        try {
            this.highlighter = new DiagramHighlighter();
        } catch (ClassNotFoundException e) {
            this.highlighter = null;
            logArea.append("Error: astah* API not found.\n");
        }
        
        engine.setStepListener(result -> {
            if (result != null) {
                firedTimers.clear(); // Reset fired timers on step
                
                String eventName = "";
                SimulationEngine.TransitionPath path = null;
                if (result.executedPaths != null && !result.executedPaths.isEmpty()) {
                    path = result.executedPaths.get(0);
                } else if (result.path != null) {
                    path = result.path;
                }
                
                if (path != null) {
                    eventName = path.getRoot().getEvent();
                    if (eventName == null || eventName.trim().isEmpty()) {
                        eventName = "(anonymous)";
                    } else {
                        eventName = eventName.trim();
                    }
                    StringBuilder guards = new StringBuilder();
                    for (ITransition t : path.transitions) {
                        String g = t.getGuard();
                        if (g != null && !g.trim().isEmpty()) {
                            if (guards.length() > 0) guards.append(" & ");
                            guards.append(g.trim());
                        }
                    }
                    if (guards.length() > 0) {
                        eventName += " [" + guards.toString() + "]";
                    }
                }
                recordStep(eventName);
                
                printStepResult(result);
                refreshUI();
            }
        });

        engine.setTimerListener(paths -> {
            SwingUtilities.invokeLater(() -> {
                paths.forEach(p -> firedTimers.add(p.getRoot().getEvent()));
                refreshUI();
            });
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 1. Top Panel (Control, Navigation, Settings & Test Tools)
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

        // 1行目: メインコントロール
        JPanel controlLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        
        startButton = new JButton("Start");
        resetButton = new JButton("Reset");
        
        // Navigation UI (Top Panelに移動)
        startNavButton = new JButton("|<<");
        prevNavButton = new JButton("<");
        nextNavButton = new JButton(">");
        endNavButton = new JButton(">>|");
        stepLabel = new JLabel("Step: 0 / 0");

        startNavButton.addActionListener(e -> { engine.goToStart(); onTimeTravel(); logArea.append("Moved to Start.\n"); });
        prevNavButton.addActionListener(e -> { engine.stepBack(); onTimeTravel(); logArea.append("Stepped back.\n"); });
        nextNavButton.addActionListener(e -> { engine.stepForward(); onTimeTravel(); logArea.append("Stepped forward.\n"); });
        endNavButton.addActionListener(e -> { engine.goToEnd(); onTimeTravel(); logArea.append("Moved to End.\n"); });

        copyDebugButton = new JButton("Copy Debug");
        stateLabel = new JLabel("Current State: -");
        showActionsCheckbox = new JCheckBox("Show Actions", true);
        fastModeCheckbox = new JCheckBox("Fast Mode", false);
        testToggleBtn = new JToggleButton("▼ Test Tools");

        startButton.addActionListener(e -> startSimulation());
        resetButton.addActionListener(e -> resetSimulation());
        copyDebugButton.addActionListener(e -> copyDebugInfo());
        fastModeCheckbox.addActionListener(e -> engine.setFastMode(fastModeCheckbox.isSelected()));

        controlLine.add(startButton);
        controlLine.add(resetButton);
        controlLine.add(Box.createHorizontalStrut(5));
        controlLine.add(startNavButton);
        controlLine.add(prevNavButton);
        controlLine.add(stepLabel);
        controlLine.add(nextNavButton);
        controlLine.add(endNavButton);
        controlLine.add(Box.createHorizontalStrut(5));
        controlLine.add(stateLabel);
        controlLine.add(Box.createHorizontalStrut(5));
        controlLine.add(showActionsCheckbox);
        controlLine.add(fastModeCheckbox);
        controlLine.add(copyDebugButton);
        controlLine.add(Box.createHorizontalStrut(5));
        controlLine.add(testToggleBtn);

        // 2行目: Test Panel
        JPanel testPanel = new JPanel(new BorderLayout()); // レイアウト変更

        // Test Panel - Top (Buttons)
        JPanel testButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        saveButton = new JButton("Save");
        renameButton = new JButton("Rename");
        deleteButton = new JButton("Delete");
        testCaseCombo = new JComboBox<>();

        saveButton.addActionListener(e -> saveTestScript());
        renameButton.addActionListener(e -> renameTest());
        deleteButton.addActionListener(e -> deleteTest());

        // ドロップダウンの選択変更時にスクリプトをロードする
        testCaseCombo.addActionListener(e -> {
            if (testCaseCombo.getSelectedItem() != null && testCaseCombo.isPopupVisible()) {
                loadSelectedTestScript();
            }
        });

        testButtonPanel.add(saveButton);
        testButtonPanel.add(new JLabel("Saved Tests:"));
        testButtonPanel.add(testCaseCombo);
        testButtonPanel.add(renameButton);
        testButtonPanel.add(deleteButton);

        // Test Panel - Center (Scripting)
        JPanel testScriptingPanel = new JPanel(new BorderLayout(5, 5));
        
        // Script Control Bar
        JPanel scriptControlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        runTestScriptButton = new JButton("▶ Run Script");
        runTestScriptButton.addActionListener(e -> runTestScript());
        
        JButton clearScriptButton = new JButton("🗑");
        clearScriptButton.setToolTipText("Clear Script");
        clearScriptButton.addActionListener(e -> testScriptArea.setText(""));

        testStatusLabel = new JLabel(" Ready");
        testStatusLabel.setFont(testStatusLabel.getFont().deriveFont(java.awt.Font.BOLD));
        
        scriptControlBar.add(runTestScriptButton);
        scriptControlBar.add(clearScriptButton);
        scriptControlBar.add(testStatusLabel);

        testScriptArea = new JTextArea(8, 40);
        testScriptArea.setBorder(BorderFactory.createTitledBorder("Test Script"));
        JScrollPane scriptScrollPane = new JScrollPane(testScriptArea);

        testResultArea = new JTextArea(8, 40);
        testResultArea.setEditable(false);
        testResultArea.setBorder(BorderFactory.createTitledBorder("Test Result"));
        JScrollPane resultScrollPane = new JScrollPane(testResultArea);


        JSplitPane testSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scriptScrollPane, resultScrollPane);
        testSplitPane.setResizeWeight(0.5); // 左右均等に分割
        testSplitPane.setOneTouchExpandable(true);
        testSplitPane.setContinuousLayout(true);

        testScriptingPanel.add(scriptControlBar, BorderLayout.NORTH);
        testScriptingPanel.add(testSplitPane, BorderLayout.CENTER);

        testPanel.add(testButtonPanel, BorderLayout.NORTH);
        testPanel.add(testScriptingPanel, BorderLayout.CENTER);
        testPanel.setVisible(false); // 初期状態は非表示

        testToggleBtn.addActionListener(e -> {
            boolean isSelected = testToggleBtn.isSelected();
            testPanel.setVisible(isSelected);
            testToggleBtn.setText(isSelected ? "▲ Test Tools" : "▼ Test Tools");
            northPanel.revalidate();
            northPanel.repaint();
        });

        northPanel.add(controlLine);
        northPanel.add(testPanel);

        add(northPanel, BorderLayout.NORTH);

        // 2. Center Panel (Events)
        JPanel eventContainer = new JPanel(new BorderLayout());

        eventPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        eventPanel.setBackground(Color.WHITE);

        JScrollPane eventScrollPane = new JScrollPane(eventPanel);
        eventScrollPane.setBorder(null);
        eventContainer.add(eventScrollPane, BorderLayout.CENTER);

        // 3. Right Panel (Log)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setRows(6);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        // SplitPane for Events and Log
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, eventContainer, logScrollPane);
        splitPane.setResizeWeight(0.4); // 左側(Events)の初期幅を40%に設定
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);

        add(splitPane, BorderLayout.CENTER);

        // Add MouseListener to refresh UI when user hovers over the view
        // This is a workaround because astah* API does not provide an event for active diagram change.
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                try {
                    ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
                    IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
                    IDiagram currentDiagram = viewManager.getCurrentDiagram();
                    updateTestCaseListIfStateMachineDiagram(currentDiagram);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private boolean clearScriptIfConfirmed(String actionName) {
        if (!testScriptArea.getText().trim().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "The current test script will be cleared. Are you sure you want to " + actionName + "?",
                "Confirm " + actionName,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        testScriptArea.setText("");
        return true;
    }

    private void startSimulation() {
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
            IDiagram currentDiagram = viewManager.getCurrentDiagram();

            if (currentDiagram instanceof IStateMachineDiagram) {
                if (!clearScriptIfConfirmed("start a new simulation")) {
                    return;
                }

                if(highlighter != null) {
                    highlighter.setDiagram(currentDiagram);
                    highlighter.clearAll();
                }

                firedTimers.clear();
                try {
                    SimulationEngine.StepResult result = engine.start((IStateMachineDiagram) currentDiagram);
                    currentTargetDiagramName = currentDiagram.getName();
                    initialStates = engine.getCurrentVertices().stream().map(IVertex::getName).collect(Collectors.toList());
                    scriptHistory.clear();
                    
                    logArea.setText("Simulation started.\n");
                    printStepResult(result);
                    refreshUI();
                    updateTestCaseList((IStateMachineDiagram) currentDiagram);
                    regenerateScriptFromHistory();
                } catch (IllegalSimulationStateException e) {
                    handleSimulationException(e);
                }
            } else {
                logArea.append("Please open a State Machine Diagram.\n");
            }
        } catch (Exception e) {
            logArea.append("Error starting simulation: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private void resetSimulation() {
        if (!clearScriptIfConfirmed("reset the simulation")) {
            return;
        }

        if(highlighter != null) {
            highlighter.clearAll();
        }

        firedTimers.clear();
        engine.start(null);
        stateLabel.setText("Current State: -");
        eventPanel.removeAll();
        eventPanel.revalidate();
        eventPanel.repaint();
        scriptHistory.clear();
        currentTargetDiagramName = null;
        initialStates.clear();
        logArea.setText("Simulation reset.\n");
    }
    
    private void regenerateScriptFromHistory() {
        if (currentTargetDiagramName == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Target: ").append(currentTargetDiagramName).append("\n");
        sb.append("AssertState: ").append(String.join(", ", initialStates)).append("\n");
        for (ScriptStep step : scriptHistory) {
            String evt = step.eventName != null ? step.eventName.trim() : "";
            sb.append("Fire: ").append(evt).append("\n");
            sb.append("AssertState: ").append(String.join(", ", step.states)).append("\n");
        }
        testScriptArea.setText(sb.toString());
    }
    
    private void saveTestScript() {
        String scriptText = testScriptArea.getText();
        if (scriptText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Script is empty.");
            return;
        }
        
        String name = JOptionPane.showInputDialog(this, "Enter test case name:", testCaseCombo.getSelectedItem());
        if (name != null && !name.trim().isEmpty()) {
            try {
                ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
                IDiagram currentDiagram = projectAccessor.getViewManager().getDiagramViewManager().getCurrentDiagram();
                if (currentDiagram instanceof IStateMachineDiagram) {
                    IStateMachine sm = ((IStateMachineDiagram) currentDiagram).getStateMachine();
                    
                    List<String> existingTests = testManager.loadTestCaseNames(sm);
                    if (existingTests.contains(name)) {
                        int confirm = JOptionPane.showConfirmDialog(this,
                            "Test case '" + name + "' already exists. Overwrite?",
                            "Confirm Overwrite",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                        if (confirm != JOptionPane.YES_OPTION) return;
                    }
                    
                    testManager.saveTestCase(name, scriptText, sm);
                    logArea.append("Test case '" + name + "' saved to StateMachine '" + sm.getName() + "'.\n");
                    updateTestCaseList((IStateMachineDiagram) currentDiagram);
                    testCaseCombo.setSelectedItem(name);
                }
            } catch (Exception e) {
                logArea.append("Failed to save test case: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
    }
    
    private void loadSelectedTestScript() {
        String selectedTest = (String) testCaseCombo.getSelectedItem();
        if (selectedTest == null) return;
        
        if (!testScriptArea.getText().trim().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "The current script will be overwritten. Are you sure you want to load '" + selectedTest + "'?", 
                "Load Test Script", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagram currentDiagram = projectAccessor.getViewManager().getDiagramViewManager().getCurrentDiagram();
            if (currentDiagram instanceof IStateMachineDiagram) {
                String scriptText = testManager.getTestCaseScript(selectedTest, ((IStateMachineDiagram) currentDiagram).getStateMachine());
                if (scriptText != null) {
                    testScriptArea.setText(scriptText);
                    logArea.append("Loaded test script: " + selectedTest + "\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onTimeTravel() {
        refreshUI();
        int currentIdx = engine.getCurrentSnapshotIndex();
        if (currentIdx >= 0 && scriptHistory.size() > currentIdx) {
            scriptHistory.subList(currentIdx, scriptHistory.size()).clear();
        }
        regenerateScriptFromHistory();
    }

    private void recordStep(String eventName) {
        int currentIdx = engine.getCurrentSnapshotIndex();
        if (scriptHistory.size() >= currentIdx) return; // Prevent duplicate recording

        List<String> currentStates = engine.getCurrentVertices().stream().map(IVertex::getName).collect(Collectors.toList());
        scriptHistory.add(new ScriptStep(eventName, currentStates));

        StringBuilder sb = new StringBuilder();
        sb.append("Fire: ").append(eventName != null ? eventName.trim() : "").append("\n");
        sb.append("AssertState: ").append(String.join(", ", currentStates)).append("\n");
        testScriptArea.append(sb.toString());
    }

    private void renameTest() {
        String selectedTest = (String) testCaseCombo.getSelectedItem();
        if (selectedTest == null) return;

        String newName = JOptionPane.showInputDialog(this, "Enter new name for '" + selectedTest + "':", selectedTest);
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(selectedTest)) {
            try {
                ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
                IDiagram currentDiagram = projectAccessor.getViewManager().getDiagramViewManager().getCurrentDiagram();
                if (currentDiagram instanceof IStateMachineDiagram) {
                    testManager.renameTestCase(selectedTest, newName, ((IStateMachineDiagram) currentDiagram).getStateMachine());
                    logArea.append("Test case renamed: " + selectedTest + " -> " + newName + "\n");
                    updateTestCaseList((IStateMachineDiagram) currentDiagram);
                    testCaseCombo.setSelectedItem(newName);
                }
            } catch (Exception e) {
                logArea.append("Error renaming test case: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
    }

    private void deleteTest() {
        String selectedTest = (String) testCaseCombo.getSelectedItem();
        if (selectedTest == null) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete '" + selectedTest + "'?", "Delete Test Case", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
                IDiagram currentDiagram = projectAccessor.getViewManager().getDiagramViewManager().getCurrentDiagram();
                if (currentDiagram instanceof IStateMachineDiagram) {
                    testManager.deleteTestCase(selectedTest, ((IStateMachineDiagram) currentDiagram).getStateMachine());
                    logArea.append("Test case deleted: " + selectedTest + "\n");
                    updateTestCaseList((IStateMachineDiagram) currentDiagram);
                }
            } catch (Exception e) {
                logArea.append("Error deleting test case: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
    }

    private void runTestScript() {
        String scriptText = testScriptArea.getText();
        testResultArea.setText("Running test script...\n");
        testStatusLabel.setText(" ⏳ Running...");
        testStatusLabel.setForeground(Color.BLACK);

        TestScriptParser parser = new TestScriptParser();
        try {
            TestScript script = parser.parse(scriptText);

            TestRunnerContext context = createTestRunnerContext();
            TestRunner runner = new TestRunner(context);

            // Run in a separate thread to avoid blocking the UI
            new Thread(() -> {
                TestResult result = runner.run(script);
                SwingUtilities.invokeLater(() -> {
                    StringBuilder sb = new StringBuilder();
                    if (result.success) {
                        sb.append("✅✅✅ TEST PASSED ✅✅✅\n");
                        testStatusLabel.setText(" ✅ PASSED");
                        testStatusLabel.setForeground(new Color(0, 128, 0)); // Dark Green
                    } else {
                        sb.append("❌❌❌ TEST FAILED ❌❌❌\n");
                        testStatusLabel.setText(" ❌ FAILED");
                        testStatusLabel.setForeground(Color.RED);
                    }
                    sb.append("---------------------------\n");
                    result.details.forEach(d -> sb.append(d).append("\n"));
                    testResultArea.setText(sb.toString());
                });
            }).start();

        } catch (ParseException e) {
            testResultArea.setText("❌ SCRIPT PARSE ERROR ❌\n" + e.getMessage());
            testStatusLabel.setText(" ❌ PARSE ERROR");
            testStatusLabel.setForeground(Color.RED);
        }
    }

    private TestRunnerContext createTestRunnerContext() {
        return new TestRunnerContext() {
            @Override
            public void changeTargetDiagram(String targetName) throws Exception {
                // Find and open the diagram
                ProjectAccessor pa = AstahAPI.getAstahAPI().getProjectAccessor();
                IDiagram[] diagrams = pa.getProject().getDiagrams();
                IStateMachineDiagram targetDiagram = null;
                for (IDiagram d : diagrams) {
                    if (d instanceof IStateMachineDiagram && targetName.equals(d.getName())) {
                        targetDiagram = (IStateMachineDiagram) d;
                        break;
                    }
                }

                if (targetDiagram == null) {
                    throw new Exception("Diagram not found: " + targetName);
                }

                // Open diagram and start simulation on UI thread
                final IStateMachineDiagram finalTargetDiagram = targetDiagram;
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            pa.getViewManager().getDiagramViewManager().open(finalTargetDiagram);
                            startSimulation(); // Reset and start on the new diagram
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (java.lang.reflect.InvocationTargetException e) {
                    if (e.getCause() instanceof Exception) {
                        throw (Exception) e.getCause();
                    }
                    throw e;
                }
            }

            @Override
            public void fireEvent(String eventName) throws Exception {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        for (Component comp : eventPanel.getComponents()) {
                            if (comp instanceof JButton && ((JButton) comp).getText().equals(eventName)) {
                                ((JButton) comp).doClick();
                                return;
                            }
                        }
                        throw new RuntimeException("Event button not found: " + eventName);
                    });
                } catch (java.lang.reflect.InvocationTargetException e) {
                    if (e.getCause() instanceof Exception) {
                        throw (Exception) e.getCause();
                    }
                    throw e;
                }
            }

            @Override
            public List<String> getActiveStateNames() {
                return engine.getCurrentVertices().stream().map(IVertex::getName).collect(Collectors.toList());
            }

            @Override
            public List<String> getRecentLogs() {
                return Arrays.asList(logArea.getText().split("\\r?\\n"));
            }
        };
    }

    private void copyDebugInfo() {
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
            IDiagram currentDiagram = viewManager.getCurrentDiagram();

            if (currentDiagram instanceof IStateMachineDiagram) {
                String debugInfo = engine.getDebugInfo((IStateMachineDiagram) currentDiagram);
                String logContent = logArea.getText();

                StringBuilder sb = new StringBuilder();
                sb.append(debugInfo);
                sb.append("\n\n=== Event & Action Log ===\n");
                sb.append(logContent);

                StringSelection selection = new StringSelection(sb.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);

                JOptionPane.showMessageDialog(this, "Debug info copied to clipboard.");
            } else {
                JOptionPane.showMessageDialog(this, "Please open a State Machine Diagram.");
            }
        } catch (Exception e) {
            logArea.append("Error copying debug info: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }
    
    private void updateTestCaseList(IStateMachineDiagram diagram) {
        testCaseCombo.removeAllItems();
        if (diagram != null) {
            List<String> tests = testManager.loadTestCaseNames(diagram.getStateMachine());
            for (String test : tests) {
                testCaseCombo.addItem(test);
            }
        }
    }

    private void refreshUI() {
        if(highlighter != null) {
            highlighter.clearAll();
        }

        int currentIdx = engine.getCurrentSnapshotIndex();
        int historySize = engine.getHistorySize();
        if (historySize > 0) {
            stepLabel.setText("Step: " + (currentIdx + 1) + " / " + historySize);
        } else {
            stepLabel.setText("Step: 0 / 0");
        }
        startNavButton.setEnabled(engine.canStepBack());
        prevNavButton.setEnabled(engine.canStepBack());
        nextNavButton.setEnabled(engine.canStepForward());
        endNavButton.setEnabled(engine.canStepForward());

        List<IVertex> currents = engine.getCurrentVertices();
        if (currents != null && !currents.isEmpty()) {
            StringBuilder sb = new StringBuilder("Current State: ");
            for (int i = 0; i < currents.size(); i++) {
                sb.append(currents.get(i).getName());
                if (i < currents.size() - 1) sb.append(", ");
            }
            stateLabel.setText(sb.toString());

            if(highlighter != null) {
                highlighter.highlight(currents, engine.getPreviousVertices(), engine.getLastTransition());
            }
            eventPanel.removeAll();
            List<SimulationEngine.TransitionPath> paths = engine.getAvailableTransitions();
            if (paths.isEmpty()) {
                eventPanel.add(new JLabel("No events available"));
            } else {
                // Group paths by a generated label to handle Run-to-Completion
                Map<String, List<SimulationEngine.TransitionPath>> eventGroups = new LinkedHashMap<>();
                for (SimulationEngine.TransitionPath path : paths) {
                    String label = getPathLabel(path, paths);
                    eventGroups.computeIfAbsent(label, k -> new ArrayList<>()).add(path);
                }

                Map<ITransition, Color> transitionColors = new HashMap<>();
                int colorIndex = 0;

                for (Map.Entry<String, List<SimulationEngine.TransitionPath>> entry : eventGroups.entrySet()) {
                    String label = entry.getKey();
                    List<SimulationEngine.TransitionPath> groupedPaths = entry.getValue();
                    
                    JButton btn = new JButton(label);
                    
                    // Check if this group is a timer event
                    String eventNameForTimer = groupedPaths.get(0).getRoot().getEvent();
                    long delay = TimerManager.parseDuration(eventNameForTimer);
                    if (delay >= 0 && !engine.isFastMode() && !firedTimers.contains(eventNameForTimer)) {
                        btn.setEnabled(false);
                        btn.setText(label + " (Waiting...)");
                    }

                    Color color = EVENT_COLORS[colorIndex % EVENT_COLORS.length];
                    btn.setBackground(color);
                    btn.setForeground(color.darker().darker().darker());
                    
                    // Highlight all root transitions in this group with the same color
                    for(SimulationEngine.TransitionPath p : groupedPaths) {
                        transitionColors.put(p.getRoot(), color);
                    }
                    colorIndex++;

                    btn.addActionListener(e -> fireTransitions(groupedPaths, label));
                    eventPanel.add(btn);
                }

                if (highlighter != null) {
                    highlighter.highlightAvailableTransitions(transitionColors);
                }
            }
        } else {
            stateLabel.setText("Current State: -");
            eventPanel.removeAll();
        }
        eventPanel.revalidate();
        eventPanel.repaint();
    }

    private String getPathLabel(SimulationEngine.TransitionPath path, List<SimulationEngine.TransitionPath> allPaths) {
        ITransition root = path.getRoot();
        String eventName = root.getEvent();
        String label = (eventName != null && !eventName.trim().isEmpty()) ? eventName.trim() : "(anonymous)";

        // Collect guards from the path
        StringBuilder guards = new StringBuilder();
        for (ITransition t : path.transitions) {
            String g = t.getGuard();
            if (g != null && !g.trim().isEmpty()) {
                if (guards.length() > 0) guards.append(" & ");
                guards.append(g.trim());
            }
        }

        if (guards.length() > 0) {
            label += " [" + guards.toString() + "]";
        } else {
            // Add [else] if another path exists with the same event name but has a guard
            String finalEventName = eventName != null ? eventName.trim() : "";
            boolean hasGuardedSibling = allPaths.stream().anyMatch(p -> 
                p != path && 
                finalEventName.equals(p.getRoot().getEvent() != null ? p.getRoot().getEvent().trim() : "") && 
                p.transitions.stream().anyMatch(t -> t.getGuard() != null && !t.getGuard().trim().isEmpty())
            );
            if(hasGuardedSibling) {
                 label += " [else]";
            }
        }
        return label;
    }

    private void fireTransitions(List<SimulationEngine.TransitionPath> paths, String label) {
        firedTimers.clear();
        try {
            SimulationEngine.StepResult result = engine.step(paths, null);
            if (result == null) return;

            recordStep(label);

            printStepResult(result);
            refreshUI();
        } catch (IllegalSimulationStateException e) {
            handleSimulationException(e);
        }
    }

    private void handleSimulationException(IllegalSimulationStateException e) {
        logArea.append("\n[Error] シミュレーションを停止しました: " + e.getMessage() + "\n");
        
        eventPanel.removeAll();
        eventPanel.revalidate();
        eventPanel.repaint();
        stateLabel.setText("Current State: Error");

        JOptionPane.showMessageDialog(this,
                "【異常検出】シミュレーションを停止しました。\n\n" + e.getMessage(),
                "Simulation Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private void printStepResult(SimulationEngine.StepResult result) {
        if (result == null) return;

        String eventName = (result.path != null) ? result.path.getRoot().getEvent() : "Initial";
        if (eventName == null || eventName.trim().isEmpty()) eventName = "(anonymous)";
        else eventName = eventName.trim();
        if (result.note != null && !result.note.isEmpty()) {
            eventName += " " + result.note;
        }
        logArea.append(String.format("--- Event: %s ---\n", eventName));

        boolean showActions = showActionsCheckbox.isSelected();

        // 1. Source Exit
        if (showActions && result.exitActions != null) {
            for (String exit : result.exitActions) {
                logArea.append("  [Exit] " + exit + "\n");
            }
        }

        // 2. Transition Action
        if (showActions && result.transitionActions != null) {
            for (String action : result.transitionActions) {
                logArea.append("  [Action] " + action + "\n");
            }
        }

        // 3. Target Entry
        if (showActions && result.entryActions != null) {
            for (String entry : result.entryActions) {
                logArea.append("  [Entry] " + entry + "\n");
            }
        }

        // 4. Target Do
        if (showActions && result.doActivity != null && !result.doActivity.isEmpty()) {
            logArea.append("  [Do] " + result.doActivity + "\n");
        }

        if (result.executedPaths != null && !result.executedPaths.isEmpty()) {
            for (SimulationEngine.TransitionPath path : result.executedPaths) {
                String sourceName = path.getSource() != null ? path.getSource().getName() : "Unknown";
                String targetName = path.getTarget() != null ? path.getTarget().getName() : "Internal";
                logArea.append(String.format("Transition: %s -> %s\n",
                        sourceName, targetName));
            }
        } else {
            String sourceName = result.source != null ? result.source.getName() : "Unknown";
            String targetName = result.target != null ? result.target.getName() : "Internal";
            logArea.append(String.format("Transition: %s -> %s\n",
                    sourceName, targetName));
        }
    }

    @Override
    public String getTitle() {
        return "Stm Analysis";
    }

    @Override
    public String getDescription() {
        return "Shows Stm analysis results.";
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void addSelectionListener(ISelectionListener listener) {
    }

    @Override
    public void activated() {
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
            IDiagram currentDiagram = viewManager.getCurrentDiagram();

            if (currentDiagram instanceof IStateMachineDiagram) {
                updateTestCaseList((IStateMachineDiagram) currentDiagram);
            } else {
                testCaseCombo.removeAllItems();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTestCaseListIfStateMachineDiagram(IDiagram diagram) {
        if (diagram instanceof IStateMachineDiagram) {
            updateTestCaseList((IStateMachineDiagram) diagram);
        } else {
            testCaseCombo.removeAllItems();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
            viewManager.addDiagramEditorSelectionListener(diagramEditorSelectionListener);
            viewManager.addEntitySelectionListener(entitySelectionListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeNotify() {
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
            viewManager.removeDiagramEditorSelectionListener(diagramEditorSelectionListener);
            viewManager.removeEntitySelectionListener(entitySelectionListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.removeNotify();
    }

    @Override
    public void deactivated() {
        if(highlighter != null) {
            highlighter.clearAll();
        }
    }
}