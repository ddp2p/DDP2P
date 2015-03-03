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

import java.util.ArrayList;

import config.Identity;

import util.P2PDDSQLException;
import util.Util;

import data.D_Constituent;
import data.D_Neighborhood;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

@Deprecated
public class ConstituentSecLayer extends Activity {
	private ArrayList<String> parentItems = new ArrayList<String>();
	private ArrayList<Object> childItems = new ArrayList<Object>();
	private static int organization_position;
	private static String organization_LID;
	private static String organization_GIDH;
	private static String organization_GID;
	private static String organization_name;
	public static final String C_TYPE = "C_TYPE";
	public static final String C_TYPE_C = "C_TYPE_C";
	public static final String C_TYPE_N = "C_TYPE_N";
	
	public static final String C_GIDH = "C_GIDH";
	public static final String C_GID = "C_GID";
	public static final String C_LID = "C_LID";
	public static final String C_NAME = "C_NAME";
	
	public static final String N_GIDH = "N_GIDH";
	public static final String N_GID = "N_GID";
	public static final String N_LID = "N_LID";
	public static final String N_NAME = "N_NAME";
	protected static final String N_PARENT_LID = null;
	protected static final String TAG = "Constituent";
	long oLID;
	private ExpandableListView elist;
	private Button new_constituent, new_neighborhood;
	ArrayList<ArrayList<Object>> consts = new ArrayList<ArrayList<Object>>();
	ArrayList<ArrayList<Object>> neighs = new ArrayList<ArrayList<Object>>();

	ArrayList<Integer> neighs_neighs = new ArrayList<Integer> ();
	ArrayList<ArrayList<Object>> neighs_consts = new ArrayList<ArrayList<Object>> ();
	ArrayList<ArrayList<Object>> neighs_neighs_ = new ArrayList<ArrayList<Object>> ();

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		
		setContentView(R.layout.constituent);
		elist = (ExpandableListView) findViewById(R.id.constituentElist);
		
		new_constituent = (Button) findViewById(R.id.new_constituent);
		new_neighborhood = (Button) findViewById(R.id.new_neighborhood);
		
		ActionBar actionbar = getActionBar();
		actionbar.setTitle("Constituent 2nd Layer");
		
	/*	Intent i = this.getIntent();
		Bundle b = i.getExtras();
		organization_position = b.getInt(Orgs.O_ID);
		organization_LID = b.getString(Orgs.O_LID);
		organization_GIDH = b.getString(Orgs.O_GIDH);*/

/*		oLID = Util.lval(organization_LID, -1);
    	if (oLID <= 0) return;
    	D_Organization organization = D_Organization.getOrgByLID(oLID, true, false);
    	organization_GID = organization.getGID();
    	
    	consts = D_Constituent.getAllConstituents(oLID, -1, false);
    	neighs = D_Neighborhood.getAllNeighborhoods(oLID, -1, false);*/


		// Create Expandable List and set it's properties
		ExpandableListView expandableList = elist;
		expandableList.setDividerHeight(2);
		expandableList.setGroupIndicator(null);
		expandableList.setClickable(true);

		// Set the Items of Parent
		setGroupParents();
		// Set The Child Data
		setChildData();

		// Create the Adapter
		MyExpandableAdapter adapter = new MyExpandableAdapter(parentItems,
				childItems);

		adapter.setInflater(
				(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE),
				this);

		// Set the Adapter to expandableList
		expandableList.setAdapter(adapter);
		// expandableList.setOnChildClickListener();

