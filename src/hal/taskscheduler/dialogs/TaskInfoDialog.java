package hal.taskscheduler.dialogs;

import hal.taskscheduler.R;
import hal.taskscheduler.listeners.TaskListener.TaskType;
import hal.taskscheduler.model.RiskCategory;
import hal.taskscheduler.model.TaskStatus;

import java.text.DecimalFormat;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
/**
 * Dialog to view info related to task including priority, status and ergonomic risk.
 * 
 * @author RadhikaMalik
 *
 */

public class TaskInfoDialog extends DialogFragment {
	
	
	String taskId;
	TaskStatus status;
	int priority;
	Map<RiskCategory,Double> ergoRisk;

	View view;
	
	ChangeTaskStatusListener mListener;
	
	public interface ChangeTaskStatusListener {
        public void onSaveTaskStatusChange(DialogFragment dialog);
    }
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		Bundle bundle = this.getArguments();
		String taskId = bundle.getString("taskId");
		TaskStatus status= (TaskStatus) bundle.getSerializable("status");
		int priority = bundle.getInt("priority");		
		TaskType type = (TaskType) bundle.getSerializable("taskType");	
		ergoRisk = (Map<RiskCategory, Double>) bundle.getSerializable("ergoRisk");
		
		Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = ((Activity) getActivity()).getLayoutInflater();
		
		view = inflater.inflate(R.layout.task_info_dialog_layout, null);
		
		Spinner taskStatus = (Spinner) view.findViewById(R.id.task_status_spinner);
		if (status == TaskStatus.NOT_STARTED)
			taskStatus.setSelection(0);
		else if (status == TaskStatus.IN_PROGRESS)
			taskStatus.setSelection(1);
		else if (status == TaskStatus.COMPLETE)
			taskStatus.setSelection(2);
		
		if (type==TaskType.SUGGESTED) //if this is part of suggested assignments make spinner uneditable
			taskStatus.setEnabled(false);
		
		TextView priorityView = (TextView)view.findViewById(R.id.taskPriority);
		priorityView.setText(Integer.toString(priority));
		
		populateErgoRisk();
		
		builder.setView(view);
				
		builder.setNegativeButton("Close",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    	dialog.dismiss();
                    }
                });
		builder.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    	
                    	//changing task status so send new task status in bundle
                    	Bundle bundle = TaskInfoDialog.this.getArguments();
                    	int selected=((Spinner)view.findViewById(R.id.task_status_spinner)).getSelectedItemPosition();
                    	TaskStatus status = null;
                    	
                    	if (selected == 0){
                    		status = TaskStatus.NOT_STARTED;
                    	} else if (selected == 1) {
                    		status = TaskStatus.IN_PROGRESS;
                    		
                    	} else if (selected == 2){
                    		status = TaskStatus.COMPLETE;
                    	}
                    	
                    	bundle.putSerializable("status", status);
                    	mListener.onSaveTaskStatusChange(TaskInfoDialog.this);
                    }
		});
		
         AlertDialog dialog = builder.create();
         return dialog;
	}
	/**
	 * Populate ergo risk table in dialog
	 */
	private void populateErgoRisk(){
		TableLayout ergoRiskTable = (TableLayout) view.findViewById(R.id.task_ergo_risk_table);
		Context context = view.getContext();
		for (RiskCategory c: RiskCategory.values()){
			TableRow r = new TableRow(context);
			TextView catText = new TextView(context);
			catText.setText(c.toString() + "     ");
			TextView riskText = new TextView(context);
			
			Double risk = ergoRisk.get(c);
			String riskStr = "";
			if ((risk==null) || (risk==0))
				riskStr = "NA";
			else{
				
				if (risk > 1){
					r.setBackgroundColor(Color.RED);
				}
				DecimalFormat df = new DecimalFormat("#.####");
				riskStr = df.format(risk);
			}		
			riskText.setText(riskStr + " ");
			r.addView(catText);
			r.addView(riskText);
			ergoRiskTable.addView(r);
		}
	}
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the ChangeTaskStatusListener so we can send events to the host
            mListener = (ChangeTaskStatusListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ChangeTaskStatusListener");
        }
    }
    
}
