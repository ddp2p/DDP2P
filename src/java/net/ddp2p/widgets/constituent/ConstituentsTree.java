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
 package net.ddp2p.widgets.constituent;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.population.ConstituentsAddressNode;
import net.ddp2p.common.population.ConstituentsBranch;
import net.ddp2p.common.population.ConstituentsIDNode;
import net.ddp2p.common.population.ConstituentsPropertyNode;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.app.MainFrame;
import static net.ddp2p.common.util.Util.__;
class ConstituentsTreeCellRenderer extends DefaultTreeCellRenderer {
	final static long serialVersionUID=0;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	ConstituentsAddressNode ib=null;
	ConstituentsPropertyNode ip=null;
	ImageIcon anonym = DDIcons.getProfileImageIcon("anonumous icon"); // new ImageIcon("icons/profile.gif");
    public ConstituentsTreeCellRenderer() {
    }
    public Component getTreeCellRendererComponent(
       JTree tree, Object value, boolean sel,
       boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row, hasFocus);
        if(value instanceof ConstituentsAddressNode)  getTreeCellRendererComponentCAN(value);
        if(value instanceof ConstituentsIDNode)  getTreeCellRendererComponentCIN(value);
        if(value instanceof ConstituentsPropertyNode)  getTreeCellRendererComponentCPN(value);
		setPreferredSize(getUI().getPreferredSize(this));
		return this;
    }
    public void getTreeCellRendererComponentCPN(Object value){
    	ip = (ConstituentsPropertyNode)value;
    	setToolTipText(ip.getTip());   
    	if(ip!=null){
    		setText(ip.getProperty().label+":"+ip.toString());
    	}
    }
    public void getTreeCellRendererComponentCAN(Object value){
	    	ib = (ConstituentsAddressNode)value;
	    	setToolTipText(ib.getTip());
	    	if((ib!=null) && (ib.getLocation()!=null)){
	    		String ibvalue = ib.getLocation().value;
	    		boolean censused = ib.getLocation().censusDone;
	    		if(ibvalue==null){
	    			ibvalue=__("Unspecified Yet!");
	    		}
	    		String str = "<html>"+ibvalue+
	    		(censused?
	    		    (" <img src='"+DDIcons.getResourceURL(DDIcons.I_NEIGHS19)+"' style='vertical-align: middle;' align='middle' title='"+__("Neighborhoods")+"'>"+
	    			ib.getNeighborhoods()+
	    			" <img src='"+DDIcons.getResourceURL(DDIcons.I_INHAB19)+"' style='padding-top: 0px; vertical-align: middle;' align='middle'>"+
	    			ib.getLocation().inhabitants):""
	    			)+
	    			"</html>";
	    		setText(str);
	    	}
	}
    /**
     * 
     * @param value
     */
 	public void getTreeCellRendererComponentCIN(Object value){
	    		ConstituentsIDNode il = (ConstituentsIDNode)value;
	    		String name = il.getConstituent().getSurname();
	    		if (name == null) name = il.getConstituent().getGivenName();
	    		else if(il.getConstituent().getGivenName() != null) name+=", "+il.getConstituent().getGivenName();
	    		String color="", fgcolor="", sfg="", sbg="";
	    		if (il.getConstituent().external) {
	    			fgcolor=" fgcolor='Blue'";
	    			sfg = " style='color:Blue;'" ;
	    		}
	    		if ((il.getConstituent().witnessed_by_me==D_Witness.UNKNOWN)&&(!il.getConstituent().external)){
	    			color=" bgcolor='#F0E68C'"; // khaki
	    			sbg = " style='background-color:#F0E68C;'";
	    		}
	    		if((il.getConstituent().witnessed_by_me==D_Witness.FAVORABLE)&&(!il.getConstituent().external)){
	    			color=" bgcolor='#7FFFD4'"; // Aquamarine
	    			sbg = " style='background-color:#7FFFD4;'";
	    		}
	    		if((il.getConstituent().witnessed_by_me==D_Witness.UNFAVORABLE)&&(!il.getConstituent().external)){
	    			color=" bgcolor='#EE82EE'"; // Violet
	    			sbg = " style='background-color:EE82EE;'";
	    		}
	    		if((il.getConstituent().witnessed_by_me==D_Witness.UNKNOWN)&&(il.getConstituent().external)){
	    			color=" fgcolor='Blue' bgcolor='#F0E68C'"; // khaki
	    			sbg = " style='background-color:#F0E68C;color:Blue;'";
	    		}
	    		if((il.getConstituent().witnessed_by_me==D_Witness.FAVORABLE)&&(il.getConstituent().external)){
	    			color=" fgcolor='Blue' bgcolor='#7FFFD4'"; // Aquamarine
	    			sbg = " style='background-color:#7FFFD4;color:Blue;'";
	    		}
	    		if((il.getConstituent().witnessed_by_me==D_Witness.UNFAVORABLE)&&(il.getConstituent().external)){
	    			color=" fgcolor='Blue' bgcolor='EE82EE'"; //Violet
	    			sbg = " style='background-color:EE82EE;color:Blue;'";
	    		}
	    		if(il.getConstituent().myself==1){
	    			color=" bgcolor='Red'";
	    			sbg = " style='background-color:Red;'";
	    			fgcolor=""; sfg="";
	    		}
	    		URL block = DDIcons.getResourceURL(DDIcons.I_BLOCK_DOWNLOAD20);
	    		URL broadcast = DDIcons.getResourceURL(DDIcons.I_BROADCAST20);
	    		URL up = DDIcons.getResourceURL(DDIcons.I_UP19);
	    		URL down = DDIcons.getResourceURL(DDIcons.I_DOWN19);
	    		String str = "<html><body"+color+fgcolor+sfg+sbg+">"+
	    				(il.getConstituent().blocked?(" <img src='"+block+"' style='padding-top: 0px; vertical-align: middle;' align='middle'>"):("")) +
	    				(il.getConstituent().broadcast?(""):(" <img src='"+broadcast+"' style='padding-top: 0px; vertical-align: middle;' align='middle'>")) +
	    			name +
	    			" <img src='"+up+"' style='padding-top: 0px; vertical-align: middle;' align='middle'>"+
	    			il.getConstituent().witness_for+
	    			" <img src='"+down+"' style='padding-top: 0px; vertical-align: middle;' align='middle'>"+
	    			il.getConstituent().witness_against+	 
	    			((!il.getConstituent().external && (il.getConstituent().getSlogan()!=null))?" <span style='background-color:yellow'>"+il.getConstituent().getSlogan()+"</span>":"")+
	    			"</body></html>";
	    		setText(str);
				if (il.getConstituent().getIcon() == null) {
					setIcon(anonym);
				}else{
					Object icon = il.getConstituent().getIcon();
					if (icon instanceof Icon)
						setIcon((Icon) icon);
				}
				setToolTipText(il.getTip());
    }
}
public class ConstituentsTree extends JTree implements TreeExpansionListener,  TreeWillExpandListener, MouseListener, ActionListener{
	final static long serialVersionUID=0;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	JFileChooser fc = new JFileChooser();
    JPopupMenu popup, popup_add, popup_leaf, popup_empty;
    ConstituentsAddAction addAction;
    ConstituentsDelAction delAction;
    AddEmptyNeighborhoodAction addENAction;
    ConstituentsWitnessAction witnessAction;
    JMenuItem m_menu_add_myself, m_menu_set_myself, m_menu_add_const, m_menu_add_neighood, m_menu_del_neighood, m_witness_const;
	private ConstituentsAddMyselfAction addMeAction;
	private ConstituentsSetMyselfAction setMeAction;
	private ConstituentsCustomAction refreshAction;
	private ConstituentsCustomAction verifyAction;
	private JMenuItem m_verify_const;
    void preparePopup() {
    	preparePopup(null);
    }
    void preparePopup(Object target) {
    	if(DEBUG) System.out.println("ConstituentsTree:preparePopup: start");
		ImageIcon icon_register = net.ddp2p.widgets.app.DDIcons.getRegistrationImageIcon("Register");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon witnessicon = DDIcons.getWitImageIcon(__("witness an item")); 
    	ConstituentsModel model = (ConstituentsModel) getModel();
    	popup_empty = new JPopupMenu();
    	popup_leaf = new JPopupMenu();
    	popup_add = new JPopupMenu();
    	if (model.getConstituentIDMyself() > 0) {
    		addAction = new ConstituentsAddAction(this, __("Add Neighbor"),addicon,__("Add new top level."),__("You may add new identity."),KeyEvent.VK_A);
    		m_menu_add_const = new JMenuItem(addAction);
    		popup_add.add(m_menu_add_const);
       		m_menu_add_const = new JMenuItem(addAction);
    		popup_empty.add(m_menu_add_const);
    	}
    	if (model.getConstituentIDMyself() > 0) {
    		witnessAction = new ConstituentsWitnessAction(this, __("Witness"),witnessicon,__("Witness constituents."),__("You may bear witness."),KeyEvent.VK_W);
    		m_witness_const = new JMenuItem(witnessAction);
    		popup_leaf.add(m_witness_const);
    	}  else {
    		if(DEBUG) System.out.println("ConstituentsTree:preparePopup: no constituentID:"+model.getConstituentIDMyself());
    	}
    	if (model.getConstituentIDMyself() > 0) {
    		verifyAction = new ConstituentsCustomAction(this, __("Verify Identity by Email"), witnessicon,__("Identify constituents."),__("You may identify constituent."),KeyEvent.VK_I, ConstituentsCustomAction.IDENTIFY);
    		m_verify_const = new JMenuItem(verifyAction);
    		popup_leaf.add(m_verify_const);
    	} else {
    		if (DEBUG) System.out.println("ConstituentsTree:preparePopup: no constituentID:"+model.getConstituentIDMyself());
    	}
    	if (model.getConstituentIDMyself() > 0) {
    		refreshAction = new ConstituentsCustomAction(this, __("Change Slogan"),addicon,__("Change Slogan."),__("Slogan."),KeyEvent.VK_S, ConstituentsCustomAction.SLOGAN);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_add.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_empty.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_leaf.add(m_menu_add_myself);
    		if (DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");
    		refreshAction = new ConstituentsCustomAction(this, __("Move Myself to this Neighborhood"),addicon,__("Move Myself."),__("Move Myself."),KeyEvent.VK_M, ConstituentsCustomAction.MOVE);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_add.add(m_menu_add_myself);
    	}
    	addMeAction = new ConstituentsAddMyselfAction(this, __("Add Myself"),icon_register,__("Add myself at top level."),__("You may detail your identity."),KeyEvent.VK_Y);
    	m_menu_add_myself = new JMenuItem(addMeAction);
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(addMeAction);
    	popup_empty.add(m_menu_add_myself);
       	if (DEBUG) System.out.println("ConstituentsTree:preparePopup: added Mysef");
    	refreshAction = new ConstituentsCustomAction(this, __("Retrieve Data"),addicon,__("Retrieve Data."),__("Retrieve Data."),KeyEvent.VK_T, ConstituentsCustomAction.TOUCH);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_empty.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_leaf.add(m_menu_add_myself);
       	if (DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");
    	refreshAction = new ConstituentsCustomAction(this, __("Refresh Needed"),addicon,__("Refresh needed at top level."),__("Refresh Needed."),KeyEvent.VK_N, ConstituentsCustomAction.REFRESH_NEED);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_empty.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_leaf.add(m_menu_add_myself);
       	if (DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");
    	refreshAction = new ConstituentsCustomAction(this, __("Refresh All"),addicon,__("Refresh at top level."),__("Refresh."),KeyEvent.VK_R, ConstituentsCustomAction.REFRESH);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_empty.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_leaf.add(m_menu_add_myself);
       	if (DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");
       	if (model.automatic_refresh)
       		refreshAction = new ConstituentsCustomAction(this, __("Refresh Manually"),addicon,__("Refresh Manually."),__("Refresh Manually."),KeyEvent.VK_A, ConstituentsCustomAction.REFRESH_AUTO);
       	else
       		refreshAction = new ConstituentsCustomAction(this, __("Refresh Automatically"),addicon,__("Refresh automatically."),__("Refresh Automatically."),KeyEvent.VK_A, ConstituentsCustomAction.REFRESH_AUTO);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_empty.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);
    	popup_leaf.add(m_menu_add_myself);
       	if(DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");
    	if (model.getConstituentIDMyself() > 0) {
    		refreshAction = new ConstituentsCustomAction(this, __("Expand Myself"),addicon,__("Expand myself."),__("Expand M."),KeyEvent.VK_M, ConstituentsCustomAction.MYSELF);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_add.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_empty.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_leaf.add(m_menu_add_myself);
    		refreshAction = new ConstituentsCustomAction(this, __("Expand Witnessed"),addicon,__("Expand witnessed."),__("Expand W."),KeyEvent.VK_E, ConstituentsCustomAction.WITNESSED);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_add.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_empty.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);
    		popup_leaf.add(m_menu_add_myself);
    	}
    	boolean broadcasted = true;
    	boolean blocked = false;
		if (target != null && target instanceof ConstituentsIDNode) {
			if (_DEBUG) System.out.println("ConstituentTree: preparePopup: target");
			ConstituentsIDNode cid = (ConstituentsIDNode) target;
			if (cid.getConstituent() != null) {
				if (cid.getConstituent().constituent == null) {
					cid.getConstituent().constituent = D_Constituent.getConstByLID(cid.getConstituent().getC_LID(), true, false);
				}
				if (cid.getConstituent().constituent != null) {
					broadcasted = cid.getConstituent().constituent.broadcasted;
					blocked = cid.getConstituent().constituent.blocked;
					if (_DEBUG) System.out.println("ConstituentTree: preparePopup: target cid "+broadcasted);
				} else {
					if (_DEBUG) System.out.println("ConstituentTree: preparePopup: target null cid "+cid.getConstituent());
				}
			}
		}
		if (! blocked)
			refreshAction = new ConstituentsCustomAction(this, __("Block Receiving"),addicon,__("Block."),__("Block."),KeyEvent.VK_K, ConstituentsCustomAction.BLOCK);
		else
			refreshAction = new ConstituentsCustomAction(this, __("UnBlock Receiving"),addicon,__("UnBlock."),__("UnBlock."),KeyEvent.VK_K, ConstituentsCustomAction.BLOCK);
		m_menu_add_myself = new JMenuItem(refreshAction);
		popup_leaf.add(m_menu_add_myself);
		m_menu_add_myself = new JMenuItem(refreshAction);
		popup_add.add(m_menu_add_myself);
		if (broadcasted)
			refreshAction = new ConstituentsCustomAction(this, __("Block Sending"),addicon,__("Block Sending."),__("Block Sending."),KeyEvent.VK_B, ConstituentsCustomAction.BROADCAST);
		else
			refreshAction = new ConstituentsCustomAction(this, __("UnBlock Sending"),addicon,__("UnBlock Sending."),__("UnBlock Sending."),KeyEvent.VK_B, ConstituentsCustomAction.BROADCAST);
		m_menu_add_myself = new JMenuItem(refreshAction);
		popup_leaf.add(m_menu_add_myself);
		m_menu_add_myself = new JMenuItem(refreshAction);
		popup_add.add(m_menu_add_myself);
		refreshAction = new ConstituentsCustomAction(this, __("Zapp local copy"),addicon,__("Delete local."),__("Delete Local."),KeyEvent.VK_Z, ConstituentsCustomAction.ZAPP);
		m_menu_add_myself = new JMenuItem(refreshAction);
		popup_leaf.add(m_menu_add_myself);
		m_menu_add_myself = new JMenuItem(refreshAction);
		popup_add.add(m_menu_add_myself);
		refreshAction = new ConstituentsCustomAction(this, __("Advertise"),addicon,__("Advertise."),__("Advertise."),KeyEvent.VK_A, ConstituentsCustomAction.ADVERTISE);
		m_menu_add_myself = new JMenuItem(refreshAction);
		popup_leaf.add(m_menu_add_myself);
		m_menu_add_myself = new JMenuItem(refreshAction);
		popup_add.add(m_menu_add_myself);
    	delAction = new ConstituentsDelAction(this, __("Delete Neighborhoods"),delicon,__("Dell constituents."),__("You may delete this information."),KeyEvent.VK_D);
    	m_menu_del_neighood = new JMenuItem(delAction);
    	popup_add.add(m_menu_del_neighood);
    	if (model.hasNeighborhoods) {
    		if(model.getConstituentIDMyself()>0) {
    			addENAction = new AddEmptyNeighborhoodAction(this, __("Add Empty Neighborhood"),addicon,__("Add uninitialized."),__("Add."),KeyEvent.VK_E);
    			m_menu_add_neighood = new JMenuItem(addENAction);
    			popup_add.add(m_menu_add_neighood);
    			m_menu_add_neighood = new JMenuItem(addENAction);
    			popup_empty.add(m_menu_add_neighood);
    		} else {
    			if(DEBUG) System.out.println("ConstituentsTree:preparePopup: no constituentID:"+model.getConstituentIDMyself());
    		}
    	}
    	setMeAction = new ConstituentsSetMyselfAction(this, __("Set Myself"),addicon,__("Set myself at top level."),__("You may detail your identity."),KeyEvent.VK_S);
    	m_menu_set_myself = new JMenuItem(setMeAction);
    	popup_leaf.add(m_menu_set_myself);
    	m_menu_add_myself = new JMenuItem(addMeAction);
    	popup_leaf.add(m_menu_add_myself);
    }
    public void reTranslate(){
    	if(DEBUG) System.out.println("ConstituentsTree:reTranslate: start");
    	preparePopup();
    }
	public ConstituentsTree(ConstituentsModel _model) {
        super(_model);
        _model.setTree(this);
    	if(DEBUG) System.out.println("ConstituentsTree: start");
		addTreeExpansionListener(this);
		addTreeWillExpandListener(this);
		preparePopup();
		addMouseListener(this);
		largeModel=true;
		setEditable(true);
		setCellEditor(new ConstituentsTreeCellEditor(this));
		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		ToolTipManager.sharedInstance().registerComponent(this);
		ConstituentsTreeCellRenderer renderer = new ConstituentsTreeCellRenderer();
        setCellRenderer(renderer);
		setShowsRootHandles(true);
		putClientProperty("JTree.lineStyle", "Angled" );
		setRootVisible(false);
    }
    public void treeCollapsed(TreeExpansionEvent e) {
    	if(DEBUG) System.out.println("ConstituentsTree:treeCollapsed: start");
		TreePath tp = e.getPath();
		if(DEBUG) System.err.println("ConstituentsTree:treeCollapsed: F: treeCollapsed "+tp);
		Object obj=tp.getLastPathComponent();
		if(obj instanceof ConstituentsBranch) {
	    	ConstituentsBranch branch = (ConstituentsBranch)obj;
	    	branch.colapsed();
		}else{
			if(DEBUG) System.err.println("ConstituentsTree:treeCollapsed: Colapse object: "+obj);
		}
    }
    public ConstituentsModel getModel(){
    	return (ConstituentsModel) super.getModel();
    }
    public void treeExpanded(TreeExpansionEvent e) {
		if(DEBUG) System.err.println("ConstituentsTree:treeCollapsed: Expanded object: "+e);
    	((ConstituentsModel)getModel()).runCensus(e.getPath());
    }
    public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
		TreePath tp = e.getPath();
		if(DEBUG) System.err.println(" *****\nConstituentsTree:treeWillExpand:F: treeWillExpand "+tp);
		Object obj=tp.getLastPathComponent();
		if(obj instanceof ConstituentsBranch) {
	    	ConstituentsBranch branch = (ConstituentsBranch)obj;
	    	branch.populate();
	    	if (branch.getNchildren()==0){
	    		branch.setNchildren(1);
	    		if(DEBUG) System.err.println("ConstituentsTree:treeWillExpand: Veto expansion of: "+branch);
	    		throw new ExpandVetoException(new TreeExpansionEvent(this,tp),"No child!");
	    	}
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
    	Object target = null;
    	TreePath selPath = getPathForLocation(evt.getX(), evt.getY());
    	if(selPath!=null) target = selPath.getLastPathComponent();
   	    if (!evt.isPopupTrigger() && ((target == null) || !(target  instanceof ConstituentsIDNode))) {
 			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no popup");
    		return;
    	}
         if(Identity.current_id_branch == null) {
        	Application_GUI.warning(__("To modify you need to select an identity"), __("Missing identity"));
        	MainFrame.tabbedPane.setSelectedComponent(MainFrame.identitiesPane);
 			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no current ID");
        	return;
        }
        if (selPath == null){
        	if (evt.isPopupTrigger()) {
        		setLeadSelectionPath(null);
            	preparePopup();
        		popup_empty.show((Component)evt.getSource(), evt.getX(), evt.getY());
        	} else {
     			if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no sel path, no trigger");
        	}
 			if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no sel path");
            return;
        } else {
            if (true || evt.isPopupTrigger()) {
            	setLeadSelectionPath(selPath);
            	target = selPath.getLastPathComponent();
            	preparePopup(target);
            	if (target == null) {
         			if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: null target -> popup_empty");
            		setLeadSelectionPath(null);
              		popup_empty.show((Component)evt.getSource(), evt.getX(), evt.getY());      			
            	} else
         			if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no-null target -> popups");
            		if (target instanceof ConstituentsAddressNode){
             			if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no-null target -> popup_add");
            			popup_add.show((Component)evt.getSource(), evt.getX(), evt.getY());
            		} else
            			if (target instanceof ConstituentsIDNode) {
                 			if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: node target -> popup_leaf");
            				popup_leaf.show((Component)evt.getSource(), evt.getX(), evt.getY());
            			} else {
            				if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: instance of:\""+target+"\"");
                      		setLeadSelectionPath(null);
                    		Component c = (Component)evt.getSource();
            				if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: source:\""+c+"\"");
            				popup_empty.show(c, evt.getX(), evt.getY());  
            			}
            } else {
     			if (DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: set sel path, not trigger");
            }
        }
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.err.println("ConstituentsTree:actionPerformed: action: "+e);
    }
    public String convertValueToText(Object value,
				     boolean selected,
				     boolean expanded,
				     boolean leaf,
				     int row,
				     boolean hasFocus) {
	String prefix="";//+row+": ";
	if (value instanceof ConstituentsPropertyNode){
	    return prefix+value.toString();
	}else if(value instanceof ConstituentsBranch){
	    return prefix+value.toString();
	}
	return prefix+value.toString();
    }
    /**
     * To remove listeners before removal of this object
     * For now it does nothing
     */
	public void clean() {		
	}
}
