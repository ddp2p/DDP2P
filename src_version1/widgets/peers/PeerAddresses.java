package widgets.peers;

import static util.Util._;
import hds.Address;
import hds.DebateDecideAction;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import ASN1.Encoder;
import util.DBInfo;
import util.DBListener;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import config.DD;
import config.DDIcons;
import config.Identity;
import data.D_PeerAddress;
@SuppressWarnings("serial")
class  PeerAddressesModel  extends AbstractTableModel implements TableModel, DBListener {

	private static final Object peers_monitor = new Object();
	private static final boolean DEBUG = false;
	private PeerAddresses table;
	D_PeerAddress peer;
	String global_peer_ID;
	boolean me;
	
	private ArrayList<ArrayList<Object>> __peers;
	String columnNames[]={_("Type"),_("Domain"),_("UDP"),_("TCP"),_("Certified"),_("Priority"),_("Transient"),_("Loopback")};
	final int TABLE_COL_TYPE = 0;
	final int TABLE_COL_DOMAIN = 1;
	final int TABLE_COL_UDP = 2;
	final int TABLE_COL_TCP = 3;
	final int TABLE_COL_CERTIFIED = 4;
	final int TABLE_COL_PRIORITY = 5;
	final int TABLE_COL_TRANSIENT = 6;
	final int TABLE_COL_LOOPBACK = 7;
	
