package hal.taskscheduler.model;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Task {

	private String taskID;

	Map<RiskCategory, Double> ergoRisk; // TODO: this is private?

	private Date startTime; 
	private Date endTime; 

	private int priority; 

	int workerReq;

	private List<String> dependencies;
	//List<Integer> assignedWorkerIds; // list of worker id's assigned to task
	Set<String> overLappingTaskIds; // a list of tasks that overlap with the given task

	Set<Integer> certifiedWorkers;
	
	private TaskStatus status;
	
	Set<Integer> certifications;
	
	// METHODS

	public Task(String taskID) {
		this.taskID = taskID;
		ergoRisk = new HashMap<RiskCategory, Double>();
		//assignedWorkerIds = new ArrayList<Integer>();
		dependencies = new ArrayList<String>();
		
		overLappingTaskIds = new HashSet<String>();
		certifiedWorkers = new HashSet<Integer>();
		
		certifications = new HashSet<Integer>();
	}

	public void setErgoRiskForCategory(RiskCategory category, Double risk_val) {
		ergoRisk.put(category, risk_val);
	}
	public Map<RiskCategory, Double> getErgoRiskAllCategories(){
		return this.ergoRisk;
	}
	public double getErgoRiskForCategory(RiskCategory category){
		if (this.ergoRisk.get(category) != null)
			return this.ergoRisk.get(category);
		else
			return 0;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}
	public String getTaskId(){
		return this.taskID;
	}
	@Override
	public String toString() {
		return "task id: " + taskID;/* + ", start time: " + startTime
				+ ", end time: " + endTime + ", priority: " + priority
				+ ", worker requirement: " + workerReq;*/
	}

	public void setWorkerReq(int workerReq) {
		this.workerReq = workerReq;
	}
	public int getWorkerReq(){
		return this.workerReq;
	}
	
	/*public void addWorker(int workerId){
		this.assignedWorkerIds.add(workerId);
	}*/
	/*
	public boolean isCompletelyStaffed(){
		return (assignedWorkerIds.size() >= workerReq);
	}*/
	public void addTaskOverlap(String overlapTaskId){
		this.overLappingTaskIds.add(overlapTaskId);
	}
	public Set<String> getOverlappingTasks(){
		return this.overLappingTaskIds;
	}
	@Override
	public boolean equals(Object other){
		Task o = (Task) other;
		return this.taskID.equals(o.getTaskId());
	}
	@Override
	public int hashCode(){
		return this.taskID.hashCode();
	}

	public double getMaxRiskFromCategories(
			List<RiskCategory> constrainedCategory) {
		double maxRisk = 0;
		for (RiskCategory r:constrainedCategory){
			double val = this.getErgoRiskForCategory(r);
			if (val > maxRisk)
				maxRisk = val;
			
		}
		return maxRisk;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public TaskStatus getStatus() {
		return status;
	}
	public void addCertifiedWorker(int workerId){
		this.certifiedWorkers.add(workerId);
	}

	public Set<Integer> getCertifiedWorkers() {

		return this.certifiedWorkers;
	}
	public void addCertifications(int cert) {
		this.certifications.add(cert);
	}


	public Set<Integer> getCertifications() {
		return certifications;
	}

	public void addDependency(String taskId) {
		this.dependencies.add(taskId);
	}

	public List<String> getDependencies() {
		return dependencies;
	}

}
