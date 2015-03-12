//package AndroidChat;
//package chatApp;

package dd_p2p.plugin;

import java.math.BigInteger;

public interface ChatReceiver {

     public void receive(BigInteger first, BigInteger sequence, String msg, String peer_GID);
     /**
      * // images/videos later
      * @param first
      * @param sequence
      * @param msg
      * @param peer_GID
      */
     public void receive(BigInteger first, BigInteger sequence, ChatElem msg, String peer_GID);
     /**
      * Acknowledged the sequence from peer_GID
      * @param sequence
      * @param peer_GID
      */
     public void ack(BigInteger sequence, BigInteger[] out_of_sequence, String peer_GID);

 }