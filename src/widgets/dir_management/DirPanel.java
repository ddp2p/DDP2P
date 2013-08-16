/**
 * Khalid Alhamed
 */
package widgets.dir_management;

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
import java.io.File;

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
import data.D_SubscriberInfo;
import util.DBInterface;
import util.Util;
import widgets.peers.PeerListener;
//import table.directory_forwarding_dir;// changed
import table.peer_address;
import static util.Util._;

@SuppressWarnings("serial")
public class DirPanel extends JPanel implements  ActionListener{

	private static final boolean _DEBUG = false;
    static boolean DEBUG = false;
    JScrollPane dirTableScroll;
	
    public DirPanel() {
    	super( new BorderLayout());
    	init();
    }
    public DirPanel(int peerID, String peerName) {
    	super( new BorderLayout());
    	init();
    }
    public JPanel getHeaderPanel() {
	    	JLabel dirTitleL = new JLabel(_("Directory Management"));
	    	dirTitleL.setFont(new Font("Times New Roman",Font.BOLD,18));
	    	JPanel headerPanel = new JPanel();
	    	headerPanel.add(dirTitleL); 
    	return headerPanel;
    }
    @Override
	public void actionPerformed(ActionEvent e) {
    	try{_actionPerformed(e);}
    	catch(Exception o){o.printStackTrace();}
    }
    
	public void _actionPerformed(ActionEvent e) throws P2PDDSQLException {
		
	}	
	
	public void init(){
    	add(getHeaderPanel(), BorderLayout.NORTH);
    	DirTable dirTable = null;

    	if(Application.db_dir == null){
    		Application.DB_PATH = new File(Application.DELIBERATION_FILE).getParent();
    		Application.db_dir = DD.load_Directory_DB(Application.DB_PATH);
    	}
    	
    	dirTable = new DirTable(Application.db_dir, this);
    	dirTableScroll = dirTable.getScrollPane();
    	add(dirTableScroll);
		addMouseListener(dirTable);
		dirTableScroll.addMouseListener(dirTable);
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
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
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