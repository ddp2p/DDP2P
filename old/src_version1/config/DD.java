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
 
import static java.lang.System.out;
import static util.Util._;
import hds.Client1;
import hds.ClientSync;
import hds.Connections;
import hds.Console;
import hds.ControlPane;
import hds.DDAddress;
import hds.DirectoryServer;
import hds.EventDispatcher;
import hds.GUIStatusHistory;
import hds.IClient;
import hds.JFrameDropCatch;
import hds.Server;
import hds.StartUpThread;
import hds.StegoStructure;
import hds.UDPServer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.List;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import simulator.Fill_database;
import simulator.SimulationParameters;
import streaming.ConstituentHandling;
import streaming.NeighborhoodHandling;
import streaming.OrgHandling;
import streaming.SpecificRequest;
import streaming.UpdateMessages;
import streaming.WB_Messages;
import streaming.WitnessingHandling;
import table.HashConstituent;
import util.DBInterface;
import util.DB_Implementation;
import util.DD_DirectoryServer;
import util.DD_IdentityVerification_Answer;
import util.DD_IdentityVerification_Request;
import util.DD_Mirrors;
import util.DD_Testers;
import util.GetOpt;
import util.P2PDDSQLException;
import util.Util;
import widgets.components.DDTranslation;
import widgets.components.Language;
import widgets.constituent.ConstituentsPanel;
import widgets.directories.Directories;
import widgets.identities.MyIdentitiesTest;
import widgets.instance.Instances;
import widgets.justifications.JustificationEditor;
import widgets.justifications.Justifications;
import widgets.justifications.JustificationsByChoicePanel;
import widgets.keys.Keys;
import widgets.motions.MotionEditor;
import widgets.motions.Motions;
import widgets.motions.MotionsListener;
import widgets.news.NewsEditor;
import widgets.news.NewsTable;
import widgets.org.Orgs;
import widgets.peers.CreatePeer;
import widgets.peers.PeerAddresses;
import widgets.peers.PeerInput;
import widgets.peers.Peers;
import widgets.wireless.WLAN_widget;
import wireless.BroadcastClient;
import wireless.BroadcastConsummerBuffer;
import wireless.BroadcastServer;
import wireless.Broadcasting_Probabilities;
import wireless.Refresh;
import census.CensusPanel;
import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;
import data.D_Constituent;
import data.D_Neighborhood;
import data.D_Organization;
import data.D_PeerAddress;
import data.D_Witness;
import ASN1.Encoder;

