package widgets.dir_management;

import static util.Util.__;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;
import util.Util;
import config.Application;
import data.D_DirectoryServerSubscriberInfo;
/*import data.D_TesterDefinition;
import data.D_UpdatesKeysInfo;
*/

import util.P2PDDSQLException;
import widgets.components.DebateDecideAction;


@SuppressWarnings("serial")
public class DirCustomAction extends DebateDecideAction {
	public static final int M_ADD = 1;
    public static final int M_DELETE = 2;
	public static final int M_UP = 3;
    public static final int M_DOWN = 4;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    
	

	DirTable tree; ImageIcon icon; int cmd;
    public DirCustomAction(DirTable tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this.cmd = cmd;
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.out.println("DirCustomAction: start");
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
    	DirModel model = tree.getModel();
    	String subscriber_ID = Util.getStringID(model.getSubscriberID(row));
//    	if(model.peerID==-1){
//    		Application.warning(_("Select a peer first!"), _("Select a peer first!"));
//    		return;
//    	}
    	if(DEBUG) System.out.println("DirAction: row = "+row);
    	//do_cmd(row, cmd);
        if(cmd == M_DELETE) {
				try {
					model.db.delete(table.subscriber.TNAME,
							new String[]{table.subscriber.subscriber_ID},
							new String[]{subscriber_ID}, _DEBUG);
  	    
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
				}
				
				model.update(null,null);
				tree.repaint();			
        	
    	}
    	if(cmd == M_ADD) {
    		try {
    			D_DirectoryServerSubscriberInfo s = new D_DirectoryServerSubscriberInfo(model.db);
    			s.storeNoSync("insert");
//				model.db.insert(table.Subscriber.TNAME,
//						new String[]{},
//						new String[]{}, DEBUG);
				tree.getModel().update(null,null);
				tree.repaint();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
    	}
    	
    	if(cmd == M_UP) {
    		
    	}
    	
    	if(cmd == M_DOWN) {
			
    	}
	}
}

