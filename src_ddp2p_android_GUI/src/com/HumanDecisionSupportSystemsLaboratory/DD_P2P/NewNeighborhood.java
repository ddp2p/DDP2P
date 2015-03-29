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

import net.ddp2p.common.config.Identity;

import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OrgParam;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NewNeighborhood extends Activity{

	private static final String TAG = "NewNeighborhood";
	private EditText name, division, subdivision;
	private Button submit; 
	private String _name, _division, _subdivision;
	private int organization_position;
	private String organization_lid;
	private String organization_gidh;
	private String organization_name;
	D_Organization organization;
	private String organization_gid;
	private long oLID;
	private long myself_constituent_LID;
	private D_Constituent myself_constituent;
	private String myself_constituent_GID;
	private String myself_constituent_GIDH;
	private long neighborhood_parent_LID;
	private String neighborhood_parent_LIDstr;
	private D_Neighborhood neighborhood_parent;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.new_neiborhood);
		name = (EditText) findViewById(R.id.new_neiborhood_name);
		division = (EditText) findViewById(R.id.new_neiborhood_division);
		subdivision = (EditText) findViewById(R.id.new_neiborhood_subdivision);
		submit = (Button) findViewById(R.id.new_neiborhood_submit);
		
		Intent i = this.getIntent();
		Bundle b = i.getExtras();
		
		//top panel setting
		organization_position = b.getInt(Orgs.O_ID);
		organization_lid = b.getString(Orgs.O_LID);
		organization_gidh = b.getString(Orgs.O_GIDH);
		organization_gid = b.getString(Orgs.O_GID);
		organization_name = b.getString(Orgs.O_NAME);
		neighborhood_parent_LIDstr = b.getString(Constituent.N_PARENT_LID);
		neighborhood_parent_LID = Util.lval(neighborhood_parent_LIDstr);
		if (neighborhood_parent_LID > 0) {
			neighborhood_parent = D_Neighborhood.getNeighByLID(neighborhood_parent_LIDstr, true, false);
		}
		
		oLID = Util.lval(organization_lid, -1);
    	if (oLID <= 0) {
       		Toast.makeText(this, "No organization selected!", Toast.LENGTH_SHORT).show();
       		finish();
    		return;
    	}

    	try {
    		Identity crt_identity = Identity.getCurrentConstituentIdentity();
    		if (crt_identity == null) {
    			Log.d(TAG, "No identity");
    		} else 
    			myself_constituent_LID = net.ddp2p.common.config.Identity.getDefaultConstituentIDForOrg(oLID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    	
    	if (myself_constituent_LID > 0) {
    		myself_constituent = D_Constituent.getConstByLID(myself_constituent_LID, true, false);
    		myself_constituent_GID = myself_constituent.getGID();
    		myself_constituent_GIDH = myself_constituent.getGIDH();
    		Log.d(TAG, "Got const: "+myself_constituent);
    	} else {
    		Toast.makeText(this, "Register Profile First!", Toast.LENGTH_SHORT).show();
    		Log.d(TAG, "no const: "+myself_constituent);
    		finish();
    		return;
    	}

    	organization = D_Organization.getOrgByLID(organization_lid, true, false);

		if (neighborhood_parent == null) {
			D_OrgParam[] subdivisions = organization.getNeighborhoodSubdivisionsDefault();
			if (subdivisions != null && subdivisions.length > 0) {
				division.setText(subdivisions[0].label);
			
				String subdivisions_of_the_first_division = ":";
				if (subdivisions.length > 1)
					subdivisions_of_the_first_division = subdivisions[1].label;
				for (int k = 2; k < subdivisions.length; k ++) {
					subdivisions_of_the_first_division += ":" + subdivisions[k].label;
				}
				subdivision.setText(subdivisions_of_the_first_division);
			}
		} else {
			String[] subdivisions = neighborhood_parent.getNames_subdivisions();
			if (subdivisions != null && subdivisions.length > 0) {
				division.setText(subdivisions[0]);
				
				String subdivisions_of_the_first_division = ":";
				if (subdivisions.length > 1)
					subdivisions_of_the_first_division = subdivisions[1];
				for (int k = 2; k < subdivisions.length; k ++) {
					subdivisions_of_the_first_division += ":" + subdivisions[k];
				}
				subdivision.setText(subdivisions_of_the_first_division);
			}		
		}
		
		submit.setOnClickListener(new OnClickListener() {
			
			private D_Neighborhood neighborhood;

			@Override
			public void onClick(View v) {
				// TODO add more actions to finish submit
				_name = name.getText().toString();
				_division = division.getText().toString();
				_subdivision = subdivision.getText().toString();
				
				neighborhood = D_Neighborhood.createNeighborhood (organization, _name, _division, _subdivision);
				if (neighborhood_parent != null) {
					neighborhood.setParent_GID(neighborhood_parent.getGID());
					neighborhood.setParentLIDstr(neighborhood_parent_LIDstr);
				}
				neighborhood.setGID(neighborhood.make_ID());
				
				D_Neighborhood n = D_Neighborhood.getNeighByGID(neighborhood.getGID(), true, true, true, null, oLID);
				n.loadRemote(neighborhood, null, null, null);
				long nID = n.storeSynchronouslyNoException();
				n.releaseReference();
				
				Log.d(TAG, "Breated neighborhood: "+neighborhood);
				Toast.makeText(NewNeighborhood.this, "Neighborhood created: "+ _name, Toast.LENGTH_SHORT);
				
				D_Witness w = new D_Witness();
				w.global_organization_ID = organization_gid;
				w.organization_ID = organization_lid;
				w.witnessing_global_constituentID = myself_constituent_GID;
				w.witnessing_constituentID = myself_constituent_LID;
				
				//w.witnessing_global_constituentID = constituent_GID;
				
				//w.witnessed_global_constituentID = constituent_GID;
				w.witnessed_global_neighborhoodID = neighborhood.getGID();
				
				int eligibility_position = 0;
				int trustworthy_position = 0;
				w.arrival_date = w.creation_date = Util.CalendargetInstance();
				w.sense_y_n = D_Witness.witness_categories_sense[eligibility_position];
				w.sense_y_trustworthiness = D_Witness.witness_categories_trustworthiness_sense[trustworthy_position];
				//w.statements = "";
				
				w.global_witness_ID = w.make_ID();
				w.sign(myself_constituent_GID);
				try {
					Log.d(TAG, "Try storing: w="+w);
					w.storeVerified();
					
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
					Toast.makeText(NewNeighborhood.this, "Witnessing failed!", Toast.LENGTH_SHORT).show();
				}
				Toast.makeText(NewNeighborhood.this, "Witnessed: "+D_Witness.witness_categories_sense[eligibility_position], Toast.LENGTH_SHORT).show();
			
				finish();
			}
		});
	}
	
}
