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

import config.Identity;
import data.D_Constituent;
import data.D_Witness;
import table.organization;
import util.P2PDDSQLException;
import util.Util;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class Witness extends Activity{

	public static int trustworthy_position;
	public static int eligibility_position;
	private Spinner eligibility, trustworty;
	private Button submit;
	private int organization_position;
	private String organization_LID;
	private String organization_GIDH;
	private long oLID;
	private String constituent_LID;
	private String constituent_GIDH;
	private String constituent_GID;
	private String constituent_name;
	private String neighborhood_LID;
	private String neighborhood_GIDH;
	private String neighborhood_GID;
	private String neighborhood_name;
	private long myself_constituent_LID;
	private D_Constituent myself_constituent;
	private String myself_constituent_GID;
	private String myself_constituent_GIDH;
	private String organization_GID; 
	private static final String[] strArrEligibility = D_Witness.witness_categories;//{"Eligible", "Ineligible"};
	private static final String[] strArrTrustworty = D_Witness.witness_categories_trustworthiness; //{"a", "b"};
	
	private static final String TAG = "witness";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent i = this.getIntent();
		Bundle b = i.getExtras();
		organization_position = b.getInt(Orgs.O_ID);
		organization_LID = b.getString(Orgs.O_LID);
		organization_GIDH = b.getString(Orgs.O_GIDH);
		organization_GID = b.getString(Orgs.O_GID);
		
		oLID = Util.lval(organization_LID, -1);
    	if (oLID <= 0) return;

    	try {
    		Identity crt_identity = Identity.getCurrentConstituentIdentity();
    		if (crt_identity == null) {
    			Log.d(TAG, "No identity");
    		} else 
    			myself_constituent_LID = config.Identity.getDefaultConstituentIDForOrg(oLID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    	
    	if (myself_constituent_LID > 0) {
    		myself_constituent = D_Constituent.getConstByLID(myself_constituent_LID, true, false);
    		myself_constituent_GID = myself_constituent.getGID();
    		myself_constituent_GIDH = myself_constituent.getGIDH();
    		Log.d(TAG, "Got const: "+myself_constituent);
    	} else {
    		Toast.makeText(this, "Register First!", Toast.LENGTH_SHORT).show();
    		Log.d(TAG, "no const: "+myself_constituent);
    		return;
    	}

    	
		String type = b.getString(Constituent.C_TYPE);
		if (Constituent.C_TYPE_C.equals(type)) {
			//constituent_position = b.getInt(Constituent.C_ID);
			constituent_LID = b.getString(Constituent.C_LID);
			constituent_GIDH = b.getString(Constituent.C_GIDH);
			constituent_GID = b.getString(Constituent.C_GID);
			constituent_name = b.getString(Constituent.C_NAME);
		} else {
			neighborhood_LID = b.getString(Constituent.N_PARENT_LID);
			neighborhood_GIDH = b.getString(Constituent.N_GIDH);
			neighborhood_GID = b.getString(Constituent.N_GID);
			neighborhood_name = b.getString(Constituent.N_NAME);
		}
		setContentView(R.layout.witness);
		
		eligibility = (Spinner) findViewById(R.id.witness_eligibility);
		trustworty = (Spinner) findViewById(R.id.witness_trustworthy);
		submit = (Button) findViewById(R.id.witness_submit);
		
		ArrayAdapter<String> eligibilityAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item , strArrEligibility);
		eligibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		eligibility.setAdapter(eligibilityAdapter);
		eligibility.setOnItemSelectedListener(new EligibilityListener());
				
		ArrayAdapter<String> trustWorthyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item , strArrTrustworty);
		trustWorthyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		trustworty.setAdapter(trustWorthyAdapter);
		trustworty.setOnItemSelectedListener(new TrustworthyListener());
		
		
		submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				D_Witness w = new D_Witness();
				w.global_organization_ID = organization_GID;
				w.organization_ID = organization_LID;
				w.witnessing_global_constituentID = myself_constituent_GID;
				w.witnessing_constituentID = myself_constituent_LID;
				
				//w.witnessing_global_constituentID = constituent_GID;
				
				w.witnessed_global_constituentID = constituent_GID;
				w.witnessed_global_neighborhoodID = neighborhood_GID;
				
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
					Toast.makeText(Witness.this, "Witnessing failed!", Toast.LENGTH_SHORT).show();
				}
				Toast.makeText(Witness.this, "Witnessed: "+D_Witness.witness_categories_sense[eligibility_position], Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private class EligibilityListener implements OnItemSelectedListener {


		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			Witness.eligibility_position = position;
			Log.d(TAG, "eligibility: "+position);
			
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

			
		}
		
	}

	private class TrustworthyListener implements OnItemSelectedListener {


		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			Witness.trustworthy_position = position;
			Log.d(TAG, "trustworthy: "+position);
			
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			
		}
		
	}
	
}
