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
 package net.ddp2p.widgets.identities;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.components.TreeModelSupport;
import net.ddp2p.widgets.org.Orgs;
import java.util.*;
import static net.ddp2p.common.util.Util.__;
class IdentityNode {
    protected static final boolean DEBUG = false;
	String name = __("Root");
    String tip = __("Root Tip");
    public IdentityNode(String _name, String _tip) {
    	name = _name;
    	tip = _tip;
    }
    public String toString() {
    	return name;
    }
    public String getTip() {
    	return tip;
    }
    public void add() {}
    public void del(Object child) {
    	if(DEBUG) System.err.println("Deleting this leaf!"+child);
    }
}
public class MyIdentitiesModel extends TreeModelSupport implements TreeModel {
    private static final boolean _DEBUG = true;
    private static final boolean DEBUG = false;
	IdentityBranch root = new IdentityBranch(this, __("List identities using this machine."));
    DBInterface db;
    public MyIdentitiesModel(DBInterface _db) {
    	db = _db;
    	if(db == null) {
    		JOptionPane.showMessageDialog(null,__("No database in Model!"));
    		return;
    	}
    	ArrayList<ArrayList<Object>> identities;
    	try {
    		identities = db.select(Queries.sql_identity_fertility_nodes, new String[0]);
    	}catch(Exception e){
    		JOptionPane.showMessageDialog(null,e.toString());
    		e.printStackTrace();
    		return;
    	}
    	for(int i=0; i<identities.size(); i++) {
    		String value;
    		Object obj = identities.get(i).get(1);
    		if(obj!=null) value = obj.toString();
    		else value = "NULL";
    		long identityID = Util.get_long(identities.get(i).get(Queries.IFN_IDENTITY));
    		int children = Util.get_int(identities.get(i).get(Queries.IFN_COUNT));
    		boolean default_id = Util.get_int(identities.get(i).get(Queries.IFN_DEFAULT))>0;
       		String secret_key = Util.getString(identities.get(i).get(Queries.IFN_SK));
       		String pk_hash = Util.getString(identities.get(i).get(Queries.IFN_S_CRED));
    		SK keys = null;
    		Cipher cipher = null;
    		if(secret_key != null){
    			keys = Cipher.getSK(secret_key);
    			cipher = Cipher.getCipher(keys, keys.getPK());
    		}try{
    			if(children == 1)
    				children=db.select(Queries.sql_identity_fertility_node, new String[]{""+identityID}).size();
    		}catch(Exception e){
    			JOptionPane.showMessageDialog(null,"Trouble with databse:"+e);
    			e.printStackTrace();
    		}
    		String params[]= new String[1];
    		params[0]=new String(""+identityID);
    		IdentityBranch ib = new IdentityBranch(this,value,
    				children,identityID,
    				default_id,__("This is the name of the profile"),
    				Queries.sql_identity_enum_leafs, params, cipher, keys, pk_hash);
    		root.addChild(ib, 0);
    		if(DEBUG) System.err.println("identity br "+value);
    	}
    }
    /**
     * Sets a given branch as default
     * @param tree (an instance of the identities tree)
     * @param ib (a branch object from the Identities tree)
     * @throws P2PDDSQLException
     */
    public static void setAnIdentityCurrent(MyIdentitiesTree tree, IdentityBranch ib) throws P2PDDSQLException {
    	if(DEBUG) System.out.println("MyIdentitiesModel:setDefaultIdentityCurrent: "+ib);
    	if(ib==null) return;
    	String sql = sql_identities_view +
    	" WHERE "+net.ddp2p.common.table.identity.default_id+"=?;";
    	ArrayList<ArrayList<Object>> sel = Application.getDB().select(sql, new String[]{ib.identityID+""}, DEBUG);
    	if (sel.size()<1) return;
    	setCurrent(sel.get(0), tree, ib);
    }
    public static String sql_identities_view =
    	"SELECT i."+net.ddp2p.common.table.identity.organization_ID + ", i."+net.ddp2p.common.table.identity.constituent_ID 
    	+ ", i."+net.ddp2p.common.table.identity.identity_ID +", i."+net.ddp2p.common.table.identity.authorship_lang +", i."+net.ddp2p.common.table.identity.authorship_charset +
    	"  FROM "+net.ddp2p.common.table.identity.TNAME+" AS i"
    	;
    /**
     * Sets as current a branch and the corresponding data in se;
     * @param sel selected by sql_identities_view
     * @param tree
     * @param ib
     */
    public static void setCurrent(ArrayList<Object> sel, MyIdentitiesTree tree, IdentityBranch ib){
    	MyIdentitiesModel model = (MyIdentitiesModel)tree.getModel();
    	Identity newID = new Identity();
    	String orgID =  Util.getString(sel.get(0));
    	if (orgID != null) newID.organizationGID = D_Organization.getGIDbyLIDstr(orgID); 
       	String consID = Util.getString(sel.get(1));
       	if (consID != null) newID.setConstituentGID(D_Constituent.getGIDFromLID(consID)); 
       	newID.identity_id=Util.getString(sel.get(2));
    	newID.authorship_lang = Util.getString(sel.get(3));
    	newID.authorship_charset = Util.getString(sel.get(4));
    	if(Identity.current_id_branch == ib) return;
    	if(Identity.current_id_branch!=null){
    		IdentityBranch oib = (IdentityBranch)Identity.current_id_branch;
    		Identity.current_id_branch = null;
    		model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(oib)},new Object[]{oib}));
    	}
    	Identity.current_id_branch = ib;
    	Orgs ao = GUI_Swing.orgs;
    	if ((newID.identity_id!=null)&&(ao != null)) {
    		try{
    			long id = new Integer(newID.identity_id).longValue();
    			ao.setCurrent(id);
    		}catch(Exception e){e.printStackTrace();}
    	}
    	Identity.setCurrentConstituentIdentity(newID);
    }
    public Object	getChild(Object parent, int index) {	
    	if (! (parent instanceof IdentityBranch)) return -1;
    	IdentityBranch ibParent = (IdentityBranch)parent;
    	return ibParent.getChild(index);
    }
    public int	getChildCount(Object parent) {
    	if (! (parent instanceof IdentityBranch)) return -1;
    	IdentityBranch ibParent = (IdentityBranch)parent;
    	return ibParent.getChildCount();
    }
    public int	getIndexOfChild(Object parent, Object child) {
    	if (! (parent instanceof IdentityBranch)) return -1;
    	IdentityBranch ibParent = (IdentityBranch)parent;
    	return ibParent.getIndexOfChild(child);
    }
    public Object	getRoot() {
    	return root;
    }
    public boolean	isLeaf(Object node) {
    	if (node instanceof IdentityLeaf) return true;
    	if (node instanceof IdentityBranch)
    		return (((IdentityBranch)node).nchildren==0);
	return false;
    }
    public void	valueForPathChanged(TreePath path, Object newValue) {
    	if(DEBUG) System.err.println("valueForPathChanged: "+path+" = "+newValue);
    	if(newValue == null) {
    		return;
    	}
    	MyIdentityData data = (MyIdentityData)newValue;
    	Object o=path.getLastPathComponent();
    	if(o instanceof IdentityBranch) {
     		IdentityBranch ib = (IdentityBranch)o;
    		if(!ib.name.equals(data.value)) {
    			ib.save(this, data.value);
    		}
    	}else{
    		if(o instanceof IdentityLeaf) {
    			IdentityLeaf il = (IdentityLeaf)o;
    			if(DEBUG) System.err.println("valueForPathChanged:"+path+"="+newValue+" ("+il+")");
    			il.save(this, data.value, data.OID, data.certificate, data.explain, data.OID_name);
    		}
    	}
    }
}
