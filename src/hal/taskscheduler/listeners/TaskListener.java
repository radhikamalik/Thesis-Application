package hal.taskscheduler.listeners;

import hal.taskscheduler.dialogs.TaskInfoDialog;
import hal.taskscheduler.model.Task;
import hal.taskscheduler.model.TaskStatus;

import java.io.Serializable;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Listener for click events when task is clicked upon.
 * @author RadhikaMalik
 *
 */

public class TaskListener implements OnClickListener{
	
	Task task;
	TaskType type;
	
	// what type of assignments the task button belongs to: original or suggested
	public enum TaskType {
		ORIGINAL,SUGGESTED;
	}
	
	public TaskListener(Task task,TaskType type){
		this.task = task;
		this.type = type;
	}
	
	@Override
	public void onClick(View v) {
		
		String taskId = task.getTaskId();
		TaskStatus status = task.getStatus();
		int priority = task.getPriority();
		Bundle bundle = new Bundle();
		bundle.putString("taskId", taskId);
		bundle.putSerializable("status",status);
		bundle.putInt("priority", priority);
		bundle.putSerializable("taskType",type);
		bundle.putSerializable("ergoRisk", (Serializable) task.getErgoRiskAllCategories());
		
		DialogFragment newFragment = new TaskInfoDialog();
		newFragment.setArguments(bundle);
		newFragment.show(((Activity) v.getContext()).getFragmentManager(), "taskInfo");

	}

}
