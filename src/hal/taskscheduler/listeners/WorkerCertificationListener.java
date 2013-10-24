package hal.taskscheduler.listeners;

import hal.taskscheduler.dialogs.CertificationDialog;

import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
/**
 * On click listener for button to view worker medical certifications
 * Opens the .
 * @author RadhikaMalik
 *
 */
public class WorkerCertificationListener implements OnClickListener{

	String restriction;
	
	public WorkerCertificationListener(){
		//this.restriction = restriction;
	}
	
	@Override
	public void onClick(View v) {
		

		Bundle bundle = new Bundle();
		
		//TODO: these are dummy certifications populated for the demo. Need to be changed to 
		// be actual worker certs.
		
		ArrayList<String> certifications = new ArrayList<String>();
		certifications.add("Certification A");
		certifications.add("Certification B");
		bundle.putStringArrayList("certification", certifications);
		
		
		DialogFragment newFragment = new CertificationDialog();
		newFragment.setArguments(bundle);
		newFragment.show(((Activity) v.getContext()).getFragmentManager(), "restriction");

	}

}
