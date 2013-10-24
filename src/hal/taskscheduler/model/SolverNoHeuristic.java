package hal.taskscheduler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SolverNoHeuristic implements SolverInterface {

	
	Map<String, List<Integer>> currentBestAssignment;
	int currentBestAssignmentTasksRemaining; //number of tasks understaffed in current best assignment
	List<String> understaffedTasks;
	Map<String,List<Integer>> initialAssignmentTaskToWorker;
	Map<String,List<Integer>> possibleWorkersForTasks; //for each task domain of workers it can be assigned to
	
	Map<String,Task> allTasks;
	Map<Integer,Worker> allWorkers;
	List<RiskCategory> constrainedCategories;
	
	public SolverNoHeuristic(Map<String,Task> allTasks, Map<String,List<Integer>> initialAssignmentTaskToWorker, Map<Integer,Worker>allWorkers,List<RiskCategory>constrainedCategories){
		this.currentBestAssignment = new HashMap<String,List<Integer>>();
		this.understaffedTasks = new ArrayList<String>();
		this.possibleWorkersForTasks = new HashMap<String,List<Integer>>();
		this.initialAssignmentTaskToWorker = initialAssignmentTaskToWorker;
		this.allTasks = allTasks;
		this.allWorkers = allWorkers;
		
		for (String tid: allTasks.keySet()){
			Task t = allTasks.get(tid);
			if (!initialAssignmentTaskToWorker.containsKey(tid))
				understaffedTasks.add(tid);
			else{
				int workerReq = t.getWorkerReq();
				int workersAssigned = initialAssignmentTaskToWorker.get(tid).size();
				if (workerReq > workersAssigned)
					understaffedTasks.add(tid);
			}
		}
		currentBestAssignmentTasksRemaining = understaffedTasks.size();
		this.constrainedCategories=constrainedCategories;
		Collections.sort(understaffedTasks,new TaskSorterNoHeuristic(this.allTasks,this.constrainedCategories,this.possibleWorkersForTasks)); //sort unassigned tasks based on heuristic
		
		populateInitialWorkerDomain();
	}
	
	private void populateInitialWorkerDomain(){
		
		for (String tId: allTasks.keySet()){
			Task t = allTasks.get(tId);
			List<Integer> workersForTask = new ArrayList<Integer>();
			for (int workerId: allWorkers.keySet()){
				
				if (t.getCertifiedWorkers().contains(workerId)){ //worker is certified to perform task
					Worker w = this.allWorkers.get(workerId);
					
					List<Integer> workersAlreadyAssigned = this.initialAssignmentTaskToWorker.get(tId); // workers initially assigned
					if (w.isAvailableForTask(t) && !workersAlreadyAssigned.contains(workerId)){ //worker not already assigned so add him
						workersForTask.add(workerId);
					}
				}
					
			}
			this.possibleWorkersForTasks.put(tId, workersForTask); //populate map of worker domain for given task
			
		}
		
	}
	/**
	 * For each task populate the workers who are skilled to perform given task
	 */
	/*
	public void populateSkilledWorkers(){

		for (String tid: this.allTasks.keySet()){
			Task t = this.allTasks.get(tid);
			List<Integer> certifiedWorkers = t.getCertifiedWorkers();
			for (int workerId: certifiedWorkers){
				Worker w = this.allWorkers.get(workerId);
				if (w.isAvailableForTask(t))
					
			}
			//System.out.println("task: " + tid + " skilled workers: " + t.getCertifiedWorkers());
		}
		
		
	}*/
	@Override
	public Map<String,List<Integer>> solve(){

		return recursiveSolve(this.initialAssignmentTaskToWorker, this.understaffedTasks);
	}
	
	/**
	 * function to check if the new assignment is consistent with current assignment
	 * @param currentSol
	 * @param task
	 * @param workerIds
	 * @return
	 */
	private boolean isConsistent(Map<String,List<Integer>> currentSol, String tid, int wId){
		
		Task task = allTasks.get(tid);
		
		Set<String> overlapIds = task.getOverlappingTasks();
		//System.out.println("OVERLAPS: " + overlapIds);
		//double maxRisk = task.getMaxRiskFromCategories(constrainedCategories);
		
			
			//System.out.println("checking consistency of task: " + tid + "with workers:"+ workerIds);
			

			Worker w = this.allWorkers.get(wId);
			//availability constraints
			//the new task must be during the worker's availability
			// since initial task domain always must have worker be available, assert this!
			assert(w.isAvailableForTask(task));
			
			//constraint 1: no worker can work on 2 tasks at the same time
			//for all overlapping tasks for current task, ensure worker is not assigned to any of these tasks
			for (String overlapId: overlapIds){
				List<Integer> workersInOverlaps = currentSol.get(overlapId);
				if (workersInOverlaps.contains(wId)) //an overlapping task contains the same worker
					return false;
			}
			//ergonomic risk threshold constraint
			
			/*if ((wId == 6) && (tid.equals("task4"))){
				System.out.println("checking ergo consistency of task: " + tid + "with workers:"+ workerIds);
			}*/

			Map<RiskCategory, Double> currentTaskErgoRisk = task
					.getErgoRiskAllCategories();

			Map<RiskCategory, Double> wErgoRisk = new HashMap<RiskCategory, Double>();
			Map<RiskCategory, Integer> wNumTasksCausingRisk = new HashMap<RiskCategory, Integer>();
			// go through all tasks in the current solution
			for (String taskId : currentSol.keySet()) {
				List<Integer> workerIdsForTask = currentSol.get(taskId);
				if (workerIdsForTask.contains(wId)) { // a task this worker
														// is working on

					Map<RiskCategory, Double> tErgoRisk = allTasks.get(taskId)
							.getErgoRiskAllCategories();
					for (RiskCategory c : constrainedCategories) {
						Double w_risk = wErgoRisk.get(c);
						Double t_risk = tErgoRisk.get(c);
						Integer numTasksCausingRisk=wNumTasksCausingRisk.get(c);
						
						if (t_risk != null) { //task has some ergo risk in this category
							if (w_risk != null) { //worker has prior risk in this category
								w_risk += t_risk;
								
								numTasksCausingRisk += 1;
							} else {
								w_risk = t_risk;
								numTasksCausingRisk = 1;
							}
							 
						}
						wNumTasksCausingRisk.put(c, numTasksCausingRisk);
						wErgoRisk.put(c, w_risk);
					}
				}

			}
			for (RiskCategory c : constrainedCategories) {
				Double risk = wErgoRisk.get(c);
				
				Double currentTaskRisk = currentTaskErgoRisk.get(c);
				//Integer numTasksCausingRisk=wNumTasksCausingRisk.get(c);
				if (currentTaskRisk != null) {
					if (risk != null) { // worker already has prior risk in
										// this category, so add task risk
						risk += currentTaskRisk;
						if (risk > 1)
							return false;
					} else {
						//risk = currentTaskRisk;
					}
				}
			}

			
		
		return true;
	}
	private Map<String,List<Integer>> recursiveSolve(Map<String,List<Integer>> currentSol, List<String>unassignedTasks){// throws NoSolutionFoundException{

		
		if (unassignedTasks.size() == 0)
			return currentSol;
		
		
		String tId = unassignedTasks.get(0);
		//System.out.println("considering task: " + tId);
		Task t = this.allTasks.get(tId);
		List<Integer> workerIdsAssigned = currentSol.get(tId); //workers currently assigned
		//int workerReq = t.getWorkerReq();
		//int numWorkersAssigned = workerIdsAssigned.size();
		//int moreWorkersReqd = workerReq-numWorkersAssigned; //number of more workers required in this task

		//workers who can be assigned to this task
		List<Integer> possibleWorkerIds = getWorkerIdsForTask(currentSol, tId); 
		int possibleNumOfWorkers=possibleWorkerIds.size();
		
		for (int i=0; i < possibleNumOfWorkers; ++i){
			List<Integer> updatedWorkersAssigned = new ArrayList<Integer>(workerIdsAssigned);
			
			//add workers to current task so now it's fully assigned workers
			int workerIdToAssign = possibleWorkerIds.get(i);
			
			if (this.isConsistent(currentSol, tId, workerIdToAssign)) {
				
				
				updatedWorkersAssigned.add(workerIdToAssign);
				currentSol.put(tId, updatedWorkersAssigned);
				
				// update current best assignment if this is best assignment so far
				
				List<String> updatedUnassignedTasks = new ArrayList<String>(unassignedTasks);
				if (t.getWorkerReq() == currentSol.get(tId).size()){ //task completely staffed 
				
					updatedUnassignedTasks.remove(tId);
					
					//update task domain and reorder tasks
					
					if (updatedUnassignedTasks.size() < this.currentBestAssignmentTasksRemaining) {
						this.currentBestAssignment = new HashMap<String, List<Integer>>(
								currentSol);
						this.currentBestAssignmentTasksRemaining = updatedUnassignedTasks
								.size();
					}
				}
				// recurse
				Map<String, List<Integer>> recursiveSolution = recursiveSolve(
						currentSol, updatedUnassignedTasks);

				// found solution!
				if (recursiveSolution != null)
					return recursiveSolution;
				
				//no result on branch
				
			}
			currentSol.put(tId, workerIdsAssigned); //otherwise put back worker id's originally assigned
		}
		//no solution on this branch
		//System.out.println("backtrack!");
		return null;
	}
	/**
	 * For a given task, return the possible workers the task can be assigned to 
	 * @param currentSol 
	 * @param t
	 * @return
	 */
	private List<Integer> getWorkerIdsForTask(Map<String, List<Integer>> currentSol, String tId) {


		List<Integer> initiallyPopulated = possibleWorkersForTasks.get(tId);
		List<Integer> alreadyInSol = currentSol.get(tId);
		if (alreadyInSol != null){
			List<Integer> updatedPopulated = new ArrayList<Integer>(initiallyPopulated);
			updatedPopulated.removeAll(alreadyInSol);
			return updatedPopulated;
		}else
			return initiallyPopulated;
	}
	@Override
	public Map<String, List<Integer>> getCurrentBestAssignment(){
		return this.currentBestAssignment;
	}
	
}
