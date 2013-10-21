package hal.taskscheduler.dialogs;

import hal.taskscheduler.R;
import hal.taskscheduler.listeners.ReplanOptionChangeListener;
import hal.taskscheduler.listeners.SelectPreferredWorkerListener;
import hal.taskscheduler.model.RiskCategory;
import hal.taskscheduler.model.Task;
import hal.taskscheduler.model.TaskStatus;
import hal.taskscheduler.model.Worker;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ReplanDialog extends DialogFragment {

	View view;
	RadioGroup planOptionRg;
	TableLayout preferenceLayout;
	Spinner additionalWorkersNumberSelect;
	LinearLayout riskAllowanceLayout;

	Map<String, Task> taskMap;

	List<RiskCategory> constrainedCategories;

	int hours;
	Date shiftStartTime;
	Date shiftEndTime;

	Map<String, List<Integer>> preferredWorkers;
	Map<String, List<Integer>> originalAssignments;
	Map<String, List<Integer>> notPreferredWorkers;
	Map<Integer, Worker> workersToCall;
	Map<Integer, Worker> scheduledWorkers;
	Map<Integer, Worker> spareWorkers;
	
	Map<Integer,Worker> workersToPlanWith;

	ExecuteReplanListener mListener;

	public interface ExecuteReplanListener {
		public void onExecuteReplan(DialogFragment dialog);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		Bundle bundle = this.getArguments();

		taskMap = (Map<String, Task>) bundle.getSerializable("taskMap");
		scheduledWorkers = (Map<Integer, Worker>) bundle
				.getSerializable("scheduledWorkers");
		spareWorkers = (Map<Integer, Worker>) bundle
				.getSerializable("spareWorkers");
		shiftStartTime = (Date) bundle.getSerializable("shiftStartTime");
		shiftEndTime = (Date) bundle.getSerializable("shiftEndTime");
		originalAssignments = (Map<String, List<Integer>>) bundle
				.getSerializable("originalAssignments");
		hours = bundle.getInt("hours");
		preferredWorkers = new HashMap<String, List<Integer>>();
		notPreferredWorkers = new HashMap<String, List<Integer>>();
		workersToCall = new HashMap<Integer, Worker>();
		
		workersToPlanWith = new HashMap<Integer,Worker>();
		workersToPlanWith.putAll(scheduledWorkers); //initially only scheduled workers
		
		constrainedCategories= new ArrayList<RiskCategory>(Arrays.asList(RiskCategory.values()));
		//constrainedCategories = new ArrayList<RiskCategory>();

		Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = ((Activity) getActivity())
				.getLayoutInflater();

		view = inflater.inflate(R.layout.replan_dialog_layout, null);
		planOptionRg = (RadioGroup) view.findViewById(R.id.planOptionGroup);

		preferenceLayout = (TableLayout) view
				.findViewById(R.id.task_table_replan);
		populatePreferenceTable(preferenceLayout);

		additionalWorkersNumberSelect = (Spinner) view
				.findViewById(R.id.additional_workers_number_select);
		populateAdditionalWorkers(additionalWorkersNumberSelect);

		Button additionalWorkersSelect = (Button) view
				.findViewById(R.id.additional_workers_select);
		List<Integer> workerIdsToCallIn = new ArrayList<Integer>();
		additionalWorkersSelect
				.setOnClickListener(new SelectPreferredWorkerListener(
						workerIdsToCallIn, new ArrayList<Worker>(
								this.spareWorkers.values()), null));
		final float scale = getResources().getDisplayMetrics().density;
		int heightDp = (int) (25 * scale + 0.5f);
		ViewGroup.LayoutParams params = additionalWorkersSelect
				.getLayoutParams();
		params.height = heightDp;
		params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		additionalWorkersSelect.setPadding(0, 0, 0, 0);
		additionalWorkersSelect.setTextAppearance(view.getContext(),
				R.style.AvailabilityButtonFontStyle);

		riskAllowanceLayout = (LinearLayout) view
				.findViewById(R.id.risk_allowance_layout);
		this.populateRiskAllowanceLayout();

		planOptionRg.setOnCheckedChangeListener(new ReplanOptionChangeListener(
				view, scheduledWorkers, spareWorkers, workersToCall));

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
						System.out.println(ReplanDialog.this.preferredWorkers);
						System.out.println("constrained categories: " + constrainedCategories);
						Bundle bundle = ReplanDialog.this.getArguments();

						bundle.putSerializable(
								"preferredWorkers",
								(Serializable) ReplanDialog.this.preferredWorkers);
						bundle.putSerializable("constrainedCategories", (Serializable) constrainedCategories);

						mListener.onExecuteReplan(ReplanDialog.this);
					}
				});

		AlertDialog dialog = builder.create();
		return dialog;
	}

	private void populateRiskAllowanceLayout() {

		int id = 0;
		for (RiskCategory c : RiskCategory.values()) {
			CheckBox cb = new CheckBox(view.getContext());
			cb.setId(id++);
			cb.setText(c.toString());

			cb.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					int id = v.getId();
					RiskCategory c = RiskCategory.values()[id]; 
						
					if (((CheckBox) v).isChecked()) {
						if (constrainedCategories.contains(c))
						constrainedCategories.remove(c);
					}else{
						if (!constrainedCategories.contains(c))
							constrainedCategories.add(c);
					}

				}
			}

			);

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(450, 0, 0, 0);
			riskAllowanceLayout.addView(cb, params);
			// params.
		}

	}

	private void populateAdditionalWorkers(Spinner additionalWorkersNumberSelect) {
		Integer[] additionalWorkers = new Integer[spareWorkers.size()];

		for (int i = 0; i < spareWorkers.size(); ++i) {
			additionalWorkers[i] = i;

		}
		ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(
				view.getContext(), android.R.layout.simple_spinner_item,
				additionalWorkers);
		additionalWorkersNumberSelect.setAdapter(adapter);
	}

	private void populatePreferenceTable(TableLayout preferenceLayout) {

		Integer[] ids = { R.id.time_1_replan, R.id.time_2_replan,
				R.id.time_3_replan, R.id.time_4_replan, R.id.time_5_replan,
				R.id.time_6_replan, R.id.time_7_replan, R.id.time_8_replan,
				R.id.time_9_replan };

		DateFormat dateFormat = new SimpleDateFormat("h:mm");
		for (int i = 0; i < hours + 1; i++) {

			Calendar cal = Calendar.getInstance();
			cal.setTime(shiftStartTime);
			cal.add(Calendar.HOUR_OF_DAY, i);
			Date newDate = cal.getTime();

			String dateStr = dateFormat.format(newDate);

			TextView userText = (TextView) view.findViewById(ids[i]);
			userText.setText(dateStr + " ");

		}

		Context viewContext = view.getContext();
		for (String tid : taskMap.keySet()) {

			Task t = taskMap.get(tid);
			if (t.getStatus() != TaskStatus.NOT_STARTED) {
				preferredWorkers.put(tid, originalAssignments.get(tid));
			} else {
				TableRow row = new TableRow(viewContext);
				row.setLayoutParams(new TableRow.LayoutParams(
						TableRow.LayoutParams.MATCH_PARENT,
						TableRow.LayoutParams.WRAP_CONTENT));

				TextView taskView = new TextView(viewContext);
				taskView.setTextAppearance(viewContext,
						R.style.ReplanTaskNameStyle);

				taskView.setText(t.getTaskId());
				taskView.setLayoutParams(new TableRow.LayoutParams(
						TableRow.LayoutParams.MATCH_PARENT,
						TableRow.LayoutParams.WRAP_CONTENT));

				row.addView(taskView);
				long tStartTime = t.getStartTime().getTime();
				long tEndTime = t.getEndTime().getTime();

				int taskStartIndex = (int) ((tStartTime - shiftStartTime
						.getTime()) / (3600 * 1000));
				int taskEndIndex = (int) ((tEndTime - shiftStartTime.getTime()) / (3600 * 1000));

				for (int i = 0; i < hours + 1; i++) {
					TextView view = new TextView(viewContext);

					if ((i >= taskStartIndex) && (i < taskEndIndex))
						view.setBackgroundColor(Color.BLUE);
					// view.setText("hello");
					TableRow.LayoutParams timeLayoutParams = new TableRow.LayoutParams(
							TableRow.LayoutParams.MATCH_PARENT,
							TableRow.LayoutParams.WRAP_CONTENT);
					timeLayoutParams.setMargins(0, 2, 0, 2);
					view.setLayoutParams(timeLayoutParams);

					row.addView(view);
				}

				// populate number of workers reqd
				TextView t1;
				t1 = new TextView(viewContext);
				Integer numReq = t.getWorkerReq();
				t1.setText(numReq.toString());
				t1.setGravity(Gravity.CENTER_HORIZONTAL);
				row.addView(t1);

				// text view to display skilled workers
				t1 = new TextView(viewContext);
				Set<Integer> skilledWorkerIds = t.getCertifiedWorkers();
				String skilledWorkerStr = "";
				/*
				for (int workerId : skilledWorkerIds) {
					Worker w = workersToPlanWith.get(workerId);
					if (w != null)
						skilledWorkerStr += w.getWorkerName() + "; ";
				}*/
				t1.setGravity(Gravity.CENTER_HORIZONTAL);
				t1.setText(skilledWorkerStr);
				row.addView(t1);
				t1.setTextAppearance(viewContext, R.style.TaskButtonFontStyle);

				// add button to select preferred workers
				Button selectPrefferedButton = new Button(viewContext);
				// selectPrefferedButton.setId(id)
				selectPrefferedButton.setPadding(0, 0, 0, 0);
				row.addView(selectPrefferedButton);

				List<Integer> preferredWorkerIds = new ArrayList<Integer>();
				this.preferredWorkers.put(tid, preferredWorkerIds);

				selectPrefferedButton
						.setOnClickListener(new SelectPreferredWorkerListener(
								preferredWorkerIds, new ArrayList<Worker>(
										this.scheduledWorkers.values()), tid));
				/*
				 * TableRow.LayoutParams params = new TableRow.LayoutParams(
				 * TableRow.LayoutParams.MATCH_PARENT,
				 * TableRow.LayoutParams.WRAP_CONTENT); params.setMargins(0, 0,
				 * 0, 0); selectPrefferedButton.setLayoutParams(params);
				 * selectPrefferedButton.setText("s");
				 */

				final float scale = getResources().getDisplayMetrics().density;
				int heightDp = (int) (25 * scale + 0.5f);
				ViewGroup.LayoutParams params = selectPrefferedButton
						.getLayoutParams();
				params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
				params.height = heightDp;
				selectPrefferedButton.setLayoutParams(params);

				List<Integer> notPreferredWorkerIds = new ArrayList<Integer>();
				this.notPreferredWorkers.put(tid, notPreferredWorkerIds);
				Button selectNotPrefferedButton = new Button(viewContext);
				// selectPrefferedButton.setId(id)
				selectNotPrefferedButton.setPadding(0, 0, 0, 0);
				row.addView(selectNotPrefferedButton);
				selectNotPrefferedButton
						.setOnClickListener(new SelectPreferredWorkerListener(
								notPreferredWorkerIds, new ArrayList<Worker>(
										this.scheduledWorkers.values()), tid));

				params = selectNotPrefferedButton.getLayoutParams();
				params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
				params.height = heightDp;
				selectNotPrefferedButton.setLayoutParams(params);

				selectPrefferedButton.setTextAppearance(viewContext,
						R.style.AvailabilityButtonFontStyle);
				selectNotPrefferedButton.setTextAppearance(viewContext,
						R.style.AvailabilityButtonFontStyle);

				preferenceLayout.addView(row, new TableLayout.LayoutParams(
						TableRow.LayoutParams.MATCH_PARENT,
						TableRow.LayoutParams.WRAP_CONTENT));
			}

		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the
			// host
			mListener = (ExecuteReplanListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement NoticeDialogListener");
		}
	}

}
