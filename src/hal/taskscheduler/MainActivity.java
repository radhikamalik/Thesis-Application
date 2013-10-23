
package hal.taskscheduler;

import hal.taskscheduler.R;
import hal.taskscheduler.dialogs.ReplanDialog.ExecuteReplanListener;
import hal.taskscheduler.listeners.ReplanListener;
import hal.taskscheduler.listeners.TaskListener;
import hal.taskscheduler.listeners.TaskListener.TaskType;
import hal.taskscheduler.listeners.WorkerAvailabilityListener;
import hal.taskscheduler.listeners.WorkerCertificationListener;
import hal.taskscheduler.listeners.WorkerMedicalRestrictionListener;
import hal.taskscheduler.model.Allocator;
import hal.taskscheduler.model.RiskCategory;
import hal.taskscheduler.model.Task;
import hal.taskscheduler.model.TaskStatus;
import hal.taskscheduler.model.Worker;
import hal.taskscheduler.model.WorkerAvailability;
import hal.taskscheduler.model.WorkerType;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * This is the main activity class for the Android application
 * @author RadhikaMalik
 *
 */

public class MainActivity extends Activity
		implements
		hal.taskscheduler.dialogs.AvailabilityDialog.ChangeAvailabilityListener,
		hal.taskscheduler.dialogs.TaskInfoDialog.ChangeTaskStatusListener,
		ExecuteReplanListener {

	Allocator alloc; //the data model
	
	TableLayout worker_table;
	TableLayout task_table;
	TableLayout task_table_proposed;
	TableLayout spare_worker_table;
	TableLayout ergo_risk_table;
	TableLayout ergo_risk_table_proposed;
	
	WorkerAvailabilityListener w_listener;
	LinearLayout replanAcceptReject;
	LinearLayout taskStatisticsLayoutProposed;
	FrameLayout proposedTaskStatisticsView;

	float scale;

	Map<String, List<Integer>> suggestedWorkers;
	private LinearLayout ergoRiskStatisticsLayoutProposed;
	private FrameLayout taskStatisticsAll;
	static int hours = 8; // assuming 8 hour shift
	
	int sortOption = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		scale = getResources().getDisplayMetrics().density;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		alloc = new Allocator();
		alloc.loadData(this.getAssets());

		// fill the worker table
		worker_table = (TableLayout) findViewById(R.id.worker_table);
		fillWorkerTable(worker_table, alloc.getWorkers());

		// fill the spare worker table
		spare_worker_table = (TableLayout) findViewById(R.id.spare_worker_table);
		fillWorkerTable(spare_worker_table, alloc.getSpareWorkers());

		task_table = (TableLayout) findViewById(R.id.task_table);
		task_table_proposed = (TableLayout) findViewById(R.id.task_table_proposed);

		// fill the tasks table
		Map<String, List<Integer>> originalAssignment = alloc
				.getTaskWorkerAssignmentsOriginal();
		fillTaskTable(task_table, originalAssignment, TaskType.ORIGINAL);

		fillTaskTableManipButtons();

		// fill shift details
		fillShiftDetails(task_table);

		// fill table of task statistics
		taskStatisticsAll = (FrameLayout) findViewById(R.id.tasks_completed_text_all);

		fillTaskStatistics(taskStatisticsAll, alloc.getTotalNumberOfTask(),
				alloc.getNumberOfTasksCompletelyStaffed(originalAssignment));

		// fill table of ergo risk statistics
		ergo_risk_table = (TableLayout) findViewById(R.id.ergo_risk_category);
		fillErgoRiskStatistics(ergo_risk_table,
				alloc.getNumberOfHighRiskWorkersOriginal());

		ergo_risk_table_proposed = (TableLayout) findViewById(R.id.ergo_risk_category_proposed);

		Button replanButton = (Button) findViewById(R.id.replan_button);
		replanButton.setOnClickListener(new ReplanListener(alloc, alloc
				.getShiftStartTime(), alloc.getShiftEndTime(), hours));

	}
	/**
	 * Function to fill in an ergo risk statistics table given a risk map
	 * @param table
	 * @param riskMap
	 */
	private void fillErgoRiskStatistics(TableLayout table,
			Map<RiskCategory, Integer> riskMap) {
		// go through each available category and add a row with number of high
		// risk workers
		table.removeAllViews();
		for (RiskCategory r : riskMap.keySet()) {
			TableRow row = new TableRow(this);
			TextView t1 = new TextView(this);
			t1.setText(r.toString() + " ");
			t1.setTextSize(12);
			row.addView(t1);
			Button b1 = new Button(this);
			Integer numHighRisk = riskMap.get(r);
			if (numHighRisk > 0) {
				b1.setText(numHighRisk.toString());
				b1.setTextSize(12);
				b1.setPadding(0, 0, 0, 0);

				int width = 20 * numHighRisk;
				FrameLayout f = new FrameLayout(this);
				f.addView(b1);
				f.setPadding(1, 1, 1, 1);
				row.addView(f);
				int heightDp = (int) (15 * scale + 0.5f);
				int widthDp = (int) (width * scale + 0.5f);

				ViewGroup.LayoutParams params = b1.getLayoutParams();
				params.height = heightDp;
				params.width = widthDp;
				b1.setBackgroundColor(Color.RED);
				b1.setLayoutParams(params);
			}
			TableLayout.LayoutParams rowParams = new TableLayout.LayoutParams(
					TableRow.LayoutParams.WRAP_CONTENT,
					TableRow.LayoutParams.WRAP_CONTENT);
			table.addView(row, rowParams);
		}

	}

	/**
	 * Function to fill in an task statistics layout given the num of total tasks and 
	 * num fully staffed.
	 * @param fLayout
	 * @param totalNumberOfTasks
	 * @param numberOfTasksFullyStaffed
	 */
	private void fillTaskStatistics(FrameLayout fLayout,
			int totalNumberOfTasks, int numberOfTasksFullyStaffed) {


		String result = numberOfTasksFullyStaffed + "/" + totalNumberOfTasks;
		Button b = new Button(this);
		b.setText(result);
		b.setTextSize(10);
		b.setPadding(0, 0, 0, 0);
		b.setBackgroundColor(Color.LTGRAY);
		fLayout.addView(b);
		ViewGroup.LayoutParams params = b.getLayoutParams();
		int heightDp = (int) (25 * scale + 0.5f);
		int width = (int) (150 * numberOfTasksFullyStaffed * 1.0 / totalNumberOfTasks);
		int widthDp = (int) (width * scale + 0.5f);
		params.height = heightDp;
		params.width = widthDp;

	}
	/**
	 * Method to fill in shift details at the top
	 * @param taskTable
	 */
	private void fillShiftDetails(TableLayout taskTable) {

		Date shiftStartTime = alloc.getShiftStartTime();
		Date shiftEndTime = alloc.getShiftEndTime();

		DateFormat dateFormat = new SimpleDateFormat("h:mm a");

		String startTimeStr = dateFormat.format(shiftStartTime);
		String endTimeStr = dateFormat.format(shiftEndTime);

		// set the title bar text to shift schedule details
		this.setTitle("Shift schedule " + startTimeStr + " to " + endTimeStr);

		// set task table headings

		Integer[] originalIds = { R.id.time_1, R.id.time_2, R.id.time_3,
				R.id.time_4, R.id.time_5, R.id.time_6, R.id.time_7,
				R.id.time_8, R.id.time_9 };
		Integer[] suggestedIds = { R.id.time_1_proposed, R.id.time_2_proposed,
				R.id.time_3_proposed, R.id.time_4_proposed,
				R.id.time_5_proposed, R.id.time_6_proposed,
				R.id.time_7_proposed, R.id.time_8_proposed,
				R.id.time_9_proposed };
		Integer[] ids = null;
		if (taskTable == this.task_table) {
			ids = originalIds;
		} else if (taskTable == this.task_table_proposed) {
			ids = suggestedIds;
		}

		for (int i = 0; i < hours + 1; i++) {

			Calendar cal = Calendar.getInstance();
			cal.setTime(shiftStartTime);
			cal.add(Calendar.HOUR_OF_DAY, i);
			Date newDate = cal.getTime();

			dateFormat = new SimpleDateFormat("h:mm");
			String dateStr = dateFormat.format(newDate);

			TextView userText = (TextView) findViewById(ids[i]);
			userText.setText(dateStr + " ");

		}

	}
	/**
	 * Fill in a worker table given a worker map
	 * @param workerMap
	 * @param workerTable
	 */
	private void fillWorkerTable(TableLayout workerTable, Map<Integer, Worker> workerMap) {
		workerTable.removeAllViews();
		TableRow header = new TableRow(this);

		header.setLayoutParams(new TableRow.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT));
		TextView h1 = new TextView(this);
		h1.setText("Worker");
		header.addView(h1);
		h1 = new TextView(this);
		h1.setText("Availability");
		header.addView(h1);
		h1 = new TextView(this);
		h1.setText("Medical Restrictions");
		header.addView(h1);
		h1 = new TextView(this);
		h1.setText("Certifications");
		header.addView(h1);
		workerTable.addView(header, new TableLayout.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT));

		for (Integer wid : workerMap.keySet()) {
			TableRow row = new TableRow(this);
			row.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));

			// fill in worker name
			TextView t1 = new TextView(this);
			Worker w = workerMap.get(wid);
			t1.setText(w.getWorkerName());
			t1.setTextAppearance(this, R.style.WorkerNameFont);

			row.addView(t1);

			// fill in worker availability edit buttons
			Button b = new Button(this);
			// b.setId(wid);
			// b.setOnClickListener(new
			// WorkerAvailabilityListener(wid,alloc.getWorkerAvailability(wid),alloc.getWorkerStartTime(wid),alloc.getWorkerEndTime(wid)));
			b.setOnClickListener(new WorkerAvailabilityListener(alloc
					.getWorker(wid)));

			WorkerAvailability avail = w.getAvailability();
			assert (avail != null);
			if (avail == WorkerAvailability.FULL_SHIFT_AVAILABLE) {
				b.setText("Full");
				b.setBackgroundResource(R.layout.avail_button_full_layout);
			} else if (avail == WorkerAvailability.FULL_SHIFT_UNAVAILABLE) {
				b.setText("None");
				row.setBackgroundColor(Color.LTGRAY);
				b.setBackgroundResource(R.layout.avail_button_unavail_layout);
			} else if (avail == WorkerAvailability.PART_SHIFT) {
				b.setText("Part");
				b.setBackgroundResource(R.layout.avail_button_part_layout);
			}
			b.setTextAppearance(this, R.style.AvailabilityButtonFontStyle);
			b.setPadding(0, 0, 0, 0);
			row.addView(b);
			TableLayout.LayoutParams rowParams = new TableLayout.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT);
			// rowParams.setMargins(0, 0, 0, 0);

			// setting button size
			// scale = getResources().getDisplayMetrics().density;
			int heightDp = (int) (18 * scale + 0.5f);
			int widthDp = (int) (25 * scale + 0.5f);

			ViewGroup.LayoutParams params = b.getLayoutParams();
			params.height = heightDp;
			params.width = widthDp;
			b.setLayoutParams(params);

			List<String> workerRestr = alloc.getWorkerMedicalRestrn(wid);
			if (workerRestr.size() > 0) { // worker has some restriction

				String restr_str = "";
				for (String restr : workerRestr) {
					restr_str += restr + '\n';
				}

				Button b2 = new Button(this);
				row.addView(b2);
				params = b2.getLayoutParams();
				params.height = heightDp;
				params.width = widthDp;
				b2.setLayoutParams(params);
				b2.setPadding(0, 0, 0, 0);
				b2.setTextAppearance(this, R.style.AvailabilityButtonFontStyle);
				b2.setText("View");

				b2.setOnClickListener(new WorkerMedicalRestrictionListener(
						restr_str));
			

			} else {
				TextView t2 = new TextView(this);
				row.addView(t2);
				params = t2.getLayoutParams();
				params.height = heightDp;
				params.width = widthDp;
				t2.setLayoutParams(params);
				t2.setPadding(0, 0, 0, 0);
				t2.setTextAppearance(this, R.style.AvailabilityButtonFontStyle);
				t2.setText("");
			}
			//add button to view certs
			b = new Button(this);
			row.addView(b);
			params = b.getLayoutParams();
			params.height = heightDp;
			params.width = widthDp;
			b.setLayoutParams(params);
			b.setPadding(0, 0, 0, 0);
			b.setTextAppearance(this, R.style.AvailabilityButtonFontStyle);
			b.setText("View");
			b.setOnClickListener(new WorkerCertificationListener());

			workerTable.addView(row, rowParams);

		}
	}
	/**
	 * Fill the task table given the worker task assignments and the type of table being populated
	 * @param tableLayout
	 * @param workerTaskAssignment
	 * @param type :ORIGINAL or SUGGESTED
	 */
	private void fillTaskTable(TableLayout tableLayout,
			Map<String, List<Integer>> workerTaskAssignment, TaskType type) {

		TableRow heading = null;
		TableRow heading2 = null;

		if (type == TaskType.ORIGINAL) {
			heading = (TableRow) this.findViewById(R.id.task_heading_row);
			heading2 = (TableRow) this.findViewById(R.id.task_heading_row_2);
		} else if (type == TaskType.SUGGESTED) {
			heading = (TableRow) this
					.findViewById(R.id.task_heading_row_proposed);
			heading2 = (TableRow) this
					.findViewById(R.id.task_heading_row_proposed_2);

		}
		tableLayout.removeAllViews();

		tableLayout.addView(heading, new TableLayout.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT));
		tableLayout.addView(heading2, new TableLayout.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT));

		Map<Integer, Worker> workerMap = alloc.getWorkers();
		Map<String, Task> taskMap = alloc.getTasks();
		for (String tid : taskMap.keySet()) {

			TableRow row = new TableRow(this);
			row.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			Task t = taskMap.get(tid);
			TaskStatus status = t.getStatus();
			if (status == TaskStatus.COMPLETE)
				row.setBackgroundColor(Color.GRAY);
			else if (status == TaskStatus.IN_PROGRESS)
				row.setBackgroundColor(Color.LTGRAY);

			Button taskName = new Button(this);
			double maxRisk = t.getMaxRiskFromCategories(Arrays
					.asList(RiskCategory.values()));

			if (maxRisk > 1) {
				taskName.setBackgroundResource(R.layout.task_button_highrisk_layout);
			} else {
				taskName.setBackgroundResource(R.layout.task_button_normal_layout);

			}

			taskName.setText(t.getTaskId());
			taskName.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			taskName.setTextAppearance(this, R.style.TaskButtonFontStyle);
			taskName.setPadding(0, 0, 0, 0);
			row.addView(taskName);
			row.setPadding(1, 0, 0, 1);

			int taskButtonHeight = (int) (20 * scale + 0.5f);
			ViewGroup.LayoutParams taskButtonParams = taskName
					.getLayoutParams();
			taskButtonParams.height = taskButtonHeight;

			taskName.setOnClickListener(new TaskListener(t, type));

			long tStartTime = t.getStartTime().getTime();
			long tEndTime = t.getEndTime().getTime();
			// long tDuration = tStartTime.getTime()-tEndTime.getTime();

			long shiftStartTime = alloc.getShiftStartTime().getTime();

			int taskStartIndex = (int) ((tStartTime - shiftStartTime) / (3600 * 1000));
			int taskEndIndex = (int) ((tEndTime - shiftStartTime) / (3600 * 1000));

			for (int i = 0; i < hours + 1; i++) {
				TextView view = new TextView(this);

				if ((i >= taskStartIndex) && (i < taskEndIndex))
					view.setBackgroundColor(Color.BLUE);
				// view.setText("hello");
				view.setLayoutParams(new TableRow.LayoutParams(
						TableRow.LayoutParams.MATCH_PARENT,
						TableRow.LayoutParams.WRAP_CONTENT));
				row.addView(view);
			}

			TextView t1;
			t1 = new TextView(this);
			Integer numReq = t.getWorkerReq();
			t1.setText(numReq.toString());
			t1.setGravity(Gravity.CENTER_HORIZONTAL);
			row.addView(t1);

			List<Integer> workersAssigned = workerTaskAssignment.get(tid);

			t1 = new TextView(this);
			Integer numAssigned = workersAssigned.size();
			t1.setText(numAssigned.toString());
			t1.setGravity(Gravity.CENTER_HORIZONTAL);
			row.addView(t1);

			LinearLayout r = new LinearLayout(this);

			for (int wId : workersAssigned) {
				Button b = new Button(this);
				int heightDp = (int) (18 * scale + 0.5f);
				double workerMaxRisk;
				if (type == TaskType.ORIGINAL)
					workerMaxRisk = alloc.getMaxErgoRiskForWorkerOriginal(wId);
				else
					workerMaxRisk = alloc.getMaxErgoRiskForWorkerSuggested(wId);
				if (workerMaxRisk > 1) {
					b.setBackgroundColor(Color.RED);

				} else {
					b.setBackgroundColor(Color.WHITE);
				}
				b.setPadding(0, 0, 0, 0);
				b.setText(workerMap.get(wId).getWorkerName());
				r.addView(b);
				b.setTextAppearance(this, R.style.WorkerButtonFontStyle);

				ViewGroup.LayoutParams params = b.getLayoutParams();
				params.height = heightDp;
			}

			// add blank buttons for more workers required.
			int numMoreWorkersReqd = numReq - numAssigned;
			for (int i = 0; i < numMoreWorkersReqd; i++) {
				Button b = new Button(this);
				b.setText("");
				r.addView(b);
				b.setTextAppearance(this, R.style.WorkerButtonFontStyle);
				int heightDp = (int) (25 * scale + 0.5f);
				ViewGroup.LayoutParams params = b.getLayoutParams();
				params.height = heightDp;

				b.setLayoutParams(params);
			}
			row.addView(r);

			tableLayout.addView(row, new TableLayout.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			tableLayout.setVisibility(View.VISIBLE);
		}
	}
	/**
	 * One option we need to include is abilitites to sort/filter tasks.
	 * Although the functionality is not implemented in this prototype, the buttons are
	 * put in so that in the demo users can see that this option will be available.
	 */
	private void fillTaskTableManipButtons() {
		Button taskSorter = (Button) this.findViewById(R.id.task_sort_button);
		
		taskSorter.setOnClickListener(new OnClickListener() {

			
			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				String[] sortOptions = new String[] { "Start Time", "Priority",
						"Worker Requirement" };
				
				
				int selected = sortOption; 
				builder.setSingleChoiceItems(
						sortOptions, 
			                  selected, 
			                  new DialogInterface.OnClickListener() {
			               
			              @Override
			              public void onClick(DialogInterface dialog,int which) {
			               sortOption=which;
			               sortTasks(sortOption);
			               dialog.dismiss();
			              }
			          });
			          AlertDialog alert = builder.create();
			          alert.show();
			}
		});
	}
	private void sortTasksByStartTime(){
		 //TODO: implement in later version!
	}
	public void sortTasks(int sortOption){
		switch(sortOption){
		case 0: sortTasksByStartTime();
		}
		// TODO: add more here in later version!
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	/**
	 * Method called when "Save" button is pressed on "Availability Dialog" to save
	 * changed worker availability
	 */
	@Override
	public void onSaveChangeAvailability(DialogFragment dialog) {

		Bundle bundle = dialog.getArguments();
		int workerId = bundle.getInt("workerId");
		WorkerAvailability availability = (WorkerAvailability) bundle
				.getSerializable("availability");

		Date startTime = null;
		Date endTime = null;

		if (availability == WorkerAvailability.PART_SHIFT) { // part time
																// availability!
			String fromHr = bundle.getString("fromHr");
			String toHr = bundle.getString("toHr");
			String fromMin = bundle.getString("fromMin");
			String toMin = bundle.getString("toMin");
			String fromAmPm = bundle.getString("fromAmPm");
			String toAmPm = bundle.getString("toAmPm");
			String startTimeStr = fromHr + ":" + fromMin + ":" + fromAmPm;
			String endTimeStr = toHr + ":" + toMin + ":" + toAmPm;
			DateFormat df = new SimpleDateFormat("hh:mm:a");
			try {
				startTime = df.parse(startTimeStr);
				endTime = df.parse(endTimeStr);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		alloc.setWorkerAvailability(workerId, availability, startTime, endTime);
		alloc.removeWorker(workerId);

		TableLayout tableToUpdate;

		WorkerType type = alloc.getWorkerType(workerId);
		Map<Integer, Worker> workerMap;
		if (type == WorkerType.SCHEDULED) {
			tableToUpdate = worker_table;
			workerMap = alloc.getWorkers();
		} else { // type == WorkerType.SPARE
			tableToUpdate = spare_worker_table;
			workerMap = alloc.getSpareWorkers();
		}

		this.fillWorkerTable(tableToUpdate, workerMap);

		this.fillTaskTable(task_table,
				alloc.getTaskWorkerAssignmentsOriginal(), TaskType.ORIGINAL);
		fillTaskStatistics(taskStatisticsAll, alloc.getTotalNumberOfTask(),
				alloc.getNumberOfTasksCompletelyStaffed(alloc
						.getTaskWorkerAssignmentsOriginal()));
		fillErgoRiskStatistics(ergo_risk_table,
				alloc.getNumberOfHighRiskWorkersOriginal());
		dialog.dismiss();
	}
	/**
	 * Method called when "Save" button is pressed to save a task status change in TaskInfoDialog
	 */
	@Override
	public void onSaveTaskStatusChange(DialogFragment dialog) {
		Bundle bundle = dialog.getArguments();
		String taskId = bundle.getString("taskId");
		TaskStatus status = (TaskStatus) bundle.getSerializable("status");

		alloc.setTaskStatus(taskId, status);
		fillTaskTable(task_table, alloc.getTaskWorkerAssignmentsOriginal(),
				TaskType.ORIGINAL);
	}

	/**
	 * Method called when the "Save" button is pressed on the ReplanDialog to re-plan for new assignments
	 */
	@Override
	public void onExecuteReplan(DialogFragment dialog) {
		Bundle bundle = dialog.getArguments();
		Map<String, List<Integer>> preferredWorkers = (Map<String, List<Integer>>) bundle
				.getSerializable("preferredWorkers");

		System.out.println("preferredWorkers: " + preferredWorkers);
		// List<RiskCategory> constrainedCategories = new
		// ArrayList<RiskCategory>();
		List<RiskCategory> constrainedCategories = (List<RiskCategory>) bundle
				.getSerializable("constrainedCategories");
		// System.out.println("constrined: " + constrainedCategories);
		alloc.setConstrainedCategories(constrainedCategories);
		
		
		alloc.assignWorkersToTasks(preferredWorkers);

		suggestedWorkers = alloc.getTaskWorkerAssignmentsSuggested();

		fillTaskTable(task_table_proposed, suggestedWorkers, TaskType.SUGGESTED);
		this.fillShiftDetails(task_table_proposed);

		proposedTaskStatisticsView = (FrameLayout) findViewById(R.id.tasks_completed_text_all_proposed);
		fillTaskStatistics(proposedTaskStatisticsView,
				alloc.getTotalNumberOfTask(),
				alloc.getNumberOfTasksCompletelyStaffed(suggestedWorkers));
		taskStatisticsLayoutProposed = (LinearLayout) findViewById(R.id.schedule_statistics_proposed);
		taskStatisticsLayoutProposed.setVisibility(View.VISIBLE);

		ergoRiskStatisticsLayoutProposed = (LinearLayout) findViewById(R.id.ergo_risk_layout);
		this.fillErgoRiskStatistics(ergo_risk_table_proposed,
				alloc.getNumberOfHighRiskWorkersSuggested());
		ergoRiskStatisticsLayoutProposed.setVisibility(View.VISIBLE);

		replanAcceptReject = (LinearLayout) this
				.findViewById(R.id.replan_accept_reject);
		replanAcceptReject.setVisibility(View.VISIBLE);

		Button replanAcceptButton = (Button) this
				.findViewById(R.id.accept_replan_button);
		replanAcceptButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// fill task table again
				fillTaskTable(task_table, suggestedWorkers, TaskType.ORIGINAL);

				// fill task statistics for original table again
				fillTaskStatistics(taskStatisticsAll, alloc
						.getTotalNumberOfTask(), alloc
						.getNumberOfTasksCompletelyStaffed(suggestedWorkers));

				// fill ergo risk statistics for original table again
				fillErgoRiskStatistics(ergo_risk_table,
						alloc.getNumberOfHighRiskWorkersSuggested());

				// hide proposed stuff
				hideProposed();
			}
		});
		Button replanRejectButton = (Button) this
				.findViewById(R.id.reject_replan_button);
		replanRejectButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				// hide proposed stuff
				hideProposed();
			}
		});
	}
	/**
	 * Hides all tables in the proposed panel.
	 */
	public void hideProposed() {
		task_table_proposed.setVisibility(View.GONE);
		replanAcceptReject.setVisibility(View.GONE);
		taskStatisticsLayoutProposed.setVisibility(View.GONE);
		ergoRiskStatisticsLayoutProposed.setVisibility(View.GONE);

	}

}
