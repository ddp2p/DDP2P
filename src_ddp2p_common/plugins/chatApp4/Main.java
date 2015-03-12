package dd_p2p.plugin;

import java.util.*;
import java.util.Hashtable;
import javax.swing.Action;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
import java.awt.Toolkit;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.math.BigInteger;

import dd_p2p.plugin.*;
import data.D_Peer;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;


class ChatPeerPluginRenderer implements plugin_data.PeerPluginRenderer, TableCellRenderer {
	public static final boolean DEBUG = false;
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable a,java.lang.Object b,boolean c,boolean d,int e,int f) {
		return new javax.swing.JLabel(Main.getImageIconFromResource("/chatApp_icons/","red-sphere.gif",null,"chat"));
    }
};
class ChatPeerPluginEditor implements plugin_data.PeerPluginEditor, TableCellEditor {
	public java.awt.Component getTableCellEditorComponent(javax.swing.JTable a,java.lang.Object b,boolean c,int d,int e){
		return null;
    }
    public void removeCellEditorListener(javax.swing.event.CellEditorListener a){}
    public void addCellEditorListener(javax.swing.event.CellEditorListener a){}
    public void cancelCellEditing(){}
    public boolean stopCellEditing(){return false;}
    public boolean shouldSelectCell(java.util.EventObject a){return false;}
    public boolean isCellEditable(java.util.EventObject a){return false;}
    public Object  getCellEditorValue(){return null;}
};
/*
class ChatPeerPlugin implements plugin_data.PeerPlugin {
    java.util.ArrayList<plugin_data.PluginRequest> queue =
	new java.util.ArrayList<plugin_data.PluginRequest>();
    public void handleReceivedMessage(byte[] msg, String peer_GID){
	System.out.println("From: "+peer_GID+" got="+msg);
    }
    public plugin_data.PluginRequest checkOutgoingMessage(){
	synchronized(queue){
	    while(queue.size()==0) {
		try {
		    queue.wait();
		} catch(Exception e){}
	    }
	    return queue.remove(0);
	}
    }
    public void answerMessage(String key, byte[] msg){
	System.out.println("From: db "+key+" got="+msg);
    }
    public void setSendPipe(plugin_data.PeerConnection connection){}
};
*/
public
class Main {
	private static  boolean DEBUG = true;
    static java.util.ArrayList<plugin_data.PluginRequest> queue = new java.util.ArrayList<plugin_data.PluginRequest>();
    static String plugin_GID = "Chat_B1_v1.0.0";
    static String peer_GID;
    static String plugin_name = "ChatApp";
    static String name;
    static ChatPeerPluginRenderer renderer = new ChatPeerPluginRenderer();
    static ChatPeerPluginEditor editor = new ChatPeerPluginEditor();
    static ChatPeer chatFrame;
    static JMenuItem ab;
    
    static ChatReceiver receiver;
	static BigInteger none = BigInteger.ZERO;
	static BigInteger first = BigInteger.ONE;
	static BigInteger crt = BigInteger.ONE;
	static Hashtable<String, ChatMessage> msgTrackingSend = new Hashtable<String, ChatMessage>();
    static Hashtable<String, ChatMessage> msgTrackingReceive = new Hashtable<String, ChatMessage>();
	
