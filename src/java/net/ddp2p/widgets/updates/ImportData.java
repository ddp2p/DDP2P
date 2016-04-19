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
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_MirrorInfo;
import net.ddp2p.common.data.D_Tester;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.BulletRenderer;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.updatesKeys.UpdatesKeysTable;
public class ImportData {
	private static final boolean DEBUG = false;
	public static final String START = "START";
	public static final String STOP = "END";
	public static final String TESTERINFO_START = "TESTERINFO_START";
	public static final String TESTERINFO_STOP = "TESTERINFO_END";
	/**
	 * Check with Khalid. It seems to be more about testers than about mirrors.
	 * @param f
	 */
	public void importMirrorFromFile(File f){
		BufferedReader in =null;
		try{in = new BufferedReader(new FileReader(f));} catch(IOException e){}
		ArrayList<D_Tester> testerDef = new ArrayList<D_Tester>();
		D_MirrorInfo result = new D_MirrorInfo();
		String tmp_inputLine = null;
		int line = 0;
		try {
			D_Tester t=null;
			while ((tmp_inputLine = in.readLine()) != null && !tmp_inputLine.equals(STOP) ) {
				if(DEBUG)System.out.println("ImportUpdatesMirror gets: "+tmp_inputLine);
				tmp_inputLine = tmp_inputLine.trim();
				if((line==0)&&(!START.equals(tmp_inputLine))) continue;
				switch(line)
				{
				case 0: line++; break; 
				case 1: result.original_mirror_name = tmp_inputLine; line++; break;
				case 2: result.url = tmp_inputLine; line++; break;
				case 3: if(tmp_inputLine.equals(STOP)) continue ;
					    if(tmp_inputLine.equals(TESTERINFO_START)){
					       t= new D_Tester();	
					       line++;break;	
					    } else continue;
				case 4: t.name = tmp_inputLine; line++; break;
				case 5: t.testerGID = tmp_inputLine; line++; break;
				case 6: t.email = tmp_inputLine; line++; break;
				case 7: t.url = tmp_inputLine; line++; break;
				case 8: if(tmp_inputLine.equals("DESC")){ t.description="";line++; break;} else continue;
				case 9: if(tmp_inputLine.equals("DESC")){ line++; break;} 
					     else t.description +=tmp_inputLine;
			    case 10: if(tmp_inputLine.equals(TESTERINFO_STOP)){
			    	     if(DEBUG) System.out.println("Tester_STOP");
			    			try{	
				    			testerDef.add(t);
				    			D_Tester ukeys = new D_Tester();
				    			ukeys.name = t.name;
				    			ukeys.testerGID = t.testerGID;
				    			ukeys.testerGIDH = Util.getGIDhashFromGID(t.testerGID, false);
				    			ukeys.email=t.email;
				    			ukeys.url=t.url;
				    			ukeys.description = t.description;
				    			ukeys.store();
				    			line=3;
				    			Application.getDB().sync(new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.tester.TNAME)));
			    			}catch(P2PDDSQLException e){}
			             }
				}
			}
			if(testerDef.size()!=0)result.testerDef = testerDef.toArray(new D_Tester[0]);
			if(result.existsInDB(Application.getDB())) result.store(D_MirrorInfo.action_update); 
			else result.store(D_MirrorInfo.action_insert);
		} catch (IOException e) {
			e.printStackTrace();
		}catch ( P2PDDSQLException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("Mirror Import Done :  "+result);
	}
	public void importTesterFromFile(File f){
		BufferedReader in =null;
		try{in = new BufferedReader(new FileReader(f));} catch(IOException e){}
		String tmp_inputLine = null;
		D_Tester t=new D_Tester();
		int line = 0;
		try {
			while ((tmp_inputLine = in.readLine()) != null ) {
				if(DEBUG)System.out.println("ImportUpdatesMirror gets: "+tmp_inputLine);
				tmp_inputLine = tmp_inputLine.trim();
				if((line==0)&&(!TESTERINFO_START.equals(tmp_inputLine))) continue;
				switch(line)
				{
				case 0: line++; break; 
				case 1: t.name = tmp_inputLine; line++; break;
				case 2: t.testerGID = tmp_inputLine; line++; break;
				case 3: t.email = tmp_inputLine; line++; break;
				case 4: t.url = tmp_inputLine; line++; break;
				case 5: if(tmp_inputLine.equals("DESC")){ t.description="";line++; break;} else continue;
				case 6: if(tmp_inputLine.equals("DESC")){ line++; break;} 
					     else t.description +=tmp_inputLine;
			    case 7: if(tmp_inputLine.equals(TESTERINFO_STOP)){
			    	     if(DEBUG) System.out.println("Tester_STOP");
			    			try{	
				    			t.testerGIDH = Util.getGIDhashFromGID(t.testerGID, false);
				    			t.store();
				    			line=0;
			    			}catch(P2PDDSQLException e){}
			             }
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("TesterImpor Done:"+t);
	}
}
