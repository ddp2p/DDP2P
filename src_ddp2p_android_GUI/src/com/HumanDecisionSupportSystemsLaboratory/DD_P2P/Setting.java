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


import android.content.Intent;
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
		Intent intent = new Intent();
		//intent.putExtra(Main.RESULT_SETTINGS, pi.encode());
		this.setResult(RESULT_OK, intent);
	}

	@Override
	protected void onDestroy() {
		Intent intent = new Intent();
		//intent.putExtra(Main.RESULT_SETTINGS, pi.encode());
		this.setResult(RESULT_OK, intent);
		super.onDestroy();
	}
}
