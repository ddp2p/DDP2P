//package dd_p2p.plugin;
package com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat;
//import dd_p2p.plugin.*;

import java.util.*;
import java.util.Hashtable;
import java.math.BigInteger;
import util.Util;

 public  class ChannelDataIn {
    	private static Hashtable<String, ChannelDataIn> channels = new Hashtable<String, ChannelDataIn>();
    	String peerGID;
    	byte[] session_ID;
    	BigInteger lastInSequence;
    	BigInteger firstInSequence;
    	ArrayList<BigInteger> outOfSequence;
    	BigInteger getLastInSequence() {
    		return lastInSequence;
    	}
    	ArrayList<BigInteger> getOutOfSequence() {
    		return outOfSequence;
    	}
    	void registerIncoming(byte[] _session_ID, BigInteger first, BigInteger in) {
			if (this.session_ID == null) {
				this.session_ID = _session_ID;
				this.firstInSequence = first;
			} else {
				if (!Util.equalBytes(session_ID, _session_ID)) {
					this.session_ID = _session_ID;
					this.firstInSequence = first;
					this.outOfSequence = new ArrayList<BigInteger>();
					if (first.equals(in))
						this.lastInSequence = in;
					else
						this.outOfSequence.add(in);
					return;
				}
			}
			// when to increment last?
			System.out.println("PLUGIN CHAT: l="+lastInSequence+" f="+firstInSequence+" in="+in);
			if ( 
			    ( in != null )
			    &&
			    (
				(
				    (this.lastInSequence == null) 
				    &&
				    (this.firstInSequence.equals(in))
				)
				|| 
				(
				    (lastInSequence != null)
				    &&
				    in.equals(lastInSequence.add(BigInteger.ONE))
				)
			    )
			    )
			    {
    			lastInSequence = in;
    			
    			for(;;) {
    				if (outOfSequence == null || outOfSequence.size() == 0) break;
    				if (outOfSequence.get(0).equals(lastInSequence.add(BigInteger.ONE))) {
    					lastInSequence = outOfSequence.get(0);
    					outOfSequence.remove(0);
    				} else {
    					break;
    				}
    			}
    		} else {
			    if (outOfSequence == null)
				outOfSequence = new ArrayList<BigInteger>();
			    insertInOrdered(outOfSequence, in);
    		}
    	}
    	static void insertInOrdered(ArrayList<BigInteger> list, BigInteger i) {
    		for (int k=0; k< list.size(); k++) {
    			if (i.compareTo(list.get(k))<0) {
    				list.add(k, i); return;
    			} else {
    				k++;
    			}
    		}
    		list.add(i);
    	}
    	static ChannelDataIn get(String peerGID) {
    		ChannelDataIn ch = channels.get(peerGID);
    		if (ch == null) {
    			ch = new ChannelDataIn();
    			ch.peerGID = peerGID;
    			//ch.session_ID = ChatMessage.createSessionID();
    			//ch.next_sequence = ch.firstInSequence = BigInteger.ONE;
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


 