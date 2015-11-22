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
 package net.ddp2p.widgets.constituent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.ConstituentListener;
import net.ddp2p.common.config.Language;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.data.DDTranslation;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.MainFrame;
import static net.ddp2p.common.util.Util.__;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
public class ConstituentsPanel extends JPanel implements OrgListener, ActionListener, RefreshListener, ConstituentListener {
    private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	public ConstituentsTree tree;
    long organizationID = -1;
    long constituentID = -1;
    String global_constituentID;
	private JLabel org_label = new JLabel("");
	private JButton refresh_button = new JButton(__("Up to Date"));
	private D_Organization organization;
	private D_Constituent me;
    private Component getNorthComponent() {
    	JPanel header = new JPanel();
     	header.setLayout(new BorderLayout());
     	header.add(org_label, BorderLayout.CENTER);
     	header.add(refresh_button,BorderLayout.EAST);
		return header;
	}
    public ConstituentsPanel(DBInterface db, int _organizationID, int _constituentID, String _global_constituentID) {
    	super(new BorderLayout());
    	constituentID = _constituentID;
    	organizationID = _organizationID;
    	global_constituentID = _global_constituentID;
    	D_Organization org = null;
    	if (organizationID > 0)
			try {
				org = D_Organization.getOrgByLID_NoKeep(organizationID, true);
				organization = org;
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
    	if (constituentID > 0)
			try {
				me = D_Constituent.getConstByLID(constituentID, true, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
    	MainFrame.status.addOrgStatusListener(this);
    	MainFrame.status.addConstituentMeStatusListener(this);
    	if (_organizationID < 0) return;
     	org_label.setText(getLabelText());
     	refresh_button.addActionListener(this);
     	disableRefresh();
        add(getNorthComponent(), BorderLayout.NORTH);
    	ConstituentsModel cm = new ConstituentsModel(db, _organizationID, _constituentID, _global_constituentID, org, this);
    	tree = new ConstituentsTree( cm );
    	long cID = cm.getConstituentIDMyself();
    	if (cID != constituentID && cID > 0) {
    		constituentID = cID;
			me = D_Constituent.getConstByLID(constituentID, true, false);
    	}
		cm.expandConstituentID(tree, ((cID<=0)?null:(""+cID)), true);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        add(scrollPane, BorderLayout.CENTER);
    }
    private String getLabelText() {
    	String result;
    	if (organization == null) return __("No current organization!");
    	if (organization.getName() == null) return __("Incomplete organization data!");
    	result = __("Current Organization:")+" \""+organization.getName()+"\"";
    	result += " || " + __("I am :")+" ";
    	if (me == null)
    		result += tree.getModel().getConstituentMyselfName();
    	else
    		result += me.getNameFull();
		return result;
	}
	public void setOrg(long _orgID, D_Organization org) throws P2PDDSQLException{
    	if (DEBUG) System.out.println("ConstituentsTest:setOrg: id="+_orgID);
    	organization = org;
    	if (tree != null) {
    		tree.clean();
    	}
    	organizationID = _orgID;
    	constituentID = net.ddp2p.common.config.Identity.getDefaultConstituentIDForOrg(organizationID);
    	global_constituentID = D_Constituent.getGIDFromLID(constituentID);
    	if (global_constituentID == null) {
    		constituentID = -1;
    		MainFrame.status.setMeConstituent(null);
    	} else {
    		MainFrame.status.setMeConstituent(me = D_Constituent.getConstByLID(constituentID, true, false));
    	}
       	if (constituentID > 0) {
    			try {
    				me = D_Constituent.getConstByLID(constituentID, true, false);
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
       	}
    	ConstituentsModel cm = new ConstituentsModel(Application.getDB(), organizationID, constituentID, global_constituentID, org, this);
     	tree = new ConstituentsTree(cm);
    	long cID = cm.getConstituentIDMyself();
       	if (cID != constituentID && cID > 0) {
    		constituentID = cID;
			me = D_Constituent.getConstByLID(constituentID, true, false);
    	}
		cm.expandConstituentID(tree, ( (cID <= 0) ? null:(""+cID)), true);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        this.removeAll();
    	if (org_label != null) {
    		org_label.setText(getLabelText());
    		add(getNorthComponent(), BorderLayout.NORTH);
    		disableRefresh();
    		this.refresh_button.addActionListener(this);
    	}
        add(scrollPane, BorderLayout.CENTER);
    }
	private static void createAndShowGUI(DBInterface db) {
        JFrame frame = new JFrame("Constituents Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ConstituentsPanel newContentPane = new ConstituentsPanel(db, -1, -1, null);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
    public static void main(String[] args) {
	if(args.length==0) return;
	final String dbname = args[0];
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    try {
		    	final DBInterface db = new DBInterface(dbname);
		    	DDTranslation.db=db;
		    	DDTranslation.preferred_languages=new Language[]{
			    		new Language("ro", "RO")	
		    	};
		    	DDTranslation.constituentID=100;
		    	DDTranslation.organizationID=100;
		    	DDTranslation.preferred_charsets=new String[]{"latin"};
		    	DDTranslation.authorship_charset="latin";
		    	DDTranslation.authorship_lang = new Language("ro","RO");
				createAndShowGUI(db);
		    }catch(Exception e){
		    	JOptionPane.showMessageDialog(null,"Error opening database!");
		    	e.printStackTrace();
		    	return;
		    }
		}
	    });
    }
	@Override
	public void constituentUpdate(D_Constituent c, boolean me, boolean selected) {
		if (!me) return;
		if (c != null)
			this.constituentID = c.getLID();
		else 
			this.constituentID = -1;
		this.me = c;
    	if (org_label != null) {
    		org_label.setText(getLabelText());
    	}		
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
    	if(DEBUG)System.out.println("ConstituentsTest:orgUpdate: id="+orgID+" col="+col);
    	long _orgID = -1;
    	if (orgID != null) {
    		_orgID = new Integer(orgID).longValue();
	   		if (org == null) org = D_Organization.getOrgByLID_NoKeep(orgID, true);
	   		if (org == null) _orgID = -1;
    	} else {
    		if (org != null) _orgID = org.getLID_forced();
     	}
		try {
			this.setOrg(_orgID, org);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
	   	if(_DEBUG)System.out.println("ConstituentsTest:forceEdit: id="+orgID);
	}
	public void disableRefresh(){
	   	if(DEBUG)System.out.println("\n********************\nConstituentsTest:disableRefresh start");
	   	this.refresh_button.removeActionListener(this);
		this.refresh_button.setEnabled(false);
		this.refresh_button.setText(__("Up to Date"));
		this.refresh_button.setBackground(this.org_label.getBackground());
		this.refresh_button.setForeground(this.org_label.getForeground());
		this.refresh_button.addActionListener(this);
	   	if(DEBUG)System.out.println("\n********************\nConstituentsTest:disableRefresh done");
	}
	public void enableRefresh(){
	   	if(DEBUG)System.out.println("\n********************\nConstituentsTest:enableRefresh");
	   	this.refresh_button.removeActionListener(this);
		this.refresh_button.setEnabled(true);
		this.refresh_button.setText(__("Refresh Needed"));
		this.refresh_button.setBackground(Color.YELLOW);
		this.refresh_button.setForeground(Color.RED);
	   	this.refresh_button.addActionListener(this);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if(DEBUG)System.out.println("ConstituentsPanel:action Refresh \n"+e);
		if(e.getSource() == this.refresh_button) {
			if(!this.refresh_button.isEnabled()) return;
			if(tree == null) return;
			ConstituentsModel model = tree.getModel();
			if(model == null) return;
			try {
				model.doRefreshAll();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
		}
	}
}
