/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package com.HumanDecisionSupportSystemsLaboratory.DDP2P;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import util.DBInterface;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import config.Application_GUI;
import data.D_Motion;
import data.D_Organization;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class Orgs extends ListFragment{	
	public final static String TAG = "orgs";
	public final static String O_ID = "org_ID";
	public final static String O_LID = "org_LID";
	public final static String O_GIDH = "org_GIDH";
	public final static String O_GID = "org_GID";
	public final static String O_NAME = "org_name";

	static OrgItem orgName[];
	static Orgs activ = null;
	public static ListAdapter listAdapter;
	public final static String sql_all_orgs = "SELECT "+table.organization.organization_ID+" FROM "+table.organization.TNAME+";";
	

	public static java.util.ArrayList<java.util.ArrayList<Object>> getAllOrganizations() {
		ArrayList<ArrayList<Object>> result;
		try {
			if (Application.db != null)
				result = Application.db.select(sql_all_orgs, new String[0]);
			else result = new ArrayList<ArrayList<Object>>();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return new ArrayList<ArrayList<Object>>();
		}
		return result;
	}
	public static void reloadOrgs() {
		OrgItem [] _orgName;
		ArrayList<ArrayList<Object>> orgName_alist = getAllOrganizations();
		_orgName = new OrgItem[orgName_alist.size()];
		for (int k = 0; k < orgName_alist.size(); k++) {
			_orgName[k] = new OrgItem();
			ArrayList<Object> org_id_item =  orgName_alist.get(k);
			if (org_id_item == null || org_id_item.size() <= 0) {
				_orgName[k].setOrgName(Util.__("Organization Position: ")+k);
				continue;
			}
			String oLID = Util.getString(org_id_item.get(0));
			D_Organization org = D_Organization.getOrgByLID(oLID, true, false);
			if (org != null) {
				Log.d(TAG, "current org: "+ org.toString());
				//String name = org.getOrgName();
				_orgName[k].org =  org; //name;
				//_orgName[k].setOrgName(_orgName[k].org.getName());
				//_orgName[k].setIcon(_orgName[k].org.getIcon());
				Log.d(TAG, "_orgname: "+_orgName[k].getOrgName());
				Log.d(TAG, "_orgicon: "+_orgName[k].getIcon());
				
				if (_orgName[k].getIcon() == null) {
					//TODO
					/*
					Bitmap bmp = BitmapFactory.decodeResource(activ.getResources(),
							R.drawable.organization_default_img);
					_orgName[k].icon = PhotoUtil.BitmapToByteArray(bmp, 100);
					*/
				}
				Log.d(TAG, "_orgname: "+_orgName[k].getOrgName());
				Log.d(TAG, "_orgicon: "+_orgName[k].getIcon());
			} else {
				_orgName[k].setOrgName(Util.__("Organization: ")+oLID);
			}
		}
		orgName = _orgName;		
		
		//orgName_alist.clear();
	}
	public static final Object monitor = new Object();
	public Orgs() {
		/*
		synchronized(monitor) {
			//pull out all safes from database		
			if (Application_GUI.dbmail == null)
				Application_GUI.dbmail = new Android_DB_Email(this.getActivity());
			if (Application_GUI.gui == null)
				Application_GUI.gui = new Android_GUI();
	
			if (Application.db == null) {
				try {			
		
					DBInterface db = new DBInterface("deliberation-app.db");
					Application.db = db;
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
				}
			}		
		}
		*/
		//D_Organization.
		reloadOrgs();
//		
//		orgName = new String[] {
//				"Arsenal",
//				"Manchester United",
//				"Liverpool",
//				"Chelsea",
//				"Manchester City",
//				"Lyon",
//				"Inter Milan"
//				
//		};
	}
	

	
	@SuppressWarnings("unchecked")
	@Override
	public void onResume() {
		super.onResume();
		((OrgAdapter)getListAdapter()).notifyDataSetChanged();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		activ = this;
		reloadOrgs();
		
		/* it could set a arraylist as the type of arrayadapter 
		   or if using simpleadapter,         this.simpleAdapter = new SimpleAdapter(getActivity(), this.list(your list), R.layout.simple_list_item1,
    		new String[]{"name"}, new int[] {R.id.motion_name_in_list});
        */
     	listAdapter = new OrgAdapter(getActivity(), orgName);
		setListAdapter(listAdapter);
		return inflater.inflate(R.layout.list_fragement, container, false);
		

	}
	
	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		
		Toast.makeText(getActivity(), getListView().getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
		
		OrgItem[] p = Orgs.orgName;
		D_Organization o = null;
		try {
			if (position >= p.length) return;
			o = p[position].org;
		} catch (Exception e) {return;}
				
		Intent intent = new Intent();
		intent.setClass(getActivity(), OrgDetail.class);
		Bundle b = new Bundle();
		b.putInt(O_ID, position);
		b.putString(O_GIDH, o.getGIDH());
		b.putString(O_LID, o.getLIDstr());
		b.putString(O_NAME, Util.getString(orgName[position]));
		intent.putExtras(b);
		startActivity(intent);
	}
	
	public static class OrgAdapter extends BaseAdapter {

		private Activity activity;
		private LayoutInflater inflater = null;
		private OrgItem[] data;

		
		public OrgAdapter(Activity _activity, 
            OrgItem[] _data) {
			activity = _activity;
			data = _data;
			inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
		}
		
		@Override
		public int getCount() {
			return data.length;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (convertView == null)
				v = inflater.inflate(R.layout.org_list, null);

			ImageView img = (ImageView) v.findViewById(R.id.org_list_pic);
			TextView name = (TextView) v.findViewById(R.id.org_list_name);
			if (data.length <= position) {
				Log.d("Orgs", "position too short: "+position+"/"+data.length);
				return v;
			}
			OrgItem oItem = data[position];
			if (oItem == null) {
				Log.d("Orgs", "position null: "+position+"/"+data.length);
				return v;
			}
			if (oItem.getIcon() == null) {
				Log.d("Orgs", "icon null: "+position+"/"+data.length);
				Bitmap bmp = BitmapFactory.decodeByteArray(Main.icon_org, 0, Main.icon_org.length); // -1
				
				img.setImageBitmap(bmp);
			} else {
				Bitmap bmp = BitmapFactory.decodeByteArray(oItem.getIcon(), 0, oItem.getIcon().length); //-1
				
				img.setImageBitmap(bmp);
			}
			Log.d(TAG, "name: "+oItem.getOrgName());
			name.setText(oItem.getOrgName());

			return v;
		}

	}
}
