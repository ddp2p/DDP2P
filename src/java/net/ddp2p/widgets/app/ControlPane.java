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
 package net.ddp2p.widgets.app;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.hds.Server;
import net.ddp2p.common.hds.UDPServer;
import net.ddp2p.common.streaming.OrgHandling;
import net.ddp2p.common.updates.ClientUpdates;
import net.ddp2p.common.util.BMP;
import net.ddp2p.common.util.DD_Address;
import net.ddp2p.common.util.DD_DirectoryServer;
import net.ddp2p.common.util.DirectoryAddress;
import net.ddp2p.common.util.EmbedInMedia;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.StegoStructure;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.Broadcasting_Probabilities;
import net.ddp2p.widgets.components.TranslatedLabel;
import net.ddp2p.widgets.components.UpdatesFilterKey;
import net.ddp2p.widgets.dir_management.DirPanel;
import net.ddp2p.widgets.threads.ThreadsView;

import java.awt.Color;


//import song.peers.DualListBox;







import static net.ddp2p.common.util.Util.__;

class DDAddressFilter extends FileFilter {
	public boolean accept(File f) {
	    if (f.isDirectory()) {
	    	return true;
	    }

	    String extension = Util.getExtension(f);
	    if (extension != null) {
	    	//System.out.println("Extension: "+extension);
	    	if (extension.equals("txt") ||
	    		extension.equals("bmp") ||
	    		extension.equals("gif") ||
	    		extension.equals("ddb")) {
	    			//System.out.println("Extension: "+extension+" passes");
		        	return true;
	    	} else {
    			//System.out.println("Extension: "+extension+" fails");
	    		return false;
	    	}
	    }
		//System.out.println("Extension: absent - "+f);
	    return false;
	}

	@Override
	public String getDescription() {
		return __("DDP2P File Type (.txt, .bmp, .gif, .ddb)");
	}
}

class UpdatesFilter extends FileFilter {
	public boolean accept(File f) {
	    if (f.isDirectory()) {
	    	return true;
	    }

	    String extension = Util.getExtension(f);
	    if (extension != null) {
	    	//System.out.println("Extension: "+extension);
	    	if (extension.toLowerCase().equals("txt") ||
	    		extension.toLowerCase().equals("xml")) {
	    			//System.out.println("Extension: "+extension+" passes");
		        	return true;
	    	} else {
    			//System.out.println("Extension: "+extension+" fails");
	    		return false;
	    	}
	    }
		//System.out.println("Extension: absent - "+f);
	    return false;
	}

	@Override
	public String getDescription() {
		return __("Updates File Type (.txt, .xml)");
	}
}
public class ControlPane extends JTabbedPane implements ActionListener, ItemListener{
	private static final String START_DIR = __("Start Directory Server");
	private static final String STOP_DIR = __("Stop Directory Server");
	public static final String START_BROADCAST_CLIENT = __("Start Broadcast Client");
	public static final String STOP_BROADCAST_CLIENT = __("Stop Broadcast Client");
	public static final String START_BROADCAST_SERVER = __("Start Broadcast Server");
	public static final String STOP_BROADCAST_SERVER = __("Stop Broadcast Server");
	public static final String START_CLIENT_UPDATES = __("Start Client Updates");
	public static final String CLEAN_DOWNLOADED_UPDATES = __("Clean Downloaded Updates");
	public static final String STOP_CLIENT_UPDATES = __("Stop Client Updates");
	private static final String STARTING_CLIENT_UPDATES = __("Starting Client Updates");
	private static final String TOUCH_CLIENT_UPDATES = __("Touch Client Updates");
	private static final String STOPPING_CLIENT_UPDATES = __("Stopping Client Updates");
	private static final String START_CLIENT = __("Start Client");
	private static final String STOP_CLIENT = __("Stop Client");
	private static final String STARTING_CLIENT = __("Starting Client");
	private static final String TOUCH_CLIENT = __("Touch Client");
	private static final String STOPPING_CLIENT = __("Stopping Client");
	private static final String START_SERVER = __("Start Server");
	private static final String STOP_SERVER = __("Stop Server");
	private static final String STARTING_SERVER = __("Starting Server");
	private static final String STOPPING_SERVER = __("Stopping Server");
	private static final String START_USERVER = __("Start UDP Server");
	private static final String STOP_USERVER = __("Stop UDP Server");
	private static final String STARTING_USERVER = __("Starting UDP Server");
	private static final String STOPPING_USERVER = __("Stopping UDP Server");
	private static final String START_NSERVER = __("Start NAT Server");
	private static final String STOP_NSERVER = __("Stop NAT Server");
	private static final String STARTING_NSERVER = __("Starting NAT Server");
	private static final String STOPPING_NSERVER = __("Stopping NAT Server");
	private static final String BROADCASTABLE_YES = __("Stop broadcastable");
	private static final String BROADCASTABLE_NO = __("Start broadcastable");
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final String START_SIMULATOR = __("Start Simulator");
	public static final String STOP_SIMULATOR = __("Stop Simulator");
	public JButton m_startBroadcastServer=new JButton(START_BROADCAST_SERVER);
	public JButton m_startBroadcastClient=new JButton(START_BROADCAST_CLIENT);
	JButton startServer=new JButton(START_SERVER);
	JButton startUServer=new JButton(START_USERVER);
	JButton startNATServer=new JButton(START_NSERVER);
	JButton toggleBroadcastable=new JButton(BROADCASTABLE_NO);
	JButton startClient=new JButton(START_CLIENT);
	public JButton startClientUpdates=new JButton(START_CLIENT_UPDATES);
	public JButton cleanDownloadedUpdates=new JButton(CLEAN_DOWNLOADED_UPDATES);
	JButton touchClient=new JButton(TOUCH_CLIENT);
	JButton setPeerName=new JButton(__("Set Peer Name"));
	JButton setNewPeerID=new JButton(__("Set New Peer ID"));
	JButton addTabPeer=new JButton(__("Add Peer Tab"));
	JButton setPeerSlogan=new JButton(__("Set Peer Slogan"));
	JButton button_clean_SMTP=new JButton(__("Clear SMTP"));
	JButton button_clean_SMTP_password=new JButton(__("Clear SMTP Password"));
	JButton startDirectoryServer=new JButton(START_DIR);
	JButton saveMyAddress = new JButton(__("Save Address"));
	JButton signUpdates = new JButton(__("Sign Updates"));
	JButton loadPeerAddress = new JButton(__("Load Peer Address"));
	JButton importText = new JButton(__("Import Text"));
	JButton exportText = new JButton(__("Export Text"));
	JButton broadcastingProbabilities = new JButton(__("Set Broadcasting Probability"));
	JButton broadcastingQueueProbabilities = new JButton(__("Set Broadcasting Queue Probability"));
	JButton generationProbabilities = new JButton(__("Set Generation Probability"));
	JButton setListingDirectories = new JButton(__("Set Listing Directories"));
	JButton exportDirectories = new JButton(__("Export Directories"));
	JButton setUpdateServers = new JButton(__("Set Update Servers"));
	JButton keepThisVersion = new JButton(__("Fix: Keep This Version"));
	JButton setLinuxScriptsPath = new JButton(__("Set Linux Scripts Path"));
	JButton setWindowsScriptsPath = new JButton(__("Set Windows Scripts Path"));
	
	JTextArea importedText = new JTextArea();
	
	public final static String c_importText = "importText";
	public final static String c_exportText = "exportText";
	public static final String c_import = "import";
	public static final String c_export = "export";
	public static final String c_peerSlogan = "peerSlogan";
	public static final String c_peerID = "peerID";
	public static final String c_peerName = "peerName";
	public static final String c_broadcastable = "broadcastable";
	private static final String c_convert = "convert";
	private static final String c_linuxScriptsPath = "linuxScriptsPath";
	private static final String c_adh_IP = "adh_IP";
	private static final String c_adh_BIP = "adh_BIP";
	private static final String c_dd_SSID = "dd_SSID";
	private static final String c_adhoc_mask = "adhoc_mask";
	private static final String c_server = "server";
	private static final String c_userver = "userver";
	private static final String c_nserver = "nserver";
	private static final String c_broadcast_server = "broadcast_server";
	private static final String c_broadcast_client = "broadcast_client";
	private static final String c_simulator = "simulator";
	private static final String c_keepThisVersion = "keepThisVersion";
	private static final String c_updates_client = "updates_client";
	private static final String c_updates_clean = "updates_clean";
	private static final String c_client = "client";
	private static final String c_t_client = "t_client";
	private static final String c_directory = "directory";
	private static final String exportDirectories_action = "saveDirectories";
	private static final String setListingDirectories_action = "listingDirectories";
	private static final String action_button_clean_SMTP = "clean_SMTP";
	private static final String action_button_clean_SMTP_password = "clean_SMTP_password";
	private static final String action_textfield_SMTPHost = "SMTPHosts";
	private static final String action_clientPause = "clientPause";
	private static final String action_natBorer = "natBorer";
	private static final String action_textfield_EmailUsername = "emailUsername";
	
	public final static JFileChooser file_chooser_address_container = new JFileChooser();
	public final static JFileChooser file_chooser_updates_to_sign = new JFileChooser();
	public JCheckBox serveDirectly;
	public JCheckBox tcpButton;
	public JCheckBox udpButton;
	public JCheckBox natButton;
	public JTextField natBorer;
	public JButton m_startSimulator = new JButton(this.START_SIMULATOR);
	
	public JCheckBox q_MD;
	public JCheckBox q_C;
	public JCheckBox q_RA;
	public JCheckBox q_RE;
	public JCheckBox q_BH;
	public JCheckBox q_BR;
	public JTextField m_area_ADHOC_Mask;
	public JTextField m_area_ADHOC_IP;
	public JTextField m_area_ADHOC_BIP;
	public JTextField m_area_ADHOC_SSID;


