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

import java.awt.*;
import java.awt.event.*;
import java.net.URL;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import config.Application;
import config.DD;
import config.DDIcons;
import config.Identity;
import data.D_Witness;
import util.Util;
import static util.Util._;
//import javax.swing.plaf.metal.MetalTreeUI;
//import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;


class ConstituentsTreeCellRenderer extends DefaultTreeCellRenderer {
	final static long serialVersionUID=0;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	ConstituentsAddressNode ib=null;
	ConstituentsPropertyNode ip=null;
	ImageIcon anonym = DDIcons.getProfileImageIcon("anonumous icon"); // new ImageIcon("icons/profile.gif");
    public ConstituentsTreeCellRenderer() {
    	//Image img = anonym.getImage();
    	//anonym = new ImageIcon(img);
    	
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
    		setText(ip.property.label+":"+ip.toString());
    	}
    }
    public void getTreeCellRendererComponentCAN(Object value){
    		   	    	//ConstituentsAddressNode 
	    	ib = (ConstituentsAddressNode)value;
	    	setToolTipText(ib.getTip());
	    	
	    	if((ib!=null) && (ib.location!=null)){
	    		String ibvalue = ib.location.value;
	    		boolean censused = ib.location.censusDone;
	    		if(ibvalue==null){
	    			ibvalue=_("Unspecified Yet!");
//	    			System.out.println("ConstituentsTree:Constituent node missing for: "+ib);
//	    			System.out.println("ConstituentsTree:Constituent node missing for n_data: "+ib.n_data);
//	    			System.out.println("ConstituentsTree:Constituent node missing for n_data nID: "+ib.n_data.neighborhoodID);
//	    			System.out.println("ConstituentsTree:Constituent node missing for n_data: "+ib.n_data.toString());
//	    			System.out.println("ConstituentsTree:Constituent node missing for anc: "+Util.concat(ib.ancestors,":"));
//	    			System.out.println("ConstituentsTree:Constituent node missing for next anc: "+Util.concat(ib.next_ancestors,":"));
//	    			System.out.println("ConstituentsTree:Constituent node missing for location: "+ib.location);
	    		}
	    		String str = "<html>"+ibvalue+
	    		(censused?
	    		    (" <img src='"+DDIcons.getResourceURL(DDIcons.I_NEIGHS19)+"' style='vertical-align: middle;' align='middle' title='"+_("Neighborhoods")+"'>"+
	    			ib.neighborhoods+
	    			" <img src='"+DDIcons.getResourceURL(DDIcons.I_INHAB19)+"' style='padding-top: 0px; vertical-align: middle;' align='middle'>"+
	    			ib.location.inhabitants):""
	    			)+
	    			"</html>";
	    		//String str = "<html>"+ib.location.value+" <img src='file:icons/neighs19.gif'>"+" "+
	    		//	ib.nchildren+" "+ib.location.inhabitants+"</html>";
	    		setText(str);
	    		/*
				Dimension height = Util.getPreferredSize(str, true, 200);
				setMinimumSize(height);
				setPreferredSize(height);
				if(DEBUG) System.err.println("Renderer size="+height);
				//tree.pack();
				*/
	    	}
	}
    /**
     * 
     * @param value
     */
 	public void getTreeCellRendererComponentCIN(Object value){
	    		ConstituentsIDNode il = (ConstituentsIDNode)value;
	    		String name = il.constituent.surname;
	    		if(name==null) name = il.constituent.given_name;
	    		else if(il.constituent.given_name != null) name+=", "+il.constituent.given_name;
	    		String color="", fgcolor="", sfg="", sbg="";
	    		/*
	    		if((il.constituent.witnessed_by_me==1)){
	    			color=" bgcolor='red'"; sbg=" style='background-color:red;'";
	    		}else if(il.constituent.witnessed_by_me==2){
	    			color=" bgcolor='#F0E68C'"; // khaki
	    			sbg = " style='background-color:#F0E68C;'";
	    		}
	    		if(il.constituent.inserted_by_me){
	    			fgcolor=" fgcolor='blue'";
	    			sfg = " style='color:blue;'" ;
	    		}
	    		if(il.constituent.inserted_by_me && !il.constituent.external){
	    			fgcolor=" fgcolor='Blue'";
	    			sfg = " style='color:Blue;'" ;
	    		}
	    		*/
	    		if(il.constituent.external){
	    			fgcolor=" fgcolor='Blue'";
	    			sfg = " style='color:Blue;'" ;
	    		}
	    		if((il.constituent.witnessed_by_me==D_Witness.UNKNOWN)&&(!il.constituent.external)){
	    			color=" bgcolor='#F0E68C'"; // khaki
	    			sbg = " style='background-color:#F0E68C;'";
	    		}
	    		if((il.constituent.witnessed_by_me==D_Witness.FAVORABLE)&&(!il.constituent.external)){
	    			color=" bgcolor='#7FFFD4'"; // Aquamarine
	    			sbg = " style='background-color:#7FFFD4;'";
	    		}
	    		if((il.constituent.witnessed_by_me==D_Witness.UNFAVORABLE)&&(!il.constituent.external)){
	    			color=" bgcolor='#EE82EE'"; // Violet
	    			sbg = " style='background-color:EE82EE;'";
	    		}
	    		if((il.constituent.witnessed_by_me==D_Witness.UNKNOWN)&&(il.constituent.external)){
	    			color=" fgcolor='Blue' bgcolor='#F0E68C'"; // khaki
	    			sbg = " style='background-color:#F0E68C;color:Blue;'";
	    		}
	    		if((il.constituent.witnessed_by_me==D_Witness.FAVORABLE)&&(il.constituent.external)){
	    			color=" fgcolor='Blue' bgcolor='#7FFFD4'"; // Aquamarine
	    			sbg = " style='background-color:#7FFFD4;color:Blue;'";
	    		}
	    		if((il.constituent.witnessed_by_me==D_Witness.UNFAVORABLE)&&(il.constituent.external)){
	    			color=" fgcolor='Blue' bgcolor='EE82EE'"; //Violet
	    			sbg = " style='background-color:EE82EE;color:Blue;'";
	    		}
	    		if(il.constituent.myself==1){
	    			color=" bgcolor='Red'";
	    			sbg = " style='background-color:Red;'";
	    			fgcolor=""; sfg="";
	    		}
	    		URL up = DDIcons.getResourceURL(DDIcons.I_UP19);
	    		URL down = DDIcons.getResourceURL(DDIcons.I_DOWN19);
	    		//if(_DEBUG) System.err.println("ConstituentsTree:getTreeCellRendererComponentCIN up="+up);
	    		//if(_DEBUG) System.err.println("ConstituentsTree:getTreeCellRendererComponentCIN down="+down);
	    		String str = "<html><body"+color+fgcolor+sfg+sbg+">"+
	    			name +
	    			//((!il.constituent.external && (il.constituent.email!=null))?("&lt;"+il.constituent.email+"&gt;"):"")+
	    			" <img src='"+up+"' style='padding-top: 0px; vertical-align: middle;' align='middle'>"+
	    			il.constituent.witness_for+
	    			" <img src='"+down+"' style='padding-top: 0px; vertical-align: middle;' align='middle'>"+
	    			il.constituent.witness_against+	 
	    			((!il.constituent.external && (il.constituent.slogan!=null))?" <span style='background-color:yellow'>"+il.constituent.slogan+"</span>":"")+
	    			"</body></html>";
	    		//System.err.println(str);
	    		setText(str);
				//setIcon(getDefaultLeafIcon());
				//setIcon(getDefaultClosedIcon());
				if(il.constituent.icon==null){
					setIcon(anonym);
				}else
					setIcon(il.constituent.icon);
				setToolTipText(il.getTip());
    }
}
/*
public int getHeight(){
	if(true && (ib!=null)) {
    String str = "<html>"+ib.location.value+" <img src:icons/neighs30.gif>"+
    			ib.nchildren+" <img src:icons/anonym.gif>"+ib.location.inhabitants+"/html";
    //setText(str);
	Dimension height = Util.getPreferredSize(str, true, 200);
	//setMinimumSize(height);
	//setPreferredSize(height);
	if(DEBUG) System.err.println("Renderer height="+height.height);
	return height.height;
	} else {
		int sh = super.getHeight();
		if(DEBUG) System.err.println("Renderer height="+sh);
		return sh;
	}
}
public Dimension getMinimumSize(){
	return getPreferredSize();
}
public Dimension getPreferredSize(){
	if(false && (ib!=null)) {
    String str = "<html>"+ib.location.value+" <img src:icons/neighs30.gif>"+
    			ib.nchildren+" <img src:icons/anonym.gif>"+ib.location.inhabitants+"/html";
    //setText(str);
	Dimension height = Util.getPreferredSize(str, true, 200);
	//setMinimumSize(height);
	//setPreferredSize(height);
	if(DEBUG) System.err.println("Renderer preferred height="+height);
	return height;
	} else {
		Dimension sh = super.getPreferredSize();
		System.err.println("Renderer preferred height="+sh);
		return sh;
	}
}
*/


