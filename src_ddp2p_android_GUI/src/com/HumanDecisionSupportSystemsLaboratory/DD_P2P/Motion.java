package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.util.ArrayList;

import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Motion;
public class Motion extends ListActivity {
	public final static String M_MOTION_ID = "motion_ID";
	public final static String M_MOTION_LID = "Motion_LID";
	public final static String M_MOTION_GIDH = "motion_GIDH";
	public final static String M_MOTION_TITLE = "motion_title";
	public final static String M_MOTION_BODY = "motion_body";
	private static final String TAG = "motion";
	/**
	 * Viewing only those enhancing this:
	 */
	public static final String M_MOTION_ENHANCED = "ENHANCED_LID";
	public static final String M_MOTION_CHOICE = "signature_choice";

	protected static final String J_JUSTIFICATION_LID = "Justification_LID";

	public static MotionItem[] motionTitle;
	private ActionBar actionbar = null;
	private static String motion_organization_lid;
	private static String motion_organization_gidh;
	private static String motion_enhanced_lid;
	private static int motion_organization_position;
	public static ListAdapter listAdapter;
	static Motion activ = null;
	
	public final static String sql_all_motions = 
			"SELECT "
					+ net.ddp2p.common.table.motion.motion_ID
			+" FROM "+net.ddp2p.common.table.motion.TNAME+
			" WHERE "+net.ddp2p.common.table.motion.organization_ID + "=? ";
	public static java.util.ArrayList<java.util.ArrayList<Object>> getAllMotions(boolean hide, String crt_enhanced_LID) {
		ArrayList<ArrayList<Object>> moti;
		if (Application.db == null) return new ArrayList<ArrayList<Object>>();
		String sql = sql_all_motions;
		if (hide)	sql	 +=	" AND "+net.ddp2p.common.table.motion.hidden+" != '1' ";
		try {
			if (crt_enhanced_LID != null) {
				sql += " AND " + net.ddp2p.common.table.motion.enhances_ID+" = ?;";
				moti = Application.db.select(sql, new String[]{Motion.getOrganizationLIDstr(), Motion.getEnhancedLIDstr()});
			} else {
				moti = Application.db.select(sql+";", new String[]{Motion.getOrganizationLIDstr()});
			}
			Log.d("onCreateMotCreatingThread", "Motion select asked: "+sql);

			//moti = new ArrayList<ArrayList<Object>>();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return new ArrayList<ArrayList<Object>>();
		}
		Log.d("onCreateMotCreatingThread", "Motion select gets: "+moti.size());
		return moti;
	}
	public static void reloadMotions() {
		MotionItem [] _motionTitle;
		ArrayList<ArrayList<Object>> orgName_alist = getAllMotions(false, null);
		_motionTitle = new MotionItem[orgName_alist.size()];
		for (int k = 0; k < orgName_alist.size(); k++) {
			_motionTitle[k] = new MotionItem();
			ArrayList<Object> org_id_item =  orgName_alist.get(k);
			if (org_id_item == null || org_id_item.size() <= 0) {
				_motionTitle[k].mot_name = Util.__("Motion Position: ")+k;
				continue;
			}
			String oLID = Util.getString(org_id_item.get(0));
			D_Motion motion = D_Motion.getMotiByLID(oLID, true, false);
			if (motion != null) {
				//String name = motion.getTitle();
				_motionTitle[k].motion =  motion; //name;
				_motionTitle[k].mot_name = Util.getString(motion.getMotionTitle().title_document.getDocumentString());
				_motionTitle[k].body = Util.getString(motion.getMotionText().getDocumentString());
			} else {
				_motionTitle[k].mot_name = Util.__("Motion: ")+oLID;
			}
		}
		motionTitle = _motionTitle;		
		//orgName_alist.clear();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = this.getIntent();
		Bundle b = i.getExtras();
		
		//top panel setting
		motion_organization_position = b.getInt(Orgs.O_ID);
		motion_organization_lid = b.getString(Orgs.O_LID);
		motion_organization_gidh = b.getString(Orgs.O_GIDH);
		motion_enhanced_lid = b.getString(Motion.M_MOTION_ENHANCED);
		
		activ = this;
		reloadMotions();
		actionbar = this.getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		
		//motionTitle = new MotionItem[] { "Should we...", "Should USA..."
		//};
		
		listAdapter = new MotionAdapter(this, R.layout.motion_list, motionTitle);
		setListAdapter(listAdapter);
		
		ListView listview = getListView();
		listview.setDivider(null);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.motion_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_new_motion) {
			Toast.makeText(this, "add a new Motion", Toast.LENGTH_SHORT).show();
			
			Intent intent = new Intent();
			intent.setClass(this, AddMotion.class);
			
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}



	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		
		Toast.makeText(this, getListView().getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
		MotionItem[] p = Motion.motionTitle;
		D_Motion m = null;
		try {
			if (position >= p.length) return;
			m = p[position].motion;
		} catch (Exception e) {return;}
		
		Intent intent = new Intent();
		intent.setClass(this, MotionDetail.class);
		Bundle b = new Bundle();
		b.putInt(M_MOTION_ID, position);
		b.putString(M_MOTION_GIDH, m.getGIDH());
		b.putString(M_MOTION_LID, m.getLIDstr());
		b.putString(M_MOTION_TITLE, Util.getString(m.getMotionTitle().title_document.getDocumentString()));
		b.putString(M_MOTION_BODY, Util.getString(m.getMotionText().getDocumentString()));
		intent.putExtras(b);
		startActivity(intent);
	}
	public static long getOrganizationLID() {
		return Util.lval(Motion.motion_organization_lid);
	}
	public static String getOrganizationLIDstr() {
		return Motion.motion_organization_lid;
	}
	public static String getEnhancedLIDstr() {
		return Motion.motion_enhanced_lid;
	}

	
	private class MotionAdapter extends ArrayAdapter<MotionItem> {


		private MotionItem[] motions;

		public MotionAdapter(Context context, int textViewResourceId,
				MotionItem[] motionTitle) {
			super(context,  textViewResourceId, motionTitle);
			this.motions = motionTitle;
		}
		@Override
		public int getCount() {
			return motions.length;
		}

		@Override
		public MotionItem getItem(int position) {
			return motions[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.motion_list, null);
			}


			Log.d(TAG, "motion: item" + motions );
			MotionItem item = motions[position];
			
			Log.d(TAG, "motion: position" + position + " : " + item.mot_name);
			
			if (item != null) {

				TextView dateAndTime = (TextView) v
						.findViewById(R.id.motion_list_date_and_time);
				TextView title = (TextView) v
						.findViewById(R.id.motion_list_name);
				TextView content = (TextView) v
						.findViewById(R.id.motion_list_body);
				TextView choice1 = (TextView) v
						.findViewById(R.id.motion_list_choice1);
				TextView choice2 = (TextView) v
						.findViewById(R.id.motion_list_choice2);
				TextView choice3 = (TextView) v
						.findViewById(R.id.motion_list_choice3);
				dateAndTime.setText("2015-1-4-1519");

				title.setText(item.mot_name);
				if (item.body.length() <= 200) {
					content.setText(item.body);
				} else {
					content.setText(item.body.substring(0, 200) + "...");
				}
				
				//TODO fill this
/*				choice1.setText(text);
				choice2.setText(text);
				choice3.setText(text);*/
			}

			return v;
		}

	}
}
