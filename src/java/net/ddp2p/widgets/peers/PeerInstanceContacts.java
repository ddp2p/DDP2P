package net.ddp2p.widgets.peers;

import static net.ddp2p.common.util.Util.__;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.Address_SocketResolved_TCP;
import net.ddp2p.common.hds.Client2;
import net.ddp2p.common.hds.Connection_Instance;
import net.ddp2p.common.hds.Connection_Peer;
import net.ddp2p.common.hds.Connections;
import net.ddp2p.common.hds.Connections_Peer_Directory;
import net.ddp2p.common.hds.Connections_Peer_Socket;
import net.ddp2p.common.util.DDP2P_ServiceRunnable;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.DebateDecideAction;

class D_PIC_Node implements TreeNode{
	private static final boolean DEBUG = false;
	public String text;
	public ArrayList<D_PIC_Node> child = new ArrayList<D_PIC_Node>();
	public D_PIC_Node parent = null;
	public String toString() {
		if (DEBUG) System.out.println("PeerIContacts: toString: "+text);
		return text;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		if(DEBUG) System.out.println("PeerIContacts: getChildAt: "+this+" return:"+childIndex);
		return child.get(childIndex);
	}

	@Override
	public int getChildCount() {
		if (DEBUG) System.out.println("PeerIContacts: getChildCount: "+this+" return:"+child.size());
		return child.size();
	}

	@Override
	public TreeNode getParent() {
		if (DEBUG) System.out.println("PeerIContacts: getParent: "+this+" return:"+parent);
		return parent;
	}

	@Override
	public int getIndex(TreeNode node) {
		if (DEBUG) System.out.println("PeerIContacts: getIndex: "+this+" node="+node+" return:"+child.indexOf(node));
		return child.indexOf(node);
	}

	@Override
	public boolean getAllowsChildren() {
		if (DEBUG) System.out.println("PeerIContacts: getAllowsChildren: "+this+" true="+true);
		return true;//child.size()!=0;
	}

	@Override
	public boolean isLeaf() {
		if (DEBUG) System.out.println("PeerIContacts: isLeaf: "+this+" true="+true);
		return child.size()==0;
	}

	@Override
	public Enumeration children() {
		if (DEBUG) System.out.println("PeerIContacts: children: "+this);
		return Collections.enumeration(child);
	}	
}

@SuppressWarnings("serial")
public
class PeerInstanceContacts  extends JPanel implements MouseListener {
	public static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static final String ALREADY_CONTACTED = __("Already contacted ***");
	public static JTree jt = null;
	public static JLabel l = null;
	public static JTree old_jt = null;
	public static JLabel old_l = null;
	public boolean refresh = true;
	//public boolean filter = true;
	Connections connections;
	
	String my_peer_name;
	D_Peer dpa;
	/**
	 * Not yet used ... (but set in menu).
	 * Should put all sockets of an instance in a single leaf
	 */
	boolean pack_sockets = true;
	
