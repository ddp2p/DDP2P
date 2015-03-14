package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SettingMain extends Fragment{
	
	private TextView adhoc;
	private TextView maxImg;
	private TextView update;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.setting_main, null);
		
		getActivity().getActionBar().setTitle("Setting");
				
		adhoc = (TextView) v.findViewById(R.id.setting_main_adhoc);
		
		adhoc.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SettingAdhoc settingAdhoc = new SettingAdhoc();
				
				FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.setting_container, settingAdhoc);
				ft.addToBackStack("adhoc");
				ft.commit();
			}
		});
		
		update = (TextView) v.findViewById(R.id.setting_main_update_server);
		
		update.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SettingUpdate settingUpdate = new SettingUpdate();
				
				FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.setting_container, settingUpdate);
				ft.addToBackStack("update");
				ft.commit();
			}
		});
		
		return v;
	}
}
