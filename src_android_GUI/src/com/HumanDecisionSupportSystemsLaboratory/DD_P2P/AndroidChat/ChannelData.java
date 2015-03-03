/*   Copyright (C) 2014  Authors: Hang Dong <hdong2012@my.fit.edu>, Khalid Alhamed, Marius Silaghi <silaghi@fit.edu>
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
//import dd_p2p.plugin.*;
import java.util.Hashtable;

 public  class ChannelData {
    	private static Hashtable<String, ChannelData> channels = new Hashtable<String, ChannelData>();
    	String peerGID;
    	byte[] session_ID;
    	BigInteger next_sequence;
    	BigInteger firstInSequence;
    	BigInteger getNextSequence() {
    		BigInteger sequence = next_sequence;
    		next_sequence = sequence.add(BigInteger.ONE);
    		return sequence;
    	}
    	static ChannelData get(String peerGID) {
    		ChannelData ch = channels.get(peerGID);
    		if (ch == null) {
    			ch = new ChannelData();
    			ch.peerGID = peerGID;
    			ch.session_ID = ChatMessage.createSessionID();
    			ch.next_sequence = ch.firstInSequence = BigInteger.ONE;
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