	/*	new_constituent.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(ConstituentSecLayer.this, NewConstituent.class);
				Bundle b = new Bundle();
				b.putInt(Orgs.O_ID, organization_position);
				b.putString(Orgs.O_GIDH, organization_GIDH);
				b.putString(Orgs.O_GID, organization_GID);
				b.putString(Orgs.O_LID, organization_LID);
				b.putString(Orgs.O_NAME, organization_name);
				b.putString(ConstituentSecLayer.N_PARENT_LID, "-1");
				intent.putExtras(b);
				startActivity(intent);
			}
		});
		
		new_neighborhood.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();			
				intent.setClass(ConstituentSecLayer.this, NewNeighborhood.class);
				Bundle b = new Bundle();
				b.putInt(Orgs.O_ID, organization_position);
				b.putString(Orgs.O_GIDH, organization_GIDH);
				b.putString(Orgs.O_GID, organization_GID);
				b.putString(Orgs.O_LID, organization_LID);
				b.putString(Orgs.O_NAME, organization_name);
				b.putString(ConstituentSecLayer.N_PARENT_LID, "-1");
				intent.putExtras(b);
				startActivity(intent);				
			}
		});*/
	}
	
	// method to add parent Items
    public void setGroupParents() 
    {
    	parentItems.clear();
		//parentItems.add("");
    	for (ArrayList<Object> _n : neighs) {
    		long n_LID = Util.lval(_n.get(0));
    		if (n_LID <= 0) continue;
    		D_Neighborhood n = D_Neighborhood.getNeighByLID(n_LID, true, false);
    		if (n == null) continue;
    		parentItems.add(n.getName_division()+": "+n.getName());
    	}
    	for (ArrayList<Object> _c : consts) {
    		long c_LID = Util.lval(_c.get(0));
    		if (c_LID <= 0) continue;
    		D_Constituent c = D_Constituent.getConstByLID(c_LID, true, false);
    		if (c == null) continue;
    		parentItems.add(c.getNameOrMy());
    	}
    	if (parentItems.size() == 0) {
    		parentItems.add(util.Util.__("No constituent yet. Fill your profile!"));
    	}
//        parentItems.add("Fruits");
//        parentItems.add("Flowers");
//        parentItems.add("Animals");
//        parentItems.add("Birds");
    }


	// method to set child data of each parent
	public void setChildData() {
//		ArrayList<String> _child = new ArrayList<String>();
//		childItems.add(_child);
	   	for (ArrayList<Object> _n : neighs) {
	   		ArrayList<String> child = new ArrayList<String>();
	   		/**
	   		 * Each neighborhood has a first empty child with buttons
	   		 */
			child.add("");
	   		
    		long n_LID = Util.lval(_n.get(0));
    		if (n_LID <= 0) {
    			child.add("N:"+n_LID);
    	   		childItems.add(child);
    			continue;
    		}
    		D_Neighborhood n = D_Neighborhood.getNeighByLID(n_LID, true, false);
    		if (n == null) {
    			child.add("N:"+n_LID);
    	   		childItems.add(child);
    			continue;
    		}
    		
        	ArrayList<ArrayList<Object>> _consts = D_Constituent.getAllConstituents(oLID, n.getLID(), false);
        	ArrayList<ArrayList<Object>> _neighs = D_Neighborhood.getAllNeighborhoods(oLID, n.getLID(), false);
        	
         	ArrayList<Object> _neighborhoods = new ArrayList<Object>();
         	for (ArrayList<Object> __n : _neighs) {
           		if (__n.size() <= 0) continue;
         		Object n0 = __n.get(0);
         		long n__LID = Util.lval(n0);
        		if (n__LID <= 0) continue;
        		D_Neighborhood n_ = D_Neighborhood.getNeighByLID(n__LID, true, false);
        		if (n_ == null) continue;
        		child.add(n_.getName_division() + ": " + n_.getName());
        		_neighborhoods.add(n0);
        	}
         	neighs_neighs_.add(_neighborhoods);
//        	if (child.size() == 0) {
//        		child.add("Add elements: "+n.getNames_subdivisions_str());
//        	}

         	_neighs.add(0, new ArrayList<Object>());
        	this.neighs_neighs_.add(0, new ArrayList<Object>());
        	this.neighs_neighs.add(Integer.valueOf(_neighs.size()));

         	ArrayList<Object> _constits = new ArrayList<Object>();
        	for (ArrayList<Object> _c : _consts) {
        		if (_c.size() <= 0) continue;
        		Object c0 = _c.get(0);
        		long c_LID = Util.lval(c0);
        		if (c_LID <= 0) continue;
        		D_Constituent c = D_Constituent.getConstByLID(c_LID, true, false);
        		if (c == null) continue;
        		child.add(c.getNameOrMy());
        		_constits.add(c0);
        	}
    		neighs_consts.add(_constits);
    		childItems.add(child);
    	}
	   	for (ArrayList<Object> _c : consts) {
	   		ArrayList<String> child = new ArrayList<String>();
    		long c_LID = Util.lval(_c.get(0));
    		if (c_LID <= 0) {
    	   		childItems.add(child);
    			continue;
    		}
    		D_Constituent c = D_Constituent.getConstByLID(c_LID, true, false);
    		if (c == null) {
    	   		childItems.add(child);
    			continue;
    		}
    		String email = c.getEmail();
    		if (email == null)  email = "No email!";
    		child.add(Util.__("Email:")+" "+email);
    		childItems.add(child);
	   	}
//
//		// Add Child Items for Fruits
//		ArrayList<String> child = new ArrayList<String>();
//		child.add("Apple");
//		child.add("Mango");
//		child.add("Banana");
//		child.add("Orange");
//		childItems.add(child);
//
//		// Add Child Items for Flowers
//		child = new ArrayList<String>();
//		child.add("Rose");
//		child.add("Lotus");
//		child.add("Jasmine");
//		child.add("Lily");
//		childItems.add(child);
//
//		// Add Child Items for Animals
//		child = new ArrayList<String>();
//		child.add("Lion");
//		child.add("Tiger");
//		child.add("Horse");
//		child.add("Elephant");
//		childItems.add(child);
//
//		// Add Child Items for Birds
//		child = new ArrayList<String>();
//		child.add("Parrot");
//		child.add("Sparrow");
//		child.add("Peacock");
//		child.add("Pigeon");
//		childItems.add(child);
	}


    private class MyExpandableAdapter extends BaseExpandableListAdapter {

		private Activity activity;
		private ArrayList<Object> childtems;
		private LayoutInflater inflater;
		private ArrayList<String> parentItems, child;

		// constructor
		public MyExpandableAdapter(ArrayList<String> parents,
				ArrayList<Object> childern) {
			this.parentItems = parents;
			this.childtems = childern;
		}

		public void setInflater(LayoutInflater inflater, Activity activity) {
			this.inflater = inflater;
			this.activity = activity;
		}

		// method getChildView is called automatically for each child view.
		// Implement this method as per your requirement
		@Override
		public View getChildView(int groupPosition, final int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {

			child = (ArrayList<String>) childtems.get(groupPosition);

			TextView textView = null;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.constituents_child_view, null);
			}
			
			boolean is_parent_constituent = true;
			if (groupPosition < neighs.size()) {
				is_parent_constituent = false;
			}
			
			boolean is_child_constituent = true;
			if (! is_parent_constituent) {
				if (groupPosition < neighs_neighs.size()  && childPosition < neighs_neighs.get(groupPosition)) {
					is_child_constituent = false;
				}
			}
			
			boolean isConstituent = ! (! is_parent_constituent && ! is_child_constituent);

			// get the textView reference and set the value
			textView = (TextView) convertView.findViewById(R.id.textViewChild);
			textView.setText(child.get(childPosition));
			Button addNeiborhood = (Button) convertView.findViewById(R.id.constituent_child_view_add_neiborhood);
			addNeiborhood.setFocusable(false);
			if (isConstituent || childPosition != 0) addNeiborhood.setVisibility(Button.GONE);
			else addNeiborhood.setVisibility(Button.VISIBLE);
			
			addNeiborhood.setOnClickListener(new My_OnClickListener(new CtxConstituent(null, null, groupPosition, childPosition)) {
				
				@Override
				public void _onClick(View arg0) {
					CtxConstituent _ctx = (CtxConstituent) ctx;
					int n_idx = _ctx.groupPosition;
					if (n_idx >= neighs.size()) return;
					String parentLID = Util.getString(neighs.get(n_idx).get(0));
					//long parentLID = Util.lval(neighs.get(n_idx).get(0));
					Intent intent = new Intent();
					intent.setClass(ConstituentSecLayer.this, NewNeighborhood.class);
					Bundle b = new Bundle();
					b.putInt(Orgs.O_ID, organization_position);
					b.putString(Orgs.O_GIDH, organization_GIDH);
					b.putString(Orgs.O_GID, organization_GID);
					b.putString(Orgs.O_LID, organization_LID);
					b.putString(Orgs.O_NAME, organization_name);
					b.putString(ConstituentSecLayer.N_PARENT_LID, parentLID);
					intent.putExtras(b);
					startActivity(intent);				
				}
			});
			
			
			Button addConstituent = (Button) convertView.findViewById(R.id.constituent_child_view_add_constituent);
			addConstituent.setFocusable(false);
			if (isConstituent || childPosition != 0)	addConstituent.setVisibility(Button.GONE);
			else addConstituent.setVisibility(Button.VISIBLE);
			
			addConstituent.setOnClickListener(new My_OnClickListener(new CtxConstituent(null, null, groupPosition, childPosition)) {
				
				@Override
				public void _onClick(View arg0) {
					CtxConstituent _ctx = (CtxConstituent) ctx;
					int n_idx = _ctx.groupPosition;
					if (n_idx >= neighs.size()) return;
					String parentLID = Util.getString(neighs.get(n_idx).get(0));
					//long parentLID = Util.lval(neighs.get(n_idx).get(0));
					Intent intent = new Intent();
					intent.setClass(ConstituentSecLayer.this, NewConstituent.class);
					Bundle b = new Bundle();
					b.putInt(Orgs.O_ID, organization_position);
					b.putString(Orgs.O_GIDH, organization_GIDH);
					b.putString(Orgs.O_GID, organization_GID);
					b.putString(Orgs.O_LID, organization_LID);
					b.putString(Orgs.O_NAME, organization_name);
					b.putString(ConstituentSecLayer.N_PARENT_LID, parentLID);
					intent.putExtras(b);
					startActivity(intent);				
				}
			});

			/**
			 * Setting myself supported for constituents at both levels.
			 * Currently the button is shown only even when i do not know the secret key of the constituent.
			 * That should be eventually fixed (but needs to be made efficient, since testing/loading the key is slow).
			 * 			 */
			Button myself = (Button) convertView.findViewById(R.id.constituent_child_view_set_myself);
			myself.setFocusable(false);
			if (! isConstituent)	myself.setVisibility(Button.GONE);
			else myself.setVisibility(Button.VISIBLE);
			
			myself.setOnClickListener(new My_OnClickListener(new CtxConstituent(null, null, groupPosition, childPosition)) {
				
				@Override
				public void _onClick(View arg0) {
					D_Constituent c;
		    		Log.d(TAG, "setting myself ");
					CtxConstituent _ctx = (CtxConstituent) ctx;
					int n_idx = _ctx.groupPosition;
					if (n_idx < neighs.size()) {
						/** 
						 * Cannot be a neighborhood
						 */
						if (_ctx.childPosition < neighs_neighs.get(n_idx)) return;
						int c_idx = _ctx.childPosition - neighs_neighs.get(n_idx);
						c = D_Constituent.getConstByLID(Util.getString(neighs_consts.get(n_idx).get(c_idx)), true, false);
					} else {
						int c_idx = n_idx - neighs.size();
						c = D_Constituent.getConstByLID(Util.getString(consts.get(c_idx).get(0)), true, false);
					}
//					String parentLID = Util.getString(neighs.get(n_idx).get(0));
//					//long parentLID = Util.lval(neighs.get(n_idx).get(0));
//		    		Log.d(TAG, "setting myself to: "+parentLID);
//					
//					D_Neighborhood neighborhood = D_Neighborhood.getNeighByLID(parentLID, true, false);
		    		Log.d(TAG, "setting myself to: "+c);
		    		if (c == null) return;
					
					String myself_constituent_GID;
					String myself_constituent_GIDH;
			    	D_Constituent myself_constituent = null;
			    	long myself_constituent_LID = -1;
			    	
					try {
			    		Identity crt_identity = Identity.getCurrentConstituentIdentity();
			    		if (crt_identity == null) {
			    			Log.d(ConstituentSecLayer.TAG, "No identity");
			    		} else 
			    			myself_constituent_LID = config.Identity.getDefaultConstituentIDForOrg(oLID);
					} catch (P2PDDSQLException e1) {
						e1.printStackTrace();
					}
					if (myself_constituent_LID > 0) {
			    		Log.d(TAG, "setting myself that exists");
			    		myself_constituent = D_Constituent.getConstByLID(myself_constituent_LID, true, true);
			    		Log.d(TAG, "setting myself to: "+myself_constituent);
			    		myself_constituent_GID = myself_constituent.getGID();
			    		myself_constituent_GIDH = myself_constituent.getGIDH();
			    		
			    		if (myself_constituent.getSK() != null) {
							try {
					    		Identity crt_identity = Identity.getCurrentConstituentIdentity();
					    		if (crt_identity == null) {
					    			Log.d(ConstituentSecLayer.TAG, "No identity");
						    		Toast.makeText(ConstituentSecLayer.this, "No Identity!", Toast.LENGTH_SHORT).show();
						    		return;
					    		} else {
					    			config.Identity.setCurrentConstituentForOrg(myself_constituent_LID, oLID);
						    		Toast.makeText(ConstituentSecLayer.this, "Set Myself!", Toast.LENGTH_SHORT).show();
						    		Log.d(TAG, "setting const: "+myself_constituent);
						    		return;
					    		}
							} catch (P2PDDSQLException e1) {
								e1.printStackTrace();
					    		Toast.makeText(ConstituentSecLayer.this, "Fail: "+e1.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
					    		return;
							}
			    		} else {
				    		Toast.makeText(ConstituentSecLayer.this, "Fail: Unknown Keys!", Toast.LENGTH_SHORT).show();
				    		return;
			    		}
//			    		myself_constituent.setNeighborhood_LID(parentLID);
//			    		myself_constituent.setNeighborhoodGID(neighborhood.getGID());
//			    		myself_constituent.sign();
//			    		myself_constituent.storeRequest();
//			    		myself_constituent.releaseReference();
			    		
			    	} else {
			    		Toast.makeText(ConstituentSecLayer.this, "Register First!", Toast.LENGTH_SHORT).show();
			    		Log.d(TAG, "no const: " + myself_constituent);
			    		return;
			    	}
		    		//Toast.makeText(Constituent.this, "Failure!", Toast.LENGTH_SHORT).show();
					
					
				}
			});
			
			Button move = (Button) convertView.findViewById(R.id.constituent_child_view_move_here);
			move.setFocusable(false);
			if (isConstituent || childPosition != 0)	move.setVisibility(Button.GONE);
			else move.setVisibility(Button.VISIBLE);
			
			/**
			 * Currently we only implement the moving of myself to the fist level of neighborhoods
			 */
			move.setOnClickListener(new My_OnClickListener(new CtxConstituent(null, null, groupPosition, childPosition)) {
				
				@Override
				public void _onClick(View arg0) {
		    		Log.d(TAG, "moving myself ");
					CtxConstituent _ctx = (CtxConstituent) ctx;
					int n_idx = _ctx.groupPosition;
					if (n_idx >= neighs.size()) return;
					String parentLID = Util.getString(neighs.get(n_idx).get(0));
					//long parentLID = Util.lval(neighs.get(n_idx).get(0));
		    		Log.d(TAG, "moving myself to: "+parentLID);
					
					D_Neighborhood neighborhood = D_Neighborhood.getNeighByLID(parentLID, true, false);
		    		Log.d(TAG, "moving myself to: "+neighborhood);
					
					String myself_constituent_GID;
					String myself_constituent_GIDH;
			    	D_Constituent myself_constituent = null;
			    	long myself_constituent_LID = -1;
			    	
					try {
			    		Identity crt_identity = Identity.getCurrentConstituentIdentity();
			    		if (crt_identity == null) {
			    			Log.d(ConstituentSecLayer.TAG, "No identity");
			    		} else 
			    			myself_constituent_LID = config.Identity.getDefaultConstituentIDForOrg(oLID);
					} catch (P2PDDSQLException e1) {
						e1.printStackTrace();
					}
					if (myself_constituent_LID > 0) {
			    		Log.d(TAG, "moving myself that exists");
			    		myself_constituent = D_Constituent.getConstByLID(myself_constituent_LID, true, true);
			    		Log.d(TAG, "moving myself to: "+myself_constituent);
			    		myself_constituent_GID = myself_constituent.getGID();
			    		myself_constituent_GIDH = myself_constituent.getGIDH();
			    		
			    		myself_constituent.setNeighborhood_LID(parentLID);
			    		myself_constituent.setNeighborhoodGID(neighborhood.getGID());
			    		myself_constituent.sign();
			    		myself_constituent.storeRequest();
			    		myself_constituent.releaseReference();
			    		
			    		Log.d(TAG, "saved neighborhood const: "+myself_constituent);
			    	} else {
			    		Toast.makeText(ConstituentSecLayer.this, "Register First!", Toast.LENGTH_SHORT).show();
			    		Log.d(TAG, "no const: " + myself_constituent);
			    		return;
			    	}
					
					
				}
			});
			
			Button witness = (Button) convertView.findViewById(R.id.constituent_child_view_witness);
			witness.setFocusable(false);
			/*
			if (! isConstituent && childPosition != 0)	witness.setVisibility(Button.GONE);
			else witness.setVisibility(Button.VISIBLE);
			*/
			
			witness.setOnClickListener(new My_OnClickListener(new CtxConstituent(null, null, groupPosition, childPosition)) {
				@Override
				public void _onClick(View v) {
					CtxConstituent _ctx = (CtxConstituent) ctx;
					Intent i = new Intent();
					Bundle b = new Bundle();
					b.putInt(Orgs.O_ID, organization_position);
					b.putString(Orgs.O_GIDH, organization_GIDH);
					b.putString(Orgs.O_LID, ConstituentSecLayer.organization_LID);
					b.putString(Orgs.O_NAME, ConstituentSecLayer.organization_name);

					if (_ctx.groupPosition >= neighs.size()) {
						int c_idx = _ctx.groupPosition - neighs.size();
						if (c_idx >= consts.size()) return;
						D_Constituent c = D_Constituent.getConstByLID(Util.getString(consts.get(c_idx).get(0)), true, false);
						b.putString(ConstituentSecLayer.C_TYPE, ConstituentSecLayer.C_TYPE_C);
						b.putString(ConstituentSecLayer.C_GIDH, c.getGIDH());
						b.putString(ConstituentSecLayer.C_GID, c.getGID());
						b.putString(ConstituentSecLayer.C_LID, c.getLIDstr());
						b.putString(ConstituentSecLayer.C_NAME, c.getNameOrMy());
					} else {
						int n_idx = _ctx.groupPosition;
						if (n_idx >= neighs.size()) return;
						D_Neighborhood n = D_Neighborhood.getNeighByLID(Util.getString(neighs.get(n_idx).get(0)), true, false);

						if (_ctx.childPosition == 0) {
							b.putString(ConstituentSecLayer.C_TYPE, ConstituentSecLayer.C_TYPE_N);
							b.putString(ConstituentSecLayer.N_GIDH, n.getGIDH());
							b.putString(ConstituentSecLayer.N_GID, n.getGID());
							b.putString(ConstituentSecLayer.N_LID, n.getLIDstr());
							b.putString(ConstituentSecLayer.N_NAME, n.getNameOrMy());
						} else {
						/**
						 * Elements of the neighborhood
						 */
						
							if (_ctx.childPosition >= neighs_neighs.get(n_idx)) {
								int c_idx = _ctx.childPosition - neighs_neighs.get(n_idx);
								if (c_idx >= neighs_consts.size()) return;
								D_Constituent c = D_Constituent.getConstByLID(Util.getString(neighs_consts.get(c_idx).get(0)), true, false);
								b.putString(ConstituentSecLayer.C_TYPE, ConstituentSecLayer.C_TYPE_C);
								b.putString(ConstituentSecLayer.C_GIDH, c.getGIDH());
								b.putString(ConstituentSecLayer.C_GID, c.getGID());
								b.putString(ConstituentSecLayer.C_LID, c.getLIDstr());
								b.putString(ConstituentSecLayer.C_NAME, c.getNameOrMy());
							} else {
								int _n_idx = _ctx.groupPosition;
								if (_n_idx >= neighs_neighs_.size()) return;
								D_Neighborhood _n = D_Neighborhood.getNeighByLID(Util.getString(neighs_neighs_.get(n_idx).get(0)), true, false);
								b.putString(ConstituentSecLayer.C_TYPE, ConstituentSecLayer.C_TYPE_N);
								b.putString(ConstituentSecLayer.N_GIDH, _n.getGIDH());
								b.putString(ConstituentSecLayer.N_GID, _n.getGID());
								b.putString(ConstituentSecLayer.N_LID, _n.getLIDstr());
								b.putString(ConstituentSecLayer.N_NAME, _n.getNameOrMy());
							}
						}
					}

					i.putExtras(b);
					
					i.setClass(ConstituentSecLayer.this, Witness.class);
					startActivity(i);				
				}
			});
			// set the ClickListener to handle the click event on child item
			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Toast.makeText(activity, child.get(childPosition),
							Toast.LENGTH_SHORT).show();
				}
			});
			return convertView;
		}

		// method getGroupView is called automatically for each parent item
		// Implement this method as per your requirement
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			

			
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.constituents_parent_view, null);
			}
			
			
			CheckedTextView parentCheckedTextView = (CheckedTextView) convertView.findViewById(R.id.constituent_parent_checked_text_view);
			

//			if (!is_parent_constituent)	witness.setVisibility(Button.GONE);
//			else witness.setVisibility(Button.VISIBLE);
			
			parentCheckedTextView.setText(parentItems
					.get(groupPosition));
			parentCheckedTextView.setChecked(isExpanded);

			return convertView;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (childtems == null || childtems.size() <= groupPosition) return 0;
			return ((ArrayList<String>) childtems.get(groupPosition)).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getGroupCount() {
			return parentItems.size();
		}

		@Override
		public void onGroupCollapsed(int groupPosition) {
			super.onGroupCollapsed(groupPosition);
		}

		@Override
		public void onGroupExpanded(int groupPosition) {
			super.onGroupExpanded(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

    }
}

