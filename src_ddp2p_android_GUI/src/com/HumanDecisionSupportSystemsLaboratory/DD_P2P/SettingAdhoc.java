package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingAdhoc extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
		getActivity().getActionBar().setTitle("Adhoc Setting");
		View v = inflater.inflate(R.layout.setting_adhoc, null);

		return v;
	}

	public interface SettingAdhocListener {

		public void onContactSelected(long rowID);

	}
}
