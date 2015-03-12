package widgets.dir_fw_terms;

import hds.Address;

import java.awt.BorderLayout;
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

import java.util.ArrayList; 

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import data.D_PeerAddress;
import data.D_TermsInfo;
import util.DBInterface;
import util.Util;
import widgets.peers.PeerListener;
import table.directory_forwarding_terms;
import table.peer_address;
import static util.Util._;

@SuppressWarnings("serial")
public class TermsPanel extends JPanel implements  ActionListener, PeerListener{
    private static final int GENERAL_YES = 1;
	private static final int GENERAL_NO = 2;
	private static final int GENERAL_CANCEL = 0;
	private static final boolean _DEBUG = true;
	TermsTable termsTable;
    long peerID=-1;
    String peerName;
    String dirAddr;
    JComboBox<String> dirList;
    static boolean DEBUG = false;
    JScrollPane termsTableScroll;
	JCheckBox cascadeBtm = new JCheckBox("Link To Global Default",false);
	JCheckBox cascadeDtm = new JCheckBox("Link To Directory Default",false);
	JCheckBox cascadePtm = new JCheckBox("Link To Peer Default",false);
	public JTextField peerTopicTxt=null;
	//peerTopicTxt.getText()
	private boolean linkToGlobal;
	private boolean linkToDirectory;
	private boolean linkToPeer;
	private String default_for_peer;
    public TermsPanel() {
    	super( new BorderLayout());
    	init();
 //   	Application.panelUpdates = this;
    }
    public TermsPanel(int peerID, String peerName) {
    	super( new BorderLayout());
    	this.peerID=peerID;
    	this.peerName=peerName;
    	init();
 //   	Application.panelUpdates = this;
    }
    public JPanel getHeaderPanel() {
    	JPanel headerPanel = new JPanel(new GridLayout(3,1));
    	if(peerID!=-1 && peerName!=null){
	    	JLabel peerIDL = new JLabel(_("Peer ID:"));
	    	peerIDL.setFont(new Font("Times New Roman",Font.BOLD,14));
	    	JTextField peerIDTxt = new JTextField(this.peerID+"", 10);
	    	peerIDTxt.setEnabled(false);
	    	JLabel peerNameL = new JLabel(_("Peer Name:"));
	    	peerNameL.setFont(new Font("Times New Roman",Font.BOLD,14));
	    	JTextField peerNameTxt = new JTextField(this.peerName, 10);
	        peerNameTxt.setEnabled(false);
	    	headerPanel.add(peerIDL);
	    	headerPanel.add(peerIDTxt);
	    	headerPanel.add(peerNameL);
	    	headerPanel.add(peerNameTxt);
    	}
    	JLabel dirL = new JLabel(_("Select Peer Directory:"));
	    dirL.setFont(new Font("Times New Roman",Font.BOLD,14));
	    dirList = new JComboBox<String>(/*new String[] {"163.118.78.44:25123"}*/); 
	    dirList.setPreferredSize(new Dimension(150,20));
	    dirList.addActionListener(this);
	    JPanel dirs = new JPanel();
	    dirs.add(dirL);
	    dirs.add(dirList);
	    headerPanel.add(dirs);
	    cascadeBtm.setEnabled(false);
	    cascadeDtm.setEnabled(false);
	    cascadePtm.setEnabled(false);
	    cascadePtm.addActionListener(this);
	    cascadeBtm.addActionListener(this);
	    cascadeDtm.addActionListener(this);
	    headerPanel.add(cascadeBtm); 
	    headerPanel.add(cascadeDtm); 
	    headerPanel.add(cascadePtm);
	    JLabel peerTopicL = new JLabel(_("Topic : "));
	    peerTopicL.setFont(new Font("Times New Roman",Font.BOLD,14));
	    peerTopicTxt = new JTextField(10);
	    JPanel topicPanel = new JPanel();
	    topicPanel.add(peerTopicL);
	    topicPanel.add(peerTopicTxt);
	    topicPanel.setAlignmentX(1);
	    headerPanel.add(topicPanel); 
    	return headerPanel;
    }
    @Override
	public void actionPerformed(ActionEvent e) {
    	try{_actionPerformed(e);}
    	catch(Exception o){o.printStackTrace();}
    }
    
