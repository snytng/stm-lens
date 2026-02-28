package snytng.astah.plugin.stm.view;

import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;

public class StmAnalysisView extends JPanel implements IPluginExtraTabView {

    public StmAnalysisView() {
        setLayout(new BorderLayout());
        add(new JTextField("Hello"), BorderLayout.CENTER);
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