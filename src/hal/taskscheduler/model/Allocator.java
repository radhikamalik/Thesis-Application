package hal.taskscheduler.model;




import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.res.AssetManager;
/**
 * The Allocator class is the main class that interfaces between the frontend interface and the solver that
 * implements the scheduling algorithm.
 * It holds the entire data model for the application.
 * It is responsible for loading the data, providing methods for the frontend to query the data it needs and to make changes
 * to the data model, for calling the solver to calculate the new schedule based and then communicating
 * the results back to the interface.
 *  
 * @author RadhikaMalik
 *
 */


public class Allocator {

	private Map<Integer, Worker> workers; // map from worker id to worker
	private Map<Integer, Worker> workersSpare; // map from worker id to worker
	private Map<Integer,Worker> workersAll; //map from worker id to worker of all workers
	
	private Map<String, Task> tasks; // map from task id to task
	
	private Map<String,List<Integer>> taskWorkerAssignmentsOriginal; //map from taskId to worker Ids assigned to task
	private Map<String,List<Integer>> taskWorkerAssignmentsSuggested; //map from taskId to worker Ids assigned to task

	private Map<Integer,List<String>> workerTaskAssignmentsOriginal; //map from workerId to taskId
	private Map<Integer,List<String>> workerTaskAssignmentsSuggested; //map from workerId to taskId
	private List<RiskCategory> constrainedCategories;
	
	private Date shiftStartTime;
	private Date shiftEndTime;
	
	//INPUT DATA FILE NAMES
	private final String shiftTimeFile = "shift_time.txt";
	private final String taskPriorityFile = "task_priorities.txt";
	private final String taskTimesFile = "task_times.txt";
	private final String taskWorkerRequirement = "workers_for_tasks.txt";
	private final String taskRiskProfile = "task_risk_profile.txt";
	private final String workerAvailability = "worker_schedule.txt";
	private final String spareWorkerAvailability = "worker_schedule_spare.txt";
	private final String taskWorkerAssignment = "task_worker_assignment.txt";
	private final String medicalRestrFile = "worker_medical_descriptive.txt";
	private final String workerCerts = "worker_certification.txt";
	private final String taskCerts = "task_certifications.txt";
	private final String taskDependencies = "task_dependencies.txt";
	
	public Allocator() {
		workers = new HashMap<Integer, Worker>();
		workersSpare = new HashMap<Integer,Worker>();
		workersAll = new HashMap<Integer,Worker>();
		tasks = new HashMap<String, Task>();
		taskWorkerAssignmentsOriginal = new HashMap<String,List<Integer>>();
		workerTaskAssignmentsOriginal = new HashMap<Integer,List<String>>();
		
		workerTaskAssignmentsSuggested= new HashMap<Integer,List<String>>();
	}

