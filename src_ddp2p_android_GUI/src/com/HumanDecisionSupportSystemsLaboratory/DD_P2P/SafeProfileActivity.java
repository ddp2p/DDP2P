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

import java.io.File;
import java.util.Calendar;

import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.util.DD_SK;
import net.ddp2p.common.util.P2PDDSQLException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import net.ddp2p.ciphersuits.KeyManagement;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;

public class SafeProfileActivity extends FragmentActivity {

	private static final String TAG = "SafeProfile";
	// private int safe_id;
	private String safe_lid;
	private int SELECT_PROFILE_PHOTO = 10;
	private int SELECT_PPROFILE_PHOTO_KITKAT = 11;
	private String[] drawerContent;
	public static boolean[] drawerState;
	private D_Peer peer;
	private ActionBarDrawerToggle drawerToggle;
	private DrawerLayout drawerLayout = null;
	private SafeProfileFragment safeProfileFragment = null;
	private ImageView imgbut;
	private String selectedImagePath;
	private File selectImageFile;
	private boolean hasSK;
	SK sk;
	SafeProfileAdapter sAdapter;
	static int cnt_try_false = 0;

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
		sk = peer.getSK();
		drawerContent = prepareDrawer(hasSK = (sk != null));

		Log.d(TAG, drawerContent[0]);
		drawerLayout = (DrawerLayout) findViewById(R.id.safe_profile_drawer_layout);

