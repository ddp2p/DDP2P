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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import net.ddp2p.common.config.DD;

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
		CheckBox showHidden = (CheckBox) v.findViewById(R.id.show_hidden);
		CheckBox toastThreadPings = (CheckBox) v.findViewById(R.id.toast_thread_pings);

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
		showHidden.setChecked(Safe.SHOW_HIDDEN);
		showHidden.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				DD.setAppBoolean(Safe.DD_SHOW_HIDDEN, Safe.SHOW_HIDDEN = ((CheckBox) view).isChecked());
				//new Main.ReloadSafe().execute(void);
				Log.d("SettingMain", "SettingMain: oCW: hidden =" + Safe.SHOW_HIDDEN);
			}
		});

		toastThreadPings.setChecked(Android_GUI.TOAST_THREAD_PINGS);
		toastThreadPings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				DD.setAppBoolean(Android_GUI.DD_TOAST_THREAD_PINGS, Android_GUI.TOAST_THREAD_PINGS = ((CheckBox) view).isChecked());
				//new Main.ReloadSafe().execute(void);
				Log.d("SettingMain", "SettingMain: oCW: toastPings ="+Android_GUI.TOAST_THREAD_PINGS);
			}
		});
		return v;
	}

	@Override
	public void onDestroyView() {
		Intent intent = new Intent();
		//intent.putExtra(Main.RESULT_SETTINGS, pi.encode());
		this.getActivity().setResult(Activity.RESULT_OK, intent);
		super.onDestroyView();
	}
}
