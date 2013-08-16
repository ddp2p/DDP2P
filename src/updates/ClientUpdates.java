/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package updates;

import static java.lang.System.out;

import hds.ControlPane;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

import util.Base64Coder;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import WSupdate.HandleService;
import WSupdate.TesterInfo;
import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;

import util.P2PDDSQLException;
import static util.Util._;
import config.Application;
import config.DD;
import data.D_ReleaseQuality;
import data.D_TesterInfo;
import data.D_TesterSignedData;
import data.D_UpdatesInfo;
import data.D_UpdatesKeysInfo;
//http://debatedecide.org/DD_Updates;http://distributedcensus.com/DD_Updates;wsdl:http://andrew.cs.fit.edu/~kalhamed2011/webservices/ddWS_doc3.php?wsdl&123331c847ba3a5d6a52e817a4d0109fe65ffbc2038f68c8db1c3f9793b50c0d
public class ClientUpdates extends Thread{
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	
	public static boolean WARN_WHEN_LESS_THAN_HALF_SOURCES_HAVE_SAME = true;
	public static boolean ASK_BEFORE_STARTING_DOWNLOAD = true;
	public static boolean ASK_BEFORE_RUNNING_DOWNLOAD_SCRIPTS = false;
	
	public static final String START = "START";
	public static final String STOP = "STOP";
	public static final String TESTERS = "TESTERS";
	private static final Object monitor = new Object();
	static public ClientUpdates clientUpdates; // = new ClientUpdates();
	private static int cnt_clients = 0;
	//Socket sock;
	String updateServers;
	boolean run = true;
	//public long milliseconds = 1000*10; // check every 10 minutes for updates
	ArrayList<String> urls = new ArrayList<String>();
	
	public ClientUpdates() {
		this.setDaemon(true);
		initializeServers();
		//sock = new Socket();
	}
	//TODO Khalid : update the arrays "urls"
	// To also call on each change to the testers/mirrors database ask how?
	public boolean initializeServers(){
		try {
		urls = D_UpdatesInfo.getUpdateURLs();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		
		if(DEBUG) 
			for(int i=0; i<urls.size(); i++)
			    System.out.println("ClientUpdates: "+urls.get(i));
		if(urls.size()==0) return false;
		return true;
	}
	@Deprecated
	public boolean _initializeServers(){
		if(DEBUG) out.println("ClientUpdates: initializeServers: start");
		try {
			updateServers = DD.getExactAppText(DD.APP_UPDATES_SERVERS);
			if(DEBUG) out.println("ClientUpdates: initializeServers: got = "+updateServers);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		return initializeServers(updateServers);
	}
	public boolean initializeServers( String updateServers){
		String updateServers_URL[];
		if(DEBUG) out.println("ClientUpdates: initializeServers2: start "+updateServers);
		if(updateServers==null) return false;
		updateServers_URL = updateServers.split(Pattern.quote(DD.APP_UPDATES_SERVERS_URL_SEP));
		if(updateServers_URL.length == 0) return false;
		if(DEBUG) out.println("ClientUpdates: initializeServers2: got #"+updateServers_URL.length);
		for(String u : updateServers_URL) {
			/*
			URL _u;
			try {
				_u = new URL(u);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				continue;
			}
			*/
			if(u != null) urls.add(u);
		}
		if(urls.size()==0) return false;
		return true;
	}
	synchronized public void turnOff(){
		run = false;
		this.notifyAll();
	}
	
	public void run() {
		if(DEBUG)System.out.println("ClientUpdates run: start");

		Calendar crt = Util.CalendargetInstance();
		if( crt.getTimeInMillis() - DD.startTime.getTimeInMillis() < DD.UPDATES_WAIT_ON_STARTUP_MILLISECONDS )
		synchronized (this){
			try {
				this.wait(DD.UPDATES_WAIT_ON_STARTUP_MILLISECONDS);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return;
			}
		}
		EventQueue.invokeLater(new Runnable() {
			public void run(){
				DD.controlPane.startClientUpdates.setText(ControlPane.STOP_CLIENT_UPDATES);
			}
		});
		if(DEBUG)System.out.println("ClientUpdates run: set stop");
		synchronized(monitor){ClientUpdates.cnt_clients++;}
		try{
			_run();
		}catch(Exception e){
			e.printStackTrace();
		}
		synchronized(monitor){
			ClientUpdates.cnt_clients--;
			if(ClientUpdates.cnt_clients == 0){
				EventQueue.invokeLater(new Runnable() {
					public void run(){
						DD.controlPane.startClientUpdates.setText(ControlPane.START_CLIENT_UPDATES);
					}
				});
				if(DEBUG)System.out.println("ClientUpdates run: set start");
			}
		}
		
		if(DEBUG)System.out.println("ClientUpdates run: stopped: "+ClientUpdates.cnt_clients);
	}
	public static PK[] getTrustedKeys(){
		String[] _trusted = new String[0];
		try {
				String __trusted = DD.getAppText(DD.TRUSTED_UPDATES_GID);
				if(__trusted != null) _trusted = __trusted.split(Pattern.quote(DD.TRUSTED_UPDATES_GID_SEP));
				if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: got splits PK: "+_trusted.length);
		} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
		}
		ArrayList<PK> trusted = new ArrayList<PK>();
		for(int k=0; k<_trusted.length; k++){
			if(_trusted[k]==null){
				if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: no key: "+k+" "+_trusted[k]);
				continue;
			}
			if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: got key: "+k+" "+_trusted[k]);
			PK _pk = Cipher.getPK(_trusted[k]);
			if(_pk!=null){
				if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: got key PK: "+k+" "+_pk);
				trusted.add(_pk);
			}
		}
		if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: got size PK: "+trusted.size());
		return trusted.toArray(new PK[0]);
	}

	@Deprecated
	private Hashtable<String, PK> getTrustedKeysHash() {
		String[] _trusted = new String[0];
		Hashtable<String, PK> result = new Hashtable<String, PK>();
		try {
				String __trusted = DD.getAppText(DD.TRUSTED_UPDATES_GID);
				if(__trusted != null) _trusted = __trusted.split(Pattern.quote(DD.TRUSTED_UPDATES_GID_SEP));
				if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: got splits PK: "+_trusted.length);
		} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
		}
		//ArrayList<PK> trusted = new ArrayList<PK>();
		for(int k=0; k<_trusted.length; k++){
			if(_trusted[k]==null) {
				if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: no key: "+k+" "+_trusted[k]);
				continue;
			}
			if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: got key: "+k+" "+_trusted[k]);
			PK _pk = Cipher.getPK(_trusted[k]);
			if(_pk!=null){
				if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: got key PK: "+k+" "+_pk);
				result.put(_trusted[k], _pk);
			}
		}
		if(DEBUG) System.out.println(" ClientUpdates: getTrustedKeys: got size PK: "+result.size());
		return result;
	}
	public static Hashtable<Object,Object> ctx = new Hashtable<Object,Object>();
	
