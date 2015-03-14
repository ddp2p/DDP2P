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
//package dd_p2p.plugin;
package com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
//import dd_p2p.plugin.*;
import java.util.Hashtable;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.util.Util;

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
				 + "\n time = "+Encoder.getGeneralizedTime(time)
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
