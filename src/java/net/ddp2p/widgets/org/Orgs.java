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
package net.ddp2p.widgets.org;
import static net.ddp2p.common.util.Util.__;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_News;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.DBSelector;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.BroadcastClient;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.components.BulletRenderer;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.components.XTableColumnModel;
import net.ddp2p.widgets.instance.Instances;
import net.ddp2p.widgets.justifications.Justifications;
import net.ddp2p.widgets.justifications.JustificationsModel;
import net.ddp2p.widgets.motions.MotionsModel;
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
	public static final int STATE_CONNECTION_FAIL =0;
	public static final int STATE_CONNECTION_TCP = 1;
	public static final int STATE_CONNECTION_UDP = 2;
	public static final int STATE_CONNECTION_UDP_NAT = 3;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DIM_X = 0;
	private static final int DIM_Y = 50;
	public static final int A_NON_FORCE_COL = D_Organization.A_NON_FORCE_COL; 
	public static final int FORCE_THREASHOLD_COL = 3; 
	BulletRenderer bulletRenderer = new BulletRenderer();
	BulletRenderer hotRenderer;
	ColorRenderer colorRenderer;
	DefaultTableCellRenderer centerRenderer;
	Hashtable<String,PluginData> plugin_applets = new Hashtable<String, PluginData>();
	Hashtable<Integer,Hashtable<String,PluginMenus>> plugin_menus = new Hashtable<Integer,Hashtable<String,PluginMenus>>();
	ArrayList<String> plugins= new ArrayList<String>();
	private XTableColumnModel yourColumnModel;
	public Orgs() {
		super(new OrgsModel(Application.getDB()));
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
		TableColumn column  = this.yourColumnModel.getColumn(crt_col);
		yourColumnModel.setColumnVisible(column, false);
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
				null, __("Recently Contacted"),  __("Not Recently Contacted"), null);
		yourColumnModel = new net.ddp2p.widgets.components.XTableColumnModel();
		setColumnModel(yourColumnModel); 
		createDefaultColumnsFromModel(); 
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		this.setPreferredScrollableViewportSize(new Dimension(DIM_X, DIM_Y));
   		try{
   			if (Identity.getCurrentConstituentIdentity().identity_id!=null) {
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
   		if(MainFrame.status!=null) this.addOrgListener(MainFrame.status);
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
		for(int col_index = 0; col_index < getModel().getColumnCount(); col_index++) {
			if(getModel().getIcon(col_index) != null)
				getTableHeader().getColumnModel().getColumn(col_index).setHeaderRenderer(rend);
		}
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
    public JPanel getPanel() {
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
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
	public OrgsModel getModel(){
		return (OrgsModel) super.getModel();
	}
	void initColumnSizes() {
        OrgsModel model = (OrgsModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
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
    	int row; 
    	int col; 
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        if((row<0)||(col<0)) return;
    	OrgsModel model = (OrgsModel)getModel();
 		int model_row=convertRowIndexToModel(row);
   	   	if(model_row>=0) {
   	   		String orgID = Util.getString(model.data.get(model_row).getLIDstr_forced());
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
		if (DEBUG) System.out.println("Orgs:fireForceEdit: row="+orgID);
		if (orgID != null) organization_crt = D_Organization.getOrgByLID_NoKeep(orgID, true);
		else organization_crt = D_Organization.getEmpty();
		for (OrgListener l: listeners) {
			if (DEBUG) System.out.println("Orgs:fireForceEdit: l="+l);
			try {
				if (orgID == null); 
				else l.org_forceEdit(orgID, organization_crt);
			} catch(Exception e){e.printStackTrace();}
		}
	}
	long _org_crt = -1;
	Object old_org_signature = null; 
	private static OrgEditor orgEPane;
	public void fireListener(int row, int col) {
		if(DEBUG) System.out.println("Orgs:fireListener: row="+row);
		String orgID = null;
		int model_row;
		OrgsModel model = this.getModel();
		if (row >= 0) {
			model_row = this.convertRowIndexToModel(row);
			ArrayList<D_Organization> _data = model.data;
			if ((model_row >= 0) && (model_row < _data.size())) {
				D_Organization org = _data.get(model_row);
				if (org != null)
					orgID = Util.getString(org.getLIDstr_forced());
			}
		}
		long _orgID = Util.lval(orgID,-1);
		if ((old_org_signature != null) && !DD.ORG_UPDATES_ON_ANY_ORG_DATABASE_CHANGE) {
			if (_orgID == _org_crt) {
				if(DEBUG)System.out.println("Orgs:fireListener: action dropped, same orgID "+_orgID);
				return;
			}
		}
		_org_crt = _orgID;
		organization_crt = null;
		if (_orgID > 0)
			try {
				organization_crt = D_Organization.getOrgByLID_NoKeep(_orgID, true);
				old_org_signature = organization_crt.signature;
			} catch (Exception e1) {
				e1.printStackTrace();
				orgID = "-1";
				old_org_signature = null;
			}
		fireListener((row<0)?null:orgID, col, organization_crt);
	}
	void fireListener(String orgID, int col, D_Organization organization_crt) {
		if (DEBUG) System.out.println("Orgs::fireListener: start orgID="+orgID);
		for (OrgListener l: listeners) {
			if (DEBUG) System.out.println("Orgs::fireListener: l="+l);
			try {
				l.orgUpdate(orgID, col, organization_crt);
			} catch(Exception e){e.printStackTrace();}
		}
		if (DEBUG) System.out.println("Orgs::fireListener: stop");
	}
	public void addOrgListener(OrgListener l){
		if( listeners.contains(l) ) return;
		listeners.add(l);
		int row = this.getSelectedRow();
		if (row >= 0) {
			String lid = this.getModel().getLIDstr(this.convertRowIndexToModel(row));
			l.orgUpdate(lid, A_NON_FORCE_COL, organization_crt);
		}
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
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	OrgsDeleteAction pdAction;
    	OrgsAddAction aAction;
    	OrgsForceEditAction eAction;
    	OrgsToggleServingAction sAction;
    	OrgsCustomAction cAction;
    	aAction = new OrgsAddAction(this, __("Add!"), addicon,__("Add new organization."), __("Add"),KeyEvent.VK_A);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	cAction = new OrgsCustomAction(this, __("Refresh!"), addicon,__("Refresh organizations."), __("Refresh"),KeyEvent.VK_R, OrgsCustomAction.O_REFRESH);
    	cAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(cAction);
    	popup.add(menuItem);
    	if (row < 0) return popup;
    	pdAction = new OrgsDeleteAction(this, __("Delete!"), delicon,__("Delete all data about this."), __("Delete"),KeyEvent.VK_D);
    	pdAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(pdAction));
    	if(getModel().isAdvertised(row))
    		sAction = new OrgsToggleServingAction(this, __("Stop Advertising!"), delicon,__("Stop advertising this organization."), __("Stop Advertising"),KeyEvent.VK_S);
    	else
       		sAction = new OrgsToggleServingAction(this, __("Advertise!"), addicon,__("Advertise this organization."), __("Advertise"),KeyEvent.VK_S);
    	sAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(sAction));
    	cAction = new OrgsCustomAction(this, __("Add to WLAN Interest!"), delicon,__("Add to WLAN Interests."), __("WLAN Request"),KeyEvent.VK_R, OrgsCustomAction.M_WLAN_REQUEST);
    	cAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(cAction));
    	cAction = new OrgsCustomAction(this, __("Add Column!"), delicon,__("Add Column."), __("Column"),KeyEvent.VK_C, OrgsCustomAction.C_ACOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	cAction = new OrgsCustomAction(this, __("Remove Column!"), delicon,__("Remove Column."), __("Column"),KeyEvent.VK_R, OrgsCustomAction.C_RCOLUMN);
    	cAction.putValue("row", new Integer(col));
    	popup.add(new JMenuItem(cAction));
    	eAction = new OrgsForceEditAction(this, __("Force Edit!"), delicon,__("Force editing rights."), __("Edit"),KeyEvent.VK_E);
    	eAction.putValue("row", new Integer(row));
    	popup.add(new JMenuItem(eAction));
    	return popup;
	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	int row; 
    	int col; 
    	if(!evt.isPopupTrigger()) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0) row = this.convertRowIndexToModel(row);
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	/**
	 * Panel with Editor
	 * @return
	 */
	public Component getComboPanel(){
        orgEPane = new net.ddp2p.widgets.org.OrgEditor();
        this.addOrgListener(orgEPane); 
    	Component orgs = MainFrame.makeOrgsPanel(orgEPane, this); 
    	return orgs;
	}
	public void connectWidget() {
		getModel().connectWidget();
		GUI_Swing.orgs = this;
		MainFrame.status.addOrgStatusListener(this);
	}
	public void disconnectWidget() {
		getModel().disconnectWidget();
		MainFrame.status.removeOrgListener(this);
		GUI_Swing.orgs = null;
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		if (orgID == null) return;
		int model_row = -2, view_row = -2;
		OrgsModel m = getModel();
		try {
			model_row = m.getRow(orgID);
			if (model_row < 0) return;
			view_row = this.convertRowIndexToView(model_row);
			this.setRowSelectionInterval(view_row, view_row);
		} catch (Exception e) {
			if (_DEBUG) System.out.println("Orgs: orgUpdate: #m=" + model_row+"/#v=" + view_row+" "+m.getRowCount());
		}
		this.fireListener(orgID, col, org);
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
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
    	String orgID = model.getLIDstr(row);
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
     	if (row < 0) return;
    	String orgID = model.getLIDstr(row);
    	model.setCurrent(-1);
    	model.dropOrg(row, orgID);
    	D_Organization dropped = D_Organization.deleteAllAboutOrg(orgID);
		MainFrame.status.droppingOrg(dropped);
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
    	if (src instanceof JMenuItem) {
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    	} else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
    	}
    	((java.awt.event.ActionListener)Application.appObject).actionPerformed(new ActionEvent(tree, row, DD.COMMAND_NEW_ORG));
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
    	} else {
    		row = tree.getSelectedRow();
       		row = tree.convertRowIndexToModel(row);
    	}
    	OrgsModel model = (OrgsModel)tree.getModel();
    	model.toggleServing(row);
    }
}
@SuppressWarnings("serial")
class OrgsCustomAction extends DebateDecideAction {
	public static final int M_WLAN_REQUEST = 1;
	public static final int C_RCOLUMN = 2;
	public static final int C_ACOLUMN = 3;
    public static final int O_REFRESH = 4;
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
    	} else {
    		row = tree.getSelectedRow();
       		row = tree.convertRowIndexToModel(row);
    	}
    	OrgsModel model = (OrgsModel)tree.getModel();
    	if(command == O_REFRESH) {
    		tree.getModel().update(new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.organization.TNAME)), null);
        }else if(command == C_RCOLUMN) {
        	tree.removeColumn(row);
    		tree.initColumnSizes();
        }else	if(command == C_ACOLUMN) {
        	int col = Application_GUI.ask(__("Add"), __("Columns"), Arrays.copyOf(model.columnNames, model.columnNames.length, new Object[]{}.getClass()), null, null);
        	if(col == JOptionPane.CLOSED_OPTION) return;
        	if(col < 0) return;
       		tree.addColumn(col);
    		tree.initColumnSizes();
        }else
    	if(command == M_WLAN_REQUEST) {
    		boolean DEBUG = true;
    		if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: start");
    		String _m_GIDH = model.getOrgGIDH(row);
    		if(_m_GIDH==null){
    			if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: null GID");
        		return;
    		}
    		if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: GID: "+_m_GIDH);
    		RequestData rq = new RequestData();;
			try {
				String interests = DD.getAppText(DD.WLAN_INTERESTS);
				if(interests != null){
					byte[] wlan_interests = Util.byteSignatureFromString(interests);
					rq = rq.decode(new net.ddp2p.ASN1.Decoder(wlan_interests));
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if(!rq.orgs.contains(_m_GIDH)) {
				rq.orgs.add(_m_GIDH);
				if(BroadcastClient.msgs == null){
					System.out.println("Orgs:OrgsCustomAction:WLANRequest: empty messages queue!");
				}else
					BroadcastClient.msgs.registerRequest(rq);
				if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: added GIDH: "+_m_GIDH);
			}
			byte[] intr = rq.getEncoder().getBytes();
			try {
				DD.setAppText(DD.WLAN_INTERESTS, Util.stringSignatureFromByte(intr));
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			if(DEBUG) System.out.println("Orgs:OrgsCustomAction:WLANRequest: done ");
    	}
    }
}
@SuppressWarnings("serial")
class OrgsModel extends AbstractTableModel implements TableModel, DBListener {
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_CREATOR = 1; 
	public static final int TABLE_COL_CATEGORY = 2; 
	public static final int TABLE_COL_CONSTITUENTS_NB = 3;
	public static final int TABLE_COL_ACTIVITY = 4; 
	public static final int TABLE_COL_CONNECTION = 5; 
	public static final int TABLE_COL_NEWS = 6; 
	public static final int TABLE_COL_PLUGINS = 7;
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static int HOT_DAYS = 10;
	static int HOT_SEC = HOT_DAYS*24*3600;
	static long HOT_MSEC = HOT_SEC*1000;
	DBInterface db;
	ArrayList<D_Organization> data = new ArrayList<D_Organization>();
	Object monitor_data = new Object();
	Hashtable<Long, Integer> rowByLID = new Hashtable<Long, Integer>();
	String columnNames[]={__("Name"),__("Founder"),__("Category"),__("Constituents"),__("Activity"),__("Hot"),__("News")
			};
	protected static String[] columnToolTips = {
		__("A name for the organization: yellow/blue -not ready, blue/yellow -created by me, red/white -blocked, blue/white -broadcasted, green/white -requested"),
		__("A name for the initiator: O: name custom for this org, P: name custom for peer, V: name verified, (?) name not verified"),
		__("A category for sorting organizations (locally)"),
		__("Number of constituents"),
		__("Number of reactions (signatures pro or against) and news"),
		__("Activity in less days than:")+""+HOT_DAYS,
		__("Number of news"),
		__("Plugins")
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
	public void dropOrg(int row, String orgID) {
		D_Organization o = data.remove(row);
		if (!Util.equalStrings_null_or_not(o.getLIDstr_forced(), orgID)) data.add(row, o);
	}
	/**
	 * By Model-row
	 * @param model_row
	 * @return
	 */
	public String getLIDstr(int model_row) {
		if ((model_row < 0) || (model_row >= data.size())) return null;
		D_Organization org = data.get(model_row);
		if (org == null) return null;
		return org.getLIDstr_forced();
	}
	public Long getLID(int row) {
		if ((row < 0) || (row >= data.size())) return null;
		return data.get(row).getLID_forced();
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
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.organization.TNAME)), null);
	}
	public String getOrgGID(int row) {
		if ((row < 0) || (row >= data.size())) return null;
		return (String)data.get(row).getGID();
	}
	public String getOrgGIDH(int row) {
		if ((row < 0) || (row >= data.size())) return null;
		return data.get(row).getGIDH_or_guess();
	}
	public boolean isServing(int row) {
		if(DEBUG) System.out.println("\n************\nOrgs:OrgsModel:isServing: row="+row);
		return isBroadcasted(row);
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
		String sql = "SELECT po."+net.ddp2p.common.table.peer_org.served+
				" FROM "+net.ddp2p.common.table.peer_org.TNAME+" AS po "+
				" WHERE po."+net.ddp2p.common.table.peer_org.peer_ID +" = ? AND po."+net.ddp2p.common.table.peer_org.organization_ID+" = ?;";
		String global_peer_ID = Application.getCurrent_Peer_ID().getPeerGID();
		D_Peer myself = D_Peer.getPeerByGID_or_GIDhash(global_peer_ID, null, true, false, false, null);
		if (myself == null) return result;
		String organization_ID = getLIDstr(row);
		s = Application.getDB().select(sql, new String[]{ myself.getLIDstr_keep_force(), organization_ID}, DEBUG);
		if(s.size()==0) throw new Exception("No record found");//return false;
		if("1".equals(s.get(0).get(0))) result = true;
		if(DEBUG) System.out.println("Orgs:OgsModel:isServingAndPresent: exit with="+result);
		if(DEBUG) System.out.println("*****************");
		return result;
	}
	public boolean toggleServing(int row) {
		if(_DEBUG) System.out.println("\n************\nOrgs:OrgsModel:toggleServing: row="+row);
		String organization_ID = getLIDstr(row);
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
		D_Peer myself = HandlingMyself_Peer.get_myself_with_wait();
		boolean old_serving = myself.servesOrg(Util.lval(organization_ID,-1));
		D_Organization.setBroadcasting(organization_ID, !old_serving); 
		myself.setServingOrg(Util.lval(organization_ID,-1), !old_serving);
		myself.storeRequest();
		if(_DEBUG) System.out.println("Orgs:OgsModel:toggleServing: exit with="+(!old_serving));
		if(_DEBUG) System.out.println("*****************");
		return !old_serving;
	}
	public void setCurrent(long org_id) {
		synchronized (monitor_data) {
			synced_setCurrent(org_id);
		}
	}
	public void synced_setCurrent(long org_id) {
		ArrayList<D_Organization> _data = data;
		if (DEBUG) System.out.println("Orgs:OrgsModel:setCurrent: id="+org_id);
		if (org_id < 0) {
			for (Orgs o: tables) {
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(-1, -1);
				o.fireListener(-1, 0);
			}	
			if (DEBUG) System.out.println("Orgs:OrgsModel:setCurrent: Done -1");
			return;
		}
		boolean found = false;
		for (int k = 0 ; k < _data.size() ; k++){
			Long i = getLID(k);
			long id = ( (i == null) ? -1 : i.longValue() );
			if (DEBUG) System.out.println ("Orgs:OrgsModel:setCurrent: k="+k+" row_org_ID="+i);
			if (id == org_id) {
				found = true;
				try {
					if(DEBUG) System.out.println("Orgs:OrgsModel:setCurrent: will set current org: row="+k);
					Identity.setCurrentOrg(org_id);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
				for (Orgs o: tables) {
					int tk = o.convertRowIndexToView(k);
					o.setRowSelectionAllowed(true);
					ListSelectionModel selectionModel = o.getSelectionModel();
					selectionModel.setSelectionInterval(tk, tk);
					o.scrollRectToVisible(o.getCellRect(tk, 0, true));
					o.fireListener(k, 0);
				}
				break;
			} else  if(DEBUG) System.out.println("Orgs:OrgsModel:setCurrent: ID not this: id="+id+" row="+k);
		}
		if (! found) {
			D_Organization missing = D_Organization.getOrgByLID_NoKeep(org_id, true);
			_data.add(missing);
			int k = _data.size() - 1;
			this.fireTableRowsInserted(k, k);
			try {
				if(DEBUG) System.out.println("Orgs:OrgsModel:setCurrent: will set new current org:row= "+k);
				Identity.setCurrentOrg(org_id);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			for (Orgs o: tables) {
				int tk = o.convertRowIndexToView(k);
				o.setRowSelectionAllowed(true);
				ListSelectionModel selectionModel = o.getSelectionModel();
				selectionModel.setSelectionInterval(tk, tk);
				o.scrollRectToVisible(o.getCellRect(tk, 0, true));
				o.fireListener(k, 0);
			}
		}
		if (DEBUG) System.out.println("Orgs:OrgsModel:setCurrent: Done");
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
		if (_table != null && !_table.contains(net.ddp2p.common.table.organization.TNAME)) {
			SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
				@Override
				public void _run() {
					((OrgsModel)ctx).fireTableDataChanged();
				}
			});
			return;
		}
		ArrayList<ArrayList<Object>> orgs = D_Organization.getAllOrganizations(); 
		if (orgs.size() == data.size()) {
			boolean different = false;
			for (int k = 0; k < data.size(); k++) {
				if (data.get(k).getLID() != Util.lval(orgs.get(k).get(D_Organization.SELECT_ALL_ORG_LID))) {
					different = true;
					break;
				}
			}
			if (! different) {
				SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
					@Override
					public void _run() {
						((OrgsModel)ctx).fireTableDataChanged();
					}
				});
				return;
			}
		}
		ArrayList<D_Organization> _data = new ArrayList<D_Organization>();
		Hashtable<String, Integer> _rowByID = new Hashtable<String, Integer>();
		Hashtable<Long, Integer> _rowByLID = new Hashtable<Long, Integer>();
		for (int k = 0; k < orgs.size(); k ++) {
				ArrayList<Object> o = orgs.get(k);
				Object oLID = o.get(D_Organization.SELECT_ALL_ORG_LID);
				D_Organization org_crt = D_Organization.getOrgByLID_NoKeep(Util.getString(oLID), true);
				if (org_crt == null) {
					Util.printCallPath("Why fail: "+oLID);
					continue;
				}
				_data.add(org_crt);
				_rowByID.put(org_crt.getLIDstr_forced(), new Integer(_data.size()-1));
				_rowByLID.put(org_crt.getLID_forced(), new Integer(_data.size()-1));
		}
		if (DEBUG) System.out.println("widgets.org.Orgs: A total of: "+_data.size());
		synchronized (monitor_data) {
			Object old_sel[] = new Object[tables.size()];
			for (int i = 0; i < old_sel.length; i ++) {
				Orgs old_view = tables.get(i);
				int sel = old_view.getSelectedRow();
				if(DEBUG) System.out.println("widgets.org.Orgs: old selected row: table["+i+"]="+sel);
				if ((sel >= 0) && (sel < data.size())) {
					int sel_model = old_view.convertRowIndexToModel(sel);
					if(DEBUG) System.out.println("widgets.org.Orgs: old selected row: table["+i+"]="+sel_model);
					old_sel[i] = getLIDstr(sel_model);
				}
			}
			data = _data;
			rowByID = _rowByID;
			rowByLID = _rowByLID;
			SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
				@Override
				public void _run() {
					((OrgsModel)ctx).fireTableDataChanged();
				}
			});
			for (int crt_view_idx = 0; crt_view_idx < old_sel.length; crt_view_idx ++) {
				Orgs crt_view = tables.get(crt_view_idx);
				int row_model = findModelRowForOrgLID(old_sel[crt_view_idx]);
				if(DEBUG) System.out.println("widgets.org.Orgs: selected row: table["+crt_view_idx+"]="+row_model);
				class O {int row_model; Orgs crt_view; O(int _row, Orgs _view){row_model = _row; crt_view = _view;}}
				SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, new O(row_model,crt_view)) {
					@Override
					public void _run() {
						O o = (O)ctx;
						if ((o.row_model >= 0) && (o.row_model < o.crt_view.getModel().getRowCount())) {
							int row_view = o.crt_view.convertRowIndexToView(o.row_model);
							o.crt_view.setRowSelectionInterval(row_view, row_view);
						}
						o.crt_view.initColumnSizes();
					}
				});
				crt_view.fireListener(row_model, Orgs.A_NON_FORCE_COL);
			}
		}
	}
	private int findModelRowForOrgLID(Object id) {
		if (id == null) return -1;
		long lID = Util.lval(id);
		Integer row = rowByLID.get(new Long(lID));
		return row;
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
		return data.size();
	}
	@Override
	public Object getValueAt(int row, int col) {
		boolean refresh = false;
		Object result = null;
		String orgID = this.getLIDstr(row);
		switch (col) {
		case TABLE_COL_NAME:
			result = data.get(row).getOrgNameOrMy();
			break;
		case TABLE_COL_CREATOR:
			{
				D_Organization org = data.get(row);
				Object creator_org_custom = org.getCreatorNameMy(); 
				Object creator_peer_custom = org.getCreatorPeerNameMy(); 
				Object creator_orig = org.getCreatorNameOriginal(); 
				Object creator_orig_verified = org.getCreatorNameVerified();
				if (org != null)
					if (creator_org_custom != null) {
						result = "O: "+Util.getString(creator_org_custom);
						if(DEBUG)System.out.println("Orgs:Got initiator org my="+result);
					}
					else {
						if (creator_peer_custom != null) {
							result = "P: "+Util.getString(creator_peer_custom);
							if(DEBUG)System.out.println("Orgs:Got initiator peer my="+result);
						}
						else {
							result = Util.getString(creator_orig);
							boolean verified = Util.stringInt2bool(creator_orig_verified, false);
							if (!verified) result = "(?) "+result;
							else result  = "V: "+result;
							if(DEBUG)System.out.println("Orgs:Got initiator="+result);
						}
					}
			}
			break;
		case TABLE_COL_CATEGORY:
			result = data.get(row).getCategoryOrMy();
			break;
		case TABLE_COL_CONSTITUENTS_NB:
			D_Organization org = data.get(row);
			result = org.getConstNBinOrganization_WithCache(refresh);
			break;
		case TABLE_COL_ACTIVITY: 
		{
			D_Organization org3 = data.get(row);
			result = new Integer (""+(org3.getCountActivity_WithCache(0, refresh) + org3.getCountNews_WithCache(0, refresh)));
		}
			break;
		case TABLE_COL_CONNECTION: 
		{
			int DAYS_OLD2 = 10;
			D_Organization org2 = data.get(row);
			result = new Boolean ((org2.getCountActivity_WithCache(DAYS_OLD2, refresh) + org2.getCountNews_WithCache(DAYS_OLD2, refresh)) > 0);
		}
		break;
		case TABLE_COL_NEWS: 
		{
			int DAYS_OLD = 10;
			D_Organization org4 = data.get(row);
			result = new Integer("" + org4.getCountNews_WithCache(-DAYS_OLD, refresh));
		}
			break;
		case TABLE_COL_PLUGINS:
		default:
		}
		return result;
	}
	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("OrgsModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return columnNames[col].toString();
	}
	@Override
	public void setValueAt(Object value, int row, int col) {
		D_Organization org = data.get(row);
		org = D_Organization.getOrgByOrg_Keep(org);
		String _value;
		switch(col) {
		case TABLE_COL_NAME:
			if (DEBUG) System.out.println("MotionsModel:setValueAt name obj: "+value);
			_value = Util.getString(value);
			if (DEBUG) System.out.println("MotionsModel:setValueAt name str: "+_value);
			if ("".equals(_value)) _value = null;
			if (DEBUG) System.out.println("MotionsModel:setValueAt name nulled: "+_value);
			if (org.getOrgNameMy() == null && _value == null) break;
			if (org.getOrgNameMy() == null && _value != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local pseudonym?") + "\n" + _value, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt name my opt = " + o);
					break;
				}
			}
			org.setOrgNameMy(_value);
			break;
		case TABLE_COL_CREATOR:
			String creator = Util.getString(value);
			if("".equals(creator)) creator = null;
			if (org.getCreatorNameMy() == null && creator == null) break;
			if (org.getCreatorNameMy() == null && creator != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local creator pseudonym?") + "\n" + creator, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt creator my opt = " + o);
					break;
				}
			}
			org.setCreatorMy(creator);
			break;
		case TABLE_COL_CATEGORY:
			if (DEBUG) System.out.println("MotionsModel:setValueAt cat obj: "+value);
			_value = Util.getString(value);
			if (DEBUG) System.out.println("MotionsModel:setValueAt cat str: "+_value);
			if ("".equals(_value)) _value = null;
			if (DEBUG) System.out.println("MotionsModel:setValueAt cat nulled: "+_value);
			if (org.getCategoryMy() == null && _value == null) break;
			if (org.getCategoryMy() == null && _value != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local creator pseudonym?") + "\n" + _value, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt category my opt = " + o);
					break;
				}
			}
			org.setCategoryMy(_value);
			break;
			default:
				org.releaseReference();
		}
		org.storeRequest();
		org.releaseReference();
		this.fireTableRowsUpdated(row, row);
	}
	public boolean isMine(int row) {
		if (row >= this.getRowCount()) return false;
		D_Organization org = data.get(row); 
		return org.currentlyEdited() || org.haveCreatorKey();
	}
	public boolean isNotReady(int row) {
		if(DEBUG) System.out.println("Orgs:isNotReady: row="+row);
		if (row >= this.getRowCount()) {
			if(DEBUG) System.out.println("Orgs:isNotReady: row>"+this.getRowCount());
			return false;
		}
		if (data.get(row).getGIDH_or_guess() == null) {
			if(DEBUG) System.out.println("Orgs:isNotReady: gid false");
			return true;
		}
		int method = -1;
		try {method = data.get(row).getCertifyingMethod();}catch(Exception e){}
		if ((method == net.ddp2p.common.table.organization._GRASSROOT) && (getOrgGIDH(row) == null)){
			if(DEBUG) System.out.println("Orgs:isNotReady: hash null");
			return true;
		}
		if(DEBUG) System.out.println("Orgs:isNotReady: exit false");
		return false;
	}
	public boolean isBlocked(int row) {
		if ((row < 0) || (row >= data.size())) return false;
		return data.get(row).getBlocked();
	}
	public boolean isBroadcasted(int row) {
		if ((row < 0) || (row >= data.size())) return false;
		return data.get(row).getBroadcasted();
	}
	public boolean isRequested(int row) {
		if ((row < 0) || (row >= data.size())) return false;
		return data.get(row).getRequested();
	}
}
