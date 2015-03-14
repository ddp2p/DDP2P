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
