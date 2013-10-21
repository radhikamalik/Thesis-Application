
package hal.taskscheduler;



import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TaskSorter implements Comparator<String> {

	Map<String, Task> allTasks;
	List<RiskCategory> constrainedCategory;
	Map<String, List<Integer>> possibleWorkersForTasks;

	public TaskSorter(Map<String, Task> allTasks,
			List<RiskCategory> constrainedCategory, Map<String, List<Integer>> possibleWorkersForTasks) {
		
		this.allTasks = allTasks;
		this.constrainedCategory = constrainedCategory;
		this.possibleWorkersForTasks = possibleWorkersForTasks;
	}

	@Override
	public int compare(String tid1, String tid2) {

		Task t1 = allTasks.get(tid1);
		Task t2 = allTasks.get(tid2);

		System.out.println(tid1+ ":" + t1.getDependencies());
		System.out.println(tid2 + ":" + t2.getDependencies());
		if (t1.getDependencies().contains(tid2)){ //t1 depends on t2, thus t1 must be assigned later than t2 -> sorting must place t1 after
			System.out.println("DEPENDENCY!" + tid1 + ";" + tid2);
			return 1;
		}
		if (t2.getDependencies().contains(tid1)){ //opp of above case 
			System.out.println("DEPENDENCY!" + tid1 + ";" + tid2);
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
		/*
		if (t1_p != t2_p)
			return t1_p - t2_p;
		else {
			
			int t1_overlap = t1.getOverlappingTasks().size();
			int t2_overlap = t2.getOverlappingTasks().size();
			return t1_overlap - t2_overlap;
			
			/*
			// order on ergonomic risk
			double maxRisk_1 = t1.getMaxRiskFromCategories(constrainedCategory);
			double maxRisk_2 = t2.getMaxRiskFromCategories(constrainedCategory);
			int ergoDiff = (int) Math.round(maxRisk_1 - maxRisk_2);

			if (ergoDiff != 0)
				return ergoDiff;
			else {

				//order them based on number of workers they require
				int t1_w = t1.getWorkerReq();
				int t2_w = t2.getWorkerReq();
				return t1_w - t2_w;
			}
		
		}*/

	}

}
