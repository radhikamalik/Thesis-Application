package hal.taskscheduler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class represents a solver that does not implement any heuristics.
 * 
 * @author RadhikaMalik
 *
 */
public class SolverMRVHeuristic implements SolverInterface{

	Map<String, List<Integer>> currentBestAssignment;
	int currentBestAssignmentTasksRemaining; // number of tasks understaffed in
												// current best assignment
	List<String> understaffedTasks;
	Map<String, List<Integer>> initialAssignmentTaskToWorker;
	Map<String, List<Integer>> possibleWorkersForTasks; // for each task domain
														// of workers it can be
														// assigned to

	Map<String, Task> allTasks;
	Map<Integer, Worker> allWorkers;
	List<RiskCategory> constrainedCategories;
	private Map<Integer, Worker> workersSpare;
	private Map<Integer, Map<RiskCategory, Double>> workerErgoRisk;
	private Map<Integer, List<String>> initialAssignmentWorkerToTask;
	
	/**
	 * Initialize a solver
	 * 
	 * @param allTasks: all tasks.
	 * @param initialAssignmentTaskToWorker : initial assignment that needs completion
	 * @param allWorkers: scheduled workers
	 * @param workersSpare: spare workers
	 * @param constrainedCategories: categories that cannot take high risk workers
	 */
	public SolverMRVHeuristic(Map<String, Task> allTasks,
			Map<String, List<Integer>> initialAssignmentTaskToWorker,
			Map<Integer, Worker> allWorkers, Map<Integer, Worker> workersSpare,
			List<RiskCategory> constrainedCategories) {
		this.currentBestAssignment = new HashMap<String, List<Integer>>();
		this.understaffedTasks = new ArrayList<String>();
		this.possibleWorkersForTasks = new HashMap<String, List<Integer>>();
		this.initialAssignmentTaskToWorker = initialAssignmentTaskToWorker;
		this.allTasks = allTasks;
		this.allWorkers = allWorkers;

		this.workersSpare = workersSpare;
		this.workerErgoRisk = new HashMap<Integer, Map<RiskCategory, Double>>();

		this.initialAssignmentWorkerToTask = new HashMap<Integer, List<String>> ();
				
		//initialize the reverse mapping of workers to tasks for all workers and then populate using
		// task to worker mappings
		
		//TODO: make new field of all workers to plan with to stop repeating for all and spare?

		for (Integer worker : this.allWorkers.keySet()) {
			
			List<String> tasksAssigned = new ArrayList<String>();
			this.initialAssignmentWorkerToTask.put(worker, tasksAssigned);
		}
		if(this.workersSpare != null){
			for (Integer worker : this.workersSpare.keySet()) {
				List<String> tasksAssigned = new ArrayList<String>();
				this.initialAssignmentWorkerToTask.put(worker, tasksAssigned);
			}
		}
		// populate!
		for (String tid:this.initialAssignmentTaskToWorker.keySet()){
			List<Integer> workersAssigned = this.initialAssignmentTaskToWorker.get(tid);
			for (Integer worker:workersAssigned){
				this.initialAssignmentWorkerToTask.get(worker).add(tid);
			}
		}
		
		// find all understaffed tasks that need more workers
		for (String tid : allTasks.keySet()) {
			Task t = allTasks.get(tid);
			if (!initialAssignmentTaskToWorker.containsKey(tid))
				understaffedTasks.add(tid);
			else {
				int workerReq = t.getWorkerReq();
				int workersAssigned = initialAssignmentTaskToWorker.get(tid)
						.size();
				if (workerReq > workersAssigned)
					understaffedTasks.add(tid);
			}
		}
		currentBestAssignmentTasksRemaining = understaffedTasks.size();
		this.constrainedCategories = constrainedCategories;
		
		// populate the ergo risk map for all workers
		for (Integer worker : this.allWorkers.keySet()) {
			Map<RiskCategory, Double> workerErgoMap = new HashMap<RiskCategory, Double>();
			for (RiskCategory c : this.constrainedCategories) {
				workerErgoMap.put(c, (double) 0);
			}
			workerErgoRisk.put(worker, workerErgoMap);
		}
		if(this.workersSpare != null){
			for (Integer worker : this.workersSpare.keySet()) {
				Map<RiskCategory, Double> workerErgoMap = new HashMap<RiskCategory, Double>();
				for (RiskCategory c : this.constrainedCategories) {
					workerErgoMap.put(c, (double) 0);
				}
				workerErgoRisk.put(worker, workerErgoMap);
			}
		}
		
		// populate initial worker domain for all tasks
		populateInitialWorkerDomain();
		
		// sort unassigned tasks based on heuristic 
		Collections.sort(understaffedTasks, new TaskSorterMRVHeuristic(this.allTasks,
				this.constrainedCategories, this.possibleWorkersForTasks)); 
		System.out.println(understaffedTasks);																

		System.out.println(this.initialAssignmentWorkerToTask);

	}

