package hal.taskscheduler.model;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Worker {

	private int workerID;
	private String workerName;
	
	private WorkerAvailability availability;
	private List<String> medicalRestr;
	
	private Date startTime; 
	private Date endTime;
	private WorkerType type;
	List<Task> tasks; //tasks assigned to current worker
	
	private Set<Integer> certifications; 
	
	public Worker(int workerID){
		this.workerID = workerID;
		tasks = new ArrayList<Task>();
		medicalRestr = new ArrayList<String>();
		certifications = new HashSet<Integer>();
	}
	

	public void setWorkerStartEndTime(Date workerStartTime, Date workerEndTime, Date shiftStartTime, Date shiftEndTime) {
		this.startTime = workerStartTime;
		this.endTime = workerEndTime;
		
		if ((workerStartTime.compareTo(shiftStartTime)>0) || (workerEndTime.compareTo(shiftEndTime)<0))
			this.availability = WorkerAvailability.PART_SHIFT;
		else
			this.availability = WorkerAvailability.FULL_SHIFT_AVAILABLE;
	}

	public void setWorkerAvailability(WorkerAvailability avail){
		this.availability = avail;
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

	public int getWorkerID() {
		return workerID;
	}
	
	public WorkerAvailability getAvailability(){
		return this.availability;
		
	}
	/**
	 * checks availability constraint for worker
	 * @param task
	 * @return
	 */
	public boolean isAvailableForTask(Task task){
		
		//worker not available at all
		if (this.availability == WorkerAvailability.FULL_SHIFT_UNAVAILABLE)
			return false;
		
		//worker available all shift
		if (this.availability == WorkerAvailability.FULL_SHIFT_AVAILABLE)
			return true;
		
		Date tStartTime = task.getStartTime();
		Date tEndTime = task.getEndTime();
		Date wStartTime = this.getStartTime();
		Date wEndTime = this.getEndTime();
		
		//worker must start before task and end after task
		return ((wStartTime.compareTo(tStartTime)<=0) && (wEndTime.compareTo(tEndTime)>=0));
		
	}
	
	@Override
	public String toString(){
		return "worker id: " + workerID + ", start time: " + startTime + ", end time: " + endTime;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	public String getWorkerName() {
		return workerName;
	}
	@Override
	public boolean equals(Object other){
		Worker o = (Worker) other;
		return this.workerID == o.getWorkerID();
	}
	@Override
	public int hashCode(){
		return this.workerID;
	}
	public void setType(WorkerType type) {
		this.type = type;
	}
	public WorkerType getType() {
		return type;
	}
	public void addMedicalRestriction(String restr_str) {
		this.medicalRestr.add(restr_str);
		
	}
	public List<String> getMedicalRestr() {
		return medicalRestr;
	}


	public void addCertifications(int cert) {
		this.certifications.add(cert);
	}


	public Set<Integer> getCertifications() {
		return certifications;
	}
	
}
