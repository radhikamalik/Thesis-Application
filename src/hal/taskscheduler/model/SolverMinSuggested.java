package hal.taskscheduler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SolverMinSuggested extends Solver {

	public SolverMinSuggested(Map<String, Task> allTasks,
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
		
		//TODO: make new field of all workers to plan with to stop repeating for all and spare
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
		for (String tid:this.initialAssignmentTaskToWorker.keySet()){
			List<Integer> workersAssigned = this.initialAssignmentTaskToWorker.get(tid);
			for (Integer worker:workersAssigned){
				this.initialAssignmentWorkerToTask.get(worker).add(tid);
			}
		}
		
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
		System.out.println(workerErgoRisk);
		populateInitialWorkerDomain();
		
		// sort unassigned tasks based on heuristc
		Collections.sort(understaffedTasks, new TaskSorterMRVHeuristic(this.allTasks,
				this.constrainedCategories, this.possibleWorkersForTasks)); 
		System.out.println(understaffedTasks);								
																			

		System.out.println(this.initialAssignmentWorkerToTask);

	}

	public Map<String, List<Integer>> solve() {

		return recursiveSolve(this.initialAssignmentTaskToWorker,
				this.understaffedTasks, this.possibleWorkersForTasks,
				this.workerErgoRisk, this.initialAssignmentWorkerToTask);
	}


	private Map<String, List<Integer>> recursiveSolve(
			Map<String, List<Integer>> currentSol,
			List<String> unassignedTasks,
			Map<String, List<Integer>> possibleWorkersForTasks,
			Map<Integer, Map<RiskCategory, Double>> currentWorkerErgoRisk,
			Map<Integer,List<String>>currentSolInvertMap) {

		if (unassignedTasks.size() == 0){
			
			if (this.workersSpare == null)
				return currentSol;
			else{ //planning option 3, so check if solution uses spare workers, if it doesn't -> return solution!
				if (this.getSpareWorkersInAssignment(currentSol).size() == 0)
					return currentSol;
				else
					return null;
			}
		}

		String tId = unassignedTasks.get(0);
		
		Task t = this.allTasks.get(tId);
		List<Integer> workerIdsAssigned = currentSol.get(tId); // workers
																// currently
																// assigned
		
		
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
					// Task otherT = this.allTasks.get(otherTId);
					List<Integer> otherCurrentDomain = workersForTasksUpdated.get(otherTId);
					List<Integer> otherNewDomain = new ArrayList<Integer>(otherCurrentDomain);
					otherNewDomain.remove((Object) workerIdToAssign);
					workersForTasksUpdated.put(otherTId, otherNewDomain);
				}
				Collections.sort(unassignedTasks, new TaskSorterMRVHeuristic(this.allTasks,
						this.constrainedCategories, workersForTasksUpdated));

				// update current best assignment if this is best assignment so
				// far

				List<String> updatedUnassignedTasks = new ArrayList<String>(
						unassignedTasks);
				if (t.getWorkerReq() == currentSol.get(tId).size()){ //task completely staffed 
					
					updatedUnassignedTasks.remove(tId);
					
					if (updatedUnassignedTasks.size() < this.currentBestAssignmentTasksRemaining) {
						this.currentBestAssignment = new HashMap<String, List<Integer>>(
								currentSol);
						this.currentBestAssignmentTasksRemaining = updatedUnassignedTasks
								.size();
					}
					else if (this.workersSpare != null){
						if (updatedUnassignedTasks.size() == this.currentBestAssignmentTasksRemaining){
							//staffs same number of tasks so now check spare worker size
							if (this.getSpareWorkersInAssignment(currentSol).size() < this.getSpareWorkersInAssignment(this.currentBestAssignment).size())
								this.currentBestAssignment = new HashMap<String, List<Integer>>(
									currentSol);
						}
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

	private Set<Integer> getSpareWorkersInAssignment(Map<String, List<Integer>> currentSol) {
		
		Set<Integer> sparesInSol = new HashSet<Integer>();
		for (String taskId:currentSol.keySet()){
			List<Integer> workers = currentSol.get(taskId);
			for (int worker: this.workersSpare.keySet()){
				if (workers.contains(worker))
					sparesInSol.add(worker);
			}
				
		}
		return sparesInSol;
	}


}
