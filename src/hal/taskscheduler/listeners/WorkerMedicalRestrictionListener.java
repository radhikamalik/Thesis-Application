package hal.taskscheduler.listeners;

import hal.taskscheduler.dialogs.MedicalRestrictionDialog;
import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
/**
 * On click listener for button to view worker medical restrictions.
 * Opens the MedicalRestrictionDialog
 * @author RadhikaMalik
 *
 */
public class WorkerMedicalRestrictionListener implements OnClickListener{

	String restriction;
	
	public WorkerMedicalRestrictionListener(String restriction){
		this.restriction = restriction;
	}
	
	@Override
	public void onClick(View v) {
		
		Bundle bundle = new Bundle();
		bundle.putString("restriction", restriction);
		
		
		DialogFragment newFragment = new MedicalRestrictionDialog();
		newFragment.setArguments(bundle);
		newFragment.show(((Activity) v.getContext()).getFragmentManager(), "restriction");

	}

}
