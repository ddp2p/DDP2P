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
package widgets.org;

import static util.Util._;

import hds.DebateDecideAction;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.DBSelector;
import util.Util;
import widgets.components.BulletRenderer;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

//import apple.laf.CoreUIUtils.Tree;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import config.DDIcons;
import config.Identity;
import data.D_Organization;

@SuppressWarnings("serial")
class OrgPluginMenuItem extends JMenuItem {}
interface OrgPluginRenderer extends TableCellRenderer{}
interface OrgPluginEditor extends TableCellEditor{}
interface OrgPluginAction extends Action {}
class PluginData{
	OrgPluginEditor editor;
	OrgPluginRenderer renderer;
	OrgPluginRenderer absent_renderer;
	String plugin_name;
	String plugin_ID;
	PluginData(String plugin_ID, String plugin_name, OrgPluginEditor editor, OrgPluginRenderer renderer){
		this.editor = editor;
		this.renderer = renderer;
		this.plugin_ID = plugin_ID;
		this.plugin_name = plugin_name;
	}
}
class PluginMenus{
	String plugin_ID;
	String plugin_name;
	ArrayList<OrgPluginAction> plugin_menu_action = new ArrayList<OrgPluginAction>();
	ArrayList<OrgPluginMenuItem> plugin_menu_item = new ArrayList<OrgPluginMenuItem>();
	PluginMenus(String plugin_ID, String plugin_name){
		this.plugin_ID = plugin_ID;
		this.plugin_name = plugin_name;
	}
	void add(int col, OrgPluginMenuItem plugin_menu_item){
		this.plugin_menu_item.add(plugin_menu_item);		
	}
	void add(int col, OrgPluginAction plugin_menu_action){
		this.plugin_menu_action.add(plugin_menu_action);
	}
}

@SuppressWarnings("serial")
public class Orgs extends JTable implements MouseListener {
	// Different icons should be displayed for each state... for now just on/off
	public static final int STATE_CONNECTION_FAIL =0;
	public static final int STATE_CONNECTION_TCP = 1;
	public static final int STATE_CONNECTION_UDP = 2;
	public static final int STATE_CONNECTION_UDP_NAT = 3;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DIM_X = 0;
	private static final int DIM_Y = 50;
	public static final int A_NON_FORCE_COL = 4;
	public static final int FORCE_THREASHOLD_COL = 3; // first non-force row

	BulletRenderer bulletRenderer = new BulletRenderer();
	ColorRenderer colorRenderer;
	DefaultTableCellRenderer centerRenderer;
	

	Hashtable<String,PluginData> plugin_applets = new Hashtable<String, PluginData>();
	Hashtable<Integer,Hashtable<String,PluginMenus>> plugin_menus = new Hashtable<Integer,Hashtable<String,PluginMenus>>();
	ArrayList<String> plugins= new ArrayList<String>();
	
	public Orgs() {
		super(new OrgsModel(Application.db));
		init();
	}
	public Orgs(DBInterface _db) {
		super(new OrgsModel(_db));
		init();
	}
	public Orgs(OrgsModel dm) {
		super(dm);
		init();
	}
	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		colorRenderer = new ColorRenderer(getModel());
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
		
