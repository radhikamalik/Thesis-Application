package hal.taskscheduler.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;



public abstract class Solver implements SolverInterface{
	protected Map<String, List<Integer>> currentBestAssignment;
	protected int currentBestAssignmentTasksRemaining; // number of tasks understaffed in
												// current best assignment
	protected List<String> understaffedTasks;
	protected Map<String, List<Integer>> initialAssignmentTaskToWorker;
	protected Map<String, List<Integer>> possibleWorkersForTasks; // for each task domain
														// of workers it can be
														// assigned to

	protected Map<String, Task> allTasks;
	protected Map<Integer, Worker> allWorkers;
	protected List<RiskCategory> constrainedCategories;
	protected Map<Integer, Worker> workersSpare;
	protected Map<Integer, Map<RiskCategory, Double>> workerErgoRisk;
	protected Map<Integer, List<String>> initialAssignmentWorkerToTask;

	
	protected void populateInitialWorkerDomain() {

		for (String tId : allTasks.keySet()) {
			Task t = allTasks.get(tId);
			List<Integer> workersForTask = new ArrayList<Integer>();
			for (int workerId : allWorkers.keySet()) {

				if (t.getCertifiedWorkers().contains(workerId)) { // worker is
																	// certified
																	
					Worker w = this.allWorkers.get(workerId);

					List<Integer> workersAlreadyAssigned = this.initialAssignmentTaskToWorker
							.get(tId); // workers initially assigned
					if (w.isAvailableForTask(t)
							&& !workersAlreadyAssigned.contains(workerId)) { 
						// worker not already assigned so add it
						workersForTask.add(workerId);
					}
				}

			}
			if (this.workersSpare != null) { // can plan with spares too so add
												// them at the end
				for (int workerId : workersSpare.keySet()) {

					if (t.getCertifiedWorkers().contains(workerId)) { 
						//worker is certified!
						Worker w = this.workersSpare.get(workerId);

						List<Integer> workersAlreadyAssigned = this.initialAssignmentTaskToWorker
								.get(tId); // workers initially assigned
						if (w.isAvailableForTask(t)
								&& !workersAlreadyAssigned.contains(workerId)) { 
							workersForTask.add(workerId);
						}
					}

				}
			}
			// edit map of worker domain for given task
			this.possibleWorkersForTasks.put(tId, workersForTask); 

		}
	}
	
	/**
	 * function to check if the new assignment is consistent with current
	 * assignment
	 * @param currentSol- current assignments
	 * @param tid- new taskid being assigned
	 * @param wId- new workerid being assigned
	 * @param currentWorkerErgoRisk- worker ergo risk map associated with current assignment
	 * @param currentSolWorkerToTask - current reverse mapping from workers to tasks
	 * @return
	 */
	protected boolean isConsistent(Map<String, List<Integer>> currentSol,
			String tid, int wId,
			Map<Integer, Map<RiskCategory, Double>> currentWorkerErgoRisk, Map<Integer, List<String>> currentSolWorkerToTask) {

		Task task = allTasks.get(tid);

		Set<String> overlapIds = task.getOverlappingTasks();

		// System.out.println("checking consistency of task: " + tid +
		// "with workers:"+ workerIds);

		Worker w = this.allWorkers.get(wId);
		// availability constraints
		// the new task must be during the worker's availability
		// since initial task domain always must have worker be available,
		// assert this!
		assert (w.isAvailableForTask(task));

		// constraint 1: no worker can work on 2 tasks at the same time
		// for all overlapping tasks for current task, ensure worker is not
		// assigned to any of these tasks
		for (String overlapId : overlapIds) {
			List<Integer> workersInOverlaps = currentSol.get(overlapId);
			if (workersInOverlaps.contains(wId)) // an overlapping task contains
													// the same worker
				return false;
		}
		// ergonomic risk threshold constraint

		Map<RiskCategory, Double> taskErgoRiskMap = task
				.getErgoRiskAllCategories();
		Map<RiskCategory, Double> workerErgoRiskMap = currentWorkerErgoRisk
				.get(wId);

		// ergo risk is not satisfied
		for (RiskCategory c : this.constrainedCategories) {
			Double tRisk = taskErgoRiskMap.get(c);
			Double wRisk = workerErgoRiskMap.get(c);

			if (tRisk == null)
				continue;
			else if (tRisk > 1){
				//task risk more than 1 so check if worker is performing any task with ergo risk in this cat
				List<String> tasksForWorker = currentSolWorkerToTask.get(wId);
				for (String priorTask : tasksForWorker){
					Task other = this.allTasks.get(priorTask);
					Map<RiskCategory,Double> otherTaskErgoMap = other.getErgoRiskAllCategories();
					Double otherTRisk = otherTaskErgoMap.get(c);
					if (otherTRisk != null){
						if (otherTRisk > 0)
							return false;
					}
				}
			}
			else {
				Double cumRisk = tRisk + wRisk;
				if (cumRisk > 1)
					return false;
			}

		}

		return true;
	}
	@Override
	public Map<String, List<Integer>> getCurrentBestAssignment() {
		return this.currentBestAssignment;
	}
	/**
	 * For a given task, return the possible workers the task can be assigned to
	 * @param currentSol
	 * @param tId
	 * @param possibleWorkersForTasks
	 * @return
	 */
	protected List<Integer> getWorkerIdsForTask(
			Map<String, List<Integer>> currentSol, String tId,
			Map<String, List<Integer>> possibleWorkersForTasks) {

		List<Integer> initiallyPopulated = possibleWorkersForTasks.get(tId);
		return initiallyPopulated;
	}
	protected void removeTaskErgoRiskFromWorker(
			Map<Integer, Map<RiskCategory, Double>> currentWorkerErgoRisk,
			Task t, int workerIdToAssign) {
		Map<RiskCategory, Double> workerErgoMap = currentWorkerErgoRisk
				.get(workerIdToAssign);
		Map<RiskCategory, Double> taskErgoMap = t.getErgoRiskAllCategories();
		for (RiskCategory c : this.constrainedCategories) {
			Double wRisk = workerErgoMap.get(c);
			Double tRisk = taskErgoMap.get(c);

			assert (wRisk != null); // this should have always been initialized
									// to 0
			if (tRisk != null) {
				wRisk -= tRisk;
			}
		}
	}
	protected void addTaskErgoRiskToWorker(
			Map<Integer, Map<RiskCategory, Double>> currentWorkerErgoRisk,
			Task t, int workerIdToAssign) {
		Map<RiskCategory, Double> workerErgoMap = currentWorkerErgoRisk
				.get(workerIdToAssign);
		Map<RiskCategory, Double> taskErgoMap = t.getErgoRiskAllCategories();
		for (RiskCategory c : this.constrainedCategories) {
			Double wRisk = workerErgoMap.get(c);
			Double tRisk = taskErgoMap.get(c);

			assert (wRisk != null); // this should have always been initialized
									// to 0
			if (tRisk != null) {
				wRisk += tRisk;
			}
		}

	}
}
