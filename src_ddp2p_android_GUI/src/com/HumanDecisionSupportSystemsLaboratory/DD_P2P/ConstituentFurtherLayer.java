package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.util.ArrayList;

import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_Organization;

class A_Witness {
    int eligibility_from_me;
    int trust_from_me;
    int eligibility_positive;
    int eligibility_negative;
    A_Witness(D_Witness w) {
        if (w == null) return;
        this.eligibility_from_me = w.sense_y_n;
        this.trust_from_me = w.sense_y_trustworthiness;
    }
}

/**
 * Currently this does not model leaves (except for constituents with no email)
 * 
 * @author M. Silaghi and Hang Dong
 * 
 */
class CTreeNode {
	/**
	 * Get child for a leaf
	 * 
	 * @param childPosition
	 * @return
	 */
	public String getDisplayText(int childPosition) {
		return null;
	}

	public int getLeavesCount() {
		return 0;
	}

	public CTreeNode getBranch(int groupPosition) {
		return null;
	}

}

class ConstituentNode extends CTreeNode {
    public long constituent_LID;
    public boolean external;
    public boolean hasSK;
    public String name_constituent;
    public String email;
    public D_Witness witness;

    public ConstituentNode(long c_LID, String name) {
        constituent_LID = c_LID;
        name_constituent = name;
    }
    public ConstituentNode(long c_LID, D_Constituent c) {
        constituent_LID = c_LID;
        if (c != null) {
            name_constituent = c.getNameOrMy();
            email = c.getEmail();
            external = c.isExternal();
            hasSK = true;// (c.getSK() != null);
        }
    }

	@Override
	public String getDisplayText(int childPosition) {
		if (childPosition == 0)
			return email;
		return null;
	}

	@Override
	public int getLeavesCount() {
		return (email == null) ? 0 : 1;
	}
}

class NeighborhoodNode extends CTreeNode {
	boolean loaded = false;
	public String name_neighborhood;
	public long neighborhood_LID = -1;
	public ArrayList<String> header = new ArrayList<String>();
	public ArrayList<NeighborhoodNode> neighborhoods = new ArrayList<NeighborhoodNode>();
	public ArrayList<ConstituentNode> constituents = new ArrayList<ConstituentNode>();
	public ArrayList<String> tail = new ArrayList<String>();

    A_Witness my_witness;


    public NeighborhoodNode() {
	}

	public NeighborhoodNode(long n_LID, String name) {
		neighborhood_LID = n_LID;
		name_neighborhood = name;
	}

	@Override
	public String getDisplayText(int groupPosition) {
		final boolean DEBUG = false;
		int idx;
		if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: h=" + header.size()+" n="+neighborhoods.size()+" c="+constituents.size()+" t="+tail.size()+" gP="+groupPosition);
		if (groupPosition < 0) {
			if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: exit with null");
			return null;
		}

		if (groupPosition < header.size()) {
			if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: got header gP="+groupPosition+" val="+header.get(groupPosition));
			return header.get(groupPosition);
		}
		idx = groupPosition - header.size();
		if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: after head gP="+groupPosition+" idx="+idx);

		if (idx < neighborhoods.size()) {
			if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: got neigh gP="+groupPosition+" val="+neighborhoods.get(idx).name_neighborhood);
			return neighborhoods.get(idx).name_neighborhood;
		}
		idx = groupPosition - header.size() - neighborhoods.size();
		if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: after neigh gP="+groupPosition+" idx="+idx);

		if (idx < constituents.size()) {
			if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: got const gP="+groupPosition+" val="+constituents.get(idx).name_constituent);
			return constituents.get(idx).name_constituent;
		}
		idx = groupPosition - header.size() - neighborhoods.size() - constituents.size();
		if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: after const gP="+groupPosition+" idx="+idx);

		if (idx < tail.size()) {
			if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: got tail gP="+groupPosition+" val="+tail.get(idx));
			return tail.get(idx);
		}
		// idx = idx - tail.size();
		if (DEBUG) Log.d(ConstituentFurtherLayer.TAG, "ConstituentsFurther: NeighborhoodNode: return null gP="+groupPosition);

		return null;
	}

	@Override
	public int getLeavesCount() {
		return header.size() + neighborhoods.size() + constituents.size()
				+ tail.size();
	}

	@Override
	public CTreeNode getBranch(int groupPosition) {
		int idx;
		if (groupPosition < 0)
			return null;
		if (groupPosition < header.size())
			return new CTreeNode();// header.get(groupPosition);
		idx = groupPosition - header.size();

		if (idx < neighborhoods.size())
			return neighborhoods.get(idx);
		idx = groupPosition - neighborhoods.size();

		if (idx < constituents.size())
			return constituents.get(idx);
		// idx = groupPosition - constituents.size();

		// returning nothing for tail or rest
		return null;
	}

	public boolean isNeighborhood(int parent, int child) {
		return this.isNeighborhood(parent)
				&& getNeighborhoodNode(parent).isNeighborhood(child);
	}

	/**
	 * Tells if this element id a constituent (e.g. to show appropriate buttons)
	 * 
	 * @param parent
	 * @param child
	 * @return
	 */
	public boolean isConstituent(int parent, int child) {
		return (this.isNeighborhood(parent) && getNeighborhoodNode(parent)
				.isConstituent(child)) || this.isConstituent(parent);
	}

	public boolean isNeighborhood(int idx) {
		if (idx >= header.size() && idx < header.size() + neighborhoods.size())
			return true;
		return false;
	}

	public boolean isConstituent(int idx) {
		if (idx >= header.size() + neighborhoods.size()
				&& idx < header.size() + neighborhoods.size()
						+ constituents.size())
			return true;
		return false;
	}

	/**
	 * Returns a constituent in this neighborhood or in the subneighborhood (if
	 * the child position fits one)
	 * 
	 * @param groupPosition
	 * @param childPosition
	 * @return
	 */
	public ConstituentNode getConstituentNode(int groupPosition, int childPosition) {
		if (isConstituent(groupPosition))
			return this.constituents.get(groupPosition - (header.size() + neighborhoods.size()));
		if (isNeighborhood(groupPosition)) {
			return this.neighborhoods.get(groupPosition - header.size())
					.getConstituentNode(childPosition);
		}
		return null;
	}

	/**
	 * To get a constituent from a this neighborhood node
	 * 
	 * @param childPosition
	 * @return
	 */
	public ConstituentNode getConstituentNode(int childPosition) {
		if (isConstituent(childPosition))
			return this.constituents.get(childPosition - (header.size() + neighborhoods.size()));
		return null;
	}

