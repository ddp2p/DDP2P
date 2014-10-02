package com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import data.D_Peer;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;

//import plugin_data.PeerPluginEditor;
//import plugin_data.PeerPluginRenderer;
public class Main {
    private static final boolean DEBUG = false;
	static java.util.ArrayList<plugin_data.PluginRequest> queue = new java.util.ArrayList<plugin_data.PluginRequest>();
	static String plugin_GID = "Chat_B1_v1.0.0";//"Dong Hang PUBLIC GID ANDROID CHAT version 1.0.0";
	static String peer_GID;
	static String plugin_name = "AndroidChat";
	static String name;
	
	// Peer_GIDH -> ArrayList<{byte[], date}> session
	// Hash(Peer_GID,session) -> Integer nb_messages
	// Hash(Peer_GID,session,sequence) -> {Date, message}
	
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
		if (peer != null && peerGID == null) peerGID = peer.getGID();
		// create a hashtable where ht(key=peerGID ) ==> cm (chatMessage)
		ChannelData cd = ChannelData.get(peer_GID);
		ChannelDataIn cdi = ChannelDataIn.get(peer_GID);
		if (DEBUG) System.out.println("PLUGIN CHAT: got channel: "+cd);
    	ChatMessage cm = new ChatMessage();
    	cm.session_id = cd.getSessionID();
		//cm.session_id_ack = cmsg.session_id;
		cm.message_type = ChatMessage.MT_TEXT;
		cm.first_in_this_sequence = cd.getFirstInSequence();//cmsg.first_in_this_sequence;
		
		ChatElem ce = new ChatElem();
		ce.type=0; //name
		ce.val=Main.name;
		cm.content = new ArrayList<ChatElem>();
		cm.content.add(ce); 
	
		cm.msg = message;
		// first time message to this peer
		cm.last_acknowledged_in_sequence = cdi.getLastInSequence();
		cm.received_out_of_sequence = cdi.getOutOfSequence();		
		cm.sequence = cd.getNextSequence();
		msgTrackingSend.put(peer_GID, cm); // only store the last message info for each peer
		//save to DB for history ( peer_GID:sender(myself): Date/Time : msg as object )
		if (DEBUG) System.out.println("PLUGIN CHAT: send: "+cm);
		
		AndroidChatReceiver.addSentMessage(cm, peerGID);
		
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
	
