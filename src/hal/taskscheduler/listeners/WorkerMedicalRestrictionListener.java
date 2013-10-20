package hal.taskscheduler.listeners;

import hal.taskscheduler.dialogs.MedicalRestrictionDialog;
import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class WorkerMedicalRestrictionListener implements OnClickListener{

	String restriction;
	
	public WorkerMedicalRestrictionListener(String restriction){
		this.restriction = restriction;
	}
	
	@Override
	public void onClick(View v) {
		
		//int workerId = v.getId();
		//System.out.println("click: " + workerId);
		Bundle bundle = new Bundle();
		bundle.putString("restriction", restriction);
		
		
		DialogFragment newFragment = new MedicalRestrictionDialog();
		newFragment.setArguments(bundle);
		newFragment.show(((Activity) v.getContext()).getFragmentManager(), "restriction");

	}

}
