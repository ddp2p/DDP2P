package net.ddp2p.common.hds;
import java.net.SocketAddress;
import java.util.Hashtable;
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
		int MTU = fragment[0].getData().length;
		int msglen =(fragment.length-1)*MTU + 
			fragment[fragment.length-1].getData().length;
		byte[] msg = new byte[msglen];
		for(int i = 0; i < fragment.length; i++) {
			Util.copyBytes(msg, i*MTU, fragment[i].getData(), 
					fragment[i].getData().length, 0);
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
		return reconstruct(candidate, frag, sa);
	}
	public static UDPMessage reconstruct(UDPMessage candidate, UDPFragment frag, SocketAddress sa) {
	    UDPMessage umsg = candidate;
	    if (umsg == null || ! Util.equalStrings_null_or_not(umsg.msgID, frag.getMsgID()))
	    	{
	    		/**
	    		 * For new messages
	    		 */
	    		umsg = new UDPMessage(frag.getFragments());
	    		umsg.uf.msgID = frag.getMsgID();
	    		umsg.uf.destinationID = frag.getSenderID();
	    		umsg.uf.transmitted = umsg.transmitted;
	    		umsg.type = frag.getMsgType();
	    		umsg.sa = sa;
	    		umsg.destination_GID = frag.getDestinationID();
	    		umsg.sender_GID = frag.getSenderID();
	    		umsg.msgID = frag.getMsgID();
	    		if (DEBUG) System.out.println("Starting new message: "+umsg);
	    	}
	    /**
	     * add new fragment
	     */
	    if (frag.getSequence() >= umsg.fragment.length) {
	    	if(DEBUG)System.err.println("Failure sequence: "+frag.getSequence()+" vs. "+umsg.fragment.length);
	    	return null;
	    }
	    /**
	     * Update date of last contact
	     */
	    umsg.date = Util.CalendargetInstance().getTimeInMillis();
	    /**
	     * Set the flag for having received this
	     */
	    if (umsg.transmitted[frag.getSequence()] == 0) {
	    	umsg.transmitted[frag.getSequence()] = 1;
	    	umsg.received ++;
	    	umsg.ack_changed = true;
	    	umsg.fragment[frag.getSequence()] = frag;
	    }
		return umsg;
	}
	/**
	 * Can add a fragment to bag and return a message (if ready to assemble()).
	 * @param frag
	 * @param factory
	 * @param sa
	 * @return
	 */
	public static UDPMessage considerFragment(UDPFragment frag, Hashtable<String, UDPMessage> factory, SocketAddress sa) {
		UDPMessage msg = factory.get(frag.getMsgID());
		if (msg == null) {
			msg = UDPMessage.reconstruct(null, frag, sa);
			factory.put(frag.getMsgID(), msg);
		}
			else
					UDPMessage.reconstruct(null, frag, sa);
		if (msg.ready_to_assemble()) {
			factory.remove(msg.msgID);
			return msg;
		}
		return null;
	}
}
