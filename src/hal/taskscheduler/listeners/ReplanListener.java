package hal.taskscheduler.listeners;

import hal.taskscheduler.dialogs.ReplanDialog;
import hal.taskscheduler.model.Allocator;

import java.io.Serializable;
import java.util.Date;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
/**
 * Listens to on click events for the Replan button.
 * Instantiates the ReplanDialog
 * 
 * @author RadhikaMalik
 *
 */
public class ReplanListener implements OnClickListener{

	Allocator alloc;
	Date shiftStartTime; 
	Date shiftEndTime;
	int hours;
	public ReplanListener(Allocator alloc, Date shiftStartTime, Date shiftEndTime, int hours){
		this.alloc = alloc;
		this.shiftStartTime=shiftStartTime;
		this.shiftEndTime=shiftEndTime;
		this.hours=hours;
	}
	
	@Override
	public void onClick(View v) {
		
		
		Bundle bundle = new Bundle();
		bundle.putSerializable("taskMap", (Serializable) alloc.getTasks());
		bundle.putSerializable("originalAssignments", (Serializable) alloc.getTaskWorkerAssignmentsOriginal());
		bundle.putSerializable("scheduledWorkers", (Serializable) alloc.getWorkers());
		bundle.putSerializable("spareWorkers", (Serializable) alloc.getSpareWorkers());
		bundle.putSerializable("shiftStartTime", shiftStartTime);
		bundle.putSerializable("shiftEndTime", shiftEndTime);
		bundle.putInt("hours", hours);
		
		DialogFragment newFragment = new ReplanDialog();
		newFragment.setArguments(bundle);
		newFragment.show(((Activity) v.getContext()).getFragmentManager(), "replan");

	}

}
