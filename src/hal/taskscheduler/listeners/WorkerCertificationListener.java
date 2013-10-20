package hal.taskscheduler.listeners;

import java.util.ArrayList;
import java.util.List;

import hal.taskscheduler.dialogs.CertificationDialog;
import hal.taskscheduler.dialogs.MedicalRestrictionDialog;
import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class WorkerCertificationListener implements OnClickListener{

	String restriction;
	
	public WorkerCertificationListener(){
		//this.restriction = restriction;
	}
	
	@Override
	public void onClick(View v) {
		
		//int workerId = v.getId();
		//System.out.println("click: " + workerId);
		Bundle bundle = new Bundle();
		
		ArrayList<String> certifications = new ArrayList<String>();
		certifications.add("Certification A");
		certifications.add("Certification B");
		bundle.putStringArrayList("certification", certifications);
		
		
		DialogFragment newFragment = new CertificationDialog();
		newFragment.setArguments(bundle);
		newFragment.show(((Activity) v.getContext()).getFragmentManager(), "restriction");

	}

}