public class DD {
	public static final String BRANCH = "FIT_HDSSL_SILAGHI";
	public static final String VERSION = "0.9.54";
	public static final String PK_Developer = "MIIEGgwDUlNBYAEwAgMBAAECggQASKs9x2VEQH1SRxRwO43yt6HXCTnOmPJVUjN8bQQUTVBdFXhQsTpnTP1yLe/qFlA0jnIzheHT4WEcsU874N800iPMWHCjpCowQwwTj9SQLTmfbfhL8z0a7Dw6ZJQ+DnYoPVhx3JHL57CK3YeVYclZCoHetZ5PEIpcAwxaPmnL3GQaOgJiVHb6CLMi+hNHLxsjQZwTYTeoUOXQKgyTcRDE6xCvw8+q0U6/Uan3KCx/KmtdRQMEtGAXSPANv12kle84Dv8AdJxT1CJGsXm0+N6+wbbvkL77kMr+79sCR/8drZmOnrbjveQpab2pSh0vO//XqslrDRbzhniGSpqFW+YNTOixWAsCp35hNPbAx5xqPXg6DEIrysGslDGo4gC3Ew5mN/JkOQA+pd6uIzC4EgbfWqJKMvrtOQN67hJR7Ysxn7cLDXGvmhK1s7oSJcnOmhWljSZ6joviVwAWKgzdm1gMBhn5+VdgwoEE7g5Inw0dH9UmgufloNiBQMM9m2igdQPaLRuVttrAEcs55F/Z5NFtJquTeQFBLAGux3MVxrYCgivRaoAzAkUMhGOA+00KU3oh3Bds0U8GYCMuYYrwSAWTZf0Z9lvUwJv8HtLJvI6p1p53oGzIW9bo20d0PMz7XrzNDOLEME9PaXKLo6vMCAxXIj19nm/bE1HBY7e7HErKMX3M7LC2xZ8PH7wsnl5M3y0ZZ6c9quwhvz/dWcUAQ5963LtDZ6bOenAGVGBjdWLhHK8/2p9Vgu1ZNA1WWHWnafExsT5GxuwZQ/PMk8YtmxqEkgGy2+xVT19oUK+yO1ok+xRUjvSRZ0IbWUEcOfQ5FvLNmMdV/NSebB6vjQwM5DGCE1YDhix+Qghr558KokVz7BPVrGVe1pUxfPo2XPwHReF8es+vr16lvwXrVEmQNG8KrX1tN5Z5I29+ZVcR6ti4t90RXY6H6lmLtU3P/PSmfOrBQraNHVvDm9y1hnSP9+EhJzuWFaS8v4+7OnodIWuZsYd2WYQp4YcDJ+7grV3s1vvacujzxCOwx5/gosLxOau45bvKqhsFrZ+le6IRNAG7T6ZwC9wesqCGBJlIwS50DlAb/KhPyDIvf+7EH1iwckG4fBtixaK9co8FHnuddn/cEIc6fkWDEzr2Cu3HyxeMeDrcGRvjTRr78Wp/ptvRoOYElOLkxrkmanetjOCMqRl1DJvl53SQKePraRx2DpRemK/TMQ3+5TQkFjjEsI2P455Th0z6vF+JzpetZ3j1NUqx+iEZ2ArMhdDk7dE/4qcn2xwLz5nNMvHSnO2N0T9tCLi96CqZm/HTqGa6jTxFhJOP11sFCCQ9jkKhxvxubs0sww75dnqXQeffpxyolcht3KHwfwwHU0hBLTUxMg==";
	public static final PK _PK_Developer = Cipher.getPK(PK_Developer);
	public static String _APP_NAME = _("Direct Democracy P2P");
	//public static String _APP_NAME = _("La Bible A Petits Pas");
	public static String APP_NAME = _APP_NAME+" "+VERSION;
	public static final String DEFAULT_EMAIL_PROVIDER = "my.fit.edu";
	public static JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);//JTabbedPane.LEFT);

	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	
    //public static widgets.org.Orgs orgsPane;// referred only from Orgs
    public static ConstituentsPanel constituentsPane;
    public static MyIdentitiesTest identitiesPane;
    //public static WLAN_widget wirelessInterfacesPane;
    public static widgets.peers.Peers peersPluginsPane;
    public static Console clientConsole;
    public static Console serverConsole;
    //public static widgets.org.OrgEditor orgEPane; // referred only from Orgs
    
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
	public static final byte TYPE_TableName = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)0);
	public static final byte TYPE_DatabaseName = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)1);
	public static final byte TYPE_FieldName = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)2);
	public static final byte TYPE_FieldType = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)3);
	public static final byte TYPE_MotionID = DD.asn1Type(Encoder.CLASS_PRIVATE, Encoder.PC_PRIMITIVE, (byte)4);
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
	public static final short STEGO_SIGN_CONSTITUENT_VERIF_REQUEST = 0x7AAD;
	public static final short STEGO_SIGN_CONSTITUENT_VERIF_ANSWER = 0x3EE3;
	public static final short STEGO_SIGN_DIRECTORY_SERVER = 0x1881;
	public static final short STEGO_SIGN_TESTERS = 0x588C;
	public static final short STEGO_SIGN_MIRRORS = 0x4774;

	
	public static byte asn1Type(int classASN1, int PCASN1, byte tag_number) {
		if((tag_number&0x1F) >= 31){
			Util.printCallPath("Need more bytes");
			tag_number = 25;
		}
		return  (byte)((classASN1<<6)+(PCASN1<<5)+tag_number);
	}
	public static StegoStructure[] getAvailableStegoStructureInstances(){
		DDAddress data1 = new DDAddress();
		DD_IdentityVerification_Request data2 = new DD_IdentityVerification_Request();
		DD_IdentityVerification_Answer data3 = new DD_IdentityVerification_Answer();
		DD_DirectoryServer data4 = new DD_DirectoryServer();
		DD_Testers data5 = new DD_Testers();
		DD_Mirrors data6 = new DD_Mirrors();
		return new StegoStructure[]{data1, data2, data3, data4, data5, data6};
	}

	public static short[] getAvailableStegoStructureISignatures() {
		StegoStructure[] a = getAvailableStegoStructureInstances();
		if(a==null) return new short[0];
		short []r = new short[a.length];
		for(int k =0 ; k<a.length; k++)
			r[k] = a[k].getSignShort();
		return r;
	}
	
	static final Object monitor_getMyPeerGIDFromIdentity = new Object();
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

	public static final String peerFields1[] = new String[]{table.peer.global_peer_ID,table.peer.name,table.peer.slogan,table.peer_address.address,table.peer_address.type,table.peer_address.arrival_date,table.organization.global_organization_ID, table.organization.name,table.peer.hash_alg,table.peer.signature};
	public static final String peersFieldsTypes1[] = new String[]{"TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT"};
	
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
	public static final String APP_my_peer_name = "my_peer_name";
	public static final String APP_my_peer_slogan = "my_peer_slogan";
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
	public static boolean ClientUDP = true;
	public static boolean ClientTCP = false; //Should the client try TCP?
	
	public static EventDispatcher ed=new EventDispatcher();
	public static boolean hasPeersPane = false;
	public static boolean hasOrgPane = false;
    public static ControlPane controlPane;

    
 	private static final String IMAGE_START_SPLASH = "LargeSplash.jpg";
	public static final String SERVE_DIRECTLY = "SERVE_DIRECTLY";
	private static final boolean SONG = true;
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
	public static boolean ENFORCE_ORG_INITIATOR = false;
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
	private static final String MY_DEBATE_TOPIC = "MY_DEBATE_TOPIC";
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
    
	public static JFrame frame;
	public static JLabel splash_label;
	//public static Motions motions;
	//public static MotionEditor _medit;
	//public static Justifications justifications;
	//public static JustificationEditor _jedit;
	
	public static JustificationsByChoicePanel _jbc;
	//public static Component jscj;	// justifications tab
	public static Component tab_organization;	// org tab, no longer used to preselect org,->indexes
	//public static Component jscm;   // motions tab
	public static JTextArea wireless_hints;
	
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
	private static Toolkit toolkit;
	public static Calendar startTime;
	public static boolean VERIFY_SENT_SIGNATURES = true;
	public static boolean ACCEPT_STREAMING_REQUEST_UNSIGNED = false;
	public static boolean USE_NEW_ARRIVING_PEERS_CONTACTING_ME = true;
	public static boolean ASK_USAGE_NEW_ARRIVING_PEERS_CONTACTING_ME = true;
	public static JTextField client_sockets=null;
	public static JTextField client_sockets_val = null;
	public static JTextField client_sockets_cntr = null;
	public static JTextField clientsockets_sleep = null;
	public static long ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP = 0;
	public static int ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP = 1;
	private static JTextField clientsockets_long_sleep_seconds;
	private static JTextField clientsockets_long_sleep_minutes_start;
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
	
	
	public static void addTabPeer() {
		/*
        if(!hasPeersPane ){
        	hasPeersPane = true;
        	DualListBox peersPane=null;
        	try{
        		peersPane = new DualListBox();
        	}catch(Exception e){e.printStackTrace();}
        	tabbedPane.addTab("Peers", peersPane);
        }	
        */	
	}
        /*
        if(hasOrgPane ){
        	try {
        		organizationPane = new organization();
        		tabbedPane.addTab("Organization", organizationPane);
        	} catch (Exception e1) {
        		e1.printStackTrace();
        	}
         }
        if(hasPeersPane ){
        	DualListBox peersPane=null;
        	try{
        		peersPane = new DualListBox();
        	}catch(Exception e){e.printStackTrace();}
        	tabbedPane.addTab("Peers", peersPane);
        }
        */
    	//orgs.setMinimumSize(new Dimension(5000,0));
    	//orgs.setMinimumSize(orgs.getPreferredSize());
    	//newOrgButton.setMaximumSize(newOrgButton.getPreferredSize());
    	//newOrgButton.setMinimumSize(newOrgButton.getPreferredSize());
	public static Component makeOrgsPanel(widgets.org.OrgEditor orgEPane, widgets.org.Orgs orgsPane) {
		JPanel orgs = new JPanel();
		int y = 0;
    	java.awt.GridBagLayout gbl = new java.awt.GridBagLayout();
    	orgs.setLayout(gbl);
    	java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
    	JButton newOrgButton;
    	newOrgButton = new JButton(_("New Organization"));
    	newOrgButton.setActionCommand(DD.COMMAND_NEW_ORG);
    	newOrgButton.addActionListener(Application.appObject);
    	//c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0; c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;
    	//orgs.add(orgsPane.getScrollPane(),c);
    	c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=0.0;c.weightx=10.0;c.anchor=GridBagConstraints.WEST;//c.insets=new java.awt.Insets(1,1,1,1);
    	orgs.add(newOrgButton,c);
    	c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;//c.insets=new java.awt.Insets(0,0,0,0);
    	orgs.add(orgEPane,c);
    	
		JSplitPane splitPanel = new JSplitPane( JSplitPane.VERTICAL_SPLIT, orgsPane.getScrollPane(),
				//new JScrollPane
				(orgs));
		return splitPanel;
       	//tabbedPane.addTab("Org", orgsPane.getScrollPane());
       	//tabbedPane.addTab("OrgE", orgEPane);
    	//jsco.setPreferredSize(new Dimension(800,0));
	}
	/**
	 * 
	 * @param _medit this should be a MotionsListener only if it is a MotionEditor!
	 * @param motions
	 * @return
	 */
	public static Component makeMotionPanel( Component _medit, Component motions) {
		JScrollPane motion_scroll;
		if(motions instanceof Motions){
			motion_scroll = ((Motions)motions).getScrollPane();
		}else{
			if(motions instanceof JScrollPane)
				motion_scroll = (JScrollPane)motions;
			else{
				motion_scroll = new JScrollPane(motions);
				if(_DEBUG) System.out.println("DD.makeMotionPanel: unexpected motion type");
			}
		}
		if(_medit instanceof MotionsListener) {
			JScrollPane edit_scroll;
			JSplitPane result;
			Dimension minimumSize;
			edit_scroll = new JScrollPane(_medit);
			result = new JSplitPane(JSplitPane.VERTICAL_SPLIT, motion_scroll, edit_scroll);
			result.setResizeWeight(0.5);
			
			minimumSize = new Dimension(0, 100);
			motion_scroll.setMinimumSize(minimumSize);

			Dimension _minimumSize = new Dimension(0, MotionEditor.DIMY);
			edit_scroll.setMinimumSize(_minimumSize);
			
			if(DEBUG) System.out.println("DD.makeMotionPanel: return splitter");
			return  result;
		}else{
			if(DEBUG) System.out.println("DD.makeMotionPanel: return motion_scroll");
			return motion_scroll;
			//Dimension dim = new Dimension(0, 0);
			//edit.setMaximumSize(dim);
		}
		//minimumSize = new Dimension(0, 600);
		//edit.setPreferredSize(minimumSize);
		/*
		JPanel motion_panel = new JPanel();
    	JScrollPane mot = motions.getScrollPane();
    	java.awt.GridBagLayout gbl = new java.awt.GridBagLayout();
    	motion_panel.setLayout(gbl);
    	java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
    	//JButton newOrgButton;
    	//newOrgButton = new JButton(_("New Motion"));
    	//newOrgButton.setActionCommand(DD.COMMAND_NEW_ORG);
    	//newOrgButton.addActionListener(Application.appObject);
    	c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=10.0;c.anchor=GridBagConstraints.CENTER;
    	motion_panel.add(mot,c);
    	//c.fill=GridBagConstraints.NONE;c.gridx=0;c.gridy=y++;c.weighty=0.0;c.anchor=GridBagConstraints.WEST;c.insets=new java.awt.Insets(1,1,1,1);
    	//motion_panel.add(newOrgButton,c);
    	c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=0.5;c.anchor=GridBagConstraints.CENTER;c.insets=new java.awt.Insets(0,0,0,0);
    	motion_panel.add(_medit, c) //.getScrollPane(),c);
		return motion_panel;
    	//tabbedPane.addTab("Motions", mot);
		//tabbedPane.addTab("Motion", _medit.getScrollPane());//new JEditorPane("text/html","<html><b>nope</b>pop</html>"));
		*/
	}
	public static JScrollPane makeCensusPanel(census.CensusPanel census){
		return census.getScrollPane();
	}
	/**
	 * Version with fixed Panel
	 * @param _nedit
	 * @param news
	 * @return
	 */
	public static JPanel _makeNewsPanel( NewsEditor _nedit, NewsTable news) {
		int y = 0;
		JPanel motion_panel = new JPanel();
    	JScrollPane _news = news.getScrollPane();
    	java.awt.GridBagLayout gbl = new java.awt.GridBagLayout();
    	motion_panel.setLayout(gbl);
    	java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
    	c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;
    	motion_panel.add(_news,c);
    	c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;c.insets=new java.awt.Insets(0,0,0,0);
    	motion_panel.add(_nedit/*.getScrollPane()*/,c);
		return motion_panel;
	}
	/**
	 * Version with splitter
	 * @param _nedit
	 * @param news
	 * @return
	 */
	public static Component makeNewsPanel( NewsEditor _nedit, NewsTable news) {
		//int y = 0;
		//JSplitPane motion_panel = new JSplitPane();
    	JScrollPane _news = news.getScrollPane();
    	//java.awt.GridBagLayout gbl = new java.awt.GridBagLayout();
    	//motion_panel.setLayout(gbl);
    	//java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
    	//c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;
    	//motion_panel.add(_news,c);
    	//c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;c.insets=new java.awt.Insets(0,0,0,0);
    	//motion_panel.add(_nedit/*.getScrollPane()*/,c);
		return new JSplitPane(JSplitPane.VERTICAL_SPLIT, _news, _nedit);
	}
	public static Component makeJustificationPanel( JustificationEditor _jedit, Justifications justifications) {
		/*
		int y = 0;
		JPanel motion_panel = new JPanel();
    	//JScrollPane _just = justifications.getScrollPane();
    	java.awt.GridBagLayout gbl = new java.awt.GridBagLayout();
    	motion_panel.setLayout(gbl);
    	java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
    	c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10;c.anchor=GridBagConstraints.CENTER;
    	motion_panel.add(justifications.getScrollPane(),c);
    	c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10;c.anchor=GridBagConstraints.CENTER;c.insets=new java.awt.Insets(0,0,0,0);
    	motion_panel.add(_jedit,c);
    	*/
		JSplitPane motion_panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				justifications.getScrollPane(),
				new JScrollPane(_jedit));
		return motion_panel;
	}
	public static JSplitPane makeWLanPanel(WLAN_widget wlan_widget) {
		JPanel hints_panel = new JPanel();
		wireless_hints = new JTextArea();
		//wireless_hints.setEnabled(false);
		wireless_hints.setEditable(false);
		wireless_hints.setText("DirectDemocracy P2P");
		hints_panel.add(wireless_hints);
		JLabel crt_cmd = Util.crtProcessLabel;
		crt_cmd.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		crt_cmd.setVerticalAlignment(SwingConstants.TOP);
		crt_cmd.setHorizontalAlignment(SwingConstants.LEFT);
		JPanel crt_cmd_panel = new JPanel();
		crt_cmd_panel.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {
			}
			@Override
			public void mousePressed(MouseEvent e) {
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				Util.stopCrtScript();
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			@Override
			public void mouseExited(MouseEvent e) {
			}
		});
		crt_cmd_panel.add(crt_cmd);
		
		// client sockets IPs:ports
		// server sockets IPs:ports
		// data_to_send size
		
		GridBagLayout bl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel dbg_panel = new JPanel(bl);
		int y = 0;
		//wireless.Broadcast
		c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(_("Client Sockets Number")), c);
		c.gridx=1; c.gridy=0; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets = new JTextField();
		DD.client_sockets = clientsockets;
		clientsockets.setText("                     ");
		dbg_panel.add(clientsockets, c);
		y++;
		
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.EAST;
		dbg_panel.add(new JLabel(_("Client Sockets")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_val = new JTextField();
		DD.client_sockets_val = clientsockets_val;
		clientsockets_val.setText("                     ");
		dbg_panel.add(clientsockets_val, c);
		y++;
		
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(_("SentMsgCounter")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_cntr = new JTextField();
		DD.client_sockets_cntr = clientsockets_cntr;
		clientsockets_cntr.setText("                     ");
		dbg_panel.add(clientsockets_cntr, c);
		y++;
		
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(_("Adhoc long intermessage delay (seconds (> 0))")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_long_sleep_seconds = new JTextField();
		DD.clientsockets_long_sleep_seconds  = clientsockets_long_sleep_seconds;
		clientsockets_long_sleep_seconds.setText(""+DD.ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP);
		clientsockets_long_sleep_seconds.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				long seconds = 0;
				JTextField f = (JTextField)e.getSource();
				try{seconds = Long.parseLong(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP = seconds));
			}
		});
		clientsockets_long_sleep_seconds.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
				long seconds = 0;
				JTextField f = (JTextField)e.getSource();
				try{seconds = Long.parseLong(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP = seconds));
			}
		});
		dbg_panel.add(clientsockets_long_sleep_seconds, c);
		y++;
		
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(_("Adhoc long intermessage delay start (minutes modulus (> 1))")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_long_sleep_minutes_start = new JTextField();
		DD.clientsockets_long_sleep_minutes_start  = clientsockets_long_sleep_minutes_start;
		clientsockets_long_sleep_minutes_start.setText(""+DD.ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP);
		clientsockets_long_sleep_minutes_start.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				int minutes = 0;
				JTextField f = (JTextField)e.getSource();
				try{minutes = Integer.parseInt(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP = minutes));
			}
		});
		clientsockets_long_sleep_seconds.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
				int minutes = 0;
				JTextField f = (JTextField)e.getSource();
				try{minutes = Integer.parseInt(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP = minutes));
			}
		});
		dbg_panel.add(clientsockets_long_sleep_minutes_start, c);
		y++;
		
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(_("Adhoc intermessage delay (milliseconds (> 0))")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_sleep = new JTextField();
		DD.clientsockets_sleep  = clientsockets_sleep;
		clientsockets_sleep.setText(""+DD.ADHOC_SENDER_SLEEP_MILLISECONDS);
		clientsockets_sleep.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				long millis = 0;
				JTextField f = (JTextField)e.getSource();
				try{millis = Long.parseLong(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_MILLISECONDS = millis));
			}
		});
		clientsockets_sleep.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
				long millis = 0;
				JTextField f = (JTextField)e.getSource();
				try{millis = Long.parseLong(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_MILLISECONDS = millis));
			}
		});
		clientsockets_sleep.getDocument().addDocumentListener(new DocumentListener(){
			private void upd(DocumentEvent e) {
				String text=null;
				long millis = 0;
				try{
					text = e.getDocument().getText(0, e.getDocument().getLength());
					millis = Long.parseLong(text);
				}catch(Exception i){}
				DD.ADHOC_SENDER_SLEEP_MILLISECONDS = millis;
				//String ntext = ""+(millis);
				//if((e.getDocument().getLength()!=0)&&(!ntext.equals(text))) DD.clientsockets_sleep.setText(ntext);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {upd(e);}

			@Override
			public void removeUpdate(DocumentEvent e) {upd(e);}

			@Override
			public void changedUpdate(DocumentEvent e) {upd(e);}
			
		});
		dbg_panel.add(clientsockets_sleep, c);
		y++;
		
		if(wireless.BroadcastClient.broadcast_client_sockets!=null)
			clientsockets.setText(""+wireless.BroadcastClient.broadcast_client_sockets.length);
		JSplitPane p1, p2;
		JSplitPane wpanel =
				new JSplitPane(JSplitPane.VERTICAL_SPLIT, crt_cmd_panel ,
				(p1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, wlan_widget.getScrollPane(),
				(p2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,hints_panel, dbg_panel)))));
		p2.setResizeWeight(1.0);
		p1.setResizeWeight(1.0);
		return wpanel;
	}
	/**
	 * No high quality icons available yet (would need some larger castles for that)
	 * @param quality
	 * @param _frame
	 */
	public static void loadAppIcons(boolean quality, JFrame _frame) {
		if(quality) {
			if (_frame != null) {
			java.awt.Image icon_conf = config.DDIcons.getImageFromResource(DDIcons.I_DDP2P40, _frame);
			_frame.setIconImage(icon_conf);// new ImageIcon(getClass().getResource("Icon.png")).getImage());
			java.util.List<Image> icons = new ArrayList<Image>();
			icons.add(icon_conf);
			java.awt.Image icon_conf32 = config.DDIcons.getImageFromResource(DDIcons.I_DDP2P32, _frame);
			icons.add(icon_conf32);
			DD.frame.setIconImages(icons);
		}
		/*
		if(DD.frame!=null) {
			DD.frame.setIconImage(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"favicon.ico")));
			List<Image> icons = new ArrayList<Image>();
			//icons.add(Toolkit.getDefaultToolkit().getImage("p2pdd_resources/folder_images_256.png"));
			icons.add(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"folder_images_256.png")));
			icons.add(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"folder_32x32.png")));
			icons.add(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"favicon.ico")));
			icons.add(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"happy.smiley19.gif")));
			DD.frame.setIconImages(icons);
		}
		*/
		} else {
			java.awt.Image icon_conf = config.DDIcons.getImageFromResource(DDIcons.I_DDP2P40, _frame);
			_frame.setIconImage(icon_conf);// new ImageIcon(getClass().getResource("Icon.png")).getImage());
		}
	}
	/**
	 * Starts the spash with dimensions in DD.FRAME_XXX
	 * @return
	 */
	public static JFrame initMainFrameSplash(){
		JFrame _frame = new JFrameDropCatch(DD.APP_NAME);
		_frame.getRootPane().addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
            	if(_jbc!=null)_jbc.adjustSize();
                //System.out.println("componentResized:"+frame.getHeight());
            }
		});
		_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		loadAppIcons(false, _frame);

		/*
		com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();
		application.setDockIconImage(icon_conf);
		*/
		
		//frame.pack();
        //frame.setVisible(true);
        String splash;
        //splash = "icons/800px-Landsgemeinde_Glarus_2006.jpg";
        splash = IMAGE_START_SPLASH;
        ImageIcon image = DDIcons.getImageIconFromResource(splash, _frame, "Starting Splash");
        splash_label = new JLabel("", image, JLabel.CENTER);
        _frame.add(splash_label);
        _frame.pack();
        _frame.setVisible(true);
        
		Rectangle r = _frame.getBounds();
		r.x = DD.FRAME_OFFSET ;
		r.width=DD.FRAME_WIDTH;
		r.y = DD.FRAME_HSTART ;
		r.width=DD.FRAME_HEIGHT;
		_frame.setBounds(r.x, r.y, r.width, r.height);
		return _frame;
	}
	static
	class tabOnFocus_changeListener implements ChangeListener {
		static boolean _settings = false;
		static final boolean _settings_reload = true;

		static boolean _keys = false;
		static final boolean _keys_reload = true;
		
		static boolean _peers = false; // tells if currently selected
		static final boolean _peers_reload = false; // tells if to garbage collect
		static final boolean _peers_disconnect = true; // tells if to disconnect
		static Peers _saved_peers = null; // saves disconnected peers that do not garbage collect

		static boolean _orgs = false;
		static final boolean _orgs_reload = false;
		static final boolean _orgs_disconnect = true; // tells if to disconnect
		static Orgs _saved_orgs = null; // saves disconnected orgs that do not garbage collect

		static boolean _mots = false;
		static final boolean _mots_reload = false;
		static final boolean _mots_disconnect = true; // tells if to disconnect
		static Motions _saved_mots = null; // saves disconnected mots that do not garbage collect

		static boolean _justs = false;
		static final boolean _justs_reload = false;
		static final boolean _justs_disconnect = true; // tells if to disconnect
		static Justifications _saved_justs = null; // saves disconnected justs that do not garbage collect

		static boolean _debate = false;
		static final boolean _debate_reload = false;
		static final boolean _debate_disconnect = true; // tells if to disconnect
		static JustificationsByChoicePanel _saved_debate = null; // saves disconnected debate that do not garbage collect

		static boolean _news = false;
		static final boolean _news_reload = false;
		static final boolean _news_disconnect = true; // tells if to disconnect
		static NewsTable _saved_news = null; // saves disconnected news that do not garbage collect

		static boolean _broadcast = false;
		static final boolean _broadcast_reload = false;
		static final boolean _broadcast_disconnect = true; // tells if to disconnect
		static WLAN_widget _saved_broadcast = null; // saves disconnected broadcast that do not garbage collect

		//static int _prev_index = -1;
	    public void stateChanged(ChangeEvent e) {
	    	int index = tabbedPane.getSelectedIndex();
	    	DD.status.setTab(index);
	    	if (!preloadedControl) {
		    	if(TAB_SETTINGS_ == index) {
		    		if(!_settings) {
		    			ControlPane settings = null;
						try {
							Application.controlPane = settings = new hds.ControlPane();
						} catch (util.P2PDDSQLException e1) {
							e1.printStackTrace();
						}
		    			tabbedPane.setComponentAt(index, settings);
		    			_settings = true;
		    		}
		    	}else{
		    		if(_settings && _settings_reload){
		    			tabbedPane.setComponentAt(TAB_SETTINGS_, JunkControlPane);
		    			Application.controlPane = null;
		    			_settings = false;
		    		}
		    	}
	    	}
	    	
	    	if(TAB_KEYS_ == index) {
	    		if(!_keys) {
	    			Keys keys = new widgets.keys.Keys();
	    			tabbedPane.setComponentAt(index, keys.getPanel());
	    			_keys = true;
	    		}
	    	}else{
	    		if(_keys && _keys_reload){
	    			tabbedPane.setComponentAt(TAB_KEYS_, JunkPanelKeys);
	    			_keys = false;
	    		}
	    	}
	    	
	    	/* the peer tab has a problem with the plugins initialization when dynamically loaded
	    	 * ideally the plugin loader should build its menus structures outside this class (an independent class)
	    	 * Then they can be used from peers on any reload
	    	 * 
	    	if(TAB_PEERS_ == index) {
	    		if(!_peers) {
	    			if(_saved_peers==null) {
		    			_saved_peers = new widgets.peers.Peers();
		    			tabbedPane.setComponentAt(index, _saved_peers.getPanel());
	    			}
		    		if(_peers_reload || _peers_disconnect) {
		    			_saved_peers.connectWidget(); // after getPanel()
		    		}
	    			_peers = true;
	    		}
	    	}else{
	    		if(_peers){
		    		_peers = false;
		    		if(_peers_reload || _peers_disconnect) {
		    			_saved_peers.disconnectWidget();
		    		}
	    			if(_peers_reload){
		    			tabbedPane.setComponentAt(TAB_PEERS_, JunkPanelPeers);
		    			_saved_peers = null;
	    			}
	    		}
	    	}
	    	*/
	    	
	    	if(TAB_ORGS_ == index) {
	    		if(!_orgs) {
	    			if(_saved_orgs==null) {
		    			_saved_orgs = new widgets.org.Orgs();
		    			tabbedPane.setComponentAt(index, _saved_orgs.getComboPanel());
	    			}
		    		if(_orgs_reload || _orgs_disconnect) {
		    			_saved_orgs.connectWidget(); // after getPanel()
		    		}
	    			_orgs = true;
	    		}
	    	}else{
	    		if(_orgs){
	    			_orgs = false;
		    		if(_orgs_reload || _orgs_disconnect) {
		    			_saved_orgs.disconnectWidget();
		    		}
	    			if(_orgs_reload) {
	    				tabbedPane.setComponentAt(TAB_ORGS_, JunkPanelOrgs);
	    				_saved_orgs = null;
	    			}
	    		}
	    	}
	    	
	    	if(TAB_MOTS_ == index) {
				if(DEBUG) System.out.println("DD.tabsChanged: motions index");
	    		if(!_mots) {
	    			if(_saved_mots==null) {
		    			_saved_mots = new widgets.motions.Motions();
		    			tabbedPane.setComponentAt(index, _saved_mots.getComboPanel());
						if(DEBUG) System.out.println("DD.tabsChanged: motions idx component set");
	    			}
		    		if(_mots_reload || _mots_disconnect) {
						if(DEBUG) System.out.println("DD.tabsChanged: motions idx will connect");
		    			_saved_mots.connectWidget(); // after getPanel()
						if(DEBUG) System.out.println("DD.tabsChanged: motions idx connected");
		    		}
	    			_mots = true;
	    		}
	    	}else{
	    		if(_mots){
	    			_mots = false;
		    		if(_mots_reload || _mots_disconnect) {
		    			_saved_mots.disconnectWidget();
		    		}
	    			if(_mots_reload) {
	    				tabbedPane.setComponentAt(TAB_MOTS_, JunkPanelMots);
	    				_saved_mots = null;
	    			}
	    		}
				if(DEBUG) System.out.println("DD.tabsChanged: motions index done");
	    	}
	    	
	    	if(TAB_JUSTS_ == index) {
	    		if(!_justs) {
	    			if(_saved_justs==null) {
		    			_saved_justs = new widgets.justifications.Justifications();
		    			tabbedPane.setComponentAt(index, _saved_justs.getComboPanel());
	    			}
		    		if(_justs_reload || _justs_disconnect) {
		    			_saved_justs.connectWidget(); // after getPanel()
		    		}
	    			_justs = true;
	    		}
	    	}else{
	    		if(_justs){
	    			_justs = false;
		    		if(_justs_reload || _justs_disconnect) {
		    			_saved_justs.disconnectWidget();
		    		}
	    			if(_justs_reload) {
	    				tabbedPane.setComponentAt(TAB_JUSTS_, JunkPanelJusts);
	    				_saved_justs = null;
	    			}
	    		}
	    	}
	    	
	    	if(TAB_JBC_ == index) {
	    		if(!_debate) {
	    			if(_saved_debate==null) {
		    			_saved_debate = new widgets.justifications.JustificationsByChoicePanel();
		    			tabbedPane.setComponentAt(index, _saved_debate.getComboPanel());
	    			}
		    		if(_debate_reload || _debate_disconnect) {
		    			_saved_debate.connectWidget(); // after getPanel()
		    		}
	    			_debate = true;
	    		}
	    	}else{
	    		if(_debate){
	    			_debate = false;
		    		if(_debate_reload || _debate_disconnect) {
		    			_saved_debate.disconnectWidget();
		    		}
	    			if(_debate_reload) {
	    				tabbedPane.setComponentAt(TAB_JBC_, JunkPanelJBC);
	    				_saved_debate = null;
	    			}
	    		}
	    	}
	    	
	    	if(TAB_NEWS_ == index) {
	    		if(!_news) {
	    			if(_saved_news==null) {
		    			_saved_news = new widgets.news.NewsTable();
		    			tabbedPane.setComponentAt(index, _saved_news.getComboPanel());
	    			}
		    		if(_news_reload || _news_disconnect) {
		    			_saved_news.connectWidget(); // after getPanel()
		    		}
	    			_news = true;
	    		}
	    	}else{
	    		if(_news){
	    			_news = false;
		    		if(_news_reload || _news_disconnect) {
		    			_saved_news.disconnectWidget();
		    		}
	    			if(_news_reload) {
	    				tabbedPane.setComponentAt(TAB_NEWS_, JunkPanelNews);
	    				_saved_news = null;
	    			}
	    		}
	    	}
	    	
	    	if(TAB_MAN_ == index) {
	    		if(!_broadcast) {
	    			if(_saved_broadcast==null) {
		    			_saved_broadcast = new widgets.wireless.WLAN_widget(Application.db);
		    			tabbedPane.setComponentAt(index, _saved_broadcast.getComboPanel());
	    			}
		    		if(_broadcast_reload || _broadcast_disconnect) {
		    			_saved_broadcast.connectWidget(); // after getPanel()
		    		}
	    			_broadcast = true;
	    		}
	    	}else{
	    		if(_broadcast){
	    			_broadcast = false;
		    		if(_broadcast_reload || _broadcast_disconnect) {
		    			_saved_broadcast.disconnectWidget();
		    		}
	    			if(_broadcast_reload) {
	    				tabbedPane.setComponentAt(TAB_MAN_, JunkPanelMAN);
	    				_saved_broadcast = null;
	    			}
	    		}
	    	}
	    	
	    }
	    public void _stateChanged(ChangeEvent e) {
	    	int index = tabbedPane.getSelectedIndex();
	    	String tabtitle = tabbedPane.getTitleAt(index);
	    	//if(TAB_KEYS == index)
	    	if(TAB_KEYS.equals(tabtitle)) {
	    		if(!_keys) {
	    			Keys keys = new widgets.keys.Keys();
	    			tabbedPane.setComponentAt(index, keys.getPanel());
	    			_keys = true;
	    		}
	    	}else{
	    		if(_keys && _keys_reload){
	    			//tabbedPane.setComponentAt(TAB_KEYS_, JunkPanelKeys);
	    			int tabs = tabbedPane.getTabCount();
	    			for(int k=0; k<tabs; k++) {
	    		    	String _tabtitle = tabbedPane.getTitleAt(k);
	    		    	if(TAB_KEYS.equals(_tabtitle)){
	    		    		tabbedPane.setComponentAt(k, JunkPanelKeys);
	    		    		_keys = false;
	    		    		break;
	    		    	}
	    			}
	    		}
	    	}
	    	
	    	if(TAB_PEERS.equals(tabtitle)) {
	    		if(!_peers) {
	    			if(_saved_peers==null) {
		    			_saved_peers = new widgets.peers.Peers();
		    			tabbedPane.setComponentAt(index, _saved_peers.getPanel());
	    			}
		    		if(_peers_reload || _peers_disconnect) {
		    			_saved_peers.connectWidget(); // after getPanel()
		    		}
	    			_peers = true;
	    		}
	    	}else{
	    		if(_peers){
		    		_peers = false;
		    		if(_peers_reload || _peers_disconnect) {
		    			Application.peers.disconnectWidget();
		    		}
	    			if(_peers_reload){
		    			int tabs = tabbedPane.getTabCount();
		    			for(int k=0; k<tabs; k++) {
		    		    	String _tabtitle = tabbedPane.getTitleAt(k);
		    		    	if(TAB_PEERS.equals(_tabtitle)){
		    		    		tabbedPane.setComponentAt(k, JunkPanelPeers);
		    		    	}
		    		    	break;
		    			}
		    			_saved_peers = null;
	    			}
	    		}
	    	}
	    	/*
	    	if(TAB_ORGS.equals(tabtitle)) {
	    		if(!_orgs) {
	    			Orgs orgs = new widgets.org.Orgs();
	    			tabbedPane.setComponentAt(index, orgs.getPanel());
	    			_orgs = true;
	    		}
	    	}else{
	    		if(_orgs && _orgs_reload){
	    			//tabbedPane.setComponentAt(TAB_ORGS_, JunkPanelOrgs);
	    			int tabs = tabbedPane.getTabCount();
	    			for(int k=0; k<tabs; k++) {
	    		    	String _tabtitle = tabbedPane.getTitleAt(k);
	    		    	if(TAB_ORGS.equals(_tabtitle)){
	    		    		tabbedPane.setComponentAt(k, JunkPanelOrgs);
	    		    		_orgs = false;
	    		    		break;
	    		    	}
	    			}
	    		}
	    	}
	    	*/
	    }
	}
	static String TAB_SETTINGS = _("Settings");
	static int TAB_SETTINGS_ = 0;
	static final JPanel JunkControlPane = new JPanel();
	static String TAB_KEYS = _("Keys");
	static int TAB_KEYS_ = 1;
	static final JPanel JunkPanelKeys = new JPanel();
	static String TAB_ID = _("Identities");
	static int TAB_ID_ = 2;
	static String TAB_CONS = _("Constituents");
	static int TAB_CONS_ = 3;
	static String TAB_DIRS = _("Directories");
	static int TAB_DIRS_ = 5;
	
	static String TAB_PEERS = _("Peers");
	static int TAB_PEERS_ = 6;
	static final JPanel JunkPanelPeers = new JPanel();
	
	static String TAB_ORGS = _("Organizations");
	static int TAB_ORGS_ = 7;
	static final JPanel JunkPanelOrgs = new JPanel();
	
	public static String TAB_MOTS = _("Motions");
	public static int TAB_MOTS_ = 8;
	public static final JPanel JunkPanelMots = new JPanel();
	
	static String TAB_JUSTS = _("Justifications");
	public static int TAB_JUSTS_ = 9;
	static final JPanel JunkPanelJusts = new JPanel();
	
	static String TAB_JBC = _("Debate");
	public static int TAB_JBC_ = 10;
	static final JPanel JunkPanelJBC = new JPanel();
	
	static String TAB_NEWS = _("News");
	static int TAB_NEWS_ = 11;
	static final JPanel JunkPanelNews = new JPanel();
	
	static String TAB_MAN = _("Adhoc");
	static int TAB_MAN_ = 12;
	static final JPanel JunkPanelMAN = new JPanel();

	public static GUIStatusHistory status = null;
	private static PeerAddresses peer_addresses;
	private static Instances peer_instances;
	public final static boolean  preloadedControl = true;
	private static void createAndShowGUI() throws P2PDDSQLException{
		if (DEBUG) System.out.println("createAndShowGUI: start");
		ImageIcon icon_conf = config.DDIcons.getConfigImageIcon("Config");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_peer = config.DDIcons.getPeerSelImageIcon("General Peer");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_org = config.DDIcons.getOrgImageIcon("General Organization");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_con = config.DDIcons.getConImageIcon("General Constituent");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_mot = config.DDIcons.getMotImageIcon("General Motion");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		//ImageIcon icon_sig = config.DDIcons.getSigImageIcon("General Signature");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_bal = config.DDIcons.getBalanceImageIcon("Justifications");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_jus = config.DDIcons.getJusImageIcon("General Justification");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_key = config.DDIcons.getKeyImageIcon("Keys");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_news = config.DDIcons.getNewsImageIcon("General News");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_dir = config.DDIcons.getDirImageIcon("Dir");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_wireless = config.DDIcons.getWirelessImageIcon("Wireless");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_census = config.DDIcons.getCensusImageIcon("Census");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_mail = config.DDIcons.getMailPostImageIcon("Addresses");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_identity = config.DDIcons.getIdentitiesImageIcon("Identities");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		status = new GUIStatusHistory();
		tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);//.SCROLL_TAB_LAYOUT); //.WRAP_TAB_LAYOUT);
		tabbedPane.addChangeListener(new tabOnFocus_changeListener());
		Application.appObject = new AppListener(); // will listen for buttons such as createOrg
        
        clientConsole = new Console();
        serverConsole = new Console();
        DD.ed.addClientListener(clientConsole);
        DD.ed.addServerListener(serverConsole);
        
        Directories listing_dicts = Application.ld = new Directories(); //now
        identitiesPane = new MyIdentitiesTest(Application.db);

        if(DEBUG) System.out.println("createAndShowGUI: identities started");
       
        //Keys keys = new widgets.keys.Keys();
        constituentsPane = new ConstituentsPanel(Application.db, /*organizationID*/-1, -1, null);
        Application.constituents = constituentsPane;
        //JPanel _constituentsPane = makeConstituentsPanel(constituentsPane);

		if(DEBUG) System.out.println("createAndShowGUI: WLAN started");

        
        CensusPanel _censusPane;
        if(SONG){
        	_censusPane = new CensusPanel();
        	JScrollPane censusPane = makeCensusPanel(_censusPane);
        }
		if(DEBUG) System.out.println("createAndShowGUI: census added");
        
    	//tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        //identitiesPane.setOpaque(true);
    	if(preloadedControl) {
    		Application.controlPane=controlPane = new ControlPane();
    		tabbedPane.addTab(TAB_SETTINGS, icon_conf, controlPane, _("Configuration"));
    	}else
    		tabbedPane.addTab(TAB_SETTINGS, icon_conf, JunkControlPane, _("Configuration"));
        
        tabbedPane.setMnemonicAt(TAB_SETTINGS_, KeyEvent.VK_S);
        //tabbedPane.addTab(_("Keys"), keys.getPanel());
        tabbedPane.addTab(TAB_KEYS, icon_key, JunkPanelKeys, _("Keys"));
        tabbedPane.setMnemonicAt(TAB_KEYS_, KeyEvent.VK_K);
        tabbedPane.addTab(TAB_ID, icon_identity, identitiesPane, _("Identities"));
        tabbedPane.setMnemonicAt(TAB_ID_, KeyEvent.VK_I);
        tabbedPane.addTab(TAB_CONS, icon_con, constituentsPane, _("Constituents"));
        tabbedPane.setMnemonicAt(TAB_CONS_, KeyEvent.VK_C);
        if(SONG)tabbedPane.addTab(_("Census"), icon_census, _censusPane, _("Census"));
        tabbedPane.addTab(TAB_DIRS, icon_dir, listing_dicts.getPanel(), _("Directory"));
        tabbedPane.setMnemonicAt(TAB_DIRS_, KeyEvent.VK_D);

        if(DEBUG) System.out.println("createAndShowGUI: added tab census");

        
        //problems with pluging initialization
        Application.peers = peersPluginsPane = new widgets.peers.Peers(Application.db);
		if(DEBUG) System.out.println("createAndShowGUI: created peers");
		tabbedPane.addTab(TAB_PEERS, icon_peer, peersPluginsPane.getPanel(), _("Peers"));//peersPluginsPane.getScrollPane());
		if(DEBUG) System.out.println("createAndShowGUI: got peer panel");
        status.addOrgStatusListener(Application.peers.privateOrgPanel);
		if(DEBUG) System.out.println("createAndShowGUI: created added peer listener");
 
		//tabbedPane.addTab(TAB_PEERS, JunkPanelPeers);//peersPluginsPane.getPanel());//peersPluginsPane.getScrollPane());
        tabbedPane.setMnemonicAt(TAB_PEERS_, KeyEvent.VK_P);

		if(DEBUG) System.out.println("createAndShowGUI: added peers");
        
        //Application.peers.privateOrgPanel.addOrgListener(); // has to be called after peersPluginsPane.getPanel()
    	//jsco = new javax.swing.JScrollPane(orgs);
    	
