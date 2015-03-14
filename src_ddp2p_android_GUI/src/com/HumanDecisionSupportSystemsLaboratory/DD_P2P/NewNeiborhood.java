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
