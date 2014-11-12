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

package config;
 
import static util.Util.__;
import hds.Address;
import hds.ClientSync;
import hds.DirectoryAnswerMultipleIdentities;
import hds.DirectoryServer;
import hds.EventDispatcher;
import hds.IClient;
import hds.Server;
import hds.UDPServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.regex.Pattern;

import simulator.Fill_database;
import simulator.SimulationParameters;
import streaming.OrgHandling;
import table.HashConstituent;
import util.BMP;
import util.DBInterface;
import util.DB_Implementation;
import util.DD_Address;
import util.DD_DirectoryServer;
import util.DD_IdentityVerification_Answer;
import util.DD_IdentityVerification_Request;
import util.DD_Mirrors;
import util.DD_SK;
import util.DD_Testers;
import util.DirectoryAddress;
import util.EmbedInMedia;
import util.P2PDDSQLException;
import util.StegoStructure;
import util.Util;
import wireless.BroadcastClient;
import wireless.BroadcastServer;
import wireless.Broadcasting_Probabilities;
import wireless.Refresh;
import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;
import data.D_Constituent;
import ASN1.ASN1DecoderFail;
import ASN1.Encoder;

public class DD {
	public static final String BRANCH = "B";//FIT_HDSSL_SILAGHI";
	public static final String VERSION = "0.10.05";
	public static final boolean ONLY_IP4 = false;

	private static final String PK_Developer = "MIIEGgwDUlNBYAEwAgMBAAECggQASKs9x2VEQH1SRxRwO43yt6HXCTnOmPJVUjN8bQQUTVBdFXhQsTpnTP1yLe/qFlA0jnIzheHT4WEcsU874N800iPMWHCjpCowQwwTj9SQLTmfbfhL8z0a7Dw6ZJQ+DnYoPVhx3JHL57CK3YeVYclZCoHetZ5PEIpcAwxaPmnL3GQaOgJiVHb6CLMi+hNHLxsjQZwTYTeoUOXQKgyTcRDE6xCvw8+q0U6/Uan3KCx/KmtdRQMEtGAXSPANv12kle84Dv8AdJxT1CJGsXm0+N6+wbbvkL77kMr+79sCR/8drZmOnrbjveQpab2pSh0vO//XqslrDRbzhniGSpqFW+YNTOixWAsCp35hNPbAx5xqPXg6DEIrysGslDGo4gC3Ew5mN/JkOQA+pd6uIzC4EgbfWqJKMvrtOQN67hJR7Ysxn7cLDXGvmhK1s7oSJcnOmhWljSZ6joviVwAWKgzdm1gMBhn5+VdgwoEE7g5Inw0dH9UmgufloNiBQMM9m2igdQPaLRuVttrAEcs55F/Z5NFtJquTeQFBLAGux3MVxrYCgivRaoAzAkUMhGOA+00KU3oh3Bds0U8GYCMuYYrwSAWTZf0Z9lvUwJv8HtLJvI6p1p53oGzIW9bo20d0PMz7XrzNDOLEME9PaXKLo6vMCAxXIj19nm/bE1HBY7e7HErKMX3M7LC2xZ8PH7wsnl5M3y0ZZ6c9quwhvz/dWcUAQ5963LtDZ6bOenAGVGBjdWLhHK8/2p9Vgu1ZNA1WWHWnafExsT5GxuwZQ/PMk8YtmxqEkgGy2+xVT19oUK+yO1ok+xRUjvSRZ0IbWUEcOfQ5FvLNmMdV/NSebB6vjQwM5DGCE1YDhix+Qghr558KokVz7BPVrGVe1pUxfPo2XPwHReF8es+vr16lvwXrVEmQNG8KrX1tN5Z5I29+ZVcR6ti4t90RXY6H6lmLtU3P/PSmfOrBQraNHVvDm9y1hnSP9+EhJzuWFaS8v4+7OnodIWuZsYd2WYQp4YcDJ+7grV3s1vvacujzxCOwx5/gosLxOau45bvKqhsFrZ+le6IRNAG7T6ZwC9wesqCGBJlIwS50DlAb/KhPyDIvf+7EH1iwckG4fBtixaK9co8FHnuddn/cEIc6fkWDEzr2Cu3HyxeMeDrcGRvjTRr78Wp/ptvRoOYElOLkxrkmanetjOCMqRl1DJvl53SQKePraRx2DpRemK/TMQ3+5TQkFjjEsI2P455Th0z6vF+JzpetZ3j1NUqx+iEZ2ArMhdDk7dE/4qcn2xwLz5nNMvHSnO2N0T9tCLi96CqZm/HTqGa6jTxFhJOP11sFCCQ9jkKhxvxubs0sww75dnqXQeffpxyolcht3KHwfwwHU0hBLTUxMg==";
	private static PK _PK_Developer = null;
	public static PK get_PK_Developer() {
		if (_PK_Developer != null) return _PK_Developer;
		return _PK_Developer = Cipher.getPK(PK_Developer);
	}
	
	public static String _APP_NAME = __("Direct Democracy P2P");
	//public static String _APP_NAME = _("La Bible A Petits Pas");
	public static String APP_NAME = _APP_NAME+" "+VERSION;
	public static final String DEFAULT_EMAIL_PROVIDER = "my.fit.edu";
	public static boolean DEBUG = false;
	static final boolean _DEBUG = true;
	
    public static final String WIRELESS_THANKS = "wireless_thanks.wav"; // in scripts
    public static String scripts_prefix = null; //Application.linux_scripts_prefix+Application.scripts_path