	    /**
     * 
     * @param message
     * @param peerGID
     * @param peer
     * @return the message actually sent (and its sequence numbers)
     */
	public static ChatMessage sendMessage(String message, String peerGID, D_Peer peer) {
		boolean DEBUG=true;
		if(DEBUG)System.out.println("Plugin Chat: Main.sendMessage:"+ message);
		if (peer != null && peerGID == null) peerGID = peer.getGID();
		// create a hashtable where ht(key=peerGID ) ==> cm (chatMessage)
		ChannelDataOut channeldata_out = ChannelDataOut.get(peerGID);
		ChannelDataIn channeldata_in = ChannelDataIn.get(peerGID);
		if (DEBUG) System.out.println("PLUGIN CHAT: got channel: "+channeldata_out);
    	ChatMessage cm = new ChatMessage();
    	cm.session_id = channeldata_out.getSessionID();
		//cm.session_id_ack = cmsg.session_id;
		cm.message_type = ChatMessage.MT_TEXT;
		cm.first_in_this_sequence = channeldata_out.getFirstInSequence();//cmsg.first_in_this_sequence;
		
		ChatElem ce = new ChatElem();
		ce.type = 0; //name
		ce.val = Main.name;
		cm.content = new ArrayList<ChatElem>();
		cm.content.add(ce); 
	
		cm.msg = message;
		// first time message to this peer
		cm.session_id_ack = channeldata_in.getSessionID();
		cm.last_acknowledged_in_sequence = channeldata_in.getLastInSequence();
		cm.received_out_of_sequence = channeldata_in.getOutOfSequence();		
		cm.sequence = channeldata_out.getNextSequence();
		//msgTrackingSend.put(peer_GID, cm); // only store the last message info for each peer
		//save to DB for history ( peer_GID:sender(myself): Date/Time : msg as object )
		if (DEBUG) System.out.println("PLUGIN CHAT: send: "+cm);
		
		// confirm the sending into the GUI
		receiver.addSentMessage(cm, peerGID, channeldata_out);
		
		byte[] _msg = cm.encode();	
		plugin_data.PluginRequest  envelope = new plugin_data.PluginRequest();
		envelope.msg = _msg;
		envelope.peer_GID = peerGID; //destination
		envelope.type = plugin_data.PluginRequest.MSG;
		envelope.plugin_GID=(String)Main.getPluginDataHashtable().get("plugin_GID");
		if (DEBUG) System.out.println("PLUGIN CHAT: send envelope: "+envelope);
		Main.enqueue(envelope);
		return cm;
	}
    
	public static ImageIcon getImageIconFromResource(String entry, String _splash, Object _frame, String descr){
		boolean DEBUG=false;
		if(DEBUG)System.err.println("ChatPeer: getImageFromResource: "+entry+_splash);
		Class<?> cl;
		if(_frame == null) cl = ChatPeer.class;
		else cl = _frame.getClass();
		String splash = entry+_splash;
		
		ImageIcon image = new ImageIcon();//");
        image.setImage( Toolkit.getDefaultToolkit().getImage(cl.getResource(splash)));
    	//Image newimg = img.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        if(descr!=null) image.setDescription(descr);
		if(DEBUG)System.err.println("ChatPeer: getImageFromResource: done");
        return image;
	}
    //static widgets.peers.BulletRenderer renderer = new widgets.peers.BulletRenderer();
    //static ChatPeerPlugin plugin = new ChatPeerPlugin();

    /*
    public static String getPluginGID() {
	return plugin_GID;
    }
    public static String getPluginName() {
	return plugin_name;
    }
    @Deprecated
    public static plugin_data.PeerPlugin getPeerPlugin() {
	return plugin;
    }
    @Deprecated
    public static data.D_PluginInfo getPluginData() {
	    return new data.D_PluginInfo(plugin_GID,plugin_name,"info","url",
					 (plugin_data.PeerPluginEditor)editor,
					 ( plugin_data.PeerPluginRenderer)renderer
					 );
    }
    */    