	PeerAddressesModel() {
		this.global_peer_ID = Identity.getMyPeerGID();
		try {
			init();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	PeerAddressesModel(String peerGID) {
		this.global_peer_ID = peerGID;
		try {
			init();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	
	public void __update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		try {
			init();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		__update(table, info);
	}
	void init() throws P2PDDSQLException{
		synchronized(this.peers_monitor){
			if(global_peer_ID == null){
				peer = null;
				me = false;
				return;
			}
			if(global_peer_ID.equals(Identity.getMyPeerGID())){
				me = true;
			}else{
				me = false;
			}
			if((peer != null) && (global_peer_ID.equals(peer.getGID()))) return;
			if(me){
				peer = D_PeerAddress.get_myself_from_Identity();
			}else{
				peer = new D_PeerAddress(global_peer_ID,0,false);
			}
		}
		SwingUtilities.invokeLater(new Refresher(this));
		//this.fireTableDataChanged();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public String getColumnName(int col) {
		String result = null;
		if(DEBUG) System.out.println("PeerAddrModel:getColumnName: col Header["+col+"]=?");
		if((result == null)&&(col<columnNames.length)){
			result = columnNames[col].toString();
			if(DEBUG) System.out.println("PeerAddrModel:getColumnName: colNames Header["+col+"/"+columnNames.length+"]="+result);
		}
		if(DEBUG) System.out.println("PeerAddrModel:getColumnName: col Header["+col+"]="+result);
		return result;
	}
	int getStoreAddrLen(){
		if(!((peer == null) || (peer.address == null))) return peer.address.length;	
		return 0;
	}
	int getDirectoriesLen() {
		if (Identity.listing_directories_string == null) return 0;
		return Identity.listing_directories_string.size();
	}
	int getSocketsLen() {
		if (Identity.my_server_domains == null) return 0;
		return Identity.my_server_domains.size();
	}
	int getLoopbackLen() {
		if (Identity.my_server_domains_loopback == null) return 0;
		return Identity.my_server_domains_loopback.size();
	}
	@Override
	public int getRowCount() {
		synchronized(this.peers_monitor){
			if(!me) {
				if((peer == null) || (peer.address == null)) return 0;
				return peer.address.length;
			}else{
				int rows = 0;
				rows += getStoreAddrLen();
				rows += getDirectoriesLen();
				rows += getSocketsLen();
				rows += getLoopbackLen();
				return rows;
			}
		}
	}
//
//	private ArrayList<ArrayList<Object>> getPeers() {
//		synchronized(this.peers_monitor) {
//			return __peers;
//		}
//	}

	@Override
	public Object getValueAt(int row, int col) {
		int crt;
		crt = this.getStoreAddrLen();
		if(row < crt) return getValueAt_Stored(row, col);
		row -= crt;

		crt = this.getDirectoriesLen();
		if(row < crt) return getValueAt_Dirs(row, col);
		row -= crt;
		
		crt = this.getSocketsLen();
		if(row < crt) return getValueAt_Socks(row, col);
		row -= crt;
				
		crt = this.getLoopbackLen();
		if(row < crt) return getValueAt_Loopback(row, col);
		row -= crt;
		return null;
	}
	private Object getValueAt_Loopback(int row, int col) {
		switch(col){
		case TABLE_COL_TYPE:
			return Address.SOCKET;
		case TABLE_COL_DOMAIN:
			return Identity.my_server_domains_loopback.get(row).getHostAddress();
		case TABLE_COL_UDP:
			return new Integer(Identity.udp_server_port);
		case TABLE_COL_TCP:
			return new Integer(Identity.port);
		case TABLE_COL_CERTIFIED:
			return Boolean.FALSE;
		case TABLE_COL_PRIORITY:
			return new Integer(0);
		case TABLE_COL_TRANSIENT:
			return Boolean.TRUE;
		case TABLE_COL_LOOPBACK:
			return Boolean.TRUE;
		}
		return null;
	}
	private Object getValueAt_Socks(int row, int col) {
		switch(col){
		case TABLE_COL_TYPE:
			return Address.SOCKET;
		case TABLE_COL_DOMAIN:
			return Identity.my_server_domains.get(row).getHostAddress();
		case TABLE_COL_UDP:
			return new Integer(Identity.udp_server_port);
		case TABLE_COL_TCP:
			return new Integer(Identity.port);
		case TABLE_COL_CERTIFIED:
			return Boolean.FALSE;
		case TABLE_COL_PRIORITY:
			return new Integer(0);
		case TABLE_COL_TRANSIENT:
			return Boolean.TRUE;
		case TABLE_COL_LOOPBACK:
			return Boolean.FALSE;
		}
		return null;
	}
	@Override
	public Class<?> getColumnClass(int col) {
		switch(col){
		case TABLE_COL_LOOPBACK:
		case TABLE_COL_TRANSIENT:
		case TABLE_COL_CERTIFIED:
			return Boolean.class;
		case TABLE_COL_PRIORITY:
		case TABLE_COL_UDP:
		case TABLE_COL_TCP:
			return Integer.class;
		}
		return String.class;
	}		
	private Object getValueAt_Dirs(int row, int col) {
		switch(col){
		case TABLE_COL_TYPE:
			return Address.DIR;
		case TABLE_COL_DOMAIN:
			return new Address(Identity.listing_directories_string.get(row)).domain;
		case TABLE_COL_UDP:
			return new Integer(new Address(Identity.listing_directories_string.get(row)).udp_port);
		case TABLE_COL_TCP:
			return new Integer(new Address(Identity.listing_directories_string.get(row)).tcp_port);
		case TABLE_COL_CERTIFIED:
			return Boolean.FALSE;
		case TABLE_COL_PRIORITY:
			return new Integer(0);
		case TABLE_COL_TRANSIENT:
			return Boolean.TRUE;
		case TABLE_COL_LOOPBACK:
			return Boolean.FALSE;
		}
		return null;
	}
	private Object getValueAt_Stored(int row, int col) {
		switch(col){
		case TABLE_COL_TYPE:
			return peer.address[row].type;
		case TABLE_COL_DOMAIN:
			return new Address(peer.address[row].address).domain;
		case TABLE_COL_UDP:
			return new Integer(new Address(peer.address[row].address).udp_port);
		case TABLE_COL_TCP:
			return new Integer(new Address(peer.address[row].address).tcp_port);
		case TABLE_COL_CERTIFIED:
			return new Boolean(peer.address[row].certified);
		case TABLE_COL_PRIORITY:
			return new Integer(peer.address[row].priority);
		case TABLE_COL_TRANSIENT:
			return Boolean.FALSE;
		case TABLE_COL_LOOPBACK:
			return Boolean.FALSE;
		}
		return null;
	}

	public void setTable(PeerAddresses peerAddresses) {
		table = peerAddresses;
	}

	public boolean store(int row) {
		int crt;
		crt = this.getStoreAddrLen();
		if(row < crt) return store_Stored(row);
		row -= crt;

		crt = this.getDirectoriesLen();
		if(row < crt) return store_Dirs(row);
		row -= crt;
		
		crt = this.getSocketsLen();
		if(row < crt) return store_Socks(row);
		row -= crt;
				
		crt = this.getLoopbackLen();
		if(row < crt) return store_Loopback(row);
		row -= crt;
		return false;
	}

	private boolean store_Stored(int row) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean store_Dirs(int row) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean store_Socks(int row) {
		Calendar _arrival_date = Util.CalendargetInstance();
		String arrival_date = Encoder.getGeneralizedTime(_arrival_date);
		String type = Address.SOCKET;
		Address crt = new Address(
				(String)getValueAt_Socks(row,TABLE_COL_DOMAIN),
				((Integer)getValueAt_Socks(row,TABLE_COL_TCP)).intValue(),
				((Integer)getValueAt_Socks(row,TABLE_COL_UDP)).intValue());
		String _address = crt.toString();
		try {
			peer.addAddress(_address, type, _arrival_date, arrival_date, true, peer.getMaxCertifiedAddressPriority()+1, true);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		this.fireTableDataChanged();
		return true;
	}

	private boolean store_Loopback(int row) {
		Calendar _arrival_date = Util.CalendargetInstance();
		String arrival_date = Encoder.getGeneralizedTime(_arrival_date);
		String type = Address.SOCKET;
		Address crt = new Address(
				(String)getValueAt_Loopback(row,TABLE_COL_DOMAIN),
				((Integer)getValueAt_Loopback(row,TABLE_COL_TCP)).intValue(),
				((Integer)getValueAt_Loopback(row,TABLE_COL_UDP)).intValue());
		String _address = crt.toString();
		try {
			peer.addAddress(_address, type, _arrival_date, arrival_date, true, peer.getMaxCertifiedAddressPriority()+1, true);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		this.fireTableDataChanged();
		return true;
	}

	public boolean delete(int row) {
		int crt;
		crt = this.getStoreAddrLen();
		if(row < crt) return delete_Stored(row);
		row -= crt;

		crt = this.getDirectoriesLen();
		if(row < crt) return delete_Dirs(row);
		row -= crt;
		
		crt = this.getSocketsLen();
		if(row < crt) return delete_Socks(row);
		row -= crt;
				
		crt = this.getLoopbackLen();
		if(row < crt) return delete_Loopback(row);
		row -= crt;
		return false;
	}

	private boolean delete_Loopback(int row) {
		return false;
	}
	private boolean delete_Socks(int row) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean delete_Dirs(int row) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean delete_Stored(int row) {
		try {
			if(DEBUG) System.out.println("PeerAddresses:deleteStored:orig: #"+this.getStoreAddrLen());
			peer.removeAddress(row, me);
			if(DEBUG) System.out.println("PeerAddresses:deleteStored:late: #"+this.getStoreAddrLen());
			if(me) {
				peer.component_basic_data.creation_date = Util.CalendargetInstance();
				peer.signMe();
				peer.storeVerified();
			}else{
				peer.storeVerified();
			}
			
			this.fireTableDataChanged();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public void setPeer(D_PeerAddress peer2) {
		if (peer2 != null)
			this.global_peer_ID = peer2.getGID();
		try {
			this.init();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}	
}

class Refresher implements Runnable{
	private PeerAddressesModel model;
	public Refresher(PeerAddressesModel _model){
		model = _model;
	}
	public void run(){
		model.fireTableDataChanged();
	}
}
@SuppressWarnings("serial")
class PeerAddrRowAction extends DebateDecideAction {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	PeerAddresses tree; ImageIcon icon; int command;
    public PeerAddrRowAction(PeerAddresses tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree;
        this.icon = icon;
        this.command = command;
    }
	public final JFileChooser filterUpdates = new JFileChooser();
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
    	PeerAddressesModel model = (PeerAddressesModel)tree.getModel();
    	ArrayList<ArrayList<Object>> a;
		switch(command){
		case PeerAddresses.COMMAND_MENU_REFRESH:
			new PeerAddressesUpdateThread(model);
			break;
		case PeerAddresses.COMMAND_MENU_STORE:
			model.store(row);
			break;
		case PeerAddresses.COMMAND_MENU_DELETE:
			model.delete(row);
			break;
		}
    }
}
class PeerAddressesUpdateThread extends Thread {
	PeerAddressesModel model;
	PeerAddressesUpdateThread(PeerAddressesModel _model){
		model = _model;
		start();
	}
	public void run(){
		model.__update(null, null);
	}
}
public class PeerAddresses extends JTable implements MouseListener, PeerListener{

	public static final int COMMAND_MENU_REFRESH = 0;
	private static final boolean DEBUG = false;
	public static final int COMMAND_MENU_STORE = 1;
	public static final int COMMAND_MENU_DELETE = 2;
	private JMenuItem refresh;
	private JMenuItem certify;
	private JMenuItem delete;

	public PeerAddresses() {
		super(new PeerAddressesModel());
		this.initPopupItems();
		init();
	}

	private void initPopupItems() {
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	//
    	refresh = new JMenuItem(new PeerAddrRowAction(this, _("Refresh!"), reseticon,_("Refresh."),
    			_("Refresh!"),KeyEvent.VK_R, PeerAddresses.COMMAND_MENU_REFRESH));
    	//
    	certify = new JMenuItem(new PeerAddrRowAction(this, _("Certify!"), addicon,_("Store."),
    			_("Store!"),KeyEvent.VK_S, PeerAddresses.COMMAND_MENU_STORE));
    	//
    	delete = new JMenuItem(new PeerAddrRowAction(this, _("Delete!"), delicon,_("Delete from storage."),
    			_("Delete!"),KeyEvent.VK_D, PeerAddresses.COMMAND_MENU_DELETE));
	}
	public PeerAddressesModel getModel(){
		return (PeerAddressesModel) super.getModel();
	}
	private void init() {
		getModel().setTable(this);
		addMouseListener(this);
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
	private void initColumnSizes() {
        PeerAddressesModel model = this.getModel();
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
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
    	jtableMouseReleased(arg0, false);
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
    	jtableMouseReleased(arg0, true);
	}

	

	JMenuItem setRow(JMenuItem item, Integer row){
		item.getAction().putValue("row", row);
    	return item;
	}
	JPopupMenu getPopup(int model_row, int col){
    	
    	JPopupMenu popup = new JPopupMenu();
    	Integer _model_row = new Integer(model_row);

    	//
    	popup.add(setRow(this.refresh,_model_row));
    	if(_model_row >= getModel().getStoreAddrLen())
    		popup.add(setRow(this.certify,_model_row));
    	if(_model_row < getModel().getStoreAddrLen())
    		popup.add(setRow(this.delete,_model_row));

    	return popup;
	}
	
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
//    			if(getModel().updates_requested){
//    				getModel().updates_requested = false;
//    				getModel().__update(null,null);
//    			}	
				getModel().__update(null,null);
    			return;
    		}else{
    			return;
    		}
    	}
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup != null)
    		popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
       // dispatchToListeners(model_row);
    }

	@Override
	public void update_peer(D_PeerAddress peer, String my_peer_name,
			boolean me, boolean selected) {
		if (!selected) return;
		this.getModel().setPeer(peer);
	}
	
}