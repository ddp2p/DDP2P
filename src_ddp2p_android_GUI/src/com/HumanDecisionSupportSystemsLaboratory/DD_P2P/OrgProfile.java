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

import net.ddp2p.ciphersuits.RSA;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.DD_SK;
import net.ddp2p.common.util.DD_SK_Entry;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.CipherSuit;
import net.ddp2p.ciphersuits.ECDSA;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_FieldValue;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OrgParam;
import net.ddp2p.common.data.D_Organization;

public class OrgProfile extends FragmentActivity {
	private static int organization_position;
	private static String organization_LID;
	private static String organization_GIDH;
	long oLID;
	D_Organization org;
	public static CipherSuit[] __keys;
	private int SELECT_PROFILE_PHOTO = 10;
	private int SELECT_PPROFILE_PHOTO_KITKAT = 11;
	private ImageView setProfilePhoto;
	private String selectedImagePath;
	private ImageView icon;

	private static byte[] byteIcon;
	public static int _selectedKey = 0;
	private EditText forename;
	private EditText surname;
	private EditText email;
	private EditText slogan;
	private CheckedTextView hasRightToVote;
	private Button neiborhood;
	private Button submit;
	private Button submit_new;
    private TextView profilePic;
    private TextView profileOrgName;
    private WebView profileInstructions;
	private ImageView profilePicImg;
	private Spinner keys;
	// private Spinner eligibility;
	private LinearLayout custom_fields;
	private int custom_index;
	private D_OrgParam[] custom_params;
	private long constituent_LID;
	D_Constituent constituent = null;
	private static String TAG = "Profile";
	// content in spinner
	public static final String[] m = { "Secure ECDSA 512", "Medium ECDSA 256",
			"Fast RSA 1024", "Fast Insecure Toy RSA 150" };

	// private static final String[] mm={"",""};

	public static CipherSuit newCipherSuit(String _cipher, String _hash_alg, int _ciphersize) {
		CipherSuit cs = new CipherSuit();
		cs.cipher = _cipher;
		cs.hash_alg = _hash_alg;
		cs.ciphersize = _ciphersize;
		return cs;
	}

	public final static int KEY_IDX_ECDSA_BIG = 0;
	public final static int KEY_IDX_ECDSA = 1;
	public final static int KEY_IDX_RSA = 2;
	public final static int KEY_IDX_RSA_FAST = 3;
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Intent i = this.getIntent();
		Bundle b = i.getExtras();

		__keys = new CipherSuit[4];
		__keys[KEY_IDX_ECDSA_BIG] = newCipherSuit(Cipher.ECDSA, Cipher.SHA384, ECDSA.P_521);
		__keys[KEY_IDX_ECDSA]     = newCipherSuit(Cipher.ECDSA, Cipher.SHA1, ECDSA.P_256);
		__keys[KEY_IDX_RSA]       = newCipherSuit(Cipher.RSA, Cipher.SHA512, 1024);
		__keys[KEY_IDX_RSA_FAST]       = newCipherSuit(Cipher.RSA, Cipher.MD5, 150);

		// top panel setting
		organization_position = b.getInt(Orgs.O_ID);
		organization_LID = b.getString(Orgs.O_LID);
		organization_GIDH = b.getString(Orgs.O_GIDH);

		oLID = Util.lval(organization_LID, -1);
		if (oLID <= 0)
			return;
		this.org = D_Organization.getOrgByLID(oLID, true, false);
		if (org == null)
			return;

