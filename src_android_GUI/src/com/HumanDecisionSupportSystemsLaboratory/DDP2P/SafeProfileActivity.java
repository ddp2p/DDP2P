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

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import util.DD_SK;
import util.P2PDDSQLException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import ciphersuits.KeyManagement;
import config.DD;
import data.D_Peer;
import data.D_PeerInstance;

public class SafeProfileActivity extends FragmentActivity {

	private static final String TAG = "SafeProfile";
	// private int safe_id;
	private String safe_lid;
	private int SELECT_PROFILE_PHOTO = 10;
	private int SELECT_PPROFILE_PHOTO_KITKAT = 11;
	private String[] drawerContent;
	private D_Peer peer;
	private ActionBarDrawerToggle drawerToggle;
	private DrawerLayout drawerLayout = null;
	private SafeProfileFragment safeProfileFragment = null;
	private ImageView imgbut;
	private String selectedImagePath;
	private File selectImageFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.safe_profile_drawer);

		Intent i = this.getIntent();
		Bundle b = i.getExtras();

		safe_lid = b.getString(Safe.P_SAFE_LID);

		peer = D_Peer.getPeerByLID(safe_lid, true, false);

		if (getSupportFragmentManager().findFragmentById(
				R.id.safe_profile_drawer_content) == null) {
			showSafeProfile();
		}
		drawerContent = prepareDrawer(peer.getSK() != null);

		Log.d(TAG, drawerContent[0]);
		drawerLayout = (DrawerLayout) findViewById(R.id.safe_profile_drawer_layout);

		// enable action bar home button
		android.app.ActionBar actBar = getActionBar();
		actBar.setDisplayHomeAsUpEnabled(true);

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.ic_drawer, R.string.safe_profile_drawer_open,
				R.string.safe_profile_drawer_close);

		ListView drawer = (ListView) findViewById(R.id.safe_profile_drawer_listview);

		drawer.setAdapter(new SafeProfileAdapter(this, drawerContent));

		SafeProfileOnItemClickListener drawerListener = new SafeProfileOnItemClickListener();
		drawer.setOnItemClickListener(drawerListener);
		drawerLayout.setDrawerListener(drawerToggle);

	}

	private void showSafeProfile() {
		if (safeProfileFragment == null)
			safeProfileFragment = new SafeProfileFragment();

		if (!safeProfileFragment.isVisible())
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.safe_profile_drawer_content,
							safeProfileFragment).commit();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.action_setNameMy) {
			Bundle id = new Bundle();
			// id.putInt(Safe.P_SAFE_ID, safe_id);
			id.putString(Safe.P_SAFE_LID, safe_lid);

			// update name my dialog
			FragmentManager fm = getSupportFragmentManager();
			UpdateSafeNameMy nameDialog = new UpdateSafeNameMy();
			nameDialog.setArguments(id);
			nameDialog.show(fm, "fragment_edit_name_my");
		}

		else if (item.getItemId() == R.id.action_setEmail) {
			Bundle id = new Bundle();
			// id.putInt("Safe_ID", safe_id);
			id.putString(Safe.P_SAFE_LID, safe_lid);

			// update email dialog
			FragmentManager fm = getSupportFragmentManager();
			UpdateSafeEmail emailDialog = new UpdateSafeEmail();
			emailDialog.setArguments(id);
			emailDialog.show(fm, "fragment_edit_email");

		}

		else if (item.getItemId() == R.id.action_setSlogan) {
			Bundle id = new Bundle();
			// id.putInt("Safe_ID", safe_id);
			id.putString(Safe.P_SAFE_LID, safe_lid);

			// update slogan dialog
			FragmentManager fm = getSupportFragmentManager();
			UpdateSafeSlogan sloganDialog = new UpdateSafeSlogan();
			sloganDialog.setArguments(id);
			sloganDialog.show(fm, "fragment_edit_slogan");
		}

		else if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		// /* else if (item.getItemId() == R.id.export_peer_pk) {
		// Bundle id = new Bundle();
		// id.putInt("Safe_ID", safe_id);
		//
		// FragmentManager fm = getSupportFragmentManager();
		// SendPK sendPKDialog = new SendPK();
		// sendPKDialog.setArguments(id);
		// sendPKDialog.show(fm, "fragment_send_public_key");
		//
		//
		// }*/

		else
			Toast.makeText(this, "to be implement", Toast.LENGTH_SHORT).show();
		return super.onOptionsItemSelected(item);
	}

	/*
	 * @TargetApi(Build.VERSION_CODES.KITKAT)
	 * 
	 * @Override protected void onActivityResult(int requestCode, int
	 * resultCode, Intent resultData) { if (resultCode == RESULT_OK &&
	 * resultData != null) { Uri uri = null;
	 * 
	 * if (requestCode == SELECT_PHOTO) { uri = resultData.getData();
	 * Log.i("Uri", "Uri: " + uri.toString()); } else if (requestCode ==
	 * SELECT_PHOTO_KITKAT) { uri = resultData.getData(); Log.i("Uri_kitkat",
	 * "Uri: " + uri.toString()); final int takeFlags = resultData.getFlags() &
	 * (Intent.FLAG_GRANT_READ_URI_PERMISSION |
	 * Intent.FLAG_GRANT_WRITE_URI_PERMISSION); // Check for the freshest data.
	 * getContentResolver().takePersistableUriPermission(uri, takeFlags); }
	 * 
	 * 
	 * selectedImagePath = FileUtils.getPath(this,uri); Log.i("path", "path: " +
	 * selectedImagePath);
	 * 
	 * selectImageFile = new File(selectedImagePath); //File testFile = new
	 * File("file://storage/emulated/0/DCIM/hobbit.bmp");
	 * 
	 * boolean success;
	 * 
	 * 
	 * String[] selected = new String[1]; DD_Address adr = new DD_Address(peer);
	 * try { //util.EmbedInMedia.DEBUG = true; Log.i("success_embed",
	 * "success_embed 1: "+selectImageFile); success =
	 * DD.embedPeerInBMP(selectImageFile, selected, adr); Log.i("success_embed",
	 * "success_embed 2: " + success); if (success == true) {
	 * Toast.makeText(this, "Export success!", Toast.LENGTH_SHORT).show(); }
	 * else Toast.makeText(this, "Unable to export:"+selected[0],
	 * Toast.LENGTH_SHORT).show(); } catch (Exception e) { Toast.makeText(this,
	 * "Unable to export!", Toast.LENGTH_SHORT).show(); e.printStackTrace(); }
	 * 
	 * }
	 * 
	 * if (resultCode == RESULT_OK && resultData != null) { Uri uri = null;
	 * 
	 * if (requestCode == PK_SELECT_PHOTO) { uri = resultData.getData();
	 * Log.i("Uri", "Uri: " + uri.toString()); } else if (requestCode ==
	 * PK_SELECT_PHOTO_KITKAT) { uri = resultData.getData(); Log.i("Uri_kitkat",
	 * "Uri: " + uri.toString()); final int takeFlags = resultData.getFlags() &
	 * (Intent.FLAG_GRANT_READ_URI_PERMISSION |
	 * Intent.FLAG_GRANT_WRITE_URI_PERMISSION); // Check for the freshest data.
	 * getContentResolver().takePersistableUriPermission(uri, takeFlags); }
	 * 
	 * 
	 * selectedImagePath = FileUtils.getPath(this,uri); Log.i("path", "path: " +
	 * selectedImagePath);
	 * 
	 * selectImageFile = new File(selectedImagePath); //File testFile = new
	 * File("file://storage/emulated/0/DCIM/hobbit.bmp");
	 * 
	 * boolean success;
	 * 
	 * try { //util.EmbedInMedia.DEBUG = true; success = saveSK(peer,
	 * selectImageFile); Log.i("success_embed", "success_embed: " + success); if
	 * (success == true) { Toast.makeText(this, "Export success!",
	 * Toast.LENGTH_SHORT).show(); } else Toast.makeText(this,
	 * "Unable to export!", Toast.LENGTH_SHORT).show(); } catch (Exception e) {
	 * Toast.makeText(this, "Unable to export!", Toast.LENGTH_SHORT).show();
	 * e.printStackTrace(); }
	 * 
	 * } super.onActivityResult(requestCode, resultCode, resultData); }
	 */
	public boolean saveSK(D_Peer pk, File f) {
		DD_SK dsk = new DD_SK();
		try {
			if (!KeyManagement.fill_sk(dsk, pk.getGID()))
				return false;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		String[] selected = new String[1];
		boolean success = DD.embedPeerInBMP(f, selected, dsk);
		return success;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.profile_main, menu);
		return true;
	}

	private class SafeProfileOnItemClickListener implements
			AdapterView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			FragmentManager fm = getSupportFragmentManager();
			Bundle s_id = new Bundle();
			s_id.putString(Safe.P_SAFE_LID, safe_lid);

			if (peer.getSK() != null) {
				switch (position) {
				case 0:
					// update name dialog
					UpdateSafeName nameDialog = new UpdateSafeName();
					nameDialog.setArguments(s_id);
					nameDialog.show(fm, "fragment_edit_name");
					break;

				case 1:
					data.HandlingMyself_Peer
							.setMyself_currentIdentity_announceDirs(peer, true,
									false);
					Toast.makeText(SafeProfileActivity.this,
							"Successfully set this instance myself!",
							Toast.LENGTH_SHORT).show();
					break;

				case 2:
					// update name dialog
					SendPK sendPKDialog = new SendPK();
					sendPKDialog.setArguments(s_id);
					sendPKDialog.show(fm, "fragment_send_public_key");
					break;

				case 3:
					Intent i = new Intent();
					i.setClass(SafeProfileActivity.this,
							SelectDirectoryServer.class);
					startActivity(i);
					break;

				default:
					break;
				}
			} else {
				switch (position) {
				case 0:
					peer = D_Peer.getPeerByPeer_Keep(peer);
					peer.setLastSyncDate(null);
					peer.storeRequest();
					peer.releaseReference();

					for (D_PeerInstance i : peer._instances.values()) {
						Calendar date = i.get_last_sync_date();
						Log.i(TAG, "last sync date: " + date);
					}
					break;

				case 1:
					Switch hideThisSafe = (Switch) view
							.findViewById(R.id.safe_profile_drawer_switch);
					if (peer.getHidden() == false)
						hideThisSafe.setChecked(false);
					else
						hideThisSafe.setChecked(true);

					hideThisSafe
							.setOnCheckedChangeListener(new OnCheckedChangeListener() {

								@Override
								public void onCheckedChanged(
										CompoundButton buttonView,
										boolean isChecked) {
									if (isChecked) {
										D_Peer.setHidden(peer, true);
										peer.storeRequest();
										peer.releaseReference();
									} else {
										D_Peer.setHidden(peer, false);
										peer.storeRequest();
										peer.releaseReference();
									}
								}							
							});
					break;

				case 2:
					Switch accessIt = (Switch) view
							.findViewById(R.id.safe_profile_drawer_switch);
					if (peer.getUsed() == true)
						accessIt.setChecked(true);
					if (peer.getUsed() == false)
						accessIt.setChecked(false);

					accessIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							if (isChecked) {
								D_Peer.setUsed(peer, true);
							} else {
								D_Peer.setUsed(peer, false);
							}
						}
					});
					break;

				case 3:
					Switch blockIt = (Switch) view
							.findViewById(R.id.safe_profile_drawer_switch);
					if (peer.getBlocked() == true)
						blockIt.setChecked(true);
					if (peer.getBlocked() == false)
						blockIt.setChecked(false);

					blockIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							if (isChecked) {
								D_Peer.setBlocked(peer, true);
							} else {
								D_Peer.setBlocked(peer, false);
							}
						}
					});
					break;

				case 4:
					Switch serveIt = (Switch) view
							.findViewById(R.id.safe_profile_drawer_switch);
					if (peer.getUsed() == true)
						serveIt.setChecked(true);
					if (peer.getUsed() == false)
						serveIt.setChecked(false);

					serveIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							if (isChecked) {
								D_Peer.setUsed(peer, true);
								peer.storeRequest();
								peer.releaseReference();
							} else {
								D_Peer.setUsed(peer, false);
								peer.storeRequest();
								peer.releaseReference();
							}
						}
					});
					break;
				default:
					break;
				}
			}

		}

	}

	private String[] prepareDrawer(boolean hasSafeSk) {
		String[] drawer = new String[5];

		if (hasSafeSk == true) {
			drawer[0] = "Set Name";
			drawer[1] = "Set Myself";
			drawer[2] = "Export Address";
			drawer[3] = "Select DirectoryServer";
			drawer[4] = " ";
		}

		if (hasSafeSk == false) {
			drawer[0] = "Reset Last Sync Date";
			drawer[1] = "Hide This Safe";
			drawer[2] = "Access It";
			drawer[3] = "Block It";
			drawer[4] = "Serve It";
		}

		return drawer;

	}

	private class SafeProfileAdapter extends BaseAdapter {

		private Activity activity;
		private LayoutInflater inflater = null;
		private String[] data;

		public SafeProfileAdapter(Activity _activity, String[] _data) {
			activity = _activity;
			data = _data;
			inflater = (LayoutInflater) activity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		}

		@Override
		public int getCount() {
			return data.length;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (convertView == null)
				v = inflater.inflate(R.layout.safe_profile_drawer_row, null);

			TextView content = (TextView) v
					.findViewById(R.id.safe_profile_drawer_row_text);

			if (peer.getSK() != null) {
				content.setText(data[position]);
			} else {
				content.setText(data[position]);
				if (position > 0) {
					v = inflater.inflate(
							R.layout.safe_profile_drawer_row_switch, null);
					Switch s = (Switch) v
							.findViewById(R.id.safe_profile_drawer_switch);
					s.setText(data[position]);
				}
			}

			return v;
		}

	}

}
