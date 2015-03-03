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

import util.Util;
import ASN1.Encoder;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import data.D_Justification;
import data.D_Motion;
import data.D_MotionChoice;
import data.D_Vote;

public class ViewVotes extends ExpandableListActivity {

	private static final boolean DEBUG = false;
	private static final String TAG = "viewvotes";
	private static final int ALL = 1;
	private static final int ENDORSE = 2;
	private static final int OPPOSE = 3;
	private static final int ABSTAIN = 4;
	private ActionBar actionbar;
	private String motionLID;
	private D_Motion motion;
	private String justificationLID;
	private int currentSortOptions;

 	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = this.getIntent();
		Bundle b = intent.getExtras();

		if (b != null) {
			motionLID = b.getString(Motion.M_MOTION_LID);
			if (motionLID != null) {
				motion = D_Motion.getMotiByLID(motionLID, true, false);
				justificationLID = b.getString(Motion.J_JUSTIFICATION_LID);
			}
		}
		if (motion == null) {
			Toast.makeText(this, "No motion!", Toast.LENGTH_SHORT).show();
			return;
		}

		actionbar = this.getActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setTitle("VOTES");

		ArrayList<D_Vote> voters;
		if (this.justificationLID == null)
			voters = D_Vote.getVotes(D_Vote.getListOfVoters(
					Util.lval(motionLID), 0, 0));
		else
			voters = D_Vote.getVotes(D_Vote.getListOfVoters(
					Util.lval(motionLID), Util.lval(justificationLID), 0, 0));

		String[] data = new String[voters.size()];
		String[] justs = new String[voters.size()];
		String[] dates = new String[voters.size()];

		for (int k = 0; k < voters.size(); k++) {
			D_Vote dv = voters.get(k);
			if (dv == null) {
				data[k] = "Unknown Voter";
				continue;
			} else {
				data[k] = "\"" + dv.getConstituentNameOrMy() + "\"";
				if (data[k] == null)
					data[k] = "No Name: "
							+ Encoder.getGeneralizedTime(dv.getArrivalDate());
			}
			D_MotionChoice choices[] = motion.getActualChoices();
			int ch = Util.ival(dv.getChoice(), 0);
			if (ch >= 0 && ch < choices.length) {
				String j = "";
				if (dv.getJustificationLID() > 0) {
					D_Justification justif = D_Justification.getJustByLID(
							dv.getJustificationLID(), true, false);
					if (justif != null) {
						j = //" with "+
                                "\"" + justif.getTitleStrOrMy() + "\"";
						justs[k] = j;
						dates[k] = Util.renderNicely(dv.getCreationDate(), Util.CALENDAR_SPACED);//"date";
					}
				} else {
                    justs[k] = null;
                    dates[k] =  Util.renderNicely(dv.getCreationDate(), Util.CALENDAR_SPACED);//"date";
                }
				data[k] += " -> " + "\"" + choices[ch] + "\"";
			} else
				data[k] += " -> \"unknown choice: \"" + ch;
		}

		ExpandableListView expandableList = getExpandableListView();
		expandableList.setDividerHeight(2);
		expandableList.setGroupIndicator(null);
		expandableList.setClickable(true);
		VoteExpandableAdapter adapter = new VoteExpandableAdapter(data, justs,
				dates);

		adapter.setInflater(
				(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE),
				this);

		expandableList.setAdapter(adapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_votes_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem Item = menu.findItem(R.id.view_votes_sort);
		Log.d(TAG, String.valueOf(currentSortOptions));
		switch (currentSortOptions) {
		case ALL:
			Item.setTitle("All");
			break;

		case ENDORSE:
			Item.setTitle("Endorse");
			break;
		case OPPOSE:
			Item.setTitle("Oppose");
			break;
		case ABSTAIN:
			Item.setTitle("Abstain");
			break;

		default:
			Item.setTitle("Sort");
			break;
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.view_votes_sort_all:
			currentSortOptions = ALL;
			return true;

		case R.id.view_votes_sort_endorse:
			currentSortOptions = ENDORSE;
			return true;

		case R.id.view_votes_sort_oppose:
			currentSortOptions = OPPOSE;
			return true;

		case R.id.view_votes_sort_abstain:
			currentSortOptions = ABSTAIN;
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public class VoteExpandableAdapter extends BaseExpandableListAdapter {

		private Activity activity;
		private LayoutInflater inflater;
		private String[] votes;
		private String[] justs;
		private String[] dates;

		public VoteExpandableAdapter(String[] votes, String[] justs,
				String[] dates) {
			this.votes = votes;
			this.justs = justs;
			this.dates = dates;
		}

		public void setInflater(LayoutInflater inflater, Activity activity) {
			this.inflater = inflater;
			this.activity = activity;
		}

		@Override
		public int getGroupCount() {
			// TODO Auto-generated method stub
			return votes.length;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			// TODO Auto-generated method stub
			return 1;
		}

		@Override
		public Object getGroup(int groupPosition) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getGroupId(int groupPosition) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.view_votes_parent_view,
						null);
			}

			TextView constituent_name = (TextView) convertView
					.findViewById(R.id.view_votes_parent_view_constituent_name);

			constituent_name.setText(votes[groupPosition]);

			return convertView;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.view_votes_child_view,
						null);
			}

			TextView justification = (TextView) convertView
					.findViewById(R.id.view_votes_child_justificaion);

            String just = justs[groupPosition];
            if (just != null) {
                justification.setText(just);
                justification.setVisibility(View.VISIBLE);
            } else {
                justification.setVisibility(View.GONE);
            }
			TextView date = (TextView) convertView
					.findViewById(R.id.view_votes_child_date);

			date.setText(dates[groupPosition]);

			return convertView;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return false;
		}

	}
}
