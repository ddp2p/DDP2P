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

import data.D_Peer;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UpdateSafeName extends DialogFragment {
	
	private D_Peer peer;
	private String sName;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  	
    	
    	View view = inflater.inflate(R.layout.dialog__profile_update, container);
    	final Button but = (Button) view.findViewById(R.id.update_safe_ok);    	
        final EditText name = (EditText) view.findViewById(R.id.text_update_safe);
                       
        getDialog().setTitle("Name");
        
        Bundle bund = getArguments();
        String lid = bund.getString(Safe.P_SAFE_LID);       
        //String sid = String.valueOf(id);
        
        peer = D_Peer.getPeerByLID(lid, true, false);
        
    	but.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {           
            	sName = name.getText().toString();
            	updateName(peer, sName);
            	Toast.makeText(getActivity(), "update successfully!", Toast.LENGTH_LONG).show();
            }
        });
        
        return view;
    }
    
	void updateName(D_Peer peer, String newName) {
		D_Peer p = D_Peer.getPeerByPeer_Keep(peer);
		p.setName(newName);
		p.storeRequest();
		p.releaseReference();
	}

}
