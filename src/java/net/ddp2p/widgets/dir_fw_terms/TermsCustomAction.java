package net.ddp2p.widgets.dir_fw_terms;

import static net.ddp2p.common.util.Util.__;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.DirectoryRequest;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
/*import data.D_UpdatesInfo;
import data.D_TesterDefinition;
import data.D_UpdatesKeysInfo;
*/

import net.ddp2p.widgets.components.DebateDecideAction;


@SuppressWarnings("serial")
public class TermsCustomAction extends DebateDecideAction {
	public static final int M_ADD = 1;
    public static final int M_DELETE = 2;
	public static final int M_UP = 3;
    public static final int M_DOWN = 4;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    
	

	TermsTable tree; ImageIcon icon; int cmd;
    public TermsCustomAction(TermsTable tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this.cmd = cmd;
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.out.println("TermsCustomAction: start");
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	//String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    		//org_id = Util.getString(act.getValue("org"));
            //System.err.println("row property: " + row);
    	} else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
    		//org_id = tree.getModel().org_id;
    		//System.err.println("Row selected: " + row);
    	}
    	TermsModel model = tree.getModel();
    	String term_ID = Util.getStringID(model.get_TermID(row));
    	if(model.peerID==-1){
    		Application_GUI.warning(__("Select a peer first!"), __("Select a peer first!"));
    		return;
    	}
    	if(DEBUG) System.out.println("TermsAction: row = "+row);
    	//do_cmd(row, cmd);
        if(cmd == M_DELETE) {
        	if((row>=0)&&(term_ID!=null)){
        		int priority= tree.getModel().getPriority(row);
        		//System.out.println("data size before delete: "+tree.getModel().getRowCount());
				try {
					Application.db.delete(net.ddp2p.common.table.directory_forwarding_terms.TNAME,
							new String[]{net.ddp2p.common.table.directory_forwarding_terms.term_ID},
							new String[]{term_ID}, _DEBUG);
				//System.out.println("data size before delete: "+tree.getModel().getRowCount());
					if(priority!= tree.getModel().getLastPriority()+1){
					tree.getModel().shiftByOne(priority);
					}  	    
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
				}
				
				tree.getModel().update(null,null);
				((TermsTable)tree).repaint();
				
        	}
    	}
    	if(cmd == M_ADD) {
    		String dir_domain= null;
    		String dir_port = null;
    		String dirAddr = tree.getModel()._dirAddr;
	    	if(dirAddr!=null){
	    		dir_domain=dirAddr.split(":")[0];
	    		dir_port=dirAddr.split(":")[1];
	    	}
    		
    		try {
				Application.db.insert(net.ddp2p.common.table.directory_forwarding_terms.TNAME,
						new String[]{net.ddp2p.common.table.directory_forwarding_terms.peer_ID,net.ddp2p.common.table.directory_forwarding_terms.peer_instance_ID,net.ddp2p.common.table.directory_forwarding_terms.dir_domain,net.ddp2p.common.table.directory_forwarding_terms.dir_tcp_port, net.ddp2p.common.table.directory_forwarding_terms.priority, net.ddp2p.common.table.directory_forwarding_terms.service,net.ddp2p.common.table.directory_forwarding_terms.priority_type},
						new String[]{""+tree.getModel()._peerID,""+tree.getModel()._selectedInstanceID, dir_domain, dir_port, ""+(tree.getModel().getLastPriority()+1 ), ""+0, ""+0}, DEBUG);
				tree.getModel().update(null,null);
				tree.repaint();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
    	
    	if(cmd == M_UP) {
//    		try{
//    			 DirectoryRequest dr = 
//    					 new DirectoryRequest("1111", "9999", 5555, "1", new Address("163.118.78.40:25123"));
//			     byte[] msg = dr.encode();
//			     DirectoryRequest dr2 =  new DirectoryRequest();
//			     ASN1.Decoder dr3 = new ASN1.Decoder(msg);
//			     dr=(DirectoryRequest)dr2.decode(dr3);
//			     if(DEBUG)
//			     	System.out.println("......................: "+dr);
//    		}catch(Exception ee){
//    			System.out.println("Exception in M_UP : ");
//    			ee.printStackTrace();
//    		}
    		try{
    	    	if(row>=0 && tree.getModel().getPriority(row)>1){
    	    		 tree.getModel().swap(row,row-1);
    	    		tree.setRowSelectionInterval(row-1,row-1);
    	    	}
    	    } catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	    tree.repaint();
    	}
    	
    	if(cmd == M_DOWN) {
			try{
    	    	if(row>=0 && tree.getModel().getPriority(row)<tree.getModel().getLastPriority()){
    	    		 tree.getModel().swap(row,row+1);
    	    		 tree.setRowSelectionInterval(row+1,row+1);
    	    		//System.out.println("row: "+ row);
    	    	}
    	    } catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	    tree.repaint();
    	}
    }
}

