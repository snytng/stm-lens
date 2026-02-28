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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

public class StmAnalysisView extends JPanel implements IPluginExtraTabView {

    private JButton startButton;
    private JButton resetButton;
    private JLabel stateLabel;
    private JPanel eventPanel;
    private JTextArea logArea;

    private final SimulationEngine engine = new SimulationEngine();
    private DiagramHighlighter highlighter;
    private IVertex previousVertex;
    private ITransition lastTransition;

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

        startButton.addActionListener(e -> startSimulation());
        resetButton.addActionListener(e -> resetSimulation());

        topPanel.add(startButton);
        topPanel.add(resetButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(stateLabel);

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

                engine.start((IStateMachineDiagram) currentDiagram);
                previousVertex = null;
                lastTransition = null;
                logArea.setText("Simulation started.\n");
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
        previousVertex = null;
        lastTransition = null;
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

        IVertex current = engine.getCurrentVertex();
        if (current != null) {
            stateLabel.setText("Current State: " + current.getName());

            if(highlighter != null) {
                highlighter.highlight(current, previousVertex, lastTransition);
            }
            eventPanel.removeAll();
            List<ITransition> transitions = engine.getAvailableTransitions();
            if (transitions.isEmpty()) {
                eventPanel.add(new JLabel("No events available"));
            } else {
                for (ITransition t : transitions) {
                    String label = t.getEvent();
                    if (label == null || label.isEmpty()) {
                        label = "Transition to " + t.getTarget().getName();
                    }
                    JButton btn = new JButton(label);
                    btn.addActionListener(e -> fireTransition(t));
                    eventPanel.add(btn);
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
        IVertex source = engine.getCurrentVertex();

        String eventName = t.getEvent();
        if (eventName == null || eventName.isEmpty()) eventName = "(anonymous)";
        logArea.append(String.format("--- Event: %s ---\n", eventName));

        // 1. Source Exit
        if (source instanceof IState) {
            String exit = ((IState) source).getExit();
            if (exit != null && !exit.isEmpty()) {
                logArea.append("  [Exit] " + exit + "\n");
            }
        }

        // 2. Transition Action
        String action = t.getAction();
        if (action != null && !action.isEmpty()) {
            logArea.append("  [Action] " + action + "\n");
        }

        previousVertex = source;
        lastTransition = t;
        engine.fire(t);
        IVertex target = engine.getCurrentVertex();

        // 3. Target Entry
        if (target instanceof IState) {
            String entry = ((IState) target).getEntry();
            if (entry != null && !entry.isEmpty()) {
                logArea.append("  [Entry] " + entry + "\n");
            }
        }

        // 4. Target Do
        if (target instanceof IState) {
            String doActivity = ((IState) target).getDoActivity();
            if (doActivity != null && !doActivity.isEmpty()) {
                logArea.append("  [Do] " + doActivity + "\n");
            }
        }

        logArea.append(String.format("Transition: %s -> %s\n",
                previousVertex.getName(), target.getName()));

        refreshUI();
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