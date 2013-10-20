package hal.taskscheduler.listeners;

import hal.taskscheduler.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SelectPreferredWorkerListener implements OnClickListener {

	List<Worker> allWorkers; // list of all workers
	CharSequence[] workerNames;

	List<Integer> selectedWorkerIndices; // list of worker indices selected from all
									// worker list

	List<Integer> selectedWorkerIds; // list of worker id's selected from all worker list
	String taskId;
	
	public SelectPreferredWorkerListener(List<Integer> selectedWorkerIds,
			List<Worker> allWorkers, String taskId) {
		this.selectedWorkerIds = selectedWorkerIds;
		this.allWorkers = allWorkers;
		this.taskId = taskId;

		workerNames = new CharSequence[allWorkers.size()];
		selectedWorkerIndices = new ArrayList<Integer>();
		for (int i = 0; i < allWorkers.size(); ++i) {
			Worker w = allWorkers.get(i);
			workerNames[i] = w.getWorkerName();
			if (selectedWorkerIds.contains(w.getWorkerID())){
				selectedWorkerIndices.add(i);
			}

		}

	}

	@Override
	public void onClick(final View v) {

		//System.out.println("hi");

		int count = allWorkers.size();
		boolean[] checkedWorkers = new boolean[count];


		for (int i = 0; i < count; i++){
			//int workerId = allWorkers.get(i).getWorkerID();
			//checkedWorkers[i] = selectedWorkerIds.contains(workerId);
			checkedWorkers[i] = selectedWorkerIndices.contains(i);
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
		String title = "";
		if (taskId != null){
			
			title += "Select Workers for Task: " + taskId;
		}else{
			title += "Select Workers";
		}
		builder.setTitle(title);


		DialogInterface.OnMultiChoiceClickListener workersDialogListener = new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which,
					boolean isChecked) {
				if (isChecked){
					selectedWorkerIds.add(allWorkers.get(which).getWorkerID());
					selectedWorkerIndices.add(which);
				}
				else{
					
					selectedWorkerIds.remove((Object)allWorkers.get(which).getWorkerID());
					selectedWorkerIndices.remove((Object)which);
				}
				
			}
		};

		builder.setMultiChoiceItems(workerNames, checkedWorkers,
				workersDialogListener);
		builder.setNeutralButton("Done",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    	
                    	String buttonText = "  ";
                    	for (int i:selectedWorkerIndices){ //go through the id's of selected workers
                    		buttonText += workerNames[i] + "; ";
                    	}
                    	((Button) v).setText(buttonText);
                    	dialog.dismiss();
                    }
                });
		
		AlertDialog dialog = builder.create();
		dialog.show();

	}

}
