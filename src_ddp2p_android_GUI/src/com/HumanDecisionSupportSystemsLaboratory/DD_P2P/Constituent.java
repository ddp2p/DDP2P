package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.util.ArrayList;

import net.ddp2p.common.config.Identity;

import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_Organization;

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

class BConstituentLayer {
	NeighborhoodNode crt_root = new NeighborhoodNode();
	
	public ArrayList<String> parentItems = new ArrayList<String>();
	public ArrayList<Object> childItems = new ArrayList<Object>();
	ArrayList<ArrayList<Object>> consts__LIDs = new ArrayList<ArrayList<Object>>();
	ArrayList<ArrayList<Object>> neighs__LIDs = new ArrayList<ArrayList<Object>>();
	ArrayList<Integer> neighs_neighs_number = new ArrayList<Integer> ();
	
	ArrayList<ArrayList<Object>> neighs__consts_LIDs = new ArrayList<ArrayList<Object>> ();
	ArrayList<ArrayList<Object>> neighs__neighs_LIDs = new ArrayList<ArrayList<Object>> ();
	long n_LID;
	D_Neighborhood n_;
}

@Deprecated
public class Constituent extends Activity {
	static ArrayList<BConstituentLayer> layers = new ArrayList<BConstituentLayer>();
	BConstituentLayer crtLayer;
	
	//private ArrayList<String> crtLayer.parentItems = new ArrayList<String>();
	//private ArrayList<Object> crtLayer.childItems = new ArrayList<Object>();
	private boolean isConstituent;
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
	//public static final String N_LID = "N_LID";
	public static final String N_NAME = "N_NAME";
	protected static final String N_PARENT_LID = "N_PARENT_LID";
	protected static final String TAG = "Constituent";
	long oLID;
	private ExpandableListView elist;
	private Button new_constituent, new_neighborhood;
	//ArrayList<ArrayList<Object>> crtLayer.consts__LIDs = new ArrayList<ArrayList<Object>>();
	//ArrayList<ArrayList<Object>> crtLayer.neighs__LIDs = new ArrayList<ArrayList<Object>>();

	//ArrayList<Integer> crtLayer.neighs_neighs_number = new ArrayList<Integer>();
	//ArrayList<ArrayList<Object>> crtLayer.neighs__consts_LIDs = new ArrayList<ArrayList<Object>>();
	//ArrayList<ArrayList<Object>> crtLayer.neighs__neighs_LIDs = new ArrayList<ArrayList<Object>>();
	private void popLayer() {
		if (ConstituentFurtherLayer.crt_layer_idx <= 0) {
			finish();
		} else {
			ConstituentFurtherLayer.crt_layer_idx --;
			Intent i = this.getIntent();
			finish();
			startActivity(i);
		}
	}

