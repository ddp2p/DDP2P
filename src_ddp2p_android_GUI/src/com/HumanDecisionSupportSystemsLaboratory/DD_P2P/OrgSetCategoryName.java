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

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

@Deprecated
public class OrgSetCategoryName extends ActionBarActivity {

	private String strSetConstituents;
	private String strSetForum;
	private String strSetMotion;
	private String strSetOrganization;
	private String strSetJustification;
	private Button submitBut;
	private EditText setConstituents;
	private EditText setForum;
	private EditText setMotions;
	private EditText setOrganization;
	private EditText setJustification;

	private TextView ConstituentsIconText;
	private TextView ForumIconText;
	private TextView MotionsIconText;
	private TextView OrganizationIconText;
	private TextView JustificationIconText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.org_set);

		Toast.makeText(OrgSetCategoryName.this, "something", Toast.LENGTH_SHORT).show();

		submitBut = (Button) this.findViewById(R.id.submit_org_setting);
		
		setConstituents = (EditText) this.findViewById(R.id.set_org_constituents_icon_title);
		setForum = (EditText) this.findViewById(R.id.set_org_forum_icon_title);
		setMotions = (EditText) this.findViewById(R.id.set_org_news_icon_title);
		setOrganization = (EditText) this.findViewById(R.id.set_org_set_icon_title);
		setJustification = (EditText) this.findViewById(R.id.set_org_profile_icon_title);
		
		setConstituents.setHint("Constituent");
		setForum.setHint("Forum");
		setOrganization.setHint("Organization");
		setMotions.setHint("Motion");
		setJustification.setHint("Justification");
		
		if (AddOrg.settings.name_constituent != null) setConstituents.setText(AddOrg.settings.name_constituent);
		if (AddOrg.settings.name_forum != null) setForum.setText(AddOrg.settings.name_forum);
		if (AddOrg.settings.name_motion != null) setMotions.setText(AddOrg.settings.name_motion);
		if (AddOrg.settings.name_org != null) setOrganization.setText(AddOrg.settings.name_org);
		if (AddOrg.settings.name_justification != null) setJustification.setText(AddOrg.settings.name_justification);

		/*
		ConstituentsIconText = (TextView) findViewById(R.id.constituent_icon_text);
		ForumIconText = (TextView) findViewById(R.id.motion_icon_text);
		MotionsIconText = (TextView) findViewById(R.id.news_icon_text);
		OrganizationIconText = (TextView) findViewById(R.id.set_icon_text);
		//JustificationIconText = (TextView) findViewById(R.id.);
		
		ForumIconText.setText("Forum");
		MotionsIconText.setText("Motion");
		OrganizationIconText.setText("Organization");
*/
		Log.d("org_set", setConstituents.getText().toString());

		submitBut.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				strSetConstituents = setConstituents.getText().toString();
				strSetForum = setForum.getText().toString();
				strSetMotion = setMotions.getText().toString();
				strSetOrganization = setOrganization.getText().toString();
				strSetJustification = setJustification.getText().toString();
				
				if (! empty(strSetConstituents)) AddOrg.settings.name_constituent = strSetConstituents;
				else AddOrg.settings.name_constituent = null;

				if (! empty(strSetForum)) AddOrg.settings.name_forum = strSetForum;
				else AddOrg.settings.name_forum = null;

				if (! empty(strSetMotion)) AddOrg.settings.name_motion = strSetMotion;
				else AddOrg.settings.name_motion = null;

				if (! empty(strSetOrganization)) AddOrg.settings.name_org = strSetOrganization;
				else AddOrg.settings.name_org = null;

				if (! empty(strSetJustification)) AddOrg.settings.name_justification = strSetJustification;
				else AddOrg.settings.name_justification = null;
				
				Log.d("org_set", strSetConstituents);

				Toast.makeText(OrgSetCategoryName.this, strSetConstituents,
						Toast.LENGTH_SHORT).show();

				
				finish();
			}

			private boolean empty(String str) {
				if (str == null) return true;
				if ("".equals(str.trim())) return true;
				return false;
			}
		});
	}

	// return button on left-top corner
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

}
