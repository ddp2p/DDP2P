/*   Copyright (C) 2015 Marius C. Silaghi
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
package net.ddp2p.common.population;
import static net.ddp2p.common.util.Util.__;
import java.util.ArrayList;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.DDTranslation;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class ConstituentsAddressNode extends ConstituentsBranch {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private Constituents_LocationData location;
	private long fieldIDs[];
	private int level;
	int next_level;
	String sql;
	String sql_params[] = new String[0];
	private Constituents_AddressAncestors[] next_ancestors;
	private Constituents_NeighborhoodData n_data;
	public ConstituentsAddressNode (ConstituentsInterfaceInput _model, ConstituentsBranch _parent, 
			Constituents_NeighborhoodData nd, Constituents_AddressAncestors[] _ancestors) {
		super(_model, _parent, _ancestors, 1);
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: start");
		setFieldIDs(_model.getFieldIDs());
		setLevel(_ancestors.length);
		next_level = getLevel()+1;
		sql = "";
		sql_params = null;
		setLocation(new Constituents_LocationData());
		{
			getLocation().value = nd.name;
			getLocation().inhabitants = 0;
			getLocation().organizationID = nd.organizationID;
			getLocation().setLabel(nd.name_division);
			if(getLevel() < getFieldIDs().length) getLocation().fieldID=getFieldIDs()[getLevel()];
			getLocation().field_valuesID = -1;
			getLocation().partNeigh = -1;
			if((getLevel()>0)&&(getLevel()<getFieldIDs().length+1))
				getLocation().fieldID_above = getFieldIDs()[getLevel()-1];
			else getLocation().fieldID_above = 0;
			if((getLevel()+1>=0)&&(getLevel()+1<getFieldIDs().length))
				getLocation().setFieldID_default_next(getFieldIDs()[getLevel()+1]);
			else getLocation().setFieldID_default_next(0);
			String tr = DDTranslation.translate(nd.name_division,nd.name_division_lang);
			getLocation().tip = ((tr==null)?"":tr)
					+" sd="+
					((nd.names_subdivisions==null)?"":nd.names_subdivisions)+
					" ("+nd.name_lang+")";
			getLocation().neighborhood = nd.neighborhoodID;
		}
		setNeighborhoodData(nd);
		{
			if(getLevel()<getFieldIDs().length) {
				setNextAncestors(new Constituents_AddressAncestors[ancestors.length+1]);
				for(int k=0; k<ancestors.length; k++) getNextAncestors()[k] = ancestors[k];
				getNextAncestors()[ancestors.length] = new Constituents_AddressAncestors(getFieldIDs()[getLevel()],nd.name);
			}else{
				setNextAncestors(new Constituents_AddressAncestors[ancestors.length+1]);
				for(int k=0; k<ancestors.length; k++) getNextAncestors()[k] = ancestors[k];				
				getNextAncestors()[ancestors.length] = new Constituents_AddressAncestors(0,nd.name);
			}
		}
		try {
			populateAddress(true);
		}catch(Exception e){
			e.printStackTrace();
			return;			
		}
		if(DEBUG) System.err.println("Creating ConstituentsAddress: "+getLocation());
	}
	public ConstituentsAddressNode (ConstituentsInterfaceInput _model, ConstituentsBranch _parent, Constituents_LocationData _data,
			String _sql_children, String[] _sql_params,
			Constituents_AddressAncestors[] _ancestors, long[] _fieldIDs, int _level, long parent_nID, Constituents_NeighborhoodData _n_data) {
		super(_model, _parent, _ancestors, 1);
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: start2");
		setFieldIDs(_fieldIDs);
		setLevel(_level);
		next_level = getLevel()+1;
		sql = _sql_children;
		sql_params = _sql_params;
		setLocation(_data);
		if(_data!=null){
			setNeighborhoodData(_n_data);
			if(getNeighborhoodData() == null)
				setNeighborhoodData(new Constituents_NeighborhoodData(_data.organizationID, parent_nID, _model.getOrganizationID()));
		}else{
		   	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: null location for: "+_n_data);
			setNeighborhoodData(_n_data);
			if(getNeighborhoodData() == null){
				setNeighborhoodData(new Constituents_NeighborhoodData(null,parent_nID,_model.getOrganizationID()));
				getNeighborhoodData().names_subdivisions=model.getSubDivisions();
				getNeighborhoodData().name_subdivisions_lang=DDTranslation.org_language;
			}
		}
		if(ancestors == null) {
			setNextAncestors(new Constituents_AddressAncestors[0]);
		}else{
			setNextAncestors(new Constituents_AddressAncestors[ancestors.length+1]);
			for(int k=0; k<ancestors.length; k++) getNextAncestors()[k] = ancestors[k];
			getNextAncestors()[ancestors.length] = new Constituents_AddressAncestors(getLocation().fieldID,getLocation().value);
		}
		if(getLocation() != null) {
			try {
				populateAddress(true);
			}catch(Exception e){
				e.printStackTrace();
				return;			
			}
		}
		if(DEBUG) System.err.println("Creating ConstituentsAddress: "+getLocation());
	}
	/**
	 * Creates an unsigned neighborhood with GID null.
	 * Needs keys and sk set in the current GUI implementation with getCrtIdentityKeys
	 */
	public void addEmptyNeighborhood() {
		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:addEmptyNeighborhood: start");
		ConstituentsAddressNode child;
		String arrival_time = Util.getGeneralizedTime();
		String submitter_ID = Util.getStringID(model.getConstituentIDMyself());
		SK sk = Application_GUI.getCrtIdentityKeys();
		Cipher keys;
		keys = Application_GUI.getCrtIdentityCipher();
		if (keys == null || sk == null) return;
		String orgGID = D_Organization.getGIDbyLIDstr(Util.getStringID(model.getOrganizationID()));
	    try {
			String subdivision = getNeighborhoodData().getChildSubDivision(0);
			String division = getNeighborhoodData().getChildDivision(0);
			String gID = null;
			String parent_nID = Util.getStringID(getNeighborhoodData().neighborhoodID);
	    	D_Neighborhood dn = D_Neighborhood.getEmpty();
	    	dn.setGID(gID);
	    	dn.setParentLIDstr(parent_nID);
	    	dn.setName(__("Not initialized"));
	    	dn.setName_lang(DDTranslation.authorship_lang.lang);
	    	dn.setName_charset(DDTranslation.authorship_lang.flavor);
	    	dn.setName_division(division);
	    	dn.setNames_division_lang(getNeighborhoodData().name_subdivisions_lang.lang);
	    	dn.setNames_division_charset(getNeighborhoodData().name_subdivisions_lang.flavor);
	    	dn.setNames_subdivisions(D_Neighborhood.splitSubDivisions(subdivision));
	    	dn.setNames_subdivisions_lang(getNeighborhoodData().name_subdivisions_lang.lang);
	    	dn.setNames_subdivisions_charset(getNeighborhoodData().name_subdivisions_lang.flavor);
	    	dn.setSubmitterLIDstr(submitter_ID);
	    	dn.setOrgIDs(orgGID, model.getOrganizationID());
	    	if (DD.NEIGHBORHOOD_SIGNED_WHEN_CREATED_EMPTY) {
	    		if (sk != null) {
	    			dn.setArrivalDateStr(arrival_time);
	    			dn.setGID(dn.make_ID());
	    			dn.sign(sk);
	    		} else {
	    			Application_GUI.warning(__("No key found in Identity!")+" "+
	    					__("Probably not yet implemented signature of empty Neighborhoods"),
	    					__("No Keys"));
	    		}
	    	}
	    	dn.storeRequest();
	    	dn.releaseReference();
	    	long nID = dn.getLID_force();
			Constituents_NeighborhoodData nd = new Constituents_NeighborhoodData(nID, model.getOrganizationID());
			this.addChild(child=new ConstituentsAddressNode(model,this,nd,getNextAncestors()), 0);
			setNeighborhoods(getNeighborhoods() + 1);
			Object tp[]=new Object[]{model.getRoot()};
			if(getParent()!=null){
				tp=getParent().getPath();
			}
			model.updateCensusInserted(this, getPath(), new int[]{0},new Object[]{child});
		}catch(Exception e) {
			e.printStackTrace();
		}
		if(DEBUG) System.err.println("ConstituentsModel:addEmptyNeighborhood:Creating ConstituentsAddress: "+getLocation());
	}
	ConstituentsAddressNode getChild(long fieldID,String value) {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: start "+fieldID);
		for(int k=0; k<getChildren().length; k++) {
			if(!(getChildren()[k] instanceof ConstituentsAddressNode)) continue;
			ConstituentsAddressNode can = (ConstituentsAddressNode)getChildren()[k];
			if(can.getLocation()==null) continue;
			if(can.getLocation().fieldID != fieldID) continue;
			if(can.getLocation().value==null) continue;
			if(!can.getLocation().value.equals(value)) continue;
			return can;
		}
		return null;
	}
	public ConstituentsAddressNode getChildByID(long neighborhoodID) {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:getChildByID start "+neighborhoodID);
		if(neighborhoodID<=0) return null;
		for(int k=0; k<getChildren().length; k++) {
			if(!(getChildren()[k] instanceof ConstituentsAddressNode)) continue;
			ConstituentsAddressNode can = (ConstituentsAddressNode)getChildren()[k];
			if(can.getNeighborhoodData()==null) continue;
			if(neighborhoodID==can.getNeighborhoodData().neighborhoodID) return can;
		}
		return null;
	}
	public ConstituentsIDNode getChildByConstituentID(long constituentID) {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:getChildByConstituentID start "+constituentID);
		if(constituentID<=0) return null;
		for(int k=0; k<getChildren().length; k++) {
			if(!(getChildren()[k] instanceof ConstituentsIDNode)) continue;
			ConstituentsIDNode can = (ConstituentsIDNode)getChildren()[k];
			if(can.getConstituent()==null) continue;
			if(constituentID==can.get_constituentID()) return can;
		}
		return null;
	}
    public int getIndexOfChild(Object child) {
    	if((child==null)||!(child instanceof ConstituentsBranch)) return -1;
    	for(int i=0; i<getChildren().length; i++){
    		if(getChildren()[i]==child) return i;
    		if(child instanceof ConstituentsIDNode) {
    			if(((ConstituentsIDNode)child).get_constituentID() == 
    				((ConstituentsIDNode)getChildren()[i]).get_constituentID()) return i;
    		}
    	}
    	if(DEBUG) System.err.println("Index of: "+child+" in: "+this);
    		for(int i=0; i<getChildren().length; i++)
    			if(DEBUG) System.err.println("Here of: "+getChildren()[i]);
    	return -1;
    }
    public String getTip() {
		if ((getLocation() == null)||(getLocation().tip == null)) return null;
    	return getLocation().getLabel()+" ("+getLocation().tip+")";
    }
    /**
     * The display is based on ConstituentTree:getTreeCellRendererComponentCIN
     */
    public String toString() {
		if ((getLocation() == null)||(getLocation().value == null)){
		   	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:null addrnode!");
			return __("Unknown!");
		}
		return getLocation().value+" ("+__("divisions")+"=#"+getNchildren()+") ("+__("inhabitants")+"=#"+getLocation().inhabitants+")";    	
    }
    public void populate() {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populate start "+this);
		if(this == model.getRoot()){
		   	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: root: populate end");
			return;
		}
    	if (! populateAddress(false)) {
        	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: will populate ID alone");
        	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: will populate ID");
    		populateIDs(false);
    	}else{
    		if(DD.CONSTITUENTS_ORPHANS_SHOWN_BESIDES_NEIGHBORHOODS){
            	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: will populateID beside neighs");
            	populateIDs(false);
    		}else{
            	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: will not populateID beside neighs");
    		}
    	}
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populate end");
    }
    public void populateIDs() {
    	populateIDs(true);
    }
    void populateIDs(boolean resetCnt) {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateIDs start "+this);
    	if(resetCnt){
    		setChildren(new ConstituentsNode[0]);
    	}else{
    		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateIDs: childrens #"+getChildren().length);
    	}
    	ArrayList<ArrayList<Object>> identities, sel;
    	String tables = " ", where=" ";
    	int static_params = 1;
    	if (this != model.getRoot()) {
    		tables = " JOIN "+net.ddp2p.common.table.field_value.TNAME+" AS f ON f."+net.ddp2p.common.table.field_value.constituent_ID+" = fv."+net.ddp2p.common.table.constituent.constituent_ID+" "+" ";
    		where = " AND f."+net.ddp2p.common.table.field_value.value+" = ? AND f."+net.ddp2p.common.table.field_value.field_extra_ID+" = ? ";
    		static_params = 3;
    	}
    	int final_params = 0;
   		if(getNeighborhoodData().neighborhoodID>=0) {
   		}
   		int ancestors_nb = 0;
   		if(ancestors!=null) ancestors_nb = ancestors.length;
    	String param[] = new String[static_params+2*(ancestors_nb)+final_params];
    	param[0] = model.getOrganizationID()+"";
    	if(this != model.getRoot()) {
    		param[1] = ""+getLocation().value;
    		param[2] = ""+getLocation().fieldID;    
    	}
       	try {
     		for(int k=0; k < ancestors_nb; k++) {
    			tables = tables + " JOIN "+net.ddp2p.common.table.field_value.TNAME+" AS fv"+k+" ON fv."+net.ddp2p.common.table.field_value.constituent_ID+"=fv"+k+"."+net.ddp2p.common.table.field_value.constituent_ID+" ";
    			where = where + " AND fv"+k+"."+net.ddp2p.common.table.field_value.value+" = ? AND fv"+k+"."+net.ddp2p.common.table.field_value.field_extra_ID+" = ? ";
     			param[2*k+static_params] = ancestors[k].getValue();
     			param[2*k+static_params+1] = ancestors[k].getFieldID()+"";
    		}
    		if(getNeighborhoodData().neighborhoodID>=0) {
    			where = where + " AND fv."+net.ddp2p.common.table.field_value.neighborhood_ID+" ISNULL ";
    		}
    		sql = "SELECT " +
    				"fv."+net.ddp2p.common.table.constituent.name+
    				", fv."+net.ddp2p.common.table.constituent.forename+
    		        ", fv."+net.ddp2p.common.table.constituent.constituent_ID+
    		        ", fv."+net.ddp2p.common.table.constituent.external+
    		        ", fv."+net.ddp2p.common.table.constituent.global_constituent_ID +
    		        ", fv."+net.ddp2p.common.table.constituent.submitter_ID +
    		        ", fv."+net.ddp2p.common.table.constituent.slogan +
    		        ", fv."+net.ddp2p.common.table.constituent.email +
		        ", fv."+net.ddp2p.common.table.constituent.blocked +
		        ", fv."+net.ddp2p.common.table.constituent.broadcasted +
    				" FROM "+net.ddp2p.common.table.constituent.TNAME+" AS fv " +
				tables+" WHERE fv."+net.ddp2p.common.table.constituent.organization_ID+" = ? "+where+" GROUP BY fv."+net.ddp2p.common.table.constituent.constituent_ID+";";
    		if(DEBUG) System.err.print("F: populateIDs will select");
    		identities = Application.getDB().select(sql, param, DEBUG);
    		String sql_additional=
    			"SELECT " +
    			" fv."+net.ddp2p.common.table.constituent.name+
    			", fv."+net.ddp2p.common.table.constituent.forename+
    			", fv."+net.ddp2p.common.table.constituent.constituent_ID+
    			", fv."+net.ddp2p.common.table.constituent.external+
    			", fv."+net.ddp2p.common.table.constituent.global_constituent_ID +
		        ", fv."+net.ddp2p.common.table.constituent.submitter_ID +
		        ", fv."+net.ddp2p.common.table.constituent.slogan +
		        ", fv."+net.ddp2p.common.table.constituent.email +
		        ", fv."+net.ddp2p.common.table.constituent.blocked +
		        ", fv."+net.ddp2p.common.table.constituent.broadcasted +
   				" FROM "+net.ddp2p.common.table.constituent.TNAME+" as fv WHERE " +
    				net.ddp2p.common.table.constituent.neighborhood_ID+" = ?;";
    		identities.addAll(
    				Application.getDB().select(sql_additional, new String[]{""+getNeighborhoodData().neighborhoodID}));
    		if(DEBUG) System.err.print("F: populateIDs selected");
    	}catch(Exception e) {
    		e.printStackTrace();
    		return;
    	}
    	if(DEBUG) System.err.print("ConstituentsAddressNode: populateIDs Records="+identities.size());
    	for(int i=0; i<identities.size(); i++) {
    		ArrayList<Object> identities_i = identities.get(i);
    		if(identities_i.size() < 6) {
    			if(_DEBUG) System.err.println("ConstituentsModel:populateIDs selected size="+identities_i.size());
    			if(_DEBUG) System.err.println("ConstituentsModel:populateIDs selected sql="+sql);
    			Util.printCallPath("Wrong size!");
    			return;
    		}
    		String name, forename, constituentID, slogan, email;
    		Object obj;
    		obj = identities_i.get(0);
    		if(obj!=null) name = obj.toString();else name = null;
    		obj = identities_i.get(1);
    		if(obj!=null) forename = obj.toString();else forename = null;
    		obj = identities_i.get(2);
    		if(obj!=null) constituentID = obj.toString();else constituentID = "-1";
       		boolean blocked = "1".equals(Util.getString(identities_i.get(8)));
       		boolean broadcasted = "1".equals(Util.getString(identities_i.get(9)));
       		boolean external = "1".equals(Util.getString(identities_i.get(3)));
       		long submitterID = Util.lval(identities_i.get(5),-1);
       		D_Constituent c = null;
    		ConstituentData data = new ConstituentData(c);
    		data.setC_GID(Util.sval(identities_i.get(4),null));
    		data.setC_LID(Integer.parseInt(constituentID));
    		data.setGivenName(forename);
    		data.setSurname(name);
    		data.inserted_by_me=(model.getConstituentIDMyself() == submitterID);
    		data.external = external;
    		data.blocked = blocked;
    		data.broadcast = broadcasted;
       		slogan = Util.getString(identities_i.get(6));
       		email = Util.getString(identities_i.get(7));
    		data.setSlogan(slogan);
    		data.email = email;
    		String submitter_ID = Util.getString(identities_i.get(5));
    		data.submitter_ID = submitter_ID;
    		if(DEBUG) System.err.print("F: populateIDs child");
    		addChild(new ConstituentsIDNode(model, this, data,"",null, getNextAncestors()),0);
    	}
    	if(resetCnt) {
    		if(identities.size()!=getNchildren()) setNChildren(identities.size());
    	}else{
    		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateIDs: nchildrens #"+getNchildren());
    		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateIDs: identities #"+identities.size());
    	}
    	if(DEBUG) System.err.print("F: populateIDs fire");
    	model.updateCensusStructure(model,getPath());
    	if(DEBUG) System.err.print("F: populateIDs done");
    }
    void delNeighborhood(long nID) {
    	D_Neighborhood.delNeighborhood(nID, model.getOrganizationID());
    }
    public void del(Object _child){
    	int constituentID=-1;
    	for (int i=0; i<getChildren().length; i++) {
    		if(_child == getChildren()[i]) {
    			ConstituentsNode _children[] = new ConstituentsNode[getChildren().length-1];
    			for(int j=0;j<i;j++)_children[j]=getChildren()[j];
    			for(int j=i+1;j<getChildren().length;j++)_children[j-1]=getChildren()[j];
    			setChildren(_children);
    			break;
    		}
    	}
    	setNChildren(getNchildren()-1);
    	try{
        	ArrayList<ArrayList<Object>> identities;
    		ConstituentsAddressNode child = (ConstituentsAddressNode)_child;
    		Constituents_AddressAncestors[] ancestors=child.ancestors;
    		long cID = child.getNeighborhoodData().neighborhoodID;
    		if(cID>=0){
    			setNeighborhoods(getNeighborhoods() - 1);
    			delNeighborhood(cID);
    		}
    		String q="fv."+net.ddp2p.common.table.field_value.constituent_ID;
    		String g="";
    		identities=child.getDynamicNeighborhoods(q,g);
    		if(DEBUG) System.err.println("Records= "+identities.size());
        	for(int i=0; i<identities.size(); i++) {   		
        			constituentID = Util.ival(identities.get(i).get(0), -1);
        			D_Constituent.delConstituent(constituentID);
        	}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    /**
     * Expand all addresses;
     * @param counting : set to true if only counting the # of neighborhoods, not really popupating them
     * @return
     */
    boolean populateAddress(boolean counting) {
    	if (DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateAddress: "+this);
    	setChildren(new ConstituentsNode[0]);
    	ArrayList<ArrayList<Object>> identities;
    	ArrayList<Long> subnei = new ArrayList<Long>();
    	if (ancestors == null) {
    		if (DEBUG) System.err.print("ConstituentsModel:ConstituentsAddressNode:populateAddress: Empty ancestors in "+this);
    		return false;
    	}
     	try {
     		String query="fv."+net.ddp2p.common.table.field_value.value+", count(*), fe."+net.ddp2p.common.table.field_extra.tip+", fv."+net.ddp2p.common.table.field_value.field_extra_ID+", fe."+net.ddp2p.common.table.field_extra.partNeigh+", fv."+net.ddp2p.common.table.field_value.fieldID_above+", fv."+net.ddp2p.common.table.field_value.field_default_next+", fv."+net.ddp2p.common.table.field_value.neighborhood_ID+" ";
     		String group="GROUP BY fv."+net.ddp2p.common.table.field_value.value+" ORDER BY fv."+net.ddp2p.common.table.field_value.value+" DESC";
     		identities = getDynamicNeighborhoods(query,group);
    		if ((getNeighborhoodData() != null) && (getNeighborhoodData().neighborhoodID >= 0)) {
    			subnei = D_Neighborhood.getNeighborhoodChildrenIDs(getNeighborhoodData().neighborhoodID);
    		}
    		if (counting) {
    			int _nchildren=0;
    			if (subnei.size() == 0) {
    				_nchildren = identities.size();
    				setNeighborhoods(_nchildren);
    				if (_nchildren < 1) _nchildren = 1;
    				setNChildren(_nchildren);
    				return identities.size() > 0;
    			}
    			int n = 0;
    			for (int i = 0; i < identities.size(); i ++) {
    				String value = Util.sval(identities.get(i).get(0),null);
    				_nchildren++;
    		    	for (; n < subnei.size(); n ++) {
    		    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(subnei.get(n), true, false);
    		    		String n_name = dn.getName(); 
    		    		int cmp = value.compareToIgnoreCase(n_name);
    		    		if (cmp > 0) break; 
    		    		if (cmp < 0) _nchildren ++;
    		    	}    				
    			}
    			_nchildren += subnei.size()-n;
    			boolean hasChildren = _nchildren>0;
    			setNeighborhoods(_nchildren);
    			if(_nchildren<=0) _nchildren =1;
				setNChildren(_nchildren);
    			return hasChildren;
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    		return false;
    	}
    	if(DEBUG) System.err.println("Records= "+identities.size());
		int n = 0;
		int _nchildren = 0;
    	for (int i=0; i<identities.size(); i++) {
    		String count, tip, fieldID;
    		Object obj;
			String value=Util.sval(identities.get(i).get(0),null);
			if(DEBUG) System.err.println("Identity= "+value);
		    if ((value != null) && (subnei != null)) {
		    	for (; n < subnei.size(); n++) {
		    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(subnei.get(n), true, false);
		    		String n_name = dn.getName(); 
		    		int cmp = value.compareToIgnoreCase(n_name);
		    		if(DEBUG) System.err.println("Neigh= "+n_name+" :"+cmp);
		    		if (cmp>0) break; 
		    		if (cmp<0) {
		    			long nID = subnei.get(n); 
		    			if(DEBUG) System.err.println("...nID = "+getNeighborhoodData().neighborhoodID);
		    			Constituents_NeighborhoodData nd=new Constituents_NeighborhoodData(nID, getNeighborhoodData().neighborhoodID, model.getOrganizationID());
		    			addChild(
		    					new ConstituentsAddressNode(model, this, nd, getNextAncestors()),
		    					0);
		    		}
		    	}
		    }			
			obj = identities.get(i).get(1);
    		if(obj!=null) count = obj.toString();else count = "0";
    		obj = identities.get(i).get(2);
    		if(obj!=null) tip = obj.toString();else tip = null;
    		obj = identities.get(i).get(3);
    		if(obj!=null) fieldID = obj.toString();else fieldID = "-1";
    		if(DEBUG) System.err.println(i+" Got: v="+value+" c="+count+" tip="+tip+" fID="+fieldID);
    		Constituents_LocationData data = new Constituents_LocationData();
    		data.value = value;
    		data.fieldID = Util.lval(fieldID,-1);
    		data.inhabitants = Util.ival(count,0);
    		data.organizationID = getLocation().organizationID;
    		data.partNeigh = Util.ival(identities.get(i).get(4),0);
    		data.fieldID_above = Util.lval(identities.get(i).get(5), -1);
    		data.setFieldID_default_next(Util.lval(identities.get(i).get(6), -1));
    		data.neighborhood = Util.ival(identities.get(i).get(7),0);
    		data.tip = tip;
    		populateChild(new ConstituentsAddressNode(model, this, data, "", null, 
    				getNextAncestors(), getFieldIDs(), next_level, getNeighborhoodData().neighborhoodID,null),0);
    		_nchildren++;
    	}
	    for (; n < subnei.size(); n++) {
    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(subnei.get(n), true, false);
    		String n_name = dn.getName(); 
	    	long nID = subnei.get(n); 
	    	if(DEBUG) System.err.println("nID = "+getNeighborhoodData().neighborhoodID);
	    	Constituents_NeighborhoodData nd=new Constituents_NeighborhoodData(nID, getNeighborhoodData().neighborhoodID, model.getOrganizationID());
	    	addChild(new ConstituentsAddressNode(model, this, nd, getNextAncestors()), 0);
	    	_nchildren++;
	    }
    	if(_nchildren!=getNchildren()) setNChildren(_nchildren);
    	Object[] mypath = getPath();
    	if(DEBUG) {
    		System.err.println("Found mypath = "+mypath.length);
    		for(int k=0; k<mypath.length; k++) System.err.println(k+": "+mypath[k]);
    	}
    	setNeighborhoods(_nchildren);
    	if(_nchildren == 0) {
    		return false;
    	}
    	model.updateCensusStructure(model,getPath());
    	return true;
    }
    ArrayList<ArrayList<Object>> getDynamicNeighborhoods(String query, String group) throws P2PDDSQLException {
		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:getDynamicNeighborhoods: start");
    	ArrayList<ArrayList<Object>> identities;
    	String tables = " ";
    	String where = " ";
    	int static_params=5;
    	String param[] = new String[static_params+2*(ancestors.length)];
       	param[0] = ""+model.getOrganizationID();
       	param[1] = ""+getLocation().fieldID;
       	param[2] = ""+getLocation().fieldID;
       	param[3] = ""+getLocation().value;
       	param[4] = ""+getLocation().fieldID;
       	for(int k=0; k < ancestors.length; k++) {
       		tables = tables + " JOIN "+net.ddp2p.common.table.field_value.TNAME+" AS fv"+k+" ON fv."+net.ddp2p.common.table.field_value.constituent_ID+"=fv"+k+"."+net.ddp2p.common.table.field_value.constituent_ID+" ";
       		where = where + " AND fv"+k+"."+net.ddp2p.common.table.field_value.value+" = ? AND fv"+k+"."+net.ddp2p.common.table.field_value.field_extra_ID+" = ? ";
       		param[2*k+static_params] = ancestors[k].getValue();
       		param[2*k+1+static_params] = ancestors[k].getFieldID()+"";
       	}
       	sql = "select "+query+
       	" from "+net.ddp2p.common.table.field_value.TNAME+" AS fv " +
       	" JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON c."+net.ddp2p.common.table.constituent.constituent_ID+"=fv."+net.ddp2p.common.table.field_value.constituent_ID +
       	" JOIN "+net.ddp2p.common.table.field_value.TNAME+" AS f ON f."+net.ddp2p.common.table.field_value.constituent_ID+"=fv."+net.ddp2p.common.table.field_value.constituent_ID +
       	tables+
       	" JOIN "+net.ddp2p.common.table.field_extra.TNAME+" AS fe ON fe."+net.ddp2p.common.table.field_extra.field_extra_ID+" = fv."+net.ddp2p.common.table.field_value.field_extra_ID +
       	" WHERE c."+net.ddp2p.common.table.constituent.organization_ID+" = ?" +
       	" AND fv."+net.ddp2p.common.table.field_value.field_extra_ID+" > ? AND fv."+net.ddp2p.common.table.field_value.fieldID_above+" = ? AND f."+net.ddp2p.common.table.field_value.value+" = ? AND f."+net.ddp2p.common.table.field_value.field_extra_ID+" = ? "+
       	where + 
       	" "+group+";";
       	identities = Application.getDB().select(sql, param);
       	return identities;
    }
    /**
     * Return language string for instances of children nodes using this value
     * @return
     */
	public String getValueLanguage() {
		if(getNeighborhoodData()==null) return "en";
		if(getNeighborhoodData().name_lang==null) return "en";
		return getNeighborhoodData().name_lang.lang;
	}
	public void advertise(String orgGIDH) { 
		String hash = D_Constituent.getGIDHashFromGID(this.getNeighborhoodData().global_nID);
		String org_hash = orgGIDH; 
		ClientSync.addToPayloadFix(net.ddp2p.common.streaming.RequestData.NEIG, hash, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
	}
	public void block() { 
		try {
			D_Neighborhood dn = D_Neighborhood.getNeighByLID(this.getNeighborhoodData().neighborhoodID, true, true);
			dn.setBlocked(! dn.isBlocked());
			this.getNeighborhoodData().blocked = dn.isBlocked();
			dn.storeRequest();
			dn.releaseReference();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public void broadcast() { 
		try {
			D_Neighborhood dn = D_Neighborhood.getNeighByLID(this.getNeighborhoodData().neighborhoodID, true, true);
			dn.setBroadcasted(! dn.isBroadcasted());
			this.getNeighborhoodData().broadcasted = dn.isBroadcasted();
			dn.storeRequest();
			dn.releaseReference();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public void zapp() { 
		D_Neighborhood.zapp(this.getNeighborhoodData().neighborhoodID);
	}
	public Constituents_NeighborhoodData getNeighborhoodData() {
		return n_data;
	}
	public void setNeighborhoodData(Constituents_NeighborhoodData n_data) {
		this.n_data = n_data;
	}
	public Constituents_LocationData getLocation() {
		return location;
	}
	public void setLocation(Constituents_LocationData location) {
		this.location = location;
	}
	public long[] getFieldIDs() {
		return fieldIDs;
	}
	public void setFieldIDs(long fieldIDs[]) {
		this.fieldIDs = fieldIDs;
	}
	public Constituents_AddressAncestors[] getNextAncestors() {
		return next_ancestors;
	}
	public void setNextAncestors(Constituents_AddressAncestors[] next_ancestors) {
		this.next_ancestors = next_ancestors;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
}
