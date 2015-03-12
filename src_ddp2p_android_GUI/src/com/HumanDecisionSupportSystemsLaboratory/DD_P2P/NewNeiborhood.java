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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class NewNeiborhood extends Activity{

	private EditText name, division, subdivision;
	private Button submit; 
	private String _name, _division, _subdivision;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.new_neiborhood);
		
		name = (EditText) findViewById(R.id.new_neiborhood_name);
		division = (EditText) findViewById(R.id.new_neiborhood_division);
		subdivision = (EditText) findViewById(R.id.new_neiborhood_subdivision);
		submit = (Button) findViewById(R.id.new_neiborhood_submit);
		
		
		submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO add more actions to finish submit
				_name = name.getText().toString();
				_division = division.getText().toString();
				_subdivision = subdivision.getText().toString();
			}
		});
	}

	
}
