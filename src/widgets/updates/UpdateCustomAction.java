package widgets.updates;

import static util.Util._;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.Point;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import util.DBInterface;
import util.Util;
import widgets.components.BulletRenderer;
import config.Application;
import config.Identity;
import config.DDIcons;
import hds.DebateDecideAction;
import data.D_UpdatesInfo;
import data.D_TesterDefinition;
import data.D_UpdatesKeysInfo;

import com.almworks.sqlite4java.SQLiteException;

import widgets.updatesKeys.UpdatesKeysTable;



@SuppressWarnings("serial")
public class UpdateCustomAction extends DebateDecideAction {
	
    public static final int M_DELORG = 2;
	public static final int M_DELMOT = 3;
	public static final int M_ADD = 1;
	
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    
	public static final int M_IMPORT = 4;
	public static final int M_DELETE = 5;
	UpdatesTable tree; ImageIcon icon; int cmd;
	private JTable subTable;
    public UpdateCustomAction(UpdatesTable tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this.cmd = cmd;
        this.subTable = tree.getSubTable();
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.out.println("ImportAction: start");
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	String org_id=null;
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
    	UpdatesModel model = tree.getModel();
    	String updates_ID = Util.getStringID(model.get_UpdatesID(row));
    	
    	if(DEBUG) System.out.println("ImportAction: row = "+row);
    	//do_cmd(row, cmd);
        if(cmd == M_DELETE) {
        	if((row>=0)&&(updates_ID!=null))
				try {
					Application.db.delete(table.updates.TNAME,
							new String[]{table.updates.updates_ID},
							new String[]{updates_ID}, _DEBUG);
    	    	    ((UpdatesTable)tree).repaint();
    	    	    if(subTable!= null) subTable.repaint();
				} catch (SQLiteException e1) {
					e1.printStackTrace();
				}
    	}
    	if(cmd == M_IMPORT) {
    		JFileChooser chooser = new JFileChooser();
//			chooser.setCurrentDirectory(new java.io.File("."));
//			chooser.setDialogTitle("choosertitle");
//			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//			chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
  			//	System.out.println("getCurrentDirectory(): " + chooser.getCurrentDirectory());
  			//	System.out.println("getSelectedFile() : " + chooser.getSelectedFile());
  				ImportData importData = new ImportData();
  				if(tree instanceof UpdatesTable){
    	    		importData.importMirrorFromFile(chooser.getSelectedFile());
    	    		((UpdatesTable)tree).getModel().refresh();
    	    	    ((UpdatesTable)tree).repaint();
    	    	    if(subTable!= null) subTable.repaint();
    	    		
  				}    	    	
			} else {
  				System.out.println("No Selection ");
			}
    	
        		
    	}
    }
}