    /**
       Called from DD to retrieve messages
       DO NOT CHANGE THIS!
     */
    public static Hashtable<String,Object> checkOutgoingMessageHashtable() {
		if(DEBUG)System.out.println("Plugin:ChatApp:checkOutgoingMessageHashtable: start");
		Hashtable<String,Object> pd;
		//pd = plugin.checkOutgoingMessage().getHashtable();
		synchronized(queue){
			while(queue.size()==0) {
				try {
					queue.wait();
				} catch(Exception e){}
			}
			pd = queue.remove(0).getHashtable();
		}
		if(DEBUG)System.out.println("Plugin:ChatApp:checkOutgoingMessageHashtable: got="+pd);//plugin_data.PluginRequest.display(pd));
		return pd;
    }
    public static Hashtable<String,Object> getPluginDataHashtable() {
		Hashtable<String,Object> pd = new Hashtable<String,Object>();
		pd.put("plugin_GID", plugin_GID);
		pd.put("plugin_name", plugin_name);
		pd.put("plugin_info", "info");
		pd.put("plugin_url", "url");
		pd.put("editor",editor);
		pd.put("renderer",renderer);
		return pd;
    }
    /**
	 * Required. First procedure ever called. Adjust to your needs.
     */
    public static void init() {
//		sb = new JMenuItem("start");
		Main.receiver=new PluginChatReceiver();
		ActionListener actionListener = new ActionListener() {
     		 	public void actionPerformed(ActionEvent actionEvent) {
     		 	if(actionEvent.getActionCommand().equals("start")){
     		 		chatFrame.setUser(name);
     		 		chatFrame.setVisible(true);
//        		    chatFrame.addPeer((String)sb.getClientProperty(plugin_data.PluginMenus.NAME),
//        		                      (String)sb.getClientProperty(plugin_data.PluginMenus.GID));
     		 	} else {
     		 		String peerGID = (String)ab.getClientProperty(plugin_data.PluginMenus.GID);
     		 		if(chatFrame!=null){
     		 		if(!chatFrame.peerExist(peerGID) && !peerGID.equals(peer_GID) )
     		 		chatFrame.addPeer((String)ab.getClientProperty(plugin_data.PluginMenus.NAME),
        		                      (String)ab.getClientProperty(plugin_data.PluginMenus.GID), true);
        		    chatFrame.setUser(name);
        		    chatFrame.setVisible(true);                  
     		 		}
        		                     //////////////////////////////////////
//						plugin_data.PluginRequest msg = new plugin_data.PluginRequest();
//						msg.type=plugin_data.PluginRequest.MSG;
//						msg.plugin_GID=plugin_GID;
//						msg.msg= "hi khalid PC".getBytes();
//						msg.peer_GID=(String)sb.getClientProperty(plugin_data.PluginMenus.GID);
//						enqueue(msg);
     		 	}		     		     		     		
      			}
    		};

//		sb.addActionListener(actionListener);
		plugin_data.PluginRequest sentMenuItem = new plugin_data.PluginRequest();
//		sentMenuItem.type = plugin_data.PluginRequest.REGISTER_MENU;
//		sentMenuItem.plugin_GID = plugin_GID;
//		sentMenuItem.column =plugin_data.PluginMenus.COLUMN_MYSELF ; // for a new plugin how to decide which col? 
//		sentMenuItem.plugin_menuItem = sb;
		
		//sb.putClientProperty(plugin_data.PluginMenus.ROW_MYPEER, new Object());
		//sb.setClientProperty("","");
	//	enqueue(sentMenuItem);
		
		ab = new JMenuItem("ADD Peer");
		ab.addActionListener(actionListener);
		//sentMenuItem = new plugin_data.PluginRequest();
		sentMenuItem.type = plugin_data.PluginRequest.REGISTER_MENU;
		sentMenuItem.plugin_GID = plugin_GID;
		sentMenuItem.column = plugin_data.PluginMenus.COLUMN_MYSELF; // for a new plugin how to decide which col? 
		sentMenuItem.plugin_menuItem = ab;
		enqueue(sentMenuItem);
		chatFrame = new ChatPeer(Main.name);
	}
	/**
	 * Interface for communication with main system: to send, simply call enqueue()
	 */
    public static void enqueue(plugin_data.PluginRequest r) {
		synchronized(queue) {
			queue.add(r);
			queue.notify();
		}
    }
	/**
	 Called from outside:
	 */
    public static void handleReceivedMessage(byte[] msg, String peer_GID) {
    	boolean DEBUG = true;
		if (DEBUG) System.out.println("PLUGIN CHAT: From: "+peer_GID+" got "+plugin_data.PluginRequest.byteToHex(msg,":"));
	    ChatMessage cmsg=null;
	    
	    
	    try {
			cmsg = new ChatMessage().decode(new Decoder(msg));
			if (DEBUG) System.out.println("PLUGIN CHAT:  got msg: "+cmsg);
			
			//
			
			if(Main.chatFrame!=null){
				if (DEBUG) System.out.println("1)=================>>>>Main.chatFrame!=null");
				if(!Main.chatFrame.peerExist(peer_GID)){
					String peer_name = cmsg.getName();
				    if(peer_name==null)
				    	peer_name="GID("+peer_GID.substring(peer_GID.length()-6)+")";
				    if(Main.chatFrame.NumberOfpeerInUserCanvas()==0)
				    	Main.chatFrame.addPeer(peer_name, peer_GID, true);
					else Main.chatFrame.addPeer(peer_name, peer_GID, false);
				    
				}
				if (DEBUG) System.out.println("2)=================>>>>Main.chatFrame!=null");
				Main.chatFrame.setUser(Main.name);
				Main.chatFrame.setVisible(true);
			}
			// extract acknowledgment piggybacked in messages
	    	ChannelDataOut channeldata_out = ChannelDataOut.get(peer_GID);
	        receiver.confirmMessages(cmsg.last_acknowledged_in_sequence, cmsg.received_out_of_sequence, peer_GID,
	                                 cmsg.session_id_ack, channeldata_out);
			if (cmsg.message_type != ChatMessage.MT_EMPTY)
				forwardMsgToReceiverAndSendAckToSender(cmsg, peer_GID);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return;
		}
	    
    }
    /**
     * The message's acknowledgments were already sent to receiver (to set the right colors on sent data)
     * @param cmsg
     * @param peer_GID
     */
    private static void forwardMsgToReceiverAndSendAckToSender(ChatMessage cmsg, String peer_GID){
    	String msgStr = cmsg.msg;
    	if (DEBUG) System.out.println("From: " + peer_GID + " got: " + msgStr );
    	String peerName = cmsg.getName();
		
	   	if (DEBUG) System.out.println("-----------receiveTxt(): new message should display");
    	ChannelDataIn channeldata_in = ChannelDataIn.get(peer_GID);
    	if (channeldata_in.registerIncoming(cmsg))	{
			sendAckMsg(channeldata_in, cmsg, peer_GID); // create new empty msg as confirmation ack
		
			receiver.receiveMessage(cmsg.first_in_this_sequence, cmsg.sequence, msgStr, peerName, peer_GID, cmsg.session_id,
					cmsg, channeldata_in);
    	}
    }
    /**
     * Sends an acknowledgment for cmsg.
     * Not setting a sequence number, since the ack message is empty.
     * @param cmsg
     * @param peer_GID
     */
    private static void sendAckMsg(ChannelDataIn channeldata_in, ChatMessage cmsg, String peer_GID){
    	//boolean DEBUG = true;
    	ChannelDataOut cd = ChannelDataOut.get(peer_GID);
     	ChatMessage cm = new ChatMessage();
 		cm.message_type = ChatMessage.MT_EMPTY; // just to inform session_id, session_id_ack, first_in_this_sequence, last_acknowledged_in_sequence, received_out_of_sequence
    	
     	cm.last_acknowledged_in_sequence = channeldata_in.getLastInSequence(); //cmsg.sequence;
     	cm.received_out_of_sequence = channeldata_in.getOutOfSequence();
		cm.session_id_ack = channeldata_in.getSessionID(); //cmsg.session_id;
		
     	cm.session_id = cd.getSessionID();
		cm.first_in_this_sequence = cd.getFirstInSequence();//cmsg.first_in_this_sequence;
		// cm.sequence = cd.getNextSequence(); //cmsg.sequence. probably should not be set for empty messages!
		ChatElem ce = new ChatElem();
		ce.type = 0; //name
		ce.val = Main.name;
		cm.content = new ArrayList<ChatElem>();
		cm.content.add(ce);  	
		cm.msg = null;
		
    	if (DEBUG) System.out.println("PLUGIN CHAT: sendAckMsg: sending "+cm);
		byte[] _msg = cm.encode();
		plugin_data.PluginRequest  envelope = new plugin_data.PluginRequest();
		envelope.msg = _msg;
		envelope.peer_GID = peer_GID; //destination
		envelope.type = plugin_data.PluginRequest.MSG;
		envelope.plugin_GID = (String) Main.getPluginDataHashtable().get("plugin_GID");
		Main.enqueue(envelope);
     	if (DEBUG) System.out.println("PLUGIN CHAT: sendAckMsg: sending env "+envelope);
    }
    /**
       Message with answer from local database
     */
    public static void answerMessage(String key, byte[] msg) {
		if(DEBUG)System.out.println("From: db "+key+" got="+msg);
		//plugin.answerMessage(key, msg);
    }
    
        
    /** 
       tells you name and GID
     */
    public static void setPluginData(String peer_GID, String name){
		if(DEBUG)System.out.println("Plugin:Hello2:setPluginData: "+peer_GID+" name="+name);
		Main.peer_GID = peer_GID;
		Main.name = name;
    }
};

