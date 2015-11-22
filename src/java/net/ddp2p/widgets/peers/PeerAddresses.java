package net.ddp2p.widgets.peers;
import static net.ddp2p.common.util.Util.__;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.PeerListener;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.DebateDecideAction;
@SuppressWarnings("serial")
class  PeerAddressesModel  extends AbstractTableModel implements TableModel, DBListener {
	private static final Object peers_monitor = new Object();
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private PeerAddresses table;
	D_Peer peer;
	String global_peer_ID;
	boolean me;
	private ArrayList<ArrayList<Object>> __peers;
	String columnNames[]={__("Instance"),__("Type"),__("Domain"),__("UDP"),__("TCP"),__("Certified"),__("Priority"),__("Transient"),__("Loopback")};
	protected static String[] columnToolTips = {
		__("A name for the instance"),
		__("Type of the address"),
		__("IP of the target"),
		__("UDP port"),
		__("TCP port"),
		__("Is this address certified?"),
		__("The Priority Order of the Address"),
		__("Is this address stored in the databased or just detected (transient)"),
		__("Is this a Loopback address")
	};
	final int TABLE_COL_INSTANCE = 0;
	final int TABLE_COL_TYPE = 1;
	final int TABLE_COL_DOMAIN = 2;
	final int TABLE_COL_UDP = 3;
	final int TABLE_COL_TCP = 4;
	final int TABLE_COL_CERTIFIED = 5;
	final int TABLE_COL_PRIORITY = 6;
	final int TABLE_COL_TRANSIENT = 7;
	final int TABLE_COL_LOOPBACK = 8;
	public boolean for_Myself;
	PeerAddressesModel() {
		this.global_peer_ID = Identity.getMyPeerGID();
		try {
			init();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public PeerAddressesModel(boolean myself) {
		for_Myself = myself;
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
		if (DEBUG) System.out.println("PeerAddress: init: " +"\n GID="+global_peer_ID+"\n me="+me+"\n peer="+peer);
		synchronized (this.peers_monitor) {
			if (global_peer_ID == null) {
				peer = null;
				me = false;
				if (DEBUG) System.out.println("PeerAddress: init: quit null");
				return;
			}
			if (global_peer_ID.equals(Identity.getMyPeerGID())) {
				me = true;
			} else {
				me = false;
			}
			if ((peer != null) && (global_peer_ID.equals(peer.getGID()))) return;
			if (me) {
				peer = HandlingMyself_Peer.get_myself_with_wait();
			} else {
				peer = D_Peer.getPeerByGID_or_GIDhash(global_peer_ID, null, true, false, false, null);
			}
		}
		if (DEBUG) System.out.println("PeerAddress: init: done peer="+peer);
		SwingUtilities.invokeLater(new RefresherSwingThread(this));
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
	@Override
	public boolean isCellEditable(int row, int col) {
		switch (col) {
		case TABLE_COL_CERTIFIED:
		case TABLE_COL_PRIORITY:
			return true;
		default:
			return false;
		}
	}
	int getStoreAddrLen() {
		int adr = 0;
		if ((peer != null) && (peer.shared_addresses != null))
			adr = peer.shared_addresses.size();	
		if ((peer != null) && (peer._instances != null))
			for ( D_PeerInstance i : peer._instances.values()) {
				if (!((i == null) || (i.addresses == null)))
					adr += i.addresses.size();	
			}
		return adr;
	}
	int getDirectoriesLen() {
		if (Identity.getListing_directories_string() == null) return 0;
		return Identity.getListing_directories_string().size();
	}
	int getSocketsLen() {
		if (Application.getMy_Server_Domains() == null) return 0;
		return Application.getMy_Server_Domains().size();
	}
	int getLoopbackLen() {
		if (Application.getMy_Server_Domains_Loopback() == null) return 0;
		return Application.getMy_Server_Domains_Loopback().size();
	}
	@Override
	public int getRowCount() {
		synchronized(this.peers_monitor){
			if(!me) {
				return this.getStoreAddrLen();
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
	@Override
	public void setValueAt(Object value, int row, int col) {
		if (col == TABLE_COL_CERTIFIED) {
			this.store_and_certify(row, (Boolean)value);
		}
		int crt;
		crt = this.getStoreAddrLen();
		if (row < crt) { setValueAt_Stored(value, row, col); return; }
		row -= crt;
	}
	private Object getValueAt_Loopback(int row, int col) {
		switch (col) {
		case TABLE_COL_INSTANCE:
			return Identity.getMyPeerInstance();
		case TABLE_COL_TYPE:
			return Address.SOCKET;
		case TABLE_COL_DOMAIN:
			return Application.getMy_Server_Domains_Loopback().get(row).getHostAddress();
		case TABLE_COL_UDP:
			return new Integer(Application.getPeerUDPPort());
		case TABLE_COL_TCP:
			return new Integer(Application.getPeerTCPPort());
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
		switch(col) {
		case TABLE_COL_INSTANCE:
			return Identity.getMyPeerInstance();
		case TABLE_COL_TYPE:
			return Address.SOCKET;
		case TABLE_COL_DOMAIN:
			return Application.getMy_Server_Domains().get(row).getHostAddress();
		case TABLE_COL_UDP:
			return new Integer(Application.getPeerUDPPort());
		case TABLE_COL_TCP:
			return new Integer(Application.getPeerTCPPort());
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
		switch (col) {
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
	private Address getAddress_Dirs(int row) {
		if (_DEBUG) {
			System.out.println("PeerAddresses: getAddress_Dirs: row="+row+" /"+Identity.getListing_directories_addr().size());
			for (Address a : Identity.getListing_directories_addr()) {
				System.out.println("PeerAddresses: getAddress_Dirs: ad="+a.toLongString());
			}
		}
		Address a = Identity.getListing_directories_addr().get(row);
		a.setDirType();
		return a;
	}
	private Object getValueAt_Dirs(int row, int col) {
		switch (col) {
		case TABLE_COL_INSTANCE:
			return Identity.getMyPeerInstance();
		case TABLE_COL_TYPE:
			return Address.DIR;
		case TABLE_COL_DOMAIN:
			return new Address(Identity.getListing_directories_string().get(row)).domain;
		case TABLE_COL_UDP:
			return new Integer(new Address(Identity.getListing_directories_string().get(row)).udp_port);
		case TABLE_COL_TCP:
			return new Integer(new Address(Identity.getListing_directories_string().get(row)).tcp_port);
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
	public Address getAddress(int row) {
		int crt = peer.shared_addresses.size();
		if (row < crt) return peer.shared_addresses.get(row);
		for (D_PeerInstance i : peer._instances.values()) {
			int old_crt = crt;
			crt += i.addresses.size();
			if (row < crt) return i.addresses.get(row-old_crt);
		}
		return null;
	}
	public D_PeerInstance getAddressInstance(int row) {
		int crt = peer.shared_addresses.size();
		if (row < crt) return null; 
		for (D_PeerInstance i : peer._instances.values()) {
			int old_crt = crt;
			crt += i.addresses.size();
			if (row < crt) return i;
		}
		return null;
	}
	/**
	 * 
	 * @param row
	 * @param me (is this about me?)
	 * @return
	 */
	public boolean removeAddress(int row, boolean me) {
		ArrayList<Address> list = null;
		Address obj = null;
		D_PeerInstance inst = null;
		int crt = peer.shared_addresses.size();
		if ((row < crt) && (row >= 0)) {
			obj = peer.shared_addresses.get(row);
			list = peer.shared_addresses;
			peer.dirty_main = true;
		}
		if (obj == null) {
			for (D_PeerInstance i : peer._instances.values()) {
				inst = i;
				int old_crt = crt;
				crt += i.addresses.size();
				if (row < crt) {
					obj = (list = i.addresses).get(row-old_crt);
					break;
				}
			}
		}
		if ((list == null) ||
				(!me && obj.certified)) return false;
		list.remove(obj);
		peer.dirty_addresses = true;
		if (inst != null) {
			inst.dirty = true;
			peer.dirty_instances = true;
		}
		return true;
	}
	private void setValueAt_Stored(Object value, int row, int col) {
		if (col == TABLE_COL_PRIORITY) {
			Address _address = null;
			int priority = Integer.parseInt("" + value);
			D_PeerInstance _dpi = getAddressInstance(row);
			if (_dpi == null) {if (D_Peer.existsCertifiedAddressPriority(peer.shared_addresses, priority)) return;}
			else if (D_Peer.existsCertifiedAddressPriority(_dpi.addresses, priority)) return;
			peer = D_Peer.getPeerByPeer_Keep(peer);
			if (peer == null) {
				if (_DEBUG) System.out.println("PeerAddress: setValueAt_Stored: null peer");
				return;
			}
			_dpi = getAddressInstance(row);
			if (_dpi == null) {if (D_Peer.existsCertifiedAddressPriority(peer.shared_addresses, priority)) return;}
			else if (D_Peer.existsCertifiedAddressPriority(_dpi.addresses, priority)) return;
			_address = getAddress(row);
			try {
				_address.priority = priority;
				_address.dirty = true;
				peer.dirty_addresses = true;
				if (row < peer.shared_addresses.size()) peer.dirty_main = true;
				else {
					D_PeerInstance dpi = getAddressInstance(row);
					if (dpi != null ) {
						peer.dirty_instances = true;
						dpi.dirty = true;
					}
				}
				peer.storeRequest();
			} catch (Exception e) {e.printStackTrace();}
			peer.releaseReference();
		}
	}
	private Object getValueAt_Stored(int row, int col) {
		Address _address= null;
		Address __address = null;
		_address = getAddress(row);
		if (_address == null) return null;
		if ((_address.domain == null) && (_address.address != null)) __address = new Address(_address.address);
		switch(col) {
		case TABLE_COL_INSTANCE:
			return _address.instance;
		case TABLE_COL_TYPE:
			return _address.pure_protocol;
		case TABLE_COL_DOMAIN:
			if (__address != null) {
				if (_address.domain != null) return _address.domain;
				else return "["+__address.domain+"]";
			} else return _address.domain;
		case TABLE_COL_UDP:
			if (__address != null) return new Integer(__address.udp_port);
			else return new Integer(_address.udp_port);
		case TABLE_COL_TCP:
			if (__address != null) return new Integer(__address.tcp_port);
			return new Integer(_address.tcp_port);
		case TABLE_COL_CERTIFIED:
			return new Boolean(_address.certified);
		case TABLE_COL_PRIORITY:
			return new Integer(_address.priority);
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
	public boolean store_and_certify(int row, Boolean certify) {
		int crt;
		if (DEBUG) System.out.println("PeerAddress: store: start row="+row);
		crt = this.getStoreAddrLen();
		if (row < crt) return store_Stored(row, certify);
		row -= crt;
		crt = this.getDirectoriesLen();
		if (row < crt) return store_Dirs(row, certify);
		row -= crt;
		crt = this.getSocketsLen();
		if (row < crt) return store_Socks(row, certify);
		row -= crt;
		crt = this.getLoopbackLen();
		if (row < crt) return store_Loopback(row, certify);
		row -= crt;
		if (DEBUG) System.out.println("PeerAddress: store: exit false row="+row);
		return false;
	}
	private boolean store_Stored(int row, Boolean certify) {
		return false;
	}
	private boolean store_Dirs(int row, Boolean value) {
		if (DEBUG) System.out.println("PeerAddress: store_Dirs: start");
		Address crt = getAddress_Dirs(row);
		if (value == null) crt.certified = !crt.certified;
		else crt.certified = value;
		if (DEBUG) System.out.println("PeerAddress: store_Dirs: new adr="+crt);
		Object[] options = new Object[]{__("Global"), __("Instance"), __("Cancel")};
		int ok = Application_GUI.ask(__("Global?"), __("Do you want to add this globally or to instance:")+
				" \""+Identity.getMyPeerInstance()+"\"\n"+crt.toLongNiceString(),
				options, options[0], null);
		if ((ok < 0) || (ok > 1)) return false;
		peer.addAddress (crt, ok == 0, peer.getInstance());
		this.fireTableDataChanged();
		if (DEBUG) System.out.println("PeerAddress: store_Socks: done: peer="+peer);
		return true;
	}
	private boolean store_Socks(int row, Boolean value) {
		if (DEBUG) System.out.println("PeerAddress: store_Socks: start");
		Calendar _arrival_date = Util.CalendargetInstance();
		String arrival_date = Encoder.getGeneralizedTime(_arrival_date);
		String type = Address.SOCKET;
		boolean certified = true;
		if (value != null) certified = value;
		Address crt = new Address(
				(String)getValueAt_Socks(row,TABLE_COL_DOMAIN),
				((Integer)getValueAt_Socks(row,TABLE_COL_TCP)).intValue(),
				((Integer)getValueAt_Socks(row,TABLE_COL_UDP)).intValue());
		String _address = crt.toString();
		D_PeerInstance dpi = peer.getPeerInstance(peer.getInstance());
		if (dpi == null) {
			peer.putPeerInstance_setDirty(peer.getInstance(), dpi = new D_PeerInstance());
			dpi.peer_instance = peer.getInstance();
			dpi.setLID(peer.getLIDstr_keep_force(), peer.getLID_keep_force());
		}
		dpi.dirty = true;
		dpi.createdLocally = true;
		dpi.branch = DD.BRANCH;
		dpi.agent_version = DD.VERSION;
		dpi.setCreationDate();
		peer.dirty_instances = true;
		if (DEBUG) System.out.println("PeerAddress: store_Socks: store for instanceID: "+dpi);
		try {
			peer.addAddress(_address, type, _arrival_date, arrival_date, 
					certified, D_Peer.getMaxCertifiedAddressPriority(dpi.addresses)+1, true,
					dpi);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		dpi.sign(peer.getSK());
		if (peer.dirty_main) peer.sign();
		if (peer.dirty_any()) peer.storeRequest();
		this.fireTableDataChanged();
		if (DEBUG) System.out.println("PeerAddress: store_Socks: done: peer="+peer);
		return true;
	}
	private boolean store_Loopback(int row, Boolean value) {
		Calendar _arrival_date = Util.CalendargetInstance();
		String arrival_date = Encoder.getGeneralizedTime(_arrival_date);
		String type = Address.SOCKET;
		boolean certified = true;
		if (value != null) certified = value;
		Address crt = new Address(
				(String)getValueAt_Loopback(row,TABLE_COL_DOMAIN),
				((Integer)getValueAt_Loopback(row,TABLE_COL_TCP)).intValue(),
				((Integer)getValueAt_Loopback(row,TABLE_COL_UDP)).intValue());
		String _address = crt.toString();
		D_PeerInstance dpi = peer.getPeerInstance(peer.getInstance());
		if (dpi == null) {
			peer.putPeerInstance_setDirty(peer.getInstance(), dpi = new D_PeerInstance());
			dpi.peer_instance = peer.getInstance();
			dpi.setLID(peer.getLIDstr_keep_force(), peer.getLID_keep_force());
		}
		dpi.dirty = true;
		dpi.createdLocally = true;
		dpi.branch = DD.BRANCH;
		dpi.agent_version = DD.VERSION;
		dpi.setCreationDate();
		peer.dirty_instances = true;
		try {
			peer.addAddress(_address, type, _arrival_date, arrival_date, 
					certified, D_Peer.getMaxCertifiedAddressPriority(dpi.addresses)+1, true,
					dpi);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		dpi.sign(peer.getSK());
		if (peer.dirty_main) peer.sign();
		if (peer.dirty_any()) peer.storeRequest();
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
		return false;
	}
	private boolean delete_Dirs(int row) {
		return false;
	}
	private boolean delete_Stored(int row) {
		try {
			if (DEBUG) System.out.println("PeerAddresses:deleteStored:orig: #"+this.getStoreAddrLen());
			if (DEBUG) System.out.println("PeerAddresses:deleteStored:orig: #"+peer);
			removeAddress(row, me);
			if (DEBUG) System.out.println("PeerAddresses:deleteStored:late: #"+this.getStoreAddrLen());
			if (DEBUG) System.out.println("PeerAddresses:deleteStored:late: #"+peer);
			if (me) {
				if (peer.dirty_main) {
					peer.setCreationDate();
					peer.signMe();
				}
				if (DEBUG) System.out.println("PeerAddresses:deleteStored:last: #"+peer);
				peer.storeRequest();
			} else {
				peer.storeRequest();
			}
			this.fireTableDataChanged();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public void setPeer(D_Peer peer2) {
		if (peer2 != null)
			this.global_peer_ID = peer2.getGID();
		try {
			this.init();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}	
}
class RefresherSwingThread implements Runnable{
	private PeerAddressesModel model;
	public RefresherSwingThread(PeerAddressesModel _model){
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
		case PeerAddresses.COMMAND_MENU_STORE_CERTIFY:
			model.store_and_certify(row, Boolean.TRUE);
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
	public void run() {
		try {
			DD.load_listing_directories();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		model.__update(null, null);
	}
}
@SuppressWarnings("serial")
public class PeerAddresses extends JTable implements MouseListener, PeerListener{
	private static final boolean DEBUG = false;
	public static final int COMMAND_MENU_REFRESH = 0;
	public static final int COMMAND_MENU_STORE_CERTIFY = 1;
	public static final int COMMAND_MENU_DELETE = 2;
	private JMenuItem refresh;
	private JMenuItem certify;
	private JMenuItem delete;
	private boolean for_Myself;
	public PeerAddresses(boolean myself) {
		super(new PeerAddressesModel(myself));
		for_Myself = myself;
		this.initPopupItems();
		init();
	}
	public PeerAddresses() {
		super(new PeerAddressesModel());
		this.initPopupItems();
		init();
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
	private void initPopupItems() {
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	refresh = new JMenuItem(new PeerAddrRowAction(this, __("Refresh!"), reseticon,__("Refresh."),
    			__("Refresh!"),KeyEvent.VK_R, PeerAddresses.COMMAND_MENU_REFRESH));
    	certify = new JMenuItem(new PeerAddrRowAction(this, __("Certify!"), addicon,__("Store/Certify."),
    			__("Store/Certify!"),KeyEvent.VK_S, PeerAddresses.COMMAND_MENU_STORE_CERTIFY));
    	delete = new JMenuItem(new PeerAddrRowAction(this, __("Delete!"), delicon,__("Delete from storage."),
    			__("Delete!"),KeyEvent.VK_D, PeerAddresses.COMMAND_MENU_DELETE));
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
        __("Click to sort; Shift-Click to sort in reverse order"));
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
	JMenuItem setRow(JMenuItem item, Integer row){
		item.getAction().putValue("row", row);
    	return item;
	}
	JPopupMenu getPopup(int model_row, int col){
    	JPopupMenu popup = new JPopupMenu();
    	Integer _model_row = new Integer(model_row);
    	popup.add(setRow(this.refresh,_model_row));
    	if(_model_row >= getModel().getStoreAddrLen())
    		popup.add(setRow(this.certify,_model_row));
    	if(_model_row < getModel().getStoreAddrLen())
    		popup.add(setRow(this.delete,_model_row));
    	return popup;
	}
	private void jtableMouseReleased(java.awt.event.MouseEvent evt, boolean release) {
    	int row; 
    	int model_row=-1; 
    	int col; 
    	Point point = evt.getPoint();
        row=this.rowAtPoint(point);
        if (DEBUG) System.out.println("Peers:jTableMouseRelease: row="+row);
        col=this.columnAtPoint(point);
        this.getSelectionModel().setSelectionInterval(row, row);
        if (row>=0)
        	model_row=this.convertRowIndexToModel(row);
    	if(!evt.isPopupTrigger()){
    		if(release){
				getModel().__update(null,null);
    			return;
    		}else{
    			return;
    		}
    	}
    	JPopupMenu popup = getPopup(model_row,col);
    	if(popup != null)
    		popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	@Override
	public void update_peer(D_Peer peer, String my_peer_name,
			boolean me, boolean selected) {
		if (!selected && !for_Myself) return;
		if (!me && for_Myself) return;
		this.getModel().setPeer(peer);
	}
}
