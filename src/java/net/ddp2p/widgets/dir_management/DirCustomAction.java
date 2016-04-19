package net.ddp2p.widgets.dir_management;
import static net.ddp2p.common.util.Util.__;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_DirectoryServerSubscriberInfo;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.components.DebateDecideAction;
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
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    	} else {
    		row=tree.getSelectedRow();
       		row=tree.convertRowIndexToModel(row);
    	}
    	DirModel model = tree.getModel();
    	String subscriber_ID = Util.getStringID(model.getSubscriberID(row));
    	if(DEBUG) System.out.println("DirAction: row = "+row);
        if(cmd == M_DELETE) {
				try {
					model.db.delete(net.ddp2p.common.table.subscriber.TNAME,
							new String[]{net.ddp2p.common.table.subscriber.subscriber_ID},
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
