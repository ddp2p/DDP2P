/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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
package widgets.identities;

import static util.Util.__;
import hds.GenerateKeys;

import java.text.MessageFormat;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.event.TreeModelEvent;

import util.Util;
import ciphersuits.Cipher;
import ciphersuits.SK;
import util.P2PDDSQLException;
import config.Application;
import config.Identity;
import data.D_Constituent;
import data.D_Organization;

public class IdentityBranch extends IdentityNode{
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	MyIdentitiesModel model;
    Object children[] = new Object[0];
    int nchildren;
    long identityID = -1;
    String sql = "";
    boolean default_id;
    private SK keys;
    String sql_params[] = new String[0];
    String globalID, globalOrgID, c_name, c_forename, org_name;
	public String pk_hash; // hash of the public key
	private Cipher cipher;
	private String c_name_my;
    public IdentityBranch(MyIdentitiesModel _model, String _tip) {
	super(__("Root"), _tip);
		model = _model;
    }
    public void save(MyIdentitiesModel model, String name) {
    	try {
    		model.db.update(table.identity.TNAME,
    				new String[]{table.identity.profile_name},
    				new String[]{table.identity_value.identity_ID},
    				new String[]{name, ""+identityID}
    				);
    	}catch(Exception e){}
		this.name = name;
    }
    /**
     * Calapse this
     */
    public void colapsed() {
    	if(DEBUG) System.err.println("colapsed: "+name);
    	if(this!=model.root)children = new Object[0];
    }
    public IdentityBranch(MyIdentitiesModel _model, String _name, int _nchildren, long _identityID, 
    		boolean _default_id, String _tip, String _sql, String _sql_params[],Cipher _cipher, SK _keys, String _pk_hash) {
    	super(_name, _tip);
    	setKeys(_cipher,_keys, _pk_hash);
		model = _model;
		nchildren = _nchildren;
		identityID=_identityID;
		sql = _sql;
		default_id = _default_id;
		if(default_id){
			if(Identity.default_id_branch!=null){
				IdentityBranch oib = (IdentityBranch)Identity.default_id_branch;
				oib.default_id = false;
				Identity.default_id_branch = null;
		    	//model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(oib)},new Object[]{oib}));
			}			
			Identity.default_id_branch = this;
		}
		if(_sql_params!=null) sql_params = _sql_params;

    	ArrayList<ArrayList<Object>> id;
    	try {
    		/*
    		String _sql = "SELECT c."+table.constituent.global_constituent_ID+", o."+table.organization.global_organization_ID+
					", o."+table.organization.name+", c."+table.constituent.name+", c."+table.constituent.forename +
					" FROM "+table.identity.TNAME+" AS i" +
					" LEFT JOIN "+table.constituent.TNAME+" AS c ON (i."+table.identity.constituent_ID+" == c."+table.constituent.constituent_ID+")" +
					" LEFT JOIN "+table.organization.TNAME+" AS o ON (o."+table.organization.organization_ID+" == i."+table.identity.organization_ID+")" +
					" WHERE i."+table.identity.identity_ID+"==? LIMIT 1;";
			*/
    		String sql = "SELECT i."+table.identity.constituent_ID + ", i." + table.identity.organization_ID +
					" FROM "+table.identity.TNAME+" AS i" +
					" WHERE i."+table.identity.identity_ID+"==? LIMIT 1;";
			id = Application.db.select(sql,
					new String[]{identityID+""});
			if (id.size() == 0) {
				if (DEBUG) System.err.println("No default identity found!");
				return;
			}
			long _cID = Util.lval(id.get(0).get(0));
			long _oID = Util.lval(id.get(0).get(1));
			
			D_Organization org = null;
			if (_oID > 0) org = D_Organization.getOrgByLID_NoKeep(_oID, true);
			D_Constituent cons = null;
			if (_cID > 0) cons = D_Constituent.getConstByLID(_cID, true, false);
			if (cons != null) {			
				globalID = cons.getGID(); //(String) id.get(0).get(0);
				globalOrgID = org.getGID(); //(String) id.get(0).get(1);
				org_name = org.getOrgNameOrMy(); //(String) id.get(0).get(2);
				c_name = cons.getSurName(); //(String) id.get(0).get(3);
				c_forename = cons.getForename(); //(String) id.get(0).get(4);
				c_name_my = cons.getNameOrMy();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		
    }
    void setNChildren(int _nchildren) {
    	nchildren = _nchildren;
    	if(DEBUG) System.err.println("#children:"+nchildren+" for "+this);
    	if((this != model.root)&&(model.root.nchildren == model.root.children.length)) {
    		model.fireTreeNodesChanged(new TreeModelEvent(model, new Object[]{model.root}, new int[]{model.root.getIndexOfChild(this)},new Object[]{this}));
    	}
    }
    void populateChild(Object child, int index) {
    	//boolean DEBUG=true;
    	if(DEBUG) System.err.println("IdentityBranch:populateChild:addChild "+child+" to "+name+"["+index+"]");
    	Object _children[] = new Object[children.length+1];
    	if(DEBUG) System.err.println("IdentityBranch:populateChild:_cl="+_children.length+" cl="+children.length);
    	if((index<0) || (index>children.length)) return;
    	for(int i=0; i<index; i++)	_children[i] = children[i];
    	_children[index] = child;
    	for(int i=index; i<children.length;i++) _children[i+1] = children[i];
    	if(DEBUG) System.err.println("IdentityBranch:populateChild:_cl="+_children.length+" cl="+children.length);
    	children = _children;
    	if(DEBUG) System.err.println("IdentityBranch:populateChild:added to "+name+", nc="+nchildren);
    	if(nchildren < children.length) 
    		if(DEBUG) System.err.println("IdentityBranch:populateChild:Will have to adjust children! "+nchildren+" < "+children.length);
    }
    void addChild(Object child, int index) {
    	populateChild(child, index);
    	setNChildren(nchildren+1);
    }
    public void del(Object child) {
    	for (int i=0; i<children.length; i++) {
    		if(child == children[i]) {
    			Object _children[] = new Object[children.length-1];
    			for(int j=0;j<i;j++)_children[j]=children[j];
    			for(int j=i+1;j<children.length;j++)_children[j-1]=children[j];
    			children=_children;
    			break;
    		}
    	}
    	setNChildren(nchildren-1);
	
    	try{
    		if(child instanceof IdentityLeaf) {
    			long id = ((IdentityLeaf)child).id;
    			model.db.delete(table.identity_value.TNAME,new String[]{table.identity_value.identity_value_ID},new String[]{""+id});
    		}else{
    			long child_idID=((IdentityBranch)child).identityID;
    			model.db.delete(table.identity_value.TNAME,new String[]{table.identity_value.identity_ID},new String[]{""+child_idID});
    			model.db.delete(table.identity.TNAME,new String[]{table.identity.identity_ID},new String[]{""+child_idID});
    		}
    	}catch(Exception e){
    		JOptionPane.showMessageDialog(null,__("Error deleting child:")+e.toString());
    		e.printStackTrace();
    	}
    }
    
    int getIndexOfChild(Object child) {
    	if(DEBUG) System.err.println("getIndexofChild "+child);
    	for(int i=0; i<children.length; i++){
    		if(children[i]==child) return i;
    		if(child instanceof IdentityBranch) {
    			if(((IdentityBranch)children[i]).identityID==((IdentityBranch)child).identityID) return i;
    		}else
    			if(child instanceof IdentityLeaf) {
    				if(((IdentityLeaf)children[i]).id==((IdentityLeaf)child).id) return i;
    			}
    	}
    	return -1;
    }
    int getChildCount() {
    	//System.err.println("#children of "+name+" ="+nchildren);
    	return nchildren;
	    //children.length;
    }
    Object getChild(int index){
    	if(index<0) return null;
    	//System.err.println("getChild of "+name+"["+index+"]");
    	if(children.length == 0) {
    		if(DEBUG) System.err.println("populating");
    		if(this !=model.root)populate();
    		if(DEBUG) System.err.println("populated, #="+children.length);
    	}
    	if(index < children.length) {
    		//System.err.println("getChild of "+name+"["+index+"]="+children[index]);
    		return children[index];
    	}
    	if(DEBUG) System.err.println("getChild of "+name+"["+index+"]=no item");
    	return __("No Item");
    }
    
    void populate() {
    	//boolean DEBUG=true;
		if(DEBUG) System.err.println("IdentityBranch:populate: start");
    	children = new Object[0]; //to avoid duplication
    	ArrayList<ArrayList<Object>> identities;
    	try {
    		identities = model.db.select(sql, sql_params, DEBUG);
    	}catch(Exception e){
    		JOptionPane.showMessageDialog(null,"populate:"+e.toString());
    		e.printStackTrace();
    		return;
    	}
    	for(int i=0; i<identities.size(); i++) {
    		String value, oid, oid_name, iv_ID;
    		Object obj = identities.get(i).get(2);
    		if(obj!=null) value = obj.toString();else value = "NULL";
    		Object o_oid = identities.get(i).get(0);
    		if(o_oid!=null) oid = o_oid.toString();else oid = "NULL";
    		Object o_oid_name = identities.get(i).get(1);
    		if(o_oid_name!=null) oid_name = o_oid_name.toString();else oid_name = null;
    		Object o_iv_ID = identities.get(i).get(3);
    		if(o_iv_ID!=null) iv_ID = o_iv_ID.toString();else iv_ID = "NULL";	
    		Object o_explain= identities.get(i).get(5);
    		String explain=(o_explain==null)?null:(String)o_explain;
       		long seq= Util.lval(identities.get(i).get(6), 0);
     		// MessageFormat.format(_("{0} ({1})"),snew Object[]{oid_name,oid})
    		populateChild(new IdentityLeaf(value, oid, oid_name,explain, new Integer(iv_ID).intValue(), seq, this), children.length);
    		if(DEBUG) System.err.println("IdentityBranch:populate: identity lf "+value);
    	}
    	if(identities.size()!=nchildren) setNChildren(identities.size());
    	model.fireTreeStructureChanged(new TreeModelEvent(model,new Object[]{model.root,this}));
    }
    public String toString() {
    	String result;
    	if(nchildren>0)
    		result =  MessageFormat.format(__("{0} ({1} properties)"), new Object[]{name,nchildren});
    	else
    		result = name;//+" #"+identityID;
    	if(org_name!=null)
    		result += "<font color='green'> - "+org_name+"</font>";

    	/*
    	if((c_name!=null) && (c_forename!=null))
    		result += " alias: "+c_name+", "+c_forename;
    	else{
        	if((c_name!=null))
        		result += " alias: "+c_name;
        	if((c_forename!=null))
        		result += " alias: "+c_forename;    		
    	}
    	*/
    	result += " alias: "+c_name_my;
    	
    	if(this==Identity.current_id_branch) 
    			result = "<html><font color='purple'>"+result+"</font></html>";
    	else if(default_id) 
    			result = "<html><font color='red'>"+result+"</font></html>";
    	else if(this.getKeys()==null){
    		result = "<html><font color='#DDA0DD'>"+result+"</font></html>";
    		//this.tip = this.tip+" No Key!";
    	}
    	if(DEBUG) System.out.println("IdentityBranch: "+result);
    	return result;
    }
    public String getTip() {
    	String result;
    	result = this.tip;
    	if(this==Identity.current_id_branch) result = result + " " + __("Current!");
    	if(default_id) result = result + " " + __("Default!");
    	if (this.getKeys()==null) result = result + " " + __("No Keys!");
    	return result;
    }
	public String convertValueToText() {
	    return toString();
	}
	/**
	 * A worker generated a new key and saved it in keys table
	 */
	public void updateKey(GenerateKeys gk) {
		try {
			model.db.update(table.identity.TNAME, new String[]{table.identity.secret_credential},
					new String[]{table.identity.identity_ID},
					new String[]{gk.gIDhash, this.identityID+""});
			setKeys(gk.keys, gk.keys.getSK(), gk.gIDhash);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void setKeys(Cipher cipher, SK keys, String _pk_hash) {
		this.cipher = cipher;
		this.keys = keys;
		this.pk_hash = _pk_hash;
	}
	public SK getKeys() {
		return keys;
	}
	public Cipher getCipher() {
		return cipher;
	}
	/*
	public void setKeys(Cipher _keys, SK _sk) {
		this.cipher = _keys;
		this.keys = _sk;
		this.pk_hash = Util.getKeyedIDPKhash(_keys);
	}
	*/
}
