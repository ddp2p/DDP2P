package widgets.dir_fw_terms;

import static util.Util._;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;
import util.Util;
import config.Application;
import hds.DebateDecideAction;
import hds.DirectoryRequest;
/*import data.D_UpdatesInfo;
import data.D_TesterDefinition;
import data.D_UpdatesKeysInfo;
*/

import util.P2PDDSQLException;


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
    		Application.warning(_("Select a peer first!"), _("Select a peer first!"));
    		return;
    	}
    	if(DEBUG) System.out.println("TermsAction: row = "+row);
    	//do_cmd(row, cmd);
        if(cmd == M_DELETE) {
        	if((row>=0)&&(term_ID!=null)){
        		int priority= tree.getModel().getPriority(row);
        		//System.out.println("data size before delete: "+tree.getModel().getRowCount());
				try {
					Application.db.delete(table.directory_forwarding_terms.TNAME,
							new String[]{table.directory_forwarding_terms.term_ID},
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
    		try {
				Application.db.insert(table.directory_forwarding_terms.TNAME,
						new String[]{table.directory_forwarding_terms.peer_ID,table.directory_forwarding_terms.dir_addr, table.directory_forwarding_terms.priority, table.directory_forwarding_terms.service,table.directory_forwarding_terms.priority_type},
						new String[]{""+tree.getModel()._peerID, tree.getModel()._dirAddr, ""+(tree.getModel().getLastPriority()+1 ), ""+0, ""+0}, DEBUG);
				tree.getModel().update(null,null);
				tree.repaint();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
    	
    	if(cmd == M_UP) {
    		try{
    			 DirectoryRequest dr = new DirectoryRequest("1111", "9999", 5555, "1", "163.118.78.40:25123");
			     byte[] msg = dr.encode();
			     DirectoryRequest dr2 =  new DirectoryRequest();
			     ASN1.Decoder dr3 = new ASN1.Decoder(msg);
			     dr=(DirectoryRequest)dr2.decode(dr3);
			     if(DEBUG)
			     	System.out.println("......................: "+dr);
    		}catch(Exception ee){
    			System.out.println("Exception in M_UP : ");
    			ee.printStackTrace();
    		}
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

