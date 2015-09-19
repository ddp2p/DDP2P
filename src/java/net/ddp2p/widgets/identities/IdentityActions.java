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
 package net.ddp2p.widgets.identities;
import java.awt.event.*;
import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.hds.GenerateKeys;
import net.ddp2p.common.hds.WorkerListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.components.DebateDecideAction;
import static net.ddp2p.common.util.Util.__;

@SuppressWarnings("serial")
class IdentitySetCurrentAction extends DebateDecideAction {
    MyIdentitiesTree tree;ImageIcon icon;
    public IdentitySetCurrentAction(MyIdentitiesTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
	this.tree = tree;this.icon = icon;
    }
    
    public void actionPerformed(ActionEvent e) {
    	MyIdentitiesModel model = (MyIdentitiesModel)tree.getModel();
            //System.err.println("Set Default action: " + e);
    	TreePath tp=tree.getLeadSelectionPath();
    	Object source = tp.getLastPathComponent();
    	if(source instanceof IdentityBranch) {
    	    IdentityBranch ib = (IdentityBranch)source;
    	    if(ib.getKeys()==null) {
    	    	Application_GUI.warning(__("No keys!"), __("No keys available!"));
    	    	return;
    	    }
    	    long identitiesID=ib.identityID;
    	    try {
    	    	ArrayList<ArrayList<Object>> sel=
				model.db.select(MyIdentitiesModel.sql_identities_view+
						" WHERE "+net.ddp2p.common.table.identity_value.identity_ID+"=?;", 
						new String[]{""+identitiesID});
    	    	if(sel.size()<1) return;
    	    	MyIdentitiesModel.setCurrent(sel.get(0), tree, ib);
		    	model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(ib)},new Object[]{ib}));				
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
    }	
}

