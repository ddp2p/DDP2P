/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
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
 package net.ddp2p.widgets.constituent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.Language;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OID;
import net.ddp2p.common.data.D_OrgParam;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.population.ConstituentData;
import net.ddp2p.common.population.ConstituentsAddressNode;
import net.ddp2p.common.population.ConstituentsBranch;
import net.ddp2p.common.population.ConstituentsCensus;
import net.ddp2p.common.population.ConstituentsIDNode;
import net.ddp2p.common.population.ConstituentsInterfaceDone;
import net.ddp2p.common.population.ConstituentsInterfaceInput;
import net.ddp2p.common.population.ConstituentsNode;
import net.ddp2p.common.population.ConstituentsPropertyNode;
import net.ddp2p.common.population.Constituents_AddressAncestors;
import net.ddp2p.common.population.Constituents_LocationData;
import net.ddp2p.common.population.Constituents_NeighborhoodData;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
















import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.components.TreeModelSupport;

import net.ddp2p.widgets.org.OrgExtra;

//import com.sun.mirror.apt.Filer.Location;
import java.util.*;
import java.text.MessageFormat;

import static net.ddp2p.common.util.Util.__;

public class ConstituentsModel extends TreeModelSupport implements TreeModel, DBListener, ConstituentsInterfaceInput, ConstituentsInterfaceDone {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private ConstituentsAddressNode root;
	DBInterface db;
	boolean automatic_refresh = DD.DEFAULT_AUTO_CONSTITUENTS_REFRESH;
	private long[] fieldIDs;
	private long organizationID;
	private D_Organization crt_org;
	
	private long my_constituentID=-1;
	private String my_global_constituentID=null;
	private String subdivisions;
	private SK my_sk = null;
	boolean hasNeighborhoods;
	public ConstituentsCensus census=null;
	private long census_value;
	ArrayList<JTree> trees = new ArrayList<JTree>();
	private RefreshListener refreshListener;
	public void setTree(JTree tree) {
		if(trees.contains(tree)) return;
		trees.add(tree);
	}
	public D_Constituent getConstituentMyself(){
		return D_Constituent.getConstByLID(getConstituentIDMyself(), false, false);
	}
	public long getConstituentIDMyself(){
		return my_constituentID;
	}
	public String getConstituentGIDMyself(){
		return my_global_constituentID;
	}
	public SK getConstituentSKMyself(){
		return my_sk;
	}
	public long getOrganizationID(){
		return this.organizationID;
	}

