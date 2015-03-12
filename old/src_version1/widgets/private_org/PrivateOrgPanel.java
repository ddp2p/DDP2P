package widgets.private_org;

import hds.Address;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.util.ArrayList; 

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import data.D_Organization;
import data.D_PeerAddress;
//import data.D_TermsInfo;
import util.DBInterface;
import util.Util;
import widgets.org.OrgListener;
import widgets.peers.PeerListener;
import table.org_distribution;
import table.peer_address;
import static util.Util._;

@SuppressWarnings("serial")
public class PrivateOrgPanel extends JPanel implements  OrgListener{
	
	private JLabel org_label = new JLabel("");
	private static final boolean _DEBUG = true;
	static boolean DEBUG = false;
	
	PrivateOrgTable privateOrgTable;
	
	public JScrollPane privateOrgTableScroll;
	private D_Organization organization = null;

	/**
	 * Listener to changes in widget.org.Orgs
	 */
	public void removeOrgListener(){
    	if(DEBUG)System.out.println("PrivateOrgPanel:removeOrgListener: start");    	
		if(DD.status==null){
			Util.printCallPath("Error: call before initialization of orgs");
			return;
		}
    	DD.status.removeOrgListener(this);
    	if(DEBUG)System.out.println("PrivateOrgPanel:removeOrgListener: done");    	
	}
	/**
	 * Listener to changes in widget.org.Orgs
	 */
	public void addOrgListener(){
    	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: start");    	
		if(DD.status==null){
			Util.printCallPath("Error: call before initialization of orgs");
			return;
		}
    	DD.status.addOrgStatusListener(this);
    	if(organization==null)
        	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: null org");    	
    	//organization = Application.orgs.getCurrentOrg();
    	if(organization==null)
        	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: null org");    	
     	//org_label.setText(getLabelText());
    	new LabelUpdater();
    	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: done");    	
	}

	/**
	 * This uses the org object rather than status
	 * Listener to changes in widget.org.Orgs
	 */
	@Deprecated
	public void _addOrgListener(){
    	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: start");    	
		if(Application.orgs==null){
			Util.printCallPath("Error: call before initialization of orgs");
			return;
		}
    	Application.orgs.addOrgListener(this);
    	if(organization==null)
        	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: null org");    	
    	//organization = Application.orgs.getCurrentOrg();
    	if(organization==null)
        	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: null org");    	
     	//org_label.setText(getLabelText());
    	new LabelUpdater();
    	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: done");    	
	}
	class LabelUpdater implements Runnable{
		LabelUpdater(){
			SwingUtilities.invokeLater(this);
		}
		public void run(){
	     	org_label.setText(getLabelText());			
		}
	}
    public PrivateOrgPanel() {
    	super( new BorderLayout());
    	if(DEBUG)System.out.println("PrivateOrgPanel:<init>: start");    	
    	//if(organization!=null) _organizationID = organization._organization_ID;
    	//if(_organizationID <0)return;
     	org_label.setText(getLabelText());
    	init();
    	if(DEBUG)System.out.println("PrivateOrgPanel:<init>: done");
    }
 	public void init(){
    	if(DEBUG)System.out.println("PrivateOrgPanel:init: start");
    	try{
	    	add(getNorthComponent(), BorderLayout.NORTH);
	    	privateOrgTable = new PrivateOrgTable(Application.db, this);
	    	privateOrgTableScroll = privateOrgTable.getScrollPane();
	    	add(privateOrgTableScroll);
			//addMouseListener(privateOrgTable);
			//privateOrgTableScroll.addMouseListener(privateOrgTable);
	    	//add(privateOrgTable);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	if(DEBUG)System.out.println("PrivateOrgPanel:init: done");
    }
    private String getLabelText() {
    	String result;
    	if((organization  == null) || (organization.name == null)) return _("No current organization!");
    	result = _("Current Organization:")+" \""+organization.name+"\"";
    	//result += " || " + _("I am :")+" "+tree.getModel().getConstituentMyselfName();
		return result;
	}
	
    private Component getNorthComponent() {
    	JPanel header = new JPanel();
     	header.setLayout(new BorderLayout());
     	header.add(org_label, BorderLayout.CENTER);
		return header;
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
    	if(DEBUG)System.out.println("PrivateOrgPanel:orgUpdate: id="+orgID+" col="+col);
		organization = org;
		if((org==null)&&(orgID!=null)) {
			try {
				organization = new D_Organization(Util.lval(orgID, -1));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			if(_DEBUG)System.out.println("PrivateOrgPanel:orgUpdate: null object for valid ID");
		}
     	//org_label.setText(getLabelText());	
    	new LabelUpdater();
     	if(this.privateOrgTable!=null)
     		this.privateOrgTable.setOrg(org);
     	else{
        	if(_DEBUG)System.out.println("PrivateOrgPanel:orgUpdate: null");

     	}
    	if(DEBUG)System.out.println("PrivateOrgUpdate:orgUpdate: done");
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
	}
	public String get_organizationID() {
	   	if(DEBUG)System.out.println("PrivateOrgUpdate:get_orgID: begin");
		if(organization == null){
		   	if(DEBUG)System.out.println("PrivateOrgUpdate:get_orgID: null org");
			return null;
		}
	   	if(DEBUG)System.out.println("PrivateOrgUpdate:get_orgID: ret="+organization.organization_ID);
		return organization.organization_ID;
	}
	public Component getTable() {
		return privateOrgTable;
	}

    
}