   		try{
   			if (Identity.getCurrentIdentity().identity_id!=null) {
    			//long id = new Integer(Identity.current.identity_id).longValue();
    			long orgID = Identity.getDefaultOrgID();
    			this.setCurrent(orgID);
    			int row =this.getSelectedRow();
     			this.fireListener(row, A_NON_FORCE_COL);
   			}
    	}catch(Exception e){e.printStackTrace();}
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		//this.setMinimumSize(new Dimension(400,200));
		//scrollPane.setMinimumSize(new Dimension(400,200));
		return scrollPane;
	}
    public JPanel getPanel() {
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        //scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setPreferredSize(new Dimension(DIM_X, DIM_Y));
        jp.add(scrollPane, BorderLayout.CENTER);
		return jp;
    }

	public TableCellRenderer getCellRenderer(int row, int column) {
		if ((column == OrgsModel.TABLE_COL_NAME)) return colorRenderer;
		if ((column == OrgsModel.TABLE_COL_CONNECTION)) return bulletRenderer;
		if ((column == getModel().TABLE_COL_CONSTITUENTS_NB)) return centerRenderer;
		if ((column == getModel().TABLE_COL_ACTIVITY)) return centerRenderer;
		if ((column == getModel().TABLE_COL_NEWS)) return centerRenderer;
		if (column >= OrgsModel.TABLE_COL_PLUGINS) {
			int plug = column-OrgsModel.TABLE_COL_PLUGINS;
			if(plug < plugins.size()) {
				String pluginID= plugins.get(plug);
				return plugin_applets.get(pluginID).renderer;
			}
		}
		return super.getCellRenderer(row, column);
	}
	protected String[] columnToolTips = {null,null,_("A name you provide")};
    @SuppressWarnings("serial")
	protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
               java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                if(realIndex >= columnToolTips.length) return null;
				return columnToolTips[realIndex];
            }
        };
    }
	public void setConnectionState(String peerID, int state){
		((OrgsModel) this.getModel()).setConnectionState(peerID, state);
	}
    /*
	public void setUDPOn(String address, Boolean on){
		((PeersModel) this.getModel()).setUDPOn(address, on);
	}
	public void setTCPOn(String address, Boolean on){
		((PeersModel) this.getModel()).setTCPOn(address, on);
	}
	public void setNATOn(String address, Boolean on){
		((PeersModel) this.getModel()).setNATOn(address, on);
	}
	*/
	public OrgsModel getModel(){
		return (OrgsModel) super.getModel();
	}
	private void initColumnSizes() {
        OrgsModel model = (OrgsModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        for (int i = 0; i < model.getColumnCount(); i++) {
        	int headerWidth = 0;
        	int cellWidth = 0;
        	column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(i)).
                             getTableCellRendererComponent(
                                 this, getValueAt(r, i),
                                 false, false, 0, i);
            	cellWidth = Math.max(comp.getPreferredSize().width, cellWidth);
            }
            if (DEBUG) {
                System.out.println("Initializing width of column "
                                   + i + ". "
                                   + "headerWidth = " + headerWidth
                                   + "; cellWidth = " + cellWidth);
            }
 
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }
	@Override
	public void mouseClicked(MouseEvent evt) {
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	//if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        if((row<0)||(col<0)) return;
        
    	OrgsModel model = (OrgsModel)getModel();
 		int model_row=convertRowIndexToModel(row);
   	   	if(model_row>=0) {
   	   		String orgID = Util.getString(model._orgs[model_row]);
   	   		try{
   	   			long oID = new Integer(orgID).longValue();
   	   			model.setCurrent(oID);
   	   		}catch(Exception e){};
   	   	}
        
        fireListener(row,col);
	}
	ArrayList<OrgListener> listeners=new ArrayList<OrgListener>();
	private D_Organization organization_crt;
	public void fireForceEdit(String orgID) {		
		if(DEBUG) System.out.println("Orgs:fireForceEdit: row="+orgID);
		organization_crt = new D_Organization();		
		for(OrgListener l: listeners){
			if(DEBUG) System.out.println("Orgs:fireForceEdit: l="+l);
			try{
				if(orgID==null) ;//l.forceEdit(orgID);
				else l.org_forceEdit(orgID, organization_crt);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	long _org_crt = -1;
	Object old_org_signature = null; //to catch editing events
	public void fireListener(int row, int col) {
		if(DEBUG) System.out.println("Orgs:fireListener: row="+row);
		String orgID = null;
		int model_row;
		OrgsModel model = this.getModel();
		if(row>=0) {
			model_row = this.convertRowIndexToModel(row);
			if ((model_row>=0) && (model_row<model._orgs.length))
				orgID = Util.getString(model._orgs[model_row]);
		}
		long _orgID = Util.lval(orgID,-1);
		if((old_org_signature!=null) && !DD.ORG_UPDATES_ON_ANY_ORG_DATABASE_CHANGE) {
			if(_orgID==_org_crt){
				if(DEBUG)System.out.println("Orgs:fireListener: action dropped, same orgID "+_orgID);
				return;
			}
		}
		_org_crt = _orgID;
		organization_crt = null;
		if(_orgID>0)
			try {
				organization_crt = new D_Organization(_orgID);
				old_org_signature = organization_crt.signature;
			} catch (Exception e1) {
				e1.printStackTrace();
				orgID = "-1";
				old_org_signature = null;
			}
		for(OrgListener l: listeners){
			if(DEBUG) System.out.println("Orgs:fireListener: l="+l);
			try{
				if(row<0) l.orgUpdate(null, col, organization_crt);
				else l.orgUpdate(orgID, col, organization_crt);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addListener(OrgListener l){
		listeners.add(l);
		int row =this.getSelectedRow();
		if(row>=0)
			l.orgUpdate(Util.getString(this.getModel()._orgs[this.convertRowIndexToModel(row)]),A_NON_FORCE_COL, organization_crt);
	}
	public void removeListener(OrgListener l){
		listeners.remove(l);
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
	}
	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);		
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);
	}
	public void setCurrent(long org_id) {
		getModel().setCurrent(org_id);
	}
	JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	//OrgExtraUpAction uAction;
    	//OrgExtraDownAction prAction;
    	OrgsDeleteAction pdAction;
    	OrgsAddAction aAction;
    	OrgsForceEditAction eAction;
    	OrgsToggleServingAction sAction;
    	
    	aAction = new OrgsAddAction(this, _("Add!"), delicon,_("Add new organization."), _("Add"),KeyEvent.VK_A);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	if(row<0) return popup;
    	
    	// uAction = new PeersUseAction(this, _("Toggle"),addicon,_("Toggle it."),_("Will be used to synchronize."),KeyEvent.VK_A);
       	//uAction = new OrgExtraUpAction(this, _("Use!"),addicon,_("Use it."),_("Will be used to synchronize."),KeyEvent.VK_A);
    	//uAction.putValue("row", new Integer(row));
    	//menuItem = new JMenuItem(uAction);
    	//popup.add(menuItem);
    	//
    	//prAction = new OrgExtraDownAction(this, _("Reset!"), reseticon,_("Bring again all data from this."), _("Go restart!"),KeyEvent.VK_R);
    	//prAction.putValue("row", new Integer(row));
    	//popup.add(new JMenuItem(prAction));
    	//
    	pdAction = new OrgsDeleteAction(this, _("Delete!"), delicon,_("Delete all data about this."), _("Delete"),KeyEvent.VK_D);
    	pdAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(pdAction));
    	
    	if(getModel().isServing(row))
    		sAction = new OrgsToggleServingAction(this, _("Stop Advertising!"), delicon,_("Stop advertising this organization."), _("Stop Advertising"),KeyEvent.VK_S);
    	else
       		sAction = new OrgsToggleServingAction(this, _("Advertise!"), delicon,_("Advertise this organization."), _("Advertise"),KeyEvent.VK_S);
    	sAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(sAction));
    	
    	eAction = new OrgsForceEditAction(this, _("Force Edit!"), delicon,_("Force editing rights."), _("Edit"),KeyEvent.VK_E);
    	eAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(eAction));
    	
    	/*
    	popup.addSeparator();
    	Hashtable<String, PluginMenus> mn = this.plugin_menus.get(new Integer(col));
    	if(mn == null) return popup;
    	for(String a : mn.keySet()){
    		PluginMenus pm = mn.get(a);
    		for(PeerPluginAction pa: pm.plugin_menu_action) popup.add(pa);
    		for(PeerPluginMenuItem ma: pm.plugin_menu_item) popup.add(ma);
    	}
    	*/
    	return popup;
	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	int row; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0) row = this.convertRowIndexToModel(row);
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
}