	//static plugin_data.PeerPluginRenderer renderer = null;//new PeerPluginRenderer();
	//static plugin_data.PeerPluginEditor editor = null;//new PeerPluginEditor();
	//static ChatPeer chatFrame;
	//static JMenuItem ab;
	/*
	    public static ImageIcon getImageIconFromResource(String entry, String _splash, Object _frame, String descr){
	            if(DEBUG)System.err.println("ChatPeer: getImageFromResource: "+entry+_splash);
	            Class<?> cl;
	            if(_frame == null) cl = chatApp.ChatPeer.class;
	            else cl = _frame.getClass();
	            String splash = entry+_splash;
	            
	            ImageIcon image = new ImageIcon();//");
	    image.setImage( Toolkit.getDefaultToolkit().getImage(cl.getResource(splash)));
	    //Image newimg = img.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
	    if(descr!=null) image.setDescription(descr);
	            if(DEBUG)System.err.println("ChatPeer: getImageFromResource: done");
	    return image;
	    }
	*/
	    /**
	       Called from DD to retrieve messages
	       DO NOT CHANGE THIS!
	     */
	    public static Hashtable<String,Object> checkOutgoingMessageHashtable() {
	    	if (DEBUG) System.out.println("Plugin:ChatApp:checkOutgoingMessageHashtable: start");
	    	Hashtable<String,Object> pd;
	    	//pd = plugin.checkOutgoingMessage().getHashtable();
	    	synchronized (queue) {
	    		while (queue.size()==0) {
	    			try {
	    				queue.wait();
	    			} catch(Exception e){}
	    		}
	    		pd = queue.remove(0).getHashtable();
	    	}
	    	if (DEBUG) System.out.println("Plugin:ChatApp:checkOutgoingMessageHashtable: got="+pd);//plugin_data.PluginRequest.display(pd));
	    	return pd;
	    }
	    public static Hashtable<String,Object> getPluginDataHashtable() {
	    	Hashtable<String,Object> pd = new Hashtable<String,Object>();
	    	pd.put("plugin_GID", plugin_GID);
	    	pd.put("plugin_name", plugin_name);
	    	pd.put("plugin_info", "info");
	    	pd.put("plugin_url", "url");
	    	//pd.put("editor",editor);
	    	//pd.put("renderer",renderer);
	    	return pd;
	    }
	    /**
	         * Required. First procedure ever called. Adjust to your needs.
	     */
	    public static void init() {
	    	if (false) {
				plugin_data.PluginRequest sentMenuItem = new plugin_data.PluginRequest();
				sentMenuItem.type = plugin_data.PluginRequest.REGISTER_MENU;
				sentMenuItem.plugin_GID = plugin_GID;
				sentMenuItem.column = plugin_data.PluginMenus.COLUMN_MYSELF; // for a new plugin how to decide which col? 
				enqueue(sentMenuItem);
	    	}	
	
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
    	//boolean DEBUG = true;
		if (DEBUG) System.out.println("PLUGIN CHAT: From: "+peer_GID+" got "+plugin_data.PluginRequest.byteToHex(msg,":"));
	    ChatMessage cmsg=null;
	    try {
			cmsg = new ChatMessage().decode(new Decoder(msg));
			if (DEBUG) System.out.println("PLUGIN CHAT:  got msg: "+cmsg);
			// implement handling of sequences ...
			// check type of the message ( chat msg or ack) call receiver.recive() or receiver.ack()
			if(cmsg.message_type == cmsg.MT_TEXT){
				if (DEBUG) System.out.println("PLUGIN CHAT:  got text");
			    //receive(cmsg.first_in_this_sequence, cmsg.sequence, cmsg.msg, peer_GID);
			    receiveTxt(cmsg, peer_GID);
			}
			else if(cmsg.message_type == cmsg.MT_IMAGE){
				if (DEBUG) System.out.println("PLUGIN CHAT:  got image");
			    //receive(cmsg.first_in_this_sequence, cmsg.sequence, null/*cmsg.content.get(0)*/, peer_GID);
				receiveImage(cmsg, peer_GID);
			}
			else if(cmsg.message_type == cmsg.MT_EMPTY){
			
				if (DEBUG) System.out.println("PLUGIN CHAT:  got empty");
				//ack(cmsg.sequence, cmsg.out_of_sequence, peer_GID);
				ack(cmsg, peer_GID);
			}
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return;
		}
	    
    }
    
