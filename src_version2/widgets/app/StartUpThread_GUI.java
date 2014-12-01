/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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

package widgets.app;

import static util.Util.__;
import hds.StartUp;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import util.DDP2P_ServiceRunnable;
import util.P2PDDSQLException;
import streaming.OrgHandling;
import util.Util;
import widgets.components.GUI_Swing;
import wireless.Broadcasting_Probabilities;
import config.Application_GUI;
import config.DD;
import data.HandlingMyself_Peer;

class ChatElem extends ASN1.ASNObj {
	public ChatElem instance() {
		return new ChatElem();
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		// TODO Auto-generated method stub
		return enc;
	}

	@Override
	public  ChatElem decode(Decoder dec) throws ASN1DecoderFail {
		// TODO Auto-generated method stub
		return this;
	}

	public static byte getASN1Type() {
		return DD.TAG_AC19;
	}
	
}

class ChatMessage extends ASN1.ASNObj {
	int version = 0;
	int message_type = 0; // 0-TEXT (message is in "msg"), 1-image, 2-request full element
	byte[] session_id; // array of 8 random numbers
	BigInteger requested_element;
	String msg;
	ArrayList<ChatElem> content;
	BigInteger first_in_this_sequence; // the id of the first message in this sequence (>0)
	BigInteger sequence;
	BigInteger sub_sequence;
	BigInteger sub_sequence_ack;
	BigInteger last_acknowledged_in_sequence;
	BigInteger[] received_out_of_sequence;
	
	static byte[] createSessionID() {
		byte[] rnd = new byte[8];
		Random r = new SecureRandom();
		r.nextBytes(rnd);
		return rnd;
	}

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(message_type));
		enc.addToSequence(new Encoder(session_id));
		if (requested_element != null) enc.addToSequence(new Encoder(requested_element).setASN1Type(DD.TAG_AP2));
		if (msg != null) enc.addToSequence(new Encoder(msg).setASN1Type(DD.TAG_AP3));
		if (content != null) enc.addToSequence(Encoder.getEncoder(content).setASN1Type(DD.TAG_AC4));
		if (first_in_this_sequence != null) enc.addToSequence(new Encoder(first_in_this_sequence).setASN1Type(DD.TAG_AP5));
		if (sequence != null) enc.addToSequence(new Encoder(sequence).setASN1Type(DD.TAG_AP6));
		if (sub_sequence != null) enc.addToSequence(new Encoder(sub_sequence).setASN1Type(DD.TAG_AP7));
		if (sub_sequence_ack != null) enc.addToSequence(new Encoder(sub_sequence_ack).setASN1Type(DD.TAG_AP8));
		if (last_acknowledged_in_sequence != null) enc.addToSequence(new Encoder(last_acknowledged_in_sequence).setASN1Type(DD.TAG_AP9));
		if (received_out_of_sequence != null) enc.addToSequence(new Encoder(received_out_of_sequence).setASN1Type(DD.TAG_AC10));
		return enc;
	}

	@Override
	public ChatMessage decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getInteger().intValue();
		message_type = d.getFirstObject(true).getInteger().intValue();
		session_id = d.getFirstObject(true).getBytes();
		if (d.getTypeByte() == DD.TAG_AP2) requested_element = d.getFirstObject(true).getInteger();
		if (d.getTypeByte() == DD.TAG_AP3) msg = d.getFirstObject(true).getString();
		if (d.getTypeByte() == DD.TAG_AC4) content = d.getFirstObject(true).getSequenceOfAL(ChatElem.getASN1Type(), new ChatElem());
		if (d.getTypeByte() == DD.TAG_AP5) first_in_this_sequence = d.getFirstObject(true).getInteger();
		if (d.getTypeByte() == DD.TAG_AP6) sequence = d.getFirstObject(true).getInteger();
		if (d.getTypeByte() == DD.TAG_AP7) sub_sequence = d.getFirstObject(true).getInteger();
		if (d.getTypeByte() == DD.TAG_AP8) sub_sequence_ack = d.getFirstObject(true).getInteger();
		if (d.getTypeByte() == DD.TAG_AP9) last_acknowledged_in_sequence = d.getFirstObject(true).getInteger();
		if (d.getTypeByte() == DD.TAG_AC10) received_out_of_sequence = d.getFirstObject(true).getBNIntsArray();
		return this;
	}
}

/**
 * This class starts the services configured in the application table
 * @author silaghi
 *
 */
public class StartUpThread_GUI extends util.DDP2P_ServiceThread {
	TrayIcon trayIcon;
	public StartUpThread_GUI() {
		super("StartUpThread",false);
	}
	
