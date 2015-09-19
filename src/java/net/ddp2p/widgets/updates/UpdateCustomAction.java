package net.ddp2p.widgets.updates;

import static net.ddp2p.common.util.Util.__;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.Point;

import javax.swing.JOptionPane;
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

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_MirrorInfo;
import net.ddp2p.common.data.D_Tester;
import net.ddp2p.common.table.mirror;
import net.ddp2p.common.updates.ClientUpdates;
import net.ddp2p.common.updates.VersionInfo;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.BulletRenderer;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.updatesKeys.UpdatesKeysTable;



@SuppressWarnings("serial")
public class UpdateCustomAction extends DebateDecideAction {
	
    public static final int M_DELORG = 2;
	public static final int M_DELMOT = 3;
	public static final int M_ADD = 1;
	
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
    
	public static final int M_IMPORT = 4;
	public static final int M_DELETE = 5;
	public static final int USE_UPDATE = 6;
	
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
    	String mirror_ID = Util.getStringID(model.get_UpdatesID(row));
    	
    	if(DEBUG) System.out.println("ImportAction: row = "+row);
    	//do_cmd(row, cmd);
        if(cmd == M_DELETE) {
        	if((row>=0)&&(mirror_ID!=null))
				try {
					Application.getDB().delete(net.ddp2p.common.table.mirror.TNAME,
							new String[]{net.ddp2p.common.table.mirror.mirror_ID},
							new String[]{mirror_ID}, _DEBUG);
					Application.getDB().sync(new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.mirror.TNAME)));
					model.update(null,null);
					
    	    	    ((UpdatesTable)tree).repaint();
    	    	    if(subTable!= null) subTable.repaint();
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
				}
    	}

    	if(cmd == USE_UPDATE) {
        	if((row>=0)&&(mirror_ID!=null)){
        		String mirror_URL = model.get_UpdatesURL(row);
        		String updates_LastVer = model.get_UpdatesLastVer(row);
        		if(updates_LastVer == null){
        			Application_GUI.warning(Util.__("No update available from url: "+mirror_URL), Util.__("No update available"));
        			//System.out.println("No update from url: "+updates_URL);
        		}
        		else
        		{   
        			ClientUpdates cu = new ClientUpdates();
        			VersionInfo newest_version_obtained=cu.getNewerVersionInfos(mirror_URL, updates_LastVer);
        			if(newest_version_obtained!=null) {
						try {
							if(cu.downloadNewer(newest_version_obtained))
								return;
						} catch (Exception ee) {
							ee.printStackTrace();
						}
        			}
        		}
        			System.out.println("url: "+mirror_URL+" ver: "+updates_LastVer);
        	}

    	}

//    	if(cmd == USE_UPDATE) {
//        	if((row>=0)&&(mirror_ID!=null)){
//        		String mirror_URL = model.get_UpdatesURL(row);
//        		String updates_LastVer = model.get_UpdatesLastVer(row);
//        		long mirrorID =model.get_UpdatesID(row);
//        		if(updates_LastVer == null){
//        			Application.warning(Util._("No update available from url: "+mirror_URL), Util._("No update available"));
//        			//System.out.println("No update from url: "+mirror_URL);
//        		}
//        		else
//        		{   
//        			ClientUpdates cu = new ClientUpdates();
//        			String sql = "SELECT "+mirror.last_version_info+" FROM "+mirror.TNAME+
//        				         " WHERE "+ mirror.mirror_ID+" =? ;";
//					String[]params = new String[]{mirrorID+""};// where clause?
//					ArrayList<ArrayList<Object>> u;
//					try {
//						u = Application.db.select(sql, params, DEBUG);
//					} catch (P2PDDSQLException ex) {
//						ex.printStackTrace();
//						return;
//					}
//					if((u==null || u.get(0)==null) || u.get(0).get(0)==null)
//					{System.out.println("No version in the database!!");
//					 return;
//					}
//					
//				    byte[] bytes=util.Base64Coder.decode((String)  u.get(0).get(0) );
//        			// use ASN1 instead of using Serializable
//        			VersionInfo newest_version_obtained= null;//VersionInfo.getVersionInfoFromSerializableBytes(bytes);//cu.getNewerVersionInfos(updates_URL, updates_LastVer);
//        			if(newest_version_obtained!=null) {
//        				if(Util.isVersionNewer(DD.VERSION, newest_version_obtained.version ))
//        				{
//        					String def = _("Keep current version");
//							Object[] options = new Object[]{def, _("Download old version")};
//        					int c = Application.ask(
//							             Util._("Old version:"), "Version: "+newest_version_obtained.version+
//								         ": is an old version comparing with the current version: "+DD.VERSION,
//							//JOptionPane.OK_CANCEL_OPTION,
//							options,
//							def,
//							null
//							);
//							if(c==JOptionPane.CLOSED_OPTION || c==0) {
//								return;
//							}
//        				}
//        					
//        					
//						try {
//							if(ClientUpdates.downloadNewer(newest_version_obtained))
//								return;
//						} catch (Exception ee) {
//							ee.printStackTrace();
//						}
//        			}
//        		}
//        		//	System.out.println("url: "+updates_URL+" ver: "+updates_LastVer);
//        	}
//
//    	}

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

