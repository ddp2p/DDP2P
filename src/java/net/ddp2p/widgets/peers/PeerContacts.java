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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.PeerListener;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.components.GUI_Swing;
import static net.ddp2p.common.util.Util.__;

class DNode implements TreeNode{
	private static final boolean DEBUG = false;
	public String text;
	public ArrayList<DNode> child=new ArrayList<DNode>();
	public DNode parent = null;
	public String toString(){
		if(DEBUG) System.out.println("PeerContacts: toString: "+text);
		return text;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		if(DEBUG) System.out.println("PeerContacts: getChildAt: "+this+" return:"+childIndex);
		return child.get(childIndex);
	}

	@Override
	public int getChildCount() {
		if(DEBUG) System.out.println("PeerContacts: getChildCount: "+this+" return:"+child.size());
		return child.size();
	}

	@Override
	public TreeNode getParent() {
		if(DEBUG) System.out.println("PeerContacts: getParent: "+this+" return:"+parent);
		return parent;
	}

	@Override
	public int getIndex(TreeNode node) {
		if(DEBUG) System.out.println("PeerContacts: getIndex: "+this+" node="+node+" return:"+child.indexOf(node));
		return child.indexOf(node);
	}

	@Override
	public boolean getAllowsChildren() {
		if(DEBUG) System.out.println("PeerContacts: getAllowsChildren: "+this+" true="+true);
		return true;//child.size()!=0;
	}

	@Override
	public boolean isLeaf() {
		if(DEBUG) System.out.println("PeerContacts: isLeaf: "+this+" true="+true);
		return child.size()==0;
	}

	@Override
	public Enumeration children() {
		if(DEBUG) System.out.println("PeerContacts: children: "+this);
		return Collections.enumeration(child);
	}
	
}
public class PeerContacts extends JPanel implements MouseListener, PeerListener {
	public static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static JTree jt = null;
	public static JLabel l = null;
	public static JTree old_jt = null;
	public static JLabel old_l = null;
	public boolean refresh = true;
	public boolean filter = true;
	String my_peer_name;
	D_Peer dpa;
	boolean pack_sockets = true;
	public PeerContacts(){
		this.setLayout(new BorderLayout());
		old_l = new JLabel(__("Latest Contacts for this Peer"));
		old_l.setHorizontalTextPosition(SwingConstants.LEFT);
		this.add(old_l, BorderLayout.NORTH);
		this.addMouseListener(this);
		GUI_Swing.peer_contacts = this;
		if(Application.peers!=null) ((Peers)Application.peers).addListener(this);
		//l.setHorizontalAlignment(JLabel.LEFT);
	}