    public static void sendAckMsg(ChatMessage cmsg, String peer_GID){
    	//boolean DEBUG = true;
    	ChannelData cd = ChannelData.get(peer_GID);
    	ChatMessage cm = new ChatMessage();
    	cm.session_id = cd.getSessionID();
		cm.session_id_ack = cmsg.session_id;
		cm.message_type = ChatMessage.MT_EMPTY; // just to inform session_id, session_id_ack, first_in_this_sequence, last_acknowledged_in_sequence, received_out_of_sequence
		cm.first_in_this_sequence = cd.getFirstInSequence();//cmsg.first_in_this_sequence;
		cm.sequence = cd.getNextSequence();//cmsg.sequence;
		cm.last_acknowledged_in_sequence = cmsg.last_acknowledged_in_sequence;
		cm.content = null; 	
		cm.msg = null;
    	if(DEBUG) System.out.println("PLUGIN CHAT: sendAckMsg: sending "+cm);
		byte[] _msg = cm.encode();
		plugin_data.PluginRequest  envelope = new plugin_data.PluginRequest();
		envelope.msg = _msg;
		envelope.peer_GID = peer_GID; //destination
		envelope.type = plugin_data.PluginRequest.MSG;
		envelope.plugin_GID=(String)Main.getPluginDataHashtable().get("plugin_GID");
		Main.enqueue(envelope);
     	if(DEBUG) System.out.println("PLUGIN CHAT: sendAckMsg: sending env "+envelope);
   }
    public static void receiveTxt(ChatMessage cmsg, String peer_GID){
    	//boolean DEBUG = true;
		if(DEBUG) System.out.println("PLUGIN CHAT: enter-----------receiveTxt()");
    	ChatMessage cm;
    	// first time to receive a msg from this peer(unless it is in the DB)	
    	if ((cm=msgTrackingReceive.get(peer_GID))!=null){
			if(DEBUG) System.out.println("PLUGIN CHAT: from known");
    		if(!Arrays.equals(cmsg.session_id, cm.session_id)){
    			cmsg.session_id_ack = cm.session_id;
    			sendAckMsg(cm, peer_GID); // tell about unknown session
    			if(DEBUG) System.out.println("-----------receiveTxt(): Not equal session");
    			return;
    		}
    		if(DEBUG) System.out.println("PLUGIN CHAT: from unknown");
    		if(!cmsg.first_in_this_sequence.equals( cm.first_in_this_sequence)){
    		    if(DEBUG) System.out.println("-----------receiveTxt(): Not equal first_in_this_sequence");
    			return; // What to do?
    		}
    		if(DEBUG) System.out.println("PLUGIN CHAT: from 2");
    		if(cmsg.sequence.compareTo(cm.sequence) <= 0){ // ignore old messages
    		    if(DEBUG) System.out.println("-----------receiveTxt(): old message<0");
    		    return; 
    		}
    		if(DEBUG) System.out.println("PLUGIN CHAT: from 3");
    		if(cmsg.sequence.compareTo(cm.sequence.add(BigInteger.ONE)) != 0){ //not the next new message
    			if(DEBUG) System.out.println("-----------receiveTxt(): too new message>0");
    			// save cmsg to database as a new message
    			// storeDB((peer_GID, cmsg));
    			if(cm.received_out_of_sequence == null)
    				cm.received_out_of_sequence = new ArrayList<BigInteger>();
    			cm.received_out_of_sequence.add(cmsg.sequence);// check for doublicate
    			msgTrackingReceive.put(peer_GID, cm); // no need for this stm
    			sendAckMsg(cm, peer_GID); // create new empty msg as incompatible seq. ack (cm can be checked)
    			return; //should we display or not??
    		}
    		if(DEBUG) System.out.println("PLUGIN CHAT: from 4");
    	} else {
    		if(DEBUG) System.out.println("PLUGIN CHAT: from unknown");
    	}
    	if(DEBUG) System.out.println("-----------receiveTxt(): new message should display");
    	ChannelDataIn cdi = ChannelDataIn.get(peer_GID);
    	cdi.registerIncoming(cmsg.session_id, cmsg.first_in_this_sequence , cmsg.sequence);
    	msgTrackingReceive.put(peer_GID, cmsg); // store the last good msg (insequence)
    	cmsg.last_acknowledged_in_sequence = cdi.getLastInSequence(); //cmsg.sequence;
    	sendAckMsg(cmsg, peer_GID); // create new empty msg as confirmation ack
    	//storeDB((peer_GID, cmsg));
    	displayMsg(cmsg, peer_GID);
    }
    
   public static void displayMsg(ChatMessage cmsg, String peer_GID){
    	String msgStr = cmsg.msg;
    	String peerName="no name provided";
    	for (ChatElem ce : cmsg.content)
			if (ce.type == 0) {
				peerName = ce.val;
				break;
			}
		if(DEBUG)System.out.println("From: "+peer_GID+" got: "+ msgStr );
		
//		if (receiver != null) {
//			if (! receiver.peerExist(peer_GID)){
//				receiver.addPeer(peerName, peer_GID,false);
//			}
//			receiver.setUser(name);
//			receiver.setVisible(true);		
//		}
		receiver.receiveMessage(cmsg.first_in_this_sequence, cmsg.sequence, msgStr, peerName, peer_GID, cmsg.session_id, cmsg);
		
		
    }
    
   public static void receiveImage(ChatMessage cmsg, String peer_GID){
    	
    }
    
   public static void ack(ChatMessage cmsg, String peer_GID){
		ChatMessage cm;	
    	if ((cm=msgTrackingSend.get(peer_GID))==null){
    		//What to do?
    		return;
    	}
   	    
    	if(!Arrays.equals(cmsg.session_id, cm.session_id)){
    		//What to do?
    		return;
    	}
    	
    	if(cmsg.session_id_ack!=null){
    		//What to do?
    		return;
    	}
    	
    	if(cmsg.first_in_this_sequence.compareTo(cm.first_in_this_sequence)!=0){
    		//What to do?
    		return;
    	}
    	
    	if(cmsg.sequence.compareTo(cm.sequence)==0 && 
    		cmsg.last_acknowledged_in_sequence.compareTo(cm.sequence)==0) {
    		System.out.println("===========================");
	        System.out.println("= CAAAAAALLLLL confirmMsg =");
	        System.out.println("===========================");
    		//change color of the message as confirmation of arrival
	        receiver.confirmMessages(cmsg.last_acknowledged_in_sequence, cmsg.received_out_of_sequence, peer_GID, cmsg.session_id_ack);
    		return;
    	}
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

