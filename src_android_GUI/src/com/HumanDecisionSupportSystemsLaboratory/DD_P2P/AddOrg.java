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
import java.util.HashSet;
import java.util.regex.Pattern;

import com.HumanDecisionSupportSystemsLaboratory.DD_P2P.Orgs.OrgAdapter;

import util.Util;
import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import config.Application_GUI;
import data.D_OrgParam;
import data.D_Organization;

class OrgSettings {
	String name_org;
	String name_forum;
	String name_motion;
	String name_justification;
	String name_constituent;

	public void clear() {
		name_org = null;
		name_forum = null;
		name_motion = null;
		name_justification = null;
		name_constituent = null;
	}
}

public class AddOrg extends ActionBarActivity {

	private static final String TAG = "AddOrg";
	static final OrgSettings settings = new OrgSettings();
	private String name;
	private String description;
	private String instructions_motion;
	private String instructions_registration;
	private int SELECT_PROFILE_PHOTO = 10;
	private int SELECT_PPROFILE_PHOTO_KITKAT = 11;
	private LinearLayout mainLayout;
	private ArrayList<ExtraFields> extraFileds;
	private byte[] icon;
	private CheckedTextView variedVotingRight;
	private EditText maxVotingLevel;
	private TextView maxVotingLevelText;
	private ImageView orgIcon;
	private CheckedTextView addOrgSetting;
	private String selectedImagePath;
	private LinearLayout catName;

	class ExtraFields {
		EditText f_label;
		EditText f_values;
		EditText f_hint;
		EditText f_default;
		EditText f_level;
	}

	class ExtraFieldData {
		String f_label;
		String f_values;
		String f_hint;
		String f_default;
		int f_level;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_org);

		Application_GUI.dbmail = new Android_DB_Email(this);
		Application_GUI.gui = new Android_GUI();

		mainLayout = (LinearLayout) findViewById(R.id.layout_add_org);
		extraFileds = new ArrayList<ExtraFields>();

		final Button butSubmit = (Button) findViewById(R.id.submit_add_org);
		final EditText add_name = (EditText) findViewById(R.id.add_org_name);
		final EditText edt_description = (EditText) findViewById(R.id.add_org_desc);
		final EditText edt_instructions_motion = (EditText) findViewById(R.id.add_org_inst_for_motion);
		final EditText edt_instructions_registration = (EditText) findViewById(R.id.add_org_inst_for_registration);
		final Button butExtra = (Button) findViewById(R.id.add_org_new_extra);
		variedVotingRight = (CheckedTextView) findViewById(R.id.add_org_varied_voting_right);
		maxVotingLevel = (EditText) findViewById(R.id.add_org_max_voting_level);
		maxVotingLevelText = (TextView) findViewById(R.id.add_org_max_voting_level_text);
		catName = (LinearLayout) findViewById(R.id.add_org_cat_name);