@SuppressWarnings("serial")
class OrgsForceEditAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    static final boolean _DEBUG = true;
	Orgs tree; ImageIcon icon;
    public OrgsForceEditAction(Orgs tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG)System.err.println("row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
       		if(DEBUG)System.err.println("Row selected: " + row);
    	}
    	OrgsModel model = (OrgsModel)tree.getModel();
     	if(row<0) return;
    	String orgID = Util.getString(model._orgs[row]);
    	tree.fireForceEdit(orgID);
    }
}

@SuppressWarnings("serial")
class OrgsDeleteAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    static final boolean _DEBUG = true;
	Orgs tree; ImageIcon icon;
    public OrgsDeleteAction(Orgs tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG)System.err.println("row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
       		System.err.println("Row selected: " + row);
    	}
    	OrgsModel model = (OrgsModel)tree.getModel();
     	if(row<0) return;
    	String orgID = Util.getString(model._orgs[row]);
    	model.setCurrent(-1);
    	try {
			//Application.db.delete(table.field_value.TNAME, new String[]{table.field_value.organization_ID}, new String[]{orgID}, DEBUG);
			//Application.db.delete(table.witness.TNAME, new String[]{table.witness.organization_ID}, new String[]{orgID}, DEBUG);
			//Application.db.delete(table.justification.TNAME, new String[]{table.justification.organization_ID}, new String[]{orgID}, DEBUG);
			//Application.db.delete(table.signature.TNAME, new String[]{table.signature.organization_ID}, new String[]{orgID}, DEBUG);
    		String sql="SELECT "+table.constituent.constituent_ID+" FROM "+table.constituent.TNAME+" WHERE "+table.constituent.organization_ID+"=?;";
    		ArrayList<ArrayList<Object>> constits = Application.db.select(sql, new String[]{orgID}, DEBUG);
    		for(ArrayList<Object> a: constits) {
    			String cID = Util.getString(a.get(0));
    			Application.db.delete(table.witness.TNAME, new String[]{table.witness.source_ID}, new String[]{cID}, DEBUG);
    			Application.db.delete(table.witness.TNAME, new String[]{table.witness.target_ID}, new String[]{cID}, DEBUG);

    			Application.db.delete(table.motion.TNAME, new String[]{table.motion.constituent_ID}, new String[]{cID}, DEBUG);
    			Application.db.delete(table.justification.TNAME, new String[]{table.justification.constituent_ID}, new String[]{cID}, DEBUG);
       			Application.db.delete(table.signature.TNAME, new String[]{table.signature.constituent_ID}, new String[]{cID}, DEBUG);
       			// Application.db.delete(table.news.TNAME, new String[]{table.news.constituent_ID}, new String[]{cID}, DEBUG);
       		}
   			Application.db.delete(table.news.TNAME, new String[]{table.news.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.delete(table.motion.TNAME, new String[]{table.motion.organization_ID}, new String[]{orgID}, DEBUG);
			String sql_del_fv_fe =
					"DELETE FROM "+table.field_value.TNAME+
					" WHERE "+table.field_value.field_extra_ID+
					" IN ( SELECT "+table.field_extra.field_extra_ID+" FROM "+table.field_extra.TNAME+" WHERE "+table.field_extra.organization_ID+"=? )";
			Application.db.delete(sql_del_fv_fe, new String[]{orgID}, DEBUG);
			String sql_del_fv =
					"DELETE FROM "+table.field_value.TNAME+
					" WHERE "+table.field_value.constituent_ID+
					" IN ( SELECT DISTINCT c."+table.constituent.constituent_ID+" FROM "+table.constituent.TNAME+" AS c " +
							" JOIN "+table.field_value.TNAME+" AS v ON (c."+table.constituent.constituent_ID+"=v."+table.field_value.constituent_ID+") " +
									" WHERE c."+table.constituent.organization_ID+"=? GROUP BY c."+table.constituent.constituent_ID+" ) ";
			Application.db.delete(sql_del_fv, new String[]{orgID}, DEBUG);
			Application.db.delete(table.constituent.TNAME, new String[]{table.constituent.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.delete(table.neighborhood.TNAME, new String[]{table.neighborhood.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.delete(table.field_extra.TNAME, new String[]{table.field_extra.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.delete(table.identity_ids.TNAME, new String[]{table.identity_ids.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.update(table.identity.TNAME, new String[]{table.identity.organization_ID}, new String[]{table.identity.organization_ID}, new String[]{"-1",orgID}, DEBUG);
			Application.db.delete(table.organization.TNAME, new String[]{table.organization.organization_ID}, new String[]{orgID}, DEBUG);
		} catch (SQLiteException e1) {
			e1.printStackTrace();
		}
    }
}
@SuppressWarnings("serial")
class OrgsAddAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Orgs tree; ImageIcon icon;
    public OrgsAddAction(Orgs tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row = -1;
    	String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    		//org_id = Util.getString(act.getValue("org"));
            //System.err.println("row property: " + row);
    	} else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
    		//org_id = tree.getModel().org_id;
    		//System.err.println("Row selected: " + row);
    	}
    	OrgsModel model = (OrgsModel)tree.getModel();
    	
    	Application.appObject.actionPerformed(new ActionEvent(tree, row, DD.COMMAND_NEW_ORG));
    }
}
@SuppressWarnings("serial")
class OrgsToggleServingAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Orgs tree; ImageIcon icon;
    public OrgsToggleServingAction(Orgs tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    		//org_id = Util.getString(act.getValue("org"));
            //System.err.println("row property: " + row);
    	} else {
    		row = tree.getSelectedRow();
       		row = tree.convertRowIndexToModel(row);
   		//org_id = tree.getModel().org_id;
    		//System.err.println("Row selected: " + row);
    	}
    	OrgsModel model = (OrgsModel)tree.getModel();
    	model.toggleServing(row);
    	
    	//Application.appObject.actionPerformed(new ActionEvent(tree, row, DD.COMMAND_NEW_ORG));
    }
}

@SuppressWarnings("serial")
class OrgsModel extends AbstractTableModel implements TableModel, DBListener {
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_CREATOR = 1; // certified by trusted?
	public static final int TABLE_COL_CATEGORY = 2; // certified by trusted?
	public static final int TABLE_COL_CONSTITUENTS_NB = 3;
	public static final int TABLE_COL_ACTIVITY = 4; // number of votes + news
	public static final int TABLE_COL_CONNECTION = 5; // any activity in the last x days?
	public static final int TABLE_COL_NEWS = 6; // unread news?
	public static final int TABLE_COL_PLUGINS = 7;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	DBInterface db;
	Object _orgs[]=new Object[0];
	Object _meth[]=new Object[0];
	Object _hash[]=new Object[0];
	Object _crea[]=new Object[0];
	boolean[] _gid=new boolean[0];
	boolean[] _blo=new boolean[0]; // block
	boolean[] _req=new boolean[0]; // request
	boolean[] _bro=new boolean[0]; // broadcast
	String columnNames[]={_("Name"),_("Initiator"),_("Category"),_("Constituents"),_("Activity"),_("Hot"),_("News"),_("Plugins")};
	ArrayList<Orgs> tables= new ArrayList<Orgs>();
	public OrgsModel(DBInterface _db) {
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.organization.TNAME)), null);
		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
		update(null, null);
	}
	public boolean isServing(int row) {
		if(DEBUG) System.out.println("\n************\nOrgs:OgsModel:isServing: row="+row);
		return isBroadcasted(row);
		/*
		boolean result= false;
		try {
			if(isServingAndPresent(row)) result = true;
		} catch (Exception e) {}
		if(DEBUG) System.out.println("Orgs:OgsModel:isServing: exit with="+result);
		if(DEBUG) System.out.println("*****************");
		return result;
		*/
	}
	public boolean isServingAndPresent(int row) throws Exception {
		boolean result= false;
		if(DEBUG) System.out.println("\n************\nOrgs:OgsModel:isServingAndPresent: row="+row);
		ArrayList<ArrayList<Object>> s;
		String sql = "SELECT "+table.peer_org.served+
			" FROM "+table.peer_org.TNAME+
			" LEFT JOIN "+table.peer.TNAME+" ON("+table.peer.peer_ID+"="+table.peer_org.peer_ID+") "+
			" WHERE "+table.peer.global_peer_ID +" = ? AND "+table.peer_org.organization_ID+" = ?;";
		String global_peer_ID = Identity.current_peer_ID.globalID;
		//String pID = table.peer.getLocalPeerID(global_peer_ID);
		String organization_ID = Util.getString(this._orgs[row]);
		s = Application.db.select(sql, new String[]{ global_peer_ID, organization_ID}, DEBUG);
		if(s.size()==0) throw new Exception("No record found");//return false;
		if("1".equals(s.get(0).get(0))) result = true;
		if(DEBUG) System.out.println("Orgs:OgsModel:isServingAndPresent: exit with="+result);
		if(DEBUG) System.out.println("*****************");
		return result;
	}
	public boolean toggleServing(int row) {
		if(_DEBUG) System.out.println("\n************\nOrgs:OrgsModel:toggleServing: row="+row);
		String organization_ID = Util.getString(this._orgs[row]);
		boolean result = toggleServing(organization_ID, true, false);
		if(_DEBUG) System.out.println("Orgs:OrgsModel:toggleServing: result="+result+"\n************\n");
		return result;
	}
	/**
	 * Sets serving both in peer.served_orgs and in organization.broadcasted
	 * has to sign the peer again because of served_orgs changes
	 * @param organization_ID
	 * @param toggle
	 * @param val
	 * @return
	 */
	public static boolean toggleServing(String organization_ID, boolean toggle, boolean val) {
		if(_DEBUG) System.err.println("\n************\nOrgs:OrgsModel:toggleServing: orgID=" + Util.trimmed(organization_ID)+" toggle="+toggle+" val="+val);
		boolean serving = true;
		
		// First set it in served orgs
		String global_peer_ID = Identity.current_peer_ID.globalID;
		String peer_ID;
		try {// get my peerID
			peer_ID = table.peer.getLocalPeerID(global_peer_ID);
		} catch (SQLiteException e2) {
			e2.printStackTrace();
			return false;
		}
		try {// is it in served org?
			ArrayList<ArrayList<Object>> s;
			String sql = "SELECT "+table.peer_org.served+
				" FROM "+table.peer_org.TNAME+
				" WHERE "+table.peer_org.peer_ID +" = ? AND "+table.peer_org.organization_ID+" = ?;";
			s = Application.db.select(sql, new String[]{ peer_ID, organization_ID}, _DEBUG);
			if(s.size()==0) throw new Exception("No record found");//return false;
			if(Util.stringInt2bool(Util.getString(s.get(0).get(0)), false)) serving = true;
			else serving = false;
			if(_DEBUG) System.out.println("\n************\nOrgs:OgsModel:toggleServing: old serving=" +serving);

			//set it in served orgs
			if(toggle) val = !serving;
			String _serving = Util.bool2StringInt(val);
			Application.db.update(table.peer_org.TNAME, new String[]{table.peer_org.served},
					new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID},
					new String[]{_serving, peer_ID, organization_ID}, _DEBUG);
			setBroadcasting(organization_ID, val); // set in organization.broadcasted
		} catch (Exception e) {
			// if not serve so far, serve now!
			try {
				if(_DEBUG) System.out.println("\n************\nOrgs:OgsModel:toggleServing: old serving=" +serving);
				if(toggle) val = !serving;
				String _serving = Util.bool2StringInt(val);
				Application.db.insert(table.peer_org.TNAME,
						new String[]{table.peer_org.served,table.peer_org.peer_ID,table.peer_org.organization_ID},
						new String[]{_serving, peer_ID, organization_ID}, _DEBUG);
				setBroadcasting(organization_ID, val);
			} catch (SQLiteException e1) {
				e1.printStackTrace();
				return false;
			}
		}
		// need to sign again my peer due to served_orgs
		hds.Server.update_my_peer_ID_peers_name_slogan();
		if(_DEBUG) System.out.println("Orgs:OgsModel:toggleServing: exit with="+(!serving));
		if(_DEBUG) System.out.println("*****************");
		return !serving;
	}
	public void setCurrent(long org_id) {
		if(DEBUG) System.out.println("Orgs:OrgsModel:setCurrent: id="+org_id);
		if(org_id<0){
			for(Orgs o: tables){
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(-1, -1);
				o.fireListener(-1, 0);
			}	
			if(DEBUG) System.out.println("Orgs:setCurrent: Done -1");
			return;
		}
		//this.fireTableDataChanged();
		for(int k=0;k<_orgs.length;k++){
			Object i = _orgs[k];
			if(DEBUG) System.out.println("Orgs:setCurrent: k="+k+" row_org_ID="+i);
			if(i instanceof Integer){
				Integer id = (Integer)i;
				if(id.longValue()==org_id) {
					try {
						//long constituent_ID = 
						if(DEBUG) System.out.println("Orgs:setCurrent: will set current org");
						Identity.setCurrentOrg(org_id);
						
					} catch (SQLiteException e) {
						e.printStackTrace();
					}
					
					for(Orgs o: tables){
						int tk = o.convertRowIndexToView(k);
						o.setRowSelectionAllowed(true);
						ListSelectionModel selectionModel = o.getSelectionModel();
						selectionModel.setSelectionInterval(tk, tk);
						//o.requestFocus();
						o.scrollRectToVisible(o.getCellRect(tk, 0, true));
						//o.setEditingRow(k);
						//o.setRowSelectionInterval(k, k);
						o.fireListener(k, 0);
					}
					break;
				}
			}
		}
		if(DEBUG) System.out.println("Orgs:setCurrent: Done");
	}
	public boolean isCellEditable(int row, int col) {
		switch(col){
		case TABLE_COL_NAME:
		case TABLE_COL_CREATOR:
		case TABLE_COL_CATEGORY:
			return true;
		}
		return false;
	}

	public void setTable(Orgs orgs) {
		tables.add(orgs);
	}

	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("\nwidgets.org.Orgs: update table= "+_table+": info= "+info);
		String sql = "SELECT "+table.organization.organization_ID+","+table.organization.certification_methods+","
		+table.organization.global_organization_ID_hash+","+table.organization.creator_ID+
		  ","+table.organization.blocked+","+table.organization.broadcasted+","+table.organization.requested
		+" FROM "+table.organization.TNAME+";";
		Object old_sel[] = new Object[tables.size()];
		for(int i=0; i<old_sel.length; i++){
			int sel = tables.get(i).getSelectedRow();
			if((sel >= 0) && (sel < _orgs.length)) old_sel[i] = _orgs[sel];
		}
		try {
			ArrayList<ArrayList<Object>> orgs = db.select(sql, new String[]{});
			_orgs = new Object[orgs.size()];
			_meth = new Object[orgs.size()];
			_hash = new Object[orgs.size()];
			_crea = new Object[orgs.size()];
			_gid = new boolean[orgs.size()];
			_blo = new boolean[orgs.size()];
			_bro = new boolean[orgs.size()];
			_req = new boolean[orgs.size()];
			for(int k=0; k<_orgs.length; k++) {
				_orgs[k] = orgs.get(k).get(0);
				_meth[k] = orgs.get(k).get(1);
				_hash[k] = orgs.get(k).get(2);
				_crea[k] = orgs.get(k).get(3);
				_gid[k] = (orgs.get(k).get(3) != null);
				_blo[k] = "1".equals(orgs.get(k).get(4));
				_bro[k] = "1".equals(orgs.get(k).get(5));
				_req[k] = "1".equals(orgs.get(k).get(6));
			}
			if(DEBUG) System.out.println("widgets.org.Orgs: A total of: "+_orgs.length);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		for(int k=0; k<old_sel.length; k++){
			Orgs i = tables.get(k);
			//int row = i.getSelectedRow();
			int row = findRow(old_sel[k]);
			if(DEBUG) System.out.println("widgets.org.Orgs: selected row: "+row);
			//i.revalidate();
			this.fireTableDataChanged();
			if((row >= 0)&&(row<_orgs.length)) i.setRowSelectionInterval(row, row);
			i.fireListener(row, Orgs.A_NON_FORCE_COL);
		}
	}

	private int findRow(Object id) {
		if(id==null) return -1;
		for(int k=0; k < _orgs.length; k++) if(id.equals(_orgs[k])) return k;
		return -1;
	}
	public void setConnectionState(String peerID, int state) {
		
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_CONNECTION) return Boolean.class;
		if(col == this.TABLE_COL_ACTIVITY) return Integer.class;
		if(col == this.TABLE_COL_CONSTITUENTS_NB) return Integer.class;
		if(col == this.TABLE_COL_NEWS) return Integer.class;
		
		return String.class;
	}
	@Override
	public int getRowCount() {
		return _orgs.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		Object result = null;
		String orgID = Util.getString(this._orgs[row]);
		switch(col) {
		case TABLE_COL_NAME:
			String sql = "SELECT o."+table.organization.name + ", m."+table.my_organization_data.name+
					" FROM "+table.organization.TNAME+" AS o" +
					" LEFT JOIN "+table.my_organization_data.TNAME+" AS m " +
							" ON (o."+table.organization.organization_ID+" = m."+table.my_organization_data.organization_ID+")" +
					" WHERE o."+table.organization.organization_ID+"= ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql, new String[]{orgID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = orgs.get(0).get(1);
						if(DEBUG)System.out.println("Orgs:Got my="+result);
					}
					else{
						result = orgs.get(0).get(0);
						if(DEBUG)System.out.println("Orgs:Got my="+result);
					}
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_CREATOR:
			String sql_cr =
				"SELECT o."+table.organization.creator_ID+", m."+table.my_organization_data.creator+", p."+table.peer.name+
				" FROM "+table.organization.TNAME+" AS o" +
				" LEFT JOIN "+table.my_organization_data.TNAME+" AS m "+" ON(o."+table.organization.organization_ID+"=m."+table.my_organization_data.organization_ID+")"+
				" LEFT JOIN "+table.peer.TNAME+" AS p "+" ON(o."+table.organization.creator_ID+"=p."+table.peer.peer_ID+")"+
				" WHERE o."+table.organization.organization_ID+" = ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cr, new String[]{orgID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = Util.getString(orgs.get(0).get(1));
						if(DEBUG)System.out.println("Orgs:Got my="+result);
					}
					else{
						result = Util.getString(orgs.get(0).get(2));
						if(DEBUG)System.out.println("Orgs:Got my="+result);
					}
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_CATEGORY:
			String sql_cat = "SELECT o."+table.organization.category + ", m."+table.my_organization_data.category+
					" FROM "+table.organization.TNAME+" AS o" +
					" LEFT JOIN "+table.my_organization_data.TNAME+" AS m " +
							" ON (o."+table.organization.organization_ID+" = m."+table.my_organization_data.organization_ID+")" +
					" WHERE o."+table.organization.organization_ID+"= ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cat, new String[]{orgID});
				if(orgs.size()>0)
					if(orgs.get(0).get(1)!=null){
						result = orgs.get(0).get(1);
						if(DEBUG)System.out.println("Orgs:Got my="+result);
					}
					else{
						result = orgs.get(0).get(0);
						if(DEBUG)System.out.println("Orgs:Got my="+result);
					}
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_CONSTITUENTS_NB:
			String sql_co = "SELECT count(*) FROM "+table.constituent.TNAME+
			" WHERE "+table.constituent.organization_ID+" = ? AND "+table.constituent.op+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_co, new String[]{orgID, "1"});
				if(orgs.size()>0) result = orgs.get(0).get(0);
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_ACTIVITY: // number of votes + news
			String sql_ac = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(s."+table.signature.motion_ID+"=m."+table.motion.motion_ID+")"+
			" WHERE "+table.motion.organization_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac, new String[]{orgID});
				if(orgs.size()>0) result = orgs.get(0).get(0);
				else result = new Integer("0");
			} catch (SQLiteException e) {
				e.printStackTrace();
				break;
			}
			
			String sql_new = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE "+table.news.organization_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new, new String[]{orgID});
				if(orgs.size()>0) result = new Integer(""+(((Integer)result).longValue()+((Integer)orgs.get(0).get(0)).longValue()));
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_CONNECTION: // any activity in the last x days?
			int DAYS_OLD2 = 10;
			String sql_ac2 = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(s."+table.signature.motion_ID+"=m."+table.motion.motion_ID+")"+
			" WHERE m."+table.motion.organization_ID+" = ? AND s."+table.signature.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_ac2, new String[]{orgID,Util.getGeneralizedDate(DAYS_OLD2)});
				if(orgs.size()>0) result = orgs.get(0).get(0);
				else result = new Integer("0");
			} catch (SQLiteException e) {
				e.printStackTrace();
				break;
			}
			
			String sql_new2 = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.organization_ID+" = ? AND n."+table.news.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new2, new String[]{orgID,Util.getGeneralizedDate(DAYS_OLD2)});
				if(orgs.size()>0){
					int result_int = new Integer(""+(((Integer)result).longValue()+((Integer)orgs.get(0).get(0)).longValue()));
					if(result_int>0) result = new Boolean(true); else result = new Boolean(false);
				}
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_NEWS: // unread news?
			int DAYS_OLD = 10;
			String sql_news = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.organization_ID+" = ? AND n."+table.news.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_news, new String[]{orgID,Util.getGeneralizedDate(DAYS_OLD)});
				if(orgs.size()>0) result = orgs.get(0).get(0);
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_PLUGINS:
		default:
		}
		return result;
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		switch(col) {
		case TABLE_COL_NAME:
			set_my_data(table.my_organization_data.name, Util.getString(value), row);
			break;
		case TABLE_COL_CREATOR:
			set_my_data(table.my_organization_data.creator, Util.getString(value), row);
			break;
		case TABLE_COL_CATEGORY:
			set_my_data(table.my_organization_data.category, Util.getString(value), row);
			break;
		}
		fireTableCellUpdated(row, col);
	}
	private void set_my_data(String field_name, String value, int row) {
		if(row >= _orgs.length) return;
		if("".equals(value)) value = null;
		if(DEBUG)System.out.println("Set value =\""+value+"\"");
		String org_ID = Util.getString(_orgs[row]);
		try {
			String sql = "SELECT "+field_name+" FROM "+table.my_organization_data.TNAME+" WHERE "+table.my_organization_data.organization_ID+"=?;";
			ArrayList<ArrayList<Object>> orgs = db.select(sql,new String[]{org_ID});
			if(orgs.size()>0){
				db.update(table.my_organization_data.TNAME, new String[]{field_name},
						new String[]{table.my_organization_data.organization_ID}, new String[]{value, org_ID});
			}else{
				if(value==null) return;
				db.insert(table.my_organization_data.TNAME,
						new String[]{field_name,table.my_organization_data.organization_ID},
						new String[]{value, org_ID});
			}
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}
	public boolean isMine(int row) {
		if(row >= _orgs.length) return false;
		// int method = new Integer(""+_meth[row]).intValue();
		// if((method==OrgEditor._GRASSROOT) && (_hash[row]==null)) return true;
		
		String sql = "SELECT p."+table.peer.name+" FROM "+table.peer.TNAME +" AS p JOIN "+table.key.TNAME+" AS k"+
		" ON ("+table.peer.global_peer_ID_hash+"=k."+table.key.ID_hash+") WHERE "+table.peer.peer_ID +"=?;";
		String cID=Util.getString(_crea[row]);
		
		if(cID == null) return true; // Unknown creator? probably just not set => editable
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(sql, new String[]{cID});
		} catch (SQLiteException e) {
			e.printStackTrace();
			return false;
		}
		if(a.size()>0) return true; // I have the key => editable
		return false; // I do not have the key => not editable;
	}
	public boolean isNotReady(int row) {
		if(DEBUG) System.out.println("Orgs:isNotReady: row="+row);
		//Util.printCallPath("Orgs:isNotReady: signals test");
		if(row >= _orgs.length) {
			if(DEBUG) System.out.println("Orgs:isNotReady: row>"+_orgs.length);
			return false;
		}
		if(!_gid[row]){
			if(DEBUG) System.out.println("Orgs:isNotReady: gid false");
			return true;
		}
		String cID=Util.getString(_crea[row]);
		if(cID == null){
			if(DEBUG) System.out.println("Orgs:isNotReady: cID null");
			return true;
		}
		int method = -1;
		try{method = new Integer(""+_meth[row]).intValue();}catch(Exception e){}
		if((method==table.organization._GRASSROOT) && (_hash[row]==null)){
			if(DEBUG) System.out.println("Orgs:isNotReady: hash null");
			return true;
		}
		if(DEBUG) System.out.println("Orgs:isNotReady: exit false");
		return false;
	}
	public boolean isBlocked(int row) {
		if(row>=_blo.length) return false;
		return _blo[row];
	}
	public boolean isBroadcasted(int row) {
		if(row>=_bro.length) return false;
		return _bro[row];
		//return this.isServing(row);
	}
	public boolean isRequested(int row) {
		if(row>=_req.length) return false;
		return _req[row];
	}
	public static void setBlocking(String orgID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
		try {
			Application.db.update(table.organization.TNAME,
					new String[]{table.organization.blocked},
					new String[]{table.organization.organization_ID},
					new String[]{Util.bool2StringInt(val), orgID}, DEBUG);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}
	/**
	 * change org.broadcasted Better change with toggleServing which sets also peer.served_orgs
	 * @param orgID
	 * @param val
	 */
	private static void setBroadcasting(String orgID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setBroadcasting: set="+val+" for orgID="+orgID);
		try {
			Application.db.update(table.organization.TNAME,
					new String[]{table.organization.broadcasted, table.organization.reset_date},
					new String[]{table.organization.organization_ID},
					new String[]{Util.bool2StringInt(val),Util.getGeneralizedTime(), orgID}, DEBUG);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("Orgs:setBroadcasting: Done");
	}
	public static void setRequested(String orgID, boolean val) {
		if(DEBUG) System.out.println("Orgs:setRequested: set="+val);
		try {
			Application.db.update(table.organization.TNAME,
					new String[]{table.organization.requested},
					new String[]{table.organization.organization_ID},
					new String[]{Util.bool2StringInt(val), orgID}, DEBUG);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}
}
