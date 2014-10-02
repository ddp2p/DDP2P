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
