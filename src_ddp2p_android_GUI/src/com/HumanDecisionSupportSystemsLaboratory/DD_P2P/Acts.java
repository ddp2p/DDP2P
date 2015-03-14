package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Acts extends ListFragment{

	private String myandroidversions[];
	
	public Acts() {
		myandroidversions= new String[] {
/*				"Jelly Bean",
				"Ice cream Sandwich",
				"GingerBread",
				"HoneyComb",
				"Friyo",*/
				
		};
		
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListAdapter listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, myandroidversions);
		setListAdapter(listAdapter);


	}
	
	@Override
	public void setEmptyText(CharSequence text) {
		TextView emptyView = (TextView) getListView().getEmptyView();
		emptyView.setText(text);
		super.setEmptyText(text);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		return inflater.inflate(R.layout.list_fragement, container, false);
	}
	
	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		
		Toast.makeText(getActivity(), getListView().getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
	}
}
