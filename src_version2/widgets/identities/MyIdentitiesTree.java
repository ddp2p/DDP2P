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
 package widgets.identities;

import java.awt.Component;
import java.awt.event.*;

import javax.swing.*;
//import javax.swing.JTree;
import javax.swing.tree.*;
//import javax.swing.ToolTipManager;
//import javax.swing.plaf.metal.MetalTreeUI;
//import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
//import javax.swing.UIManager;
import javax.swing.event.*;

import util.P2PDDSQLException;

import config.Identity;
import util.*;
import widgets.app.Util_GUI;
import static util.Util.__;

class MyIdentitiesTreeCellRenderer extends DefaultTreeCellRenderer {
    public MyIdentitiesTreeCellRenderer() {}
    public Component getTreeCellRendererComponent(
       JTree tree, Object value, boolean sel,
       boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row, hasFocus);
	if(value instanceof IdentityBranch) {
	    IdentityBranch ib = (IdentityBranch)value;
	    setToolTipText(ib.getTip());
	}else{
	    if(value instanceof IdentityLeaf) {
		IdentityLeaf il = (IdentityLeaf)value;
		//setIcon(getDefaultLeafIcon());
		//setIcon(getDefaultClosedIcon());
		setIcon(null);
		setToolTipText(il.getTip());
	    }
	}
	return this;
    }
}

public class MyIdentitiesTree extends JTree implements TreeExpansionListener,  TreeWillExpandListener, MouseListener, ActionListener{
	final static long serialVersionUID=0;
	private static final boolean DEBUG = false;
    //MyIdentitiesModel model;
    JPopupMenu popup, popup_add, popup_leaf;
    IdentityAddAction addAction;
    IdentityPropertyAddAction addPAction;
    IdentityDelAction delAction;
    IdentitySetDefAction setDefAction;
    IdentityUnSetDefAction unsetDefAction;
    IdentitySetCurrentAction setCurrentAction;
    IdentityUnSetCurrentAction unsetCurrentAction;
    JMenuItem m_menu_add_id, m_menu_del_id, m_menu_add_prop, m_menu_del_prep,
    	m_menu_set_current, m_menu_set_def, m_menu_unset_current, m_menu_unset_def;
	private IdentityCustomAction cmdAction;
    
    void preparePopup(){
    	ImageIcon addicon = Util_GUI.createImageIcon("icons/add.png",__("add an item"));
    	ImageIcon delicon = Util_GUI.createImageIcon("icons/remove.png",__("delete an item"));

    	popup_add = new JPopupMenu();
    	// add
    	addAction = new IdentityAddAction(this, __("Add"),addicon,__("Add new profile."),__("You may add new profile."),KeyEvent.VK_A);
    	m_menu_add_id = new JMenuItem(addAction);//_("Add")
    	popup_add.add(m_menu_add_id);

    	popup_leaf = new JPopupMenu();
    	// add
    	delAction = new IdentityDelAction(this, __("Del"),delicon,__("Dell property."),__("You may delete this information about yourself."),KeyEvent.VK_D);
    	m_menu_del_prep = new JMenuItem(delAction);//_("Delete");
    	popup_leaf.add(m_menu_del_prep);
    	
    	cmdAction = new IdentityCustomAction(this, __("Move To Bottom"),delicon,__("Down property."),__("Bottom."),KeyEvent.VK_B,IdentityCustomAction.CMD_BOTTOM);
    	m_menu_del_prep = new JMenuItem(cmdAction);//_("Delete");
    	popup_leaf.add(m_menu_del_prep);
    	
    	cmdAction = new IdentityCustomAction(this, __("Move To Top"),delicon,__("Up property."),__("Top."),KeyEvent.VK_T,IdentityCustomAction.CMD_UP);
    	m_menu_del_prep = new JMenuItem(cmdAction);//_("Delete");
    	popup_leaf.add(m_menu_del_prep);
    	
    	

    	popup = new JPopupMenu();
    	// add
    	addPAction = new IdentityPropertyAddAction(this, __("Add property"),addicon,__("Add property."),__("You may add new information about yourself."),KeyEvent.VK_A);
    	m_menu_add_prop = new JMenuItem(addPAction);//_("Add")
    	popup.add(m_menu_add_prop);
    	// delete
    	delAction = new IdentityDelAction(this, __("Delete Identity"),delicon,__("Dell identity."),__("You may delete this information about yourself."),KeyEvent.VK_D);
    	m_menu_del_id = new JMenuItem(delAction);//_("Delete");
    	popup.add(m_menu_del_id);

    	setDefAction = new IdentitySetDefAction(this, __("Set As Default"),null,__("Set profile as default."),__("Set this profile as default."),KeyEvent.VK_S);
    	m_menu_set_def = new JMenuItem(setDefAction);//_("Add")
    	popup.add(m_menu_set_def);
    	//
    	unsetDefAction = new IdentityUnSetDefAction(this, __("UnSet As Default"),null,__("UnSet profile as default."),__("UnSet this profile as default."),KeyEvent.VK_U);
    	m_menu_unset_def = new JMenuItem(unsetDefAction);//_("Add")
    	popup.add(m_menu_unset_def);
    	//
    	setCurrentAction = new IdentitySetCurrentAction(this, __("Set Current"),null,__("Set profile as current."),__("Set this profile as default."),KeyEvent.VK_S);
    	m_menu_set_current = new JMenuItem(setCurrentAction);//_("Add")
    	popup.add( m_menu_set_current);
    	//
    	unsetCurrentAction = new IdentityUnSetCurrentAction(this, __("UnSet Current"),null,__("UnSet profile as current."),__("UnSet this profile as default."),KeyEvent.VK_S);
    	m_menu_unset_current = new JMenuItem(setCurrentAction);//_("Add")
    	popup.add( m_menu_unset_current);
    }
    public void reTranslate(){
    	preparePopup();
    }