	public String getConstituentMyselfName() {
		long lid = getConstituentIDMyself();
		D_Constituent cons = null;
		if (lid > 0) cons = D_Constituent.getConstByLID(lid, true, false);
		if (cons == null) return __("None");
		return cons.getSurname();
	}
//		ArrayList<ArrayList<Object>> c;
//		try {
//			c = Application.db.select("SELECT "+table.constituent.name+" FROM "+table.constituent.TNAME+
//					" WHERE "+table.constituent.constituent_ID+"=?;", new String[]{Util.getStringID(getConstituentIDMyself())});
//			if(c.size()==0) return __("None");
//			return "\""+c.get(0).get(0)+"\"";
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//		return __("Error");
	public String getConstituentMyselfNames() {
		D_Constituent cons = D_Constituent.getConstByLID(getConstituentIDMyself(), true, false);
		if (cons == null) return __("None");
		return cons.getNameFull();
//		ArrayList<ArrayList<Object>> c;
//		try {
//			c = Application.db.select("SELECT "+table.constituent.name+","+table.constituent.forename+" FROM "+table.constituent.TNAME+
//					" WHERE "+table.constituent.constituent_ID+"=?;", new String[]{Util.getStringID(getConstituentIDMyself())});
//			if(c.size()==0) return __("None");
//			return "\""+c.get(0).get(0)+", "+c.get(0).get(1)+"\"";
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//		return __("Error");
	}
	/**
	 * Set a current constituent as myself, for witnessing, etc.
	 * @param _constituent_ID
	 * @param global_constituent_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	boolean setConstituentIDMyself(long _constituent_ID, String global_constituent_ID) throws P2PDDSQLException{
		if ((_constituent_ID <= 0) && (global_constituent_ID == null)) {
			my_constituentID = -1;
			my_global_constituentID = null;
			my_sk = null;
			return true;
		}
		if (global_constituent_ID == null) {
			global_constituent_ID = D_Constituent.getGIDFromLID(_constituent_ID);
		}
		
		if (global_constituent_ID == null) {
			Util.printCallPathTop("lID="+_constituent_ID+" GID="+global_constituent_ID);
			Application_GUI.warning(__("This Constituent cannot be set to myself (no GID"), __("Cannot be Myself!"));
			return false;
		}
		
		
		if (_constituent_ID < 0)
			_constituent_ID = D_Constituent.getLIDFromGID(global_constituent_ID, this.organizationID);

		SK sk = DD.getConstituentSK(_constituent_ID);
		if (sk == null) {
			Application_GUI.warning(__("Constituent cannot be set to myself (no SK)"+_constituent_ID), __("Cannot be Myself!"));
			return false;
		}
		my_sk = sk;
		my_constituentID = _constituent_ID;
		my_global_constituentID = global_constituent_ID;
		if(DEBUG) System.err.println("ConstituentsModel:setConstituentIDMyself: my_ID="+_constituent_ID+" my_GID="+global_constituent_ID);
		return true;
	}
	
	public ConstituentsModel(DBInterface _db, long organizationID2, 
			long _constituentID, String _global_constituentID, D_Organization org, RefreshListener _refreshListener) {
		if(DEBUG) System.err.println("ConstituentsModel: start org="+organizationID2+
				" myconstID="+_constituentID+" gID="+_global_constituentID);
		db = _db;
		refreshListener = _refreshListener;
		if(db == null) {
			JOptionPane.showMessageDialog(null,__("No database in Model!"));
			return;
		}
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.constituent.TNAME, net.ddp2p.common.table.witness.TNAME, net.ddp2p.common.table.neighborhood.TNAME, net.ddp2p.common.table.field_value.TNAME)), null);
		try {
			init(organizationID2, _constituentID, _global_constituentID, org);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(Exception e){
			JOptionPane.showMessageDialog(null,e.toString());
			e.printStackTrace();
			return;
		}
	}
	
	public ConstituentsIDNode expandConstituentID(JTree tree, String constituentID, boolean census) {
		//boolean DEBUG=true;
		ConstituentsIDNode cin = null;
		if(DEBUG) System.err.println("ConstituentsModel:expandConstituentID start cID="+constituentID);
		if(constituentID == null) return null;
		D_Constituent c;

		c = D_Constituent.getConstByLID(constituentID, true, false); //new D_Constituent(constituentID, D_Constituent.EXPAND_ALL);
		if (c == null) {
			if (_DEBUG) System.err.println("ConstituentsModel:expandConstituentID null constituent = "+constituentID);
			return null;
		}
		c.loadNeighborhoods(D_Constituent.EXPAND_ALL);
		
		if ((c.getNeighborhood()==null) || (c.getNeighborhood().length == 0)) {
			if(DEBUG) System.err.println("ConstituentsModel:expandConstituentID root constituent="+c);
			return null;
		}
		ConstituentsAddressNode n = expandNeighborhoodID(tree, getRoot(), c.getNeighborhood());
		if(census)runCensus();
		if(n!=null){
			cin = n.getChildByConstituentID(new Integer(constituentID).longValue());
			/*
			if(cin!=null) {
				if(DEBUG) System.err.println("ConstituentsModel:fire chanfed="+cin);
				TreePath tp = new TreePath(cin.getPath());
				((ConstituentsModel)tree.getModel()).fireTreeNodesChanged(new TreeModelEvent(tree, tp.getParentPath(), 
        			new int[]{n.getIndexOfChild(cin)},
        			new Object[]{cin}));
			}
			*/
		}
		if(DEBUG) System.err.println("ConstituentsModel:expandConstituentID end");
		return cin;
	}
	public ConstituentsAddressNode expandNeighborhoodID(ConstituentsTree tree, String nID) throws P2PDDSQLException {
		D_Neighborhood neighborhood[] = D_Neighborhood.getNeighborhoodHierarchy(null, nID, D_Constituent.EXPAND_ALL, this.getOrganizationID());
		return expandNeighborhoodID(tree, getRoot(), neighborhood);
	}
	public static ConstituentsAddressNode expandNeighborhoodID(JTree tree, ConstituentsAddressNode crt, D_Neighborhood neighborhood[]) {
		//boolean DEBUG=true;
		if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID begin");
		ConstituentsAddressNode child=null;
		ArrayList<Object> _crt_path= new ArrayList<Object>();
		_crt_path.add(crt);
		for(int k=neighborhood.length-1; k>=0; k--) {
			if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID k="+k);

			String nGID = neighborhood[k].getGID();
			String nID = neighborhood[k].getLIDstr(); 
			if (nID == null) {
				nID = D_Neighborhood.getLIDstrFromGID(nGID, neighborhood[k].getOrgLID());
				if (nID == null) return null;
			}
			long neighborhoodID = Util.lval(nID, 0);
			if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID nID="+neighborhoodID+" n="+neighborhood[k]);
			child = crt.getChildByID(neighborhoodID);
			if(child == null) {
				if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID end of children, STOP");
				return null;
			}
			if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID end of child="+child);
			_crt_path.add(child);
			Object crtpath[] = _crt_path.toArray();
			if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID expand path="+Util.concat(crtpath, "#"));
			if(child.isColapsed()) {
				child.populate();
				tree.expandPath(new TreePath(crtpath));
			}
			crt = child;
		}
		if(DEBUG) System.err.println("ConstituentsModel:expandNeighborhoodID end");
		return child;
	}
	
	public void runCensus() {
		this.stopCensusRequest();
		this.startCensus();
	}
	
	public void doRefreshAll() throws P2PDDSQLException{
		if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: start");
		ConstituentsModel model = this;
		Object oldRoot = model.getRoot();
		model.init(model.getOrganizationID(), model.getConstituentIDMyself(), model.getConstituentGIDMyself(), model.getOrganization());
		if(trees.size()>1)if(_DEBUG) System.err.println("ConstituentsModel: doRefreshAll:Too many JTrees");
		Object model_root = model.getRoot();
		for(JTree tree: trees) {
			if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: tree="+tree);
			if(model_root!=null)model.fireTreeStructureChanged(new TreeModelEvent(tree,new Object[]{model_root}));
			model.refresh(new JTree[]{tree}, oldRoot);
			if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: refreshed");
		}
		if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: will census");
		model.runCensus();
		if(DEBUG) System.err.println("ConstituentsModel:deRefreshAll: done");
	}
	/**
	 * Will try to keep the same nodes expanded
	 * @param organizationID2
	 * @param _constituentID
	 * @param _global_constituentID
	 * @throws P2PDDSQLException
	 */
	public void refresh(JTree trees[], Object _old_root) throws P2PDDSQLException {
		
		//boolean DEBUG = true;
		if(this.refreshListener != null) this.refreshListener.disableRefresh();
		if(DEBUG) System.err.println("ConstituentsModel:refresh start");
		if((_old_root==null) || !(_old_root instanceof ConstituentsAddressNode)){
			if(DEBUG) System.err.println("ConstituentsModel:refresh  Abandoned no root: "+getRoot());
			return;
		}
		ConstituentsAddressNode old_root = (ConstituentsAddressNode)_old_root;
		//init(organizationID2, _constituentID, _global_constituentID);
		//if(old_root==null) return;
		for(JTree tree: trees)
			translate_expansion(tree, old_root, getRoot());
		if(DEBUG) System.err.println("ConstituentsModel:refresh Done");
	}
	private void translate_expansion(JTree tree, ConstituentsNode _old_root,
			ConstituentsNode _new_root) {
		//boolean DEBUG = true;
		if(DEBUG) System.err.println("ConstituentsModel:translate_expansion start \""+_old_root+"\" vs. \""+_new_root+"\"");
		if(!(_old_root instanceof ConstituentsBranch)){
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop old is leaf");
			return;
		}
		if(!(_new_root instanceof ConstituentsBranch)){
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop new is leaf");
			return;
		}
		ConstituentsBranch old_root = (ConstituentsBranch)_old_root;
		ConstituentsBranch new_root = (ConstituentsBranch)_new_root;
		
		if(old_root.isColapsed()){
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop old not expanded");
			return;
		}
		if(new_root.isColapsed()){
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion populating "+new_root);
			new_root.populate();
			tree.expandPath(new TreePath(new_root.getPath()));
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion populated "+new_root);
		}
		
		for(int k=0; k < old_root.getChildren().length; k++) {
			ConstituentsNode cb =	old_root.getChildren()[k];
			if(cb instanceof ConstituentsAddressNode) {
				if(!(new_root instanceof ConstituentsAddressNode)){
					if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop new root not address parent");
					continue;
				}
				
				ConstituentsAddressNode o_can = (ConstituentsAddressNode) cb;
				ConstituentsAddressNode n_can = (ConstituentsAddressNode) new_root;
				long neighborhoodID = o_can.getNeighborhoodData().neighborhoodID;
				ConstituentsNode nc = n_can.getChildByID(neighborhoodID);
				if((nc!=null)&&(cb!=null))translate_expansion(tree, cb,nc);
			}
			if(cb instanceof ConstituentsIDNode) {
				if(!(new_root instanceof ConstituentsAddressNode)){
					if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop new root not constituent parent (address)");
					continue;
				}
				
				ConstituentsIDNode o_can = (ConstituentsIDNode) cb;
				ConstituentsAddressNode n_can = (ConstituentsAddressNode) new_root;
				long constituentID = o_can.get_constituentID();
				ConstituentsNode nc = n_can.getChildByConstituentID(constituentID);
				if((nc!=null)&&(cb!=null))translate_expansion(tree, cb,nc);
			}
				
			if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop round for "+k);
		}
		if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop");		
	}
	public void init(long _organizationID, 
			long _constituentID, String _global_constituentID, D_Organization org) throws P2PDDSQLException {
		if(DEBUG) System.err.println("ConstituentsModel:init start org="+_organizationID+
				" myconstID="+_constituentID+" gID="+_global_constituentID);
		
		
		setRoot(null);
		setFieldIDs(null);
		setSubDivisions(null);
		organizationID = -1;
		hasNeighborhoods = false;
		crt_org = null;
		
		/*
		my_global_constituentID=null;
		my_constituentID=-1;
		my_sk = null;
		 */
		this.setConstituentIDMyself(-1, null);
		
		// Util.printCallPath("Create const");
		ArrayList<ArrayList<Object>> fields_neighborhood, subneighborhoods;
		//, neighborhood_branch_objects;
		organizationID = _organizationID;
		if (organizationID <= 0) return;
		crt_org = org;
		if (crt_org == null)
			try {
				crt_org = D_Organization.getOrgByLID_NoKeep(organizationID, true);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
	
		// constituentID = constituentID2;
		// global_constituentID = _global_constituentID;
		
		setConstituentIDMyself(_constituentID, _global_constituentID);
		ArrayList<Object> subdivision_fields = D_Organization.getDefaultRootSubdivisions(organizationID);
		setSubDivisions(Util.getString(subdivision_fields.get(0)));
		setFieldIDs((long[]) subdivision_fields.get(1));
		if (getFieldIDs().length > 0) hasNeighborhoods = true;
		
		setRoot(new ConstituentsAddressNode(this,null,null,"",null,null,getFieldIDs(),0,-2,null));
		
		
			//String sql = "select value, COUNT(*) from field_value where field_extra_ID = ? GROUP BY value;";
		if ( getFieldIDs().length > 0 ) {
			subneighborhoods = D_Constituent.getRootConstValues(getFieldIDs()[0], organizationID);
//				String constituents_by_values_sql =
//						"SELECT "+table.field_value.value+
//							", fv."+table.field_value.field_extra_ID+", COUNT(*), fe."+table.field_extra.tip+", fe."+table.field_extra.partNeigh+", fv."+table.field_value.fieldID_above+", fv."+table.field_value.field_default_next+", fv."+table.field_value.neighborhood_ID +
//							" FROM "+table.field_value.TNAME+" AS fv " +
//							// " JOIN field_extra ON fv.fieldID = field_extra.field_extra_ID " +
//			    			" JOIN "+table.constituent.TNAME+" AS c ON c."+table.constituent.constituent_ID+" = fv."+table.field_value.constituent_ID +
//			    			" JOIN "+table.field_extra.TNAME+" AS fe ON fe."+table.field_extra.field_extra_ID+" = fv."+table.field_value.field_extra_ID+
//							" WHERE c."+table.constituent.organization_ID+"=? AND " +
//							" (fv."+table.field_value.field_extra_ID+" = ?) OR ("+table.field_value.fieldID_above+" ISNULL AND "+table.field_extra.partNeigh+" > 0) " +
//							" GROUP BY "+table.field_value.value+" ORDER BY "+table.field_value.value+" DESC;";
//				subneighborhoods = db.select( constituents_by_values_sql,
//		    		new String[]{""+organizationID,
//		    		""+fieldIDs[0]}, DEBUG);
		}
		else subneighborhoods = new ArrayList<ArrayList<Object>>();
		
//		String neighborhoods_sql = 
//		    	"SELECT n."+table.neighborhood.name+", n."+table.neighborhood.neighborhood_ID+
//	    		" FROM "+table.neighborhood.TNAME + " AS n "+
//	    		" LEFT JOIN "+table.neighborhood.TNAME + " AS p ON(n."+table.neighborhood.parent_nID+"=p."+table.neighborhood.neighborhood_ID+") "+
//		    		" WHERE n."+table.neighborhood.organization_ID+" = ? AND ( n."+table.neighborhood.parent_nID+" ISNULL OR p."+table.neighborhood.neighborhood_ID+" ISNULL ) " +
////		    				" GROUP BY "+table.neighborhood.name+
//		    				" ORDER BY n."+table.neighborhood.name+" DESC;";
//		neighborhood_branch_objects = db.select(neighborhoods_sql, new String[]{""+organizationID}, DEBUG);

		ArrayList<Long> neighborhood_branch_obj = D_Neighborhood.getNeighborhoodRootsLIDs(organizationID);
		
		int n = 0;
		if(DEBUG) System.err.println("ConstituentsModel: Sub-neighborhoods (branches) Records= "+subneighborhoods.size());
		for(int i=0; i<subneighborhoods.size(); i++) {
		    String count, fieldID;
		    Object obj;
			String value=Util.sval(subneighborhoods.get(i).get(0),null);
		    if(value != null) {
		    	for (; n < neighborhood_branch_obj.size(); n ++) {
		    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(neighborhood_branch_obj.get(n), true, false);
		    		String n_name = dn.getName(); // Util.sval(neighborhood_branch_objects.get(n).get(0), "");
		    		int cmp = value.compareToIgnoreCase(n_name);
		    		if (cmp > 0) break; 
		    		if (cmp < 0) {
		    			long nID = neighborhood_branch_obj.get(n); // Util.lval(neighborhood_branch_objects.get(n).get(1), -1);
		    			//NeighborhoodData nd=new NeighborhoodData(n_name, -1, organizationID);
		    			Constituents_NeighborhoodData nd=new Constituents_NeighborhoodData(nID, -1, organizationID);
		    			nd.neighborhoodID = nID;
		    			getRoot().addChild(new ConstituentsAddressNode(this, getRoot(), nd, new Constituents_AddressAncestors[0]),0);
		    		}
		    	}
		    }
		    obj = subneighborhoods.get(i).get(1);
		    if(obj!=null) fieldID = obj.toString(); else fieldID = "-1";
		    obj = subneighborhoods.get(i).get(2);
		    if(obj!=null) count = obj.toString(); else count = null;
		    if(DEBUG) System.err.println(i+" Got: v="+value+" c="+count+" fID="+fieldID);
		    Constituents_LocationData data=new Constituents_LocationData();
		    data.value = value;
		    data.fieldID = Long.parseLong(fieldID);
		    data.inhabitants = Integer.parseInt(count);
		    data.tip = (String)subneighborhoods.get(i).get(3);
		    data.partNeigh = Util.ival(subneighborhoods.get(i).get(4),0);
		    data.fieldID_above = Util.lval(subneighborhoods.get(i).get(5),-1);
		    data.setFieldID_default_next(Util.lval(subneighborhoods.get(i).get(6),-1));
		    data.neighborhood = Util.ival(subneighborhoods.get(i).get(4),0);
		    getRoot().addChild(
		    		new ConstituentsAddressNode(this, getRoot(),
		    				data,
		    				"", null,
		    				new Constituents_AddressAncestors[0],
		    				getFieldIDs(),0, -1,null), 
		    			0);
		}
	    for (; n < neighborhood_branch_obj.size(); n++) {
    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(neighborhood_branch_obj.get(n), true, false);
    		String n_name = dn.getName(); // Util.sval(neighborhood_branch_objects.get(n).get(0), "");
	    	long nID = neighborhood_branch_obj.get(n); // Util.lval(neighborhood_branch_objects.get(n).get(1), -1);
	    	Constituents_NeighborhoodData nd=new Constituents_NeighborhoodData(nID, -1, organizationID);
	    	//NeighborhoodData nd=new NeighborhoodData(n_name, -1, organizationID);
	    	nd.neighborhoodID = nID;
	    	getRoot().addChild(new ConstituentsAddressNode(this, getRoot(), nd, new Constituents_AddressAncestors[0]), 0);
	    }
	    if (getFieldIDs().length == 0) getRoot().populateIDs();
	    else{
	    	if(DD.CONSTITUENTS_ORPHANS_SHOWN_IN_ROOT) populateOrphans();
	    }
	    //stopCensusRequest();
	    //startCensus();
	    runCensus(); // this may be too expensive
	}
	public void populateOrphans() {
		ArrayList<Long> orphans = D_Constituent.getOrphans(this.organizationID);
		//if (DEBUG) System.err.print("ConstituentsModel: populateOrphans Records="+identities.size());
		for (int i = 0; i < orphans.size(); i ++ ) {
			D_Constituent c = D_Constituent.getConstByLID(orphans.get(i), true, false);
			//ArrayList<Object> identities_i = identities.get(i);
			if (c == null) { //(identities_i.size() < 6) {
				//if(_DEBUG) System.err.println("ConstituentsModel: populateOrphans selected size="+identities_i.size());
				//if(_DEBUG) System.err.println("ConstituentsModel: populateOrphans selected sql="+sql);
				if(_DEBUG) Util.printCallPath("Wrong size!");
				return;
			}
			String name, forename, slogan, email;
			if (DEBUG) System.err.println("ConstituentsModel: populateOrphans got const="+c.getNameOrMy());
			name = Util.getString(c.getSurname(),__("Unknown Yet"));
			forename = c.getForename(); //Util.getString(identities_i.get(1));
			//constituentID = c.getConstituentIDstr(); //""+Util.lval(identities_i.get(2), -1);
			boolean external = c.isExternal(); //"1".equals(Util.getString(identities_i.get(3)));
			//boolean external = Util.ival(identities.get(i).get(3),-1);
			long submitterID = c.getSubmitterLID(); //Util.lval(identities_i.get(5),-1);
			ConstituentData data = new ConstituentData();
			data.constituent = c;
			data.setC_GID(c.getGID()); ////Util.sval(identities_i.get(4),null);
			data.setC_LID(c.getLID()); //Integer.parseInt(constituentID);
			data.given_name = forename;
			data.surname = name;
			//data.inserted_by_me=(model.constituentID == external);
			data.inserted_by_me=((getConstituentIDMyself() == submitterID)&&(getConstituentIDMyself()>=0));
			data.external = external;
			data.blocked = c.blocked;
			data.broadcast = c.broadcasted;
			slogan = c.getSlogan(); //Util.getString(identities_i.get(6));
			email = c.getEmail(); //Util.getString(identities_i.get(7));
			data.setSlogan(slogan);
			data.email = email;
			String submitter_ID = c.getSubmitterLIDstr(); // Util.getString(identities_i.get(5));
			data.submitter_ID = submitter_ID;
			if (DEBUG) System.err.print("ConstituentsModel: populateOrphans child");
			getRoot().populateChild(new ConstituentsIDNode(this, getRoot(), data,"",null, getRoot().getNextAncestors()),0);
		}
		// if(identities.size()!=root.nchildren)
		getRoot().setNChildren(orphans.size()+getRoot().getNchildren());
		if (DEBUG) System.err.print("ConstituentsModel: populateOrphans fire");
	}
	public Object	getChild(Object parent, int index) {	
		if (! (parent instanceof ConstituentsBranch)) return -1;
		ConstituentsBranch cbParent = (ConstituentsBranch)parent;
		return cbParent.getChild(index);
	}
	public int	getChildCount(Object parent) {	
		if (! (parent instanceof ConstituentsBranch)) return -1;
		ConstituentsBranch cbParent = (ConstituentsBranch)parent;
		return cbParent.getChildCount();
	}
	public int	getIndexOfChild(Object parent, Object child) {	
		if (! (parent instanceof ConstituentsBranch)) return -1;
		ConstituentsBranch cbParent = (ConstituentsBranch)parent;
		return cbParent.getIndexOfChild(child);
	}
    public ConstituentsAddressNode	getRoot() {
    	return root;
    }
    public boolean	isLeaf(Object node) {
    	if (node instanceof ConstituentsPropertyNode) return true;
    	if (node instanceof ConstituentsBranch)
    	    return (((ConstituentsBranch)node).getNchildren()==0);
    	return false;
    }
    /*
    public byte[] signNeighborhood(long nID) {
		if(DEBUG) System.err.println("ConstituentsModel:signNeighborhood: start "+nID);
    	byte[] signature=null;
    	String sql = "SELECT "+Util.setDatabaseAlias(table.neighborhood.fields_neighborhoods,"n")+
    	",p."+table.neighborhood.global_neighborhood_ID+
       	",c."+table.constituent.global_constituent_ID+
       	",o."+table.organization.global_organization_ID+
    	" FROM "+table.neighborhood.TNAME+" AS n "+
    	" LEFT JOIN "+table.neighborhood.TNAME+" AS p ON(p."+table.neighborhood.neighborhood_ID+"=n"+table.neighborhood.parent_nID+")"+
    	" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=n."+table.neighborhood.submitter_ID+")"+
    	" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.neighborhood.organization_ID+")"+
    	" WHERE "+table.neighborhood.neighborhood_ID+"=?;";
    	
    	ArrayList<ArrayList<Object>> n;
		try {
			n = Application.db.select(sql, new String[]{nID+""}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
    	WB_Neighborhood sn  = new WB_Neighborhood();
    	sn.creation_date = Util.getCalendar(Util.getString(n.get(0).get(table.neighborhood.IDX_CREATION_DATE)));
    	sn.name = Util.getString(n.get(0).get(table.neighborhood.IDX_NAME));
    	sn.name_division = Util.getString(n.get(0).get(table.neighborhood.IDX_NAME_DIVISION));
    	String ns = Util.getString(n.get(0).get(table.neighborhood.IDX_NAMES_DUBDIVISIONS));
    	if(ns!=null)sn.names_subdivisions = WB_Neighborhood.splitSubDivisions(ns);
    	sn.name_lang = Util.getString(n.get(0).get(table.neighborhood.IDX_NAME_LANG));
    	sn.picture = Util.byteSignatureFromString(Util.getString(n.get(0).get(table.neighborhood.IDX_PICTURE)));
    	sn.description = Util.getString(n.get(0).get(table.neighborhood.IDX_ADDRESS));
    	sn.parent_global_ID = Util.getString(n.get(0).get(table.neighborhood.IDX_FIELDs+0));
    	sn.submitter_global_ID = Util.getString(n.get(0).get(table.neighborhood.IDX_FIELDs+1));
    	ciphersuits.SK sk = ciphersuits.Cipher.getSK(sn.submitter_global_ID);
    	String orgGID = Util.getString(n.get(0).get(table.neighborhood.IDX_FIELDs+2));
    	String gID = sn.make_ID(orgGID);
    	signature = sn.sign(sk, orgGID);
    	String _signature = Util.stringSignatureFromByte(signature);
    	try {
			Application.db.updateNoSync(table.neighborhood.TNAME, new String[]{table.neighborhood.signature, table.neighborhood.global_neighborhood_ID}, new String[]{table.neighborhood.neighborhood_ID},
					new String[]{_signature, gID, nID+""}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
    	return signature;
    }
    */
    public void	valueForPathChanged(TreePath path, Object newValue) {
    	boolean HARD_SAVE = false;
    	if(DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: "+path+" = "+newValue);
    	if(newValue == null) {
    		return;
    	}
    	Calendar creation_date = Util.CalendargetInstance();
    	String s_creation_date = Encoder.getGeneralizedTime(creation_date);
    	Object node=path.getLastPathComponent();
    	if(!(node instanceof ConstituentsAddressNode)) return;
    	ConstituentsAddressNode neigh = (ConstituentsAddressNode)node;
    	Constituents_NeighborhoodData nd = (Constituents_NeighborhoodData)newValue; // the new data to be saved
    	if (DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: old edited neigh_ID="+nd.neighborhoodID);
     	try {
    		Constituents_NeighborhoodData ndo = neigh.getNeighborhoodData(); // the edited data node
        	if (DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: old neigh_ID="+ndo.neighborhoodID);
    		if (ndo.global_nID == null) HARD_SAVE = true;
    		else{
    			Application_GUI.warning(__("Cannot change data of signed neighborhood! Create a new one"),__("Cannot change!"));
    			return;
    		}
    		ndo.name = nd.name;
    		neigh.getLocation().value = nd.name;
    		ndo.name_lang = nd.name_lang;
    		ndo.name_division = nd.name_division;
    		ndo.name_division_lang = nd.name_division_lang;
    		ndo.names_subdivisions = nd.names_subdivisions;
    		ndo.name_subdivisions_lang = nd.name_subdivisions_lang;
     		if (HARD_SAVE) { // hard save should not update but rather should insert new neighborhoods, now possible by additions in the parent or root
       			String submitter_ID = Util.getStringID(this.getConstituentIDMyself());
       			String submitter_GID = (this.getConstituentGIDMyself());
    			String org_local_ID = Util.getStringID(this.organizationID);
    			String arrival_time = Util.getGeneralizedTime();
    			SK sk = Util.getStoredSK(this.getConstituentGIDMyself());
    			String orgGID = D_Organization.getGIDbyLID(this.organizationID);
    			
     			D_Neighborhood d_neighborhood = //D_Neighborhood.getNeighborhood(Util.getStringID(ndo.neighborhoodID), null);
     					D_Neighborhood.getNeighByLID(ndo.neighborhoodID, true, true);
     			if (DEBUG) System.out.println("Modifying neigh: "+d_neighborhood);
     			if (d_neighborhood.getGID() != null) {
     				d_neighborhood.releaseReference();
    				Application_GUI.warning(__("Signed Neighborhood!"), __("Not editable"));
    				return;
     			}
     			if ( ! Util.equalStrings_null_or_not(d_neighborhood.getSubmitterLIDstr(),submitter_ID) ) {
     				Application_GUI.warning(__("Submitter differs. Changed to current!"), __("Submitter conflict"));
     				d_neighborhood.setSubmitterLIDstr(submitter_ID);
     				d_neighborhood.setSubmitter_GID(submitter_GID);
     			}
     			
     			d_neighborhood.setNames_subdivisions(D_Neighborhood.splitSubDivisions(nd.names_subdivisions));
     			d_neighborhood.setName(nd.name);
     			d_neighborhood.setName_lang(nd.name_lang.toString());//.lang;// added flavor
     			d_neighborhood.setName_division(nd.name_division);
      			d_neighborhood.setCreationDateStr(arrival_time);
     			//... could update other fields
     			/*
     			db.update(table.neighborhood.TNAME,
    			new String[]{table.neighborhood.name,table.neighborhood.name_lang,table.neighborhood.name_charset,
    				table.neighborhood.name_division,table.neighborhood.name_division_lang,table.neighborhood.name_division_charset,
    				table.neighborhood.names_subdivisions,table.neighborhood.name_subdivisions_lang,table.neighborhood.name_subdivisions_charset,
    				table.neighborhood.creation_date},
    			new String[]{table.neighborhood.neighborhood_ID},
    			new String[]{nd.name,nd.name_lang.lang,nd.name_lang.flavor,
    				nd.name_division,nd.name_division_lang.lang,nd.name_division_lang.flavor,
    				nd.names_subdivisions,nd.name_subdivisions_lang.lang,nd.name_subdivisions_lang.flavor,
    				s_creation_date,
    				""+neigh.n_data.neighborhoodID},
    				DEBUG);
    				*/
    			////this.signNeighborhood(neigh.n_data.neighborhoodID);
    			//WB_Neighborhood.readSignStore(neigh.n_data.neighborhoodID, sk, orgGID, submitter_ID, org_local_ID, arrival_time);
      			d_neighborhood.setOrgIDs(orgGID, this.organizationID);
      			d_neighborhood.setGID(d_neighborhood.make_ID());
     			d_neighborhood.sign(sk);
     			
     			d_neighborhood.storeRequest();
     			d_neighborhood.releaseReference();
     			//d_neighborhood.storeVerified(submitter_ID, orgGID, org_local_ID, arrival_time, null, null);
     			
     			ndo.global_nID = d_neighborhood.getGID();
     			ndo.signature = d_neighborhood.getSignature();
     			ndo.neighborhoodID = d_neighborhood.getLID_force();
     			ndo.submitterID = d_neighborhood.getSubmitterLID();
    		}
    		int idx = neigh.getParent().getIndexOfChild(neigh);
    		this.fireTreeNodesChanged(new TreeModelEvent(this,path.getParentPath(),new int[]{idx},new Object[]{neigh}));
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	if(DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: exit");
    }
	public boolean setCurrentConstituent(long _constituentID, ConstituentsTree tree) {
    	if (DEBUG) System.err.println("ConstitentsModel:setCurrentConstituent: set "+_constituentID);
		try {
			/*
			SK sk = DD.getConstituentSK(_constituentID);
			if(sk==null){
				Application.warning(_("No keys known for this constituent!"), _("No keys!"));
				return;
			}
			*/
			//this.constituentID = _constituentID;
			if ( ! this.setConstituentIDMyself(_constituentID, null) ) {
		    	if (_DEBUG) System.err.println("ConstitentsModel:setCurrentConstituent: myself failed ");
				return false;
			}
			Identity.setCurrentConstituentForOrg(_constituentID, this.organizationID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		tree.preparePopup();
    	if (DEBUG) System.err.println("ConstitentsModel:setCurrentConstituent: Done");
    	return true;
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if (this.automatic_refresh) {
			//JTree tree = Application.constituents.tree;
			try {
				//this.refresh(trees.toArray(new JTree[0]), root);
				this.doRefreshAll();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;
		}
		if(refreshListener != null) this.refreshListener.enableRefresh();
		else  System.err.println("ConstituentsModel:update: No refresh listener!");
		if(DEBUG)System.err.println("ConstituentsModel:update: Need to update Constituents!");
	}
	/**
	 * This removed model from census thread, and it will no longer fire events
	 * It will also request it to eventually stop.
	 */
	public synchronized void stopCensusRequest(){
		if(DEBUG) System.err.println("ConstituentsModel:stopCensusRequest: start");
	    if(census!=null){
	    	census.giveUp();
			if(DEBUG) System.err.println("ConstituentsModel:stopCensusRequest: gaveUP");
	    }else{
			if(DEBUG) System.err.println("ConstituentsModel:stopCensusRequest: no census running");	    	
	    }
		if(DEBUG) System.err.println("ConstituentsModel:stopCensusRequest: stop");
	}
	public synchronized void startCensus(){
		if(DEBUG) System.err.println("ConstituentsModel:startCensus: start");
	    census = new ConstituentsCensus(this, this, getRoot());
	    census_value = 0;
	    census.start();
		if(DEBUG) System.err.println("ConstituentsModel:startCensus: done");
	}
	public void updateCensus(Object source,
			Object[] path2parent, int idx) {
		this.fireTreeNodesChanged(new TreeModelEvent(this, path2parent, 
				new int[]{idx},new Object[]{source}));
	}
	public void updateCensus(Object source, Object[] path) {
		try{
			fireTreeNodesChanged(new TreeModelEvent(source, path));
		}catch(Exception e){
			System.err.println("ConstituentsCensus: announce: "+e.getLocalizedMessage());
			System.err.println("ConstituentsCensus: announce: path="+Util.concat(path, " ; "));
		}
	}
	public void updateCensusStructure(Object source, Object[] path) {
		fireTreeStructureChanged(new TreeModelEvent(source,path));
	}
	public void updateCensusInserted(
			Object source, Object[] path2parent,
			int[] idx, Object[] children) {
		fireTreeNodesInserted(new TreeModelEvent(this, path2parent,
				idx, children));
	}
	/**
	 * Clean up if still relevant
	 * @param constituentsCensus
	 * @param result
	 */
	public synchronized void censusDone(ConstituentsCensus constituentsCensus, long result) {
		if(DEBUG) System.err.println("ConstituentsModel:censusDone: Got="+result);
		if(census!=constituentsCensus) {
			if(DEBUG) System.err.println("ConstituentsModel:censusDone: quit as irrelevant");
			return;
		}
		census = null;
		census_value = result;
		if(DEBUG) System.err.println("ConstituentsModel:censusDone: Done!");
	}
	public synchronized void runCensus(TreePath path) {
		if(DEBUG) System.err.println("ConstituentsModel:runCensus: Got="+path);
		if(census!=null){
			if(DEBUG) System.err.println("ConstituentsModel:runCensus: interrupting");
			return;
		}
		//stopCensusRequest();
		Object expanded = path.getLastPathComponent();
		if(!(expanded instanceof ConstituentsAddressNode)) {
			if(DEBUG) System.err.println("ConstituentsModel:runCensus: not address");
			return;
		}
		ConstituentsAddressNode can = ((ConstituentsAddressNode)expanded);
		if(can==null) return;
		census = new ConstituentsCensus(this, this, (ConstituentsAddressNode)can.getParent());
		census.start();
		if(DEBUG) System.err.println("ConstituentsModel:runCensus: done");
	}
	public D_Organization getOrganization() {
		return crt_org;
	}
	public String getOrgGID() {
		if(crt_org==null)
			try {
				crt_org = D_Organization.getOrgByLID_NoKeep(this.getOrganizationID(), true);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		return crt_org.getGID();
	}
	public void enableRefresh() {
		this.refreshListener.enableRefresh();
	}
	public String getSubDivisions() {
		return subdivisions;
	}
	public void setSubDivisions(String subdivisions) {
		this.subdivisions = subdivisions;
	}
	public long[] getFieldIDs() {
		return fieldIDs;
	}
	public void setFieldIDs(long[] fieldIDs) {
		this.fieldIDs = fieldIDs;
	}
	public void setRoot(ConstituentsAddressNode root) {
		this.root = root;
	}
}
