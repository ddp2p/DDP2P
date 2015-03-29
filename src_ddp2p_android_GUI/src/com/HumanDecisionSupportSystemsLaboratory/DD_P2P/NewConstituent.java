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

import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_Organization;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Toast;

public class NewConstituent extends Activity{

	private static final String TAG = "NewConstituent";
	protected static final boolean ANONYOUS_SUBMISSION = true;
	private EditText name, email, forename;
	private Button submit; 
	private String strname, strEmail, strForename;
	private CheckedTextView votingRight;
	private int organization_position;
	private String organization_lid;
	private String organization_gidh;
	private String organization_gid;
	private String organization_name;
	private String neighborhood_parent_LIDstr;
	private long neighborhood_parent_LID;
	private D_Neighborhood neighborhood_parent;
	private long oLID;
	private long myself_constituent_LID;
	private D_Constituent myself_constituent;
	private String myself_constituent_GID;
	private String myself_constituent_GIDH;
	private D_Organization organization;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_constituent);
		name = (EditText) findViewById(R.id.new_constituent_name);
		forename = (EditText) findViewById(R.id.new_constituent_forename);
		email = (EditText) findViewById(R.id.new_constituent_email);
		votingRight = (CheckedTextView) findViewById(R.id.new_constituent_voting_right);
		submit = (Button) findViewById(R.id.new_constituent_submit);
		
		
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
       		Toast.makeText(this, "Select organization First!", Toast.LENGTH_SHORT).show();
			Log.d(TAG, "No organization loaded for New Constituent");
    		finish();
    		return;
    	}

    	try {
    		Identity crt_identity = Identity.getCurrentConstituentIdentity();
    		if (crt_identity == null) {
    			Log.d(TAG, "No identity for NewConstituent");
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
    		Log.d(TAG, "NewConstituent no myself const: "+myself_constituent);
    		finish();
    		return;
    	}

    	organization = D_Organization.getOrgByLID(organization_lid, true, false);

		
		votingRight.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				votingRight.setChecked(! votingRight.isChecked());
			}
		});

/*		
		custom_fields = (LinearLayout) findViewById(R.id.profile_view); 
		custom_index = 8;
//		custom_fields = (LinearLayout) findViewById(R.id.profile_custom); 
//		custom_index = 0;
				
		custom_params = org.params.orgParam;
		
		if (custom_params == null || custom_params.length == 0) {
			custom_params = new D_OrgParam[0];//3
//			
//			custom_params[0] = new D_OrgParam();
//			custom_params[0].label = "School";
//			custom_params[0].entry_size = 5;
//			custom_params[1] = new D_OrgParam();
//			custom_params[1].label = "Street";
//			custom_params[2] = new D_OrgParam();
//			custom_params[2].label = "Year";
//			custom_params[2].list_of_values = new String[]{"2010","2011","2012"};
//			
		}
		D_FieldValue[] field_values = null;
		if ( constituent != null && constituent.address != null)
			field_values = constituent.address;
		for (int crt_field = 0; crt_field < custom_params.length; crt_field ++) {
			D_OrgParam field = custom_params[crt_field];
			LinearLayout custom_entry = new LinearLayout(this);
			custom_entry.setOrientation(LinearLayout.HORIZONTAL);
			custom_entry.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			TextView custom_label = new TextView(this);
			custom_label.setText(field.label);
			custom_entry.addView(custom_label);
			
			if (field.list_of_values != null && field.list_of_values.length > 0) {
				Log.d(TAG, "spinner:"+field);
				Spinner custom_spin = new Spinner(this);
				ArrayAdapter<String> custom_spin_Adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, field.list_of_values);
				custom_spin_Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				custom_spin.setAdapter(custom_spin_Adapter);
				custom_entry.addView(custom_spin);
				D_FieldValue fv = locateFV(field_values, field);
				if (fv != null) {
					int position = 0;
					for (int k = 0; k <= field.list_of_values.length; k++) {
						if (Util.equalStrings_null_or_not(field.list_of_values[k], fv.value))
							{position = k; break;}
					}
					custom_spin.setSelection(position);
				}
			} else {
				Log.d(TAG, "edit: "+field);
				EditText edit_text = new EditText(this);
				edit_text.setText(field.default_value);
				edit_text.setInputType(InputType.TYPE_CLASS_TEXT);
				if (field.entry_size > 0) edit_text.setMinimumWidth(field.entry_size * 60);
				Log.d(TAG, "edit: size="+field.entry_size);
				custom_entry.addView(edit_text);
				//Button child = new Button(this);
				//child.setText("Test");
				D_FieldValue fv = locateFV(field_values, field);
				if (fv != null)
					edit_text.setText(fv.value);
			}
			custom_fields.addView(custom_entry, custom_index ++);
		}
		
		ArrayAdapter<String> keysAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m);
		keysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		keys.setAdapter(keysAdapter);
		keys.setOnItemSelectedListener(new KeysListener());
	
		ArrayAdapter<String> eligibilityAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m);
		eligibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//		eligibility.setAdapter(eligibilityAdapter);
//		eligibility.setOnItemSelectedListener(new EligibilityListener());
*/		
		
		submit.setOnClickListener(new OnClickListener() {
			
			private boolean b_votingRight;

			@Override
			public void onClick(View v) {
				Log.d(TAG, "In submit constituent");
				strname = name.getText().toString();
				strForename = forename.getText().toString();
				strEmail = email.getText().toString();
				
				b_votingRight = votingRight.isChecked();
				
				boolean external = true;
				
				D_Constituent submitter = null;
				if (! ANONYOUS_SUBMISSION) submitter = myself_constituent;
				String _weight = b_votingRight?"1":"0";
				D_Constituent c = D_Constituent.createConstituent(
						strForename, strname, strEmail, 
						oLID, external, _weight, null, null, null, 0,
						neighborhood_parent, submitter); // myself_constituent
				Log.d(TAG, "NewConstituent: C2 Added const="+c);
				
				finish();
			}
		});
	}

	
}
