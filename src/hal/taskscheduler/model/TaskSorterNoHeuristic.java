package hal.taskscheduler.model;


import java.util.List;
import java.util.Map;
/**
 * Comparator that sorts only based on dependency and priority and no other heuristic.
 * @author RadhikaMalik
 *
 */
public class TaskSorterNoHeuristic extends TaskSorter {

	public TaskSorterNoHeuristic(Map<String, Task> allTasks,
			List<RiskCategory> constrainedCategory,
			Map<String, List<Integer>> possibleWorkersForTasks) {
		super(allTasks, constrainedCategory, possibleWorkersForTasks);
	}

	@Override
	public int compare(String tid1, String tid2) {

		Task t1 = allTasks.get(tid1);
		Task t2 = allTasks.get(tid2);

		if (t1.getDependencies().contains(tid2)){ //t1 depends on t2, thus t1 must be assigned later than t2 -> sorting must place t1 after
			return 1;
		}
		if (t2.getDependencies().contains(tid1)){ //opp of above case 
			return -1;
			
		}
		
		//neither dependent on the other
				
		//priority heuristic
		int t1_p = t1.getPriority();
		int t2_p = t2.getPriority();
		
		//higher priority should be assigned first
		
		if (t1_p > t2_p)
			return -1;
		if (t2_p > t1_p)
			return 1;
		return 0;

	}

}
