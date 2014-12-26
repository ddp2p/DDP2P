/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Khalid Alhamed, Marius Silaghi <silaghi@fit.edu>
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
    private static final boolean DEBUG = true;
	static java.util.ArrayList<plugin_data.PluginRequest> queue = new java.util.ArrayList<plugin_data.PluginRequest>();
	static String plugin_GID = "Chat_B1_v1.0.0";//"Dong Hang PUBLIC GID ANDROID CHAT version 1.0.0";
	static String peer_GID;
	static String plugin_name = "AndroidChat";
	static String name;
	final static Object monitor = new Object(); 
	
	// Peer_GIDH -> ArrayList<{byte[], date}> session
	// Hash(Peer_GID,session) -> Integer nb_messages
	// Hash(Peer_GID,session,sequence) -> {Date, message}
	
	public static ChatReceiver receiver;
	static BigInteger none = BigInteger.ZERO;
	static BigInteger first = BigInteger.ONE;
	static BigInteger crt = BigInteger.ONE;
//	static Hashtable<String, ChatMessage> msgTrackingSend = new Hashtable<String, ChatMessage>();
//    static Hashtable<String, ChatMessage> msgTrackingReceive = new Hashtable<String, ChatMessage>();
	
    /**
     * 
     * @param message
     * @param peerGID
     * @param peer
     * @return the message actually sent (and its sequence numbers)
     */
	public static ChatMessage sendMessage(String message, String peerGID, D_Peer peer) {
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: sendMessage enter: "+message);
		if (peer != null && peerGID == null) peerGID = peer.getGID();
		// create a hashtable where ht(key=peerGID ) ==> cm (chatMessage)
		ChannelDataOut channeldata_out = ChannelDataOut.get(peerGID);
		ChannelDataIn channeldata_in = ChannelDataIn.get(peerGID);
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: sendMessage got channel out: "+channeldata_out);
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: sendMessage got channel in: "+channeldata_in);
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
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: send: "+cm);
		
		// confirm the sending into the GUI
		receiver.addSentMessage(cm, peerGID, channeldata_out);
		
		byte[] _msg = cm.encode();	
		plugin_data.PluginRequest  envelope = new plugin_data.PluginRequest();
		envelope.msg = _msg;
		envelope.peer_GID = peerGID; //destination
		envelope.type = plugin_data.PluginRequest.MSG;
		envelope.plugin_GID=(String)Main.getPluginDataHashtable().get("plugin_GID");
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: send envelope: "+envelope);
		Main.enqueue(envelope);
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: sendMessage exit");
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
	    	synchronized(monitor) {
				cmsg = new ChatMessage().decode(new Decoder(msg));
				if (DEBUG) System.out.println("PLUGIN CHAT:  got msg: "+cmsg);
				// extract acknowledgment piggybacked in messages
		    	ChannelDataOut channeldata_out = ChannelDataOut.get(peer_GID);
		        receiver.confirmMessages(cmsg.last_acknowledged_in_sequence, cmsg.received_out_of_sequence, peer_GID, cmsg.session_id_ack,
		        		channeldata_out);
	    	
		        if (cmsg.message_type != ChatMessage.MT_EMPTY)
		        	forwardMsgToReceiverAndSendAckToSender(cmsg, peer_GID);
	    	}
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
    	if (DEBUG) System.out.println("PLUGIN CHAT: Main:  forwardMsgToReceiverAndSendAckToSender: enter");
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
       	if (DEBUG) System.out.println("PLUGIN CHAT: Main:  forwardMsgToReceiverAndSendAckToSender: exit");
    }
    /**
     * Sends an acknowledgment for cmsg.
     * Not setting a sequence number, since the ack message is empty.
     * @param cmsg
     * @param peer_GID
     */
    private static void sendAckMsg(ChannelDataIn channeldata_in, ChatMessage cmsg, String peer_GID){
    	//boolean DEBUG = true;
    	if (DEBUG) System.out.println("PLUGIN CHAT: Main:  sendAckMsg: enter");
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
		//cm.content = null; 	
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
    	if (DEBUG) System.out.println("PLUGIN CHAT: Main:  sendAckMsg: exit");
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

