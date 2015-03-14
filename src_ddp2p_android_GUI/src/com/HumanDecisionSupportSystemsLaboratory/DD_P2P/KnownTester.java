package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class KnownTester extends Fragment{

	private static final String TAG = "KnownTester";
	private TextView title;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		ArrayList<ServerItem> servers = new ArrayList<ServerItem>();
		ServerItem server1 = new ServerItem();
		server1.setID(1);
		server1.setName("Arsenal");
		server1.setScore(100);
		
		ServerItem server2 = new ServerItem();
		server2.setID(2);
		server2.setName("Lyon");
		server2.setScore(100);
		
		servers.add(server1);
		servers.add(server2);
		
		View v = inflater.inflate(R.layout.update_fragment_view, null);
		
		title = (TextView) v.findViewById(R.id.update_fragment_title);
		title.setText("Known Tester");
		
		ListView listview = (ListView) v.findViewById(R.id.update_fragment_listview);
		KnownAdapter adapter = new KnownAdapter(getActivity(), R.layout.update_server_list, servers);
		listview.setAdapter(adapter);
		
		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public static KnownTester newInstance() {
		KnownTester kt = new KnownTester();
		return kt;
	}
	
	private class KnownAdapter extends ArrayAdapter<ServerItem> {


		private ArrayList<ServerItem> servers;

		public KnownAdapter(Context context, int textViewResourceId,
				List<ServerItem> objects) {
			super(context,  textViewResourceId, objects);
			this.servers = (ArrayList<ServerItem>) objects;
		}
		@Override
		public int getCount() {
			return servers.size();
		}

		@Override
		public ServerItem getItem(int position) {
			return servers.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.update_server_list, null);
			}


			Log.d(TAG, "server: item" +  servers);
			ServerItem item = servers.get(position);
			
			Log.d(TAG, "servers: name" + position + " : " + item.getName());
			
			if (item != null) {

				TextView name = (TextView) v
						.findViewById(R.id.update_server_list_name);
				TextView score = (TextView) v
						.findViewById(R.id.update_server_list_score);

				name.setText(item.getName());
				score.setText(String.valueOf(item.getScore()));
			}

			return v;
		}

	}
	
}
