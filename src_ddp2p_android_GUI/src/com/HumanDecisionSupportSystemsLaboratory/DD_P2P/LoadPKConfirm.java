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
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class LoadPKConfirm extends DialogFragment {

	private Button yes;
	private Button no;
	private String display;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.dialog_load_pk_confirm,
				container);

		
		yes = (Button) view.findViewById(R.id.dialog_load_pk_yes);
		no = (Button) view.findViewById(R.id.dialog_load_pk_no);
		
		getDialog().setTitle("Load peer's address confirmation");


		yes.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

				
			}
		});

		no.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

				
			}
		});


		return view;
	}
	
	public static LoadPKConfirm newInstance(String info) {

        
		return null;
		
	}
}
