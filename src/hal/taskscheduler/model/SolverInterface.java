package hal.taskscheduler.model;

import java.util.List;
import java.util.Map;

/**
 * This represents the interface that a solver class must implement.
 * The public methods in this interface are the only ones that should be 
 * exposed to the Allocator.
 * 
 * @author RadhikaMalik
 *
 */
public interface SolverInterface {
	public Map<String, List<Integer>> solve();
	public Map<String, List<Integer>> getCurrentBestAssignment();
}
