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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.config.PeerListener;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.GUIStatusHistory;
import net.ddp2p.common.streaming.GlobalClaimedDataHashes;
import net.ddp2p.common.streaming.OrgPeerDataHashes;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DDP2P_ServiceRunnable;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.components.DebateDecideAction;

class D_OR_Node implements TreeNode {
	private static final boolean DEBUG = false;
	public D_OR_Node parent = null;
	public int level;
	public int position;
	public String text;
	
	public Hashtable<String, Hashtable<Long, String>> type;
	public Hashtable<String, Hashtable<Long, Object>> type_dated;

	private Hashtable<Long, String> peer_GID_set;
	private Hashtable<Long, Object> peer_GID_dated_set;

	public ArrayList<D_OR_Node> child = new ArrayList<D_OR_Node>();
	public boolean children_built = false;
	

	private void build_children() {
		switch (level) {
		case OrganizationRequests.LEVEL_ROOT: // should have been done in constructor
			break;
		case OrganizationRequests.LEVEL_TYPE:
		{
			//if (position == 1) break; // ORG not supported
			if (type_dated != null) {
				for (String s : type_dated.keySet()) {
					D_OR_Node node = new D_OR_Node();
					node.text = s;
					if (position == OrganizationRequests.TYPE_CONST_5) {
						D_Constituent c = D_Constituent.getConstByGID_or_GIDH(s, D_Constituent.getGIDHashFromGID(s), true, false, -1l);
						if (c != null) {
							String cn = c.getNameOrMy();
							if (cn != null)
								node.text = "name=\""+cn+"\" " + node.text;
							else
								node.text = "TMP"+c.getLID()+": GID=" + node.text;
						} else {
							node.text += " \"UNKNOWN\"";
						}
					}
					node.peer_GID_dated_set = type_dated.get(s);
					node.level = OrganizationRequests.LEVEL_GID;
					child.add(node);
				}
			}
			if (type != null) {
				for (String s : type.keySet()) {
					D_OR_Node node = new D_OR_Node();
					node.text = s;
					node.peer_GID_set = type.get(s);
					node.level = OrganizationRequests.LEVEL_GID;
					child.add(node);
				}
			}
		}
			break;
		case OrganizationRequests.LEVEL_GID:
		{
			if (peer_GID_dated_set != null) {
				for (Long s : peer_GID_dated_set.keySet()) {
					D_OR_Node node = new D_OR_Node();
					D_Peer peer = D_Peer.getPeerByLID(s, true, false);
					node.text = "(LID="+s+") \"" + peer.getName_MyOrDefault()+ "\" : \"" + peer_GID_dated_set.get(s)+"\"";
					node.level = OrganizationRequests.LEVEL_PEER;
					child.add(node);
				}
			}
			if (peer_GID_set != null) {
				for (Long s : peer_GID_set.keySet()) {
					D_OR_Node node = new D_OR_Node();
					D_Peer peer = D_Peer.getPeerByLID(s, true, false);
					node.text = "(LID="+s+") \"" + peer.getName_MyOrDefault()+ "\" : \"" + peer_GID_set.get(s)+"\"";
					node.level = OrganizationRequests.LEVEL_PEER;
					child.add(node);
				}
			}
		}
			break;
		case OrganizationRequests.LEVEL_PEER:
			break;
		}
		
		children_built = true;
	}

	public String toString() {
		if (DEBUG) System.out.println("D_OR_Node: toString: " + text);
		return text + " #"+getChildCount();
	}

	@Override
	public TreeNode getChildAt(int childIndex) {
		if (! children_built) build_children();
		if (childIndex > getChildCount()) {
			if (DEBUG) System.out.println("D_OR_Node: getChildAt: recursive: " + text+" "+childIndex);
			return this;
		}
		return child.get(childIndex);
	}

	@Override
	public int getChildCount() {
		if (! children_built) build_children();
		return child.size();
	}

	@Override
	public TreeNode getParent() {
		return parent;
	}

	@Override
	public int getIndex(TreeNode node) {
		return child.indexOf(node);
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public boolean isLeaf() {
		if (! children_built) build_children();
		return child.size() == 0;
	}

	@Override
	public Enumeration children() {
		return Collections.enumeration(child);
	}
}

@SuppressWarnings("serial")
public class OrganizationRequests extends JPanel implements MouseListener, OrgListener {
	public static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	static final int LEVEL_ROOT = 0;
	static final int LEVEL_TYPE = 1;
	static final int LEVEL_GID = 2;
	static final int LEVEL_PEER = 3;
	
