package net.ddp2p.common.examplePlugin;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.data.D_Peer;
/**
 * @author Marius Silaghi and Khalid Alhamed
 * 
 * Example entry point for a plugin that is GUI independent (i.e. can be used in Android).
 * This plugin example is about chat.
 *
 */
public class Main {
    private static final boolean DEBUG = false;
	static java.util.ArrayList<net.ddp2p.common.plugin_data.PluginRequest> queue = new java.util.ArrayList<net.ddp2p.common.plugin_data.PluginRequest>();
	static String plugin_GID = "Chat_B1_v1.0.0";//"Dong Hang PUBLIC GID ANDROID CHAT version 1.0.0";
	static String peer_GID;
	static String plugin_name = "AndroidChat";
	static String name;
	static ChatReceiver receiver;
	static BigInteger none = BigInteger.ZERO;
	static BigInteger first = BigInteger.ONE;
	static BigInteger crt = BigInteger.ONE;
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
		ChannelDataOut channeldata_out = ChannelDataOut.get(peerGID);
		ChannelDataIn channeldata_in = ChannelDataIn.get(peerGID);
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: sendMessage got channel out: "+channeldata_out);
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: sendMessage got channel in: "+channeldata_in);
    	ChatMessage cm = new ChatMessage();
    	cm.session_id = channeldata_out.getSessionID();
		cm.message_type = ChatMessage.MT_TEXT;
		cm.first_in_this_sequence = channeldata_out.getFirstInSequence();
		ChatElem ce = new ChatElem();
		ce.type = 0; 
		ce.val = Main.name;
		cm.content = new ArrayList<ChatElem>();
		cm.content.add(ce); 
		cm.msg = message;
		cm.session_id_ack = channeldata_in.getSessionID();
		cm.last_acknowledged_in_sequence = channeldata_in.getLastInSequence();
		cm.received_out_of_sequence = channeldata_in.getOutOfSequence();		
		cm.sequence = channeldata_out.getNextSequence();
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: send: "+cm);
		receiver.addSentMessage(cm, peerGID, channeldata_out);
		byte[] _msg = cm.encode();	
		net.ddp2p.common.plugin_data.PluginRequest  envelope = new net.ddp2p.common.plugin_data.PluginRequest();
		envelope.msg = _msg;
		envelope.peer_GID = peerGID; 
		envelope.type = net.ddp2p.common.plugin_data.PluginRequest.MSG;
		envelope.plugin_GID=(String)Main.getPluginDataHashtable().get("plugin_GID");
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: send envelope: "+envelope);
		Main.enqueue(envelope);
		if (DEBUG) System.out.println("PLUGIN CHAT: Main: sendMessage exit");
		return cm;
	}
	    /**
	       Called from DD to retrieve messages
	       DO NOT CHANGE THIS!
	     */
	    public static Hashtable<String,Object> checkOutgoingMessageHashtable() {
	    	if (DEBUG) System.out.println("Plugin:ChatApp:checkOutgoingMessageHashtable: start");
	    	Hashtable<String,Object> pd;
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
	    	return pd;
	    }
	    /**
	         * Required. First procedure ever called. Adjust to your needs.
	     */
	    public static void init() {
	    	if (false) {
				net.ddp2p.common.plugin_data.PluginRequest sentMenuItem = new net.ddp2p.common.plugin_data.PluginRequest();
				sentMenuItem.type = net.ddp2p.common.plugin_data.PluginRequest.REGISTER_MENU;
				sentMenuItem.plugin_GID = plugin_GID;
				sentMenuItem.column = net.ddp2p.common.plugin_data.PluginMenus.COLUMN_MYSELF; 
				enqueue(sentMenuItem);
	    	}	
	}
	/**
	* Interface for communication with main system: to send, simply call enqueue()
	*/
	public static void enqueue(net.ddp2p.common.plugin_data.PluginRequest r) {
		synchronized(queue) {
		        queue.add(r);
		        queue.notify();
		}
	}
	/**
	 Called from outside:
	 */
    public static void handleReceivedMessage(byte[] msg, String peer_GID) {
		if (DEBUG) System.out.println("PLUGIN CHAT: From: "+peer_GID+" got "+net.ddp2p.common.plugin_data.PluginRequest.byteToHex(msg,":"));
	    ChatMessage cmsg=null;
	    try {
			cmsg = new ChatMessage().decode(new Decoder(msg));
			if (DEBUG) System.out.println("PLUGIN CHAT:  got msg: "+cmsg);
	    	ChannelDataOut channeldata_out = ChannelDataOut.get(peer_GID);
	        receiver.confirmMessages(cmsg.last_acknowledged_in_sequence, cmsg.received_out_of_sequence, peer_GID, cmsg.session_id_ack,
	        		channeldata_out);
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
    	if (DEBUG) System.out.println("PLUGIN CHAT: Main:  forwardMsgToReceiverAndSendAckToSender: enter");
    	String msgStr = cmsg.msg;
    	if (DEBUG) System.out.println("From: " + peer_GID + " got: " + msgStr );
    	String peerName = cmsg.getName();
	   	if (DEBUG) System.out.println("-----------receiveTxt(): new message should display");
    	ChannelDataIn channeldata_in = ChannelDataIn.get(peer_GID);
    	if (channeldata_in.registerIncoming(cmsg))	{
			sendAckMsg(channeldata_in, cmsg, peer_GID); 
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
    	if (DEBUG) System.out.println("PLUGIN CHAT: Main:  sendAckMsg: enter");
    	ChannelDataOut cd = ChannelDataOut.get(peer_GID);
     	ChatMessage cm = new ChatMessage();
 		cm.message_type = ChatMessage.MT_EMPTY; 
     	cm.last_acknowledged_in_sequence = channeldata_in.getLastInSequence(); 
     	cm.received_out_of_sequence = channeldata_in.getOutOfSequence();
		cm.session_id_ack = channeldata_in.getSessionID(); 
     	cm.session_id = cd.getSessionID();
		cm.first_in_this_sequence = cd.getFirstInSequence();
		ChatElem ce = new ChatElem();
		ce.type = 0; 
		ce.val = Main.name;
		cm.content = new ArrayList<ChatElem>();
		cm.content.add(ce); 
		cm.msg = null;
    	if (DEBUG) System.out.println("PLUGIN CHAT: sendAckMsg: sending "+cm);
		byte[] _msg = cm.encode();
		net.ddp2p.common.plugin_data.PluginRequest  envelope = new net.ddp2p.common.plugin_data.PluginRequest();
		envelope.msg = _msg;
		envelope.peer_GID = peer_GID; 
		envelope.type = net.ddp2p.common.plugin_data.PluginRequest.MSG;
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
    }
    /** 
       tells you name and GID
     */
    public static void setPluginData(String peer_GID, String name){
		if(DEBUG)System.out.println("Plugin:Chat:setPluginData: "+peer_GID+" name="+name);
		Main.peer_GID = peer_GID;
		Main.name = name;
    }
};