	/**
	 * If childPosition is 0 returns the parent neighborhood
	 * 
	 * @param groupPosition
	 * @param childPosition
	 * @return
	 */
	public NeighborhoodNode getNeighborhoodNode(int groupPosition,
			int childPosition) {
		if (isNeighborhood(groupPosition)) {
			return this.neighborhoods.get(groupPosition - header.size())
					.getNeighborhoodNode(childPosition);
		}
		return null;
	}

	/**
	 * Returns a neighborhood node for this position. return this in case the
	 * position is in the header
	 * 
	 * @param childPosition
	 * @return
	 */
	NeighborhoodNode getNeighborhoodNode(int childPosition) {
		if (childPosition < header.size())
			return this;
		if (isNeighborhood(childPosition))
			return this.neighborhoods.get(childPosition - header.size());
		return null;
	}

	/**
	 * Shows +neighborhood only in first header child of a neighborhood.
	 * 
	 * @param groupPosition
	 * @param childPosition
	 * @return
	 */
	boolean isParentNeighborhoodArea(int groupPosition, int childPosition) {
		return (this.isNeighborhood(groupPosition) && childPosition == 0 && childPosition < this
				.getNeighborhoodNode(groupPosition).header.size());
	}

	public boolean showAddNeighborhoodButton(int groupPosition,
			int childPosition) {
		return isParentNeighborhoodArea(groupPosition, childPosition);
	}

	public boolean showAddConstituentButton(int groupPosition, int childPosition) {
		return isParentNeighborhoodArea(groupPosition, childPosition);
	}

	public boolean showMoveHereButton(int groupPosition, int childPosition) {
		return isParentNeighborhoodArea(groupPosition, childPosition);
	}

	public void clean() {
		this.header.clear();
		this.neighborhoods.clear();
		this.constituents.clear();
		this.tail.clear();
	}
}

class ConstituentLayer {
	NeighborhoodNode crt_root = new NeighborhoodNode();

	// public ArrayList<String> parentItems = new ArrayList<String>();
	// public ArrayList<Object> childItems = new ArrayList<Object>();
	ArrayList<ArrayList<Object>> consts_LIDs = new ArrayList<ArrayList<Object>>();
	ArrayList<ArrayList<Object>> neighs_LIDs = new ArrayList<ArrayList<Object>>();
	// ArrayList<Integer> neighs_neighs_number = new ArrayList<Integer> ();

	// ArrayList<ArrayList<Object>> neighs_consts_LIDs = new
	// ArrayList<ArrayList<Object>> ();
	// ArrayList<ArrayList<Object>> neighs_neighs_LIDs = new
	// ArrayList<ArrayList<Object>> ();
	long n_LID;
	private D_Neighborhood n_;

	D_Neighborhood getN_() {
		return n_;
	}

	void setN_(D_Neighborhood n_) {
		this.n_ = n_;
	}
}

public class ConstituentFurtherLayer extends Activity {
	static ArrayList<ConstituentLayer> layers = new ArrayList<ConstituentLayer>();
    final private static int Color_Myself = Color.YELLOW;
    final private static int Color_Others = Color.DKGRAY;
    final private static int Color_Has_SK = Color.WHITE;

    ConstituentLayer crtLayer;

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
	// public static final String N_LID = "N_LID";
	public static final String N_NAME = "N_NAME";
	protected static final String N_PARENT_LID = "N_PARENT_LID";
	protected static final String TAG = "Constituent";
	long oLID;
	private ExpandableListView elist;
	private Button new_constituent, new_neighborhood;

	// private String orgName;
	// private String organization_lid;
	// private String organization_gidh;

	private void popLayer() {
		if (ConstituentFurtherLayer.crt_layer_idx <= 0) {
			finish();
		} else {
			ConstituentFurtherLayer.crt_layer_idx--;
			Intent i = this.getIntent();
			finish();
			startActivity(i);
		}
	}