	private static final int TYPES_NB = 12;
	private static final int TYPES_G_NB = 4;
	static final int TYPE_CONST_5 = 5;

	public static JTree jt = null;
	public static JTree old_jt = null;
	public static JLabel l = null;
	public static JLabel old_l = null;
	public boolean refresh = true;
	D_Organization organization;
	
	public OrganizationRequests () {
		this.setLayout(new BorderLayout());
		old_l = new JLabel(__("Latest Requests Advertised by this Organization."));
		old_l.setHorizontalTextPosition(SwingConstants.LEFT);
		this.add(old_l, BorderLayout.NORTH);
		
		D_OR_Node root = new D_OR_Node();
		Object[] data = new Object[0];
		old_jt = new JTree(root); this.add(old_jt, BorderLayout.CENTER);
		this.addMouseListener(this);
		this.orgUpdate(null, 0, null);
		
		MainFrame.status.addOrgStatusListener(this);
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
	}
	@Override
	public void mousePressed(MouseEvent e) {
	   	jtableMouseReleased(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
	   	jtableMouseReleased(e);
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		
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
    	ORAction prAction;
    	//
    	prAction = new ORAction(this, __("Refresh!"), reseticon,__("Let it refresh."),
    			__("Go refresh!"),KeyEvent.VK_R, ORAction.REFRESH);
    	//prAction.putValue("row", new Integer(model_row));
    	popup.add(new JMenuItem(prAction));

    	return popup;
	}

	@Override
	public void orgUpdate(String orgID, int col, D_Organization _org) {
		organization = _org;
		if (_org == null) {
			if (orgID != null) organization = D_Organization.getOrgByLID(orgID, true, false);
			else organization = null;
		}
		D_OR_Node root = new D_OR_Node();
		Object[] data = new Object[0];
		if (organization != null) {
			data = getTree(organization.getSpecificRequest(), root);
		} else {
			data = getTree(null, root);
		}
		jt = new JTree(root);
		jt.setRootVisible(false);
		jt.expandPath(new TreePath(new Object[]{root}));
		for (Object o: data)	jt.expandPath(new TreePath(new Object[]{root, o}));
		jt.addMouseListener(this);
		
		EventQueue.invokeLater(new DDP2P_ServiceRunnable("OrganizationRequests", false, false, this) {
			public void _run() {
				OrganizationRequests pic = (OrganizationRequests) ctx;
				String now = Util.getGeneralizedTime();
				if (DEBUG) System.out.println("OrganizationRequests: invoked later:do "+now);
				if (pic.old_jt != null) pic.remove(pic.old_jt);
				pic.old_jt = pic.jt;
				if (pic.jt != null) pic.add(pic.jt, BorderLayout.CENTER);
				if (pic.old_l != null) pic.remove(pic.old_l);
				String oName = "None";
				if (organization != null) oName = organization.getOrgNameOrMy();
				pic.l = new JLabel(__("Advertisements for org:")+" "+oName+" " + now);
				pic.l.setHorizontalTextPosition(SwingConstants.LEFT);
				pic.old_l = pic.l;
				pic.add(l, BorderLayout.NORTH);
				pic.revalidate();
				if (DEBUG) System.out.println("PeerIContacts: invoked later:did");
			}}
		);		
	}

	private Object[] getTree(OrgPeerDataHashes specificRequest, D_OR_Node root) {
		GlobalClaimedDataHashes _opdh = GlobalClaimedDataHashes.get();
		
		if (specificRequest == null) {
			Object[] result = new Object[TYPES_G_NB];
			for (int i = 0; i < TYPES_G_NB; i ++) {
				D_OR_Node elem;
				elem = new D_OR_Node(); elem.level = LEVEL_TYPE; elem.position = i; elem.parent = root; root.child.add(elem);
				result[i] = elem;
			}
			if (_opdh != null) {
				((D_OR_Node) result[0]).type = _opdh.peers;
				((D_OR_Node) result[1]).type = _opdh.orgs;
				((D_OR_Node) result[2]).type = _opdh.news;
				((D_OR_Node) result[3]).type = _opdh.tran;
			}			
			((D_OR_Node) result[0]).text = "PEERS";
			((D_OR_Node) result[1]).text = "ORGANIZATIONS";
			((D_OR_Node) result[2]).text = "NEWS GLOBAL";
			((D_OR_Node) result[3]).text = "TRANSLATIONS GLOBAL";
			root.children_built = true;
			return result;
		}
		Object[] result = new Object[TYPES_NB];
		for (int i = 0; i < TYPES_NB; i ++) {
			D_OR_Node elem;
			elem = new D_OR_Node(); elem.level = LEVEL_TYPE; elem.position = i; elem.parent = root; root.child.add(elem);
			result[i] = elem;
		}
		if (_opdh != null) {
			((D_OR_Node) result[0]).type = _opdh.peers;
			((D_OR_Node) result[1]).type = _opdh.orgs;
			((D_OR_Node) result[2]).type = _opdh.news;
			((D_OR_Node) result[3]).type = _opdh.tran;
		}
		((D_OR_Node) result[4]).type = specificRequest.neig;
		((D_OR_Node) result[TYPE_CONST_5]).type_dated = specificRequest.cons;
		((D_OR_Node) result[6]).type = specificRequest.witn;
		((D_OR_Node) result[7]).type = specificRequest.moti;
		((D_OR_Node) result[8]).type = specificRequest.just;
		((D_OR_Node) result[9]).type = specificRequest.sign;
		((D_OR_Node) result[10]).type = specificRequest.news;
		((D_OR_Node) result[11]).type = specificRequest.tran;
		
		((D_OR_Node) result[0]).text = "PEERS";
		((D_OR_Node) result[1]).text = "ORGANIZATIONS";
		((D_OR_Node) result[2]).text = "NEWS GLOBAL";
		((D_OR_Node) result[3]).text = "TRANSLATIONS GLOBAL";
		((D_OR_Node) result[4]).text = "Neighborhoods";
		((D_OR_Node) result[TYPE_CONST_5]).text = "Constituents";
		((D_OR_Node) result[6]).text = "Witnesses";
		((D_OR_Node) result[7]).text = "Motions";
		((D_OR_Node) result[8]).text = "Justifications";
		((D_OR_Node) result[9]).text = "Signatures";
		((D_OR_Node) result[10]).text = "News";
		((D_OR_Node) result[11]).text = "Translations";
		
		root.children_built = true;
		return result;
	}

	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		// TODO Auto-generated method stub
		
	}
	public static void main(String[] args) {
		String dfname = Application.DELIBERATION_FILE;
		//System.out.println("1");
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		//System.out.println("2");
		net.ddp2p.widgets.components.GUI_Swing.initSwingGUI();
		//System.out.println("3");
		try {
			Application.setDB(new DBInterface(dfname));
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("4");
		createAndShowGUI(Application.getDB());
		if (args.length > 0)
			newContentPane.orgUpdate(args[0], 0, null);
	}
	static OrganizationRequests newContentPane;
	public static void createAndShowGUI(DBInterface db) {
		MainFrame.status = new GUIStatusHistory();
		JFrame frame = new JFrame("Directories Test");
		//System.out.println("5");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		newContentPane = new OrganizationRequests();
		newContentPane.setOpaque(true);
		frame.setContentPane(newContentPane);
		frame.pack();
		frame.setVisible(true);
	}
}

@SuppressWarnings("serial")
class ORAction extends DebateDecideAction {
    public static final int REFRESH = 0;
//    public static final int NO_REFRESH = 1;
//	public static final int RESET = 2;
//	public static final int UNFILTER = 3;
//	public static final int FILTER = 4;
//	public static final int PACK_SOCKET = 5;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    OrganizationRequests tree; ImageIcon icon; int command;
    public ORAction(OrganizationRequests tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int command) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree;
        this.icon = icon;
        this.command = command;
    }
    public void actionPerformed(ActionEvent e) {
    	Object src = e.getSource();
        if(DEBUG)System.err.println("PeersRowAction:command property: " + command);
    	JMenuItem mnu;
		switch (command) {
		case REFRESH:
			tree.refresh = true;
			tree.orgUpdate(null, 0, tree.organization);
			break;
//		case NO_REFRESH:
//			tree.refresh = false;
//			break;
		}
    }
}


    