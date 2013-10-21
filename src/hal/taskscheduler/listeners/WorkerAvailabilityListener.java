package hal.taskscheduler.listeners;

import hal.taskscheduler.dialogs.AvailabilityDialog;
import hal.taskscheduler.model.Worker;
import hal.taskscheduler.model.WorkerAvailability;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class WorkerAvailabilityListener implements OnClickListener{

	Worker worker;
	
	public WorkerAvailabilityListener(Worker w){
		this.worker = w;
	}
	
	@Override
	public void onClick(View v) {
		
		int workerId = worker.getWorkerID();
		
		Bundle bundle = new Bundle();
		bundle.putInt("workerId", workerId);
		
		WorkerAvailability availability = worker.getAvailability();
		
		bundle.putSerializable("availability", availability);
		
		if (availability == WorkerAvailability.PART_SHIFT){
			
			//in case of part time availability also get the start and end time
			
			Date startTime = worker.getStartTime();
			Date endTime = worker.getEndTime();
			DateFormat df = new SimpleDateFormat("h:mm:a");
			
			String fromStr = df.format(startTime);

			String[] fromArr = fromStr.split(":");
			bundle.putString("fromHr", fromArr[0]);
    		bundle.putString("fromMin", fromArr[1]);
    		bundle.putString("fromAmPm", fromArr[2]);
    		
    		String toStr = df.format(endTime);

			String[] toArr = toStr.split(":");
			bundle.putString("toHr", toArr[0]);
    		bundle.putString("toMin", toArr[1]);
    		bundle.putString("toAmPm", toArr[2]);
    		
		}
		
		DialogFragment newFragment = new AvailabilityDialog();
		newFragment.setArguments(bundle);
		newFragment.show(((Activity) v.getContext()).getFragmentManager(), "availability");

	}

}