	public Map<Integer, Worker> getWorkers(){
		return this.workers;
	}
	public Map<String, Task> getTasks(){
		return this.tasks;
	}
	/**
	 * Load all the initial input data. 
	 * 
	 * @param sourceFolderPath
	 */
	public void loadData(AssetManager am){
		try {
			loadShiftTime(am.open(shiftTimeFile));
			loadWorkers(am.open(workerAvailability), this.workers);
			loadWorkers(am.open(spareWorkerAvailability), this.workersSpare);
			loadTasks(am.open(taskPriorityFile), am.open(taskTimesFile), am.open(taskWorkerRequirement),am.open(taskRiskProfile));
			loadWorkerTaskAssignments(am.open(taskWorkerAssignment),this.taskWorkerAssignmentsOriginal);
			loadWorkerMedicalRestrictions(am.open(medicalRestrFile));
			loadWorkerCerts(am.open(workerCerts));
			loadTaskCerts(am.open(taskCerts));
			loadTaskDependencies(am.open(taskDependencies));
			
			workersAll.putAll(workers);
			workersAll.putAll(workersSpare);
			this.populateCertifiedWorkers();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * load data related to certifications required for each task.
	 * @param inputStream
	 */
	private void loadTaskCerts(InputStream inputStream) {
		try {

			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);

			String s;

			while ((s = in.readLine()) != null) {
				// splitting on comma gives array [taskid, cert1, cert2, etc.]
				String[] task_info_arr = s.split(","); 
				String taskId = task_info_arr[0];

				Task t = this.getTask(taskId);

				for (int i = 1; i < task_info_arr.length; i++) {
					String cert_str = task_info_arr[i];
					
					int cert = Integer.parseInt(cert_str);
					t.addCertifications(cert);
					
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/**
	 * Load data on task dependencies.
	 * @param inputStream
	 */
	private void loadTaskDependencies(InputStream inputStream) {
		try {

			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);

			String s;

			while ((s = in.readLine()) != null) {
				String[] task_info_arr = s.split(","); 
				String taskId = task_info_arr[0];

				Task t = this.getTask(taskId);

				for (int i = 1; i < task_info_arr.length; i++) {
					String dependency = task_info_arr[i];
					assert(this.getTask(dependency)!=null);
					t.addDependency(dependency);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Load data on worker certifications
	 * @param inputStream
	 */
	private void loadWorkerCerts(InputStream inputStream) {
		try {

			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);

			String s;

			while ((s = in.readLine()) != null) {
				// splitting on comma gives array [workerid, cert1, cert2, etc.]
				String[] worker_info_arr = s.split(","); 
				int workerId = Integer.parseInt(worker_info_arr[0]);

				Worker w = this.getWorker(workerId);

				for (int i = 1; i < worker_info_arr.length; i++) {
					String cert_str = worker_info_arr[i];
					cert_str = cert_str.trim();
					int cert = Integer.parseInt(cert_str);
					w.addCertifications(cert);
					
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/**
	 * Method that the interface would call to actually compute new worker task assignments.
	 * The initial worker task assignments would be provided by the frontend. For experiments without 
	 * involvind the frontend, this method takes in the initial assignments from a text file
	 * 
	 */
	public void assignWorkersToTasks(Map<String, List<Integer>> preferredWorkers){
		Map<String, List<Integer>> initialAssignment;
		if (preferredWorkers == null){
			initialAssignment= new HashMap<String,List<Integer>>();
			String initialAssignmentPath = "Data/initial_task_worker_assignment.txt";

			try {
				loadWorkerTaskAssignments(new FileInputStream(initialAssignmentPath),
						initialAssignment);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			initialAssignment = preferredWorkers;
		}

		// solve!
		this.populateTaskOverlaps();
		this.populateCertifiedWorkers();
		//Toggle the class instance to test outputs for different kinds of solvers
		SolverInterface s = new SolverMRVHeuristic(this.tasks, initialAssignment, this.workers, null,	constrainedCategories);
		//Solver s = new Solver(this.tasks, initialAssignment, this.workers, this.workersSpare,	constrainedCategories);
		//SolverMinSuggested s = new SolverMinSuggested(this.tasks, initialAssignment, this.workers, null,	constrainedCategories);
		//SolverMinSuggested s = new SolverMinSuggested(this.tasks, initialAssignment, this.workers, this.workersSpare,constrainedCategories);
		
		//uncomment to calculate time for schedulers.
		//long begin = System.currentTimeMillis();
		s.solve();
		//long end = System.currentTimeMillis();
		//long diff = end-begin;
		//System.out.println("**********************************************************************: " + Long.toString(diff));
		
		this.taskWorkerAssignmentsSuggested = s.getCurrentBestAssignment();
		//this.understaffedSuggested = s.getUnderstaffedTasks();
		populateWorkerTaskAssignment(this.taskWorkerAssignmentsSuggested,this.workerTaskAssignmentsSuggested);
	}
	/** 
	 * Find number of tasks that are completely staffed in original assignments.
	 * This method will be called by the frontend controller.
	 * @return
	 */
	public int numOfTasksStaffedOriginal(){
		int count = 0;
		for (String taskId: tasks.keySet()){
			Task t = this.tasks.get(taskId);
			List<Integer> assignedWorkers = this.taskWorkerAssignmentsOriginal.get(taskId);
			if (t.getWorkerReq() == assignedWorkers.size())
				count++;
		}
		return count;
	}
	/** 
	 * Find number of tasks that are completely staffed in suggested assignments.
	 * This method will be called by the frontend controller.
	 * @return
	 */
	public int numOfTasksStaffedSuggested(){
		int count = 0;
		for (String taskId: tasks.keySet()){
			Task t = this.tasks.get(taskId);
			List<Integer> assignedWorkers = this.taskWorkerAssignmentsSuggested.get(taskId);
			if (assignedWorkers!=null)
				if (t.getWorkerReq() == assignedWorkers.size())
					count++;
		}
		return count;
	}
	/**
	 * For each task populate the workers who are skilled to perform given task
	 */
	private void populateCertifiedWorkers(){
		for (String tid: this.tasks.keySet()){
			Task t = this.getTask(tid);
			Set<Integer> taskCerts = t.getCertifications();
			for (int workerId: this.workersAll.keySet()){
				Worker w = this.getWorker(workerId);
				Set<Integer> workerCerts = w.getCertifications();
				if (workerCerts.containsAll(taskCerts)){ //worker contains all certs reqd by task
					t.addCertifiedWorker(workerId);
				}
			}
		}		
	}
	
	/**
	 * For each task, populate overlapping tasks
	 * Naive brute worse. O(n^2) complexity
	 */
	public void populateTaskOverlaps(){
		
		String[] taskIdArray = tasks.keySet().toArray(new String[tasks.keySet().size()]);
		
		
		for (int i = 0; i< taskIdArray.length-1; ++i){
			for (int j=i+1; j<taskIdArray.length; ++j){

				String tid1= taskIdArray[i];
				String tid2= taskIdArray[j];
				Task t1 = tasks.get(tid1);
				Task t2 = tasks.get(tid2);
					
					//two tasks overlap if t1 starts before t2 starts and ends after t2 starts
					// or if t2 starts before t1 starts and ends after t1 starts
				if (((t1.getStartTime().compareTo(t2.getStartTime()) <=0) && (t1.getEndTime().compareTo(t2.getStartTime())>0))||
						((t2.getStartTime().compareTo(t1.getStartTime())<=0) &&(t2.getEndTime().compareTo(t1.getStartTime())>0))){
					t1.addTaskOverlap(tid2);
					t2.addTaskOverlap(tid1);
				}
			}
		}
		
	}
	/**
	 * Faster version of @populateTaskOverlaps
	 * O(nlogn) complexity
	 * TODO: benchmark to see performance difference?
	 */
	public void populateTaskOverlapsFaster(){
		
		//populate list of time intervals
		List<IntervalValue> listStartEndTimes = new ArrayList<IntervalValue>();
		for (Task t: tasks.values()){
			IntervalValue ivStart = new IntervalValue(t.getStartTime(),'s',t.getTaskId());
			IntervalValue ivEnd = new IntervalValue(t.getEndTime(),'e', t.getTaskId());
			listStartEndTimes.add(ivStart);
			listStartEndTimes.add(ivEnd);
		}
		
		//sort
		Collections.sort(listStartEndTimes);
		
		//traverse sorted list and find overlapping intervals
		
		Set<String> currentProcessing = new HashSet<String>(); //list of task id's currently open
		
		for (IntervalValue iv: listStartEndTimes){
			if (iv.type == 's'){
				Task t = tasks.get(iv.taskId);
				//all intervals currently open are overlapping
				for (String tid: currentProcessing){
					t.addTaskOverlap(tid);
				}
				currentProcessing.add(iv.taskId);
			}else {
			// type is end so remove id from tasks being processed
				currentProcessing.remove(iv.taskId);		
			}
		}
	}
	/**
	 * Load data about shift start time and end time
	 * @param shiftTimeIs
	 */
	private void loadShiftTime(InputStream shiftTimeIs){
		
		try {
			
			InputStreamReader inputStreamReader = new InputStreamReader(shiftTimeIs);
			BufferedReader in = new BufferedReader(inputStreamReader);
			
			String s = in.readLine();
			// splitting on comma gives array [start-time, end_time]
			String[] shift_info_arr = s.split(",", -1); 

			assert (shift_info_arr.length == 2);
			
			DateFormat formatter = new SimpleDateFormat("h:mm a");
			Date startTime = (Date)formatter.parse(shift_info_arr[0]);
			Date endTime = (Date)formatter.parse(shift_info_arr[1]);
			
			this.setShiftStartTime(startTime);
			this.setShiftEndTime(endTime);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Function to load workers and their start and end times
	 * @param workerMap 
	 * 
	 */
	
	private void loadWorkers(InputStream workerInputStream, Map<Integer, Worker> workerMap) {
		try {
			
			InputStreamReader inputStreamReader = new InputStreamReader(workerInputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);
			
			String s;

			while ((s = in.readLine()) != null) {
				String[] worker_info_arr = s.split(",", -1); // splitting on comma gives array [start-time, end_time]

				assert (worker_info_arr.length == 4);
				int worker_id = Integer.parseInt(worker_info_arr[0]);
				String worker_name = worker_info_arr[1];
				
				DateFormat formatter = new SimpleDateFormat("h:mm a");
				Date startTime = (Date)formatter.parse(worker_info_arr[2]);
				Date endTime = (Date)formatter.parse(worker_info_arr[3]);

				Worker w = new Worker(worker_id);
				w.setWorkerName(worker_name);
				w.setWorkerStartEndTime(startTime,endTime, this.shiftStartTime, this.shiftEndTime);
				
				workerMap.put(worker_id, w);
			}


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Function to load medical restrictions for workers. Medical restrictions are not 
	 * featured in the planner yet but are only needed for display on the frontend
	 * @param workerInputStream
	 */
	private void loadWorkerMedicalRestrictions(InputStream workerInputStream) {
		try {
			
			InputStreamReader inputStreamReader = new InputStreamReader(workerInputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);
			
			String s;

			while ((s = in.readLine()) != null) {
				String[] worker_info_arr = s.split(","); // splitting on comma gives array [workerid, restriction1, restriction2 etc.]	
				int worker_id = Integer.parseInt(worker_info_arr[0]);
				
				Worker w = this.getWorker(worker_id);
				
				for (int i = 1; i< worker_info_arr.length; i++){
					String restr_str = worker_info_arr[i];
					w.addMedicalRestriction(restr_str);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Function to load task data including times, priorities, worker requirements and risk profiles
	 * @param priorityIS - input stream of file storing task priorities
	 * @param timeIS - input stream of file storing task times
	 * @param workerReqIS - input stream of file storing task worker requirements
	 * @param riskProfileIS - input stream of file storing task risk profiles
	 */
	
	private void loadTasks(InputStream priorityIS, InputStream timeIS, InputStream workerReqIS, InputStream riskProfileIS) {
		try {	
			InputStreamReader inputStreamReader = new InputStreamReader(timeIS);
			BufferedReader in = new BufferedReader(inputStreamReader);
			String s;
			while ((s = in.readLine()) != null) {
				// splitting on comma
				// gives array of task, start-time, end_time
				String[] task_info_arr = s.split(","); 
				String task_id = task_info_arr[0];
				Task t = new Task(task_id);
				
				DateFormat formatter = new SimpleDateFormat("h:mm a");
				Date startTime = (Date)formatter.parse(task_info_arr[1]);
				Date endTime = (Date)formatter.parse(task_info_arr[2]);
				
				t.setStartTime(startTime);
				t.setEndTime(endTime);
				
				//all tasks initially are those that have not yet started
				t.setStatus(TaskStatus.NOT_STARTED);
				
				tasks.put(task_id, t);
			}

			// add task priorities

			inputStreamReader = new InputStreamReader(priorityIS);
			in = new BufferedReader(inputStreamReader);
			
			while ((s = in.readLine()) != null) {
				String[] task_info_arr = s.split(","); // splitting on comma gives array of task, priority
				String task_id = task_info_arr[0];
				
				int priority = Integer.parseInt(task_info_arr[1]);
				Task t = tasks.get(task_id);
				t.setPriority(priority);
			}

			
			// add worker requirements
			inputStreamReader = new InputStreamReader(workerReqIS);
			in = new BufferedReader(inputStreamReader);
			
			while ((s = in.readLine()) != null) {
				String[] task_info_arr = s.split(","); 
				String task_id = task_info_arr[0];
				int worker_req = Integer.parseInt(task_info_arr[1]);
				
				Task t = tasks.get(task_id);
				t.setWorkerReq(worker_req);

			}


			inputStreamReader = new InputStreamReader(riskProfileIS);
			in = new BufferedReader(inputStreamReader);
			
			while ((s = in.readLine()) != null) {
				String[] task_info_arr = s.split(",",-1); // splitting on comma gives array of task, priority
				String task_id = task_info_arr[0];

				Task t = tasks.get(task_id);
				// array of the categories in the order they were declared in the enum

				RiskCategory[] categories = RiskCategory.values();
				assert (categories.length == 6);
				assert(task_info_arr.length == 7);

				for (int i = 0; i < categories.length; ++i) {
					String risk_str = task_info_arr[i+1]; // the risk value read from file
					Double risk_val;
					if (risk_str.equals(" ")) { // task does not have that risk assigned
						risk_val = null;
					} else {
						risk_val = Double.parseDouble(risk_str.replace(" ", ""));
						
					}
					t.setErgoRiskForCategory(categories[i], risk_val);
				}
				
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Load initial worker task assignments and populate a given assignment with it
	 * @param inputStream
	 * @param assignment
	 */
	private void loadWorkerTaskAssignments(InputStream inputStream, Map<String,List<Integer>> assignment) {
		try {

			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);
			
			String s;
			
			while ((s = in.readLine()) != null) {
				String[] task_info_arr = s.split(","); // splitting on comma gives array of task, workers assigned to task
				
				String task_id = task_info_arr[0];
				List<Integer> assignedWorkers =new ArrayList<Integer>();
				
				//go through the worker id's assigned to the task and add them to worker list
				for (int i = 1; i<task_info_arr.length; ++i){
					int workerId = Integer.parseInt(task_info_arr[i]);
					assignedWorkers.add(workerId);
					
					//add task to workers in reverse mapping
					List<String> tasksAssignedToWorker = workerTaskAssignmentsOriginal.get(workerId);
					if (tasksAssignedToWorker == null){
						tasksAssignedToWorker = new ArrayList<String>();
					}
					tasksAssignedToWorker.add(task_id);
					workerTaskAssignmentsOriginal.put(workerId, tasksAssignedToWorker);
					
				}	
				assignment.put(task_id,assignedWorkers);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private double getMaxErgoRiskForWorker(int workerId, Map<Integer,List<String>>workerTaskAssignment){
		double maxRisk = 0;
		for (RiskCategory cat: RiskCategory.values()){
			double risk = this.getErgonomicRiskForWorker(workerTaskAssignment, workerId, cat);
			if (risk>maxRisk)
				maxRisk = risk;
		}
		return maxRisk;
	}
	public double getMaxErgoRiskForWorkerOriginal(int workerId){
		return getMaxErgoRiskForWorker(workerId,this.workerTaskAssignmentsOriginal);
	}
	
	/**
	 * Takes in task to worker assignment and populates worker to task assignment
	 * @param taskWorkerAssignment
	 * @param workerTaskAssignment
	 */
	public void populateWorkerTaskAssignment(Map<String,List<Integer>> taskWorkerAssignment, Map<Integer, List<String>> workerTaskAssignment){
		workerTaskAssignment.clear(); //empty assignments first and then refill
		for (String tid : taskWorkerAssignment.keySet()) {
			
			List<Integer> workerIds = taskWorkerAssignment.get(tid);
			
			for (int workerId : workerIds) {
				List<String> tasksAssignedToWorker = workerTaskAssignment
						.get(workerId);
				if (tasksAssignedToWorker == null) {
					tasksAssignedToWorker = new ArrayList<String>();
				}
				tasksAssignedToWorker.add(tid);
				workerTaskAssignment.put(workerId,
						tasksAssignedToWorker);
			}
		}
	}
	
	public double getErgonomicRiskForWorker(Map<Integer,List<String>> assignment, int workerId, RiskCategory c){
		
		List<String> assignedTasks = assignment.get(workerId);
		
		if (assignedTasks == null)
			return 0;
		
		double result = 0;
		for (String tid: assignedTasks){
			Task t = tasks.get(tid);
			
			Map<RiskCategory,Double>ergoRiskByTask = t.getErgoRiskAllCategories();
			Double risk = ergoRiskByTask.get(c);
			
			if (risk!=null)
				result += risk;
		}
		return result;
	}
	public Map<RiskCategory,Integer> getNumberOfHighRiskWorkersOriginal(){
		
		Map<RiskCategory,Integer> ergoRiskMap = new HashMap<RiskCategory,Integer>();

		for (RiskCategory c : RiskCategory.values()) {
			int count = 0;

			for (int workerId : workers.keySet()) {
				double risk = getErgonomicRiskForWorker(
						this.workerTaskAssignmentsOriginal, workerId, c);

				if (risk > 1) {
					count++;
				}
			}
			ergoRiskMap.put(c, count);
			
		}
		return ergoRiskMap;
	}
	public Map<RiskCategory,Integer> getNumberOfHighRiskWorkersSuggested(){
		
		Map<RiskCategory,Integer> ergoRiskMap = new HashMap<RiskCategory,Integer>();

		for (RiskCategory c : RiskCategory.values()) {
			int count = 0;

			for (int workerId : workers.keySet()) {
				double risk = getErgonomicRiskForWorker(
						this.workerTaskAssignmentsSuggested, workerId, c);

				if (risk > 1) {
					count++;
				}
			}
			ergoRiskMap.put(c, count);
			
		}
		return ergoRiskMap;
	}

	public void setShiftStartTime(Date shiftStartTime) {
		this.shiftStartTime = shiftStartTime;
	}

	public Date getShiftStartTime() {
		return shiftStartTime;
	}

	public void setShiftEndTime(Date shiftEndTime) {
		this.shiftEndTime = shiftEndTime;
	}

	public Date getShiftEndTime() {
		return shiftEndTime;
	}

	public Map<String,List<Integer>> getTaskWorkerAssignmentsSuggested() {
		return taskWorkerAssignmentsSuggested;
	}
	public void removeWorker(int workerId) {
		
		//remove from worker list
		//this.workers.remove(workerId);
		
		//remove from original assignments
		List<String> originalAssignments = this.workerTaskAssignmentsOriginal.get(workerId);
		if (originalAssignments != null) {
			for(String tid: originalAssignments){
				List<Integer> workersForTask = this.taskWorkerAssignmentsOriginal.get(tid);
				workersForTask.remove((Object)workerId);
			}
		}
		this.workerTaskAssignmentsOriginal.remove(workerId);
		
	}
	public List<Integer> getWorkersForTaskOriginal (String taskId){
		return this.taskWorkerAssignmentsOriginal.get(taskId);
	}

	public WorkerAvailability getWorkerAvailability(int workerId) {
		
		return getWorker(workerId).getAvailability();
	}

	public Date getWorkerStartTime(int workerId) {
		return getWorker(workerId).getStartTime();
	}

	public Date getWorkerEndTime(int workerId) {
		return getWorker(workerId).getEndTime();

	}
	public Worker getWorker(int workerId){
		if (this.workers.containsKey(workerId))
			return workers.get(workerId);
		else if (this.workersSpare.containsKey(workerId))
			return this.workersSpare.get(workerId);
		
		return null;
	}

	public void setWorkerAvailability(int workerId,
			WorkerAvailability availability, Date startTime, Date endTime) {
		Worker w = getWorker(workerId);
		w.setWorkerAvailability(availability);
		
		if (availability == WorkerAvailability.PART_SHIFT){
			w.setWorkerStartEndTime(startTime, endTime, shiftStartTime, shiftEndTime);
		}
	}

	public Map<Integer, Worker> getSpareWorkers() {
		return this.workersSpare;
	}

	public WorkerType getWorkerType(int workerId) {
		if (this.workers.containsKey(workerId))
			return WorkerType.SCHEDULED;
		else if (this.workersSpare.containsKey(workerId))
			return WorkerType.SPARE;
		return null;
	}

	public int getTotalNumberOfTask() {
		return this.tasks.size();
	}

	public int getNumberOfTasksCompletelyStaffed(Map<String,List<Integer>> assignment) {
		int result = 0;
		for (String tId: this.tasks.keySet()){
			
			Task t = this.tasks.get(tId);
			
			List<Integer> workerAssigned = assignment.get(tId);
			if (t.getWorkerReq() == workerAssigned.size() )
				result++;
			
		}
		return result;
	}

	public List<String> getWorkerMedicalRestrn(Integer wid) {
		return getWorker(wid).getMedicalRestr();
	}


	public void setConstrainedCategories(List<RiskCategory> constrainedCategories) {
		this.constrainedCategories = constrainedCategories;
	}

	public List<RiskCategory> getConstrainedCategories() {
		return constrainedCategories;
	}

	public Map<String, List<Integer>> getTaskWorkerAssignmentsOriginal() {
		return this.taskWorkerAssignmentsOriginal;
	}

	public Task getTask(String tid) {
		return this.tasks.get(tid);
	}
	public void setTaskStatus(String taskId, TaskStatus status) {
		this.tasks.get(taskId).setStatus(status);
		
	}

	public double getMaxErgoRiskForWorkerSuggested(int workerId) {
		return getMaxErgoRiskForWorker(workerId,this.workerTaskAssignmentsSuggested);
	}

}