/*        
        //organization organizationPane;
        orgsPane = new widgets.org.Orgs();
        Application.orgs = orgsPane;
        orgEPane = new widgets.org.OrgEditor();
        orgsPane.addOrgListener(orgEPane); // this remains connected to Orgs rather than status to enable force edit
    	JPanel orgs = makeOrgsPanel(orgEPane, orgsPane); //new JPanel();
    	tabbedPane.addTab(TAB_ORGS, DD.tab_organization=orgs); // tab_organization no longer used
*/
    	tabbedPane.addTab(TAB_ORGS, icon_org, JunkPanelOrgs, _("Organizations")); // tab_organization no longer used
    	tabbedPane.setMnemonicAt(TAB_ORGS_, KeyEvent.VK_O);
		if(DEBUG) System.out.println("createAndShowGUI: added orgs");
    	
    	tabbedPane.addTab(TAB_MOTS, icon_mot, JunkPanelMots, _("Motions"));
    	tabbedPane.setMnemonicAt(TAB_MOTS_, KeyEvent.VK_M);
		if(DEBUG) System.out.println("createAndShowGUI: added mots");
    	
    	tabbedPane.addTab(TAB_JUSTS, icon_bal, JunkPanelJusts, _("Justifications"));
    	tabbedPane.setMnemonicAt(TAB_JUSTS_, KeyEvent.VK_J);
		if(DEBUG) System.out.println("createAndShowGUI: added justs");
    	
    	tabbedPane.addTab(TAB_JBC, icon_jus, JunkPanelJBC, _("Debate"));
    	tabbedPane.setMnemonicAt(TAB_JBC_, KeyEvent.VK_D);
		if(DEBUG) System.out.println("createAndShowGUI: added debate");
    	
    	tabbedPane.addTab(TAB_NEWS, icon_news, JunkPanelNews, _("News"));
    	tabbedPane.setMnemonicAt(TAB_NEWS_, KeyEvent.VK_N);
		if(DEBUG) System.out.println("createAndShowGUI: added news");
    	
    	tabbedPane.addTab(TAB_MAN, icon_wireless, JunkPanelMAN, _("Mobile Adhoc Networks"));
    	tabbedPane.setMnemonicAt(TAB_MAN_, KeyEvent.VK_A);
		if(DEBUG) System.out.println("createAndShowGUI: added MAN");

		/*
    	justifications = new Justifications();
		if(DEBUG) System.out.println("createAndShowGUI: added justif");
    	_jedit = new JustificationEditor();
		if(DEBUG) System.out.println("createAndShowGUI: added justif editor");
    	justifications.addListener(_jedit);
		*/
		
		/*
    	_jbc = new JustificationsByChoicePanel();
    	//_jbc.addListener(_jedit);
    	_jbc.addListener(status);
 		status.addMotionStatusListener(_jbc);
     	//javax.swing.JScrollPane jbc = new javax.swing.JScrollPane(_jbc);
		tabbedPane.addTab("JBC", _jbc);
		if(DEBUG) System.out.println("createAndShowGUI: added jbc");
		*/

    	// Link widgets
    	if(Application.orgs!=null) Application.orgs.addOrgListener(status);
        //if (SONG)orgsPane.addListener(_censusPane.getModel());
        //if (SONG) orgsPane.addOrgListener(_censusPane);
        if (SONG) status.addOrgStatusListener(_censusPane);
		

    	// Initialize widgets
		/*
    	motions = new widgets.motions.Motions();
		if(DEBUG) System.out.println("createAndShowGUI: added motions");
		
    	//orgsPane.addOrgListener(motions.getModel());
    	status.addOrgStatusListener(motions.getModel());
    	_medit = new MotionEditor();
		if(DEBUG) System.out.println("createAndShowGUI: added mot editor");
    	//motions.addListener(justifications.getModel());
    	//motions.addListener(_jbc);
    	motions.addListener(status);
    	motions.addListener(_medit);
    	
    	   	
    	JSplitPane motion_panel = makeMotionPanel(_medit, motions);
    	jscm = motion_panel; //new javax.swing.JScrollPane(motion_panel);
    	//tabbedPane.addTab("Motions", jscm);
		if(DEBUG) System.out.println("createAndShowGUI: added motions");
    	
		*/
        
