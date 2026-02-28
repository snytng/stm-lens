package snytng.astah.plugin.stm.view;

import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;

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

public class StmAnalysisView extends JPanel implements IPluginExtraTabView {

    private JButton startButton;
    private JButton resetButton;
    private JLabel stateLabel;
    private JPanel eventPanel;
    private JTextArea logArea;

    public StmAnalysisView() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 1. Top Panel (Control & Status)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Start");
        resetButton = new JButton("Reset");
        stateLabel = new JLabel("Current State: -");

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
    }
}