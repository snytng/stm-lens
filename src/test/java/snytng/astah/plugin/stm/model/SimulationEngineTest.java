package snytng.astah.plugin.stm.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.change_vision.jude.api.inf.model.IPseudostate;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.IStateMachine;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.model.IVertex;

@ExtendWith(MockitoExtension.class)
public class SimulationEngineTest {

    @Mock
    IStateMachineDiagram diagram;
    @Mock
    IStateMachine stateMachine;
    @Mock
    IPseudostate initialPseudostate;
    @Mock
    IState stateA;
    @Mock
    ITransition transition;

    @Test
    public void testStart_FindsInitialStateAndTransitionsToNext() {
        // --- Setup (Given) ---
        // 1. Diagram has a StateMachine
        when(diagram.getStateMachine()).thenReturn(stateMachine);

        // 2. StateMachine has vertices (Initial and StateA)
        IVertex[] vertices = new IVertex[] { initialPseudostate, stateA };
        when(stateMachine.getVertexes()).thenReturn(vertices);

        // 3. InitialPseudostate setup
        when(initialPseudostate.isInitialPseudostate()).thenReturn(true);
        when(initialPseudostate.getOutgoings()).thenReturn(new ITransition[] { transition });

        // 4. Transition setup (Initial -> StateA)
        when(transition.getTarget()).thenReturn(stateA);
        
        // 5. StateA setup
        // (Optional) If StateA has entry actions, mock getEntry() here.
        // when(stateA.getEntry()).thenReturn("entry action");

        // --- Execute (When) ---
        SimulationEngine engine = new SimulationEngine();
        SimulationEngine.StepResult result = engine.start(diagram);

        // --- Verify (Then) ---
        assertNotNull(result, "Start result should not be null");
        assertEquals(initialPseudostate, result.source, "Source should be Initial");
        assertEquals(stateA, result.target, "Target should be StateA");
        
        assertEquals(1, engine.getCurrentVertices().size());
        assertEquals(stateA, engine.getCurrentVertices().get(0), "Current vertex should be StateA");
    }

    @Nested
    @DisplayName("N6: 異常状態の検出")
    class AbnormalStateDetectionTest {

        @Mock
        IState stateA;
        @Mock
        IState stateB;
        @Mock
        IState containerState;

        private Method validateMethod;
        private SimulationEngine engine;

        @BeforeEach
        void setUp() throws NoSuchMethodException {
            engine = new SimulationEngine();
            // privateメソッドにアクセスするためのリフレクション設定
            validateMethod = SimulationEngine.class.getDeclaredMethod("validateCurrentStates");
            validateMethod.setAccessible(true);
        }

        @Test
        @DisplayName("非並行状態でカレントステートが複数存在する場合に例外をスローする")
        void validate_shouldThrowException_whenMultipleStatesInNonParallelRegion() {
            // 状態Aと状態Bが同じ非並行状態(containerState)に属しているとモックする
            when(stateA.getContainer()).thenReturn(containerState);
            when(stateB.getContainer()).thenReturn(containerState);
            when(containerState.getSubvertexes()).thenReturn(null); // 非並行状態であることをシミュレート

            engine.getCurrentVertices().add(stateA);
            engine.getCurrentVertices().add(stateB);

            // privateメソッドをリフレクション経由で呼び出すとInvocationTargetExceptionにラップされる
            InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> {
                validateMethod.invoke(engine);
            });

            assertInstanceOf(IllegalSimulationStateException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("Multiple active states"));
        }

        @Test
        @DisplayName("カレントステートが喪失した場合に例外をスローする")
        void validate_shouldThrowException_whenCurrentStatesAreLost() {
            engine.getCurrentVertices().clear(); // カレントステートなし

            InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> {
                validateMethod.invoke(engine);
            });