	public void _actionPerformed(ActionEvent e) throws P2PDDSQLException {
		if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:start");
    	Object source = e.getSource();
    	if(source == (Object)dirList) {
    		@SuppressWarnings("unchecked")
    		JComboBox<String> cb = (JComboBox<String>)e.getSource();
    		dirAddr = (String)cb.getSelectedItem();
    		if(dirAddr==null){
    			cascadeBtm.setEnabled(false);
    			cascadeDtm.setEnabled(false);
    			cascadePtm.setEnabled(false);
    		}else{
    			cascadeBtm.setEnabled(true);
    			cascadeDtm.setEnabled(true);    			
    			cascadePtm.setEnabled(true);    			
    		}
    		if(dirAddr == this.default_for_peer){
    			dirAddr = null;
    		}
    		termsTable.getModel().setPeerID(peerID);
    		termsTable.getModel().setDirAddr(dirAddr);
    		if(_DEBUG) System.out.println("TermsPanelL_actionPerformed:Dir Selected: "+ dirAddr);
    		termsTable.repaint();
    		termsTable.getModel().update(null,null);
    		termsTable.repaint();
    		termsTableScroll.repaint();
    		this.repaint();
    	}else
    		if(source == cascadeBtm){
				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Global start");
    			linkToGlobal = cascadeBtm.isSelected();
    			if(linkToGlobal) {
    				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Global");
    				
    	    		Object[] options = new Object[]{_("Cancel"), _("Load into Global Default"), _("Load Global Default")};
    	    		int setThisToGeneral = Application.ask(_("Are you sure?"),
       						(linkToPeer||linkToDirectory)?_("Current display is linked"):_("Curent display is specific"),
       						options, options[1], null);
    	    		if((setThisToGeneral == GENERAL_CANCEL)||(setThisToGeneral == JOptionPane.CLOSED_OPTION)){
    	    			linkToGlobal = false;
        	    		removeListeners();
           				cascadeBtm.setSelected(false);
        	    		restoreListeners();
    	    			return;
    	    		}
    	    		removeListeners();
    				cascadeDtm.setSelected(false);
    				cascadePtm.setSelected(false);
    	    		restoreListeners();
       				linkToDirectory = linkToPeer = false;
    				if(setThisToGeneral==GENERAL_YES){
    					deleteGlobalTerms();
    					setThisAsGlobalTerms(peerID, dirAddr);
    				}else if(setThisToGeneral==GENERAL_NO){
    					deleteAllTerms(peerID, dirAddr);
    					loadGlobalTerms();
    				}
    			}else{
    				termsTable.getModel();
					ArrayList<ArrayList<Object>> t = TermsModel.getTerms(0, null);
					deleteAllTerms(peerID, dirAddr);
    				saveTerms(t, peerID, dirAddr);
    				termsTable.getModel()._dirAddr = dirAddr;
    				termsTable.getModel()._peerID = peerID;
    				termsTable.getModel().update(null,null);
    			}
    		}else if(source == cascadeDtm){
				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Dir start");
    			linkToDirectory = cascadeDtm.isSelected();
    			if(linkToDirectory){
    				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Dir");
    	    		Object[] options = new Object[]{_("Cancel"), _("Load into Dir Default"), _("Load Dir Default")};
       				int setThisToGeneral = Application.ask(_("Are you sure?"),
       						(linkToPeer||linkToGlobal)?_("Current display is linked"):_("Curent display is specific"),
       						options, options[1], null);
       						//GENERAL_CANCEL;
    	    		if((setThisToGeneral == GENERAL_CANCEL)||(setThisToGeneral == JOptionPane.CLOSED_OPTION)){
    	    			linkToDirectory = false;
        	    		removeListeners();
           				cascadeDtm.setSelected(false);
        	    		restoreListeners();
        	    		return;
    	    		}
    	    		removeListeners();
       				cascadeBtm.setSelected(false);
       				cascadePtm.setSelected(false);
    	    		restoreListeners();
       				linkToGlobal = linkToPeer = false;
    				if(setThisToGeneral==GENERAL_YES){
    					deleteDirectoryTerms(dirAddr);
    					setThisAsDirectoryTerms(peerID, dirAddr);
    				}else if(setThisToGeneral==GENERAL_NO){
    					deleteAllTerms(peerID, dirAddr);
    					loadDirectoryTerms(dirAddr);
    				}
    			}else{
    				termsTable.getModel();
					ArrayList<ArrayList<Object>> t = TermsModel.getTerms(0, dirAddr);
					deleteAllTerms(peerID, dirAddr);
    				saveTerms(t, peerID, dirAddr);
    				termsTable.getModel()._dirAddr = dirAddr;
    				termsTable.getModel()._peerID = peerID;
    				termsTable.getModel().update(null,null);
    			}
    		}else if(source == cascadePtm){
				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer start");
    			linkToPeer = cascadePtm.isSelected();
    			if(linkToPeer){
    				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer");
    	    		Object[] options = new Object[]{_("Cancel"), _("Load into Peer Default"), _("Load Peer Default")};
    	    		int setThisToGeneral = Application.ask(_("Are you sure?"),
       						(linkToDirectory||linkToGlobal)?_("Current display is linked"):_("Curent display is specific"),
       						options, options[1], null);
    	    		if((setThisToGeneral == GENERAL_CANCEL)||(setThisToGeneral == JOptionPane.CLOSED_OPTION)){
    	    			linkToPeer = false;
        	    		removeListeners();
           				cascadePtm.setSelected(false);
        	    		restoreListeners();
        	    		return;
    	    		}
    	    		removeListeners();
    				cascadeBtm.setSelected(false);
    				cascadeDtm.setSelected(false);
    	    		restoreListeners();
       				linkToDirectory = linkToGlobal = false;
    				if(setThisToGeneral==GENERAL_YES){
    					deletePeerTerms(peerID);
    					setThisAsPeerTerms(peerID, dirAddr);
    				}else if(setThisToGeneral==GENERAL_NO){
    					deleteAllTerms(peerID, dirAddr);
    					loadPeerTerms(peerID);
    				}
    			}else{
    				termsTable.getModel();
					ArrayList<ArrayList<Object>> t = TermsModel.getTerms(peerID, null);
					if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer will delete terms");
					deleteAllTerms(peerID, dirAddr);
					if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer terms deleted");
					saveTerms(t, peerID, dirAddr);
					if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer terms saved");
    				termsTable.getModel()._dirAddr = dirAddr;
    				termsTable.getModel()._peerID = peerID;
    				termsTable.getModel().update(null,null);
    			}
    		}
	}	
	private void restoreListeners() {
		cascadeBtm.addActionListener(this);
		cascadeDtm.addActionListener(this);
		cascadePtm.addActionListener(this);
	}
	private void removeListeners() {
		cascadeBtm.removeActionListener(this);
		cascadeDtm.removeActionListener(this);
		cascadePtm.removeActionListener(this);
	}
	private void saveTerms(ArrayList<ArrayList<Object>> t, long peerID2,
			String dirAddr2) {
		for(int k=0; k<t.size(); k++) {
			ArrayList<Object> e = t.get(k);
			D_TermsInfo ti = new D_TermsInfo(e);
			ti.peer_ID = peerID2;
			ti.dir_addr = dirAddr2;
			try {
				ti.storeNoSync("insert");
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	private void setThisAsPeerTerms(long peerID2, String dirAddr2) throws P2PDDSQLException {
		Application.db.updateNoSync(table.directory_forwarding_terms.TNAME,
				new String[]{
					table.directory_forwarding_terms.dir_addr},
				new String[]{
					table.directory_forwarding_terms.peer_ID,
					table.directory_forwarding_terms.dir_addr},
				new String[]{null, ""+peerID2, dirAddr2}, DEBUG);
	}
	static void setThisAsGlobalTerms(long peerID2, String dirAddr2) throws P2PDDSQLException {
		Application.db.updateNoSync(table.directory_forwarding_terms.TNAME,
				new String[]{
					table.directory_forwarding_terms.peer_ID,
					table.directory_forwarding_terms.dir_addr},
				new String[]{
					table.directory_forwarding_terms.peer_ID,
					table.directory_forwarding_terms.dir_addr},
				new String[]{"0", null, ""+peerID2, dirAddr2}, DEBUG);
	}
	private void deleteGlobalTerms() throws P2PDDSQLException {
//		Application.db.delete("DELETE FROM "+table.directory_forwarding_terms.TNAME+
//				" WHERE "+table.directory_forwarding_terms.peer_ID+" =? AND "+
//				table.directory_forwarding_terms.dir_addr+" IS NULL;",
//				new String[]{"0"}, DEBUG);
		Application.db.deleteNoSyncNULL(
				table.directory_forwarding_terms.TNAME,
				new String[]{
						table.directory_forwarding_terms.peer_ID,
						table.directory_forwarding_terms.dir_addr},
				new String[]{"0",null}, DEBUG);
	}
	private void deletePeerTerms(long peerID2) throws P2PDDSQLException {
//		Application.db.delete("DELETE FROM "+table.directory_forwarding_terms.TNAME+
//				" WHERE "+table.directory_forwarding_terms.peer_ID+" =? AND "+
//				table.directory_forwarding_terms.dir_addr+" IS NULL;",
//				new String[]{""+peerID2}, DEBUG);
		Application.db.deleteNoSyncNULL(table.directory_forwarding_terms.TNAME,
				new String[]{
					table.directory_forwarding_terms.peer_ID,
					table.directory_forwarding_terms.dir_addr},
				new String[]{""+peerID2, null}, _DEBUG);
	}
	static void deleteAllTerms(long peerID2, String dirAddr2) throws P2PDDSQLException {
		Application.db.deleteNoSync(table.directory_forwarding_terms.TNAME,
				new String[]{
					table.directory_forwarding_terms.peer_ID,
					table.directory_forwarding_terms.dir_addr
				}, new String[]{""+peerID2, ""+dirAddr2}, DEBUG);
	}
	static void deleteDirectoryTerms(String dirAddr2) throws P2PDDSQLException {
		Application.db.deleteNoSync(table.directory_forwarding_terms.TNAME,
				new String[]{
					table.directory_forwarding_terms.peer_ID,
					table.directory_forwarding_terms.dir_addr},
				new String[]{"0", dirAddr2}, DEBUG);
	}
	static void setThisAsDirectoryTerms(long peerID2, String dirAddr2) throws P2PDDSQLException {
		Application.db.updateNoSync(table.directory_forwarding_terms.TNAME,
				new String[]{
					table.directory_forwarding_terms.peer_ID},
				new String[]{
					table.directory_forwarding_terms.peer_ID,
					table.directory_forwarding_terms.dir_addr},
				new String[]{"0", ""+peerID2, dirAddr2}, DEBUG);
	}
    private void loadPeerTerms(long peerID2) {
    	termsTable.getModel()._dirAddr = null;
    	termsTable.getModel()._peerID = peerID;
    	termsTable.getModel().update(null, null);
	}
	private void loadDirectoryTerms(String dirAddr2) {
    	termsTable.getModel()._peerID = 0;
    	termsTable.getModel()._dirAddr = dirAddr;
    	termsTable.getModel().update(null, null);
	}
	private void loadGlobalTerms() {
    	termsTable.getModel()._dirAddr = null;
    	termsTable.getModel()._peerID = 0;
    	termsTable.getModel().update(null, null);
	}
	public void init(){
    	add(getHeaderPanel(), BorderLayout.NORTH);
    	termsTable = new TermsTable(Application.db, this);
    	termsTableScroll = termsTable.getScrollPane();
    	add(termsTableScroll);
		addMouseListener(termsTable);
		termsTableScroll.addMouseListener(termsTable);
    }
    public void queryDir(){
		if(DEBUG) System.out.println("TermsPanel:queryDir:start");
    	String sql = "SELECT "+peer_address.address+
    				" FROM  "+peer_address.TNAME+
    				" WHERE "+peer_address.peer_ID+" = ? "+
    				" AND peer_address.type = ? ;";
		String[]params = new String[]{""+peerID, Address.DIR};
		DBInterface db = Application.db; 
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		dirList.removeActionListener(this);
		dirList.removeAllItems();
		for(ArrayList<Object> _u :u){
		    
			if(DEBUG) System.out.println("TermsPanel: dirList: add: "+ui);
			dirList.addItem(Util.getString(_u.get(0)));
			//data.add(ui); // add a new item to data list (rows)
		}
		if(dirList.getItemCount()>0) {
			dirAddr = Util.getString(dirList.getItemAt(0));
			default_for_peer = _("Default for peer");
			dirList.addItem(default_for_peer);
			//System.out.println(dirAddr);
			this.cascadeBtm.setEnabled(true);
			this.cascadeDtm.setEnabled(true);
			this.cascadePtm.setEnabled(true);
		}else{
			this.cascadeBtm.setEnabled(false);
			this.cascadeDtm.setEnabled(false);			
			this.cascadePtm.setEnabled(false);			
		}
		dirList.addActionListener(this);
		removeListeners();
		this.cascadeBtm.setSelected(false);
		this.cascadeDtm.setSelected(false);			
		this.cascadePtm.setSelected(false);
		restoreListeners();
		if(DEBUG) System.out.println("TermsPanel:queryDir:done");
    }
    public void setPeerID(long _peer_ID){
    	if(this.peerID == _peer_ID) return;
    	this.peerID = _peer_ID;
    	termsTable.getModel().setPeerID(_peer_ID);
    	queryDir();	
    	termsTable.getModel().setDirAddr(dirAddr);
		if(DEBUG) System.out.println("TermsPanel:setPeerID:call update");
    	termsTable.getModel().update(null,null);
    	termsTable.setModel(termsTable.getModel());
    	termsTable.repaint();
    	termsTableScroll.repaint();
    //	termsTable.getScrollPane().repaint();
    }
    public void showJFrame(){
   		final JFrame frame = new JFrame();
   		//TermsPanel termsPanel = new TermsPanel();
   		JPanel p = new JPanel(new BorderLayout());
		p.add(this);
		frame.setContentPane(p);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.pack();
		frame.setSize(450,200);
		frame.setVisible(true);
        JButton okBt = new JButton("   OK   ");
        okBt.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            frame.hide();
           }
        });

		p.add(okBt,BorderLayout.SOUTH);
   				
    } 
 /*   	public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
			UpdatesPanel updatePanel = new UpdatesPanel();
			frame.setContentPane(updatePanel);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setSize(800,300);
			frame.setVisible(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	/*	public JTable getTestersTable() {
			return this.updateKeysTable;
		}*/
	public void setGeneralGlobal() {
		this.cascadeBtm.setSelected(true);
		if(DEBUG) System.out.println("TermsPanel:setGeneralGlobal:Link to global");
	}
	public void setGeneralDirectory() {
		this.cascadeDtm.setSelected(true);
	}
	public void setGeneralPeer() {
		this.cascadePtm.setSelected(true);
	}
	@Override
	public void update_peer(D_PeerAddress peer, String my_peer_name, boolean me, boolean selected) {
		if(DEBUG) System.out.println("TermsPanel:update:set my="+my_peer_name);
		if(peer!=null){
			if(DEBUG) System.out.println("TermsPanel:update:set="+peer.component_basic_data.name);
			this.setPeerID(peer._peer_ID);
		}else{
			if(DEBUG) System.out.println("TermsPanel:update:set"+null);
		}
	}
    
}