    public MyIdentitiesTree(MyIdentitiesModel _model) {
        super(_model);
        //model=_model;
        addTreeExpansionListener(this);
        addTreeWillExpandListener(this);
        preparePopup();
        addMouseListener(this);
        largeModel=true;
        setEditable(true);
        setCellEditor(new MyIdentityTreeCellEditor(this));
        //setUI(new MetalTreeUI());
        try {UIManager.setLookAndFeel("javax.swing.plaf.windows.WindowsLookAndFeel");
		}catch(Exception e){/*System.err.println(e);*/}
		//getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		//getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		//Enable tool tips.
		ToolTipManager.sharedInstance().registerComponent(this);
		MyIdentitiesTreeCellRenderer renderer = new MyIdentitiesTreeCellRenderer();
        Icon personIcon = null;
        //ImageIcon leafIcon = createImageIcon("images/middle.gif"); if (leafIcon != null) {
        renderer.setLeafIcon(personIcon);
        renderer.setClosedIcon(personIcon);
        renderer.setOpenIcon(personIcon);
        setCellRenderer(renderer);
        setShowsRootHandles(true);
        putClientProperty("JTree.lineStyle", "Angled" /*"Horizontal"*/);
        setRootVisible(false);

		try {
			MyIdentitiesModel.setAnIdentityCurrent(this, (IdentityBranch) Identity.default_id_branch);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    /*
      This allows us to free memory when a Branch has colapsed 
     */
    public void treeCollapsed(TreeExpansionEvent e) {
    	TreePath tp = e.getPath();
    	if(DEBUG) System.err.println("Colapse: "+tp);
    	Object obj=tp.getLastPathComponent();//e.getSource();
    	if(obj instanceof IdentityBranch) {
    		IdentityBranch branch = (IdentityBranch)obj;
    		branch.colapsed();
    	}else
    		if(DEBUG) System.err.println("Colapse object: "+obj);
    }
    public void treeExpanded(TreeExpansionEvent e) {}
    public void treeWillExpand(TreeExpansionEvent e) 
    throws ExpandVetoException {
    	TreePath tp = e.getPath();
    	if(DEBUG) System.err.println("to populate: "+tp);
    	Object obj=tp.getLastPathComponent();
    	if(obj instanceof IdentityBranch) {
    		IdentityBranch branch = (IdentityBranch)obj;
    		branch.populate();
    	}
    }
    public void treeWillCollapse(TreeExpansionEvent e) {}
    public void mouseReleased(MouseEvent e) {
    	jTreeMouseReleased(e);
    }
    public void mouseClicked(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mousePressed(MouseEvent e) {
    	jTreeMouseReleased(e);
    }
    private void jTreeMouseReleased(java.awt.event.MouseEvent evt) {
        TreePath selPath = getPathForLocation(evt.getX(), evt.getY());
        //System.err.println("mouse release: "+selPath);
        if (selPath == null){
        	if (evt.isPopupTrigger()) {
        		//System.err.println("popup: "+selPath);
        		JPopupMenu popup_add = new JPopupMenu();
    			if(Identity.current_id_branch!=null)
    				popup_add.add(this.unsetCurrentAction);
    			popup_add.add(addAction);
        		popup_add.show((Component)evt.getSource(), evt.getX(), evt.getY());
        	}else{
        		//System.err.println("no popup trigger.");
        	}
            return;
        }else{
            if (evt.isPopupTrigger()) {
            	setLeadSelectionPath(selPath);
            	Object lastComponent = selPath.getLastPathComponent();
            	//System.err.println("popup: "+selPath);
            	if(lastComponent instanceof IdentityBranch) {
            		IdentityBranch ib = (IdentityBranch) lastComponent;
            		JPopupMenu popup_def = new JPopupMenu();
            		popup_def.add(delAction);
            		popup_def.add(addAction);
            		popup_def.add(this.addPAction);
        			if(Identity.current_id_branch!=null)
        				popup_def.add(this.unsetCurrentAction);
        			if((Identity.current_id_branch != ib) && (ib.getKeys()!= null))
        				popup_def.add(this.setCurrentAction);
            		if(ib.default_id)
            			popup_def.add(this.unsetDefAction);
            		else
            			popup_def.add(this.setDefAction);
            			
            		popup_def.show((Component)evt.getSource(), evt.getX(), evt.getY());
            		
            		//popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
            	}else
            		popup_leaf.show((Component)evt.getSource(), evt.getX(), evt.getY());
            }else{
            	//System.err.println("no popup trigger: "+selPath);
            }
        }
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.err.println("action: "+e);
    }
    /* **********************
     * This allows to return complex rendering values for a node
     * usable instead of toString() of the node 
     ************************/
    public String convertValueToText(Object value,
				     boolean selected,
				     boolean expanded,
				     boolean leaf,
				     int row,
				     boolean hasFocus) {
	String prefix="";//+row+": ";
	if(value instanceof IdentityLeaf){
	    return prefix+value.toString();
	}else if(value instanceof IdentityBranch){
	    return prefix+value.toString();
	}
	return prefix+value.toString();
    }
}
