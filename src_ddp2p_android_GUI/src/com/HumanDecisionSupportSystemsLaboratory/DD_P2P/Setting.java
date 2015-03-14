package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;


import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class Setting extends FragmentActivity {

	private SettingMain settingMain;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting);
		
		if (findViewById(R.id.setting_container) != null) {
			settingMain = new SettingMain();
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.setting_container, settingMain);
			ft.commit();
		}
	}
	
	

}
