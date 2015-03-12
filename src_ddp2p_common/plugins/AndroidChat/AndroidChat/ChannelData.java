//package dd_p2p.plugin;
package com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat;
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