	private void populateInitialWorkerDomain() {

		for (String tId : allTasks.keySet()) {
			Task t = allTasks.get(tId);
			List<Integer> workersForTask = new ArrayList<Integer>();
			for (int workerId : allWorkers.keySet()) {

				if (t.getCertifiedWorkers().contains(workerId)) { 
					//worker certified!
					Worker w = this.allWorkers.get(workerId);

					List<Integer> workersAlreadyAssigned = this.initialAssignmentTaskToWorker
							.get(tId); // workers initially assigned
					if (w.isAvailableForTask(t)
							&& !workersAlreadyAssigned.contains(workerId)) { 
						// worker available!
						workersForTask.add(workerId);
					}
				}

			}
			if (this.workersSpare != null) { // can plan with spares too so add
												// them at the end
				for (int workerId : workersSpare.keySet()) {

					if (t.getCertifiedWorkers().contains(workerId)) { 
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
			// add to task->worker domain map
			this.possibleWorkersForTasks.put(tId, workersForTask);

		}
	}

	/**
	 * For each task populate the workers who are skilled to perform given task
	 */
	/*
	 * public void populateSkilledWorkers(){
	 * 
	 * for (String tid: this.allTasks.keySet()){ Task t =
	 * this.allTasks.get(tid); List<Integer> certifiedWorkers =
	 * t.getCertifiedWorkers(); for (int workerId: certifiedWorkers){ Worker w =
	 * this.allWorkers.get(workerId); if (w.isAvailableForTask(t))
	 * 
	 * } //System.out.println("task: " + tid + " skilled workers: " +
	 * t.getCertifiedWorkers()); }
	 * 
	 * 
	 * }
	 */
	@Override
	public Map<String, List<Integer>> solve() {
		// this.numCalls = 0;
		/*
		 * try{
		 * 
		 * } catch (NoSolutionFoundException e){ return null; }
		 */
		return recursiveSolve(this.initialAssignmentTaskToWorker,
				this.understaffedTasks, this.possibleWorkersForTasks,
				this.workerErgoRisk, this.initialAssignmentWorkerToTask);
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
	private boolean isConsistent(Map<String, List<Integer>> currentSol,
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

	private Map<String, List<Integer>> recursiveSolve(
			Map<String, List<Integer>> currentSol,
			List<String> unassignedTasks,
			Map<String, List<Integer>> possibleWorkersForTasks,
			Map<Integer, Map<RiskCategory, Double>> currentWorkerErgoRisk,
			Map<Integer,List<String>>currentSolInvertMap) {

		if (unassignedTasks.size() == 0)
			return currentSol;

		String tId = unassignedTasks.get(0);

		Task t = this.allTasks.get(tId);
		List<Integer> workerIdsAssigned = currentSol.get(tId); // workers currently assigned
		
		// workers who can be assigned to this task
		List<Integer> possibleWorkerIds = getWorkerIdsForTask(currentSol, tId,
				possibleWorkersForTasks);
		int possibleNumOfWorkers = possibleWorkerIds.size();
		
		List<Integer> currentDomain = possibleWorkersForTasks.get(tId);
		List<Integer> newDomain = new ArrayList<Integer>(currentDomain);

		for (int i = 0; i < possibleNumOfWorkers; ++i) {
			int workerIdToAssign = possibleWorkerIds.get(i);
			
			List<String> tasksForWorker = currentSolInvertMap.get(workerIdToAssign);
			

			if (this.isConsistent(currentSol, tId, workerIdToAssign,
					currentWorkerErgoRisk,currentSolInvertMap)) {
				List<Integer> updatedWorkersAssigned = new ArrayList<Integer>(
						workerIdsAssigned);
				updatedWorkersAssigned.add(workerIdToAssign);
				currentSol.put(tId, updatedWorkersAssigned);
				this.addTaskErgoRiskToWorker(currentWorkerErgoRisk, t,
						workerIdToAssign);
				
				
				List<String> updatedTasksForWorker = new ArrayList<String>(tasksForWorker);
				updatedTasksForWorker.add(tId);
				currentSolInvertMap.put(workerIdToAssign, updatedTasksForWorker);

				// update task domain for this task and overlaps and reorder tasks

				Map<String, List<Integer>> workersForTasksUpdated = new HashMap<String, List<Integer>>(
						possibleWorkersForTasks);
				
				newDomain.remove((Object) workerIdToAssign);
				workersForTasksUpdated.put(tId, newDomain);

				Set<String> overlaps = t.getOverlappingTasks();
				for (String otherTId : overlaps) {
					List<Integer> otherCurrentDomain = workersForTasksUpdated.get(otherTId);
					List<Integer> otherNewDomain = new ArrayList<Integer>(otherCurrentDomain);
					otherNewDomain.remove((Object) workerIdToAssign);
					workersForTasksUpdated.put(otherTId, otherNewDomain);
				}
				// sort unassigned tasks based on heuristic
				Collections.sort(unassignedTasks, new TaskSorterMRVHeuristic(this.allTasks,
						this.constrainedCategories, workersForTasksUpdated));

				// update current best assignment if this is best assignment so far

				List<String> updatedUnassignedTasks = new ArrayList<String>(
						unassignedTasks);
				if (t.getWorkerReq() == currentSol.get(tId).size()) { // task
																		// completely staffed
					updatedUnassignedTasks.remove(tId);
					if (updatedUnassignedTasks.size() < this.currentBestAssignmentTasksRemaining) {
						this.currentBestAssignment = new HashMap<String, List<Integer>>(
								currentSol);
						this.currentBestAssignmentTasksRemaining = updatedUnassignedTasks
								.size();
						
					}
				}
				// recurse
				Map<String, List<Integer>> recursiveSolution = recursiveSolve(
						currentSol, updatedUnassignedTasks,
						workersForTasksUpdated, currentWorkerErgoRisk,currentSolInvertMap);

				// found solution!
				if (recursiveSolution != null)
					return recursiveSolution;

				// no result on branch
				currentSol.put(tId, workerIdsAssigned); // otherwise put back workers originally assigned
				currentSolInvertMap.put(workerIdToAssign, tasksForWorker);
				this.removeTaskErgoRiskFromWorker(currentWorkerErgoRisk, t,workerIdToAssign);

			}
		}
		// no solution on this branch
		//System.out.println("backtrack!");
		return null;
	}

	private void addTaskErgoRiskToWorker(
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

	private void removeTaskErgoRiskFromWorker(
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

	/**
	 * For a given task, return the possible workers the task can be assigned to
	 * @param currentSol
	 * @param tId
	 * @param possibleWorkersForTasks
	 * @return
	 */
	private List<Integer> getWorkerIdsForTask(
			Map<String, List<Integer>> currentSol, String tId,
			Map<String, List<Integer>> possibleWorkersForTasks) {

		List<Integer> initiallyPopulated = possibleWorkersForTasks.get(tId);
		return initiallyPopulated;
	}

	public Map<String, List<Integer>> getCurrentBestAssignment() {
		return this.currentBestAssignment;
	}

}