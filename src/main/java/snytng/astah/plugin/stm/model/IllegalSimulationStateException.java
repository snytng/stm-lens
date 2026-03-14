package snytng.astah.plugin.stm.model;

/**
 * Represents an exception thrown when the simulation engine detects an inconsistent or illegal state.
 * <p>
 * This indicates a bug in the simulation logic or a corrupted model state that prevents further safe execution.
 * For example, finding multiple active states in a non-parallel region.
 * </p>
 */
public class IllegalSimulationStateException extends RuntimeException {

    /**
     * Constructs a new IllegalSimulationStateException with the specified detail message.
     * @param message the detail message.
     */
    public IllegalSimulationStateException(String message) {
        super(message);
    }

}