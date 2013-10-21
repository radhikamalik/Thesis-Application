package hal.taskscheduler.model;

import java.util.Date;


/**
 * Class for faster version of overlap finding
 * @author RadhikaMalik
 *
 */


public class IntervalValue implements Comparable<IntervalValue>{

	Date time;
	char type; //zero for start, one for end
	String taskId; //associated task id
	
	public IntervalValue(Date time, char type, String taskId){
		this.time=time;
		this.taskId=taskId;
		this.type=type;
	}

	@Override
	public int compareTo(IntervalValue other) {
		
		if (this.time.compareTo(other.time) != 0){
			if (this.time.compareTo(other.time) == -1)
				return -1;
			else //this.time > other.time
				return 1;
		} else{ //times are equal so compare type
			if (this.type==other.type){
				//end types always first since intervals are open
				if ((this.type == 'e') && (other.type == 's'))
					return -1;
				else //this type is 's' and other is 'e'
					return 1;
			} else{
				//if both time and task type are the same break ties on taskId
				return (this.taskId.compareTo(other.taskId)); 
			}
		}
	}

	@Override
	public boolean equals (Object o){
		IntervalValue other = (IntervalValue) o;
		return ((this.time == other.time) && (this.type==other.type) && (this.taskId==other.taskId));
	}

	
	
}
