package hal.taskscheduler.listeners;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableLayout;

public class ReplanAcceptListener implements OnClickListener {

	
	TableLayout task_table;
	TableLayout task_table_proposed;
	
	public ReplanAcceptListener(TableLayout task_table,
			TableLayout task_table_proposed) {
		this.task_table = task_table;
		this.task_table_proposed = task_table_proposed;
	}

	@Override
	public void onClick(View v) {
		// replace the content in task_table with that in proposed_table
		

	}

}
