/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
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
package widgets.directories;

import hds.Address;
import hds.DebateDecideAction;
import hds.DirectoryAnswer;

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

import ASN1.Encoder;

import config.Application;
import config.DDIcons;

import util.Util;

import static util.Util._;

class DNode implements TreeNode{
	public String text;
	public ArrayList<DNode> child=new ArrayList<DNode>();
	public DNode parent = null;
	public String toString(){
		return text;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		return child.get(childIndex);
	}

	@Override
	public int getChildCount() {
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
		return false;
	}

	@Override
	public boolean isLeaf() {
		return child.size()==0;
	}

	@Override
	public Enumeration<?> children() {
		return Collections.enumeration(child);
	}
	
}
@SuppressWarnings("serial")
public class DirectoriesData extends JPanel implements MouseListener  {
	public static final boolean _DEBUG = true;
	// dir_IP: (GID: ()addresses)
	public static Hashtable<String,Hashtable<String,DirectoryAnswer>> dir_data = new Hashtable<String,Hashtable<String,DirectoryAnswer>>();

	// new and old trees (to remove old and set new in the panel)
	public static JTree jt;
	public static JTree old_jt = null;
	public boolean refresh = true;
	DirectoriesData(){
		this.setLayout(new BorderLayout());
		JLabel l = new JLabel(_("Latest Data from Directories"));
		l.setHorizontalTextPosition(SwingConstants.LEFT);
		//l.setHorizontalAlignment(JLabel.LEFT);
		this.add(l, BorderLayout.NORTH);
		this.addMouseListener(this);
	}
	//String colNames[] = {_("Directory"),_("Peer"),_("Date"),_("Address")};
	public void setData(Hashtable<String,Hashtable<String,DirectoryAnswer>> ad) {
		if(_DEBUG) System.out.println("DirectoriesData: setData: start");
		if(!refresh){
			if(_DEBUG) System.out.println("DirectoriesData: setData: norefresh");
			return;
		}
		Object[] data = getTree(ad);
		DNode root = new DNode(); root.text = "root";
		for(Object o: data){
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
				if(old_jt!=null)Application.directoriesData.remove(old_jt);
				old_jt = jt;
				Application.directoriesData.add(jt,BorderLayout.CENTER);
				Application.peer.revalidate();
			}}
		);
	}
	public Object[] getTree(
			Hashtable<String, Hashtable<String, DirectoryAnswer>> ad) {
		Object[] result = new Object[ad.size()];
		int d=0;
		for(String dir : ad.keySet()){
			DNode n = new DNode();
			result[d] = n;
			n.text = dir;
			Hashtable<String, DirectoryAnswer> peers = ad.get(dir);
			for(String p : peers.keySet()) {
				DNode  da = getDANode(peers.get(p), p);
				da.parent=n;
				n.child.add(da);
			}
			d++;
		}
		return result;
	}
	private DNode getDANode(DirectoryAnswer da, String p) {
		DNode n = new DNode();
		n.text = Util.trimmed(p);
		if(da==null){
			n.text += ":null";
			return n;
		}
		n.text += ": "+Encoder.getGeneralizedTime(da.date);
		if(da.addresses==null){
			return n;
		}
		for(Address ad: da.addresses) {
			DNode a = new DNode();
			a.parent = n;
			a.text = ad.toString();
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
		JMenuItem menuItem;
    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(_("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(_("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(_("reset item"));
    	JPopupMenu popup = new JPopupMenu();
       	DirDataAction prAction;
    	//
    	prAction = new DirDataAction(this, _("Refresh!"), reseticon,_("Let it refresh."),
    			_("Go refresh!"),KeyEvent.VK_R, DirDataAction.REFRESH);
    	//prAction.putValue("row", new Integer(model_row));
    	popup.add(new JMenuItem(prAction));

    	prAction = new DirDataAction(this, _("No Refresh!"), reseticon,_("Stop refresh."),
    			_("No refresh!"), KeyEvent.VK_S, DirDataAction.NO_REFRESH);
    	popup.add(new JMenuItem(prAction));

    	return popup;
	}
}
@SuppressWarnings("serial")
class DirDataAction extends DebateDecideAction {
    public static final int REFRESH = 0;
    public static final int NO_REFRESH = 1;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	DirectoriesData tree; ImageIcon icon; int command;
    public DirDataAction(DirectoriesData tree,
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
        System.err.println("PeersRowAction:command property: " + command);
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
			break;
		case NO_REFRESH:
			tree.refresh = false;
			break;
		}
    }
}