		try {
			Identity crt_identity = Identity.getCurrentConstituentIdentity();
			if (crt_identity == null) {
				Log.d(TAG, "No identity");
			} else
				constituent_LID = net.ddp2p.common.config.Identity
						.getDefaultConstituentIDForOrg(oLID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}

		if (constituent_LID > 0) {
			constituent = D_Constituent.getConstByLID(constituent_LID, true,
					false);
			Log.d(TAG, "Got const: " + constituent);
		}

		setContentView(R.layout.org_profile);

        this.profileInstructions = (WebView) findViewById(R.id.profile_instructions);
        this.profileOrgName = (TextView) findViewById(R.id.profile_orgname);
        if (org.getName() != null) {
            profileOrgName.setText(org.getName());
            profileOrgName.setVisibility(View.VISIBLE);
        }
        if (org.getInstructionsRegistration() != null) {
            //profileInstructions.loadData(org.getInstructionsRegistration(), "text/html", null);
            profileInstructions.loadUrl("data:text/html;charset=UTF-8,"+org.getInstructionsRegistration());
            profileInstructions.setVisibility(View.VISIBLE);
        }

        forename = (EditText) findViewById(R.id.profile_furname);
		surname = (EditText) findViewById(R.id.profile_surname);
		neiborhood = (Button) findViewById(R.id.profile_neiborhood);
		submit = (Button) findViewById(R.id.submit_profile);
		submit_new = (Button) findViewById(R.id.submit_profile_new);
		if (constituent == null)
			submit.setVisibility(Button.GONE);
		else
			submit.setVisibility(Button.VISIBLE);
		keys = (Spinner) findViewById(R.id.profile_keys);
		hasRightToVote = (CheckedTextView) findViewById(R.id.profile_hasRightToVote);
		email = (EditText) findViewById(R.id.profile_email);
		slogan = (EditText) findViewById(R.id.profile_slogan);
		slogan.setActivated(false);
		profilePic = (TextView) findViewById(R.id.profile_picture);
		profilePicImg = (ImageView) findViewById(R.id.profile_picture_img);
		// eligibility = (Spinner) findViewById(R.id.profile_eligibility);

		if (constituent != null) {
			forename.setText(constituent.getForename());
			surname.setText(constituent.getSurname());
			hasRightToVote
					.setChecked(Util.ival(constituent.getWeight(), 0) > 0);
			email.setText(constituent.getEmail());
			slogan.setText(constituent.getSlogan());
		}

		custom_fields = (LinearLayout) findViewById(R.id.profile_view);
		custom_index = 8;
		// custom_fields = (LinearLayout) findViewById(R.id.profile_custom);
		// custom_index = 0;

		custom_params = org.params.orgParam;

		if (custom_params == null || custom_params.length == 0) {
			custom_params = new D_OrgParam[0];// 3
			/*
			 * custom_params[0] = new D_OrgParam(); custom_params[0].label =
			 * "School"; custom_params[0].entry_size = 5; custom_params[1] = new
			 * D_OrgParam(); custom_params[1].label = "Street"; custom_params[2]
			 * = new D_OrgParam(); custom_params[2].label = "Year";
			 * custom_params[2].list_of_values = new
			 * String[]{"2010","2011","2012"};
			 */
		}
		D_FieldValue[] field_values = null;
		if (constituent != null && constituent.address != null)
			field_values = constituent.address;
		for (int crt_field = 0; crt_field < custom_params.length; crt_field++) {
			D_OrgParam field = custom_params[crt_field];
			LinearLayout custom_entry = new LinearLayout(this);
			custom_entry.setOrientation(LinearLayout.HORIZONTAL);
			custom_entry.setLayoutParams(new LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			TextView custom_label = new TextView(this);
			custom_label.setText(field.label);
			custom_entry.addView(custom_label);

			if (field.list_of_values != null && field.list_of_values.length > 0) {
				Log.d(TAG, "spinner:" + field);
				Spinner custom_spin = new Spinner(this);
				ArrayAdapter<String> custom_spin_Adapter = new ArrayAdapter<String>(
						this, android.R.layout.simple_spinner_item,
						field.list_of_values);
				custom_spin_Adapter
						.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				custom_spin.setAdapter(custom_spin_Adapter);
				custom_entry.addView(custom_spin);
				D_FieldValue fv = locateFV(field_values, field);
				if (fv != null) {
					int position = 0;
					for (int k = 0; k <= field.list_of_values.length; k++) {
						if (Util.equalStrings_null_or_not(
								field.list_of_values[k], fv.value)) {
							position = k;
							break;
						}
					}
					custom_spin.setSelection(position);
				}
			} else {
				Log.d(TAG, "edit: " + field);
				EditText edit_text = new EditText(this);
				edit_text.setText(field.default_value);
				edit_text.setInputType(InputType.TYPE_CLASS_TEXT);
				if (field.entry_size > 0)
					edit_text.setMinimumWidth(field.entry_size * 60);
				Log.d(TAG, "edit: size=" + field.entry_size);
				custom_entry.addView(edit_text);
				// Button child = new Button(this);
				// child.setText("Test");
				D_FieldValue fv = locateFV(field_values, field);
				if (fv != null)
					edit_text.setText(fv.value);
			}
			custom_fields.addView(custom_entry, custom_index++);
		}

		ArrayAdapter<String> keysAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, m);
		keysAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		keys.setAdapter(keysAdapter);
		keys.setOnItemSelectedListener(new KeysListener());

		if (constituent != null) {
			SK sk = constituent.getSK();
			if (sk != null) {
				Cipher cipher = Cipher.getCipher(sk, null);
				if (cipher instanceof net.ddp2p.ciphersuits.RSA) {
					RSA ecdsa = (RSA) cipher;
					CipherSuit e = RSA.getCipherSuite(ecdsa.getPK());
					if (e.hash_alg == Cipher.MD5)
						keys.setSelection(KEY_IDX_RSA_FAST, true);
					else
						keys.setSelection(KEY_IDX_RSA, true);
				}
				if (cipher instanceof net.ddp2p.ciphersuits.ECDSA) {
					ECDSA ecdsa = (ECDSA) cipher;
					CipherSuit e = ECDSA.getCipherSuite(ecdsa.getPK());
					if (e.hash_alg == Cipher.SHA1) {
						keys.setSelection(KEY_IDX_ECDSA, true);
					} else {
						keys.setSelection(KEY_IDX_ECDSA_BIG, true);
					}
				}
			}
		}
		
		ArrayAdapter<String> eligibilityAdapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, m);
		eligibilityAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// eligibility.setAdapter(eligibilityAdapter);
		// eligibility.setOnItemSelectedListener(new EligibilityListener());

