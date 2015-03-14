package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingUpdate extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
		getActivity().getActionBar().setTitle("Update Setting");
		View v = inflater.inflate(R.layout.setting_update, null);

		ViewPager pager = (ViewPager) v.findViewById(R.id.setting_update_viewpager);
		pager.setAdapter(new SettingUpdateAdapter(getActivity().getSupportFragmentManager()));
		return v;
	}

	private class SettingUpdateAdapter extends FragmentPagerAdapter {

		public SettingUpdateAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);

		}

		@Override
		public Fragment getItem(int pos) {
			
			 switch (pos) { 
			 case 0: 
				 return KnownTester.newInstance(); 
			 case 1: 
				 return UsedTester.newInstance();
		     }
			 
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

	}
}