    // May want to let users edit the next value, as part of making the LIST_OF_VALUES
    // in fields_extra delete-able easier
	public static boolean DELETE_COMBOBOX_WITHOUT_CTRL = true;
	//public static final byte TAG_SyncReq_push = DD.asn1Type(Encoder.CLASS_UNIVERSAL, Encoder.PC_CONSTRUCTED, Encoder.TAG_SEQUENCE);
	public static final byte TAG_AP0 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)0);
	public static final byte TAG_AP1 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)1);
	public static final byte TAG_AP2 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)2);
	public static final byte TAG_AP3 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)3);
	public static final byte TAG_AP4 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)4);
	public static final byte TAG_AP5 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)5);
	public static final byte TAG_AP6 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)6);
	public static final byte TAG_AP7 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)7);
	public static final byte TAG_AP8 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)8);
	public static final byte TAG_AP9 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)9);
	public static final byte TAG_AP10 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)10);
	public static final byte TAG_AP11 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)11);
	public static final byte TAG_AP12 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)12);
	public static final byte TAG_AP13 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)13);
	public static final byte TAG_AP14 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)14);
	public static final byte TAG_AP15 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)15);
	public static final byte TAG_AP16 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)16);
	public static final byte TAG_AP17 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)17);
	
	public static final byte TAG_AC0 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)0);
	public static final byte TAG_AC1 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)1);
	public static final byte TAG_AC2 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)2);
	public static final byte TAG_AC3 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)3);
	public static final byte TAG_AC4 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)4);
	public static final byte TAG_AC5 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)5);
	public static final byte TAG_AC6 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)6);
	public static final byte TAG_AC7 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)7);
	public static final byte TAG_AC8 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)8);
	public static final byte TAG_AC9 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)9);
	public static final byte TAG_AC10 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)10);
	public static final byte TAG_AC11 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)11);
	public static final byte TAG_AC12 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)12);
	public static final byte TAG_AC13 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)13);
	public static final byte TAG_AC14 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)14);
	public static final byte TAG_AC15 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)15);
	public static final byte TAG_AC16 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)16);
	public static final byte TAG_AC17 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)17);
	public static final byte TAG_AC18 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)18);
	public static final byte TAG_AC19 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)19);
	public static final byte TAG_AC20 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)20);
	public static final byte TAG_AC21 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)21);
	public static final byte TAG_AC22 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)22);
	public static final byte TAG_AC23 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)23);
	public static final byte TAG_AC24 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)24);
	public static final byte TAG_AC25 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)25);
	public static final byte TAG_AC26 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)26);
	public static final byte TAG_AC27 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)27);
	public static final byte TAG_AC28 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)28);
	public static final byte TAG_AC29 = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)29);
	public static final byte TAG_PP0  = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)0);
	public static final byte TYPE_DatabaseName = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)1);
	public static final byte TYPE_FieldName = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)2);
	public static final byte TYPE_FieldType = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)3);
	public static final byte TAG_PP4 = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)4);
	public static final byte TYPE_SignSyncReq = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_PRIMITIVE, (byte)5);
	public static final byte MSGTYPE_EmptyPing = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)20);;
	public static final byte TYPE_ORG_DATA = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)29);
	/*
	 * TYPES OF IMAGES
	 * should never go over 30 for the type value in one byte
	 */
	public static final byte TYPE_DD_IDENTITY_VERIFICATION = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)30);
	public static final byte TYPE_DD_IDENTITY_VERIFICATION_ANSWER = DD.asn1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)29);
	/**
	 * SIGN of images
	 */
	public static final short STEGO_SIGN_PEER = 0x0D0D;
	public static final short STEGO_SIGN_DIRECTORY_SERVER = 0x1881;
	public static final short STEGO_SIGN_CONSTITUENT_VERIF_ANSWER = 0x3EE3;
	public static final short STEGO_SIGN_MIRRORS = 0x4774;
	public static final short STEGO_SIGN_TESTERS = 0x588C;
	public static final short STEGO_SK = (short) 0xBEEF;
	public static final short STEGO_SIGN_CONSTITUENT_VERIF_REQUEST = 0x7AAD;
	public static final short STEGO_SLOGAN = (short) 0xDEAD;

	/**
	 * class(2bits)||pc(1b)||number
	 * @param classASN1
	 * @param PCASN1
	 * @param tag_number
	 * @return
	 */
	public static byte asn1Type(int classASN1, int PCASN1, byte tag_number) {
		if((tag_number&0x1F) >= 31){
			Util.printCallPath("Need more bytes");
			tag_number = 25;
		}
		return  (byte)((classASN1<<6)+(PCASN1<<5)+tag_number);
	}
	public static ArrayList<StegoStructure> available_stego_structure =
			new ArrayList<StegoStructure>(Arrays.asList(_getInitialStegoStructureInstances()));
			//new ArrayList<StegoStructure>();
	
	private static StegoStructure[] _getInitialStegoStructureInstances() {
		DD_Address data1 = new DD_Address();
		DD_IdentityVerification_Request data2 = new DD_IdentityVerification_Request();
		DD_IdentityVerification_Answer data3 = new DD_IdentityVerification_Answer();
		DD_DirectoryServer data4 = new DD_DirectoryServer();
		DD_Testers data5 = new DD_Testers();
		DD_Mirrors data6 = new DD_Mirrors();
		DD_SK data7 = new DD_SK();
		return new StegoStructure[]{data1, data2, data3, data4, data5, data6, data7};
	}
	/**
	 * Function used to query available StegoStructures.
	 * @return
	 */
	public static StegoStructure[] getAvailableStegoStructureInstances(){
		return available_stego_structure.toArray(new StegoStructure[0]);
	}
	/**
	 * Function used to register a StegoStructure for enabling its encoding at 
	 * drag and drop
	 * @param ss
	 */
	public static void registerStegoStructure(StegoStructure ss) {
		if (available_stego_structure == null)
			available_stego_structure = 
			new ArrayList<StegoStructure>(Arrays.asList(_getInitialStegoStructureInstances()));
		available_stego_structure.add(ss);
	}
	public static short[] getAvailableStegoStructureISignatures() {
		StegoStructure[] a = getAvailableStegoStructureInstances();
		if(a==null) return new short[0];
		short []r = new short[a.length];
		for(int k =0 ; k<a.length; k++)
			r[k] = a[k].getSignShort();
		return r;
	}
	
	public static boolean STREAM_SEND_ALL_ORG_CREATOR = true;
	public static boolean STREAM_SEND_ALL_FUTURE_ORG = false;
	public static boolean WARN_BROADCAST_LIMITS_REACHED = true;
	public static boolean WARN_OF_WRONG_SYNC_REQ_SK = false;
	public static boolean EXPORT_DDADDRESS_WITH_LOCALHOST = false; // should localhost addresses be in exported images?
	public static boolean VERIFY_SIGNATURE_MYPEER_IN_REQUEST = false; //for debugging my signature in requests
	public static boolean ADHOC_MESSAGES_USE_DICTIONARIES = true;
	public static boolean ADHOC_DD_IP_WINDOWS_DETECTED_WITH_NETSH = true; //for seeing the network IP when nobody is present (will broadcast messages wildly in such cases)
	public static boolean ADHOC_DD_IP_WINDOWS_DETECTED_ON_EACH_SEND = true;
	public static String ADHOC_DD_IP_WINDOWS_NETSH_IP_IDENTIFIER = "IP";
	public static String ADHOC_DD_IP_WINDOWS_IPCONFIG_IPv4_IDENTIFIER = "IPv4";
	public static String ADHOC_DD_IP_WINDOWS_NETSH_INTERFACE_IDENTIFIER = "Name"; // One may prefer to just extract first label in output
	public static String ADHOC_DD_IP_WINDOWS_NETSH_SSID_IDENTIFIER = "SSID";
	public static Refresh START_REFRESH = null;

	public static final String newsFields[] = new String[]{"global_news_ID","global_author_ID","date","news","type","signature"};
	public static final String newsFieldsTypes[] = new String[]{"TEXT","TEXT","TEXT","TEXT","TEXT","TEXT"};
	public static final String DD_DATA_CLIENT_UPDATES_INACTIVE_ON_START = "data_client_updates_on_start";
	public static final String DD_DATA_CLIENT_INACTIVE_ON_START = "data_client_on_start";
	public static final String DD_DATA_SERVER_ON_START = "data_server_on_start";
	public static final String DD_DATA_USERVER_INACTIVE_ON_START = "data_userver_on_start";
	public static final String DD_DIRECTORY_SERVER_ON_START = "directory_server_on_start";
	public static final String COMMAND_NEW_ORG = "COMMAND_NEW_ORG";
	public static final int MSGTYPE_SyncAnswer = 10;
	public static final int MSGTYPE_SyncRequest = 11;

	public static final String APP_NET_INTERFACES = "INTERFACES";
	public static final String APP_NON_ClientUDP = "!ClientUDP";
	public static final String APP_ClientTCP = "ClientTCP";
	public static final String APP_LISTING_DIRECTORIES = "listing_directories";
	public static final String APP_LISTING_DIRECTORIES_SEP = ",";
	public static final String APP_LISTING_DIRECTORIES_ELEM_SEP = ":";

	/*
	public static final String APP_INSTALLED_PLUGINS = "INSTALLED PLUGINS";
	public static final String APP_INSTALLED_PLUGINS_SEP = ",";
	public static final String APP_INSTALLED_PLUGINS_ELEM_SEP = ":";
	*/
	public static final String APP_stop_automatic_creation_of_default_identity = "stop_automatic_creation_of_default_identity";
	public static final String APP_hidden_from_my_peers = "hidden_from_my_peers";
	public static final String APP_my_global_peer_ID = "my_global_peer_ID";
	public static final String APP_my_peer_instance = "my_peer_instance";
	//public static final String APP_my_peer_name = "my_peer_name";
	//public static final String APP_my_peer_slogan = "my_peer_slogan";
	public static final String APP_my_global_peer_ID_hash = "my_global_peer_ID_hash";
	public static final String APP_ID_HASH = Cipher.SHA1; // default hash alg for new ID Cipher.MD5;
	public static final String APP_INSECURE_HASH = Cipher.MD5; // default hash alg for new ID
	public static final String APP_ORGID_HASH = Cipher.SHA256;  // default hash alg for new OrgID
	public static final String APP_ID_HASH_SEP = ":"; // default hash alg for new ID
	public static final String DD_WIRELESS_SERVER_ON_START = "WIRELESS_SERVER_ON_START";
	public static final String DD_CLIENT_SERVER_ON_START = "CLIENT_SERVER_ON_START";
	public static final String DD_SIMULATOR_ON_START = "SIMULATOR_ON_START";

	public static final String APP_LINUX_INSTALLATION_PATH = "SCRIPT_WIRELESS_LINUX_PATH";
	public static final String APP_WINDOWS_INSTALLATION_PATH = "SCRIPT_WIRELESS_WINDOWS_PATH";
	public static final String APP_LINUX_INSTALLATION_ROOT_PATH = "APP_LINUX_INSTALLATION_ROOT_PATH";
	public static final String APP_WINDOWS_INSTALLATION_ROOT_PATH = "SCRIPT_WIRELESS_WINDOWS_ROOT_PATH";

	public static final String BROADCASTING_PROBABILITIES = "BROADCASTING_PROBABILITIES";
	public static final String GENERATION_PROBABILITIES = "GENERATION_PROBABILITIES";
	
	public static final String PROB_CONSTITUENTS = "C";
	public static final String PROB_ORGANIZATIONS = "O";
	public static final String PROB_MOTIONS = "M";
	public static final String PROB_JUSTIFICATIONS = "J";
	public static final String PROB_WITNESSES = "W";
	public static final String PROB_NEIGHBORS = "N";
	public static final String PROB_VOTES = "V";
	public static final String PROB_PEERS = "P";
	public static final String PROB_SEP = ",";
	public static final String PROB_KEY_SEP = ":";
	
	public static final int WINDOWS = 1;
	public static final int LINUX = 2;
	public static final int MAC = 3;
	public static boolean DEBUG_PLUGIN = false;
	public static int OS = WINDOWS;
	
	public static String DEFAULT_DD_SSID = "DirectDemocracy";
	public static String DEFAULT_WIRELESS_ADHOC_DD_NET_MASK = "255.0.0.0";
	public static String DEFAULT_WIRELESS_ADHOC_DD_NET_IP_BASE = "10.0.0.";
	public static String DEFAULT_WIRELESS_ADHOC_DD_NET_BROADCAST_IP = "10.255.255.255";
	
	public static String DD_SSID = DEFAULT_DD_SSID;
	public static String WIRELESS_ADHOC_DD_NET_MASK = DEFAULT_WIRELESS_ADHOC_DD_NET_MASK;
	public static String WIRELESS_ADHOC_DD_NET_IP_BASE = DEFAULT_WIRELESS_ADHOC_DD_NET_IP_BASE;
	public static String WIRELESS_ADHOC_DD_NET_BROADCAST_IP = DEFAULT_WIRELESS_ADHOC_DD_NET_BROADCAST_IP;
	public static String WIRELESS_IP_BYTE; // last byte of 10.0.0.
	public static String WIRELESS_ADHOC_DD_NET_IP;
	public static final String APP_LAST_IP = "LAST_IP"; // last wireless adhoc broadcast IP

	public static final String APP_UPDATES_SERVERS = "UPDATES_SERVERS";
	public static final String APP_UPDATES_SERVERS_URL_SEP = ";";
	public static final String LATEST_DD_VERSION_DOWNLOADED = "LATEST_DD_VERSION_DOWNLOADED";
	public static final String TRUSTED_UPDATES_GID = "TRUSTED_UPDATES_GID";
	public static final String TRUSTED_UPDATES_GID_SEP = ",";
	public static final String BROADCASTING_QUEUE_PROBABILITIES = "BROADCASTING_QUEUE_PROBABILITIES";
	public static final String APP_Q_MD = "Q_MD";
	public static final String APP_Q_C = "Q_C";
	public static final String APP_Q_RA = "Q_RA";
	public static final String APP_Q_RE = "Q_RE";
	public static final String APP_Q_BH = "Q_BH";
	public static final String APP_Q_BR = "Q_BR";
	public static final int RSA_BITS_TRUSTED_FOR_UPDATES = 1<<12;
	public static final String APP_DB_TO_IMPORT = "APP_DB_TO_IMPORT";
	
	public static final String APP_LINUX_SCRIPTS_PATH = "APP_LINUX_SCRIPTS_PATH";
	public static final String APP_LINUX_PLUGINS_PATH = "APP_LINUX_PLUGINS_PATH";
	public static final String APP_LINUX_LOGS_PATH = "APP_LINUX_LOGS_PATH";
	public static final String APP_LINUX_DATABASE_PATH = "APP_LINUX_DATABASE_PATH";
	public static final String APP_LINUX_DD_JAR_PATH = "APP_LINUX_DD_JAR_PATH";
	
	public static final String APP_WINDOWS_SCRIPTS_PATH = "APP_WINDOWS_SCRIPTS_PATH";
	public static final String APP_WINDOWS_PLUGINS_PATH = "APP_WINDOWS_PLUGINS_PATH";
	public static final String APP_WINDOWS_LOGS_PATH = "APP_WINDOWS_LOGS_PATH";
	public static final String APP_WINDOWS_DATABASE_PATH = "APP_WINDOWS_DATABASE_PATH";
	public static final String APP_WINDOWS_DD_JAR_PATH = "APP_WINDOWS_DD_JAR_PATH";

	//static public DirectoryServer ds;
	//static public Server server;
	//static public Client client;
	//static public UDPServer userver;
	public static int MTU=32000;
	public static ArrayList<InetSocketAddress> directories_failed = new ArrayList<InetSocketAddress>();
	/**
	 * Use ClientUDP?
	 */
	public static boolean ClientUDP = true;
	public static boolean ClientTCP = false; //Should the client try TCP?
	
	public static EventDispatcher ed=new EventDispatcher();
	public static final String SERVE_DIRECTLY = "SERVE_DIRECTLY";
	public static final boolean DD_DATA_CLIENT_ON_START_DEFAULT = true;
	public static final boolean DD_DATA_CLIENT_UPDATES_ON_START_DEFAULT = true;
	public static final boolean DD_DATA_USERVER_ON_START_DEFAULT = true;
	public static final boolean ORG_UPDATES_ON_ANY_ORG_DATABASE_CHANGE = false;
	public static final String CONSTITUENT_PICTURE_FORMAT = "jpg";
	public static final String WIRELESS_SELECTED_INTERFACES = "WIRELESS_SELECTED_INTERFACES";
	public static final String WIRELESS_SELECTED_INTERFACES_SEP = ":";
	public static final long GETHOSTNAME_TIMEOUT_MILLISECONDS = (long)(1000*0.05);
	public static final String LAST_SOFTWARE_VERSION = "LAST_SOFTWARE_VERSION";
	public static final String DD_DB_VERSION = "DD_DB_VERSION";
	public static final String EMPTYDATE = "";
	public static final String UPDATES_TESTERS_THRESHOLD_WEIGHT = "UPDATES_TESTERS_THRESHOLD_WEIGHT";
	public static final String UPDATES_TESTERS_THRESHOLD_COUNT_VALUE = "UPDATES_TESTERS_THRESHOLD_COUNT_VALUE";
	public static final String UPDATES_TESTERS_THRESHOLD_WEIGHT_VALUE = "UPDATES_TESTERS_THRESHOLD_WEIGHT_VALUE";
	public static final String UPDATES_TESTERS_THRESHOLDS_RELATIVE = "UPDATES_TESTERS_THRESHOLDS_RELATIVE";
	public static final int UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT = 1;
	public static final float UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT = 0.0f;
	public static final int MAX_DISPLAYED_CONSTITUENT_SLOGAN = 100;
	public static final String WLAN_INTERESTS = "WLAN_INTERESTS";
	public static final boolean SUBMITTER_REQUIRED_FOR_EXTERNAL = false;
	public static final String P2PDDSQLException = null;
	public static boolean VERIFY_FRAGMENT_RECLAIM_SIGNATURE = false;
	public static boolean VERIFY_FRAGMENT_NACK_SIGNATURE = false;
	public static boolean VERIFY_FRAGMENT_ACK_SIGNATURE = false;
	public static boolean VERIFY_FRAGMENT_SIGNATURE = false;
	public static boolean PRODUCE_FRAGMENT_RECLAIM_SIGNATURE = false;
	public static boolean PRODUCE_FRAGMENT_NACK_SIGNATURE = false;
	public static boolean PRODUCE_FRAGMENT_ACK_SIGNATURE = false;
	public static boolean PRODUCE_FRAGMENT_SIGNATURE = false;
	public static final int FRAGMENTS_WINDOW = 10;
	public static final int FRAGMENTS_WINDOW_LOW_WATER = FRAGMENTS_WINDOW/2;
	public static final boolean AVOID_REPEATING_AT_PING = false;
	public static final boolean ORG_CREATOR_REQUIRED = false;
	public static final boolean CONSTITUENTS_ADD_ASK_TRUSTWORTHINESS = false;
	public static final String MY_DEBATE_TOPIC = "MY_DEBATE_TOPIC";
	public static final long LARGEST_BMP_FILE_LOADABLE = 10000000;
	public static final long PAUSE_BEFORE_CONNECTIONS_START = 5*1000;
	public static final long PAUSE_BEFORE_CLIENT_START = 4*1000; //after connections
	public static final long PAUSE_BEFORE_UDP_SERVER_START = 4*1000;
	public static final boolean DROP_DUPLICATE_REQUESTS = false;
	public static final int UDP_SENDING_CONFLICTS = 10; // how many requests are dropped waiting to send a message
	public static final boolean ACCEPT_UNSIGNED_CONSTITUENTS = false;
	public static final boolean ACCEPT_UNSIGNED_NEIGHBORHOOD = false;
	public static final boolean ACCEPT_UNSIGNED_PEERS_FROM_TABLES = false;
	public static final boolean ACCEPT_UNSIGNED_PEERS_FOR_STORAGE = false;
	public static final boolean DEBUG_CHANGED_ORGS = false;
	public static final boolean DEBUG_PRIVATE_ORGS = false;
	/**
	 * For debugging other peers (due to errors sent to us) set the next to true!
	 */
	public static final boolean WARN_ABOUT_OTHER = false;
	public static final boolean DEBUG_TODO = false;
	public static int MAX_MOTION_ANSWERTO_CHOICES = 100;
	/**
	 * 0 = undecided
	 * 1 = true
	 * -1 = false
	 */
	public static int AUTOMATE_PRIVATE_ORG_SHARING = 0; 
	public static boolean DEBUG_LIVE_THREADS = false;
	public static boolean DEBUG_COMMUNICATION = false;
	public static boolean DEBUG_COMMUNICATION_LOWLEVEL = false;
	//public static int TCP_MAX_LENGTH = 10000000;
	public static int UDP_MAX_FRAGMENT_LENGTH = 100000;
	public static int UDP_MAX_FRAGMENTS = 100;
	public static boolean WARN_ON_IDENTITY_CHANGED_DETECTION = false;
	public static boolean CONSTITUENTS_ORPHANS_SHOWN_BESIDES_NEIGHBORHOODS = true;
	public static boolean CONSTITUENTS_ORPHANS_FILTER_BY_ORG = true;
	public static boolean CONSTITUENTS_ORPHANS_SHOWN_IN_ROOT = false;
	public static boolean NEIGHBORHOOD_SIGNED_WHEN_CREATED_EMPTY = false; // otherwise they cannot be now edited!
	public static boolean ACCEPT_STREAMING_SYNC_REQUEST_PAYLOAD_DATA_FROM_UNKNOWN_PEERS = false;
	public static boolean ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS = true;
	public static long UDP_SERVER_WAIT_MILLISECONDS = 1000;
	public static long ADHOC_SENDER_SLEEP_MILLISECONDS = 5;
	public static boolean VERIFY_AFTER_SIGNING_NEIGHBORHOOD = true;
	public static boolean EDIT_VIEW_UNEDITABLE_NEIGHBORHOODS = true;
	public static boolean BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME = false;
	public static boolean BLOCK_NEW_ARRIVING_PEERS_ANSWERING_ME = false;
	public static boolean BLOCK_NEW_ARRIVING_PEERS_FORWARDED_TO_ME = false;
	//public static final String EMPTYDATE = "00000000000000.000Z";
	public static boolean BLOCK_NEW_ARRIVING_ORGS = false;
	public static boolean BLOCK_NEW_ARRIVING_ORGS_WITH_BAD_SIGNATURE = true;
	//public static boolean WARN_WRONG_SIGNATURE_RECEIVED = true; // duplicate for WARN_OF_FAILING_SIGNATURE_ONRECEPTION
    
	public static  boolean TEST_SIGNATURES = false;
	public static  boolean WARN_OF_UNUSED_PEERS = true;
	public static  boolean ACCEPT_DATA_FROM_UNSIGNED_PEERS = false;
	public static  boolean EDIT_RELEASED_ORGS = false;
	public static  boolean EDIT_RELEASED_JUST = false;
	public static  boolean ACCEPT_UNSIGNED_DATA = false;
	public static  boolean WARN_OF_INVALID_PLUGIN_MSG = true;
	public static  boolean DEFAULT_BROADCASTABLE_PEER_MYSELF = false;
	public static boolean WARN_OF_FAILING_SIGNATURE_ONRECEPTION = true;
	public static boolean WARN_OF_FAILING_SIGNATURE_ONSEND = true;;
	public static boolean DEFAULT_RECEIVED_PEERS_ARE_USED = false;
	public static boolean DEFAULT_AUTO_CONSTITUENTS_REFRESH = false;
	public static long UPDATES_WAIT_MILLISECONDS = 1000*60*10;
	public static long UPDATES_WAIT_ON_STARTUP_MILLISECONDS = 1000*60*5;
	public static boolean UPDATES_AUTOMATIC_VALIDATION_AND_INSTALL = true;
	public static boolean DELETE_UPGRADE_FILES_WITH_BAD_HASH = false;
	public static boolean ADHOC_WINDOWS_DD_CONTINUOUS_REFRESH = true;
	public static long ADHOC_EMPTY_TIMEOUT_MILLISECONDS = 1000*1; // 1 seconds
	public static long ADHOC_REFRESH_TIMEOUT_MILLISECONDS = 1000*1;
	public static int ADHOC_SERVER_CONSUMMER_BUFFER_SIZE = 20000;
	public static String TESTED_VERSION;
	public static boolean ACCEPT_STREAMING_ANSWER_FROM_ANONYMOUS_PEERS = false;
	public static boolean ACCEPT_STREAMING_ANSWER_FROM_NEW_PEERS = true;
	public static int ACCEPT_STREAMING_UPTO_MAX_PEERS = 1000;
	public static int FRAME_OFFSET = 100;
	public static int FRAME_WIDTH = 600;
	public static int FRAME_HSTART = 100;
	public static int FRAME_HEIGHT = 600;
	public static Calendar startTime;
	public static boolean VERIFY_SENT_SIGNATURES = true;
	public static boolean ACCEPT_STREAMING_REQUEST_UNSIGNED = false;
	public static boolean USE_NEW_ARRIVING_PEERS_CONTACTING_ME = true;
	public static boolean ASK_USAGE_NEW_ARRIVING_PEERS_CONTACTING_ME = true;
	public static long ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP = 0;
	public static int ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP = 1;
	public static byte[] Random_peer_Number;
	public static boolean SCRIPTS_ERRORS_WARNING = true;
	public static boolean WARNED_NO_DIRS = false;
	public static boolean REJECT_NEW_ARRIVING_PEERS_CONTACTING_ME = false;


    
	/**
	 * Needs testing,
	 * Needs tuning the amount of data advertisement sent for indirect ads (increase from current value)
	 * Need to set served orgs when broadcasting, or to disable broadcasting for orgs not in served_orgs
	 * 
	 * @param direct
	 */
	public static void serveDataDirectly(boolean direct){
		OrgHandling.SERVE_DIRECTLY_DATA = direct;
	}
	
	public final static boolean  preloadedControl = true;

	public static String[] get_preferred_charsets() throws P2PDDSQLException {
    	ArrayList<ArrayList<Object>> id;
    	id=Application.db.select("SELECT "+table.identity.preferred_charsets +
    			" FROM "+table.identity.TNAME+" AS i" +
    			" WHERE i."+table.identity.default_id+"==1 LIMIT 1;",
    			new String[]{});
    	if(id.size()==0){
    		if(DEBUG)System.err.println("No default identity found!");
    		return null;
    	}
    	String preferred_charsets = Util.getString(id.get(0).get(0));
    	if(preferred_charsets == null) return new String[]{};
    	return preferred_charsets.split(Pattern.quote(":"));
	}
	public static String get_authorship_charset() throws P2PDDSQLException {
    	ArrayList<ArrayList<Object>> id;
    	id=Application.db.select("SELECT "+table.identity.authorship_charset +
    			" FROM "+table.identity.TNAME+" AS i" +
    			" WHERE i."+table.identity.default_id+"==1 LIMIT 1;",
    			new String[]{});
    	if(id.size()==0){
    		if(DEBUG)System.err.println("No default identity found!");
    		return null;
    	}
    	return Util.getString(id.get(0).get(0));
	}
	public static Language get_authorship_lang() throws P2PDDSQLException {
    	ArrayList<ArrayList<Object>> id;
    	id=Application.db.select("SELECT "+table.identity.authorship_lang +
    			" FROM "+table.identity.TNAME+" AS i" +
    			" WHERE i."+table.identity.default_id+"==1 LIMIT 1;",
    			new String[]{});
    	if(id.size()==0){
    		if(DEBUG)System.err.println("No default identity found!");
    		return new Language("en","US");//null;
    	}
    	String alang= Util.getString(id.get(0).get(0));
    	String[] lang = alang.split(Pattern.quote("_"));
    	if(lang.length>=2)return new Language(lang[0],lang[1]);
    	return new Language(lang[0],lang[0]);
	}

	public static boolean test_proper_directory(String ld) {
    	String dirs[] = ld.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	//Identity.listing_directories_string.clear();
    	for(int k=0; k<dirs.length; k++) {
    		if(dirs[k] == null){
    			Application_GUI.warning(__("Test Error for "+dirs[k]), __("Error installing directories (null)"));
    			return false;
    		}
    		//String[] d=dirs[k].split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
    		
    		Address adr;
    		try {
    			adr = new Address(dirs[k]);
    		} catch (Exception e) {
    		//if(d.length!=2){
    			Application_GUI.warning(__("Error for")+" "+dirs[k]+"\nParsing Error: "+e.getMessage(), __("Error installing directories (impropper)"));
    			return false;
    		}
    		//Identity.listing_directories_string.add(dirs[k]);
    		try{
    			new InetSocketAddress(InetAddress.getByName(adr.getIP()),adr.getTCPPort());
    		} catch(Exception e) {
    			Application_GUI.warning(__("Error for")+" "+dirs[k]+"\nConnection Error: "+e.getMessage(), __("Error installing directories"));
    			return false;
    		}
    	}
		return true;
	}
	public static void load_listing_directories() throws P2PDDSQLException, NumberFormatException, UnknownHostException{
		DirectoryAddress dirs[] = DirectoryAddress.getActiveDirectoryAddresses();
		if ((dirs == null) || (dirs.length == 0)) {
			// Only reinit dirs if there is no directory (even inactive) 
			DirectoryAddress _dirs[] = DirectoryAddress.getDirectoryAddresses();
			if ((_dirs == null) || (_dirs.length == 0)) {
	     		String listing_directories;
				try {
					listing_directories = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
					if (DEBUG) System.out.println("DD: load_listing_directories: Got :"+listing_directories);
					DirectoryAddress.reset(listing_directories);
					dirs = DirectoryAddress.getActiveDirectoryAddresses();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
     	}
     	if ((dirs == null) || (dirs.length == 0)) {
    		if (! DD.WARNED_NO_DIRS) {
    			Application_GUI.warning(__("Currently there are no listing_directories for connections found at Connections initialization: " +
    					"\nDo not forget to add some later \n" +
    					"(e.g., from the DirectDemocracyP2P.net list)!\n" +
    					"If you have a stable IP, than you probably do not need it."), __("Configuration"));
    			DD.WARNED_NO_DIRS = true;
    		}
    		return;
    	}
     	/*
    	String ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
    	if(ld == null){
    		if(!DD.WARNED_NO_DIRS) {
    			new DDP2P_ServiceThread("Warning no listing directories", true) {
    				public void _run() {
    					Application.warning(_("You have not yet configured listing directories. " +
    							"\nSuch directories allow you to travel and use multiple devices." +
    							"\nDo not forget to add some later \n" +
    							"(e.g., from the list found DirectDemocracyP2P.net)!\n" +
    							"If you have a stable IP, than you probably do not need it."), _("Configuration"));
    				}
    			}.start();
    			DD.WARNED_NO_DIRS = true;
    		}
    		return;
    	}
    	String dirs[] = ld.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	*/
    	Identity.listing_directories_string.clear();
    	Identity.listing_directories_inet.clear(); // just added
    	Identity.listing_directories_addr.clear();
    	for (int k=0; k<dirs.length; k++) {
    		//String[] d=dirs[k].split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
    		try{
        		Address adr = new Address(dirs[k]);
        		InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(adr.getIP()),adr.getTCPPort());
        		adr.inetSockAddr = isa;
        		Identity.listing_directories_addr.add(adr);
        		Identity.listing_directories_string.add(dirs[k].toString());
    			Identity.listing_directories_inet.add(isa);
    		} catch (Exception e) {
    			Application_GUI.warning(__("Error for")+" "+dirs[k]+"\nLoad Error: "+e.getMessage(), __("Error installing directories"));
    			e.printStackTrace();
    		}
    	}
	}
	static public boolean setAppTextNoSync(String field, String value) throws P2PDDSQLException{
		synchronized(Application.db){
			ArrayList<ArrayList<Object>> rows = Application.db.select("SELECT "+table.application.value+
					" FROM "+table.application.TNAME+
					" WHERE "+table.application.field+"=?;",
					new String[]{field});
			if(rows.size()>0){
				String oldvalue = Util.getString(rows.get(0).get(0));
				if(((oldvalue==null) && (value==null)) || 
					((oldvalue!=null) && (value!=null) && oldvalue.equals(value))) return true;
				Application.db.updateNoSync(
					table.application.TNAME,
					new String[]{table.application.value},
					new String[]{table.application.field},
					new String[]{value, field});
			}else{
					try{
						Application.db.insertNoSync(table.application.TNAME, new String[]{table.application.field, table.application.value}, new String[]{field, value});
					}catch(Exception e){
						e.printStackTrace();
						Application_GUI.warning(__("Error inserting:")+"\n"+__("value=")+Util.trimmed(value)+"\n"+__("field=")+field+"\n"+__("Error:")+e.getLocalizedMessage(), __("Database update error"));
					}
					if(DEBUG){
						Application_GUI.warning(__("Added absent property: ")+field, __("Properties"));
					
						System.err.println("Why absent");
						Util.printCallPath("");
					}
			}
//			if (value!=null){
//				String actual = getExactAppText(field);
//				if ((actual == null) || !value.equals(actual)) {
//						System.err.println(_("Error inserting:")+"\n"+_("value=")+value+"\n"+_("field=")+field+"\nold="+actual);
//				Application.db.insertNoSync(table.application.TNAME, new String[]{table.application.field, table.application.value}, new String[]{field, value});
//				}
//			}
		}
		return true;
	}
	/**
	 * Uses Application.db, which should be set to the right DB
	 * @param field
	 * @param value
	 * @return
	 * @throws P2PDDSQLException
	 */
	static public boolean setAppText(String field, String value) throws P2PDDSQLException{
		return setAppText(field,value,false);
	}
	public static boolean setAppText(String field, String value,
			boolean debug) throws P2PDDSQLException {
		return setAppText(Application.db, field, value, debug);
	}
	public static boolean setAppText(DBInterface db, String field, String value,
			boolean debug) throws P2PDDSQLException {
		boolean DEBUG = DD.DEBUG || debug;
		if(DEBUG) System.err.println("DD:setAppText: field="+field+" new="+value);
		String _value = getExactAppText(db.getImplementation(), field);
		if(DEBUG) System.err.println("DD:setAppText: field="+field+" old="+_value);
    	db.update(table.application.TNAME, new String[]{table.application.value}, new String[]{table.application.field},
    			new String[]{value, field}, DEBUG);
    	if (value!=null){
    		String old_val = getExactAppText(db.getImplementation(), field);
    		if(DEBUG) System.err.println("DD:setAppText: field="+field+" old="+old_val);
    		if (!value.equals(old_val)) {
    			db.insert(
    					table.application.TNAME,
    					new String[]{table.application.field, table.application.value},
    					new String[]{field, value},
    					DEBUG);
    			if(DEBUG)Application_GUI.warning(__("Added absent property: ")+field, __("Properties"));
    		}
    	}
    	/* //was used to debug when the error was a wrong Application.db object 
    	else{
    		if(DEBUG) System.err.println("DD:setAppText: field="+field+" set null");
    		String old_val = getExactAppText(field);
    		if(DEBUG) System.err.println("DD:setAppText: field="+field+" _old="+old_val);
    		if(old_val!=null){
    			if(DEBUG)Application.warning(_("Deleting property: ")+field+" old_value", _("Properties"));
    			int q=Application.ask(_("Want to force delete property:")+" "+field, _("Property"), JOptionPane.OK_CANCEL_OPTION);
    			if(q==0)Application.db.delete(table.application.TNAME, new String[]{table.application.field},
    					new String[]{field}, DEBUG);
    		}
    	}
    	*/
		return true;
	}
	static public boolean setAppBoolean(String field, boolean val){
		String value = Util.bool2StringInt(val);
		try {
			return setAppText(field, value);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	/**
	 * For empty string "" it returns null;
	 * @param field
	 * @return
	 * @throws P2PDDSQLException
	 */
	static public String getAppText(String field) throws P2PDDSQLException {
		String result = getExactAppText(field);
   		if("".equals(result)) result = null;
   		return result;
	}
	/**
	 * Exact value needed for exact comparison with new valued to preclude reinsertion
	 * @param field
	 * @return
	 * @throws P2PDDSQLException
	 */
	static public String getExactAppText(String field) throws P2PDDSQLException{
		return getExactAppText(Application.db.getImplementation(), field);
	}
	/**
	 * 
	 * @param db
	 * @param field
	 * @return
	 * @throws P2PDDSQLException
	 */
	static public String getExactAppText(DB_Implementation db, String field) throws P2PDDSQLException{
    	ArrayList<ArrayList<Object>> id;
    	id=db.select("SELECT "+table.application.value +
    			" FROM "+table.application.TNAME+" AS a " +
    			" WHERE "+table.application.field+"=? LIMIT 1;",
    			new String[]{field}, DEBUG);
    	if(id.size()==0){
    		if(DEBUG) System.err.println(__("No application record found for field: ")+field);
    		return null;
    	}
    	String result = Util.getString(id.get(0).get(0));
   		return result;
	}

	public static boolean getAppBoolean(String field, boolean _def) {
    	String aval = null;
		try {
			aval = DD.getExactAppText(field);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}//Util.getString(id.get(0).get(0));
		if (aval==null) return false;
		if(aval.equals("1")) return true; 
		return false;
	}

	static public boolean getAppBoolean(String field) throws P2PDDSQLException{
		return getAppBoolean(field, false);
		/*
    	ArrayList<ArrayList<Object>> id;
    	id=Application.db.select("SELECT "+table.application.value+
    			" FROM "+table.application.TNAME+" AS a " +
    			" WHERE "+table.application.field+"=? LIMIT 1;",
    			new String[]{field});
    	if(id.size()==0){
    		if(DEBUG) System.err.println(_("No boolean application record found for field: ")+field);
    		return false;
    	}
    	*/
	}
	static public boolean startDirectoryServer(boolean on, int port) throws NumberFormatException, P2PDDSQLException {
		DirectoryServer ds= Application.ds;
		
		if (on == false) {
			if (ds != null) {
				ds.turnOff();
				Application.ds=null;
				//DirectoryServer.db=null;
				if(DEBUG)System.out.println("DD:startDirectoryServer:Turning off");
				return true;
			} else {
				return false;
			}
		}
		// if on = true
		if (ds != null) {
			if(DEBUG)System.out.println("DD:startDirectoryServer:Turned off already");
			return false;
		}
		if (port <= 0) {
			String ds_port = getAppText("DirectoryServer_PORT");
			if(DEBUG)System.out.println("DD:startDirectoryServer:Saved port="+ds_port);
			if(ds_port!=null)port = Integer.parseInt(ds_port);
			else port = DirectoryServer.PORT;
		}
		try {
			Application.ds = new DirectoryServer(port);
			Application.ds.start();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	static public boolean startServer(boolean on, Identity peer_id) throws NumberFormatException, P2PDDSQLException {
		Server as = Application.as;
		if(DEBUG)System.err.println("Will set server as="+as+" id="+peer_id);
		if (on == false) {
			if (as != null) {
				as.turnOff(); Application.as=null;
				if(DEBUG)System.err.println("Turned off");
				return true;
			} else {
				return false;
			}
		}
		// for on = true
		if (as != null){
			if(DEBUG)System.err.println("Was not null");
			return false;
		}
		try {
			Application.as = new Server(peer_id);
			Application.as.start();
		} catch (Exception e) {
			if(DEBUG)System.err.println("Error:"+e);
			//e.printStackTrace();
			return false;
		}
		return true;
	}
	static public boolean startUServer(boolean on, Identity peer_id) throws NumberFormatException, P2PDDSQLException {
		//boolean DEBUG = true;
		UDPServer aus = Application.aus;
		if(DEBUG) System.err.println("Will set server aus="+aus+" id="+peer_id);
		if (on == false) {
			if (aus != null) {
				aus.turnOff(); Application.aus=null;
				if(DEBUG) System.err.println("Turned off");
				return true;
			} else {
				return false;
			}
		}
		// if on = true
		if (aus != null) {
			if (DEBUG) System.err.println("Was not null");
			return false;
		}
		try {
			if(DEBUG) System.err.println("DD:startUServ: <init>");
			Application.aus = new UDPServer(peer_id);
			if(DEBUG) System.err.println("DD:startUServ: <init> done, start");
			Application.aus.start();
		} catch (Exception e) {
			if(DEBUG) System.err.println("Error:"+e);
			//e.printStackTrace();
			return false;
		}
		return true;
	}
	static public boolean startClient(boolean on) throws NumberFormatException, P2PDDSQLException {
		IClient ac = Application.ac;
		
		if (on == false) {
			if (ac != null) {
				ac.turnOff();
				Application.ac=null;
				return true;
			} else {
				return false;
			}
		}
		// Here on = true
		if(ac != null) return false;
		try {
			Application.ac = ClientSync.startClient();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	static public void touchClient() throws NumberFormatException, P2PDDSQLException {
		IClient ac = Application.ac;
		if(ac==null) {
			startClient(true);
			ac = Application.ac;
		}
		ac.wakeUp();
	}

	public static SK getConstituentSK(long constituentID) throws P2PDDSQLException {
		String constGID = D_Constituent.getGIDFromLID(constituentID);
		return Util.getStoredSK(constGID);
	}
	public static void setBroadcastServerStatus(boolean run) {
		if(run) {
			if(Application.g_BroadcastServer != null) return;
			try {
				Application.g_BroadcastServer = new BroadcastServer();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
			Application.g_BroadcastServer.start();
		}else{
			if(Application.g_BroadcastServer == null) return;
			Application.g_BroadcastServer.stopServer();
			Application.g_BroadcastServer=null;
		}
		Application_GUI.setBroadcastServerStatus_GUI(run);
	}
	public static void setBroadcastClientStatus(boolean run) {
		if(run) {
			if(Application.g_BroadcastClient != null) return;
			try {
				Application.g_BroadcastClient = new BroadcastClient();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
			Application.g_BroadcastClient.start();
		}else{
			if(Application.g_BroadcastClient == null) return;
			Application.g_BroadcastClient.stopClient();
			Application.g_BroadcastClient=null;
		}		
		Application_GUI.setBroadcastClientStatus_GUI(run);
	}
	public static void setSimulatorStatus(boolean run) {
		if(run) {
			if(Application.g_Simulator != null) return;
			Application.g_Simulator = new Fill_database();
			Application.g_Simulator.start();
		}else{
			if(Application.g_Simulator == null) return;
			Application.g_Simulator.stopSimulator();
			Application.g_Simulator=null;
		}
		Application_GUI.setSimulatorStatus_GUI(run);
	}
	/**
	 * Called from simulator generating data
	 * @param keys
	 * @param name
	 * @throws P2PDDSQLException
	 */
	public static void storeSK(Cipher keys, String name) throws P2PDDSQLException{
		storeSK(keys, name+Util.getGeneralizedTime(), null, null, null);
	}
	/**
	 * 
	 * @param keys
	 * @param name
	 * @param date
	 * @throws P2PDDSQLException
	 */
	public static void storeSK(Cipher keys, String name, String date) throws P2PDDSQLException{
		storeSK(keys, name, null, null, null, date);
	}
	/**
	 * 
	 * @param keys
	 * @param pGIDname
	 * @param public_key_ID
	 * @param secret_key
	 * @param pGIDhash
	 * @throws P2PDDSQLException
	 */
	public static void storeSK(Cipher keys, String pGIDname, 
			String public_key_ID, String secret_key, String pGIDhash) throws P2PDDSQLException{
		storeSK(keys, pGIDname, public_key_ID, secret_key, pGIDhash, Util.getGeneralizedTime());
	}
	/**
	 * s
	 * @param keys
	 * @param pGIDname
	 * @param public_key_ID
	 * @param secret_key
	 * @param pGIDhash
	 * @param date
	 * @throws P2PDDSQLException
	 */
	public static void storeSK(Cipher keys, String pGIDname, String public_key_ID, String secret_key, String pGIDhash, String date) throws P2PDDSQLException{
		if (public_key_ID ==  null) {
			byte[] pIDb = Util.getKeyedIDPKBytes(keys);
			public_key_ID = Util.getKeyedIDPK(pIDb);
			if(DEBUG) System.out.println("DD:storeSK public key: "+public_key_ID);
		}
		if (secret_key == null) {
			secret_key = Util.getKeyedIDSK(keys);
			if(DEBUG) System.out.println("DD:storeSK secret key: "+secret_key);
		}
		if (pGIDhash == null) {
			pGIDhash = Util.getGIDhash(public_key_ID);
			if(DEBUG) System.out.println("DD:storeSK public key hash: "+pGIDhash);
		}
		//String date = Util.getGeneralizedTime();
		if(pGIDname == null) pGIDname = "KEY:"+date;
		Application.db.insert(table.key.TNAME,
				new String[]{table.key.public_key,table.key.secret_key,table.key.ID_hash,table.key.creation_date,
				table.key.name,table.key.type},
				new String[]{public_key_ID, secret_key, pGIDhash,date,
				/*Util.getKeyedIDPKhash(pIDb)*/
				pGIDname,
				Util.getKeyedIDType(keys)}, DEBUG);
	}
	public static final int[] VERSION_INTS = Util.getVersion(VERSION);
	public static final boolean SIGN_DIRECTORY_ANNOUNCEMENTS = false;
	public static final boolean KEEP_UNCERTIFIED_SOCKET_ADDRESSES = false;
	public static String WARN_OF_INVALID_SCRIPTS_BASE_DIR = null;
	public static final int SIZE_DA_PREFERRED = 1000;
	public static final int SIZE_DA_MAX = 10000;
	public static final Object status_monitor = new Object();
	public static final int MAX_DPEER_UNCERTIFIED_ADDRESSES = 5;
	public static final boolean DIRECTORY_ANNOUNCEMENT_UDP = true;
	public static final boolean DIRECTORY_ANNOUNCEMENT_TCP = true;
	//public static final String MISSING_PEERS = "MISSING_PEERS";
	//public static final String MISSING_NEWS = "MISSING_NEWS";
	public static final String WINDOWS_NO_IP = "No IP"; // to signal no IP4 in ipconfig
	public static final String ALREADY_CONTACTED = __("Already contacted ***");
	/**
	 * Peers
	 */
	// Different icons should be displayed for each state... for now just on/off
	public static final int PEERS_STATE_CONNECTION_FAIL =0;
	public static final int PEERS_STATE_CONNECTION_TCP = 1;
	public static final int PEERS_STATE_CONNECTION_UDP = 2;
	public static final int PEERS_STATE_CONNECTION_UDP_NAT = 3;
	public static final boolean STREAMING_TABLE_PEERS = false;
	public static final boolean STREAMING_TABLE_PEERS_ADDRESS_CHANGE = false;
	public static final boolean ANONYMOUS_ORG_ACCEPTED = true; 
	public static final boolean ANONYMOUS_ORG_AUTHORITARIAN_CREATION = true; 
	public static final boolean ANONYMOUS_ORG_GRASSROOT_CREATION = true; 
	public static boolean ANONYMOUS_ORG_ENFORCED_AT_HANDLING = false;
	public static final boolean ANONYMOUS_CONST_ACCEPTED = true; 
	public static final boolean ANONYMOUS_CONST_CREATION = true; 
	public static final boolean ANONYMOUS_MOTI_ACCEPTED = true; 
	public static final boolean ANONYMOUS_MOTI_CREATION = true; 
	public static final boolean ANONYMOUS_JUST_ACCEPTED = true; 
	public static final boolean ANONYMOUS_JUST_CREATION = true; 
	public static final boolean ANONYMOUS_NEWS_ACCEPTED = true; 
	public static final boolean ANONYMOUS_NEWS_CREATION = true; 
	public static final boolean VERIFY_GIDH_ALWAYS = false;
	public static final String NO_CONTACT = "No contact";
	public static final int CLIENTS_NB_MEMORY = 100; // how much  memory assigned to recent clients to avoid duplication & DOS
	public static final int CLIENTS_RANDOM_MEMORY = 10; // max size of randomID for ASNSyncRequest
	public static final String APP_CLAIMED_DATA_HASHES = "CLAIMED_DATA_HASHES";
	public static final long DOMAINS_UPDATE_WAIT = 1000 * 200;
	/**
	 *  // when motions signatures were deleted by error, this fixes those who were signed with a key I know.
	 */
	public static final boolean FIX_UNSIGNED_MOTIONS = false;
	/**
	 * Accept unsigned motions (but normally accompanied with some signatures
	 */
	public static final boolean ACCEPT_ANONYMOUS_MOTIONS = true;
	/**
	 * We can use the null name to detect container D_Organization data that only encode the GIDH of the orh in a message.
	 * That is possible only when ACCEPT_ORGANIZATIONS_WITH_NULL_NAME is FALSE
	 */
	public static final boolean ACCEPT_ORGANIZATIONS_WITH_NULL_NAME = false;
	
	public static boolean RELEASE = true;
	/** dir_IP: (GID: ()addresses) */
	public static Hashtable<String,Hashtable<String,DirectoryAnswerMultipleIdentities>> dir_data = new Hashtable<String,Hashtable<String,DirectoryAnswerMultipleIdentities>>();

	/**
	 * Is the data for me as constituent fully input?
	 * @param organization_ID
	 * @param constituent_ID
	 * @return
	 */
	public boolean isMyConstituentReady(long constituent_ID){
		D_Constituent c = D_Constituent.getConstByLID(constituent_ID, true, false);
		if (c != null && c.getSurName() != null) return true;
		return false;
		/*
		try{
			String sql = "SELECT "+table.constituent.name+" FROM "+table.constituent.TNAME+
			" WHERE "+table.constituent.constituent_ID+"=?;";
			ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{""+constituent_ID});
			if ((a.size()>=1) && (a.get(0).get(0)!=null)) return true;
		}catch(Exception e){}
		return false;
		*/
	}
	/**
	 * String explain[] = new String[1];
	 * if (! config.DD.embedPeerInBMP (file, explain, new util.DD_Address(peer)))
	 * 	//DisplayError(explain[0]);
	 * 
	 * 
	 * Hopefully somebody will create additional StegoStructures to holding (helping to import-export)
	 * objects of type D_Motion, D_Organization, D_Vote (containing eventually the corresponding D_Constituent, D_Vote objects)
	 * 
	 * @param file : handle to the file where the result will be stored. 
	 * @param explain : array of strings with size "1", to store an explanation in case of error.
	 * @param myAddress : An object that will be stored in the bitmap. Can be any subclass of StegoStructure,
	 *  such as:
	 *     util.DD_Address,  (for holding a peer)
	 *     util.DD_Slogan,  (for holding nice slogans to be imported)
	 *     util.DD_Testers,  (for holding a information about testers that voluteer to evaluate new releases)
	 *     util.DD_DirectoryServer, (for holding the address of a directory server)
	 *     util.DD_EmailableAttachment (to verify somebody's identity, .. I forgot in which step)
	 *     util.DD_IdentityVerification_Answer (to carry the answer to a identity verification challenge)
	 *     util.DD_IdentityVerification_Request (to carry the challenge for an identity verification)
	 *     
	 *     
	 * @return returns true on success
	 */
	public static boolean embedPeerInBMP(File file,
			String explain[],
			StegoStructure myAddress
			) {
		BMP[] _data = new BMP[1];
		byte[][] _buffer_original_data = new byte[1][]; // old .bmp file 
		byte[] adr_bytes = myAddress.getBytes();
		
		if (file.exists()) {
			boolean fail = EmbedInMedia.cannotEmbedInBMPFile(file, adr_bytes, explain, _buffer_original_data, _data);
			
			if (fail) {
				if (_DEBUG) System.out.println("DD: embedPeerInBMP: failed embedding in existing image: "+explain[0]);
				return false;
			}
		}
		
		if ( EmbedInMedia.DEBUG ) System.out.println("EmbedInMedia:actionExport:bmp");
		try {
			if ( ! file.exists()) {
				EmbedInMedia.saveSteganoBMP(file, adr_bytes, myAddress.getSignShort()); //DD.STEGO_SIGN_PEER);
			} else {
				FileOutputStream fo;
				fo = new FileOutputStream(file);
				int offset = _data[0].startdata;
				int word_bytes = 1;
				int bits = 4;
				////Util.copyBytes(b, BMP.CREATOR, adr_bytes.length);
				fo.write(EmbedInMedia.getSteganoBytes(adr_bytes, _buffer_original_data[0], offset, word_bytes, bits, myAddress.getSignShort()));
				fo.close();
			}
		} catch (IOException e) {
			if (explain != null && explain.length > 0) explain[0] = e.getLocalizedMessage();
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	/**
		StegoStructure adr[] = DD.getAvailableStegoStructureInstances();
		int[] selected = new int[1];
		String error;
		if ((error = DD.loadBMP(adr, selected)) == null)	
			adr[selected[0]].save();
		else
			// DisplayErrorMessage(error);
		
		On success, call the following:
		adr[selected[0].save();
	 * 
	 * @param file
	 * @param adr
	 * @param selected
	 * @return null on success, otherwise returns explanation of error.
	 * @throws IOException
	 */
	public static String loadBMP(File file, StegoStructure[] adr, int[] selected) throws IOException {
		String explain="";
		boolean fail= false;
		FileInputStream fis=new FileInputStream(file);
		byte[] b = new byte[(int) file.length()];
		fis.read(b);
		fis.close();
		BMP data = new BMP(b, 0);
	
		if ((data.compression != BMP.BI_RGB) || (data.bpp < 24)) {
			explain = " - "+__("Not supported compression: "+data.compression+" "+data.bpp);
			fail = true;
		} else {
			int offset = data.startdata;
			int word_bytes = 1;
			int bits = 4;
			try {
				EmbedInMedia.setSteganoBytes(adr, selected, b, offset, word_bytes, bits);
			} catch (ASN1DecoderFail e1) {
				explain = " - "+ __("No valid data in picture!");
				fail = true;
			}
		}
		if (fail) {
			return null;
		}
		return explain;
	}
	/**
	 * Create a constituent without name (never called?)
	 * 
	 * first, on ConstituentTree or Orgs, select/create an Identity
	 * In the create Identity, have to select/create a key, slogan, then call this function
	 * 
	 * Later go to ConstituentTree and in popup select "Register" to finalize adding one's address and name.
	 * The name is defining the end of the registration in grass-root
	 * In authoritarian, will wait certificate
	 * 
	 * @param organization_ID
	 * @param key_ID
	 */
	/*
	public static long createConstituent(long organization_ID, HashConstituent data) {
		long result = -1;
		try {
			String hash_constituent = Util.stringSignatureFromByte(data.encode());//Util.byteToHex(data.encode());
			result = Application.db.insert(table.constituent.TNAME,
					new String[]{table.constituent.global_constituent_ID,
					table.constituent.global_constituent_ID_hash,
					table.constituent.organization_ID,
					table.constituent.hash_constituent_alg,
					table.constituent.hash_constituent,
					table.constituent.slogan,
					table.constituent.creation_date
					},
					new String[]{
					data.global_constituent_ID,
					data.global_constituent_ID_hash,
					""+organization_ID,
					table.constituent.CURRENT_HASH_CONSTITUENT_ALG,
					hash_constituent,
					data.slogan,
					data.creation_date
					});
		} catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}
*/
	public static void load_broadcast_probabilities(String val) {
		if(val==null) return;
		String[] probs= val.split(Pattern.quote(DD.PROB_SEP));
		float constit = Broadcasting_Probabilities.broadcast_constituent;
		float orgs = Broadcasting_Probabilities.broadcast_organization;
		float motions = Broadcasting_Probabilities.broadcast_motion;
		float justifications = Broadcasting_Probabilities.broadcast_justification;
		float witness = Broadcasting_Probabilities.broadcast_witness;
		float neighbors = Broadcasting_Probabilities.broadcast_neighborhood;
		float votes = Broadcasting_Probabilities.broadcast_vote;
		float peers = Broadcasting_Probabilities.broadcast_peer;
		for(String e: probs) {
			if(e==null) continue;
			String prob[] = e.split(Pattern.quote(DD.PROB_KEY_SEP));
			if(prob.length<2) continue;
			if(DD.PROB_CONSTITUENTS.equals(prob[0])) constit = new Float(prob[1]).floatValue();
			if(DD.PROB_ORGANIZATIONS.equals(prob[0])) orgs = new Float(prob[1]).floatValue();
			if(DD.PROB_MOTIONS.equals(prob[0])) motions = new Float(prob[1]).floatValue();
			if(DD.PROB_JUSTIFICATIONS.equals(prob[0])) justifications = new Float(prob[1]).floatValue();
			if(DD.PROB_WITNESSES.equals(prob[0])) witness = new Float(prob[1]).floatValue();
			if(DD.PROB_NEIGHBORS.equals(prob[0])) neighbors = new Float(prob[1]).floatValue();
			if(DD.PROB_VOTES.equals(prob[0])) votes = new Float(prob[1]).floatValue();
			if(DD.PROB_PEERS.equals(prob[0])) peers = new Float(prob[1]).floatValue();
		}
		float sum = constit + orgs + motions + justifications + witness + neighbors + votes + peers;
		constit = constit/sum;
		orgs = orgs/sum;
		motions = motions/sum;
		justifications = justifications/sum;
		witness = witness/sum;
		neighbors = neighbors/sum;
		votes = votes/sum;
		peers = peers/sum;

		Broadcasting_Probabilities.broadcast_constituent = constit;
		Broadcasting_Probabilities.broadcast_organization = orgs;
		Broadcasting_Probabilities.broadcast_motion = motions;
		Broadcasting_Probabilities.broadcast_justification = justifications;
		Broadcasting_Probabilities.broadcast_witness = witness;
		Broadcasting_Probabilities.broadcast_neighborhood = neighbors;
		Broadcasting_Probabilities.broadcast_vote = votes;
		Broadcasting_Probabilities.broadcast_peer = peers;
	}

	public static void load_generation_probabilities(String val) {
		if(val==null) return;
		String[] probs= val.split(Pattern.quote(DD.PROB_SEP));
		float constit = Broadcasting_Probabilities.broadcast_constituent;
		float orgs = Broadcasting_Probabilities.broadcast_organization;
		float motions = Broadcasting_Probabilities.broadcast_motion;
		float justifications = Broadcasting_Probabilities.broadcast_justification;
		float witness = Broadcasting_Probabilities.broadcast_witness;
		float neighbors = Broadcasting_Probabilities.broadcast_neighborhood;
		float votes = Broadcasting_Probabilities.broadcast_vote;
		float peers = Broadcasting_Probabilities.broadcast_peer;
		for(String e: probs) {
			if(e==null) continue;
			String prob[] = e.split(Pattern.quote(DD.PROB_KEY_SEP));
			if(prob.length<2) continue;
			if(DD.PROB_CONSTITUENTS.equals(prob[0])) constit = new Float(prob[1]).floatValue();
			if(DD.PROB_ORGANIZATIONS.equals(prob[0])) orgs = new Float(prob[1]).floatValue();
			if(DD.PROB_MOTIONS.equals(prob[0])) motions = new Float(prob[1]).floatValue();
			if(DD.PROB_JUSTIFICATIONS.equals(prob[0])) justifications = new Float(prob[1]).floatValue();
			if(DD.PROB_WITNESSES.equals(prob[0])) witness = new Float(prob[1]).floatValue();
			if(DD.PROB_NEIGHBORS.equals(prob[0])) neighbors = new Float(prob[1]).floatValue();
			if(DD.PROB_VOTES.equals(prob[0])) votes = new Float(prob[1]).floatValue();
			if(DD.PROB_PEERS.equals(prob[0])) peers = new Float(prob[1]).floatValue();
		}
		float sum = constit + orgs + motions + justifications + witness + neighbors + votes + peers;
		constit = constit/sum;
		orgs = orgs/sum;
		motions = motions/sum;
		justifications = justifications/sum;
		witness = witness/sum;
		neighbors = neighbors/sum;
		votes = votes/sum;
		peers = peers/sum;

		SimulationParameters.adding_new_constituent = constit;
		SimulationParameters.adding_new_organization = orgs;
		SimulationParameters.adding_new_motion = motions;
		SimulationParameters.adding_new_justification_in_vote = justifications;
		SimulationParameters.adding_new_witness = witness;
		SimulationParameters.adding_new_neighbor = neighbors;
		SimulationParameters.adding_new_vote = votes;
		SimulationParameters.adding_new_peer = peers;		
	}
	/**
	 * Return error message in case of error, null on success
	 * As side effect it sets Application.db
	 * @param attempt
	 * @return
	 */
	public static String testProperDB(String attempt) {
		File dbfile = new File(attempt);
		DD.TESTED_VERSION = null;
		if(!dbfile.exists() || !dbfile.isFile() || !dbfile.canRead()) return __("File not readable.");
		try{
			Application.db = new DBInterface(attempt);
			ArrayList<ArrayList<Object>> v = Application.db.select(
					"SELECT "+table.application.value+" FROM "+table.application.TNAME+
					" WHERE "+table.application.field+"=? LIMIT 1;",
					new String[]{DD.DD_DB_VERSION}, DEBUG);
//			ArrayList<ArrayList<Object>> v = Application.db.select(
//					"SELECT * FROM "+table.application.TNAME+
//					" LIMIT 1;",
//					new String[]{}, DEBUG);
			if(v.size()>0)DD.TESTED_VERSION=Util.getString(v.get(0).get(0));
		}catch(Exception e){
			try {
				Application.db.close();
			} catch (util.P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			Application.db = null;
			e.printStackTrace();
			return e.getLocalizedMessage();
		}
		return null;
	}
	/**
	 * Return error message in case of errr, null on success
	 * @param attempt
	 * @return
	 */
	public static String try_open_database(String attempt) {
		String error = testProperDB(attempt);
		if(error==null) {
			Application.DELIBERATION_FILE = attempt;
			return null;
		}else{
			return error;
		}
	}
	

	public static DBInterface load_Directory_DB(String dB_PATH) {
		DBInterface dbdir = null;
		String sql = "SELECT "+table.subscriber.subscriber_ID+
				" FROM "+table.subscriber.TNAME+
				" LIMIT 1;";
		try {
			String[]params = new String[]{};
			String dbase = Application.DIRECTORY_FILE;
			if(dB_PATH!=null) dbase = dB_PATH+Application.OS_PATH_SEPARATOR+dbase;
			dbdir = DirectoryServer.getDirDB(dbase);
			//dbdir = new DBInterface(dbase);
			dbdir.select(sql, params, DEBUG);
		} catch (util.P2PDDSQLException e) {
			System.out.print(sql);
			e.printStackTrace();
			return null;
		}
		
		return dbdir;
	}

	public static boolean asking_topic = false;

	public static boolean isThisAnApprovedPeer(String senderID) {
		// TODO Auto-generated method stub
		return true;
	}
	public static boolean GUI = true;
	//static final String CONSOLE="CONSOLE";
	// parameters: 
	// last parameter is the database (if different from CONSOLE)
	// the parameter 1 is the GID of the peer
	//
	// Should be: -d database, -p peerGID, -c [for colsole]
	/**
	 * @deprecated Use {@link MainFrame#main(String[])} instead
	 */
/*
	static public void main(String args[]) throws util.P2PDDSQLException {
		MainFrame.main(args);
	}
*/
	static public void set_DEBUG(){
		/*
		DD.DEBUG = true;
		DD.DEBUG_LIVE_THREADS = true;
		DD.DEBUG_COMMUNICATION = true;
		ClientSync.DEBUG = true;
		Connections.DEBUG=true;
		D_Constituent.DEBUG = true;
		D_Witness.DEBUG = true;
		D_Organization.DEBUG = true;
		D_Neighborhood.DEBUG = true;
		UpdateMessages.DEBUG = true;
		OrgHandling.DEBUG = true;
		SpecificRequest.DEBUG = true;
		WB_Messages.DEBUG = true;
		WitnessingHandling.DEBUG = true;
		NeighborhoodHandling.DEBUG = true;
		ConstituentHandling.DEBUG = true;
		/*
		*/
	}
}
