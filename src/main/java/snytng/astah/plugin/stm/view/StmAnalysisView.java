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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class StmAnalysisView extends JPanel implements IPluginExtraTabView {

    private JButton startButton;
    private JButton resetButton;
    private JButton copyDebugButton;
    private JLabel stateLabel;
    private JCheckBox showActionsCheckbox;
    private JCheckBox fastModeCheckbox;
    private JPanel eventPanel;
    
    // Test UI
    private JButton recordButton;
    private JButton stopButton;
    private JButton playButton;
    private JButton infoButton;
    private JButton renameButton;
    private JButton deleteButton;
    private JComboBox<String> testCaseCombo;
    
    private JTextArea logArea;

    private final SimulationEngine engine = new SimulationEngine();
    private final TestManager testManager = new TestManager();
    private DiagramHighlighter highlighter;
    
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
                testManager.recordTransition(result.path);
                printStepResult(result);
                refreshUI();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 1. Top Panel (Control & Status)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Start");
        resetButton = new JButton("Reset");
        copyDebugButton = new JButton("Copy Debug");
        stateLabel = new JLabel("Current State: -");
        showActionsCheckbox = new JCheckBox("Show Actions", true);
        fastModeCheckbox = new JCheckBox("Fast Mode", false);

        startButton.addActionListener(e -> startSimulation());
        resetButton.addActionListener(e -> resetSimulation());
        copyDebugButton.addActionListener(e -> copyDebugInfo());
        fastModeCheckbox.addActionListener(e -> engine.setFastMode(fastModeCheckbox.isSelected()));

        topPanel.add(startButton);
        topPanel.add(resetButton);
        topPanel.add(copyDebugButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(stateLabel);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(showActionsCheckbox);
        topPanel.add(fastModeCheckbox);

        // Test Panel
        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        testPanel.setBorder(BorderFactory.createTitledBorder("Test"));
        
        recordButton = new JButton("Record");
        stopButton = new JButton("Stop");
        playButton = new JButton("Play");
        infoButton = new JButton("Info");
        renameButton = new JButton("Rename");
        deleteButton = new JButton("Delete");
        testCaseCombo = new JComboBox<>();
        
        stopButton.setEnabled(false);
        
        recordButton.addActionListener(e -> startRecording());
        stopButton.addActionListener(e -> stopRecording());
        playButton.addActionListener(e -> playTest());
        infoButton.addActionListener(e -> showTestInfo());
        renameButton.addActionListener(e -> renameTest());
        deleteButton.addActionListener(e -> deleteTest());
        
        testPanel.add(recordButton);
        testPanel.add(stopButton);
        testPanel.add(new JLabel("Saved Tests:"));
        testPanel.add(testCaseCombo);
        testPanel.add(playButton);
        testPanel.add(infoButton);
        testPanel.add(renameButton);
        testPanel.add(deleteButton);

        // Combine Top and Test
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(topPanel, BorderLayout.NORTH);
        northPanel.add(testPanel, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);

        // 2. Center Panel (Events)
        eventPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        eventPanel.setBorder(BorderFactory.createTitledBorder("Events"));
        eventPanel.setBackground(Color.WHITE);

        JScrollPane eventScrollPane = new JScrollPane(eventPanel);
        eventScrollPane.setBorder(null);
        add(eventScrollPane, BorderLayout.CENTER);

        // 3. Bottom Panel (Log)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setRows(6);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        add(logScrollPane, BorderLayout.SOUTH);

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

    private void startSimulation() {
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagramViewManager viewManager = projectAccessor.getViewManager().getDiagramViewManager();
            IDiagram currentDiagram = viewManager.getCurrentDiagram();

            if (currentDiagram instanceof IStateMachineDiagram) {
                if(highlighter != null) {
                    highlighter.setDiagram(currentDiagram);
                    highlighter.clearAll();
                }

                SimulationEngine.StepResult result = engine.start((IStateMachineDiagram) currentDiagram);
                logArea.setText("Simulation started.\n");
                printStepResult(result);
                refreshUI();
                updateTestCaseList((IStateMachineDiagram) currentDiagram);
            } else {
                logArea.append("Please open a State Machine Diagram.\n");
            }
        } catch (Exception e) {
            logArea.append("Error starting simulation: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private void resetSimulation() {
        if(highlighter != null) {
            highlighter.clearAll();
        }

        engine.start(null);
        stateLabel.setText("Current State: -");
        eventPanel.removeAll();
        eventPanel.revalidate();
        eventPanel.repaint();
        logArea.setText("Simulation reset.\n");
    }
    
    private void startRecording() {
        testManager.startRecording();
        recordButton.setEnabled(false);
        stopButton.setEnabled(true);
        logArea.append("Recording started...\n");
    }
    
    private void stopRecording() {
        testManager.stopRecording();
        recordButton.setEnabled(true);
        stopButton.setEnabled(false);
        
        String name = JOptionPane.showInputDialog(this, "Enter test case name:");
        if (name != null && !name.trim().isEmpty()) {
            try {
                ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
                IDiagram currentDiagram = projectAccessor.getViewManager().getDiagramViewManager().getCurrentDiagram();
                if (currentDiagram instanceof IStateMachineDiagram) {
                    IStateMachine sm = ((IStateMachineDiagram) currentDiagram).getStateMachine();
                    testManager.saveTestCase(name, sm);
                    logArea.append("Test case '" + name + "' saved to StateMachine '" + sm.getName() + "'.\n");
                    updateTestCaseList((IStateMachineDiagram) currentDiagram);
                }
            } catch (Exception e) {
                logArea.append("Failed to save test case: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
    }
    
    private void playTest() {
        String selectedTest = (String) testCaseCombo.getSelectedItem();
        if (selectedTest == null) return;
        
        resetSimulation();
        startSimulation(); // Start fresh
        
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagram currentDiagram = projectAccessor.getViewManager().getDiagramViewManager().getCurrentDiagram();
            if (currentDiagram instanceof IStateMachineDiagram) {
                logArea.append("Playing test case '" + selectedTest + "'...\n");
                testManager.playTestCase(selectedTest, ((IStateMachineDiagram) currentDiagram).getStateMachine(), engine, this::refreshUI);
                logArea.append("Test case playback finished.\n");
                refreshUI();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showTestInfo() {
        String selectedTest = (String) testCaseCombo.getSelectedItem();
        if (selectedTest == null) return;
        
        try {
            ProjectAccessor projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
            IDiagram currentDiagram = projectAccessor.getViewManager().getDiagramViewManager().getCurrentDiagram();
            if (currentDiagram instanceof IStateMachineDiagram) {
                String info = testManager.getTestCaseInfo(selectedTest, ((IStateMachineDiagram) currentDiagram).getStateMachine());
                logArea.append("--- Test Info ---\n" + info + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                Map<ITransition, Color> transitionColors = new HashMap<>(); // Map root transition to color
                int colorIndex = 0;
                for (SimulationEngine.TransitionPath path : paths) {
                    ITransition root = path.getRoot();
                    boolean hasOtherTransitionsWithSameEvent = false;
                    String eventName = root.getEvent();

                    if (eventName != null) {
                        for (SimulationEngine.TransitionPath other : paths) {
                            if (path != other && eventName.equals(other.getRoot().getEvent())) {
                                hasOtherTransitionsWithSameEvent = true;
                                break;
                            }
                        }
                    }

                    String label = eventName != null ? eventName : "(anonymous)";
                    
                    // Collect guards from the path
                    StringBuilder guards = new StringBuilder();
                    for (ITransition t : path.transitions) {
                        String g = t.getGuard();
                        if (g != null && !g.isEmpty()) {
                            if (guards.length() > 0) guards.append(" & ");
                            guards.append(g);
                        }
                    }
                    
                    if (guards.length() > 0) {
                        label += " [" + guards.toString() + "]";
                    } else if (hasOtherTransitionsWithSameEvent) {
                        label += " [else]"; // Fallback if no explicit guard but multiple paths
                    }

                    if (label == null || label.isEmpty()) {
                       label = "Transition to " + path.getTarget().getName();
                    }

                    JButton btn = new JButton(label);
                    Color color = EVENT_COLORS[colorIndex % EVENT_COLORS.length];
                    btn.setBackground(color);
                    btn.setForeground(color.darker().darker().darker());
                    transitionColors.put(root, color); // Highlight the root transition on diagram
                    colorIndex++;

                    btn.addActionListener(e -> fireTransition(path));
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

    private void fireTransition(SimulationEngine.TransitionPath path) {
        SimulationEngine.StepResult result = engine.step(path, null);
        testManager.recordTransition(path);
        if (result == null) return;

        printStepResult(result);
        refreshUI();
    }

    private void printStepResult(SimulationEngine.StepResult result) {
        if (result == null) return;

        String eventName = (result.path != null) ? result.path.getRoot().getEvent() : "Initial";
        if (eventName == null || eventName.isEmpty()) eventName = "(anonymous)";
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

        logArea.append(String.format("Transition: %s -> %s\n",
                result.source.getName(), result.target.getName()));
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