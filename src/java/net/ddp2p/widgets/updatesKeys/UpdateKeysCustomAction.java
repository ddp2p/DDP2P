package net.ddp2p.widgets.updatesKeys;


import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.updates.ImportData;
import net.ddp2p.widgets.updates.UpdateCustomAction;
import net.ddp2p.widgets.updates.UpdatesModel;
import net.ddp2p.widgets.updates.UpdatesTable;

public class UpdateKeysCustomAction extends DebateDecideAction {
	public static final int M_DELORG = 2;
	public static final int M_DELMOT = 3;
	public static final int M_ADD = 1;

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;

	public static final int M_IMPORT = 4;
	public static final int M_DELETE = 5;
	UpdatesKeysTable tree; ImageIcon icon; int cmd;
	public UpdateKeysCustomAction(UpdatesKeysTable tree,
			String text, ImageIcon icon,
			String desc, String whatis,
			Integer mnemonic, int cmd) {
		super(text, icon, desc, whatis, mnemonic);
		this.tree = tree; this.icon = icon; this.cmd = cmd;
	}
	public void actionPerformed(ActionEvent e) {
		if(_DEBUG) System.out.println("UpdatesKeysCustomAction: start");
		Object src = e.getSource();
		JMenuItem mnu;
		int row = -1;
		//String org_id = null;
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
		UpdatesKeysModel model = tree.getModel();
		String tester_ID = Util.getStringID(model.get_UpdatesKeysID(row));
//		data.D_UpdatesKeysInfo uki = model.get_UpdatesKeysInfo(row);

		if(_DEBUG) System.out.println("UpdatesKeysCustomAction: row = "+row);
		if(cmd == M_DELETE) {
			if(row>=0)
				try {
					Application.getDB().deleteNoSync(net.ddp2p.common.table.tester.TNAME,
							new String[]{net.ddp2p.common.table.tester.tester_ID},
							new String[]{tester_ID}, _DEBUG);
					
//					Application.db.deleteNoSync(table.tester.TNAME,
//							new String[]{table.tester.public_key},
//							new String[]{uki.public_key}, _DEBUG);
			    Application.getDB().sync(new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.tester.TNAME)));
				model.update(null,null);
				((UpdatesKeysTable)tree).repaint();	
				} catch (Exception e1) {
					e1.printStackTrace();
				}
		}
		if(cmd == M_IMPORT) {
			JFileChooser chooser = new JFileChooser();
			//				chooser.setCurrentDirectory(new java.io.File("."));
			//				chooser.setDialogTitle("choosertitle");
			//				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			//				chooser.setAcceptAllFileFilterUsed(false);
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				//	System.out.println("getCurrentDirectory(): " + chooser.getCurrentDirectory());
				//	System.out.println("getSelectedFile() : " + chooser.getSelectedFile());
				ImportData importData = new ImportData();
				if(tree instanceof UpdatesKeysTable){
					importData.importTesterFromFile(chooser.getSelectedFile());
					((UpdatesKeysTable)tree).getModel().refresh();
					((UpdatesKeysTable)tree).repaint();
				}

			} else {
				System.out.println("UpdatesKeysCustomAction:No Selection ");
			}


		}
	}
}

