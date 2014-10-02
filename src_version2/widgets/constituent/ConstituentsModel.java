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
 package widgets.constituent;
import hds.ClientSync;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import ASN1.Encoder;
import ciphersuits.Cipher;
import ciphersuits.SK;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Identity;
import config.Language;
import data.D_OID;
import data.D_OrgParam;
import data.D_Organization;
import data.D_Constituent;
import data.D_Neighborhood;
import data.D_Witness;










//import com.sun.mirror.apt.Filer.Location;
import java.util.*;
import java.text.MessageFormat;

import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import widgets.app.MainFrame;
import widgets.components.DDTranslation;
import widgets.components.TreeModelSupport;
import widgets.org.OrgExtra;
import static util.Util.__;
class ConstituentProperty{
	long field_valuesID;
	String OID;
	String explain;
	String OID_name;
	long certificateID;
	String certificate;
	String value;
	public String label;
	public String toString(){
		return "ConstituentProperty: "+value+"("+OID+")";
	}
}	
class ConstituentData {
	String global_constituentID;
	long constituentID;
	String given_name;
	String surname;
	int witness_for;
	int witness_against;
	Icon icon;
	String slogan;
	boolean inserted_by_me;
	int witnessed_by_me; // 1 for positive, -1 for negative, 2-added by myself
	boolean external;
	public String email;
	public String submitter_ID;
	public boolean blocked;
	public boolean broadcast;
	public int witness_neuter;
	public int myself;
	public D_Constituent constituent;
	public String toString(){
		return "ConstituentData: "+surname+"," +given_name+": "+slogan+"="+ witness_for+"/"+witness_against;
	}
}
class LocationData {
	String value;
	int inhabitants;
	long organizationID;
	String label;
	long fieldID;
	long field_valuesID;
	String list_of_values;
	int partNeigh;
	long fieldID_above;
	long fieldID_default_next;
	String tip;
	long neighborhood;
	boolean censusDone = false;
	public String toString(){
		return "LocationData: "+value+" #"+fieldID;
	}
}
class ConstituentsNode {
	ConstituentsBranch parent;
	Object[] getPath(){
		if(parent == null) return new Object[]{this};
		Object[] presult = parent.getPath();
		Object[] result = new Object[presult.length+1];
		for(int k=0; k<presult.length; k++) result[k] = presult[k];
		result[presult.length] = this;
		return result;
	}
	ConstituentsNode(ConstituentsBranch _parent){
		parent = _parent;
	}
}
class AddressAncestors {
	String value;
	long fieldID;
	AddressAncestors(long _fieldID, String _value) {
		fieldID = _fieldID;
		value = _value;
	}
	public String toString(){
		return value+"::"+fieldID;
	}
}
abstract class ConstituentsBranch extends ConstituentsNode{
	protected static final boolean DEBUG = false;
	int nchildren = 0;
	ConstituentsModel model;
	AddressAncestors ancestors[];
	ConstituentsNode children[]=new ConstituentsNode[0];
	int neighborhoods = 0;
	ConstituentsBranch (ConstituentsModel _model, ConstituentsBranch _parent,
			AddressAncestors[] _ancestors, int _nchildren) {
		super(_parent);
		model = _model;
		ancestors = _ancestors;
		nchildren = _nchildren;
		//if(ancestors == null) System.err.println("ROOOOOOOOOR\nROOOOOT?\nROOOOT!");
	}
    abstract int getIndexOfChild(Object child);
    abstract void populate();
    int getChildCount() {
    	return nchildren;
    }
	public void colapsed() {
		if(DEBUG) System.err.println("colapsed: "+this);
		if(this!=model.root) children = new ConstituentsNode[0];
		if(nchildren == 0) nchildren=1;
	}

	public boolean isColapsed() {
		if ((children==null) || (children.length == 0))
			return true;
		return false;
	}

