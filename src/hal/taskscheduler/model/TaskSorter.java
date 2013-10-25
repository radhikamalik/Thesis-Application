package hal.taskscheduler.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;


public abstract class TaskSorter implements Comparator<String> {
	Map<String, Task> allTasks;
	List<RiskCategory> constrainedCategory;
	Map<String, List<Integer>> possibleWorkersForTasks;
	
	public TaskSorter(Map<String, Task> allTasks,
			List<RiskCategory> constrainedCategory, Map<String, List<Integer>> possibleWorkersForTasks) {
		
		this.allTasks = allTasks;
		this.constrainedCategory = constrainedCategory;
		this.possibleWorkersForTasks = possibleWorkersForTasks;
	}

}
