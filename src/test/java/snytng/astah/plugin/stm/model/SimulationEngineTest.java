package snytng.astah.plugin.stm.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class SimulationEngineTest {

    @Test
    public void testEnvironment() {
        SimulationEngine engine = new SimulationEngine();
        assertNotNull(engine);
        assertTrue(engine.getCurrentVertices().isEmpty());
    }
}