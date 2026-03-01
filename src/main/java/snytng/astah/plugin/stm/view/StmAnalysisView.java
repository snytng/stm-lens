package snytng.astah.plugin.stm.view;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;
import snytng.astah.plugin.stm.model.DiagramHighlighter;
import snytng.astah.plugin.stm.model.SimulationEngine;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class StmAnalysisView extends JPanel implements IPluginExtraTabView {

    private JButton startButton;
    private JButton resetButton;
    private JLabel stateLabel;
    private JCheckBox showActionsCheckbox;
    private JPanel eventPanel;
    private JTextArea logArea;

    private final SimulationEngine engine = new SimulationEngine();
    private DiagramHighlighter highlighter;

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
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 1. Top Panel (Control & Status)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Start");
        resetButton = new JButton("Reset");
        stateLabel = new JLabel("Current State: -");
        showActionsCheckbox = new JCheckBox("Show Actions", true);

        startButton.addActionListener(e -> startSimulation());
        resetButton.addActionListener(e -> resetSimulation());

        topPanel.add(startButton);
        topPanel.add(resetButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(stateLabel);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(showActionsCheckbox);

        add(topPanel, BorderLayout.NORTH);

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
            List<ITransition> transitions = engine.getAvailableTransitions();
            if (transitions.isEmpty()) {
                eventPanel.add(new JLabel("No events available"));
            } else {
                Map<ITransition, Color> transitionColors = new HashMap<>();
                int colorIndex = 0;
                for (ITransition t : transitions) {
                    boolean hasOtherTransitionsWithSameEvent = false;
                    String eventName = t.getEvent();

                    if (eventName != null) {
                        for (ITransition other : transitions) {
                            if (t != other && eventName.equals(other.getEvent())) {
                                hasOtherTransitionsWithSameEvent = true;
                                break;
                            }
                        }
                    }

                    String label = t.getEvent() != null ? t.getEvent() : "(anonymous)";
                    String guard = t.getGuard();
                    if (guard != null && !guard.isEmpty()) {
                        label += " [" + guard + "]";
                    } else if (hasOtherTransitionsWithSameEvent) {
                        //ガードがないときのelseを表現
                        label += " [else]";
                    }

                    if (label == null || label.isEmpty()) {
                       label = "Transition to " + t.getTarget().getName();
                    }

                    JButton btn = new JButton(label);
                    Color color = EVENT_COLORS[colorIndex % EVENT_COLORS.length];
                    btn.setBackground(color);
                    btn.setForeground(color.darker().darker().darker());
                    transitionColors.put(t, color);
                    colorIndex++;

                    btn.addActionListener(e -> fireTransition(t));
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

    private void fireTransition(ITransition t) {
        SimulationEngine.StepResult result = engine.step(t);
        if (result == null) return;

        printStepResult(result);
        refreshUI();
    }

    private void printStepResult(SimulationEngine.StepResult result) {
        if (result == null) return;

        String eventName = (result.transition != null) ? result.transition.getEvent() : "Initial";
        if (eventName == null || eventName.isEmpty()) eventName = "(anonymous)";
        logArea.append(String.format("--- Event: %s ---\n", eventName));

        boolean showActions = showActionsCheckbox.isSelected();

        // 1. Source Exit
        if (showActions && result.exitActions != null) {
            for (String exit : result.exitActions) {
                logArea.append("  [Exit] " + exit + "\n");
            }
        }

        // 2. Transition Action
        if (showActions && result.transitionAction != null && !result.transitionAction.isEmpty()) {
            logArea.append("  [Action] " + result.transitionAction + "\n");
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
    }

    @Override
    public void deactivated() {
        if(highlighter != null) {
            highlighter.clearAll();
        }
    }
}