            assertInstanceOf(IllegalSimulationStateException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("Current active state is lost"));
        }

        @Test
        @DisplayName("正常な状態では例外をスローしない")
        void validate_shouldNotThrowException_forValidStates() {
            // nullを返すとgroupingByでNPEになるため、コンテナをモックする
            when(stateA.getContainer()).thenReturn(containerState);
            engine.getCurrentVertices().add(stateA); // 単一のカレントステート

            assertDoesNotThrow(() -> {
                validateMethod.invoke(engine);
            });
        }
    }

    @Nested
    @DisplayName("N8: タイムトラベルデバッグ機能")
    class TimeTravelTest {

        @Mock IStateMachineDiagram diagram2;
        @Mock IStateMachine sm2;
        @Mock IPseudostate initial2;
        @Mock IState stateA2;
        @Mock IState stateB2;
        @Mock IState stateC2;
        @Mock ITransition t1;
        @Mock ITransition t2;

        @BeforeEach
        void setupModel() {
            lenient().when(diagram2.getStateMachine()).thenReturn(sm2);
            lenient().when(sm2.getVertexes()).thenReturn(new IVertex[]{initial2, stateA2, stateB2, stateC2});
            lenient().when(initial2.isInitialPseudostate()).thenReturn(true);
            lenient().when(initial2.getOutgoings()).thenReturn(new ITransition[]{t1});
            lenient().when(t1.getTarget()).thenReturn(stateA2);
            lenient().when(t1.getSource()).thenReturn(initial2);

            lenient().when(t2.getSource()).thenReturn(stateA2);
            lenient().when(t2.getTarget()).thenReturn(stateB2);
        }

        @Test
        @DisplayName("step実行時にスナップショットが保存され、指定インデックスの状態に復元できること")
        void testSaveAndRestoreSnapshot() {
            SimulationEngine engine = new SimulationEngine();
            engine.start(diagram2);

            assertEquals(1, engine.getHistorySize());
            assertEquals(0, engine.getCurrentSnapshotIndex());
            assertTrue(engine.getCurrentVertices().contains(stateA2));

            // step to StateB
            SimulationEngine.TransitionPath pathB = new SimulationEngine.TransitionPath(java.util.Collections.singletonList(t2));
            engine.step(pathB, null);

            assertEquals(2, engine.getHistorySize());
            assertEquals(1, engine.getCurrentSnapshotIndex());
            assertTrue(engine.getCurrentVertices().contains(stateB2));

            // Step back to StateA
            assertTrue(engine.canStepBack());
            engine.stepBack();
            assertEquals(0, engine.getCurrentSnapshotIndex());
            assertTrue(engine.getCurrentVertices().contains(stateA2));

            // Step forward to StateB
            assertTrue(engine.canStepForward());
            engine.stepForward();
            assertEquals(1, engine.getCurrentSnapshotIndex());
            assertTrue(engine.getCurrentVertices().contains(stateB2));
        }

        @Test
        @DisplayName("過去に戻った状態でstepを実行すると、未来の履歴が破棄されて新しい履歴が分岐すること")
        void testHistoryBranching() {
            SimulationEngine engine = new SimulationEngine();
            engine.start(diagram2); // index 0: stateA2

            SimulationEngine.TransitionPath pathB = new SimulationEngine.TransitionPath(java.util.Collections.singletonList(t2));
            engine.step(pathB, null); // index 1: stateB2

            engine.stepBack(); // back to index 0

            // Step to StateC instead (branching)
            ITransition t3 = mock(ITransition.class);
            when(t3.getSource()).thenReturn(stateA2);
            when(t3.getTarget()).thenReturn(stateC2);
            SimulationEngine.TransitionPath pathC = new SimulationEngine.TransitionPath(java.util.Collections.singletonList(t3));
            engine.step(pathC, null); // index 1 is now stateC2

            assertEquals(2, engine.getHistorySize());
            assertEquals(1, engine.getCurrentSnapshotIndex());
            assertTrue(engine.getCurrentVertices().contains(stateC2));
        }
    }
}