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
package net.ddp2p.widgets.peers;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.ddp2p.ciphersuits.KeyManagement;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.PeerListener;
import net.ddp2p.common.config.Peers_View;
import net.ddp2p.common.data.D_OrgDistribution;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.hds.ASNPluginInfo;
import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.plugin_data.D_PluginInfo;
import net.ddp2p.common.plugin_data.PluginMenus;
import net.ddp2p.common.plugin_data.PluginRegistration;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.DBSelector;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.DD_SK;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.ControlPane;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.app.Util_GUI;
import net.ddp2p.widgets.components.BulletRenderer;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.components.TableUpdater;
import net.ddp2p.widgets.dir_fw_terms.TermsPanel;
import net.ddp2p.widgets.private_org.PrivateOrgPanel;
import static net.ddp2p.common.util.Util.__;

class NoPluginRenderer implements TableCellRenderer{

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		return new JLabel("Absent");
	}
	
}

class MyComboBoxRenderer extends JLabel  implements TableCellRenderer {
//	class MyComboBoxRenderer extends DefaultTableCellRenderer  implements TableCellRenderer {
  public MyComboBoxRenderer() {
    super();
	setOpaque(true);
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
	  if(value instanceof JComboBox){
		  int cnt = ((JComboBox) value).getItemCount();
		  String text;
		  if (cnt <= 0) {
			  text = null;
			  this.setBackground(Color.WHITE);
		  } else {
			  text = "/#"+cnt+": "+Util.getString(((JComboBox) value).getItemAt(0));
			  if(cnt > 1) this.setBackground(Color.YELLOW);
			  else this.setBackground(Color.LIGHT_GRAY);
		  }
		  //if(cnt>1) text = text;
		  setText(text);
	  }else{
			setBackground(Color.WHITE);
		  setText(Util.getString(value));
	  }
    return this;
  }
}

@SuppressWarnings("serial")
class MyCComboBoxRenderer extends JComboBox<Object> implements TableCellRenderer {
  public MyCComboBoxRenderer() {
    super();
  }

  @SuppressWarnings("unchecked")
public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
	  this.removeAllItems();
	  if(value instanceof JComboBox){
		  for(int k=0; k<((JComboBox<Object>) value).getItemCount(); k++)
			  this.addItem(((JComboBox<Object>) value).getItemAt(k));
		  return this;
	  }
	  this.addItem(value);
    if (isSelected) {
      setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    } else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
    }
    setSelectedItem(value);
    return this;
  }
}

@SuppressWarnings("serial")
public class Peers extends JTable implements Peers_View, MouseListener, PeerListener {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final int COMMAND_MENU_SET_MYSELF = 0;
	static final int COMMAND_MENU_NEW_MYSELF = 1;
	static final int COMMAND_MENU_NEW_PLUGINS = 2;
	public static final int COMMAND_REFRESH_VIEWS = 3;
	public static final int COMMAND_MENU_SAVE_SK = 4;
	//public static final int COMMAND_MENU_LOAD_SK = 5;
	public static final int COMMAND_MENU_SET_NAME = 6;
	public static final int COMMAND_MENU_SET_SLOGAN = 7;
	public static final int COMMAND_TOUCH_CLIENT = 8;
	public static final int COMMAND_MENU_SET_TERMS = 9;
	public static final int COMMAND_MENU_SHARE_ORG = 10;
	static final int COMMAND_MENU_SET_EMAILS = 11;
	public static final int COMMAND_MENU_REFRESH = 12;
	public static final int COMMAND_MENU_BLOCK = 13;
	public static final int COMMAND_MENU_LOAD_PEER = 14;
	static final int COMMAND_MENU_REPLICATE_MYSELF = 15;
	public static final int COMMAND_MENU_RESIZE = 16;

	
	BulletRenderer bulletRenderer = new BulletRenderer();
	MyComboBoxRenderer myComboBoxRenderer;
	ColorRenderer colorRenderer;
	NoPluginRenderer noPluginRenderer = new NoPluginRenderer();
	ArrayList<PeerListener> listeners = new ArrayList<PeerListener>();
	public TermsPanel termsPanel;
	public PeerContacts peer_contacts;
	public PrivateOrgPanel privateOrgPanel; // to listen to Orgs
	private ImageIcon icon_register;
	private ImageIcon icon_org;
	static private JMenuItem refresh = new JMenuItem();
	static private JMenuItem resize = refresh;
	static private JMenuItem myself_new = refresh;
	static private JMenuItem myself_replicate = refresh;
	static private JMenuItem use_stop = refresh;
	static private JMenuItem use_start = refresh;
	static private JMenuItem block_stop = refresh;
	static private JMenuItem block_start = refresh;
	static private JMenuItem filter_stop = refresh;
	static private JMenuItem filter_start = refresh;
	static private JMenuItem serving_stop = refresh;
	static private JMenuItem serving_start = refresh;
	static private JMenuItem serve_default = refresh;
	static private JMenuItem share_org = refresh;
	static private JMenuItem myself_set = refresh;
	static private JMenuItem name_set = refresh;
	static private JMenuItem slogan_set = refresh;
	static private JMenuItem emails_set = refresh;
	static private JMenuItem terms_set = refresh;
	static private JMenuItem export_SK = refresh;
	static private JMenuItem import_SK = refresh;
	static private JMenuItem import_PEER = refresh;
	static private JMenuItem reset_peer = refresh;
	static private JMenuItem delete_row = refresh;
	static private JMenuItem wake_up = refresh;
	static private JMenuItem refresh_plugins = refresh;
	static private JMenuItem refresh_model = refresh;
	private static boolean popup_inited;
	
	/**
	 * Uses Application.db
	 */
	public Peers() {
		super(new PeersModel(Application.getDB()));
		this.initPopupItems();
		init();
	}
	public Peers(DBInterface _db) {
		super(new PeersModel(_db));
		if(DEBUG) System.out.println("PeersModel:did model");
		this.initPopupItems();
		if(DEBUG) System.out.println("PeersModel:did popup");
		init();
		if(DEBUG) System.out.println("PeersModel:did init");
	}
	public Peers(PeersModel dm) {
		super(dm);
		this.initPopupItems();
		init();
	}
	//area between 4x-x^2   and x-2
   	public void connectWidget(){
		getModel().connectWidget();
		Application.peers = (net.ddp2p.common.config.Peers_View) this;
		GUI_Swing.peer_contacts = this.peer_contacts;
		this.privateOrgPanel.addOrgListener();
		if(MainFrame.status!=null){
			addListener(MainFrame.status);
			if (DEBUG) System.out.println("Peers: connectWidget status");
		} else {
			if (DEBUG) System.out.println("Peers: connectWidget: status was null");
		}
		MainFrame.status.addPeerSelectedStatusListener(this);
	}
	public void disconnectWidget(){
		getModel().disconnectWidget();
		Application.peers = null;
		GUI_Swing.peer_contacts = null;
		this.privateOrgPanel.removeOrgListener();
		// not needed: if(DD.status!=null) removeListener(DD.status);
		MainFrame.status.removePeerSelectedListener(this);
	}

	/**
	 * 
	 * @param plugin_ID : unique string
	 * @param plugin_name : descriptive name
	 * @param column : column in widget table where the menu item appears
	 * @param plugin_menuItem : item
	 * @return
	 */
	public boolean registerPluginMenu(String plugin_ID, String plugin_name, int column, Action plugin_menuItem){
		return true;
	}
	/**
	 * 
	 * @param plugin_ID : unique string
	 * @param plugin_name : descriptive name
	 * @param column : column in widget table where the menu item appears
	 * @param plugin_menu_item
	 * @return
	 */
	public boolean registerPluginMenu(String plugin_ID, String plugin_name, int column, JMenuItem plugin_menu_item){
		return true;
	}
	/**
	 * 
	 * @param plugin_ID
	 * @param column : the column freed
	 * @return
	 */
	public boolean deregisterPluginMenu(String plugin_ID, int column){
		return true;
	}
	/**
	 * Does it need to be executed on Dispatcher Thread or not?
	 * @param plugin_GID
	 * @param plugin_name
	 * @param plugin_info
	 * @param plugin_url
	 * @param renderer
	 * @param editor
	 * @return
	 */
	public boolean registerPlugin(
			String plugin_GID,
			String plugin_name,
			String plugin_info,
			String plugin_url,
			TableCellRenderer renderer,
			TableCellEditor editor
			//,PeerPlugin plugin
			) {
		int columns = this.getModel().columnNames.length;//this.getColumnModel().getColumnCount();
		TableColumnModel cm = this.getColumnModel();
		int model_columns = cm.getColumnCount();
		if(columns>model_columns){
			System.err.println("Peers:registerPlugin: Less columns than desired: "+model_columns+" < "+columns);
		}
		int crt = D_PluginInfo.plugins.size()+PeersModel.TABLE_COL_PLUGINS-1;
		
		String colName = plugin_name;
		if(colName==null) colName=__("Plugin");
		colName = colName.substring(0, 5)+" "+(crt-columns+2);
		if(DEBUG)System.out.println("Peers:registerPlugin: adding plugin column val="+ crt+" = "+colName+" column="+columns+" mc="+model_columns);
		if(crt > model_columns) {
			if(_DEBUG)System.out.println("Peers:registerPlugin: fail adding plugin column val="+ crt+" = "+colName);
			return false;
		}
		if(crt < model_columns) {
			if(crt == columns-1) {
				String cl = this.getModel().getColumnName(crt);
				if(DEBUG)System.out.println("Peers:registerPlugin: adding plugin column val="+ crt+" = "+colName+" cl="+cl);
				try{
					TableColumn c = this.getColumn(cl); // Plugin not found
					c.setHeaderValue(colName);
				}catch(Exception e){e.printStackTrace();}
				//this.getColumnModel().addColumn(c);
				this.getModel().addColumn(crt, colName);
			}else{
				if(_DEBUG)System.out.println("Peers:registerPlugin: fail adding plugin column val="+ crt+" = "+colName+" m="+model_columns);
				return false;
			}
		}
		if(crt == model_columns) {
			TableColumn c = new TableColumn(crt);
	        c.setHeaderValue(colName);
			this.getColumnModel().addColumn(c);
			this.getModel().addColumn(crt, colName);
			if(DEBUG)System.out.println("Peers:registerPlugin: adding plugin column="+ crt+" = "+colName);
			//crt = columns-1;
		}		
		TableColumn appletColumn = this.getColumnModel().getColumn(crt);
		try{
			if(editor != null) appletColumn.setCellEditor(editor);
			if(renderer != null) appletColumn.setCellRenderer(renderer);
		}catch(Exception e){e.printStackTrace(); return false;}
		return true;
	}
	/**
	 * 
	 * @param plugin_ID
	 * @return
	 */
	public boolean deregisterPlugin(String plugin_GID){
		return true;
	}
	void init(){
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		colorRenderer = new ColorRenderer(getModel());
		myComboBoxRenderer = new MyComboBoxRenderer();
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        __("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
		icon_register = net.ddp2p.widgets.app.DDIcons.getRegistrationImageIcon("Register");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		icon_org = net.ddp2p.widgets.app.DDIcons.getOrgImageIcon("Org");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");

		
		DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel headerLabel = (JLabel)
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Icon icon = Peers.this.getModel().getIcon(column);
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
		return scrollPane;
	}
	/**
	 * Creates a panel and sets peer_contacts
	 * @return
	 */
    public JPanel getPanel() {
    	if(DEBUG) System.out.println("Peers:getPanel: start");
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setMinimumSize(new Dimension(100,50));
        
        //jp.add(scrollPane, BorderLayout.CENTER);
        //Application.peer_contacts = 
        peer_contacts = new PeerContacts();
        //jp.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, new JScrollPane(Application.peer)), BorderLayout.CENTER);
        JPanel p = new JPanel(new BorderLayout());
	//	p.add(new TermsPanel(1, "khalid"));
	//	p.add(Application.peer);
//        scrollPane.setPreferredSize(new Dimension(400, 200));
        termsPanel = new TermsPanel();
        this.addListener(termsPanel);
    	if(DEBUG) System.out.println("Peers:getPanel: added termsPanel");
    	JScrollPane scrollPane2 = new JScrollPane(termsPanel);
        scrollPane2.setMinimumSize(new Dimension(100,100));
        //p.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane2 ,new JScrollPane(Application.peer)));
       // p.add(new JScrollPane(new JScrollPane(Application.peer)) );

        privateOrgPanel = new PrivateOrgPanel();
        //if(DD.status != null) DD.status.addOrgStatusListener(privateOrgPanel);
        JScrollPane scrollPane3 = new JScrollPane(privateOrgPanel);
        scrollPane3.setMinimumSize(new Dimension(100,100));
        p.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        		new JSplitPane(JSplitPane.VERTICAL_SPLIT,scrollPane2,scrollPane3),
        		new JScrollPane(GUI_Swing.peer_contacts)));
        