public class ConstituentsTree extends JTree implements TreeExpansionListener,  TreeWillExpandListener, MouseListener, ActionListener{
	final static long serialVersionUID=0;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	JFileChooser fc = new JFileChooser();
    JPopupMenu popup, popup_add, popup_leaf, popup_empty;
    ConstituentsAddAction addAction;
    //ConstituentsPropertyAddAction addPAction;
    ConstituentsDelAction delAction;
    AddEmptyNeighborhoodAction addENAction;
    /*
    EditNeighborhoodAction editNAction;
    AddNeighborhoodAction addNAction;
    AddConstituentToNeighborhoodAction addC2NAction;
    EditConstituentAction editCAction;
    VetoNeighborhoodAction vetoNAction;
    VetoTranslationAction vetoTAction;
    VetoConstituentAction vetoCAction;
    DeleteVetoConstituentAction delVCAction;
    */
    ConstituentsWitnessAction witnessAction;
    JMenuItem m_menu_add_myself, m_menu_set_myself, m_menu_add_const, m_menu_add_neighood, m_menu_del_neighood, m_witness_const;
	private ConstituentsAddMyselfAction addMeAction;
	private ConstituentsSetMyselfAction setMeAction;
	private ConstituentsCustomAction refreshAction;
    void preparePopup() {
    	if(DEBUG) System.out.println("ConstituentsTree:preparePopup: start");
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon witnessicon = DDIcons.getWitImageIcon(_("witness an item")); 
    	//ImageIcon witnessicon = Util.createImageIcon(DDIcons.I_WIT,_("witness an item"));
    	
    	ConstituentsModel model = (ConstituentsModel) getModel();
    	popup_empty = new JPopupMenu();
    	popup_leaf = new JPopupMenu();
    	popup_add = new JPopupMenu();
    	// add neighbor
    	if(model.getConstituentIDMyself()>0) {
    		addAction = new ConstituentsAddAction(this, _("Add Neighbor"),addicon,_("Add new top level."),_("You may add new identity."),KeyEvent.VK_A);
    		m_menu_add_const = new JMenuItem(addAction);//_("Add")
    		popup_add.add(m_menu_add_const);
       		m_menu_add_const = new JMenuItem(addAction);//_("Add")
    		popup_empty.add(m_menu_add_const);
    	}


    	//if(DEBUG) System.err.println("ConstituentsTree:preparePopup: menus3:"+popup_empty.getSubElements().length);
    	// add
    	if(model.getConstituentIDMyself()>0) {
    		witnessAction = new ConstituentsWitnessAction(this, _("Witness"),witnessicon,_("Witness constituents."),_("You may bear witness."),KeyEvent.VK_W);
    		m_witness_const = new JMenuItem(witnessAction);//_("Delete");
    		popup_leaf.add(m_witness_const);
    	}else{
    		if(DEBUG) System.out.println("ConstituentsTree:preparePopup: no constituentID:"+model.getConstituentIDMyself());
    		//Application.warning(_("To act, you should select/create a constituent!"), _("No constituent ID"));
    	}

    	
    	if(model.getConstituentIDMyself()>0) {
    		refreshAction = new ConstituentsCustomAction(this, _("Change Slogan"),addicon,_("Change Slogan."),_("Slogan."),KeyEvent.VK_S, ConstituentsCustomAction.SLOGAN);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_add.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_empty.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_leaf.add(m_menu_add_myself);
    		
    		if(DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");


    		refreshAction = new ConstituentsCustomAction(this, _("Move Myself to this Neighborhood"),addicon,_("Move Myself."),_("Move Myself."),KeyEvent.VK_M, ConstituentsCustomAction.MOVE);
    		//m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		//popup_leaf.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_add.add(m_menu_add_myself);
    	}

    	// add
    	addMeAction = new ConstituentsAddMyselfAction(this, _("Add Myself"),addicon,_("Add myself at top level."),_("You may detail your identity."),KeyEvent.VK_Y);
    	m_menu_add_myself = new JMenuItem(addMeAction);//_("Add")
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(addMeAction);//_("Add")
    	popup_empty.add(m_menu_add_myself);
       	if(DEBUG) System.out.println("ConstituentsTree:preparePopup: added Mysef");
		//if(DEBUG) System.err.println("ConstituentsTree:preparePopup: menus:"+popup_empty.getSubElements().length);

    	refreshAction = new ConstituentsCustomAction(this, _("Retrieve Data"),addicon,_("Retrieve Data."),_("Retrieve Data."),KeyEvent.VK_T, ConstituentsCustomAction.TOUCH);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_empty.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_leaf.add(m_menu_add_myself);
       	if(DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");

    	refreshAction = new ConstituentsCustomAction(this, _("Refresh Needed"),addicon,_("Refresh needed at top level."),_("Refresh Needed."),KeyEvent.VK_N, ConstituentsCustomAction.REFRESH_NEED);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_empty.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_leaf.add(m_menu_add_myself);
       	if(DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");

    	refreshAction = new ConstituentsCustomAction(this, _("Refresh All"),addicon,_("Refresh at top level."),_("Refresh."),KeyEvent.VK_R, ConstituentsCustomAction.REFRESH);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_empty.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_leaf.add(m_menu_add_myself);
       	if(DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");

       	if(model.automatic_refresh)
       		refreshAction = new ConstituentsCustomAction(this, _("Refresh Manually"),addicon,_("Refresh Manually."),_("Refresh Manually."),KeyEvent.VK_A, ConstituentsCustomAction.REFRESH_AUTO);
       	else
       		refreshAction = new ConstituentsCustomAction(this, _("Refresh Automatically"),addicon,_("Refresh automatically."),_("Refresh Automatically."),KeyEvent.VK_A, ConstituentsCustomAction.REFRESH_AUTO);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_add.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_empty.add(m_menu_add_myself);
    	m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    	popup_leaf.add(m_menu_add_myself);
       	if(DEBUG) System.out.println("ConstituentsTree:preparePopup: added Refresh");

    	if(model.getConstituentIDMyself()>0) {
    		refreshAction = new ConstituentsCustomAction(this, _("Expand Myself"),addicon,_("Expand myself."),_("Expand M."),KeyEvent.VK_M, ConstituentsCustomAction.MYSELF);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_add.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_empty.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_leaf.add(m_menu_add_myself);
    		
    		refreshAction = new ConstituentsCustomAction(this, _("Expand Witnessed"),addicon,_("Expand witnessed."),_("Expand W."),KeyEvent.VK_E, ConstituentsCustomAction.WITNESSED);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_add.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_empty.add(m_menu_add_myself);
    		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
    		popup_leaf.add(m_menu_add_myself);
    	}

		
		refreshAction = new ConstituentsCustomAction(this, _("Block Receiving"),addicon,_("Block."),_("Block."),KeyEvent.VK_K, ConstituentsCustomAction.BLOCK);
		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		popup_leaf.add(m_menu_add_myself);
		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		popup_add.add(m_menu_add_myself);
		//m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		//popup_empty.add(m_menu_add_myself);

		refreshAction = new ConstituentsCustomAction(this, _("Block Sending"),addicon,_("Block Sending."),_("Block Sending."),KeyEvent.VK_B, ConstituentsCustomAction.BROADCAST);
		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		popup_leaf.add(m_menu_add_myself);
		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		popup_add.add(m_menu_add_myself);

		refreshAction = new ConstituentsCustomAction(this, _("Zapp local copy"),addicon,_("Delete local."),_("Delete Local."),KeyEvent.VK_Z, ConstituentsCustomAction.ZAPP);
		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		popup_leaf.add(m_menu_add_myself);
		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		popup_add.add(m_menu_add_myself);
		
		refreshAction = new ConstituentsCustomAction(this, _("Advertise"),addicon,_("Advertise."),_("Advertise."),KeyEvent.VK_A, ConstituentsCustomAction.ADVERTISE);
		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		popup_leaf.add(m_menu_add_myself);
		m_menu_add_myself = new JMenuItem(refreshAction);//_("Add")
		popup_add.add(m_menu_add_myself);

    	
    	delAction = new ConstituentsDelAction(this, _("Delete Neighborhoods"),delicon,_("Dell constituents."),_("You may delete this information."),KeyEvent.VK_D);
    	m_menu_del_neighood = new JMenuItem(delAction);//_("Delete");
    	popup_add.add(m_menu_del_neighood);
    	
    	if(model.hasNeighborhoods) {
    		if(model.getConstituentIDMyself()>0) {
    			addENAction = new AddEmptyNeighborhoodAction(this, _("Add Empty Neighborhood"),addicon,_("Add uninitialized."),_("Add."),KeyEvent.VK_E);
    			m_menu_add_neighood = new JMenuItem(addENAction);//_("Delete");
    			popup_add.add(m_menu_add_neighood);
    			m_menu_add_neighood = new JMenuItem(addENAction);//_("Delete");
    			popup_empty.add(m_menu_add_neighood);
    		}else{
    			if(DEBUG) System.out.println("ConstituentsTree:preparePopup: no constituentID:"+model.getConstituentIDMyself());
    			//Application.warning(_("To act, you should select/create a neighborhood!"), _("No constituent ID"));
    		}
    	}
 
    	// add
    	setMeAction = new ConstituentsSetMyselfAction(this, _("Set Myself"),addicon,_("Set myself at top level."),_("You may detail your identity."),KeyEvent.VK_S);
    	m_menu_set_myself = new JMenuItem(setMeAction);//_("Add")
    	popup_leaf.add(m_menu_set_myself);
		//if(DEBUG) System.err.println("ConstituentsTree:preparePopup: menus2:"+popup_empty.getSubElements().length);
    	
    	
    	m_menu_add_myself = new JMenuItem(addMeAction);//_("Add")
    	popup_leaf.add(m_menu_add_myself);
		//if(DEBUG) System.err.println("ConstituentsTree:preparePopup: menus5:"+popup_empty.getSubElements().length);
    	/*
    	popup = new JPopupMenu();
    	// add
    	addPAction = new ConstituentsPropertyAddAction(this, _("Add"),addicon,_("Add property."),_("You may add new information about yourself."),KeyEvent.VK_A);
    	menuItem = new JMenuItem(addPAction);//_("Add")
    	//menuItem.setMnemonic(KeyEvent.VK_A);menuItem.addActionListener(this);
    	popup.add(menuItem);
    	// delete
    	delAction = new ConstituentsDelAction(this, _("Del"),delicon,_("Dell property."),_("You may delete this information about yourself."),KeyEvent.VK_D);
    	menuItem = new JMenuItem(delAction);//_("Delete");
    	//menuItem.setMnemonic(KeyEvent.VK_D);menuItem.addActionListener(this);
    	popup.add(menuItem);
    	//menuItem = new JMenuItem("Edit");menuItem.setMnemonic(KeyEvent.VK_E);
    	//menuItem.addActionListener(this);popup.add(menuItem);
    	 */
		//if(DEBUG) System.err.println("ConstituentsTree:preparePopup: menus4:"+popup_empty.getSubElements().length);
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
		//Enable tool tips.
		ToolTipManager.sharedInstance().registerComponent(this);
		ConstituentsTreeCellRenderer renderer = new ConstituentsTreeCellRenderer();
        //Icon personIcon = null;
        //renderer.setLeafIcon(personIcon);
        //renderer.setClosedIcon(personIcon);
        //renderer.setOpenIcon(personIcon);
        setCellRenderer(renderer);
		setShowsRootHandles(true);
		putClientProperty("JTree.lineStyle", "Angled" /*"Horizontal"*/);
		setRootVisible(false);
    }
    public void treeCollapsed(TreeExpansionEvent e) {
    	if(DEBUG) System.out.println("ConstituentsTree:treeCollapsed: start");
		TreePath tp = e.getPath();
		if(DEBUG) System.err.println("ConstituentsTree:treeCollapsed: F: treeCollapsed "+tp);
		Object obj=tp.getLastPathComponent();//e.getSource();
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
	    	if (branch.nchildren==0){
	    		branch.nchildren = 1;
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
   		//if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: menus:\""+popup_empty.getSubElements().length);
    	TreePath selPath = getPathForLocation(evt.getX(), evt.getY());
    	if(selPath!=null) target = selPath.getLastPathComponent();
   	    if (!evt.isPopupTrigger() && ((target == null) || !(target  instanceof ConstituentsIDNode))) {
   	    	
 			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no popup");
    		return;
    	}
   	    //preparePopup();
         if(Identity.current_id_branch == null) {
        	Application.warning(_("To modify you need to select an identity"), _("Missing identity"));
        	DD.tabbedPane.setSelectedComponent(DD.identitiesPane);
 			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no current ID");
        	return;
        }
        //System.err.println("mouse release: "+selPath);
        if (selPath == null){
        	if (evt.isPopupTrigger()) {
        		//System.err.println("popup: "+selPath);
        		setLeadSelectionPath(null);
            	preparePopup();
        		popup_empty.show((Component)evt.getSource(), evt.getX(), evt.getY());
        	}else{
     			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no sel path, no trigger");
        	}
 			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no sel path");
            return;
        }else{
            if (true || evt.isPopupTrigger()) {
            	preparePopup();
            	setLeadSelectionPath(selPath);
            	target = selPath.getLastPathComponent();
            	if(target == null){
         			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: null target -> popup_empty");
            		setLeadSelectionPath(null);
              		popup_empty.show((Component)evt.getSource(), evt.getX(), evt.getY());      			
            	}else
         			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no-null target -> popups");
            		//System.err.println("popup: "+selPath);
            		if(target instanceof ConstituentsAddressNode){
             			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: no-null target -> popup_add");
            			popup_add.show((Component)evt.getSource(), evt.getX(), evt.getY());
            		}else
            			if(target instanceof ConstituentsIDNode) {
                 			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: node target -> popup_leaf");
            				popup_leaf.show((Component)evt.getSource(), evt.getX(), evt.getY());
            			} else {
            		   	    //preparePopup();
            				if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: instance of:\""+target+"\"");
                       		//if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: menus:\""+popup_empty.getSubElements().length);
                      		setLeadSelectionPath(null);
                    		//if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: menus:\""+popup_empty.getSubElements().length);
                    		Component c = (Component)evt.getSource();
            				if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: source:\""+c+"\"");
            				popup_empty.show(c, evt.getX(), evt.getY());  
            			}
            }else{
     			if(DEBUG) System.err.println("ConstituentsTree:jTreeMouseReleased: set sel path, not trigger");
            	//System.err.println("no popup trigger: "+selPath);
            }
        }
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.err.println("ConstituentsTree:actionPerformed: action: "+e);
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
	if(value instanceof ConstituentsPropertyNode){
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
