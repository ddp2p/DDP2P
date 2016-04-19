package net.ddp2p.widgets.dir_fw_terms;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import javax.swing.JTextPane;
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
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.PeerListener;
import net.ddp2p.common.data.D_DirectoryServerPreapprovedTermsInfo;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.table.directory_forwarding_terms;
import net.ddp2p.common.table.directory_tokens;
import net.ddp2p.common.table.peer_address;
import net.ddp2p.common.table.peer_instance;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import java.util.ArrayList; 
import static net.ddp2p.common.util.Util.__;
@SuppressWarnings("serial")
public class TermsPanel extends JPanel implements  ActionListener,DocumentListener, PeerListener{
    private static final int GENERAL_YES = 1;
	private static final int GENERAL_NO = 2;
	private static final int GENERAL_CANCEL = 0;
	private static final boolean _DEBUG = true;
	TermsTable termsTable;
    long peerID=-1;
    String peerName;
    String dirAddr;
    long selectedInstanceID;
    JComboBox<String> dirList;
    JComboBox<String> instanceList;
    public static boolean DEBUG = false;
    JScrollPane termsTableScroll;
	JCheckBox cascadeBtm = new JCheckBox("Link To Global Default",false);
	JCheckBox cascadeDtm = new JCheckBox("Link To Directory Default",false);
	JCheckBox cascadePtm = new JCheckBox("Link To Peer Default",false);
	public JTextField peerTopicTxt=null;
	JTextPane dirMsg=null;
	String dirMsgTitle="<center>Directory Instructions</center>";
	private boolean linkToGlobal;
	private boolean linkToDirectory;
	private boolean linkToPeer;
	private String default_for_peer;
	private String default_for_all_Instances;
    public TermsPanel() {
    	super( new BorderLayout());
    	init();
    }
    public TermsPanel(int peerID, String peerName) {
    	super( new BorderLayout());
    	this.peerID=peerID;
    	this.peerName=peerName;
    	init();
    }
    public JPanel getHeaderPanel() {
    	JLabel instanceL = new JLabel(__("Select an Instance:  "));
	    instanceL.setFont(new Font("Times New Roman",Font.BOLD,14));
	    instanceList = new JComboBox<String>(); 
	    instanceList.setPreferredSize(new Dimension(150,20));
	    instanceList.addActionListener(this);
	    JPanel instances = new JPanel(new FlowLayout(FlowLayout.LEADING));
	    instances.add(instanceL);
	    instances.add(instanceList);
    	JLabel dirL = new JLabel(__("Select Peer Directory:"));
	    dirL.setFont(new Font("Times New Roman",Font.BOLD,14));
	    dirList = new JComboBox<String>(); 
	    dirList.setPreferredSize(new Dimension(150,20));
	    dirList.addActionListener(this);
	    JPanel dirs = new JPanel(new FlowLayout(FlowLayout.LEADING));
	    dirs.add(dirL);
	    dirs.add(dirList);
	    JLabel peerTopicL = new JLabel(__("Directory Tocken (Topic): "));
	    peerTopicL.setFont(new Font("Times New Roman",Font.BOLD,14));
	    peerTopicTxt = new JTextField(12);
	    JPanel topicPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
	    topicPanel.add(peerTopicL);
	    topicPanel.add(peerTopicTxt);
	    cascadeBtm.setEnabled(false);
	    cascadeDtm.setEnabled(false);
	    cascadePtm.setEnabled(false);
	    cascadePtm.addActionListener(this);
	    cascadeBtm.addActionListener(this);
	    cascadeDtm.addActionListener(this);
	    dirMsg = new JTextPane();
    	dirMsg.setEditable(false);
    	dirMsg.setContentType("text/html");
    	dirMsg.setBackground(Color.GRAY);
    	dirMsg.setText(dirMsgTitle);
	    JPanel headerPanel = new JPanel(new GridLayout(1,3));
	    JPanel headerPanel1 = new JPanel(new GridLayout(3,1));
	    JPanel headerPanel2 = new JPanel(new GridLayout(3,1));
	    JPanel headerPanel3 = new JPanel(new BorderLayout());
	    headerPanel1.add(instances);    headerPanel2.add(cascadeBtm);    headerPanel3.add(dirMsg);      
	    headerPanel1.add(dirs);         headerPanel2.add(cascadeDtm);
	    headerPanel1.add(topicPanel);   headerPanel2.add(cascadePtm);
	    headerPanel.add(headerPanel1);
	    headerPanel.add(headerPanel2);
	    headerPanel.add(headerPanel3);
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
    	if(source == (Object)instanceList) {
    		@SuppressWarnings("unchecked")
    		JComboBox<String> cb = (JComboBox<String>)e.getSource();
    		selectedInstanceID = Integer.parseInt(Util.getPrefix((String)cb.getSelectedItem(),":"));
    		queryDir();
    		queryToken();
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
    		termsTable.getModel().setSelectedInstanceID(selectedInstanceID);
    		if(_DEBUG) System.out.println("TermsPanelL_actionPerformed:Dir Selected: "+ dirAddr);
    		termsTable.repaint();
    		termsTable.getModel().update(null,null);
    		termsTable.repaint();
    		termsTableScroll.repaint();
    		this.repaint();
    	}
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
    		termsTable.getModel().setSelectedInstanceID(selectedInstanceID);
    		queryToken();
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
    	    		Object[] options = new Object[]{__("Cancel"), __("Load into Global Default"), __("Load Global Default")};
    	    		int setThisToGeneral = Application_GUI.ask(__("Are you sure?"),
       						(linkToPeer||linkToDirectory)?__("Current display is linked"):__("Curent display is specific"),
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
    					setThisAsGlobalTerms(peerID, dirAddr, selectedInstanceID);
    				}else if(setThisToGeneral==GENERAL_NO){
    					deleteAllTerms(peerID, dirAddr, selectedInstanceID);
    					loadGlobalTerms();
    				}
    			}else{
    				System.out.println("(peerID, dirAddr, selectedInstanceID) = ("+peerID+", "+ dirAddr+", "+ selectedInstanceID+")");
    				termsTable.getModel();
					ArrayList<ArrayList<Object>> t = TermsModel.getTerms(0, null,-1);
					deleteAllTerms(peerID, dirAddr, selectedInstanceID);
    				saveTerms(t, peerID, dirAddr, selectedInstanceID);
    				termsTable.getModel()._dirAddr = dirAddr;
    				termsTable.getModel()._peerID = peerID;
    				termsTable.getModel()._selectedInstanceID =selectedInstanceID;
    				termsTable.getModel().update(null,null);
    			}
    		}else if(source == cascadeDtm){
				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Dir start");
    			linkToDirectory = cascadeDtm.isSelected();
    			if(linkToDirectory){
    				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Dir");
    	    		Object[] options = new Object[]{__("Cancel"), __("Load into Dir Default"), __("Load Dir Default")};
       				int setThisToGeneral = Application_GUI.ask(__("Are you sure?"),
       						(linkToPeer||linkToGlobal)?__("Current display is linked"):__("Curent display is specific"),
       						options, options[1], null);
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
    					deleteAllTerms(peerID, dirAddr, selectedInstanceID);
    					loadDirectoryTerms(dirAddr);
    				}
    			}else{
    				termsTable.getModel();
					ArrayList<ArrayList<Object>> t = TermsModel.getTerms(0, dirAddr, -1);
					deleteAllTerms(peerID, dirAddr,selectedInstanceID);
    				saveTerms(t, peerID, dirAddr, selectedInstanceID);
    				termsTable.getModel()._dirAddr = dirAddr;
    				termsTable.getModel()._peerID = peerID;
    				termsTable.getModel().update(null,null);
    			}
    		}else if(source == cascadePtm){
				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer start");
    			linkToPeer = cascadePtm.isSelected();
    			if(linkToPeer){
    				if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer");
    	    		Object[] options = new Object[]{__("Cancel"), __("Load into Peer Default"), __("Load Peer Default")};
    	    		int setThisToGeneral = Application_GUI.ask(__("Are you sure?"),
       						(linkToDirectory||linkToGlobal)?__("Current display is linked"):__("Curent display is specific"),
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
    					deleteAllTerms(peerID, dirAddr, selectedInstanceID);
    					loadPeerTerms(peerID);
    				}
    			}else{
    				termsTable.getModel();
					ArrayList<ArrayList<Object>> t = TermsModel.getTerms(peerID, null, selectedInstanceID);
					if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer will delete terms");
					deleteAllTerms(peerID, dirAddr, selectedInstanceID);
					if(_DEBUG) System.out.println("TermsPanel:_actionPerformed:cascade Peer terms deleted");
					saveTerms(t, peerID, dirAddr, selectedInstanceID);
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
			String dirAddr2, long selectedInstanceID) {
		String dir_domain= null;
		String dir_port = null;
    	if(dirAddr2!=null){
    		dir_domain=dirAddr2.split(":")[0];
    		dir_port=dirAddr2.split(":")[1];
    	}	
		for(int k=0; k<t.size(); k++) {
			ArrayList<Object> e = t.get(k);
			D_DirectoryServerPreapprovedTermsInfo ti = new D_DirectoryServerPreapprovedTermsInfo(e);
			ti.peer_ID = peerID2;
			ti.dir_addr = dir_domain;
			ti.dir_tcp_port = dir_port;
			ti.peer_instance_ID=selectedInstanceID;
			try {
				ti.storeNoSync("insert");
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	private void setThisAsPeerTerms(long peerID2, String dirAddr2) throws P2PDDSQLException {
		String dir_domain= null;
		String dir_port = null;
    	if(dirAddr2!=null){
    		dir_domain=dirAddr2.split(":")[0];
    		dir_port=dirAddr2.split(":")[1];
    	}	
		Application.getDB().updateNoSync(net.ddp2p.common.table.directory_forwarding_terms.TNAME,
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
					net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port,
					net.ddp2p.common.table.directory_forwarding_terms.peer_instance_ID},
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.peer_ID,
					net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
					net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port},
				new String[]{null, null, "-1", ""+peerID2, dir_domain, dir_port}, DEBUG);
	}
	static void setThisAsGlobalTerms(long peerID2, String dirAddr2, long instID) throws P2PDDSQLException {
		String dir_domain= null;
		String dir_port = null;
    	if(dirAddr2!=null){
    		dir_domain=dirAddr2.split(":")[0];
    		dir_port=dirAddr2.split(":")[1];
    	}
		Application.getDB().updateNoSync(net.ddp2p.common.table.directory_forwarding_terms.TNAME,
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.peer_ID,
					net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
					net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port,
					net.ddp2p.common.table.directory_forwarding_terms.peer_instance_ID},
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.peer_ID,
					net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
					net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port,
					net.ddp2p.common.table.directory_forwarding_terms.peer_instance_ID	},
				new String[]{"0", null, null,"-1", ""+peerID2, dir_domain, dir_port, ""+instID}, DEBUG);
	}
	private void deleteGlobalTerms() throws P2PDDSQLException {
		Application.getDB().deleteNoSyncNULL(
				net.ddp2p.common.table.directory_forwarding_terms.TNAME,
				new String[]{
						net.ddp2p.common.table.directory_forwarding_terms.peer_ID,
						net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
						net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port,
						net.ddp2p.common.table.directory_forwarding_terms.peer_instance_ID},
				new String[]{"0",null,null,"-1"}, DEBUG);
	}
	private void deletePeerTerms(long peerID2) throws P2PDDSQLException {
		Application.getDB().deleteNoSyncNULL(net.ddp2p.common.table.directory_forwarding_terms.TNAME,
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.peer_ID,
					net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
					net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port,
					net.ddp2p.common.table.directory_forwarding_terms.peer_instance_ID	},
				new String[]{""+peerID2, null, null, "-1"}, _DEBUG);
	}
	static void deleteAllTerms(long peerID2, String dirAddr2, long selectedInstanceID) throws P2PDDSQLException {
		String dir_domain= null;
		String dir_port = null;
    	if(dirAddr2!=null){
    		dir_domain=dirAddr2.split(":")[0];
    		dir_port=dirAddr2.split(":")[1];
    	}
		Application.getDB().deleteNoSync(net.ddp2p.common.table.directory_forwarding_terms.TNAME,
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.peer_ID,
					net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
					net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port,
					net.ddp2p.common.table.directory_forwarding_terms.peer_instance_ID
				}, new String[]{""+peerID2, dir_domain, dir_port, ""+selectedInstanceID}, DEBUG);
	}
	static void deleteDirectoryTerms(String dirAddr2) throws P2PDDSQLException {
		String dir_domain= null;
		String dir_port = null;
    	if(dirAddr2!=null){
    		dir_domain=dirAddr2.split(":")[0];
    		dir_port=dirAddr2.split(":")[1];
    	}
		Application.getDB().deleteNoSync(net.ddp2p.common.table.directory_forwarding_terms.TNAME,
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.peer_ID,
					net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
					net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port},
				new String[]{"0", dir_domain, dir_port}, DEBUG);
	}
	static void setThisAsDirectoryTerms(long peerID2, String dirAddr2) throws P2PDDSQLException {
		String dir_domain= null;
		String dir_port = null;
    	if(dirAddr2!=null){
    		dir_domain=dirAddr2.split(":")[0];
    		dir_port=dirAddr2.split(":")[1];
    	}
		Application.getDB().updateNoSync(net.ddp2p.common.table.directory_forwarding_terms.TNAME,
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.peer_ID},
				new String[]{
					net.ddp2p.common.table.directory_forwarding_terms.peer_ID,
					net.ddp2p.common.table.directory_forwarding_terms.dir_domain,
					net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port},
				new String[]{"0", ""+peerID2, dir_domain, dir_port}, DEBUG);
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
    	termsTable = new TermsTable(Application.getDB(), this);
    	termsTableScroll = termsTable.getScrollPane();
    	add(termsTableScroll);
		addMouseListener(termsTable);
		termsTableScroll.addMouseListener(termsTable);
		peerTopicTxt.getDocument().addDocumentListener( this );  
	}
	public void changedUpdate(DocumentEvent e) {
		updateAction();
	}
	public void removeUpdate(DocumentEvent e) {
		updateAction();
	}
	public void insertUpdate(DocumentEvent e) {
		updateAction();
	}
	public void updateAction(){
		String dir_domain= null;
    	String dir_port = null;
    	if(dirAddr!=null){
    		dir_domain=dirAddr.split(":")[0];
    		dir_port=dirAddr.split(":")[1];
    	}
		ArrayList<Object> record= directory_tokens.searchForToken(peerID, selectedInstanceID ,dir_domain, dir_port);
		if(record == null && peerTopicTxt!=null && !peerTopicTxt.getText().trim().equals(""))
			insertToken(peerID, selectedInstanceID ,dir_domain, dir_port, peerTopicTxt.getText().trim());
		else if(record!=null && peerTopicTxt!=null && !peerTopicTxt.getText().equals(""))	
			updateToken(Long.parseLong((String)record.get(directory_tokens.PA_DIRECTORY_TOKEN_ID)), peerTopicTxt.getText().trim());   
	}
    public void updateToken(long tokens_ID, String newToken){
    	String sql;
		String[]params;
		sql =   "UPDATE "+directory_tokens.TNAME+
				"SET    "+directory_tokens.token+" =? " +
				"WHERE  "+directory_tokens.directory_tokens_ID+" =? ;";
		params = new String[]{newToken,""+tokens_ID};
		try{
			Application.getDB().updateNoSyncNULL(
					directory_tokens.TNAME, 
					new String[]{directory_tokens.token},
					new String[]{directory_tokens.directory_tokens_ID},
					params,_DEBUG);
	    } catch (P2PDDSQLException e) {
				e.printStackTrace();
			}    	
    }
    public void insertToken(long peerID2, long selectedInstanceID , String dir_domain2, String dir_port2, String newToken){
    	String sql;
		String[]params;
		params = new String[]{""+peerID2, ""+selectedInstanceID , dir_domain2, dir_port2, newToken, null, null};
		try{
			Application.getDB().insertNoSync(
							net.ddp2p.common.table.directory_tokens.TNAME,
							net.ddp2p.common.table.directory_tokens.fields_noID_list,
							params, _DEBUG); 
		} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
    }
    public void queryToken(){
    	String dir_domain= null;
    	String dir_port = null;
    	if(dirAddr!=null){
    		dir_domain=dirAddr.split(":")[0];
    		dir_port=dirAddr.split(":")[1];
    	}
		ArrayList<Object> record= directory_tokens.searchForToken(peerID, selectedInstanceID ,dir_domain, dir_port);
		if(record!=null){
			peerTopicTxt.setText((String)record.get(net.ddp2p.common.table.directory_tokens.PA_TOKEN));
			dirMsg.setText(dirMsgTitle + "<br\\>" + (String)record.get(net.ddp2p.common.table.directory_tokens.PA_INSTRUCTIONS_DIR));
		}
		else {peerTopicTxt.setText("");
		      dirMsg.setText(dirMsgTitle);
		      }
    }
    public void queryInstance(){
		if(DEBUG) System.out.println("TermsPanel:queryInstance:start");
    	String sql = "SELECT "+
    			peer_instance.peer_instance_ID+", "+ 
    			peer_instance.peer_instance +
    				" FROM  "+peer_instance.TNAME+
    				" WHERE "+peer_instance.peer_ID+" = ? ;";
		String[]params = new String[]{""+peerID};
		DBInterface db = Application.getDB(); 
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		instanceList.removeActionListener(this);
		instanceList.removeAllItems();
		instanceList.addItem("0:Original Instance");
		for(ArrayList<Object> _u :u){
			if(DEBUG) System.out.println("TermsPanel: instanceList: add: "+ui);
			instanceList.addItem(Util.getString(_u.get(0)+":"+_u.get(1)));
		}
		if(instanceList.getItemCount()>0) {
			selectedInstanceID = Integer.parseInt(Util.getPrefix(Util.getString(instanceList.getItemAt(0)),":"));
			default_for_all_Instances = __("Default for all instance");
			instanceList.addItem("-1:"+default_for_all_Instances);
		}else{
		}
		instanceList.addActionListener(this);
		if(DEBUG) System.out.println("TermsPanel:queryInstance:done");
    }
    public void queryDir(){
		if(DEBUG) System.out.println("TermsPanel:queryDir:start");
    	String sql_only, sql_null;
    	sql_only = "SELECT "+peer_address.address+", "+
				  peer_address.domain+", "+peer_address.tcp_port+
				" FROM  "+peer_address.TNAME+
				" WHERE "+peer_address.peer_ID+" = ? "+
				" AND "+peer_address.instance+" = ? "+ // is it instance or instance_ID??
				" AND peer_address.type = ? ;";
    	sql_null = "SELECT "+peer_address.address+", "+
				  peer_address.domain+", "+peer_address.tcp_port+
				" FROM  "+peer_address.TNAME+
				" WHERE "+peer_address.peer_ID+" = ? "+
				" AND ("+peer_address.instance+" IS NULL "+ // is it instance or instance_ID??
				" OR "+peer_address.instance+" < '1' )"+
				" AND peer_address.type = ? ;";
		DBInterface db = Application.getDB(); 
		ArrayList<ArrayList<Object>> u;
		try {
			if (selectedInstanceID > 0)
				u = db.select(sql_only, 
						new String[]{Util.getStringID(peerID), Util.getStringID(selectedInstanceID), Address.DIR},
						DEBUG);
			else
				u = db.select(sql_null, 
						new String[]{Util.getStringID(peerID), Address.DIR},
						DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		dirList.removeActionListener(this);
		dirList.removeAllItems();
		if(u==null || u.size()==0)
			dirAddr=null;
		for(ArrayList<Object> _u :u){
			if(DEBUG) System.out.println("TermsPanel: dirList: add: "+ui);
			if(_u.get(1)!=null && _u.get(2)!=null)
				 dirList.addItem(Util.getString(_u.get(1))+":"+Util.getString(_u.get(2)));
			else{
				 Address addr = new Address(Util.getString(_u.get(0)));
				 dirList.addItem(addr.domain+":"+addr.tcp_port);
			}
		}
		if(dirList.getItemCount()>0) {
			dirAddr = Util.getString(dirList.getItemAt(0));
			default_for_peer = __("Default for peer");
			dirList.addItem(default_for_peer);
			this.cascadeBtm.setEnabled(true);
			this.cascadeDtm.setEnabled(true);
			this.cascadePtm.setEnabled(true);
			this.peerTopicTxt.setEnabled(true);
		}else{
			this.cascadeBtm.setEnabled(false);
			this.cascadeDtm.setEnabled(false);			
			this.cascadePtm.setEnabled(false);
			this.peerTopicTxt.setEnabled(false);			
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
    	queryInstance();
    	queryToken();	
    	termsTable.getModel().setDirAddr(dirAddr);
		if(DEBUG) System.out.println("TermsPanel:setPeerID:call update");
    	termsTable.getModel().update(null,null);
    	termsTable.setModel(termsTable.getModel());
    	termsTable.repaint();
    	termsTableScroll.repaint();
    }
    public void showJFrame(){
   		final JFrame frame = new JFrame();
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
	public void update_peer(D_Peer peer, String my_peer_name, boolean me, boolean selected) {
		if (DEBUG) System.out.println("TermsPanel:update:set my="+my_peer_name);
		if (peer != null) {
			if (DEBUG) System.out.println("TermsPanel:update:set="+peer.component_basic_data.name);
			this.setPeerID(peer.getLID());
		}else{
			if (DEBUG) System.out.println("TermsPanel:update:set"+null);
		}
	}
}