	public void _run() {
		//DEBUG = true;
		boolean starting = true;
		for(;;) {
			synchronized(this){
				try {
					if(!run) return;
					if(!starting)this.wait(DD.UPDATES_WAIT_MILLISECONDS );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(!run) return;
			}
			starting = false;
			
			if(DEBUG) System.out.println(" ClientUpdates: will work on urls #"+urls.size());
			//PK[] trusted = getTrustedKeys();
			//Hashtable<String, PK> trusted_hash = getTrustedKeysHash();
			//if(trusted_hash.size() == 0){
			
			boolean has_trusted_testers = D_UpdatesInfo.hasTrustedTesters();
			if(!has_trusted_testers) {
				Application.warning(Util._("No trusted updates provider key. Please install an update/tester key."), Util._("No trusted updates provider key!"));
				DD.controlPane.setClientUpdatesStatus(false);
				return;
			}
			
			Hashtable<String,VersionInfo> is = getNewerVersionInfos(); // downloads VersionInfo
			
			Hashtable<VersionInfo, Hashtable<String, VersionInfo>> versions = classifyVersionInfo(is);
			
			// Should update database with QoTS and RoTs
			D_UpdatesInfo.store_QoTs_and_RoTs(versions);
			
			if(!DD.UPDATES_AUTOMATIC_VALITATION_AND_INSTALL) continue;
			
			Hashtable<VersionInfo, Hashtable<String, VersionInfo>> valid_versions = D_UpdatesInfo.validateVersionInfo(versions);
			if(valid_versions.size() == 0) continue;
			
			VersionInfo[] _newest_version_obtained = new VersionInfo[1];
			String newest = selectNewest(valid_versions, _newest_version_obtained);
			
			if(newest == null) {
				if(DEBUG)System.out.println("ClientUpdates run: newest ="+newest);
				DD.controlPane.setClientUpdatesStatus(false);
				return;
			}
			
			int cnt_new = valid_versions.size();
			VersionInfo newest_version_obtained = _newest_version_obtained[0]; //getVersionInfoFor(is, newest);
			/*
			if((WARN_WHEN_LESS_THAN_HALF_SOURCES_HAVE_SAME)&&areTestersPassingTrustCondition(is, newest)&&(cnt_new != is.size())) {
				int c = Application.ask(Util._("Quit updating?\nNewest do not match: "+cnt_new+"/"+is.size()), Util._("Newest versions do not match!"),
						JOptionPane.OK_CANCEL_OPTION);
				if(c==0) {
					DD.controlPane.setClientUpdatesStatus(false);
					return;
				}else{
				}
			}
			*/
			
			if(ClientUpdates.ASK_BEFORE_STARTING_DOWNLOAD) {
				String def = _("Download Now");
				Object[] options = new Object[]{def, _("Will inspect available versions in panel")};
				int c = Application.ask(
						Util._("Upgrade available: #")+cnt_new+"/"+is.size()+", version:"+newest_version_obtained.version+".\nYou can download it now or after inspecting its quality in the Updates panel.\n "+Util._("Download now?"),
						Util._("Upgrade available. Download?"),
						//JOptionPane.OK_CANCEL_OPTION
						options,
						def,
						null
						);
				if(c==1) {
					DD.UPDATES_AUTOMATIC_VALITATION_AND_INSTALL = false;
					continue;
					//DD.controlPane.setClientUpdatesStatus(false);
					//return;
				}else{
				}				
			}

			// try downloading from the first selected
			if(newest_version_obtained!=null) {
				try {
					if(downloadNewer(newest_version_obtained))
						continue;
				} catch (Exception e) {
					e.printStackTrace();
				}
				// try downloading from any with same version and content
				//for(VersionInfo a : is.values()) {
				for(VersionInfo a : valid_versions.get(newest_version_obtained).values()) {
					if(!newest.equals(a.version)) continue;
					if(!newest_version_obtained.equals(a)) continue;
					if(a==newest_version_obtained) continue; // this was tested
					try {
						if(downloadNewer(a)) break;
					} catch (Exception e2) {
						e2.printStackTrace();
						continue;
					}
				}
			}
		}
	}
	/**
	 * Return null for abandoning auto-updates
	 * @param valid_versions
	 * @param _newest_version_obtained
	 * @return
	 */
	private String selectNewest(
			Hashtable<VersionInfo, Hashtable<String, VersionInfo>> valid_versions,
			VersionInfo[] _newest_version_obtained) {
		if(valid_versions.size()==1) {
			for(VersionInfo vi: valid_versions.keySet()) {
				_newest_version_obtained[0] = vi;
				return vi.version;
			}
		}
		String newest = null;
		ArrayList<String> version_ids = new ArrayList<String>();
		ArrayList<VersionInfo> choice = new ArrayList<VersionInfo>();
		for(VersionInfo vi: valid_versions.keySet()) {
			//_newest_version_obtained[0] = vi;
			//return vi.version;
			version_ids.add(vi.version);
			choice.add(vi);
			if(newest==null){ newest = vi.version; _newest_version_obtained[0] = vi;}
			else if(newer(vi.version, newest)) { newest = vi.version; _newest_version_obtained[0] = vi;}
		}
		Object def_option=_("Select Newest");
		Object[] options = new Object[]{def_option, _("Let me select"), _("Turn off auto-updates")};
		int c = Application.ask(
				_("Multiple upgrades"),
				_("Several new versions available. Do you want the newest?"),
				options,
				def_option, null);
		switch(c) {
		case 0: 
			return newest;
		case 2:
		case JOptionPane.CLOSED_OPTION:
			return null;
		default:
		}

		def_option = newest;
		c = Application.ask(_("Several new version?"), _("Multiple upgrades"),
				version_ids.toArray(),
				def_option, null);
		switch(c) {
		case JOptionPane.CLOSED_OPTION:
			return null;
		default:
		}
		newest = version_ids.get(c);
		_newest_version_obtained[0] = choice.get(c);
		
		return newest;
	}
	Hashtable<VersionInfo, Hashtable<String,VersionInfo>> classifyVersionInfo(Hashtable<String, VersionInfo> is){
		Hashtable<VersionInfo, Hashtable<String,VersionInfo>> result = new 	Hashtable<VersionInfo, Hashtable<String,VersionInfo>>();
		for(String url_str : is.keySet()) {
			VersionInfo a = is.get(url_str);
			
			Hashtable<String, VersionInfo> crt_set = result.get(a);
			if(crt_set == null){
				crt_set = new Hashtable<String,VersionInfo>();
				result.put(a, crt_set);
			}
			crt_set.put(url_str, a);
		}
		return result;
	}
	@Deprecated
	private String getNewestVersionInfo(Hashtable<String, VersionInfo> is) {
		int cnt_new = 0;
		String newest = null;
		String newest_url = null;
		String _latestAvailable = latestAvailable_Including_Current_And_Downloaded();
		VersionInfo newest_version_obtained = null;
		
		for(String url_str : is.keySet()) {
			VersionInfo a = is.get(url_str);
			if(newer(a.version, _latestAvailable)) {
				if(newest == null){
					newest = a.version;
					newest_version_obtained = a;
					newest_url = url_str;
					cnt_new=1;
				}
				else{
					Object def_option = _("Upgrade to:")+a.version;
					Object[] options = new Object[]{_("Upgrade to:")+" "+newest, def_option, _("Cancel and disconfigure auto-updates")};
					
					if(newer(a.version, newest)){
						int c = Application.ask(
										_("Found different new versions!"),
								_("Continue updating and take newest, 2nd below or cancel to disconfigure auto updates?")+"\n"+
										_("Newer versions found with different releases:")+
										_("Version")+" \""+newest+"\" "+_("from:")+" \""+newest_url+"\"\n"+
										_("versus version")+" \""+a.version+"\" "+_("from:")+" \""+url_str+"\".\n"+
										_("I.e., Version from:")+" "+newest_url+"\n"+
										newest_version_obtained.warningPrint()+"\n"+
										_("versus version from:")+url_str+"\n"+
										a.warningPrint()+"\n",
										//JOptionPane.OK_CANCEL_OPTION
										options,
										def_option,
										null
										);
						if((c==2)||(c==JOptionPane.CLOSED_OPTION)) {
							DD.controlPane.setClientUpdatesStatus(false);
							return null;
						}
						if(c==0){
							newest_version_obtained = a;
							newest_url = url_str;
							newest = a.version; cnt_new=1;
						}
//						if(c==1){
//							newest_version_obtained = a;
//							newest_url = url_str;
//							newest = a.version;cnt_new=1;
//						}
					}
					/*
					else{
						if(newest.equals(a.version)){
							if((newest_version_obtained==null) ||
								(!newest_version_obtained.equals(a))) {
								int c = Application.ask(_("Cancel updating?\nNewest do not match:")+
										_("Select (YES) for 1st, (NO) for 2nd, or (Cancel) to disconfigure auto updates")+"\n"+
										_("Version")+" \""+newest+"\" "+_("from:")+" \""+newest_url+"\"\n"+
										_("versus version")+" \""+a.version+"\" "+_("from:")+" \""+url_str+"\".\n"+
										_("I.e., Version from:")+" "+newest_url+"\n"+
										newest_version_obtained.warningPrint()+"\n"+
										_("versus version from:")+url_str+"\n"+
										a.warningPrint()+"\n",
										_("Newest versions do not match! Potential attack!"),
										JOptionPane.YES_NO_CANCEL_OPTION);
								if(c==0) { // keep old
									continue;
								}else
									if(c==1) { // take newest
										newest_version_obtained = a;
										newest_url = url_str;
										newest = a.version;cnt_new=1;
									}
									else{ // cancel updating
										DD.controlPane.setClientUpdatesStatus(false);
										return null;
									}
							}else{
								cnt_new++;
							}
						}
					}
					*/
				}
				
			}
		}
		return newest;
	}
	/**
	 * Returns only those VI that are newer than current DD.VERSION and already downloaded
	 * @return (url: VersionInfo)
	 */
	private Hashtable<String,VersionInfo> getNewerVersionInfos() {
		String _latestAvailable = latestAvailable_Including_Current_And_Downloaded();
		Hashtable<String,VersionInfo> is = new Hashtable<String,VersionInfo>();
		for(String url_str : urls) {
			VersionInfo a= null;
		 
			if(DEBUG) System.out.println(" ClientUpdates: will work on url: "+url_str);
			
			URL _url = HandleService.isWSVersionInfoService(url_str);
			// old update
			if(_url == null) {
				URL url;
				try {
					url = new URL(url_str);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					continue;
				}
				if(url==null) continue;
				a = fetchLastVersionNumberAndSiteTXT(url);
			}else{
				String myPeerGID = DD.getMyPeerGIDFromIdentity();
				SK myPeerSK = DD.getMyPeerSK();
				if (myPeerSK!=null) {
					try{
						a = HandleService.getWSVersionInfo(_url, myPeerGID, myPeerSK, ctx);
					}catch(Exception e){System.err.println("ClientUpdates:run:WSupdate:"+e.getLocalizedMessage());}
				}
			}
			// a is null if 1) invalid Mirror Signature 2) receive fault(s) from Mirror server
			if(a==null){
				if(DEBUG) System.out.println(" ClientUpdates: got : null from "+url_str);
				continue;
			}
			// This used to be displayed all the time: will be in GUI
			if(DEBUG) System.out.println(" ClientUpdates: got : "+a.version+" from "+url_str);
			if(DEBUG) {
				for(int i=0; i<a.releaseQD.length; i++){
					System.out.println("QL ( "+ i+" ) desc: "+ a.releaseQD[i].description);
					for(int j=0; j<a.releaseQD[i]._quality.length;j++)
						System.out.println("Level ("+j+")"+ a.releaseQD[i]._quality[j] );
				}
				System.out.println("QL END .............");
			}
			if(DEBUG){				
					for(int i=0; i<a.testers_data.length; i++){
						System.out.println("tester name : "+ i+" : "+ a.testers_data[i].name);
						System.out.println("tester PK_hash : "+ i+" : "+ a.testers_data[i].public_key_hash);
						for(int j=0; j<a.testers_data[i].tester_QoT.length;j++){
							System.out.println("tester_QoT ref = " + j + " "+ a.testers_data[i].tester_QoT[j] );
							System.out.println("tester_RoT ref = " + j + " "+ a.testers_data[i].tester_RoT[j] );
						}
					}
					System.out.println("Tester END .............");
				}
			if(!newer(a.version, _latestAvailable)) { //DD.VERSION)){
				if(DEBUG) System.out.println(" ClientUpdates: got : obtained is not newer than "+DD.VERSION);
				continue;
			}
			
			/*
			PK _pk_a;
			PK[] _trusted = trusted_hash.values().toArray(new PK[0]);// trusted;
			if(a.trusted_public_key != null){
				_pk_a = trusted_hash.get(a.trusted_public_key);
				if(_pk_a!=null) {
					_trusted = new PK[1];
					_trusted[0] = _pk_a;
				}
			}
			*/
			boolean signed = false;
			// need to verify all testers signatures
			signed = D_UpdatesKeysInfo.verifySignaturesOfVI(a);
			if(!signed) {
				if(_DEBUG) System.out.println(" ClientUpdates: run: unsigned updates info\n from "+url_str+": "+a);
				Application.warning(Util._("Unsigned updates info\n from "+url_str+": "+a.warningPrint()), Util._("Unsigned updates info!"));
				//run =false;
				//DD.controlPane.setClientUpdatesStatus(false);
				//startClient(false);
				continue;
			}
			is.put(url_str, a);
		}
		return is;
	}
	
	static String latestAvailable_Including_Current_And_Downloaded(){
		String latest = DD.VERSION;
		String db=null;
		try {
			db = DD.getExactAppText(DD.LATEST_DD_VERSION_DOWNLOADED);
			if(DEBUG)System.out.println("ClientUpdates latestAvailable: latest downloaded ="+db);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(db!=null) {
			if(latest == null) latest = db;
			else if(newer(db,latest)) latest = db;
		}
		return latest;
	}
	
	/**
	 * 
	 * @param version_server
	 * @param version_local
	 * @return : true if server is newer than local
	 */
	public static boolean newer(String version_server, String version_local) {
		if(DEBUG)System.out.println("ClientUpdates newer: start server="+version_server+" vs local="+version_local);
		//Util.printCallPath("newer?");
		boolean result = false;
		String v_server[];
		String v_local[];
		if(version_server==null){
			if(DEBUG)System.out.println("ClientUpdates newer: start v_server null");
			return false;
		}
		v_server = version_server.split(Pattern.quote("."));
		if(v_server.length<3){
			if(DEBUG)System.out.println("ClientUpdates newer: start v_server < 3 : "+v_server.length+" "+Util.concat(v_server, "--"));
			return false;
		}
		if(version_local==null){
			if(DEBUG)System.out.println("ClientUpdates newer:  local null");
			return true;
		}
		v_local = version_local.split(Pattern.quote("."));
		if(v_local.length<3){
			if(DEBUG)System.out.println("ClientUpdates newer:  v_local < 3");
			return true;
		}
		if(DEBUG)System.out.println("ClientUpdates newer: server["+Util.concat(v_server,",")+"] ? v_local ["+Util.concat(v_local,",")+"]");
		
		result = false;
		
		int i_server[] = new int[3];
		int i_local[] = new int[3];
		for(int k=0; k<3; k++){
			try{
				i_server[k] = Integer.parseInt(v_server[k]);
				i_local[k] = Integer.parseInt(v_local[k]);
				if(i_server[k]<i_local[k]) return false;
				if(i_server[k]>i_local[k]) return true;
			}catch(Exception e){
				int s = Util.prefixNumber(v_server[k]);
				int l = Util.prefixNumber(v_local[k]);
				if(s<l) return false;
				if(s>l) return true;

				if(v_server[k].compareTo(v_local[k]) > 0) return true; 
				if(v_server[k].compareTo(v_local[k]) < 0) return false; 
			}
		}
		/*
		if(v_server[0].compareTo(v_local[0]) > 0) {
			if(DEBUG)System.out.println("ClientUpdates newer: server["+v_server[0]+"] > v_local ["+v_local[0]+"]");
			result = true;
		}else{
			if(DEBUG)System.out.println("ClientUpdates newer: server["+v_server[0]+"] <= v_local ["+v_local[0]+"]");
			if(v_server[1].compareTo(v_local[1]) > 0) {
				if(DEBUG)System.out.println("ClientUpdates newer: server["+v_server[1]+"] > v_local ["+v_local[1]+"]");
				result = true;
			}else{
				if(DEBUG)System.out.println("ClientUpdates newer: server["+v_server[1]+"] <= v_local ["+v_local[1]+"]");
				if(v_server[2].compareTo(v_local[2]) > 0) {
					if(DEBUG)System.out.println("ClientUpdates newer: server["+v_server[2]+"] > v_local ["+v_local[2]+"]");
					result = true;
				}else{
					if(DEBUG)System.out.println("ClientUpdates newer: server["+v_server[2]+"] <= v_local ["+v_local[2]+"]");
				}
			}	
		}
*/		
		if(DEBUG)System.out.println("ClientUpdates newer: "+result);
		return result;
	}
	
	public static VersionInfo fetchLastVersionNumberAndSiteTXT(URL url) {
		BufferedReader in;
		try {
			in = new BufferedReader( new InputStreamReader(url.openStream()));
		} catch (IOException e1) {
			System.err.println(e1.getMessage());
			//e1.printStackTrace();
			return null;
		}
		return fetchLastVersionNumberAndSiteTXT_BR(in);
	}
	public static VersionInfo fetchLastVersionNumberAndSiteTXT_BR(BufferedReader in) {
		//boolean DEBUG = true;
		VersionInfo result = new VersionInfo();
		ArrayList<Downloadable> datas = new ArrayList<Downloadable>();
		//String inputLine = "";
		String tmp_inputLine = null;
		int nb_testers = 0;
		int nb_files = 0, crt_files=0;
		int line = 0;
		try {
			Downloadable w=null;
			while ((tmp_inputLine = in.readLine()) != null) {
				//inputLine += tmp_inputLine;
				if(DEBUG)System.out.println("ClientUpdates gets: "+tmp_inputLine);
				tmp_inputLine = tmp_inputLine.trim();
				if((line==0)&&(!START.equals(tmp_inputLine))) continue;
				switch(line)
				{
				case 0: line++; break; //START
				case 1: result.version = tmp_inputLine; line++; break;
				case 2:{
					if("00000000000000.000Z".equals(tmp_inputLine)) result.date = Util.CalendargetInstance();
					else result.date = Util.getCalendar(tmp_inputLine);
					if(DEBUG)System.out.println("ClientUpdates gets: "+tmp_inputLine+" date: "+Encoder.getGeneralizedTime(result.date));
					line++; break;}
				case 3:
					//result.signature = Util.byteSignatureFromString(tmp_inputLine);
					nb_files = Integer.parseInt(tmp_inputLine);
					line++; break;
				case 4: result.script = tmp_inputLine; line++; break;
				default:
					if((line % 3)==2) {
						w = new Downloadable();
						w.filename = tmp_inputLine; line++; break;
					}else if((line % 3)==0) {
						w.url = tmp_inputLine; line++; break;
						//datas.add(w); break;
					}else{
						w.digest = Util.byteSignatureFromString(tmp_inputLine); line++; crt_files++;
						datas.add(w); break;
					}
				}
				if((line > 4) && (crt_files >= nb_files)) break;
			}
			if(DEBUG)System.out.println("ClientUpdates gets: "+tmp_inputLine+" look for testers ");
			
			while ((tmp_inputLine = in.readLine()) != null) {
				tmp_inputLine = tmp_inputLine.trim();				
				if(!TESTERS.equals(tmp_inputLine))  continue;
	
				tmp_inputLine = in.readLine();
				if(tmp_inputLine == null) break;
				tmp_inputLine = tmp_inputLine.trim();
				nb_testers = Integer.parseInt(tmp_inputLine);
				if(DEBUG)System.out.println("ClientUpdates gets: "+tmp_inputLine+" #testers= "+nb_testers);
				
				tmp_inputLine = in.readLine();
				if(tmp_inputLine == null) break;
				tmp_inputLine = tmp_inputLine.trim();
				if(tmp_inputLine.length()==0) break;
				//result.trusted_public_key = tmp_inputLine;
				result.releaseQD = parseReleaseQD(tmp_inputLine);
				if(DEBUG)System.out.println("ClientUpdates gets: "+tmp_inputLine+" rq= #"+result.releaseQD.length+"="+Util.concat(result.releaseQD," || "));

				break;
			}
			ArrayList<D_TesterInfo> testers = new ArrayList<D_TesterInfo>();
			line = 0;
			int crt_tester = 0;
			D_TesterInfo crt_tester_info = new D_TesterInfo();
			int[]indexes = null;
			while ((tmp_inputLine = in.readLine()) != null) {
				String name;
				String pk_hash;
				String IDs_strings[];
				String QTs_strings[];
				String RTs_strings[];
				String sign;
				if(DEBUG)System.out.println("ClientUpdates gets: "+tmp_inputLine+" #tester line= "+line+" crt tester="+crt_tester);
				tmp_inputLine = tmp_inputLine.trim();
				if(line%6==0){ crt_tester_info = new D_TesterInfo(); name = tmp_inputLine; crt_tester_info.name=name; line++;}
				else if(line%6==1){ pk_hash = tmp_inputLine; crt_tester_info.public_key_hash=pk_hash; line++;}
				else if(line%6==2){ IDs_strings = tmp_inputLine.split(Pattern.quote(D_TesterInfo.TEST_SEP)); indexes = Util.mkIntArray(IDs_strings); line++;}
				else if(line%6==3){ QTs_strings = tmp_inputLine.split(Pattern.quote(D_TesterInfo.TEST_SEP)); crt_tester_info.tester_QoT = mkFloatArray(indexes, QTs_strings, result.releaseQD); line++;}
				else if(line%6==4){ RTs_strings = tmp_inputLine.split(Pattern.quote(D_TesterInfo.TEST_SEP)); crt_tester_info.tester_RoT = mkFloatArray(indexes, RTs_strings, result.releaseQD); line++;}
				else if(line%6==5){
					sign = tmp_inputLine; line++; crt_tester++; crt_tester_info.signature = Util.byteSignatureFromString(sign);
					testers.add(crt_tester_info);
				}
				if(crt_tester>=nb_testers){
					if(DEBUG)System.out.println("ClientUpdates gets: "+tmp_inputLine+"  got all testers="+crt_tester);
					break;
				}
			}
			result.testers_data = testers.toArray(new D_TesterInfo[0]);
			while ((tmp_inputLine = in.readLine()) != null) {
				tmp_inputLine = tmp_inputLine.trim();				
				if(!STOP.equals(tmp_inputLine))  continue;
				break;
			}			
			result.data = datas.toArray(new Downloadable[0]);
		} catch (Exception e) {
			e.printStackTrace();
			result = null;
		}
		try {
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			result = null;
		}
		if(DEBUG)System.out.println("ClientUpdate: getchLastVersonNumber: got "+result);
		return result;
	}
	private static float[] mkFloatArray(int[] indexes, String[] qTs_strings, D_ReleaseQuality[] releaseQD) {
		int max = getMax(indexes)+1;
		if(DEBUG) System.out.println("ClientUpdates:mkFloatArray: start max="+max);
		if(releaseQD!=null) {
			if(max>releaseQD.length){
				System.err.println("ClientUpdates:mkFloatArray: error tester release quality ID larger than release definitions length! "+ max+">"+releaseQD.length);
			}else{
				max = releaseQD.length;
				if(DEBUG) System.out.println("ClientUpdates:mkFloatArray: max set to reQD="+max);
			}
		}
		float[] result = new float[max];
		for(int i=0; i<indexes.length; i++) {
			result[indexes[i]] = Float.parseFloat(qTs_strings[i]);
		}
		return result;
	}
	private static int getMax(int[] indexes) {
		int result = 0;
		for(int i = 0; i< indexes.length; i++)
			if(indexes[i]> result) result = indexes[i];
		return result;
	}
	private static D_ReleaseQuality[] parseReleaseQD(String rqd64) {
		byte[] _rqd = Base64Coder.decode(rqd64);
		Decoder d = new Decoder(_rqd);
		try {
			return d.getSequenceOf(D_ReleaseQuality.getASNType(), new D_ReleaseQuality[0], new D_ReleaseQuality());
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static VersionInfo fetchASNLastVersionNumberAndSite(URL url) {
		//VersionInfo result = new VersionInfo();
		BufferedReader in;
		try {
			in = new BufferedReader( new InputStreamReader(url.openStream()));
		} catch (IOException e2) {
			e2.printStackTrace();
			return null;
		}

		String inputLine = "";
		String tmp_inputLine = null;
		//int line = 0;
		try {
			while ((tmp_inputLine = in.readLine()) != null) {
				inputLine += tmp_inputLine;
				if(DEBUG)System.out.println(""+tmp_inputLine);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		if(inputLine==null) return null;
		String msg=inputLine.trim();
		byte[]data = Util.byteSignatureFromString(msg);
		if(data==null) return null;
		Decoder dec = new Decoder(data);
		try {
			return new VersionInfo().decode(dec);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean downloadNewer(VersionInfo a) throws IOException {
		boolean DEBUG = true;
		if(DEBUG)System.out.println("ClientUpdates downloadNewer: "+a);
		String install_root_path_with_sep = getFileInstallRootPathWithSeparator();
		String filepath;
		String newDirName = install_root_path_with_sep+a.version;
		if(DEBUG)System.out.println("ClientUpdates downloadNewer: newDirName="+newDirName);
		File newDirFile = new File(newDirName);
		if(!newDirFile.exists()){
			//create
			newDirFile.mkdirs();
			if(DEBUG)System.out.println("ClientUpdates downloadNewer: new Dir created ");
		}else{
			if(!newDirFile.isDirectory()){
				Application.warning(_("Cannot install updates:"+"\n"+newDirName), _("Path busy for updates"));
				if(DEBUG)System.out.println("ClientUpdates downloadNewer: new Dir not directory ");
				return false;
			}
		}
		/*org.apache.commons.io.FileUtils.copyURLToFile(URL, File)*/
	    for(Downloadable d: a.data) {
			if(DEBUG)System.out.println("ClientUpdates downloadNewer: downloadable "+d);
	    	
	    	URL website = new URL(d.url);
	    	InputStream istream = website.openStream();
	    	//ReadableByteChannel rbc = Channels.newChannel(istream);
	    	
	    	filepath = newDirName+Application.OS_PATH_SEPARATOR+d.filename;
	    	FileOutputStream fos = new FileOutputStream(filepath);
	    	long len=0;
	    	int crt=0;
	    	//FileChannel ch = fos.getChannel();
	    	
	    	byte[] message = new byte[1<<13];
	    	MessageDigest digest;
	    	String hash_alg = Cipher.SHA256;
	    	try {
	    		if(DEBUG)System.out.println("ClientUpdates downloadNewer:hash = "+hash_alg);
	    		digest = MessageDigest.getInstance(hash_alg);
	    	} catch (NoSuchAlgorithmException e) {
	    		e.printStackTrace(); return false;}
	    	//long len=0;
	    	do{
	    		
				if(DEBUG)System.out.print(".");
				crt=istream.read(message);
				if(DEBUG)System.out.print(""+crt);
				if((crt<0) || (crt>message.length)){
					if(DEBUG)System.out.print(""+crt+"/"+message.length);
					break;
				}
				len+=crt;
	    		digest.update(message, 0, crt);
				fos.write(message, 0, crt);
	    		//crt=ch.transferFrom(rbc, len, 1 << 32);
	    		//if(crt>0) len+=crt;
				//if(DEBUG)System.out.println("ClientUpdates downloadNewer: done crt="+crt+" len="+len);
	    	}while((crt>0));
	    	fos.close();
			if(DEBUG)System.out.println("ClientUpdates downloadNewer: done = "+len+" for "+d);
	    	byte mHash[] = digest.digest();
	    	if(!Util.equalBytes(mHash, d.digest)) {
	    		if(DD.DELETE_UPGRADE_FILES_WITH_BAD_HASH) new File(filepath).delete();
				if(DEBUG)System.out.println("ClientUpdates downloadNewer: bad hash = "+Util.stringSignatureFromByte(mHash));//Util.byteToHex(mHash));
	    		return false;
	    	}
			if(DEBUG)System.out.println("ClientUpdates downloadNewer: done "+d+" len="+len);
	    }
	    try {
			DD.setAppTextNoSync(DD.LATEST_DD_VERSION_DOWNLOADED, a.version);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		};
		
		// update scripts assume that the PREVIOUS file exists....to insert it in db
		try{
			ensure_PREVIOUS(install_root_path_with_sep);
		}catch(Exception e){
			Application.warning(_("A new version is dowloaded.")+"\n" +
					_("So far we failed in creating a file in folder:")+" \""+install_root_path_with_sep+"\"\n"+
					_("The file was supposed to be called:")+" \""+Application.PREVIOUS+"\"\n"+
					_("We will continue the attempt to install the new version."),
					_("New Version Downloaded"));
		}
		
		// run update scripts
		String[] script_absolute = new String[]{newDirName+Application.OS_PATH_SEPARATOR+a.script};
		String[] _script_absolute;
		File _update_script;
		File update_script = new File(script_absolute[0]);
		if(!update_script.setExecutable(true, false)) {
			if(DEBUG)System.out.println("ClientUpdates downloadNewer: "+_("Cannot set executable permission for ")+Util.concat(script_absolute,","));
			//Application.warning(Util._("Cannot set executable permission for: ")+Util.concat(script_absolute,","), Util._("Cannot set exec permission"));
		}
		Application.warning(_("A new version is dowloaded.")+"\n" +
				_("Will first attempt to execute script:")+" \""+update_script+"\"\n"+
				_("A separate confirmation will be requested before running it, if available."),
				_("New Version Downloaded"));
		if(update_script.exists()&&!update_script.setExecutable(true, false))
			Application.warning(_("Cannot set executable permission for:")+" \""+script_absolute[0]+"\".",
					_("Cannot set execution permission!"));
		if(update_script.exists()&&update_script.canExecute()) {
			if(DEBUG)System.out.println("ClientUpdates downloadNewer: "+_("Executing: ")+Util.concat(script_absolute,","));
			int q = Application.ask(
					_("A new version is dowloaded.")+"\n"+
							_("Execute script:")+" \""+update_script+"\"?",
							_("New Version Downloaded"),
					JOptionPane.OK_CANCEL_OPTION);
			if(q!=0) return false;
			BufferedReader output = Util.getProcessOutput(script_absolute, null, newDirFile);
			String outp = Util.readAll(output);
			String lines[] = outp.split(Pattern.quote("\n"));
			System.out.println("\n\nClientUpdates:downloadNewer: updating base script:\n"+outp+"\n\n");
			Application.warning(
					_("Basic Output From:")+" \""+update_script+"\"\n"+
							Util.concat(Util.selectRange(lines, lines.length-20, new String[20]), "\n", "null"),
							_("Updates Process Output"));
			//Application.warning(Util._("A new version is dowloaded. Executed script: "+update_script), Util._("New Version Downloaded"));
		}else{
			int q;
			switch(DD.OS) {
			case DD.LINUX:
			case DD.MAC:
				_script_absolute = new String[]{newDirName+Application.OS_PATH_SEPARATOR+a.script+Application.UPDATES_UNIX_SCRIPT_EXTENSION};//".sh";
				_update_script = new File(_script_absolute[0]);
				Application.warning(_("Let us try LINUX/MAC installers for the new version dowloaded.")+"\n" +
						_("Will now attempt to execute script:")+" \""+_update_script+"\"\n"+
						_("A separate confirmation will be requested before running it, if available."),
						_("New Version Downloaded"));
				if(!_update_script.setExecutable(true, false))
					Application.warning(
							_("Cannot set executable permission for:")+" \""+_script_absolute[0]+"\".",
							_("Cannot set executable permission!"));
				if(_update_script.exists()&&_update_script.canExecute()) {
					q = Application.ask(
							_("A new version is dowloaded.")+"\n"+
									_("Execute script:")+" \""+_update_script+"\"?",
									_("New Version Downloaded"),
							JOptionPane.OK_CANCEL_OPTION);
					if(q!=0) return false;
					if(DEBUG)System.out.println("ClientUpdates downloadNewer: "+_("Executing: ")+Util.concat(_script_absolute,","));
					BufferedReader output = Util.getProcessOutput(_script_absolute, null, newDirFile);
					String outp = Util.readAll(output);
					String lines[] = outp.split(Pattern.quote("\n"));
					System.out.println("\n\nClientUpdates:downloadNewer: updating script:\n"+outp+"\n\n");
					Application.warning(
							_("Basic Output From:")+" \""+_update_script+"\"\n"+
									Util.concat(Util.selectRange(lines, lines.length-20, new String[20]), "\n", "null"),
									_("Updates Process Output"));
					//Runtime.getRuntime().exec(_script_absolute, null, newDirFile);
				}else{
					if(DEBUG)System.out.println("ClientUpdates downloadNewer: "+_("Not executable: ")+Util.concat(script_absolute,","));
					Application.warning(_("No executable script found for updates")+" \""+_script_absolute[0]+"\".\n",
							_("New Version Downloaded, No Installation Script Found!"));
					return false;					
				}
				break;
			case DD.WINDOWS:
				_script_absolute = new String[]{newDirName+Application.OS_PATH_SEPARATOR+a.script+Application.UPDATES_WIN_SCRIPT_EXTENSION};//".bat";
				_update_script = new File(_script_absolute[0]);
				Application.warning(_("Let us try WINDOWS installers for the new version dowloaded.")+"\n" +
						_("Will now attempt to execute script:")+" \""+_update_script+"\"\n"+
						_("A separate confirmation will be requested before running it, if available."),
						_("New Version Downloaded"));
				if(!_update_script.setExecutable(true, false))
					Application.warning(
							_("Cannot set executable permission for:")+" \""+_script_absolute[0]+"\".",
							_("Cannot set executable permission!"));
				if(_update_script.exists()&&_update_script.canExecute()) {
					q = Application.ask(
							_("A new version is dowloaded.")+"\n"+
									_("Execute script:")+" \""+_update_script+"\"?",
									_("New Version Downloaded"),
							JOptionPane.OK_CANCEL_OPTION);
					if(q!=0) return false;
					if(DEBUG)System.out.println("ClientUpdates downloadNewer: "+_("Executing: ")+Util.concat(_script_absolute,","));
					BufferedReader output = Util.getProcessOutput(_script_absolute, null, newDirFile);
					String outp = Util.readAll(output);
					String lines[] = outp.split(Pattern.quote("\n"));
					System.out.println("\n\nClientUpdates:downloadNewer: updating script:\n"+outp+"\n\n");
					Application.warning(
							_("Basic Output From:")+" \""+_update_script+"\"\n"+
									Util.concat(Util.selectRange(lines, lines.length-20, new String[20]), "\n", "null"),
									_("Updates Process Output"));
				}else{
					if(DEBUG)System.out.println("ClientUpdates downloadNewer: "+_("not executable: ")+Util.concat(_script_absolute,","));
					Application.warning(_("No executable script found for updates")+" \""+_script_absolute[0]+"\".\n",
							_("New Version Downloaded, No Installation Script Found!"));
					return false;					
				}
				break;
			default:
				Application.warning(
						_("A new version is dowloaded. No specific script founf for undetected OS:")+
						"\n"+System.getProperty("os.name"),
						_("New Version Downloaded"));					
			}
		}
		int c;
		c=Application.ask(
				_("A new version is dowloaded.\nDo you want to start next time with version:")+" "+a.version+"?",
				_("New Version Downloaded"),
				JOptionPane.OK_CANCEL_OPTION);
		if(c != 0) return false;
		if(prepareLatest(install_root_path_with_sep, a.version)) {
			Application.warning(_("A new version is dowloaded. Restart your application to use it. VERSION:")+" "+a.version,
					_("New Version Downloaded"));
			return true;
		}else{
			Application.warning(
					_("Exception encountered establishing link to download")+" \""+a.script+"\"",
					_("New Version Downloaded, no link made to it."));
			return false;			
		}
	}
	private static void ensure_PREVIOUS(String parent_dir_with_sep) throws IOException{
		String PREVIOUS = parent_dir_with_sep+Application.PREVIOUS;
		File old_previous = new File(PREVIOUS);
		if(!old_previous.exists()){
			Util.storeStringInFile(old_previous, DD.VERSION);
		}
	}
	/**
	 * Prepare files LATEST, PREVIOUS and HISTORY
	 * @param parent_dir_with_sep
	 * @param version
	 * @return
	 */
	private static boolean prepareLatest(String parent_dir_with_sep, String version) {
		if(DEBUG)System.out.println("ClientUpdates prepareLatest: start ver"+version+" parent"+parent_dir_with_sep);
		try{
			//String parent_dir = getFileInstallRootPath();
			String LATEST = parent_dir_with_sep+Application.LATEST;
			String PREVIOUS = parent_dir_with_sep+Application.PREVIOUS;
			String HISTORY = parent_dir_with_sep+Application.HISTORY+"."+Util.getGeneralizedTime();
			File old_previous = new File(PREVIOUS);
			if(old_previous.exists()&&!old_previous.delete()){
				if(DEBUG)System.out.println("ClientUpdates prepareLatest: "+_("Fail to delete ")+PREVIOUS+"\n");
				Application.warning(Util._("Fail to delete ")+PREVIOUS, Util._("Download stopped. No link made"));
				return false;
			}
			if(DEBUG)System.out.println("ClientUpdates prepareLatest: success delete previous");

			File history = new File(HISTORY);
			if(!history.exists())Util.storeStringInFile(history, version);
			else{
				if(DEBUG)System.out.println("ClientUpdates prepareLatest: "+_("File date conflict for: ")+HISTORY+"\n");
				Application.warning(Util._("File date conflict for: ")+HISTORY, Util._("New Version Downloaded, No link made"));
				return false;
			}
			if(DEBUG)System.out.println("ClientUpdates prepareLatest: success store HISTORY");
			File old_latest = new File(LATEST);
			if(!old_latest.exists()){
				if(DEBUG)System.out.println("ClientUpdates prepareLatest: no LATEST: unusual configuration, I quit!");
				Application.warning(Util._("I QUIT. Non-standard configuration. No file:")+LATEST, Util._("Download stopped. No link made"));
				return false;
			}
			if(!old_latest.renameTo(old_previous)){
				if(DEBUG)System.out.println("ClientUpdates prepareLatest: cannot rename LATEST to PREVIOUS");
				return false;
			}
			File latest = new File(LATEST);
			if(!latest.exists()){
				if(DEBUG)System.out.println("ClientUpdates prepareLatest: will store LATEST");
				Util.storeStringInFile(latest, version);
			}
			else{
				if(DEBUG)System.out.println("ClientUpdates prepareLatest: "+Util._("Fail to delete&install ")+LATEST+"\n");
				Application.warning(Util._("Fail to delete&install ")+LATEST, Util._("Download stopped. No link made"));
				return false;
			}
			if(DEBUG)System.out.println("ClientUpdates prepareLatest: success store LATEST");
		}catch(Exception e){
			if(DEBUG)System.out.println("ClientUpdates prepareLatest: ver"+_("Fail to delete&install ")+version+"\n");
			Application.warning(Util._("Fail to delete&install ")+version+"\n"+e.getLocalizedMessage(), Util._("New Version Downloaded, Exception"));
			e.printStackTrace();
			return false;
		}
		if(DEBUG)System.out.println("ClientUpdates prepareLatest: success ver"+version);
		return true;
	}

	public static boolean fixLATEST() throws IOException, P2PDDSQLException{
		String parent_dir_with_sep = getFileInstallRootPathWithSeparator();
		String version = getFileInstallLastComponent();
		if((version == null) || ("".equals(version.trim())))
			version = DD.VERSION;
		String LATEST = parent_dir_with_sep+Application.LATEST;
		String PREVIOUS = parent_dir_with_sep+Application.PREVIOUS;
		String previous = null;
		try{previous = Util.loadFile(PREVIOUS);}catch(Exception e){}
		if(previous!=null) {
			previous = previous.trim();
			int c = Application.ask(_("Return to")+" \""+version+"\" "+_("(YES) or to")+" \""+previous+"\" "+"(NO)", _("What to do?"),
				JOptionPane.YES_NO_CANCEL_OPTION);
			if(c==2) return false;
			if(c==1) version = previous;
		}
		if(DEBUG)System.out.println("ClientUpdates prepareLatest: success store HISTORY");
		File old_latest = new File(LATEST);
		if(!old_latest.exists()){
			if(DEBUG)System.out.println("ClientUpdates prepareLatest: no LATEST: unusual configuration, I quit!");
			Application.warning(Util._("I QUIT. Non-standard configuration. No file:")+LATEST, Util._("Download stopped. No link made"));
			return false;
		}
		File bck;
		if(!old_latest.renameTo(bck = new File(LATEST+"."+Util.random(100)))){
			if(DEBUG)System.out.println("ClientUpdates prepareLatest: cannot rename LATEST to PREVIOUS");
			Application.warning(Util._("Fail to rename ")+LATEST, Util._("Failure to fix Latest"));
			return false;
		}
		File latest = new File(LATEST);
		if(!latest.exists()){
			if(DEBUG)System.out.println("ClientUpdates prepareLatest: will store LATEST");
			try{
				Util.storeStringInFile(latest, version);
			}catch(Exception e){
				bck.renameTo(latest);
			}
		}
		else{
			if(DEBUG)System.out.println("ClientUpdates prepareLatest: "+Util._("Fail to delete&install ")+LATEST+"\n");
			Application.warning(Util._("Fail to rename ")+LATEST, Util._("Failure to fix Latest"));
			return false;
		}
		
		DD.setAppTextNoSync(DD.LATEST_DD_VERSION_DOWNLOADED, null);
		return true;
	}
	public static String getFileInstallLastComponent() {
		if(DEBUG)System.out.println("ClientUpdates: getFileInstallLastComponent: start");
		String filepath = Application.CURRENT_INSTALLATION_VERSION_BASE_DIR();
		if(DEBUG)System.out.println("ClientUpdates: getFileInstallLastComponent: path ="+ filepath);
		if(filepath == null){
			if(DEBUG)System.out.println("ClientUpdates: getFileInstallLastComponent: path null ="+ filepath);
			return DD.VERSION;
		}
		String[] f = filepath.split(Pattern.quote(Application.OS_PATH_SEPARATOR));
		String cmp=null;
		if(DEBUG)System.out.println("ClientUpdates: getFileInstallLastComponent: components ="+ f.length);
		for(int i=f.length-1; i>=0; i--){
			if(DEBUG)System.out.println("ClientUpdates: getFileInstallLastComponent: component ="+i+" "+ cmp);
			cmp = f[i].trim();
			if((cmp==null) || ("".equals(cmp))) continue;
			break;
		}
		if(cmp == null){
			if(DEBUG)System.out.println("ClientUpdates: getFileInstallLastComponent: cmp null ="+ cmp);
			return DD.VERSION;
		}
		return cmp;
	}
	public static void startClient(boolean b) {
		synchronized(monitor){
			if(DEBUG)System.out.println("ClientUpdates startClient: start "+b);
			if(b) {
				if(DEBUG)System.out.println("ClientUpdates startClient: start really "+b);
				ClientUpdates.clientUpdates = new ClientUpdates();
				ClientUpdates.clientUpdates.start();
			} else {
				if(ClientUpdates.clientUpdates == null) return;
				ClientUpdates.clientUpdates.run=false;
				ClientUpdates.clientUpdates.turnOff();
				ClientUpdates.clientUpdates.interrupt();
				ClientUpdates.clientUpdates = null;
				if(DEBUG)System.out.println("ClientUpdates startClient: stopped");
			}
		}
	}
	/**
	 * Rooth path terminated with separator
	 * @return
	 */
	public static String getFileInstallRootPathWithSeparator(){
	    String filepath=System.getProperty("user.home");
	    //String file_sep=System.getProperty("file.separator");
	    //String userdir = System.getProperty("user.dir");
	    
	    filepath = Application.CURRENT_INSTALLATION_ROOT_BASE_DIR();
	    if(filepath==null) {
	    	if(DD.OS == DD.WINDOWS) {
	    		try {
	    			filepath = DD.getAppText(DD.APP_WINDOWS_INSTALLATION_ROOT_PATH);
	    			//file_sep = "\\";
	    		} catch (P2PDDSQLException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	if(DD.OS == DD.LINUX) {
	    		try {
	    			filepath = DD.getAppText(DD.APP_LINUX_INSTALLATION_ROOT_PATH);
	    			//file_sep = "/";
	    		} catch (P2PDDSQLException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    }
	    if((filepath==null)||(filepath.length()==0)){
	    	filepath = System.getProperty("user.home");
	    }
	    return filepath+Application.OS_PATH_SEPARATOR;
	
	}
	public static String getFilePath(){
	    String filepath=System.getProperty("user.home");
	    //String file_sep=System.getProperty("file.separator");
	    String userdir = System.getProperty("user.dir");
	    if(DD.OS == DD.WINDOWS) {
			try {
				filepath = DD.getAppText(DD.APP_WINDOWS_INSTALLATION_PATH);
				//file_sep = "\\";
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
	    }
	    if(DD.OS == DD.LINUX) {
			try {
				filepath = DD.getAppText(DD.APP_LINUX_INSTALLATION_PATH);
				//file_sep = "/";
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
	    }
	    if(filepath==null){
	    	filepath = System.getProperty("user.home");
	    }
	    return filepath+Application.OS_PATH_SEPARATOR;
	
	}
	public static boolean create_info(File fileUpdates, File fileTrustedSK,
			File fileOutput, String filePath) throws IOException {
		return create_info(fileUpdates.getCanonicalPath(), fileTrustedSK.getCanonicalPath(), fileOutput.getCanonicalPath(), filePath);
	}
	public static boolean create_info (String fileName, String trustedName, String outputFile, String filePath) throws IOException {
		if(DEBUG)System.out.println("ClientUpdates: create_info: start");
		//boolean DEBUG=true;
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		VersionInfo i = ClientUpdates.fetchLastVersionNumberAndSiteTXT_BR(in);
		in.close();
		if(DEBUG)System.out.println("Input="+i);
		if(i==null){
			Application.warning(Util._("Inappropriate input file for updates"), Util._("Bad input file!"));
			System.out.println("ClientUpdates: create_info: error no VI");
			return false;
		}
		//PK[] trusted = getTrustedKeys();
		//if(trusted.length==0) trusted = generateTrustedKey();

		//if(Encoder.getGeneralizedTime(Util.getCalendar("00000000000000.000Z")).equals(Encoder.getGeneralizedTime(i.date)))i.date = Util.CalendargetInstance();
		//if(DEBUG)System.out.println("Input with date="+i);

		String trusted = Util.loadFile(trustedName);
		if(trusted==null){
			System.out.println("ClientUpdates: create_info: error null trusted");
			return false;
		}
		trusted = trusted.trim();
		String[] _trusted = trusted.split(Pattern.quote(","));
		if(_trusted.length<1){
			System.out.println("ClientUpdates: create_info: error no trusted");
			return false;
		}
		SK sk  = null; // may contain PK after SK
		String tested_pk=null;
		for(int k=0; k<_trusted.length; k++) {
			String t = _trusted[k];
			if(t==null) continue;
			t = t.trim();
			if(t.length()==0) continue;
		 	sk = Cipher.getSK(t);
		 	if(sk==null) continue;
		 	if(k+1<_trusted.length){
		 		String pk=null;
		 		pk = _trusted[k+1];
		 		if(pk == null) break;
		 		pk = pk.trim();
		 		if(pk.length()==0) break;
		 		try{
		 			PK _pk = Cipher.getPK(pk);
		 			if(_pk != null) tested_pk=pk;//i.trusted_public_key = pk;
		 		}catch(Exception e){e.printStackTrace();}
		 	}
		 	break;
		}
	 	if(sk==null){
			System.out.println("ClientUpdates: create_info: error exit no sk");
	 		return false;
	 	}

	 	if((i.testers_data==null)||(i.testers_data.length<1)){
	 		Application.warning(_("Need 1 tester"), _("No tester space"));
			System.out.println("ClientUpdates: create_info: error no testers data");
	 		return false;
	 	}

		i.updateHash(filePath);
		D_TesterSignedData tsd = new D_TesterSignedData(i, 0);
		i.testers_data[0].public_key_hash = Util.getGIDhashFromGID(tested_pk, true);
		i.testers_data[0].signature = tsd.sign(sk);
		//i.sign(sk);
		if(DEBUG)System.out.println("Output="+i);
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
		out.write(i.toTXT());
		out.close();
		if(DEBUG)System.out.println("ClientUpdates: create_info: success done");
		return true;
	}
	public static void main(String args[]) {
		try {
			//System.out.println("ClientUpdates: main: start");
			if(args.length != 4) {System.err.println("Call with parameters: input key output install: ["+args.length+"] "+Util.concat(args, ",")); return;}
			String input = args[0];
			String key = args[1];
			String output = args[2];
			String install = args[3];
			System.out.println(_("Called with parameters: ")+input+" "+key+"  "+output+" "+install);
			install += Application.OS_PATH_SEPARATOR;
			File _input = new File(input);
			File _key = new File(key);
			File _output = new File(output);
			File _install = new File(install);

			boolean ya=true;
			if(!_input.exists()|| !_input.isFile())
			{ya = false;Application.warning(_("Input file is not good:")+input, _("Signing did not work"));}
			if(!_key.exists()|| !_key.isFile())
			{ya=false;Application.warning(_("Key file is not good:")+key, _("Signing did not work"));}
			if(_output.exists())
			{ya=false;Application.warning(_("Ouput file exists:")+output, _("Signing did not work"));}
			if(!_install.exists() || !_install.isDirectory())
			{ya=false;Application.warning(_("Install director wrong:")+install, _("Signing did not work"));}
			if(!ya){
				System.out.println(_("Call with bad parameters: ")+input+" "+key+"  "+output);
				return;
			}

			System.out.println("Called with good parameters: "+input+" "+key+"  "+output+" "+install);

			
			//System.out.println("ClientUpdates: main: will sign");
			boolean result = ClientUpdates.create_info(input, key, output, install);
			if(!result) Application.warning(_("Something bad happened signing"), _("Signing did not work"));
			System.out.println("ClientUpdates: main: done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}