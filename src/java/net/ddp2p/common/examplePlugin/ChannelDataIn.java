package net.ddp2p.common.examplePlugin;
import java.util.*;
import java.math.BigInteger;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.util.Util;
/**
 * 
 * @author msilaghi
 * Class for handling received data, compacting sequences for acknowledgments.
 */
 public  class ChannelDataIn {
	 public static final int MAX_OLD_MESSAGES = 100;
	 private static final boolean DEBUG = false;
	 private static Hashtable<String, ChannelDataIn> channels = new Hashtable<String, ChannelDataIn>();
	 public String peerGID;
	 public byte[] session_ID;
	 public BigInteger lastInSequence;
	 public BigInteger firstInSequence;
	 public ArrayList<BigInteger> outOfSequence = new ArrayList<BigInteger>();
	 public String peerName;
	 public Calendar time;
	 public ArrayList<DatedChatMessage> messages = new ArrayList<DatedChatMessage>();
	 public ArrayList<DatedChatMessage> messages_old = new ArrayList<DatedChatMessage>();
	 public String toString() {
		 return 
	   				 "ChannelDataOut["
	   				 + "\n peerGID = "+peerGID
	   				 + "\n session_ID = "+Util.byteToHex(session_ID)
	   				 + "\n firstInSequence = "+firstInSequence
	   				 + "\n lastInSequence = "+lastInSequence
	   				 + "\n recv = "+Util.concat(outOfSequence, " , ", null)
	   				 + "\n peerName = "+peerName
	   				 + "\n time = "+Encoder.getGeneralizedTime(time)
	   				 + "\n msg = "+Util.concat(messages, "\n m=", null)
	   				 + "\n old = "+Util.concat(messages_old, "\n o=", null)
	   				 + "\n]";
	 }
	 BigInteger getLastInSequence() {
		 return lastInSequence;
	 }
	 ArrayList<BigInteger> getOutOfSequence() {
		 return outOfSequence;
	 }
	 public void setNewSession(byte[] _session_id, BigInteger _first_in_this_sequence, BigInteger sequence_crt_in) {
		 if (DEBUG) System.out.println("ChannelDataIn: setNewSession: in="+Util.byteToHex(_session_id)+" vs "+this.session_ID);
		 this.firstInSequence = _first_in_this_sequence;
		 this.session_ID = _session_id;
		 this.time = Util.CalendargetInstance();
		 int old_size = messages_old.size();
		 this.messages_old.addAll(messages);
		 /**
		  * May search for old occurrences of this session in messages_old (also setting lastInSequence & outOfSequence);
		  */
		 this.messages = new ArrayList<DatedChatMessage>();
		 this.outOfSequence = new ArrayList<BigInteger>();
		 for (int index = 0; index < old_size; ) {
			 DatedChatMessage dcm = this.messages_old.get(index);
			 if (! Util.equalBytes_null_or_not(_session_id, dcm.msg.session_id)) { index ++; continue; }
			 if (! _first_in_this_sequence.equals(dcm.msg.first_in_this_sequence)) { index ++; continue; }
			 this.messages.add(dcm);
			 if (this.lastInSequence == null) {
				 if (_first_in_this_sequence.equals(dcm.msg.sequence))
					 this.lastInSequence = dcm.msg.sequence;
				 else
					 this.outOfSequence.add(dcm.msg.sequence);
			 } else {
				 if (this.lastInSequence.add(BigInteger.ONE).equals(dcm.msg.sequence)) {
					 this.lastInSequence = dcm.msg.sequence;
				 } else {
					 this.outOfSequence.add(dcm.msg.sequence);
				 }
			 }
			 this.messages.remove(index);
			 old_size --;
		 }
		 if (_first_in_this_sequence.equals(sequence_crt_in))
			 this.lastInSequence = sequence_crt_in;
		 else
			 this.outOfSequence.add(sequence_crt_in);
		 if (DEBUG) System.out.println("ChannelDataIn: setNewSession: exit with="+this);
	 }
	 /**
	  * Updating the session, sequence and out_of_sequence data.
	  * @param cmsg
	  * @return
	  */
	 boolean registerIncoming(ChatMessage cmsg) {
		 if ( cmsg.sequence == null ) {
			 if (DEBUG) System.out.println("PLUGIN CHAT: ChannelDataIn: register Incomming: why null seq ="+cmsg);
			 return false;
		 }
		 return this.registerIncoming(cmsg.session_id, cmsg.first_in_this_sequence , cmsg.sequence);
	 }
	 private boolean registerIncoming(byte[] _session_ID, BigInteger first, BigInteger sequence_crt_in) {
		 if (this.session_ID == null) {
			 this.session_ID = _session_ID;
			 this.firstInSequence = first;
		 } else {
			 if (! Util.equalBytes(session_ID, _session_ID)) {
				 setNewSession (_session_ID, first, sequence_crt_in);
				 return true;
			 }
		 }
		 if (DEBUG) System.out.println("PLUGIN CHAT: l="+lastInSequence+" f="+firstInSequence+" in="+sequence_crt_in);
		 if ( 
				 (
						 (
								 (this.lastInSequence == null) 
								 &&
								 (this.firstInSequence.equals(sequence_crt_in))
    						)
    						|| 
    						(
    								(lastInSequence != null)
    								&&
    								sequence_crt_in.equals(lastInSequence.add(BigInteger.ONE))
    						)
    				)
				 )
		 { 
			 lastInSequence = sequence_crt_in;
			 for(;;) {
    				if (outOfSequence == null || outOfSequence.size() == 0) break;
    				if (outOfSequence.get(0).equals(lastInSequence.add(BigInteger.ONE))) {
    					lastInSequence = outOfSequence.get(0);
    					outOfSequence.remove(0);
    				} else {
    					break;
    				}
    			}
    			return true;
    		} else {
    			if (firstInSequence != null  && sequence_crt_in.compareTo(this.firstInSequence) <= 0)
    				return false; 
			    if (outOfSequence == null)
			    	outOfSequence = new ArrayList<BigInteger>();
			    return insertInOrderedBinary(outOfSequence, sequence_crt_in);
    		}
	 }
    	/**
    	 * Used to insert a new received message in the list of outOfSequence.
    	 * TODO use binary search.
    	 * @param list
    	 * @param i
    	 */
    	static boolean _insertInOrdered(ArrayList<BigInteger> list, BigInteger i) {
    		for (int k = 0; k < list.size(); k ++) {
    			if (i.compareTo(list.get(k)) < 0) {
    				list.add(k, i); return true;
    			} else {
        			if (i.compareTo(list.get(k)) == 0) { 
        				return false;
        			}
    				k++;
    			}
    		}
    		list.add(i);
    		return true;
    	}
    	static boolean insertInOrderedBinary(ArrayList<BigInteger> list, BigInteger i) {
    		if (list.size() == 0) {
    			list.add(i);
    			return true;
    		}
    		return insertInOrderedBinary(list, i, 0, list.size()-1);
    	}
       	static boolean insertInOrderedBinary(ArrayList<BigInteger> list, BigInteger i, int b, int e) {
       		if (b > e) {
       			list.add(b, i);
       			return true;
       		}
       		if (b == e) {
           		int cmp = i.compareTo(list.get(b));
      			if (cmp == 0) {
       				return false;
       			} else {
       				if (cmp < 0) {
       					list.add(b, i);
       					return true;
       				} else {
       					list.add(e + 1, i);
       					return true;
       				}
       			}
       		}
       		int m = (b + e)/2;
       		int cmp = i.compareTo(list.get(m));
       		if (cmp == 0) {
   				return false;
       		}
       		if (cmp < 0) {
       			return insertInOrderedBinary(list, i, b, m);
       		} else {
       			return insertInOrderedBinary(list, i, m+1, e);
       		}
    	}
    	/**
    	 * Create a new channel if absent.
    	 * Only sets the peerGID.
    	 * s
    	 * @param peerGID
    	 * @return
    	 */
    	static ChannelDataIn get(String peerGID) {
    		ChannelDataIn ch = channels.get(peerGID);
    		if (ch == null) {
    			ch = new ChannelDataIn();
    			ch.peerGID = peerGID;
    			channels.put(peerGID, ch);
    		}
    		return ch;
    	}
    	BigInteger getFirstInSequence() {
    		return firstInSequence;
    	}
    	byte[] getSessionID() {
    		return session_ID;
    	}
    	public static void main(String[] args) {
    		ArrayList<BigInteger> list = new ArrayList<BigInteger>();
    		list.add(new BigInteger("10"));
    		list.add(new BigInteger("12"));
    		list.add(new BigInteger("13"));
    		list.add(new BigInteger("20"));
    		list.add(new BigInteger("25"));
    		boolean r = ChannelDataIn.insertInOrderedBinary(list, new BigInteger(args[0]));
    		for (BigInteger a : list) {
    			System.out.println("=>"+a);
    		}
   			System.out.println("r=>"+r);
    	}
    }