	protected void pushLayer(int position) {
		Log.d(TAG, "C2: pushLayer: start at pos=" + position + " layer_idx="
				+ crt_layer_idx + " /size=" + layers.size());
		ConstituentLayer new_layer = new ConstituentLayer();
		for (ConstituentLayer cl : layers) {
			if (cl == null) {
				Log.d(TAG, "Condt L 1");
				continue;
			}
			if (cl.getN_() == null) {
				Log.d(TAG, "Condt L 2");
				continue;
			}
			if (cl.getN_().getName() == null) {
				Log.d(TAG, "Condt L 3");
				continue;
			}
			if (new_layer == null) {
				Log.d(TAG, "Condt L 4");
				continue;
			}
			if (new_layer.crt_root == null) {
				Log.d(TAG, "Condt L 5");
				continue;
			}
			if (new_layer.crt_root.header == null) {
				Log.d(TAG, "Condt L 6");
				continue;
			}
			new_layer.crt_root.header.add(cl.getN_().getName());
		}
		if (position >= crtLayer.neighs_LIDs.size()) {
			Log.d(TAG, "C2: pushLayer: at pos=" + position + " > "
					+ crtLayer.neighs_LIDs.size());
			Toast.makeText(
					ConstituentFurtherLayer.this,
					"pushLayer: position " + position + "/"
							+ crtLayer.neighs_LIDs.size(), Toast.LENGTH_SHORT)
					.show();
			return;
		}
		new_layer.n_LID = Util.lval(crtLayer.neighs_LIDs.get(position).get(0));
		crt_layer_idx = layers.size();
		layers.add(new_layer);
		crtLayer = new_layer;
		Log.d(TAG, "C2: pushLayer: new pos=" + position + " layer_idx="
				+ crt_layer_idx + " /size=" + layers.size() + " nLID="
				+ new_layer.n_LID);
		crtLayer.setN_(D_Neighborhood
				.getNeighByLID(crtLayer.n_LID, true, false));
		if (crtLayer.getN_() == null) {
			Log.d(TAG, "C2: pushLayer: null neigh exit for: LID="
					+ crtLayer.n_LID + "at pos=" + position);
			Toast.makeText(
					ConstituentFurtherLayer.this,
					"At Layer " + crt_layer_idx + " null name "
							+ crtLayer.n_LID, Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(TAG, "C2: pushLayer: neigh layer_idx=" + crt_layer_idx + " lID="
				+ crtLayer.n_LID + " name=" + crtLayer.getN_().getName());
		Toast.makeText(
				ConstituentFurtherLayer.this,
				"At Layer " + crt_layer_idx + " name="
						+ crtLayer.getN_().getName(), Toast.LENGTH_SHORT)
				.show();

		Intent i = this.getIntent();
		finish();
		startActivity(i);

		/*
		 * if (crtLayer.n_LID > 0) { crtLayer.consts =
		 * D_Constituent.getAllConstituents(oLID, crtLayer.n_LID, false);
		 * crtLayer.neighs = D_Neighborhood.getAllNeighborhoods(oLID,
		 * crtLayer.n_LID, false);
		 * 
		 * // Set the Items of Parent setGroupParents(); // Set The Child Data
		 * setChildData(); }
		 */
	}

	static int crt_layer_idx = 0;
	static long prior_oLID = -1;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		// enable action bar home button
		android.app.ActionBar actBar = getActionBar();
		actBar.setDisplayHomeAsUpEnabled(true);

		Intent i = this.getIntent();
		Bundle b = i.getExtras();
		organization_name = b.getString(Orgs.O_NAME);
		organization_LID = b.getString(Orgs.O_LID);
		organization_GIDH = b.getString(Orgs.O_GIDH);
		oLID = Util.lval(organization_LID);

		/**
		 * Set up the stack for navigating in child neighborhoods
		 */
		Log.d(TAG, "C2: onCreate: start layer_idx=" + crt_layer_idx + " /#="
				+ layers.size() + " oLID=" + oLID + " vs old=" + prior_oLID);
		if (oLID == prior_oLID && crt_layer_idx < layers.size()) {
			crtLayer = layers.get(crt_layer_idx);
			Log.d(TAG, "C2: onCreate: keep layer_idx=" + crt_layer_idx + " /#="
					+ layers.size() + " nLID=" + crtLayer.n_LID);
			while (layers.size() > crt_layer_idx + 1) {
				Log.d(TAG, "C2: onCreate: remove layer_idx="
						+ (crt_layer_idx + 1));
				layers.remove(crt_layer_idx + 1);
			}
		} else {
			layers.clear();
			crtLayer = new ConstituentLayer();
			layers.add(crtLayer);
			crt_layer_idx = 0;
			crtLayer.n_LID = Util.lval(b.getString(Constituent.N_PARENT_LID));
			Log.d(TAG, "C2: onCreate: reset layer_idx=" + crt_layer_idx
					+ " /#=" + layers.size() + " nLID=" + crtLayer.n_LID);
		}
		prior_oLID = oLID;

		/**
		 * Check if this is a valid organization and neighborhood
		 */
		if (oLID < 0) {
			Toast.makeText(this, "No Organization", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		if (crtLayer.n_LID >= 0) {
			crtLayer.setN_(D_Neighborhood.getNeighByLID(crtLayer.n_LID, true,
					false));
			if (crtLayer.getN_() == null) {
				Toast.makeText(this,
						"Missing Neighborhood: LID=" + crtLayer.n_LID,
						Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
		}
		/**
		 * Init the GUI
		 */
		setContentView(R.layout.constituent);
		elist = (ExpandableListView) findViewById(R.id.constituentElist);

		new_constituent = (Button) findViewById(R.id.new_constituent);
		new_neighborhood = (Button) findViewById(R.id.new_neighborhood);

		D_Organization organization = D_Organization.getOrgByLID(oLID, true,
				false);
		organization_name = organization.getOrgNameOrMy();
		// actionbar.setTitle("Constituents for \""+organization.getOrgNameOrMy()+"\"");
		organization_GID = organization.getGID();

		ActionBar actionbar = getActionBar();
		if (crtLayer.getN_() != null)
			actionbar.setTitle(crtLayer.getN_().getName());
		else
			actionbar.setTitle(organization_name);

		// if (crtLayer.n_LID > 0) {
		crtLayer.consts_LIDs = D_Constituent.getAllConstituents(oLID,
				crtLayer.n_LID, false);
		crtLayer.neighs_LIDs = D_Neighborhood.getAllNeighborhoods(oLID,
				crtLayer.n_LID, false);
		// }

		// Create Expandable List and set it's properties
		ExpandableListView expandableList = elist;
		expandableList.setDividerHeight(2);
		expandableList.setGroupIndicator(null);
		expandableList.setClickable(true);

		if (! crtLayer.crt_root.loaded) { // if wanting to refresh, one has to
											// empty the existing lists!
			crtLayer.crt_root.clean();
			// Set the Items of Parent
			setGroupParents();
			// Set The Child Data
			setChildData();

			// If we do not reload, then we do not see newly added items until
			// we change organization
			// commenting next line forces reload
			// crtLayer.crt_root.loaded = true;
		}

		// Create the Adapter
		MyExpandableAdapter adapter = new MyExpandableAdapter(crtLayer.crt_root);// crtLayer.parentItems,
																					// crtLayer.childItems);

		adapter.setInflater(
				(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE),
				this);

		// Set the Adapter to expandableList
		expandableList.setAdapter(adapter);
		// expandableList.setOnChildClickListener();

		new_constituent.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent();
					intent.setClass(ConstituentFurtherLayer.this,
							NewConstituent.class);
					Bundle b = new Bundle();
					b.putInt(Orgs.O_ID, organization_position);
					b.putString(Orgs.O_GIDH, organization_GIDH);
					b.putString(Orgs.O_GID, organization_GID);
					b.putString(Orgs.O_LID, organization_LID);
					b.putString(Orgs.O_NAME, organization_name);
					b.putString(ConstituentFurtherLayer.N_PARENT_LID,
							crtLayer.n_LID + "");
					// b.putString(ConstituentFurtherLayer.N_PARENT_LID,
					// Util.getStringID(crtLayer.n_LID));
					intent.putExtras(b);
					startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		new_neighborhood.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent();
					intent.setClass(ConstituentFurtherLayer.this,
							NewNeighborhood.class);
					Bundle b = new Bundle();
					b.putInt(Orgs.O_ID, organization_position);
					b.putString(Orgs.O_GIDH, organization_GIDH);
					b.putString(Orgs.O_GID, organization_GID);
					b.putString(Orgs.O_LID, organization_LID);
					b.putString(Orgs.O_NAME, organization_name);
					b.putString(ConstituentFurtherLayer.N_PARENT_LID,
							crtLayer.n_LID + "");
					// b.putString(ConstituentFurtherLayer.N_PARENT_LID,
					// Util.getStringID(crtLayer.n_LID));
					intent.putExtras(b);
					startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		registerForContextMenu(expandableList);
	}
	public final int MENU_ID_WITNESS = 1;
	public final int MENU_ID_MYSELF = 2;
	public final int MENU_ID_BLOCKED = 3;
	public final int MENU_ID_HERE = 4;
	public final int MENU_ID_NEIG = 5;
	public final int MENU_ID_CONST = 6;
	public final int MENU_ID_BROADCAST = 7;
	public final int MENU_ID_HIDE = 8;
	public final int MENU_ID_PSEUDONYM = 9;
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int position = ExpandableListView.getPackedPositionGroup(info.packedPosition);
		boolean neighborhood = isParentNeighborhood(position); 
		boolean constituent = isParentConstituent(position); 

		if (neighborhood) {
			menu.setHeaderTitle("action on neighborhood");
			menu.add(0, MENU_ID_WITNESS, Menu.NONE, "Witness");
			//menu.add(0, MENU_ID_MYSELF, Menu.NONE, "set myself");
			menu.add(0, MENU_ID_BLOCKED, Menu.NONE, "Set blocked");
			menu.add(0, MENU_ID_BROADCAST, Menu.NONE, "Set broadcasting");
			menu.add(0, MENU_ID_HIDE, Menu.NONE, "Hide");
			menu.add(2, MENU_ID_HERE, Menu.NONE, "Move here");
			menu.add(1, MENU_ID_NEIG, Menu.NONE, "Add neighborhood here");
			menu.add(1, MENU_ID_CONST, Menu.NONE, "Add constituent here"); // v.getID()
			menu.add(1, MENU_ID_PSEUDONYM, Menu.NONE, "Change Pseudonym"); // v.getID()
		}
		if (constituent) {
			menu.setHeaderTitle("action on constituent");
			menu.add(0, MENU_ID_WITNESS, Menu.NONE, "witness");
			menu.add(0, MENU_ID_MYSELF, Menu.NONE, "set myself");
			menu.add(0, MENU_ID_BLOCKED, Menu.NONE, "set blocked");
			menu.add(0, MENU_ID_BROADCAST, Menu.NONE, "set broadcasting");
			menu.add(0, MENU_ID_HIDE, Menu.NONE, "Hide");
			menu.add(1, MENU_ID_PSEUDONYM, Menu.NONE, "Change Pseudonym"); // v.getID()
			//menu.add(2, MENU_ID_HERE, Menu.NONE, "Move here");
			//menu.add(1, MENU_ID_NEIG, Menu.NONE, "add neighborhood here");
			//menu.add(1, MENU_ID_CONST, Menu.NONE, "add constituent here"); // v.getID()
		}
	}

	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		//the position which is clicked
		//TODO finish the 
		int position = ExpandableListView.getPackedPositionGroup(info.packedPosition);
		CTreeNode node = this.crtLayer.crt_root.getBranch(position);
		NeighborhoodNode neighborhood = null;
		ConstituentNode constituent = null;
		
		if (node instanceof NeighborhoodNode) neighborhood = (NeighborhoodNode) node;
		if (node instanceof ConstituentNode) constituent = (ConstituentNode) node;
		
		boolean _neighborhood = isParentNeighborhood(position); 
		boolean _constituent = isParentConstituent(position);
		
		if (item.getItemId() == MENU_ID_WITNESS) {
			Toast.makeText(this, "Action 1 invoked || " + "position: "+ position, Toast.LENGTH_SHORT).show();
			if (_neighborhood) action_witness(neighborhood.neighborhood_LID, neighborhood);
			if (_constituent) action_witness(constituent.constituent_LID, constituent);
		} else if (item.getItemId() == MENU_ID_MYSELF) {
			Toast.makeText(this, "Action 2 invoked", Toast.LENGTH_SHORT).show();
			action_setMyself(constituent.constituent_LID, constituent);
		} else if (item.getItemId() == MENU_ID_HERE) {
			action_moveHere(neighborhood.neighborhood_LID, neighborhood);
			Toast.makeText(this, "Action 3 invoked", Toast.LENGTH_SHORT).show();
		} else if (item.getItemId() == MENU_ID_NEIG) {
			Toast.makeText(this, "Action 4 invoked", Toast.LENGTH_SHORT).show();
			action_addNeighborhood(neighborhood.neighborhood_LID, neighborhood);
		} else if (item.getItemId() == MENU_ID_CONST) {
			Toast.makeText(this, "Action 5 invoked", Toast.LENGTH_SHORT).show();
			action_addConstituent(neighborhood.neighborhood_LID, neighborhood);
		}  else if (item.getItemId() == MENU_ID_BLOCKED) {
			Toast.makeText(this, "Action 6 invoked", Toast.LENGTH_SHORT).show();
			if (_neighborhood) action_blocked(neighborhood.neighborhood_LID, neighborhood);
			if (_constituent) action_blocked(constituent.constituent_LID, constituent);
		}  else if (item.getItemId() == MENU_ID_BROADCAST) {
			Toast.makeText(this, "Action 7 invoked", Toast.LENGTH_SHORT).show();
			if (_neighborhood) action_broadcast(neighborhood.neighborhood_LID, neighborhood);
			if (_constituent) action_broadcast(constituent.constituent_LID, constituent);
		}  else if (item.getItemId() == MENU_ID_HIDE) {
			Toast.makeText(this, "Action 8 invoked", Toast.LENGTH_SHORT).show();
			if (_neighborhood) action_hide(neighborhood.neighborhood_LID, neighborhood);
			if (_constituent) action_hide(constituent.constituent_LID, constituent);
		}  else if (item.getItemId() == MENU_ID_PSEUDONYM) {
			Toast.makeText(this, "Action 9 invoked", Toast.LENGTH_SHORT).show();
			if (_neighborhood) action_pseudonym(neighborhood.neighborhood_LID, neighborhood);
			if (_constituent) action_pseudonym(constituent.constituent_LID, constituent);
		} else {
			return false;
		}
		return true;
	}

	private void action_addConstituent(long neighborhood_LID,
			NeighborhoodNode neighborhood) {
		// TODO Auto-generated method stub
		
	}

	private void action_addNeighborhood(long neighborhood_LID,
			NeighborhoodNode neighborhood) {
		// TODO Auto-generated method stub
		
	}

	private void action_blocked(long constituent_LID,
			ConstituentNode constituent) {
		// TODO Auto-generated method stub
		
	}

	private void action_blocked(long neighborhood_LID,
			NeighborhoodNode neighborhood) {
		// TODO Auto-generated method stub
		
	}

	private void action_broadcast(long constituent_LID,
			ConstituentNode constituent) {
		// TODO Auto-generated method stub
		
	}

	private void action_broadcast(long neighborhood_LID,
			NeighborhoodNode neighborhood) {
		// TODO Auto-generated method stub
		
	}

	private void action_hide(long constituent_LID, ConstituentNode constituent) {
		// TODO Auto-generated method stub
		
	}

	private void action_hide(long neighborhood_LID,
			NeighborhoodNode neighborhood) {
		// TODO Auto-generated method stub
		
	}

	private void action_pseudonym(long constituent_LID,
			ConstituentNode constituent) {
		// TODO Auto-generated method stub
		
	}

	private void action_pseudonym(long neighborhood_LID,
			NeighborhoodNode neighborhood) {
		// TODO Auto-generated method stub
		
	}

	private void action_witness(long neighborhood_LID, NeighborhoodNode neighborhood) {
		// TODO Auto-generated method stub
		
	}

	private void action_witness(long neighborhood_LID, ConstituentNode constituent) {
		// TODO Auto-generated method stub
		
	}

	private void action_moveHere(long neighborhood_LID, NeighborhoodNode neighborhood) {
		// TODO Auto-generated method stub
		
	}

	private void action_setMyself(long constituent_LID, ConstituentNode constituent) {
		// TODO Auto-generated method stub
		
	}

	private boolean isParentNeighborhood(int position) {
		CTreeNode cr = this.crtLayer.crt_root.getBranch(position);
		return (cr instanceof NeighborhoodNode);
	}
	private boolean isParentConstituent(int position) {
		CTreeNode cr = this.crtLayer.crt_root.getBranch(position);
		return (cr instanceof ConstituentNode);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// return button on left-top corner
		if (item.getItemId() == android.R.id.home) {
			ConstituentFurtherLayer.this.popLayer();
			return true;
		}
		return false;
	}

	// method to add parent Items
	public void setGroupParents() {
		for (ArrayList<Object> _n : crtLayer.neighs_LIDs) {
			long n_LID = Util.lval(_n.get(0));
			if (n_LID <= 0)
				continue;
			D_Neighborhood n = D_Neighborhood.getNeighByLID(n_LID, true, false);
			if (n == null)
				continue;

            NeighborhoodNode nn = new NeighborhoodNode(n_LID, n
                    .getName_division() + ": " + n.getName());
            D_Witness w = D_Witness.getMyWitnessForNeighborhood(n);
            //D_Witness.getCountWitness();
            nn.my_witness = new A_Witness(w);
			crtLayer.crt_root.neighborhoods.add(nn);// .parentItems.add(n.getName_division()+": "+n.getName());
		}
		for (ArrayList<Object> _c : crtLayer.consts_LIDs) {
			long c_LID = Util.lval(_c.get(0));
			if (c_LID <= 0)
				continue;
			D_Constituent c = D_Constituent.getConstByLID(c_LID, true, false);
			if (c == null)
				continue;
			crtLayer.crt_root.constituents.add(new ConstituentNode(c_LID, c));
		}
		if (crtLayer.crt_root.getLeavesCount() == 0) {
			crtLayer.crt_root.header.add(net.ddp2p.common.util.Util.__("Nothing here yet!"));
		}
	}

	// method to set child data of each parent
	public void setChildData() {
		for (int n_idx = 0; n_idx < crtLayer.neighs_LIDs.size(); n_idx++) {
			long n_LID = Util.lval(crtLayer.neighs_LIDs.get(n_idx).get(0));

			/**
			 * Each neighborhood has a first empty child with buttons
			 */
			crtLayer.crt_root.neighborhoods.get(n_idx).header.add("");

			if (n_LID <= 0) {
				crtLayer.crt_root.neighborhoods.get(n_idx).header.add("N:"
						+ n_LID);
				continue;
			}
			D_Neighborhood n = D_Neighborhood.getNeighByLID(n_LID, true, false);
			if (n == null) {
				crtLayer.crt_root.neighborhoods.get(n_idx).header.add("N:"
						+ n_LID);
				continue;
			}

			ArrayList<ArrayList<Object>> crt_neigh_neighs = D_Neighborhood
					.getAllNeighborhoods(oLID, n.getLID(), false);
			ArrayList<ArrayList<Object>> crt_neigh_consts = D_Constituent
					.getAllConstituents(oLID, n.getLID(), false);

			for (ArrayList<Object> __n : crt_neigh_neighs) {
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
				crtLayer.crt_root.neighborhoods.get(n_idx).neighborhoods
						.add(new NeighborhoodNode(n__LID, n_.getName_division()
								+ ": " + n_.getName()));
			}

			crt_neigh_neighs.add(0, new ArrayList<Object>());
			// crtLayer.neighs_neighs_LIDs.add(0, new ArrayList<Object>());

			ArrayList<Object> _constits = new ArrayList<Object>();
			for (ArrayList<Object> _c : crt_neigh_consts) {
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
				crtLayer.crt_root.neighborhoods.get(n_idx).constituents
						.add(new ConstituentNode(c_LID, c));
				_constits.add(c0);
			}
		}
		for (int c_idx = 0; c_idx < crtLayer.consts_LIDs.size(); c_idx++) {
			ArrayList<Object> _c = crtLayer.consts_LIDs.get(c_idx);
			long c_LID = Util.lval(_c.get(0));
			if (c_LID <= 0) {
				continue;
			}
			D_Constituent c = D_Constituent.getConstByLID(c_LID, true, false);
			if (c == null) {
				continue;
			}
			String email = c.getEmail();
			if (email == null)
				email = "No email!";
			crtLayer.crt_root.constituents.get(c_idx).email = Util.__("Email:")
					+ " " + email;
		}
	}

	private class MyExpandableAdapter extends BaseExpandableListAdapter {

		private Activity activity;
		// private ArrayList<Object> childtems;
		private LayoutInflater inflater;
		// private ArrayList<String> parentItems;
		// private ArrayList<String> child;
		private NeighborhoodNode crt_root;

		// constructor
		/*
		 * public MyExpandableAdapter(ArrayList<String> parents,
		 * ArrayList<Object> childern) { this.parentItems = parents;
		 * this.childtems = childern; }
		 */
		public MyExpandableAdapter(NeighborhoodNode crt_node) {
			this.crt_root = crt_node;
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

			CTreeNode group_node;
			group_node = this.crt_root.getBranch(groupPosition); // (ArrayList<String>)
															// childtems.get(groupPosition);

			TextView textView = null;

			if (convertView == null) {
				convertView = inflater.inflate(
						R.layout.constituents_child_view, null);
			}

			/*
			 * boolean is_parent_constituent =
			 * this.crt_root.isConstituent(groupPosition);
			 * 
			 * boolean is_child_constituent = true; if (! is_parent_constituent)
			 * { if (groupPosition < crtLayer.neighs_neighs_number.size() &&
			 * childPosition < crtLayer.neighs_neighs_number.get(groupPosition))
			 * { is_child_constituent = false; } }
			 * 
			 * boolean isConstituent = is_parent_constituent ||
			 * is_child_constituent;
			 */

			// get the textView reference and set the value
			textView = (TextView) convertView.findViewById(R.id.textViewChild);
            textView.setTextColor(Color.BLUE);
			textView.setText(group_node.getDisplayText(childPosition));

            if (crt_root.isNeighborhood(groupPosition) && crt_root.isConstituent(groupPosition, childPosition)) {
                ConstituentNode constituentNode = crt_root.getConstituentNode(groupPosition, childPosition);

                D_Constituent _constituent_me = Identity.getCrtConstituent(ConstituentFurtherLayer.organization_LID);
                if (_constituent_me != null && _constituent_me.getLID() == constituentNode.constituent_LID) {
                    textView.setTextColor(ConstituentFurtherLayer.Color_Myself);
                } else {
                    //D_Constituent c = D_Constituent.getConstByLID(constituentNode.constituent_LID, true, true);
                    //if (c == null || c.isExternal() || c.getSK() == null) {
                    if (constituentNode != null && !constituentNode.external && constituentNode.hasSK) {
                        textView.setTextColor(ConstituentFurtherLayer.Color_Has_SK);
                    } else
                        textView.setTextColor(ConstituentFurtherLayer.Color_Others);
                }
            }

            Button addNeiborhood = (Button) convertView
					.findViewById(R.id.constituent_child_view_add_neiborhood);
			addNeiborhood.setFocusable(false);

			if (this.crt_root.showAddNeighborhoodButton(groupPosition,
					childPosition))
				addNeiborhood.setVisibility(Button.VISIBLE);
			else
				addNeiborhood.setVisibility(Button.GONE);

			addNeiborhood
					.setOnClickListener(new My_OnClickListener(
							new CtxConstituent(null, null, groupPosition,
									childPosition)) {

						@Override
						public void _onClick(View arg0) {
							CtxConstituent _ctx = (CtxConstituent) ctx;
							int n_idx = _ctx.groupPosition;
							if (n_idx >= crtLayer.neighs_LIDs.size())
								return;
							String parentLID = Util
									.getString(crtLayer.neighs_LIDs.get(n_idx)
											.get(0));
							// long parentLID =
							// Util.lval(neighs.get(n_idx).get(0));
							Intent intent = new Intent();
							intent.setClass(ConstituentFurtherLayer.this,
									NewNeighborhood.class);
							Bundle b = new Bundle();
							b.putInt(Orgs.O_ID, organization_position);
							b.putString(Orgs.O_GIDH, organization_GIDH);
							b.putString(Orgs.O_GID, organization_GID);
							b.putString(Orgs.O_LID, organization_LID);
							b.putString(Orgs.O_NAME, organization_name);
							b.putString(ConstituentFurtherLayer.N_PARENT_LID,
									parentLID);
							intent.putExtras(b);
							startActivity(intent);
						}
					});

			Button addConstituent = (Button) convertView
					.findViewById(R.id.constituent_child_view_add_constituent);
			addConstituent.setFocusable(false);
			if (this.crt_root.showAddConstituentButton(groupPosition,
					childPosition))
				addConstituent.setVisibility(Button.VISIBLE);
			else
				addConstituent.setVisibility(Button.GONE);
			// if (isConstituent || childPosition != 0)
			// addConstituent.setVisibility(Button.GONE);
			// else addConstituent.setVisibility(Button.VISIBLE);

			addConstituent
					.setOnClickListener(new My_OnClickListener(
							new CtxConstituent(null, null, groupPosition,
									childPosition)) {

						@Override
						public void _onClick(View arg0) {
							CtxConstituent _ctx = (CtxConstituent) ctx;
							int n_idx = _ctx.groupPosition;
							if (n_idx >= crtLayer.neighs_LIDs.size())
								return;
							String parentLID = Util
									.getString(crtLayer.neighs_LIDs.get(n_idx)
											.get(0));
							// long parentLID =
							// Util.lval(neighs.get(n_idx).get(0));
							Intent intent = new Intent();
							intent.setClass(ConstituentFurtherLayer.this,
									NewConstituent.class);
							Bundle b = new Bundle();
							b.putInt(Orgs.O_ID, organization_position);
							b.putString(Orgs.O_GIDH, organization_GIDH);
							b.putString(Orgs.O_GID, organization_GID);
							b.putString(Orgs.O_LID, organization_LID);
							b.putString(Orgs.O_NAME, organization_name);
							b.putString(ConstituentFurtherLayer.N_PARENT_LID,
									parentLID);
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

			if (this.crt_root.isConstituent(groupPosition, childPosition)) {
                D_Constituent c;
                //Log.d(TAG, "setting myself ");
                //CtxConstituent _ctx = (CtxConstituent) ctx;
                ConstituentNode constituentNode = crtLayer.crt_root
                        .getConstituentNode(groupPosition,
                                childPosition);
                if (constituentNode != null) {
                    c = D_Constituent.getConstByLID(
                            Util.getStringID(constituentNode.constituent_LID),
                            true, false);
                    if (!c.isExternal() && c.getSK() != null)
                        myself.setVisibility(Button.VISIBLE);
                    else
                        myself.setVisibility(Button.GONE);
                } else
                    myself.setVisibility(Button.GONE);
            } else
				myself.setVisibility(Button.GONE);

			myself.setOnClickListener(new My_OnClickListener(
					new CtxConstituent(null, null, groupPosition, childPosition)) {

				@Override
				public void _onClick(View arg0) {
					D_Constituent c;
					Log.d(TAG, "setting myself ");
					CtxConstituent _ctx = (CtxConstituent) ctx;
					ConstituentNode constituentNode = crtLayer.crt_root
							.getConstituentNode(_ctx.groupPosition,
									_ctx.childPosition);
					if (constituentNode == null) {
						Toast.makeText(getApplicationContext(), "This is not a Constituent!", Toast.LENGTH_SHORT).show(); return;}
					c = D_Constituent.getConstByLID(
							Util.getStringID(constituentNode.constituent_LID),
							true, false);
					/*
					 * int n_idx = _ctx.groupPosition; if (n_idx <
					 * crtLayer.neighs_LIDs.size()) { // Cannot be a
					 * neighborhood if (_ctx.childPosition <
					 * crtLayer.neighs_neighs_number.get(n_idx)) return; int
					 * c_idx = _ctx.childPosition -
					 * crtLayer.neighs_neighs_number.get(n_idx); c =
					 * D_Constituent
					 * .getConstByLID(Util.getString(crtLayer.neighs_consts_LIDs
					 * .get(n_idx).get(c_idx)), true, false); } else { int c_idx
					 * = n_idx - crtLayer.neighs_LIDs.size(); c =
					 * D_Constituent.getConstByLID
					 * (Util.getString(crtLayer.consts_LIDs.get(c_idx).get(0)),
					 * true, false); }
					 */
					// String parentLID =
					// Util.getString(neighs.get(n_idx).get(0));
					// //long parentLID = Util.lval(neighs.get(n_idx).get(0));
					// Log.d(TAG, "setting myself to: "+parentLID);
					//
					// D_Neighborhood neighborhood =
					// D_Neighborhood.getNeighByLID(parentLID, true, false);
					Log.d(TAG, "setting myself to: " + c);
					if (c == null) {
                        Toast.makeText(getApplicationContext(), "Constituent Not Found In Database!", Toast.LENGTH_SHORT).show(); return;
                    }
                    if (c.getSK() == null) {
                        Toast.makeText(getApplicationContext(), "No Keys for: \""+c.getNameOrMy()+"\"", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        net.ddp2p.common.config.Identity
                                .setCurrentConstituentForOrg(
                                        c.getLID(),
                                        oLID);
                        Toast.makeText(
                                ConstituentFurtherLayer.this,
                                "New Myself " + c.getNameOrMy() + "\"", Toast.LENGTH_SHORT)
                                .show();
                        Log.d(TAG, "setting const: "
                                + c);
                    } catch (P2PDDSQLException e1) {
                        e1.printStackTrace();
                        Toast.makeText(ConstituentFurtherLayer.this,
                                "Fail: " + e1.getLocalizedMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    return;
/*
                    //Toast.makeText(getApplicationContext(), "New Myself: \""+c.getNameOrMy()+"\"", Toast.LENGTH_SHORT).show();


					String myself_constituent_GID;
					String myself_constituent_GIDH;
					D_Constituent myself_constituent = null;
					long myself_constituent_LID = -1;

					try {
						myself_constituent_LID = Identity
								.getDefaultConstituentIDForOrg(oLID);
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
					// try {
					// Identity crt_identity = Identity.getCurrentIdentity();
					// if (crt_identity == null) {
					// Log.d(ConstituentFurtherLayer.TAG, "No identity");
					// } else
					// myself_constituent_LID =
					// config.Identity.getDefaultConstituentIDForOrg(oLID);
					// } catch (P2PDDSQLException e1) {
					// e1.printStackTrace();
					// }
					if (myself_constituent_LID > 0) {
						Log.d(TAG, "setting myself that exists");
						myself_constituent = D_Constituent.getConstByLID(
								myself_constituent_LID, true, true);
						Log.d(TAG, "setting myself old was: " + myself_constituent);
						myself_constituent_GID = myself_constituent.getGID();
						myself_constituent_GIDH = myself_constituent.getGIDH();

						if (myself_constituent.getSK() != null) {
							try {
								Identity crt_identity = Identity
										.getCurrentConstituentIdentity();
								if (crt_identity == null) {
									Log.d(ConstituentFurtherLayer.TAG,
											"No identity");
									Toast.makeText(
											ConstituentFurtherLayer.this,
											"No Identity!", Toast.LENGTH_SHORT)
											.show();
									return;
								} else {
									config.Identity
											.setCurrentConstituentForOrg(
													myself_constituent_LID,
													oLID);
									Toast.makeText(
											ConstituentFurtherLayer.this,
											"Set Myself!", Toast.LENGTH_SHORT)
											.show();
									Log.d(TAG, "setting const: "
											+ myself_constituent);
									return;
								}
							} catch (P2PDDSQLException e1) {
								e1.printStackTrace();
								Toast.makeText(ConstituentFurtherLayer.this,
										"Fail: " + e1.getLocalizedMessage(),
										Toast.LENGTH_SHORT).show();
								return;
							}
						} else {
							Toast.makeText(ConstituentFurtherLayer.this,
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
						Toast.makeText(ConstituentFurtherLayer.this,
								"Register First!", Toast.LENGTH_SHORT).show();
						Log.d(TAG, "no const: " + myself_constituent);
						return;
					}
					// Toast.makeText(Constituent.this, "Failure!",
					// Toast.LENGTH_SHORT).show();
*/
				}
			});

			Button move = (Button) convertView
					.findViewById(R.id.constituent_child_view_move_here);
			move.setFocusable(false);
			if (this.crt_root.showMoveHereButton(groupPosition, childPosition))
				move.setVisibility(Button.VISIBLE);
			else
				move.setVisibility(Button.GONE);

			/**
			 * Currently we only implement the moving of myself to the fist
			 * level of neighborhoods
			 */
			move.setOnClickListener(new My_OnClickListener(new CtxConstituent(
					null, null, groupPosition, childPosition)) {

				@Override
				public void _onClick(View arg0) {
					Log.d(TAG, "moving myself ");
					CtxConstituent _ctx = (CtxConstituent) ctx;
					int n_idx = _ctx.groupPosition;
					if (n_idx >= crtLayer.neighs_LIDs.size())
						return;
					String parentLID = Util.getString(crtLayer.neighs_LIDs.get(
							n_idx).get(0));
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
						Identity crt_identity = Identity
								.getCurrentConstituentIdentity();
						if (crt_identity == null) {
							Log.d(ConstituentFurtherLayer.TAG, "No identity");
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
						Toast.makeText(ConstituentFurtherLayer.this,
								"Register First!", Toast.LENGTH_SHORT).show();
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
					b.putString(Orgs.O_LID,
							ConstituentFurtherLayer.organization_LID);
					b.putString(Orgs.O_NAME,
							ConstituentFurtherLayer.organization_name);

					/*
					 * if (crtLayer.crt_root.isConstituent(_ctx.groupPosition))
					 * { ConstituentNode constituentNode =
					 * crtLayer.crt_root.getConstituentNode(_ctx.groupPosition,
					 * _ctx.childPosition); if (constituentNode == null) return;
					 * D_Constituent c =
					 * D_Constituent.getConstByLID(Util.getString
					 * (constituentNode.constituent_LID), true, false);
					 * b.putString(ConstituentFurtherLayer.C_TYPE,
					 * ConstituentFurtherLayer.C_TYPE_C);
					 * b.putString(ConstituentFurtherLayer.C_GIDH, c.getGIDH());
					 * b.putString(ConstituentFurtherLayer.C_GID, c.getGID());
					 * b.putString(ConstituentFurtherLayer.C_LID,
					 * c.getLIDstr());
					 * b.putString(ConstituentFurtherLayer.C_NAME,
					 * c.getNameOrMy()); } else { if (!
					 * crtLayer.crt_root.isNeighborhood(_ctx.groupPosition))
					 * return; NeighborhoodNode neighborhoodNode =
					 * crtLayer.crt_root
					 * .getNeighborhoodNode(_ctx.groupPosition);//,
					 * _ctx.childPosition); D_Neighborhood n =
					 * D_Neighborhood.getNeighByLID
					 * (Util.getString(neighborhoodNode.neighborhood_LID), true,
					 * false);
					 * 
					 * if (_ctx.childPosition == 0) {
					 * b.putString(ConstituentFurtherLayer.C_TYPE,
					 * ConstituentFurtherLayer.C_TYPE_N);
					 * b.putString(ConstituentFurtherLayer.N_GIDH, n.getGIDH());
					 * b.putString(ConstituentFurtherLayer.N_GID, n.getGID());
					 * b.putString(ConstituentFurtherLayer.N_LID,
					 * n.getLIDstr());
					 * b.putString(ConstituentFurtherLayer.N_NAME,
					 * n.getNameOrMy()); } else { // moved from here below } }
					 */
					/**
					 * Elements of the neighborhood. The remaining code should
					 * work without the above code
					 */
					ConstituentNode cn;
					if ((cn = crtLayer.crt_root.getConstituentNode(
							_ctx.groupPosition, _ctx.childPosition)) != null) {
						D_Constituent c = D_Constituent.getConstByLID(
								Util.getString(cn.constituent_LID), true, false);
						b.putString(ConstituentFurtherLayer.C_TYPE,
								ConstituentFurtherLayer.C_TYPE_C);
						b.putString(ConstituentFurtherLayer.C_GIDH, c.getGIDH());
						b.putString(ConstituentFurtherLayer.C_GID, c.getGID());
						b.putString(ConstituentFurtherLayer.C_LID,
								c.getLIDstr());
						b.putString(ConstituentFurtherLayer.C_NAME,
								c.getNameOrMy());
					} else {
						NeighborhoodNode nn;
						if ((nn = crtLayer.crt_root.getNeighborhoodNode(
								_ctx.groupPosition, _ctx.childPosition)) != null) {
							D_Neighborhood _n = D_Neighborhood.getNeighByLID(
									Util.getString(nn.neighborhood_LID), true,
									false);
							b.putString(ConstituentFurtherLayer.C_TYPE,
									ConstituentFurtherLayer.C_TYPE_N);
							b.putString(ConstituentFurtherLayer.N_GIDH,
									_n.getGIDH());
							b.putString(ConstituentFurtherLayer.N_GID,
									_n.getGID());
							b.putString(ConstituentFurtherLayer.N_PARENT_LID,
									_n.getLIDstr());
							b.putString(ConstituentFurtherLayer.N_NAME,
									_n.getNameOrMy());
						} else
							return;
					}

					i.putExtras(b);

					i.setClass(ConstituentFurtherLayer.this, Witness.class);
					startActivity(i);
				}
			});

			// set the ClickListener to handle the click event on child item
			/**
			 * This is executed when somebody clicks on the space around a
			 * neighborhood at the parent level.
			 */
			convertView
					.setOnClickListener(new My_OnClickListener(
							new CtxConstituent(null, null, groupPosition,
									childPosition)) {

						@Override
						public void _onClick(View view) {
							CtxConstituent _ctx = (CtxConstituent) ctx;
							// Toast.makeText(activity,
							// "clicked",//child.get(childPosition),
							// Toast.LENGTH_SHORT).show();

							ConstituentFurtherLayer.this
									.pushLayer(_ctx.groupPosition);

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

			ImageView parentImg = (ImageView) convertView
					.findViewById(R.id.constituents_parent_view_img);

            parentCheckedTextView.setTextColor(Color.WHITE);
			if (crt_root.isConstituent(groupPosition)) {
                // TODO if (cons)
                parentImg.setImageResource(R.drawable.constitutent_person_icon);

                ConstituentNode constituentNode = crt_root.getConstituentNode(groupPosition);

                D_Constituent _constituent_me = Identity.getCrtConstituent(ConstituentFurtherLayer.organization_LID);
                Log.d(TAG, "constituent color LID="+constituentNode.constituent_LID+" e="+constituentNode.external+" k="+constituentNode.hasSK);
                if (_constituent_me != null && _constituent_me.getLID() == constituentNode.constituent_LID) {
                    Log.d(TAG, "constituent me color LID="+constituentNode.constituent_LID+" e="+constituentNode.external+" k="+constituentNode.hasSK);
                    parentCheckedTextView.setTextColor(ConstituentFurtherLayer.Color_Myself);
                } else {
                    //D_Constituent c = D_Constituent.getConstByLID(constituentNode.constituent_LID, true, true);
                    //if (c == null || c.isExternal() || c.getSK() == null) {
                    if (constituentNode != null && !constituentNode.external && constituentNode.hasSK) {
                        parentCheckedTextView.setTextColor(ConstituentFurtherLayer.Color_Has_SK);
                    } else
                        parentCheckedTextView.setTextColor(ConstituentFurtherLayer.Color_Others);
                }
            }
			if (crt_root.isNeighborhood(groupPosition)) {
				parentImg
						.setImageResource(R.drawable.constituent_neighborhood_icon);
			}

			// if (!is_parent_constituent) witness.setVisibility(Button.GONE);
			// else witness.setVisibility(Button.VISIBLE);

			parentCheckedTextView.setText(this.crt_root
                    .getDisplayText(groupPosition));
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
			// if (childtems == null || childtems.size() <= groupPosition)
			// return 0;
			// return ((ArrayList<String>) childtems.get(groupPosition)).size();
			CTreeNode branch = this.crt_root.getBranch(groupPosition);
			if (branch == null)
				return 0;
			return branch.getLeavesCount();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getGroupCount() {
			return this.crt_root.getLeavesCount();
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
