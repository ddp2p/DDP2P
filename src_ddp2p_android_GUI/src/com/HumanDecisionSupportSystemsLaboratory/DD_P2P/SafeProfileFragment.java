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

import net.ddp2p.common.util.DD_SK;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.ASN1.Encoder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.ddp2p.ciphersuits.KeyManagement;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;

public class SafeProfileFragment extends Fragment {

	private static final String TAG = "SafeProfile";
	private ImageView imgbut;
	private TextView whoText;
	// private int safe_id;
	private String safe_gidh;
	private String safe_lid;
	private D_Peer peer;
	private String whoStr;
	private TextView ipAddress;
	private TextView lastContact;
	private TextView email;
	private TextView device;
	private TextView slogan;
	
	private int SELECT_PROFILE_PHOTO = 10;
	private int SELECT_PPROFILE_PHOTO_KITKAT = 11;
	private String selectedImagePath;
	private File selectImageFile;
	public final static String SAFE_PROFILE_SLOGAN = "slogan";

/*	private Button setMyself;
	private Button exportAddress;
	private Button selectDirectoryServer;
	private Button resetLastSyncDate;*/
	private Button sendMsg;
	private Button setProfilePhoto;

/*	private Switch hideThisSafe;
	private Switch accessIt;
	private Switch blockIt;
	private Switch serveIt;*/

/*	private ActionBarDrawerToggle drawerToggle;
	private DrawerLayout drawerLayout = null;*/

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.safe_profile, container);

		// retrieve text and image from main activity
		imgbut = (ImageView) v.findViewById(R.id.profImg); // profile img
		whoText = (TextView) v.findViewById(R.id.profName); // profile name
        email = (TextView) v.findViewById(R.id.safe_profile_email);
        slogan = (TextView) v.findViewById(R.id.safe_profile_slogan);
        device = (TextView) v.findViewById(R.id.safe_profile_device);
      	
		
		Intent i = getActivity().getIntent();
		Bundle b = i.getExtras();

		whoStr = b.getString("who");
		safe_gidh = b.getString(Safe.P_SAFE_GIDH);
		safe_lid = b.getString(Safe.P_SAFE_LID);

		// safe_id = b.getInt(Safe.P_SAFE_ID) + 1;

		whoText.setText(whoStr);

		peer = D_Peer.getPeerByLID(safe_lid, true, false);
		boolean gotIcon = false;
		try {
			byte[] icon = peer.getIcon();
			if (icon != null) {
				Bitmap bmp = BitmapFactory.decodeByteArray(icon, 0,
						icon.length - 1);
				imgbut.setImageBitmap(bmp);
				gotIcon = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!gotIcon) {
			int imgPath = Integer.parseInt(b.getString("profImg"));
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), imgPath);
			imgbut.setImageBitmap(bmp);
		}

		imgbut.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				byte[] icon = peer.getIcon();
				if (icon != null) {
					ImageFragment fragment = ImageFragment.newInstance(icon);
					FragmentManager fm = getActivity()
							.getSupportFragmentManager();
					fragment.show(fm, "icon");
				}

				if (icon == null) {
					int imgPath = R.drawable.placeholder;
					Bitmap bmp = BitmapFactory.decodeResource(getResources(),
							imgPath);
					icon = PhotoUtil.BitmapToByteArray(bmp, 100);
					ImageFragment fragment = ImageFragment.newInstance(icon);
					FragmentManager fm = getActivity()
							.getSupportFragmentManager();
					fragment.show(fm, "icon");
				}

			}
		});

		email.setText(peer.getEmail());
		slogan.setText(peer.getSlogan());
		String instances = null;
		String contacts_Date = null;
		String sync_Date = null;
		for (String _inst : peer._instances.keySet()) {
			String contact = Encoder.getGeneralizedTime(peer._instances.get(_inst).get_last_contact_date());
			String sync = peer._instances.get(_inst).get_last_sync_date_str();
			if (contact == null) contact = Util.__("NEVER");
			if (sync == null) sync = Util.__("NEVER");
			if (instances == null) {
				instances = _inst; 
				contacts_Date = contact;
				sync_Date = sync;
			} else {
				instances += ", " + _inst;
				contacts_Date += ", " + contact;
				sync_Date += ", " + sync;
			}
		}
		device.setText(Util.getStringNonNullUnique(peer.getInstance())+" / {"+instances+"}");
		
		if (peer.getSK() != null) {
			ipAddress = (TextView) v.findViewById(R.id.safe_profile_ip);
			ipAddress.setText("Known Secret Key");

			setProfilePhoto = (Button) v
					.findViewById(R.id.safe_profile_set_profile_photo);
			setProfilePhoto.setVisibility(View.VISIBLE);
			setProfilePhoto.setOnClickListener(new OnClickListener() {

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
						startActivityForResult(intent,
								SELECT_PPROFILE_PHOTO_KITKAT);
					}
				}
			});

		} else {
			lastContact = (TextView) v
					.findViewById(R.id.safe_profile_last_contact);
			lastContact.setText(contacts_Date);//peer.getLastSyncDate(safe_lid));

			ipAddress = (TextView) v.findViewById(R.id.safe_profile_ip);
			ipAddress.setText(sync_Date);
			
			/*
			 * hideThisSafe = (Switch)
			 * v.findViewById(R.id.switch_hide_this_safe);
			 * hideThisSafe.setVisibility(View.VISIBLE);
			 * 
			 * if (peer.getHidden() == false) hideThisSafe.setChecked(false);
			 * else hideThisSafe.setChecked(true);
			 * 
			 * hideThisSafe .setOnCheckedChangeListener(new
			 * OnCheckedChangeListener() {
			 * 
			 * @Override public void onCheckedChanged(CompoundButton buttonView,
			 * boolean isChecked) { if (isChecked) { D_Peer.setHidden(peer,
			 * true); peer.storeRequest(); peer.releaseReference(); } else {
			 * D_Peer.setHidden(peer, false); peer.storeRequest();
			 * peer.releaseReference(); }
			 * 
			 * } });
			 * 
			 * resetLastSyncDate = (Button) v
			 * .findViewById(R.id.button_reset_LastSyncDate);
			 * resetLastSyncDate.setVisibility(View.VISIBLE);
			 * resetLastSyncDate.setOnClickListener(new OnClickListener() {
			 * 
			 * @Override public void onClick(View v) { peer =
			 * D_Peer.getPeerByPeer_Keep(peer); peer.setLastSyncDate(null);
			 * peer.storeRequest(); peer.releaseReference();
			 * 
			 * for (D_PeerInstance i : peer._instances.values()) { Calendar date
			 * = i.get_last_sync_date(); Log.i("last_sync_date",
			 * "last sync date: " + date); } } });
			 */

			sendMsg = (Button) v.findViewById(R.id.button_send_msg);
			sendMsg.setVisibility(View.VISIBLE);
			sendMsg.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					Intent myIntent = new Intent();
					myIntent.setClass(getActivity(), Chat.class);

					// pass data to chat
					myIntent.putExtra("who", whoStr);
					// myIntent.putExtra(Safe.P_SAFE_ID, safe_id);
					myIntent.putExtra(Safe.P_SAFE_LID, safe_lid);
					myIntent.putExtra(Safe.P_SAFE_GIDH, safe_gidh);
					myIntent.putExtra("profImg",
							String.valueOf(R.drawable.placeholder));
					startActivity(myIntent);
				}
			});

			/*
			 * // set up switch access accessIt = (Switch)
			 * v.findViewById(R.id.switch_access);
			 * accessIt.setVisibility(View.VISIBLE); if (peer.getUsed() == true)
			 * accessIt.setChecked(true); if (peer.getUsed() == false)
			 * accessIt.setChecked(false);
			 * 
			 * accessIt.setOnCheckedChangeListener(new OnCheckedChangeListener()
			 * {
			 * 
			 * @Override public void onCheckedChanged(CompoundButton buttonView,
			 * boolean isChecked) { if (isChecked) { D_Peer.setUsed(peer, true);
			 * } else { D_Peer.setUsed(peer, false); } } });
			 * 
			 * // set up switch block blockIt = (Switch)
			 * v.findViewById(R.id.switch_block);
			 * blockIt.setVisibility(View.VISIBLE); if (peer.getBlocked() ==
			 * true) blockIt.setChecked(true); if (peer.getBlocked() == false)
			 * blockIt.setChecked(false);
			 * 
			 * blockIt.setOnCheckedChangeListener(new OnCheckedChangeListener()
			 * {
			 * 
			 * @Override public void onCheckedChanged(CompoundButton buttonView,
			 * boolean isChecked) { if (isChecked) { D_Peer.setBlocked(peer,
			 * true); } else { D_Peer.setBlocked(peer, false); } } });
			 * 
			 * // set up switch serve serveIt = (Switch)
			 * v.findViewById(R.id.switch_serve);
			 * serveIt.setVisibility(View.VISIBLE);
			 * 
			 * if (peer.getUsed() == true) serveIt.setChecked(true); if
			 * (peer.getUsed() == false) serveIt.setChecked(false);
			 * 
			 * serveIt.setOnCheckedChangeListener(new OnCheckedChangeListener()
			 * {
			 * 
			 * @Override public void onCheckedChanged(CompoundButton buttonView,
			 * boolean isChecked) { if (isChecked) { D_Peer.setUsed(peer, true);
			 * peer.storeRequest(); peer.releaseReference(); } else {
			 * D_Peer.setUsed(peer, false); peer.storeRequest();
			 * peer.releaseReference(); } } });
			 */
		}

		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			Intent resultData) {
		if (peer.getSK() == null) {
			return;
		}
		if (resultCode == Activity.RESULT_OK && resultData != null) {
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
				getActivity().getContentResolver()
						.takePersistableUriPermission(uri, takeFlags);
			}

			selectedImagePath = FileUtils.getPath(getActivity(), uri);
			Log.i("path", "path: " + selectedImagePath);

			selectImageFile = new File(selectedImagePath);

/*			Bitmap bmp = BitmapFactory.decodeFile(selectedImagePath);*/
			// String strFromBmp = PhotoUtil.BitmapToString(bmp);

			Bitmap bmp = PhotoUtil.decodeSampledBitmapFromFile(selectedImagePath, 80, 80);
			//TODO fix the loading image
			byte[] icon;
			icon = PhotoUtil.BitmapToByteArray(bmp, 100);
	/*		int quality = 100;*/
			Log.i(TAG, "SafeProfile: Icon length=" + icon.length);
			/*DD.MAX_PEER_ICON_LENGTH = 30000;
			while (icon.length > DD.MAX_PEER_ICON_LENGTH && quality > 0) {
				quality -= 5;
				icon = PhotoUtil.BitmapToByteArray(bmp, quality);
				Log.i(TAG, "SafeProfile: Icon length=" + icon.length
						+ " quality=" + quality);
			}
			Log.i(TAG, "SafeProfile: Icon length=" + icon.length + " quality="
					+ quality);// Util.stringSignatureFromByte(icon));
*/			if (peer != null) {
				peer = D_Peer.getPeerByPeer_Keep(peer);
			}
			if (peer != null) {
				if (peer.getSK() != null) {
					if (peer.setIcon(icon)) {
						peer.sign();
						peer.storeRequest();
					}
					peer.releaseReference();
				}
			}

			imgbut.setImageBitmap(bmp);

		}
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

}