	public void registerTray(){
        if(StartUp.DEBUG)System.err.println("TrayIcon support checked.");


		if (SystemTray.isSupported()) {
		       if(StartUp.DEBUG)System.err.println("TrayIcon supported.");

		    SystemTray tray = SystemTray.getSystemTray();
		    //Image image = Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"favicon.ico"));
		    Image image = widgets.app.DDIcons.getImageFromResource(DDIcons.I_DDP2P40, MainFrame.frame); //Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"angry16.png"));
		    if(image==null) System.err.println("TrayIcon image failed.");
		    //Image image = Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"favicon.ico"));
		    //Image image = DDIcons.getImageIconFromResource("favicon.ico", "P2P Direct Democracy");//Toolkit.getDefaultToolkit().getImage("tray.gif");

		    MouseListener mouseListener = new MouseListener() {
		                
		        public void mouseClicked(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse clicked!");                 
		        }

		        public void mouseEntered(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse entered!");                 
		        }

		        public void mouseExited(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse exited!");                 
		        }

		        public void mousePressed(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse pressed!");                 
		        }

		        public void mouseReleased(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse released!");                 
		        }
		    };

		    ActionListener exitListener = new ActionListener() {
		        public void actionPerformed(ActionEvent e) {
		            System.out.println("Exiting...");
		            System.exit(0);
		        }
		    };
		            
		    PopupMenu popup = new PopupMenu();
		    MenuItem defaultItem = new MenuItem("Exit");
		    defaultItem.addActionListener(exitListener);
		    popup.add(defaultItem);

		    trayIcon = new TrayIcon(image, "P2P Direct Democracy", popup);

		    ActionListener actionListener = new ActionListener() {
				@Override
		        public void actionPerformed(ActionEvent e) {
		            trayIcon.displayMessage("Action Event", 
		                "An Action Event Has Been Performed!",
		                TrayIcon.MessageType.INFO);
		        }
		    };
		            
		    trayIcon.setImageAutoSize(true);
		    trayIcon.addActionListener(actionListener);
		    trayIcon.addMouseListener(mouseListener);

		    try {
		        tray.add(trayIcon);
		    } catch (AWTException e) {
		        System.err.println("TrayIcon could not be added.");
		    }
		    
//		    trayIcon.displayMessage("Finished downloading", 
//		            "Your Java application has finished downloading",
//		            TrayIcon.MessageType.INFO);

		} else {

	        System.err.println("TrayIcon not supported.");
		    //  System Tray is not supported

		}
		
		MainFrame.loadAppIcons(true, MainFrame.frame);
	}
	