	// (GID: (instance: (DIR_Address:(ADR:date))))
	public void update(Hashtable<String, Hashtable<String, Hashtable<String, Hashtable<String, String>>>> peer_contacts) {
		if (DEBUG) System.out.println("PeerContacts: update: start");
		if (! refresh) {
			if (_DEBUG) System.out.println("PeerContacts: update: norefresh");
			return;
		}
		Object[] data = getTree(peer_contacts);
		//if(_DEBUG) System.out.println("PeerContacts: new tree");
		DNode root = new DNode(); root.text = "root";
		for (Object o: data) {
			((DNode)o).parent = root;
			root.child.add((DNode)o);
		}
		//jt = new JTree(data);
		jt = new JTree(root);
		jt.setRootVisible(false);
		jt.expandPath(new TreePath(new Object[]{root}));
		for(Object o: data)	jt.expandPath(new TreePath(new Object[]{root, o}));
		jt.addMouseListener(this);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				String now = Util.getGeneralizedTime();
				if(DEBUG) System.out.println("PeerContacts: invoked later:do "+now);
				//Application.peer.remo.removeAll();
				if(old_jt!=null) GUI_Swing.peer_contacts.remove(old_jt);
				old_jt = jt;
				if(jt!=null) GUI_Swing.peer_contacts.add(jt,BorderLayout.CENTER);
				if(old_l!=null) GUI_Swing.peer_contacts.remove(old_l);
				l = new JLabel(__("Latest Contacts for this Peer:")+" "+now);
				l.setHorizontalTextPosition(SwingConstants.LEFT);
				old_l = l;
				GUI_Swing.peer_contacts.add(l, BorderLayout.NORTH);
//				l.repaint();
//				jt.repaint();
				GUI_Swing.peer_contacts.revalidate();
				if(DEBUG) System.out.println("PeerContacts: invoked later:did");
			}}
		);		
	}
	// (GID: (instance: (DIR_Address:(ADR:date))))
	private Object[] getTree(Hashtable<String, Hashtable<String, Hashtable<String, Hashtable<String, String>>>> peer_contacts) {
		ArrayList<Object> result = new ArrayList<Object>();
		for (String peer : peer_contacts.keySet()){
			if (DEBUG) System.out.println("PeerContacts: getTree: peer="+peer);
			// if filter and a value is set, remove everything else
			if (filter && (dpa != null)) {
				if (!peer.equals(this.dpa.component_basic_data.name) &&
						!peer.equals(this.my_peer_name) &&
						!(peer.equals(Util.trimmed(dpa.component_basic_data.globalID))))
					continue;
			}
			DNode n = new DNode();
			result.add(n);
			n.text = peer;
			Hashtable<String, Hashtable<String, Hashtable<String, String>>> instances = peer_contacts.get(peer);
			for (String instance_nonull : instances.keySet()) {
				if (DEBUG) System.out.println("PeerContacts: getTree: peer instance/dir="+instance_nonull);
				DNode  da = getInstNode(instances.get(instance_nonull), instance_nonull);
				da.parent = n;
				n.child.add(da);
			}
		}
		return result.toArray();
	}
	// (DIR_Address:(ADR:date))
	private DNode getInstNode(Hashtable<String, Hashtable<String, String>> dir_addresses, String instance_nonull) {
		DNode  inst = new DNode();
		inst.text = instance_nonull;
		for (String dir : dir_addresses.keySet()){
			if (DEBUG) System.out.println("PeerContacts: getInstNode: dir="+dir);
			DNode n = getDANode(dir_addresses.get(dir), dir);
			n.parent = inst;
			inst.child.add(n);
		}
		return inst;
	}
	// (ADR:date)
	private DNode getDANode(Hashtable<String, String> da, String p) {
		DNode n = new DNode();
		n.text = p; // Util.trimmed(p);
		if (da == null) {
			n.text = ":null";
			return n;
		}
		if (pack_sockets && p.startsWith(Address.SOCKET) && (da.size()==1)) {
			for (String ad: da.keySet()) {
				if (DD.ALREADY_CONTACTED.equals(ad)) break;
				DNode a = new DNode();
				a.parent = n;
				a.text = ad+" --- "+da.get(ad);
				n.child.add(a);
				return a;
			}
		}
		for (String ad: da.keySet()) {
			DNode a = new DNode();
			a.parent = n;
			a.text = ad+" --- "+da.get(ad);
			n.child.add(a);
		}
		return n;
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
//		JMenuItem menuItem;
//    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
//    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	PeerContactsAction prAction;
    	//
    	prAction = new PeerContactsAction(this, __("Refresh!"), reseticon,__("Let it refresh."),
    			__("Go refresh!"),KeyEvent.VK_R, PeerContactsAction.REFRESH);
    	//prAction.putValue("row", new Integer(model_row));
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerContactsAction(this, __("No Refresh!"), reseticon,__("Stop refresh."),
    			__("No refresh!"), KeyEvent.VK_S, PeerContactsAction.NO_REFRESH);
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerContactsAction(this, __("Filter by Peer!"), reseticon,__("Filter By Peer."),
    			__("Filter!"), KeyEvent.VK_F, PeerContactsAction.FILTER);
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerContactsAction(this, __("Remove Filter by Peer!"), reseticon,__("Remove Filter By Peer."),
    			__("Unfilter!"), KeyEvent.VK_U, PeerContactsAction.UNFILTER);
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerContactsAction(this, __("Delete Old data!"), reseticon,__("Delete old data."),
    			__("Unfilter!"), KeyEvent.VK_D, PeerContactsAction.RESET);
    	popup.add(new JMenuItem(prAction));

    	prAction = new PeerContactsAction(this, __("Pack Socket entries!"), reseticon,__("Pack Socket entries."),
    			__("Pack Socket!"), KeyEvent.VK_P, PeerContactsAction.PACK_SOCKET);
    	popup.add(new JMenuItem(prAction));

    	return popup;
	}

	@Override
	public void update_peer(D_Peer peer, String my_peer_name, boolean me, boolean selected) {
		this.dpa = peer;
		this.my_peer_name = my_peer_name;
		this.update(D_Peer.peer_contacts);
	}
}
@SuppressWarnings("serial")
class PeerContactsAction extends DebateDecideAction {
    public static final int REFRESH = 0;
    public static final int NO_REFRESH = 1;
	public static final int RESET = 2;
	public static final int UNFILTER = 3;
	public static final int FILTER = 4;
	public static final int PACK_SOCKET = 5;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	PeerContacts tree; ImageIcon icon; int command;
    public PeerContactsAction(PeerContacts tree,
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
        if(DEBUG)System.err.println("PeersRowAction:command property: " + command);
    	JMenuItem mnu;
//    	int row =-1;
//    	if(src instanceof JMenuItem){
//    		mnu = (JMenuItem)src;
//    		Action act = mnu.getAction();
//    		row = ((Integer)act.getValue("row")).intValue();
//            System.err.println("PeersRowAction:row property: " + row);
//    	}
//    	PeersModel model = (PeersModel)tree.getModel();
//    	ArrayList<ArrayList<Object>> a;
		switch(command){
		case REFRESH:
			tree.refresh = true;
			tree.update(D_Peer.peer_contacts);
			break;
		case NO_REFRESH:
			tree.refresh = false;
			break;
		case UNFILTER:
			tree.filter = false;
			tree.update(D_Peer.peer_contacts);
			break;
		case FILTER:
			tree.filter = true;
			tree.update(D_Peer.peer_contacts);
			break;
		case RESET:
			D_Peer.peer_contacts = new Hashtable<String, Hashtable<String, Hashtable<String, Hashtable<String, String>>>>();
			tree.update(D_Peer.peer_contacts);
			break;
		case PACK_SOCKET:
			tree.pack_sockets = !tree.pack_sockets;
			tree.update(D_Peer.peer_contacts);
			break;
		}
    }
}