/**
 * Khalid Alhamed
 */
package net.ddp2p.widgets.dir_management;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.Enumeration;

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

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.PeerListener;
import net.ddp2p.common.data.D_DirectoryServerSubscriberInfo;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.DirMessage;
import net.ddp2p.common.hds.DirectoryMessageStorage;
import net.ddp2p.common.table.peer_address;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

import java.util.ArrayList; 

//import table.directory_forwarding_dir;// changed
import static net.ddp2p.common.util.Util.__;

@SuppressWarnings("serial")
public class DirPanel extends JPanel implements  ActionListener{

	private static final boolean _DEBUG = false;
    static boolean DEBUG = false;
    
	
    public DirPanel() {
    	super( new GridLayout(3,1));
    	//super( new BorderLayout());
    	init();
    }
    public DirPanel(int peerID, String peerName) {
    	//super( new BorderLayout());
    	super( new GridLayout(3,1));
    	init();
    }
    public JPanel getHeaderPanel(String title) {
    	JLabel dirTitleL = new JLabel(__(title));
    	dirTitleL.setFont(new Font("Times New Roman",Font.BOLD,18));
    	JPanel headerPanel = new JPanel();
    	headerPanel.add(dirTitleL); 
    	return headerPanel;
    }
    public JPanel getDirTablePanel() {
    	JPanel panel = new JPanel(new BorderLayout());
    	panel.add(getHeaderPanel("Directory Management"), BorderLayout.NORTH);
    	DirTable dirTable = null;
	    dirTable = new DirTable(Application.getDB_Dir(), this);
	    JScrollPane dirTableScroll;
	    dirTableScroll = dirTable.getScrollPane();
	    panel.add(dirTableScroll);
	    addMouseListener(dirTable);
		dirTableScroll.addMouseListener(dirTable); 
		//addMouseListener(this);
	    return panel;
    }
    public PeerStatusTable peerStatusTable = null;
    public JPanel getPeerStatusTablePanel() {
    	JPanel panel = new JPanel(new BorderLayout());
    	JButton peerStatus = new JButton("Peer Status");
    	peerStatus.setActionCommand(peerStatus.getLabel());
    	peerStatus.addActionListener(this);
    	//panel.add(getHeaderPanel("Peer Status"), BorderLayout.NORTH);
    	panel.add(peerStatus, BorderLayout.NORTH);
	    peerStatusTable = new PeerStatusTable(Application.getDB_Dir(), this);
	    JScrollPane peerStatusTableScroll;
    	peerStatusTableScroll = peerStatusTable.getScrollPane();
	    panel.add(peerStatusTableScroll); 
	    return panel;
    }
     public ConnPeerTable connPeerTable = null;
     public JPanel getConnPeerTablePanel() {
    	JPanel panel = new JPanel(new BorderLayout());
    	panel.add(getHeaderPanel("Connected Peers"), BorderLayout.NORTH);
    		
	    connPeerTable = new ConnPeerTable(Application.getDB_Dir(), this);
	    JScrollPane connPeerTableScroll;
    	connPeerTableScroll = connPeerTable.getScrollPane();
	    panel.add(connPeerTableScroll); 
	    return panel;
    }
     public HistoryTable historyTable = null;
     public JPanel getHistoryTablePanel() {
    	JPanel panel = new JPanel(new BorderLayout());
    	panel.add(getHeaderPanel("History of Messages"), BorderLayout.NORTH);
    		
	    historyTable = new HistoryTable(Application.getDB_Dir(), this);
	    JScrollPane historyTableScroll;
    	historyTableScroll = historyTable.getScrollPane();
	    panel.add(historyTableScroll); 
	    return panel;
    }
    @Override
	public void actionPerformed(ActionEvent e) {
    	try{_actionPerformed(e);}
    	catch(Exception o){o.printStackTrace();}
    }
    