        jp.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, p ), BorderLayout.CENTER);
        connectWidget();
		return jp;
    }

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
	   Object value = super.getValueAt(row, column);
	   if(value != null) {
	      if(value instanceof JComboBox) {
	           return new DefaultCellEditor((JComboBox)value);
	      }
	            return getDefaultEditor(value.getClass());
	   }
	   return super.getCellEditor(row, column);
	}
	public TableCellRenderer getCellRenderer(int row, int column) {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("Peers:getCellRenderer: start row="+row);
		//if ((column == PeersModel.COL_UDP_ON)) return bulletRenderer;
		//if ((column == PeersModel.COL_TCP_ON)) return bulletRenderer;
		//if ((column == DirectoriesModel.COL_NAT)) return bulletRenderer;
		if ((column == PeersModel.TABLE_COL_SERVING)) return myComboBoxRenderer;
		if ((column == PeersModel.TABLE_COL_NAME)) return colorRenderer;
		if ((column == PeersModel.TABLE_COL_SLOGAN)) return colorRenderer;
		if ((column == PeersModel.TABLE_COL_CONNECTION)) return bulletRenderer;
		if(DEBUG) System.out.println("Peers:getCellRenderer: start col="+column);
		if (column >= PeersModel.TABLE_COL_PLUGINS) {
			if(DEBUG) System.out.println("Peers:getCellRenderer: start plugin col="+column);
			
			
			int plug = column-PeersModel.TABLE_COL_PLUGINS;
			if(plug < D_PluginInfo.plugins.size()) {
				
				int model_row = this.convertRowIndexToModel(row);
				
				String pluginGID = D_PluginInfo.plugins.get(plug);
				//if(!presentPlugin(this.getModel(), model_row, pluginGID)) return noPluginRenderer;
				
				if(DEBUG) System.out.println("Peers:getCellRenderer: contained or myself");
				D_PluginInfo p = net.ddp2p.common.plugin_data.PluginRegistration.plugin_applets.get(pluginGID);
				//if(p!=null)return p.renderer;
				
				return new PluginPresenceRenderer((TableCellRenderer) p.mTableCellRenderer, noPluginRenderer, super.getCellRenderer(row, column));
			}else{
				if(DEBUG) System.out.println("Peers:getCellRenderer: outside column");				
			}
		}
		return super.getCellRenderer(row, column);
	}
	public static boolean presentPlugin(PeersModel peersModel, int model_row, String pluginGID) {
		String myselfGID = peersModel.getMyselfPeerGID();
		String peer_GID = peersModel.getGID(model_row);
		if((peer_GID!=null)&&(peer_GID.equals(myselfGID))){
			return true;// plugin_data.PluginRegistration.plugin_applets.get(pluginID).renderer;
		}
		if((peer_GID==null) || (pinfo==null)) return false;
		ASNPluginInfo[] info = pinfo.get(peer_GID);
		if(info == null){
			if(DEBUG) System.out.println("Peers:getCellRenderer: start empty plugin");
			return false;//super.getCellRenderer(row, column); // Empty renderer?
		}
		if(!D_PluginInfo.contains(info, pluginGID)){
			if(DEBUG) System.out.println("Peers:getCellRenderer: start absent plugin");
			return false;
		}
		return true;
	}
	public String getPluginGID(int column) {
		if(column < PeersModel.TABLE_COL_PLUGINS) return null;
		int plug = column-PeersModel.TABLE_COL_PLUGINS;
		if(plug >= D_PluginInfo.plugins.size()) return null;
		return D_PluginInfo.plugins.get(plug);
	}
    @SuppressWarnings("serial")
	protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
               java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                if(realIndex >= getModel().columnToolTips.length) return null;
				return getModel().columnToolTips[realIndex];
            }
        };
    }
	public void setConnectionState(String peerID, int state){
		((PeersModel) this.getModel()).setConnectionState(peerID, state);
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
	public PeersModel getModel(){
		return (PeersModel) super.getModel();
	}
	void initColumnSizes() {
        PeersModel model = (PeersModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        if (model.getColumnCount() != this.getColumnModel().getColumnCount()) {
        	if (DEBUG) System.out.println("Peers: initColumnSizes: conflict "+model.getColumnCount() +" vs "+ this.getColumnModel().getColumnCount());
        }
        for (int i = 0; i < model.getColumnCount(); i++) {
        	int headerWidth = 0;
        	int cellWidth = 0;
        	try{
        		if(i < this.getColumnModel().getColumnCount())
        			column = this.getColumnModel().getColumn(i);
        		else break;
        	}catch(Exception e){
        		e.printStackTrace();
        		break;
        	}
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	/*comp = this.getDefaultRenderer(model.getColumnClass(i)).
                        getTableCellRendererComponent(
                            this, getValueAt(r, i),
                            false, false, 0, i);*/
            	comp = this.getCellRenderer(r,i).
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
            if (this.getModel().getColumnClass(i).equals(Boolean.class)) {
            	column.setMaxWidth(Math.max(headerWidth, cellWidth));
             }
            if (this.getModel().getColumnClass(i).equals(Integer.class)) {
            	if (cellWidth != 0 && headerWidth > 2*cellWidth)
                	column.setMaxWidth(cellWidth);
            }
        }
    }
	/**
	 * @param args
	 * @throws P2PDDSQLException 
	 */
	public static void main(String[] args) throws P2PDDSQLException {
		String dfname = Application.DELIBERATION_FILE;
		Application.setDB(new DBInterface(dfname));
		PeersTest.createAndShowGUI(Application.getDB());
	}
	@Override
	public void mouseClicked(MouseEvent arg0) {
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
	}
	@Override
	public void mousePressed(MouseEvent arg0) {
    	jtableMouseReleased(arg0, false);
	}
	@Override
	public void mouseReleased(MouseEvent arg0) {
    	jtableMouseReleased(arg0, true);
	}
	static Object popup_monitor;
	void initPopupItems(){
		popup_monitor = this;
		new DDP2P_ServiceThread("Peers: initPopup", true) {
			public void _run() {
				_initPopupItems();
			}
		}.start();
	}
	static void _initPopupItems(){
		Peers _this = (Peers)Peers.popup_monitor;
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	//
    	refresh = new JMenuItem(new PeersRowAction(_this, __("Refresh!"), reseticon,__("Refresh."),
    			__("Refresh!"),KeyEvent.VK_R, Peers.COMMAND_MENU_REFRESH));
    	resize = new JMenuItem(new PeersRowAction(_this, __("Resize!"), reseticon,__("Resize."),
    			__("Resize!"),KeyEvent.VK_Z, Peers.COMMAND_MENU_RESIZE));
       	
        	//
//    	myself_new = new JMenuItem(new PeersRowAction(_this, _("New Myself!"), reseticon,_("Create new myself peer."),
//        			_("New Myself!"),KeyEvent.VK_N, Peers.COMMAND_MENU_NEW_MYSELF));
       	use_stop = new JMenuItem(new PeersUseAction(_this, __("Do not access it!"),addicon,__("Stop pulling from it."),__("Will not be used to synchronize."),KeyEvent.VK_A));
       	use_start = new JMenuItem(new PeersUseAction(_this, __("Access it!"),addicon,__("Pull from it."),__("Will be used to synchronize."),KeyEvent.VK_A));
    	//
       	block_stop = new JMenuItem(new PeersRowAction(_this, __("Unblock!"),addicon,__("Stop blocking it."),__("Will not be blocked to synchronize."),KeyEvent.VK_A,  Peers.COMMAND_MENU_BLOCK));
       	block_start = new JMenuItem(new PeersRowAction(_this, __("Block!"),addicon,__("Block it."),__("Will be blocked to synchronize."),KeyEvent.VK_A,  Peers.COMMAND_MENU_BLOCK));
    	//
    	filter_stop = new JMenuItem(new PeersFilterAction(_this, __("UnFilter!"),addicon,__("Get anything."),__("Will ask for anything new."),KeyEvent.VK_F));
    	filter_start = new JMenuItem(new PeersFilterAction(_this, __("Filter!"),addicon,__("Request only specified orgs."),__("Will only ask for organizations marked as requested."),KeyEvent.VK_F));
    	//
    	serving_stop  = new JMenuItem(new PeersBroadcastAction(_this, __("Stop Serving!"),addicon,__("Stop serving."),__("Will not be sent at synchronization."),KeyEvent.VK_B, false));
    	serving_start = new JMenuItem(new PeersBroadcastAction(_this, __("Serve!"),addicon,__("Serve it."),__("Will be sent at synchronize."),KeyEvent.VK_B, false));
    	//
    	serve_default = new JMenuItem(new PeersBroadcastAction(_this, __("Peer-Defined Serve!"),addicon,__("Default serving."),__("Will let peer decide serving."),KeyEvent.VK_D, true));
    	//
    	share_org = new JMenuItem(new PeersRowAction(_this, __("Share Org!"), reseticon,__("Share Organization."),
    			__("Share Organization!"),KeyEvent.VK_H, Peers.COMMAND_MENU_SHARE_ORG));
    	//
    	myself_set = new JMenuItem(new PeersRowAction(_this, __("Set as Myself!"), reseticon,__("Set this as myself peer."),
    			__("Set as Myself!"),KeyEvent.VK_M, Peers.COMMAND_MENU_SET_MYSELF));
    	//
    	name_set = new JMenuItem(new PeersRowAction(_this, __("Set New Name!"), reseticon,__("Set New Name."),
    			__("Set New Name!"),KeyEvent.VK_N, Peers.COMMAND_MENU_SET_NAME));
    	//
    	slogan_set = new JMenuItem(new PeersRowAction(_this, __("Set Slogan!"), reseticon,__("Set New Slogan."),
    			__("Set Slogan!"),KeyEvent.VK_M, Peers.COMMAND_MENU_SET_SLOGAN));
    	//
    	emails_set = new JMenuItem(new PeersRowAction(_this, __("Set Emails!"), reseticon,__("Set New Emails."),
    			__("Set Emails!"),KeyEvent.VK_E, Peers.COMMAND_MENU_SET_EMAILS));
    	//
    	terms_set = new JMenuItem(new PeersRowAction(_this, __("Set Terms"), reseticon,__("Set New Terms."),
    			__("Set Terms"),KeyEvent.VK_M, Peers.COMMAND_MENU_SET_TERMS));
    	//
    	export_SK = new JMenuItem(new PeersRowAction(_this, __("Export Secret keys!"), reseticon,__("Export Secret keys."),
    			__("Export Secret Keys!"),KeyEvent.VK_E, Peers.COMMAND_MENU_SAVE_SK));
    	//
    	/*
    	import_SK = new JMenuItem(new PeersRowAction(_this, _("Import Secret keys!"), reseticon,_("Import Secret keys."),
    			_("Import Secret Keys!"),KeyEvent.VK_E, Peers.COMMAND_MENU_LOAD_SK));
    	*/
    	//
    	import_PEER = new JMenuItem(new PeersRowAction(_this, __("Import New Peer!"), reseticon,__("Import New Peer."),
    			__("Import Secret Keys!"),KeyEvent.VK_E, Peers.COMMAND_MENU_LOAD_PEER));
    	//
    	reset_peer = new JMenuItem(new PeersResetAction(_this, __("Reset!"), reseticon,__("Bring again all data from this."), __("Go restart!"),KeyEvent.VK_R));
    	//
    	delete_row = new JMenuItem(new PeersDeleteAction(_this, __("Delete!"), delicon,__("Delete all data about this."), __("Reget"),KeyEvent.VK_D));
    	//
    	wake_up = new JMenuItem(new PeersRowAction(_this, __("Wake Up Communication!"), delicon,__("Wake Up Communication."),
    			__("Wake Up"),KeyEvent.VK_W, Peers.COMMAND_TOUCH_CLIENT));
    	//
    	// General actions
    	myself_new = new JMenuItem(new PeersRowAction(_this, __("New Myself!"), reseticon,__("Create new myself peer."),
    			__("New Myself!"),KeyEvent.VK_N, Peers.COMMAND_MENU_NEW_MYSELF));
    	
    	myself_replicate = new JMenuItem(new PeersRowAction(_this, __("Replicate Myself!"), reseticon,__("Replicate myself peer."),
    			__("Replicate Myself!"),KeyEvent.VK_P, Peers.COMMAND_MENU_REPLICATE_MYSELF));
       	
    	// General refresh plugins
    	refresh_plugins = new JMenuItem(new PeersRowAction(_this, __("RefreshPlugins!"), reseticon,__("Refresh installed Plugins."),
    			__("Refresh Plugins!"),KeyEvent.VK_P, Peers.COMMAND_MENU_NEW_PLUGINS));
       	
    	// General refresh plugins
    	refresh_model = new JMenuItem(new PeersRowAction(_this, __("RefreshModel!"), reseticon,__("Refresh Model."),
    			__("Refresh Model!"),KeyEvent.VK_R, Peers.COMMAND_REFRESH_VIEWS));
    	Peers.popup_inited = true;
		synchronized(Peers.popup_monitor){
			Peers.popup_monitor.notifyAll();
		}
	}

	JMenuItem setRow(JMenuItem item, Integer row){
		item.getAction().putValue("row", row);
    	return item;
	}
	
	JPopupMenu getPopup(int model_row, int col){
    	
		synchronized(Peers.popup_monitor){
			while(!Peers.popup_inited){
				try {
					Peers.popup_monitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
    	JPopupMenu popup = new JPopupMenu();
    	Integer _model_row = new Integer(model_row);
    	JMenuItem i;

    	//
    	popup.add(setRow(this.refresh,_model_row));
    	popup.add(setRow(this.resize,_model_row));
       	
       	if(model_row<0) {
        	//
        	popup.add(i = setRow(this.myself_new,_model_row));
        	i.setIcon(icon_register);
        	popup.add(i = setRow(this.myself_replicate,_model_row));
        	i.setIcon(icon_register);
       		return popup;
       	}
       	
       	// Variable Fields and toggles
    	if(this.getModel().used(model_row)){
        	popup.add(setRow(this.use_stop,_model_row));
    	}else{
        	popup.add(setRow(this.use_start,_model_row));
    	}
    	//blocked
    	if(this.getModel().blocked(model_row)){
        	popup.add(setRow(this.block_stop,_model_row));
    	}else{
        	popup.add(setRow(this.block_start,_model_row));
    	}
    	//
    	if(this.getModel().broadcastable(model_row)){
        	popup.add(setRow(this.serving_stop,_model_row));
    	}else{
        	popup.add(setRow(this.serving_start,_model_row));
    	}
    	//
    	if(this.getModel().filtered(model_row)){
        	popup.add(setRow(this.filter_stop,_model_row));
    	}else{
        	popup.add(setRow(this.filter_start,_model_row));
    	}
    	//
    	if(!this.getModel().broadcastable_default(model_row)) {
        	popup.add(setRow(this.serve_default,_model_row));
    	}
    	//
    	popup.addSeparator();
    	//
    	popup.add(i=setRow(this.share_org,_model_row));
    	i.setIcon(icon_org);
    	//
    	popup.add(setRow(this.terms_set,_model_row));
    	//
    	popup.add(setRow(this.export_SK,_model_row));
    	//
    	//popup.add(setRow(this.import_SK,_model_row));
    	//
    	popup.add(setRow(this.import_PEER,_model_row));
    	//
    	popup.add(setRow(this.reset_peer,_model_row));
    	//
    	popup.addSeparator();
    	//
    	popup.add(setRow(this.delete_row,_model_row));
    	//
    	popup.addSeparator();
    	//
    	popup.add(setRow(this.wake_up,_model_row));
    	//
    	popup.add(setRow(this.myself_set,_model_row));
    	//
    	popup.addSeparator();
    	//Actions_Myself
    	//
    	popup.add(setRow(this.slogan_set,_model_row));
    	//
    	popup.add(setRow(this.emails_set,_model_row));
    	//
    	//
    	popup.add(setRow(this.name_set,_model_row));
    	popup.add(i=setRow(this.myself_new,_model_row));
       	i.setIcon(icon_register);
    	popup.add(i=setRow(this.myself_replicate,_model_row));
    	i.setIcon(icon_register);

    	// General actions
    	// General refresh plugins
    	popup.add(setRow(this.refresh_plugins,_model_row));
    	// General refresh model
    	popup.add(setRow(this.refresh_model,_model_row));

    	// Plugin Actions
    	popup.addSeparator();
    	// Add Plugin-related menus
    	Hashtable<String, PluginMenus> mn = new Hashtable<String, PluginMenus>();
    	Hashtable<String, PluginMenus> mn1 = net.ddp2p.common.plugin_data.PluginRegistration.plugin_menus.get(new Integer(col));
    	Hashtable<String, PluginMenus> mn2 = net.ddp2p.common.plugin_data.PluginRegistration.plugin_menus.get(new Integer(PluginMenus.COLUMN_MYSELF));
    	Hashtable<String, PluginMenus> mn3 = net.ddp2p.common.plugin_data.PluginRegistration.plugin_menus.get(new Integer(PluginMenus.COLUMN_ALL));
     	String pluginGID = this.getPluginGID(col);
    	if(mn1 != null) mn.putAll(mn1);
    	if((mn2 != null)&&(pluginGID!=null)) mn = PluginMenus.merge(mn, mn2, pluginGID);
    	if(mn3 != null) mn = PluginMenus.merge(mn, mn3, null);
    	
    	
    	addPluginPopupMenusColumn(popup, model_row, mn);
    	return popup;
	}
	private void addPluginPopupMenusColumn(JPopupMenu popup, int model_row, Hashtable<String, PluginMenus> mn) {
    	//Hashtable<String, PluginMenus> mn = plugin_data.PluginRegistration.plugin_menus.get(new Integer(col));
    	if(mn == null) return;
     	Object peer_gid = this.getModel().getGID(model_row);
    	Object peer_alias = this.getModel().getAlias(model_row);
    	Object peer_name = this.getModel().getName(model_row);
    	Object peer_slogan = this.getModel().getSlogan(model_row);
    	boolean myPeer = false, otherAbsent = false, otherPresent = false;
    	
    	Object my_peer_GID = this.getModel().getMyselfPeerGID();
    	myPeer = (my_peer_GID!=null) && my_peer_GID.equals(peer_gid);
   	
    	for(String plug_gid : mn.keySet()){
    		if(!myPeer) {
    			if(presentPlugin(this.getModel(), model_row, plug_gid))
    				otherPresent = true;
    			else
    				otherAbsent = true;
    		}
    		popup.addSeparator();
    		D_PluginInfo i = D_PluginInfo.getPluginInfo(plug_gid);
    		String sep, name= null;
    		if (i!=null) name = i.plugin_name;
    		if(name != null) sep = name;
    		else sep = plug_gid;
    		popup.add(new JMenuItem("Plugin: "+sep));
    		PluginMenus pm = mn.get(plug_gid);
     		for(//plugin_data.Action
     				Object _pa: pm.plugin_menu_action){
     			Object __pa = (Object)_pa;
     			Action pa = (Action) __pa;
     			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: act myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
     			if(myPeer && (pa.getValue(PluginMenus.ROW_MYPEER)!=null)) continue;
     			if(otherAbsent && (pa.getValue(PluginMenus.ROW_OTHERS_ABSENT)!=null)) continue;
     			if(otherPresent && (pa.getValue(PluginMenus.ROW_OTHERS_PRESENT)!=null)) continue;
     			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: -act myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
     			
    			pa.putValue(PluginMenus.GID, peer_gid);
    			pa.putValue(PluginMenus.NAME, peer_name);
    			pa.putValue(PluginMenus.ALIAS, peer_alias);
    			pa.putValue(PluginMenus.SLOGAN, peer_slogan);
    			popup.add(pa);
    		}
    		for(//plugin_data.JMenuItem
    				Object _ma:  pm.plugin_menu_item){
    			Object __ma = (Object) _ma;
    			JMenuItem ma = (JMenuItem) __ma;
    			Action pa = ma.getAction();
     			if(pa!=null){
         			
         			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: mi.act myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
        			if(myPeer && (pa.getValue(PluginMenus.ROW_MYPEER)!=null)) continue;
         			if(otherAbsent && (pa.getValue(PluginMenus.ROW_OTHERS_ABSENT)!=null)) continue;
         			if(otherPresent && (pa.getValue(PluginMenus.ROW_OTHERS_PRESENT)!=null)) continue;
         			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: -mi.act myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
         			
    				pa.putValue(PluginMenus.GID, peer_gid);
        			pa.putValue(PluginMenus.NAME, peer_name);
        			pa.putValue(PluginMenus.ALIAS, peer_alias);
        			pa.putValue(PluginMenus.SLOGAN, peer_slogan);
    			}else{

         			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: mi myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
         			if(myPeer && (ma.getClientProperty(PluginMenus.ROW_MYPEER)!=null)) continue;
         			if(otherAbsent && (ma.getClientProperty(PluginMenus.ROW_OTHERS_ABSENT)!=null)) continue;
         			if(otherPresent && (ma.getClientProperty(PluginMenus.ROW_OTHERS_PRESENT)!=null)) continue;
         			if(DEBUG) System.out.println("Peers:addPluginPopupColumn: -mi myPeer="+myPeer+" otherAbsent="+otherAbsent+" otherPres="+otherPresent);
        			    				
    				ma.putClientProperty(PluginMenus.GID, peer_gid);
        			ma.putClientProperty(PluginMenus.NAME, peer_name);
        			ma.putClientProperty(PluginMenus.ALIAS, peer_alias);
        			ma.putClientProperty(PluginMenus.SLOGAN, peer_slogan);
    			}
    			popup.add(ma);
    		}
    	}
	}
	class DispatchPeer extends net.ddp2p.common.util.DDP2P_ServiceThread {
		Peers peers;
		int model_row;
		private boolean me;
		private boolean selected;
		private D_Peer peer;
		public DispatchPeer(Peers peers, int model_row, boolean me, boolean selected) {
			super("DispatchPeer", true);
			this.peers = peers;
			this.model_row = model_row;
			this.me = me;
			this.selected = selected;
			this.peer = null;
		}
		public DispatchPeer(D_Peer peer, boolean me, boolean selected) {
			super("DispatchPeer", true);
			this.peer = peer;
			this.me = me;
			this.selected = selected;
		}
		public void _run() {
			String my_peer_name = null;
			if (DEBUG) System.out.println("DispatchPeer:run:start");
			if (peer == null) {
				PeersModel model = peers.getModel();
				long id = -1;
				if (model_row >= 0){
					id = Util.lval(model.getID(model_row), -1);
					if (id <= 0) return;
					peer = D_Peer.getPeerByLID_NoKeep(id, true);
					my_peer_name = Util.getString(model.getValueAt(model_row, PeersModel.TABLE_COL_NAME));
					if(DEBUG) System.out.println("DispatchPeer:run: dispatch="+my_peer_name);
				}
			}else{
				my_peer_name = peer.component_basic_data.name;
			}
			if(DEBUG) System.out.println("DispatchPeer:run:listeners=#"+listeners.size());
			for(PeerListener l : listeners) {
				if(DEBUG) System.out.println("DispatchPeer:run:listener="+l);
				try{l.update_peer(peer, my_peer_name, me, selected);}catch(Exception e){e.printStackTrace();}
			}
		}
		
	}
	public void fireListener(D_Peer peer, boolean me, boolean selected){
		for(PeerListener l : listeners) {
			try{l.update_peer(peer, peer.component_basic_data.name, me, selected);}catch(Exception _e){_e.printStackTrace();}
		}
	}
	void dispatchToListeners(int model_row, boolean me, boolean selected){
		new DispatchPeer(this, model_row, me, selected).start();
	}
//	void updateTerms(int model_row){
//		if(model_row!=-1){
//			termsPanel.setPeerID(Integer.parseInt(this.getModel().getID(model_row)));
//			this.repaint();
//		}
//	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt, boolean release) {
    	int row; //=this.getSelectedRow();
    	int model_row=-1; //=this.getSelectedRow();
    	int col; //=this.getSelectedColumn();
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        if(DEBUG) System.out.println("Peers:jTableMouseRelease: row="+row);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if(row>=0)
        	model_row=this.convertRowIndexToModel(row);
        //updateTerms(model_row);
    	if(!evt.isPopupTrigger()){
    		if(release){
    			if(getModel().updates_requested){
    				getModel().updates_requested = false;
    				getModel().__update(null,null);
    				//initColumnSizes();
    			}	
    			return;
    		}else{
    			if(DEBUG) System.out.println("Peer:jtableMouseRelease: press");
    	        dispatchToListeners(model_row, false, true);
    			return;
    		}
    	}
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup != null)
    		popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
        dispatchToListeners(model_row, false, true);
		if(DEBUG) System.out.println("Peer:jtabkeMouseRelease: end");
   }
	/**
	 * Mark available plugins for this peer in the table, for display 
	 * (only if I also have that plugin)
	 * @param plugins
	 * @param _global_peer_ID
	 * @param _peer_ID
	 */
	public void setPluginsInfo(ASNPluginInfo[] plugins, String _global_peer_ID, String _peer_ID) {
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nPeers: setPluginsInfo: learning about available plugins from pID="+_peer_ID +" pGID="+Util.trimmed(_global_peer_ID));
		if(_global_peer_ID == null) {
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nPeers: setPluginsInfo: no source peerGID, drop!");
			//Util.printCallPath("setting plugin info from null");
			return;
		}
		pinfo.put(_global_peer_ID, plugins);
		this.getModel().updates_requested = true;
		if(this.getModel().automatic_refresh) {
			int row;
			row = this.getModel().getRowForPeerID(_peer_ID);
			for (int col = PeersModel.TABLE_COL_PLUGINS; col < this.getModel().getColumnCount(); col++) {
				this.tableChanged(new TableModelEvent(this.getModel(), row, row, col, col));
			}
			this.initColumnSizes();
		}
	}
	static Hashtable<String,ASNPluginInfo[]> pinfo = new Hashtable<String,ASNPluginInfo[]>();


	public void addListener(PeerListener l) {
		if(!listeners.contains(l)) listeners.add(l);
	}
	public void removeListener(PeerListener l) {
		listeners.remove(l);
	}
	@Override
	public void update_peer(D_Peer peer, String my_peer_name,
			boolean me, boolean selected) {
		if(!selected) return;
		if(peer==null) return;
		int model_row = getModel().getRowForPeerID(peer.getLIDstr());
		if(model_row<0) return;
		int view_row = this.convertRowIndexToView(model_row);
		this.setRowSelectionInterval(view_row, view_row);
		
		this.fireListener(peer, false, selected);
	}
	@Override
	public Object get_privateOrgPanel() {
		return this.privateOrgPanel;
	}
	@Override
	public String get_privateOrgPanel__get_organizationID() {

		if (this.privateOrgPanel == null) return null;
		
		return this.privateOrgPanel.get_organizationID();
	}
	@Override
	public boolean registerPluginMenu(String plugin_ID, String plugin_name,
			int column, //plugin_data.JMenuItem
			Object plugin_menu_item) {
		Object _plugin_menu_item = (Object) plugin_menu_item;
		return registerPluginMenu(plugin_ID, plugin_name, column, (JMenuItem) _plugin_menu_item);
	}
	@Override
	public void registerPlugin(String plugin_GID, String plugin_name,
			String plugin_info, String plugin_url,
			//plugin_data.TableCellRenderer
			Object renderer,
			//plugin_data.TableCellEditor
			Object editor) {

		Object _renderer = (Object) renderer;
		Object _editor = (Object) editor;

		registerPlugin(plugin_GID, plugin_name,
				plugin_info, plugin_url,
				(TableCellRenderer) _renderer,
				(TableCellEditor) _editor);
	}
	@Override
	public boolean registerPluginMenuAction(String plugin_GID, String plugin_name,
			int column, 
			//plugin_data.Action
			Object plugin_menuItem) {
		Object _plugin_menuItem = (Object) plugin_menuItem;
		return registerPluginMenu(plugin_GID, plugin_name,
				column, (Action) _plugin_menuItem);
	}
}
@SuppressWarnings("serial")
class PeersResetAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon;
    public PeersResetAction(Peers tree,
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
    	if (src instanceof JMenuItem) {
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG) System.err.println("PeersResetAction:row property: " + row);
    	} else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
   		if(DEBUG) System.err.println("PeersResetAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if (row < 0) return;
     	
     	
    	String peerID = model.getID(row);// Util.getString(model.getPeers().get(row).get(0));
    	D_Peer.reset(peerID, //null,
    			Util.CalendargetInstance());
    }
}
@SuppressWarnings("serial")
class PeersDeleteAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon;
    public PeersDeleteAction(Peers tree,
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
            if(DEBUG)System.err.println("Peers:PeersDeleteAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
       		if(DEBUG)System.err.println("Peers:PeersDeleteAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if(row<0) return;
     	D_Peer peer = model.getPeer(row);
    	String peerID = peer.getLIDstr_keep_force(); //Util.getString(model.getPeers().get(row).get(0));
    	int d = Application_GUI.ask(__("Are you willing to delete peer:")+" #"+peerID+":"+peer.getName(),
    			__("It is not recommended to delete peers"), JOptionPane.YES_NO_OPTION);
    	if (d!=0) return;
		peer.purge();
    }
}
@SuppressWarnings("serial")
class PeersUseAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon;
    public PeersUseAction(Peers tree,
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
            if(DEBUG)System.err.println("PeersUseAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row=tree.convertRowIndexToModel(row);
    		if(DEBUG)System.err.println("PeersUseAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if (row<0) return;
     	D_Peer peer = model.getPeer(row);
     	D_Peer.setUsed(peer, !peer.getUsed());
		try {
			DD.touchClient();
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		}
     	/*
    	String peerID = Util.getString(model.getPeers().get(row).get(0));
    	String used="1";
    	if(model.used(row)) used="0";
    	try {
			Application.db.update(table.peer.TNAME, new String[]{table.peer.used}, new String[]{table.peer.peer_ID},
					new String[]{used, peerID}, DEBUG);
			DD.touchClient();
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		*/
    }
}
@SuppressWarnings("serial")
class PeersFilterAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon;
    public PeersFilterAction(Peers tree,
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
            System.err.println("PeersUseAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row=tree.convertRowIndexToModel(row);
    		System.err.println("PeersUseAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if(row<0) return;
     	D_Peer peer = model.getPeer(row);
     	D_Peer.setFiltered(peer, !peer.getFiltered());
     	/*
    	String peerID = Util.getString(model.getPeers().get(row).get(0));
    	String filtered="1";
    	if(model.filtered(row)) filtered="0";
    	try {
			Application.db.update(table.peer.TNAME, new String[]{table.peer.filtered}, new String[]{table.peer.peer_ID},
					new String[]{filtered, peerID}, DEBUG);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    	*/
    }
}
class PeerUpdateThread extends Thread {
	PeersModel model;
	PeerUpdateThread(PeersModel _model){
		model = _model;
		start();
	}
	public void run(){
		model.__update(null, null);
	}
}
@SuppressWarnings("serial")
class PeersRowAction extends DebateDecideAction {
    public static final boolean DEBUG = false;
    public static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon; int command;
    public PeersRowAction(Peers tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree;
        this.icon = icon;
        this.command = command;
    }
	public static final JFileChooser filterUpdates = new JFileChooser();
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	String peerID;
        if(DEBUG)System.out.println("PeersRowAction:command property: " + command);
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG)System.out.println("PeersRowAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row=tree.convertRowIndexToModel(row);
    		if(DEBUG)System.out.println("PeersRowAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
    	ArrayList<ArrayList<Object>> a;
		switch(command){
		case Peers.COMMAND_MENU_BLOCK:
	    	//PeersModel model = (PeersModel)tree.getModel();
	     	if (row < 0) return;
	     	D_Peer peer = model.getPeer(row);
	     	D_Peer.setBlocked(peer, !peer.getBlocked());
			try {
				DD.touchClient();
			} catch (NumberFormatException e6) {
				e6.printStackTrace();
			}
	     	/*
	    	peerID = Util.getString(model.getPeers().get(row).get(0));
	    	String blocked="1";
	    	if(model.blocked(row)) blocked="0";
	    	try {
				Application.db.update(table.peer.TNAME, new String[]{table.peer.blocked}, new String[]{table.peer.peer_ID},
						new String[]{blocked, peerID}, DEBUG);
				DD.touchClient();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
	    	*/
			break;
		case Peers.COMMAND_MENU_REFRESH:
			new PeerUpdateThread(model);
			break;
		case Peers.COMMAND_MENU_RESIZE:
			//new PeerUpdateThread(model);
			tree.initColumnSizes();
			break;
		case Peers.COMMAND_MENU_SHARE_ORG:
			String peer_ID = model.getID(row);
			String org_ID = null;
			if((Application.peers!=null)&&(Application.peers.get_privateOrgPanel()!=null))
				org_ID = Application.peers.get_privateOrgPanel__get_organizationID();
			if(org_ID == null){
				Application_GUI.warning(__("No Organization selected"), __("Failure Adding Peer"));
				break;
			}
			try {
				D_OrgDistribution.add(org_ID, peer_ID);
				//((PrivateOrgTable)Application.peers.privateOrgPanel.getTable()).getPModel().update(null,null);
				//((PrivateOrgTable)Application.peers.privateOrgPanel.getTable()).getPModel().fireTableDataChanged();
				//Application.peers.privateOrgPanel.getTable().repaint();
				//Application.peers.privateOrgPanel.privateOrgTableScroll.repaint();
				//Application.peers.privateOrgPanel.repaint();
				//Application.peers.privateOrgPanel.getTable().repaint();
			} catch (P2PDDSQLException e5) {
				e5.printStackTrace();
			}
			break;
		case Peers.COMMAND_TOUCH_CLIENT:
			if (! DD.touchClient()) {
				// e4.printStackTrace();
				Application_GUI.warning(__("Error trying to restart comunication:")+"\n ",
				//+e4.getLocalizedMessage(),
				__("Error trying to restart communication!"));
			}
			break;
		case Peers.COMMAND_MENU_SET_NAME:
			try {
				ControlPane.changeMyPeerName(tree);
			} catch (P2PDDSQLException e4) {
				e4.printStackTrace();
				Application_GUI.warning(__("Error trying to change names:")+"\n "+e4.getLocalizedMessage(), __("Error trying to change names!"));
			}
			break;
		case Peers.COMMAND_MENU_SET_SLOGAN:
			try {
				ControlPane.changeMyPeerSlogan(tree);
			} catch (P2PDDSQLException e4) {
				e4.printStackTrace();
				Application_GUI.warning(__("Error trying to change slogan:")+"\n "+e4.getLocalizedMessage(), __("Error trying to change slogan!"));
			}
			break;
		case Peers.COMMAND_MENU_SET_EMAILS:
			try {
				if(_DEBUG)System.out.println("Peer:PeerRowAction: changing emails");
				ControlPane.changeMyPeerEmails(tree);
			} catch (P2PDDSQLException e4) {
				e4.printStackTrace();
				Application_GUI.warning(__("Error trying to change emails:")+"\n "+e4.getLocalizedMessage(), __("Error trying to change emails!"));
			}
			break;
		case Peers.COMMAND_MENU_SET_TERMS:
			TermsPanel termsPanel = new TermsPanel(Integer.parseInt(tree.getModel().getID(row)), tree.getModel().getName(row).toString());
			termsPanel.showJFrame();
//			try {
//			//	D_PeerAddress.changeMyPeerSlogan(tree);
//			} catch (P2PDDSQLException e4) {
//			//	e4.printStackTrace();
//			//	Application.warning(_("Error trying to change slogan:")+"\n "+e4.getLocalizedMessage(), _("Error trying to change slogan!"));
//			}
			break;
		case Peers.COMMAND_MENU_SAVE_SK:
			
			filterUpdates.setFileFilter(new net.ddp2p.widgets.components.UpdatesFilterKey());
			filterUpdates.setName(__("Select Secret Trusted Key"));
			//filterUpdates.setSelectedFile(null);
			Util_GUI.cleanFileSelector(filterUpdates);
			int returnVal = filterUpdates.showDialog(tree,__("Specify Trusted Secret Key File"));
			if (returnVal != JFileChooser.APPROVE_OPTION)  return;
			File fileTrustedSK = filterUpdates.getSelectedFile();
			SK sk;
			PK pk;
			if (fileTrustedSK.exists()) {
				int c = Application_GUI.ask(__("Existing file. Overwrite: "+fileTrustedSK+"?"), __("Overwrite file?"), JOptionPane.OK_CANCEL_OPTION);
				if (c != 0) {
					String gid = model.getGID(row);
					DD_SK dsk =  new DD_SK(); 
					try {
						if (KeyManagement.fill_sk(dsk, gid)) {
							byte[] esk = dsk.getBytes();
							StringSelection stringSelection = new StringSelection (Util.stringSignatureFromByte(esk));
							Clipboard clpbrd = Toolkit.getDefaultToolkit ().getSystemClipboard ();
							clpbrd.setContents (stringSelection, null);
						}
					} catch (HeadlessException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (P2PDDSQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					break;
				}
			}
			String gid = model.getGID(row);
			try {
				boolean result = false;
				if (fileTrustedSK.getName().endsWith("."+net.ddp2p.widgets.components.UpdatesFilterKey.EXT_SK)) {
					result = KeyManagement.saveSecretKey(gid, fileTrustedSK.getCanonicalPath());
				} else {
					DD_SK dsk =  new DD_SK(); 
					if (KeyManagement.fill_sk(dsk, gid)) {
						dsk.sign_and_set_sender(D_Peer.getPeerByGID_or_GIDhash_NoCreate(gid, null, true, false));
						if (_DEBUG) System.out.println("Peers: PeersRowAction: actionPerformed: savesk: will encode: "+dsk);
						
						String []explain = new String[1];
						result = DD.embedPeerInBMP(fileTrustedSK, explain, dsk);
					}		
				}
				if (result) break;
			} catch (P2PDDSQLException e3) {
				Application_GUI.warning(__("Failed to save key: "+e3.getMessage()), __("Failed to save key"));
				e3.printStackTrace();
				break;
//			} catch (IOException e3) {
//				Application_GUI.warning(__("Failed to save key: "+e3.getMessage()), __("Failed to save key"));
//				e3.printStackTrace();
//				break;
			} catch (Exception e4) {
				Application_GUI.warning(__("Failed to save key: "+e4.getMessage()), __("Failed to save key"));
				e4.printStackTrace();
				break;
			}
			Application_GUI.warning(__("Failed to save key: absent"), __("Failed to save key"));
//			String file_dest=JOptionPane.showInputDialog(tree,
//					_("Enter the name of the file"),
//					_("File to save secret key"),
//					JOptionPane.QUESTION_MESSAGE);
			break;
		case Peers.COMMAND_MENU_LOAD_PEER:
			PeersRowAction.loadPeer(tree);
			break;
			/*
		case Peers.COMMAND_MENU_LOAD_SK:
			filterUpdates.setFileFilter(new hds.UpdatesFilterKey());
			filterUpdates.setName(_("Select Secret Trusted Key"));
			//filterUpdates.setSelectedFile(null);
			Util.cleanFileSelector(filterUpdates);
			int loadVal = filterUpdates.showDialog(tree,_("Specify Trusted Secret Key File"));
			if (loadVal != JFileChooser.APPROVE_OPTION)  return;
			File fileLoadSK = filterUpdates.getSelectedFile();
			if(!fileLoadSK.exists()) {
				Application.warning(_("Inexisting file: "+fileLoadSK.getPath()), _("Inexisting file!"));
				break;
			}
			try{
				String []__pk = new String[1];
				SK new_sk = KeyManagement.loadSecretKey(fileLoadSK.getCanonicalPath(), __pk);
				String old_gid = model.getGID(row);
				String _pk=__pk[0];//Util.stringSignatureFromByte(new_sk.getPK().getEncoder().getBytes());
				D_PeerAddress old_peer = new D_PeerAddress(old_gid);
				PeerInput old_data = old_peer.getPeerInput();
				D_PeerAddress peer = D_PeerAddress.getPeer(new_sk, old_data, _pk);
				if (peer == null) {
					Application.warning(_("Failure loading SK"), _("Failure loading SK"));
				}
				//peer.component_basic_data.globalID = _pk;
				//peer.component_basic_data.globalIDhash=null;
				//peer._peer_ID = -1;
				//peer.peer_ID = null;
				//peer.sign(new_sk);
				//peer.storeVerified();
			} catch(Exception e2){}
			break;
			*/
	   	case Peers.COMMAND_MENU_NEW_MYSELF:
	   		//
    		try {
				D_Peer peer_old = HandlingMyself_Peer.get_myself_with_wait();
				PeerInput pi = PeerInput.getPeerInput(peer_old);
				pi.incName();
				//MyselfHandling.createMyPeerID(pi);
				D_Peer mepeer = net.ddp2p.common.data.HandlingMyself_Peer.createMyselfPeer_by_dialog_inited_w_Addresses(true, Application.getCurrent_Peer_ID(), pi);
				//D_Peer mepeer = HandlingMyself_Peer.createMyselfPeer_w_Addresses(pi, true);
				
				if ((mepeer != null) && (mepeer.component_basic_data != null))
					
					tree.fireListener(mepeer, true, false);
//					for(PeerListener l : tree.listeners) {
//						try{l.update_peer(peer, peer.component_basic_data.name, true, false);}catch(Exception _e){_e.printStackTrace();}
//					}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			break;
	   	case Peers.COMMAND_MENU_REPLICATE_MYSELF:
    		//
    		try {
				D_Peer peer_old = HandlingMyself_Peer.get_myself_with_wait();
				PeerInput pi = PeerInput.getPeerInput(peer_old);
				pi.incName();
				//MyselfHandling.createMyPeerID(pi);
				
				D_Peer mepeer = HandlingMyself_Peer.createMyselfPeer_w_Addresses(pi, true);
				
				if ((mepeer != null) && (mepeer.component_basic_data != null))
					tree.fireListener(mepeer, true, false);
//					for(PeerListener l : tree.listeners) {
//						try{l.update_peer(peer, peer.component_basic_data.name, true, false);}catch(Exception _e){_e.printStackTrace();}
//					}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			break;
    	case Peers.COMMAND_REFRESH_VIEWS:
			model.fireTableDataChanged();
    		break;
    	case Peers.COMMAND_MENU_NEW_PLUGINS:
    		//PluginRegistration.removePlugins();
    		new DDP2P_ServiceThread("Peers: loadPlugins", true) {
    			public void _run() {
    				PluginRegistration.loadNewPlugins();
    			}
    		}.start();
    		break;
    	case Peers.COMMAND_MENU_SET_MYSELF:
    		if (row < 0) return;
    		peerID = Util.getString(model.getPeers().get(row).get(PeersModel.SELECT_COL_ID));
    		try {
    			D_Peer me_peer = D_Peer.getPeerByLID(peerID, true, true);//.getPeerByLID_NoKeep(peerID, false);
    			String current = me_peer.getInstance();
    			String expected = HandlingMyself_Peer.peekSomeInstance(me_peer);
    			switch (Application_GUI.ask(__("Do you want to set the Noname instance?\nRather than: expected=\"")+expected+"\" / current=\""+current+"\"", __("Noname instance?"), JOptionPane.YES_NO_CANCEL_OPTION)) {
    			case 0:
    				if (_DEBUG) System.out.println("Peers:setmyself action: set some instance:"+expected);
        			HandlingMyself_Peer.forceSomeInstance(me_peer);
    				break;
    			case 1:
    				if (_DEBUG) System.out.println("Peers:setmyself action: set null instance");
    				me_peer.setCurrentInstance(null);
    				break;
    			default:
    				if (_DEBUG) System.out.println("Peers:setmyself action: keep current instance:"+current);
    				me_peer.releaseReference();
    				return;
    			}
				HandlingMyself_Peer.setMyself_currentIdentity_announceDirs(me_peer, true, true); // kept
				HandlingMyself_Peer.updateAddress(me_peer);
				me_peer.sign(me_peer.getSK());
				me_peer.storeRequest();
				me_peer.releaseReference();
				//D_PeerAddress peer = new D_PeerAddress(Util.lval(peerID));
				
				if ((me_peer != null) && (me_peer.component_basic_data != null))
					tree.fireListener(me_peer, true, false);
//					for(PeerListener l : tree.listeners) {
//						try{l.update_peer(peer, peer.component_basic_data.name, true, false);}catch(Exception _e){_e.printStackTrace();}
//					}
				
    		} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				Application_GUI.warning(__("Failure to set new ID:")+"\n"+e1.getLocalizedMessage(), __("Failure in message!"));
			}
    		break;
    	default:
    	}
    	/*
    	String filtered="1";
    	if(model.filtered(row)) filtered="0";
    	try {
			Application.db.update(table.peer.TNAME, new String[]{table.peer.filtered}, new String[]{table.peer.peer_ID},
					new String[]{filtered, peerID}, DEBUG);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		*/
    }
	/**
	 * Queries a file with secret keys and installs the peer
	 * @param parent
	 */
	public static D_Peer loadPeer(Component parent) {
		if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: start");
		D_Peer peer = null;
		JFileChooser filterUpdates = new JFileChooser();
		filterUpdates.setFileFilter(new net.ddp2p.widgets.components.UpdatesFilterKey());
		filterUpdates.setName(__("Select Secret Trusted Key"));
		//filterUpdates.setSelectedFile(null);
		Util_GUI.cleanFileSelector(filterUpdates);
		int loadNewPeerVal = filterUpdates.showDialog(parent,__("Specify Trusted Secret Key File"));
		if (loadNewPeerVal != JFileChooser.APPROVE_OPTION){
			if (D_Peer._DEBUG) System.out.println("D_Peer:LoadPeer: cancelled");
			return null;
		}
		File fileLoadPEER = filterUpdates.getSelectedFile();
		if (!fileLoadPEER.exists()) {
			if (D_Peer._DEBUG) System.out.println("D_Peer:LoadPeer: inexistant file: "+fileLoadPEER);
			Application_GUI.warning(__("Inexisting file: "+fileLoadPEER.getPath()), __("Inexisting file!"));
			return null;
		}
		if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: choice="+fileLoadPEER);
		try{
			String []__pk = new String[1];
			PeerInput file_data[] = new PeerInput[]{new PeerInput()};
			String _file_data[] = new String[]{null};
			boolean is_new[] = new boolean[1];
			if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: will load pk");
			SK new_sk = KeyManagement.loadSecretKey(fileLoadPEER.getCanonicalPath(), __pk, _file_data, is_new);
			file_data[0].name = _file_data[0];
			if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: loaded sk");
			if (new_sk == null) {
				Application_GUI.warning(__("Failure to load key!"), __("Loading Secret Key"));
				return null;
			}
			if (!is_new[0]) {
				Application_GUI.warning(__("Secret key already available!"), __("Loading Secret Key"));
				return null;
			}
			
			PK new_pk = new_sk.getPK();
			String new_gid = Util.getKeyedIDPK(new_pk);
			//String _pk=__pk[0];//Util.stringSignatureFromByte(new_sk.getPK().getEncoder().getBytes());
			if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: will load="+new_gid);
			peer = D_Peer.getPeerByGID_or_GIDhash(new_gid, null, true, true, false, null);
			if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: loaded peer="+peer);
			if (peer.getLIDstr_keep_force() == null) {
				if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: loaded ID=null");
				PeerInput data = new CreatePeer(MainFrame.frame, file_data[0], false).getData();
				if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: loaded ID data set");
				peer.setPeerInputNoCiphersuit(data);
			}
			if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: will make instance");
			peer.makeNewInstance();
			
			//if (isMyself(peer)){setInstance} //cannot be since I had no key
			//peer.component_basic_data.globalID = _pk;
			//peer.component_basic_data.globalIDhash=null;
			//peer._peer_ID = -1;
			//peer.peer_ID = null;
			if (D_Peer.DEBUG) System.out.println("D_Peer:LoadPeer: will sign peer");
			peer.sign(new_sk);
			peer.storeRequest();
		}catch(Exception e2){
			e2.printStackTrace();
			if (D_Peer._DEBUG) System.out.println("D_Peer:LoadPeer: exception");
			return null;
		}
		return peer;
	}
}
@SuppressWarnings("serial")
class PeersBroadcastAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	Peers tree; ImageIcon icon; boolean _default;
	/**
	 * _default se if we want to use peers.broadcastable vaue
	 * @param tree
	 * @param text
	 * @param icon
	 * @param desc
	 * @param whatis
	 * @param mnemonic
	 * @param _default
	 */
    public PeersBroadcastAction(Peers tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, boolean _default) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this._default=_default;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
            if(DEBUG) System.err.println("PeersBroadcastAction:row property: " + row);
    	}else {
    		row=tree.getSelectedRow();
    		row=tree.convertRowIndexToModel(row);
   		if (DEBUG) System.err.println("PeersBroadcastAction:Row selected: " + row);
    	}
    	PeersModel model = (PeersModel)tree.getModel();
     	if (row < 0) return;
    	//String peerID = model.getID(row); // Util.getString(model.getPeers().get(row).get(0));
    	//if (peerID == null) return;
    	D_Peer peer = model.getPeer(row); //D_Peer.getPeerByLID(peerID, true);
    	if (peer == null) return;
    	D_Peer.setBroadcastableMy(peer, !peer.getBroadcastableMyOrDefault());
    	//peer.dirty_my_data = true;
    	//peer.storeRequest();
    	/*
    	String broadcastable="1";
    	if (_default) broadcastable=null;
    	else if (model.broadcastable(row)) broadcastable="0";
    	try {
    		String sql="SELECT "+table.peer_my_data.broadcastable+" FROM "+table.peer_my_data.TNAME+" WHERE "+table.peer_my_data.peer_ID+"=?;";
    		ArrayList<ArrayList<Object>> m = Application.db.select(sql,new String[]{peerID}, DEBUG);
    		if(m.size()>0)
    			Application.db.update(table.peer_my_data.TNAME, new String[]{table.peer_my_data.broadcastable}, new String[]{table.peer_my_data.peer_ID},
    					new String[]{broadcastable, peerID}, DEBUG);
    		else
    			Application.db.insert(table.peer_my_data.TNAME, new String[]{table.peer_my_data.peer_ID,table.peer_my_data.broadcastable},
    					new String[]{peerID, broadcastable}, DEBUG);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    	*/
    }
}
@SuppressWarnings("serial")
class ColorRenderer extends JLabel implements TableCellRenderer {
	PeersModel model;
	public ColorRenderer(PeersModel _model) {
		super();
		model = _model;
		setOpaque(true);
	}
	@Override
	public Component getTableCellRendererComponent(JTable table, Object obj,
			boolean isSelected, boolean hasFocus, int row, int column) {
		row=table.convertRowIndexToModel(row);
		D_Peer peer = model.getPeer(row);
		if (peer == null) return this;
		if (peer.getBroadcastable()) { // model.broadcastable(row)) {
			setBackground(Color.WHITE);
		} else {
			setBackground(Color.LIGHT_GRAY);		
		}
		
		if(model.used(row)) {
			//setBackground(Color.WHITE);
			this.setForeground(Color.green);
			this.setText(Util.getString(obj));
			//System.out.println("Row="+row+" Col="+column+"  val="+obj);
		}else{
			//setBackground(Color.WHITE);
			this.setForeground(Color.BLACK);		
			this.setText(Util.getString(obj));
		}
		//setToolTipText("");
		return this;
	}	
}
/**
 * 
 */
@SuppressWarnings("serial")
class PeersModel extends AbstractTableModel implements TableModel, DBListener {
	public boolean automatic_refresh = true;
	static final int COL_NAT = 3;
	static final int COL_UDP_ON = 5;
	static final int COL_TCP_ON = 6;

	static int idx=0;
	public static final int TABLE_COL_NAME = 0;//idx++;
	public static final int TABLE_COL_VERIF_NAME = 1;//idx++;
	public static final int TABLE_COL_SERVING = 2;//idx++;
	public static final int TABLE_COL_CONNECTION = 3;//idx++;
	public static final int TABLE_COL_BROADCASTED = 4;//idx++;
	public static final int TABLE_COL_BLOCKED = 5;//idx++;
	public static final int TABLE_COL_HIDDEN = 6;//idx++;
	public static final int TABLE_COL_VALID = 7;//idx++;
	public static final int TABLE_COL_TEMP = 8;//idx++;
	public static final int TABLE_COL_REVOKED = 9;//idx++;
	public static final int TABLE_COL_EMAIL = 10;//idx++;
	public static final int TABLE_COL_VERIF_EMAIL = 11;//idx++;
	public static final int TABLE_COL_CATEGORY = 12;//idx++;
	public static final int TABLE_COL_SLOGAN = 13;//idx++;
	public static final int TABLE_COL_PROVIDER = 14;//idx++;
	public static final int TABLE_COL_LAST = 15;//idx++;
	public static final int TABLE_COL_PLUGINS = 16;//idx++;

	static final int SELECT_COL_ID = 0;
	static final int SELECT_COL_NAME = 1;
	static final int SELECT_COL_SLOGAN = 6;
	static final int SELECT_COL_USED = 7;
	static final int SELECT_COL_PICTURE = 8;
	static final int SELECT_COL_BLOCKED = 9;
	static final int SELECT_COL_BROADCASTABLE = 2;
	private static final int SELECT_COL_FILTERED = 12;
	static final int SELECT_COL_M_ID = 13;
	static final int SELECT_COL_M_NAME = 14;
	static final int SELECT_COL_M_SLOGAN = 15;
	static final int SELECT_COL_M_PICTURE = 16;
	static final int SELECT_COL_M_BROADCASTABLE = 17;
	static final int SELECT_COL_GID = 18;
	static final int SELECT_COL_M_TOPIC = 19;
	static final int SELECT_COL_HIDDEN = 20;
	static final int SELECT_COL_REVOKED = 21;
	static final int SELECT_COL_REVOKATION_INSTR = 22;
	static final int SELECT_COL_EMAIL_VERIF = 23;
	static final int SELECT_COL_NAME_VERIF = 24;
	static final int SELECT_COL_CATEG = 25;
	static final int SELECT_COL_EMAIL = 26;
	static final int SELECT_COL_SIGN = 27;
	static final int SELECT_COL_PROVIDER = 28;
	static final int SELECT_COL_LAST = 29;
/*
	static final String select_fields =
		" p."+table.peer.peer_ID+
		", p."+table.peer.name+
		", p."+table.peer.broadcastable+
		", p."+table.peer.last_sync_date+
		", p."+table.peer.arrival_date+
		", p."+table.peer.creation_date+
		", p."+table.peer.slogan+
		", p."+table.peer.used+
		", p."+table.peer.picture+
		", p."+table.peer.blocked+
		", p."+table.peer.exp_avg+
		", p."+table.peer.experience+
		", p."+table.peer.filtered+
		", m."+table.peer_my_data.peer_ID+
		", m."+table.peer_my_data.name+
		", m."+table.peer_my_data.slogan+
		", m."+table.peer_my_data.picture+
		", m."+table.peer_my_data.broadcastable+
		", p."+table.peer.global_peer_ID+
		", m."+table.peer_my_data.my_topic+
		", p."+table.peer.hidden+
		", p."+table.peer.revoked+
		", p."+table.peer.revokation_instructions+
		", p."+table.peer.email_verified+
		", p."+table.peer.name_verified+
		", p."+table.peer.category+
		", p."+table.peer.emails+
		", p."+table.peer.signature+
		", p."+table.peer.first_provider_peer+
		", p."+table.peer.last_sync_date
		;
*/
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final Object peers_monitor = new Object();
	protected static final long TIME_WAIT = 0;
	Hashtable<String,Integer> __rowByPeerID = new Hashtable<String,Integer>();
	Hashtable<String,Integer> connTCPByID = new Hashtable<String,Integer>();
	//Hashtable<String,Boolean> natByIPport = new Hashtable<String,Boolean>();
	//Hashtable<String,Integer> rowByIPport = new Hashtable<String,Integer>();
	//Hashtable<String,Boolean> onUDPByIPport = new Hashtable<String,Boolean>();
	//Hashtable<String,Boolean> onTCPByIPport = new Hashtable<String,Boolean>();
	DBInterface db;
	//String ld;	//-
	//String _ld[];  //-
	ArrayList<ArrayList<Object>> __peers;
	
	final String columnNames[]={__("Safes' Name"),"V","Serving",
			"Connection","Br","B","H","S","T","R","Email","V","Categ","Slogan","Provider","LastSync","Pluggins"};
	protected final String[] columnToolTips = {
			__("The name declared by the peer (white for default broadcastable)!")
			,__("Have you verified personally this peer name?")
			,__("The set of organizations advertised by this peer!")
			,__("Turn on (green) if the last connection attempt was successfull!")
			,__("Is this peer broadcasted?")
			,__("Is this peer blocked?")
			,__("Hide this peer in this widget?")
			,__("Is this peer information signed (validly)?")
			,__("Is this peer data temporary (under editing)?")
			,__("Is the key of this peer revoked?")
			,__("List of email addresses declared by this peer, separated by comma!")
			,__("Have you verified that these email addresses correspond to the user (in my_name)?")
			,__("Category for classification!")
			,__("A slogan provided by this peer!")
			,__("The peer from which you received the data leading to the creation of this item!")
			,__("The date in his database up to which you are synchronized with this peer!")
			,__("Pluggins installed on your system!")
			// Key Certified by a trusted authority (X509 certificate present)
			// preferences date
			// arrival date
			// date of the last connection/answer from this peer
			// number of instances recently connected
			};
	Hashtable<Integer,String> columnNamesHash=new Hashtable<Integer,String>();
	int plugin_applets = 0;
	int columns = columnNames.length-1 + plugin_applets;
	ArrayList<TableCellRenderer> appletRenderers;
	ArrayList<TableCellEditor> appletEditors;
	Peers p_table;
	public boolean updates_requested;
	private boolean verifyingSignatures = true;
	//private String[] _peers_ID;
	public void setTable(Peers _table){
		p_table = _table;
	}
	public int getRowForPeerID(String _peer_ID) {
		Integer _rowByPeerID = getRowByPeerID().get(_peer_ID);
		if (_rowByPeerID == null) return -1;
		return _rowByPeerID.intValue();
	}
	public String getMyselfPeerGID() {
		return HandlingMyself_Peer.getMyPeerGID();
	}
	public String getMyselfPeerID() {
		return HandlingMyself_Peer.getMyPeerID();
	}
	D_Peer getPeer(int model_row) {
		String id = getID(model_row);
		if (id == null) return null;
		return D_Peer.getPeerByLID_NoKeep(id, true);
	}
	
	public String getID(int model_row) {
		return Util.getString(getPeers().get(model_row).get(SELECT_COL_ID));
	}
	public Object getSlogan(int model_row) {
		return getPeer(model_row).getSlogan();
	}
	public Object getName(int model_row) {
		return getPeer(model_row).getName();
	}
	public Object getAlias(int model_row) {
		return getPeer(model_row).getNameMy();
	}
	/**
	 * Get GID for plugins
	 * @param model_row
	 * @return
	 */
	public String getGID(int model_row) {
		D_Peer p = getPeer(model_row);
		if (p == null) {
			System.err.println("Peer: getGID: null peer for row="+model_row);
			return null;
		}
		return p.getGID();
	}
	public boolean broadcastable_default(int row) {
		D_Peer peer = getPeer(row);
		return peer.getBroadcastableMyOrDefault();
		/*
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:broadcastable: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if(ao.size()<=SELECT_COL_M_BROADCASTABLE){
			if(DEBUG) System.out.println("PeersModel:broadcastable: BROADCASTABLE cols>="+ao.size());
			return true;
		}
		boolean result = true;
		Object _broadcastable_m = ao.get(SELECT_COL_M_BROADCASTABLE);
		if ("1".equals(Util.getString(_broadcastable_m))) result = false;
		else if ("0".equals(Util.getString(_broadcastable_m))) result = false;
		return result;
		*/
	}
	/**
	 * Checks both peers_my_data and peers
	 * @param row
	 * @return
	 */
	public boolean broadcastable(int row) {
		D_Peer peer = getPeer(row);
		if (peer == null) return false;
		return peer.getBroadcastableMyOrDefault();
		/*
		ArrayList<Object> ao;
		synchronized(this.peers_monitor){
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:broadcastable: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if((ao.size()<=SELECT_COL_BROADCASTABLE) && (ao.size()<=SELECT_COL_M_BROADCASTABLE)){
			if(DEBUG) System.out.println("PeersModel:broadcastable: BROADCASTABLE cols>="+ao.size());
			return false;
		}
		boolean result;
		Object _broadcastable_m = ao.get(SELECT_COL_M_BROADCASTABLE);
		if ("1".equals(Util.getString(_broadcastable_m))) result = true;
		else if ("0".equals(Util.getString(_broadcastable_m))) result = false;
		else {
			Object _broadcastable = ao.get(SELECT_COL_BROADCASTABLE);
			if(DEBUG) System.out.println("PeersModel:broadcastable: BROADCASTABLE val=\""+_broadcastable+"\"");
			result = "1".equals(Util.getString(_broadcastable));
		}
		if(DEBUG) System.out.println("PeersModel:broadcastable: BROADCASTABLE val="+result);
		return result;
		*/
	}
	public boolean filtered(int row) {
		return getPeer(row).getFiltered();
/*		
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:filtered: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if(ao.size()<=SELECT_COL_FILTERED){
			if(DEBUG) System.out.println("PeersModel:filtered: FILTERED cols>="+ao.size());
			return false;
		}
		Object _filtered = ao.get(SELECT_COL_FILTERED);
		if(DEBUG) System.out.println("PeersModel:filtered: FILTERED val=\""+_filtered+"\"");
		boolean result = "1".equals(Util.getString(_filtered));
		if(DEBUG) System.out.println("PeersModel:filtered: FILTERED val="+result);
		return result;
		*/
	}
	public boolean blocked(int row) {
		return getPeer(row).getBlocked();
		/*
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:used: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if(ao.size()<=SELECT_COL_BLOCKED){
			if(DEBUG) System.out.println("PeersModel:used: USED cols>="+ao.size());
			return false;
		}
		Object _used = ao.get(SELECT_COL_BLOCKED);
		if(DEBUG) System.out.println("PeersModel:used: USED val=\""+_used+"\"");
		boolean result = Util.stringInt2bool(_used, false);
		if(DEBUG) System.out.println("PeersModel:used: USED val="+result);
		return result;
		*/
	}
	public boolean used(int row) {
		D_Peer p = getPeer(row);
		if (p == null) return false;
		return p.getUsed();
		/*
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if((row > getPeers().size()) || (row < 0)){
				if(DEBUG) System.out.println("PeersModel:used: row>"+row+" psize="+getPeers().size());
				return false;
			}
			ao=getPeers().get(row);
		}
		if(ao.size()<=SELECT_COL_USED){
			if(DEBUG) System.out.println("PeersModel:used: USED cols>="+ao.size());
			return false;
		}
		Object _used = ao.get(SELECT_COL_USED);
		if(DEBUG) System.out.println("PeersModel:used: USED val=\""+_used+"\"");
		boolean result = "1".equals(Util.getString(_used));
		if(DEBUG) System.out.println("PeersModel:used: USED val="+result);
		return result;
		*/
	}
	net.ddp2p.common.util.DDP2P_ServiceThread refresher;
	PeersModel(DBInterface _db) {
		db = _db;
		if(DEBUG) System.out.println("PeersModel:will connectWidget");
		connectWidget();
		if(DEBUG) System.out.println("PeersModel:did connectWidget");
		__update(null, null);
		if(DEBUG) System.out.println("PeersModel:did update");
		
		refresher = new net.ddp2p.common.util.DDP2P_ServiceThread("Peers Refresher", true, this) {
			public void _run() {
				for (;;) {
					final int TIME_WAIT = 1000;
					synchronized(this) {
						try {
							wait(TIME_WAIT);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					PeersModel peer = (PeersModel) ctx;
					if (peer.updates_requested && peer.automatic_refresh)
						peer.__update(null,null);
				}
			}
		};
		refresher.start();
	}
	public void connectWidget() {
		db.addListener(this, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.peer.TNAME, net.ddp2p.common.table.peer_address.TNAME, net.ddp2p.common.table.peer_my_data.TNAME, net.ddp2p.common.table.peer_org.TNAME)),
				DBSelector.getHashTable(net.ddp2p.common.table.peer.TNAME, net.ddp2p.common.table.peer.used, "0"));
	}
	public void disconnectWidget(){
		db.delListener(this);
	}
	@Override
	public int getColumnCount() {
		if(DEBUG) System.out.println("PeersModel:getColumnCount:columns="+columnNames.length);
		if(p_table == null) return columnNames.length;
		return columnNames.length+D_PluginInfo.plugins.size()-1;
	}
	public void addColumn(int col, String string) {
		if(DEBUG) System.out.println("PeersModel:addColumn: ["+col+"]="+string);
		columnNamesHash.put(new Integer(""+col), string);
	}
	@Override
	public String getColumnName(int col) {
		String result = null;
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"]=?");
		if(columnNames.length-1<=col){
			result = columnNamesHash.get(new Integer(""+col));
			if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"/"+columnNames.length+"]="+result);
		}
		if((result == null)&&(col<columnNames.length)){
			result = columnNames[col].toString();
			if(DEBUG) System.out.println("PeersModel:getColumnName: colNames Header["+col+"/"+columnNames.length+"]="+result);
		}
		if(DEBUG) System.out.println("PeersModel:getColumnName: col Header["+col+"]="+result);
		return result;
	}
	public Icon getIcon(int column) {
		if(column == TABLE_COL_HIDDEN){ 
			return DDIcons.getHideImageIcon("Hidden");
		}
		if(column == TABLE_COL_TEMP){ 
			return DDIcons.getTmpImageIcon("TMP");
		}
//		if(column == PeersModel.GID){ 
//			return DDIcons.getGIDImageIcon("GID");
//		}
		if(column == PeersModel.TABLE_COL_CONNECTION){ 
			return DDIcons.getConnectedImageIcon("Connected");
		}
		if(column == PeersModel.TABLE_COL_BROADCASTED){ 
			return DDIcons.getBroadcastImageIcon("Broadcast");
		}
		if(column == PeersModel.TABLE_COL_BLOCKED){ 
			return DDIcons.getBlockImageIcon("Block");
		}
		if(column == PeersModel.TABLE_COL_SERVING){ 
			return DDIcons.getOrgImageIcon("Serving");
		}
		if(column == PeersModel.TABLE_COL_SLOGAN){ 
			return DDIcons.getNewsImageIcon("Serving");
		}
		if(column == PeersModel.TABLE_COL_VALID){ 
			return DDIcons.getSignedImageIcon("Signed");
		}
		if(column == PeersModel.TABLE_COL_VERIF_EMAIL){ 
			return DDIcons.getVerifImageIcon("Signed");
		}
		if(column == PeersModel.TABLE_COL_VERIF_NAME){ 
			return DDIcons.getVerifImageIcon("Signed");
		}
		if(column == PeersModel.TABLE_COL_REVOKED){ 
			return DDIcons.getRevokedImageIcon("Revoked");
		}
//		if (column == PeersModel.TABLE_COL_ARRIVAL_DATE)
//			return DDIcons.getLandingImageIcon("Arrival");	
		return null;
	}
	private String getPluginGIDcol(int col) {
		int plugin_col = col - columnNames.length+1;
		if(plugin_col<0) return null;
		if(plugin_col>=D_PluginInfo.plugins.size()) return null;
		return D_PluginInfo.plugins.get(plugin_col);
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == this.TABLE_COL_BLOCKED) return Boolean.class;
		if(col == this.TABLE_COL_CONNECTION) return Integer.class;
		if(col == this.TABLE_COL_BROADCASTED) return Boolean.class;
		if(col == this.TABLE_COL_HIDDEN) return Boolean.class;
		if(col == this.TABLE_COL_REVOKED) return Boolean.class;
		if(col == this.TABLE_COL_VERIF_EMAIL) return Boolean.class;
		if(col == this.TABLE_COL_VERIF_NAME) return Boolean.class;
		if(col == this.TABLE_COL_TEMP) return Boolean.class;
		if(col == this.TABLE_COL_VALID) return Boolean.class;
		if(col == this.TABLE_COL_SERVING) return JComboBox.class;
		
			return String.class;
	}		
	@Override
	public int getRowCount() {
		synchronized (this.peers_monitor) {
			ArrayList<ArrayList<Object>> p = getPeers();
			if (p == null) {
				if (DEBUG) System.out.println("PeersModel:getRowCount:rows=00");
				return 0;
			}
			if (DEBUG) System.out.println("PeersModel:getRowCount:rows="+p.size());
			return p.size();
		}
	}
	@Override
	public Object getValueAt(int row, int col) {
		String peerID;
		synchronized (this.peers_monitor) {
			peerID = getID(row);
			if (peerID == null) {
				if(DEBUG) System.out.println("PeersModel:getValueAt:No peer at: row="+row);
				return null;
			}
		}		
		if (col >= TABLE_COL_PLUGINS) {
			if (Peers.presentPlugin(this, row, this.getPluginGIDcol(col))) return peerID;
			return null;
		}
		D_Peer peer = D_Peer.getPeerByLID_NoKeep(peerID, true); //getPeer(row);
		if (peer == null) return null; // peer could have been deleted
		//if((col==0)&&(row==0))  Util.printCallPath("No");
		switch (col) {
		case TABLE_COL_LAST:
			return peer.getLastSyncDate(null);
		case TABLE_COL_NAME:
			return peer.getName_MyOrDefault();
		case TABLE_COL_SLOGAN:
			return peer.getSlogan_MyOrDefault();
		case TABLE_COL_SERVING:
			/*
			peerID = Util.getString(ao.get(0));
			String[] params = new String[]{peerID};
			ArrayList<ArrayList<Object>> orgs;
			try {
				orgs = Application.db.select("SELECT p."+table.peer_org.organization_ID+", o."+table.organization.name +
						" FROM "+table.peer_org.TNAME+" AS p " +
						" LEFT JOIN "+table.organization.TNAME+" AS o ON (p."+table.peer_org.organization_ID+" = o."+table.organization.organization_ID+") " +
						" WHERE p."+table.peer_org.peer_ID+" = ? AND o."+table.organization.name+" IS NOT NULL;", params, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return null;
			}
			if ((orgs == null) || (orgs.size()==0)) return _("Nothing");
			if(orgs.size()==1) return orgs.get(0).get(1);
			JComboBox<Object> ed = new JComboBox<Object>(Util.getColumn(orgs,1).toArray());
			*/
			//String orgs[] = new String[peer.served_orgs.length];
			JComboBox<Object> ed;
			try {
				if (peer.served_orgs != null)
					ed = new JComboBox<Object>(peer.served_orgs);
				else ed = new JComboBox<Object>();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Peers:getValueAt: served, faulty peer="+peer);
				System.out.println("Peers:getValueAt: faulty served="+peer.served_orgs+" ::= ["+Util.concat(peer.served_orgs, " --- ")+"]");
				ed = new JComboBox<Object>();
			}
			return ed;
			//break;
		case TABLE_COL_CONNECTION:
			//peerID = Util.getString(ao.get(0));
			return this.connTCPByID.get(peerID);
		case TABLE_COL_BROADCASTED:
			return peer.getBroadcastableMyOrDefault();
		case TABLE_COL_BLOCKED:
			//peerID = Util.getString(ao.get(0));
			//return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_BLOCKED), false));
			return peer.getBlocked();
		case TABLE_COL_HIDDEN:
			//peerID = Util.getString(ao.get(0));
			//return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_HIDDEN), false));
			return peer.getHidden();
		case TABLE_COL_PROVIDER:
			//peerID = Util.getString(ao.get(0));
			//return D_Peer.getDisplayName(Util.lval(ao.get(this.SELECT_COL_PROVIDER)));
			return peer.getProviderName();
		case TABLE_COL_VALID:
			if (verifyingSignatures && !peer.isTemporary()) {
				if (peer.signature_verified) return new Boolean(peer.last_signature_verified_successful);
				else return new Boolean(peer.verifySignature());
			} else return new Boolean(peer.last_signature_verified_successful);
			//peerID = Util.getString(ao.get(0));
			//return new Boolean(D_Peer.checkValid(Util.lval(peerID)));
		case TABLE_COL_TEMP:
			//peerID = Util.getString(ao.get(0));
			//return new Boolean(ao.get(this.SELECT_COL_SIGN)==null);
			return new Boolean(peer.isTemporary());
		case TABLE_COL_REVOKED:
			//peerID = Util.getString(ao.get(0));
			//return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_REVOKED), false));
			return new Boolean(peer.isRevoked());
		case TABLE_COL_VERIF_EMAIL:
			//peerID = Util.getString(ao.get(0));
			//return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_EMAIL_VERIF), false));
			return new Boolean(peer.isEmailVerified());
		case TABLE_COL_VERIF_NAME:
			//peerID = Util.getString(ao.get(0));
			//return new Boolean(Util.stringInt2bool(ao.get(this.SELECT_COL_NAME_VERIF), false));
			return new Boolean(peer.isNameVerified());
		case TABLE_COL_EMAIL:
			//peerID = Util.getString(ao.get(0));
			//return ao.get(this.SELECT_COL_EMAIL);
			return peer.getEmail();
		case TABLE_COL_CATEGORY:
			//peerID = Util.getString(ao.get(0));
			//return ao.get(this.SELECT_COL_CATEG);
			return peer.getCategory();
		default:
			if (DEBUG) System.out.println("PeersModel:getValueAt:Peer");
			return null;
		}
		/*
		if(col == COL_UDP_ON)return this.onUDPByIPport.get(this.ipPort(row));
		if(col == COL_TCP_ON)return this.onTCPByIPport.get(this.ipPort(row));
		if(col == COL_NAT) return this.natByIPport.get(this.ipPort(row));
		*/
//		if(ao.size() <= col){
//			if(DEBUG) System.out.println("PeersModel:getValueAt:No peer at: row="+row+" col="+col);
//			return null;
//		}
		//boolean used = "1".equals(Util.getString(peers.get(row).get(SELECT_COL_USED)));
		//String el = Util.getString(ao.get(col));
		//if((el!=null) && (used)) el = "<html><font color='green'>"+el+"</font></html>";
		//if(DEBUG) System.out.println("PeersModel:getValueAt:Peer at: row="+row+" col="+col+" val="+el);		
		//return null;
	}
	@Override
	public boolean isCellEditable(int row, int col) {
		switch(col){
		case TABLE_COL_NAME:
		case TABLE_COL_CATEGORY:
		case TABLE_COL_SLOGAN:
		case TABLE_COL_BROADCASTED:
		case TABLE_COL_BLOCKED:
		case TABLE_COL_HIDDEN:
		case TABLE_COL_VERIF_NAME:
		case TABLE_COL_VERIF_EMAIL:
		case TABLE_COL_TEMP:
		case TABLE_COL_VALID:
		case TABLE_COL_SERVING:
			return true;
		}
		return false;
	}
	/*
	public void set_my_data(String field, int SELECT_COL_M, String value, int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if(row >= getPeers().size()) return;
			if(SELECT_COL_ID >= getPeers().get(row).size()) return;
			ao = getPeers().get(row);
		}
		String peer_ID = Util.getString(ao.get(SELECT_COL_ID));
		if((SELECT_COL_M_ID >= ao.size())||(ao.get(SELECT_COL_M_ID)==null)) {
				try {
					db.insert(table.peer_my_data.TNAME, new String[]{table.peer_my_data.peer_ID,field},
							new String[]{peer_ID, value}, DEBUG);
					ao.set(SELECT_COL_M_ID, peer_ID);
					ao.set(SELECT_COL_M, value);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
		} else {
				try {
					db.update(table.peer_my_data.TNAME, new String[]{field}, new String[]{table.peer_my_data.peer_ID},
							new String[]{value, peer_ID}, DEBUG);
					ao.set(SELECT_COL_M, Util.getString(value));
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}					
		}		
	}
	public void set_data(String field, int SELECT_COL, String value, int row) {
		ArrayList<Object> ao;
		synchronized(this.peers_monitor) {
			if(row >= getPeers().size()) return;
			if(SELECT_COL_ID >= getPeers().get(row).size()) return;
			ao = getPeers().get(row);
		}
		String peer_ID = Util.getString(ao.get(SELECT_COL_ID));
		try {
			db.update(table.peer.TNAME, new String[]{field},
					new String[]{table.peer.peer_ID},
					new String[]{value, peer_ID}, DEBUG);
			ao.set(SELECT_COL, Util.getString(value));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}					
	}
	*/
	@Override
	public void setValueAt(Object value, int row, int col) {
		D_Peer peer = getPeer(row);
		if (peer == null) return;
		String _value;
		switch (col) {
		case TABLE_COL_NAME:
			//set_my_data(table.peer_my_data.name, SELECT_COL_M_NAME, Util.getString(value), row);
			if (DEBUG) System.out.println("MotionsModel:setValueAt name obj: "+value);
			_value = Util.getString(value);
			if (DEBUG) System.out.println("MotionsModel:setValueAt name str: "+_value);
			if ("".equals(_value)) _value = null;
			if (DEBUG) System.out.println("MotionsModel:setValueAt name nulled: "+_value);
			if (peer.getNameMy() == null && _value == null) break;
			if (peer.getNameMy() == null && _value != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local pseudonym?") + "\n" + _value, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt name my opt = " + o);
					break;
				}
			}
			D_Peer.setName_My(peer, _value);
			break;
		case TABLE_COL_BROADCASTED:
			D_Peer.setBroadcastableMy(peer, (Boolean)value);
			break;
		case TABLE_COL_REVOKED:
		{
			peer = this.getPeer(row);
			if (peer.getSK() == null)
				break;
			
			String msg;
			if ((Boolean)value) 
				msg = __("Do you really want to revoke:"+" "+getName(row));
			else
				msg = __("Do you really want to unrevoke:"+" "+getName(row));
			if (0 == Application_GUI.ask(msg,
					__("Are you sure?"), JOptionPane.OK_CANCEL_OPTION)) {
				D_Peer.setRevoked(peer, (Boolean)value);
			}
		}
			break;
		case TABLE_COL_BLOCKED:
			//set_data(table.peer.blocked, SELECT_COL_BLOCKED, Util.getString(value), row);
			D_Peer.setBlocked(peer, (Boolean)value);
			break;
		case TABLE_COL_CATEGORY:
			if (DEBUG) System.out.println("MotionsModel:setValueAt name obj: "+value);
			_value = Util.getString(value);
			if (DEBUG) System.out.println("MotionsModel:setValueAt name str: "+_value);
			if ("".equals(_value)) _value = null;
			if (DEBUG) System.out.println("MotionsModel:setValueAt name nulled: "+_value);
			//set_data(table.peer.category, SELECT_COL_CATEG, Util.getString(value), row);
			if (peer.getCategoryMy() == null && _value == null) break;
			if (peer.getCategoryMy() == null && _value != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local pseudocategory?") + "\n" + _value, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt name my opt = " + o);
					break;
				}
			}
			D_Peer.setCategory(peer, _value);
			break;
		case TABLE_COL_HIDDEN:
			//set_data(table.peer.hidden, SELECT_COL_HIDDEN, Util.getString(value), row);
			D_Peer.setHidden(peer, (Boolean)value);
			break;
		case TABLE_COL_TEMP:
			if(0 == Application_GUI.ask(__("Do you really want to drop signature for:"+" "+getName(row)),
					__("Are you sure?"), JOptionPane.OK_CANCEL_OPTION)){
				//String _value = null;
				//set_data(table.peer.signature, SELECT_COL_SIGN, _value, row);
				D_Peer.setTemporary(peer);
			}
			break;
		case TABLE_COL_VALID:
			if(0 == Application_GUI.ask(__("Do you really want to attempt re-signature for:"+" "+peer.getName()),
					__("Are you sure?"), JOptionPane.OK_CANCEL_OPTION)){
				//long ID = Util.lval(getID(row));
				try {
					//D_Peer.readSignSave(ID, ID, true);
					D_Peer.sign(peer);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		case TABLE_COL_VERIF_NAME:
			//set_data(table.peer.name_verified, SELECT_COL_NAME_VERIF, Util.getString(value), row);
			D_Peer.setName_Verified(peer, (Boolean)value);
			break;
		case TABLE_COL_VERIF_EMAIL:
			//set_data(table.peer.name_verified, SELECT_COL_NAME_VERIF, Util.getString(value), row);
			D_Peer.setEmail_Verified(peer, (Boolean)value);
			break;
		case TABLE_COL_SLOGAN:
			//set_my_data(table.peer_my_data.slogan, SELECT_COL_M_SLOGAN, Util.getString(value), row);
			if (DEBUG) System.out.println("MotionsModel:setValueAt name obj: "+value);
			_value = Util.getString(value);
			if (DEBUG) System.out.println("MotionsModel:setValueAt name str: "+_value);
			if ("".equals(_value)) _value = null;
			if (DEBUG) System.out.println("MotionsModel:setValueAt name nulled: "+_value);
			if (peer.getSloganMy() == null && _value == null) break;
			if (peer.getSloganMy() == null && _value != null) {
				int o = net.ddp2p.common.config.Application_GUI.ask(
						__("Do you want to set local pseudoslogan?") + "\n" + _value, 
						__("Changing local display"), JOptionPane.OK_CANCEL_OPTION);
				if (o != 0) {
					if (_DEBUG) System.out.println("MotionsModel: setValueAt name my opt = " + o);
					break;
				}
			}
			D_Peer.setSlogan_My(peer, _value);
			break;
		}
		/*
		if((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP) >= 0) return; 
		if((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_SEP) >= 0) return; 
		String el[] = _ld[row].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		String result="";
		for(int k=0; k<columns; k++) {
			if(k > 0) result = result + DD.APP_LISTING_DIRECTORIES_ELEM_SEP;
			if(k==col) result = result + value;
			else if(k<el.length) result = result + el[k];
			else result = result+"";
		}
		_ld[row] = result;
		try {
			String dirs = Util.concat(_ld, DD.APP_LISTING_DIRECTORIES_SEP);
			if(DEBUG)System.out.println("Directories: setValueAt: Setting "+dirs);
			DD.setAppTextNoSync(DD.APP_LISTING_DIRECTORIES, dirs);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		*/
		//fireTableCellUpdated(row, col);
		this.fireTableRowsUpdated(row, row);
	}
	public void setConnectionState(String peerID, int state) {
		if (peerID != null) this.connTCPByID.put(peerID, new Integer(state));
		else if (DEBUG) Util.printCallPath("Peers:PeerModel:setConnectionState setting null peer: "+state);
	}
	/*
	String ipPort(int k) {
		if((_ld==null)||(_ld.length<=k)) return null;
		int id1 = _ld[k].indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		if(id1<0) return null;
		int id2 = _ld[k].indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP,id1+1);
		if(id2<0) return _ld[k];//id2 = _ld[k].length();
		return _ld[k].substring(0, id2);
	}
	void setUDPOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setUDPOn:"+address+" at "+on);
		onUDPByIPport.put(address, on);
		int row = this.rowByIPport.get(address).intValue();
		this.fireTableCellUpdated(row, COL_UDP_ON);
	}
	void setTCPOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setTCPOn:"+address+" at "+on);
		onTCPByIPport.put(address, on);
		int row = this.rowByIPport.get(address).intValue();
		this.fireTableCellUpdated(row, COL_TCP_ON);
	}
	void setNATOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setNATOn:"+address+" at "+on);
		natByIPport.put(address, on);
		int row = this.rowByIPport.get(address).intValue();
		this.fireTableCellUpdated(row, COL_NAT);
	}
	*/
	@Override
	public void update(ArrayList<String> a_table, Hashtable<String,DBInfo> info) {
		if(DEBUG) System.out.println("Peers:update:peers start");
		
		if (a_table != null && !a_table.contains(net.ddp2p.common.table.peer.TNAME)) {
			SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
				// daemon?
				@Override
				public void _run() {
					((PeersModel)ctx).fireTableDataChanged();
				}
			});
			return;
		}
		
		updates_requested = true;
		/*
		if (automatic_refresh){
			__update(a_table, info);
		}
		*/
	}
	/*
	public static ArrayList<ArrayList<Object>> getAllPeersAllData() {
		ArrayList<ArrayList<Object>> _peers;
		try {
			String sql = "SELECT "+select_fields+ 
					" FROM "+table.peer.TNAME+" AS p " +
					" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON(p."+table.peer.peer_ID+"=m."+table.peer_my_data.peer_ID+") "+ 
					" ORDER BY "+ table.peer.used+" DESC, "+table.peer.last_sync_date+" DESC; ";
			String[] params = new String[0];
			if(DEBUG) System.out.println("PeersModel:update: will select");
			_peers = Application.db.select(sql, params, DEBUG);
			if(DEBUG) System.out.println("PeersModel:update: did select");
			
//			if(ld!=null) {
//				_ld=ld.split(DD.APP_LISTING_DIRECTORIES_SEP);
//				for(int k=0; k<_ld.length; k++) {
//					if(DEBUG)System.out.println("Directories:update:"+k+" is "+_ld[k]);
//					String ipPort = ipPort(k);
//					if(ipPort == null) continue;
//					this.rowByIPport.put(ipPort, new Integer(k));
//				}
//			}
			
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		return _peers;
	}
	*/
	public void __update(ArrayList<String> a_table, Hashtable<String,DBInfo> info) {
		if(DEBUG) System.out.println("Peers:update:peers start");
		ArrayList<ArrayList<Object>> _peers = D_Peer.getAllPeers();
		
		if (__peers != null && (_peers.size() == __peers.size())) {
			boolean different = false;
			for (int k = 0; k < __peers.size(); k++) {
				//Util.lval(_justifications[k]);
				if (Util.lval(__peers.get(k).get(SELECT_COL_ID)) != Util.lval(_peers.get(k).get(SELECT_COL_ID))) {
					different = true;
					break;
				}
			}
			if (! different) {
				SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
					// daemon?
					@Override
					public void _run() {
						((PeersModel)ctx).fireTableDataChanged();
					}
				});
				return;
			}
		}
		

		Hashtable<String,Integer> _rowByPeerID = new Hashtable<String,Integer>();
		if (DEBUG) System.out.println("Peers:update:peers#="+_peers.size());
		//_peers_ID = new String[peers.size()];
		for (int i = 0; i < _peers.size(); i++) {
			_rowByPeerID.put(Util.getString(_peers.get(i).get(SELECT_COL_ID)), new Integer(i));
			//_peers_ID[i] = Util.getString(peers.get(i).get(SELECT_COL_ID));
		}
		synchronized(this.peers_monitor) {
			__peers = _peers;
			__rowByPeerID = _rowByPeerID;
		}
		
		SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable(__("invoke swing"), false, false, this) {
			// daemon?
			@Override
			public void _run() {
				((PeersModel)ctx).fireTableDataChanged();
			}
		});
		
		//this.fireTableStructureChanged();