		// enable action bar home button
		android.app.ActionBar actBar = getActionBar();
		actBar.setDisplayHomeAsUpEnabled(true);

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.ic_drawer, R.string.safe_profile_drawer_open,
				R.string.safe_profile_drawer_close);

		ListView drawer = (ListView) findViewById(R.id.safe_profile_drawer_listview);

		sAdapter = new SafeProfileAdapter(this, drawerContent, drawerState);
		drawer.setAdapter(sAdapter);

		SafeProfileOnItemClickListener drawerListener = new SafeProfileOnItemClickListener();
		drawer.setOnItemClickListener(drawerListener);
		
		drawerLayout.setDrawerListener(drawerToggle);
	}
	public final static int SAFE_DRAWER_SK_SET_NAME = 0;
	public final static int SAFE_DRAWER_SK_SET_MYSELF = 1;
	public final static int SAFE_DRAWER_SK_EXPORT_ADDR = 2;
	public final static int SAFE_DRAWER_SK_SELECT_DIRS = 3;
	public final static int SAFE_DRAWER_SK_SERVE = 4;
	public final static int SAFE_DRAWER_SK_SET_INSTANCE = 5;
	public final static int SAFE_DRAWER_SK_HIDE_TOGGLE = 6;

	public final static int SAFE_DRAWER____RESET_SYNC = 0;
	public final static int SAFE_DRAWER____HIDE_TOGGLE = 1;
	public final static int SAFE_DRAWER____ACCESS = 2;
	public final static int SAFE_DRAWER____BLOCK = 3;
	public final static int SAFE_DRAWER____SERVE = 4;

	/**
	 * Prepare the boolean member array drawerState, and returns array with strings
	 * */
	private String[] prepareDrawer(boolean hasSafeSk) {
		String[] drawer = null;
		if (hasSafeSk) {
			drawer = new String[7];
			drawerState = new boolean[7];
			drawer[SAFE_DRAWER_SK_SET_NAME] =  getString(R.string.drawer_safe_Set_Name);
			drawer[SAFE_DRAWER_SK_SET_MYSELF] = getString(R.string.drawer_safe_Set_Myself);
			drawer[SAFE_DRAWER_SK_EXPORT_ADDR] = getString(R.string.drawer_safe_Export_Address);
			drawer[SAFE_DRAWER_SK_SELECT_DIRS] = getString(R.string.drawer_safe_Select_DirectoryServer);
			drawer[SAFE_DRAWER_SK_SERVE] = getString(R.string.drawer_safe_Serve_It);
			drawer[SAFE_DRAWER_SK_SET_INSTANCE] = getString(R.string.drawer_safe_Set_Instance);
			drawer[SAFE_DRAWER_SK_HIDE_TOGGLE] = getString(R.string.drawer_safe_Hide_This_Safe);

			drawerState[SAFE_DRAWER_SK_HIDE_TOGGLE] = this.peer.getHidden();
			drawerState[SAFE_DRAWER_SK_SERVE] = this.peer.getBroadcastable();

			if (drawerState[SAFE_DRAWER_SK_HIDE_TOGGLE])
				drawer[SAFE_DRAWER_SK_HIDE_TOGGLE] = getString(R.string.drawer_safe_Unhide_This_Safe);
			if (drawerState[SAFE_DRAWER_SK_SERVE])
				drawer[SAFE_DRAWER_SK_SERVE] = getString(R.string.drawer_safe_Stop_Serving_This_Safe);
		}

		if (! hasSafeSk) {
			drawer = new String[5];
			drawerState = new boolean[5];
			drawerState[SAFE_DRAWER____RESET_SYNC] = false;
			drawer[SAFE_DRAWER____RESET_SYNC] = getString(R.string.drawer_safe_Reset_Last_Sync_Date);

			if (drawerState[SAFE_DRAWER____HIDE_TOGGLE] = this.peer.getHidden())
				drawer[SAFE_DRAWER____HIDE_TOGGLE] = getString(R.string.drawer_safe_Unhide_This_Safe);
			else	drawer[SAFE_DRAWER____HIDE_TOGGLE] = getString(R.string.drawer_safe_Hide_This_Safe);

			if (drawerState[SAFE_DRAWER____ACCESS] = this.peer.getUsed())
				drawer[SAFE_DRAWER____ACCESS] = getString(R.string.drawer_safe_Stop_Accessing_It);
			else	drawer[SAFE_DRAWER____ACCESS] = getString(R.string.drawer_safe_Access_It);

			if (drawerState[SAFE_DRAWER____BLOCK] = this.peer.getBlocked())
				drawer[SAFE_DRAWER____BLOCK] = getString(R.string.drawer_safe_Stop_Blocking_It);
			else	drawer[SAFE_DRAWER____BLOCK] = getString(R.string.drawer_safe_Block_It);

			if (drawerState[SAFE_DRAWER____SERVE] = this.peer.getBroadcastableMyOrDefault())
				drawer[SAFE_DRAWER____SERVE] = getString(R.string.drawer_safe_Stop_Serving_It);
			else	drawer[SAFE_DRAWER____SERVE] = getString(R.string.drawer_safe_Serve_It);

//			drawer[5] = " ";
//			drawer[6] = " ";

			Log.d(TAG, "SafeProfileAct: prepareDrawer: block="+drawerState[SAFE_DRAWER____BLOCK]);
		}

		return drawer;

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
		Log.d(TAG, "onConfigChanged: ?");
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
		Log.d(TAG, "onPostCreate: ?");
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
			Toast.makeText(this, "To be implement", Toast.LENGTH_SHORT).show();
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
			if (! KeyManagement.fill_sk(dsk, pk.getGID()))
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
			Log.d(TAG, "SafeProfileAct: onClick: position="+position+" id="+id+" peer="+peer.getName());
			FragmentManager fm = getSupportFragmentManager();
			Bundle s_id = new Bundle();
			s_id.putString(Safe.P_SAFE_LID, safe_lid);

			if (peer.getSK() != null) {
				Log.d(TAG, "SafeProfileAct: onClick: hasSK");
				switch (position) {
				case SafeProfileActivity.SAFE_DRAWER_SK_SET_NAME: // 0
					Log.d(TAG, "SafeProfileAct: onClick: position setName");
					// update name dialog
					UpdateSafeName nameDialog = new UpdateSafeName();
					nameDialog.setArguments(s_id);
					nameDialog.show(fm, "fragment_edit_name");
					break;

				case SafeProfileActivity.SAFE_DRAWER_SK_SET_MYSELF: // 1:
					Log.d(TAG, "SafeProfileAct: onClick: position setMyself");
                    net.ddp2p.common.data.HandlingMyself_Peer
							.setMyself_currentIdentity_announceDirs(peer, true,
									false);
					Toast.makeText(SafeProfileActivity.this,
							"Successfully set this instance myself!",
							Toast.LENGTH_SHORT).show();
					break;

				case SafeProfileActivity.SAFE_DRAWER_SK_EXPORT_ADDR: // 2:
					Log.d(TAG, "SafeProfileAct: onClick: position export Addr");
					// update name dialog
					SendPK sendPKDialog = new SendPK();
					sendPKDialog.setArguments(s_id);
					sendPKDialog.show(fm, "fragment_send_public_key");
					break;

				case SafeProfileActivity.SAFE_DRAWER_SK_SELECT_DIRS: // 3:
					Log.d(TAG, "SafeProfileAct: onClick: position select dirs");
					Intent i = new Intent();
					i.setClass(SafeProfileActivity.this,
							SelectDirectoryServer.class);
					startActivity(i);
					break;

				case SafeProfileActivity.SAFE_DRAWER_SK_SERVE: // 4
				{
					Log.d(TAG, "SafeProfileAct: onClick: position sk serve");
					boolean served = peer.getBroadcastable();
					Log.d(TAG, "SafeProfileAct: onClick: position served old=" + served);
					D_Peer.setBroadcastable(peer, served = !served);
					Toast.makeText(SafeProfileActivity.this, "Served = " + served, Toast.LENGTH_LONG).show();
					drawerContent = prepareDrawer(hasSK);
					sAdapter.setData(drawerContent);
				}
					break;
				case SafeProfileActivity.SAFE_DRAWER_SK_SET_INSTANCE: // 5
				{
					Log.d(TAG, "SafeProfileAct: onClick: position set instance");
					D_Peer p = D_Peer.getPeerByPeer_Keep(peer);
					//p.addInstanceElem(instance, true);
					p.makeNewInstance();
					p.releaseReference();
					break;
				}
				case SafeProfileActivity.SAFE_DRAWER_SK_HIDE_TOGGLE: // 6
				{
					boolean hidden = peer.getHidden();
					Log.d(TAG, "SafeProfileAct: onClick: position hide old=" + hidden);
					D_Peer.setHidden(peer, hidden = !hidden);
					Toast.makeText(SafeProfileActivity.this, "Hidden = " + hidden, Toast.LENGTH_LONG).show();
					drawerContent = prepareDrawer(hasSK);
					sAdapter.setData(drawerContent);
				}
					break;
				default:
					Log.d(TAG, "SafeProfileAct: onClick: position default (not impl)");
					break;
				}
			} else {
				Log.d(TAG, "SafeProfileAct: onClick: no SK");

				switch (position) {
					case SafeProfileActivity.SAFE_DRAWER____RESET_SYNC: // 0:
					{
//						Switch resetThisSafe = (Switch) view
//								.findViewById(R.id.safe_profile_drawer_switch);
						peer = D_Peer.getPeerByPeer_Keep(peer);
						peer.setLastSyncDate(null);
						peer.storeRequest();
						peer.releaseReference();

						for (D_PeerInstance i : peer._instances.values()) {
							Calendar date = i.get_last_sync_date();
							Log.i(TAG, "last sync date: " + date);
						}
						// resetThisSafe.setChecked(false);
						drawerContent = prepareDrawer(hasSK);
						sAdapter.setData(drawerContent);
					}
					break;

					case SafeProfileActivity.SAFE_DRAWER____HIDE_TOGGLE: // 1:
					{
//						Switch hideThisSafe = (Switch) view
//								.findViewById(R.id.safe_profile_drawer_switch);
//						D_Peer.setHidden(peer, hideThisSafe.isChecked());
//						hideThisSafe
//								.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//									@Override
//									public void onCheckedChanged(
//											CompoundButton buttonView,
//											boolean isChecked) {
//										D_Peer.setHidden(peer, isChecked);
//									}
//								});
					}
					drawerState[SAFE_DRAWER____HIDE_TOGGLE] = !drawerState[SAFE_DRAWER____HIDE_TOGGLE];
					D_Peer.setHidden(peer, drawerState[SAFE_DRAWER____HIDE_TOGGLE]);
					drawerContent = prepareDrawer(hasSK);
					sAdapter.setData(drawerContent);
					break;

				case SafeProfileActivity.SAFE_DRAWER____ACCESS: // 2:
				{
//					Switch accessIt = (Switch) view
//							.findViewById(R.id.safe_profile_drawer_switch);
//					D_Peer.setUsed(peer, accessIt.isChecked());
//					accessIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//						@Override
//						public void onCheckedChanged(CompoundButton buttonView,
//													 boolean isChecked) {
//							D_Peer.setUsed(peer, isChecked);
//						}
//					});
				}
				drawerState[SAFE_DRAWER____ACCESS] = !drawerState[SAFE_DRAWER____ACCESS];
				D_Peer.setUsed(peer, drawerState[SAFE_DRAWER____ACCESS]);
				drawerContent = prepareDrawer(hasSK);
				sAdapter.setData(drawerContent);
					break;

				case SafeProfileActivity.SAFE_DRAWER____BLOCK: // 3:
				{
//					Switch blockIt = (Switch) view
//							.findViewById(R.id.safe_profile_drawer_switch);
//					boolean chstate = blockIt.isChecked();
//					D_Peer.setBlocked(peer, chstate);
//					Log.d(TAG, "SafeProfileAct: onClick: block=" + peer.getBlocked() + " after setting " + chstate);
//					blockIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//						@Override
//						public void onCheckedChanged(CompoundButton buttonView,
//													 boolean isChecked) {
//							D_Peer.setBlocked(peer, isChecked);
//							Log.d(TAG, "SafeProfileAct: onChecked: block=" + peer.getBlocked() + " after setting " + isChecked);
//						}
//					});
				}
				drawerState[SAFE_DRAWER____BLOCK] = !drawerState[SAFE_DRAWER____BLOCK];
				D_Peer.setBlocked(peer, drawerState[SAFE_DRAWER____BLOCK]);
				drawerContent = prepareDrawer(hasSK);
				sAdapter.setData(drawerContent);
					break;

				case SafeProfileActivity.SAFE_DRAWER____SERVE: // 4:
				{
//					Switch serveIt = (Switch) view
//							.findViewById(R.id.safe_profile_drawer_switch);
//					D_Peer.setUsed(peer, serveIt.isChecked());
//					serveIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//						@Override
//						public void onCheckedChanged(CompoundButton buttonView,
//													 boolean isChecked) {
//							D_Peer.setUsed(peer, isChecked);
//						}
//					});
				}
				drawerState[SAFE_DRAWER____SERVE] = !drawerState[SAFE_DRAWER____SERVE];
				D_Peer.setBroadcastableMy(peer, drawerState[SAFE_DRAWER____SERVE]);
				drawerContent = prepareDrawer(hasSK);
				sAdapter.setData(drawerContent);
					break;
				default:
					break;
				}
			}
		}
	}

	private class SafeProfileAdapter extends BaseAdapter {

		private Activity activity;
		private LayoutInflater inflater = null;
		private String[] data;
		private boolean[] state;

		public SafeProfileAdapter(Activity _activity, String[] _data, boolean[] _state) {
			activity = _activity;
			data = _data;
			state = _state;
			inflater = (LayoutInflater) activity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		}

		public void setData(String[] _data) {
			data = _data;
			this.notifyDataSetChanged();
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
			if (true || (peer.getSK() != null)) {
				content.setText(data[position]);
				//s.setPressed(state[position]);
			} else {
				content.setText(data[position]);
				if (position > 0) {
					v = inflater.inflate(
							R.layout.safe_profile_drawer_row_switch, null);
					Switch s = (Switch) v
							.findViewById(R.id.safe_profile_drawer_switch);
					s.setText(data[position]);
					//s.setPressed(state[position]);
					s.setChecked(state[position]);
					Log.d(TAG, "SafeProfileAdapt: getView: state="+state[position]);

                    Switch blockIt = (Switch) v
                            .findViewById(R.id.safe_profile_drawer_switch);
                    boolean chstate = blockIt.isChecked();

					switch (position) {
					case SafeProfileActivity.SAFE_DRAWER____BLOCK: // 3:
						//D_Peer.setBlocked(peer, chstate);
						Log.d(TAG, "SafeProfileAdapt: getView: block="+peer.getBlocked()+" after setting "+chstate);
						//blockIt.setOnClickListener(new View.OnClickListener() {
						blockIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {


//							@Override
//							public void onClick(View v) {
//								Switch blockIt = (Switch) v
//										.findViewById(R.id.safe_profile_drawer_switch);
//								final CompoundButton buttonView_ = blockIt;
//								
//							boolean isChecked = true;
////							}
							@Override
							public void onCheckedChanged(CompoundButton buttonView,
									boolean isChecked) {
                                //Toast.makeText(getApplicationContext(), "Setting Block = "+isChecked, Toast.LENGTH_SHORT).show();
								final CompoundButton buttonView_ = buttonView;
						    	boolean _isChecked = ! peer.getBlocked();
                                if (isChecked)
                                    cnt_try_false = 0;
                                if (! _isChecked) {
                                    if (cnt_try_false++ % 5 != 0) {
                                        buttonView_.setChecked(!_isChecked);
                                        Toast.makeText(getApplicationContext(), "Try again "+(5-((cnt_try_false-1)%5))+" times to set blocking to " + _isChecked, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                                Toast.makeText(getApplicationContext(), "Setting Block = "+isChecked, Toast.LENGTH_SHORT).show();

				                AlertDialog.Builder confirm = new AlertDialog.Builder(SafeProfileActivity.this);
				                confirm.setTitle("Do you wish to change blocking?");
				                confirm.setMessage("Do you want to set blocking of \""+peer.getName()+"\" to " + _isChecked + ((isChecked != _isChecked)?"!":""))
				                    .setCancelable(false)
								    .setPositiveButton("Yes", new MyDialog_OnClickListener("Dia 2") {
									    public void _onClick(DialogInterface dialog, int id) {
									    	
									    	boolean isChecked = ! peer.getBlocked();
											D_Peer.setBlocked(peer, isChecked);
											Log.d(TAG, "SafeProfileAdapt: getView onChecked: block="+peer.getBlocked()+" after setting "+isChecked);
								    		buttonView_.setChecked(isChecked);
								
									    	dialog.cancel();
									    }
								    })
								    .setNegativeButton("No",new DialogInterface.OnClickListener() {
								    	public void onClick(DialogInterface dialog,int id) {
									    	boolean isChecked = peer.getBlocked();
								    		buttonView_.setChecked(isChecked);
								    		dialog.cancel();
								    		return;
								    	}
								    });
			                
				                AlertDialog confirmDialog = confirm.create();
				                confirmDialog.show();
								
								//Util.printCallPath("");
							}
						});
						break;

                        case SafeProfileActivity.SAFE_DRAWER____ACCESS: // 2:
                            //D_Peer.setUsed(peer, chstate);
                            Log.d(TAG, "SafeProfileAdapt: getView: access="+peer.getUsed()+" after setting "+chstate);
                            blockIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView,
                                                             boolean isChecked) {
                                    //Toast.makeText(getApplicationContext(), "Setting Access = "+isChecked, Toast.LENGTH_SHORT).show();
                                    final CompoundButton buttonView_ = buttonView;
                                    boolean _isChecked = ! peer.getUsed();
                                    if (isChecked)
                                        cnt_try_false = 0;
                                    if (! _isChecked) {
                                        if (cnt_try_false++ % 5 != 0) {
                                            buttonView_.setChecked(!_isChecked);
                                            Toast.makeText(getApplicationContext(), "Try again "+(5-((cnt_try_false-1)%5))+" times to set access to " + _isChecked, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }
                                    Toast.makeText(getApplicationContext(), "Setting Access = "+isChecked, Toast.LENGTH_SHORT).show();

                                    AlertDialog.Builder confirm = new AlertDialog.Builder(SafeProfileActivity.this);
                                    confirm.setTitle("Do you wish to change access?");
                                    confirm.setMessage("Do you want to set access of \""+peer.getName()+"\" to " + _isChecked + ((isChecked != _isChecked)?"!":""))
                                            .setCancelable(false)
                                            .setPositiveButton("Yes", new MyDialog_OnClickListener("Dia 2") {
                                                public void _onClick(DialogInterface dialog, int id) {

                                                    boolean isChecked = ! peer.getUsed();
                                                    D_Peer.setUsed(peer, isChecked);
                                                    Log.d(TAG, "SafeProfileAdapt: getView onChecked: access="+peer.getUsed()+" after setting "+isChecked);
                                                    buttonView_.setChecked(isChecked);

                                                    dialog.cancel();
                                                }
                                            })
                                            .setNegativeButton("No",new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,int id) {
                                                    boolean isChecked = peer.getUsed();
                                                    buttonView_.setChecked(isChecked);
                                                    dialog.cancel();
                                                    return;
                                                }
                                            });

                                    AlertDialog confirmDialog = confirm.create();
                                    confirmDialog.show();

                                    //Util.printCallPath("");
                                }
                            });
                            break;

                        case SafeProfileActivity.SAFE_DRAWER____SERVE: // 2:
                            //D_Peer.setBroadcastableMy(peer, chstate);
                            Log.d(TAG, "SafeProfileAdapt: getView: serve="+peer.getBroadcastableMyOrDefault()+" after setting "+chstate);
                            blockIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView,
                                                             boolean isChecked) {
                                    //Toast.makeText(getApplicationContext(), "Setting Serve = "+isChecked, Toast.LENGTH_SHORT).show();
                                    final CompoundButton buttonView_ = buttonView;
                                    boolean _isChecked = ! peer.getBroadcastableMyOrDefault();
                                    if (isChecked)
                                        cnt_try_false = 0;
                                    if (! _isChecked) {
                                        if (cnt_try_false++ % 5 != 0) {
                                            buttonView_.setChecked(!_isChecked);
                                            Toast.makeText(getApplicationContext(), "Try again "+(5-((cnt_try_false-1)%5))+" times to set serve to " + _isChecked, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }
                                    Toast.makeText(getApplicationContext(), "Setting Serve = "+isChecked, Toast.LENGTH_SHORT).show();

                                    AlertDialog.Builder confirm = new AlertDialog.Builder(SafeProfileActivity.this);
                                    confirm.setTitle("Do you wish to change serve?");
                                    confirm.setMessage("Do you want to set serve of \""+peer.getName()+"\" to " + _isChecked + ((isChecked != _isChecked)?"!":""))
                                            .setCancelable(false)
                                            .setPositiveButton("Yes", new MyDialog_OnClickListener("Dia 3") {
                                                public void _onClick(DialogInterface dialog, int id) {

                                                    boolean isChecked = ! peer.getBroadcastableMyOrDefault();
                                                    D_Peer.setBroadcastableMy(peer, isChecked);
                                                    Log.d(TAG, "SafeProfileAdapt: getView onChecked: serve="+peer.getBroadcastableMyOrDefault()+" after setting "+isChecked);
                                                    buttonView_.setChecked(isChecked);

                                                    dialog.cancel();
                                                }
                                            })
                                            .setNegativeButton("No",new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,int id) {
                                                    boolean isChecked = peer.getBroadcastableMyOrDefault();
                                                    buttonView_.setChecked(isChecked);
                                                    dialog.cancel();
                                                    return;
                                                }
                                            });

                                    AlertDialog confirmDialog = confirm.create();
                                    confirmDialog.show();

                                    //Util.printCallPath("");
                                }
                            });
                            break;

                        case SafeProfileActivity.SAFE_DRAWER____HIDE_TOGGLE: // 0:
                            //D_Peer.setHidden(peer, chstate);
                            Log.d(TAG, "SafeProfileAdapt: getView: hidden="+peer.getHidden()+" after setting "+chstate);
                            blockIt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView,
                                                             boolean isChecked) {
                                    //Toast.makeText(getApplicationContext(), "Setting Hidden = "+isChecked, Toast.LENGTH_SHORT).show();
                                    final CompoundButton buttonView_ = buttonView;
                                    boolean _isChecked = ! peer.getHidden();
                                    if (isChecked)
                                        cnt_try_false = 0;
                                    if (! _isChecked) {
                                        if (cnt_try_false ++ % 5 != 0) {
                                            buttonView_.setChecked(!_isChecked);
                                            Toast.makeText(getApplicationContext(), "Try again "+(5-((cnt_try_false-1)%5))+" times to set hidden to " + _isChecked, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }
                                    Toast.makeText(getApplicationContext(), "Setting Hidden = "+isChecked, Toast.LENGTH_SHORT).show();

                                    AlertDialog.Builder confirm = new AlertDialog.Builder(SafeProfileActivity.this);
                                    confirm.setTitle("Do you wish to change hidden?");
                                    confirm.setMessage("Do you want to set hidden of \""+peer.getName()+"\" to " + _isChecked + ((isChecked != _isChecked)?"!":""))
                                            .setCancelable(false)
                                            .setPositiveButton("Yes", new MyDialog_OnClickListener("Dia 0") {
                                                public void _onClick(DialogInterface dialog, int id) {

                                                    boolean isChecked = ! peer.getHidden();
                                                    D_Peer.setHidden(peer, isChecked);
                                                    Log.d(TAG, "SafeProfileAdapt: getView onChecked: hidden="+peer.getHidden()+" after setting "+isChecked);
                                                    buttonView_.setChecked(isChecked);

                                                    dialog.cancel();
                                                }
                                            })
                                            .setNegativeButton("No",new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,int id) {
                                                    boolean isChecked = peer.getHidden();
                                                    buttonView_.setChecked(isChecked);
                                                    dialog.cancel();
                                                    return;
                                                }
                                            });

                                    AlertDialog confirmDialog = confirm.create();
                                    confirmDialog.show();

                                    //Util.printCallPath("");
                                }
                            });
                            break;
					}
					
				}
			}

			return v;
		}

	}

}
