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
}