	/**
	 * Should be called only from dispatcher
	 */
	public static void initQueuesStatus(){
		if(StartUp.DEBUG) System.out.println("StartUpThread:initQueuesStatus: start");
		if(StartUp._DEBUG) if(!EventQueue.isDispatchThread()) Util.printCallPath("Not dispatcher");
		if(MainFrame.controlPane == null){
			if(StartUp._DEBUG) System.out.println("StartUpThread:initQueuesStatus: no control pane");
			return;
		}
		
		MainFrame.controlPane.q_MD.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_C.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_RA.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_RE.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_BH.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_BR.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_MD.setSelected(Broadcasting_Probabilities._broadcast_queue_md);
		MainFrame.controlPane.q_C.setSelected(Broadcasting_Probabilities._broadcast_queue_c);
		MainFrame.controlPane.q_RA.setSelected(Broadcasting_Probabilities._broadcast_queue_ra);
		MainFrame.controlPane.q_RE.setSelected(Broadcasting_Probabilities._broadcast_queue_re);
		MainFrame.controlPane.q_BH.setSelected(Broadcasting_Probabilities._broadcast_queue_bh);
		MainFrame.controlPane.q_BR.setSelected(Broadcasting_Probabilities._broadcast_queue_br);
		MainFrame.controlPane.q_MD.addItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_C.addItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_RA.addItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_RE.addItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_BH.addItemListener(MainFrame.controlPane);
		MainFrame.controlPane.q_BR.addItemListener(MainFrame.controlPane);
			
		MainFrame.controlPane.tcpButton.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.udpButton.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.tcpButton.setSelected(DD.ClientTCP);
		MainFrame.controlPane.udpButton.setSelected(DD.ClientUDP);
		MainFrame.controlPane.tcpButton.addItemListener(MainFrame.controlPane);
		MainFrame.controlPane.udpButton.addItemListener(MainFrame.controlPane);
			
			
		MainFrame.controlPane.serveDirectly.removeItemListener(MainFrame.controlPane);
		MainFrame.controlPane.serveDirectly.setSelected(OrgHandling.SERVE_DIRECTLY_DATA);
		MainFrame.controlPane.serveDirectly.addItemListener(MainFrame.controlPane);
			
		if(StartUp.DEBUG) System.out.println("StartUpThread:initQueuesStatus: start");
	}
	/**
	 * Clients/Servers/Directory/Updates
	 * @throws NumberFormatException
	 * @throws P2PDDSQLException
	 */
	public static void initServers_GUI() throws NumberFormatException, P2PDDSQLException {
		if (StartUp.directory_server_on_start){
			MainFrame.controlPane.setDirectoryStatus(true);//startDirectoryServer(true, -1);
		}

		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat dir");
		if (StartUp.data_userver_on_start){
			MainFrame.controlPane.setUServerStatus(true);
		}
		
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat userver");
		if (StartUp.data_server_on_start){
			MainFrame.controlPane.setServerStatus(true);
		}

		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat tcpserver");
		if (StartUp.data_client_on_start){
			MainFrame.controlPane.setClientStatus(true);//startClient(true);
		}
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat client");
		if (StartUp.data_client_updates_on_start) {
			MainFrame.controlPane.setClientUpdatesStatus(true);//startClient(true);
		}
	}
	public static void initBroadcastGUI() {
		if(DD.GUI) {
			DDP2P_ServiceRunnable initQS = new DDP2P_ServiceRunnable("StartUp:InitQueues", false, false, "StartUp:InitQueues"){
				public void _run(){StartUpThread_GUI.initQueuesStatus();}
			};
			Application_GUI.eventQueue_invokeLater(initQS);
		}
		EventQueue.invokeLater(new Runnable(){
			public void run(){
				if(GUI_Swing.controlPane == null){
					if(StartUp.DEBUG) System.out.println("StartUpThread:run: No control pane");
					return;
				}
				if(GUI_Swing.controlPane.m_area_ADHOC_BIP==null) return;
				GUI_Swing.controlPane.m_area_ADHOC_BIP.setText(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP);
				GUI_Swing.controlPane.m_area_ADHOC_IP.setText(DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
				if(StartUp.DEBUG) System.out.println("StartUpThread:run: IPBase later = "+DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
				GUI_Swing.controlPane.m_area_ADHOC_Mask.setText(DD.WIRELESS_ADHOC_DD_NET_MASK);
				GUI_Swing.controlPane.m_area_ADHOC_SSID.setText(DD.DD_SSID);
				
				if(GUI_Swing.panelUpdates!=null) {
					try {
						String testers_count_value=DD.getAppText(DD.UPDATES_TESTERS_THRESHOLD_COUNT_VALUE);
						try{if(testers_count_value != null)
								testers_count_value = ""+Integer.parseInt(testers_count_value);}catch(Exception e){};
						if (testers_count_value == null) testers_count_value = "" + DD.UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT; 
						GUI_Swing.panelUpdates.numberTxt.setText(testers_count_value);
						
						String testers_count_weight = DD.getAppText(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_VALUE);
						try{if(testers_count_weight != null)
							testers_count_weight = ""+Float.parseFloat(testers_count_weight);}catch(Exception e){};
						if (testers_count_weight == null) testers_count_weight = "" + DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT; 							
						GUI_Swing.panelUpdates.percentageTxt.setText(testers_count_weight);
						if(DD.getAppBoolean(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT)){
							GUI_Swing.panelUpdates.percentageButton.setSelected(true);
						}else{
							GUI_Swing.panelUpdates.numberButton.setSelected(true);								
						}
						GUI_Swing.panelUpdates.absoluteCheckBox.setSelected(!DD.getAppBoolean(DD.UPDATES_TESTERS_THRESHOLDS_RELATIVE));
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	public void _run() {
		//boolean DEBUG = true;
		try{
			StartUp.init_startup();
			//if(DEBUG) System.out.println("Starting UDP Server");
			//DD.userver= new UDPServer(Server.PORT);
			//DD.userver.start();
			//if(DEBUG) System.out.println("StartUpThread:run: will create peer");
			//MyselfHandling.createMyPeerIDIfEmpty();
			//D_Peer me = HandlingMyself_Peer.get_myself();
			/*
			if (me == null) {
				me = HandlingMyself_Peer.createMyselfPeer_by_dialog_w_Addresses(true);
				if (me == null){
					if (_DEBUG) System.out.println("StartUpThread:run: fail to create peer!");
					System.exit(1);
				}
			}
			*/
			//if(DEBUG) System.out.println("StartUpThread:run: created peer");
			if (MainFrame.controlPane == null) StartUp.initServers_no_GUI();
			else initServers_GUI();
			
			StartUp.initBroadcastServers();
			if (DD.GUI) initBroadcastGUI();			

			if (DD.GUI) {
				plugin_data.PluginRegistration.loadPlugins(HandlingMyself_Peer.getMyPeerGID(), HandlingMyself_Peer.getMyPeerName());
				
				//plugin_data.PluginRegistration.loadPlugin(AndroidChat.Main.class, HandlingMyself_Peer.getMyPeerGID(), HandlingMyself_Peer.getMyPeerName());
			}

			StartUp.initBroadcastSettings();

			if (StartUp.DEBUG) System.out.println("StartUpThread:run: startThread done, go in Tray");
			registerTray();
	
		}
		catch(Exception e){e.printStackTrace();}
	}

}