    public String convertValueToText() {
    	return toString();
    }
    void setNChildren(int _nchildren) {
    	nchildren = _nchildren;
    	Object[] path2parent;
    	if(DEBUG) System.err.println("#children:"+nchildren+" for "+this);
    	if(parent == null) {return;}
    	path2parent = parent.getPath();
    	if(parent.nchildren == ((ConstituentsAddressNode)parent).children.length) {
    		int idx = parent.getIndexOfChild(this);
    		if(idx<0) return;
    		model.fireTreeNodesChanged(new TreeModelEvent(model, path2parent, 
    				new int[]{idx},new Object[]{this}));
    	}
    }
    Object getChild (int index) {
		if (DEBUG) System.err.println("ConstituentsBranch:getChild:Creating ConstituentsPropertyNode ");
    	if (index < 0) return null;
     	if ((children.length == 0) && (this != model.root)) populate();
    	if (index < children.length) return children[index];
    	System.out.println("ConstituentsModel: getChild: "+index+" this="+this);
    	return __("Right click for a menu!");
    }
    void populateChild(ConstituentsNode child, int index) {
    	ConstituentsNode _children[] = new ConstituentsNode[children.length+1];
    	if((index<0) || (index>children.length)) return;
    	for(int i=0; i<index; i++)	_children[i] = children[i];
    	_children[index] = child;
    	for(int i=index; i<children.length;i++) _children[i+1] = children[i];
    	children = _children;
    }
    void addChild(ConstituentsNode child, int index) {
    	populateChild(child, index);
    	if(children.length > nchildren)
    		setNChildren(nchildren+1);
    }
}
class ConstituentsPropertyNode extends ConstituentsNode {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	ConstituentProperty property;
	public ConstituentsPropertyNode (ConstituentProperty _property, ConstituentsBranch _parent) {
		super(_parent);
		property = _property;
		if(DEBUG) System.err.println("Creating ConstituentsPropertyNode: "+property);
	}
	public String getTip() {
		if(DEBUG)System.err.println("ConstituentModel:ConstituentPropertyNode:getTip");
		if(property==null) return __("Poperty not initialized!");
			
    	if(property.label==null) return property.explain+"("+property.OID_name+": "+property.OID+")";
    	if(property.explain == null)
    		return property.label+" ("+property.OID_name+": "+property.OID+")";
    	return property.label+"("+property.explain+": "+property.OID_name+": "+property.OID+")";
	}
	public long get_field_valuesID(){
		if(property == null) return -1;
		return property.field_valuesID;
	}
	public String toString() {
		if(property==null){
			if(DEBUG) System.err.println("ConstituentsPropertyModel: toString null const");
			return __("Unknown!");
		}
		return property.value;
	}
}
class ConstituentsIDNode extends ConstituentsBranch {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private int fixed_fields = 1;
	ConstituentData constituent;
	String sql;
	String sql_params[] = new String[0];
	void updateWitness(){  
		ArrayList<ArrayList<Object>> identities;
        try{
                String sql = "select w."+table.witness.sense_y_n+", count(*) " +
                                " from "+table.witness.TNAME+" as w " +
                                " where w."+table.witness.target_ID+" = ? " +
                                " GROUP BY w."+table.witness.sense_y_n+"; ";
                identities = model.db.select(sql, new String[]{constituent.constituentID+""});
                constituent.witness_against = constituent.witness_for = 0;
                for (int k = 0; k < identities.size(); k ++) {
                        int wsense = Util.ival(identities.get(k).get(0), D_Witness.UNKNOWN);
                        int wcount = Util.ival(identities.get(k).get(1), 1);
                        if (wsense == D_Witness.FAVORABLE) constituent.witness_for = wcount;
                        else if(wsense==D_Witness.UNFAVORABLE) constituent.witness_against = wcount;
                        else constituent.witness_neuter = wcount;
                }
                
                sql = "select w."+table.witness.sense_y_n +
                " from "+table.witness.TNAME+" as w " +
                " where w."+table.witness.target_ID+" = ? and w."+table.witness.source_ID+" = ? " +
                " ; ";
                long myself = model.getConstituentIDMyself();
                identities = model.db.select(sql, 
                		new String[]{constituent.constituentID+"", myself+""});
                constituent.witnessed_by_me=D_Witness.UNSPECIFIED;
                if (identities.size() > 1) Application_GUI.warning(__("For constituent:")+" "+constituent.constituentID, __("Multiple witness from me"));
                for(int k=0; k<identities.size(); k++) {
                	int wsense = Util.ival(identities.get(k).get(0), D_Witness.UNKNOWN);
                	constituent.witnessed_by_me = wsense;
                	if(DEBUG) System.err.println("Witnessed by me with: "+wsense);
                }
                if(myself == constituent.constituentID) constituent.myself = 1; 
                else constituent.myself = 0; 
        }catch(Exception e){
                JOptionPane.showMessageDialog(null,"populate witnesses: "+e.toString());
                e.printStackTrace();                    
        }
	}
	public ConstituentsIDNode (ConstituentsModel _model, ConstituentsBranch _parent, ConstituentData _data,
			String _sql_children, String[] _sql_params,
			AddressAncestors[] _ancestors) {
		super(_model, _parent, _ancestors, 0);
		sql = _sql_children;
		sql_params = _sql_params;
		constituent = _data;
		
		D_Constituent cons = D_Constituent.getConstByLID(constituent.constituentID, true, false);
		if (cons == null) return;
		if(_data.external)  fixed_fields ++;
		nchildren = cons.getFieldValuesFixedNB() + fixed_fields;
		
//		ArrayList<ArrayList<Object>> identities;
//		String params[] = new String[]{""+constituent.constituentID};
//		try {
//			String sql = "select count(*) from "+table.field_value.TNAME+" AS fv " +
//					" JOIN "+table.constituent.TNAME+" as c ON c."+table.constituent.constituent_ID+"=fv."+table.field_value.constituent_ID +
//					" JOIN "+table.field_extra.TNAME+" as fe ON fv."+table.field_value.field_extra_ID+"=fe."+table.field_extra.field_extra_ID +
//					" where fe."+table.field_extra.partNeigh+" <= 0 AND fv."+table.field_value.constituent_ID+" = ?;";
//			identities = model.db.select(sql, params, DEBUG);
//		}catch(Exception e) {
//			JOptionPane.showMessageDialog(null,"populate: "+e.toString());
//    		e.printStackTrace();
//    		return;
//		}
//		nchildren = Integer.parseInt(identities.get(0).get(0).toString()) + fixed_fields;

		updateWitness();
		if(DEBUG) System.err.println("ConstituentsModel: ConstituentsIDNode: "+constituent+" #"+nchildren);
	}
    int getIndexOfChild(Object child) {
     	if((child==null)||!(child instanceof ConstituentsPropertyNode)) return -1;
    	for(int i=0; i<children.length; i++){
    		if(children[i]==child) return i;
    		if(((ConstituentsPropertyNode)children[i]).get_field_valuesID()==
    			((ConstituentsPropertyNode)child).get_field_valuesID()) return i;
    	}
    	return -1;
    }
    public long get_constituentID() {
    	if(constituent == null) return -1;
    	return constituent.constituentID;
    }
    public String getTip() {
		if ((constituent == null)||(constituent.slogan == null)) return null;
    	return constituent.slogan;
    }
    /**
     * The display is based on ConstituentTree:getTreeCellRendererComponentCIN
     */
    public String toString() {
    	String result;
		if(constituent == null){
			if(DEBUG) System.err.println("ConstituentsIDNodeModel: toString null const");
			return __("Unknown!");
		}
		if ((constituent.given_name == null) || 
				(constituent.given_name.equals("")))
			result = constituent.surname;
		else result = constituent.surname+", "+constituent.given_name;    
		return result+ " "+constituent.email+ " ::"+ constituent.slogan;
    }
    void populate() {
    	//boolean DEBUG = true;
    	if (DEBUG) System.err.println("ConstituentsIDNode: populate this="+this);
		children = new ConstituentsPropertyNode[0];
		D_Constituent c = D_Constituent.getConstByLID(constituent.constituentID, true, false);
		if (c == null) { // || c.address == null) {
	    	if (_DEBUG) System.err.println("ConstituentsIDNode: populate null c or address for: "+constituent.constituentID);
	    	if (_DEBUG) System.err.println("ConstituentsIDNode: populate null c or address: "+c);
			//return;
		}
		
//		ArrayList<ArrayList<Object>> identities;
//		String params[] = new String[]{""+constituent.constituentID};
//		try {
//			String sql =
//				"SELECT fv."+table.field_value.value + ", fe."+table.field_extra.label+
//				", o."+table.oid.OID_name+", o."+table.oid.sequence+", o."+table.oid.explanation+
//					" FROM "+table.field_value.TNAME+" AS fv " +
//					" JOIN "+table.constituent.TNAME+" AS c ON c."+table.constituent.constituent_ID+"=fv."+table.field_value.constituent_ID +
//					" JOIN "+table.field_extra.TNAME+" AS fe ON fv."+table.field_value.field_extra_ID+"=fe."+table.field_extra.field_extra_ID +
//					" LEFT JOIN "+table.oid.TNAME+" AS o ON o."+table.oid.sequence+"=fe."+table.field_extra.oid +
//					" WHERE ( fe."+table.field_extra.partNeigh+" <= 0 OR fe."+table.field_extra.partNeigh+" IS NULL ) AND fv."+table.constituent.constituent_ID+" = ?;";
//			identities = model.db.select(sql, params, DEBUG);
//		}catch(Exception e) {
//			JOptionPane.showMessageDialog(null,"ConstituentsIDNode:populate: "+e.toString());
//    		e.printStackTrace();
//    		return;
//		}
		int identities_size = 0;
		D_Organization org = null;
		
		org = D_Organization.getOrgByLID_NoKeep(c.getOrganizationID(), true);
		
		if (c.address != null) {
			if (DEBUG) System.out.println("ConstituentsModel: populate: addresses #"+c.address.length);
			for ( int i = 0 ; i < c.address.length; i ++ ) {
				if (DEBUG) System.out.println("ConstituentsModel: populate: addresses #"+i+" -> "+c.address[i]);
	    		
	    		D_OrgParam fe = c.address[i].field_extra;
	    		if (fe == null) {
	    			if (_DEBUG) System.out.println("ConstituentsModel: populate: addresses 1 null fe #"+i+" -> "+c.address[i].field_extra_GID);
	    			fe = org.getFieldExtra(c.address[i].field_extra_GID);
	    		}
	    		if (fe == null) {
	    			if (_DEBUG) System.out.println("ConstituentsModel: populate: addresses 2 null fe #"+i+" -> "+c.address[i].field_extra_GID);
	    			continue;
	    		}
	    		if (fe.partNeigh > 0) {
	    			if (DEBUG) System.out.println("ConstituentsModel: populate: addresses 3 neigh fe #"+i+" - "+fe.partNeigh+": -> "+fe);
	    			continue;
	    		}
	    		identities_size ++;
	    		D_OID oid = D_OID.getBySequence(fe.oid);
	    		String value;
	    		Object obj;
	    		obj = c.address[i].value; //identities.get(i).get(0);
	    		if (obj != null) value = obj.toString();else value = null;
	    		ConstituentProperty data = new ConstituentProperty();
	    		data.value = value;
	    		data.label = fe.label; //Util.getString(identities.get(i).get(1));
	    		if (oid != null) {
		    		data.OID_name = oid.OID_name; //Util.getString(identities.get(i).get(2));
		    		data.OID = oid.sequence; //Util.getString(identities.get(i).get(3));
		    		data.explain = oid.explanation; //Util.getString(identities.get(i).get(4));
	    		}
	    		populateChild(new ConstituentsPropertyNode(data, this),0);
	    	}
		}
		if ( fixed_fields >= 1) {// email, fixed field
    		ConstituentProperty data = new ConstituentProperty();
    		data.value = this.constituent.email;
    		data.label = __("Email");//;Util.getString(identities.get(i).get(1));
    		//data.OID_name = Util.getString(identities.get(i).get(2));
    		//data.OID = Util.getString(identities.get(i).get(3));
    		//data.explain = Util.getString(identities.get(i).get(4));
    		populateChild(new ConstituentsPropertyNode(data, this),0);			
		}
		if( fixed_fields >= 2){// submitter
    		ConstituentProperty data = new ConstituentProperty();
    		data.label = __("Submitter");
    		String subm_ID = this.constituent.submitter_ID;
    		data.value = subm_ID;
    		long s_ID = Util.lval(subm_ID, -1);
    		if (s_ID > 0) {
    			D_Constituent wc = D_Constituent.getConstByLID(subm_ID, true, false);
    			wc.loadNeighborhoods(D_Constituent.EXPAND_ONE);
    			data.value = wc.surname+", "+wc.forename+" <"+wc.email+">";
    			if ((wc.neighborhood!=null) && (wc.neighborhood.length>0))
    				data.value += "("+wc.neighborhood[0].getName_division()+":"+wc.neighborhood[0].getName()+")";
    		} else {
    			data.value = "-";
    		}
   			populateChild(new ConstituentsPropertyNode(data, this),0);
		}
    	if( (fixed_fields+identities_size)!=nchildren) setNChildren(identities_size+ fixed_fields);
    	model.fireTreeStructureChanged(new TreeModelEvent(model,getPath()));
   }
	public void setMySelf(ConstituentsTree tree) {
    	ConstituentsModel model = (ConstituentsModel)tree.getModel();
    	model.setCurrentConstituent(this.constituent.constituentID, tree);
	}
	public void advertise(ConstituentsTree tree) {
		String hash = D_Constituent.getGIDHashFromGID(this.constituent.global_constituentID);
		String org_hash = D_Organization.getOrgGIDHashGuess(tree.getModel().getOrgGID());
		ClientSync.addToPayloadFix(streaming.RequestData.CONS, hash, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
	}
	public void block(ConstituentsTree tree) {
		try {
			D_Constituent constit = D_Constituent.getConstByLID(this.constituent.constituentID, true, true);
			boolean blocked = constit.toggleBlock();
			constit.storeRequest();
			constit.releaseReference();
			this.constituent.blocked = blocked;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public void broadcast(ConstituentsTree tree) {
		try {
			D_Constituent constit = D_Constituent.getConstByLID(this.constituent.constituentID, true, true);
			boolean broadcast = constit.toggleBroadcast();
			constit.storeRequest();
			constit.releaseReference();
			this.constituent.broadcast = broadcast;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public void zapp(ConstituentsTree tree) {
		D_Constituent constit = D_Constituent.zapp(this.constituent.constituentID);
		MainFrame.status.droppingConst(constit);
	}
}
//http://www.britishpathe.com/record.php?id=700
class NeighborhoodData {
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	String address;
	long neighborhoodID = -2;
	String global_nID;
	long parent_nID;
	String name;
	Language name_lang;
	String name_division;
	Language name_division_lang;
	String names_subdivisions;
	Language name_subdivisions_lang;
	long submitterID;
	long organizationID;
	byte[] signature;
	public boolean blocked;
	public boolean broadcasted;
	
//	String neighborhoods_fields = table.neighborhood.address+","+ table.neighborhood.neighborhood_ID+","+ table.neighborhood.global_neighborhood_ID+","+
//	table.neighborhood.parent_nID+","+ table.neighborhood.name+"," +table.neighborhood.name_lang+"," +table.neighborhood.name_charset+", " +
//	table.neighborhood.name_division+", "+table.neighborhood.name_division_lang+"," +table.neighborhood.name_division_charset+","+
//	table.neighborhood.names_subdivisions+","+ table.neighborhood.name_subdivisions_lang+","+ table.neighborhood.name_subdivisions_charset+"," +
//	table.neighborhood.submitter_ID+","+ table.neighborhood.organization_ID+"," +table.neighborhood.signature;

	void getFrom(D_Neighborhood dn) {
		if (dn == null) return;
		organizationID = dn.getOrgLID();
		address = dn.getAddress();
		neighborhoodID = dn.getLID();
		global_nID = dn.getGID();
		parent_nID = dn.getParentLID();
		name = dn.getName();
		name_lang = dn.getName_Language();
		name_division = dn.getName_division();
		name_division_lang = dn.getName_division_Language();
		names_subdivisions = dn.getNames_subdivisions_str();
		name_subdivisions_lang = dn.getNames_subdivisions_Language();
		submitterID = dn.getSubmitterLID();
		signature = dn.getSignature();
	}
	/*
	void getFrom(ArrayList<ArrayList<Object>> sel) {
		//name_lang = new Language(null,null);
		//name_division_lang = new Language(null,null);
		//name_subdivisions_lang = new Language(null,null);
		if(sel.size()==0) neighborhoodID = -1;
		for(int k = 0; k<sel.size(); k++) {
				address = Util.sval(sel.get(k).get(table.neighborhood.IDX_ADDRESS), null); // 0
				neighborhoodID = Util.lval(sel.get(k).get(table.neighborhood.IDX_ID), -1); // 1
				global_nID = Util.sval(sel.get(k).get(table.neighborhood.IDX_GID), null); //2
				parent_nID = Util.lval(sel.get(k).get(table.neighborhood.IDX_PARENT_ID), -1); //3
				name = Util.sval(sel.get(k).get(table.neighborhood.IDX_NAME), null);  // 4
				name_lang = new Language(Util.sval(sel.get(k).get(table.neighborhood.IDX_NAME_LANG), null),Util.sval(sel.get(k).get(table.neighborhood.IDX_NAME_CHARSET), null)); //5,6
				name_division = Util.sval(sel.get(k).get(table.neighborhood.IDX_NAME_DIVISION), null); // 7
				name_division_lang = new Language(Util.sval(sel.get(k).get(table.neighborhood.IDX_NAME_DIVISION_LANG), null), Util.sval(sel.get(k).get(table.neighborhood.IDX_NAME_DIVISION_CHARSET),null));//8,9
				names_subdivisions = Util.sval(sel.get(k).get(table.neighborhood.IDX_NAMES_DUBDIVISIONS), null); // 10
				name_subdivisions_lang = new Language(Util.sval(sel.get(k).get(table.neighborhood.IDX_NAMES_DUBDIVISIONS_LANG), null), Util.sval(sel.get(k).get(table.neighborhood.IDX_NAMES_DUBDIVISIONS_CHARSET),null)); // 11, 12
				submitterID = Util.lval(sel.get(k).get(table.neighborhood.IDX_SUBMITTER_ID), -1); // 13
				signature = Util.sval(sel.get(0).get(table.neighborhood.IDX_SIGNATURE), "").getBytes(); // 14
				break;
			}		
	}
	*/
	public String toString(){
		String result="";
		result += "address="+address+"\n";
		result += "neighborhoodID="+neighborhoodID+"\n";
		result += "global_nID="+global_nID+"\n";
		result += "parent_nID="+parent_nID+"\n";
		result += "name="+name+"\n";
		result += "name_lang="+name_lang+"\n";		
		result += "name_division="+name_division+"\n";		
		result += "name_division_lang="+name_division_lang+"\n";		
		result += "names_subdivisions="+names_subdivisions+"\n";		
		result += "name_subdivisions_lang="+name_lang+"\n";		
		result += "submitterID="+submitterID+"\n";		
		result += "organizationID="+organizationID+"\n";		
		return result;
	}
	/**
	 * Get the neighborhood subdivision at index idx
	 * @param idx
	 * @return
	 */
	String getChildDivision(int idx) {
		return D_Neighborhood.getChildDivision(names_subdivisions, idx);
	}
	/**
	 * Returns the subdivisions after the idx-th position
	 * @param idx
	 * @return
	 */
	String getChildSubDivision(int idx) {
		return D_Neighborhood.getChildSubDivision(names_subdivisions, idx);
	}
	NeighborhoodData(long nID, long organizationID) {
		D_Neighborhood dn = D_Neighborhood.getNeighByLID(nID, true, false);
		this.organizationID = organizationID;
		getFrom(dn);
		/*
		ArrayList<ArrayList<Object>> sel;
		try{
			sel = DDTranslation.db.select("SELECT "+table.neighborhood.fields_neighborhoods+
					" FROM "+table.neighborhood.TNAME+" WHERE "+table.neighborhood.neighborhood_ID+" = ?;", new String[]{""+nID});
			this.organizationID = organizationID;
			getFrom(sel);
		}catch(Exception e){
			e.printStackTrace();
		}
		*/
	}
	NeighborhoodData(String value, long parent_nID, long organizationID) {
		read(value, parent_nID, organizationID);
	}
	NeighborhoodData(long nID, long parent_nID, long organizationID) {
		read(nID, parent_nID, organizationID);
	}
	public NeighborhoodData() {
		this.neighborhoodID = -2;
	}
	void read(String value, long parent_nID, long organizationID) {
		if (parent_nID == -2) {
			this.name = value;
			this.organizationID = organizationID;
			this.neighborhoodID = -2;
			this.parent_nID = parent_nID;
			return;
		}
		
		this.organizationID = organizationID;
		long LID = D_Neighborhood.getNeighLIDByNameAndParent(value, parent_nID, organizationID);
		D_Neighborhood dn = D_Neighborhood.getNeighByLID(LID, true, false);
		getFrom(dn);
				
	}
	void read(long nID, long parent_nID, long organizationID) {
		
		if (parent_nID == -2) {
			this.name = ""+nID;
			this.organizationID = organizationID;
			this.neighborhoodID = -2;
			this.parent_nID = parent_nID;
			return;
		}

		/*
		ArrayList<ArrayList<Object>> sel;
		try {
			if (parent_nID <= 0)
				sel = DDTranslation.db.select(
						"select  "+ table.neighborhood.fields_neighborhoods +
						" from "+table.neighborhood.TNAME+
						" where "+table.neighborhood.organization_ID+" = ? "
						+ " and ( "+table.neighborhood.parent_nID+" ISNULL "
						+ " OR "+table.neighborhood.parent_nID+" < 0 ) "
						+ " and "+table.neighborhood.neighborhood_ID+" = ?;",
						new String[]{Util.getStringID(organizationID), Util.getStringID(nID)});
			else
				sel = DDTranslation.db.select(
					"select " + table.neighborhood.fields_neighborhoods +
					" from " + table.neighborhood.TNAME +
					" where " + table.neighborhood.organization_ID + " = ? "
					+ " and "+table.neighborhood.parent_nID+" = ? "
					+ " and "+table.neighborhood.neighborhood_ID+" = ?;",
					new String[]{Util.getStringID(organizationID), Util.getStringID(parent_nID), Util.getStringID(nID)});
				this.organizationID = organizationID;
				getFrom(sel);
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		this.organizationID = organizationID;
		D_Neighborhood dn = D_Neighborhood.getNeighByLID(nID, true, false);
		if (dn == null) return;
		if (parent_nID > 0) {
			if (dn.getParentLID() != parent_nID) {
				System.out.println("ConstituentModel: read: inconsistent parent "+parent_nID+ " for "+ dn);
				return;
			}
		} else {
			if (dn.getParentLID() > 0) {
				System.out.println("ConstituentModel: read: inconsistent null parent "+parent_nID+ " for "+ dn);
				return;
			}
		}
		getFrom(dn);
	}
	
}
class ConstituentsAddressNode extends ConstituentsBranch {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	LocationData location;
	long fieldIDs[];
	int level, next_level;
	String sql;
	String sql_params[] = new String[0];
	AddressAncestors[] next_ancestors;
	NeighborhoodData n_data;
	
	public ConstituentsAddressNode (ConstituentsModel _model, ConstituentsBranch _parent, 
			NeighborhoodData nd, AddressAncestors[] _ancestors) {
		super(_model, _parent, _ancestors, 1);
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: start");
		fieldIDs = _model.fieldIDs;
		level = _ancestors.length;
		next_level = level+1;
		sql = "";
		sql_params = null;
		location = new LocationData();
		{
			location.value = nd.name;
			location.inhabitants = 0;
			location.organizationID = nd.organizationID;
			location.label = nd.name_division;
			if(level < fieldIDs.length) location.fieldID=fieldIDs[level];
			location.field_valuesID = -1;
			location.partNeigh = -1;
			if((level>0)&&(level<fieldIDs.length+1))
				location.fieldID_above = fieldIDs[level-1];
			else location.fieldID_above = 0;
			
			if((level+1>=0)&&(level+1<fieldIDs.length))
				location.fieldID_default_next = fieldIDs[level+1];
			else location.fieldID_default_next = 0;
			//((nd.description==null)?"":nd.description)+" "+
			String tr = DDTranslation.translate(nd.name_division,nd.name_division_lang);
			location.tip = ((tr==null)?"":tr)
					+" sd="+
					((nd.names_subdivisions==null)?"":nd.names_subdivisions)+
					" ("+nd.name_lang+")";
			location.neighborhood = nd.neighborhoodID;
		}
		n_data = nd;
		{
			if(level<fieldIDs.length) {
				next_ancestors = new AddressAncestors[ancestors.length+1];
				for(int k=0; k<ancestors.length; k++) next_ancestors[k] = ancestors[k];
				next_ancestors[ancestors.length] = new AddressAncestors(fieldIDs[level],nd.name);
			}else{
				next_ancestors = new AddressAncestors[ancestors.length+1];
				for(int k=0; k<ancestors.length; k++) next_ancestors[k] = ancestors[k];				
				next_ancestors[ancestors.length] = new AddressAncestors(0,nd.name);
			}
				
		}
		try {
			populateAddress(true);
			//nchildren = 1;
		}catch(Exception e){
			JOptionPane.showMessageDialog(null,"populate: "+e.toString());
			e.printStackTrace();
			return;			
		}
		if(DEBUG) System.err.println("Creating ConstituentsAddress: "+location);
	}
	
	public ConstituentsAddressNode (ConstituentsModel _model, ConstituentsBranch _parent, LocationData _data,
			String _sql_children, String[] _sql_params,
			AddressAncestors[] _ancestors, long[] _fieldIDs, int _level, long parent_nID, NeighborhoodData _n_data) {
		super(_model, _parent, _ancestors, 1);
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: start2");
		fieldIDs = _fieldIDs;
		level = _level;
		next_level = level+1;
		sql = _sql_children;
		sql_params = _sql_params;
		location = _data;
		if(_data!=null){
			n_data = _n_data;
			if(n_data == null)
				n_data = new NeighborhoodData(_data.organizationID, parent_nID, _model.getOrganizationID());
				//n_data = new NeighborhoodData(_data.value, parent_nID, _model.getOrganizationID());
		}else{
			
		   	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: null location for: "+_n_data);
		   	//Util.printCallPath("null location");
			n_data = _n_data;
			if(n_data == null){
				n_data = new NeighborhoodData(null,parent_nID,_model.getOrganizationID());
				n_data.names_subdivisions=model.subdivisions;
				n_data.name_subdivisions_lang=DDTranslation.org_language;
				//org_language=new Language("en","US");//organization language
			}
		}
		if(ancestors == null) {
			next_ancestors = new AddressAncestors[0];
		}else{
			next_ancestors = new AddressAncestors[ancestors.length+1];
			for(int k=0; k<ancestors.length; k++) next_ancestors[k] = ancestors[k];
			next_ancestors[ancestors.length] = new AddressAncestors(location.fieldID,location.value);
		}
		
		if(location != null) {
			//ArrayList<ArrayList<Object>> identities;
			try {
				populateAddress(true);
				//nchildren = 1;
			}catch(Exception e){
				JOptionPane.showMessageDialog(null,"populate: "+e.toString());
				e.printStackTrace();
				return;			
			}
		}
		if(DEBUG) System.err.println("Creating ConstituentsAddress: "+location);
	}
	/**
	 * Creates an unsigned neighborhood with GID null
	 */
	void addEmptyNeighborhood() {
		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:addEmptyNeighborhood: start");
		ConstituentsAddressNode child;
		widgets.identities.IdentityBranch ib = ((widgets.identities.IdentityBranch)Identity.current_id_branch);
		String arrival_time = Util.getGeneralizedTime();
		String submitter_ID = Util.getStringID(model.getConstituentIDMyself());
		SK sk = ib.getKeys();
		Cipher keys;
		keys = ib.getCipher();
		String orgGID = D_Organization.getGIDbyLIDstr(Util.getStringID(model.getOrganizationID()));
	    try {
			String subdivision = n_data.getChildSubDivision(0);
			String division = n_data.getChildDivision(0);
			String gID = null;
			//Util.getGlobalID("neighborhood", ""+((location!=null)?location.value:"")+model.constituentID);
			//if(null==WB_Neighborhood.localForNeighborhoodGID(gID)) return;
			String parent_nID = Util.getStringID(n_data.neighborhoodID);
			/*
			long nID=model.db.insert(table.neighborhood.TNAME,
					new String[]{table.neighborhood.global_neighborhood_ID, table.neighborhood.parent_nID, table.neighborhood.name, 
					table.neighborhood.name_lang, table.neighborhood.name_charset,
					table.neighborhood.name_division,table.neighborhood.name_division_lang,table.neighborhood.name_division_charset,
					table.neighborhood.names_subdivisions,table.neighborhood.name_subdivisions_lang,table.neighborhood.name_subdivisions_charset,
					table.neighborhood.submitter_ID,table.neighborhood.organization_ID},
					new String[]{gID, parent_nID, __("Not initialized"),
					DDTranslation.authorship_lang.lang, DDTranslation.authorship_lang.flavor,
					division,n_data.name_subdivisions_lang.lang,n_data.name_subdivisions_lang.flavor,
					subdivision,n_data.name_subdivisions_lang.lang,n_data.name_subdivisions_lang.flavor,
					Util.getStringID(model.getConstituentIDMyself()),
					Util.getStringID(model.getOrganizationID())}, DEBUG);
			*/
	    	D_Neighborhood dn = D_Neighborhood.getEmpty();
	    	dn.setGID(gID);
	    	dn.setParentLIDstr(parent_nID);
	    	dn.setName(__("Not initialized"));
	    	dn.setName_lang(DDTranslation.authorship_lang.lang);
	    	dn.setName_charset(DDTranslation.authorship_lang.flavor);
	    	dn.setName_division(division);
	    	dn.setNames_division_lang(n_data.name_subdivisions_lang.lang);
	    	dn.setNames_division_charset(n_data.name_subdivisions_lang.flavor);
	    	dn.setNames_subdivisions(D_Neighborhood.splitSubDivisions(subdivision));
	    	dn.setNames_subdivisions_lang(n_data.name_subdivisions_lang.lang);
	    	dn.setNames_subdivisions_charset(n_data.name_subdivisions_lang.flavor);
	    	dn.setSubmitterLIDstr(submitter_ID);
	    	dn.setOrgIDs(orgGID, model.getOrganizationID());

	    	if (DD.NEIGHBORHOOD_SIGNED_WHEN_CREATED_EMPTY) {
	    		if (sk != null) {
	    			dn.setArrivalDateStr(arrival_time);
	    			dn.setGID(dn.make_ID());
	    			dn.sign(sk);
	    			//D_Neighborhood.readSignStore(nID, sk, orgGID, parent_nID, submitter_ID, arrival_time);
	    		} else {
	    			Application_GUI.warning(__("No key found in Identity!")+" "+
	    					__("Probably not yet implemented signature of empty Neighborhoods"),
	    					__("No Keys"));
	    		}
	    	}
	    	dn.storeRequest();
	    	dn.releaseReference();
	    	long nID = dn.getLID_force();
			NeighborhoodData nd = new NeighborhoodData(nID, model.getOrganizationID());
			this.addChild(child=new ConstituentsAddressNode(model,this,nd,next_ancestors), 0);
			neighborhoods++;
			Object tp[]=new Object[]{model.root};
			if(parent!=null){
				tp=parent.getPath();
			}
			model.fireTreeNodesInserted(new TreeModelEvent(this,getPath(),
					new int[]{0},new Object[]{child}));
			//}else{
			//model.fireTreeStructureChanged(new TreeModelEvent(this,tp));
			//}
		}catch(Exception e) {
			e.printStackTrace();
		}
		if(DEBUG) System.err.println("ConstituentsModel:addEmptyNeighborhood:Creating ConstituentsAddress: "+location);
	}
	ConstituentsAddressNode getChild(long fieldID,String value) {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode: start "+fieldID);
		for(int k=0; k<children.length; k++) {
			if(!(children[k] instanceof ConstituentsAddressNode)) continue;
			ConstituentsAddressNode can = (ConstituentsAddressNode)children[k];
			if(can.location==null) continue;
			if(can.location.fieldID != fieldID) continue;
			if(can.location.value==null) continue;
			if(!can.location.value.equals(value)) continue;
			return can;
		}
		return null;
	}
	ConstituentsAddressNode getChildByID(long neighborhoodID) {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:getChildByID start "+neighborhoodID);
		if(neighborhoodID<=0) return null;
		for(int k=0; k<children.length; k++) {
			if(!(children[k] instanceof ConstituentsAddressNode)) continue;
			ConstituentsAddressNode can = (ConstituentsAddressNode)children[k];
			if(can.n_data==null) continue;
			if(neighborhoodID==can.n_data.neighborhoodID) return can;
		}
		return null;
	}
	ConstituentsIDNode getChildByConstituentID(long constituentID) {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:getChildByConstituentID start "+constituentID);
		if(constituentID<=0) return null;
		for(int k=0; k<children.length; k++) {
			if(!(children[k] instanceof ConstituentsIDNode)) continue;
			ConstituentsIDNode can = (ConstituentsIDNode)children[k];
			if(can.constituent==null) continue;
			if(constituentID==can.get_constituentID()) return can;
		}
		return null;
	}
    int getIndexOfChild(Object child) {
    	if((child==null)||!(child instanceof ConstituentsBranch)) return -1;
    	for(int i=0; i<children.length; i++){
    		if(children[i]==child) return i;
    		if(child instanceof ConstituentsIDNode) {
    			if(((ConstituentsIDNode)child).get_constituentID() == 
    				((ConstituentsIDNode)children[i]).get_constituentID()) return i;
    		}
    		/*
    		if(child instanceof ConstituentsAddressNode) {
    			if(((ConstituentsAddressNode)child).get_constituentID() == 
    				((ConstituentsAddressNode)children[i]).get_constituentID()) return i;
    		}
    		*/
    	}
    	if(DEBUG) System.err.println("Index of: "+child+" in: "+this);
    		for(int i=0; i<children.length; i++)
    			if(DEBUG) System.err.println("Here of: "+children[i]);
    	return -1;
    }
    /*
    public long get_constituentID(){
    	if(location == null) return -1;
    	return location.field_valuesID;
    }
    */
    public String getTip() {
		if ((location == null)||(location.tip == null)) return null;
    	//String tip=DDTranslation.translate(location.label,n_data.name_division_lang);
    	return location.label+" ("+location.tip+")";
    }
    /**
     * The display is based on ConstituentTree:getTreeCellRendererComponentCIN
     */
    public String toString() {
		if ((location == null)||(location.value == null)){
		   	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:null addrnode!");
			return __("Unknown!");
		}
		return location.value+" ("+__("divisions")+"=#"+nchildren+") ("+__("inhabitants")+"=#"+location.inhabitants+")";    	
    }
    void populate() {
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populate start "+this);
		if(this == model.root){
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
    void populateIDs() {
    	populateIDs(true);
    }
    void populateIDs(boolean resetCnt) {
    	//boolean DEBUG = true;
    	//if(true)return;
    	if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateIDs start "+this);
    	if(resetCnt){
    		children = new ConstituentsNode[0];
    	}else{
    		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateIDs: childrens #"+children.length);
    	}
    	ArrayList<ArrayList<Object>> identities, sel;
    	String tables = " ", where=" ";
    	int static_params = 1;
    	if (this != model.root) {
    		tables = " JOIN "+table.field_value.TNAME+" AS f ON f."+table.field_value.constituent_ID+" = fv."+table.constituent.constituent_ID+" "+" ";
    		where = " AND f."+table.field_value.value+" = ? AND f."+table.field_value.field_extra_ID+" = ? ";
    		static_params = 3;
    	}
    	int final_params = 0;
   		if(n_data.neighborhoodID>=0) {
   			////final_params=1;
   		}
   		int ancestors_nb = 0;
   		if(ancestors!=null) ancestors_nb = ancestors.length;
    	String param[] = new String[static_params+2*(ancestors_nb)+final_params];
    	param[0] = model.getOrganizationID()+"";
    	if(this != model.root) {
    		param[1] = ""+location.value;
    		param[2] = ""+location.fieldID;    
    	}
       	try {
     		for(int k=0; k < ancestors_nb; k++) {
    			tables = tables + " JOIN "+table.field_value.TNAME+" AS fv"+k+" ON fv."+table.field_value.constituent_ID+"=fv"+k+"."+table.field_value.constituent_ID+" ";
    			where = where + " AND fv"+k+"."+table.field_value.value+" = ? AND fv"+k+"."+table.field_value.field_extra_ID+" = ? ";
     			param[2*k+static_params] = ancestors[k].value;
     			param[2*k+static_params+1] = ancestors[k].fieldID+"";
    			//where = where + "AND fv"+k+".value = ? AND fv"+k+".fieldID = ? AND fv"+k+".constituentID = ?";
     			//param[2*k+1] = ancestors[k].constituentID;
    		}
    		if(n_data.neighborhoodID>=0) {
    			where = where + " AND fv."+table.field_value.neighborhood_ID+" ISNULL ";
    			////where = " ( " + where + " ) OR fv.neighborhoodID = ? ";
    			////param[param.length-1] = ""+n_data.neighborhoodID;
    		}
    		sql = "SELECT " +
    				"fv."+table.constituent.name+
    				", fv."+table.constituent.forename+
    		        ", fv."+table.constituent.constituent_ID+
    		        ", fv."+table.constituent.external+
    		        ", fv."+table.constituent.global_constituent_ID +
    		        ", fv."+table.constituent.submitter_ID +
    		        ", fv."+table.constituent.slogan +
    		        ", fv."+table.constituent.email +
    				" FROM "+table.constituent.TNAME+" AS fv " +
    						
				tables+" WHERE fv."+table.constituent.organization_ID+" = ? "+where+" GROUP BY fv."+table.constituent.constituent_ID+";";
     		
    		if(DEBUG) System.err.print("F: populateIDs will select");
    		
    		identities = model.db.select(sql, param, DEBUG);
    		
    		
    		String sql_additional=
    			"SELECT " +
    			" fv."+table.constituent.name+
    			", fv."+table.constituent.forename+
    			", fv."+table.constituent.constituent_ID+
    			", fv."+table.constituent.external+
    			", fv."+table.constituent.global_constituent_ID +
		        ", fv."+table.constituent.submitter_ID +
		        ", fv."+table.constituent.slogan +
		        ", fv."+table.constituent.email +
   				" FROM "+table.constituent.TNAME+" as fv WHERE " +
    				table.constituent.neighborhood_ID+" = ?;";
    		//// using this instead of similar commented lines above
    		identities.addAll(
    				model.db.select(sql_additional, new String[]{""+n_data.neighborhoodID}));
    		if(DEBUG) System.err.print("F: populateIDs selected");
    	}catch(Exception e) {
    		JOptionPane.showMessageDialog(null,"populate: "+e.toString());
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
       		boolean external = "1".equals(Util.getString(identities_i.get(3)));
       		//boolean external = Util.ival(identities.get(i).get(3),-1);
       		long submitterID = Util.lval(identities_i.get(5),-1);
    		ConstituentData data = new ConstituentData();
    		data.global_constituentID = Util.sval(identities_i.get(4),null);
    		data.constituentID = Integer.parseInt(constituentID);
    		data.given_name = forename;
    		data.surname = name;
    		//data.inserted_by_me=(model.constituentID == external);
    		data.inserted_by_me=(model.getConstituentIDMyself() == submitterID);
    		data.external = external;
       		slogan = Util.getString(identities_i.get(6));
       		email = Util.getString(identities_i.get(7));
    		data.slogan = slogan;
    		data.email = email;
    		String submitter_ID = Util.getString(identities_i.get(5));
    		data.submitter_ID = submitter_ID;
    		if(DEBUG) System.err.print("F: populateIDs child");
    		addChild(new ConstituentsIDNode(model, this, data,"",null, next_ancestors),0);
    	}
    	if(resetCnt) {
    		if(identities.size()!=nchildren) setNChildren(identities.size());
    	}else{
    		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateIDs: nchildrens #"+nchildren);
    		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:populateIDs: identities #"+identities.size());
    	}
    	if(DEBUG) System.err.print("F: populateIDs fire");
    	model.fireTreeStructureChanged(new TreeModelEvent(model,getPath()));
    	if(DEBUG) System.err.print("F: populateIDs done");
    }
    void delNeighborhood(long nID) {
    	D_Neighborhood.delNeighborhood(nID, model.getOrganizationID());
    }
    void del(Object _child){
    	int constituentID=-1;
    	for (int i=0; i<children.length; i++) {
    		if(_child == children[i]) {
    			ConstituentsNode _children[] = new ConstituentsNode[children.length-1];
    			for(int j=0;j<i;j++)_children[j]=children[j];
    			for(int j=i+1;j<children.length;j++)_children[j-1]=children[j];
    			children=_children;
    			break;
    		}
    	}
    	setNChildren(nchildren-1);
    	try{
        	ArrayList<ArrayList<Object>> identities;
    		ConstituentsAddressNode child = (ConstituentsAddressNode)_child;
    		AddressAncestors[] ancestors=child.ancestors;
    		long cID = child.n_data.neighborhoodID;
    		if(cID>=0){
    			neighborhoods--;
    			delNeighborhood(cID);
    		}
    		String q="fv."+table.field_value.constituent_ID;
    		String g="";
    		identities=child.getDynamicNeighborhoods(q,g);
    		/*
    		String param[];
        	if(this.parent!=null){
        		String tables = " ";
        		String where = " ";
        		int static_params=4;
        		if(ancestors==null) {
        			if(DEBUG) System.err.print("Empty ancestors in "+this);
        			return;
        		}
        		param = new String[static_params+3*(ancestors.length)-1];
        		param[0] = ""+this.location.fieldID; //fv.fieldID>?
        		param[1] = ""+this.location.fieldID; //fv.fieldID_above=?
        		param[2] = ""+child.location.value; //f.value
        		param[3] = ""+child.location.fieldID; //f.fieldID
        		long fieldID_above = -1;
        		int par_idx = static_params;
        		for(int k=0; k < ancestors.length; k++) {
        			tables = tables + " JOIN field_value AS fv"+k+" ON fv.constituent_ID=fv"+k+".constituent_ID ";
        			where = where + " AND fv"+k+".value = ? AND fv"+k+".field_extra_ID = ? ";
         			param[par_idx++] = ancestors[k].value;
         			param[par_idx++] = ancestors[k].fieldID+"";
        			if(fieldID_above >=0) {
        				where = where + " AND fv"+k+".fieldID_above = ? ";
        				param[par_idx++] = ancestors[k].field_extra_ID+"";
        			}else{
        				where = where + " AND fv"+k+".fieldID_above ISNULL ";
        			}
        			fieldID_above = ancestors[k].field_extra_ID;
        		}
        		sql = "select fv.constituentID " +
         				" from field_value AS fv " +
         				//" JOIN field_value AS f ON f.constituent_ID = fv.constituent_ID " +
         				//" JOIN constituent AS c ON c.constituent_ID = fv.constituent_ID "+
         				tables+
         				" WHERE fv.field_extra_ID > ? AND fv.fieldID_above = ? " +
         				" AND fv.value = ? AND fv.field_extra_ID = ? "+where + 
         				";";
        	}else{
        		param = new String[3];
        		param[0]=model.organization_ID+"";
        		param[1]=child.location.field_extra_ID+"";
        		param[2]=child.location.value;
        		sql = "select fv.constituent_ID from field_value AS fv " +_
        				" JOIN constituent AS c ON c.constituent_ID=fv.constituentID " +
        				" WHERE c.organization_ID = ? AND " +
        				" fv.field_extra_ID = ? AND fv.fieldID_above ISNULL AND fv.value = ?;";
        	}
        	identities = model.db.select(sql, param);
        	*/
    		if(DEBUG) System.err.println("Records= "+identities.size());
        	for(int i=0; i<identities.size(); i++) {   		
        			constituentID = Util.ival(identities.get(i).get(0), -1);
        			D_Constituent.delConstituent(constituentID);
        	}
    	}catch(Exception e){
    		JOptionPane.showMessageDialog(null,"delete: "+e.toString());
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
    	children = new ConstituentsNode[0];
    	ArrayList<ArrayList<Object>> identities;
    	//ArrayList<ArrayList<Object>> nei=new ArrayList<ArrayList<Object>>();
    	ArrayList<Long> subnei = new ArrayList<Long>();
    	if (ancestors == null) {
    		if (DEBUG) System.err.print("ConstituentsModel:ConstituentsAddressNode:populateAddress: Empty ancestors in "+this);
    		return false;
    	}
     	try {
     		String query="fv."+table.field_value.value+", count(*), fe."+table.field_extra.tip+", fv."+table.field_value.field_extra_ID+", fe."+table.field_extra.partNeigh+", fv."+table.field_value.fieldID_above+", fv."+table.field_value.field_default_next+", fv."+table.field_value.neighborhood_ID+" ";
     		String group="GROUP BY fv."+table.field_value.value+" ORDER BY fv."+table.field_value.value+" DESC";
     		identities = getDynamicNeighborhoods(query,group);
    		if ((n_data != null) && (n_data.neighborhoodID >= 0)) {
    			subnei = D_Neighborhood.getNeighborhoodChildrenIDs(n_data.neighborhoodID);
    			/*
    			String neighborhoods_sql =
    				"SELECT "+table.neighborhood.name+", "+table.neighborhood.neighborhood_ID+
    				" FROM "+table.neighborhood.TNAME +
    				" WHERE "+table.neighborhood.organization_ID+" = ? AND "+table.neighborhood.parent_nID+" = ? " +
    						//" GROUP BY "+table.neighborhood.name+
    						" ORDER BY "+table.neighborhood.name+" DESC;";
    			nei = model.db.select(neighborhoods_sql,
    					new String[]{""+model.getOrganizationID(), ""+n_data.neighborhoodID}, DEBUG);
    			*/
    		}//else nei = null;
    		if (counting) {
    			int _nchildren=0;
    			if (subnei.size() == 0) {
    				_nchildren = identities.size();
    				neighborhoods = _nchildren;
    				if (_nchildren < 1) _nchildren = 1;
    				setNChildren(_nchildren);
    				return identities.size() > 0;
    			}
    			int n = 0;
    			//if(identities.size()==0) _nchildren = nei.size();
    			for (int i = 0; i < identities.size(); i ++) {
    				String value = Util.sval(identities.get(i).get(0),null);
    				_nchildren++;
    		    	for (; n < subnei.size(); n ++) {
    		    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(subnei.get(n), true, false);
    		    		String n_name = dn.getName(); // Util.sval(nei.get(n).get(0), "");
    		    		int cmp = value.compareToIgnoreCase(n_name);
    		    		if (cmp > 0) break; 
    		    		if (cmp < 0) _nchildren ++;
    		    	}    				
    			}
    			_nchildren += subnei.size()-n;
    			boolean hasChildren = _nchildren>0;
    			neighborhoods = _nchildren;
    			if(_nchildren<=0) _nchildren =1;
				setNChildren(_nchildren);
    			return hasChildren;
    		}
    	}catch(Exception e){
    		JOptionPane.showMessageDialog(null,"populate: "+e.toString());
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
		    		String n_name = dn.getName(); // Util.sval(nei.get(n).get(0), "");
		    		//String n_name = //Util.sval(nei.get(n).get(0), "");
		    		int cmp = value.compareToIgnoreCase(n_name);
		    		if(DEBUG) System.err.println("Neigh= "+n_name+" :"+cmp);
		    		if (cmp>0) break; 
		    		if (cmp<0) {
		    			long nID = subnei.get(n); //Util.lval(nei.get(n).get(1), -1);
		    			if(DEBUG) System.err.println("...nID = "+n_data.neighborhoodID);
		    			//NeighborhoodData nd=new NeighborhoodData(n_name, n_data.neighborhoodID, model.getOrganizationID());
		    			NeighborhoodData nd=new NeighborhoodData(nID, n_data.neighborhoodID, model.getOrganizationID());
		    			addChild(
		    					new ConstituentsAddressNode(model, this, nd, next_ancestors),
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
    		LocationData data = new LocationData();
    		data.value = value;
    		data.fieldID = Util.lval(fieldID,-1);
    		//if(data.fieldID <= location.fieldID) continue;
    		data.inhabitants = Util.ival(count,0);
    		data.organizationID = location.organizationID;
    		data.partNeigh = Util.ival(identities.get(i).get(4),0);
    		data.fieldID_above = Util.lval(identities.get(i).get(5), -1);
    		data.fieldID_default_next = Util.lval(identities.get(i).get(6), -1);
    		data.neighborhood = Util.ival(identities.get(i).get(7),0);
    		data.tip = tip;
    		populateChild(new ConstituentsAddressNode(model, this, data, "", null, 
    				next_ancestors, fieldIDs, next_level, n_data.neighborhoodID,null),0);
    		_nchildren++;
    	}
	    for (; n < subnei.size(); n++) {
    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(subnei.get(n), true, false);
    		String n_name = dn.getName(); // Util.sval(nei.get(n).get(0), "");
	   	    //String n_name = Util.sval(nei.get(n).get(0), "");
	    	long nID = subnei.get(n); //Util.lval(nei.get(n).get(1), -1);
	    	if(DEBUG) System.err.println("nID = "+n_data.neighborhoodID);
	    	NeighborhoodData nd=new NeighborhoodData(nID, n_data.neighborhoodID, model.getOrganizationID());
	    	//NeighborhoodData nd=new NeighborhoodData(n_name, n_data.neighborhoodID, model.getOrganizationID());
	    	addChild(new ConstituentsAddressNode(model, this, nd, next_ancestors), 0);
	    	_nchildren++;
	    }
    	//if(identities.size()!=nchildren) setNChildren(identities.size());
    	if(_nchildren!=nchildren) setNChildren(_nchildren);
    	Object[] mypath = getPath();
    	if(DEBUG) {
    		System.err.println("Found mypath = "+mypath.length);
    		for(int k=0; k<mypath.length; k++) System.err.println(k+": "+mypath[k]);
    	}
    	neighborhoods = _nchildren;
    	if(_nchildren == 0) {
    		return false;
    	}
    	model.fireTreeStructureChanged(new TreeModelEvent(model,getPath()));
    	return true;
    }
    // fv is field_values of next child, fe is field_extra 
    ArrayList<ArrayList<Object>> getDynamicNeighborhoods(String query, String group) throws P2PDDSQLException {
		if(DEBUG) System.err.println("ConstituentsModel:ConstituentsAddressNode:getDynamicNeighborhoods: start");
    	ArrayList<ArrayList<Object>> identities;
    	String tables = " ";
    	String where = " ";
    	int static_params=5;
    	String param[] = new String[static_params+2*(ancestors.length)];
       	param[0] = ""+model.getOrganizationID();
       	param[1] = ""+location.fieldID;
       	param[2] = ""+location.fieldID;
       	param[3] = ""+location.value;
       	param[4] = ""+location.fieldID;
       	for(int k=0; k < ancestors.length; k++) {
       		tables = tables + " JOIN "+table.field_value.TNAME+" AS fv"+k+" ON fv."+table.field_value.constituent_ID+"=fv"+k+"."+table.field_value.constituent_ID+" ";
       		where = where + " AND fv"+k+"."+table.field_value.value+" = ? AND fv"+k+"."+table.field_value.field_extra_ID+" = ? ";
       		param[2*k+static_params] = ancestors[k].value;
       		param[2*k+1+static_params] = ancestors[k].fieldID+"";
       	}
       	sql = "select "+query+
       	" from "+table.field_value.TNAME+" AS fv " +
       	" JOIN "+table.constituent.TNAME+" AS c ON c."+table.constituent.constituent_ID+"=fv."+table.field_value.constituent_ID +
       	" JOIN "+table.field_value.TNAME+" AS f ON f."+table.field_value.constituent_ID+"=fv."+table.field_value.constituent_ID +
       	tables+
       	" JOIN "+table.field_extra.TNAME+" AS fe ON fe."+table.field_extra.field_extra_ID+" = fv."+table.field_value.field_extra_ID +
       	" WHERE c."+table.constituent.organization_ID+" = ?" +
       	" AND fv."+table.field_value.field_extra_ID+" > ? AND fv."+table.field_value.fieldID_above+" = ? AND f."+table.field_value.value+" = ? AND f."+table.field_value.field_extra_ID+" = ? "+
       	where + 
       	" "+group+";";
       	identities = model.db.select(sql, param);
       	return identities;
    }

    /**
     * Return language string for instances of children nodes using this value
     * @return
     */
	public String getValueLanguage() {
		if(n_data==null) return "en";
		if(n_data.name_lang==null) return "en";
		return n_data.name_lang.lang;
	}

	public void advertise(ConstituentsTree tree) {
		String hash = D_Constituent.getGIDHashFromGID(this.n_data.global_nID);
		String org_hash = tree.getModel().getOrganization().global_organization_IDhash;
	//	String org_hash = D_Organization.getOrgGIDHashGuess(tree.getModel().getOrgGID());
		ClientSync.addToPayloadFix(streaming.RequestData.NEIG, hash, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
	}

	public void block(ConstituentsTree tree) {
		try {
			D_Neighborhood dn = D_Neighborhood.getNeighByLID(this.n_data.neighborhoodID, true, true);
			dn.setBlocked(! dn.isBlocked());
			this.n_data.blocked = dn.isBlocked();
			dn.storeRequest();
			dn.releaseReference();
			//boolean blocked = D_Neighborhood.toggleBlock(this.n_data.neighborhoodID);
			//this.n_data.blocked = blocked;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
			//boolean broadcast = D_Neighborhood.toggleBroadcast(this.n_data.neighborhoodID);
	public void broadcast(ConstituentsTree tree) {
		try {
			D_Neighborhood dn = D_Neighborhood.getNeighByLID(this.n_data.neighborhoodID, true, true);
			dn.setBroadcasted(! dn.isBroadcasted());
			this.n_data.broadcasted = dn.isBroadcasted();
			dn.storeRequest();
			dn.releaseReference();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	public void zapp(ConstituentsTree tree) {
		D_Neighborhood.zapp(this.n_data.neighborhoodID);
	}
}
public class ConstituentsModel extends TreeModelSupport implements TreeModel, DBListener {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	ConstituentsAddressNode root;
	DBInterface db;
	boolean automatic_refresh = DD.DEFAULT_AUTO_CONSTITUENTS_REFRESH;
	long[] fieldIDs;
	private long organizationID;
	private D_Organization crt_org;
	
	private long my_constituentID=-1;
	private String my_global_constituentID=null;
	String subdivisions;
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
		return D_Constituent.getConstByLID(getConstituentIDMyself(), false, true);
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
		D_Constituent cons = D_Constituent.getConstByLID(getConstituentIDMyself(), true, false);
		if (cons == null) return __("None");
		return cons.getSurName();
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
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.constituent.TNAME, table.witness.TNAME, table.neighborhood.TNAME, table.field_value.TNAME)), null);
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
		
		if ((c.neighborhood==null) || (c.neighborhood.length == 0)) {
			if(DEBUG) System.err.println("ConstituentsModel:expandConstituentID root constituent="+c);
			return null;
		}
		ConstituentsAddressNode n = expandNeighborhoodID(tree, root, c.neighborhood);
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
		return expandNeighborhoodID(tree, root, neighborhood);
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
		Object model_root = model.root;
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
			if(DEBUG) System.err.println("ConstituentsModel:refresh  Abandoned no root: "+root);
			return;
		}
		ConstituentsAddressNode old_root = (ConstituentsAddressNode)_old_root;
		//init(organizationID2, _constituentID, _global_constituentID);
		//if(old_root==null) return;
		for(JTree tree: trees)
			translate_expansion(tree, old_root, root);
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
		
		for(int k=0; k < old_root.children.length; k++) {
			ConstituentsNode cb =	old_root.children[k];
			if(cb instanceof ConstituentsAddressNode) {
				if(!(new_root instanceof ConstituentsAddressNode)){
					if(DEBUG) System.err.println("ConstituentsModel:translate_expansion stop new root not address parent");
					continue;
				}
				
				ConstituentsAddressNode o_can = (ConstituentsAddressNode) cb;
				ConstituentsAddressNode n_can = (ConstituentsAddressNode) new_root;
				long neighborhoodID = o_can.n_data.neighborhoodID;
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
		
		
		root = null;
		fieldIDs = null;
		subdivisions = null;
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
		if(organizationID<=0) return;
		crt_org = org;
		if(crt_org==null)
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
		subdivisions = Util.getString(subdivision_fields.get(0));
		fieldIDs = (long[]) subdivision_fields.get(1);
		if (fieldIDs.length > 0) hasNeighborhoods = true;
		
		root = new ConstituentsAddressNode(this,null,null,"",null,null,fieldIDs,0,-2,null);
		
		
			//String sql = "select value, COUNT(*) from field_value where field_extra_ID = ? GROUP BY value;";
		if ( fieldIDs.length > 0 ) {
			subneighborhoods = D_Constituent.getRootConstValues(fieldIDs[0], organizationID);
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
		    			NeighborhoodData nd=new NeighborhoodData(nID, -1, organizationID);
		    			nd.neighborhoodID = nID;
		    			root.addChild(new ConstituentsAddressNode(this, root, nd, new AddressAncestors[0]),0);
		    		}
		    	}
		    }
		    obj = subneighborhoods.get(i).get(1);
		    if(obj!=null) fieldID = obj.toString(); else fieldID = "-1";
		    obj = subneighborhoods.get(i).get(2);
		    if(obj!=null) count = obj.toString(); else count = null;
		    if(DEBUG) System.err.println(i+" Got: v="+value+" c="+count+" fID="+fieldID);
		    LocationData data=new LocationData();
		    data.value = value;
		    data.fieldID = Long.parseLong(fieldID);
		    data.inhabitants = Integer.parseInt(count);
		    data.tip = (String)subneighborhoods.get(i).get(3);
		    data.partNeigh = Util.ival(subneighborhoods.get(i).get(4),0);
		    data.fieldID_above = Util.lval(subneighborhoods.get(i).get(5),-1);
		    data.fieldID_default_next = Util.lval(subneighborhoods.get(i).get(6),-1);
		    data.neighborhood = Util.ival(subneighborhoods.get(i).get(4),0);
		    root.addChild(
		    		new ConstituentsAddressNode(this, root,
		    				data,
		    				"", null,
		    				new AddressAncestors[0],
		    				fieldIDs,0, -1,null), 
		    			0);
		}
	    for (; n < neighborhood_branch_obj.size(); n++) {
    		D_Neighborhood dn = D_Neighborhood.getNeighByLID(neighborhood_branch_obj.get(n), true, false);
    		String n_name = dn.getName(); // Util.sval(neighborhood_branch_objects.get(n).get(0), "");
	    	long nID = neighborhood_branch_obj.get(n); // Util.lval(neighborhood_branch_objects.get(n).get(1), -1);
	    	NeighborhoodData nd=new NeighborhoodData(nID, -1, organizationID);
	    	//NeighborhoodData nd=new NeighborhoodData(n_name, -1, organizationID);
	    	nd.neighborhoodID = nID;
	    	root.addChild(new ConstituentsAddressNode(this, root, nd, new AddressAncestors[0]), 0);
	    }
	    if (fieldIDs.length == 0) root.populateIDs();
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
			name = Util.getString(c.getSurName(),__("Unknown Yet"));
			forename = c.getForename(); //Util.getString(identities_i.get(1));
			//constituentID = c.getConstituentIDstr(); //""+Util.lval(identities_i.get(2), -1);
			boolean external = c.isExternal(); //"1".equals(Util.getString(identities_i.get(3)));
			//boolean external = Util.ival(identities.get(i).get(3),-1);
			long submitterID = c.getSubmitterLID(); //Util.lval(identities_i.get(5),-1);
			ConstituentData data = new ConstituentData();
			data.constituent = c;
			data.global_constituentID = c.getGID(); ////Util.sval(identities_i.get(4),null);
			data.constituentID = c.getLID(); //Integer.parseInt(constituentID);
			data.given_name = forename;
			data.surname = name;
			//data.inserted_by_me=(model.constituentID == external);
			data.inserted_by_me=((getConstituentIDMyself() == submitterID)&&(getConstituentIDMyself()>=0));
			data.external = external;
			slogan = c.getSlogan(); //Util.getString(identities_i.get(6));
			email = c.getEmail(); //Util.getString(identities_i.get(7));
			data.slogan = slogan;
			data.email = email;
			String submitter_ID = c.getSubmitterLIDstr(); // Util.getString(identities_i.get(5));
			data.submitter_ID = submitter_ID;
			if (DEBUG) System.err.print("ConstituentsModel: populateOrphans child");
			root.populateChild(new ConstituentsIDNode(this, root, data,"",null, root.next_ancestors),0);
		}
		// if(identities.size()!=root.nchildren)
		root.setNChildren(orphans.size()+root.nchildren);
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
    public Object	getRoot() {
    	return root;
    }
    public boolean	isLeaf(Object node) {
    	if (node instanceof ConstituentsPropertyNode) return true;
    	if (node instanceof ConstituentsBranch)
    	    return (((ConstituentsBranch)node).nchildren==0);
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
    	NeighborhoodData nd = (NeighborhoodData)newValue; // the new data to be saved
    	if (DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: old edited neigh_ID="+nd.neighborhoodID);
     	try {
    		NeighborhoodData ndo = neigh.n_data; // the edited data node
        	if (DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: old neigh_ID="+ndo.neighborhoodID);
    		if (ndo.global_nID == null) HARD_SAVE = true;
    		else{
    			Application_GUI.warning(__("Cannot change data of signed neighborhood! Create a new one"),__("Cannot change!"));
    			return;
    		}
    		ndo.name = nd.name;
    		neigh.location.value = nd.name;
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
    		int idx = neigh.parent.getIndexOfChild(neigh);
    		this.fireTreeNodesChanged(new TreeModelEvent(this,path.getParentPath(),new int[]{idx},new Object[]{neigh}));
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	if(DEBUG) System.err.println("ConstitentsModel:valueForPathChanged: exit");
    }
	public boolean setCurrentConstituent(long _constituentID, ConstituentsTree tree) {
    	if(DEBUG) System.err.println("ConstitentsModel:setCurrentConstituent: set "+_constituentID);
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
    	if(DEBUG) System.err.println("ConstitentsModel:setCurrentConstituent: Done");
    	return true;
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if(this.automatic_refresh){
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
	    census = new ConstituentsCensus(this, root);
	    census_value = 0;
	    census.start();
		if(DEBUG) System.err.println("ConstituentsModel:startCensus: done");
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
		census = new ConstituentsCensus(this, (ConstituentsAddressNode)can.parent);
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
		return crt_org.global_organization_ID;
	}
	public void enableRefresh() {
		this.refreshListener.enableRefresh();
	}
}
