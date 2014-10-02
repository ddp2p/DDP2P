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
    		if ( ((this.lastInSequence == null) && (this.firstInSequence.equals(in)))
    			|| in.equals(lastInSequence.add(BigInteger.ONE))) {
    			lastInSequence = in;
    			
    			for(;;) {
    				if(outOfSequence.size() == 0) break;
    				if (outOfSequence.get(0).equals(lastInSequence.add(BigInteger.ONE))) {
    					lastInSequence = outOfSequence.get(0);
    					outOfSequence.remove(0);
    				} else {
    					break;
    				}
    			}
    		} else {
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


 