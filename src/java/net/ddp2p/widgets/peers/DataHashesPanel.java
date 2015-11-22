/*   Copyright (C) 2015 Marius C. Silaghi
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
package net.ddp2p.widgets.peers;
import static net.ddp2p.common.util.Util.__;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.config.PeerListener;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.streaming.GlobalClaimedDataHashes;
import net.ddp2p.common.streaming.OrgPeerDataHashes;
import net.ddp2p.common.streaming.OrgPeerDataHashes.PeerData;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.GUI_Swing;
/**
 * Class designed for displaying the set of desired hashed (not yet known) for the current organization,
 * together with the globally searched terms (peers, news, translations).
 * 
 * Currently it only tells the first reference/advertisement from each peer.
 * However, we should set an extra flag to tell that a peer tells us it does not have the item and we should not request it.
 * Could be marked by setting the date to a structure richer than a String (2 dates:announcement,rejection, and a rejection flag)
 * @author msilaghi
 *
 */
public class DataHashesPanel extends JPanel implements MouseListener, OrgListener {
	public static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static JTree jt = null;
	public static JLabel l = null;
	public static JTree old_jt = null;
	public static JLabel old_l = null;
	public boolean refresh = true;
	public boolean filter = true;
	String my_org_name;
	D_Organization org;
	public DataHashesPanel(){
		this.setLayout(new BorderLayout());
		old_l = new JLabel(__("Hashes for this org"));
		old_l.setHorizontalTextPosition(SwingConstants.LEFT);
		this.add(old_l, BorderLayout.NORTH);
		this.addMouseListener(this);
		GUI_Swing.dataHashesPanel = this;
	}
	void update() {
		if (DEBUG) System.out.println("DataHashesPanel: update: start");
		if (! refresh) {
			if (_DEBUG) System.out.println("DataHashesPanel: update: norefresh");
			return;
		}
		Object[] data = getTree(org.getSpecificRequest());
		DNode root = new DNode(); root.text = "root";
		for (Object o: data) {
			((DNode)o).parent = root;
			root.child.add((DNode)o);
		}
		jt = new JTree(root);
		jt.setRootVisible(false);
		jt.expandPath(new TreePath(new Object[]{root}));
		for(Object o: data)	jt.expandPath(new TreePath(new Object[]{root, o}));
		jt.addMouseListener(this);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				String now = Util.getGeneralizedTime();
				if(DEBUG) System.out.println("DataHashesPanel: invoked later:do "+now);
				if (old_jt != null) GUI_Swing.dataHashesPanel.remove(old_jt);
				old_jt = jt;
				if (jt != null) GUI_Swing.dataHashesPanel.add(jt,BorderLayout.CENTER);
				if (old_l != null) GUI_Swing.dataHashesPanel.remove(old_l);
				l = new JLabel(__("Latest Contacts for this Peer:")+" "+now);
				l.setHorizontalTextPosition(SwingConstants.LEFT);
				old_l = l;
				GUI_Swing.dataHashesPanel.add(l, BorderLayout.NORTH);
				GUI_Swing.dataHashesPanel.revalidate();
				if(DEBUG) System.out.println("PeerContacts: invoked later:did");
			}}
		);		
	}
	static DNode getNode(Hashtable<String,Hashtable<Long,String>> hashset, String name) {
		DNode n = new DNode();
		n.text = name;
		for (String cGIDH: hashset.keySet()) {
			DNode cn = new DNode();
			cn.parent = n;
			cn.text = cGIDH;
			n.child.add(cn);
			Hashtable<Long, String> hashes = hashset.get(cGIDH);
			for (Long pLID: hashes.keySet()) {
				DNode pn = new DNode();
				pn.parent = cn;
				pn.text = pLID+":"+hashes.get(pLID);
				cn.child.add(pn);
			}
		}
		return n;
	}
	static DNode getNodeObj(Hashtable<String,Hashtable<Long,Object>> hashset, String name) {
		DNode n = new DNode();
		n.text = name;
		for (String cGIDH: hashset.keySet()) {
			DNode cn = new DNode();
			cn.parent = n;
			cn.text = cGIDH;
			n.child.add(cn);
			Hashtable<Long, Object> hashes = hashset.get(cGIDH);
			for (Long pLID: hashes.keySet()) {
				DNode pn = new DNode();
				pn.parent = cn;
				Object o = hashes.get(pLID);
				if (o instanceof PeerData)
					pn.text = pLID+":"+ ((PeerData) o).toLongString();
				else pn.text = pLID+":"+o;
				cn.child.add(pn);
			}
		}
		return n;
	}
	private Object[] getTree(OrgPeerDataHashes odph) {
		GlobalClaimedDataHashes gcdh = GlobalClaimedDataHashes.get();
		ArrayList<Object> result = new ArrayList<Object>();
		result.add(getNodeObj(odph.cons, "Constituents"));
		result.add(getNode(odph.neig, "Neighborhoods"));
		result.add(getNode(odph.witn, "Witnesses"));
		result.add(getNode(odph.moti, "Motions"));
		result.add(getNode(odph.just, "Justification"));
		result.add(getNode(odph.sign, "Signature"));
		result.add(getNode(odph.news, "News"));
		result.add(getNode(odph.tran, "Translations"));
		result.add(getNode(gcdh.peers, "Peers"));
		result.add(getNode(gcdh.news, "GNews"));
		result.add(getNode(gcdh.tran, "GTran"));
		return result.toArray();
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
    	JPopupMenu popup = getPopup(evt);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	private JPopupMenu getPopup(MouseEvent evt) {
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	return popup;
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		this.org = org;
		if (org != null) this.my_org_name = org.getOrgNameOrMy();
		else this.my_org_name = null;
		this.update();
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		this.org = org;
		if (org != null) this.my_org_name = org.getOrgNameOrMy();
		else this.my_org_name = null;
		this.update();
	}
}
