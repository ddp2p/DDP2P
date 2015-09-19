package util.tools;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_RecommendationOfTestersBundle;
import net.ddp2p.common.data.D_TesterRatingByPeer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.recommendationTesters.MessageReceiver;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB;

public class RecommendationOfTesterTestTool {
	
	private static D_RecommendationOfTestersBundle
	buildRecommendationOfTestersMessage(String senderPeerGIDH) {
		D_RecommendationOfTestersBundle testerItemsMsg = new D_RecommendationOfTestersBundle();
		testerItemsMsg.senderGIDH = senderPeerGIDH;
		testerItemsMsg.creation_date = Util.CalendargetInstance();
		//testerItemsMsg.creation_date.add(Calendar.DAY_OF_MONTH, -5);
		D_TesterRatingByPeer testerRating;
		for (int i = 1; i < 4; i ++){
			testerRating = new D_TesterRatingByPeer();
			testerRating.testerGID = ""+i*100; // we need a correct GID
			testerRating._weight= i * 10;
			testerRating.weight = ""+testerRating._weight;
			testerRating.address = "http://www.testers.com/tester"+i;
			testerItemsMsg.testersRatingList.add(testerRating);
		}
		testerItemsMsg.signature = testerItemsMsg.sign();
		return testerItemsMsg;
	}
	public static void main (String args[]){
		boolean _DEBUG = true;
		try {
			Vendor_JDBC_EMAIL_DB.initJDBCEmail();
			Application.setDB(new net.ddp2p.common.util.DBInterface(Application.DELIBERATION_FILE));
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		MessageReceiver mr = new MessageReceiver();
		Identity.getCurrentPeerIdentity_NoQuitOnFailure();
		//D_Peer.createPeerAndSetMyself("khalid alhamed", "kalhamed2011@my.fit.edu", "unitTest", Cipher.RSA, Cipher.SHA256, 2048);
		
		D_Peer senderPeer = HandlingMyself_Peer.get_myself_or_null();
		// use myself as a sender for debug purpose (send to myself from myself)
		//D_Peer senderPeer = D_Peer.getPeerByLID(1, false, false); 
		if (senderPeer ==null){
			if (_DEBUG) System.out.println("myPeer is null!!!!");
			return;
		}
		
		D_RecommendationOfTestersBundle testerItemsMsg1 = buildRecommendationOfTestersMessage(senderPeer.getGIDH());
		// encode and decode msg.
		byte[] msg = testerItemsMsg1.encode();
		//send and receive
		D_RecommendationOfTestersBundle testerItemsMsg2 = null;
		try {
			testerItemsMsg2 = new D_RecommendationOfTestersBundle().decode(new Decoder(msg));
		} catch (ASN1DecoderFail e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		mr.msgListener(senderPeer, testerItemsMsg2);	
		/*try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}