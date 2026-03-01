package snytng.astah.plugin.stm.model;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;
import java.awt.Color;
import java.util.List;

public class DiagramHighlighter {

    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 153); // Light Yellow

    private IDiagram diagram;
    private IDiagramViewManager viewManager;

    public DiagramHighlighter() throws ClassNotFoundException {
        try {
            ProjectAccessor pa = AstahAPI.getAstahAPI().getProjectAccessor();
            this.viewManager = pa.getViewManager().getDiagramViewManager();
        } catch (Exception e) {
            System.err.println("Failed to initialize DiagramHighlighter: " + e.getMessage());
            this.viewManager = null;
        }
    }

    public void setDiagram(IDiagram diagram) {
        this.diagram = diagram;
    }

    public void highlight(List<IVertex> currentVertices, IVertex previous, ITransition transition) {
        if (viewManager == null || diagram == null) {
            return;
        }
        try {
            for (IPresentation p : diagram.getPresentations()) {
                Object model = p.getModel();
                if (model == null) continue;

                if (currentVertices != null && currentVertices.contains(model)) {
                    viewManager.setViewProperty(p, IDiagramViewManager.BACKGROUND_COLOR, HIGHLIGHT_COLOR);
                    viewManager.setViewProperty(p, IDiagramViewManager.BORDER_COLOR, Color.GREEN);
                } else if (previous != null && previous.equals(model)) {
                    viewManager.setViewProperty(p, IDiagramViewManager.BORDER_COLOR, Color.MAGENTA);
                } else if (transition != null && transition.equals(model)) {
                    viewManager.setViewProperty(p, IDiagramViewManager.LINE_COLOR, Color.MAGENTA);
                }
            }
        } catch (InvalidUsingException e) {
            System.err.println("Failed to highlight: " + e.getMessage());
        }
    }

    public void clearAll() {
        if (viewManager != null && diagram != null) {
            try {
                viewManager.clearAllViewProperties(diagram);
            } catch (InvalidUsingException e) {
                System.err.println("Failed to clear highlights: " + e.getMessage());
            }
        }
    }
}