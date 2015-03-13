package dd_p2p.plugin;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
//import dd_p2p.plugin.*;
import java.util.Hashtable;

import util.Util;

/**
 * 
 * @author msilaghi
 * This class store a channel for sending data OUT (managing the sequence and session number to be used in the next message
 * for each peer.
 */
 public  class ChannelDataOut {
	 /**
	  * A ChannelData for each peer_GID.
	  */
	 private static Hashtable<String, ChannelDataOut> channels = new Hashtable<String, ChannelDataOut>();
	 String peerGID;
	 byte[] session_ID;
	 BigInteger next_sequence;
	 BigInteger firstInSequence;
	 
	 String peerName;  // last name known for the peer
	 BigInteger sequence_ack;  // last acknowledged in sequence
	 Calendar time; // time when the session info was created
	 
	 ArrayList<DatedChatMessage> messages = new ArrayList<DatedChatMessage>(); // the list of all messages on this channel & session
	 ArrayList<Boolean> received = new ArrayList<Boolean>();   // boolean array to signal what was confirmed
	 ArrayList<Boolean> jam = new ArrayList<Boolean>();  		// boolean array to signal what could not be sent (buffers full)
	 
	 public String toString() {
		 return 
				 "ChannelDataOut["
				 + "\n peerGID = "+peerGID
				 + "\n session_ID = "+Util.byteToHex(session_ID)
				 + "\n firstInSequence = "+firstInSequence
				 + "\n next_sequence = "+next_sequence
				 + "\n peerName = "+peerName
				 + "\n sequence_ack = "+sequence_ack
				 + "\n time = "+time
				 + "\n msg = "+Util.concat(messages, "\n msg=", null)
				 + "\n recv = "+Util.concat(received, ", ", null)
				 + "\n]";
	 }
    	
	 /**
	  * Returns crt next_sequence value, then increments the value.
	  * @return
	  */
	 BigInteger getNextSequence() {
		 BigInteger sequence = next_sequence;
		 next_sequence = sequence.add(BigInteger.ONE);
		 return sequence;
	 }
	 /**
	  * If absent, then create and return one.
	  * New ones are inited with a new session_ID and a sequence set to ONE
	  * @param peerGID
	  * @return
	  */
	 static ChannelDataOut get(String peerGID) {
		 ChannelDataOut ch = channels.get(peerGID);
		 if (ch == null) {
			 ch = new ChannelDataOut();
			 ch.peerGID = peerGID;
			 ch.session_ID = ChatMessage.createSessionID();
			 ch.next_sequence = ch.firstInSequence = BigInteger.ONE;
			 ch.time = Util.CalendargetInstance();
			 //ch.messages = new ArrayList<DatedChatMessage>();
			 channels.put(peerGID, ch);
		 }
		 return ch;
	 }
	 BigInteger getFirstInSequence() {
		 return firstInSequence;
	 }
	 byte[] getSessionID(){
		 return session_ID;
	 }
 }
