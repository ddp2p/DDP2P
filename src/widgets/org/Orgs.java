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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import streaming.RequestData;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.DBSelector;
import util.Util;
import widgets.components.BulletRenderer;
import widgets.components.XTableColumnModel;
import widgets.instance.Instances;
import wireless.BroadcastClient;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

//import apple.laf.CoreUIUtils.Tree;




import util.P2PDDSQLException;
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
public class Orgs extends JTable implements MouseListener, OrgListener {
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
	BulletRenderer hotRenderer;
	ColorRenderer colorRenderer;
	DefaultTableCellRenderer centerRenderer;
	

	Hashtable<String,PluginData> plugin_applets = new Hashtable<String, PluginData>();
	Hashtable<Integer,Hashtable<String,PluginMenus>> plugin_menus = new Hashtable<Integer,Hashtable<String,PluginMenus>>();
	ArrayList<String> plugins= new ArrayList<String>();
	private XTableColumnModel yourColumnModel;
	
	public Orgs() {
		super(new OrgsModel(Application.db));
		if(DEBUG) System.out.println("Orgs: constr from db");
		init();
	}
	public Orgs(DBInterface _db) {
		super(new OrgsModel(_db));
		if(DEBUG) System.out.println("Orgs: constr from dbintf");
		init();
	}
	public Orgs(OrgsModel dm) {
		super(dm);
		if(DEBUG) System.out.println("Orgs: constr from model");
		init();
	}
	
	/**
	 * Call this to remove a current column
	 * @param crt_col
	 */
	public void removeColumn(int crt_col){
		//TableColumn column  = this.yourColumnModel.getColumnByModelIndex(crt_col);
		TableColumn column  = this.yourColumnModel.getColumn(crt_col);
		yourColumnModel.setColumnVisible(column, false);
//		_ColumnNames.remove(crt_col);
//		this.fireTableStructureChanged();
	}
	public void addColumn(int crt_col){
		TableColumn column  = this.yourColumnModel.getColumnByModelIndex(crt_col);
		yourColumnModel.setColumnVisible(column, true);
	}
	/**
	 * Adds status as listener
	 */
	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.getTableHeader().addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		colorRenderer = new ColorRenderer(getModel());
		centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		hotRenderer = new BulletRenderer(
				DDIcons.getHotImageIcon("Hot"), DDIcons.getHotGImageIcon("Hot"),
				null, _("Recently Contacted"),  _("Not Recently Contacted"), null);
		
		yourColumnModel = new widgets.components.XTableColumnModel();
		setColumnModel(yourColumnModel); 
		createDefaultColumnsFromModel(); 
		
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
		
   		try{
   			if (Identity.getCurrentIdentity().identity_id!=null) {
    			//long id = new Integer(Identity.current.identity_id).longValue();
    			long orgID = Identity.getDefaultOrgID();
    			if(DEBUG) System.out.println("Orgs:init: crt orgID="+orgID);
    			this.setCurrent(orgID);
    			int row =this.getSelectedRow();
    			if(DEBUG) System.out.println("Orgs:init: crt row="+row);
     			this.fireListener(row, A_NON_FORCE_COL);
   			}else{
   	   			if(DEBUG) System.out.println("Orgs:init: crt orgID= NONE :(");   				
   			}
    	}catch(Exception e){e.printStackTrace();}
   		if(DD.status!=null) this.addOrgListener(DD.status);
   		
		DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel headerLabel = (JLabel)
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Icon icon = Orgs.this.getModel().getIcon(column);
				if(icon != null)  headerLabel.setText(null);
				headerLabel.setIcon(icon);
			    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			    setHorizontalAlignment(JLabel.CENTER);
			    return headerLabel;
			}
		};
		
		//getTableHeader().setDefaultRenderer(rend);
		for(int col_index = 0; col_index < getModel().getColumnCount(); col_index++) {
			if(getModel().getIcon(col_index) != null)
				getTableHeader().getColumnModel().getColumn(col_index).setHeaderRenderer(rend);
		}

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

	public TableCellRenderer getCellRenderer(int row, int _column) {
		int column = this.convertColumnIndexToModel(_column);
		if ((column == OrgsModel.TABLE_COL_NAME)) return colorRenderer;
		if ((column == OrgsModel.TABLE_COL_CONNECTION)) return hotRenderer;
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
		return super.getCellRenderer(row, _column);
	}
	//protected String[] columnToolTips = {null,null,_("A name you provide")};
    @SuppressWarnings("serial")
	protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
               java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                if(realIndex >= OrgsModel.columnToolTips.length) return null;
				return OrgsModel.columnToolTips[realIndex];
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
	void initColumnSizes() {
        OrgsModel model = (OrgsModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
//        for (int i = 0; i < model.getColumnCount(); i++)
        for (int i = 0; i < this.getColumnCount(); i++)
        {
        	int headerWidth = 0;
        	int cellWidth = 0;
        	column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(this.convertColumnIndexToModel(i))).
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
	private static OrgEditor orgEPane;
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
		fireListener((row<0)?null:orgID, col, organization_crt);
	}
	void fireListener(String orgID, int col, D_Organization organization_crt) {
		for(OrgListener l: listeners){
			if(DEBUG) System.out.println("Orgs:fireListener: l="+l);
			try{
				l.orgUpdate(orgID, col, organization_crt);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addOrgListener(OrgListener l){
		if( listeners.contains(l) ) return;
		listeners.add(l);
		int row = this.getSelectedRow();
		if(row>=0)
			l.orgUpdate(Util.getString(this.getModel()._orgs[this.convertRowIndexToModel(row)]),A_NON_FORCE_COL, organization_crt);
	}
	public void removeOrgListener(OrgListener l){
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
    	OrgsCustomAction cAction;
    	
    	aAction = new OrgsAddAction(this, _("Add!"), addicon,_("Add new organization."), _("Add"),KeyEvent.VK_A);
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
    	
    	if(getModel().isAdvertised(row))
    		sAction = new OrgsToggleServingAction(this, _("Stop Advertising!"), delicon,_("Stop advertising this organization."), _("Stop Advertising"),KeyEvent.VK_S);
    	else
       		sAction = new OrgsToggleServingAction(this, _("Advertise!"), addicon,_("Advertise this organization."), _("Advertise"),KeyEvent.VK_S);
    	sAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(sAction));
      	
    	
    	cAction = new OrgsCustomAction(this, _("Add to WLAN Interest!"), delicon,_("Add to WLAN Interests."), _("WLAN Request"),KeyEvent.VK_R, OrgsCustomAction.M_WLAN_REQUEST);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new OrgsCustomAction(this, _("Add Column!"), delicon,_("Add Column."), _("Column"),KeyEvent.VK_C, OrgsCustomAction.C_ACOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
    	cAction = new OrgsCustomAction(this, _("Remove Column!"), delicon,_("Remove Column."), _("Column"),KeyEvent.VK_R, OrgsCustomAction.C_RCOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	
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
	public D_Organization getCurrentOrg() {
		String gID = this.getModel().getOrgGID(convertRowIndexToModel(this.getSelectedRow()));
		try {
			return new D_Organization(gID, null);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Panel with Editor
	 * @return
	 */
	public Component getComboPanel(){
        //DD.orgsPane = new widgets.org.Orgs();
        //Application.orgs = DD.orgsPane;
        orgEPane = new widgets.org.OrgEditor();
        //DD.orgsPane
        this.addOrgListener(orgEPane); // this remains connected to Orgs rather than status to enable force edit
    	Component orgs = DD.makeOrgsPanel(orgEPane, this); //DD.orgsPane); //new JPanel();
    	return orgs;
	}
	public void connectWidget() {
		getModel().connectWidget();
		Application.orgs = this;
		DD.status.addOrgStatusListener(this);
	}
	public void disconnectWidget() {
		getModel().disconnectWidget();
		DD.status.removeOrgListener(this);
		Application.orgs = null;
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		if(orgID==null) return;
		int model_row = getModel().getRow(orgID);
		if(model_row<0) return;
		int view_row = this.convertRowIndexToView(model_row);
		this.setRowSelectionInterval(view_row, view_row);
		
		this.fireListener(orgID, col, org);
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		// TODO Auto-generated method stub
		
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
		} catch (P2PDDSQLException e1) {
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
class OrgsCustomAction extends DebateDecideAction {
    public static final int M_WLAN_REQUEST = 1;
	public static final int C_RCOLUMN = 2;
	public static final int C_ACOLUMN = 3;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Orgs tree; ImageIcon icon;
	int command;
    public OrgsCustomAction(Orgs tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon;
        this.command = command;
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
    	if(command == C_RCOLUMN) {
        	tree.removeColumn(row);
    		tree.initColumnSizes();
        }else	if(command == C_ACOLUMN) {
        	int col = Application.ask(_("Add"), _("Columns"), Arrays.copyOf(model.columnNames, model.columnNames.length, new Object[]{}.getClass()), null, null);
        	if(col == JOptionPane.CLOSED_OPTION) return;
        	if(col < 0) return;
       		tree.addColumn(col);
    		tree.initColumnSizes();
        }else
    	if(command == M_WLAN_REQUEST) {
    		boolean DEBUG = true;
    		if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: start");
    		String _m_GID = model.getOrgGID(row);
    		if(_m_GID==null){
    			if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: null GID");
        		return;
    		}
    		if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: GID: "+_m_GID);
    		RequestData rq = new RequestData();;
    		
			try {
				String interests = DD.getAppText(DD.WLAN_INTERESTS);
				if(interests != null){
					byte[] wlan_interests = Util.byteSignatureFromString(interests);
					rq = rq.decode(new ASN1.Decoder(wlan_interests));
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if(!rq.orgs.contains(_m_GID)) {
				rq.orgs.add(_m_GID);
				if(BroadcastClient.msgs == null){
					System.out.println("Orgs:OrgsCustomAction:WLANRequest: empty messages queue!");
				}else
					BroadcastClient.msgs.registerRequest(rq);
				if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: added GID: "+_m_GID);
			}
			
			byte[] intr = rq.getEncoder().getBytes();
			try {
				DD.setAppText(DD.WLAN_INTERESTS, Util.stringSignatureFromByte(intr));
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: done ");
    	}
    	
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
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static int HOT_DAYS = 10;
	static int HOT_SEC = HOT_DAYS*24*3600;
	static long HOT_MSEC = HOT_SEC*1000;
	DBInterface db;
	Object _orgs[]=new Object[0];
	Object _meth[]=new Object[0];
	Object _hash[]=new Object[0];
	Object _crea[]=new Object[0];
	boolean[] _gid=new boolean[0];
	boolean[] _blo=new boolean[0]; // block
	boolean[] _req=new boolean[0]; // request
	boolean[] _bro=new boolean[0]; // broadcast
	String columnNames[]={_("Name"),_("Initiator"),_("Category"),_("Constituents"),_("Activity"),_("Hot"),_("News")
			//,_("Plugins")
			};
	protected static String[] columnToolTips = {
		_("A name for the organization"),
		_("A name for the initiator"),
		_("A category for sorting organizations (locally)"),
		_("Number of constituents"),
		_("Number of reactions (signatures pro or against) and news"),
		_("Activity in less days than:")+""+HOT_DAYS,
		_("Number of news"),
		_("Plugins")
		};
	public Icon getIcon(int column) {
		switch(column) { 
		case TABLE_COL_CONNECTION: 
			return DDIcons.getHotImageIcon("Fire");
		case TABLE_COL_NEWS: 
			return DDIcons.getNewsImageIcon("News");
		case TABLE_COL_CONSTITUENTS_NB: 
			return DDIcons.getConImageIcon("Const");
		case TABLE_COL_ACTIVITY: 
			return DDIcons.getSigImageIcon("Votes");
		}
		return null;
	}
	ArrayList<Orgs> tables= new ArrayList<Orgs>();
	Hashtable<String, Integer> rowByID =  new Hashtable<String, Integer>();
	public OrgsModel(DBInterface _db) {
		db = _db;
		connectWidget();
		update(null, null);
	}
	public int getRow(String orgID) {
		Integer row = rowByID.get(orgID);
		if(row==null) return -1;
		return row.intValue();
	}
	public void disconnectWidget() {
		db.delListener(this);
	}
	public void connectWidget() {
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.organization.TNAME)), null);
		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
	}
	public String getOrgGID(int row) {
		if((row<0) || (row>_hash.length)) return null;
		return (String)_hash[row];
	}
	public boolean isServing(int row) {
		if(DEBUG) System.out.println("\n************\nOrgs:OrgsModel:isServing: row="+row);
		return isBroadcasted(row);
		/*
		*/
	}
	public boolean isAdvertised(int row) {
		if(DEBUG) System.out.println("\n************\nOrgs:OrgsModel:isAdvertised: row="+row);
		boolean result= false;
		try {
			if(isServingAndPresent(row)) result = true;
		} catch (Exception e) {
			result = false;
		}
		if(DEBUG) System.out.println("Orgs:OgsModel:isAdvertised: exit with="+result);
		if(DEBUG) System.out.println("*****************");
		return result;
	}	
	public boolean isServingAndPresent(int row) throws Exception {
		boolean result= false;
		if(DEBUG) System.out.println("\n************\nOrgs:OrgsModel:isServingAndPresent: row="+row);
		ArrayList<ArrayList<Object>> s;
		String sql = "SELECT po."+table.peer_org.served+
			" FROM "+table.peer_org.TNAME+" AS po "+
			" LEFT JOIN "+table.peer.TNAME+" AS p" +
					" ON(p."+table.peer.peer_ID+"=po."+table.peer_org.peer_ID+") "+
			" WHERE p."+table.peer.global_peer_ID +" = ? AND po."+table.peer_org.organization_ID+" = ?;";
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
		boolean old_serving = true;
		
		// First set it in served orgs
		String global_peer_ID = Identity.current_peer_ID.globalID;
		String peer_ID;
		try {// get my peerID
			peer_ID = table.peer.getLocalPeerID(global_peer_ID);
		} catch (P2PDDSQLException e2) {
			e2.printStackTrace();
			return false;
		}
		try {// is it in served org?
			ArrayList<ArrayList<Object>> s;
			String sql = "SELECT "+table.peer_org.served+
				" FROM "+table.peer_org.TNAME+
				" WHERE "+table.peer_org.peer_ID +" = ? AND "+table.peer_org.organization_ID+" = ?;";
			s = Application.db.select(sql, new String[]{ peer_ID, organization_ID}, _DEBUG);
			if(s.size()==0){
				old_serving = false;
				throw new Exception("No record found");//return false;
			}
			if(Util.stringInt2bool(Util.getString(s.get(0).get(0)), false)) old_serving = true;
			else old_serving = false;
			if(_DEBUG) System.out.println("\n************\nOrgs:OgsModel:toggleServing: old serving=" +old_serving);

			//set it in served orgs
			if(toggle) val = !old_serving;
			String _serving = Util.bool2StringInt(val);
			if(val) {
				Application.db.update(table.peer_org.TNAME, new String[]{table.peer_org.served},
						new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID},
						new String[]{_serving, peer_ID, organization_ID}, _DEBUG);
			}else{
				Application.db.delete(table.peer_org.TNAME,
						new String[]{table.peer_org.peer_ID,  table.peer_org.organization_ID},
						new String[]{peer_ID, organization_ID}, _DEBUG);
			}
			D_Organization.setBroadcasting(organization_ID, val); // set in organization.broadcasted
		} catch (Exception e) {
			// if not serve so far, serve now!
			try {
				if(_DEBUG) System.out.println("\n************\nOrgs:OrgModel:toggleServing: old serving=" +old_serving);
				if(old_serving) util.Util.printCallPath("Old Serving unexpected!");
				if(toggle) val = !old_serving;
				String _serving = Util.bool2StringInt(val);
				Application.db.insert(table.peer_org.TNAME,
						new String[]{table.peer_org.served,table.peer_org.peer_ID,table.peer_org.organization_ID},
						new String[]{_serving, peer_ID, organization_ID}, _DEBUG);
				D_Organization.setBroadcasting(organization_ID, val);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				return false;
			}
		}
		if(val) {
			ArrayList<ArrayList<Object>> s;
			String sql = "SELECT "+table.organization.broadcast_rule+
				" FROM "+table.organization.TNAME+
				" WHERE "+table.organization.organization_ID+" = ?;";
			try {
				s = Application.db.select(sql, new String[]{organization_ID}, _DEBUG);
				if(s.size()>0)
					if(!Util.stringInt2bool(s.get(0).get(0), false)){
						Application.warning(_("It is not recommended to advertise a private organization!"), _("Private organization"));
					}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		// need to sign again my peer due to served_orgs
		hds.Server.update_my_peer_ID_peers_name_slogan();
		if(_DEBUG) System.out.println("Orgs:OgsModel:toggleServing: exit with="+(!old_serving));
		if(_DEBUG) System.out.println("*****************");
		return !old_serving;
	}
	public void setCurrent(long org_id) {
		//boolean DEBUG = true;
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
			long id;
			if(DEBUG) System.out.println("Orgs:setCurrent: k="+k+" row_org_ID="+i);
			if(i instanceof Integer){
				Integer _id = (Integer)i;
				id = _id.longValue();
			}else
				if(i instanceof Long){
					Long _id = (Long)i;
					id = _id.longValue();
				}else{
					String _id = i+"";
					try{id = Long.parseLong(_id);}catch(Exception e){
						if(DEBUG) System.out.println("Orgs:setCurrent: ID not parsable: ["+k+"]="+i);
						e.printStackTrace();
						continue; //id=-2;
					}
				}
			
			if(id==org_id) {
				try {
					//long constituent_ID = 
					if(DEBUG) System.out.println("Orgs:setCurrent: will set current org");
					Identity.setCurrentOrg(org_id);
							
				} catch (P2PDDSQLException e) {
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
			}else  if(DEBUG) System.out.println("Orgs:setCurrent: ID not this: "+id);
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
	final static String sql_orgs = "SELECT "+table.organization.organization_ID+","+table.organization.certification_methods+","
		+table.organization.global_organization_ID_hash+","+table.organization.creator_ID+
		  ","+table.organization.blocked+","+table.organization.broadcasted+","+table.organization.requested
		+" FROM "+table.organization.TNAME+";";

	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		final int SELECT_ORG_ID = 0;
		final int SELECT_METHODS = 1;
		final int SELECT_ORG_GIDH = 2;
		final int SELECT_ORG_CREAT_ID = 3;
		final int SELECT_BLOCKED = 4;
		final int SELECT_BROADCASTED = 5;
		final int SELECT_REQUESTED = 6;
		if(DEBUG) System.out.println("\nwidgets.org.Orgs: update table= "+_table+": info= "+info);
		Object old_sel[] = new Object[tables.size()];
		for(int i=0; i<old_sel.length; i++){
			int sel = tables.get(i).getSelectedRow();
			if(DEBUG) System.out.println("widgets.org.Orgs: old selected row: table["+i+"]="+sel);
			if((sel >= 0) && (sel < _orgs.length)) old_sel[i] = _orgs[sel];
		}
		try {
			ArrayList<ArrayList<Object>> orgs = db.select(sql_orgs, new String[]{},DEBUG);
			_orgs = new Object[orgs.size()];
			_meth = new Object[orgs.size()];
			_hash = new Object[orgs.size()];
			_crea = new Object[orgs.size()];
			_gid = new boolean[orgs.size()];
			_blo = new boolean[orgs.size()];
			_bro = new boolean[orgs.size()];
			_req = new boolean[orgs.size()];
			rowByID = new Hashtable<String, Integer>();
			for(int k=0; k<_orgs.length; k++) {
				ArrayList<Object> o = orgs.get(k);
				_orgs[k] = o.get(SELECT_ORG_ID);
				rowByID.put(Util.getString(_orgs[k]), new Integer(k));
				_meth[k] = o.get(SELECT_METHODS);
				_hash[k] = o.get(SELECT_ORG_GIDH);
				_crea[k] = o.get(SELECT_ORG_CREAT_ID);
				_gid[k] = (o.get(SELECT_ORG_CREAT_ID) != null);
				_blo[k] = Util.stringInt2bool(o.get(SELECT_BLOCKED), false);
				_bro[k] = Util.stringInt2bool(o.get(SELECT_BROADCASTED), false);
				_req[k] = Util.stringInt2bool(o.get(SELECT_REQUESTED), false);
			}
			if(DEBUG) System.out.println("widgets.org.Orgs: A total of: "+_orgs.length);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		for(int k=0; k<old_sel.length; k++){
			Orgs i = tables.get(k);
			//int row = i.getSelectedRow();
			int row = findRow(old_sel[k]);
			if(DEBUG) System.out.println("widgets.org.Orgs: selected row: table["+k+"]="+row);
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
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_CREATOR:
			String sql_cr =
				"SELECT o."+table.organization.creator_ID+", m."+table.my_organization_data.creator+
				", p."+table.peer.name+", pm."+table.peer_my_data.name+",p."+table.peer.name_verified+
				" FROM "+table.organization.TNAME+" AS o" +
				" LEFT JOIN "+table.my_organization_data.TNAME+" AS m "+" ON(o."+table.organization.organization_ID+"=m."+table.my_organization_data.organization_ID+")"+
				" LEFT JOIN "+table.peer.TNAME+" AS p "+" ON(o."+table.organization.creator_ID+"=p."+table.peer.peer_ID+")"+
				" LEFT JOIN "+table.peer_my_data.TNAME+" AS pm "+" ON(o."+table.organization.creator_ID+"=pm."+table.peer_my_data.peer_ID+")"+
				" WHERE o."+table.organization.organization_ID+" = ? LIMIT 1;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_cr, new String[]{orgID}, DEBUG);
				Object creator_org_custom = orgs.get(0).get(1);
				Object creator_peer_custom = orgs.get(0).get(3);
				Object creator_orig = orgs.get(0).get(2);
				Object creator_orig_verified = orgs.get(0).get(4);
				if(orgs.size()>0)
					if(creator_org_custom!=null){
						result = "O: "+Util.getString(creator_org_custom);
						if(DEBUG)System.out.println("Orgs:Got initiator org my="+result);
					}
					else{
						if(creator_peer_custom!=null){
							result = "P: "+Util.getString(creator_peer_custom);
							if(DEBUG)System.out.println("Orgs:Got initiator peer my="+result);
						}
						else{
							result = Util.getString(creator_orig);
							boolean verified = Util.stringInt2bool(creator_orig_verified, false);
							if(!verified) result = "(?) "+result;
							else result  = "V: "+result;
							if(DEBUG)System.out.println("Orgs:Got initiator="+result);
						}
					}
			} catch (P2PDDSQLException e) {
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
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_CONSTITUENTS_NB:
			String sql_co = "SELECT count(*) FROM "+table.constituent.TNAME+
			" WHERE "+table.constituent.organization_ID+" = ? AND "+table.constituent.op+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_co, new String[]{orgID, "1"});
				if(orgs.size()>0) result = orgs.get(0).get(0);
			} catch (P2PDDSQLException e) {
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
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				break;
			}
			
			String sql_new = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE "+table.news.organization_ID+" = ?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new, new String[]{orgID});
				if(orgs.size()>0) result = new Integer(""+(Util.get_long(result)+Util.get_long(orgs.get(0).get(0))));
			} catch (P2PDDSQLException e) {
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
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				break;
			}
			
			String sql_new2 = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.organization_ID+" = ? AND n."+table.news.arrival_date+">?;";
			try {
				ArrayList<ArrayList<Object>> orgs = db.select(sql_new2, new String[]{orgID,Util.getGeneralizedDate(DAYS_OLD2)});
				if(orgs.size()>0){
					int result_int = new Integer(""+((Util.get_long(result))+(Util.get_long(orgs.get(0).get(0)))));
					if(result_int>0) result = new Boolean(true); else result = new Boolean(false);
				}
			} catch (P2PDDSQLException e) {
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
			} catch (P2PDDSQLException e) {
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
			String creator = Util.getString(value);
			if("".equals(creator)) creator = null;
			set_my_data(table.my_organization_data.creator, creator, row);
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
		if(_DEBUG)System.out.println("Set value =\""+value+"\"");
		String org_ID = Util.getString(_orgs[row]);
		try {
			String sql = "SELECT "+field_name+" FROM "+table.my_organization_data.TNAME+" WHERE "+table.my_organization_data.organization_ID+"=?;";
			ArrayList<ArrayList<Object>> orgs = db.select(sql,new String[]{org_ID});
			if(orgs.size()>0){
				db.update(table.my_organization_data.TNAME, new String[]{field_name},
						new String[]{table.my_organization_data.organization_ID}, new String[]{value, org_ID}, _DEBUG);
			}else{
				if(value==null) return;
				db.insert(table.my_organization_data.TNAME,
						new String[]{field_name,table.my_organization_data.organization_ID},
						new String[]{value, org_ID}, _DEBUG);
			}
		} catch (P2PDDSQLException e) {
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
		} catch (P2PDDSQLException e) {
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
}
