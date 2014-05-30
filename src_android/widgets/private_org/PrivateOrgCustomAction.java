package widgets.private_org;

import static util.Util._;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;
import util.Util;
import config.Application;
import data.D_OrgDistribution;
import hds.DirectoryRequest;
/*import data.D_UpdatesInfo;
import data.D_TesterDefinition;
import data.D_UpdatesKeysInfo;
*/

import util.P2PDDSQLException;
import widgets.components.DebateDecideAction;


@SuppressWarnings("serial")
public class PrivateOrgCustomAction extends DebateDecideAction {
	public static final int M_ADD = 1;
    public static final int M_DELETE = 2;
	public static final int M_UP = 3;
    public static final int M_DOWN = 4;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    
	

	PrivateOrgTable tree; ImageIcon icon; int cmd;
    public PrivateOrgCustomAction(PrivateOrgTable tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this.cmd = cmd;
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.out.println("PrivateOrgCustomAction: start");
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
    	PrivateOrgModel model = tree.getModel();
    	String privateOrg_ID = Util.getStringID(model.get_PrivateOrgID(row));
    	String peer_ID = Util.getStringID(model.get_PeerID(row));
    	String emails = model.get_Emails(row);
    	String org_ID = Util.getStringID(model.get_OrgID(row));
//    	if(model.peerID==-1){
//    		Application.warning(_("Select a peer first!"), _("Select a peer first!"));
//    		return;
//    	}
    	if(DEBUG) System.out.println("PrivateOrgAction: row = "+row);
    	//do_cmd(row, cmd);
        if(cmd == M_DELETE) {
        	try {
				D_OrgDistribution.del(org_ID, peer_ID);
			} catch (P2PDDSQLException e2) {
				e2.printStackTrace();
			}
			//tree.getModel().update(null,null);
			//((PrivateOrgTable)tree).repaint();
    	}
    	if(cmd == M_ADD) {
    		
    	}
    	
    	if(cmd == M_UP) {
    		
    	}
    	
    	if(cmd == M_DOWN) {
			
    	}
    }
}