	protected void pushLayer(int position) {
		Log.d(TAG, "C2: pushLayer: start at pos="+position+" layer_idx="+crt_layer_idx+" /size="+layers.size());
		BConstituentLayer new_layer = new BConstituentLayer();
		if (position >= crtLayer.neighs__LIDs.size()) {
			Log.d(TAG, "C2: pushLayer: at pos="+position+" > "+crtLayer.neighs__LIDs.size());
			Toast.makeText(Constituent.this, "pushLayer: position "+position+"/"+crtLayer.neighs__LIDs.size(), Toast.LENGTH_SHORT).show();
			return;
		}
		new_layer.n_LID = Util.lval(crtLayer.neighs__LIDs.get(position).get(0));
		crt_layer_idx = layers.size();
		layers.add(new_layer);
		crtLayer = new_layer;
		Log.d(TAG, "C2: pushLayer: new pos="+position+" layer_idx="+crt_layer_idx+" /size="+layers.size()+ " nLID="+new_layer.n_LID);
		crtLayer.n_ = D_Neighborhood.getNeighByLID(crtLayer.n_LID, true, false);
		if (crtLayer.n_ == null) {
			Log.d(TAG, "C2: pushLayer: null neigh exit for: LID="+crtLayer.n_LID+ "at pos="+position);
			Toast.makeText(Constituent.this, "At Layer "+crt_layer_idx+" null name "+crtLayer.n_LID, Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(TAG, "C2: pushLayer: neigh layer_idx="+crt_layer_idx+" lID="+crtLayer.n_LID+" name="+crtLayer.n_.getName());
		Toast.makeText(Constituent.this, "At Layer "+crt_layer_idx+" name="+crtLayer.n_.getName(), Toast.LENGTH_SHORT).show();
		
		Intent i = this.getIntent();
		finish();
		startActivity(i);
		
		/*
    	if (crtLayer.n_LID > 0) {
			crtLayer.consts = D_Constituent.getAllConstituents(oLID, crtLayer.n_LID, false);
			crtLayer.neighs = D_Neighborhood.getAllNeighborhoods(oLID, crtLayer.n_LID, false);

			// Set the Items of Parent
			setGroupParents();
			// Set The Child Data
			setChildData();
    	}		
    	*/
	}

	static int crt_layer_idx = 0;
	static long prior_oLID = -1;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		setContentView(R.layout.constituent);
		elist = (ExpandableListView) findViewById(R.id.constituentElist);

		ActionBar actionbar = getActionBar();
		actionbar.setTitle("Constituent");
		
		new_constituent = (Button) findViewById(R.id.new_constituent);
		new_neighborhood = (Button) findViewById(R.id.new_neighborhood);

		Intent i = this.getIntent();
		Bundle b = i.getExtras();
		organization_position = b.getInt(Orgs.O_ID);
		organization_LID = b.getString(Orgs.O_LID);
		organization_GIDH = b.getString(Orgs.O_GIDH);
		organization_name  = b.getString(Orgs.O_NAME);
		
		oLID = Util.lval(organization_LID, -1);

		/**
		 * Set up the stack for navigating in child neighborhoods
		 */
		Log.d(TAG, "C2: onCreate: start layer_idx=" + crt_layer_idx + " /#="+layers.size()+ " oLID="+oLID+" vs old="+prior_oLID);
		if (oLID == prior_oLID && crt_layer_idx < layers.size()) {
			crtLayer = layers.get(crt_layer_idx);
			Log.d(TAG, "C2: onCreate: keep layer_idx=" + crt_layer_idx + " /#="+layers.size()+" nLID="+crtLayer.n_LID);
			while (layers.size() > crt_layer_idx + 1) {
				Log.d(TAG, "C2: onCreate: remove layer_idx=" + (crt_layer_idx + 1));
				layers.remove(crt_layer_idx+1);
			}
		} else {
			layers.clear();
			crtLayer = new BConstituentLayer();
			layers.add(crtLayer);
			crt_layer_idx = 0;
			crtLayer.n_LID = Util.lval(b.getString(Constituent.N_PARENT_LID));
			Log.d(TAG, "C2: onCreate: reset layer_idx=" + crt_layer_idx + " /#="+layers.size()+" nLID="+crtLayer.n_LID);
		}
		prior_oLID = oLID;
		
		/**
		 * Check if this is a valid organization and neighborhood
		 */
		if (oLID < 0) {
			Toast.makeText(this, "No Organization", Toast.LENGTH_SHORT).show();
			return;
		}
		if (crtLayer.n_LID >= 0) {
			crtLayer.n_ = D_Neighborhood.getNeighByLID(crtLayer.n_LID, true, false);
			if (crtLayer.n_ == null) {
				Toast.makeText(this, "Missing Neighborhood: LID="+crtLayer.n_LID, Toast.LENGTH_SHORT).show();
				return;
			}
		}
		
		D_Organization organization = D_Organization.getOrgByLID(oLID, true, false);
		organization_name = organization.getOrgNameOrMy();
		//actionbar.setTitle("Constituents for \""+organization.getOrgNameOrMy()+"\"");
		organization_GID = organization.getGID();
		if (crtLayer.n_ != null)
			actionbar.setTitle(crtLayer.n_.getName());
		else
			actionbar.setTitle(organization_name);

//		crtLayer.consts__LIDs = D_Constituent.getAllConstituents(oLID, -1, false);
//		crtLayer.neighs__LIDs = D_Neighborhood.getAllNeighborhoods(oLID, -1, false);
		crtLayer.consts__LIDs = D_Constituent.getAllConstituents(oLID, crtLayer.n_LID, false);
		crtLayer.neighs__LIDs = D_Neighborhood.getAllNeighborhoods(oLID, crtLayer.n_LID, false);

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
		MyExpandableAdapter adapter = new MyExpandableAdapter(crtLayer.parentItems,
				crtLayer.childItems);

		adapter.setInflater(
				(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE),
				this);

		// Set the Adapter to expandableList
		expandableList.setAdapter(adapter);
		// expandableList.setOnChildClickListener();

		new_constituent.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(Constituent.this, NewConstituent.class);
				Bundle b = new Bundle();
				b.putInt(Orgs.O_ID, organization_position);
				b.putString(Orgs.O_GIDH, organization_GIDH);
				b.putString(Orgs.O_GID, organization_GID);
				b.putString(Orgs.O_LID, organization_LID);
				b.putString(Orgs.O_NAME, organization_name);
				b.putString(Constituent.N_PARENT_LID, "-1");
				intent.putExtras(b);
				startActivity(intent);
			}
		});

		new_neighborhood.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(Constituent.this, NewNeighborhood.class);
				Bundle b = new Bundle();
				b.putInt(Orgs.O_ID, organization_position);
				b.putString(Orgs.O_GIDH, organization_GIDH);
				b.putString(Orgs.O_GID, organization_GID);
				b.putString(Orgs.O_LID, organization_LID);
				b.putString(Orgs.O_NAME, organization_name);
				b.putString(Constituent.N_PARENT_LID, "-1");
				intent.putExtras(b);
				startActivity(intent);
			}
		});
	}

	// method to add parent Items
	public void setGroupParents() {
		crtLayer.parentItems.clear();
		// crtLayer.parentItems.add("");
		for (ArrayList<Object> _n : crtLayer.neighs__LIDs) {
			long n_LID = Util.lval(_n.get(0));
			if (n_LID <= 0)
				continue;
			D_Neighborhood n = D_Neighborhood.getNeighByLID(n_LID, true, false);
			if (n == null)
				continue;
			crtLayer.parentItems.add(n.getName_division() + ": " + n.getName());
		}
		for (ArrayList<Object> _c : crtLayer.consts__LIDs) {
			long c_LID = Util.lval(_c.get(0));
			if (c_LID <= 0)
				continue;
			D_Constituent c = D_Constituent.getConstByLID(c_LID, true, false);
			if (c == null)
				continue;
			crtLayer.parentItems.add(c.getNameOrMy());
		}
		if (crtLayer.parentItems.size() == 0) {
			crtLayer.parentItems.add(net.ddp2p.common.util.Util
					.__("No constituent yet. Fill your profile!"));
		}
	}

	// method to set child data of each parent
	public void setChildData() {
		// ArrayList<String> _child = new ArrayList<String>();
		// crtLayer.childItems.add(_child);
		for (ArrayList<Object> _n : crtLayer.neighs__LIDs) {
			ArrayList<String> child = new ArrayList<String>();
			/**
			 * Each neighborhood has a first empty child with buttons
			 */
			child.add("");

			long n_LID = Util.lval(_n.get(0));
			if (n_LID <= 0) {
				child.add("N:" + n_LID);
				crtLayer.childItems.add(child);
				continue;
			}
			D_Neighborhood n = D_Neighborhood.getNeighByLID(n_LID, true, false);
			if (n == null) {
				child.add("N:" + n_LID);
				crtLayer.childItems.add(child);
				continue;
			}

			ArrayList<ArrayList<Object>> _consts = D_Constituent
					.getAllConstituents(oLID, n.getLID(), false);
			ArrayList<ArrayList<Object>> _neighs = D_Neighborhood
					.getAllNeighborhoods(oLID, n.getLID(), false);

			ArrayList<Object> _neighborhoods = new ArrayList<Object>();
			for (ArrayList<Object> __n : _neighs) {
				if (__n.size() <= 0)
					continue;
				Object n0 = __n.get(0);
				long n__LID = Util.lval(n0);
				if (n__LID <= 0)
					continue;
				D_Neighborhood n_ = D_Neighborhood.getNeighByLID(n__LID, true,
						false);
				if (n_ == null)
					continue;
				child.add(n_.getName_division() + ": " + n_.getName());
				_neighborhoods.add(n0);
			}
			crtLayer.neighs__neighs_LIDs.add(_neighborhoods);
			// if (child.size() == 0) {
			// child.add("Add elements: "+n.getNames_subdivisions_str());
			// }

			_neighs.add(0, new ArrayList<Object>());
			this.crtLayer.neighs__neighs_LIDs.add(0, new ArrayList<Object>());
			this.crtLayer.neighs_neighs_number.add(Integer.valueOf(_neighs.size()));

			ArrayList<Object> _constits = new ArrayList<Object>();
			for (ArrayList<Object> _c : _consts) {
				if (_c.size() <= 0)
					continue;
				Object c0 = _c.get(0);
				long c_LID = Util.lval(c0);
				if (c_LID <= 0)
					continue;
				D_Constituent c = D_Constituent.getConstByLID(c_LID, true,
						false);
				if (c == null)
					continue;
				child.add(c.getNameOrMy());
				_constits.add(c0);
			}
			crtLayer.neighs__consts_LIDs.add(_constits);
			crtLayer.childItems.add(child);
		}
		for (ArrayList<Object> _c : crtLayer.consts__LIDs) {
			ArrayList<String> child = new ArrayList<String>();
			long c_LID = Util.lval(_c.get(0));
			if (c_LID <= 0) {
				crtLayer.childItems.add(child);
				continue;
			}
			D_Constituent c = D_Constituent.getConstByLID(c_LID, true, false);
			if (c == null) {
				crtLayer.childItems.add(child);
				continue;
			}
			String email = c.getEmail();
			if (email == null)
				email = "No email!";
			child.add(Util.__("Email:") + " " + email);
			crtLayer.childItems.add(child);
		}
		//
		// // Add Child Items for Fruits
		// ArrayList<String> child = new ArrayList<String>();
		// child.add("Apple");
		// child.add("Mango");
		// child.add("Banana");
		// child.add("Orange");
		// crtLayer.childItems.add(child);
		//
		// // Add Child Items for Flowers
		// child = new ArrayList<String>();
		// child.add("Rose");
		// child.add("Lotus");
		// child.add("Jasmine");
		// child.add("Lily");
		// crtLayer.childItems.add(child);
		//
		// // Add Child Items for Animals
		// child = new ArrayList<String>();
		// child.add("Lion");
		// child.add("Tiger");
		// child.add("Horse");
		// child.add("Elephant");
		// crtLayer.childItems.add(child);
		//
		// // Add Child Items for Birds
		// child = new ArrayList<String>();
		// child.add("Parrot");
		// child.add("Sparrow");
		// child.add("Peacock");
		// child.add("Pigeon");
		// crtLayer.childItems.add(child);
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
		@SuppressWarnings("unchecked")
		@Override
		public View getChildView(int groupPosition, final int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			long parent_n_ID = -1;
			child = (ArrayList<String>) childtems.get(groupPosition);

			TextView textView = null;

			if (convertView == null) {
				convertView = inflater.inflate(
						R.layout.constituents_child_view, null);
			}

			boolean is_parent_constituent = true;
			if (groupPosition < crtLayer.neighs__LIDs.size()) {
				is_parent_constituent = false;
				parent_n_ID = Util.lval(crtLayer.neighs__LIDs.get(groupPosition).get(0));
			}

			boolean is_child_constituent = true;
			if (! is_parent_constituent) {
				if (groupPosition < crtLayer.neighs_neighs_number.size()
						&& childPosition < crtLayer.neighs_neighs_number.get(groupPosition)) {
					is_child_constituent = false;
				}
			}

		    isConstituent = !(!is_parent_constituent && !is_child_constituent);

			// get the textView reference and set the value
			textView = (TextView) convertView.findViewById(R.id.textViewChild);
			textView.setText(child.get(childPosition));
			Button addNeiborhood = (Button) convertView
					.findViewById(R.id.constituent_child_view_add_neiborhood);
			addNeiborhood.setFocusable(false);
			if (isConstituent || childPosition != 0)
				addNeiborhood.setVisibility(Button.GONE);
			else
				addNeiborhood.setVisibility(Button.VISIBLE);

			addNeiborhood
					.setOnClickListener(new My_OnClickListener(
							new CtxConstituent(null, null, groupPosition,
									childPosition)) {

						@Override
						public void _onClick(View arg0) {
							CtxConstituent _ctx = (CtxConstituent) ctx;
							int n_idx = _ctx.groupPosition;
							if (n_idx >= crtLayer.neighs__LIDs.size())
								return;
							String parentLID = Util.getString(crtLayer.neighs__LIDs.get(n_idx)
									.get(0));
							// long parentLID =
							// Util.lval(neighs.get(n_idx).get(0));
							Intent intent = new Intent();
							intent.setClass(Constituent.this,
									NewNeighborhood.class);
							Bundle b = new Bundle();
							b.putInt(Orgs.O_ID, organization_position);
							b.putString(Orgs.O_GIDH, organization_GIDH);
							b.putString(Orgs.O_GID, organization_GID);
							b.putString(Orgs.O_LID, organization_LID);
							b.putString(Orgs.O_NAME, organization_name);
							b.putString(Constituent.N_PARENT_LID, parentLID);
							intent.putExtras(b);
							startActivity(intent);
						}
					});

			Button addConstituent = (Button) convertView
					.findViewById(R.id.constituent_child_view_add_constituent);
			addConstituent.setFocusable(false);
			if (isConstituent || childPosition != 0)
				addConstituent.setVisibility(Button.GONE);
			else
				addConstituent.setVisibility(Button.VISIBLE);

			addConstituent
					.setOnClickListener(new My_OnClickListener(
							new CtxConstituent(null, null, groupPosition,
									childPosition)) {

						@Override
						public void _onClick(View arg0) {
							CtxConstituent _ctx = (CtxConstituent) ctx;
							int n_idx = _ctx.groupPosition;
							if (n_idx >= crtLayer.neighs__LIDs.size())
								return;
							String parentLID = Util.getString(crtLayer.neighs__LIDs.get(n_idx)
									.get(0));
							// long parentLID =
							// Util.lval(neighs.get(n_idx).get(0));
							Intent intent = new Intent();
							intent.setClass(Constituent.this,
									NewConstituent.class);
							Bundle b = new Bundle();
							b.putInt(Orgs.O_ID, organization_position);
							b.putString(Orgs.O_GIDH, organization_GIDH);
							b.putString(Orgs.O_GID, organization_GID);
							b.putString(Orgs.O_LID, organization_LID);
							b.putString(Orgs.O_NAME, organization_name);
							b.putString(Constituent.N_PARENT_LID, parentLID);
							intent.putExtras(b);
							startActivity(intent);
						}
					});

			/**
			 * Setting myself supported for constituents at both levels.
			 * Currently the button is shown only even when i do not know the
			 * secret key of the constituent. That should be eventually fixed
			 * (but needs to be made efficient, since testing/loading the key is
			 * slow).
			 * */
			Button myself = (Button) convertView
					.findViewById(R.id.constituent_child_view_set_myself);
			myself.setFocusable(false);
			if (!isConstituent)
				myself.setVisibility(Button.GONE);
			else
				myself.setVisibility(Button.VISIBLE);

			myself.setOnClickListener(new My_OnClickListener(
					new CtxConstituent(null, null, groupPosition, childPosition)) {

				@Override
				public void _onClick(View arg0) {
					D_Constituent c;
					Log.d(TAG, "setting myself ");
					CtxConstituent _ctx = (CtxConstituent) ctx;
					int n_idx = _ctx.groupPosition;
					if (n_idx < crtLayer.neighs__LIDs.size()) {
						/**
						 * Cannot be a neighborhood
						 */
						if (_ctx.childPosition < crtLayer.neighs_neighs_number.get(n_idx))
							return;
						int c_idx = _ctx.childPosition
								- crtLayer.neighs_neighs_number.get(n_idx);
						c = D_Constituent.getConstByLID(
								Util.getString(crtLayer.neighs__consts_LIDs.get(n_idx).get(
										c_idx)), true, false);
					} else {
						int c_idx = n_idx - crtLayer.neighs__LIDs.size();
						c = D_Constituent.getConstByLID(
								Util.getString(crtLayer.consts__LIDs.get(c_idx).get(0)), true,
								false);
					}
					// String parentLID =
					// Util.getString(neighs.get(n_idx).get(0));
					// //long parentLID = Util.lval(neighs.get(n_idx).get(0));
					// Log.d(TAG, "setting myself to: "+parentLID);
					//
					// D_Neighborhood neighborhood =
					// D_Neighborhood.getNeighByLID(parentLID, true, false);
					Log.d(TAG, "setting myself to: " + c);
					if (c == null)
						return;

					String myself_constituent_GID;
					String myself_constituent_GIDH;
					D_Constituent myself_constituent = null;
					long myself_constituent_LID = -1;

					try {
						Identity crt_identity = Identity.getCurrentConstituentIdentity();
						if (crt_identity == null) {
							Log.d(Constituent.TAG, "No identity");
						} else
							myself_constituent_LID = net.ddp2p.common.config.Identity
									.getDefaultConstituentIDForOrg(oLID);
					} catch (P2PDDSQLException e1) {
						e1.printStackTrace();
					}
					if (myself_constituent_LID > 0) {
						Log.d(TAG, "setting myself that exists");
						myself_constituent = D_Constituent.getConstByLID(
								myself_constituent_LID, true, true);
						Log.d(TAG, "setting myself to: " + myself_constituent);
						myself_constituent_GID = myself_constituent.getGID();
						myself_constituent_GIDH = myself_constituent.getGIDH();

						if (myself_constituent.getSK() != null) {
							try {
								Identity crt_identity = Identity
										.getCurrentConstituentIdentity();
								if (crt_identity == null) {
									Log.d(Constituent.TAG, "No identity");
									Toast.makeText(Constituent.this,
											"No Identity!", Toast.LENGTH_SHORT)
											.show();
									return;
								} else {
                                    net.ddp2p.common.config.Identity
											.setCurrentConstituentForOrg(
													myself_constituent_LID,
													oLID);
									Toast.makeText(Constituent.this,
											"Set Myself!", Toast.LENGTH_SHORT)
											.show();
									Log.d(TAG, "setting const: "
											+ myself_constituent);
									return;
								}
							} catch (P2PDDSQLException e1) {
								e1.printStackTrace();
								Toast.makeText(Constituent.this,
										"Fail: " + e1.getLocalizedMessage(),
										Toast.LENGTH_SHORT).show();
								return;
							}
						} else {
							Toast.makeText(Constituent.this,
									"Fail: Unknown Keys!", Toast.LENGTH_SHORT)
									.show();
							return;
						}
						// myself_constituent.setNeighborhood_LID(parentLID);
						// myself_constituent.setNeighborhoodGID(neighborhood.getGID());
						// myself_constituent.sign();
						// myself_constituent.storeRequest();
						// myself_constituent.releaseReference();

					} else {
						Toast.makeText(Constituent.this, "Register First!",
								Toast.LENGTH_SHORT).show();
						Log.d(TAG, "no const: " + myself_constituent);
						return;
					}
					// Toast.makeText(Constituent.this, "Failure!",
					// Toast.LENGTH_SHORT).show();

				}
			});

			Button move = (Button) convertView
					.findViewById(R.id.constituent_child_view_move_here);
			move.setFocusable(false);
			if (isConstituent || childPosition != 0)
				move.setVisibility(Button.GONE);
			else
				move.setVisibility(Button.VISIBLE);

			/**
			 * Currently we only implement the moving of myself to the first
			 * level of neighborhoods
			 */
			move.setOnClickListener(new My_OnClickListener(new CtxConstituent(
					null, null, groupPosition, childPosition)) {

				@Override
				public void _onClick(View arg0) {
					Log.d(TAG, "moving myself ");
					CtxConstituent _ctx = (CtxConstituent) ctx;
					int n_idx = _ctx.groupPosition;
					if (n_idx >= crtLayer.neighs__LIDs.size())
						return;
					String parentLID = Util.getString(crtLayer.neighs__LIDs.get(n_idx).get(0));
					// long parentLID = Util.lval(neighs.get(n_idx).get(0));
					Log.d(TAG, "moving myself to: " + parentLID);

					D_Neighborhood neighborhood = D_Neighborhood.getNeighByLID(
							parentLID, true, false);
					Log.d(TAG, "moving myself to: " + neighborhood);

					String myself_constituent_GID;
					String myself_constituent_GIDH;
					D_Constituent myself_constituent = null;
					long myself_constituent_LID = -1;

					try {
						Identity crt_identity = Identity.getCurrentConstituentIdentity();
						if (crt_identity == null) {
							Log.d(Constituent.TAG, "No identity");
						} else
							myself_constituent_LID = net.ddp2p.common.config.Identity
									.getDefaultConstituentIDForOrg(oLID);
					} catch (P2PDDSQLException e1) {
						e1.printStackTrace();
					}
					if (myself_constituent_LID > 0) {
						Log.d(TAG, "moving myself that exists");
						myself_constituent = D_Constituent.getConstByLID(
								myself_constituent_LID, true, true);
						Log.d(TAG, "moving myself to: " + myself_constituent);
						myself_constituent_GID = myself_constituent.getGID();
						myself_constituent_GIDH = myself_constituent.getGIDH();

						myself_constituent.setNeighborhood_LID(parentLID);
						myself_constituent.setNeighborhoodGID(neighborhood
								.getGID());
						myself_constituent.sign();
						myself_constituent.storeRequest();
						myself_constituent.releaseReference();

						Log.d(TAG, "saved neighborhood const: "
								+ myself_constituent);
					} else {
						Toast.makeText(Constituent.this, "Register First!",
								Toast.LENGTH_SHORT).show();
						Log.d(TAG, "no const: " + myself_constituent);
						return;
					}

				}
			});

			Button witness = (Button) convertView
					.findViewById(R.id.constituent_child_view_witness);
			witness.setFocusable(false);
			/*
			 * if (! isConstituent && childPosition != 0)
			 * witness.setVisibility(Button.GONE); else
			 * witness.setVisibility(Button.VISIBLE);
			 */

			witness.setOnClickListener(new My_OnClickListener(
					new CtxConstituent(null, null, groupPosition, childPosition)) {
				@Override
				public void _onClick(View v) {
					CtxConstituent _ctx = (CtxConstituent) ctx;
					Intent i = new Intent();
					Bundle b = new Bundle();
					b.putInt(Orgs.O_ID, organization_position);
					b.putString(Orgs.O_GIDH, organization_GIDH);
					b.putString(Orgs.O_LID, Constituent.organization_LID);
					b.putString(Orgs.O_NAME, Constituent.organization_name);

					if (_ctx.groupPosition >= crtLayer.neighs__LIDs.size()) {
						int c_idx = _ctx.groupPosition - crtLayer.neighs__LIDs.size();
						if (c_idx >= crtLayer.consts__LIDs.size())
							return;
						D_Constituent c = D_Constituent.getConstByLID(
								Util.getString(crtLayer.consts__LIDs.get(c_idx).get(0)), true, false);
						b.putString(Constituent.C_TYPE, Constituent.C_TYPE_C);
						b.putString(Constituent.C_GIDH, c.getGIDH());
						b.putString(Constituent.C_GID, c.getGID());
						b.putString(Constituent.C_LID, c.getLIDstr());
						b.putString(Constituent.C_NAME, c.getNameOrMy());
					} else {
						int n_idx = _ctx.groupPosition;
						if (n_idx >= crtLayer.neighs__LIDs.size())
							return;
						D_Neighborhood n = D_Neighborhood.getNeighByLID(
								Util.getString(crtLayer.neighs__LIDs.get(n_idx).get(0)), true, false);

						if (_ctx.childPosition == 0) {
							b.putString(Constituent.C_TYPE,
									Constituent.C_TYPE_N);
							b.putString(Constituent.N_GIDH, n.getGIDH());
							b.putString(Constituent.N_GID, n.getGID());
							b.putString(Constituent.N_PARENT_LID, n.getLIDstr());
							b.putString(Constituent.N_NAME, n.getNameOrMy());
						} else {
							/**
							 * Elements of the neighborhood
							 */

							if (_ctx.childPosition >= crtLayer.neighs_neighs_number.get(n_idx)) {
								int c_idx = _ctx.childPosition
										- crtLayer.neighs_neighs_number.get(n_idx);
								if (c_idx >= crtLayer.neighs__consts_LIDs.size())
									return;
								D_Constituent c = D_Constituent.getConstByLID(
										Util.getString(crtLayer.neighs__consts_LIDs.get(c_idx)
												.get(0)), true, false);
								b.putString(Constituent.C_TYPE,
										Constituent.C_TYPE_C);
								b.putString(Constituent.C_GIDH, c.getGIDH());
								b.putString(Constituent.C_GID, c.getGID());
								b.putString(Constituent.C_LID, c.getLIDstr());
								b.putString(Constituent.C_NAME, c.getNameOrMy());
							} else {
								/**
								 * The group Position
								 */
								int _n_idx = _ctx.groupPosition;
								if (_n_idx >= crtLayer.neighs__neighs_LIDs.size()) {
									Toast.makeText(Constituent.this, "No such neighborhood:"+_n_idx+" or "+n_idx+" / "+crtLayer.neighs__neighs_LIDs.size(),
											Toast.LENGTH_SHORT).show();
									Log.d("CONS", "No such neighborhood:"+_n_idx+" or "+n_idx+" / "+crtLayer.neighs__neighs_LIDs.size());
									return;
								}
								
								if (crtLayer.neighs__neighs_LIDs.size() <= n_idx) {
									Toast.makeText(Constituent.this, "No such neighborhood:"+_n_idx+" or "+n_idx+" / "+crtLayer.neighs__neighs_LIDs.size(),
											Toast.LENGTH_SHORT).show();
									Log.d("CONS", "No such neighborhood:"+_n_idx+" or "+n_idx+" / "+crtLayer.neighs__neighs_LIDs.size());
									return;
								}
								if (crtLayer.neighs__neighs_LIDs.get(n_idx).size() <= 0) {
									Toast.makeText(Constituent.this, "No such neighborhoodLID:"+_n_idx+" or "+n_idx+" / "+crtLayer.neighs__neighs_LIDs.size(),
											Toast.LENGTH_SHORT).show();
									Log.d("CONS", "No such neighborhoodLID:"+_n_idx+" or "+n_idx+" / "+crtLayer.neighs__neighs_LIDs.size());
									return;
								}
								
								D_Neighborhood _n = D_Neighborhood
										.getNeighByLID(Util
												.getString(crtLayer.neighs__neighs_LIDs.get(n_idx).get(0)), true, false);
								b.putString(Constituent.C_TYPE,
										Constituent.C_TYPE_N);
								b.putString(Constituent.N_GIDH, _n.getGIDH());
								b.putString(Constituent.N_GID, _n.getGID());
								b.putString(Constituent.N_PARENT_LID, _n.getLIDstr());
								b.putString(Constituent.N_NAME, _n.getNameOrMy());
							}
						}
					}

					i.putExtras(b);

					i.setClass(Constituent.this, Witness.class);
					startActivity(i);
				}
			});

			// set the ClickListener to handle the click event on child item
			convertView.setOnClickListener(new My_OnClickListener(Long.valueOf(parent_n_ID)) {

				@Override
				public void _onClick(View view) {
					if (! isConstituent) {

						Long nLID = (Long)ctx;
						Intent i = new Intent();
						//i.setClass(Constituent.this, ConstituentFurtherLayer.class);
						i.setClass(Constituent.this, Constituent.class);
						
						Bundle b = new Bundle();
						//b.putInt(Orgs.O_ID, organization_position);
						b.putString(Orgs.O_GIDH, Constituent.this.organization_GIDH);
						b.putString(Orgs.O_LID, Constituent.this.organization_LID);
						b.putString(Orgs.O_NAME, Constituent.this.organization_name);
						b.putString(Constituent.N_PARENT_LID, nLID+"");
						i.putExtras(b);
						
						startActivity(i);
					} else {
						Log.d(TAG, "Believed Constituent");
					}
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
				convertView = inflater.inflate(
						R.layout.constituents_parent_view, null);
			}

			CheckedTextView parentCheckedTextView = (CheckedTextView) convertView
					.findViewById(R.id.constituent_parent_checked_text_view);

			// if (!is_parent_constituent) witness.setVisibility(Button.GONE);
			// else witness.setVisibility(Button.VISIBLE);

			parentCheckedTextView.setText(crtLayer.parentItems.get(groupPosition));
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
			if (childtems == null || childtems.size() <= groupPosition)
				return 0;
			return ((ArrayList<String>) childtems.get(groupPosition)).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getGroupCount() {
			return crtLayer.parentItems.size();
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

class CtxConstituent {
	D_Constituent c;
	D_Neighborhood n;
	int groupPosition;
	int childPosition;

	CtxConstituent(D_Constituent _c, D_Neighborhood _n, int pos, int sub) {
		c = _c;
		n = _n;
		groupPosition = pos;
		childPosition = sub;
	}
}

abstract class My_OnClickListener implements OnClickListener {
	public Object ctx;

	public My_OnClickListener(Object _ctx) {
		ctx = _ctx;
	}

	public abstract void _onClick(View v);

	@Override
	public void onClick(View v) {
		_onClick(v);
	}
}
