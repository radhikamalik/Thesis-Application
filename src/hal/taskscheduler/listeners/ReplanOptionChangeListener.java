package hal.taskscheduler.listeners;

import hal.taskscheduler.R;
import hal.taskscheduler.model.Worker;

import java.util.Map;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TextView;

public class ReplanOptionChangeListener implements
		RadioGroup.OnCheckedChangeListener {

	TableLayout preferenceLayout;
	LinearLayout riskAllowanceLayout;
	View view;
	Map<Integer,Worker>scheduledWorkers;
	Map<Integer,Worker>spareWorkers;
	Map<Integer,Worker>workersToPlanWith;

	public ReplanOptionChangeListener(View view,Map<Integer,Worker>scheduledWorkers,
	Map<Integer,Worker>spareWorkers,
	Map<Integer,Worker>workersToPlanWith) {
		this.view = view;
		preferenceLayout = (TableLayout) view
				.findViewById(R.id.task_table_replan);
		riskAllowanceLayout = (LinearLayout) view.findViewById(R.id.risk_allowance_layout);
		this.scheduledWorkers=scheduledWorkers;
		this.spareWorkers = spareWorkers;
		this.workersToPlanWith=workersToPlanWith;
	}

	@Override
	public void onCheckedChanged(RadioGroup rg, int id) {
		// first get which option was clicked
		preferenceLayout.setVisibility(View.VISIBLE);
		riskAllowanceLayout.setVisibility(View.VISIBLE);
		Button additionalWorkersSelect = (Button) view
				.findViewById(R.id.additional_workers_select);

		View radioButton = rg.findViewById(id);
		int idx = rg.indexOfChild(radioButton);
		// LinearLayout additionalWorkerLayout = (LinearLayout)
		// view.findViewById(R.id.additional_workers_option);
		LinearLayout numberWorkersLayout = (LinearLayout) view
				.findViewById(R.id.number_workers_layout);
		LinearLayout selectWorkersLayout = (LinearLayout) view
				.findViewById(R.id.select_workers_layout);

		TextView additionalWorkersNumberText = (TextView) view
				.findViewById(R.id.additional_workers_number_text);
		TextView additionalWorkersSelectText = (TextView) view
				.findViewById(R.id.additional_workers_select_text);

		if (idx == 0) {
			selectWorkersLayout.setVisibility(View.GONE);
			numberWorkersLayout.setVisibility(View.GONE);
			additionalWorkersSelect.setVisibility(View.GONE);
			additionalWorkersSelect.setEnabled(false);
		} else if (idx == 1) { // plan with selected additional workers

			selectWorkersLayout.setVisibility(View.VISIBLE);
			numberWorkersLayout.setVisibility(View.GONE);

			additionalWorkersSelectText.setText("Additional Workers:");
			additionalWorkersSelect.setVisibility(View.VISIBLE);
			additionalWorkersSelect.setEnabled(true);

		} else if (idx == 2) { // suggest workers to call in

			selectWorkersLayout.setVisibility(View.VISIBLE);
			numberWorkersLayout.setVisibility(View.VISIBLE);

			additionalWorkersNumberText
					.setText("Maximum No. of Additional Workers: ");
			additionalWorkersSelectText
					.setText("Preferred Workers To Bring In:");
			additionalWorkersSelect.setVisibility(View.VISIBLE);
			additionalWorkersSelect.setEnabled(true);
		}

	}

}
