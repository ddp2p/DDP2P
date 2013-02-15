package widgets.updates;

import static util.Util._;
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

public class ImportData {
	private static final boolean DEBUG = false;
	public static final String START = "START";
	public static final String STOP = "END";
	public static final String TESTERINFO_START = "TESTERINFO_START";
	public static final String TESTERINFO_STOP = "TESTERINFO_END";
	
	public void importMirrorFromFile(File f){
			//boolean DEBUG = true;
		BufferedReader in =null;
		try{in = new BufferedReader(new FileReader(f));} catch(IOException e){}
		ArrayList<D_TesterDefinition> testerDef = new ArrayList<D_TesterDefinition>();
		D_UpdatesInfo result = new D_UpdatesInfo();
	//	ArrayList<Downloadable> datas = new ArrayList<Downloadable>();
		//String inputLine = "";
		String tmp_inputLine = null;
		int line = 0;
		try {
			D_TesterDefinition t=null;
			while ((tmp_inputLine = in.readLine()) != null && !tmp_inputLine.equals(STOP) ) {
				//inputLine += tmp_inputLine;
				if(DEBUG)System.out.println("ImportUpdatesMirror gets: "+tmp_inputLine);
				tmp_inputLine = tmp_inputLine.trim();
				if((line==0)&&(!START.equals(tmp_inputLine))) continue;
				switch(line)
				{
				case 0: line++; break; //START
				case 1: result.original_mirror_name = tmp_inputLine; line++; break;
				case 2: result.url = tmp_inputLine; line++; break;
				case 3: if(tmp_inputLine.equals(STOP)) continue ;
					    if(tmp_inputLine.equals(TESTERINFO_START)){
					       t= new D_TesterDefinition();	
					       line++;break;	
					    } else continue;
				case 4: t.name = tmp_inputLine; line++; break;
				case 5: t.public_key = tmp_inputLine; line++; break;
				case 6: t.email = tmp_inputLine; line++; break;
				case 7: t.url = tmp_inputLine; line++; break;
				case 8: if(tmp_inputLine.equals("DESC")){ t.description="";line++; break;} else continue;
				case 9: if(tmp_inputLine.equals("DESC")){ line++; break;} 
					     else t.description +=tmp_inputLine;
			    case 10: if(tmp_inputLine.equals(TESTERINFO_STOP)){
			    	     if(DEBUG) System.out.println("Tester_STOP");
			    			try{	
				    			t.store();
				    			testerDef.add(t);
				    			D_UpdatesKeysInfo ukeys = new D_UpdatesKeysInfo();
				    			ukeys.original_tester_name = t.name;
				    			ukeys.public_key = t.public_key;
				    			ukeys.public_key_hash = Util.getGIDhashFromGID(t.public_key, false);
				    			if(ukeys.existsInDB()) ukeys.store("update"); 
				    			else ukeys.store("insert");
				    			line=3;
				    			Application.db.sync(new ArrayList<String>(Arrays.asList(table.updatesKeys.TNAME)));
			    			}catch(SQLiteException e){}
			             }
				}//switch
			}// loop
			if(testerDef.size()!=0)result.testerDef = testerDef.toArray(new D_TesterDefinition[0]);
			
			if(result.existsInDB()) result.store("update"); 
			else result.store("insert");
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}catch ( SQLiteException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("Mirror Import Done :  "+result);
		
	//	return result;
	}
	public void importTesterFromFile(File f){
			//boolean DEBUG = true;
		BufferedReader in =null;
		try{in = new BufferedReader(new FileReader(f));} catch(IOException e){}
		String tmp_inputLine = null;
		D_TesterDefinition t=new D_TesterDefinition();
		int line = 0;
		try {
			while ((tmp_inputLine = in.readLine()) != null ) {
				//inputLine += tmp_inputLine;
				if(DEBUG)System.out.println("ImportUpdatesMirror gets: "+tmp_inputLine);
				tmp_inputLine = tmp_inputLine.trim();
				if((line==0)&&(!TESTERINFO_START.equals(tmp_inputLine))) continue;
				switch(line)
				{
				case 0: line++; break; //TESTERINFO_START
				case 1: t.name = tmp_inputLine; line++; break;
				case 2: t.public_key = tmp_inputLine; line++; break;
				case 3: t.email = tmp_inputLine; line++; break;
				case 4: t.url = tmp_inputLine; line++; break;
				case 5: if(tmp_inputLine.equals("DESC")){ t.description="";line++; break;} else continue;
				case 6: if(tmp_inputLine.equals("DESC")){ line++; break;} 
					     else t.description +=tmp_inputLine;
			    case 7: if(tmp_inputLine.equals(TESTERINFO_STOP)){
			    	     if(DEBUG) System.out.println("Tester_STOP");
			    			try{	
				    			t.store();
				    			D_UpdatesKeysInfo ukeys = new D_UpdatesKeysInfo();
				    			ukeys.original_tester_name = t.name;
				    			ukeys.public_key = t.public_key;
				    			ukeys.public_key_hash = Util.getGIDhashFromGID(t.public_key, false);
				    			ukeys.store("insert");
				    			line=0;
			    			}catch(SQLiteException e){}
			             }
				}//switch
			}// loop
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("TesterImpor Done:"+t);
	//	return result;
	}
}