package snytng.astah.plugin.stm.model;

import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;
import java.util.Collections;
import java.util.List;

public class SimulationEngine {

    private IVertex currentVertex;

    public void start(IStateMachineDiagram diagram) {
        // TODO: 初期状態の探索と設定
    }

    public List<ITransition> getAvailableTransitions() {
        if (currentVertex == null) {
            return Collections.emptyList();
        }
        // TODO: 遷移可能なトランジションの取得
        return Collections.emptyList();
    }

    public void fire(ITransition transition) {
        // TODO: 状態遷移の実行
        // currentVertex = transition.getTarget();
    }

    public IVertex getCurrentVertex() {
        return currentVertex;
    }
}