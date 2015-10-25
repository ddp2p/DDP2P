/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.util.ArrayList;

import net.ddp2p.common.data.D_MotionChoice;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
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
    D_Organization org = null;
    String motionName = "Motion";
    //int HEADER_LENGTH = 0;
	
	public final static String sql_all_motions = 
			"SELECT "
					+ net.ddp2p.common.table.motion.motion_ID
			+" FROM "+net.ddp2p.common.table.motion.TNAME+
			" WHERE "+net.ddp2p.common.table.motion.organization_ID + "=? ";
	public static java.util.ArrayList<java.util.ArrayList<Object>> getAllMotions(boolean hide, String crt_enhanced_LID) {
		ArrayList<ArrayList<Object>> moti;
		if (Application.getDB() == null) return new ArrayList<ArrayList<Object>>();
		String sql = sql_all_motions;
		if (hide)	sql	 +=	" AND "+net.ddp2p.common.table.motion.hidden+" != '1' ";
		try {
			if (crt_enhanced_LID != null) {
				sql += " AND " + net.ddp2p.common.table.motion.enhances_ID+" = ?;";
				moti = Application.getDB().select(sql, new String[]{Motion.getOrganizationLIDstr(), Motion.getEnhancedLIDstr()});
			} else {
				moti = Application.getDB().select(sql+";", new String[]{Motion.getOrganizationLIDstr()});
			}
			Log.d("onCreateMotCT", "Motion select asked: "+sql);

			//moti = new ArrayList<ArrayList<Object>>();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return new ArrayList<ArrayList<Object>>();
		}
		Log.d("onCreateMotCT", "Motion select gets: "+moti.size());
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
                String date = motion.getCreationDateStr();
                if (date != null) {
                    _motionTitle[k].mot_date = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + "-" + date.substring(8, 12);
                } else {_motionTitle[k].mot_date = null;/*Util.__("Undated");*/ }
                D_MotionChoice _choices [] = motion.getActualChoices();
                for (int c = 0; c < 3 && c < _choices.length; c ++) {
                    D_Motion.MotionChoiceSupport mcs = motion.getMotionSupport_WithCache(c, true);
                    switch (c) {
                        case 0:
                            _motionTitle[k].choice1 = _choices[c].name + " " + mcs.getCnt() + "/" + mcs.getWeight(); break;
                        case 1:
                            _motionTitle[k].choice2 = _choices[c].name + " " + mcs.getCnt() + "/" + mcs.getWeight(); break;
                        case 2:
                            _motionTitle[k].choice3 = _choices[c].name + " " + mcs.getCnt() + "/" + mcs.getWeight(); break;
                    }
                }
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
        org = D_Organization.getOrgByLID_NoKeep(motion_organization_lid, true);
        if (org == null) {
            finish();
            return;
        }
		reloadMotions();
		actionbar = this.getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		
		//motionTitle = new MotionItem[] { "Should we...", "Should USA..."
		//};
		
		listAdapter = new MotionAdapter(this, R.layout.motion_list, motionTitle);
		setListAdapter(listAdapter);
		
		ListView listview = getListView();
		listview.setDivider(null);

        String[] mname = org.getNamesMotion();
        if (mname != null && mname.length > 0) motionName = mname[0];
        listview.setFooterDividersEnabled(true);
        TextView footerView = new TextView(this);
        footerView.setText(Html.fromHtml("Click the + menu to add a <b>\"" + motionName + "\"</b> item"));
        footerView.setPadding(5, 30, 0, 60);
        listview.addFooterView(footerView);
        footerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addMotion();
            }
        });

        TextView headerView = new TextView(this);
        headerView.setText(org.getName());
        listview.setHeaderDividersEnabled(true);
        listview.addHeaderView(headerView);
        //HEADER_LENGTH ++;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.motion_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case R.id.add_new_motion:
                addMotion();
                /*
                // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
*/
		}
		return super.onOptionsItemSelected(item);
	}

    void addMotion() {
        Toast.makeText(this, "add a new Motion", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent();
        intent.setClass(this, AddMotion.class);
        Bundle b = new Bundle();
        //b.putString(M_MOTION_LID, m.getLIDstr());
        if (org != null) b.putString(Orgs.O_LID, org.getLIDstr());
        intent.putExtras(b);

        startActivity(intent);
    }

	@Override
	public void onListItemClick(ListView list, View v, int _position, long id) {
		int position = _position - this.getListView().getHeaderViewsCount();// HEADER_LENGTH;
        if (position < 0) return;
        Object o = getListView().getItemAtPosition(_position);
        if (o == null) o = "None";
		Toast.makeText(this, o.toString(), Toast.LENGTH_SHORT).show();
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
				TextView content = (TextView) v // WebView
						.findViewById(R.id.motion_list_body);
				TextView choice1 = (TextView) v
						.findViewById(R.id.motion_list_choice1);
				TextView choice2 = (TextView) v
						.findViewById(R.id.motion_list_choice2);
				TextView choice3 = (TextView) v
						.findViewById(R.id.motion_list_choice3);
                if (item.mot_date == null) {
                    dateAndTime.setText("Undated");//"2015-1-4-1519");
                    dateAndTime.setVisibility(View.INVISIBLE);
                } else {
                    dateAndTime.setVisibility(View.VISIBLE);
                    dateAndTime.setText(item.mot_date);//"2015-1-4-1519");
                }
				title.setText(item.mot_name);
                String text = ""+Html.fromHtml(item.body);
				if (text.length() <= 200) {
					//content.loadData(item.body, "text/html",null);
					content.setText(Html.fromHtml(item.body));
				} else {

					//content.loadData(item.body.substring(0, 200) + "...", "text/html", null);
                    content.setText(text.substring(0, 200) + "...");
				}



                if (item.choice1 != null) {choice1.setText(item.choice1); choice1.setVisibility(View.VISIBLE);} else {choice1.setText(""); choice1.setVisibility(View.INVISIBLE);}
                if (item.choice2 != null) {choice2.setText(item.choice2); choice2.setVisibility(View.VISIBLE);} else {choice2.setText(""); choice2.setVisibility(View.INVISIBLE);}
                if (item.choice3 != null) {choice3.setText(item.choice3); choice3.setVisibility(View.VISIBLE);} else {choice3.setText(""); choice3.setVisibility(View.INVISIBLE);}
			}

			return v;
		}

	}
}