	public void _actionPerformed(ActionEvent e) throws P2PDDSQLException {
		if(e.getActionCommand().equals("Peer Status")){
			peerStatusTable.getModel().update(null,null);
			peerStatusTable.repaint();
			if(_DEBUG){
				 System.out.println("Call update(): peerStatusTable");
				 if(DirectoryMessageStorage.latestRequest_storage==null)
				 	System.out.println("DirectoryMessageStorage.latestRequest_storage = null");
				 else{
					 	 System.out.println("DirectoryMessageStorage.latestRequest_storage.size = "+
					 		                 DirectoryMessageStorage.latestRequest_storage.size());
					 	 Enumeration keys = DirectoryMessageStorage.latestRequest_storage.keys();
					 	 while (keys.hasMoreElements()) {
					 	 	 String GID = (String)keys.nextElement(); 
	        				 System.out.println("GID_HASH:"+Util.getGIDhash(GID)+" masseges NO:"+
	        				                     DirectoryMessageStorage.latestRequest_storage.get(GID).MsgType);
	      				 }
				 	}
				 
				 if(DirectoryMessageStorage.noping_storage==null)
				 	System.out.println("DirectoryMessageStorage.noping_storage = null");
				 else{
					 	System.out.println("DirectoryMessageStorage.noping_storage.size = "+
					 		DirectoryMessageStorage.noping_storage.size());
					 	Enumeration keys = DirectoryMessageStorage.noping_storage.keys();
						 	 while (keys.hasMoreElements()) {
						 	 	 String GID = (String) keys.nextElement(); 
		        				 System.out.println("GID_HASH:"+Util.getGIDhash(GID)+" masseges NO:"+
		        				                     DirectoryMessageStorage.noping_storage.get(GID).size());
		      				 }
				 	}
				 		
				 if(DirectoryMessageStorage.announcement_storage==null)
				 	System.out.println("DirectoryMessageStorage.announcement_storage = null");
				 else{
					 	System.out.println("DirectoryMessageStorage.announcement_storage.size = "+
					 		DirectoryMessageStorage.announcement_storage.size());
					 	Enumeration keys = DirectoryMessageStorage.announcement_storage.keys();
						while (keys.hasMoreElements()) {
							String GID = (String) keys.nextElement(); 
							System.out.println("GID_HASH:"+Util.getGIDhash(GID)+" masseges NO:"+
						                 DirectoryMessageStorage.announcement_storage.get(GID).size());
						}
				 	}
				 
				 if(DirectoryMessageStorage.ping_storage==null)
				 	System.out.println("DirectoryMessageStorage.ping_storage = null");
				 else{
					 	System.out.println("DirectoryMessageStorage.ping_storage.size = "+
					 		DirectoryMessageStorage.ping_storage.size());
					 	Enumeration keys = DirectoryMessageStorage.ping_storage.keys();
						while (keys.hasMoreElements()) {
						 String GID = (String) keys.nextElement(); 
						 System.out.println("GID_HASH:"+Util.getGIDhash(GID)+" masseges NO:"+
						                     DirectoryMessageStorage.ping_storage.get(GID).size());
						}
				 	}
			} 
		}
	}	
	public JPanel peerStatusTablePanel;
	public JPanel historyTablePanel;
	public void init(){
		if(Application.getDB_Dir() == null){
    		Application.DB_PATH = new File(Application.DELIBERATION_FILE).getParent();
    		Application.setDB_Dir(DD.load_Directory_DB(Application.DB_PATH));
		}
    	add(getDirTablePanel());
    	JPanel twoPanelsInRow1 = new JPanel(new GridLayout(1,2));
    	peerStatusTablePanel = getPeerStatusTablePanel(); 
    	twoPanelsInRow1.add(peerStatusTablePanel);
    //	twoPanelsInRow1.add(getConnPeerTablePanel());
    	add(twoPanelsInRow1);
    	JPanel twoPanelsInRow2 = new JPanel(new GridLayout(1,2));
    	historyTablePanel = getHistoryTablePanel();
    	twoPanelsInRow2.add(historyTablePanel);
    	add(twoPanelsInRow2);
 //   	twoPanelsInRow2.add(getConnPeerTablePanel());
    	
//    	connPeerTable = new ConnPeerDirTable(Application.db_dir, this);
//    	connPeerTableScroll = conPeerTable.getScrollPane();
//    	historyTable = new HistoryTable(Application.db_dir, this);
//    	historyTableScroll = historyTable.getScrollPane();
    	
 //   	bodyPanel.add(peerStatusPanel);
//    	bodyPanel.add(connPeerTableScroll);
//    	bodyPanel.add(historyTableScroll);
 //   	add(bodyPanel);
		
    }
   
    public void showJFrame(){
   		final JFrame frame = new JFrame();
   		//DirPanel dirPanel = new DirPanel();
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
    	public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.setDB(new DBInterface(Application.DEFAULT_DELIBERATION_FILE));
			DirPanel dirPanel = new DirPanel();
			frame.setContentPane(dirPanel);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setSize(800,300);
			frame.setVisible(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*	public JTable getTestersTable() {
			return this.updateKeysTable;
		}*/
	
    
}