/*      
		status.addMotionStatusListener(justifications.getModel());    	
    	JPanel just_panel = makeJustificationPanel(_jedit, justifications);
    	jscj = new javax.swing.JScrollPane(just_panel);
		tabbedPane.addTab("Justifications", jscj);
    	//JScrollPane just = justifications.getScrollPane();
		//tabbedPane.addTab("Justifications", just);
		//tabbedPane.addTab("Justification", _jedit.getScrollPane());
		if(DEBUG) System.out.println("createAndShowGUI: justif pane");
*/
		
/*
    	widgets.news.NewsTable news = new widgets.news.NewsTable();
	    NewsEditor _nedit = new NewsEditor();
    	//orgsPane.addOrgListener(news.getModel());
    	status.addOrgStatusListener(news.getModel());
    	status.addMotionStatusListener(news.getModel());
	    news.addListener(_nedit);
    	//orgsPane.addOrgListener(_nedit);
    	status.addOrgStatusListener(_nedit);
    	status.addMotionStatusListener(_nedit);
		if(DEBUG) System.out.println("createAndShowGUI: some news");
    	JPanel news_panel = makeNewsPanel(_nedit, news);
    	javax.swing.JScrollPane jscn = new javax.swing.JScrollPane(news_panel);
		tabbedPane.addTab("News", jscn);
    	//JScrollPane _news_scroll = news.getScrollPane();
		//tabbedPane.addTab("News", _news_scroll);
		//tabbedPane.addTab("New", _nedit.getScrollPane());
*/
        
		 
        // Application.wlan =
        //WLAN_widget wirelessInterfacesPane = new widgets.wireless.WLAN_widget(Application.db);
        //tabbedPane.addTab("WLAN", makeWLanPanel(wirelessInterfacesPane));
   
    	tabbedPane.addTab("Addr", icon_mail, new JScrollPane(new PeerAddresses()), _("My Addresses"));

    	peer_addresses = new PeerAddresses();    	
    	tabbedPane.addTab("IPs", icon_mail, new JScrollPane(peer_addresses), _("Addresses Selected Peer"));
    	status.addPeerSelectedStatusListener(peer_addresses);
    	
    	peer_instances = new Instances();
    	tabbedPane.addTab("Instance", icon_identity, new JScrollPane(peer_instances), _("Instances"));
    	status.addPeerSelectedStatusListener(peer_instances.getModel());
    	
		if(DEBUG) System.out.println("createAndShowGUI: done tabs");

    	
        //frame.setVisible(false);
        frame.remove(splash_label);
        JPanel main = new JPanel(new BorderLayout());
        main.add(tabbedPane, BorderLayout.CENTER);
        StatusBar statusBar = new StatusBar(status);
        JScrollPane status_bar = new JScrollPane(statusBar);
        status_bar.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        main.add(status_bar, BorderLayout.SOUTH);
        frame.setContentPane(main);
      
        //frame.pack();
        frame.setVisible(true);		

		if(DEBUG) System.out.println("createAndShowGUI: done frame");

        
        /*
		Dimension dim = frame.getPreferredSize();
		System.out.println("Old preferred="+dim);
		dim.width = 600;
		frame.setPreferredSize(dim);
		frame.setMaximumSize(dim);
		*/
		Rectangle r = frame.getBounds();
		r.x = DD.FRAME_OFFSET ;
		r.width=DD.FRAME_WIDTH;
		r.y = DD.FRAME_HSTART ;
		r.width=DD.FRAME_HEIGHT;
		frame.setBounds(r.x, r.y, r.width, r.height);
		// if no identity, focus on identity
		if(Identity.default_id_branch==null){
			if(_DEBUG) System.out.println("DD:createAndShowGUI:No default identity!");
			DD.tabbedPane.setSelectedComponent(DD.identitiesPane);
			return;
		}
		// no org focus on org
		long _organizationID=Identity.getDefaultOrgID();
		if(_organizationID<=0){
			if(_DEBUG) System.out.println("DD:createAndShowGUI:No default organization!");
			//DD.tabbedPane.setSelectedComponent(DD.tab_organization);
			DD.tabbedPane.setSelectedIndex(TAB_ORGS_);
			return;
		}
		if(data.D_Organization.isIDavailable(_organizationID, DEBUG) != 1) {
			if(_DEBUG) System.out.println("DD:createAndShowGUI: Temporary organization!");
			//DD.tabbedPane.setSelectedComponent(DD.tab_organization);
			DD.tabbedPane.setSelectedIndex(TAB_ORGS_);
			return;
		}
		status.setSelectedOrg(new D_Organization(_organizationID));
		
		// if no constituent on constituent
		long _constID=-1;
		if((_constID=Identity.getDefaultConstituentIDForOrg(_organizationID))<=0){
			if(_DEBUG) System.out.println("DD:createAndShowGUI:No default constituent!");
			DD.tabbedPane.setSelectedComponent(DD.constituentsPane);
			return;
		}
		// else on motions or news
		if(_DEBUG) System.out.println("DD:createAndShowGUI:Default org="+_organizationID+" const="+_constID+" selected!");
		DD.status.setMeConstituent(new D_Constituent(_constID));
		//DD.tabbedPane.setSelectedComponent(DD.jscm);
		DD.tabbedPane.setSelectedIndex(TAB_MOTS_);
        //frame.pack();
	}
	/**
	 * The preferred languages are specified in the (default) identity in the identities table
	 * @return
	 * @throws P2PDDSQLException
	 */
	static Language[] get_preferred_languages() throws P2PDDSQLException {
    	ArrayList<ArrayList<Object>> id;
    	id=Application.db.select("SELECT "+table.identity.preferred_lang +
    			" FROM "+table.identity.TNAME+" AS i" +
    			" WHERE i."+table.identity.default_id+"==1 LIMIT 1;",
    			new String[]{});
    	if(id.size()==0){
    		if(DEBUG)System.err.println("DD:get_preferred_languages:No default identity found!");
    		return new Language[]{new Language("en", "US"),new Language("en", null)};
    	}
    	String preferred_lang = Util.getString(id.get(0).get(0));
    	String[]langs=preferred_lang.split(Pattern.quote(":"));
    	Language[]result = new Language[langs.length];
    	for(int k=0; k<result.length; k++){
    		String[]lcrt = langs[k].split(Pattern.quote("_"));
    		if(lcrt.length==2) result[k]= new Language(lcrt[0],lcrt[1]);
    		else if(lcrt.length==1) result[k]= new Language(lcrt[0],lcrt[0].toUpperCase());
    		if(DEBUG)System.err.println("DD:get_preferred_languages:Language="+langs[k]+"->"+result[k]);
    	}
    	/*
		new Language[]{
	    		new Language("ro", "RO"),new Language("ro", null),	
	    		new Language("en", "US"),new Language("en", null)};
	    		*/
		return result;
	}
	static String[] get_preferred_charsets() throws P2PDDSQLException {
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
	static String get_authorship_charset() throws P2PDDSQLException {
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
	static Language get_authorship_lang() throws P2PDDSQLException {
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
    			Application.warning(_("Error for "+dirs[k]), _("Error installing directories"));
    			return false;
    		}
    		String[] d=dirs[k].split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
    		if(d.length!=2){
    			Application.warning(_("Error for "+dirs[k]), _("Error installing directories"));
    			return false;
    		}
    		//Identity.listing_directories_string.add(dirs[k]);
    		try{
    			new InetSocketAddress(InetAddress.getByName(d[0]),Integer.parseInt(d[1]));
    		}catch(Exception e) {
    			Application.warning(_("Error for "+dirs[k]+"\nError: "+e.getMessage()), _("Error installing directories"));
    			return false;
    		}
    	}
		return true;
	}
	public static void load_listing_directories() throws P2PDDSQLException, NumberFormatException, UnknownHostException{
    	String ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
    	if(ld == null){
    		if(!DD.WARNED_NO_DIRS) {
    			Application.warning(_("No listing_directories found at initialization: " +
    					"\nDo not forget to add some later \n" +
    					"(e.g., from the DirectDemocracyP2P.net list)!\n" +
    					"If you have a stable IP, than you probably do not need it."), _("Configuration"));
    			DD.WARNED_NO_DIRS = true;
    		}
    		return;
    	}
    	String dirs[] = ld.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	Identity.listing_directories_string.clear();
    	Identity.listing_directories_inet.clear(); // just added
    	for(int k=0; k<dirs.length; k++) {
    		String[] d=dirs[k].split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
    		Identity.listing_directories_string.add(dirs[k]);
    		try{
    			Identity.listing_directories_inet.add(new InetSocketAddress(InetAddress.getByName(d[0]),Integer.parseInt(d[1])));
    		}catch(Exception e) {
    			Application.warning(_("Error for "+dirs[k]+"\nError: "+e.getMessage()), _("Error installing directories"));
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
						Application.warning(_("Error inserting:")+"\n"+_("value=")+Util.trimmed(value)+"\n"+_("field=")+field+"\n"+_("Error:")+e.getLocalizedMessage(), _("Database update error"));
					}
					if(DEBUG){
						Application.warning(_("Added absent property: ")+field, _("Properties"));
					
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
    			if(DEBUG)Application.warning(_("Added absent property: ")+field, _("Properties"));
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
    		if(DEBUG) System.err.println(_("No application record found for field: ")+field);
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
		
		if((on == false)&&(ds!=null)) {
				ds.turnOff();
				Application.ds=null;
				//DirectoryServer.db=null;
				if(DEBUG)System.out.println("DD:startDirectoryServer:Turning off");
		}
		if(ds != null){
			if(DEBUG)System.out.println("DD:startDirectoryServer:Turned off already");
			return false;
		}
		if(port <= 0) {
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
		if((on == false)&&(as!=null)) {
			as.turnOff(); Application.as=null;
			if(DEBUG)System.err.println("Turned off");
		}
		if(as != null){
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
		if((on == false)&&(aus!=null)) {
			aus.turnOff(); Application.aus=null;
			if(DEBUG) System.err.println("Turned off");
		}
		if(aus != null){
			if(DEBUG) System.err.println("Was not null");
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
		
		if((on == false)&&(ac!=null)) {ac.turnOff(); Application.ac=null;}
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
		String constGID = D_Constituent.getConstituentGlobalID(""+constituentID);
		return Util.getStoredSK(constGID);
	}
	/**
	 * Compute the SK based on my peerID (PK) from application table
	 * @return
	 */
	public static SK getMyPeerSK() {
		String myPeerGID = getMyPeerGIDFromIdentity();
		String mySecretID;
		SK result;
	   	if(DEBUG)System.out.println("DD:getMyPeerGID="+myPeerGID);		
		if(myPeerGID==null){
        	//if(_DEBUG)
        		System.err.println("DD:getMyPeerSK:no local peer ID!");
			return null;
		}
		result=Util.getStoredSK(myPeerGID, null);
		/*
		String sql = "SELECT "+table.key.secret_key+" FROM "+table.key.TNAME+" WHERE "+table.key.public_key+"=?;";
		try {
			 ArrayList<ArrayList<Object>> d = Application.db.select(sql, new String[]{myPeerID}, DEBUG);
			 if(d.size()==0) return null;
			 mySecretID = (String)d.get(0).get(0);
			 if(mySecretID==null) return null;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
	   	if(DEBUG)System.out.println("DD:mySecretID:mySecretPeerID="+mySecretID);		
		//return Cipher.getSK(myPeerID);
		result = Cipher.getSK(mySecretID);
		*/
		return result;
	}
	public static String getMyPeerName() {
		try {return DD.getAppText(DD.APP_my_peer_name);
		} catch (P2PDDSQLException e) {}
		return null;
	}
	/**
	 * Get my PK as string from application database
	 * @return
	 */
	public static String getMyPeerGIDFromDB() {
		try {
			String GID = DD.getAppText(DD.APP_my_global_peer_ID);
			String instance = DD.getAppText(DD.APP_my_peer_instance);
			D_PeerAddress.init_myself(GID, instance);
			return GID;
		} catch (P2PDDSQLException e) {}
		return null;
	}
	/**
	 * Get my PK as string from Identity
	 * @return
	 */
	public static String getMyPeerGIDFromIdentity() {
		synchronized(monitor_getMyPeerGIDFromIdentity) {
			if((Identity.current_peer_ID!=null) && (Identity.current_peer_ID.globalID!=null))
				return Identity.current_peer_ID.globalID;

			String result = getMyPeerGIDFromDB();
			if(Identity.current_peer_ID!=null) {
				Identity.current_peer_ID.globalID = result;
				Identity.current_peer_ID.instance = D_PeerAddress.get_myself().getInstance();
				Identity.current_peer_ID.name = D_PeerAddress.get_myself().getName();
			}
			return result;
		}
	}
	/**
	 * Set my PK and its hash in the application table
	 * @param pk
	 * @return
	 */
	public static void setMyPeerGID(PK pk) {
		String gID = Util.getKeyedIDPK(pk);
		setMyPeerGID(gID);
	}
	/**
	 * Sets gID and its hash in the
	 *  Identity.current_peer_ID
	 *  DD.APP_my_global_peer_ID
	 * @param gID
	 */
	public static void setMyPeerGID(String gID) {
		String gIDhash = Util.getGIDhash(gID);
		setMyPeerGID(gID, gIDhash);
	}
	public static void setMyPeerGID(String gID, String gIDhash) {
		if(DEBUG) out.println("\n*********\nDD:setMyPeerID;: start");
		if(Identity.current_peer_ID==null){
			Identity.current_peer_ID = Identity.initMyCurrentPeerIdentity();
		}
		Identity.current_peer_ID.globalID=gID;
		try {
			//byte[] bPK = pk.encode();
			//String gID = Util.byteToHex(bPK);
			//String gID = Util.getKeyedIDPK(pk);
			//String gIDhash = Util.getHash(bPK);
			DD.setAppText(DD.APP_my_global_peer_ID, gID);
			DD.setAppText(DD.APP_my_global_peer_ID_hash, gIDhash);
		} catch (P2PDDSQLException e) {}
		if(DEBUG) out.println("DD:setMyPeerID;: exit");
		if(DEBUG) out.println("*********");
		return;
	}
	/**
	 * Set my PK and its hash in the application table, and store pID is Identity.current_peer_ID.globalID
	 * @param pk
	 * @param pID =Util.getKeyedIDPK(Util.getKeyedIDPKBytes(keys));
	 * @return
	 */
	/*
	private static void setMyPeerID(PK pk, String global_pID) {
		if(DEBUG) out.println("\n*********\nDD:setMyPeerID: start");
		DD.setMyPeerID(pk); // sets ID in application table
		if(DEBUG) out.println("DD:setMyPeerID: exit");
		if(DEBUG) out.println("**************");
	}
	*/
	/**
	 * Get the hash of my peer ID from application table
	 * @return
	 */
	public static String getMyPeerIDhash() {
		try {return DD.getAppText(DD.APP_my_global_peer_ID_hash);
		} catch (P2PDDSQLException e) {}
		return null;
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
			if(EventQueue.isDispatchThread()){if(DD.controlPane!=null)  DD.controlPane.m_startBroadcastServer.setText(DD.controlPane.STOP_BROADCAST_SERVER);
			}else
				EventQueue.invokeLater(new Runnable() {
					public void run(){
						if(DD.controlPane!=null) DD.controlPane.m_startBroadcastServer.setText(DD.controlPane.STOP_BROADCAST_SERVER);
					}
				});
			return;
		}else{
			if(Application.g_BroadcastServer == null) return;
			Application.g_BroadcastServer.stopServer();
			Application.g_BroadcastServer=null;
			if(EventQueue.isDispatchThread()){if(DD.controlPane!=null)  DD.controlPane.m_startBroadcastServer.setText(DD.controlPane.START_BROADCAST_SERVER);
			}else
				EventQueue.invokeLater(new Runnable() {
					public void run(){
						if(DD.controlPane!=null) DD.controlPane.m_startBroadcastServer.setText(DD.controlPane.START_BROADCAST_SERVER);
					}
				});
			return;
		}
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
			if(EventQueue.isDispatchThread()){ if(DD.controlPane!=null) DD.controlPane.m_startBroadcastClient.setText(DD.controlPane.STOP_BROADCAST_CLIENT);
			}else
				EventQueue.invokeLater(new Runnable() {
					public void run(){
						if(DD.controlPane!=null) DD.controlPane.m_startBroadcastClient.setText(DD.controlPane.STOP_BROADCAST_CLIENT);
					}
				});
			return;
		}else{
			if(Application.g_BroadcastClient == null) return;
			Application.g_BroadcastClient.stopClient();
			Application.g_BroadcastClient=null;
			if(EventQueue.isDispatchThread()){ if(DD.controlPane!=null) DD.controlPane.m_startBroadcastClient.setText(DD.controlPane.START_BROADCAST_CLIENT);
			}else
				EventQueue.invokeLater(new Runnable() {
					public void run(){
						if(DD.controlPane!=null) DD.controlPane.m_startBroadcastClient.setText(DD.controlPane.START_BROADCAST_CLIENT);
					}
				});
			return;
		}		
	}

	public static void setSimulatorStatus(boolean run) {
		if(run) {
			if(Application.g_Simulator != null) return;
			Application.g_Simulator = new Fill_database();
			Application.g_Simulator.start();
			if(EventQueue.isDispatchThread()){if(DD.controlPane!=null)  DD.controlPane.m_startSimulator.setText(DD.controlPane.STOP_SIMULATOR);
			}else
				EventQueue.invokeLater(new Runnable() {
					public void run(){
						if(DD.controlPane!=null) DD.controlPane.m_startSimulator.setText(DD.controlPane.STOP_SIMULATOR);
					}
				});
			return;
		}else{
			if(Application.g_Simulator == null) return;
			Application.g_Simulator.stopSimulator();
			Application.g_Simulator=null;
			if(EventQueue.isDispatchThread()) DD.controlPane.m_startSimulator.setText(DD.controlPane.START_SIMULATOR);
			else
				EventQueue.invokeLater(new Runnable() {
					public void run(){
						DD.controlPane.m_startSimulator.setText(DD.controlPane.START_SIMULATOR);
					}
				});
			return;
		}		
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
	/**
	 * Creates peer ID if empty (i,e, if none in the Identity.current_peer_ID)
	 * @throws P2PDDSQLException
	 */
	public static void createMyPeerIDIfEmpty(){
		try{
			DD.getMyPeerGIDFromIdentity();
			
			if((Identity.current_peer_ID.globalID==null)){ //&&(Identity.create_missing_ID())) {
				if(DEBUG) out.println("DD:createMyPeerIDIfEmpty: will generate keys");
				
				String name = System.getProperty("user.name", _("MySelf"));
				String email = System.getProperty("user.name", _("MySelf"))+"@"+DD.DEFAULT_EMAIL_PROVIDER;
				PeerInput pi = new PeerInput(name, null, email);
				CreatePeer dialog = new CreatePeer(DD.frame, pi);
				PeerInput data = dialog.getData();
				if(!data.valid) {
					System.exit(-1);
				}
				if (DEBUG) System.out.println("DD:createMyPeerIDIfE New Peer"+data);
				
				//String name = D_PeerAddress.queryName(DD.frame);
				//Identity.emails = D_PeerAddress.queryNewEmails(DD.frame);
				//if(name==null) name = System.getProperty("user.name", _("MySelf"));
				//if(Identity.emails==null) name = System.getProperty("user.name", _("MySelf"))+"@localhost";
				Identity.emails = data.email;
				if(Identity.current_peer_ID.name==null)
					Identity.current_peer_ID.name = data.name;
				
				D_PeerAddress.createMyPeerID(data);
			}
		} catch (P2PDDSQLException e) {
			Application.warning(_("Error accessing database!"), _("Database troubles"));
		}	
	}

	public static final int[] VERSION_INTS = Util.getVersion(VERSION);
	public static final boolean SIGN_DIRECTORY_ANNOUNCEMENTS = false;
	public static final boolean KEEP_UNCERTIFIED_SOCKET_ADDRESSES = false;
	public static String WARN_OF_INVALID_SCRIPTS_BASE_DIR = null;

	/**
	 * Is the data for me as constituent fully input?
	 * @param organization_ID
	 * @param constituent_ID
	 * @return
	 */
	public boolean isMyConstituentReady(long constituent_ID){
		try{
			String sql = "SELECT "+table.constituent.name+" FROM "+table.constituent.TNAME+
			" WHERE "+table.constituent.constituent_ID+"=?;";
			ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{""+constituent_ID});
			if ((a.size()>=1) && (a.get(0).get(0)!=null)) return true;
		}catch(Exception e){}
		return false;
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
		if(!dbfile.exists() || !dbfile.isFile() || !dbfile.canRead()) return _("File not readable.");
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
		String sql = "SELECT "+table.Subscriber.subscriber_ID+
				" FROM "+table.Subscriber.TNAME+
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


	public static String getTopic(String globalID, String peer_ID) {
		String topic = null;
		try {
			topic = DD.getAppText(DD.MY_DEBATE_TOPIC);
			if((topic == null)||(topic.length()==0)) {
				topic = Util.getString(JOptionPane.showInputDialog(JFrameDropCatch.mframe,
						_("Declare a topic for your discussions"),
						_("Topic"), JOptionPane.PLAIN_MESSAGE, null, null, null));
				if(topic != null) DD.setAppTextNoSync(DD.MY_DEBATE_TOPIC, topic);
			}
		} catch (util.P2PDDSQLException e) {
			e.printStackTrace();
		}
		return topic;
	}


	public static boolean isThisAnApprovedPeer(String senderID) {
		// TODO Auto-generated method stub
		return true;
	}
	static void initGUILookAndFeel(){
	    //try {
		    System.setProperty("Quaqua.tabLayoutPolicy", "wrap");

            // Set cross-platform Java L&F (also called "Metal")
	    	//UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName());
		    //System.setProperty("Quaqua.tabLayoutPolicy", "wrap");
	    //}
	    //catch (UnsupportedLookAndFeelException e) {}
	    //catch (ClassNotFoundException e) {}
	    //catch (InstantiationException e) {}
	    //catch (IllegalAccessException e) {}
	}
	public static boolean GUI = true;
	//static final String CONSOLE="CONSOLE";
	// parameters: 
	// last parameter is the database (if different from CONSOLE)
	// the parameter 1 is the GID of the peer
	//
	// Should be: -d database, -p peerGID, -c [for colsole]
	static public void main(String args[]) throws util.P2PDDSQLException {
		ArrayList<String> potentialDatabases = new ArrayList<String>();
		String dfname = Application.DELIBERATION_FILE;
		String guID ="";
		
		char c;
		while((c=GetOpt.getopt(args, "d:p:c"))!=GetOpt.END){
			switch(c){
			case 'c':
				System.out.println("CONSOLE");
				GUI = false;
				break;
			case 'p':
				System.out.println("peer= "+GetOpt.optarg);
				guID = GetOpt.optarg;
				break;
			case 'd':
				System.out.println("db+="+GetOpt.optarg);
				potentialDatabases.add(GetOpt.optarg);
				break;
			case GetOpt.END:
				System.out.println("REACHED END OD OPTIONS");
				break;
			case '?':
				System.out.println("Options ?:"+GetOpt.optopt);
				return;
			default:
				System.out.println("Error: "+c);
				return;
			}
		}
		if(GetOpt.optind<args.length)
			System.out.println("OPTS: \""+args[GetOpt.optind]+"\"");

//		// take database as last parameter, or as default
//		if(args.length>0) {
//			dfname = args[args.length-1];
//			if(!CONSOLE.equals(dfname))
//				potentialDatabases.add(dfname);
//		}
		if(!Application.DELIBERATION_FILE.equals(dfname)) potentialDatabases.add(Application.DELIBERATION_FILE);
		
		//if((args.length>0) && CONSOLE.equals(args[0])) GUI=false;
		set_DEBUG();
		if(GUI) {
			initGUILookAndFeel();
			toolkit = Toolkit.getDefaultToolkit();
			int screen_width = (int)toolkit.getScreenSize().getWidth();
			int screen_height = (int)toolkit.getScreenSize().getHeight();
			if(screen_width > 680){
				DD.FRAME_OFFSET = (screen_width-600)/3;
				DD.FRAME_WIDTH = 600;
				DD.FRAME_HSTART = (screen_height-600)/3;
				DD.FRAME_HEIGHT = 450;
			}else{
				DD.FRAME_OFFSET = 0;
				DD.FRAME_WIDTH = screen_width;
				DD.FRAME_HSTART = 0;
				DD.FRAME_HEIGHT = screen_height;
			}
			frame = initMainFrameSplash();
		}
		startTime = Util.CalendargetInstance();
		//boolean DEBUG = true;
		System.setProperty("java.net.preferIPv4Stack", "true");
		//DD.USERDIR = System.getProperty("user.dir");
		if(DEBUG)System.out.println("User="+Application.USERNAME);
		if(DEBUG)System.out.println("Params: ["+args.length+"]="+Util.concat(args, " ; "));
		//System.out.println(Util.byteToHex((new BigInteger("1025")).toByteArray(), ":"));
		//System.out.println(Util.byteToHex((new BigInteger("257")).toByteArray(), ":"));
		
		if(DEBUG) System.out.println("DD:run: try databases");
		
		Hashtable<String, String> errors_db = new Hashtable<String, String>();
		for(String attempt : potentialDatabases) {
			if(DEBUG) System.out.println("DD:run: try db: "+attempt);
			String error = try_open_database(attempt);
			if(error == null ){
				if(DEBUG) System.out.println("DD:run: try db success: "+attempt);
				break;
			}
			errors_db.put(attempt, error);
			if(args.length>1) System.err.println(_("Failed attempt to open first choice file:")+" \""+attempt+"\": "+error);
		}
		if(Application.db == null) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new hds.DatabaseFilter());
			chooser.setName(_("Select database"));
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(false);

			int returnVal = chooser.showDialog(frame,_("Specify Database"));
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File fileDB = chooser.getSelectedFile();
				String error;
				try {
					error = try_open_database(fileDB.getCanonicalPath());
					if(error != null )
						errors_db.put(fileDB.getPath(), error);
				} catch (IOException e) {
					e.printStackTrace();
					errors_db.put(fileDB.getPath(), e.getLocalizedMessage());
				}
			}
			if(Application.db == null) {
				Application.warning(_("Missing database. Tried:\n "+Util.concat(errors_db, ",\n ", null, ": ")), _("Need to reinstall database!"));
				//Application.warning(_("Missing database. Tried: "+Util.concat(potentialDatabases, "\n, ", null)), _("Need to reinstall database!"));
				return;
			}
		}
		Identity.init_Identity();

		StartUpThread.detect_OS_and_store_in_DD_OS_var();
		StartUpThread.fill_install_paths_all_OSs_from_DB(); // to be done before importing!!!
		StartUpThread.switch_install_paths_to_ones_for_current_OS();
		if(DD.WARN_OF_INVALID_SCRIPTS_BASE_DIR != null) {
			Application.fixScriptsBaseDir(Application.CURRENT_SCRIPTS_BASE_DIR());
			StartUpThread.fill_install_paths_all_OSs_from_DB(); // to be done before importing!!!
			StartUpThread.switch_install_paths_to_ones_for_current_OS();
		}
		
		if(DEBUG) System.out.println("DD:run: done database, try import databases");
		DBInterface selected_db = Application.db; //save it as the import code may change the globals

		final String db_to_import = DD.getExactAppText (DD.APP_DB_TO_IMPORT);
		if(DEBUG) System.out.println("DD:run: done database, try import database from: "+db_to_import);
		if(db_to_import!=null) {
			//new Thread(){public void run(){
			//Application.warning(_("Trying to import data from old database indicator:"+"\n"+db_to_import), _("Importing database!"));
			//}}.start();
			//StartUpThread.detect_OS_fill_var();
			//StartUpThread.fill_OS_install_path();
			//StartUpThread.fill_OS_scripts_path();
			boolean imported = util.DB_Import.import_db(db_to_import, Application.DELIBERATION_FILE);

			if(_DEBUG) System.out.println("DD:run: done database, importing database from: "+db_to_import+" result="+imported);

			int q=0;
			if(!imported){
				q = Application.ask(_("Do you want to attempt import on the next startups?"),
						_("Want to be asked to import in the future?"),
						JOptionPane.YES_NO_OPTION);
			}else{
				String oldDB = db_to_import;
				String newDB = Application.DELIBERATION_FILE;
				DBInterface dbSrc = new DBInterface(oldDB);
				DBInterface dbDes = new DBInterface(newDB);
				boolean r=tools.MigrateMirrors.migrateIfNeeded(db_to_import, Application.DELIBERATION_FILE, dbSrc, dbDes);
				dbSrc.close();
				dbDes.close();
				if(!r){
					Application.warning(_("A failure happened while trying to migrate your mirrors and testers!\nInstall new mirrors."), _("Mirrors Failure"));
				}
			}
			if(imported || (q!=0)){
				if(_DEBUG) System.out.println("DD:run: done database, will clean "+DD.APP_DB_TO_IMPORT);
				Application.db = selected_db; // needed to enable setAppText
				DD.setAppText(DD.APP_DB_TO_IMPORT, null);
			}
			Application.warning(_("Result trying to import data from old database:")+"\n"+db_to_import+"\n result="+(imported?_("Success"):_("Failure")), _("Importing database!"));
		}

		Application.db = selected_db;
		if(DEBUG) System.out.println("DD:run: done database, done import databases");
		DDTranslation.db=Application.db;

		//Application.DB_PATH = new File(Application.DELIBERATION_FILE).getParent();
		//Application.db_dir = load_Directory_DB(Application.DB_PATH);
		
		Identity peer_ID = Identity.current_peer_ID;//getDefaultIdentity();
    	if(DEBUG) System.err.println("DD:main: identity");
		Identity id = Identity.getCurrentIdentity();
		if(guID!=null) {
			//guID = args[1];
			Identity.setCurrentIdentity(guID); // current_identity.globalID = ;
		}else{
			if(id!=null) guID = id.globalID;
		}
		if(DEBUG) System.out.println("My ID: "+guID);

		if(DEBUG) System.out.println("DD:run: init languages");		
		
    	DDTranslation.preferred_languages=get_preferred_languages();
    	DDTranslation.constituentID=UpdateMessages.getonly_constituent_ID(guID);
    	DDTranslation.organizationID=UpdateMessages.getonly_organizationID(id.globalOrgID, null);
    	DDTranslation.preferred_charsets=get_preferred_charsets();//new String[]{"latin"};
    	DDTranslation.authorship_charset=get_authorship_charset();//"latin";
    	DDTranslation.authorship_lang = get_authorship_lang();//new Language("ro","RO");
    	
		try {
			load_listing_directories();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		if(GUI) {
			if(DEBUG) System.out.println("DD:run: start GUI");		
	
			createAndShowGUI();
		}
		if(DEBUG) System.out.println("DD:run: start threads");
		
		BroadcastConsummerBuffer.queue = new BroadcastConsummerBuffer();
		new StartUpThread().start();
		DD.setAppText(DD.LAST_SOFTWARE_VERSION, DD.VERSION);
		/*
		boolean directory_server_on_start = getAppBoolean(DD_DIRECTORY_SERVER_ON_START);//"directory_server_on_start");
		boolean data_server_on_start = getAppBoolean(DD_DATA_SERVER_ON_START);//"data_server_on_start");
		boolean data_client_on_start = getAppBoolean(DD_DATA_CLIENT_ON_START);//"data_client_on_start");
		if (directory_server_on_start) startDirectoryServer(true, -1);
		if (data_server_on_start) startServer(true, Identity.current_peer_ID);
		if (data_client_on_start) startClient(true);
		*/
	}

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