//		try {
//			this.fireTableDataChanged();
//		} catch(Exception e) {if (DEBUG) e.printStackTrace();}
		/**
		 * The next updater is executed in the Swing thread (called with SwingUtilities.invokeLater(this);).
		 * Should be modified to keep the selected value.
		 */
		new TableUpdater(this, p_table, null);
		//this.fireTableDataChanged();
		if(DEBUG) System.out.println("Peers:update:peers done");
	}
	
	ArrayList<ArrayList<Object>> getPeers() {
		synchronized(this.peers_monitor) {
			return __peers;
		}
	}
	Hashtable<String, Integer> getRowByPeerID(){
		synchronized(this.peers_monitor) {
			return __rowByPeerID;
		}
	}
/*
	@Override
	public void addTableModelListener(TableModelListener arg0) {		
	}


	@Override
	public void removeTableModelListener(TableModelListener arg0) {
		// 
	}
	*/
}
class PeersTest extends JPanel {
    Peers tree;
    public PeersTest(DBInterface db) {
    	super(new BorderLayout());
    	tree = new Peers( new PeersModel(db));
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        add(scrollPane, BorderLayout.CENTER);
		//JScrollPane scrollPane = new JScrollPane(dirs);
		tree.setFillsViewportHeight(true);
    }
    public static void createAndShowGUI(DBInterface db) {
        JFrame frame = new JFrame("Directories Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PeersTest newContentPane = new PeersTest(db);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
}
