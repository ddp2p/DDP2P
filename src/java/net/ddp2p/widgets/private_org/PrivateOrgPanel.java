package net.ddp2p.widgets.private_org;
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
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.config.PeerListener;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.table.org_distribution;
import net.ddp2p.common.table.peer_address;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.components.GUI_Swing;
import java.util.ArrayList; 
import static net.ddp2p.common.util.Util.__;
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
		if(MainFrame.status==null){
			Util.printCallPath("Error: call before initialization of orgs");
			return;
		}
    	MainFrame.status.removeOrgListener(this);
    	if(DEBUG)System.out.println("PrivateOrgPanel:removeOrgListener: done");    	
	}
	/**
	 * Listener to changes in widget.org.Orgs
	 */
	public void addOrgListener(){
    	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: start");    	
		if(MainFrame.status==null){
			Util.printCallPath("Error: call before initialization of orgs");
			return;
		}
    	MainFrame.status.addOrgStatusListener(this);
    	if(organization==null)
        	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: null org");    	
    	if(organization==null)
        	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: null org");    	
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
		if(GUI_Swing.orgs==null){
			Util.printCallPath("Error: call before initialization of orgs");
			return;
		}
    	GUI_Swing.orgs.addOrgListener(this);
    	if(organization==null)
        	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: null org");    	
    	if(organization==null)
        	if(DEBUG)System.out.println("PrivateOrgPanel:addOrgListener: null org");    	
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
     	org_label.setText(getLabelText());
    	init();
    	if(DEBUG)System.out.println("PrivateOrgPanel:<init>: done");
    }
 	public void init(){
    	if(DEBUG)System.out.println("PrivateOrgPanel:init: start");
    	try{
	    	add(getNorthComponent(), BorderLayout.NORTH);
	    	privateOrgTable = new PrivateOrgTable(Application.getDB(), this);
	    	privateOrgTableScroll = privateOrgTable.getScrollPane();
	    	add(privateOrgTableScroll);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	if(DEBUG)System.out.println("PrivateOrgPanel:init: done");
    }
    private String getLabelText() {
    	String result;
    	if((organization  == null) || (organization.getName() == null)) return __("No current organization!");
    	result = __("Current Organization:")+" \""+organization.getName()+"\"";
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
			organization = D_Organization.getOrgByLID_NoKeep(orgID, true);
			if(_DEBUG)System.out.println("PrivateOrgPanel:orgUpdate: null object for valid ID");
		}
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
	   	if(DEBUG)System.out.println("PrivateOrgUpdate:get_orgID: ret="+organization.getLIDstr());
		return organization.getLIDstr();
	}
	public Component getTable() {
		return privateOrgTable;
	}
}
