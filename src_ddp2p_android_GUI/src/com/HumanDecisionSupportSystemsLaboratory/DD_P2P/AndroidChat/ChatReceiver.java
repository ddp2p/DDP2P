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
package com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat;
//package chatApp;
//package dd_p2p.plugin;

import java.math.BigInteger;
import java.util.ArrayList;
/**

You only need to implement a class "AndroidChatReceiver"
that implements the interface "ChatReceiver" with its two functions
to receive a message, and a confirmation.

class AndroidChatReceiver implements ChatReceiver {
    // see documentation in file ChatReceiver.java
    public void receiveMessage(BigInteger first_in_this_sequence,
            BigInteger sequence, String msgStr, String peerName,
            String peer_GID, byte[] session_id, ChatMessage cmsg) {
        // TODO ...
    }
    // see documentation in file ChatReceiver.java
    public void confirmMessages(BigInteger sequence,
            ArrayList<BigInteger> out_of_sequence, String peer_GID,
            byte[] session_id) {
        // TODO ...
    }
}

Then somewhere in the initialization of your system, call:
AndroidChat.Main.receiver = new AndroidChatReceiver();

To send messages, call the function:
  
ChatMessage Main.sendMessage(String message, String peerGID, D_Peer peer);

The result of this function contains the "session_id and sequence" of your sent messages. Store them such that
you can flag in GUI their reception, confirmation that will contain this field.

 *
 */
public
interface ChatReceiver {
	public void addSentMessage(ChatMessage cm, String peer_GID, ChannelDataOut crt);
	/**
	 * 
	 * @param first_in_this_sequence : what was the sequence nb of the first message?
	 * @param sequence : the sequence number of the current message
	 * @param msgStr : does the message contain a simple text? Then here it is (if not null)
	 * @param peerName : did the peer change its name (then this is not null)
	 * @param peer_GID : the GID of the peer sending the message
	 * @param session_id : a unique identifier of the communication (on change, start from scratch)
	 * @param cmsg : The actual message received (with all fields), may not need to be processed
	 */
	public boolean receiveMessage(BigInteger first_in_this_sequence,
			BigInteger sequence, String msgStr, String peerName,
			String peer_GID, byte[] session_id, ChatMessage cmsg, ChannelDataIn crt); 
	/**
	 * Mark that this message sent by you has arrived...
	 * @param sequence : all sent messages with sequences up to this value have arrived at destination
	 * @param out_of_sequence : also the messages with these sequence numbers have arrived.
	 * @param peer_GID : This is the peer that acknowledges;
	 * @param session_id : this is the session (if you no not have, then discard)
	 */
	public void confirmMessages(BigInteger sequence,
			ArrayList<BigInteger> out_of_sequence, String peer_GID,
			byte[] session_id, ChannelDataOut crt);
	
	public void jamMessages(BigInteger sequence,
			String peer_GID,
			byte[] session_id, ChannelDataOut crt);
}
