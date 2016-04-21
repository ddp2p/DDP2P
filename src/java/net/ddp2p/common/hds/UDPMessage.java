package net.ddp2p.common.hds;
import java.net.SocketAddress;
import net.ddp2p.common.util.Util;
public class UDPMessage {
	private static final boolean DEBUG = false;
	public int type;
	UDPFragment[] fragment;
	byte[] transmitted;
	int[] sent_attempted; 
	int unacknowledged = 0;
	long date;
	int received;
	public String msgID;
	SocketAddress sa;
	UDPFragmentAck uf;
	byte[] ack;
	boolean ack_changed=false;
	Object lock_ack=new Object();
	public String destination_GID, sender_GID, sender_instance;
	public int checked = 0; 
	public String toString() {
		String res = "UDPMesage:" +
				" ID="+msgID+
				"\n type="+type+
				"\n date: "+date+"ms"+
				"\n unack: "+unacknowledged+
				"\n rec="+received+"/"+fragment.length+
		"\n transmitted=";
		for (int k = 0; k < fragment.length; k ++)
			res+="."+transmitted[k];
		res += "\n attempts";
		for (int k = 0; k < fragment.length; k ++)
			res+="."+sent_attempted[k];
		return res;
	}
	public UDPMessage(int len) {
		fragment = new UDPFragment[len];
		transmitted = new byte[len];
		sent_attempted = new int[len];
		date = Util.CalendargetInstance().getTimeInMillis();
		uf = new UDPFragmentAck();
	}
	public byte[] assemble() {
		if(received<fragment.length) return null;
		int MTU = fragment[0].data.length;
		int msglen =(fragment.length-1)*MTU + 
			fragment[fragment.length-1].data.length;
		byte[] msg = new byte[msglen];
		for(int i = 0; i < fragment.length; i++) {
			Util.copyBytes(msg, i*MTU, fragment[i].data, 
					fragment[i].data.length, 0);
		}
		return msg;
	}
	public boolean ready_to_assemble() {
		if (received < fragment.length) return false;
		return true;
	}
	public boolean no_ack_received() {
		for(int i = 0; i < fragment.length; i ++) {
			if(this.transmitted[i] == 1) return false;
		}
		return true;
	}
	public static UDPMessage reconstruct(UDPMessage candidate, UDPFragment frag) {
		SocketAddress sa = null;
	    UDPMessage umsg = candidate;
	    if (umsg == null || ! Util.equalStrings_null_or_not(umsg.msgID, frag.msgID))
	    	{
	    		/**
	    		 * For new messages
	    		 */
	    		umsg = new UDPMessage(frag.fragments);
	    		umsg.uf.msgID = frag.msgID;
	    		umsg.uf.destinationID = frag.senderID;
	    		umsg.uf.transmitted = umsg.transmitted;
	    		umsg.type = frag.msgType;
	    		umsg.sa = sa;
	    		umsg.destination_GID = frag.destinationID;
	    		umsg.sender_GID = frag.senderID;
	    		umsg.msgID = frag.msgID;
	    		if (DEBUG) System.out.println("Starting new message: "+umsg);
	    	}
	    /**
	     * add new fragment
	     */
	    if (frag.sequence >= umsg.fragment.length) {
	    	if(DEBUG)System.err.println("Failure sequence: "+frag.sequence+" vs. "+umsg.fragment.length);
	    	return null;
	    }
	    /**
	     * Update date of last contact
	     */
	    umsg.date = Util.CalendargetInstance().getTimeInMillis();
	    /**
	     * Set the flag for having received this
	     */
	    if (umsg.transmitted[frag.sequence] == 0) {
	    	umsg.transmitted[frag.sequence] = 1;
	    	umsg.received ++;
	    	umsg.ack_changed = true;
	    	umsg.fragment[frag.sequence] = frag;
	    }
		return umsg;
	}
}