		hasRightToVote.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				hasRightToVote.setChecked(!hasRightToVote.isChecked());
			}
		});

        setProfilePhoto = (ImageView) findViewById(R.id.org_profile_set_profile_photo);
        /*TODO this part only make the whole stuff slow       
		if (constituent_LID > 0) {
			constituent = D_Constituent.getConstByLID(constituent_LID, true,
					true);
			Log.d(TAG, "Got const: " + constituent);
		}
		*/
/*		boolean gotIcon = false;
		if (constituent != null) {
			if (constituent.getPicture() != null) {
				byte[] icon = constituent.getPicture();
				Bitmap bmp = BitmapFactory.decodeByteArray(icon, 0,
						icon.length - 1);
				setProfilePhoto.setImageBitmap(bmp);
				gotIcon = true;
			}

			if (!gotIcon) {
				int imgPath = R.drawable.constitutent_person_icon;
				Bitmap bmp = BitmapFactory.decodeResource(getResources(),
						imgPath);
				setProfilePhoto.setImageBitmap(bmp);
			}
		} else {
			int imgPath = R.drawable.constitutent_person_icon;
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), imgPath);
			setProfilePhoto.setImageBitmap(bmp);
		}

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
					startActivityForResult(intent, SELECT_PPROFILE_PHOTO_KITKAT);
				}
			}
		});*/

		submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String _forename = forename.getText().toString();
				String _surname = surname.getText().toString();
				int _keys = OrgProfile._selectedKey;
				boolean rightToVote = hasRightToVote.isChecked();
				String _weight = rightToVote ? "1" : "0";
				String _email = email.getText().toString();
				String _slogan = slogan.getText().toString();
				boolean external = false;

				if (constituent == null) {
					D_Constituent new_const = D_Constituent.createConstituent(
							_forename, _surname, _email, oLID, external,
							_weight, _slogan, OrgProfile.__keys[_keys].cipher,
							OrgProfile.__keys[_keys].hash_alg,
							OrgProfile.__keys[_keys].ciphersize, null, null);

					Log.d(TAG, "saved constituent=" + new_const.getNameFull());
					try {
						// Identity.DEBUG = true;
						Identity.setCurrentConstituentForOrg(
								new_const.getLID(), oLID);
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
					Log.d(TAG, "saved new constituent=" + new_const);
					constituent = new_const;
				} else {
					constituent = D_Constituent
							.getConstByConst_Keep(constituent);
					constituent.setEmail(_email);
					constituent.setForename(_forename);
					constituent.setSurname(_surname);
					constituent.setWeight(rightToVote);
					constituent.setSlogan(_slogan);
					constituent.setExternal(false);
                    constituent.setCreationDate();
					constituent.sign();
					if (constituent.dirty_any())
						constituent.storeRequest();
					constituent.releaseReference();
					Log.d(TAG, "saved constituent=" + constituent);
					try {
						// Identity.DEBUG = true;
						Identity.setCurrentConstituentForOrg(
								constituent.getLID(), oLID);
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
					Log.d(TAG, "saved constituent=" + constituent.getLID()
							+ " oLID=" + oLID);
				}
				if (constituent != null) {
					constituent = D_Constituent
							.getConstByConst_Keep(constituent);
					if (constituent != null) {
						if (constituent.getSK() != null) {
							constituent.setPicture(byteIcon);
                            constituent.setCreationDate();
							constituent.sign();
							constituent.storeRequest();
							constituent.releaseReference();
							Log.d(TAG,
									"saved constituent pic: "
											+ constituent.getPicture());
						}
					}
				}

                if (! org.getBroadcasted()) {
                    D_Organization _org = D_Organization.getOrgByOrg_Keep(org);
                    if (_org != null) {
                        _org.setBroadcasting(true);
                        if (_org.dirty_any()) _org.storeRequest();
                        _org.releaseReference();
                        org = _org;
                    }
                }
				Log.d(TAG, "saved constituent pic: " + constituent.getPicture());
				Log.d(TAG, "saved constituent Done");
				finish();
			}
		});
		submit_new.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String _forename = forename.getText().toString();
				String _surname = surname.getText().toString();
				int _keys = OrgProfile._selectedKey;
				boolean rightToVote = hasRightToVote.isChecked();
				String _weight = rightToVote ? "1" : "0";
				String _email = email.getText().toString();
				String _slogan = slogan.getText().toString();
				boolean external = false;

				D_Constituent new_const = D_Constituent.createConstituent(
						_forename, _surname, _email, oLID, external, _weight,
						_slogan, OrgProfile.__keys[_keys].cipher,
						OrgProfile.__keys[_keys].hash_alg,
						OrgProfile.__keys[_keys].ciphersize, null, null);

				Log.d(TAG, "saved constituent=" + new_const.getNameFull());
				try {
					// Identity.DEBUG = true;
					Identity.setCurrentConstituentForOrg(new_const.getLID(),
							oLID);
					Log.d("CONST",
							"No Set: oLID=" + oLID + " c=" + new_const.getLID());
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
                constituent = new_const;
                constituent_LID = new_const.getLID();
				if (constituent_LID > 0) {
					constituent = D_Constituent.getConstByLID(constituent_LID,
							true, true);
					Log.d(TAG, "Got const: " + constituent);
				}

				if (constituent != null) {
					if (constituent.getSK() != null) {
						constituent.setPicture(byteIcon);
                        constituent.setCreationDate();
                        constituent.sign();
						constituent.storeRequest();
						constituent.releaseReference();
						Log.d(TAG,
								"saved constituent pic: "
										+ constituent.getPicture());
					}
				}

                if (! org.getBroadcasted()) {
                    D_Organization _org = D_Organization.getOrgByOrg_Keep(org);
                    if (_org != null) {
                        _org.setBroadcasting(true);
                        if (_org.dirty_any()) _org.storeRequest();
                        _org.releaseReference();
                        org = _org;
                    }
                }
				Log.d(TAG, "saved constituent=" + new_const);
				finish();
			}
		});
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.org_profile_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	
	
	@Override
	protected void onDestroy() {
		if (setProfilePhoto != null)
		    PhotoUtil.ClearImage(setProfilePhoto);
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.org_profile_export) {
			if (constituent == null) {
				Toast.makeText(getApplicationContext(), "No constituent yet!", Toast.LENGTH_SHORT).show();
				return  super.onOptionsItemSelected(item);
			}
			DD_SK d_SK = new DD_SK();
			d_SK.constit.add(constituent);
			constituent.loadNeighborhoods(D_Constituent.EXPAND_ALL);
			D_Neighborhood[] n = constituent.getNeighborhood();
			if (n != null) {
				for (D_Neighborhood _n : n) {
					// could also add a witness for the neighborhood (with its source... if it is me)
					d_SK.neigh.add(_n);
					//new D_Witness(a,b,c,e);
				}
			}
			//constituent.setNeighborhood(null);
			d_SK.org.add(org);
			DD_SK_Entry dsk = new DD_SK_Entry();
			dsk.key = constituent.getSK();
			dsk.name = constituent.getNameOrMy();
			dsk.creation = constituent.getCreationDate();
			dsk.type = "Profile!";
			d_SK.sk.add(dsk);
			
			String testText = net.ddp2p.common.config.DD.getExportTextObjectBody(d_SK);
			String testSubject = DD.getExportTextObjectTitle(this.org, constituent);
            //"DDP2P: Organization \""+ this.org.getName()+"\" Profile of \""+ constituent.getNameOrMy() + " " + Safe.SAFE_TEXT_MY_HEADER_SEP;
/*			if (organization_gidh == null) {
				Toast.makeText(this, "No peer. Reload!", Toast.LENGTH_SHORT).show();
				return true;
			}*/
			//DD_Address adr = new DD_Address();
			
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_TEXT, testText);
			i.putExtra(Intent.EXTRA_SUBJECT, testSubject);
			i = Intent.createChooser(i, "send org profile Public key");
			startActivity(i);
		}
		
		return super.onOptionsItemSelected(item);
	}
		
	private static D_FieldValue locateFV(D_FieldValue[] field_values,
			D_OrgParam field) {
		if (field_values == null)
			return null;
		if (field == null)
			return null;
		for (int k = 0; k < field_values.length; k++) {
			if (field_values[k].field_extra_ID == field.field_LID)
				return field_values[k];
			if (Util.equalStrings_null_or_not(field_values[k].field_extra_GID,
					field.global_field_extra_ID))
				return field_values[k];
		}
		return null;
	}

	static class KeysListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			// add action to this...
			OrgProfile._selectedKey = position;
			Log.d("org_profile", "keys");
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}

	}


	public static D_Constituent getCrtConstituent(String organization_LID) {
		long oLID = Util.lval(organization_LID, -1);
		return getCrtConstituent(oLID);
	}

	public static D_Constituent getCrtConstituent(long oLID) {
		if (oLID <= 0)
			return null;
		// D_Organization org;
		// org = D_Organization.getOrgByLID(oLID, true, false);
		// if (org == null) return null;

		long constituent_LID = -1;
		D_Constituent constituent = null;
		try {
			Identity crt_identity = Identity.getCurrentConstituentIdentity();
			if (crt_identity == null) {
				Log.d(TAG, "No identity");
			} else
				constituent_LID = net.ddp2p.common.config.Identity
						.getDefaultConstituentIDForOrg(oLID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}

		if (constituent_LID > 0) {
			constituent = D_Constituent.getConstByLID(constituent_LID, true,
					false);
			Log.d(TAG, "Got const: " + constituent);
		}

		return constituent;
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

			Bitmap bmp = BitmapFactory.decodeFile(selectedImagePath);
			// String strFromBmp = PhotoUtil.BitmapToString(bmp);
			byte[] icon = PhotoUtil.BitmapToByteArray(bmp, 100);
			Log.i(TAG, Util.stringSignatureFromByte(icon));

			byteIcon = icon;
			Log.d(TAG, "selected constituent pic: " + byteIcon.length);
			setProfilePhoto.setImageBitmap(bmp);

		}
	}
}
