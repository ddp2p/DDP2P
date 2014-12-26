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

import java.util.regex.Pattern;

import config.DD;

import util.DD_Address;
import util.P2PDDSQLException;
import util.StegoStructure;
import util.Util;
import ASN1.Decoder;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoadPK extends DialogFragment {

	protected static final boolean _DEBUG = true;
	protected static final boolean DEBUG = false;
	protected static final String TAG = null;
	private Button load;
	private EditText address;

	private String strAddress;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.dialog_load_pk,
				container);

		
		load = (Button) view.findViewById(R.id.dialog_load_pk_load);
		load.setText(Util.__("Import"));
		
		address = (EditText) view.findViewById(R.id.dialog_load_pk_editText);
		
		getDialog().setTitle("Import from Text");


		load.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				strAddress = address.getText().toString();
				
				//Interpret
				String body = extractMessage(strAddress);
				
				if (body == null) {
					if (_DEBUG) Log.d(TAG, "LoadPK: Extraction of body failed");
					Toast.makeText(getActivity(), "Separators not found: \""+Safe.SAFE_TEXT_MY_HEADER_SEP+Safe.SAFE_TEXT_ANDROID_SUBJECT_SEP+"\"", Toast.LENGTH_SHORT).show();
			        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
			        ft.detach(LoadPK.this);
			        ft.commit();
					return;
				}

				util.StegoStructure imported_object = interprete(body);
				
				if (imported_object == null) {
					if (_DEBUG) Log.d(TAG, "LoadPK: Decoding failed");
					Toast.makeText(getActivity(), "Failed to decode", Toast.LENGTH_SHORT).show();
			        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
			        ft.detach(LoadPK.this);
			        ft.commit();
					return;
				}
				
				String interpretation = imported_object.getNiceDescription();
				//ask confirm
                address.setText(interpretation);
                

                AlertDialog.Builder confirm = new AlertDialog.Builder(getActivity());
                confirm.setTitle("Do you wish to load?");
                confirm.setMessage(interpretation)
                    .setCancelable(false)
				    .setPositiveButton("Yes", new MyDialog_OnClickListener(imported_object) {
					    public void _onClick(DialogInterface dialog, int id) {
					    	Log.d("PK", "LoadPK: Trying to save");
					    	StegoStructure imported_object = (StegoStructure) ctx;
					    	try {
								imported_object.save();
								Toast.makeText(getActivity(), "Saving successful!", Toast.LENGTH_SHORT).show();
							} catch (P2PDDSQLException e) {
								e.printStackTrace();
						    	Log.d("PK", "LoadPK: Failed to save: "+e.getLocalizedMessage());
							}

					        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
					        ft.detach(LoadPK.this);
					        ft.commit();
					    	dialog.cancel();
					    }
				    })
				    .setNegativeButton("No",new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
					        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
					        ft.detach(LoadPK.this);
					        ft.commit();
					    	dialog.cancel();
					    }
				    });
                
                AlertDialog confirmDialog = confirm.create();
                confirmDialog.show();
			}

			private String extractMessage(String strAddress) {
				try {
					if (strAddress == null) {
						if (DEBUG) Log.d(TAG, "LoadPK: Address = null");
				        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
				        ft.detach(LoadPK.this);
				        ft.commit();
						return null;
					}
					strAddress = strAddress.trim();
					if (DEBUG) Log.d(TAG, "LoadPK: Address="+strAddress);
					
					String[] chunks = strAddress.split(Pattern.quote(Safe.SAFE_TEXT_MY_HEADER_SEP));
					if (chunks.length == 0 || chunks[chunks.length - 1] == null) {
						if (DEBUG) Log.d(TAG, "LoadPK: My Body chunk = null");
				        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
				        ft.detach(LoadPK.this);
				        ft.commit();
						return null;
					}
					
					String body = chunks[chunks.length - 1];
					if (DEBUG) Log.d(TAG, "LoadPK: Body="+body);
					
					String[] _chunks = strAddress.split(Pattern.quote(Safe.SAFE_TEXT_ANDROID_SUBJECT_SEP));
					if (_chunks.length == 0 || _chunks[_chunks.length - 1] == null) {
						if (DEBUG) Log.d(TAG, "LoadPK: Android Body chunk = null");
				        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
				        ft.detach(LoadPK.this);
				        ft.commit();
						return null;
					}
	
					String addressASN1B64 = _chunks[_chunks.length - 1];
					if (DEBUG) Log.d(TAG, "LoadPK: Body=" + addressASN1B64);
					return addressASN1B64;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			private util.StegoStructure interprete(String addressASN1B64) {
				try {
					byte[] msg = Util.byteSignatureFromString(addressASN1B64);
					
					Decoder dec = new Decoder(msg);
					StegoStructure ss = DD.getStegoStructure(dec);
					if (ss == null) ss = new DD_Address();
					//DD_Address da =
					ss.setBytes(msg);
					return ss;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
	    });

		return view;
	}
}