	public PeerInstanceContacts() {
		this.setLayout(new BorderLayout());
		old_l = new JLabel(__("Current Connections (right click for a menu to update view)"));
		old_l.setHorizontalTextPosition(SwingConstants.LEFT);
		this.add(old_l, BorderLayout.NORTH);
		this.addMouseListener(this);
		//l.setHorizontalAlignment(JLabel.LEFT);
	}
	public void setConnections(Connections c) {
		connections = c;
	}
	public void update() {
		if (connections == null) this.setConnections(Client2.g_Connections);
		if (connections == null) {
			if (_DEBUG) System.out.println("PeerIContacts: update: no connections object");
			return;
		};
		
		if (DEBUG) System.out.println("PeerIContacts: update: start");
		if (!refresh) {
			if (_DEBUG) System.out.println("PeerIContacts: update: norefresh");
			return;
		}
		
		//System.out.println("PeerInstanceConnections: "+connections._toString());
		
		Object[] data = getTree();
		D_PIC_Node root = new D_PIC_Node(); root.text = "root";
		for (Object o: data) {
			((D_PIC_Node)o).parent = root;
			root.child.add((D_PIC_Node)o);
		}
		jt = new JTree(root);
		jt.setRootVisible(false);
		jt.expandPath(new TreePath(new Object[]{root}));
		for (Object o: data)	jt.expandPath(new TreePath(new Object[]{root, o}));
		jt.addMouseListener(this);
		EventQueue.invokeLater(new DDP2P_ServiceRunnable("PeerInstanceContacts", false, false, this) {
			public void _run() {
				PeerInstanceContacts pic = (PeerInstanceContacts)ctx;
				String now = Util.getGeneralizedTime();
				if (DEBUG) System.out.println("PeerInstanceContacts: invoked later:do "+now);
				if (pic.old_jt != null) pic.remove(pic.old_jt);
				pic.old_jt = pic.jt;
				if (pic.jt != null) pic.add(pic.jt, BorderLayout.CENTER);
				if (pic.old_l != null) pic.remove(pic.old_l);
				pic.l = new JLabel(__("Latest Contacts for:")+" "+now);
				pic.l.setHorizontalTextPosition(SwingConstants.LEFT);
				pic.old_l = pic.l;
				pic.add(l, BorderLayout.NORTH);
				pic.revalidate();
				if (DEBUG) System.out.println("PeerIContacts: invoked later:did");
			}}
		);		
	}
	private Object[] getTree() {
		ArrayList<Object> result = new ArrayList<Object>();
		for (Connection_Peer peer : connections.used_peers_AL_CP) {
			if (DEBUG) System.out.println("PeerIContacts: getTree: peer="+peer);
			D_PIC_Node n = new D_PIC_Node();
			result.add(n);
			n.text = "\""+peer.getName()+"\" Contacted="+peer.isContactedSinceStart()+" (ok="+peer.isLastContactSuccessful()+")";
			
			if (peer.getSharedPeerDirectories().size() > 0) {
				D_PIC_Node dirs = getAddressesDirNodes(peer.getSharedPeerDirectories());
				dirs.parent = n;
				n.child.add(dirs);
			}
			if (peer.shared_peer_sockets.size() > 0) {
				D_PIC_Node socks = getAddressesSockNodes(peer.shared_peer_sockets);
				socks.parent = n;
				n.child.add(socks);
			}
	
			for (Connection_Instance ci : peer.instances_AL) {
				if (DEBUG) System.out.println("PeerIContacts: getTree: peer instance="+ci);
				D_PIC_Node  da = getInstance(ci);
				da.parent=n;
				n.child.add(da);
			}
		}
		return result.toArray();
	}
	private D_PIC_Node getInstance(Connection_Instance i) {
		D_PIC_Node n = new D_PIC_Node();
		n.text = "INST=\""+i.dpi.peer_instance+"\" Contacted="+i.isContactedSinceStart_TCP()+" (ok="+i.isLastContactSuccessful_TCP()+")";
		if (i.peer_directories.size() > 0) {
			D_PIC_Node dirs = getAddressesDirNodes(i.peer_directories);
			dirs.parent = n;
			n.child.add(dirs);
		}
		if (i.peer_sockets.size() > 0) {
			D_PIC_Node socks = getAddressesSockNodes(i.peer_sockets);
			socks.parent = n;
			n.child.add(socks);
		}
		if (i.peer_sockets_transient.size() > 0) {
			D_PIC_Node socks = getAddressesSockNodes(i.peer_sockets_transient);
			socks.parent = n;
			n.child.add(socks);
		}
		return n;
	}
	private D_PIC_Node getAddressesDirNodes(ArrayList<Connections_Peer_Directory> addr) {
		D_PIC_Node n = new D_PIC_Node();
		n.text = Address.DIR;
		for (Connections_Peer_Directory ad: addr) {
			D_PIC_Node a = new D_PIC_Node();
			a.parent = n;
			if (ad == null) Util.printCallPath("Null peer directory here!");
			String rep = ad.getReportedAddresses();
			a.text =
					ad.supernode_addr.getAddress()+"/"+ad.supernode_addr.isa_tcp+" (#"+ad.address_LID+") rep=" + rep +
					"contact="+ ad._last_contact_TCP+" tried="+ad.contacted_since_start_TCP+"/ok="+ad.last_contact_successful_TCP
					;
			n.child.add(a);
		}
		return n;
	}	
	private D_PIC_Node getAddressesSockNodes(ArrayList<Connections_Peer_Socket> addr) {
		D_PIC_Node n = new D_PIC_Node();
		n.text = Address.SOCKET;
		for (Connections_Peer_Socket as: addr) {
			D_PIC_Node a = new D_PIC_Node();
			a.parent = n;
			a.text = ((as.addr != null)?(as.addr.addr+" (#"+as.address_LID+") IP="+as.addr.ia):"No addresss ")+
					" date="+as._last_contact_date_TCP+
					" TCP="+as.contacted_since_start_TCP+"(ok="+as.last_contact_successful_TCP+" "+
					"open="+as.tcp_connection_open+"/busy="+as.tcp_connection_open_busy+")"+
					" UDP="+as.replied_since_start_UDP+"(pend="+as.last_contact_pending_UDP+")"
					+" NAT="+as.behind_NAT
					;
			n.child.add(a);
		}
		return n;
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
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);		
	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	JPopupMenu popup = getPopup(evt);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	private JPopupMenu getPopup(MouseEvent evt) {
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	PeerInstanceContactsAction prAction;
    	//
    	prAction = new PeerInstanceContactsAction(this, __("Refresh!"), reseticon,__("Let it refresh."),
    			__("Go refresh!"),KeyEvent.VK_R, PeerInstanceContactsAction.REFRESH);
    	//prAction.putValue("row", new Integer(model_row));
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerInstanceContactsAction(this, __("No Refresh!"), reseticon,__("Stop refresh."),
    			__("No refresh!"), KeyEvent.VK_S, PeerInstanceContactsAction.NO_REFRESH);
    	popup.add(new JMenuItem(prAction));


    	prAction = new PeerInstanceContactsAction(this, __("Reset!"), reseticon,__("Reset."),
    			__("Reset!"), KeyEvent.VK_D, PeerInstanceContactsAction.RESET);
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerInstanceContactsAction(this, __("Pack Socket entries!"), reseticon,__("Pack Socket entries."),
    			__("Pack Socket!"), KeyEvent.VK_P, PeerInstanceContactsAction.PACK_SOCKET);
    	popup.add(new JMenuItem(prAction));

    	return popup;
	}
	public void connectWidget() {
		// TODO Auto-generated method stub
		
	}
	public void disconnectWidget() {
		// TODO Auto-generated method stub
		
	}
	public Component getComboPanel() {
		return this;
	}
}

@SuppressWarnings("serial")
class PeerInstanceContactsAction extends DebateDecideAction {
    public static final int REFRESH = 0;
    public static final int NO_REFRESH = 1;
	public static final int RESET = 2;
	public static final int UNFILTER = 3;
	public static final int FILTER = 4;
	public static final int PACK_SOCKET = 5;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	PeerInstanceContacts tree; ImageIcon icon; int command;
    public PeerInstanceContactsAction(PeerInstanceContacts tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree;
        this.icon = icon;
        this.command = command;
    }
	//public final JFileChooser filterUpdates = new JFileChooser();
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
        if (DEBUG) System.err.println("PeerInstanceConnectionsRowAction:command property: " + command);
    	JMenuItem mnu;
		switch(command){
		case REFRESH:
			tree.refresh = true;
			tree.update();
			break;
		case NO_REFRESH:
			tree.refresh = false;
			break;
		case RESET:
			tree.update();
			break;
		case PACK_SOCKET:
			tree.pack_sockets = !tree.pack_sockets;
			tree.update();
			break;
		}
    }
}
    