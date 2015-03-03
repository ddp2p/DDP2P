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

import util.DD_Address;
import util.DD_SK;
import util.P2PDDSQLException;
import ciphersuits.KeyManagement;
import config.DD;

import data.D_Peer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class SendPK extends DialogFragment {

	private Button exportByPhoto;
	private Button sendVia;
	private int SELECT_PHOTO = 42;
	private int SELECT_PHOTO_KITKAT = 43;
	private int PK_SELECT_PHOTO = 44;
	private int PK_SELECT_PHOTO_KITKAT = 45;
	
	private String selectedImagePath;
	private File selectImageFile;
    private D_Peer peer;
	private String safe_lid;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.dialog_send_pk,
				container);

		Intent i = getActivity().getIntent();
		Bundle b = i.getExtras();
		safe_lid = b.getString(Safe.P_SAFE_LID);
		
	    peer = D_Peer.getPeerByLID(safe_lid, true, false);
		if (peer == null) {
			Toast.makeText(getActivity(), "No peer. Reload!", Toast.LENGTH_SHORT).show();
			//finish();
		}
		
		exportByPhoto = (Button) view.findViewById(R.id.dialog_send_pk_export_by_picture);
		sendVia = (Button) view.findViewById(R.id.dialog_send_pk_send_via);

		
		
		getDialog().setTitle("Export public key");


		exportByPhoto.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

		      if (Build.VERSION.SDK_INT <19){
		        Intent intent = new Intent(); 
		        intent.setType("image/*");
		        intent.setAction(Intent.ACTION_GET_CONTENT);
		        startActivityForResult(intent, SELECT_PHOTO);
		        
		        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
		        ft.detach(SendPK.this);
		        ft.commit();
		      } else {
		        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		        intent.setType("image/*");
		        startActivityForResult(intent, SELECT_PHOTO_KITKAT);
		        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
		        ft.detach(SendPK.this);
		        ft.commit();
		      }
			}  
	    });


		sendVia.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (peer == null) {
					Toast.makeText(getActivity(), "No peer. Reload!", Toast.LENGTH_SHORT).show();
					return;
					//finish();
				}
				DD_Address adr = new DD_Address(peer);
				
				String msgBody = Safe.getExportTextObject(adr.getBytes());
				
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_TEXT, msgBody); //Safe.SAFE_TEXT_MY_BODY_SEP + Util.stringSignatureFromByte(adr.getBytes()));
				
				String slogan = peer.getSlogan_MyOrDefault();
				if (slogan == null) slogan = "";
				else slogan = "\""+slogan+"\"";
				i.putExtra(Intent.EXTRA_SUBJECT, "DDP2P: Safe Address of \""+peer.getName()+"\",  "+slogan+Safe.SAFE_TEXT_MY_HEADER_SEP);
				i = Intent.createChooser(i, "send Public key");
				startActivity(i);
		        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
		        ft.detach(SendPK.this);
		        ft.commit();
			}
		});

		return view;
	}

	@SuppressLint("NewApi")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		if (resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = null;
            
            if (requestCode == SELECT_PHOTO) {
                uri = resultData.getData();
                Log.i("Uri", "Uri: " + uri.toString());
            } else if (requestCode == SELECT_PHOTO_KITKAT) {
                uri = resultData.getData();
                Log.i("Uri_kitkat", "Uri: " + uri.toString());
                final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data.
                getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            
 
            selectedImagePath = FileUtils.getPath(getActivity(),uri);
            Log.i("path", "path: " + selectedImagePath); 
                
            selectImageFile = new File(selectedImagePath);
            //File testFile = new File("file://storage/emulated/0/DCIM/hobbit.bmp");
            
            boolean success;    
            
                   
            String[] selected = new String[1];
            DD_Address adr = new DD_Address(peer);
            try {
            	//util.EmbedInMedia.DEBUG = true;
            	Log.i("success_embed", "success_embed 1: "+selectImageFile); 
            	success = DD.embedPeerInBMP(selectImageFile, selected, adr);
            	Log.i("success_embed", "success_embed 2: " + success); 
			    if (success == true) {
			        Toast.makeText(getActivity(), "Export success!", Toast.LENGTH_SHORT).show();
			    } else 
			    	 Toast.makeText(getActivity(), "Unable to export:"+selected[0], Toast.LENGTH_SHORT).show();  
            }
		    catch (Exception e) {
		    	Toast.makeText(getActivity(), "Unable to export!", Toast.LENGTH_SHORT).show();
			    e.printStackTrace();
		    } 
                	
		}
		

/*		if (resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = null;
            
            if (requestCode == PK_SELECT_PHOTO) {
                uri = resultData.getData();
                Log.i("Uri", "Uri: " + uri.toString());
            } else if (requestCode == PK_SELECT_PHOTO_KITKAT) {
                uri = resultData.getData();
                Log.i("Uri_kitkat", "Uri: " + uri.toString());
                final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data.
                getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            
 
            selectedImagePath = FileUtils.getPath(getActivity(),uri);
            Log.i("path", "path: " + selectedImagePath); 
                
            selectImageFile = new File(selectedImagePath);
            //File testFile = new File("file://storage/emulated/0/DCIM/hobbit.bmp");
            
            boolean success;    

            try {
            	//util.EmbedInMedia.DEBUG = true;
            	success = saveSK(peer, selectImageFile);
            	Log.i("success_embed", "success_embed: " + success); 
			    if (success == true) {
			        Toast.makeText(getActivity(), "Export success!", Toast.LENGTH_SHORT).show();
			    } else 
			    	 Toast.makeText(getActivity(), "Unable to export!", Toast.LENGTH_SHORT).show();  
            }
		    catch (Exception e) {
		    	Toast.makeText(getActivity(), "Unable to export!", Toast.LENGTH_SHORT).show();
			    e.printStackTrace();
		    } 
                	
		}*/
		super.onActivityResult(requestCode, resultCode, resultData);
	}
	
	public boolean saveSK(D_Peer pk, File f) {
		DD_SK dsk = new DD_SK();
		try {
			if (!KeyManagement.fill_sk(dsk, pk.getGID())) return false;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
        String[] selected = new String[1];
    	boolean success = DD.embedPeerInBMP(f, selected, dsk);
    	return success;
	}
}
