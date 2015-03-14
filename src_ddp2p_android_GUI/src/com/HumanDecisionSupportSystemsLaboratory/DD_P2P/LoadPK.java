package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.util.regex.Pattern;

import net.ddp2p.common.config.DD;

import net.ddp2p.common.util.DD_Address;
import net.ddp2p.common.util.DD_SK;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.StegoStructure;
import net.ddp2p.common.util.Util;
import net.ddp2p.ASN1.Decoder;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
					Toast.makeText(getActivity(), "Separators not found: \""+Safe.SAFE_TEXT_MY_HEADER_SEP+Safe.SAFE_TEXT_ANDROID_SUBJECT_SEP+Safe.SAFE_TEXT_MY_BODY_SEP+"\"", Toast.LENGTH_SHORT).show();
			        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
			        ft.detach(LoadPK.this);
			        ft.commit();
					return;
				}

                net.ddp2p.common.util.StegoStructure imported_object = interprete(body);
				
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
				//boolean DEBUG = true;
				String addressASN1B64;
				try {
					if (strAddress == null) {
						if (DEBUG) Log.d(TAG, "LoadPK: Address = null");
				        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
				        ft.detach(LoadPK.this);
				        ft.commit();
						return null;
					}
					//strAddress = strAddress.trim();
					if (DEBUG) Log.d(TAG, "LoadPK: Address="+strAddress);
					
					String[] __chunks = strAddress.split(Pattern.quote(Safe.SAFE_TEXT_MY_BODY_SEP));
					if (__chunks.length == 0 || __chunks[__chunks.length - 1] == null) {
						if (DEBUG) Log.d(TAG, "LoadPK: My top Body chunk = null");
				        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
				        ft.detach(LoadPK.this);
				        ft.commit();
						return null;
					}
					if (__chunks.length > 1) {
						addressASN1B64 = __chunks[__chunks.length - 1];
						addressASN1B64 = addressASN1B64.trim();
						if (DEBUG) Log.d(TAG, "LoadPK: got Body=" + addressASN1B64);
						addressASN1B64 = Util.B64Join(addressASN1B64);
						if (DEBUG) Log.d(TAG, "LoadPK: got Body=" + addressASN1B64);
						return addressASN1B64;
					}
					
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
	
					addressASN1B64 = _chunks[_chunks.length - 1];
					addressASN1B64 = addressASN1B64.trim();
					if (DEBUG) Log.d(TAG, "LoadPK: Body=" + addressASN1B64);
					addressASN1B64 = Util.B64Join(addressASN1B64);
					if (DEBUG) Log.d(TAG, "LoadPK: Body=" + addressASN1B64);
					return addressASN1B64;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
//			public  StegoStructure getStegoStructure(BigInteger ASN1TAG) {
//				Log.d("Import", "BN = "+ ASN1TAG);
//				for (StegoStructure ss : DD.getAvailableStegoStructureInstances()) {
//					Log.d("Import", "Available = "+ ss+" ID="+""+ss.getSignShort());
//					if (ASN1TAG.equals (new BigInteger(""+ss.getSignShort()))) {
//						try {
//							Log.d("Import", "Match");
//							return (StegoStructure) ss.getClass().newInstance();
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//				}
//				return null;
//			}

			private net.ddp2p.common.util.StegoStructure interprete(String addressASN1B64) {
				byte[] msg = null;
				StegoStructure ss = null;
				try {
					Log.d("Import", addressASN1B64);
					msg = Util.byteSignatureFromString(addressASN1B64);
					
					Decoder dec = new Decoder(msg);
					
//					StegoStructure s2s = getStegoStructure(dec.getTagValueBN());
					
					ss = DD.getStegoStructure(dec);
					if (ss == null) {
						Log.d("Import", "LoadPK. Use default stego");
						ss = new DD_Address();
					}
					//DD_Address da =
					ss.setBytes(msg);
					return ss;
				} catch (Exception e) {
					e.printStackTrace();
					DD_SK dsk = new DD_SK();
					Log.d("Import", "LoadPK. Try SK");
				
					try {
						dsk.setBytes(msg);
						Log.d("Import", "LoadPK. got="+dsk);
						return dsk;
					} catch (Exception e2) {
						e2.printStackTrace();
						Log.d("Import", "LoadPK. err sk="+e2.getLocalizedMessage());
					}
				}
				return null;
			}
	    });

		return view;
	}
}