@SuppressWarnings("serial")
class IdentityUnSetCurrentAction extends DebateDecideAction {
    MyIdentitiesTree tree;ImageIcon icon;
    public IdentityUnSetCurrentAction(MyIdentitiesTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
	this.tree = tree;this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	MyIdentitiesModel model = (MyIdentitiesModel)tree.getModel();
            //System.err.println("Set Default action: " + e);
    	TreePath tp=tree.getLeadSelectionPath();
    	Object source = tp.getLastPathComponent();
    	if(source instanceof IdentityBranch) {
			if(Identity.current_id_branch!=null){
				IdentityBranch oib = (IdentityBranch)Identity.current_id_branch;
				Identity.current_id_branch = null;
		    	model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(oib)},new Object[]{oib}));
			}
    	}
    }	
}
@SuppressWarnings("serial")
class IdentitySetDefAction extends DebateDecideAction {
    MyIdentitiesTree tree;ImageIcon icon;
    public IdentitySetDefAction(MyIdentitiesTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
	this.tree = tree;this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	MyIdentitiesModel model = (MyIdentitiesModel)tree.getModel();
            //System.err.println("Set Default action: " + e);
    	TreePath tp=tree.getLeadSelectionPath();
    	Object source = tp.getLastPathComponent();
    	if(source instanceof IdentityBranch) {
    		setDefault((IdentityBranch)source, model, tree);
    	}
    }
    /**
     * Set default and current
     * @param source
     * @param model
     * @param tree
     */
    public static void setDefault(IdentityBranch source, MyIdentitiesModel model, MyIdentitiesTree tree){
    	 IdentityBranch ib = source;
    	 long identitiesID=ib.identityID;
    	 try {
				model.db.update(net.ddp2p.common.table.identity.TNAME, new String[]{net.ddp2p.common.table.identity.default_id}, new String[]{},
						new String[]{"0"});
				if(Identity.default_id_branch!=null){
					IdentityBranch oib = (IdentityBranch)Identity.default_id_branch;
					oib.default_id = false;
					Identity.default_id_branch = null;
					int old_idx = model.root.getIndexOfChild(oib);
					if(old_idx >= 0)
						try{
							model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{old_idx}, new Object[]{oib}));
						}catch(Exception e){//e.printStackTrace();
						}
				}
				model.db.update(net.ddp2p.common.table.identity.TNAME, new String[]{net.ddp2p.common.table.identity.default_id}, new String[]{net.ddp2p.common.table.identity.identity_ID}, 
						new String[]{"1", ""+identitiesID});
				Identity.default_id_branch = ib;
				MyIdentitiesModel.setAnIdentityCurrent(tree, ib);
				ib.default_id = true;
		    	model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(ib)},new Object[]{ib}));				
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    }
}
@SuppressWarnings("serial")
class IdentityUnSetDefAction extends DebateDecideAction {
    MyIdentitiesTree tree;ImageIcon icon;
    public IdentityUnSetDefAction(MyIdentitiesTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
	this.tree = tree;this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	MyIdentitiesModel model = (MyIdentitiesModel)tree.getModel();
            //System.err.println("Set Default action: " + e);
    	TreePath tp=tree.getLeadSelectionPath();
    	Object source = tp.getLastPathComponent();
    	if(source instanceof IdentityBranch) {
    		unsetDefault((IdentityBranch)source, model, tree);
    	}
    }	
    public static void unsetDefault(IdentityBranch source, MyIdentitiesModel model, MyIdentitiesTree tree){
    	 IdentityBranch ib = source;
    	 long identitiesID=ib.identityID;
    	 try {
				model.db.update(net.ddp2p.common.table.identity.TNAME, new String[]{net.ddp2p.common.table.identity.default_id}, new String[]{net.ddp2p.common.table.identity.identity_ID},
						new String[]{"0", identitiesID+""});
				if(Identity.default_id_branch!=null){
					IdentityBranch oib = (IdentityBranch)Identity.default_id_branch;
					oib.default_id = false;
					Identity.default_id_branch = null;
					int old_idx = model.root.getIndexOfChild(oib);
					if(old_idx >= 0)
						try{
							model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{old_idx}, new Object[]{oib}));
						}catch(Exception e){//e.printStackTrace();
						}
				}
				/*
				model.db.update(table.identity.TNAME, new String[]{table.identity.default_id}, new String[]{table.identity.identity_ID}, 
						new String[]{"1", ""+identitiesID});
				Identity.default_id_branch = ib;
				ib.default_id = true;
		    	model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(ib)},new Object[]{ib}));				
		    	*/
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    }
}
class IdentityAddAction extends DebateDecideAction implements WorkerListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	IdentityBranch newid;
	MyIdentitiesTree tree;ImageIcon icon;
	MyIdentitiesModel model;
	public IdentityAddAction(MyIdentitiesTree tree,
			String text, ImageIcon icon,
			String desc, String whatis,
			Integer mnemonic) {
		super(text, icon, desc, whatis, mnemonic);
		this.tree = tree;this.icon = icon;
	}
	public void actionPerformed(ActionEvent e) {
		model = (MyIdentitiesModel)tree.getModel();
		//System.err.println("Add Action for first button/menu item: " + e);
		long identityID=0;
		try{
			identityID=model.db.insert(
					net.ddp2p.common.table.identity.TNAME,
					new String[]{net.ddp2p.common.table.identity.profile_name},
					new String[]{__("New Identity")});
		}catch(Exception ex){
			JOptionPane.showMessageDialog(tree,__("Error inserting identity:"),__("Insertion Error"),JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
			return;
		}
		String params[]= new String[1];
		params[0]=new String(""+identityID);
		int new_index=0;
		model.root.addChild(
				newid=new IdentityBranch(
						model,
						__("New Identity"),
						0,
						identityID, false,
						__("'New Identity' has just been added"), Queries.sql_identity_enum_leafs, params, null,null, null),new_index);
		if(DEBUG) System.out.println("IdentityActions:IdentityAddAction:Firing add: "+
				model.root+" idx="+new_index+" newid="+newid);

		net.ddp2p.common.hds.GenerateKeys keygen = new net.ddp2p.common.hds.GenerateKeys("Identity", this); keygen.start();

		model.fireTreeNodesInserted(new TreeModelEvent(tree,
				new Object[]{model.root}, new int[]{new_index},new Object[]{newid}));
		if(model.root.getChildCount()==1) {
			model.fireTreeStructureChanged(new TreeModelEvent(tree,new Object[]{model.root}));
			IdentitySetDefAction.setDefault(newid, model, tree);
		}
	}
	@Override
	public void Done(Object source) {
		GenerateKeys gk = (GenerateKeys) source;
		newid.updateKey(gk);
		model.fireTreeNodesChanged(new TreeModelEvent(tree, new Object[]{model.root,newid}));
	}
}
class IdentityCustomAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    public static final int CMD_BOTTOM = 1;
	public static final int CMD_UP = 2;
	MyIdentitiesTree tree;ImageIcon icon;
	int cmd;
    public IdentityCustomAction(MyIdentitiesTree tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int _cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree;this.icon = icon; cmd = _cmd;
    }
    public void actionPerformed(ActionEvent e) {
    	MyIdentitiesModel model = (MyIdentitiesModel)tree.getModel();
    	//System.err.println("Add Action for first button/menu item: " + e);
    	TreePath tp=tree.getLeadSelectionPath();
    	Object source = tp.getLastPathComponent();
    	if(cmd == CMD_UP) {
    		if(source instanceof IdentityBranch) {
    			//IdentityBranch ib = (IdentityBranch)source;
    			//long identitiesID = ib.identityID;
    		}    	
    		if(source instanceof IdentityLeaf) {
    			IdentityLeaf il = (IdentityLeaf)source;
    			//long pID = il.id;
    			il.sequence = 0;
    			try {
    				Application.getDB().update(net.ddp2p.common.table.identity_value.TNAME,
    						new String[]{net.ddp2p.common.table.identity_value.sequence_ordering},
    						new String[]{net.ddp2p.common.table.identity_value.identity_value_ID},
    						new String[]{""+il.sequence, Util.getStringID(il.id)}, DEBUG);
    			} catch (P2PDDSQLException e1) {
    				e1.printStackTrace();
    			}
				//TreePath new_path = tp.getParentPath();//.pathByAddingChild(newchild);
				//model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(ib)},new Object[]{ib}));
				il.identityBranch.colapsed();
				il.identityBranch.populate();
				model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(il.identityBranch)},new Object[]{il.identityBranch}));
    		}
    	}
    	if(cmd == CMD_BOTTOM) {
    		if(source instanceof IdentityBranch) {
    			//IdentityBranch ib = (IdentityBranch)source;
    			//long identitiesID = ib.identityID;
    		}    	
    		if(source instanceof IdentityLeaf) {
    			IdentityLeaf il = (IdentityLeaf)source;
    			//long pID = il.id;
    			String sql_max = 
    				"SELECT max(iv."+net.ddp2p.common.table.identity_value.sequence_ordering+") " +
					" FROM "+net.ddp2p.common.table.identity_value.TNAME+" AS iv "+
					" JOIN "+net.ddp2p.common.table.identity_value.TNAME+" AS oi ON(iv."+net.ddp2p.common.table.identity_value.identity_ID+" = oi."+net.ddp2p.common.table.identity_value.identity_ID+") "+
    						" WHERE oi."+net.ddp2p.common.table.identity_value.identity_value_ID+"="+"?;";
    			try {
					ArrayList<ArrayList<Object>> s = Application.getDB().select(sql_max, new String[]{Util.getStringID(il.id)}, DEBUG);
					if(s.size()==0) il.sequence=0;
					else il.sequence = Util.lval(s.get(0).get(0), -1)+1;
				} catch (P2PDDSQLException e2) {
					e2.printStackTrace();
					il.sequence++;
				}
    			try {
    				Application.getDB().update(net.ddp2p.common.table.identity_value.TNAME,
    						new String[]{net.ddp2p.common.table.identity_value.sequence_ordering},
    						new String[]{net.ddp2p.common.table.identity_value.identity_value_ID},
    						new String[]{""+il.sequence, Util.getStringID(il.id)}, DEBUG);
    			} catch (P2PDDSQLException e1) {
    				e1.printStackTrace();
    			}
				//TreePath new_path = tp.getParentPath();//.pathByAddingChild(newchild);
				//model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(ib)},new Object[]{ib}));
				il.identityBranch.colapsed();
				il.identityBranch.populate();
				model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(il.identityBranch)},new Object[]{il.identityBranch}));
    		}
    	}
    }
}
class IdentityPropertyAddAction extends DebateDecideAction {
	private static final boolean DEBUG = false;
	MyIdentitiesTree tree;ImageIcon icon;
	public IdentityPropertyAddAction(MyIdentitiesTree tree,
			String text, ImageIcon icon,
			String desc, String whatis,
			Integer mnemonic) {
		super(text, icon, desc, whatis, mnemonic);
		this.tree = tree;this.icon = icon;
	}
	public void actionPerformed(ActionEvent e) {
		MyIdentitiesModel model = (MyIdentitiesModel)tree.getModel();
		//System.err.println("Add Action for first button/menu item: " + e);
		TreePath tp=tree.getLeadSelectionPath();
		Object source = tp.getLastPathComponent();
		if(source instanceof IdentityBranch) {
			IdentityBranch ib = (IdentityBranch)source;
			long identitiesID=ib.identityID;
			String newname=__("New Value");
			long iv_ID=0;
			try{
				IdentityLeaf newchild;
				iv_ID=model.db.insert(
						net.ddp2p.common.table.identity_value.TNAME,
						new String[] {net.ddp2p.common.table.identity_value.identity_ID, net.ddp2p.common.table.identity_value.value, net.ddp2p.common.table.identity_value.oid_ID, net.ddp2p.common.table.identity_value.sequence_ordering},
						new String[]{Util.getStringID(identitiesID),newname,null,"0"});
				ib.addChild(newchild=new IdentityLeaf(newname,__("Newly inserted property"),iv_ID,ib),0);
				TreePath new_path = tp.pathByAddingChild(newchild);
				model.fireTreeNodesChanged(new TreeModelEvent(tree,new Object[]{model.root},new int[]{model.root.getIndexOfChild(ib)},new Object[]{ib}));
				if(true ||(ib.children.length == ib.nchildren)) {
					int id = ib.getIndexOfChild(newchild);
					model.fireTreeNodesInserted(new TreeModelEvent(tree,tp,new int[]{id},new Object[]{newchild}));
					tree.scrollPathToVisible(new_path);
					if(DEBUG) System.out.println("fireInserted: "+id+" "+newchild);
				}else
					if(DEBUG) System.out.println("fireInserted delayed: "+ib.children.length+" != "+ib.nchildren);

				if (ib.nchildren == 1){
					tree.expandPath(new TreePath(new Object[]{model.root,ib}));
					tree.scrollPathToVisible(new_path);
				}
			}catch(Exception ex){
				JOptionPane.showMessageDialog(tree,__("Error inserting identity:"),__("Insertion Error"),JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
			return;	    
		}else
			JOptionPane.showMessageDialog(tree,__("Only adding properties to identities!"),__("Insertion Error"),JOptionPane.ERROR_MESSAGE);
	}
}

class IdentityDelAction extends DebateDecideAction {
	MyIdentitiesTree tree;ImageIcon icon;
	MyIdentitiesModel model;
	public IdentityDelAction(MyIdentitiesTree tree,
			String text, ImageIcon icon,
			String desc, String whatis,
			Integer mnemonic) {
		super(text, icon, desc, whatis, mnemonic);
		this.tree = tree;this.icon = icon;
	}
	boolean remove(TreePath tp){
		TreePath tpp = tp.getParentPath();
		Object source = tp.getLastPathComponent();
		Object o_parent = tpp.getLastPathComponent();
		if(!(o_parent instanceof IdentityBranch)) return false;
		IdentityBranch parent = (IdentityBranch)o_parent;
		//tree.clearSelection();
		////tree.collapsePath(tpp);
		int old_index = parent.getIndexOfChild(source);
		if(old_index == -1) {
			JOptionPane.showMessageDialog((Component)tree,(new Object[] {__("Item not found!"),tp}),__("Delete item"),JOptionPane.ERROR_MESSAGE);
			return false;
		}
		parent.del(source);
		model.fireTreeNodesRemoved(new TreeModelEvent(tree,tp.getParentPath(),new int[]{old_index},new Object[]{source}));
		return true;
	}
	public void actionPerformed(ActionEvent e) {
		model = (MyIdentitiesModel)tree.getModel();
		//System.err.println("Del Action for first button/menu item: " + e);
		TreePath tp=tree.getLeadSelectionPath();
    	tree.addSelectionPath(tp);
    	TreePath sp[] = tree.getSelectionPaths();
    	if(tp.getPathCount() <= 1) {
    		JOptionPane.showMessageDialog((Component)tree,(new Object[] {__("Cannot remove this item!")}),__("Delete item"),JOptionPane.ERROR_MESSAGE);
    		return;
    	}
    	//int result = JOptionPane.showConfirmDialog((Component)tree,(new Object[]{_("Are you sure you want to remove this item!")}),_("Delete item"),JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
    	Object options[]=new Object[]{__("Yes"),__("Cancel")};
    	int result = JOptionPane.showOptionDialog((Component)tree,(new Object[]{__("Are you sure you want to remove selected items!")}),__("Delete items"),JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
    	if(result == 0){
    		//System.err.println("Removing "+tp);
    		//remove(tp);
    		for (int i=0; i<sp.length; i++) {
    	   		System.err.println("Removing "+sp[i]);
    	   		remove(sp[i]);
    		}
    	}
    }
}