	public JCheckBox m_dbgVerifySent;
	public JCheckBox m_dbgPlugin;
	public JCheckBox m_dbgUpdates;
	public JCheckBox m_dbgClient;
	public JCheckBox m_dbgConnections;
	public JCheckBox m_dbgUDPServerComm;
	public JCheckBox m_dbgUDPServerCommThread;
	public JCheckBox m_dbgDirServerComm;
	public JCheckBox m_dbgAliveServerComm;
	public JCheckBox m_dbgOrgs;
	public JCheckBox m_dbgOrgsHandling;
	public JCheckBox m_dbgPeers;
	public JCheckBox m_dbgTesters;
	public JCheckBox m_dbgPeersHandling;
	public JCheckBox m_dbgStreamingHandling;
	public Component makeDEBUGPanel(Container c) {
		
		m_dbgVerifySent = new JCheckBox("Verify sent signatures",DD.VERIFY_SENT_SIGNATURES);
		c.add(m_dbgVerifySent);
		m_dbgVerifySent.addItemListener(this);
		
		m_dbgTesters = new JCheckBox("DBG Testers",DD.DEBUG_PLUGIN);
		c.add(m_dbgTesters);
		m_dbgTesters.addItemListener(this);
		
		m_dbgPlugin = new JCheckBox("DBG Plugins",DD.DEBUG_PLUGIN);
		c.add(m_dbgPlugin);
		m_dbgPlugin.addItemListener(this);
		
		m_dbgUpdates = new JCheckBox("DBG Updates",net.ddp2p.common.updates.ClientUpdates.DEBUG);
		c.add(m_dbgUpdates);
		m_dbgUpdates.addItemListener(this);
		
		m_dbgClient = new JCheckBox("DBG Streaming Client",net.ddp2p.common.hds.ClientSync.DEBUG);
		c.add(m_dbgClient);
		m_dbgClient.addItemListener(this);
		
		m_dbgConnections = new JCheckBox("DBG Connections",net.ddp2p.common.hds.Connections.DEBUG);
		c.add(m_dbgConnections);
		m_dbgConnections.addItemListener(this);
		
		m_dbgUDPServerComm = new JCheckBox("DBG Communication UDP Streaming",net.ddp2p.common.hds.UDPServer.DEBUG);
		c.add(m_dbgUDPServerComm);
		m_dbgUDPServerComm.addItemListener(this);
		
		m_dbgUDPServerCommThread = new JCheckBox("Communication UDP Streaming Threads",net.ddp2p.common.hds.UDPServerThread.DEBUG);
		c.add(m_dbgUDPServerCommThread);
		m_dbgUDPServerCommThread.addItemListener(this);
		
		m_dbgDirServerComm = new JCheckBox("DBG Communication Dir",net.ddp2p.common.hds.UDPServer.DEBUG_DIR);
		c.add(m_dbgDirServerComm);
		m_dbgDirServerComm.addItemListener(this);
		
		m_dbgAliveServerComm = new JCheckBox("DBG Communication Alive Check",DD.DEBUG_COMMUNICATION_LOWLEVEL);
		c.add(m_dbgAliveServerComm);
		m_dbgAliveServerComm.addItemListener(this);
		
		m_dbgOrgs = new JCheckBox("DBG Organization",net.ddp2p.common.data.D_Organization.DEBUG);
		c.add(m_dbgOrgs);
		m_dbgOrgs.addItemListener(this);
		
		m_dbgOrgsHandling = new JCheckBox("DBG Organization Handling",net.ddp2p.common.streaming.OrgHandling.DEBUG);
		c.add(m_dbgOrgsHandling);
		m_dbgOrgsHandling.addItemListener(this);
		
		m_dbgPeers = new JCheckBox("DBG Peers", D_Peer.DEBUG);
		c.add(m_dbgPeers);
		m_dbgPeers.addItemListener(this);
		
		m_dbgPeersHandling = new JCheckBox("DBG Peers Streaming", net.ddp2p.common.streaming.UpdatePeersTable.DEBUG);
		c.add(m_dbgPeersHandling);
		m_dbgPeersHandling.addItemListener(this);
		
		m_dbgStreamingHandling = new JCheckBox("DBG General Streaming Reception", net.ddp2p.common.streaming.UpdateMessages.DEBUG);
		c.add(m_dbgStreamingHandling);
		m_dbgStreamingHandling.addItemListener(this);

		return c;
	}
	public boolean itemStateChangedDBG(ItemEvent e, Object source) {
	    if (source == this.m_dbgVerifySent) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.VERIFY_SENT_SIGNATURES = val;
	    } else if (source == this.m_dbgTesters) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.widgets.updatesKeys.UpdatesKeysModel.DEBUG = val;
	    	net.ddp2p.widgets.updates.UpdatesModel.DEBUG = val;
	    	net.ddp2p.widgets.updates.UpdatesTable.DEBUG = val;
	    	net.ddp2p.common.data.D_SoftwareUpdatesReleaseInfoByTester.DEBUG = val;
	    	net.ddp2p.common.data.D_Tester.DEBUG = val;
	    	net.ddp2p.common.data.D_MirrorInfo.DEBUG = val;
	    	net.ddp2p.common.data.D_Tester.DEBUG = val;
	    } else if (source == this.m_dbgUpdates) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.updates.ClientUpdates.DEBUG = val;
	    	//WSupdate.HandleService.DEBUG = val;
	    } else if (source == this.m_dbgPlugin) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.DEBUG_PLUGIN = val;
	    } else if (source == this.m_dbgClient) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.hds.ClientSync.DEBUG = val;
	    } else if (source == this.m_dbgConnections) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.hds.Connections.DEBUG = val;
	    } else if (source == this.m_dbgUDPServerComm) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.hds.UDPServer.DEBUG = val;
	    } else if (source == this.m_dbgUDPServerCommThread) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.hds.UDPServerThread.DEBUG = val;
	    } else if (source == this.m_dbgDirServerComm) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.hds.UDPServer.DEBUG_DIR = val;
	    } else if (source == this.m_dbgAliveServerComm) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.DEBUG_COMMUNICATION_LOWLEVEL = val;
	    } else if (source == this.m_dbgOrgs) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.data.D_Organization.DEBUG = val;
	    } else if (source == this.m_dbgOrgsHandling) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.streaming.OrgHandling.DEBUG = val;
	    } else if (source == this.m_dbgPeers) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.data.D_Peer.DEBUG = val;
	    } else if (source == this.m_dbgPeersHandling) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.streaming.UpdatePeersTable.DEBUG = val;
	    } else if (source == this.m_dbgStreamingHandling) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	net.ddp2p.common.streaming.UpdateMessages.DEBUG = val;
	    } else return false;
	    return true;
	}	
	
	public JCheckBox m_const_orphans_show_with_neighbors;
	public JCheckBox m_const_orphans_filter_by_org;
	public JCheckBox m_const_orphans_show_in_root;
	public Component makeGUIConfigPanel(Container c) {
		m_const_orphans_show_with_neighbors = new JCheckBox("Constituents Shown with Neighbors", DD.CONSTITUENTS_ORPHANS_SHOWN_BESIDES_NEIGHBORHOODS);
		c.add(m_const_orphans_show_with_neighbors);
		
		m_const_orphans_filter_by_org = new JCheckBox("Orphan Constituents/Neigh shown only from curent org", DD.CONSTITUENTS_ORPHANS_FILTER_BY_ORG);
		c.add(m_const_orphans_filter_by_org);
		
		m_const_orphans_show_in_root = new JCheckBox("Show constituent/neighborhood orphans in root", DD.CONSTITUENTS_ORPHANS_SHOWN_IN_ROOT);
		c.add(m_const_orphans_show_in_root);
		
		m_const_orphans_show_with_neighbors.addItemListener(this);
		m_const_orphans_filter_by_org.addItemListener(this);
		m_const_orphans_show_in_root.addItemListener(this);
		
		return c;
	}
	
	public JCheckBox m_wireless_use_dictionary;
	public boolean itemStateChangedWireless(ItemEvent e, Object source) {
	    if (source == this.m_wireless_use_dictionary) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.ADHOC_MESSAGES_USE_DICTIONARIES = val;
	    } else if (source == q_MD) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	Broadcasting_Probabilities._broadcast_queue_md = val;
	    	Broadcasting_Probabilities.setCurrentProbabilities();
	    	DD.setAppBoolean(DD.APP_Q_MD, (val));
	    } else if (source == q_C) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	Broadcasting_Probabilities._broadcast_queue_c = val;
	    	Broadcasting_Probabilities.setCurrentProbabilities();
	    	DD.setAppBoolean(DD.APP_Q_C, (val));
	    } else if (source == q_RA) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	Broadcasting_Probabilities._broadcast_queue_ra = val;
	    	Broadcasting_Probabilities.setCurrentProbabilities();
	    	DD.setAppBoolean(DD.APP_Q_RA, (val));
	    } else if (source == q_RE) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	Broadcasting_Probabilities._broadcast_queue_re = val;
	    	Broadcasting_Probabilities.setCurrentProbabilities();
	    	DD.setAppBoolean(DD.APP_Q_RE, (val));
	    } else if (source == q_BH) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	Broadcasting_Probabilities._broadcast_queue_bh = val;
	    	Broadcasting_Probabilities.setCurrentProbabilities();
	    	DD.setAppBoolean(DD.APP_Q_BH, (val));
	    } else if (source == q_BR) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	Broadcasting_Probabilities._broadcast_queue_br = val;
	    	Broadcasting_Probabilities.setCurrentProbabilities();
	    	DD.setAppBoolean(DD.APP_Q_BR, (val));
	    } else return false;
	    return true;
	}
	public Container makeWirelessPanel(Container c){
		
		m_wireless_use_dictionary = new JCheckBox("Use Dictionaries for Ad-hoc communication", DD.ADHOC_MESSAGES_USE_DICTIONARIES);
		c.add(m_wireless_use_dictionary);
		m_wireless_use_dictionary.addItemListener(this);
		
		c.add(broadcastingQueueProbabilities);
		broadcastingQueueProbabilities.addActionListener(this);
		broadcastingQueueProbabilities.setActionCommand("broadcastingQueueProbabilities");
		
		JPanel b = new JPanel();
		b.setBorder(new BevelBorder(2));
		q_MD = new JCheckBox("QM");
		b.add(q_MD);
		q_MD.setMnemonic(KeyEvent.VK_M); 

		q_C = new JCheckBox("QC");
		b.add(q_C);
		q_C.setMnemonic(KeyEvent.VK_C); 

		q_RA = new JCheckBox("QRA");
		b.add(q_RA);
		q_RA.setMnemonic(KeyEvent.VK_R); 

		q_RE = new JCheckBox("QRE");
		b.add(q_RE);
		q_RE.setMnemonic(KeyEvent.VK_E); 
		//System.out.println("cont Selecting right check:"+Broadcasting_Probabilities._broadcast_queue_re);

		q_BH = new JCheckBox("QBH");
		b.add(q_BH);
		q_BH.setMnemonic(KeyEvent.VK_B); 

		q_BR = new JCheckBox("QBR");
		b.add(q_BR);
		q_BR.setMnemonic(KeyEvent.VK_R); 
		c.add(b);//new JScrollPane(b));
		
		q_MD.addItemListener(this);
		q_C.addItemListener(this);
		q_RA.addItemListener(this);
		q_RE.addItemListener(this);
		q_BH.addItemListener(this);
		q_BR.addItemListener(this);

		JTextField _255255 = new JTextField("255.255.255,255");
		
		JPanel mask = new JPanel();
		mask.add(new JLabel(__("ADHOC Mask, e.g.:")+DD.DEFAULT_WIRELESS_ADHOC_DD_NET_MASK), BorderLayout.EAST);
		m_area_ADHOC_Mask = new JTextField();
		m_area_ADHOC_Mask.setPreferredSize(_255255.getPreferredSize());
		mask.add(m_area_ADHOC_Mask, BorderLayout.WEST);
		m_area_ADHOC_Mask.setText(DD.WIRELESS_ADHOC_DD_NET_MASK);
		m_area_ADHOC_Mask.addActionListener(this);
		m_area_ADHOC_Mask.setActionCommand("adhoc_mask");
		c.add(mask);

		JPanel adh_IP = new JPanel();
		adh_IP.add(new JLabel(__("ADHOC IP Base, e.g.:")+DD.DEFAULT_WIRELESS_ADHOC_DD_NET_IP_BASE), BorderLayout.EAST);
		m_area_ADHOC_IP = new JTextField();
		m_area_ADHOC_IP.setPreferredSize(_255255.getPreferredSize());
		adh_IP.add(m_area_ADHOC_IP, BorderLayout.WEST);
		m_area_ADHOC_IP.setText(DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
		m_area_ADHOC_IP.addActionListener(this);
		m_area_ADHOC_IP.setActionCommand("adh_IP");
		c.add(adh_IP);

		JPanel adh_BIP = new JPanel();
		adh_BIP.add(new JLabel(__("ADHOC Broadcast IP, e.g.:")+DD.DEFAULT_WIRELESS_ADHOC_DD_NET_BROADCAST_IP), BorderLayout.EAST);
		m_area_ADHOC_BIP = new JTextField();
		m_area_ADHOC_BIP.setPreferredSize(_255255.getPreferredSize());
		adh_BIP.add(m_area_ADHOC_BIP, BorderLayout.WEST);
		m_area_ADHOC_BIP.setText(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP);
		m_area_ADHOC_BIP.addActionListener(this);
		m_area_ADHOC_BIP.setActionCommand("adh_BIP");
		c.add(adh_BIP);

		JPanel dd_SSID = new JPanel();
		dd_SSID.add(new JLabel(__("ADHOC SSID, e.g.:")+DD.DEFAULT_DD_SSID), BorderLayout.EAST);
		m_area_ADHOC_SSID = new JTextField();
		m_area_ADHOC_IP.setPreferredSize(new JTextField(DD.DEFAULT_DD_SSID+"      ").getPreferredSize());
		dd_SSID.add(m_area_ADHOC_SSID, BorderLayout.WEST);
		m_area_ADHOC_SSID.setText(DD.DD_SSID);
		m_area_ADHOC_SSID.addActionListener(this);
		m_area_ADHOC_SSID.setActionCommand("dd_SSID");
		c.add(dd_SSID);
		
		c.add(broadcastingProbabilities);
		broadcastingProbabilities.addActionListener(this);
		broadcastingProbabilities.setActionCommand("broadcastingProbabilities");

		c.add(m_startBroadcastClient);
		m_startBroadcastClient.addActionListener(this);
		m_startBroadcastClient.setMnemonic(KeyEvent.VK_B);
		m_startBroadcastClient.setActionCommand("broadcast_client");

		c.add(m_startBroadcastServer);
		m_startBroadcastServer.addActionListener(this);
		//m_startBroadcastServer.setMnemonic(KeyEvent.VK_B);
		m_startBroadcastServer.setActionCommand("broadcast_server");
		
		return c;
	}
	
	public JCheckBox m_comm_Block_New_Orgs;
	public JCheckBox m_comm_Block_New_Orgs_Bad_signature;
	public JCheckBox m_comm_Warns_New_Wrong_Signature;
	public JCheckBox m_comm_Fail_Frag_Wrong_Signature;
	public JCheckBox m_comm_Accept_Unknown_Constituent_Fields;
	public JCheckBox m_comm_Accept_Answer_From_New;
	public JCheckBox m_comm_Accept_Answer_From_Anonymous;
	public JCheckBox m_comm_Accept_Unsigned_Requests;
	public JCheckBox m_comm_Use_New_Arriving_Peers_Contacting_Me;
	public JCheckBox m_comm_Block_New_Arriving_Peers_Contacting_Me;
	public JCheckBox m_comm_Block_New_Arriving_Peers_Answering_Me;
	public JCheckBox m_comm_Block_New_Arriving_Peers_Forwarded_To_Me;
	private JTextField clientPause;
	private JTextField textfield_SMTPHost;
	private TranslatedLabel label_SMTPHost;
	private JTextField textfield_EmailUsername;
	private TranslatedLabel label_EmailUsername;
	private ThreadsView threadsView;
	
	public boolean itemStateChangedCOMM(ItemEvent e, Object source) {
	    if (source == this.m_comm_Block_New_Orgs) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.BLOCK_NEW_ARRIVING_ORGS = val;
	    } else if (source == this.m_comm_Use_New_Arriving_Peers_Contacting_Me) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME = val;
	    } else if (source == this.m_comm_Block_New_Arriving_Peers_Contacting_Me) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME = val;
	    } else if (source == this.m_comm_Block_New_Arriving_Peers_Answering_Me) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.BLOCK_NEW_ARRIVING_PEERS_ANSWERING_ME = val;
	    } else if (source == this.m_comm_Block_New_Arriving_Peers_Forwarded_To_Me) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.BLOCK_NEW_ARRIVING_PEERS_FORWARDED_TO_ME = val;
	    } else if (source == this.m_comm_Block_New_Orgs_Bad_signature) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.BLOCK_NEW_ARRIVING_ORGS_WITH_BAD_SIGNATURE = val;
	    } else if (source == this.m_comm_Warns_New_Wrong_Signature) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.WARN_OF_FAILING_SIGNATURE_ONRECEPTION = val;
	    } else if (source == this.m_comm_Fail_Frag_Wrong_Signature) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.PRODUCE_FRAGMENT_SIGNATURE = DD.VERIFY_FRAGMENT_SIGNATURE = val;
	    	Application_GUI.warning(__("Currently this option is not saved"), __("Partial Implementation"));
	    } else if (source == this.m_comm_Accept_Unsigned_Requests) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.ACCEPT_STREAMING_REQUEST_UNSIGNED = val;
	    } else if (source == this.m_comm_Accept_Unknown_Constituent_Fields) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS = val;
	    } else if (source == this.m_comm_Accept_Answer_From_New) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.ACCEPT_STREAMING_ANSWER_FROM_NEW_PEERS = val;
	    } else if (source == this.m_comm_Accept_Answer_From_Anonymous) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.ACCEPT_STREAMING_ANSWER_FROM_ANONYMOUS_PEERS = val;
	    }else return false;
	    return true;
	}
	public Container makeWiredCommunicationPanel(Container c) {		
		
		m_comm_Use_New_Arriving_Peers_Contacting_Me = new JCheckBox("Request streaming from peers contacting me", DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME);
		c.add(m_comm_Use_New_Arriving_Peers_Contacting_Me);
		m_comm_Use_New_Arriving_Peers_Contacting_Me.addItemListener(this);
		
		m_comm_Block_New_Arriving_Peers_Contacting_Me = new JCheckBox("Refuse streaming from peers contacting me", DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME);
		c.add(m_comm_Block_New_Arriving_Peers_Contacting_Me);
		m_comm_Block_New_Arriving_Peers_Contacting_Me.addItemListener(this);
		
		m_comm_Block_New_Arriving_Peers_Answering_Me = new JCheckBox("Refuse streaming from new peers answering my requests", DD.BLOCK_NEW_ARRIVING_PEERS_ANSWERING_ME);
		c.add(m_comm_Block_New_Arriving_Peers_Answering_Me);
		m_comm_Block_New_Arriving_Peers_Answering_Me.addItemListener(this);
		
		m_comm_Block_New_Arriving_Peers_Forwarded_To_Me = new JCheckBox("Refuse streaming from new peers forwarded to me", DD.BLOCK_NEW_ARRIVING_PEERS_FORWARDED_TO_ME);
		c.add(m_comm_Block_New_Arriving_Peers_Forwarded_To_Me);
		m_comm_Block_New_Arriving_Peers_Forwarded_To_Me.addItemListener(this);
		
		m_comm_Accept_Unsigned_Requests = new JCheckBox("Accept streaming requests that are not signed", DD.ACCEPT_STREAMING_REQUEST_UNSIGNED);
		c.add(m_comm_Accept_Unsigned_Requests);
		m_comm_Accept_Unsigned_Requests.addItemListener(this);
		
		m_comm_Accept_Answer_From_New = new JCheckBox("Accept streaming answers from new peers", DD.ACCEPT_STREAMING_ANSWER_FROM_NEW_PEERS);
		c.add(m_comm_Accept_Answer_From_New);
		m_comm_Accept_Answer_From_New.addItemListener(this);
		
		m_comm_Accept_Answer_From_Anonymous = new JCheckBox("Accept streaming answers from anonymous peers (not broadcasting themselves)", DD.ACCEPT_STREAMING_ANSWER_FROM_ANONYMOUS_PEERS);
		c.add(m_comm_Accept_Answer_From_Anonymous);
		m_comm_Accept_Answer_From_Anonymous.addItemListener(this);

		m_comm_Block_New_Orgs = new JCheckBox("Block New Orgs",DD.BLOCK_NEW_ARRIVING_ORGS);
		c.add(m_comm_Block_New_Orgs);
		m_comm_Block_New_Orgs.addItemListener(this);
		
		m_comm_Block_New_Orgs_Bad_signature = new JCheckBox("Block New Orgs on Bad Signature", DD.BLOCK_NEW_ARRIVING_ORGS_WITH_BAD_SIGNATURE);
		c.add(m_comm_Block_New_Orgs_Bad_signature);
		m_comm_Block_New_Orgs_Bad_signature.addItemListener(this);
		
		m_comm_Warns_New_Wrong_Signature = new JCheckBox("Warn on incoming signature verification failure (if verified)", DD.WARN_OF_FAILING_SIGNATURE_ONRECEPTION);
		c.add(m_comm_Warns_New_Wrong_Signature);
		m_comm_Warns_New_Wrong_Signature.addItemListener(this);
		
		m_comm_Fail_Frag_Wrong_Signature = new JCheckBox("Fail on fragment signature verification failure", DD.VERIFY_FRAGMENT_SIGNATURE);
		c.add(m_comm_Fail_Frag_Wrong_Signature);
		m_comm_Fail_Frag_Wrong_Signature.addItemListener(this);
		
		m_comm_Accept_Unknown_Constituent_Fields = new JCheckBox("Accept temporary or new constituent data field types", DD.ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS);
		c.add(m_comm_Accept_Unknown_Constituent_Fields);
		m_comm_Accept_Unknown_Constituent_Fields.addItemListener(this);

		c.add(startServer);
		startServer.addActionListener(this);
		startServer.setMnemonic(KeyEvent.VK_S);
		startServer.setActionCommand("server");	

		c.add(startUServer);
		startUServer.addActionListener(this);
		startUServer.setMnemonic(KeyEvent.VK_S);
		startUServer.setActionCommand("userver");
		
		c.add(startClient);
		startClient.addActionListener(this);
		startClient.setActionCommand("client");
		
		c.add(touchClient);
		touchClient.addActionListener(this);
		touchClient.setActionCommand("t_client");

		serveDirectly = new JCheckBox("Serve Directly");
		c.add(new JPanel().add(serveDirectly));
		serveDirectly.setMnemonic(KeyEvent.VK_T); 
		serveDirectly.setSelected(OrgHandling.SERVE_DIRECTLY_DATA);
		serveDirectly.addItemListener(this);
		
		JPanel cli = new JPanel();
		cli.setBorder(new BevelBorder(1));
		tcpButton = new JCheckBox("TCP Client");
		cli.add(tcpButton);
	    tcpButton.setMnemonic(KeyEvent.VK_T); 
	    tcpButton.setSelected(DD.ClientTCP);
	    tcpButton.addItemListener(this);

	    udpButton = new JCheckBox("UDP Client");
		cli.add(udpButton);
	    udpButton.setMnemonic(KeyEvent.VK_U); 
	    udpButton.setSelected(DD.ClientUDP);
	    udpButton.addItemListener(this);
	    c.add(cli);

	    natButton = new JCheckBox("NAT Client");
		cli.add(natButton);
	    natButton.setMnemonic(KeyEvent.VK_N); 
	    natButton.setSelected(DD.ClientNAT);
	    natButton.addItemListener(this);
	    c.add(cli);
	    
	    JPanel intervals = new JPanel();
	    JPanel nats = new JPanel();
	    natBorer = new JTextField(Server.TIMEOUT_UDP_NAT_BORER+"");
	    nats.add(new TranslatedLabel("NAT: "));
	    nats.add(natBorer);
	    natBorer.setActionCommand(action_natBorer);
	    natBorer.addActionListener(this);
	    intervals.add(nats);
	    
	    JPanel client_pause = new JPanel();
	    clientPause = new JTextField(ClientSync.PAUSE+"");
	    client_pause.add(new TranslatedLabel("Pull Interval: "));
	    client_pause.add(clientPause);
	    clientPause.setActionCommand(action_clientPause);
	    clientPause.addActionListener(this);
	    intervals.add(client_pause);
	    c.add(intervals);
	    
	    return c;
	}
	public Container makeSimulatorPanel(Container c){
		
		c.add(generationProbabilities);
		generationProbabilities.addActionListener(this);
		generationProbabilities.setActionCommand("generationProbabilities");

		c.add(m_startSimulator);
		m_startSimulator.addActionListener(this);
		//m_startSimulator.setMnemonic(KeyEvent.VK_B);
		m_startSimulator.setActionCommand("simulator");

		return c;
	}
	public Container makePathsPanel(Container c){
		
		c.add(setLinuxScriptsPath);
		setLinuxScriptsPath.addActionListener(this);
		setLinuxScriptsPath.setActionCommand("linuxScriptsPath");
		
		c.add(setWindowsScriptsPath);
		setWindowsScriptsPath.addActionListener(this);
		setWindowsScriptsPath.setActionCommand("windowsScriptsPath");
		
		return c;
	}
	public Container makeDirectoriesPanel(Container c){
		
		JPanel headerControls = new JPanel();
		headerControls.add(startDirectoryServer);
		startDirectoryServer.addActionListener(this);
		startDirectoryServer.setActionCommand("directory");
		
		headerControls.add(setListingDirectories);
		setListingDirectories.addActionListener(this);
		setListingDirectories.setActionCommand(setListingDirectories_action);
		
		headerControls.add(exportDirectories);
		exportDirectories.addActionListener(this);
		exportDirectories.setActionCommand(exportDirectories_action);
		headerControls.setBackground(Color.DARK_GRAY);
		
		JPanel p= new JPanel(new BorderLayout());
		p.add(headerControls, BorderLayout.NORTH);
		DirPanel dirPanel = new DirPanel();
		p.add(dirPanel);
		c.add(p); 
		return c;
	}
	public Container makeUpdatesPanel(Container c){
		c.add(new JLabel(__("Version: ")+DD.VERSION));
		
		c.add(setUpdateServers);
		setUpdateServers.addActionListener(this);
		setUpdateServers.setActionCommand("updateServers");
		
		c.add(keepThisVersion);
		keepThisVersion.addActionListener(this);
		keepThisVersion.setActionCommand("keepThisVersion");
		
		c.add(signUpdates);
		signUpdates.addActionListener(this);
		signUpdates.setActionCommand("signUpdates");
		
		c.add(startClientUpdates);
		startClientUpdates.addActionListener(this);
		startClientUpdates.setActionCommand(c_updates_client);
		
		c.add(cleanDownloadedUpdates);
		cleanDownloadedUpdates.addActionListener(this);
		cleanDownloadedUpdates.setActionCommand(c_updates_clean);
		
		return c;
	}
	public Container makeSMTPPanel(Container cc){
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
        //c.ipadx=40;
        //c.ipady=30;
        //c.insets= new Insets(5, 5, 5, 5);//Insets(int top, int left, int bottom, int right)  
        //c.fill = GridBagConstraints.BOTH;//.HORIZONTAL;
        c.fill = GridBagConstraints.NONE;

        button_clean_SMTP.setFont(new Font("Times New Roman",Font.BOLD,14));
        button_clean_SMTP.addActionListener(this);
        button_clean_SMTP.setActionCommand(action_button_clean_SMTP);
		c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
		p.add(button_clean_SMTP, c);
		
        button_clean_SMTP_password.setFont(new Font("Times New Roman",Font.BOLD,14));
        button_clean_SMTP_password.addActionListener(this);
        button_clean_SMTP.setActionCommand(action_button_clean_SMTP_password);
		c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
		p.add(button_clean_SMTP_password, c);
		
	    textfield_SMTPHost = new JTextField(net.ddp2p.java.email.EmailManager.getSMTPHost(Identity.current_id_branch));
	    label_SMTPHost = new TranslatedLabel("SMTP Host");
	    textfield_SMTPHost.setActionCommand(action_textfield_SMTPHost);
	    textfield_SMTPHost.addActionListener(this);
		c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
	    p.add(label_SMTPHost, c);
		c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
	    p.add(textfield_SMTPHost, c);
		
	    textfield_EmailUsername = new JTextField(net.ddp2p.java.email.EmailManager.getEmailUsername(Identity.current_id_branch));
	    label_EmailUsername = new TranslatedLabel("Email Username");
	    textfield_EmailUsername.setActionCommand(action_textfield_EmailUsername);
	    textfield_EmailUsername.addActionListener(this);
		c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
	    p.add(label_EmailUsername, c);
		c.gridx = 1;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
	    p.add(textfield_EmailUsername, c);
		
        cc.add(p);
        JScrollPane js = new JScrollPane(cc);
		return js;//pBL;
	}
	public Container makePeerPanel(Container cc){

		JPanel p = new JPanel(new GridBagLayout());
		//p.setBackground(Color.DARK_GRAY);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
        c.gridy = 0;
        //c.weightx = 0.5;
        //c.weighty = 0.5;
        c.ipadx=40;
        c.ipady=30;
        c.insets= new Insets(5, 5, 5, 5);//Insets(int top, int left, int bottom, int right)  
        c.fill = GridBagConstraints.BOTH;//.HORIZONTAL;
        //pp.add(p,c);
        toggleBroadcastable.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(toggleBroadcastable,c);
		toggleBroadcastable.addActionListener(this);
		toggleBroadcastable.setMnemonic(KeyEvent.VK_B);
		toggleBroadcastable.setActionCommand(c_broadcastable);
		boolean val = Identity.getAmIBroadcastable();
		this.toggleBroadcastable.setText(val?this.BROADCASTABLE_YES:this.BROADCASTABLE_NO);
		c.gridx = 1;
		c.gridy = 0;
		setPeerName.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(setPeerName, c);
		setPeerName.addActionListener(this);
		setPeerName.setActionCommand(c_peerName);
		c.gridx = 0;
        c.gridy = 1;
        setNewPeerID.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(setNewPeerID, c);
		setNewPeerID.addActionListener(this);
		setNewPeerID.setActionCommand(c_peerID);
		
		c.gridx = 1;
        c.gridy = 1;
        setPeerSlogan.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(setPeerSlogan, c);
		setPeerSlogan.addActionListener(this);
		setPeerSlogan.setActionCommand(c_peerSlogan);
		
		c.gridx = 0;
        c.gridy = 2;
        saveMyAddress.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(saveMyAddress, c);
		saveMyAddress.addActionListener(this);
		saveMyAddress.setActionCommand(c_export);
		c.gridx = 1;
        c.gridy = 2;
        loadPeerAddress.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(loadPeerAddress, c);
		loadPeerAddress.addActionListener(this);
		loadPeerAddress.setActionCommand(c_import);
		
		c.gridx = 0;
        c.gridy = 3;
        // importedText.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(new JScrollPane(importedText), c);
        importedText.setText(__("Paste here a text \nencapsulating an object, \nand push the ImportText Button!"));
        importedText.selectAll();
		//saveMyAddress.addActionListener(this);
		//saveMyAddress.setActionCommand("export");
		c.gridx = 1;
        c.gridy = 3;
        importText.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(importText, c);
		importText.addActionListener(this);
		importText.setActionCommand(c_importText);
		
		c.gridx = 1;
        c.gridy = 4;
        exportText.setFont(new Font("Times New Roman",Font.BOLD,14));
		p.add(exportText, c);
		exportText.addActionListener(this);
		exportText.setActionCommand(c_exportText);
        
// only for test
//        JButton convertGIT_BMP = new JButton(_("Convert GIF to BMP"));
//        		
//		c.add(convertGIT_BMP);
//		convertGIT_BMP.addActionListener(this);
//		convertGIT_BMP.setActionCommand("convert");
		
//		c.add(addTabPeer);
//		addTabPeer.addActionListener(this);
//		addTabPeer.setActionCommand("tab_peer");
//        pBL.add(p, BorderLayout.CENTER);
        cc.add(p);
        JScrollPane js = new JScrollPane(cc);
		return js;//pBL;
	}
	Component makeUpdatesMirrorsPanel(){
		if(DEBUG) System.out.println("ControlPane:makeUpdatedPanel: start");
		JPanel panel = new JPanel(new BorderLayout());
		
		//UpdatesTable t = new UpdatesTable();
		net.ddp2p.widgets.updates.UpdatesPanel t = new net.ddp2p.widgets.updates.UpdatesPanel();
		if(DEBUG) System.out.println("ControlPane:makeUpdatedPanel: constr");
		//t.getColumnModel().getColumn(TABLE_COL_QOT_ROT).setCellRenderer(new ComboBoxRenderer());
		panel.add(t);
		//PeersTest newContentPane = new PeersTest(db);
		//newContentPane.setOpaque(true);
		//frame.setContentPane(t.getScrollPane());
		//panel.add(t.getTableHeader(),BorderLayout.NORTH);
		if(DEBUG) System.out.println("ControlPane:makeUpdatedPanel: done");
		return panel;
	}
	public ControlPane() throws P2PDDSQLException {
		//super(new GridLayout(0, 4, 5, 5));
		JTabbedPane tabs = this; //new JTabbedPane();
		//this.add(tabs);
		GridLayout gl = new GridLayout(0,1);
//		JPanel
//		peer=new JPanel(gl),
//		path=new JPanel(gl),
//		wired=new JPanel(gl),
//		dirs=new JPanel(gl),
//		updates=new JPanel(gl),
//		wireless=new JPanel(gl),
//		sim=new JPanel(gl);
		
		tabs.addTab("Peers Data", makePeerPanel(new JPanel(gl)));
		tabs.addTab("Comm", makeWiredCommunicationPanel(new JPanel(gl)));
		tabs.addTab("SMTP", makeSMTPPanel(new JPanel(gl)));
		tabs.addTab("Paths", makePathsPanel(new JPanel(gl)));
		tabs.addTab("Dirs", makeDirectoriesPanel(new JPanel(gl)));
		tabs.addTab("Updates", makeUpdatesPanel(new JPanel(gl)));
		tabs.addTab(__("Update Mirrors"), makeUpdatesMirrorsPanel());
		tabs.addTab("Wireless", makeWirelessPanel(new JPanel(gl)));
		tabs.addTab("Simulator", makeSimulatorPanel(new JPanel(gl)));
		tabs.addTab("GUI", makeGUIConfigPanel(new JPanel(gl)));
		tabs.addTab("DBG", makeDEBUGPanel(new JPanel(gl)));
        tabs.addTab("Client", MainFrame.clientConsole);
        tabs.addTab("Server", MainFrame.serverConsole);
        threadsView = new ThreadsView();
        tabs.addTab("Threads", threadsView.getScrollPane());

		
		/*
		synchronized(StartUpThread.monitorInit){
			if(StartUpThread.monitoredInited){
				q_MD.setSelected(Broadcasting_Probabilities._broadcast_queue_md);
				q_C.setSelected(Broadcasting_Probabilities._broadcast_queue_c);
				q_RA.setSelected(Broadcasting_Probabilities._broadcast_queue_ra);
				q_RE.setSelected(Broadcasting_Probabilities._broadcast_queue_re);
				q_BH.setSelected(Broadcasting_Probabilities._broadcast_queue_bh);
				q_BR.setSelected(Broadcasting_Probabilities._broadcast_queue_br);
			}
		}
		*/
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				file_chooser_address_container.setFileFilter(new DDAddressFilter());
				// file_chooser_updates_to_sign.setFileFilter(new UpdatesFilter());
				//filterUpdates.setSelectedFile(new File(userdir));
				try {
					if(DEBUG)System.out.println("ControlPane:<init>: set Dir = "+Application.USER_CURRENT_DIR);
					File userdir = new File(Application.USER_CURRENT_DIR);
					if(DEBUG)System.out.println("ControlPane:<init>: set Dir FILE = "+userdir);
					file_chooser_updates_to_sign.setCurrentDirectory(userdir);
				} catch(Exception e){if(_DEBUG)e.printStackTrace();}
			}
		});
	    
	    
		/*
		Dimension dim = this.getPreferredSize();
		System.out.println("Old preferred="+dim);
		dim.width = 600;
		this.setPreferredSize(dim);
		this.setMaximumSize(dim);
*/

	}
	void actionSignUpdates() {
		FileFilter crtFilter = new UpdatesFilter();
		int returnVal;
		String _filePath = ClientUpdates.getFilePath();
		try {
			file_chooser_updates_to_sign.setFileFilter(crtFilter);
			file_chooser_updates_to_sign.setName(__("Select data to sign, with Downloadables in:")+" "+_filePath);
			//file_chooser_updates_to_sign.setFileSelectionMode(JFileChooser.FILES_ONLY);
			file_chooser_updates_to_sign.setMultiSelectionEnabled(false);
			Util_GUI.cleanFileSelector(file_chooser_updates_to_sign);
			//filterUpdates.setSelectedFile(null);
			returnVal = file_chooser_updates_to_sign.showDialog(this,__("Open Input Updates File"));
		}
		finally {
			file_chooser_updates_to_sign.removeChoosableFileFilter(crtFilter);
		}
		
		if (returnVal != JFileChooser.APPROVE_OPTION)  return;
		File fileUpdates = file_chooser_updates_to_sign.getSelectedFile();
		
		if (! fileUpdates.exists()) {
			Application_GUI.warning(__("No file: "+fileUpdates), __("No such file"));
			return;
		}
		
		/**
		 * Now the updates file is loaded in fileUpdates
		 */
		
		crtFilter = new UpdatesFilterKey();
		try {
			file_chooser_updates_to_sign.setFileFilter(crtFilter);
			file_chooser_updates_to_sign.setName(__("Select Secret Trusted Key (created if new)"));
			//filterUpdates.setSelectedFile(null);
			Util_GUI.cleanFileSelector(file_chooser_updates_to_sign);
			returnVal = file_chooser_updates_to_sign.showDialog(this,__("Specify Trusted Secret Key File"));
		}
		finally {
			file_chooser_updates_to_sign.removeChoosableFileFilter(crtFilter);
		}
		
		if (returnVal != JFileChooser.APPROVE_OPTION)  return;
		File fileTrustedSK = file_chooser_updates_to_sign.getSelectedFile();
		if (! fileTrustedSK.exists()) {
			Application_GUI.warning(__("No Trusted file. Will create one RSA/SHA512:")+" "+fileTrustedSK+" "+__("with")+" "+DD.RSA_BITS_TRUSTED_FOR_UPDATES+" "+__("bits!"), __("Will create new trusted file!"));
			DD.createTrustedSKFile(fileTrustedSK);
			//return;
		}
		
		crtFilter = new UpdatesFilter();
		try {
			file_chooser_updates_to_sign.setFileFilter(crtFilter);
			file_chooser_updates_to_sign.setName(__("Select Output File Name"));
			//filterUpdates.setSelectedFile(null);
			Util_GUI.cleanFileSelector(file_chooser_updates_to_sign);
			returnVal = file_chooser_updates_to_sign.showDialog(this,__("Specify Output File"));
		}
		finally {
			file_chooser_updates_to_sign.removeChoosableFileFilter(crtFilter);
		}
		if (returnVal != JFileChooser.APPROVE_OPTION)  return;
		File fileOutput = file_chooser_updates_to_sign.getSelectedFile();
		if (fileOutput.exists()) {
			int n;
			n = JOptionPane.showConfirmDialog(this, __("Overwrite: ")+fileOutput+"?",
        			__("Overwrite?"), JOptionPane.YES_NO_OPTION,
        			JOptionPane.QUESTION_MESSAGE);
			if (n != 0) return;
		}
		
		
		if ("txt".equals(Util.getExtension(fileUpdates)) && fileUpdates.isFile()) {
			//FileInputStream fis;
			try {
				String filePath = ClientUpdates.getFilePath();
				if (! ClientUpdates.create_info(fileUpdates, fileTrustedSK, fileOutput, filePath))
					Application_GUI.warning(__("Bad key or input!"+fileUpdates.getCanonicalPath()+"\n"+fileTrustedSK.getCanonicalPath()), __("Bad key or input!"));
			} catch (IOException e) {
				e.printStackTrace();
				Application_GUI.warning(__("Bad key or input!"+fileUpdates.getAbsolutePath()+"\n"+fileTrustedSK.getAbsolutePath()+"\n"+e.getLocalizedMessage()), __("Bad key or input!"));
			}
		} else {
			Application_GUI.warning(__("Unsupported input file type extension:"+Util.getExtension(fileUpdates)), __("Unsupported file type"));
		}
	}
	
	public void setServerStatus(boolean run) throws NumberFormatException, P2PDDSQLException  {
		if(DEBUG)System.out.println("Setting server running status to: "+run);
		
		if(DD.GUI){
			if(EventQueue.isDispatchThread()) {
				if(run)startServer.setText(STARTING_SERVER);
				else startServer.setText(STOPPING_SERVER);
			}
			else{
				if(run)
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							startServer.setText(STARTING_SERVER);
						}
					});
				else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							startServer.setText(STOPPING_SERVER);
						}
					});
			}
		}
		if(run){
			if(DEBUG)System.err.println("Setting id: "+Application.getCurrent_Peer_ID());
			DD.startServer(true, Application.getCurrent_Peer_ID());
		}else{
			DD.startServer(false, null);
		}
		if(DEBUG)System.err.println("Done Setting: "+run);

		if(DD.GUI){
			if(EventQueue.isDispatchThread()) {
				if(Application.getG_TCPServer()!=null) startServer.setText(STOP_SERVER);
				else startServer.setText(START_SERVER);		
			}else{
				if(run)
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							if(Application.getG_TCPServer()!=null) startServer.setText(STOP_SERVER);
							else startServer.setText(START_SERVER);		
						}
					});
			}
		}
		if(DEBUG)System.err.println("Save Setting: "+Application.getG_TCPServer());

		DD.setAppText(DD.DD_DATA_SERVER_ON_START, (Application.getG_TCPServer()==null)?"0":"1");
	}
	public void setUServerStatus(boolean _run) throws NumberFormatException, P2PDDSQLException  {
		if(DEBUG)System.out.println("ControlPane:Setting userver running status to: "+_run);
		if(DD.GUI){
			if(EventQueue.isDispatchThread()) {
				if(_run)startUServer.setText(STARTING_USERVER);
				else startUServer.setText(STOPPING_USERVER);	
			}else{
				if(_run)
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							startUServer.setText(STARTING_USERVER);
						}
					});
				else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							startUServer.setText(STOPPING_USERVER);	
						}
					});
			}
		}
		if(DEBUG)System.out.println("ControlPanel: userver started");

		if(_run){
			if(DEBUG)System.err.println("Setting userver id: "+Application.getCurrent_Peer_ID());
			DD.startUServer(true, Application.getCurrent_Peer_ID());
		}else{
			DD.startUServer(false, null);
		}
		if(DEBUG)System.err.println("Done Setting: "+_run);
		if(DD.GUI) {
			if(EventQueue.isDispatchThread()) {
				if(Application.getG_UDPServer()!=null) startUServer.setText(STOP_USERVER);
				else startUServer.setText(START_USERVER);		
			}
			else
				EventQueue.invokeLater(new Runnable(){
					public void run(){
						if(Application.getG_UDPServer()!=null) startUServer.setText(STOP_USERVER);
						else startUServer.setText(START_USERVER);		
					}
				});
		}
		
		if(DEBUG)System.err.println("Save Setting: "+Application.getG_UDPServer());
		
		DD.setAppBoolean(DD.DD_DATA_USERVER_INACTIVE_ON_START, DD.DD_DATA_USERVER_ON_START_DEFAULT^(Application.getG_UDPServer()!=null));

		if(DEBUG)System.err.println("ControlPane:userv:done");

	}
	public void setNServerStatus(boolean _run) throws NumberFormatException, P2PDDSQLException  {
		if(DEBUG)System.out.println("ControlPane:Setting nserver running status to: "+_run);
		if(DD.GUI){
			if(EventQueue.isDispatchThread()) {
				if(_run)startNATServer.setText(STARTING_NSERVER);
				else startNATServer.setText(STOPPING_NSERVER);	
			}else{
				if(_run)
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							startNATServer.setText(STARTING_NSERVER);
						}
					});
				else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							startNATServer.setText(STOPPING_NSERVER);	
						}
					});
			}
		}
		if(DEBUG)System.out.println("ControlPanel: nserver started");

		if(_run){
			if(DEBUG)System.err.println("Setting nserver id: "+Application.getCurrent_Peer_ID());
			DD.startNATServer(true);
		}else{
			DD.startNATServer(false);
		}
		if(DEBUG)System.err.println("Done Setting: "+_run);
		if(DD.GUI) {
			if(EventQueue.isDispatchThread()) {
				if(Application.getG_NATServer()!=null) startNATServer.setText(STOP_NSERVER);
				else startNATServer.setText(START_NSERVER);		
			}
			else
				EventQueue.invokeLater(new Runnable(){
					public void run(){
						if(Application.getG_NATServer()!=null) startNATServer.setText(STOP_NSERVER);
						else startNATServer.setText(START_NSERVER);		
					}
				});
		}
		
		if(DEBUG)System.err.println("Save Setting: "+Application.getG_NATServer());
		
		DD.setAppBoolean(DD.DD_DATA_NSERVER_INACTIVE_ON_START, DD.DD_DATA_NSERVER_ON_START_DEFAULT^(Application.getG_NATServer()!=null));

		if(DEBUG)System.err.println("ControlPane:nserv:done");

	}
	public void setClientUpdatesStatus(boolean _run, boolean force) {
		boolean DEBUG = ControlPane.DEBUG || ClientUpdates.DEBUG;
		if (DEBUG) System.out.println("ControlPane:setClientUpdatesStatus: got servers status: updates="+_run);
		if (DD.GUI) {
			if(EventQueue.isDispatchThread()) {		
				if(_DEBUG) System.out.println("ControlPane:setClientUpdatesStatus: in dispatch: updates="+_run);
				if(_run)startClientUpdates.setText(STARTING_CLIENT_UPDATES);
				else startClientUpdates.setText(STOPPING_CLIENT_UPDATES);
			}
			else{
				if (_run) {
					EventQueue.invokeLater(new Runnable() {
						boolean DEBUG = ControlPane.DEBUG || ClientUpdates.DEBUG;
						public void run() {
							if(DEBUG) System.out.println("ControlPane:setClientUpdatesStatus: in dispatch: updates starting");
							startClientUpdates.setText(STARTING_CLIENT_UPDATES);
						}
					});
				}
				else {
					EventQueue.invokeLater(new Runnable() {
						boolean DEBUG = ControlPane.DEBUG || ClientUpdates.DEBUG;
						public void run() {
							if(DEBUG) System.out.println("ControlPane:setClientUpdatesStatus: in dispatch: updates stopping");
							startClientUpdates.setText(STOPPING_CLIENT_UPDATES);
						}
	
					});
				}
			}
		}
		if (_run) {
			if(DEBUG) System.out.println("ControlPane: setClientUpdatesStatus: here: updates stopping");
			ClientUpdates.startClient(true, force);
		} else {
			if (DEBUG) System.out.println("ControlPane: setClientUpdatesStatus: here: updates stopping");
			ClientUpdates.startClient(false, false);
		}
		
		//if(ClientUpdates.clientUpdates!=null) startClientUpdates.setText(STOP_CLIENT_UPDATES);
		//else startClientUpdates.setText(START_CLIENT_UPDATES);
		
		DD.setAppBoolean(DD.DD_DATA_CLIENT_UPDATES_INACTIVE_ON_START, DD.DD_DATA_CLIENT_UPDATES_ON_START_DEFAULT^(_run));	
	}

	public void setClientStatus(boolean _run) throws NumberFormatException, P2PDDSQLException {
		if(DD.GUI){
			if(EventQueue.isDispatchThread()) {		
				if(_run) startClient.setText(STARTING_CLIENT);
				else startClient.setText(STOPPING_CLIENT);
			}
			else{
				if(_run) {
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							startClient.setText(STARTING_CLIENT);
						}
					});
				}
				else{
					if(_run) {
						EventQueue.invokeLater(new Runnable(){
							public void run(){
								startClient.setText(STOPPING_CLIENT);
							}
						});
					}
				}
			}
		}
		if(_run)DD.startClient(true);
		else DD.startClient(false);

		if(DD.GUI){
			if(EventQueue.isDispatchThread()) {
				if(Application.getG_PollingStreamingClient()!=null) startClient.setText(STOP_CLIENT);
				else startClient.setText(START_CLIENT);
			}else{
				EventQueue.invokeLater(new Runnable() {
					public void run(){
						if(Application.getG_PollingStreamingClient()!=null) startClient.setText(STOP_CLIENT);
						else startClient.setText(START_CLIENT);
					}
				});
			}
		}
		DD.setAppBoolean(DD.DD_DATA_CLIENT_INACTIVE_ON_START, DD.DD_DATA_CLIENT_ON_START_DEFAULT^(Application.getG_PollingStreamingClient()!=null));	
	}
	public void setDirectoryStatus(boolean run) throws NumberFormatException, P2PDDSQLException {
				if(run) DD.startDirectoryServer(true, -1);
				else DD.startDirectoryServer(false, -1);
				
				if(DEBUG) System.out.println("ControlPanel:setDirStatus:Will run="+run);

				if(DD.GUI){
					if(EventQueue.isDispatchThread()) {
						if(Application.getG_DirectoryServer()!=null) startDirectoryServer.setText(STOP_DIR);
						else startDirectoryServer.setText(START_DIR);
					}
					else
						EventQueue.invokeLater(new Runnable() {
							public void run(){
								if(Application.getG_DirectoryServer()!=null) startDirectoryServer.setText(STOP_DIR);
								else startDirectoryServer.setText(START_DIR);
							}
						});
				}
				DD.setAppText(DD.DD_DIRECTORY_SERVER_ON_START, (Application.getG_DirectoryServer()==null)?"0":"1");		
				if(DEBUG) System.out.println("ControlPanel:setDirStatus:DS running="+Application.getG_DirectoryServer());
	}
	//@Override
	public void actionPerformed(ActionEvent e) {
		if(DEBUG)System.out.println("Action ="+e);
		try {
			if (action_button_clean_SMTP.equals(e.getActionCommand())) {
				net.ddp2p.java.email.EmailManager.setEmailPassword(Identity.current_id_branch, null);
				net.ddp2p.java.email.EmailManager.setEmailUsername(Identity.current_id_branch, null);
				net.ddp2p.java.email.EmailManager.setSMTPHost(Identity.current_id_branch, null);
			} else if(action_button_clean_SMTP_password.equals(e.getActionCommand())) {
				String val = Application_GUI.input(__("You may enter a new password (it is visible!!!)"),
						__("New password"), JOptionPane.QUESTION_MESSAGE);
				if (val == null) {
					Application_GUI.warning(__("Cleaning of password abandoned"), __("Canceled"));
					return;
				}
				if (val != null) {
					val = val.trim();
					if("".equals(val)) val = null;
				}
				net.ddp2p.java.email.EmailManager.setEmailPassword(Identity.current_id_branch, val);
			}else if (c_adh_IP.equals(e.getActionCommand())) {
				String old = DD.WIRELESS_ADHOC_DD_NET_IP_BASE;
				DD.WIRELESS_ADHOC_DD_NET_IP_BASE = Util.validateIPBase(this.m_area_ADHOC_IP.getText(), DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
				if(!Util.equalStrings_null_or_not(this.m_area_ADHOC_IP.getText(), DD.WIRELESS_ADHOC_DD_NET_IP_BASE))
					this.m_area_ADHOC_IP.setText(DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
				
				if(!Util.equalStrings_null_or_not(old, DD.WIRELESS_ADHOC_DD_NET_IP_BASE))
					DD.setAppText("WIRELESS_ADHOC_DD_NET_IP_BASE", DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
				
			}else if (c_adh_BIP.equals(e.getActionCommand())) {
				String old = DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP;
				DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP = Util.validateIP(this.m_area_ADHOC_BIP.getText(), DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP);
				if(!Util.equalStrings_null_or_not(this.m_area_ADHOC_BIP.getText(), DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP))
					this.m_area_ADHOC_BIP.setText(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP);
				
				if(!Util.equalStrings_null_or_not(old, DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP))
					DD.setAppText("WIRELESS_ADHOC_DD_NET_BROADCAST_IP", DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP);
				
			}else if (c_dd_SSID.equals(e.getActionCommand())) {
				String old = DD.DD_SSID;
				DD.DD_SSID = this.m_area_ADHOC_SSID.getText();
				if(!Util.equalStrings_null_or_not(this.m_area_ADHOC_SSID.getText(), DD.DD_SSID))
					this.m_area_ADHOC_SSID.setText(DD.DD_SSID);
				if(!Util.equalStrings_null_or_not(old, DD.DD_SSID))
					DD.setAppText("DD_SSID", DD.DD_SSID);
					
			}else if (c_adhoc_mask.equals(e.getActionCommand())) {
				String old = DD.WIRELESS_ADHOC_DD_NET_MASK;
				DD.WIRELESS_ADHOC_DD_NET_MASK = Util.validateIP(this.m_area_ADHOC_Mask.getText(), DD.WIRELESS_ADHOC_DD_NET_MASK);
				if(!Util.equalStrings_null_or_not(this.m_area_ADHOC_Mask.getText(), DD.WIRELESS_ADHOC_DD_NET_MASK))
					this.m_area_ADHOC_Mask.setText(DD.WIRELESS_ADHOC_DD_NET_MASK);
				if(!Util.equalStrings_null_or_not(old, DD.WIRELESS_ADHOC_DD_NET_MASK))
					DD.setAppText("WIRELESS_ADHOC_DD_NET_MASK", DD.WIRELESS_ADHOC_DD_NET_MASK);
				
			}else if (c_broadcastable.equals(e.getActionCommand())) {
				boolean val = !Identity.getAmIBroadcastable();
				this.toggleBroadcastable.setText(val?this.BROADCASTABLE_YES:this.BROADCASTABLE_NO);
				if(DEBUG)System.out.println("Broadcastable will be toggled to ="+val);
				HandlingMyself_Peer.setAmIBroadcastable(val);
			}else if (c_server.equals(e.getActionCommand())) {
				setServerStatus(Application.getG_TCPServer() == null);
			}else if (c_userver.equals(e.getActionCommand())) {
				setUServerStatus(Application.getG_UDPServer() == null);
			}else if (c_nserver.equals(e.getActionCommand())) {
				setNServerStatus(Application.getG_NATServer() == null);
			}else if (c_broadcast_server.equals(e.getActionCommand())) {
				if(DEBUG)System.out.println("Action Broadcast Server");
				DD.setBroadcastServerStatus(Application.getG_BroadcastServer() == null);
			}else if (c_broadcast_client.equals(e.getActionCommand())) {
				if(DEBUG)System.out.println("Action Broadcast Client");
				DD.setBroadcastClientStatus(Application.getG_BroadcastClient() == null);
			}else if (c_simulator.equals(e.getActionCommand())) {
				if(DEBUG)System.out.println("Action Simulator");
				DD.setSimulatorStatus(Application.g_Simulator == null);
			}else if (c_keepThisVersion.equals(e.getActionCommand())) {
				try {
					ClientUpdates.fixLATEST();
				} catch (IOException e1) {
					e1.printStackTrace();
					Application_GUI.warning(__("Keep version failes with: "+e1.getLocalizedMessage()),
							__("Failed Keep Version"));
				}
			}else if (c_updates_client.equals(e.getActionCommand())) {
				boolean _run = (ClientUpdates.clientUpdates == null);
				if (DEBUG || ClientUpdates.DEBUG) System.out.println("ControlPane: actionPerformed: update_client object _run = "+_run);
				setClientUpdatesStatus(_run, true);
			}else if (c_updates_clean.equals(e.getActionCommand())) {
				if (DEBUG || ClientUpdates.DEBUG) System.out.println("ControlPane: actionPerformed: update_clean");
				clientUpdatesCleanDownload();
			}else if (c_client.equals(e.getActionCommand())) {
				setClientStatus(Application.getG_PollingStreamingClient() == null);
			}else if (c_t_client.equals(e.getActionCommand())) {
				DD.touchClient();
			}else if (c_directory.equals(e.getActionCommand())) {
				setDirectoryStatus(Application.getG_DirectoryServer() == null);
			}else if (c_peerID.equals(e.getActionCommand())) {
				//MyselfHandling.createMyPeerID();
				String name = HandlingMyself_Peer.getMyPeerName(); //Identity.current_peer_ID.name;
				String slogan = HandlingMyself_Peer.getMyPeerSlogan(); //Identity.current_peer_ID.slogan;
				//createMyPeerID(new PeerInput(name, slogan, Identity.emails, null));
				PeerInput pi = new PeerInput();
				pi.email = HandlingMyself_Peer.getMyPeerEmail();//Identity.getAgentWideEmails();
				pi.name = name;
				pi.slogan = slogan;
				D_Peer me = HandlingMyself_Peer.createMyselfPeer_w_Addresses(pi, true);
				
			} else if (c_peerName.equals(e.getActionCommand())) {
				ControlPane.changeMyPeerName(this);
			} else if (c_peerSlogan.equals(e.getActionCommand())) {
				ControlPane.changeMyPeerSlogan(this);
			} else if (c_convert.equals(e.getActionCommand())) {
				try {GIF_Convert.main(null);} catch(Exception eee){}
			} else if (c_importText.equals(e.getActionCommand())) {
				try {
					String textObj = importedText.getDocument().getText(0, importedText.getDocument().getLength()); 
					importedText.getDocument().remove(0, importedText.getDocument().getLength());
					DD.importText(textObj);} catch(Exception eee) {eee.printStackTrace();}
			} else if (c_exportText.equals(e.getActionCommand())) {
				try {
					DD_Address myAddress;
					try {
						myAddress = HandlingMyself_Peer.getMyDDAddress();
						//System.out.println("ControlPane: actionExportPeerAddress: "+myAddress);
					} catch (P2PDDSQLException ex2) {
						ex2.printStackTrace();
						return;
					}
					String exportText = DD.getExportTextObjectTitle(myAddress) + "\n"+ DD.getExportTextObjectBody(myAddress.getBytes());
					//System.out.println("\n\nExporting peer object=\n"+exportText+"\n");
					net.ddp2p.widgets.components.XUtil.clipboardCopy(exportText);
				} catch (Exception ex) {ex.printStackTrace();}
			} else if (c_linuxScriptsPath.equals(e.getActionCommand())) {
				if (DEBUG) System.out.println("ControlPane: scripts: "+Application.getCurrent_Peer_ID().getPeerGID());
				String SEP = ",";
				String previous = Application.getCurrentLinuxPathsString(SEP);
				System.out.println("Previous linux path: "+previous);
				String _previous = Application.getCurrentLinuxPathsString(SEP+"\n ");
				String val = JOptionPane.showInputDialog(this,
						__("Change Linux Installation Path.")+"\n"+
								__("V=INSTALLATION_VERSION (no default)")+"\n"+
								__("R=INSTALLATION_ROOT (default: parent of version)")+"\n"+
								__("S=INSTALLATION_SCRIPTS")+"\n"+
								__("P=INSTALLATION_PLUGINS")+"\n"+
								__("D=INSTALLATION_DATABASE")+"\n"+
								__("L=INSTALLATION_LOGS")+"\n"+
								__("J=INSTALLATION_JARS")+"\n"+
								__("Previously: ")+"\n "+
						//Application.LINUX_INSTALLATION_VERSION_BASE_DIR
						_previous
						,
						__("Linux Installation"),
						JOptionPane.QUESTION_MESSAGE);
				if ((val != null) /*&&(!"".equals(val))*/ ) {
					Application.parseLinuxPaths(val);
					if (DD.OS == DD.LINUX) {
						Application.switchToLinuxPaths();
					}
					if (DD.OS == DD.MAC) {
						Application.switchToMacOSPaths();
					}
					if (_DEBUG) System.out.println("ControlPane: Application.LINUX_INSTALLATION_DIR ="+ Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
				}
			} else if ("windowsScriptsPath".equals(e.getActionCommand())) {
				if (DEBUG) System.out.println("ControlPane: scripts Win: "+Application.getCurrent_Peer_ID().getPeerGID());
				String SEP = ",";
				String previous = Application.getCurrentWindowsPathsString();
				System.out.println("Previous windows path: "+previous);
				String _previous = Application.getCurrentWindowsPathsString(SEP+"\n ");
				String val=JOptionPane.showInputDialog(this,
						
						__("Change Windows Installation Path, using ':' for default based on INSTALLATION_VERSION.")+"\n"+
								__("V=INSTALLATION_VERSION (no default)")+"\n"+
								__("R=INSTALLATION_ROOT (default: parent of version)")+"\n"+
								__("S=INSTALLATION_SCRIPTS")+"\n"+
								__("P=INSTALLATION_PLUGINS")+"\n"+
								__("D=INSTALLATION_DATABASE")+"\n"+
								__("L=INSTALLATION_LOGS")+"\n"+
								__("J=INSTALLATION_JARS")+"\n"+
						__("Previously:")+"\n "+
						_previous
						,
						__("Windows Installation"),
						JOptionPane.QUESTION_MESSAGE);
				if((val!=null)/*&&(!"".equals(val))*/){
					Application.parseWindowsPaths(val);
					if(DD.OS == DD.WINDOWS){
						Application.switchToWindowsPaths();
					}
				}
			}else if (exportDirectories_action.equals(e.getActionCommand())) {
				DD_DirectoryServer ds = new DD_DirectoryServer();
				/*
				String listing_directories = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
				if(_DEBUG)System.out.println("export directories: "+listing_directories);
				ds.parseAddress(listing_directories);
				*/
				
				ds.parseAddress(DirectoryAddress.getDirectoryAddresses());
				net.ddp2p.widgets.app.ControlPane.actionExport(file_chooser_address_container, this, ds, null);
				
				String exportText = DD.getExportTextObjectTitle(ds) + "\n"+ DD.getExportTextObjectBody(ds.getBytes());
				//System.out.println("\n\nExporting peer object=\n"+exportText+"\n");
				net.ddp2p.widgets.components.XUtil.clipboardCopy(exportText);
			    
			}else if(setListingDirectories_action.equals(e.getActionCommand())){
				String listing_directories = //DD.getAppText(DD.APP_LISTING_DIRECTORIES);
						DirectoryAddress.getDirectoryAddressesStr();
				if(_DEBUG)System.out.println("listing directories: "+listing_directories);
				if (listing_directories != null)
					listing_directories = Util.concat(listing_directories.split(Pattern.quote(",")),"\n ");
				String val=
					JOptionPane.showInputDialog(
							this,
							__("Change Address of Directories Listing Your Address in form")+"\n"+
							" IP1:port1,IP2:port2,....\n"
							+("Previously:")+"\n "+listing_directories,
							__("Listing Directories"), JOptionPane.QUESTION_MESSAGE);
				if((val!=null)&&(!"".equals(val))&&(DD.test_proper_directory(val))) {
					//DD.setAppText(DD.APP_LISTING_DIRECTORIES, val);
					//String listing_directories = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
					DirectoryAddress.reset(val);
					DD.load_listing_directories();
//					if(Application.as!=null) {
//						Identity peer_ID = new Identity();
//						peer_ID.globalID = Identity.current_peer_ID.globalID;
//						peer_ID.instance = Identity.current_peer_ID.instance;
//						peer_ID.name = Identity.current_peer_ID.name;
//						peer_ID.slogan = Identity.current_peer_ID.slogan;
//						MyselfHandling.set_my_peer_ID_TCP(peer_ID);
//					}
					UDPServer.announceMyselfToDirectories();
				}
			}else if("updateServers".equals(e.getActionCommand())){
				String update_directories = DD.getAppText(DD.APP_UPDATES_SERVERS);
				if(_DEBUG)System.out.println("update directories: "+update_directories);
				if(update_directories!=null) {
					update_directories = Util.concat(update_directories.split(Pattern.quote(DD.APP_UPDATES_SERVERS_URL_SEP)),DD.APP_UPDATES_SERVERS_URL_SEP+"\n");
				}
				String val=JOptionPane.showInputDialog(this,
						__("Change Address of Update Mirrors")+"\n URL1"+
						DD.APP_UPDATES_SERVERS_URL_SEP+
						" URL2"+
						DD.APP_UPDATES_SERVERS_URL_SEP+
						" URL3....\n"+__("Previously:")+"\n "
						+update_directories,
						__("Update Servers"), JOptionPane.QUESTION_MESSAGE);
				if((val!=null)&&(!"".equals(val))){
					DD.setAppText(DD.APP_UPDATES_SERVERS, val);
					if(ClientUpdates.clientUpdates!=null) {
						ClientUpdates.clientUpdates.initializeServers(val);
					}
				}
			}else if("broadcastingQueueProbabilities".equals(e.getActionCommand())){
				String broadcastingQueueProbabilities = DD.getAppText(DD.BROADCASTING_QUEUE_PROBABILITIES);
				broadcastingQueueProbabilities = net.ddp2p.common.wireless.Broadcasting_Probabilities.getCurrentQueueProbabilitiesListAsString(broadcastingQueueProbabilities);
				if(_DEBUG)System.out.println("broadcasting  queue probabilities: "+broadcastingQueueProbabilities);
				String val=JOptionPane.showInputDialog(this,
						__("Change Broadcast QUEUES Unnormalized Probability in form")+"\n "+
								Broadcasting_Probabilities.PROB_Q_MD+Broadcasting_Probabilities.SEP+
								Broadcasting_Probabilities.PROB_Q_C+Broadcasting_Probabilities.SEP+
								Broadcasting_Probabilities.PROB_Q_RA+Broadcasting_Probabilities.SEP+
								Broadcasting_Probabilities.PROB_Q_RE+Broadcasting_Probabilities.SEP+
								Broadcasting_Probabilities.PROB_Q_BH+Broadcasting_Probabilities.SEP+
								Broadcasting_Probabilities.PROB_Q_BR+
								"\n"+("Previously:")+" "+broadcastingQueueProbabilities,
						__("Broadcasting Probabilities"), JOptionPane.QUESTION_MESSAGE);
				if((val!=null)&&(!"".equals(val))) {
					DD.setAppText(DD.BROADCASTING_QUEUE_PROBABILITIES, val);
					Broadcasting_Probabilities.load_broadcast_queue_probabilities(val);
				}
			}else if("broadcastingProbabilities".equals(e.getActionCommand())){
				String broadcastingProbabilities = DD.getAppText(DD.BROADCASTING_PROBABILITIES);
				broadcastingProbabilities = net.ddp2p.common.wireless.Broadcasting_Probabilities.getCurrentProbabilitiesListAsString(broadcastingProbabilities);
				if(_DEBUG)System.out.println("broadcasting probabilities: "+broadcastingProbabilities);
				String val=JOptionPane.showInputDialog(this,
						__("Change Broadcast Unnormalized Probability in the form")+"\n "+
						DD.PROB_CONSTITUENTS+DD.PROB_KEY_SEP+__("constituents_prob")+
						DD.PROB_SEP+DD.PROB_ORGANIZATIONS+DD.PROB_KEY_SEP+__("organizations_probs")+
						",M:motion,W:witness,N:neighborhood,V:votes,P:peers\n"+
						__("Previously:")+"\n "+
						broadcastingProbabilities,
						__("Broadcasting Probabilities"), JOptionPane.QUESTION_MESSAGE);
				if((val!=null)&&(!"".equals(val))){
					DD.setAppText(DD.BROADCASTING_PROBABILITIES, val);
					DD.load_broadcast_probabilities(val);
				}
			}else if("generationProbabilities".equals(e.getActionCommand())){
				String generationProbabilities = DD.getAppText(DD.GENERATION_PROBABILITIES);
				generationProbabilities = net.ddp2p.common.simulator.SimulationParameters.getCurrentProbabilitiesListAsString(generationProbabilities);
				if(_DEBUG)System.out.println("generation probabilities: "+generationProbabilities);
				String val=JOptionPane.showInputDialog(this,
						__("Change Generation Unnormalized Probability in form")+"\n "+
						//DD.PROB_CONSTITUENTS+DD.PROB_KEY_SEP+"const"+DD.PROB_SEP+DD.PROB_ORGANIZATIONS+DD.PROB_KEY_SEP+"org,M:motion,W:witn,N:neigh,V:votes,P:peers\n"+
						DD.PROB_CONSTITUENTS+DD.PROB_KEY_SEP+__("constituents_prob")+
						DD.PROB_SEP+DD.PROB_ORGANIZATIONS+DD.PROB_KEY_SEP+__("organizations_probs")+
						",M:motion,W:witness,N:neighborhood,V:votes,P:peers\n"+
						__("Previously:")+"\n "+generationProbabilities,
						__("Generation Probabilities"), JOptionPane.QUESTION_MESSAGE);
				if((val!=null)&&(!"".equals(val))){
					DD.setAppText(DD.GENERATION_PROBABILITIES, val);
					DD.load_generation_probabilities(val);
				}
			}
			//else if ("tab_peer".equals(e.getActionCommand())) {DD.addTabPeer();}
			else if ("signUpdates".equals(e.getActionCommand())) {
				actionSignUpdates();
			}else if ("export".equals(e.getActionCommand())) {
				actionExportPeerAddress(file_chooser_address_container, JFrameDropCatch.mframe);
			}else if ("import".equals(e.getActionCommand())) {
				StegoStructure d[] = DD.getAvailableStegoStructureInstances();
				int[] selected = new int[1];
				ControlPane.actionImport(ControlPane.file_chooser_address_container, JFrameDropCatch.mframe, d, selected);
			}else if (action_textfield_SMTPHost.equals(e.getActionCommand())) {
				String text = this.textfield_SMTPHost.getText();
				net.ddp2p.java.email.EmailManager.setSMTPHost(Identity.current_id_branch, text);
				if(DEBUG)System.out.println("ControlPanel: Now Host is: "+text);
			}else if (action_textfield_EmailUsername.equals(e.getActionCommand())) {
				String text = this.textfield_EmailUsername.getText();
				net.ddp2p.java.email.EmailManager.setEmailUsername(Identity.current_id_branch, text);
				if(DEBUG)System.out.println("ControlPanel: Now Username is: "+text);
			}else if (action_natBorer.equals(e.getActionCommand())) {
				String text = natBorer.getText();
				try{Server.TIMEOUT_UDP_NAT_BORER = Integer.parseInt(text);}catch(Exception a){}
				if(DEBUG)System.out.println("Now NAT Borrer is: "+Server.TIMEOUT_UDP_NAT_BORER);
			}else if (action_clientPause.equals(e.getActionCommand())) {
				String text = clientPause.getText();
				try{ClientSync.PAUSE = Integer.parseInt(text);}catch(Exception a){}
				if(DEBUG)System.out.println("Now Pull Interval is: "+ClientSync.PAUSE);
			}
			
		} catch (NumberFormatException e1) {
				e1.printStackTrace();
		} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
		} catch (UnknownHostException e3) {
			e3.printStackTrace();
		}
	}
	public void clientUpdatesCleanDownload() {
		DD.setAppTextNoException(DD.LATEST_DD_VERSION_DOWNLOADED, null);
		String install_root_path_with_sep = ClientUpdates.getFileInstallRootPathWithSeparator();
		//ClientUpdates.ensure_PREVIOUS(install_root_path_with_sep);
		String result = Application_GUI.input(__("What should be the version to boot?\nDefault: "+DD.VERSION), __("Version to boot"), Application_GUI.QUESTION_MESSAGE);
		if (result == null || result.trim().length() == 0)
			result = DD.VERSION;
		
		String newDirName = install_root_path_with_sep+result;
		if(DEBUG)System.out.println("ControlPane: clientUpdatesCleanDownload: newDirName="+newDirName);
		File newDirFile = new File(newDirName);
		if (! newDirFile.exists()) {
			//create
			//newDirFile.mkdirs();
			if(DEBUG)System.out.println("ControlPane: clientUpdatesCleanDownload: new Dir created ");
			Application_GUI.warning(__("Version not present:")+"\n"+newDirName, "Failure to set new version");
			return;
		} else {
			if (! newDirFile.isDirectory()) {
				Application_GUI.warning(__("Version not a directory:"+"\n"+newDirName), __("Path busy for selected release"));
				if(DEBUG)System.out.println("ControlPane: clientUpdatesCleanDownload: new Dir not directory ");
				return;
			}
		}

		
		ClientUpdates.prepareLatest(install_root_path_with_sep, DD.VERSION);
	}
	/**
	 * Export the peer addresses  (IPs)
	 * @param fc
	 * @param parent
	 */
	static void actionExportPeerAddress(JFileChooser fc, Component parent){
		//boolean DEBUG = true;
		DD_Address myAddress;
		try {
			myAddress = HandlingMyself_Peer.getMyDDAddress();
			//System.out.println("ControlPane: actionExportPeerAddress: "+myAddress);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		String exportText = DD.getExportTextObjectTitle(myAddress) + "\n"+ DD.getExportTextObjectBody(myAddress.getBytes());
		//System.out.println("\n\nExporting peer object=\n"+exportText+"\n");
		net.ddp2p.widgets.components.XUtil.clipboardCopy(exportText);
	    
		DD_Address  x = new DD_Address();
		ControlPane.actionExport(fc, parent, myAddress, x);
	}
	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		if(itemStateChangedDBG(e, source)) return;
		if(itemStateChangedCOMM(e,source)) return;
		if(itemStateChangedWireless(e, source)) return;
		
		if(source.equals(serveDirectly)) {
			boolean direct = this.serveDirectly.isSelected();
			DD.serveDataDirectly(direct);
			DD.setAppBoolean(DD.SERVE_DIRECTLY, direct);
		}
	    if (source == tcpButton) {
	    	DD.ClientTCP = (e.getStateChange() == ItemEvent.SELECTED);
	    	try {
				DD.setAppText(DD.APP_ClientTCP, Util.bool2StringInt(DD.ClientTCP));
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
	    } else if (source == udpButton) {
	    	DD.ClientUDP = (e.getStateChange() == ItemEvent.SELECTED);
	    	try {
	    		if(DD.ClientUDP) MainFrame.controlPane.setUServerStatus(true);
				DD.setAppText(DD.APP_NON_ClientUDP, Util.bool2StringInt(DD.ClientUDP));
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
	    	
	    } else if (source == natButton) {
	    	DD.ClientNAT = (e.getStateChange() == ItemEvent.SELECTED);
	    	try {
	    		if(DD.ClientNAT) MainFrame.controlPane.setNServerStatus(true);
				DD.setAppText(DD.APP_NON_ClientNAT, Util.bool2StringInt(DD.ClientNAT));
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
	    } if (source == this.m_const_orphans_show_with_neighbors) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.CONSTITUENTS_ORPHANS_SHOWN_BESIDES_NEIGHBORHOODS = val;
	    } if (source == this.m_const_orphans_show_in_root) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.CONSTITUENTS_ORPHANS_SHOWN_IN_ROOT = val;
	    } if (source == this.m_const_orphans_filter_by_org) {
	    	boolean val = (e.getStateChange() == ItemEvent.SELECTED);
	    	DD.CONSTITUENTS_ORPHANS_FILTER_BY_ORG = val;
	    }

	    //if (e.getStateChange() == ItemEvent.DESELECTED);
	}
	/**
	 * Called by user (e.g. ControlPane button)
	 * @param fc
	 * @param parent : for dialogs
	 * @param adr : obtained with StegoStructure d[] = DD.getAvailableStegoStructureInstances();
	 * @param selected : new int[1] (to tell the result)
	 * @throws P2PDDSQLException
	 */
	
	public static void actionImport(JFileChooser fc, Component parent, StegoStructure[] adr, int[] selected) throws P2PDDSQLException{
		if(DEBUG)System.err.println("ControlPane:actionImport: import file");
		int returnVal = file_chooser_address_container.showOpenDialog(parent);
		if(DEBUG)System.err.println("ControlPane:actionImport: Got: selected");
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	if(DEBUG)System.err.println("ControlPane:actionImport: Got: ok");
	        File file = fc.getSelectedFile();
	        if(!file.exists()){
	        	Application_GUI.warning(__("The file does not exists: "+file),__("Importing Address")); return;
	        }
	        try {
	        	if("txt".equals(Util.getExtension(file))) {
	        		if(DEBUG)System.err.println("ControlPane:actionImport: Got: txt");
					String content = new Scanner(file).useDelimiter("\\Z").next();
					int _selected = -1;
					for(int k=0; k<adr.length; k++) {
						if(adr[k].parseAddress(content)){
							_selected = k;
							break;
						}
					}
					if(_selected == -1) {
						Application_GUI.warning(__("Failed to parse text file: "+file), __("Failed to parse address!"));
						return;
					}
					if ((selected != null) && (selected.length > 0)) selected[0] = _selected;
				} else if ("gif".equals(Util.getExtension(file))) {
						if (DEBUG) System.err.println("ControlPane:actionImport: Got: gif");
				    	String err = DD.loadGIF(file, adr, selected);
				    	if (err != null) {
							Application_GUI.warning(__("Error parsing file:")+file+"\n"+err, __("Failed to parse GIF!"));
				    	}
				}
				else
				if("ddb".equals(Util.getExtension(file))){
					if(DEBUG)System.err.println("ControlPane:actionImport: Got: ddb");
					FileInputStream fis=new FileInputStream(file);
					byte[] b = new byte[(int) file.length()];  
					fis.read(b);
					fis.close();
					int _selected = -1;
					for(int k=0; k<adr.length; k++) {
						try {
							adr[k].setBytes(b);
							_selected = k;
							break;
						} catch (ASN1DecoderFail e1) {
							if(EmbedInMedia.DEBUG){
								e1.printStackTrace();
								Application_GUI.warning(__("Failed to parse ddb file: "+file+"\n"+e1.getMessage()), __("Failed to parse address!"));
							}
						}
					}
					if(_selected == -1){
						Application_GUI.warning(__("Failed to parse _ddb file: "+file+"\n"), __("Failed to parse address!"));
						return;							
					}
					if((selected!=null)&&(selected.length>0)) selected[0] = _selected;
				}else
					if ("bmp".equals(Util.getExtension(file))) {
						//System.err.println("Got: bmp");
						String explain = DD.loadBMP(file, adr, selected);
						if (DEBUG) System.err.println("ControlPanel: actionImport: Saving expl=\""+explain+"\"");
						boolean fail = explain != null; //false;
						if (DEBUG) System.err.println("ControlPanel: actionImport: Saving fail="+fail);
						/*
						FileInputStream fis=new FileInputStream(file);
						//System.err.println("Got: open");
						//System.err.println("Got: open size:"+file.length());
						byte[] b = new byte[(int) file.length()];
						//System.err.println("Got: alloc="+b.length);
						fis.read(b);
						//System.err.println("Got: read");
						fis.close();
						//System.err.println("Got: close");
						//System.out.println("File data: "+Util.byteToHex(b,0,200," "));
						BMP data = new BMP(b, 0);
						//System.out.println("BMP Header: "+data);
	
						if((data.compression!=BMP.BI_RGB) || (data.bpp<24)){
							explain = " - "+__("Not supported compression: "+data.compression+" "+data.bpp);
							fail = true;
						}else{
							int offset = data.startdata;
							int word_bytes=1;
							int bits = 4;
							try {
								//System.err.println("Got: steg");
								////adr.setSteganoBytes(b, offset, word_bytes, bits,data.creator);
								EmbedInMedia.setSteganoBytes(adr, selected, b, offset, word_bytes, bits);
							} catch (ASN1DecoderFail e1) {
								explain = " - "+ __("No valid data in picture!");
								fail = true;
							}
						}
						*/
						if (fail) {
								JOptionPane.showMessageDialog(parent,
									__("Cannot Extract address in: ")+file+explain,
									__("Inappropriate File"), JOptionPane.WARNING_MESSAGE);
								return;
						}
					}
	        	if (selected[0] != -1) {
	        		// boolean DEBUG = true;
					if (DEBUG) System.err.println("ControlPanel: actionImport: Got Data: "+adr[selected[0]]);
		        	Application_GUI.warning(adr[selected[0]].getNiceDescription(), __("Obtained Data (CP)"));
		        	//adr[selected[0]].save();
					new net.ddp2p.common.util.DDP2P_ServiceThread("Stego Saver Import", false, adr[selected[0]]) {
						// boolean DEBUG = true;
						public void _run() {
							try {
								if (DEBUG) System.err.println("ControlPanel: actionImport: Saving");
								((StegoStructure)ctx).save();
							} catch (P2PDDSQLException e) {
								e.printStackTrace();
							}
						}
					}.start();
	        	}
	        }catch(Exception e3){
	        	e3.printStackTrace();
	        }
	    }		
	}
	/**
	 * Exporting current Address
	 * use test just to verify parsing (can be null)
	 * @param fc
	 * @param parent
	 * @param myAddress
	 * @param test
	 */
	public static void actionExport(JFileChooser fc, Component parent,
			StegoStructure myAddress, StegoStructure test) {
		
		if (EmbedInMedia.DEBUG)System.out.println("EmbedInMedia: actionExport:"+myAddress);
		if (myAddress == null) {
			if(DEBUG) System.out.println("EmbedInMedia: actionExport: no address");
			return;
		}
		if (DEBUG) System.out.println("EmbedInMedia: actionExport: Got to write: "+myAddress);
		BMP[] _data = new BMP[1];
		byte[][] _buffer_original_data = new byte[1][]; // old .bmp file 
		byte[] adr_bytes = myAddress.getBytes();
		if (test != null) {
			try {
				test.setBytes(adr_bytes);
			} catch (ASN1DecoderFail e1) {
								e1.printStackTrace();
								Application_GUI.warning(__("Failed to parse exported file: \n"+e1.getMessage()), __("Failed to parse address!"));
								return;
							}
		}
		if (DEBUG) System.out.println("EmbedInMedia:actionExport: Got bytes("+adr_bytes.length+"): to write: "+Util.byteToHex(adr_bytes, " "));
		int returnVal = fc.showSaveDialog(parent);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	fc.setName(__("Select file with image or text containing address"));
	        File file = fc.getSelectedFile();
	        String extension = Util.getExtension(file);
	        if ( ! "bmp".equals(extension) && ! "txt".equals(extension) && ! "ddb".equals(extension) && ! "gif".equals(extension))  {
	        	file = new File(file.getPath()+".bmp");
	        	extension = "bmp";
	        }
	
	       if (file.exists()) {
	        	//Application.warning(_("File exists!"));
	        	
	        	JOptionPane optionPane = new JOptionPane(__("Overwrite/Embed in: ")+file+"?",
	        			JOptionPane.QUESTION_MESSAGE,
	        			JOptionPane.YES_NO_OPTION);
	        	int n;
	        	if ("gif".equals(extension) && file.isFile()) {
	        		try{
	        			FileOutputStream fos = new FileOutputStream(file, true);
	        			fos.write(adr_bytes);
	        			fos.close();
	        		} catch(FileNotFoundException ex) {
					    System.out.println("EmbedInMedia:actionExport: FileNotFoundException : " + ex);
					} catch(IOException ioe){
					    System.out.println("EmbedInMedia:actionExport: IOException : " + ioe);
					}
	        	}
	        	if ("bmp".equals(extension) && file.isFile()) {
					//FileInputStream fis;
					boolean fail= false;
					String _explain[]=new String[]{""};
					fail = EmbedInMedia.cannotEmbedInBMPFile(file, adr_bytes, _explain, _buffer_original_data, _data);
					if(fail)
						JOptionPane.showMessageDialog(parent,
							__("Cannot Embed address in: ")+file+" - "+_explain,
							__("Inappropriate File"), JOptionPane.WARNING_MESSAGE);
							
	        		n = JOptionPane.showConfirmDialog(parent, __("Embed address in: ")+file+"?",
	            			__("Overwrite prior details?"), JOptionPane.YES_NO_OPTION,
	            			JOptionPane.QUESTION_MESSAGE);
	        	}else{
	        		n = JOptionPane.showConfirmDialog(parent, __("Overwrite: ")+file+"?",
	        			__("Overwrite?"), JOptionPane.YES_NO_OPTION,
	        			JOptionPane.QUESTION_MESSAGE);
	        	}
	        	if (n != JOptionPane.YES_OPTION)
	        		return;
	        	//Application.warning(_("File exists!"));
	        }
	        try {
				if("txt".equals(extension)) {
					BufferedWriter out = new BufferedWriter(new FileWriter(file));
					out.write(myAddress.getString());
					out.close();
				}else
				if("ddb".equals(extension)){
					FileOutputStream fo=new FileOutputStream(file);
					fo.write(myAddress.getBytes());
					fo.close();
				}else
					if("bmp".equals(extension)){
						if ( EmbedInMedia.DEBUG ) System.out.println("EmbedInMedia:actionExport:bmp");
						if ( ! file.exists()) {
							EmbedInMedia.saveSteganoBMP(file, adr_bytes, myAddress.getSignShort()); //DD.STEGO_SIGN_PEER);
						} else {
							FileOutputStream fo=new FileOutputStream(file);
							int offset = _data[0].startdata;
							int word_bytes=1;
							int bits = 4;
							////Util.copyBytes(b, BMP.CREATOR, adr_bytes.length);
							fo.write(EmbedInMedia.getSteganoBytes(adr_bytes, _buffer_original_data[0], offset, word_bytes, bits, myAddress.getSignShort()));
							fo.close();
						}
					}
			} catch (IOException e1) {
				Application_GUI.warning(__("Error writing file:")+file+" - "+ e1,__("Export Address"));
			}
	    }		
	}
	public static void changeMyPeerSlogan(Component win) throws P2PDDSQLException {
		if(Application.getCurrent_Peer_ID().getPeerGID()==null){
			JOptionPane.showMessageDialog(win,
					__("You are not yet a peer.\n Start your server first!"),
					__("Peer Init"), Application_GUI.WARNING_MESSAGE);
			return;
		}
		if (HandlingMyself_Peer.DEBUG) System.out.println("HandlingMyself_Peer:chgSlogan:peer_ID: "+Application.getCurrent_Peer_ID().getPeerGID());
		String peer_Slogan = HandlingMyself_Peer.getMyPeerSlogan();//Identity.current_peer_ID.slogan;
		String val = JOptionPane.showInputDialog(win, __("Change Peer Slogan.\nPreviously: ")+peer_Slogan, __("Peer Slogan"), Application_GUI.QUESTION_MESSAGE);
		if ((val != null) && (!"".equals(val))) {
			D_Peer me = HandlingMyself_Peer.get_myself_with_wait();
			
			//Identity.current_peer_ID.slogan = val;
			//DD.setAppText(DD.APP_my_peer_slogan, val);
			
			me = D_Peer.getPeerByPeer_Keep(me);
			me.setSlogan(val);
			me.setCreationDate();
			me.sign();
			me.dirty_main = true;
			me.setArrivalDate();
			me.storeRequest();
			me.releaseReference();
			DD.touchClient();
			if(D_Peer.DEBUG) System.out.println("D_Peer:changeMyPeerSlodan:Saving:"+me);
		}
	}
	public static String queryEmails (Component win) throws P2PDDSQLException{
		String peer_Emails = HandlingMyself_Peer.get_myself_with_wait().getEmail();
		String val=JOptionPane.showInputDialog(win, __("Change Peer Email.\nPreviously: ")+
				peer_Emails,
				__("Peer Emails"), Application_GUI.QUESTION_MESSAGE);
		return val;
	}
	public static void changeMyPeerEmails(Component win) throws P2PDDSQLException {
		if(Application.getCurrent_Peer_ID().getPeerGID()==null){
			JOptionPane.showMessageDialog(win,
					__("You are not yet a peer.\n Start your server first!"),
					__("Peer Init"), Application_GUI.WARNING_MESSAGE);
			return;
		}
		if(D_Peer.DEBUG)System.out.println("peer_ID: "+Application.getCurrent_Peer_ID().getPeerGID());
		//String peer_Slogan = Identity.current_peer_ID.slogan;
		String val = ControlPane.queryEmails(win);
		if ((val!=null) && (!"".equals(val))) {
			D_Peer me = HandlingMyself_Peer.get_myself_with_wait();
			
			Identity.setAgentWideEmails(val);
			
			me = D_Peer.getPeerByPeer_Keep(me);
			me.setEmail(val);
			me.setCreationDate();
			me.sign();
			me.dirty_main = true;
			me.setArrivalDate();
			me.storeRequest();
			me.releaseReference();
			DD.touchClient();
			if(D_Peer.DEBUG) System.out.println("D_Peer:changeMyPeerEmails:Saving:"+me);
		}
	}
	public static String queryName(Component win) {
		String peer_Name = HandlingMyself_Peer.getMyPeerName(); //Identity.current_peer_ID.name;
		String val=Application_GUI.input((peer_Name != null)?
				(__("Change Peer Name.")+"\n"+__("Previously:")+" "+peer_Name):
				(__("Dear:")+" "+System.getProperty("user.name")+"\n"+__("Select a Peer Name recognized by your peers, such as: \"John Smith\"")),
				__("Peer Name"), Application_GUI.QUESTION_MESSAGE);
		return val;
	}
	public static void changeMyPeerName(Component win) throws P2PDDSQLException {
		if (D_Peer.DEBUG) System.out.println("HandlingMyself_Peer: peer_ID: "+Application.getCurrent_Peer_ID().getPeerGID());
		if (Application.getCurrent_Peer_ID().getPeerGID() == null) {
				JOptionPane.showMessageDialog(win,
					__("You are not yet a peer.\n Start your server first!"),
					__("Peer Init"), JOptionPane.WARNING_MESSAGE);
				return;
		}
		
		String val = ControlPane.queryName(win);
		if ((val != null) && (!"".equals(val))) {
			D_Peer me = HandlingMyself_Peer.get_myself_with_wait();
			
			//Identity.current_peer_ID.name = val;
			//DD.setAppText(DD.APP_my_peer_name, val);
			me = D_Peer.getPeerByPeer_Keep(me);
			me.setName(val);
			me.setCreationDate();
			me.sign();
			me.dirty_main = true;
			me.setArrivalDate();
			me.storeRequest();
			me.releaseReference();
			DD.touchClient();
			if (D_Peer.DEBUG) System.out.println("HandlingMyself_Peer:changeMyPeerName:Saving:"+me);
		} else {
			if (D_Peer.DEBUG)System.out.println("HandlingMyself_Peer:peer_ID: Empty Val");
		}
	}
}
