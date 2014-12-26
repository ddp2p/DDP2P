/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package com.HumanDecisionSupportSystemsLaboratory.DDP2P;

import hds.Address;

import java.util.ArrayList;

import util.DD_DirectoryServer;
import util.DirectoryAddress;
import util.Util;
import ASN1.Encoder;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import config.DD;
import config.Identity;
import data.D_Justification;
import data.D_Motion;
import data.D_MotionChoice;
import data.D_Vote;

public class ViewVotes extends ListActivity {

	private static final boolean DEBUG = false;
	private String[] name = { "a", "b", "c"};
	
	private String[] votes = {"support", "against", "support"};
	private int checkedPos[];
	private ActionBar actionbar;

	private ArrayAdapter<String> adapter;
	private String motionLID;
	private D_Motion motion;
	private String justificationLID;

	@Override
	public void onCreate(Bundle savedInstanceState) {
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
		if (motion == null){
			Toast.makeText(this, "No motion!", Toast.LENGTH_SHORT).show();
			return;
		}
		/*
		String[] data = {"","",""};
		for (int i = 0; i < name.length; i ++) {
			data[i] = name[i] + "   votes on  " + votes[i];
		}
		*/
		
		ArrayList<D_Vote> voters;
		if (this.justificationLID == null)
			voters = D_Vote.getVotes(D_Vote.getListOfVoters(Util.lval(motionLID), 0, 0));
		else
			voters = D_Vote.getVotes(D_Vote.getListOfVoters(Util.lval(motionLID), Util.lval(justificationLID), 0, 0));
			
		String[] data = new String[voters.size()];
		for (int k = 0; k < voters.size(); k ++) {
			D_Vote dv = voters.get(k);
			if (dv == null) {
				data[k] = "Unknown";
				continue;
			} else {
				data[k] = "\""+dv.getConstituentNameOrMy()+"\"";
				if (data[k] == null) data[k] = "No Name: "+Encoder.getGeneralizedTime(dv.getArrivalDate());
			}
			D_MotionChoice choices[] = motion.getActualChoices();			
			int ch = Util.ival(dv.getChoice(), 0);
			if (ch > 0 && ch < choices.length) {
				String j = "";
				if (dv.getJustificationLID() > 0) {
					D_Justification justif = D_Justification.getJustByLID(dv.getJustificationLID(), true, false);
					if (justif != null) j = " with \""+ justif.getTitleOrMy()+"\"";
				}
				data[k] += " -> " + "\""+choices[k]+"\" with "+j;
			} else
				data[k] += " -> \"unknown\"";
		}
		
		actionbar = this.getActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setTitle("Votes");

		// Getting object reference to listview of main.xml
		ListView listView = getListView();

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, data);

		listView.setAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

	
	}

	@Override
	protected void onResume() {
		super.onResume();
		adapter.setNotifyOnChange(true);

	}

}