        final EditText edt_name_constituent = (EditText) findViewById(R.id.add_org_cat_name_constituent);
        final EditText edt_name_motion = (EditText) findViewById(R.id.add_org_cat_name_motion);
        final EditText edt_name_organization = (EditText) findViewById(R.id.add_org_cat_name_organization);
        final EditText edt_name_forum = (EditText) findViewById(R.id.add_org_cat_name_forum);
        final EditText edt_name_justification = (EditText) findViewById(R.id.add_org_cat_name_justification);
        final EditText edt_name_profile = (EditText) findViewById(R.id.add_org_cat_name_profile);
        final EditText edt_name_news = (EditText) findViewById(R.id.add_org_cat_name_news);
        /**
         * The options for naming organization elements
         */
        AddOrg.settings.clear();
        addOrgSetting = (CheckedTextView) findViewById(R.id.add_org_setting);
        addOrgSetting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (! addOrgSetting.isChecked()) {
                    addOrgSetting.setChecked(true);
                    addOrgSetting.setVisibility(View.VISIBLE);
                    catName.setVisibility(View.VISIBLE);
                } else {
                    addOrgSetting.setChecked(false);
                    catName.setVisibility(View.GONE);
                }
            }
        });

        variedVotingRight.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!variedVotingRight.isChecked()) {
					variedVotingRight.setChecked(true);
					maxVotingLevel.setVisibility(View.VISIBLE);
					maxVotingLevelText.setVisibility(View.VISIBLE);
					if (maxVotingLevel.getText().toString().trim().equals(""))
						maxVotingLevel.setText("1");
				} else {
					variedVotingRight.setChecked(false);
					maxVotingLevel.setVisibility(View.GONE);
					maxVotingLevelText.setVisibility(View.GONE);
				}
			}
		});
		butSubmit.setOnClickListener(new View.OnClickListener() {

            private boolean empty(String str) {
                if (str == null) return true;
                if ("".equals(str.trim())) return true;
                return false;
            }
			public void onClick(View v) {
				name = add_name.getText().toString();
				if ("".equals(name.trim())) {
					name = null;
					Toast.makeText(
							AddOrg.this,
							"Configured as refusing to make organization with no name!",
							Toast.LENGTH_SHORT).show();
					return;
				}

                String strSetConstituents = edt_name_constituent.getText().toString();
                String strSetMotion = edt_name_motion.getText().toString();
                String strSetOrganization = edt_name_organization.getText().toString();
                String strSetForum = edt_name_forum.getText().toString();
                String strSetJustification = edt_name_justification.getText().toString();

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


                description = edt_description.getText().toString();
				if ("".equals(description.trim()))
					description = null;

				instructions_motion = edt_instructions_motion.getText()
						.toString();
				if ("".equals(instructions_motion.trim()))
					instructions_motion = null;

				instructions_registration = edt_instructions_registration
						.getText().toString();
				if ("".equals(instructions_registration.trim()))
					instructions_registration = null;

				boolean b_variedVotingRight = variedVotingRight.isChecked();
				String s_maxVotingLevel = "1";
				int i_maxVotingLevel = 1;
				if (b_variedVotingRight) {
					try {
						s_maxVotingLevel = maxVotingLevel.getText().toString();
						i_maxVotingLevel = Util.ival(s_maxVotingLevel, 0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				ArrayList<ExtraFieldData> efd_list = new ArrayList<ExtraFieldData>();
				if (!extraFileds.isEmpty()) {
					HashSet<String> labels = new HashSet<String>();
					for (ExtraFields et : extraFileds) {
						ExtraFieldData efd = new ExtraFieldData();
						efd.f_label = et.f_label.getText().toString();
						if (efd.f_label == null
								|| "".equals(efd.f_label.trim()))
							continue;
						if (labels.contains(efd.f_label)) {
							Toast.makeText(AddOrg.this,
									"Two labels named: " + efd.f_label,
									Toast.LENGTH_SHORT).show();
							return;
						}
						labels.add(efd.f_label);

						efd.f_values = et.f_values.getText().toString();
						efd.f_hint = et.f_hint.getText().toString();
						efd.f_level = Util.ival(
								et.f_level.getText().toString(), 0);
						efd.f_default = et.f_default.getText().toString();

						efd_list.add(efd);
					}
				}

				newOrg(name, efd_list, b_variedVotingRight, i_maxVotingLevel,
						description, instructions_motion,
						instructions_registration, icon);

				finish();

			}
		});

		orgIcon = (ImageView) findViewById(R.id.add_org_icon);
		orgIcon.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (Build.VERSION.SDK_INT < 19) {
					Intent intent = new Intent();
					intent.setType("image/*");
					intent.setAction(Intent.ACTION_GET_CONTENT);
					startActivityForResult(intent, SELECT_PROFILE_PHOTO);
				} else {
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
					intent.setType("image/*");
					startActivityForResult(intent, SELECT_PPROFILE_PHOTO_KITKAT);
				}
			}
		});

		butExtra.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				LinearLayout ll = new LinearLayout(AddOrg.this);
				/*
				 * TextView tv = new TextView(AddOrg.this); tv.setText("test");
				 * tv.setTextSize(20);
				 */

				LayoutParams params = new LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				params.setMargins(10, 0, 0, 0);

				ExtraFields new_field = new ExtraFields();
				ll.setOrientation(LinearLayout.VERTICAL);
				ll.setBackground(AddOrg.this.getResources().getDrawable(
						R.drawable.add_org_extra_bg));
				EditText etLabel = new EditText(AddOrg.this);
				etLabel.setLayoutParams(params);
				new_field.f_label = etLabel;
				etLabel.setHint("Label");

				EditText etValues = new EditText(AddOrg.this);
				new_field.f_values = etValues;
				etValues.setLayoutParams(params);
				etValues.setHint("Values divided by ':'");

				EditText etDefault = new EditText(AddOrg.this);
				new_field.f_default = etDefault;
				etDefault.setLayoutParams(params);
				etDefault.setHint("Default");

				EditText etLevel = new EditText(AddOrg.this);
				new_field.f_level = etLevel;
				etLevel.setLayoutParams(params);
				etLevel.setHint("Level");

				EditText etHint = new EditText(AddOrg.this);
				new_field.f_hint = etHint;
				etHint.setLayoutParams(params);
				etHint.setHint("Hint");

				extraFileds.add(new_field);

				ll.addView(etLabel);
				ll.addView(etValues);
				ll.addView(etDefault);
				ll.addView(etLevel);
				ll.addView(etHint);
				mainLayout.addView(ll, mainLayout.getChildCount() - 1);

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

	public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BIT MAP
		matrix.postScale(scaleWidth, scaleHeight);

		// "RECREATE" THE NEW BITMAP
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height,
				matrix, false);
		return resizedBitmap;
	}

	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent resultData) {
		if (resultCode == RESULT_OK && resultData != null) {
			Uri uri = null;

			if (requestCode == SELECT_PROFILE_PHOTO) {
				uri = resultData.getData();
				Log.i("Uri", "Uri: " + uri.toString());
			} else if (requestCode == SELECT_PPROFILE_PHOTO_KITKAT) {
				uri = resultData.getData();
				Log.i("Uri_kitkat", "Uri: " + uri.toString());
				final int takeFlags = resultData.getFlags()
						& (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				// Check for the freshest data.
				getContentResolver().takePersistableUriPermission(uri,
						takeFlags);
			}

			selectedImagePath = FileUtils.getPath(this, uri);
			Log.i("path", "path: " + selectedImagePath);

			Bitmap bmp = PhotoUtil.decodeSampledBitmapFromFile(
					selectedImagePath, 55, 55);
			byte[] selectedIcon = PhotoUtil.BitmapToByteArray(bmp, 100);
			/*
			 * for (int c = 0; c < 10; c ++) { // String strFromBmp =
			 * PhotoUtil.BitmapToString(bmp);
			 * 
			 * Log.i(TAG, "Image size start = " + selectedIcon.length+" c="+c);
			 * if (selectedIcon.length > DD.MAX_ORG_ICON_LENGTH) { double frac =
			 * (DD.MAX_ORG_ICON_LENGTH)/(selectedIcon.length * 1.0d); frac =
			 * Math.sqrt(frac)*.9; Log.i(TAG, "Image scale frac=" +
			 * frac+" H="+bmp.getHeight()+" W="+bmp.getWidth()); bmp =
			 * getResizedBitmap(bmp, (int) (bmp.getHeight()*frac), (int)
			 * (bmp.getWidth()*frac)); continue; } Log.i(TAG,
			 * "Image scale H="+bmp.getHeight()+" W="+bmp.getWidth());
			 * 
			 * Log.i(TAG, "Image size last = " + selectedIcon.length); String
			 * b64 = Util.stringSignatureFromByte(selectedIcon); Log.i(TAG,
			 * "AddOrg: onActivityResult: " +
			 * Util.byteSignatureFromString(b64).length);
			 */
			/*
			 * Log.i(TAG, "Image size quit = " + selectedIcon.length); break; }
			 */
			orgIcon.setImageBitmap(bmp);

			// TODO set up this icon
			icon = selectedIcon;
			Log.i(TAG, "SafeProfile: Icon length=" + icon.length);
		}
	}

	// function for add a new peer
	private void newOrg(String _name, ArrayList<ExtraFieldData> efd_list,
			boolean b_variedVotingRight, int i_maxVotingLevel,
			String description2, String instructions_motion2,
			String instructions_registration2, byte[] oIcon) {


		/*
		 * ciphersuite did not initialize, add a new ciphersuit class
		 */

		new OrgCreatingThread(name, efd_list, b_variedVotingRight,
				i_maxVotingLevel, description, instructions_motion,
				instructions_registration, oIcon).start();
		Log.d("onCreatePeerCreatingThread", "AddSafe: newPeer: run: done");

	}

	class OrgCreatingThread extends Thread {
		String name;
		private String description;
		private String instructions_motion;
		private String instructions_registration;
		private ArrayList<ExtraFieldData> efd_list;
		private boolean b_variedVotingRight;
		private int i_maxVotingLevel;
		private byte[] oIcon;

		OrgCreatingThread(String _name, ArrayList<ExtraFieldData> _efd_list,
				boolean _b_variedVotingRight, int _i_maxVotingLevel,
				String _description, String _instructions_motion,
				String _instructions_registration, byte[] _oIcon) {
			name = _name;
			b_variedVotingRight = _b_variedVotingRight;
			i_maxVotingLevel = _i_maxVotingLevel;
			description = _description;
			instructions_motion = _instructions_motion;
			instructions_registration = _instructions_registration;
			efd_list = _efd_list;
			oIcon = _oIcon;
		}

		public void run() {
			D_OrgParam[] params = new D_OrgParam[efd_list.size()];
			for (int k = 0; k < efd_list.size(); k++) {
				ExtraFieldData ef = efd_list.get(k);
				data.D_OrgParam dop = new D_OrgParam();
				dop.label = ef.f_default;
				dop.tip = ef.f_hint;
				dop.partNeigh = ef.f_level;
				if (ef.f_values != null)
					dop.list_of_values = Util.trimmed(ef.f_values.split(Pattern
							.quote(",")));
				params[k] = dop;
			}
			// This can be used instead of the next sequence...
			// D_Organization o =
			// D_Organization.createGrassrootOrganization(name, params);

			D_Organization new_org = D_Organization.getEmpty();
			new_org.setName(name);
			// if (new_org.params == null) new_org.params = new D_OrgParams();
			new_org.setOrgParams(params);
			// new_org.updateExtraGIDs();

			// use the following to set concepts (default names plus list of
			// translations in official languages of the org):
			// new_org.setNamesOrg(null, new String[]{"Country","Pays","Land"});
			// new_org.setNamesForum(null, new
			// String[]{"Forum","Forum","Sura"});
			// new_org.setNamesMotion(null, new
			// String[]{"Petition","Initiative Populaire","Gesetz"});
			// new_org.setNamesJustification(null, new String[]{"Justification",
			// "Explication", "Erklarung"});

			if (AddOrg.settings.name_org != null)
				new_org.setNamesOrg(null,
						new String[] { AddOrg.settings.name_org });
			if (AddOrg.settings.name_forum != null)
				new_org.setNamesForum(null,
						new String[] { AddOrg.settings.name_forum });
			if (AddOrg.settings.name_motion != null)
				new_org.setNamesMotion(null,
						new String[] { AddOrg.settings.name_motion });
			if (AddOrg.settings.name_justification != null)
				new_org.setNamesJustification(null,
						new String[] { AddOrg.settings.name_justification });
			if (AddOrg.settings.name_constituent != null)
				new_org.setNamesConstituent(null,
						new String[] { AddOrg.settings.name_constituent });

			new_org.setDescription(description);
			new_org.setInstructionsNewMotions(instructions_motion);
			new_org.setInstructionsRegistration(instructions_registration);

			// TODO
			if (oIcon != null)
				Log.d(TAG, "Setting icon #" + oIcon.length);
			else
				Log.d(TAG, "Setting null icon");

			new_org.setIcon(oIcon);

			if (!b_variedVotingRight) {

				new_org.setWeightsType(table.organization.WEIGHTS_TYPE_NONE);

			} else {

				if (i_maxVotingLevel == 1) {
					new_org.setWeightsType(table.organization.WEIGHTS_TYPE_0_1);
				} else {
					new_org.setWeightsType(table.organization.WEIGHTS_TYPE_INT);
				}

				new_org.setWeightsMax(i_maxVotingLevel);
			}

			// new_org.concepts.name_motion=new String[]{name};
			new_org.params.certifMethods = table.organization._GRASSROOT;

			new_org.setTemporary(false);
			new_org.setCreationDate();
			D_Organization.DEBUG = true;
			// String GID =
			new_org.global_organization_IDhash = new_org.global_organization_ID = new_org
					.getOrgGIDandHashForGrassRoot(); // sign();

			// D_Organization org = D_Organization.getOrgByGID_or_GIDhash(GID,
			// GID, true, true, true, null);
			D_Organization o = D_Organization.storeRemote(new_org, null);

			Log.d(TAG, "AddOrg: OrgThread: got org=" + o);
			 Orgs.reloadOrgs();

			Message msgObj = handler.obtainMessage();
			handler.sendMessage(msgObj);
		}
	}

	/*
	 * class PeerCreatingThread extends Thread { PeerInput pi;
	 * PeerCreatingThread(PeerInput _pi) { pi = _pi; } public void run() {
	 * Log.d("onCreatePeerCreatingThread", "PeerCreatingThread: run: start");
	 * gen(); Log.d("onCreatePeerCreatingThread",
	 * "PeerCreatingThread: run: generated"); threadMsg("");
	 * Log.d("onCreatePeerCreatingThread",
	 * "PeerCreatingThread: run: announced"); } public D_Peer gen() {
	 * 
	 * D_Peer peer = HandlingMyself_Peer.createMyselfPeer_w_Addresses(pi, true);
	 * Log.d("onCreatePeerCreatingThread", "PeerCreatingThread: gen: start");
	 * peer.setEmail(email); peer.setSlogan(slogan); peer.storeRequest();
	 * 
	 * Log.d("onCreatePeerCreatingThread", "PeerCreatingThread: gen: inited");
	 * data.HandlingMyself_Peer.setMyself(peer, true, false); if
	 * (!Main.serversStarted) Main.startServers(); return peer; } private void
	 * threadMsg(String msg) { if (!msg.equals(null) && !msg.equals("")) {
	 * Message msgObj = handler.obtainMessage(); Bundle b = new Bundle();
	 * b.putString("message", msg); msgObj.setData(b);
	 * handler.sendMessage(msgObj); } } }
	 */
	private final Handler handler = new Handler() {

		// Create handleMessage function

		public void handleMessage(Message msg) {

			String aResponse = msg.getData().getString("message");
			Toast.makeText(AddOrg.this, "Added a new organization successfully!",
					Toast.LENGTH_LONG).show();
			if (Orgs.activ != null) {
				OrgAdapter adapt = (OrgAdapter) Orgs.activ.getListAdapter();
				if (adapt != null)
					adapt.notifyDataSetChanged();
			}

			//this one works to refresh the list
			Orgs.listAdapter = new OrgAdapter(Orgs.activ.getActivity(),
					Orgs.orgName);
			Orgs.activ.setListAdapter(Orgs.listAdapter);

		}
	};
}
